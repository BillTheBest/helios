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
package org.helios.ot.agent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.helpers.InetAddressHelper;
import org.helios.ot.agent.discovery.UDPDiscoveryListener;

/**
 * <p>Title: OTServerDiscovery</p>
 * <p>Description: Multicast discovery agent for Helios OT Endpoint</p> 
 * <p>Company: Helios Development Group LLC</p>
 * <p>Discovery Process:<ol>
 * <li>Starts a UDP listener on <b><code>0.0.0.0:0</code></b>, so we're listening on all interfaces.</li>
 * <li>Once the listener is started, we capture the designated ephemeral port</li>
 * <li>Iterates all network interfaces supporting multicast, and all addresses for each interface.</li>
 * <li>For each, binds to the configured or default multicast network (discovery address, discovery port) and 
 * transmits a discovery request consisting of the port the discovery listener is listening on and the preferred protocol
 * that the client would like to establish a connection to the server with.</li>
 * <li>The OT Server discovery service will [hopefully] receive one of these discovery requests and will return a connection URI back to the client
 * (which will be the preferred requested if available).</li>
 * <li>The return is sent to the address where the packet was received from and the listening port specified in the received request.
 * Currently, discovery clients can only listen on UDP, so only UDP response transmits are implemented in the discovery service.</li>
 * <li>Steps #3 and #4 will be repeated the configured or default number of times, waiting the configured or default period of time for a response between each attempt.</li>
 * </ol></p>
 * <p>An InfoDump response in (<b><code>TXT</code></b>) format looks something like this:<pre>
************************
Helios Open Trace Server
************************

Version:Crazy Unstable Dev Build 0.00001
Spring Version:3.1.0.RELEASE
Deployed Bean Count:113
JVM:Sun Microsystems Inc.Java HotSpot(TM) 64-Bit Server VM 20.5-b03
Java Runtime Version:1.6.0_30-b12
PID:147032
Host:NE-WK-NWHI-01
Start Time:Wed Apr 04 12:00:42 EDT 2012
Up Time:250 minutes
Helios OT Remote Endpoints
	tcp://0.0.0.0:9428
	udp://0.0.0.0:9427
WebApp Endpoints
	http://localhost:8161/helios
	http://localhost:8161/netty
	http://localhost:8161/jmx
JMX Connector URLs
	service:jmx:rmi://localhost:8002/jndi/rmi://localhost:8005/jmxrmi
************************
 * </pre><p>
 * <p>An InfoDump response in (<b><code>XML</code></b> format looks something like this:<pre>
&lt;HeliosOpenTraceServer&gt;
	&lt;Version&gt;Crazy Unstable Dev Build 0.00001&lt;/Version&gt;
	&lt;SpringVersion&gt;3.1.0.RELEASE&lt;/SpringVersion&gt;
	&lt;DeployedBeanCount&gt;113&lt;/DeployedBeanCount&gt;
	&lt;JVM&gt;Sun Microsystems Inc.Java HotSpot(TM) 64-Bit Server VM 20.5-b03&lt;/JVM&gt;
	&lt;PID&gt;263736&lt;/PID&gt;
	&lt;Host&gt;NE-WK-NWHI-01&lt;/Host&gt;
	&lt;StartTime&gt;Wed Apr 04 09:26:22 EDT 2012&lt;/StartTime&gt;
	&lt;UpTime&gt;7 minutes&lt;/UpTime&gt;
	&lt;HeliosOTRemoteEndpoints&gt;
		&lt;tcp&gt;tcp://0.0.0.0:9428&lt;/tcp&gt;
		&lt;udp&gt;udp://0.0.0.0:9427&lt;/udp&gt;
	&lt;/HeliosOTRemoteEndpoints&gt;
	&lt;WebAppEndpoints&gt;
		&lt;HeliosWebConsole&gt;http://localhost:8161/helios&lt;/HeliosWebConsole&gt;
		&lt;TheNettyHTTPEndpoint&gt;http://localhost:8161/netty&lt;/TheNettyHTTPEndpoint&gt;
		&lt;JSONJMXAgent&gt;http://localhost:8161/jmx&lt;/JSONJMXAgent&gt;
	&lt;/WebAppEndpoints&gt;
	&lt;JMXConnectorURLs&gt;
		&lt;rmi&gt;service:jmx:rmi://localhost:8002/jndi/rmi://localhost:8005/jmxrmi&lt;/rmi&gt;
	&lt;/JMXConnectorURLs&gt;
&lt;/HeliosOpenTraceServer&gt;
 * </pre></p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.OTServerDiscovery</code></p>
 */
