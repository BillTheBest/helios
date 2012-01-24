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

import java.io.File;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.auth.UserAuthPublicKey;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.helios.net.ssh.auth.KeyDirectoryPublickeyAuthenticator;
import org.helios.net.ssh.auth.PropFilePasswordAuthenticator;

/**
 * <p>Title: ApacheSSHDServer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.ApacheSSHDServer</code></p>
 */
public class ApacheSSHDServer {
	/** Static class logger */
	static final Logger LOG = Logger.getLogger(ApacheSSHDServer.class);
	/** The server instance */
	static final AtomicReference<SshServer> server = new AtomicReference<SshServer>(null);
	
	/**
	 * @param args
	 */
	public static void main(String...args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		//Logger.getLogger(ChannelSession.class).setLevel(Level.WARN);
		Logger.getLogger("org.apache").setLevel(Level.WARN);
		//Logger.getLogger(ServerSession.class).setLevel(Level.WARN);
		//Logger.getLogger(SecurityUtils.class).setLevel(Level.WARN);

		SshServer sshd = server.get();
		if(sshd==null) {
			synchronized(server) {
				sshd = server.get();
				if(sshd!=null) {
					LOG.info("Server already running on port [" + sshd.getPort() + "]");
					return;
				} else {
					sshd = SshServer.setUpDefaultServer();
					server.set(sshd);
				}
			}
		}		

		LOG.info("Starting SSHd Server");
		
		int port = -1;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			port = 0;
		}
		sshd.setPort(port);
		sshd.setHost("0.0.0.0");
		//LOG.info("Listening Port [" + port + "]");
		Provider provider = new BouncyCastleProvider();
		Security.addProvider(provider);
		List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
		userAuthFactories.add(new UserAuthPublicKey.Factory());		
		userAuthFactories.add(new UserAuthPassword.Factory());
		//sshd.setUserAuthFactories(userAuthFactories);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(System.getProperty("java.io.tmpdir") + File.separator + "hostkey.ser"));
		sshd.setPasswordAuthenticator(NO_AUTH);
//		sshd.setPasswordAuthenticator(new PropFilePasswordAuthenticator("./src/test/resources/auth/password/credentials.properties"));
//		sshd.setPublickeyAuthenticator(new KeyDirectoryPublickeyAuthenticator("./src/test/resources/auth/keys"));
		
		
		
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			boolean useBash = false;
			if(System.getenv().containsKey("Path")) {
				for(String pathEntry: System.getenv().get("Path").split(";")) {
					File bashFile = new File(pathEntry + File.separator + "bash.exe");
					if(bashFile.exists() && bashFile.canExecute()) {
						useBash = true;
						break;
					}
				}
			}
			if(useBash) {
				LOG.info("Windows shell is bash");
				sshd.setShellFactory(new ProcessShellFactory(new String[] { "bash.exe", "-i", "-l"}, EnumSet.of(ProcessShellFactory.TtyOptions.ONlCr)));				
			} else {
				LOG.info("Windows shell is cmd");
				sshd.setShellFactory(new ProcessShellFactory(new String[] { "cmd.exe"}, EnumSet.of(ProcessShellFactory.TtyOptions.Echo, ProcessShellFactory.TtyOptions.ICrNl, ProcessShellFactory.TtyOptions.ONlCr)));
			}
			
		} else {
			sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/bash", "-i", "-l" }, EnumSet.of(ProcessShellFactory.TtyOptions.ONlCr)));
		}
		
		try {
			sshd.start();
			LOG.info("Server started on port [" + sshd.getPort() + "]");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * Returns the port of the running server
	 * @return the port of the running server
	 */
	public static int getPort() {
		SshServer sshd = server.get();
		if(sshd==null) throw new IllegalStateException("The SSHd server is not running", new Throwable());
		return sshd.getPort();
	}
	
	/** Passthrough authenticator. Always authenticates. */
	static final PasswordAuthenticator NO_AUTH = new PasswordAuthenticator() {
		public boolean authenticate(String username, String password, ServerSession session) {
			return true;
		}		
	};
	/** Property file driven password authenticator */
	static final PropFilePasswordAuthenticator PW_AUTH = new PropFilePasswordAuthenticator("./src/test/resources/auth/password/credentials.properties");
	/** Public key file driven authenticator */
	static final KeyDirectoryPublickeyAuthenticator KEY_AUTH = new KeyDirectoryPublickeyAuthenticator("./src/test/resources/auth/keys");
	
	/**
	 * Removes all authenticators and activates the NO_AUTH 
	 */
	public static void resetAuthenticators() {
		SshServer sshd = server.get();
		if(sshd==null) throw new IllegalStateException("The SSHd server is not running", new Throwable());
		sshd.setPasswordAuthenticator(NO_AUTH);
		sshd.setPublickeyAuthenticator(null);
	}
	
	/**
	 * Enables or disables the property file driven password authenticator
	 * @param active If true, enables the property file driven password authenticator, otherwise disables password based authentication
	 */
	public static void activatePasswordAuthenticator(boolean active) {
		SshServer sshd = server.get();
		if(sshd==null) throw new IllegalStateException("The SSHd server is not running", new Throwable());
		if(active) {
			sshd.setPasswordAuthenticator(PW_AUTH);
		} else {
			sshd.setPasswordAuthenticator(null);
		}
	}
	
	/**
	 * Enables or disables the key based authenticator
	 * @param active If true, enables the key authenticator, otherwise disables key based authentication
	 */
	public static void activateKeyAuthenticator(boolean active) {
		SshServer sshd = server.get();
		if(sshd==null) throw new IllegalStateException("The SSHd server is not running", new Throwable());
		if(active) {
			sshd.setPublickeyAuthenticator(KEY_AUTH);
		} else {
			sshd.setPublickeyAuthenticator(null);
		}
	}
	
	
	
	/**
	 * Stops the SSHd server immediately
	 */
	public static void stop() {
		stop(true);
	}
	
	
	/**
	 * Stops the SSHd server
	 * @param immediately If true, stops the server immediately, otherwise waits for pending requests.
	 */
	public static void stop(boolean immediately) {
		SshServer sshd = server.get();
		if(sshd==null) return;
		try {
			sshd.stop(immediately);
			server.set(null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to stop SSHd server", e);
		}		
	}
	
	public static String getAlgoList(Provider p) {
		StringBuilder b = new StringBuilder();
		Set<String> types = new HashSet<String>();
		for(Provider.Service svc: p.getServices()) {
			types.add(svc.getType());
			if("KeyGenerator".equals(svc.getType())) 
			b.append("\n\t").append("[").append(svc.getType()).append("] ").append(svc.getAlgorithm());
		}
		System.out.println("Types:\n" + types);
//		for(Map.Entry<Object, Object> entry: p.entrySet()) {
//			String key = (String)entry.getKey();
//			String value = (String)entry.getValue();
//			if(key.startsWith("Cipher."))
//			b.append("\n\t[").append(key).append("]:").append(value);			
//			if(key!=null && key.startsWith("Cipher.") && !key.contains(" ")) {
//				key = key.split("\\.")[1];				
//				b.append("\n\t[").append(key).append("]:").append(value);
//			}
//		}
		return b.toString();
	}

}
