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
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/**
 * <p>Title: PropFilePasswordAuthenticator</p>
 * <p>Description: Password authenticator that validates credentials against a resources property file</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.auth.PropFilePasswordAuthenticator</code></p>
 */
public class PropFilePasswordAuthenticator implements PasswordAuthenticator {
	/** The properties containing the username/password pairs to authenticate with */
	final Properties credentials = new Properties();
	/** The default bad password */
	final String DEF = "" + System.identityHashCode(this);
	/** The instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	/**
	 * Creates a new PropFilePasswordAuthenticator
	 * @param fileName The name of the file to read the properties from
	 */
	public PropFilePasswordAuthenticator(CharSequence fileName) {
		if(fileName==null) throw new IllegalArgumentException("Passed file name was null", new Throwable());
		File f = new File(fileName.toString());
		if(!f.canRead()) {
			throw new RuntimeException("Cannot read the file [" + fileName + "]", new Throwable());
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			if(f.getName().toLowerCase().endsWith(".xml")) {
				credentials.loadFromXML(fis);
			} else {
				credentials.load(fis);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to load the file [" + fileName + "]", e);
		} finally {
			try { fis.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.apache.sshd.server.PasswordAuthenticator#authenticate(java.lang.String, java.lang.String, org.apache.sshd.server.session.ServerSession)
	 */
	@Override
	public boolean authenticate(String username, String password, ServerSession session) {
		if(username==null || password==null) {
			log.info("Authentication failed for [" + username + "]. No username or password.");
			return false;
		}
		if(!password.equals(credentials.getProperty(username, DEF))) {
			log.info("Authentication failed for [" + username + "]. Invalid username or password.");
			return false;
		}
		log.info("Authenticated [" + username + "]");
		return true;
	}

}