public class OTServerDiscovery  {
	/** Static class logger */
	protected static final Logger log = Logger.getLogger(OTServerDiscovery.class);
	/** Discovery thread serial */
	protected static final AtomicInteger serial = new AtomicInteger(0);
	/** A NetworkInterface that was discovered to work with multicast after the original join failed */
	protected static NetworkInterface goodNic = null;
	
	/**
	 * Issues an InfoDump command over the multicast network and prints the response.
	 * @param format The format of the output (<b><code>TXT</code></b> or <b><code>XML</code></b>)
	 * @return The info dump output which may be null if the request times out.
	 */
	public static String info(String format) {
		return rt("INFO|udp://%s:%s" + (format==null ? "" : "|" + format));
	}
	
	/**
	 * Issues an InfoDump command over the multicast network and prints the response in <b><code>TXT</code></b> format.
	 * @return The info dump output which may be null if the request times out.
	 */
	public static String info() {
		return info(null);
	}
	
	/**
	 * Issues a Discover request over the multicast network and returns the URI of the endpoint to connect to
	 * @param protocol The preferred protocol to connect to
	 * @return the URI of the endpoint to connect to which will be of the preferred protocol if available and may be null of no server answered.
	 */
	public static String discover(String protocol) {
		return rt("DISCOVER|udp://%s:%s"  + (protocol==null ? "" : "|" + protocol));
	}
	
	/**
	 * Issues a Discover request over the multicast network and returns the URI of the endpoint to connect to
	 * @return the URI of the endpoint to connect to which will be of the first protocol located and may be null of no server answered.
	 */
	public static String discover() {
		return discover(null);
	}
	
	
	
	/**
	 * Executes an OT Server discovery service call
	 * @return the OT Server Discovery Service supplied response or null if there was no response.
	 */
	protected static String rt(String command) {
		// the designated amount of time to wait for a response after each iteration
		final int dsTimeout = Configuration.getDiscoveryTimeout();
		// the maximum number of attempts to execute discovery before it is abandoned
		final int dsMaxAttempts = Configuration.getDiscoveryMaxAttempts();
		// When a discovery request succeeds, the connection URI will be writen here.
		final AtomicReference<byte[]> responseRef = new AtomicReference<byte[]>(); 
		final CountDownLatch completionLatch = new CountDownLatch(1);
		MulticastSocket ms =  null;
		UDPDiscoveryListener responseListener = new UDPDiscoveryListener(completionLatch, responseRef);
		MulticastSocket[] msockets = getDiscoveryMSocks();
		byte[] payload = command.getBytes();
		String multicastGroup = Configuration.getDiscoveryNetwork();
		int multicastPort = Configuration.getDiscoveryPort();
		InetAddress group = null;
		try {
			group = InetAddress.getByName(multicastGroup);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create InetAddress for configured multicast group [" + multicastGroup + "]", e);
		}
		
		if(msockets.length==0) {
			log.warn("Failed to bind any multicast sockets for discovery. Result will be null.");
			return null;
		}
		try {
			for(int attempt = 0; attempt < dsMaxAttempts; attempt++) {
				
				for(MulticastSocket msock: msockets) {
					if(log.isDebugEnabled()) log.debug("Executing Discovery Request #" + attempt + "\n\tRemote Address is [" + msock.getInetAddress() + ":" + msock.getPort() + "]");
					DatagramPacket datagram = new DatagramPacket(payload, payload.length, group, multicastPort);					
					msock.send(datagram);
				}
				if(log.isDebugEnabled()) log.debug("Waiting for response on Discovery Request #" + attempt);
				try {
					if(completionLatch.await(dsTimeout, TimeUnit.MILLISECONDS)) {
						byte[] response = responseRef.get();
						if(response!=null) {
							if(log.isDebugEnabled()) log.debug("Discovery Request #" + attempt + " Succeeded");
							return new String(response);
						} else {
							// if the response is null, we have to return null since the latch has been dropped.
							// we should fix this so the response can simply be ignored and we can retry for a new one
							log.warn("Discovery Request #" + attempt + " Succeeded but response was null");
							return null;
						}
					}
				} catch (InterruptedException iex) {
					log.warn("Thread was interrupted while waiting for a response. Discovery terminating.");
					return null;
				}
			}
			// if we still have no response here, discovery failed.s
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Discovery Failed for Unexpected Reason", e);
		} finally {
			try { responseListener.stop(); } catch (Exception e) {}
			for(MulticastSocket msock: msockets) {
				try { msock.close(); } catch (Exception e) {}
			}			
		
		}
	}
	
	
	/**
	 * Attempts to acquire a connection to the multicast network specified by 
	 * the configured/default discovery network and port.
	 * Deprecated but keeping it around 'cause it could be useful for debugging 
	 * @return a connected multicast socket
	 * @throws IOException
	 * 
	 */
	@Deprecated()
	protected static MulticastSocket connectMulticast() throws IOException {
		InetAddress group = InetAddress.getByName(Configuration.getDiscoveryNetwork());
		MulticastSocket ms = new MulticastSocket(Configuration.getDiscoveryPort());
		boolean joined = false;
		try {
			if(goodNic!=null) {
				ms.setNetworkInterface(goodNic);
			}
			ms.joinGroup(group);
			joined = true;
		} catch (Exception e) {
			if(e.getMessage().contains("No such device")) {
				log.warn("\n\tFailed to bind to multicast group because of a \"No such device\" error."
						+ "\n\tYou are probably not connected to a network or do not have a link."
						+ "\n\tWill attempt overriding the network interface...."
				);
				for(Enumeration<NetworkInterface> nifaces = NetworkInterface.getNetworkInterfaces(); nifaces.hasMoreElements();) {
					NetworkInterface niface = nifaces.nextElement();
					try {						
						ms.setNetworkInterface(niface);
						ms.joinGroup(group);
						joined = true;
						log.info("SUCCESS! Joined Multicast Group [" + group + "] with Network Interface [" + niface + "]");
						goodNic = niface;
						break;
					} catch (Exception ex) {
						log.warn("Failed with network interface override [" + niface + "]");
					}
				}
				if(!joined) {
					log.error("Failed to join multicast group [" + group + "] with any NetworkInterface");
					throw new IllegalStateException("Failed to join multicast group [" + group + "] with any NetworkInterface");
				}
			}
		}					
		return ms;
	}
	
