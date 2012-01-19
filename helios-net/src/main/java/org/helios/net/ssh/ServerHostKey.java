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
package org.helios.net.ssh;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import ch.ethz.ssh2.KnownHosts;

/**
 * <p>Title: ServerHostKey</p>
 * <p>Description: A container for a SSH server host key</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.ServerHostKey</code></p>
 */

public class ServerHostKey implements Serializable {
	/**  */
	private static final long serialVersionUID = 5958718325321177220L;
	/** The SSH Server host key */
	private final byte[] hostKey;
	/** The timestamp of the creation of this host key */
	private final long created;
	/** The host name of the SSH server */
	private final String hostName;
	/** The user used to connect to the SSH server */
	private final String userName;
	/** The key type of the server key */
	private final String keyType;
	/** Indicates if this is a shared connection */
	private final boolean shared;
	
	/** The listening port of the SSH server */
	private final int port;

	/**
	 * Creates a new ServerHostKey
	 * @param sshService The {@link SSHService} that this host key was received by
	 * @return a ServerHostKey
	 */
	public static ServerHostKey newInstance(SSHService sshService) {
		return new ServerHostKey(sshService);
	}
	
	
	/**
	 * Creates a new ServerHostKey
	 * @param sshService The {@link SSHService} that this host key was received by
	 */
	private ServerHostKey(SSHService sshService) {
		created = System.currentTimeMillis();
		hostKey = sshService.getConnectionInfo().serverHostKey;
		keyType = sshService.getConnectionInfo().serverHostKeyAlgorithm;
		hostName = sshService.getHost();
		userName = sshService.getSshUserName();
		port = sshService.getPort();
		shared = sshService.isSharedConnection();
	}

	/**
	 * Indicates if this is a shared connection
	 * @return true if this connection is shared, false if it is dedicated
	 */
	public boolean isShared() {
		return shared;
	}

	/**
	 * The host key provided by the connect SSH server
	 * @return the SSH server hostKey
	 */
	public byte[] getHostKey() {
		return hostKey;
	}


	/**
	 * Returns the creation timestamp
	 * @return the creation timestamp
	 */
	public long getCreated() {
		return created;
	}


	/**
	 * Returns the SSH server host name
	 * @return the SSH server host name
	 */
	public String getHostName() {
		return hostName;
	}


	/**
	 * Returns the SSH server listening port
	 * @return the SSH server listening port
	 */
	public int getPort() {
		return port;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = 1;
		if(shared) {
			final int prime = 31;			
			result = prime * result + Arrays.hashCode(hostKey);
		} else {
			result = System.identityHashCode(this);
		}
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
		if(!shared) {
			return false;
		}
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerHostKey other = (ServerHostKey) obj;
		if (!Arrays.equals(hostKey, other.hostKey))
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
	    StringBuilder retValue = new StringBuilder("ServerHostKey [")
	    	.append(TAB).append("keyType = ").append(keyType)
	        .append(TAB).append("hostKey = ").append(KnownHosts.createHexFingerprint(keyType, hostKey))
	        .append(TAB).append("created = ").append(new Date(this.created))
	        .append(TAB).append("hostName = ").append(this.hostName)
	        .append(TAB).append("userName = ").append(this.userName)
	        .append(TAB).append("port = ").append(this.port)
	        .append(TAB).append("shared = ").append(this.shared)
	        .append("\n]");    
	    return retValue.toString();
	}


	/**
	 * Returns the user name the SSH server was connected to with
	 * @return the SSH user Name
	 */
	public String getUserName() {
		return userName;
	}


	/**
	 * Returns the server host key type (ssh-rsa or ssh-dss)
	 * @return the keyType
	 */
	public String getKeyType() {
		return keyType;
	}


}
