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

/**
 * <p>Title: SSHServiceConnectionListener</p>
 * <p>Description: Defines a listener that is notified when an SSHService instance is disconnected or reconnected.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.SSHServiceConnectionListener</code></p>
 */
public interface SSHServiceConnectionListener {
	/**
	 * Callback from an {@link SSHService} when the connection is unintentionally lost.
	 * @param t The associated exception
	 * @param sshService The {@link SSHService} that had a connection failure. 
	 */
	public void onConnectionFailure(Throwable t, SSHService sshService);
	
	/**
	 * Callback from an {@link SSHService} when shared connection is soft closed (but not really)
	 * @param sshService The {@link SSHService} that closed
	 * @param sharesRemaining The number of shares left on the connection
	 */
	public void onConnectionSoftClosed(SSHService sshService, int sharesRemaining);
	
	
	/**
	 * Callback from an {@link SSHService} when the connection is <i>really</i> closed.
	 * @param sshService The {@link SSHService} that closed
	 */
	public void onConnectionHardClosed(SSHService sshService);
	
	/**
	 * Callback from an {@link SSHService} when the connection is initially made
	 * @param sshService The {@link SSHService} that connected
	 */
	public void onConnect(SSHService sshService);
	
	/**
	 * Callback from an {@link SSHService} when a lost connection is reconnected
	 * @param sshService The {@link SSHService} that reconnected
	 */
	public void onReconnect(SSHService sshService);
	
	
}
