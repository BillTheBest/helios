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
package org.helios.helpers;

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Title: InetAddressHelper</p>
 * <p>Description: A class of static helper methods for net related operations and settings.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.helpers.InetAddressHelper</code></p>
 */
public class InetAddressHelper {
	private InetAddressHelper() {};
	/** The pattern for an IPV4 Octet */
	public static final String OCTET = "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.";
	/** Pattern to match an IP V4 address */
	public static final Pattern IPV4_ADDRESS_PATTERN = Pattern.compile("^" + OCTET + OCTET + OCTET + OCTET + "?");
	/** Pattern to match an IP V6 address */
	public static final Pattern IPV6_ADDRESS_PATTERN = Pattern.compile("");
	
	/** The timeout on host reachability tests (ms.) */
	protected static int reachableTimeOut = 100;
	
	/**
	 * Returns a map of {@link NetworkInterface}s keyed by the interface name
	 * @return a map of {@link NetworkInterface}s keyed by the interface name
	 */
	public static Map<String, NetworkInterface> getNICMap() {
		Map<String, NetworkInterface> nics = new HashMap<String, NetworkInterface>();
		try { 
			for(Enumeration<NetworkInterface> nenum = NetworkInterface.getNetworkInterfaces(); nenum.hasMoreElements();) {
				NetworkInterface nic = nenum.nextElement();
				nics.put(nic.getName(), nic);
			}
			return nics;
		} catch (Exception e) {
			throw new RuntimeException("Failed to retrieve NIC Map", e);
		}
	}
	
	/**
	 * Returns a map of {@link NetworkInterface}s that are up keyed by the interface name
	 * @return a map of {@link NetworkInterface}s that are up keyed by the interface name
	 */
	public static Map<String, NetworkInterface> getUpNICMap() {
		Map<String, NetworkInterface> nics = new HashMap<String, NetworkInterface>();
		try { 
			for(Enumeration<NetworkInterface> nenum = NetworkInterface.getNetworkInterfaces(); nenum.hasMoreElements();) {
				NetworkInterface nic = nenum.nextElement();
				if(!nic.isUp()) continue;
				nics.put(nic.getName(), nic);
			}
			return nics;
		} catch (Exception e) {
			throw new RuntimeException("Failed to retrieve NIC Map", e);
		}
	}
	
	/**
	 * Returns a map of {@link NetworkInterface}s that are up and support multicast keyed by the interface name
	 * @return a map of {@link NetworkInterface}s that are up and support multicast keyed by the interface name
	 */
	public static Map<String, NetworkInterface> getUpMCNICMap() {
		Map<String, NetworkInterface> nics = new HashMap<String, NetworkInterface>();
		try { 
			for(Enumeration<NetworkInterface> nenum = NetworkInterface.getNetworkInterfaces(); nenum.hasMoreElements();) {
				NetworkInterface nic = nenum.nextElement();
				if(!nic.isUp() || !nic.supportsMulticast()) continue;
				nics.put(nic.getName(), nic);
			}
			return nics;
		} catch (Exception e) {
			throw new RuntimeException("Failed to retrieve NIC Map", e);
		}
	}
	
	
	
	/**
	 * Determines the best name or IP address to use as the default binding address
	 * @return the current host name or ip address.
	 */
	public static synchronized String hostName() {
		String rmiName = System.getProperty("java.rmi.server.hostname");
		if(rmiName!=null) {
			return rmiName;
		}
		
		
		
		try {
			for(Enumeration<NetworkInterface> nenum = NetworkInterface.getNetworkInterfaces(); nenum.hasMoreElements();) {
				NetworkInterface nic = nenum.nextElement();
				if(!nic.isLoopback() && nic.isUp()) {
					for(InterfaceAddress addr: nic.getInterfaceAddresses()) {
						InetAddress inetAddr = addr.getAddress();
						if(inetAddr.isSiteLocalAddress()) {
							if(inetAddr.getCanonicalHostName().equals(InetAddress.getLocalHost().getCanonicalHostName())) {
								String hostName = inetAddr.getCanonicalHostName();
								System.setProperty("java.rmi.server.hostname", hostName);
								return hostName;
							}
						}						
					}
				}
			}
		} catch (Exception e) {}
		String osName = System.getProperty("os.name").toLowerCase();
		String hn = null;
		if(osName.contains("windows")) {
				hn = System.getenv("COMPUTERNAME");
				if(hn!=null) return hn;
		} else if(osName.contains("linux") || osName.contains("unix")) {
				hn = System.getenv("HOSTNAME");
				if(hn!=null) return hn;
		}
		return ManagementFactory.getRuntimeMXBean().getName().split("@")[1];
	}

	
	
