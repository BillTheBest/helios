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
import java.net.URI;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;

/**
 * <p>Title: AbstractEndpointConnector</p>
 * <p>Description: Base class Netty connector for {@link HeliosEndpoint}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.AbstractEndpointConnector</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public abstract class AbstractEndpointConnector implements Runnable {
	/** The worker thread pool */
	protected Executor workerExecutor;
	/** The pipeline factory */
	protected ChannelPipelineFactory channelPipelineFactory;
	/** The connection's channel future */
	protected ChannelFuture channelFuture;
	/** The client bootstrap */
	protected ClientBootstrap bootstrap; 
	/** The failed send counter */
	protected final AtomicLong failedSendCounter = new AtomicLong(0);
	/** The send counter */
	protected final AtomicLong sendCounter = new AtomicLong(0);
	/** The local bind address */
	protected InetSocketAddress localSocketAddress = null;
	/** Flag to indicate the flush thread should keep running */
	protected boolean flushThreadRunning = false;
	/** Flag to indicate that a flush is executing */
	protected final AtomicBoolean flushRunning = new AtomicBoolean(false);
	/** The flush thread */
	protected Thread flushThread;
	/** The flush count */
	protected long flushCount = 0;
	
	/** The exception listener */
	protected final ExceptionListener exceptionListener = new ExceptionListener();
	/** The send listener */
	protected final ChannelFutureListener sendListener = new ChannelFutureListener() {
		@Override
		public void operationComplete(ChannelFuture f) throws Exception {
			if(f.isDone()) {
				if(f.isSuccess()) {
					sendCounter.incrementAndGet();   
				} else {
					failedSendCounter.incrementAndGet();
				}
			}
		}
	};
	/** The endpoint this connector was created for */
	@SuppressWarnings("rawtypes")
	protected final HeliosEndpoint endpoint;
	/** The connected indicator. This is kept private to force impls. to set the connected state through the articlated methods */
	private final AtomicBoolean connected = new AtomicBoolean(false);	
	/** The instrumentation */
	protected final ConnectorChannelInstrumentation instrumentation = new ConnectorChannelInstrumentation();
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The receive listener */
	protected final ChannelUpstreamHandler receiveDebugListener = new ChannelUpstreamHandler() {
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
			if(e instanceof MessageEvent) {
				Object obj = ((MessageEvent)e).getMessage();
				if(log.isDebugEnabled()) log.debug("Upstream Return Value [" + obj.getClass().getName() + "]:" + obj.toString());
			}
			ctx.sendUpstream(e);			
		}
	};
	/** The send listener */
	protected final ChannelDownstreamHandler sendDebugListener = new ChannelDownstreamHandler() {
		public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
			if(e instanceof MessageEvent) {
				Object obj = ((MessageEvent)e).getMessage();
				if(log.isDebugEnabled()) log.debug("Downstream Return Value [" + obj.getClass().getName() + "]:" + obj.toString());
			}
			ctx.sendDownstream(e);			
		}
	};
	/** The max trace buffer size before a flush */
	protected final int maxFlushSize;
	/** The max elapsed time before a flush */
	protected long maxFlushTime;
	/** The blocking queue for buffering flushes */
	@SuppressWarnings("rawtypes")
	protected final BlockingQueue<Trace[]> traceBuffer;
	/** Serial number factory for flush thread naming */
	protected static final AtomicInteger serial = new AtomicInteger(0);
	
	/** A set of connect listeners that will be added when an asynch connect is initiated */
	protected final Set<ChannelFutureListener> connectListeners = new CopyOnWriteArraySet<ChannelFutureListener>();
	

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
	 * Returns the trace buffer size
	 * @return the trace buffer size
	 */
	@JMXAttribute(name="FlushQueueSize", description="The flush queue size", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getFlushQueueSize() {
		return traceBuffer.size();
	}
	
	/**
	 * Returns the flush queue size that triggers a flush
	 * @return the flush queue size that triggers a flush
	 */
	@JMXAttribute(name="FlushQueueTriggerSize", description="The flush queue size that triggers a flush", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getFlushQueueTriggerSize() {
		return maxFlushSize;
	}  
	
	/**
	 * Returns the maximum elapsed time between flush attempts (ms.)
	 * @return the maximum elapsed time between flush attempts (ms.)
	 */
	@JMXAttribute(name="FlushQueueTriggerTime", description="The maximum elapsed time between flush attempts (ms.)", mutability=AttributeMutabilityOption.READ_WRITE)
	public long getFlushQueueTriggerTime() {
		return maxFlushTime;
	}  
	
	/**
	 * Sets the maximum elapsed time between flush attempts (ms.)
	 * @param time the maximum elapsed time between flush attempts (ms.)
	 */
	public void setFlushQueueTriggerTime(long time) {
		maxFlushTime = time;
	}
	

	
	/**
	 * Returns the flush count
	 * @return the flush count
	 */
	@JMXAttribute(name="FlushCount", description="The number of flush events", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFlushCount() {
		return flushCount;
	}
	
	/**
	 * Returns the flush thread state
	 * @return the flush thread state
	 */
	@JMXAttribute(name="FlushThreadState", description="The state of the flush thread", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getFlushThreadState() {
		return flushThread==null ? "Null" : flushThread.getState().name();
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
	 * Creates a new AbstractEndpointConnector
	 * @param endpoint The endpoint this connector was created for
	 */
	@SuppressWarnings("rawtypes")
	protected AbstractEndpointConnector(HeliosEndpoint endpoint) {
		maxFlushTime = HeliosEndpointConfiguration.getMaxFlushTime();
		maxFlushSize = HeliosEndpointConfiguration.getMaxFlushSize();
		traceBuffer = new ArrayBlockingQueue<Trace[]>(maxFlushSize, false);
		this.endpoint = endpoint;
	}
	
	/**
	 * Creates and starts the flush thread
	 */
	protected void createFlushThread() {
		if(flushThread==null) {
			flushThread = new Thread(this, getClass().getSimpleName() + "FlushThread#" + serial.incrementAndGet());
			flushThread.setDaemon(true);
			flushThread.setPriority(Thread.MAX_PRIORITY);
			flushThreadRunning = true;
			flushThread.start();
		} else {
			throw new IllegalStateException("creatFlushThread called but flush thread already exists", new Throwable());
		}
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
	 * Writes the passed traces out to the listening server
	 * @param traces The array of traces to write
	 */
	@SuppressWarnings("rawtypes")
	protected abstract void flushTraceBuffer(Trace[] traces); 

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
	
	/**
	 * The runnable execution for the flush thread
	 */
	public void run() {
		log.info("[" + Thread.currentThread().toString() + "] Started");
		while(flushThreadRunning) {
			
			try {
				Thread.currentThread().join(maxFlushTime);
			} catch (InterruptedException e) {
				flushRunning.set(true);
				Thread.interrupted();
			}
			try { 
				flush();
				flushCount++;
			} catch (Exception e) {
				e.printStackTrace(System.err);
			} finally {
				flushRunning.set(false);
			}
		}
		log.info("[" + Thread.currentThread().toString() + "] Terminating");
	}
	
	/**
	 * Trips a flush on account of a full buffer
	 */
	protected void tripFlush() {
		if(!flushRunning.get()) {
			flushThread.interrupt();
		}
	}
	
	
	/**
	 * Writes the passed trace collection to the flush queue
	 * @param traceCollection the trace collection to enqueue
	 */
	@SuppressWarnings("rawtypes")
	public void write(TraceCollection<?> traceCollection) {
		Set<?> traces = traceCollection.getTraces();
		if(traces.size()<1) return;
		Trace[] traceArr = traces.toArray(new Trace[traces.size()]);
		int dropCount = 0;
		boolean trippedFlush = false;
		if(traceArr.length>0) {
			if(!traceBuffer.offer(traceArr)) {
				if(!trippedFlush) {
					tripFlush();
					trippedFlush=true;
					if(!traceBuffer.offer(traceArr)) {
						dropCount += traceArr.length;
					}
				}
				dropCount += traceArr.length;
			} 
		}
		if(dropCount>0) failedSendCounter.addAndGet(dropCount);
	}

	
	/**
	 * Writes out the trace buffer
	 */
	@SuppressWarnings("rawtypes")
	protected void flush() {
		int flushSize = traceBuffer.size();
		if(flushSize<1) return;
		Set<Trace[]> flushItems = new HashSet<Trace[]>(flushSize);
		traceBuffer.drainTo(flushItems);
		
		int traceCount = 0;
		for(Trace[] traceArr: flushItems) {
			traceCount += traceArr.length;
		}
		Trace[] bigTraceArr = new Trace[traceCount];
		int cntr = 0;
		for(Trace[] traceArr: flushItems) {
			for(Trace t: traceArr) {
				bigTraceArr[cntr] = t;
				cntr++;
			}
		}
		if(log.isDebugEnabled()) log.debug("Flushing [" + cntr + "] Traces from flush queue");
		flushTraceBuffer(bigTraceArr);
	}
	
	/**
	 * Returns this connector's protocol
	 * @return this connector's protocol
	 */
	protected abstract Protocol getProtocol();
	
	/**
	 * Directs the connector to connect to the configured endpoint.
	 * This is the sequence of connection attempt procedures:<ol>
	 * <li>If the environment or system property {@link HeliosEndpointConfiguration#HOST} is set, the endpoint will attempt to connect to that host
	 * reading the remaining connection properties from set environment variables/system properties and the default properties, with the former taking presedence.</li>
	 * <li>If the environment or system property {@link HeliosEndpointConfiguration#DISCOVERY_ENABLED} is true (the default)
	 * then OT server discovery will be executed first. If a response is acquired, the endpoint will attempt to connect to the returned URI.</li>
	 * <li>If the discovery fails, or a connect to the discovered URI fails, the endpoint will attempt to connect using the merged properties defined
	 * between the set environment variables/system properties and the default properties, with the former taking presedence.</li>
	 * </ol>
	 * In short, discovery takes presedence unless a host has been defined. After that, default values overriden by env/system properties take over. This connection procedure
	 * continues until a connection is made. 
	 * 
	 * Note that when the endpoint discovery request receives a response, the URI is parsed and the system properties of the JVM are set in accordance with 
	 * the properties therein. However, if the post discovery connection fails, the system properties set as a result of the discovery response are
	 * reset to their former values under the assumption that some contraint is preventing the discovery supplied URI connection to be made.
	 * @param endpoint The endpoint being connected
	 * @return the connected connector
	 */
	public static synchronized AbstractEndpointConnector connect(@SuppressWarnings("rawtypes") HeliosEndpoint endpoint) {
		Logger LOG = Logger.getLogger(HeliosEndpoint.class);
		AbstractEndpointConnector connector = null;
		// If the OT Server host name is specified, first do a standard connect attempt
		if(ConfigurationHelper.isDefined(HeliosEndpointConfiguration.HOST)) {
			if(LOG.isDebugEnabled()) LOG.debug("Connect Phase 1: OT Server Host is defined [" + ConfigurationHelper.getSystemThenEnvProperty(HeliosEndpointConfiguration.HOST, "<None>") + "]. Attempting Regular Connection....");
			try {
				connector = HeliosEndpointConfiguration.getProtocol().createConnector(endpoint);
				connector.doConnect();
				if(!connector.isConnected()) throw new Exception();
				if(LOG.isDebugEnabled()) LOG.debug("Connect Phase 1: Succeeded");
			} catch (Exception e) {
				// connection failed.
				connector = null;
			}
		}
		if(connector!=null) return connector;
		if(LOG.isDebugEnabled()) LOG.debug("Connect Phase 1: Failed");
		// Since the HOST was not defined, or this attempt failed, we will try discovery
		// First, backup all the connection sysprops
		Properties beforeDiscoveryProperties = HeliosEndpointConfiguration.getAllConnectionProperties();
		if(LOG.isDebugEnabled()) LOG.debug("Connect Phase 2: Issuing Multicast Discovery Request");
		try {
			try {
				String discoveredURI = OTServerDiscovery.discover(HeliosEndpointConfiguration.getDiscoveryPreferredProtocol());
				if(discoveredURI!=null && !discoveredURI.trim().isEmpty()) {
					// We got a discovery response, so parse it and set the ac cording system props.
					URI uri = new URI(discoveredURI);
					if(LOG.isDebugEnabled()) LOG.debug("Connect Phase 2: Discovery Response Received:[" + uri + "]");
					System.setProperty(HeliosEndpointConfiguration.PROTOCOL, uri.getScheme().toUpperCase().trim());
					System.setProperty(HeliosEndpointConfiguration.HOST, uri.getHost().toUpperCase().trim());
					System.setProperty(HeliosEndpointConfiguration.PORT, "" + uri.getPort());					
					connector = HeliosEndpointConfiguration.getProtocol().createConnector(endpoint);
					if(LOG.isDebugEnabled()) LOG.debug("Connect Phase 2: Connecting....");
					connector.doConnect();					
				}
				if(!connector.isConnected()) throw new Exception();
				if(LOG.isDebugEnabled()) LOG.debug("Connect Phase 2: Succeeded");
			} catch (Exception e) {
				// connection failed.
				connector = null;
			}			
		} catch (Exception e) {
			
		}
		if(connector!=null) return connector;
		if(LOG.isDebugEnabled()) LOG.debug("Connect Phase 2: Failed");
		// If we get here, discovery got no response or we failed to connect with the properties supplied by the discovery
		// First reset the OT related connection system props to what they were before discovery
		System.setProperties(beforeDiscoveryProperties);
		// Now try and standard connect
		if(LOG.isDebugEnabled()) {
			String uri = new StringBuilder(HeliosEndpointConfiguration.getProtocol().name().toLowerCase())
				.append("://").append(HeliosEndpointConfiguration.getHost())
				.append(":").append(HeliosEndpointConfiguration.getPort()).toString();
			LOG.debug("Connect Phase 3: Connecting to [" + uri + "] ........");
		}
		connector = HeliosEndpointConfiguration.getProtocol().createConnector(endpoint);
		LOG.debug("Connect Phase 3: " + (connector.isConnected() ? "Succeeded" : "Failed"));
		return connector;
	}
	
	/**
	 * Directs the connector to disconnect
	 */
	public abstract void disconnect();
	
	/**
	 * Directs the connector to connect to the configured endpoint
	 */
	public abstract void doConnect();
	
	
	/**
	 * Indicates if this connector is connected
	 * @return true if this connector is connected, false otherwise
	 */
	public boolean isConnected() {
		return connected.get();
	}
	
	/**
	 * Marks this connector as connected
	 */
	protected void setConnected() {
		connected.set(true);
		createFlushThread();
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run() {
				try {
					log.info("Disconnecting......");
					disconnect();
					log.info("Disconnected.");
				} catch (Exception e) {}
			}
		});
		fireConnectedEvent();
	}
	
	/**
	 * Executes any events registered for connection events
	 */
	protected void fireConnectedEvent() {
		
	}
	
	/**
	 * Marks this connector as disconnected
	 * @param deliberate true if the disconnect was requested, false if for any other reason
	 */
	protected void setDisconnected(boolean deliberate) {
		connected.set(false);
		flushThreadRunning = false;
		if(flushThread!=null && flushThread.isAlive()) {
			flushThread.interrupt();
			flushThread = null;
		}
		fireDisconnectedEvent(deliberate);
	}
	
	/**
	 * Executes any events registered for disconnection events
	 * @param deliberate true if the disconnect was requested, false if for any other reason
	 */
	protected void fireDisconnectedEvent(boolean deliberate) {
		
	}

	/**
	 * Returns 
	 * @return the instrumentation
	 */
	public ConnectorChannelInstrumentation getInstrumentation() {
		return instrumentation;
	}

	/**
	 * Returns the number of failed sends
	 * @return the failedSendCounter
	 */
	@JMXAttribute(name="FailedSends", description="The number of failed sends", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFailedSendCounter() {
		return failedSendCounter.get();
	}
	
	/**
	 * Returns the number of sends
	 * @return the sendCounter
	 */
	@JMXAttribute(name="Sends", description="The number of sends", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSendCounter() {
		return sendCounter.get();
	}
	

	
}
