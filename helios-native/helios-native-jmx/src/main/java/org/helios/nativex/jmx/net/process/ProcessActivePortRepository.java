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
package org.helios.nativex.jmx.net.process;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.nativex.jmx.net.ConnectionType;
import org.helios.nativex.sigar.HeliosSigar;
import org.helios.time.SystemClock;
import org.helios.time.SystemClock.ElapsedTime;
import org.hyperic.sigar.NetConnection;

/**
 * <p>Title: ProcessActivePortRepository</p>
 * <p>Description: Tracks all active ports for a given process.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.net.process.ProcessActivePortRepository</code></p>
 */
@SuppressWarnings("serial")
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating a new active port", types={
                @JMXNotificationType(type=ProcessActivePortRepository.NEW_PORT)
        }),
        @JMXNotification(description="Notification indicating a closed port", types={
                @JMXNotificationType(type=ProcessActivePortRepository.CLOSED_PORT)
        })
})
public class ProcessActivePortRepository extends ManagedObjectDynamicMBean {
	/** Tracks the PIDS of the processes registered */
	protected static final Map<Long, ProcessActivePortRepository> trackedProcesses = new ConcurrentHashMap<Long, ProcessActivePortRepository>();
	/** The port harvesting thread */
	protected static final Thread harvesterThread = new Thread(new Harvester(), "ProcessActivePortRepository Harvester Thread");
	/** The keep running flag for the harvester thread */
	protected static final AtomicBoolean harvest = new AtomicBoolean(true); 

	/** Notification prefix for new port */
	public static final String NEW_PORT = "org.helios.nativex.jmx.net.process.port.open";
	/** Notification prefix for closed port */
	public static final String CLOSED_PORT = "org.helios.nativex.jmx.net.process.port.closed";
	
	public static final Logger LOG = Logger.getLogger(ProcessActivePortRepository.class);
	
	/** The PID of the process ports are being tracked for */
	protected final long pid;
	
	/**
	 * Returns the currently tracked process pids.
	 * @return an array of pids
	 */
	public static long[] getTrackedPids() {
		Set<Long> tpids = trackedProcesses.keySet();
		long[] pids = new long[tpids.size()];
		int cnt = 0;
		for(Long p: tpids) {
			pids[cnt] = p;
			cnt++;
		}
		return pids;
	}
	
	/**
	 * Acquires a ProcessActivePortRepository for the passed PID
	 * @param pid The process ID of the process to track ports for
	 * @return a ProcessActivePortRepository for the passed PID
	 */
	public static ProcessActivePortRepository getInstance(long pid) {
		ProcessActivePortRepository repo = trackedProcesses.get(pid);
		if(repo==null) {
			synchronized(trackedProcesses) {
				repo = trackedProcesses.get(pid);
				if(repo==null) {
					repo = new ProcessActivePortRepository(pid);
					trackedProcesses.put(pid, repo);
				}
			}
		}
		return repo;
	}

	/**
	 * Creates a new ProcessActivePortRepository for the passed PID
	 * @param pid The process ID for the process ports are being tracked for
	 */
	private ProcessActivePortRepository(long pid) {
		this.pid = pid;
	}
	
	private static class Harvester implements Runnable {
		/** The sigar ref to get active ports */
		final HeliosSigar sigar = HeliosSigar.getInstance();
		/** Static class logger */
		final Logger LOG = Logger.getLogger(getClass());
		
		public void run() {
			while(harvest.get()) {
				SystemClock.startTimer();
				NetConnection[] netConns = sigar.getNetConnectionList(ConnectionType.flagForAll(), getTrackedPids());
				ElapsedTime et = SystemClock.endTimer();
				LOG.info("Retrieved [" + netConns.length + "] NetConnections in " + et.toString());
				for(NetConnection netConn: netConns) {
					long pid = sigar.getProcPort(netConn.getType(), netConn.getLocalPort());
				}
				try { Thread.sleep(5000); } catch (Exception e) {}
			}
		}
	}
	
}

