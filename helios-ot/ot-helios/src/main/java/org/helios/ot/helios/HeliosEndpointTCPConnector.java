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

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.Executor;

import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.helios.ot.trace.Trace;
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
 * <p>Title: HeliosEndpointTCPConnector</p>
 * <p>Description: UDP Netty Connector for {@link HeliosEndpoint}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.HeliosEndpointTCPConnector</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public class HeliosEndpointTCPConnector extends AbstractEndpointConnector {
	/** The channel socket factory */
	protected NioClientSocketChannelFactory socketChannelFactory;
	/** The channel socket  */
	protected SocketChannel socketChannel;	
	/** The boss thread pool */
	protected Executor bossExecutor;
	
	/** This connector's protocol */
	public static final Protocol PROTOCOL = Protocol.TCP;

	/**
	 * Creates a new HeliosEndpointTCPConnector
	 * @param endpoint The endpoint this connector was created for
	 */
	public HeliosEndpointTCPConnector(@SuppressWarnings("rawtypes") HeliosEndpoint endpoint) {
		super(endpoint);
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
				.setPoolObjectName(new StringBuilder("org.helios.endpoints:name=").append(getClass().getSimpleName()).append(",service=ThreadPool,type=Boss,protocol=").append(getProtocol().name()))
				.setPrestartThreads(1)
				.setTaskQueueSize(100)
				.setTerminationTime(5000)
				.setThreadGroupName(getClass().getSimpleName() + "BossThreadGroup")
				.setUncaughtExceptionHandler(endpoint)
				.build();
	    // Configure the client.
		socketChannelFactory = new NioClientSocketChannelFactory(bossExecutor, workerExecutor);
		bootstrap = new ClientBootstrap(socketChannelFactory);
		// Set up the pipeline factory.
		channelPipelineFactory = new ChannelPipelineFactory() {
	          public ChannelPipeline getPipeline() throws Exception {
	              return Channels.pipeline(
	            		  instrumentation, 
	                      new ObjectEncoder(),
	                      new ObjectDecoder(),
	                      responseProcessor, 
	                      responseProcessor2,
	                      exceptionListener
	                      );
	          }
		};	                     
		bootstrap.setPipelineFactory(channelPipelineFactory);
		bootstrap.setOption("remoteAddress", new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
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
	 * {@inheritDoc}
	 * @see org.helios.ot.helios.AbstractEndpointConnector#connect()
	 */
	public void connect() {
		if(isConnected()) throw new IllegalStateException("This connector is already connected", new Throwable());
		channelFuture = bootstrap.connect();
		for(ChannelFutureListener listener: connectListeners) {
			channelFuture.addListener(listener);
		}
		channelFuture.awaitUninterruptibly();
		 if (channelFuture.isCancelled()) {
			 throw new RuntimeException("Connection request cancelled");
		 } else if (!channelFuture.isSuccess()) {
			 throw new RuntimeException(channelFuture.getCause().getMessage());		     
		 } else {			 
			 // no exception means a good connect
			 socketChannel = (SocketChannel)channelFuture.getChannel();
			 localSocketAddress = socketChannel.getLocalAddress();
			 log.info("Connected [" + socketChannel + "]");
			 socketChannel.getCloseFuture().addListener(new ChannelFutureListener(){
				 @Override
				public void operationComplete(ChannelFuture future)throws Exception {
					 log.warn("\n\tHELIOS ENDPOINT DISCONNECTED:" + future + "\n\t Cancelled:" + future.isCancelled() + "\n\t Success:" + future.isSuccess() + "\n\t Done:" + future.isDone());
					 if(!future.isDone()) {
						 log.warn("Waiting for future to complete...");
						 future.awaitUninterruptibly(10000);
					 }
					 
					 Throwable t = future.getCause();
					 if(t!=null) {
						 t.printStackTrace(System.err);
					 } 
				}
			 });
			 setConnected();
			 log.info("Socket Channel Connected [" + socketChannel + "]");
		 }
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.helios.AbstractEndpointConnector#disconnect()
	 */
	public void disconnect() {
		final ClientSocketChannelFactory finalFactory = socketChannelFactory;		
		channelFuture.getChannel().getCloseFuture().addListener(new ChannelFutureListener(){
			public void operationComplete(ChannelFuture future) throws Exception {				
				finalFactory.releaseExternalResources();	
				setDisconnected(true);
			}
		});		 
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.helios.AbstractEndpointConnector#write(org.helios.ot.tracer.disruptor.TraceCollection)
	 */
	public void write(TraceCollection<?> traceCollection) {
		Set<?> traces = traceCollection.getTraces();
		Trace[] traceArr = traces.toArray(new Trace[traces.size()]);
//		System.out.println("Submitting [" + traceArr.length + "] in one [" + traceArr.getClass().getName() + "]");
		socketChannel.write(traceArr).addListener(sendListener);;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.helios.AbstractEndpointConnector#getProtocol()
	 */
	protected Protocol getProtocol() {
		return PROTOCOL;
	}
}
