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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.helpers.InetAddressHelper;

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
		final AtomicReference<String> responseRef = new AtomicReference<String>(); 
		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch listeningLatch = new CountDownLatch(1);
		MulticastSocket ms =  null;
		
		// Start
		try {
			InetSocketAddress insock = new InetSocketAddress("0.0.0.0", 0); 
			final DatagramSocket ds = new DatagramSocket(insock);
			ds.setSoTimeout(dsTimeout);
			final byte[] buffer = new byte[1000];
			final DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
			Runnable runnable = new Runnable(){
				public void run() {
					try {
						listeningLatch.countDown();
						ds.receive(dp);
						log.info("Received Discovery Service Response");
						responseRef.set(new String(dp.getData()).trim());
					} catch (SocketTimeoutException  ste) {
						log.warn("Discovery Request Timed Out After [" + dsTimeout + "] ms.");
					} catch (Exception e) {
						log.warn("Unexpected Discovery Request Failure", e);
					} finally {
						try { ds.close(); } catch (Exception ex) {}
						latch.countDown();
					}
				}
			};
			Thread t = new Thread(runnable, "OTServerDiscoveryThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			// ==============================================
			ms =  new MulticastSocket(Configuration.getDiscoveryPort());
			InetAddress group = InetAddress.getByName(Configuration.getDiscoveryNetwork());
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
			//String fc = String.format(command, Configuration.getDiscoveryListenAddress(), ds.getLocalPort());
			//String fc = String.format(command, InetAddressHelper.hostName(), ds.getLocalPort());
			String fc = String.format(command, InetAddress.getByName(InetAddressHelper.hostName()).getHostAddress(), ds.getLocalPort());
			log.info("Discovery Request [" + fc + "]");
			byte[] buff = fc.getBytes();	
			t.start();
			listeningLatch.await();
			ms.send(new DatagramPacket(buff, buff.length, group, Configuration.getDiscoveryPort()));
			ms.close();			
			try {
				latch.await(Configuration.getDiscoveryTimeout()*2, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
			
			return responseRef.get();
		} catch (Exception e) {
			return null;
		} finally {
			try { ms.close(); } catch (Exception e) {}
		}
	}
	
	
	/**
	 * Tests the OTServerDiscovery component
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		log("Discovery Test");
//		log(rt("INFO|udp://%s:%s"));
		//log(rt("INFO|udp://%s:%s|TEXT"));
		while(true) {
			log(discover());
			log(discover("UDP"));
			log(discover("TCP"));
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
