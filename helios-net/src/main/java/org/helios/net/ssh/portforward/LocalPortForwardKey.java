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
package org.helios.net.ssh.portforward;

import java.io.Serializable;
import java.util.Date;

import org.helios.net.ssh.ServerHostKey;

import ch.ethz.ssh2.KnownHosts;

/**
 * <p>Title: LocalPortForwardKey</p>
 * <p>Description: A synthetic key that uniquely identifies a logical {@link LocalPortForward} </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.portforward.LocalPortForwardKey</code></p>
 */

public class LocalPortForwardKey implements Serializable {
	/**  */
	private static final long serialVersionUID = -6885697187894303159L;
	/** The host key that uniquely identifies the underlying {@link org.helios.net.ssh.SSHService} */
	private final ServerHostKey hostKey;
	/** The remote port that is being forwarded to */
	private final int remotePort;
	
	/**
	 * Creates a new LocalPortForwardKey
	 * @param hostKey The host key that uniquely identifies the underlying {@link org.helios.net.ssh.SSHService}
	 * @param remotePort The remote port that is being forwarded to
	 * @return a new LocalPortForwardKey
	 */
	public static LocalPortForwardKey newInstance(ServerHostKey hostKey, int remotePort) {
		return new LocalPortForwardKey(hostKey, remotePort);
	}
	
	/**
	 * Creates a new LocalPortForwardKey
	 * @param hostKey The host key that uniquely identifies the underlying {@link org.helios.net.ssh.SSHService}
	 * @param remotePort The remote port that is being forwarded to
	 */
	private LocalPortForwardKey(ServerHostKey hostKey, int remotePort) {
		this.hostKey = hostKey;
		this.remotePort = remotePort;
	}

	/**
	 * Returns the host key that uniquely identifies the underlying {@link org.helios.net.ssh.SSHService}
	 * @return the host key that uniquely identifies the underlying {@link org.helios.net.ssh.SSHService}
	 */
	public ServerHostKey getHostKey() {
		return hostKey;
	}

	/**
	 * Returns the remote port that is being forwarded to
	 * @return the remote port that is being forwarded to
	 */
	public int getRemotePort() {
		return remotePort;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hostKey == null) ? 0 : hostKey.hashCode());
		result = prime * result + remotePort;
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalPortForwardKey other = (LocalPortForwardKey) obj;
		if (hostKey == null) {
			if (other.hostKey != null)
				return false;
		} else if (!hostKey.equals(other.hostKey))
			return false;
		if (remotePort != other.remotePort)
			return false;
		return true;
	}
	
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("LocalPortForwardKey [")
	    	.append(TAB).append("keyType = ").append(hostKey.getKeyType())
	        .append(TAB).append("hostKey = ").append(KnownHosts.createHexFingerprint(hostKey.getKeyType(), hostKey.getHostKey()))
	        .append(TAB).append("created = ").append(new Date(hostKey.getCreated()))
	        .append(TAB).append("hostName = ").append(hostKey.getHostName())
	        .append(TAB).append("userName = ").append(hostKey.getUserName())
	        .append(TAB).append("sshd port = ").append(hostKey.getPort())
	        .append(TAB).append("forwarded port = ").append(remotePort)
	        .append("\n]");    
	    return retValue.toString();
	}
	
}
