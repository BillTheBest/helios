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
	/**
	 * Validates connectivity with the HOT server
	 */
	public void ping();
	/**
	 * Sends a payload to the HOT server, requesting that the server echo it back
	 * @param payload The payload to echo
	 * @return The value echoed from the server
	 */
	public <T extends Serializable> T echo(T payload);
	/**
	 * Connects the client to the HOT server
	 */
	public void connect();
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
	 * @param traces
	 * @return
	 */
	public int submitTraces(@SuppressWarnings("rawtypes") Trace[] traces);
	public void addListener(HeliosOTClientEventListener listener);
	public void removeListener(HeliosOTClientEventListener listener);
}
