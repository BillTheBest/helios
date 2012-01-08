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
package org.helios.nativex.jmx.net.process;

import org.helios.nativex.jmx.net.ConnectionType;

/**
 * <p>Title: NetConn</p>
 * <p>Description: A wrapper class to represent a NetConnection.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.net.process.NetConn</code></p>
 */
public class NetConn {
	/** The connection type */
	protected ConnectionType connType = null;
	/** The local address */
	protected String localAddress = null;
	/** The remote address */
	protected String remoteAddress = null;
	/** The local port */
	protected int localPort = -1;
	/** The remote port */
	protected int remotePort = -1;
	/** The state of the connection */
	protected String state = null;
	/** The process id of the process owning the local socket */
	protected long pid = 01L;
	
}
