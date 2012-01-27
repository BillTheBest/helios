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

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.helios.net.ssh.SSHService;
import org.helios.net.ssh.ServerHostKey;

import ch.ethz.ssh2.LocalPortForwarder;
import ch.ethz.ssh2.channel.LocalAcceptThreadStateListener;

/**
 * <p>Title: LocalPortForward</p>
 * <p>Description: Encapsulates an SSH local port forward</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.LocalPortForward</code></p>
 */

public class LocalPortForward implements LocalAcceptThreadStateListener {
	/** Instance logger */
	protected Logger log = Logger.getLogger(getClass());	
	/** The wrapped port forwarder */
	protected LocalPortForwarder portForwarder;
	/** The remote address */
	protected final InetAddress remoteAddress;	
	/** The local listening port */
	protected final int localPort;
	/** The remote port */
	protected final int remotePort;
	/** The host address*/
	protected final String hostAddress;	
	/** The parent SSH Service */
	protected final SSHService service;
	/** The LocalPortForwardKey used to uniquely identify this port forward */
	protected final LocalPortForwardKey portForwardKey;
	
	/** the last connect time */
	protected Date connectTime = null;
	/** the last disconnect time */
	protected Date disconnectTime = null;
	/** Connection state indicator */
	protected final  AtomicBoolean connected = new AtomicBoolean(false);

	/**
	 * Creates a new LocalPortForward
	 * @param service The {@link SSHService} through which the port forward is created
	 * @param localPort The local port
	 * @param remotePort The remote port
	 */
	public LocalPortForward(SSHService service, int localPort, int remotePort) {
		try {
			this.portForwarder = service.getConnection().createLocalPortForwarder(localPort, service.getHost(), remotePort, this);
			this.service = service;
			this.remoteAddress = InetAddress.getByName(this.portForwarder.getRemoteHost());
			this.localPort = portForwarder.getLocalPort();
			this.remotePort = this.portForwarder.getRemotePort();
			this.hostAddress = remoteAddress.getHostAddress();
			this.portForwardKey = LocalPortForwardKey.newInstance(this.service.getHostKey(), this.remotePort);
			log.info("PortForward [" + this.localPort + ":" + service.getHost() + ":" + this.remotePort + "]:" + portForwarder.isBound());
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize LocalPortForward", e);
		}
//		try {
//			objectName = ObjectNameFactory.create(service.objectName.toString() + ",service=LocalPortForward,localPort=" + localPort + ",remotePort=" + remotePort);
//			server = ManagementFactory.getPlatformMBeanServer();
//			server.registerMBean(this, objectName);
//		} catch (Exception e) {
//			log.warn("Failed to register management interface for " + this, e);
//		}
		
	}
	
	/**
	 * Reconnects the port forward, reconnecting the parent service if necessary.
	 * @throws Exception thrown on reconnect failure
	 */
	public void restart() throws Exception {
		if(connected.get()) return;
		synchronized(connected) {
			if(connected.get()) return;
			if(!service.isConnected()) {
				service.connect();
			}
			portForwarder = service.getConnection().createLocalPortForwarder(localPort, hostAddress, remotePort, this);
		}
	}

	/**
	 * Returns the remote host name
	 * @return the remote host name
	 */
	public String getRemoteHostName() {
		return remoteAddress.getHostName();
	}
	
	/**
	 * Returns the remote host address
	 * @return the remote host address
	 */
	public String getRemoteHostAddress() {
		return remoteAddress.getHostAddress();
	}
	
	/**
	 * Returns the receive buffer size
	 * @return the receive buffer size
	 */
	public int getReceiveBufferSize() {
		try {
			return portForwarder.getReceiveBufferSize();
		} catch (SocketException e) {
			return -1;
		}
	}

	/**
	 * Returns the local port
	 * @return the localPort
	 */
	public int getLocalPort() {
		return localPort;
	}

	/**
	 * Returns the remote port
	 * @return the remotePort
	 */
	public int getRemotePort() {
		return this.portForwarder.getRemotePort();
	}
	
	/**
	 * Returns the number of bytes transferred from the local to the remote
	 * @return the number of bytes transferred from the local to the remote
	 */
	public long getLocalToRemoteBytesTransferred() {		
		return portForwarder.getBytesOutMetric().getBytesOut();
	}
	
	/**
	 * Returns the number of bytes transferred from the remote to the local
	 * @return the number of bytes transferred from the remote to the local
	 */
	public long getRemoteToLocalBytesTransferred() {
		return portForwarder.getBytesInMetric().getBytesIn();
	}
	
	/**
	 * Resets the bytes transferred counters
	 */
	public void resetTransferCounters() {		
		portForwarder.getBytesInMetric().resetMetrics();
		portForwarder.getBytesOutMetric().resetMetrics();
	}

	/**
	 * Closes the port forward
	 * @see ch.ethz.ssh2.LocalPortForwarder#close()
	 */
	public void close()  {
		try {
			portForwarder.close();
		} catch (Exception e) {}
	}

	/**
	 * Returns the Socket SO Timeout
	 * @return the Socket SO Timeout
	 * @see ch.ethz.ssh2.LocalPortForwarder#getSoTimeout()
	 */
	public int getSoTimeout()  {
		try {
			return portForwarder.getSoTimeout();
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Indicates if the socket is bound
	 * @return true if if the socket is bound
	 * @see ch.ethz.ssh2.LocalPortForwarder#isBound()
	 */
	public boolean isBound() {
		return portForwarder.isBound();
	}

	/**
	 * Indicates if the socket is closed
	 * @return true if the socket is closed
	 * @see ch.ethz.ssh2.LocalPortForwarder#isClosed()
	 */
	public boolean isClosed() {
		return portForwarder.isClosed();
	}
	

	/**
	 * Callback from local acceptor thread when the port forward starts
	 * @see ch.ethz.ssh2.channel.LocalAcceptThreadStateListener#onStart()
	 */
	@Override
	public void onStart() {
		connected.set(true);		
		connectTime = new Date();
	}

	/**
	 * Callback from local acceptor thread when the port forward stops
	 * @see ch.ethz.ssh2.channel.LocalAcceptThreadStateListener#onStop()
	 */
	@Override
	public void onStop() {
		connected.set(false);
		disconnectTime = new Date();
	}

	/**
	 * Indicates the connection status
	 * @return true if connected, false if not
	 */
	public boolean isConnected() {
		return connected.get();
	}
	
	
	/**
	 * Returns the last connect time
	 * @return the last connect time
	 */
	public Date getConnectTime() {
		return connectTime;
	}

	/**
	 * Returns the last disconnect time
	 * @return the last disconnect time
	 */
	public Date getDisconnectTime() {
		return disconnectTime;
	}

	/**
	 * Returns the LocalPortForwardKey used to uniquely identify this port forward
	 * @return the LocalPortForwardKey used to uniquely identify this port forward
	 */
	public LocalPortForwardKey getPortForwardKey() {
		return portForwardKey;
	}


}
