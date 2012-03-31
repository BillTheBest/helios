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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;

/**
 * <p>Title: AbstractEndpointConnector</p>
 * <p>Description: Base class Netty connector for {@link HeliosEndpoint}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.AbstractEndpointConnector</code></p>
 */

public abstract class AbstractEndpointConnector {
	/** The worker thread pool */
	protected Executor workerExecutor;
	/** The pipeline factory */
	protected ChannelPipelineFactory channelPipelineFactory;
	/** The connection's channel future */
	protected ChannelFuture channelFuture;
	/** The endpoint this connector was created for */
	@SuppressWarnings("rawtypes")
	protected final HeliosEndpoint endpoint;
	/** The connected indicator. This is kept private to force impls. to set the connected state through the articlated methods */
	private final AtomicBoolean connected = new AtomicBoolean(false);	
	/** The instrumentation */
	protected final ConnectorChannelInstrumentation instrumentation = new ConnectorChannelInstrumentation();
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/** A set of connect listeners that will be added when an asynch connect is initiated */
	protected final Set<ChannelFutureListener> connectListeners = new CopyOnWriteArraySet<ChannelFutureListener>();
	

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
	 * Writes an object out to the listener at the end of the connector
	 * @param obj The object to send
	 */
	public abstract void write(Object obj);
	

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

	
}
