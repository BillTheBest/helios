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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.ServerHostKeyVerifier;

/**
 * <p>Title: SSHService</p>
 * <p>Description: SSH2 service for providing remot and local port tunneling, SCP file transfer and virtual terminal functionality.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.SSHService</code></p>
 */

public class SSHService implements ConnectionMonitor {
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
	/** An SSH Connection */
	protected Connection sshConnection = null;
	/** Connection timeout in ms. */
	protected int connectionTimeout = 10000;
	/** Connection state indicator */
	protected final  AtomicBoolean connected = new AtomicBoolean(false);
	/** A static map of created SSH Services */
	protected static final Map<String, SSHService> services = new ConcurrentHashMap<String, SSHService>();
	/** A map of created local port forwards for this connection */
	protected final Map<String, LocalPortForward> localPortForwarders = new ConcurrentHashMap<String, LocalPortForward>();
	/** the last connect time */
	protected Date connectTime = null;
	/** the last disconnect time */
	protected Date disconnectTime = null;
	/** Service listeners for this instance */
	protected final Set<SSHServiceConnectionListener> connectionListeners = new CopyOnWriteArraySet<SSHServiceConnectionListener>();
	
	
	/** Service listeners to be automatically added to all created instances */
	protected static final Set<SSHServiceConnectionListener> staticConnectionListeners = new CopyOnWriteArraySet<SSHServiceConnectionListener>();

	
	
	/**
	 * Creates a new SSHService
	 * @param host The SSH server host name or IP address
	 * @param port The SSH server port 
	 * @param sshUserName The SSH server user name
	 */
	protected SSHService(String host, int port, String sshUserName) {
		this.host = host;
		this.port = port;
		this.sshUserName = sshUserName;
		log.info("Created SSH Service For " + this  );
	}
	
	/**
	 * Generates a key unqiuely identifying an SSHService
	 * @param host The SSH server host name or IP address
	 * @param port The SSH server port 
	 * @param userName The SSH server user name
	 * @return An SSHService key
	 */
	public static String serviceKey(String host, int port, String userName) {
		return new StringBuilder(userName).append("@").append(host).append(":").append(port).toString();
	}
	
	/**
	 * Generates a key unqiuely identifying a port forward
	 * @param local_port The local port to listen on
	 * @param remote_port The remote port to forward to
	 * @return the port forward service key that uniquely identifies this port forward 
	 */
	public String portForwardServiceKey(int local_port,  int remote_port)   {		
		return new StringBuilder("localhost:").append(local_port).append("-->").append(this).append("[").append(remote_port).append("]").toString();
	}
	

	/**
	 * Creates a new SSHService. Check to see if it is connected before using it.
	 * @param host The SSH server host name or IP address
	 * @param port The SSH server port 
	 * @param userName The SSH server user name
	 * @return an SSHService
	 */	
	public static SSHService createSSHService(String host, int port, String userName) {
		String key = serviceKey(host, port, userName);
		SSHService sshService = services.get(key);
		if(sshService==null) {
			synchronized(services) {
				sshService = services.get(key);
				if(sshService==null) {
					sshService =  new SSHService(host, port, userName);
					services.put(key, sshService);
				}
			}
		}
		return sshService;
	}
	
	
	/**
	 * Connects and authenticates to the SSH server.
	 * The order of authentication methods will be:<ol>
	 * <li>Non authentication will be attempted first.</li>
	 * <li>If a private key and a passphrase have been supplied, public key authentication will be attempted .</li>
	 * <li>If a private key has been supplied, but not a passphrase, public key authentication will be attempted without passphrase. </li>
	 * <li>If a password has been supplied, password authentication will be attempted</li>
	 * <li>If a private key and a password have been supplied, public key authentication will be attempted using the password as the private key passphrase.</li>
	 * </ol>
	 * @throws Exception An exception is thrown if the connection or authentication or host key verification fails. 
	 */
	public void connect() throws Exception {
		if(connected.get()) return;
		synchronized(connected) {
			validate();
			log.info(this + "  Connecting....");
			sshConnection = new Connection(host, port);
			sshConnection.connect(hostKeyVerifier, connectionTimeout, connectionTimeout);
			log.info(this + "  Connected. Starting authentication....");
			Map<String, Exception> exceptions = authenticate();
			if(exceptions!=null) {
				connected.set(false);
				throw new Exception("Failed to authenticate to [" + this + "] using any method");
			}
			if(!sshConnection.isAuthenticationComplete()) {
				throw new Exception("Authentication incomplete for [" + this + "] after successful methods");
			}
			log.info("Connected to [" + this + "]");
			sshConnection.setTCPNoDelay(true);
			sshConnection.addConnectionMonitor(this);
			connected.set(true);
			connectTime = new Date();
		}
	}
	
	/**
	 * Indicates the connection status
	 * @return true if connected, false if not
	 */
	public boolean isConnected() {
		return connected.get();
	}
	
	
	/**
	 * Executes the stacked authentication process. If the return is null, authentication succeeded.
	 * @return a map of exceptions keyed by the authentication type name attempted.
	 */
	protected Map<String, Exception> authenticate() {
		Map<String, Exception> exceptionMap = new HashMap<String, Exception>();
		try { noAuth(); return null; } catch (Exception e) {exceptionMap.put("NoAuth", e); }
		try { keyAuthFull(); return null; } catch (Exception e) {exceptionMap.put("KeyAuthFull", e); }
		try { keyAuthNoPass(); return null; } catch (Exception e) {exceptionMap.put("KeyAuthNoPass", e); }
		try { authPassword(); return null; } catch (Exception e) {exceptionMap.put("PasswordAuth", e); }
		try { keyAuthPassword(); return null; } catch (Exception e) {exceptionMap.put("KeyPasswordAuth", e); }		
		return exceptionMap;
	}
	
	/**
	 * Callback when the connection is lost
	 * @param reason The root cause of the connection loss.
	 * @see ch.ethz.ssh2.ConnectionMonitor#connectionLost(java.lang.Throwable)
	 */
	public void connectionLost(java.lang.Throwable reason) {
		connected.set(false);
		disconnectTime = new Date();
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
		if(sshConnection.authenticateWithPublicKey(sshUserName, pemPrivateKey, sshUserPassword)) {
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
	 * Creates a new loca port forward in this connection to the connection's SSH host if one does not exist already.
	 * If the connection is not open, will attempt to connect and authenticate. 
	 * @param local_port The local port to listen on
	 * @param remote_port The remote port to forward to
	 * @throws Exception Thrown if the connection fails or the port forward request fails.
	 */
	public void localPortForward(int local_port,  int remote_port)  throws Exception {
		try {
			if(!connected.get()) {
				connect(); 
			}
			String portForwardKey = portForwardServiceKey(local_port, remote_port);
			LocalPortForward lpf  = localPortForwarders.get(portForwardKey);
			
			if(lpf==null) {
				synchronized(localPortForwarders) {
					lpf = localPortForwarders.get(portForwardKey);
					if(lpf==null) {
						lpf = new LocalPortForward(this, local_port, remote_port); 							
						localPortForwarders.put(portForwardKey, lpf);
						log.info("Created New Loca Port Forward [" + portForwardKey + "]");
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to create local portforward", e);
			throw e;
		}
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
	 * Sets the ssh private key passphrase
	 * @param sshPassphrase the sshPassphrase to set
	 */
	public void setSshPassphrase(String sshPassphrase) {
		this.sshPassphrase = sshPassphrase;
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
	
	
	
}
