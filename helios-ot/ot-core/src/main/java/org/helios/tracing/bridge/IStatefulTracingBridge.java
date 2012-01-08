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

/**
 * <p>Title: IStatefulTracingBridge</p>
 * <p>Description: Defines a stateful tracing bridge.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.bridge.IStatefulTracingBridge</code></p>
 */

public interface IStatefulTracingBridge extends ITracingBridge {
	/**
	 * Determines if the bridge is connected. Stateless bridges should return true.
	 * @return true if the brige is connected or the bridge is stateless, false if the bridge is not connected.
	 */
	public boolean isConnected();
	
	/**
	 * Directs the bridge to connect to the configured endpoint. 
	 */
	public void connect();
	
	/**
	 * Directs the bridge to disconnect from the configured endpoint. 
	 */
	public void disconnect();
	
	/**
	 * Directs a disconnected bridge to start reconnect polling.
	 */
	public void startReconnectPoll();
	
	/**
	 * Directs a disconnected or connected bridge to stop reconnect polling.
	 */
	public void stopReconnectPoll();
	
	/**
	 * Determines if the bridge is running a reconnect poll.
	 * @return true if the bridge is running a reconnect poll, false if it is not.
	 */
	public boolean isReconnecting();
	
	/**
	 * Registers a bridge state listener that will be notified of bridge connectivity state changes
	 * @param listener The listener to register
	 */
	public void registerBridgeStateListener(IBridgeStateListener listener);
	
	/**
	 * Unregisters a bridge state listener
	 * @param listener The listener to unregister
	 * @return true if the listener was removed, false if it was not registered
	 */	
	public boolean unregisterBridgeStateListener(IBridgeStateListener listener);

	// ADD REPRESENTATION HANDSHAKE calls for optimized remote serialization.
	// Or seperate interface ??
	
	// Register stateful bridge state listeners.
	// Execute callbacks on state change
	
	
}
