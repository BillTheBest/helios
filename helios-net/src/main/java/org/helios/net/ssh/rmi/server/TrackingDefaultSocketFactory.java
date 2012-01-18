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
package org.helios.net.ssh.rmi.server;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <p>Title: TrackingDefaultSocketFactory</p>
 * <p>Description: A server socket factory and cache of created sockets.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.rmi.server.TrackingDefaultSocketFactory</code></p>
 */

public class TrackingDefaultSocketFactory implements RMIServerSocketFactory, Serializable {
	/**  */
	private static final long serialVersionUID = 3995869541520202471L;
	/** A collection of server sockets created */
	protected final Set<ServerSocket> serverSockets = new CopyOnWriteArraySet<ServerSocket>();
	/** The bind address that this factory creates sockets for */
	protected transient InetAddress bindAddress;	
	/** The configured backlog */
	protected int backlog;
	/** The configured receive buffer size */
	protected int receiveBufferSize = DEFAULT_REC_BUFFER_SIZE;
	/** The configured accept timeout in ms. */
	protected int soTimeout;
	/** The default socket connection backlog */
	public static final int DEFAULT_BACKLOG = 100;
	/** The default receive buffer size */
	public static final int DEFAULT_REC_BUFFER_SIZE = 43690;
	/** The maximum receive buffer size that can be set after a server socket is bound */
	public static final int MAX_LIVE_BUFFER_SIZE = 64000;
	/** The default accept timeout */
	public static final int DEFAULT_SO_TIMEOUT = 0;
	
	/**
	 * Creates a new TrackingDefaultSocketFactory
	 * @param bindAddress The bind address that this factory creates sockets for
	 */
	public TrackingDefaultSocketFactory(InetAddress bindAddress) {
		this.bindAddress = bindAddress;
		backlog = DEFAULT_BACKLOG;
	}

	/**
	 * Creates a new TrackingDefaultSocketFactory
	 * @param backlog the maximum length of the queue on sockets created by this factory
	 */
	public TrackingDefaultSocketFactory(int backlog) {
		this.backlog = backlog;
	}
	
	/**
	 * Creates a new TrackingDefaultSocketFactory
	 * @param bindAddress The bind address that this factory creates sockets for
	 * @param backlog the maximum length of the queue on sockets created by this factory
	 */
	public TrackingDefaultSocketFactory(InetAddress bindAddress, int backlog) {
		this.bindAddress = bindAddress;
		this.backlog = backlog;
	}
	

	
	/**
	 * Creates a new unbound {@link ServerSocket}
	 * @return a new unbound  {@link ServerSocket}
	 * @throws IOException thrown on socket creation failures
	 */
	public ServerSocket createServerSocket() throws IOException {
		ServerSocket ss  = createServerSocket();
		ss.setReuseAddress(true);
		ss.setReceiveBufferSize(receiveBufferSize);
		ss.setSoTimeout(soTimeout);
		serverSockets.add(ss);
		return ss;
	}
	
	/**
	 * Creates a new {@link ServerSocket} bound to the passed port
	 * @param port The port to bind to
	 * @return a new bound  {@link ServerSocket}
	 * @throws IOException thrown on socket creation failures
	 * @see java.rmi.server.RMIServerSocketFactory#createServerSocket(int)
	 */	
	@Override
	public ServerSocket createServerSocket(int port) throws IOException {
		ServerSocket ss = createServerSocket(port);
		ss.setReuseAddress(true);		
		ss.setSoTimeout(soTimeout);
		ss.setReceiveBufferSize(receiveBufferSize > MAX_LIVE_BUFFER_SIZE ? MAX_LIVE_BUFFER_SIZE : receiveBufferSize);
		serverSockets.add(ss);
		return ss;
	}

	/**
	 * Creates a new {@link ServerSocket} bound to the passed port
	 * @param port The port to bind to
	 * @param backlog the maximum length of the queue. 
	 * @return a new bound  {@link ServerSocket}
	 * @throws IOException thrown on socket creation failures
	 * @see java.rmi.server.RMIServerSocketFactory#createServerSocket(int)
	 */		
	public ServerSocket createServerSocket(int port, int backlog) throws IOException {
		ServerSocket ss = createServerSocket(port, backlog);
		ss.setReuseAddress(true);
		ss.setSoTimeout(soTimeout);
		ss.setReceiveBufferSize(receiveBufferSize > MAX_LIVE_BUFFER_SIZE ? MAX_LIVE_BUFFER_SIZE : receiveBufferSize);  		
		serverSockets.add(ss);
		return ss;
	}
	
	/**
	 * Creates a new {@link ServerSocket} bound to the passed port on the pass address
	 * @param port The port to bind to
	 * @param backlog the maximum length of the queue. 
	 * @param ifAddress The address to bind to
	 * @return a new bound  {@link ServerSocket}
	 * @throws IOException thrown on socket creation failures
	 * @see java.rmi.server.RMIServerSocketFactory#createServerSocket(int)
	 */		
	public ServerSocket createServerSocket(int port, int backlog,  InetAddress ifAddress) throws IOException {
		ServerSocket ss = createServerSocket(port, backlog, ifAddress);
		ss.setReuseAddress(true);
		ss.setSoTimeout(soTimeout);
		ss.setReceiveBufferSize(receiveBufferSize > MAX_LIVE_BUFFER_SIZE ? MAX_LIVE_BUFFER_SIZE : receiveBufferSize);  				
		serverSockets.add(ss);
		return ss;
	}

	/**
	 * Returns the socket backlog queue size that will be configured for created server sockets
	 * @return the backlog the socket backlog queue size 
	 */
	public int getBacklog() {
		return backlog;
	}

	/**
	 * Sets the socket backlog queue size that will be configured for created server sockets
	 * @param backlog the backlog the socket backlog queue size
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * Returns the receive buffer size used to configure created server sockets
	 * @return the receiveBufferSize
	 */
	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}

	/**
	 * Sets the receive buffer size used to configure created server sockets
	 * @param receiveBufferSize the receiveBufferSize to set
	 */
	public void setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
	}

	/**
	 * Returns the bind address that created server sockets are bound to
	 * @return the bind address that created server sockets are bound to
	 */
	public InetAddress getBindAddress() {
		return bindAddress;
	}

	/**
	 * Returns the server socket accept timeout for created server sockets
	 * @return the server socket accept timeout for created server sockets
	 */
	public int getSoTimeout() {
		return soTimeout;
	}

	/**
	 * Sets the server socket accept timeout for created server sockets
	 * @param soTimeout the soTimeout to set in ms.
	 */
	public void setSoTimeout(int soTimeout) {
		if(soTimeout<0) throw new IllegalArgumentException("Illegal timeout value [" + soTimeout + "]", new Throwable());
		this.soTimeout = soTimeout;
	}
	

	

}
