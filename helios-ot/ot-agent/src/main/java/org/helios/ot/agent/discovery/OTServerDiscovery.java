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
package org.helios.ot.agent.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
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
import org.helios.ot.agent.Configuration;
import org.helios.time.SystemClock;

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
 * <p><code>org.helios.ot.agent.discovery.OTServerDiscovery</code></p>
 */
public class OTServerDiscovery  {
	/** Static class logger */
	protected static final Logger log = Logger.getLogger(OTServerDiscovery.class);
	/** Discovery thread serial */
	protected static final AtomicInteger serial = new AtomicInteger(0);
	/** A NetworkInterface that was discovered to work with multicast after the original join failed */
	protected static NetworkInterface goodNic = null;
	
	/** A map of known NICs that are up and support multicast */
	protected final static Map<String, NetworkInterface> knownNics = InetAddressHelper.getUpMCNICMap();
	/** An array of established multicast sockets */
	protected static volatile MulticastSocket[] multicastSockets = null; 
	
	/** A thread local containing the CountDownLatch that a thread will wait on */
	protected static final ThreadLocal<CountDownLatch> threadLatch = new ThreadLocal<CountDownLatch>();
	/**
	 * Refreshes the map of known NICs that are up and support multicast
	 */
	public static void refreshNics() {
		knownNics.clear();
		knownNics.putAll(InetAddressHelper.getUpMCNICMap());
	}
	
	/**
	 * Kills the multicast socket cache
	 */
	public synchronized static void resetMulticastSockets() {
		if(multicastSockets!=null) {
			for(MulticastSocket msock: multicastSockets) {
				try { msock.close(); } catch (Exception e) {}
			}						
		}
		multicastSockets = null;
	}
	
	/**
	 * Issues an InfoDump command over the multicast network and prints the response.
	 * @param format The format of the output (<b><code>TXT</code></b> or <b><code>XML</code></b>)
	 * @return The info dump output which may be null if the request times out.
	 */
	public static String info(String format) {
		threadLatch.set(new CountDownLatch(1));
		return rt("INFO|udp://%s:%s" + (format==null ? "" : "|" + format));
	}
	
