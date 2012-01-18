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

import org.apache.log4j.Logger;

import ch.ethz.ssh2.ServerHostKeyVerifier;

/**
 * <p>Title: YesManHostKeyVerifier</p>
 * <p>Description: A host key verifier that approves all keys</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.YesManHostKeyVerifier</code></p>
 */

public class YesManHostKeyVerifier implements ServerHostKeyVerifier {

	/** Static logger */
	private static final Logger LOG = Logger.getLogger(YesManHostKeyVerifier.class); 
	/**
	 * Verifies an SSH host key on every key exchange
	 * @param hostname the hostname used to create the Connection object
	 * @param port the remote TCP port
	 * @param serverHostKeyAlgorithm the public key algorithm (ssh-rsa or ssh-dss)
	 * @param serverHostKey the server's public key blob 
	 * @return Always returns true
	 * @throws Exception
	 * @see ch.ethz.ssh2.ServerHostKeyVerifier#verifyServerHostKey(java.lang.String, int, java.lang.String, byte[])
	 */
	@Override
	public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
		LOG.info("Validated [" + serverHostKeyAlgorithm + "] host key from [" + hostname + ":" + port + "]");
		return true;
	}
	
    /**
     * Converts the passed byte array to a string
     * @param data the bye array
     * @return a string
     */
    public static String convertToHex(byte[] data) { 
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) { 
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do { 
                if ((0 <= halfbyte) && (halfbyte <= 9)) 
                    buf.append((char) ('0' + halfbyte));
                else 
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        } 
        return buf.toString();
    } 

}
