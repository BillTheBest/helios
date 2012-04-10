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
package org.helios.ot.agent.impl.netty;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.helios.ot.agent.AbstractHeliosOTClientImpl;
import org.helios.ot.agent.Configuration;
import org.helios.ot.agent.protocol.impl.ClientProtocolOperation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;

/**
 * <p>Title: AbstractNettyHeliosOTClient</p>
 * <p>Description: Abstract base class for Netty based Helios OT Client implementations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.impl.netty.AbstractNettyHeliosOTClient</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public abstract class AbstractNettyHeliosOTClient extends AbstractHeliosOTClientImpl {
	/**  */
	private static final long serialVersionUID = -8596112631080470057L;

	/** The worker thread pool shared by all Netty NIO client instances */
	protected volatile static Executor workerExecutor;
	/** The host name to connect to */
	protected String host = null;
	/** The listening port on the host to connect to */
	protected int port = -1;
	
	/** The pipeline factory */
	protected ChannelPipelineFactory channelPipelineFactory;
	/** The channel factory */
	protected ChannelFactory channelFactory;
	
	/** The connection's channel future */
	protected ChannelFuture channelFuture;
	/** The client bootstrap */
	protected ClientBootstrap bootstrap;
	
	/** The client bootstrap options */
	protected final Map<String, Object> bootstrapOptions = new HashMap<String, Object>();
	/** The remote socket address */
	protected InetSocketAddress remoteSocketAddress = null;
	/** The local bind address */
	protected InetSocketAddress localSocketAddress = null;
	/** The synchronous request handler */
	protected final SynchronousRequestHandler synchronousRequestHandler = new SynchronousRequestHandler();
	/** The instrumentation */
	protected final ConnectorChannelInstrumentation instrumentation = new ConnectorChannelInstrumentation();
	/** The send listener */
	protected final ChannelFutureListener sendListener = new ChannelFutureListener() {
		@Override
		public void operationComplete(ChannelFuture f) throws Exception {
			if(f.isDone()) {
				if(f.isSuccess()) {					
					opCounter.incrementAndGet();   
				} else {
					failedOpCounter.incrementAndGet();					
				}
			}
		}
	};
	

	
	/** The configuration name for the tcp no delay option */
	public static final String CONFIG_NODELAY = "tcpNoDelay";
	/** The default tcp no delay option */
	public static final boolean DEFAULT_NODELAY = true;
	
	/** The configuration name for the tcp keep alive option */
	public static final String CONFIG_KEEPALIVE = "keepAlive";
	/** The default keep alive option */
	public static final boolean DEFAULT_KEEPALIVE = true;
	
	/** The configuration name for the reuse address option */
	public static final String CONFIG_REUSEADDRESS = "reuseAddress";
	/** The default reuse address option option */
	public static final boolean DEFAULT_REUSEADDRESS = true;
	
	/** The configuration name for the soLinger option */
	public static final String CONFIG_SOLINGER = "soLinger";
	/** The default soLinger option */
	public static final int DEFAULT_SOLINGER = 0;
	
	/** The configuration name for the traffic class option */
	public static final String CONFIG_TRAFFIC_CLASS = "trafficClass";
	/** The default traffic class option */
	public static final int DEFAULT_TRAFFIC_CLASS = 0;
	
	
	/** The configuration name for the receive buffer size */
	public static final String CONFIG_RECEIVE_BUFFER = "receiveBufferSize";
	/** The default receive buffer size */
	public static final int DEFAULT_RECEIVE_BUFFER = 8192;
	
	/** The configuration name for the send buffer size */
	public static final String CONFIG_SEND_BUFFER = "sendBufferSize";
	/** The default send buffer size */
	public static final int DEFAULT_SEND_BUFFER = 1048576;
	
	
	/**
	 * Creates a new AbstractNettyHeliosOTClient
	 */
	public AbstractNettyHeliosOTClient() {
		if(workerExecutor==null) {
			synchronized(AbstractNettyHeliosOTClient.class) {
				if(workerExecutor==null) {
					workerExecutor = ExecutorBuilder.newBuilder()
							.setCoreThreads(5)
							.setCoreThreadTimeout(false)
							.setDaemonThreads(true)
							.setExecutorType(true)
							.setFairSubmissionQueue(false)
							.setKeepAliveTime(15000)
							.setMaxThreads(100)
							.setJmxDomains(JMXHelper.getRuntimeHeliosMBeanServer().getDefaultDomain())
							// org.helios.endpoints:type=HeliosEndpoint,name=HeliosEndpoint
							//.setPoolObjectName(new StringBuilder("org.helios.endpoints:name=").append(getClass().getSimpleName()).append(",service=ThreadPool,type=Worker,protocol=").append(getProtocol().name()))
							.setPrestartThreads(1)
							.setTaskQueueSize(1000)
							.setTerminationTime(5000)
							.setThreadGroupName("HeliosOTClientNettyWorkerThreadGroup")
							//.setUncaughtExceptionHandler(endpoint)
							.build();		
				}
			}
		}
	}
	
	/**
	 * Extracts the generic Netty configuration parameters
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doConfigureClient(java.net.URI)
	 */
	protected void doConfigureClient(URI uri) {		
		host = uri.getHost();
		if(host==null) {
			host = Configuration.getHost();
		}
		port = uri.getPort();
		if(port==-1) {
			port = ConfigurationHelper.getIntSystemThenEnvProperty(Configuration.PORT, getDefaultTargetPort());
		}
		remoteSocketAddress = new InetSocketAddress(host, port);
		bootstrapOptions.put(CONFIG_KEEPALIVE, Configuration.getBooleanConfigurationOption(CONFIG_KEEPALIVE, Configuration.CONFIG_KEEPALIVE,  DEFAULT_KEEPALIVE, uriParameters));
		bootstrapOptions.put(CONFIG_NODELAY, Configuration.getBooleanConfigurationOption(CONFIG_NODELAY, Configuration.CONFIG_NODELAY,  DEFAULT_NODELAY, uriParameters));
		bootstrapOptions.put(CONFIG_REUSEADDRESS, Configuration.getBooleanConfigurationOption(CONFIG_REUSEADDRESS, Configuration.CONFIG_REUSEADDRESS,  DEFAULT_REUSEADDRESS, uriParameters));
		bootstrapOptions.put(CONFIG_SOLINGER, Configuration.getIntConfigurationOption(CONFIG_SOLINGER, Configuration.CONFIG_SOLINGER,  DEFAULT_SOLINGER, uriParameters));
		bootstrapOptions.put(CONFIG_TRAFFIC_CLASS, Configuration.getIntConfigurationOption(CONFIG_TRAFFIC_CLASS, Configuration.CONFIG_TRAFFIC_CLASS,  DEFAULT_TRAFFIC_CLASS, uriParameters));
		bootstrapOptions.put(CONFIG_RECEIVE_BUFFER, Configuration.getLongConfigurationOption(CONFIG_RECEIVE_BUFFER, Configuration.CONFIG_RECEIVE_BUFFER,  DEFAULT_RECEIVE_BUFFER, uriParameters));
		bootstrapOptions.put(CONFIG_SEND_BUFFER, Configuration.getLongConfigurationOption(CONFIG_SEND_BUFFER, Configuration.CONFIG_SEND_BUFFER,  DEFAULT_SEND_BUFFER, uriParameters));
		initChannelFactory();
		reflectObject(this);
		reflectObject(this.instrumentation);
		//=================================
		//    Push this up to the top abstract with abstracts getting the right details from the impls.
		//=================================
		objectName = JMXHelper.objectName(new StringBuilder("org.helios.agent:service=HeliosOTClient,host=").append(host).append(",port=").append(port).append(",protocol=").append(getProtocol()));
		JMXHelper.getRuntimeHeliosMBeanServer().registerMBean(this, objectName);
		//=================================
	}
	
