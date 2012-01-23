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
import org.apache.sshd.common.SessionListener;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

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
		Set<String> files = new HashSet<String>();
		for(File f: keyDir.listFiles()) {
			if(f.getName().contains("rsa") || f.getName().contains("dsa")) {
				if(f.getName().toLowerCase().endsWith(".pub")) {
//			if(f.getName().contains("sally")) {
				files.add(f.getAbsolutePath());
				}
			}
		}
		log.info("Adding [" + files.size() + "] keys to auth provider");
		keyPairProvider = new FileKeyPairProvider(files.toArray(new String[files.size()]));
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
		int fails = 0;		
		byte[] h = session.getKex().getH();
		byte[] k = session.getKex().getK();
		for(KeyPair kp: keyPairProvider.loadKeys()) {

				try {
					if(key.equals(kp.getPublic())) return true;
				} catch (Throwable e) {}
				fails++;
				log.info("Auth PK Fails for [" + username + "]:" + fails);
		}
		log.warn("Failed to match PK for [" + username + "] after [" + fails + "] attempts");
		return false;
		
//		String extension = null;
//		if("DSA".equals(key.getAlgorithm())) {
//			extension = "_dsa.pub";
//		} else if("DSA".equals(key.getAlgorithm())) {
//			extension = "_rsa.pub";
//		} else {
//			log.warn("Unrecognized algorithm for key [" + key.getAlgorithm() + "]");
//			return false;
//		}
//		File keyFile = new File(keyDir.getAbsolutePath() + File.separator + username + extension );
//		if(!keyFile.exists()) {
//			log.warn("No key file for [" + keyFile.getAbsolutePath() + "]");
//			return false;
//		}
//		byte[] passedKeyBytes = key.getEncoded();
		//return false;
	}
	
//	protected byte[] getKeyBytes(File f) {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream((int)f.length());
//		FileInputStream fis = null;
//	}

}