	/**
	 * Returns an array of multicast sockets to use in transmitting the discovery request
	 * @return an array of multicast sockets 
	 */
	protected static MulticastSocket[] getDiscoveryMSocks() {
		Set<MulticastSocket> msockets = new HashSet<MulticastSocket>();
		Map<String, NetworkInterface> nics = InetAddressHelper.getNICMap();
		Pattern nicPattern = Configuration.getDiscoveryTransmitNic();
		String multicastGroup = Configuration.getDiscoveryNetwork();
		int multicastPort = Configuration.getDiscoveryPort();
		log.info("Preparing Multicast Transmission Sockets for [" + multicastGroup + ":" + multicastPort + "]");
		InetAddress group = null;
		try {
			group = InetAddress.getByName(multicastGroup);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create InetAddress for configured multicast group [" + multicastGroup + "]", e);
		}
		// First get matches only, but if no interface matches, 
		// or matching interfaces cannot join the group, iterate all the nics.
		for(Map.Entry<String, NetworkInterface> nic: nics.entrySet()) {
			if(nicPattern.matcher(nic.getKey()).matches()) {
				try {
					MulticastSocket ms = new MulticastSocket(multicastPort);
					ms.setNetworkInterface(nic.getValue());
					ms.joinGroup(group);
					msockets.add(ms);
				} catch (Exception e) {
					if(log.isDebugEnabled()) log.debug("Failed to create multicast socket on matching NIC [" + nic.getKey() + "]", e);
				}
			}
		}
		if(msockets.isEmpty()) {
			// No hits so try all nics
			for(Map.Entry<String, NetworkInterface> nic: nics.entrySet()) {
				try {
					MulticastSocket ms = new MulticastSocket(multicastPort);
					ms.setNetworkInterface(nic.getValue());
					ms.joinGroup(group);
					msockets.add(ms);
				} catch (Exception e) {
					if(log.isDebugEnabled()) log.debug("Failed to create multicast socket on matching NIC [" + nic.getKey() + "]", e);
				}
			}			
		}
		return msockets.toArray(new MulticastSocket[msockets.size()]);
	}
	
	
	/**
	 * Tests the OTServerDiscovery component
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		log("Discovery Test");
//		log(rt("INFO|udp://%s:%s"));
		//log(rt("INFO|udp://%s:%s|TEXT"));
		while(true) {
//			log(discover());
//			log(discover("UDP"));
//			log(discover("TCP"));
			info();
			info("XML");
		}
//		log(rt("DISCOVER|udp://%s:%s"));
//		log(rt("DISCOVER|udp://%s:%s|UDP"));
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	

}