	/**
	 * Issues a ping against the multicast network
	 * @param the name of the NIC that sent the request
	 * @return the name of the NIC that sent the request
	 */
	public static String ping(String nic) {
		threadLatch.set(new CountDownLatch(1));
		return rt("PING|udp://%s:%s|" + (nic==null ? "null" : nic));
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
		threadLatch.set(new CountDownLatch(1));
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
		try {
			// the designated amount of time to wait for a response after each iteration
			final int dsTimeout = Configuration.getDiscoveryTimeout();
			// the maximum number of attempts to execute discovery before it is abandoned
			final int dsMaxAttempts = Configuration.getDiscoveryMaxAttempts();
			// When a discovery request succeeds, the connection URI will be writen here.
			final AtomicReference<byte[]> responseRef = new AtomicReference<byte[]>(); 
			final CountDownLatch completionLatch = threadLatch.get();
			if(completionLatch==null || completionLatch.getCount()<1) {
				throw new RuntimeException("Completion Latch was null or had a < 1 count. Programmer Error", new Throwable());
			}
			UDPDiscoveryListener responseListener = new UDPDiscoveryListener(completionLatch, responseRef);
			int responseListeningPort = -1;
			try {
				responseListeningPort = responseListener.start();
			} catch (Exception e) {
				log.warn("Failed to start response listener. Discovery failed.", e);
			}
			MulticastSocket[] msockets = getDiscoveryMSocks();
			String formattedCommand = String.format(command, InetAddressHelper.hostName(), responseListeningPort);
			if(log.isDebugEnabled()) log.debug("Sending Discovery Command [" + formattedCommand + "]");
			byte[] payload = formattedCommand.getBytes();
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
						if(log.isDebugEnabled()) log.debug("Executing Discovery Request #" + attempt +
								"\n\tNIC [" + msock.getNetworkInterface().getName() + "]" +
								"\n\tMulticast Interface [" + msock.getInterface() + "]" +
								"\n\tLocal Address[" + msock.getLocalSocketAddress() + "]" 							
						);
						DatagramPacket datagram = new DatagramPacket(payload, payload.length, group, multicastPort);					
						msock.send(datagram);
					}
					if(log.isDebugEnabled()) log.debug("Waiting for response on Discovery Request #" + attempt);
					try {
						if(completionLatch.await(dsTimeout, TimeUnit.MILLISECONDS)) {
							byte[] response = responseRef.get();
							if(response!=null) {
								if(log.isDebugEnabled()) log.debug("Discovery Request #" + attempt + " Succeeded");
								String message = new String(response);
								if(message.startsWith("***Error***")) {
									throw new RuntimeException("Discovery Service Command Error:" + message);
								} else {
									return message;
								}
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
				// if we still have no response here, discovery failed.
				return null;
			} catch (Exception e) {
				throw new RuntimeException("Discovery Failed for Unexpected Reason", e);
			} finally {
				try { responseListener.stop(); } catch (Exception e) {}
			
			}
		} finally {
			threadLatch.remove();
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
		if(multicastSockets!=null) {
			return multicastSockets;
		}
		Set<MulticastSocket> msockets = new HashSet<MulticastSocket>();
		
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
		for(Map.Entry<String, NetworkInterface> entry: knownNics.entrySet()) {
			NetworkInterface nic = entry.getValue();
			String name = entry.getKey();			
			if(nicPattern.matcher(name).matches()) {
				try {
					if(!nic.isUp() || !nic.supportsMulticast()) continue;
					MulticastSocket ms = new MulticastSocket(multicastPort);
					ms.setNetworkInterface(nic);
					//ms.joinGroup(group);
					msockets.add(ms);
				} catch (Exception e) {
					if(log.isDebugEnabled()) log.debug("Failed to create multicast socket on matching NIC [" + name + "]:" + e);
				}
			}
		}
		if(msockets.isEmpty()) {
			// No hits so try all nics
			for(Map.Entry<String, NetworkInterface> entry: knownNics.entrySet()) {
				NetworkInterface nic = entry.getValue();
				String name = entry.getKey();
				try {
					if(!nic.isUp() || !nic.supportsMulticast()) continue;
					MulticastSocket ms = new MulticastSocket(multicastPort);
					ms.setNetworkInterface(nic);
					ms.joinGroup(group);
					msockets.add(ms);
				} catch (Exception e) {
					if(log.isDebugEnabled()) log.debug("Failed to create multicast socket on matching NIC [" + name + "]:" + e);
				}
			}			
		}
		multicastSockets =  msockets.toArray(new MulticastSocket[msockets.size()]);
		return multicastSockets;
	}
	
	
	/**
	 * Tests the OTServerDiscovery component
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		log("Discovery Test");
		log(info());
		System.setProperty(Configuration.DISCOVERY_TRANSMIT_NIC, "eth12");
		System.setProperty("org.helios.connection.discovery.timeout", "500");
		for(int i = 0; i < 10000; i++) {
			if(ping("eth12")==null) {
				throw new RuntimeException("Info failed");
			}
		}
		System.setProperty(Configuration.PREF_IP4, "true");
		int loopCount = 1000;
		long totalTime = 0;
		
		log("Ping:" + ping("eth12"));
		for(int i = 0; i < loopCount; i++) {
			long start = System.currentTimeMillis();
			String response = ping("eth12");			
			if(response==null) throw new RuntimeException("Info call failed");
			totalTime += (System.currentTimeMillis()-start);
		}
		
		
		
		log("Total Time for IP V4:" + totalTime);
		totalTime = 0;
		System.setProperty(Configuration.PREF_IP4, "false");
		for(int i = 0; i < loopCount; i++) {
			long start = System.currentTimeMillis();
			String response = info();			
			if(response==null) throw new RuntimeException("Info call failed");
			totalTime += (System.currentTimeMillis()-start);
		}
		log("Total Time for no IP Pref:" + totalTime);
//		log(rt("INFO|udp://%s:%s"));
		//log(rt("INFO|udp://%s:%s|TEXT"));
//		while(true) {
//			log(discover());
//			log(discover("UDP"));
//			log(discover("TCP"));
//			info();
//			info("XML");
//		}
		//log(info());
//		for(NetworkInterface nic: InetAddressHelper.getUpMCNICMap().values()) {
//			String name = nic.getName();
//			if(name==null) continue;
//			System.setProperty(Configuration.DISCOVERY_TRANSMIT_NIC, name);
//			if(name.equals("eth3")) {
//				Logger.getRootLogger().setLevel(Level.DEBUG);
//			} else {
//				Logger.getRootLogger().setLevel(Level.INFO);
//			}
//			log("TESTING NIC:" + name);
//			String result = info();
//			if(result!=null) {
//				log("Discovery Succeeded on [" + name + "]");
//			} else {
//				log("Discovery Failed on [" + name + "]");
//			}
//		}
//		log(rt("DISCOVER|udp://%s:%s"));
//		log(rt("DISCOVER|udp://%s:%s|UDP"));
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	

}
