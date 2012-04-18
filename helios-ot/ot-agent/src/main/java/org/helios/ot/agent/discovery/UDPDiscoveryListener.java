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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

/**
 * <p>Title: UDPDiscoveryListener</p>
 * <p>Description: A UDP listener that listens for discovery responses from an OT Server Discovery Service that picked up our transmissions.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.discovery.UDPDiscoveryListener</code></p>
 */

public class UDPDiscoveryListener implements Runnable {
	protected final Logger log = Logger.getLogger(getClass());
	/** The socket address where we're listening */
	protected InetSocketAddress insock = null;
	/** The datagram socket receiving (hopefully) the discovery response */
	protected DatagramSocket datsock = null;
	/** The thread handling the listening  */
	protected Thread listenerThread = null;
	/** The ephemeral port number that will be packaged into the multicast request */
	protected int listeningPort = -1;
	/** The completion latch to drop when a discovery completes successfully */
	protected final CountDownLatch completionLatch;
	/** The reference into which the response of a successful discovery attempt will be placed */
	protected final AtomicReference<byte[]> discoveryResponse;
	/** Indicates the thread should keep running */
	protected boolean run = true;
	/** A serial number generator for thread names */
	private static final AtomicLong serial = new AtomicLong(0L);
	/**
	 * Creates a new UDPDiscoveryListener
	 * @param completionLatch
	 * @param discoveryResponse
	 */
	public UDPDiscoveryListener(final CountDownLatch completionLatch, final AtomicReference<byte[]> discoveryResponse) {
		this.completionLatch = completionLatch;
		this.discoveryResponse =  discoveryResponse;
		listenerThread = new Thread(this, "OTServerDiscoveryThread#" + serial.incrementAndGet());
		listenerThread.setDaemon(true);
	}
	
	/**
	 * Runs the listener
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		byte[] buffer = new byte[2048];
		DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
		while(run) {
			try {
				datsock.receive(dp);
				byte[] response = new byte[dp.getLength()];
				System.arraycopy(dp.getData(), dp.getOffset(), response, 0, dp.getLength());
				discoveryResponse.set(response);
				completionLatch.countDown();
				break;
			} catch (Exception e) {
				if(!run) break;
				log.warn("Discovery Listener Exception", e);
			}
		}
		if(log.isDebugEnabled()) log.debug("Discovery Listener Run Loop Exited");
	}
	
	/**
	 * Starts the discovery listener
	 * @return the listening port that the OT Server Dicovery Service should ping back to
	 * @throws Exception
	 */
	public int start() throws Exception {
		if(log.isDebugEnabled()) log.debug("Starting Discovery Listener");
		run = true;
		insock = new InetSocketAddress("0.0.0.0", 0);
		datsock = new DatagramSocket(insock);
		datsock.setSoTimeout(0);  // don't let the listener timeout until it is done, or stopped.
		listeningPort = datsock.getLocalPort();
		listenerThread.start();
		return listeningPort;		
	}
	
	/**
	 * Stops the discovery listener
	 */
	public void stop() {
		run = false;
		try { listenerThread.interrupt(); } catch (Exception e) {};
		try { datsock.close(); } catch (Exception e) {};
		if(log.isDebugEnabled()) log.debug("Stopped Discovery Listener");
	}
	
	/*
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

	 */
	
}
