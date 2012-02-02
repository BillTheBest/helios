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

import java.io.Closeable;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.helios.net.ssh.SSHService;

import ch.ethz.ssh2.LocalPortForwarder;
import ch.ethz.ssh2.channel.LocalAcceptThreadStateListener;

/**
 * <p>Title: LocalPortForward</p>
 * <p>Description: Encapsulates an SSH local port forward</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.LocalPortForward</code></p>
 */

public class LocalPortForward implements LocalAcceptThreadStateListener, Closeable {
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
	
	/** The connect latch released when the service thread makes the start callback */
	protected final AtomicReference<CountDownLatch> connectLatch = new AtomicReference<CountDownLatch>(new CountDownLatch(1)); 
	/** The timeout while waiting on the portforward service thread to callback */
	protected long startTimeOut = 2000;
	/** the last connect time */
	protected Date connectTime = null;
	/** the last disconnect time */
	protected Date disconnectTime = null;
	/** Connection state indicator */
	protected final  AtomicBoolean connected = new AtomicBoolean(false);
	
	/** Port forward listeners for this instance */
	protected final Set<LocalPortForwardStateListener> portForwardListeners = new CopyOnWriteArraySet<LocalPortForwardStateListener>();
	/** Port forward listeners installed in all listeners */
	protected static final Set<LocalPortForwardStateListener> globalPortForwardListeners = new CopyOnWriteArraySet<LocalPortForwardStateListener>();
	

	/**
	 * Creates a new shared LocalPortForward with an ephemeral local port 
	 * @param service The {@link SSHService} through which the port forward is created
	 * @param remoteHost The host to forward to
	 * @param remotePort The remote port
	 */
	public LocalPortForward(SSHService service, String remoteHost, int remotePort) {
		this(service, remoteHost, 0, remotePort);
	}
	
	/**
	 * Creates a new shared LocalPortForward to the SSH host with an ephemeral local port 
	 * @param service The {@link SSHService} through which the port forward is created
	 * @param remotePort The remote port
	 */
	public LocalPortForward(SSHService service, int remotePort) {
		this(service, service.getHost(), 0, remotePort);
	}
	
	/**
	 * Creates a new LocalPortForward to the SSH host
	 * @param service The {@link SSHService} through which the port forward is created
	 * @param localPort The local port
	 * @param remotePort The remote port
	 */
	public LocalPortForward(SSHService service, int localPort, int remotePort) {
		this(service, service.getHost(), localPort, remotePort);
	}
	
	/**
	 * Creates a new LocalPortForward
	 * @param service The {@link SSHService} through which the port forward is created
	 * @param remoteHost The host to forward to
	 * @param localPort The local port
	 * @param remotePort The remote port
	 */
	public LocalPortForward(SSHService service, String remoteHost, int localPort, int remotePort) {
		try {
			this.portForwarder = service.getConnection().createLocalPortForwarder(localPort, remoteHost, remotePort, this);
			boolean started = connectLatch.get().await(startTimeOut, TimeUnit.MILLISECONDS);
			if(!started) {
				throw new Exception("Portforward start wait timed out after [" + startTimeOut + "] ms");
			}
			connectLatch.set(new CountDownLatch(1));
			this.service = service;
			this.remoteAddress = InetAddress.getByName(this.portForwarder.getRemoteHost());
			this.localPort = portForwarder.getLocalPort();
			this.remotePort = this.portForwarder.getRemotePort();
			this.hostAddress = remoteAddress.getHostAddress();
			this.portForwardKey = LocalPortForwardKey.newInstance(this.service.getHostKey(), remoteHost, this.remotePort, localPort, this.portForwarder.getLocalPort());
			portForwardListeners.addAll(globalPortForwardListeners);
			log.info("PortForward [" + this.localPort + ":" + remoteHost + ":" + this.remotePort + "]:" + portForwarder.isBound());
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
			boolean started = connectLatch.get().await(startTimeOut, TimeUnit.MILLISECONDS);
			if(!started) {
				throw new Exception("Portforward start wait timed out after [" + startTimeOut + "] ms");
			}
			connectLatch.set(new CountDownLatch(1));
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
	 * Executes a close on the local port forward
	 * @see ch.ethz.ssh2.LocalPortForwarder#close()
	 */
	@Override
	public void close() {
		try {
			portForwarder.close();	
			connected.set(false);
			for(LocalPortForwardStateListener listener: portForwardListeners) {
				listener.onClose(this);
			}
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
		connectLatch.get().countDown();
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

	/**
	 * Registers a global LocalPortForwardStateListener that will be registered with every new local port forward
	 * @param listener the global listener to register
	 */
	public static void addGlobalListener(LocalPortForwardStateListener listener) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null", new Throwable());
		globalPortForwardListeners.add(listener);
	}
	
	/**
	 * Unregisters a global LocalPortForwardStateListener
	 * @param listener the global listener to unregister
	 */
	public static void removeGlobalListener(LocalPortForwardStateListener listener) {
		if(listener!=null) {
			globalPortForwardListeners.remove(listener);
		}
	}
	
	/**
	 * Registers a LocalPortForwardStateListener for this instance
	 * @param listener the listener to register
	 */
	public void addListener(LocalPortForwardStateListener listener) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null", new Throwable());
		portForwardListeners.add(listener);
	}
	
	/**
	 * Unregisters a LocalPortForwardStateListener from this instance
	 * @param listener The listener to unregister
	 */
	public void removeListener(LocalPortForwardStateListener listener) {
		if(listener!=null) {
			portForwardListeners.remove(listener);
		}
	}


	
	

}
