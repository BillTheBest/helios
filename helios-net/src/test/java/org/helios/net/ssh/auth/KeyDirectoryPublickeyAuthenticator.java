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

import java.io.File;
import java.security.PublicKey;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.helios.net.ssh.keys.KeyDecoder;
import org.helios.net.ssh.keys.UserAwarePublicKey;


/**
 * <p>Title: KeyDirectoryPublickeyAuthenticator</p>
 * <p>Description: Public key authenticator that validates usernames and public keys based on a directory structure.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.auth.KeyDirectoryPublickeyAuthenticator</code></p>
 */
public class KeyDirectoryPublickeyAuthenticator implements PublickeyAuthenticator {
	/** The key location directory */
	protected final File keyDir;
	/** The key pair provider */
	protected final FileKeyPairProvider keyPairProvider;
	/** The loaded public keys */
	protected final Map<String, Set<PublicKey>> pks = new ConcurrentHashMap<String, Set<PublicKey>>();
	/** The instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	
	
	
	/**
	 * Creates a new KeyDirectoryPublickeyAuthenticator
	 * @param keyDir The key location directory
	 */
	public KeyDirectoryPublickeyAuthenticator(File keyDir) {
		if(keyDir==null) throw new IllegalArgumentException("The passed directory was null", new Throwable());
		if(!keyDir.exists()) throw new IllegalArgumentException("The passed directory [" + keyDir + "] does not exist.", new Throwable());
		if(!keyDir.isDirectory()) throw new IllegalArgumentException("The passed file [" + keyDir + "] is not a directory.", new Throwable());
		this.keyDir = keyDir;	
		int loaded = 0;
		for(File f: keyDir.listFiles()) {
			if(f.getName().contains("rsa") || f.getName().contains("dsa")) {
				if(f.getName().toLowerCase().endsWith(".pub")) {
					try {						
						UserAwarePublicKey pubKey = KeyDecoder.getInstance().decodePublicKey(f);
						Set<PublicKey> userPks = pks.get(pubKey.getUserName());
						if(userPks==null) {
							userPks = new CopyOnWriteArraySet<PublicKey>();
							pks.put(pubKey.getUserName(), userPks);
						}
						userPks.add(pubKey.getPublicKey());
						//log.info("Added [" + pubKey + "]");
					} catch (Exception e) {
						log.warn("Failed to load public key in file [" + f.getAbsolutePath() + "]", e);
					}
				}
			}
		}
		for(Set<PublicKey> upks: pks.values()) {
			loaded += upks.size();
		}
		log.info("Loaded [" + loaded + "] keys for [" + pks.size() + "] users to auth provider");
		keyPairProvider = null;
	}

	/**
	 * Creates a new KeyDirectoryPublickeyAuthenticator
	 * @param keyDir The key location directory name
	 */
	public KeyDirectoryPublickeyAuthenticator(CharSequence keyDir) {
		this(new File(keyDir.toString()));
	}
	
	


	/**
	 * {@inheritDoc}
	 * @see org.apache.sshd.server.PublickeyAuthenticator#authenticate(java.lang.String, java.security.PublicKey, org.apache.sshd.server.session.ServerSession)
	 */
	@Override
	public boolean authenticate(String username, PublicKey key, ServerSession session) {
		if(username==null) throw new IllegalArgumentException("The passed user name was null", new Throwable());
		if(key==null) throw new IllegalArgumentException("The passed key was null", new Throwable());
		Set<PublicKey> userKeys = pks.get(username);
		if(userKeys==null) {
			synchronized(pks) {
				userKeys = pks.get(username);
				if(userKeys==null) {
					return false;
				}
			}
		}
		return userKeys.contains(key);
	}
	
//	protected byte[] getKeyBytes(File f) {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream((int)f.length());
//		FileInputStream fis = null;
//	}

}
