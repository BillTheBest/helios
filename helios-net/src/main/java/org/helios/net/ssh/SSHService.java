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

import java.io.CharArrayWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.net.ssh.portforward.LocalPortForward;
import org.helios.net.ssh.portforward.LocalPortForwardStateListener;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.ServerHostKeyVerifier;

/**
 * <p>Title: SSHService</p>
 * <p>Description: SSH2 service for providing remot and local port tunneling, SCP file transfer and virtual terminal functionality.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.SSHService</code></p>
 */

public class SSHService implements ConnectionMonitor, Closeable, LocalPortForwardStateListener {
	/** Instance logger */
	protected Logger log = Logger.getLogger(getClass());
	/** The remote SSH server host name or IP address */
	protected String host = null;
	/** The remote SSH server sshd port */
	protected int port = 22;
	/** The SSH server user name */
	protected String sshUserName = null;
	/** The SSH server user password */
	protected String sshUserPassword = null;
	/** The SSH private key passphrase */
	protected String sshPassphrase = null;
	/** An SSH private key */
	protected char[] pemPrivateKey = null;
	/** An SSH HostKey Verifier  */
	protected ServerHostKeyVerifier hostKeyVerifier = new YesManHostKeyVerifier();
	/** A SSH Connection */
	protected Connection sshConnection = null;
	/** The connection info */
	protected ConnectionInfo connectionInfo = null;
	/** The server host key */
	protected ServerHostKey hostKey = null;
	/** Connection timeout in ms. */
	protected int connectionTimeout = 10000;
	/** Indicates if this connection should be shared */
	protected final AtomicBoolean sharedConnection = new AtomicBoolean(true);
	/** The number of shared references */
	protected final AtomicInteger shareCount = new AtomicInteger(0);
	/** Connection state indicator */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** Authenticated state indicator */
	protected final AtomicBoolean authenticated = new AtomicBoolean(false);
	/** A transient stack of remaining authentication methods for this connection */
	protected final Stack<String> remainingAuthMethods = new Stack<String>();
	/** A static map of created SSHServices keyed by ServerHostKey */
	protected static final Map<ServerHostKey, SSHService> keyedServices = new ConcurrentHashMap<ServerHostKey, SSHService>();
	/** A map of created local port forwards for this connection */
	protected final Set<LocalPortForward> localPortForwarders = new CopyOnWriteArraySet<LocalPortForward>();
	
	/** the last connect time */
	protected Date connectTime = null;
	/** the last disconnect time */
	protected Date disconnectTime = null;
	/** Service listeners for this instance */
	protected final Set<SSHServiceConnectionListener> connectionListeners = new CopyOnWriteArraySet<SSHServiceConnectionListener>();
	
	
	/** Service listeners to be automatically added to all created instances */
	protected static final Set<SSHServiceConnectionListener> globalConnectionListeners = new CopyOnWriteArraySet<SSHServiceConnectionListener>();
	/** The default host key verifier that accepts all host keys */
	public static final ServerHostKeyVerifier DEFAULT_VERIFIER = new YesManHostKeyVerifier();
	/** The system property or env name for the configured connect timeout in ms. */
	public static final String CONNECT_TIMEOUT_PROP = "org.helios.net.ssh.default.timeout.connect";
	/** The system property or env name for the configured kex timeout in ms. */
	public static final String KEX_TIMEOUT_PROP = "org.helios.net.ssh.default.timeout.kex";	
	/** The default connect timeout in ms. */
	public static final int DEFAULT_CONNECT_TIMEOUT = 2000;
	/** The default kex timeout in ms. */
	public static final int DEFAULT_KEX_TIMEOUT = 5000;

	
	
	/**
	 * Creates a new SSHService
	 * @param host The SSH server host name or IP address
	 * @param port The SSH server port 
	 * @param sshUserName The SSH server user name
	 * @param sharedConnection Indicates if this connection should be shared
	 */
	protected SSHService(String host, int port, String sshUserName, boolean sharedConnection) {
		this.host = host;
		this.port = port;
		this.sshUserName = sshUserName;
		this.sharedConnection.set(sharedConnection);
		connectionListeners.addAll(globalConnectionListeners);
		log.info("Created SSH Service For " + this  );
	}
	
	/**
	 * Generates a visual key describing an SSHService
	 * @param host The SSH server host name or IP address
	 * @param port The SSH server port 
	 * @param userName The SSH server user name
	 * @return An SSHService visual key
	 */
	public static String serviceKey(String host, int port, String userName) {
		return new StringBuilder(userName).append("@").append(host).append(":").append(port).toString();
	}
	
