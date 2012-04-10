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
package org.helios.ot.agent;

import java.io.Serializable;
import java.net.URI;

import org.helios.ot.trace.Trace;

/**
 * <p>Title: HeliosOTClient</p>
 * <p>Description: Defines the base client for executing and consuming services on a Helios Open Trace Server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.HeliosOTClient</code></p>
 */

public interface HeliosOTClient {
	
	/** The JMX notification type for a client connected event */
	public static final String NOTIFICATION_CONNECT = "org.helios.ot.agent.connected";
	/** The JMX notification type for a client disconnected event */
	public static final String NOTIFICATION_DISCONNECT = "org.helios.ot.agent.disconnected";
	
	/**
	 * Validates connectivity with the HOT server
	 */
	public boolean ping();
	/**
	 * Sends a payload to the HOT server, requesting that the server echo it back
	 * @param payload The payload to echo
	 * @return The value echoed from the server
	 */
	public <T extends Serializable> T echo(T payload);
	
	/**
	 * Sets the URI to use to establish a connection to the Helios OT Server
	 * @param uri the URI to use to establish a connection to the Helios OT Server
	 */
	public void configureClient(URI uri);
	
	
	/**
	 * Synchronously connects the client to the HOT server.
	 * When this call returns (and no exception is thrown) the client is assumed to be connected.
	 */
	public void connect();
	/**
	 * Issues a connect command directing the client to connect to the HOT server.
	 * If the call is syncrhonous, When this call returns (and no exception is thrown) the client is assumed to be connected.
	 * Otherwise, the client is assumed disconnected until a connect event is emitted.
	 * The passed listeners will be fully registered (i.e. beyond the connection event) and will be called on a connect event 
	 * on synch or asynch connections. If the client is already connected when this operation is called, the listeners will still be called.
	 * @param asynch If true, the connect is performed asynchronously, otherwise, it is executed synchronously.
	 * @param listeners An optional array of listeners to register
	 */
	public void connect(boolean asynch, HeliosOTClientEventListener...listeners);
	
	/**
	 * Disconnects from the HOT server
	 */
	public void disconnect();
	/**
	 * Determines if the client is connected to the HOT server
	 * @return true if the client is connected, false otherwise
	 */
	public boolean isConnected();
	/**
	 * Returns the URI of the HOT server connection endpoint
	 * @return the connection URI or null if the client is not connected
	 */
	public URI getConnectionURI();
	
	/**
	 * Returns the name of the remoting protocol implemented by this client
	 * @return the name of the remoting protocol implemented by this client
	 */
	public String getProtocol();
	
	/**
	 * Submits an array of collected traces to the OT server
	 * @param traces An array of traces
	 */
	public void submitTraces(@SuppressWarnings("rawtypes") Trace[] traces);
	/**
	 * Registers a client event listener
	 * @param listener The listener to register
	 */
	public void addListener(HeliosOTClientEventListener listener);
	/**
	 * unregisters a client event listener
	 * @param listener The listener to unregister
	 */
	public void removeListener(HeliosOTClientEventListener listener);
	
	/**
	 * Returns the connection timeout in ms.
	 * @return the connection timeout
	 */
	public long getConnectTimeout();
	/**
	 * Sets the connection timeout in ms.
	 * @param timeout the connection timeout
	 */
	public void setConnectTimeout(long timeout);
	/**
	 * Returns the operation timeout in ms.
	 * @return the operation timeout
	 */
	public long getOperationTimeout();
	/**
	 * Sets the the operation timeout in ms
	 * @param timeout the operation timeout
	 */
	public void setOperationTimeout(long timeout);
	
}
