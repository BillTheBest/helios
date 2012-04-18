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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.helios.ot.agent.AbstractHeliosOTClientImpl;
import org.helios.ot.agent.Configuration;
import org.helios.ot.agent.HeliosOTClient;
import org.helios.ot.agent.HeliosOTClientEventListener;
import org.helios.ot.agent.impl.netty.handler.HeliosProtocolHandler;
import org.helios.ot.agent.impl.netty.handler.listeners.ConnectionOpenedEventListener;
import org.helios.ot.agent.impl.netty.handler.listeners.ConnectionResponseListener;
import org.helios.ot.agent.impl.netty.handler.listeners.InvocationDispatchListener;
import org.helios.ot.agent.impl.netty.handler.listeners.SynchronousInvocationListener;
import org.helios.ot.agent.jmx.WrappedLoggingHandler;
import org.helios.ot.agent.protocol.impl.ClientProtocolOperation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation;
import org.helios.ot.trace.Trace;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
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
	/** This client's channel */
	protected Channel channel;
	/** This client's channel pipeline */
	protected ChannelPipeline pipeline = null;
	
	/** The client bootstrap */
	protected ClientBootstrap bootstrap;

	/** The connection opened event handler */
	protected ConnectionOpenedEventListener onConnectListener = new ConnectionOpenedEventListener();
	/** The connection handshake response listener */
	protected ConnectionResponseListener onHandshakeListener = new ConnectionResponseListener(this);
	protected InvocationDispatchListener dispatchListener = new InvocationDispatchListener();
	/** The helios protocol handler */
	protected HeliosProtocolHandler protocolHandler = new HeliosProtocolHandler(onConnectListener, onHandshakeListener, dispatchListener);
	
	
	/** The client bootstrap options */
	protected final Map<String, Object> bootstrapOptions = new HashMap<String, Object>();
	/** The remote socket address */
	protected InetSocketAddress remoteSocketAddress = null;
	/** The local bind address */
	protected InetSocketAddress localSocketAddress = null;
	/** The synchronous request handler */
	protected SynchronousInvocationListener synchronousInvocationListener;
	/** The logging channel handler for enabling debug of the events occuring in the pipeline */
	protected WrappedLoggingHandler loggingHandler; 
	/** The instrumentation */
	protected final ConnectorChannelInstrumentation instrumentation = new ConnectorChannelInstrumentation();
	/** The channel close listener */
	protected final ChannelFutureListener closeListener = new ChannelFutureListener() {
		// ====================
		//  Need to clean up connected resources
		// ====================
		public void operationComplete(ChannelFuture future) throws Exception {
			connected.set(false);
			if(deliberateDisconnect.get()) {
				fireOnDisconnect(null);
			} else {
				fireOnDisconnect(new Exception("Unexpected Disconnect", future.getCause()));
			}
		}
	};
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
	
	/** The name of the logging handler */
	public static final String LOGGING_HANDLER_NAME = "loggingHandler";
	
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
							.setPoolObjectName(new StringBuilder("org.helios.agent:service=HeliosOTClient,threadPool=Worker"))
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
		try {
			this.uri = new URI(getProtocol(), null, host, port, null, null, null);
		} catch (Exception e) {
			log.error("Failed to rebuild URI from [" + getProtocol() + "/" + host + "/" +  port + "]", e);
		}
		bootstrapOptions.put(CONFIG_KEEPALIVE, Configuration.getBooleanConfigurationOption(CONFIG_KEEPALIVE, Configuration.CONFIG_KEEPALIVE,  DEFAULT_KEEPALIVE, uriParameters));
		bootstrapOptions.put(CONFIG_NODELAY, Configuration.getBooleanConfigurationOption(CONFIG_NODELAY, Configuration.CONFIG_NODELAY,  DEFAULT_NODELAY, uriParameters));
		bootstrapOptions.put(CONFIG_REUSEADDRESS, Configuration.getBooleanConfigurationOption(CONFIG_REUSEADDRESS, Configuration.CONFIG_REUSEADDRESS,  DEFAULT_REUSEADDRESS, uriParameters));
		bootstrapOptions.put(CONFIG_SOLINGER, Configuration.getIntConfigurationOption(CONFIG_SOLINGER, Configuration.CONFIG_SOLINGER,  DEFAULT_SOLINGER, uriParameters));
		bootstrapOptions.put(CONFIG_TRAFFIC_CLASS, Configuration.getIntConfigurationOption(CONFIG_TRAFFIC_CLASS, Configuration.CONFIG_TRAFFIC_CLASS,  DEFAULT_TRAFFIC_CLASS, uriParameters));
		bootstrapOptions.put(CONFIG_RECEIVE_BUFFER, Configuration.getLongConfigurationOption(CONFIG_RECEIVE_BUFFER, Configuration.CONFIG_RECEIVE_BUFFER,  DEFAULT_RECEIVE_BUFFER, uriParameters));
		bootstrapOptions.put(CONFIG_SEND_BUFFER, Configuration.getLongConfigurationOption(CONFIG_SEND_BUFFER, Configuration.CONFIG_SEND_BUFFER,  DEFAULT_SEND_BUFFER, uriParameters));
		synchronousInvocationListener = new SynchronousInvocationListener(operationTimeout);
		protocolHandler.addResponseListener(synchronousInvocationListener);
		protocolHandler.addRequestListener(synchronousInvocationListener);
		
		initChannelFactory();
		reflectObject(this);
		reflectObject(this.instrumentation);
		reflectObject(this.logController);
		//=================================
		//    Push this up to the top abstract with abstracts getting the right details from the impls.
		//=================================
		objectName = JMXHelper.objectName(new StringBuilder("org.helios.agent:service=HeliosOTClient,host=").append(host).append(",port=").append(port).append(",protocol=").append(getProtocol()));
		loggingHandler = new WrappedLoggingHandler(objectName, workerExecutor, getClass().getName(), false);
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
	 * <p>Implements the abstract connection procedure for Netty protocol implementations.
	 * <p>If the connection is already connected, the connection callback should be called on the temporary listeners only.
	 * See {@link HeliosOTClient#connect(boolean, org.helios.ot.agent.HeliosOTClientEventListener...)}.
	 * <p>If the connection is <b>not</b> already connected, the {@link AbstractNettyHeliosOTClient#bootstrap} connect is called.
	 * A {@link ChannelFutureListener} is registered to detect the operation completion and the the procedure therein is as follows:<ol>
	 * 
	 * <li>If the operation is <i>cancelled</i> or <i>not successful</i>, 
	 * a {@link org.helios.ot.agent.HeliosOTClientEventListener#onConnectFailure(HeliosOTClient, Throwable)} 
	 * is fired against all listeners and no further action is taken.</li>
	 * 
	 * <li>If the operation <i>is</i> successful:<ol>
	 * 		<li>Initialize the impl's Channel (e.g. the SocketChannel for TCP). Delegate down using {@link AbstractNettyHeliosOTClient#onImplConnect(ChannelFuture)}.</li>
	 * 		<li>Initialize this class's ChannelFuture using {@link org.jboss.netty.channel.Channel#getCloseFuture()} which serves as the singular disconnect listener.</li>
	 * 		<li>Initialize the local and remote socket references (useful meta-data exposed in JMX) </li>
	 * 		<li>Set the connected flag to true</li>
	 * 		<li>Fire {@link org.helios.ot.agent.AbstractHeliosOTClientImpl#fireOnConnect()}</li>
	 * </li>
	 * </ol>
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doConnect()
	 */
	@Override
	protected void doConnect(final HeliosOTClientEventListener...listeners)  {
		if(isConnected()) {
			// callback on temp listeners only
			if(listeners!=null && listeners.length>0) {
				for(HeliosOTClientEventListener listener: listeners) {
					listener.onConnect(this);
				}
			}
			return;
		}
		final AbstractNettyHeliosOTClient finalClient = this;
		bootstrap.connect(remoteSocketAddress).addListener(new ChannelFutureListener(){
			public void operationComplete(ChannelFuture f) throws Exception {
				if(f.isCancelled() || !f.isSuccess()) {
					fireOnConnectFailure(f.getCause());
					if(listeners!=null && listeners.length>0) {
						for(HeliosOTClientEventListener listener: listeners) {
							listener.onConnectFailure(finalClient, f.getCause());
						}
					}
					
				} else {
					channel = f.getChannel();
					pipeline = channel.getPipeline();
 					localSocketAddress = (InetSocketAddress) channel.getLocalAddress();
					remoteSocketAddress = (InetSocketAddress) channel.getRemoteAddress();
					onImplConnect(f);					
					channelFuture = f.getChannel().getCloseFuture(); // Need to attach close listener here.
					channelFuture.addListener(closeListener);
					//sessionId = postConnectHandshake();
					//System.out.println("PING---->" + ping());
//					sessionId = waitForSessionId();
//					System.out.println("SESSION ID:" + sessionId);
					connected.set(true);
					for(HeliosOTClientEventListener listener: listeners) {
						listener.onConnect(finalClient);
					}					
					fireOnConnect();
				}
			}
		});
	}
	
	/**
	 * Returns a string value map representation of the client's channel pipeline
	 * @return a string value map representation of the client's channel pipeline
	 */
	@JMXAttribute(name="ChannelPipeline", description="A string value map representation of the client's channel pipeline", mutability=AttributeMutabilityOption.READ_ONLY)
	public Map<String, String> getChannelPipeline() {
		if(pipeline==null) return Collections.emptyMap();
		Map<String, ChannelHandler> pipelineMap = pipeline.toMap();
		Map<String, String> map = new HashMap<String, String>(pipelineMap.size());
		for(Map.Entry<String, ChannelHandler> entry: pipelineMap.entrySet()) {
			map.put(entry.getKey(), entry.getValue().getClass().getName());
		}
		return Collections.unmodifiableMap(map);
	}
	
//	/**
//	 * Indicates if the logging handler is enabled anywhere in the pipeline
//	 * @return true if the logging handler is enabled anywhere in the pipeline, false otherwise
//	 */
//	@JMXAttribute(name="LoggingHandlerEnabled", description="Indicates if the logging handler is enabled anywhere in the pipeline", mutability=AttributeMutabilityOption.READ_WRITE)
//	public boolean getLoggingHandlerEnabled() {
//		for(ChannelHandler handler: pipeline.toMap().values()) {
//			if(handler==loggingHandler) {
//				return true;
//			}
//		}
//		return false;
//	}
//	
//	
//	/**
//	 * Sets the enabled state of the logging handler.
//	 * When disabled, simply removes the logging handler from the pipeline.
//	 * When enabled, adds the logging handler to the front of the pipeline  
//	 * @param enable true to enable, false to disable
//	 */
//	public void setLoggingHandlerEnabled(boolean enable) {
//		boolean isEnabled = getLoggingHandlerEnabled();
//		if(enable==isEnabled) return;
//		if(enable) {
//			pipeline.addFirst(LOGGING_HANDLER_NAME, loggingHandler);
//		} else {
//			pipeline.remove(loggingHandler);
//		}
//	}
//	
//	
//	/**
//	 * Adds the logging handler before the named handler in the pipeline, removing it first if it already enabled.
//	 * @param handlerName The name of the handler to add the logging handler before
//	 */
//	@JMXOperation(name="enableLoggingHandlerBefore", description="Adds the logging handler before the named handler in the pipeline, removing it first if it already enabled.")
//	public void enableLoggingHandlerBefore(@JMXParameter(name="handlerName", description="The name of the handler to add the logging handler before") String handlerName) {
//		if(handlerName==null) throw new IllegalArgumentException("The passed handler name was null", new Throwable());
//		try {
//			if(!pipeline.toMap().containsKey(handlerName)) throw new IllegalArgumentException("The passed handler name [" + handlerName + "] is not registered in the pipeline", new Throwable());
//			setLoggingHandlerEnabled(false);
//			pipeline.addBefore(handlerName, LOGGING_HANDLER_NAME, loggingHandler);
//		} catch (Exception e) {
//			e.printStackTrace(System.err);
//			throw new RuntimeException(e);
//		}
//	}
//	
//	/**
//	 * Adds the logging handler after the named handler in the pipeline, removing it first if it already enabled.
//	 * @param handlerName The name of the handler to add the logging handler after
//	 */
//	@JMXOperation(name="enableLoggingHandlerAfter", description="Adds the logging handler after the named handler in the pipeline, removing it first if it already enabled.")
//	public void enableLoggingHandlerAfter(@JMXParameter(name="handlerName", description="The name of the handler to add the logging handler after") String handlerName) {
//		if(handlerName==null) throw new IllegalArgumentException("The passed handler name was null", new Throwable());
//		if(!pipeline.toMap().containsKey(handlerName)) throw new IllegalArgumentException("The passed handler name [" + handlerName + "] is not registered in the pipeline", new Throwable());
//		setLoggingHandlerEnabled(false);
//		pipeline.addAfter(handlerName, LOGGING_HANDLER_NAME, loggingHandler);
//	}
//	
//	/**
//	 * Returns the internal logging level of the logging handler
//	 * @return the internal logging level of the logging handler
//	 */
//	@JMXAttribute(name="LoggingHandlerLevel", description="The internal logging level of the logging handler" , mutability=AttributeMutabilityOption.READ_WRITE)
//	public String getLoggingHandlerLevel() {
//		return loggingHandler.getLogLevel();
//	}
//	
//	/**
//	 * Sets the logging level of the logging handler
//	 * @param level The name of the level to set
//	 */
//	public void setLoggingHandlerLevel(String level) {
//		loggingHandler.setLogLevel(level);
//	}
//	
//	/**
//	 * Indicates if the logging handler has JMX notifications enabled
//	 * @return true if the logging handler has JMX notifications enabled, false otherwise
//	 */
//	@JMXAttribute(name="JmxLoggingEnabled", description="Indicates if the logging handler has JMX notifications enabled" , mutability=AttributeMutabilityOption.READ_WRITE)
//	public boolean getJmxLoggingEnabled() {
//		return loggingHandler.isJmxEnabled();
//	}
//	
//	/**
//	 * Sets the enabled state of the logging handler's JMX notifications
//	 * @param enabled true to enable, false to disable
//	 */
//	public void setJmxLoggingEnabled(boolean enabled) {
//		loggingHandler.setJmxEnabled(enabled);
//	}
	
	/**
	 * Event passed down from the Abstract client managing the connect process to the impl that has no idea what's going on.
	 * @param cf The connection event channel future
	 */
	protected abstract void onImplConnect(ChannelFuture cf);
	
//	/**
//	 * Event callback when the client connects
//	 * @param client The client that connected
//	 */
//	public void onConnect(HeliosOTClient client) {
//		super.onConnect(client);
//	}
//	/**
//	 * Event callback when the client fails to connect
//	 * @param client The client that connected
//	 * @param cause The associated cause of the connection failure
//	 */
//	public void onConnectFailure(HeliosOTClient client, Throwable cause) {
//		
//	}
//	
//	/**
//	 * Event callback when the client disconnects
//	 * @param client The client that disconnected
//	 * @param cause The cause of an unintended disconnect. Null if disconnect was requested.
//	 */
//	public void onDisconnect(HeliosOTClient client, Throwable cause) {
//		
//	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doPurge()
	 */
	protected void doPurge() {
		
	}
	
	/**
	 * Passes the disconnect event to the concrete impls so they can clean up
	 */
	protected abstract void onImplDisconnect();
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doDisconnect()
	 */
	@Override
	protected void doDisconnect() {
		channel.close().awaitUninterruptibly();
		channel = null;		
		localSocketAddress = null;
		remoteSocketAddress = null;						
		channelFuture = null;
		onImplDisconnect();	
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
			e.printStackTrace(System.err);
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
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doSubmitTraces(org.helios.ot.trace.Trace[])
	 */
	@Override
	protected void doSubmitTraces(Trace[] traces) {
		channel.write(HeliosProtocolInvocation.newInstance(ClientProtocolOperation.TRACE, traces)).addListener(sendListener);
	}
	
	
	/**
	 * Executes a server destined operation
	 * @param op The operation code
	 * @param payload The invication payload
	 * @return The created wrapped invocation which was sent
	 */
	protected HeliosProtocolInvocation doInvoke(ClientProtocolOperation op, Object payload) {
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
	 * Returns the string representation of the remote socket address
	 * @return the string representation of the remote socket address
	 */
	@JMXAttribute(name="RemoteAddress", description="The string representation of the remote socket address", mutability=AttributeMutabilityOption.READ_ONLY) 
	public String getRemoteAddress() {
		return remoteSocketAddress!=null ? remoteSocketAddress.toString() : "Not Connected";
	}
	
	/**
	 * Returns the remote bind address
	 * @return the remote bind address
	 */
	@JMXAttribute(name="RemoteBindAddress", description="The remote bind address", mutability=AttributeMutabilityOption.READ_ONLY) 
	public String getRemoteBindAddress() {
		return remoteSocketAddress!=null ? remoteSocketAddress.getAddress().getHostAddress() : "Not Connected";
	}
	
	/**
	 * Returns the remote bind port
	 * @return the remote bind port
	 */
	@JMXAttribute(name="RemoteBindPort", description="The remote bind port", mutability=AttributeMutabilityOption.READ_ONLY) 
	public int getRemoteBindPort() {
		return remoteSocketAddress!=null ? remoteSocketAddress.getPort() : -1;
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
	    	.append("SessionId:").append(sessionId)
	        .append("URI:").append(uri)
	        .append(" Connected:").append(connected.get())
	        .append(" LocalSocketAddress:").append(this.localSocketAddress)
	        .append(" RemoteSocketAddress:").append(this.remoteSocketAddress)
	        .append("]");    
	    return retValue.toString();
	}
	

}
