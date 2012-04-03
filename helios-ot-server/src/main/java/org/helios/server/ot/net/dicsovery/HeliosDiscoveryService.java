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
package org.helios.server.ot.net.dicsovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.helpers.Banner;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>Title: HeliosDiscoveryService</p>
 * <p>Description: Server discovery service to help Helios OT Clients locate a Helios OT Server through Multicast</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.net.dicsovery.HeliosDiscoveryService</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class HeliosDiscoveryService extends ManagedObjectDynamicMBean implements ApplicationContextAware, Runnable {
	/**  */
	private static final long serialVersionUID = -6480059684528924961L;
	/** The spring application context */
	protected ApplicationContext applicationContext = null;
	/** The multicast network to listen on */
	protected String network = null;
	/** The multicast InetAddress to listen on */
	protected InetSocketAddress mcastGroup = null;
	
	/** The multicast port to listen on */
	protected int port = -1;
	/** The multicast socket */
	protected MulticastSocket mcastSocket = null;
	/** The thread that runs the multicast listener */
	protected Thread mcastThread = null;
	/** The flag indicating if the mcast thread should keep running */
	protected boolean keepRunning = false;
	
	/** The request counter */
	protected final AtomicLong requestCounter = new AtomicLong(0);
	/** The invalid request counter */
	protected final AtomicLong invalidCounter = new AtomicLong(0);	
	/** The exception counter */
	protected final AtomicLong exceptionCounter = new AtomicLong(0);
	
	
	/** A map of commands keyed by the command name */
	protected static final Map<String, IDiscoveryCommand> commands = new HashMap<String, IDiscoveryCommand>(); 
	
	protected static void register(IDiscoveryCommand command) {
		commands.put(command.getCommandName().trim().toUpperCase(), command);
	}
	
	static {
		register(new InfoDumpDiscoveryCommand());
	}
	
	/** Instance logger */
	protected Logger log = Logger.getLogger(getClass());
	
	/**
	 * Starts the discovery service
	 * @throws Exception
	 */
	public void start() throws Exception {
		log.info(Banner.banner("=", 3, 10, "Starting HeliosDiscoveryService....", "Network:" + network, "Port:" + port));
		objectName = JMXHelper.objectName(new StringBuilder(getClass().getPackage().getName()).append(":network=").append(network).append(",port=").append(port));
		if(JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
			log.info(Banner.banner("#", 3, 10, "HeliosDiscoveryService [" + network + ":" + port + "] already running" ));
			return;
		}		
		mcastGroup = new InetSocketAddress(network, port);
		mcastSocket = new MulticastSocket(port);
		mcastSocket.joinGroup(mcastGroup.getAddress());
		
		mcastThread = new Thread(this, getClass().getSimpleName() + "Thread[" + network + ":" + port + "]");
		mcastThread.setDaemon(true);
		keepRunning = true;
		mcastThread.start();
		log.info(Banner.banner("=", 3, 10, "Started HeliosDiscoveryService.", "Network:" + network, "Port:" + port));
	}
	
	/**
	 * Stops the discovery service if it was started. 
	 */
	public void stop() {
		if(mcastSocket!=null) {
			log.info(Banner.banner("=", 3, 10, "Stopping HeliosDiscoveryService....", "Network:" + network, "Port:" + port));		
			keepRunning = false;
			mcastThread.interrupt();
			JMXHelper.getRuntimeHeliosMBeanServer().unregisterMBean(objectName);
			log.info(Banner.banner("=", 3, 10, "Stopped HeliosDiscoveryService....", "Network:" + network, "Port:" + port));
		} 
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(keepRunning) {
			try {
				byte[] message = new byte[1024];
				DatagramPacket packet = new DatagramPacket(message, message.length, mcastGroup.getAddress(), port);
				mcastSocket.receive(packet);
				requestCounter.incrementAndGet();
				String request = new String(packet.getData()).trim();
				log.info("Discovery Request[" + request + "]");
				String[] frags = request.split("\\|");
				if(frags.length>0 && commands.containsKey(frags[0].trim().toUpperCase()))  {
					String response = commands.get(frags[0].trim().toUpperCase()).execute(request, applicationContext);
					sendResponse(frags, response);
					
				} else {
					log.warn("Discovery Request[" + request + "] could not be interpreted");
				}
			} catch (Exception e) {
				exceptionCounter.incrementAndGet();
				log.error("HeliosDiscoveryService Error", e);
			}
			
		}
	}
	
	/**
	 * Sends a response back to the caller
	 * @param commandFragments The parsed command received from the caller
	 * @param message The response message
	 */
	protected void sendResponse(String[] commandFragments, String message) {
		try {
			URI uri = new URI(commandFragments[1]);
			if(uri.getScheme().trim().toLowerCase().equals("udp")) {
				DatagramSocket socket = null;
				try {
					InetAddress ina = InetAddress.getByName(uri.getHost());
					socket = new DatagramSocket();
					byte[] bytes = message.getBytes();
					DatagramPacket dp = new DatagramPacket(bytes, bytes.length, ina, uri.getPort());
					socket.send(dp);
					log.info("Sent Repsonse to [" + uri + "] with [" + bytes.length + "] bytes");
				} finally {
					try { socket.close(); } catch (Exception e) {}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Sets the injected app context
	 * @param applicationContext the applicationContext to set
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Returns the multicast network this discovery service is listening on
	 * @return the multicast network this discovery service is listening on
	 */
	@JMXAttribute(name="Network", description="The multicast network this discovery service is listening on", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getNetwork() {
		return network;
	}

	/**
	 * Sets the multicast network this discovery service is listening on
	 * @param network the multicast network this discovery service is listening on
	 */	
	public void setNetwork(String network) {
		this.network = network;
	}

	/**
	 * Returns the multicast port this discovery service is listening on
	 * @return the multicast port this discovery service is listening on
	 */
	@JMXAttribute(name="Port", description="The multicast port this discovery service is listening on", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getPort() {
		return port;
	}

	/**
	 * Sets the multicast port this discovery service is listening on
	 * @param port the multicast port this discovery service is listening on
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Returns a string representation of the multicast socket
	 * @return a string representation of the multicast socket
	 */
	@JMXAttribute(name="MulticastSocket", description="A string representation of the multicast socket", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getMulticastSocket() {
		return mcastSocket==null ? null : mcastSocket.toString();
	}
	
	/**
	 * Returns the network interface the mcast socket is bound to
	 * @return the network interface the mcast socket is bound to
	 */
	@JMXAttribute(name="NetworkInterface", description="A string representation network interface the mcast socket is bound to", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getNetworkInterface() {
		try {
			return mcastSocket==null ? null : mcastSocket.getNetworkInterface().toString();
		} catch (SocketException e) {
			return e.toString();			
		}
	}
	

	/**
	 * Returns the number of received requests
	 * @return the number of received requests
	 */
	@JMXAttribute(name="RequestCounter", description="The number of requests", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRequestCounter() {
		return requestCounter.get();
	}

	/**
	 * Returns the number of invalid requests that could not be processed
	 * @return the number of invalid requests that could not be processed
	 */
	@JMXAttribute(name="InvalidCounter", description="The number of invalid requests that could not be processed", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getInvalidCounter() {
		return invalidCounter.get();
	}

	/**
	 * Returns the number of server exceptions
	 * @return the number of server exceptions
	 */
	@JMXAttribute(name="ExceptionCounter", description="The number of server exceptions", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getExceptionCounter() {
		return exceptionCounter.get();
	}
}