	/**
	 * Generates a visual key describing a port forward
	 * @param hostName The hostname or IP address to tunnel a port to 
	 * @param local_port The local port to listen on
	 * @param remote_port The remote port to forward to
	 * @return a port forward visual key 
	 */
	public static String portForwardServiceKey(String hostName, int local_port,  int remote_port)   {		
		return new StringBuilder("127.0.0.1:").append(local_port).append("-->").append(hostName).append(":").append(remote_port).toString();
	}
	
	/**
	 * Generates a visual key describing a stream forward
	 * @param hostName The hostname or IP address to tunnel a stream to 
	 * @param remote_port The remote port to stream forward to
	 * @return a stream forward visual key 
	 */
	public static String streamForwardServiceKey(String hostName, int remote_port)   {		
		return new StringBuilder(hostName).append(":").append(remote_port).toString();
	}	
	

	/**
	 * Creates a new SSHService. Check to see if it is connected before using it.
	 * @param host The SSH server host name or IP address
	 * @param port The SSH server port 
	 * @param userName The SSH server user name
	 * @param sharedConnection Indicates if this connection should be shared
	 * @return an SSHService
	 */
	public static SSHService createSSHService(String host, int port, String userName, boolean sharedConnection) {
		return new SSHService(host, port, userName, sharedConnection);
//		String key = serviceKey(host, port, userName);
//		SSHService sshService = services.get(key);
//		if(sshService==null) {
//			synchronized(services) {
//				sshService = services.get(key);
//				if(sshService==null) {
//					sshService =  new SSHService(host, port, userName, sharedConnection, sharedPortForwards);
//					services.put(key, sshService);
//				}
//			}
//		}
//		return sshService;
	}
	
	/**
	 * Creates a new shared connection SSHService that creates shared port forwards. 
	 * Check to see if it is connected before using it.
	 * @param host The SSH server host name or IP address
	 * @param port The SSH server port 
	 * @param userName The SSH server user name
	 * @return an SSHService
	 */
	public static SSHService createSSHService(String host, int port, String userName) {
		return createSSHService(host, port, userName, true);
	}
	
	/**
	 * Creates a new shared connection SSHService with a target port of 22 that creates shared port forwards. 
	 * Check to see if it is connected before using it.
	 * @param host The SSH server host name or IP address
	 * @param userName The SSH server user name
	 * @return an SSHService
	 */
	public static SSHService createSSHService(String host, String userName) {
		return createSSHService(host, 22, userName, true);
	}
	
	/**
	 * Creates a new shared connection SSHService with a target port of 22 and the current JVM user that creates shared port forwards. 
	 * Check to see if it is connected before using it.
	 * @param host The SSH server host name or IP address
	 * @return an SSHService
	 */
	public static SSHService createSSHService(String host) {
		return createSSHService(host, 22, System.getProperty("user.name"), true);
	}
	
	/**
	 * Returns the number of cached SSHService instances
	 * @return the number of cached SSHService instances
	 */
	public static int getSSHServiceInstanceCount() {
		return keyedServices.size();
	}
	
