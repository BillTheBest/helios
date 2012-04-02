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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.threads.ExecutorBuilder;
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
public abstract class AbstractEndpointConnector {
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
	protected final ChannelUpstreamHandler responseProcessor = new ChannelUpstreamHandler() {
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
			//log.info("Handling Upstream:" + e.getClass().getSimpleName());
			if(e instanceof MessageEvent) {
				Object obj = ((MessageEvent)e).getMessage();
				log.info("Upstream Return Value [" + obj.getClass().getName() + "]:" + obj.toString());
			}
			ctx.sendUpstream(e);			
		}
	};
	/** The receive listener */
	protected final ChannelDownstreamHandler responseProcessor2 = new ChannelDownstreamHandler() {
		public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
			//log.info("Handling Downstream:" + e.getClass().getSimpleName());
			if(e instanceof MessageEvent) {
				Object obj = ((MessageEvent)e).getMessage();
				//log.info("Downstream Return Value [" + obj.getClass().getName() + "]:" + obj.toString());
			}
			ctx.sendDownstream(e);			
		}
	};
	
	
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
	protected AbstractEndpointConnector(@SuppressWarnings("rawtypes") HeliosEndpoint endpoint) {
		this.endpoint = endpoint;
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
				// org.helios.endpoints:type=HeliosEndpoint,name=HeliosEndpoint
				.setPoolObjectName(new StringBuilder("org.helios.endpoints:name=").append(getClass().getSimpleName()).append(",service=ThreadPool,type=Worker,protocol=").append(getProtocol().name()))
				.setPrestartThreads(1)
				.setTaskQueueSize(1000)
				.setTerminationTime(5000)
				.setThreadGroupName(getClass().getSimpleName() + "WorkerThreadGroup")
				.setUncaughtExceptionHandler(endpoint)
				.build();
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
	 * Writes a trace collection out to the listener at the end of the connector
	 * @param traceCollection The closed traces to send
	 */
	public abstract void write(TraceCollection<?> traceCollection);

	

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
	 * Returns this connector's protocol
	 * @return this connector's protocol
	 */
	protected abstract Protocol getProtocol();
	
	/**
	 * Directs the connector to connect to the configured endpoint
	 */
	public abstract void connect();
	
	/**
	 * Directs the connector to disconnect
	 */
	public abstract void disconnect();
	
	
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
