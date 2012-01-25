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
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * <p>Title: LocalConfig</p>
 * <p>Description: Reads config from user's home directory in <code>.helios-net.properties</code></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.LocalConfig</code></p>
 */
public class LocalConfig {
	
	/** The standard location of the local config file */
	public static final File LOCAL_CONFIG = new File(System.getProperty("user.home") + File.separator + ".helios-net.properties");
	
	/** The loaded properties, keyed by user name */
	private static final Map<String, Map<String, String>> props = new HashMap<String, Map<String, String>>(); 

	/** The props key for the user target SSH host */
	public static final String HOST = "host";
	/** The props key for the user target SSH port */
	public static final String PORT = "port";		
	/** The props key for the user password */
	public static final String PASSWORD = "password";
	/** The props key for the RSA private key passphrase */
	public static final String RSA_PASS_PHRASE = "rsa.passphrase";
	/** The props key for the DSA private key passphrase */
	public static final String DSA_PASS_PHRASE = "dsa.passphrase";
	/** The props key for the RSA private key */
	public static final String RSA_PK = "rsa.pk";
	/** The props key for the DSA private key */
	public static final String DSA_PK = "dsa.pk";
	/** The props key for the RSA public key */
	public static final String RSA_PUB = "rsa.pub";
	/** The props key for the DSA public key */
	public static final String DSA_PUB = "dsa.pub";
	
	/**
	 * Returns a keyed value for the passed user
	 * @param user The user name
	 * @param key The value key to retrieve
	 * @return The value or null if not found
	 */				
	public static String getValue(String user, String key) {
		if(props.isEmpty()) {
			load();
		}
		Map<String, String> subMap = props.get(user);
		if(subMap==null) return null;
		return subMap.get(key);
	}
	
	/**
	 * Returns the SSH password for the passed user
	 * @param user The user name
	 * @return The user's SSH password
	 */			
	public static String getPassword(String user) {
		return getValue(user, PASSWORD);
	}
	
	/**
	 * Returns the private RSA key passphrase for the passed user
	 * @param user The user name
	 * @return The user's private RSA key passphrase
	 */			
	public static String getRsaPassphrase(String user) {
		return getValue(user, RSA_PASS_PHRASE);
	}
	
	/**
	 * Returns the private DSA key passphrase for the passed user
	 * @param user The user name
	 * @return The user's private DSA key passphrase
	 */		
	public static String getDsaPassphrase(String user) {
		return getValue(user, DSA_PASS_PHRASE);
	}
	
	/**
	 * Returns the private RSA key for the passed user
	 * @param user The user name
	 * @return The user's private RSA key
	 */	
	public static char[] getRsaPk(String user) {
		String b64 = getValue(user, RSA_PK);
		return new String(Base64.decode(b64)).toCharArray();		
	}
	
	/**
	 * Returns the private DSA key for the passed user
	 * @param user The user name
	 * @return The user's private DSA key
	 */	
	public static char[] getDsaPk(String user) {
		String b64 = getValue(user, DSA_PK);
		return new String(Base64.decode(b64)).toCharArray();		
	}
	
	/**
	 * Returns the public RSA key for the passed user
	 * @param user The user name
	 * @return The user's public RSA key
	 */
	public static String getRsaPub(String user) {
		return getValue(user, RSA_PUB);				
	}
	
	/**
	 * Returns the public DSA key for the passed user
	 * @param user The user name
	 * @return The user's public DSA key
	 */
	public static String getDsaPub(String user) {
		return getValue(user, DSA_PUB);				
	}
	
	/**
	 * Returns the target SSH host for the passed user
	 * @param user The user name
	 * @return The user's target SSH host
	 */
	public static String getSSHHost(String user) {
		return getValue(user, HOST);				
	}
	
	/**
	 * Returns the target SSH port for the passed user
	 * @param user The user name
	 * @return The user's target SSH port
	 */
	public static int getSSHPort(String user) {
		return Integer.parseInt(getValue(user, PORT));				
	}
	
	/**
	 * Returns an array of the unique user names in the config
	 * @return an array of the unique user names in the config
	 */
	public static String[] getUsers() {
		return props.keySet().toArray(new String[0]);
	}
	
	/**
	 * Loads and returns the indexed local config in an unmodifiable map.
	 * @return the indexed local config in an unmodifiable map
	 */
	public synchronized static Map<String, Map<String, String>> load() {
		if(props.isEmpty()) {
			Properties p = loadProps();
			for(Enumeration<Object> penum = p.keys(); penum.hasMoreElements();) {
				String key = penum.nextElement().toString();
				String value = p.getProperty(key);
				String user = key.split("\\.")[0];
				String userKey = key.replace(user + "." , "");
				Map<String, String> subMap = props.get(user);
				if(subMap==null) {
					subMap = new HashMap<String, String>();
					props.put(user, subMap);					
				}
				subMap.put(userKey, value);
				//System.out.println("LocalConfig [" + user + "]:[" + userKey + "]:[" + value + "]");
				
			}
		}
		Map<String, Map<String, String>> tmpMap = new HashMap<String, Map<String, String>>();
		for(Map.Entry<String, Map<String, String>> entry: props.entrySet()) {
			tmpMap.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
		}
		return Collections.unmodifiableMap(tmpMap);
	}
	
	/**
	 * Resets the global props
	 */
	public synchronized void reset() {
		props.clear();
		load();
	}
	
	/**
	 * Loads the local config properties
	 * @return the loaded local config properties
	 */
	public static Properties loadProps() {
		if(!LOCAL_CONFIG.exists()) throw new RuntimeException("No file found at [" + LOCAL_CONFIG.getAbsolutePath() + "]");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(LOCAL_CONFIG);
			Properties p = new Properties();
			p.load(fis);
			return p;
		} catch (Exception e) {
			throw new RuntimeException("Failed to read file at [" + LOCAL_CONFIG.getAbsolutePath() + "]", e);
		} finally {
			if(fis!=null) try { fis.close(); } catch (Exception e) {}
		}
		
	}
	
	
}
/*
Prop Gen Helper Script:
=======================
import org.apache.commons.codec.binary.Base64;


// println new File("/home/nwhitehead/tunnel/id_dsa.pub").getText();
keyDir = "/home/nwhitehead/tunnel/";
user = "tunnel";
def bytes = null;
def s = null;

bytes = new File(keyDir + "id_rsa").getText().getBytes();
s = new String(Base64.encodeBase64(bytes));
println "${user}.rsa.pk=${s}";
bytes = new File(keyDir + "id_dsa").getText().getBytes();
s = new String(Base64.encodeBase64(bytes));
println "${user}.dsa.pk=${s}";

s = new File(keyDir + "id_rsa.pub").getText().replace("\n", "");
println "${user}.rsa.pub=${s}";

s = new File(keyDir + "id_dsa.pub").getText().replace("\n", "");
println "${user}.dsa.pub=${s}";


*/