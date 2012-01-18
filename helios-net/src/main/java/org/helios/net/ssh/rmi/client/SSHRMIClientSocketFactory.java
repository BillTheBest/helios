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
package org.helios.net.ssh.rmi.client;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

import org.apache.log4j.Logger;

/**
 * <p>Title: SSHRMIClientSocketFactory</p>
 * <p>Description: A {@link RMIClientSocketFactory} implementation that connects to the designated endpoint through an SSH port forward.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.rmi.client.SSHRMIClientSocketFactory</code></p>
 */

public class SSHRMIClientSocketFactory implements RMIClientSocketFactory, Serializable {

	/** Static logger */
	protected static final Logger LOG = Logger.getLogger(SSHRMIClientSocketFactory.class);
	/** The host IP address to connect back to */
	protected String targetAddress;
	/** The target port to reach through the tunnel */
	protected int tunneledPort;
	/** The mbean serverid of the originating JVM */
	protected String agentId = null;

	
	/**
	 * Creates a new SSHRMIClientSocketFactory that will generate sockets that attempt to connect back to the passed host name
	 * using the port found in a matching local port forward MBean in the client platofrm agent MBeanServer.
	 * @param targetAddress The host of the server to connect back to though a tunnel.
	 * @param tunneledPort THe remote port to tunnel to
	 * @param sshRmiSockets A set of locally created sshrmi sockets
	 */
	public SSHRMIClientSocketFactory(String targetAddress, int tunneledPort) {
		this.targetAddress = targetAddress;
		this.tunneledPort = tunneledPort;
		this.agentId = getAgentId();
		//this.sshRmiSockets = sshRmiSockets;
	}
	
	/**
	 * Retrieves the platform agent mbean server id.
	 * @return the platform agent mbean server id.
	 */
	public static String getAgentId() {
		try {
			return (String)ManagementFactory.getPlatformMBeanServer().getAttribute(MBEAN_SERVER_DELEGATE, "MBeanServerId");
		} catch (Exception e) {
			throw new RuntimeException("Failed to get platform agent mbeanserver id", e);
		} 
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.rmi.server.RMIClientSocketFactory#createSocket(java.lang.String, int)
	 */
	@Override
	public Socket createSocket(String host, int port) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