	/**
	 * Determines if the passed string is a V4 IP address
	 * @param address The string to test
	 * @return true if the passed string matches a V4 IP address pattern, false if not.
	 */
	public static boolean isV4Address(String address) {
		if(address==null || address.trim().length() < 8) return false;
		return IPV4_ADDRESS_PATTERN.matcher(address.trim()).matches();
	}
	
	/**
	 * Determines if the passed string is a V6 IP address
	 * @param address The string to test
	 * @return true if the passed string matches a V6 IP address pattern, false if not.
	 */
	public static boolean isV6Address(String address) {
		if(address==null || address.trim().length() < 3) return false;
		return IPV6_ADDRESS_PATTERN.matcher(address.trim()).matches();
	}
	
	/**
	 * Determines if the passed string is a V4 or V6 IP address
	 * @param address The string to test
	 * @return true if the passed string matches a V4 or V6 IP address pattern, false if not.
	 */
	public static boolean isIPAddress(String address) {
		if(address==null || address.trim().length() < 8) return false;
		return isV4Address(address) || isV6Address(address); 
	}
	
	/**
	 * Determines if the passed string is a V4 or V6 IP address
	 * @param address The string to test
	 * @return 0 if the string is not an address, 1 if the string is a V4 address, 2 if the string is a V6 address 
	 */
	public static int testIPAddress(String address) {
		if(address==null || address.trim().length() < 8) return 0;
		if(isV4Address(address)) return 1;
		else if(isV6Address(address)) return 2;
		else return 0;
	}
	
	
	// ipv4 only
	// timeout on isReachable
	
	/**
	 * Returns a set of unique derivable aliases for the passed address with no extendeds.
	 * These will be the following: <ul>
	 * <li>The address canonical host name</li>
	 * <li>The address host name</li>
	 * <li>The address host address</li>
	 * </ul>
	 * @param address The address to derive aliases for.
	 * @return A set of unique aliases for the given address.
	 */
	public Set<String> getAddressAliases(InetAddress address) {
		return getAddressAliases(address, false);
	}
	
	/**
	 * Returns a set of unique derivable aliases for the passed host name with no extendeds and including itself.
	 * These will be the following: <ul>
	 * <li>The address canonical host name</li>
	 * <li>The address host name</li>
	 * <li>The address host address</li>
	 * </ul>
	 * @param name The host name to derive aliases for.
	 * @return A set of unique aliases for the given address.
	 */
	public Set<String> getAddressAliases(String name) {
		return getAddressAliases(name, false);
	}
	
	/**
	 * Returns a set of unique derivable aliases for the passed address with some extra derived names based on
	 * parsing the short name from the host name and canonical name.
	 * These will be the following: <ul>
	 * <li>The address canonical host name</li>
	 * <li>The address host name</li>
	 * <li>The address host address</li>
	 * <li>The first dot portion of the canonical host name</li>
	 * <li>The first dot portion of the host name</li>
	 * </ul>
	 * @param address The address to derive aliases for.
	 * @param extended Looks for extended aliases. May have a longer elapsed time.
	 * @return A set of unique aliases for the given address.
	 */
	public static Set<String> getAddressAliases(InetAddress address, boolean extended) {
		Set<String> aliases = new HashSet<String>();
		String canonicalName = address.getCanonicalHostName();
		String name = address.getHostName();
		aliases.add(canonicalName);
		aliases.add(address.getHostAddress());
		aliases.add(name);
		if(extended) {
			if(address.isLoopbackAddress()) {
				aliases.add("localhost");
				aliases.addAll(getSimpleAliases("localhost"));
				aliases.addAll(getLocalAliases(true));
			}			
			allByName(name, address, aliases);
			allByName(canonicalName, address, aliases);
			String shortName = shortName(canonicalName, address, aliases);
			allByName(shortName, address, aliases);
			shortName = shortName(name, address, aliases);
			allByName(shortName, address, aliases);			
		}
		return aliases;
	}

	/**
	 * Returns a set of unique derivable aliases for the passed host name with some extra derived names based on
	 * parsing the short name from the host name and canonical name. The returned set will include the name passed in.
	 * These will be the following: <ul>
	 * <li>The address canonical host name</li>
	 * <li>The address host name</li>
	 * <li>The address host address</li>
	 * <li>The first dot portion of the canonical host name</li>
	 * <li>The first dot portion of the host name</li>
	 * </ul>
	 * @param name The host name to derive aliases for.
	 * @param extended Looks for extended aliases. May have a longer elapsed time.
	 * @return A set of unique aliases for the given address.
	 */	
	public static Set<String> getAddressAliases(String name, boolean extended) {
		Set<String> aliases = new HashSet<String>();
		try {
			aliases.addAll(getAddressAliases(InetAddress.getByName(name), extended));
			//aliases.remove(name);
			return aliases;
		} catch (Exception e) {
			return aliases;
		}		
	}
	
