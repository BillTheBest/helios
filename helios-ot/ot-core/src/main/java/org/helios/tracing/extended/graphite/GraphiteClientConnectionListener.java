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
package org.helios.tracing.extended.graphite;

/**
 * <p>Title: GraphiteClientConnectionListener</p>
 * <p>Description: Defines a listener that is notified of GraphiteClient connect and disconnect events.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.graphite.GraphiteClientConnectionListener</p></code>
 */
public interface GraphiteClientConnectionListener {
	/**
	 * Called when a GraphiteClient connects or reconnects.
	 * @param graphiteHost The Graphite Server host name or IP address
	 * @param graphitePort The Graphite Server listening port
	 */
	public void onConnect(String graphiteHost, int graphitePort);
	
	/**
	 * Called when a GraphiteClient disconnects.
	 * @param graphiteHost The Graphite Server host name or IP address
	 * @param graphitePort The Graphite Server listening port
	 * @param ex The exception that caused the disconnect. Null if client was closed cleanly.
	 */
	public void onDisconnect(String graphiteHost, int graphitePort, Exception ex);
	
	/**
	 * Called when a GraphiteClient fails to connect.
	 * @param graphiteHost The Graphite Server host name or IP address
	 * @param graphitePort The Graphite Server listening port
	 * @param failureCount The number of consecutive failures.
	 * @param ex The exception that caused the disconnect. Null if client was closed cleanly.
	 */
	public void onConnectFailure(String graphiteHost, int graphitePort, int failureCount, Exception ex);
	
}
