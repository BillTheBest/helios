/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.ot.helios;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.helios.ot.endpoint.AbstractEndpoint;
import org.helios.ot.endpoint.EndpointConnectException;
import org.helios.ot.endpoint.EndpointTraceException;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.SocketChannel;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

/**
 * <p>Title: HeliosEndpoint</p>
 * <p>Description: OpenTrace endpoint optimized for the Helios OT Server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.HeliosEndpoint</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public class HeliosEndpoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T> implements UncaughtExceptionHandler {
	/** The helios OT server host name or ip address */
	protected String host;
	/** The helios OT server listening port */
	protected int port;
	/** The helios OT server comm protocol */
	protected Protocol protocol;
	/** The channel socket factory */
	protected NioClientSocketChannelFactory socketChannelFactory;
	/** The channel socket  */
	protected SocketChannel socketChannel;
	
	/** The boss thread pool */
	protected Executor bossExecutor;
	/** The worker thread pool */
	protected Executor workerExecutor;
	/** The client bootstrap */
	protected ClientBootstrap bootstrap; 
	/** The pipeline factory */
	protected ChannelPipelineFactory channelPipelineFactory;
	/** The connection's channel future */
	protected ChannelFuture channelFuture;
	
	/** The count of exceptions */
	protected final AtomicLong exceptionCount = new AtomicLong(0);
	
	/** A set of connect listeners that will be added when an asynch connect is initiated */
	protected final Set<ChannelFutureListener> connectListeners = new CopyOnWriteArraySet<ChannelFutureListener>();
	
	
	/**  */
	private static final long serialVersionUID = -433677190518825263L;

	
	/**
	 * Creates a new HeliosEndpoint from system properties and an optional external XML file
	 */
	public HeliosEndpoint() {
		// Read the basic config
		host = HeliosEndpointConstants.getHost();
		port = HeliosEndpointConstants.getPort();
		protocol = HeliosEndpointConstants.getProtocol();
		// Initialize the boss worker pool
		bossExecutor = ExecutorBuilder.newBuilder()
				.setCoreThreads(1)
				.setCoreThreadTimeout(false)
				.setDaemonThreads(true)
				.setExecutorType(true)
				.setFairSubmissionQueue(false)
				.setKeepAliveTime(15000)
				.setMaxThreads(3)
				.setJmxDomains(JMXHelper.getRuntimeHeliosMBeanServer().getDefaultDomain())
				.setPoolObjectName(new StringBuilder("org.helios.endpoints:name=").append(getClass().getSimpleName()).append(",service=ThreadPool,type=Boss,protocol=").append(protocol.name()))
				.setPrestartThreads(1)
				.setTaskQueueSize(100)
				.setTerminationTime(5000)
				.setThreadGroupName(getClass().getSimpleName() + "BossThreadGroup")
				.setUncaughtExceptionHandler(this)
				.build();
		// Initialize the worker worker pool
		workerExecutor = ExecutorBuilder.newBuilder()
				.setCoreThreads(5)
				.setCoreThreadTimeout(false)
				.setDaemonThreads(true)
				.setExecutorType(true)
				.setFairSubmissionQueue(false)
				.setKeepAliveTime(15000)
				.setMaxThreads(100)
				.setJmxDomains(JMXHelper.getRuntimeHeliosMBeanServer().getDefaultDomain())
				.setPoolObjectName(new StringBuilder("org.helios.endpoints:name=").append(getClass().getSimpleName()).append(",service=ThreadPool,type=Worker,protocol=").append(protocol.name()))
				.setPrestartThreads(1)
				.setTaskQueueSize(1000)
				.setTerminationTime(5000)
				.setThreadGroupName(getClass().getSimpleName() + "WorkerThreadGroup")
				.setUncaughtExceptionHandler(this)
				.build();
	    // Configure the client.
		socketChannelFactory = new NioClientSocketChannelFactory(bossExecutor, workerExecutor);
		bootstrap = new ClientBootstrap(socketChannelFactory);
		// Set up the pipeline factory.
		channelPipelineFactory = new ChannelPipelineFactory() {
	          public ChannelPipeline getPipeline() throws Exception {
	              return Channels.pipeline(
	                      new ObjectEncoder(),
	                      new ObjectDecoder());
	          }
		};	                     
		bootstrap.setPipelineFactory(channelPipelineFactory);
		bootstrap.setOption("remoteAddress", new InetSocketAddress(host, port));
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("receiveBufferSize", 1048576);
		bootstrap.setOption("sendBufferSize", 1048576);
		bootstrap.setOption("keepAlive", true);
		bootstrap.setOption("connectTimeoutMillis", 2000);


		/*
		 * OTHER OPTIONS
		 * =============
		"localAddress" ....
		"keepAlive"	setKeepAlive(boolean)
		"reuseAddress"	setReuseAddress(boolean)
		"soLinger"	setSoLinger(int)
		"tcpNoDelay"	setTcpNoDelay(boolean)
		"receiveBufferSize"	setReceiveBufferSize(int)
		"sendBufferSize"	setSendBufferSize(int)
		"trafficClass"	setTrafficClass(int)
		"bufferFactory"	setBufferFactory(ChannelBufferFactory)
		"connectTimeoutMillis"	setConnectTimeoutMillis(int)
		"pipelineFactory"	setPipelineFactory(ChannelPipelineFactory)
		 */
	}
	
	/**
	 * Adds a channel listener
	 * @param listener the listener
	 * @see org.jboss.netty.channel.ChannelFuture#addListener(org.jboss.netty.channel.ChannelFutureListener)
	 */
	public void addChannelConnectedListener(ChannelFutureListener listener) {
		if(listener!=null) {
			connectListeners.add(listener);
		}
		//channelFuture.addListener(listener);
	}
	

	/**
	 * Removes a channel listener
	 * @param listener the listener
	 * @see org.jboss.netty.channel.ChannelFuture#removeListener(org.jboss.netty.channel.ChannelFutureListener)
	 */
	public void removeChannelConnectedListener(ChannelFutureListener listener) {
		if(listener!=null) {
			connectListeners.remove(listener);
		}		
		//channelFuture.removeListener(listener);
	}
	
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger LOG = Logger.getLogger(HeliosEndpoint.class);
		LOG.info("Test");
		HeliosEndpoint he = new HeliosEndpoint();
		boolean b = he.connect();
		LOG.info("Connected:"+b);
		for(int i = 0; i < 1000; i++) {
			try {
				he.socketChannel.write(new Date());
				Thread.sleep(5000);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		try { Thread.currentThread().join(); } catch (Exception e) {}
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error("Uncaught exception on thread [" + t + "]", e);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#newBuilder()
	 */
	@Override
	public org.helios.ot.endpoint.AbstractEndpoint.Builder newBuilder() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#connectImpl()
	 */
	@Override
	protected void connectImpl() throws EndpointConnectException {
		channelFuture = bootstrap.connect();
		for(ChannelFutureListener listener: connectListeners) {
			channelFuture.addListener(listener);
		}
		channelFuture.awaitUninterruptibly();
		 if (channelFuture.isCancelled()) {
			 throw new EndpointConnectException("Connection request cancelled");
		 } else if (!channelFuture.isSuccess()) {
			 throw new EndpointConnectException(channelFuture.getCause().getMessage());		     
		 } else {
			 // no exception means a good connect
			 socketChannel = (SocketChannel)channelFuture.getChannel();
			 log.info("Socket Channel Connected [" + socketChannel + "]");
			 socketChannel.getCloseFuture().addListener(new ChannelFutureListener(){
				 @Override
				public void operationComplete(ChannelFuture future)throws Exception {
					 log.warn("\n\tHELIOS ENDPOINT DISCONNECTED:" + future + "\n\t");
				}
			 });
		 }
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#disconnectImpl()
	 */
	@Override
	protected void disconnectImpl() {
		final ClientSocketChannelFactory finalFactory = socketChannelFactory;		
		channelFuture.getChannel().getCloseFuture().addListener(new ChannelFutureListener(){
			public void operationComplete(ChannelFuture future) throws Exception {				
				finalFactory.releaseExternalResources();				
			}
		});		 
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#processTracesImpl(org.helios.ot.tracer.disruptor.TraceCollection)
	 */
	@Override
	protected boolean processTracesImpl(TraceCollection<T> traceCollection) throws EndpointConnectException, EndpointTraceException {
		return false;
	}

	
	

	/**
	 * Returns the helios OT server host name or ip address
	 * @return the host
	 */
	@JMXAttribute(name="Host", description="The helios OT server host name or ip address", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getHost() {
		return host;
	}

	/**
	 * Returns the helios OT server listening port
	 * @return the port
	 */
	@JMXAttribute(name="Port", description="The helios OT server listening port", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getPort() {
		return port;
	}

	/**
	 * Returns the helios OT server comm protocol
	 * @return the protocol
	 */
	@JMXAttribute(name="Protocol", description="The helios OT server comm protocol", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getProtocol() {
		return protocol.name();
	}

	/**
	 * Returns the cumulative exception count
	 * @return the exceptionCount
	 */
	@JMXAttribute(name="ExceptionCount", description="The cumulative exception count", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getExceptionCount() {
		return exceptionCount.get();
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {
	    StringBuilder retValue = new StringBuilder("HeliosEndpoint [")
	        .append("host:").append(this.host)
	        .append(" port:").append(this.port)
	        .append(" protocol:").append(this.protocol)
	        .append(" connected:").append(isConnected.get())
	        .append("]");    
	    return retValue.toString();
	}






}
