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
package org.helios.net.ssh.keys;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Scanner;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * <p>Title: AuthorizedKeysDecoder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author WhiteFang34 http://stackoverflow.com/questions/3531506/using-public-key-from-authorized-keys-with-java-security
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.keys.AuthorizedKeysDecoder</code></p>
 */

public class AuthorizedKeysDecoder {
    private byte[] bytes;
    private int pos;
    
    /**
     * @param publicKeyFile
     * @return
     * @throws Exception
     */
    public PublicKey decodePublicKey(File publicKeyFile) throws Exception {
    	if(publicKeyFile==null) throw new IllegalArgumentException("The passed file was null", new Throwable());
    	if(!publicKeyFile.canRead()) throw new IllegalArgumentException("The passed file [" + publicKeyFile + "] cannot be read", new Throwable());
    	FileInputStream fis = null;
    	StringBuilder b = new StringBuilder((int)publicKeyFile.length());
    	try {
    		fis = new FileInputStream(publicKeyFile);
    		BufferedReader bfr = new BufferedReader(new InputStreamReader(fis));
    		String line = null;
    		while((line = bfr.readLine())!=null) {
    			b.append(line);
    		}
    		return decodePublicKey(b.toString());
    	} finally {
    		try { fis.close(); } catch (Exception e) {}
    	}
    }

    public PublicKey decodePublicKey(String keyLine) throws Exception {
        bytes = null;
        pos = 0;

        // look for the Base64 encoded part of the line to decode
        // both ssh-rsa and ssh-dss begin with "AAAA" due to the length bytes
        for (String part : keyLine.split(" ")) {
            if (part.startsWith("AAAA")) {
                bytes = Base64.decodeBase64(part);
                break;
            }
        }
        if (bytes == null) {
            throw new IllegalArgumentException("no Base64 part to decode");
        }

        String type = decodeType();
        if (type.equals("ssh-rsa")) {
            BigInteger e = decodeBigInt();
            BigInteger m = decodeBigInt();
            RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } else if (type.equals("ssh-dss")) {
            BigInteger p = decodeBigInt();
            BigInteger q = decodeBigInt();
            BigInteger g = decodeBigInt();
            BigInteger y = decodeBigInt();
            DSAPublicKeySpec spec = new DSAPublicKeySpec(y, p, q, g);
            //return KeyFactory.getInstance("DSA", new BouncyCastleProvider()).generatePublic(spec);
            return KeyFactory.getInstance("DSA").generatePublic(spec);
            //return new org.bouncycastle.jce.provider.JDKDSAPublicKey(KeyFactory.getInstance("DSA").generatePublic(spec));
        } else {
            throw new IllegalArgumentException("unknown type " + type);
        }
    }

    private String decodeType() {
        int len = decodeInt();
        String type = new String(bytes, pos, len);
        pos += len;
        return type;
    }

    private int decodeInt() {
        return ((bytes[pos++] & 0xFF) << 24) | ((bytes[pos++] & 0xFF) << 16)
                | ((bytes[pos++] & 0xFF) << 8) | (bytes[pos++] & 0xFF);
    }

    private BigInteger decodeBigInt() {
        int len = decodeInt();
        byte[] bigIntBytes = new byte[len];
        System.arraycopy(bytes, pos, bigIntBytes, 0, len);
        pos += len;
        return new BigInteger(bigIntBytes);
    }

    public static void main(String[] args) throws Exception {
        AuthorizedKeysDecoder decoder = new AuthorizedKeysDecoder();
        File file = new File("authorized_keys");
        Scanner scanner = new Scanner(file).useDelimiter("\n");
        while (scanner.hasNext()) {
            System.out.println(decoder.decodePublicKey(scanner.next()));
        }
        scanner.close();
    }

}
