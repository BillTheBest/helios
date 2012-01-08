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
package org.helios.nativex.jmx.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helios.helpers.CollectionHelper;

/**
 * <p>Title: ConnectionType</p>
 * <p>Description: Enumerates the different connection type flags</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.net.ConnectionType</code></p>
 */

public enum ConnectionType {
	CLIENT(1),
	SERVER(2),
	TCP(16),
	UDP(32),
	RAW(64),
	UNIX(128);
	
	public static final int ENUMCOUNT = ConnectionType.values().length;
	
	public static final Map<Integer, ConnectionType> CODE2ENUM = Collections.unmodifiableMap(new HashMap<Integer, ConnectionType>(CollectionHelper.createKeyedValueMap(Integer.class, ConnectionType.class,  
			new Integer(CLIENT.flag), CLIENT,
			new Integer(SERVER.flag), SERVER,
			new Integer(TCP.flag), TCP,
			new Integer(UDP.flag), UDP,
			new Integer(RAW.flag), RAW,
			new Integer(UNIX.flag), UNIX
	)));
	
	

	
	/**
	 * Creates a new ConnectionType
	 * @param flag The connection attribute flag
	 */
	private ConnectionType(int flag) {
		this.flag = flag;
	}
	
	/** The connection attribute flag */
	private final int flag;
	
	/**
	 * Returns the connection type for the passed flag
	 * @param flag The flag
	 * @return the connection type or null if one was not found
	 */
	public static ConnectionType forFlag(int flag) {
		return CODE2ENUM.get(flag);
	}
	
	/**
	 * Returns an array of the connection types enabled in the passed flags
	 * @param flags The flags
	 * @return a (possibly empty) array of connection types enabled in the passed flags
	 */
	public static ConnectionType[] enabledFlags(Number...flags) {
		if(flags==null || flags.length<1) return new ConnectionType[0];
		List<ConnectionType> types = new ArrayList<ConnectionType>(ENUMCOUNT);
		for(ConnectionType ct: ConnectionType.values()) {
			for(Number flag: flags) {
				if(flag==null) continue;
				try { // in case the Number cannot be cajoled to an int
					if((ct.flag & flag.intValue())!=0) {
						types.add(ct);
					}
				} catch (Exception e) {}
			}
		}
		return types.toArray(new ConnectionType[types.size()]);
	}
	
	/**
	 * Returns an array of the connection type names enabled in the passed flags
	 * @param flags The flags
	 * @return a (possibly empty) array of connection type names enabled in the passed flags
	 */
	public static String[] enabledFlagNames(Number...flags) {
		List<String> types = new ArrayList<String>(ENUMCOUNT);
		for(ConnectionType ct: enabledFlags(flags)) {
			types.add(ct.name());
		}
		return types.toArray(new String[types.size()]);
	}
	
	
	/**
	 * Determines if the passed flag has this connection type enabled
	 * @param flag the flag to test
	 * @return true if the passed flag is enabled for this connection type 
	 */
	public boolean isEnabled(Number flag) {
		if(flag==null) return false;
		try { // in case the Number cannot be cajoled to an int
			return ((this.flag & flag.intValue())!=0);
		} catch (Exception e) {
			return false;
		}		
	}
	
	/**
	 * Turns on this connection type's flag in the passed flag and returns the new mask.
	 * @param flag The flag to turn on tis type for
	 * @return the new flag
	 */
	public int setEnabled(Number flag) {
		if(flag==null) return 0;		
		try {			
			return (flag.intValue() | this.flag);  
		} catch (Exception e) {
			return 0;
		}
	}
	
	/**
	 * Creates a flag that represents a mask with the passed types enabled.
	 * @param types An array of connection types to enable in the returned flag
	 * @return a new flag
	 */
	public static int flagFor(ConnectionType...types) {
		int f = 0;
		if(types!=null && types.length>0) {
			for(ConnectionType ct: ConnectionType.values()) {
				if(ct==null) continue;
				f = f | ct.flag; 
			}
		}
		return f;
	}
	
	public static int flagForAll() {
		int f = 0;
			for(ConnectionType ct: ConnectionType.values()) {
				f = f | ct.flag; 
			}
		return f;
	}
	
	public static void main(String[] args) {
		System.out.println(flagForAll());
	}
	
}