	/**
	 * Returns a set of the canonical host name, host name and address  (string) for the passed address.
	 * @param address The address to get the names of.
	 * @return A set of names.
	 */
	public static Set<String> getSimpleAliases(InetAddress address) {
		
		Set<String> aliases = new HashSet<String>();
		try {
			if(address.isReachable(reachableTimeOut)) {
				String canonicalName = address.getCanonicalHostName();
				String name = address.getHostName();
				aliases.add(canonicalName);
				aliases.add(address.getHostAddress());
				aliases.add(name);
			}
		} catch (Exception e) {}
		return aliases;		
	}
	
	/**
	 * Returns a set of all the simple aliases for the addresses bound to all local interfaces.
	 * @return A set of alias names for all local network interfaces.
	 */
	public static Set<String> getLocalAliases(boolean ip4Only) {
		Set<String> aliases = new HashSet<String>();
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces() ;
			while(ifaces.hasMoreElements()) {
				 Enumeration<InetAddress> addrs = ifaces.nextElement().getInetAddresses();
				 while(addrs.hasMoreElements()) {
					 InetAddress addr = addrs.nextElement();
					 if(ip4Only && addr instanceof Inet4Address) {
						 if(addr.isReachable(reachableTimeOut)) {
							 aliases.addAll(getSimpleAliases(addr));
						 }
					 }
				 }
			}
		} catch (Exception e) {}		
		return aliases;
	}
	
	/**
	 * Returns a set of the canonical host name, host name and address  (string) for the passed host name. Includes the passed name.
	 * @param name The host name.
	 * @return A set of names.
	 */
	public static Set<String> getSimpleAliases(String name) {
		Set<String> aliases = new HashSet<String>();
		try {
			aliases.addAll(getSimpleAliases(InetAddress.getByName(name)));
			return aliases;
		} catch (Exception e) {
			return aliases;
		}
	}
	
	
	/**
	 * Examines the fullName passed and if it is a compound name (contains dots), will extract and validate the first dot portion and add to the passed alias if it resolves to the same originalAddress.
	 * @param fullName The full name to parse.
	 * @param originalAddress The original address that the shortName must resolve to in order to match.
	 * @param aliases The aliases set the validated shortName will be added to.
	 * @return The alias found or null if one was not found or was a duplicate.
	 */
	protected static String  shortName(String fullName, InetAddress originalAddress, Set<String> aliases) {
		if(fullName.contains(".")) {
			String tName = fullName.split("\\.")[0];
			if(!aliases.contains(tName)) {
				try {
					if(isSame(tName, originalAddress)) {
						aliases.add(tName);
						return tName;
					}
				} catch (Exception e) {}
			}
		}
		return null;
	}
	
	/**
	 * Determines if the passed name resolves to the same address as the original address.
	 * @param name The name to test/
	 * @param originalAddress The original address that the name must resolve to.
	 * @return true if they match.
	 */
	protected static boolean isSame(String name, InetAddress originalAddress) {
		try {
			return InetAddress.getByName(name).equals(originalAddress);
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * @param name
	 * @param originalAddress
	 * @param aliases
	 * @return
	 */
	protected static int allByName(String name, InetAddress originalAddress, Set<String> aliases) {
		if(name==null) return 0;
		int cnt = 0;
		InetAddress[] addrs = null;
		try {
			addrs = InetAddress.getAllByName(name);
		} catch (Exception e) {
			return 0;
		}
		for(InetAddress addr: addrs) {
			try {
				if(addr.isReachable(reachableTimeOut)) {
					if(addr.equals(originalAddress)) {
						for(String s: getSimpleAliases(addr)) {
							if(!aliases.contains(s)) {
								aliases.add(s);
							}
						}
					}
				}
			} catch (Exception e) {}
		}
		return cnt;
	}

	public static void main(String[] args) {
		log("NetHelper Test");
		String name = "heliojira.dnsalias.org";
		//String name = "hengine";
		log("Acquiring Aliases for [" + name + "]");
		log("\n" + formatSet(getAddressAliases(name, false)));
		
		log("Acquiring Extended Aliases for [" + name + "]");
		log("\n" + formatSet(getAddressAliases(name, true)));
		log("HostName:" + hostName());
		
	}
	
	public static String formatSet(Set<String> aliases) {
		StringBuilder b = new StringBuilder();
		for(String s: aliases) {
			b.append(s).append("\n");
		}
		return b.toString();
	}
	
	public static void log(Object message) {
		System.out.println(message);
	}



}