//	public Hashtable<String, String> objectNameKeys() {
//		Hashtable<String, String> keys = new Hashtable<String, String>();
//		
//		return keys;
//	}
	
	/**
	 * Initializes the pipeline and channel factory
	 */
	protected abstract void initChannelFactory();
	
	/**
	 * Returns the 
	 * @return
	 */
	protected abstract ChannelFactory getChannelFactory();
	
	/**
	 * Returns the default netty listening port
	 * @return the default netty listening port
	 */
	protected abstract int getDefaultTargetPort();
	
	/**
	 * Returns the connection channel for this client instance
	 * @return the connection channel for this client instance
	 */
	public abstract Channel getConnectionChannel();


	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doConnect()
	 */
	@Override
	protected void doConnect()  {
		if(isConnected()) {
			// callback on listeners
			fireOnConnect();
			return;
		}
		bootstrap.connect(remoteSocketAddress).addListener(new ChannelFutureListener(){
			public void operationComplete(ChannelFuture f) throws Exception {
				if(f.isCancelled() || !f.isSuccess()) {
					fireOnConnectFailure(f.getCause());
				} else {
					fireOnConnect();
				}
			}
		});
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doPurge()
	 */
	protected void doPurge() {
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doDisconnect()
	 */
	@Override
	protected void doDisconnect() {
		/*
		channelFuture.awaitUninterruptibly();
		 if (channelFuture.isCancelled()) {
			 throw new RuntimeException("Connection request cancelled");
		 } else if (!channelFuture.isSuccess()) {
			 throw new RuntimeException(channelFuture.getCause().getMessage());		     
		 } else {			 
			 // no exception means a good connect
			 socketChannel = (SocketChannel)channelFuture.getChannel();
			 localSocketAddress = socketChannel.getLocalAddress();		
			 //socketChannel.write(HeliosProtocolInvocation.newInstance(ClientProtocolOperation.CONNECT, new String[]{MetricId.getHostname(), MetricId.getApplicationId()})).addListener(sendListener);
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
		*/
		
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doPing()
	 */
	protected boolean doPing() {
		int randomSeed = random.nextInt();
		int response = randomSeed + 1;
		try {
			response = (Integer)invoke(ClientProtocolOperation.PING, randomSeed).getSynchronousResponse();
		} catch (Exception e) {
			return false;
		}
		return randomSeed==response;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doEcho(java.io.Serializable)
	 */
	protected <T extends Serializable> T doEcho(T payload) {
		try {
			return (T) invoke(ClientProtocolOperation.ECHO, payload).getSynchronousResponse();
		} catch (InterruptedException e) {
			throw new RuntimeException("Thread was interrupted while waiting on echo completion", e);
		} catch (TimeoutException e) {
			throw new RuntimeException("Echo operation timed out", e);
		}		
	}
	
	/**
	 * Executes a server destined operation
	 * @param op The operation code
	 * @param payload The invication payload
	 * @return The created wrapped invocation which was sent
	 */
	protected HeliosProtocolInvocation invoke(ClientProtocolOperation op, Object payload) {
		HeliosProtocolInvocation hpi = HeliosProtocolInvocation.newInstance(op, payload);
		getConnectionChannel().write(hpi).addListener(sendListener);
		return hpi;
	}	
	
	/**
	 * Returns the string representation of the local socket address
	 * @return the string representation of the local socket address
	 */
	@JMXAttribute(name="LocalAddress", description="The string representation of the local socket address", mutability=AttributeMutabilityOption.READ_ONLY) 
	public String getLocalAddress() {
		return localSocketAddress!=null ? localSocketAddress.toString() : "Not Connected";
	}
	
	/**
	 * Returns the local bind address
	 * @return the local bind address
	 */
	@JMXAttribute(name="LocalBindAddress", description="The local bind address", mutability=AttributeMutabilityOption.READ_ONLY) 
	public String getLocalBindAddress() {
		return localSocketAddress!=null ? localSocketAddress.getAddress().getHostAddress() : "Not Connected";
	}
	
	/**
	 * Returns the local bind port
	 * @return the local bind port
	 */
	@JMXAttribute(name="LocalBindPort", description="The local bind port", mutability=AttributeMutabilityOption.READ_ONLY) 
	public int getLocalBindPort() {
		return localSocketAddress!=null ? localSocketAddress.getPort() : -1;
	}

	/**
	 * Returns the target host to connect to
	 * @return the target host to connect to
	 */
	@JMXAttribute(name="TargetHost", description="The target host to connect to", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getTargetHost() {
		return host;
	}

	/**
	 * Returns the listening port on the target host 
	 * @return the listening port on the target host
	 */
	@JMXAttribute(name="TargetPort", description="The listening port on the target host", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTargetPort() {
		return port;
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {	    
	    StringBuilder retValue = new StringBuilder(getClass().getSimpleName()).append(" [")
	        .append("URI:").append(uri)
	        .append(" Connected:").append(connected.get())
	        .append(" LocalSocketAddress:").append(this.localSocketAddress)
	        .append("]");    
	    return retValue.toString();
	}
	

}
