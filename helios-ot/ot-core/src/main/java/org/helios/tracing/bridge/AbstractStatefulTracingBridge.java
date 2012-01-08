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
package org.helios.tracing.bridge;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;


/**
 * <p>Title: AbstractStatefulTracingBridge</p>
 * <p>Description: An abstract base class for StatefulTracingBridge implementations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.bridge.AbstractStatefulTracingBridge</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public abstract class AbstractStatefulTracingBridge extends AbstractTracingBridge implements IStatefulTracingBridge {
	/** A set of registered bridge state listeners */
	protected final Set<IBridgeStateListener> stateListeners = new CopyOnWriteArraySet<IBridgeStateListener>();
	/** A counter for the number of consecutive connection failures */
	protected final AtomicLong connectFailures = new AtomicLong(0);
	/** The bridge connected state */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** The bridge re-connecting state */
	protected final AtomicBoolean reconnecting = new AtomicBoolean(false);
	
	/**
	 * Creates a new AbstractStatefulTracingBridge 
	 * @param name The tracing bridge name
	 * @param bufferSize The maximum size of the trace bufffer before it is flushed. 0 will disable size triggered flushes.
	 * @param frequency The frequency on which the trace buffer is flushed (ms.) 0 will disable time triggered flushes.
	 * @param intervalCapable Indicates if the bridge is interval capable
	 * 
	 */
	public AbstractStatefulTracingBridge(String name,
			int bufferSize, long frequency, boolean intervalCapable) {
		super(name, bufferSize, frequency, intervalCapable);
	}
	
	
	/**
	 * Determines if the bridge is connected. Stateless bridges should return true.
	 * @return true if the brige is connected or the bridge is stateless, false if the bridge is not connected.
	 */
	public boolean isConnected() {
		return connected.get();
	}
	
	/**
	 * Directs the bridge to connect to the configured endpoint. NoOp for stateless bridges.
	 */
	public void connect() {
		if(isConnected()) return;
		try {
			doConnect();
			connected.set(true);
			connectFailures.set(0L);
			fireBridgeConnected();
		} catch (Exception e) {
			connectFailures.incrementAndGet();
			fireBridgeConnectFailure();
		}
	}
	
	/**
	 * The internal connect 
	 */
	protected abstract void doConnect();
	
	/**
	 * Directs the bridge to disconnect from the configured endpoint. 
	 */
	public void disconnect() {
		if(!isConnected()) return;
		try {
			doDisconnect();			
		} catch (Exception e) {
		} finally {
			connected.set(false);
			fireBridgeDisconnected();
		}
	}
	
	/**
	 * The internal disconnect 
	 */
	protected abstract void doDisconnect();
	
	
	/**
	 * Registers a bridge state listener that will be notified of bridge connectivity state changes
	 * @param listener The listener to register
	 */
	public void registerBridgeStateListener(IBridgeStateListener listener) {
		if(listener!=null) {
			stateListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a bridge state listener
	 * @param listener The listener to unregister
	 * @return true if the listener was removed, false if it was not registered
	 */	
	public boolean unregisterBridgeStateListener(IBridgeStateListener listener) {
		if(listener!=null) {
			return stateListeners.remove(listener);
		} else {
			return false;
		}
	}
	
	/**
	 * Returns the number of registered state listeners
	 * @return the number of registered state listeners
	 */
	@JMXAttribute(name="StateListenerCount", description="The number of registered state listeners", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getStateListenerCount() {
		return stateListeners.size();
	}
	
	/**
	 * Returns true if the bridge is stateful and can be up or down.
	 * @return true if the bridge is stateful and can be up or down, false if it is stateless.
	 */
	public boolean isStateful() {
		return true;
	}
	
	
	/**
	 * Fires a bridge connected event to registered state listeners.
	 */
	protected void fireBridgeConnected() {
		final IStatefulTracingBridge bridge = this;
		for(IBridgeStateListener listener: stateListeners) {
			final IBridgeStateListener ibsl = listener;			
			threadPool.execute(new Runnable(){
				public void run() {
					ibsl.onConnect(bridge);
				}
			});			
		}
	}
	
	/**
	 * Fires a bridge disconnected event to registered state listeners.
	 */
	protected void fireBridgeDisconnected() {
		final IStatefulTracingBridge bridge = this;
		for(IBridgeStateListener listener: stateListeners) {
			final IBridgeStateListener ibsl = listener;			
			threadPool.execute(new Runnable(){
				public void run() {
					ibsl.onDisconnect(bridge);
				}
			});			
		}
	}
	
	/**
	 * Fires a bridge connect failure event to registered state listeners.
	 */
	protected void fireBridgeConnectFailure() {
		final IStatefulTracingBridge bridge = this;
		for(IBridgeStateListener listener: stateListeners) {
			final IBridgeStateListener ibsl = listener;			
			threadPool.execute(new Runnable(){
				public void run() {
					ibsl.onConnectFailure(bridge, connectFailures.get());
				}
			});			
		}
	}
	
	/**
	 * Determines if the bridge is running a reconnect poll.
	 * @return true if the bridge is running a reconnect poll, false if it is not.
	 */
	public boolean isReconnecting() {
		return getReconnecting();
	}
	

	/**
	 * Returns the number of consecutive connection failures.
	 * @return the number of consecutive connection failures.
	 */
	@JMXAttribute(name="ConnectFailures", description="The number of consecutive connection failures", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getConnectFailures() {
		return connectFailures.get();
	}

	/**
	 * Indicates if the bridge is connected
	 * @return true if the bridge is connected
	 */
	@JMXAttribute(name="Connected", description="Indicates if the bridge is connected", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getConnected() {
		return isConnected();
	}

	/**
	 * Indicates if the bridge is reconnecting
	 * @return true if the bridge is reconnecting.
	 */
	@JMXAttribute(name="Reconnecting", description="Indicates if the bridge is reconnecting", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getReconnecting() {
		return reconnecting.get();
	}

}


/**
	@TODO:
	Implement basic startConnectPolling / stopConnectPolling
	Will require inner start..../stop....
	Can managed reconnecting boolean
	
*/