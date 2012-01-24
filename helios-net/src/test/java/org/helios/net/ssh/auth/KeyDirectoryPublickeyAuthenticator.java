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
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.helios.net.ssh.keys.AuthorizedKeysDecoder;

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
	protected final Set<PublicKey> pks = new HashSet<PublicKey>();
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
		AuthorizedKeysDecoder akd = new AuthorizedKeysDecoder();
		for(File f: keyDir.listFiles()) {
			if(f.getName().contains("rsa") || f.getName().contains("dsa")) {
				if(f.getName().toLowerCase().endsWith(".pub")) {
					try {
						pks.add(akd.decodePublicKey(f));
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}
		}
		log.info("Adding [" + pks.size() + "] keys to auth provider");
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
		String keyType = key.getAlgorithm();
		File keyFile = new File(keyDir.getAbsolutePath() + File.separator + username + "_" + keyType.toLowerCase() + ".pub");
		log.info("Testing file [" + keyFile + "]");
		if(!keyFile.exists()) return false;
		PublicKey loadedKey = null;
		try {
			loadedKey = new AuthorizedKeysDecoder().decodePublicKey(keyFile);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return false;
		}			
		return loadedKey.equals(key);
	}
	
//	protected byte[] getKeyBytes(File f) {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream((int)f.length());
//		FileInputStream fis = null;
//	}

}
