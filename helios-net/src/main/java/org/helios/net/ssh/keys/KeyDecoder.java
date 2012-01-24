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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;

import ch.ethz.ssh2.crypto.Base64;
/**
 * <p>Title: KeyDecoder</p>
 * <p>Description: Utility class to decode public keys. Instances are not thread safe.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @author WhiteFang34 http://stackoverflow.com/questions/3531506/using-public-key-from-authorized-keys-with-java-security
 * <p><code>org.helios.net.ssh.keys.KeyDecoder</code></p>
 */
public class KeyDecoder {
    /** The byte stream to process */
    byte[] bytes = null;
    /** The parser position */
    int pos = 0;
    
    /** A null user value */
    public static final String[] NULL_USER = new String[]{null, null};
    
    private KeyDecoder() {
    	
    }
    
    /**
     * Creates a new KeyDecoder
     * @return a new KeyDecoder
     */
    public static KeyDecoder getInstance() {
    	return new KeyDecoder();
    }
    
    /**
     * Decodes a public key from an input stream
     * @param publicKeyFile The file to decode
     * @return a PublicKey
     * @throws Exception
     */
    public UserAwarePublicKey decodePublicKey(InputStream publicKeyBytes) throws Exception {
    	if(publicKeyBytes==null) throw new IllegalArgumentException("The passed input stream was null", new Throwable());
    	StringBuilder b = new StringBuilder((int)publicKeyBytes.available());
    	try {    		
    		BufferedReader bfr = new BufferedReader(new InputStreamReader(publicKeyBytes));
    		String line = null;
    		while((line = bfr.readLine())!=null) {
    			b.append(line);
    		}
    		return decodePublicKey(b.toString());
    	} finally {
    		try { publicKeyBytes.close(); } catch (Exception e) {}
    	}    	
    }
    
    /**
     * Decodes a public key from a URL
     * @param publicKeyURL The URL to decode
     * @return a PublicKey
     * @throws Exception
     */
    public UserAwarePublicKey decodePublicKey(URL publicKeyURL) throws Exception {
    	if(publicKeyURL==null) throw new IllegalArgumentException("The passed URL was null", new Throwable());
    	return decodePublicKey(publicKeyURL.openStream());
    }
    
    /**
     * Decodes a public key from a Reader
     * @param publicKeyReader The reader to decode
     * @return a PublicKey
     * @throws Exception
     */
    public UserAwarePublicKey decodePublicKey(Reader publicKeyReader) throws Exception {
    	if(publicKeyReader==null) throw new IllegalArgumentException("The passed Reader was null", new Throwable());
    	try {
    		BufferedReader br = new BufferedReader(publicKeyReader);
    		StringBuilder b = new StringBuilder();
    		String line = null;
    		while((line=br.readLine())!=null) {
    			b.append(line);
    		}
    		return decodePublicKey(b.toString());
    	} finally {
    		try { publicKeyReader.close(); } catch (Exception e) {}
    	}
    }
    
    
    /**
     * Decodes a public key from a file
     * @param publicKeyFile The file to decode
     * @return a PublicKey
     * @throws Exception
     */
    public UserAwarePublicKey decodePublicKey(File publicKeyFile) throws Exception {
    	if(publicKeyFile==null) throw new IllegalArgumentException("The passed file was null", new Throwable());
    	if(!publicKeyFile.canRead()) throw new IllegalArgumentException("The passed file [" + publicKeyFile + "] cannot be read", new Throwable());
    	return decodePublicKey(new FileInputStream(publicKeyFile));
    }

    /**
     * Decodes a public key in the form of a string
     * @param keyLine The key content
     * @return a PublicKey
     * @throws Exception
     */
    public UserAwarePublicKey decodePublicKey(String keyLine) throws Exception {
        bytes = null;
        pos = 0;
        for (String part : keyLine.split(" ")) {
            if (part.startsWith("AAAA")) {
                bytes = Base64.decode(part.toCharArray());
                break;
            }
        }
        if (bytes == null) {
            throw new IllegalArgumentException("no Base64 part to decode");
        }

        String type = decodeType();
        String[] user = decodeUser(keyLine);
        if (type.equals("ssh-rsa")) {
            BigInteger e = decodeBigInt();
            BigInteger m = decodeBigInt();
            RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
            return new UserAwarePublicKey(KeyFactory.getInstance("RSA").generatePublic(spec), user[0], user[1]);
        } else if (type.equals("ssh-dss")) {
            BigInteger p = decodeBigInt();
            BigInteger q = decodeBigInt();
            BigInteger g = decodeBigInt();
            BigInteger y = decodeBigInt();
            DSAPublicKeySpec spec = new DSAPublicKeySpec(y, p, q, g);
            return new UserAwarePublicKey(KeyFactory.getInstance("DSA").generatePublic(spec), user[0], user[1]);
        } else {
            throw new IllegalArgumentException("unknown type " + type);
        }
    }

    /**
     * Extracts the user and domain from the key line
     * @param line The key line
     * @return a string array with 0=user name and 1=user domain
     */
    private String[] decodeUser(String line) {
    	String name = line.split("== ")[1];
    	try {
    		if(name.trim().isEmpty() || !name.contains("@")) return NULL_USER;
    		return name.trim().split("@");
    	} catch (Exception e) {
    		return NULL_USER;
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



}
