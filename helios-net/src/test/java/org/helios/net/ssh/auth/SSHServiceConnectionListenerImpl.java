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
package org.helios.net.ssh.auth;

import org.helios.net.ssh.SSHService;
import org.helios.net.ssh.SSHServiceConnectionListener;

/**
 * <p>Title: SSHServiceConnectionListenerImpl</p>
 * <p>Description: A No Op implementation of a SSHServiceConnectionListener for extending.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.auth.SSHServiceConnectionListenerImpl</code></p>
 */

public class SSHServiceConnectionListenerImpl implements SSHServiceConnectionListener {

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.SSHServiceConnectionListener#onConnectionFailure(java.lang.Throwable, org.helios.net.ssh.SSHService)
	 */
	@Override
	public void onConnectionFailure(Throwable t, SSHService sshService) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.SSHServiceConnectionListener#onConnectionSoftClosed(org.helios.net.ssh.SSHService, int)
	 */
	@Override
	public void onConnectionSoftClosed(SSHService sshService, int sharesRemaining) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.SSHServiceConnectionListener#onConnectionHardClosed(org.helios.net.ssh.SSHService)
	 */
	@Override
	public void onConnectionHardClosed(SSHService sshService) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.SSHServiceConnectionListener#onConnect(org.helios.net.ssh.SSHService)
	 */
	@Override
	public void onConnect(SSHService sshService) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.SSHServiceConnectionListener#onReconnect(org.helios.net.ssh.SSHService)
	 */
	@Override
	public void onReconnect(SSHService sshService) {

	}

}