	/**
	 * Indicates if the service cache contains a shared SSHService instance for the passed SSH server host and port
	 * @param host The target SSH server host name or IP Address
	 * @param port The target SSH server listening port
	 * @return true if the cache has such an instance, false otherwise
	 */
	public static boolean hasSharedServiceFor(String host, int port) {
		if(host==null) throw new IllegalArgumentException("The passed host was null", new Throwable());
		for(SSHService service: keyedServices.values()) {
			if(service.getHost().equals(host) && service.getPort()==port) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Indicates if the service cache contains a shared SSHService instance for the passed SSH server host and port 22
	 * @param host The target SSH server host name or IP Address
	 * @return true if the cache has such an instance, false otherwise
	 */
	public static boolean hasSharedServiceFor(String host) {
		return hasSharedServiceFor(host, 22);
	}
	
	
	/**
	 * Returns the number of shared cached SSHService instances
	 * @return the number of shared cached SSHService instances
	 */
	public static int getSharedSSHServiceInstanceCount() {
		int cnt = 0;
		for(ServerHostKey key: keyedServices.keySet()) {
			if(key.isShared()) {
				cnt++;
			}
		}
		return cnt++;
	}
	
	/**
	 * Returns the number of unshared cached SSHService instances
	 * @return the number of unshared cached SSHService instances
	 */
	public static int getExclusiveSSHServiceInstanceCount() {
		int cnt = 0;
		for(ServerHostKey key: keyedServices.keySet()) {
			if(!key.isShared()) {
				cnt++;
			}
		}
		return cnt++;
	}
	
	/**
	 * Connects this SSHService
	 * @return a connected service which may be the same instance or a shared instance connected to the same SSH server.
	 * @throws SSHConnectionException
	 */
	public SSHService connect() throws SSHConnectionException {
		if(connected.get()) return this;
		SSHService svc = null;
		synchronized(connected) {
			try { validate(); } catch (Exception e) { throw new SSHConnectionException("SSHConnection failed pre-connect validation", e); }
			log.info(this + "  Connecting....");
			
			sshConnection = new Connection(host, port);
			try {
				connectionInfo = sshConnection.connect(hostKeyVerifier, connectionTimeout, connectionTimeout);
				for(String authMethod: sshConnection.getRemainingAuthMethods(sshUserName)) {
					remainingAuthMethods.push(authMethod);
				}				
				log.info("Remaining Auth Methods:" + Arrays.toString(sshConnection.getRemainingAuthMethods(sshUserName)));
				hostKey = ServerHostKey.newInstance(this);
				for(SSHServiceConnectionListener listener: connectionListeners) {
					listener.onConnect(this);
				}
			} catch (Exception e) {
				throw new SSHConnectionException("SSHConnection failed to connect to [" + host + ":" + port + "]", e );
			}
			
			if(isSharedConnection()) {
				SSHService sharedService = keyedServices.get(hostKey);
				if(sharedService==null) {
					synchronized(keyedServices) {
						sharedService = keyedServices.get(hostKey);
						if(sharedService==null) {													
							svc = this;
							svc.shareCount.incrementAndGet();
							keyedServices.put(hostKey, this);
							log.info("Connected to [" + this + "]");
							try { sshConnection.setTCPNoDelay(true); } catch (Exception e) {}
							sshConnection.addConnectionMonitor(this);
							connected.set(true);							
							connectTime = new Date();									
						}
					}
				}
				if(sharedService!=null) {
					
					this.sshConnection.close();					
					log.info("Returning shared service for " + sharedService.hostKey);
					svc = sharedService;
					svc.shareCount.incrementAndGet();
				}
			} else {				
				svc = this;
				svc.shareCount.incrementAndGet();
				keyedServices.put(hostKey, this);
				log.info("Connected to [" + this + "]");
				try { sshConnection.setTCPNoDelay(true); } catch (Exception e) {}
				sshConnection.addConnectionMonitor(this);
				connected.set(true);							
				connectTime = new Date();									
			}			
		}
		return svc;
	}
	
	/**
	 * Attempts to reconnect this disconnected SSHService
	 * @return true if connection was re-established, false otherwise.
	 */
	public boolean reconnect() {
		if(connected.get()) return true;
		try {
			connectionInfo = sshConnection.connect(hostKeyVerifier, connectionTimeout, connectionTimeout);
			connected.set(true);
			connectTime = new Date();
			completeAuthentication();
			// reconnect local port forwards
			// remove SSHService from keyedServices using old key
			// and reinsert using new key
			hostKey = ServerHostKey.newInstance(this);
			for(SSHServiceConnectionListener listener: connectionListeners) {
				listener.onReconnect(this);
			}

			return true;
		} catch (Exception e) {
			log.warn("Reconnect failed", e);
		}
		return false;
	}
	

	/**
	 * Attempts to find an existing shared SSHService that is already connected to the passed SSH server.
	 * Implements the connect and kex timeouts defined in {@literal CONNECT_TIMEOUT_PROP} and {@literal KEX_TIMEOUT_PROP}
	 * If either are not defined, they default to {@value SSHService#DEFAULT_CONNECT_TIMEOUT} and {@value SSHService#DEFAULT_KEX_TIMEOUT} respectively. 
	 * @param host The host name or IP address of the target SSH server
	 * @param port The listening port of the target SSH server
	 * @return an already existing and connected SSHService to the target SSH server, or null if one was not found
	 * @throws SSHConnectionException Thrown if initial connection fails to be established
	 */
	public static SSHService findServiceFor(String host, int port) throws SSHConnectionException {
		return findServiceFor(host, port, 
				ConfigurationHelper.getIntSystemThenEnvProperty(CONNECT_TIMEOUT_PROP, DEFAULT_CONNECT_TIMEOUT),
				ConfigurationHelper.getIntSystemThenEnvProperty(KEX_TIMEOUT_PROP, DEFAULT_KEX_TIMEOUT)
		);
	}
	
	/**
	 * Attempts to find an existing shared SSHService that is already connected to the passed SSH server on port 22.
	 * Implements the connect and kex timeouts defined in {@literal CONNECT_TIMEOUT_PROP} and {@literal KEX_TIMEOUT_PROP}
	 * If either are not defined, they default to {@value SSHService#DEFAULT_CONNECT_TIMEOUT} and {@value SSHService#DEFAULT_KEX_TIMEOUT} respectively. 
	 * @param host The host name or IP address of the target SSH server
	 * @return an already existing and connected SSHService to the target SSH server, or null if one was not found
	 * @throws SSHConnectionException Thrown if initial connection fails to be established
	 */
	public static SSHService findServiceFor(String host) throws SSHConnectionException {
		return findServiceFor(host, 22, 
				ConfigurationHelper.getIntSystemThenEnvProperty(CONNECT_TIMEOUT_PROP, DEFAULT_CONNECT_TIMEOUT),
				ConfigurationHelper.getIntSystemThenEnvProperty(KEX_TIMEOUT_PROP, DEFAULT_KEX_TIMEOUT)
		);
	}
	
	/**
	 * Attempts to find an existing shared SSHService that is already connected to the passed SSH server.
	 * @param host The host name or IP address of the target SSH server
	 * @param port The listening port of the target SSH server
	 * @param connectTimeout Connect the underlying TCP socket to the server with the given timeout value (non-negative, in milliseconds). Zero means no timeout. 
	 * @param kexTimeout Timeout for complete connection establishment (non-negative, in milliseconds). Zero means no timeout. 
	 * @return an already existing and connected SSHService to the target SSH server, or null if one was not found
	 * @throws SSHConnectionException Thrown if initial connection fails to be established
	 */
	public static SSHService findServiceFor(String host, int port, int connectTimeout, int kexTimeout) throws SSHConnectionException {
		Connection temporaryConnection = null;
		try {
			temporaryConnection = new Connection(host, port);
			ConnectionInfo cInfo = null;
			try {
				cInfo = temporaryConnection.connect(DEFAULT_VERIFIER, connectTimeout, kexTimeout);
			} catch (Exception e) {
				throw new SSHConnectionException("Failed to connect to SSH Server at [" + host + ":" + port + "]", e);
			}		
			ServerHostKey shk = ServerHostKey.newInstance(cInfo.serverHostKey);
			return keyedServices.get(shk);
		} finally {
			if(temporaryConnection!=null) {
				try { temporaryConnection.close(); } catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Connects and authenticates to the SSH server.
	 * If this SSHService is defined as a shared connection, and a shared connection for the same SSH server already exists,
	 * this service will be closed and discarded and the shared service will be returned. Otherwise, this service will
	 * be connected, authenticated and returned.
	 * The order of authentication methods will be:<ol>
	 * <li>Non authentication will be attempted first.</li>
	 * <li>If a private key and a passphrase have been supplied, public key authentication will be attempted .</li>
	 * <li>If a private key has been supplied, but not a passphrase, public key authentication will be attempted without passphrase. </li>
	 * <li>If a password has been supplied, password authentication will be attempted</li>
	 * <li>If a private key and a password have been supplied, public key authentication will be attempted using the password as the private key passphrase.</li>
	 * </ol>
	 * @return A connected and authenticated SSHService connected to the requested SSH server. May not be the same instance.
	 * @throws SSHAuthenticationException Thrown if the connection cannot authenticate or host key verification fails. 
	 */
	public SSHService authenticate() throws SSHAuthenticationException {
		if(!connected.get()) {
			try { 
				connect();
			} catch (SSHConnectionException she) {
				throw new SSHAuthenticationException("SSHService was not connected and connect failed in authentication phase", she);
			}
		}
		if(authenticated.get()) return this;
		try {
			completeAuthentication();
			return this;
		} catch (Exception e) {
			try { this.close(); } catch (Exception e2) {}
			if(e instanceof SSHAuthenticationException) throw (SSHAuthenticationException)e;
			throw new SSHAuthenticationException("Unknown authentication failure", e);
		}
		
	}
	
	/**
	 * Completes authentication on this service
	 * @throws Exception thrown on any error comleting authentication and configuring the connection
	 */
	protected void completeAuthentication() throws SSHAuthenticationException {
		log.info(this + "Starting authentication....");
		Map<String, Exception> exceptions = doAuthenticate();
		if(exceptions!=null) {							
			throw new SSHAuthenticationException("Failed to authenticate to [" + this + "]", exceptions);
		}
		if(!sshConnection.isAuthenticationComplete()) {				
			throw new SSHAuthenticationException("Authentication incomplete for [" + this + "] after successful methods");
		}
		log.info("Authenticated [" + this + "]");
		authenticated.set(true);
	}
	
	
	/**
	 * Returns the actual SSH connection
	 * @return the actual SSH connection
	 */
	public Connection getConnection() {
		return sshConnection;
	}
	
	
	/**
	 * Returns the SSH {@link ConnectionInfo} for this service
	 * @return the SSH {@link ConnectionInfo} for this service
	 */
	public ConnectionInfo getConnectionInfo() {
		return connectionInfo;
	}
	
	/**
	 * Indicates the connection status
	 * @return true if connected, false if not
	 */
	public boolean isConnected() {
		return connected.get();
	}
	
	/**
	 * Indicates the authentication status
	 * @return true if authenticated, false if not
	 */
	public boolean isAuthenticated() {
		return authenticated.get();
	}
	
	/**
	 * Executes the stacked authentication process. If the return is null, authentication succeeded.
	 * @return a map of exceptions keyed by the authentication type name attempted.
	 */
	protected Map<String, Exception> doAuthenticate() {
		Map<String, Exception> exceptionMap = new HashMap<String, Exception>();
		try { noAuth(); return null; } catch (Exception e) {}
		try { keyAuthFull(); return null; } catch (Exception e) {exceptionMap.put("KeyAuthFull", e); }
		try { keyAuthNoPass(); return null; } catch (Exception e) {exceptionMap.put("KeyAuthNoPass", e); }
		if(this.sshUserPassword!=null) try { authPassword(); return null; } catch (Exception e) {exceptionMap.put("PasswordAuth", e); }
		try { keyAuthPassword(); return null; } catch (Exception e) {exceptionMap.put("KeyPasswordAuth", e); }		
		return exceptionMap;
	}
	
	/**
	 * Callback when the connection is lost
	 * @param reason The root cause of the connection loss.
	 * @see ch.ethz.ssh2.ConnectionMonitor#connectionLost(java.lang.Throwable)
	 */
	@Override
	public void connectionLost(java.lang.Throwable reason) {
		for(SSHServiceConnectionListener listener: connectionListeners) {
			listener.onConnectionFailure(reason, this);
		}
		testConnection();
	}
	
	/**
	 * Tests the connection. If this fails, the connection is hard closed.
	 * @param timeout The timeout period to wait for the connection test
	 * @param unit The unit of the timeout
	 * @return true if the service is connected, false if the connection test failed and was hard closed.
	 */
	public boolean testConnection(long timeout, TimeUnit unit) {
		try {
			sshConnection.waitForKeyExchange(timeout, unit);
			return true;
		} catch (TimeoutException tex) {
			log.error("Test connection failed after timeout of [" + timeout + " " +  unit.name()+ "]. Will be scheduled for reconnect", tex);
			_disconnect();			
		} catch (Exception e) {
			log.error("Failed to validate connection after connection loss for " + this + ". Will be scheduled for reconnect", e);
			_disconnect();
		}				
		return false;
	}
	
	/**
	 * Tests the connection uisng the service's configured timeout. If this fails, the connection is hard closed.
	 * @return true if the service is connected, false if the connection test failed and was hard closed.
	 */
	public boolean testConnection() {
		return testConnection(this.connectionTimeout, TimeUnit.MILLISECONDS);
	}
	
	
	/**
	 * Attempts authentication with user name only
	 * @throws Exception on an authentication failire
	 */
	protected void noAuth() throws Exception {
		if(sshConnection.authenticateWithNone(sshUserName)) {
			if(log.isDebugEnabled()) log.debug("noAuth succeeded");
		} else {
			throw new Exception();
		}
	}
	
	
	/**
	 * Attempts authentication with the private key and passphrase
	 * @throws Exception on an authentication failire
	 */
	protected void keyAuthFull() throws Exception {
		if(sshConnection.authenticateWithPublicKey(sshUserName, pemPrivateKey, sshPassphrase)) {
			if(log.isDebugEnabled()) log.debug("KeyAuthFull succeeded");
		} else {
			throw new Exception();
		}
			
	}
	
	/**
	 * Attempts authentication with the private key and null passphrase
	 * @throws Exception on an authentication failire
	 */
	protected void keyAuthNoPass() throws Exception {
		if(sshConnection.authenticateWithPublicKey(sshUserName, pemPrivateKey, null)) {
			if(log.isDebugEnabled()) log.debug("KeyAuthNoPass succeeded");
		} else {
			throw new Exception();
		}			
	}
	
	/**
	 * Attempts authentication with password
	 * @throws Exception on an authentication failire
	 */
	protected void authPassword() throws Exception {
		if(sshConnection.authenticateWithPassword(sshUserName, sshUserPassword)) {
			if(log.isDebugEnabled()) log.debug("AuthPass succeeded");
		} else {
			throw new Exception();
		}			
	}
	
	/**
	 * Attempts authentication with the private key and password
	 * @throws Exception on an authentication failire
	 */
	protected void keyAuthPassword() throws Exception {
		if(sshConnection.authenticateWithPublicKey(sshUserName, pemPrivateKey, sshPassphrase)) {
			if(log.isDebugEnabled()) log.debug("KeyAuthPass succeeded");
		} else {
			throw new Exception();
		}			
	}
	

	/**
	 * Pre-connect validation.
	 * @throws Exception thrown if SSH server host name cannot be validated
	 */
	protected void validate() throws Exception {
		if(sshUserName==null || sshUserName.length()<1) {
			throw new Exception("No user name was provided");
		}
		try {
			InetAddress.getByName(host);
			if(log.isDebugEnabled()) log.debug("Validated host [" + host + "]");
		} catch (Exception e) {
			throw new Exception("Failed to validate SSH Server Host Name [" + host + "]");
		}
	}
	
	/**
	 * Closes the SSHService and all associated services.
	 */
	@Override
	public void close() {
		if(connected.get()) {
			int _sharedCount = shareCount.decrementAndGet();
			for(SSHServiceConnectionListener listener: connectionListeners) {
				listener.onConnectionSoftClosed(this, _sharedCount);
			}
			if(sharedConnection.get()) {			
				if(_sharedCount < 1) {
					_close();
				}
			} else {
				_close();
			}
		}
	}
	
	/**
	 * Returns the current shared count for this connection
	 * @return the current shared count for this connection
	 */
	public int getSharedCount() {
		return shareCount.get();
	}
	
	/**
	 * The actual close operation that closes the SSH connection and removes the service from the cache.
	 * This will be called when a non-shared connection is closed, or the last reference to a shared connection is closed and the share count drops to zero.
	 */
	protected void _close() {
		authenticated.set(false);
		disconnectTime = new Date();
		for(LocalPortForward  lpf: localPortForwarders) {   ////  NEEDS TO CALL HARD CLOSE ON LISTENERS
				lpf.close();
		}
		localPortForwarders.clear();
		if(connected.get()) {
			try { sshConnection.close(); } catch (Exception e) {}
			connected.set(false);
		}
		for(SSHServiceConnectionListener listener: connectionListeners) {
			listener.onConnectionHardClosed(this);
		}		
		shareCount.set(0);
		if(this.isSharedConnection()) {						
			SSHService removedService = keyedServices.remove(ServerHostKey.newInstance(this.hostKey.getHostKey()));
			assert removedService!=null;
		} else {
			SSHService removedService = keyedServices.remove(this.hostKey);
			assert removedService!=null;
		}
		
	}
	
	/**
	 * Cleans up the state of this SSHService when there is an unintentional disconnect and there is some expectation that reconnect will be successful.
	 */
	protected void _disconnect() {
		disconnectTime = new Date();
		connected.set(false);
		try { sshConnection.close(); } catch (Exception e) {}
		for(SSHServiceConnectionListener listener: connectionListeners) {
			listener.onConnectionHardClosed(this);
		}		
		Reconnector.getInstance().scheduleReconnect(this);
	}
	
	
	/**
	 * Creates a new local port forward in this connection using an ephemeral local port to the connection's SSH host if one does not exist already.
	 * If the connection is not open, will attempt to connect and authenticate. 
	 * @param remote_port The remote port to forward to
	 * @return The local port forwarder
	 * @throws Exception Thrown if the connection fails or the port forward request fails.
	 */
	public LocalPortForward localPortForward(int remote_port)  throws Exception {
		return localPortForward(0, remote_port);
	}
	
	/**
	 * Creates a new local port forward in this connection to the connection's SSH host if one does not exist already.
	 * If the connection is not open, will attempt to connect and authenticate. 
	 * @param local_port The local port to listen on
	 * @param remote_port The remote port to forward to
	 * @return The local port forwarder
	 * @throws Exception Thrown if the connection fails or the port forward request fails.
	 */
	public LocalPortForward localPortForward(int local_port,  int remote_port)  throws Exception {
		return localPortForward(getHost(), local_port, remote_port);
	}
	
	/**
	 * Creates a new local port forward in this connection to the specified host if one does not exist already.
	 * If the connection is not open, will attempt to connect and authenticate. 
	 * @param hostName The host name or IP address to forward to 
	 * @param remote_port The remote port to forward to
	 * @return The local port forwarder
	 * @throws Exception Thrown if the connection fails or the port forward request fails.
	 */
	public LocalPortForward localPortForward(String hostName, int remote_port)  throws Exception {
		return localPortForward(hostName, 0, remote_port);
	}
	
	/**
	 * Creates a new local port forward in this connection to the specified host if one does not exist already.
	 * If the connection is not open, will attempt to connect and authenticate. 
	 * @param hostName The host name or IP address to forward to 
	 * @param local_port The local port to listen on
	 * @param remote_port The remote port to forward to
	 * @return The local port forwarder
	 * @throws Exception Thrown if the connection fails or the port forward request fails.
	 */
	public LocalPortForward localPortForward(String hostName, int local_port,  int remote_port)  throws Exception {
		try {
			if(!connected.get()) {
				connect(); 
			}
			LocalPortForward lpf = new LocalPortForward(this, local_port, remote_port);
			String portForwardKey = portForwardServiceKey(hostName, local_port, remote_port);
			log.info("Created New Local Port Forward [" + portForwardKey + "]");
			localPortForwarders.add(lpf);
			return lpf;
		} catch (Exception e) {
			log.error("Failed to create local portforward", e);
			throw e;
		}
	}
	
	
	/**
	 * Returns the number of current local port forwards
	 * @return the number of current local port forwards
	 */
	public int getLocalPortForwardCount() {
		return localPortForwarders.size();
	}
	
	/**
	 * Returns an unmodifiable list of the current local port forwards
	 * @return an unmodifiable list of the current local port forwards
	 */
	public List<LocalPortForward> getLocalPortForwards() {
		return Collections.unmodifiableList(new ArrayList<LocalPortForward>(localPortForwarders));
	}
	


	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString()  {
	    return new StringBuilder("SSHService [").append(sshUserName).append("@").append(host).append(":").append(port).append("]").toString();    
	}

	/**
	 * Returns the private key character array
	 * @return the private key character array
	 */
	public char[] getPemPrivateKey() {
		return pemPrivateKey;
	}

	/**
	 * Sets the private key character array
	 * @param pemPrivateKey the private key character array to set
	 */
	public void setPemPrivateKey(char[] pemPrivateKey) {
		this.pemPrivateKey = pemPrivateKey;
	}
	
	/**
	 * Sets the private key character array
	 * @param pemPrivateKey the private key character array to set
	 * @return this SSHService
	 */
	public SSHService pemPrivateKey(char[] pemPrivateKey) {
		if(!isAuthenticated()) {
			this.pemPrivateKey = pemPrivateKey;
		}
		return this;
	}
	
	
	/**
	 * Sets the private key characters from the characters in the passed CharSequence
	 * @param privateKeyChars The private key
	 */
	public void setPemPrivateKeyString(CharSequence privateKeyChars) {
		this.pemPrivateKey = privateKeyChars.toString().toCharArray();
	}
	
	/**
	 * Writes the SSH private key from the passed File
	 * @param file The private key file
	 */
	public void setPemPrivateKeyFile(File file) {
		try {
			setPemPrivateKeyReader(new FileReader(file));
		} catch (IOException e) {
			throw new RuntimeException("Failed to open reader on file [" + file + "] for read of private key", e);
		}
	}
	
	
	/**
	 * Writes the SSH private key from the passed File
	 * @param file The private key file
	 * @return this service
	 */
	public SSHService pemPrivateKeyFile(File file) {
		if(!isAuthenticated()) {
			setPemPrivateKeyFile(file);
		}
		return this;
	}
	
	
	/**
	 * Writes the SSH private key from the passed URL
	 * @param url a URL resolving to the private key
	 */
	public void setPemPrivateKeyURL(URL url) {
		try {
			setPemPrivateKeyInputStream(url.openStream());
		} catch (IOException e) {
			throw new RuntimeException("Failed to open stream on URL [" + url + "] for read of private key", e);
		}
	}
	
	
	/**
	 * Writes the SSH private key from the passed input stream
	 * @param is an input stream providing the characters for the private key
	 */
	public void setPemPrivateKeyInputStream(InputStream is) {
		setPemPrivateKeyReader(new InputStreamReader(is));
	}
	
	/**
	 * Writes the SSH private key from the passed reader
	 * @param reader A reader that will read in the private key
	 */
	public void setPemPrivateKeyReader(Reader reader) {
		char[] buffer = new char[1024];
		CharArrayWriter wr = new CharArrayWriter();
		int charsRead = -1;
		try {
			while((charsRead=reader.read(buffer))!=-1) {
				wr.write(buffer, 0, charsRead);
			}
			pemPrivateKey = wr.toCharArray();
		} catch (Exception e) {
			log.error("Failed to read private key from reader", e);
			throw new RuntimeException("Failed to read private key from reader", e);
		} finally {
			
		}
	}
	
	// Set<SSHServiceConnectionListener> connectionListeners 

	/**
	 * Returns the implemented host key verifier
	 * @return the hostKeyVerifier
	 */
	public ServerHostKeyVerifier getHostKeyVerifier() {
		return hostKeyVerifier;
	}
	
	/**
	 * Returns the connected SSH server's host key
	 * @return the connected SSH server's host key
	 */
	public ServerHostKey getHostKey() {
		return hostKey;
	}

	/**
	 * Sets a new host key verifier
	 * @param hostKeyVerifier the host key verifier to set. Ignored if null.
	 */
	public void setHostKeyVerifier(ServerHostKeyVerifier hostKeyVerifier) {
		if(hostKeyVerifier!=null) {
			this.hostKeyVerifier = hostKeyVerifier;
		}
	}

	/**
	 * Returns the host name or IP address of the SSHd server
	 * @return the host name or IP address of the SSHd server
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Returns the listening port of the SSHd server
	 * @return the listening port of the SSHd server
	 */
	public int getPort() {
		return port;
	}

	/**
	 * The configured SSH user name
	 * @return the configured SSH user name
	 */
	public String getSshUserName() {
		return sshUserName;
	}

	/**
	 * Sets the user password.
	 * @param sshUserPassword the sshUserPassword to set
	 */
	public void setSshUserPassword(String sshUserPassword) {
		this.sshUserPassword = sshUserPassword;
	}

	/**
	 * Sets the user password.
	 * @param sshUserPassword the sshUserPassword to set
	 * @return this service
	 */	
	public SSHService sshUserPassword(String sshUserPassword) {
		if(!isAuthenticated()) {
			setSshUserPassword(sshUserPassword);
		}
		return this;
	}

	/**
	 * Sets the ssh private key passphrase
	 * @param sshPassphrase the sshPassphrase to set
	 */
	public void setSshPassphrase(String sshPassphrase) {
		this.sshPassphrase = sshPassphrase;
	}
	
	/**
	 * Sets the ssh private key passphrase
	 * @param sshPassphrase the sshPassphrase to set
	 * @return this service
	 */
	public SSHService sshPassphrase(String sshPassphrase) {
		if(!isAuthenticated()) {
			this.sshPassphrase = sshPassphrase;
		}
		return this;
	}
	

	/**
	 * Returns the connection timeout in ms.
	 * @return the connection timeout in ms.
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * Sets the connection timeout in ms.
	 * @param connectionTimeout the connection timeout in ms.
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	/**
	 * Indicates if this is a shared connection
	 * @return true if this is a shared connection, false if it is dedicated.
	 */
	public boolean isSharedConnection() {
		return sharedConnection.get();
	}
	
	/**
	 * @return the Object hashcode
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result
				+ ((sshUserName == null) ? 0 : sshUserName.hashCode());
		return result;
	}

	/**
	 * @param obj The object to compare to for equality
	 * @return true if the objects are equal, false otherwise
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
		SSHService other = (SSHService) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		if (sshUserName == null) {
			if (other.sshUserName != null)
				return false;
		} else if (!sshUserName.equals(other.sshUserName))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.portforward.LocalPortForwardStateListener#onClose(org.helios.net.ssh.portforward.LocalPortForward)
	 */
	@Override
	public void onClose(LocalPortForward lpf) {
		localPortForwarders.remove(lpf.getPortForwardKey());		
	}
	
	/**
	 * Registers a global SSHServiceConnectionListener that will be registered with every new SSHService
	 * @param listener the global listener to register
	 */
	public static void addGlobalListener(SSHServiceConnectionListener listener) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null", new Throwable());
		globalConnectionListeners.add(listener);
	}
	
	/**
	 * Unregisters a global SSHServiceConnectionListener
	 * @param listener the global listener to unregister
	 */
	public static void removeGlobalListener(SSHServiceConnectionListener listener) {
		if(listener!=null) {
			globalConnectionListeners.remove(listener);
		}
	}
	
	/**
	 * Registers a SSHServiceConnectionListener for this instance
	 * @param listener the listener to register
	 */
	public void addListener(SSHServiceConnectionListener listener) {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null", new Throwable());
		connectionListeners.add(listener);
	}
	
	/**
	 * Unregisters a SSHServiceConnectionListener from this instance
	 * @param listener The listener to unregister
	 */
	public void removeListener(SSHServiceConnectionListener listener) {
		if(listener!=null) {
			connectionListeners.remove(listener);
		}
	}
	
	
}
