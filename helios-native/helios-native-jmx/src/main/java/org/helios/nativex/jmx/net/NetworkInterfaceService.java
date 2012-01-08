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

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.LongDeltaRollingCounter;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;

/**
 * <p>Title: NetworkInterfaceService</p>
 * <p>Description: Native monitor service for network interfaces</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.net.NetworkInterfaceService</code></p>
 */

public class NetworkInterfaceService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = -2325853835948961275L;
	/** The NIC name */
	protected final String interfaceName;
	/** The NIC type */
	protected final String interfaceType;
	
	/** The NIC's stats gatherer */
	protected final NetInterfaceStat interfaceStat;
	/** The NIC's config gatherer */
	protected final NetInterfaceConfig interfaceConfig;
	
	
	/** Received Bytes Counter */
	protected final LongDeltaRollingCounter rxBytesCounter = new LongDeltaRollingCounter("ReceivedBytes", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Transmitted Bytes Counter */
	protected final LongDeltaRollingCounter txBytesCounter = new LongDeltaRollingCounter("SentBytes", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Received Packets Counter */
	protected final LongDeltaRollingCounter rxPacketsCounter = new LongDeltaRollingCounter("ReceivedPackets", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Transmitted Packets Counter */
	protected final LongDeltaRollingCounter txPacketsCounter = new LongDeltaRollingCounter("SentPackets", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Receiving Errors Counter */
	protected final LongDeltaRollingCounter rxErrorsCounter = new LongDeltaRollingCounter("RXErrors", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Transmit Errors Counter */
	protected final LongDeltaRollingCounter txErrorsCounter = new LongDeltaRollingCounter("TXErrors", DEFAULT_ROLLING_SIZE, registerGroup);	
	/** Receiving Overruns Counter */
	protected final LongDeltaRollingCounter rxOversCounter = new LongDeltaRollingCounter("RXOverruns", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Transmit Overruns Counter */
	protected final LongDeltaRollingCounter txOversCounter = new LongDeltaRollingCounter("TXOverruns", DEFAULT_ROLLING_SIZE, registerGroup);	
	/** Receiving Drops Counter */
	protected final LongDeltaRollingCounter rxDropsCounter = new LongDeltaRollingCounter("RXDrops", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Transmit Drops Counter */
	protected final LongDeltaRollingCounter txDropsCounter = new LongDeltaRollingCounter("TXDrops", DEFAULT_ROLLING_SIZE, registerGroup);
	
	/** Receiving Frame Counter */
	protected final LongDeltaRollingCounter rxFrameCounter = new LongDeltaRollingCounter("RXFrame", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Transmit Carrier Counter */
	protected final LongDeltaRollingCounter txCarrierCounter = new LongDeltaRollingCounter("TXCarrier", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Transmit Collision  Counter */
	protected final LongDeltaRollingCounter txCollisionCounter = new LongDeltaRollingCounter("TXCollisions", DEFAULT_ROLLING_SIZE, registerGroup);
	
	
	
	
	/**
	 * Creates a new NetworkInterfaceService for the passed NIC name
	 * @param interfaceName The NIC name
	 */
	public NetworkInterfaceService(String interfaceName) {
		super();
		this.interfaceName = interfaceName;
		interfaceConfig = HeliosSigar.getInstance().getNetInterfaceConfig(interfaceName);
		interfaceType = interfaceConfig.getType();
		interfaceStat = HeliosSigar.getInstance().getNetInterfaceStat(interfaceName);
		this.scheduleSampling();
		registerCounterMBean("nic", interfaceName, "type", interfaceType);
		initPerfCounters();

	}

	/**
	 * 
	 */
	@Override
	public void run() {
		try {
			interfaceStat.gather(sigar, interfaceName);
			rxBytesCounter.put(interfaceStat.getRxBytes());
			txBytesCounter.put(interfaceStat.getTxBytes());
			rxPacketsCounter.put(interfaceStat.getRxPackets());
			txPacketsCounter.put(interfaceStat.getTxPackets());
			rxErrorsCounter.put(interfaceStat.getRxErrors());
			txErrorsCounter.put(interfaceStat.getTxErrors());
			rxOversCounter.put(interfaceStat.getRxOverruns());
			txOversCounter.put(interfaceStat.getTxOverruns());
			rxDropsCounter.put(interfaceStat.getRxDropped());
			txDropsCounter.put(interfaceStat.getTxDropped());
			rxFrameCounter.put(interfaceStat.getRxFrame());
			txCarrierCounter.put(interfaceStat.getTxCarrier());
			txCollisionCounter.put(interfaceStat.getTxCollisions());
			
			
		} catch (Exception e) {
			log.error("Failed to gather on service [" + objectName + "]", e);
			throw new RuntimeException("Failed to gather on service [" + objectName + "]", e);
		}
	}
	
	/**
	 * Returns the NIC's transmit collision rate
	 * @return the NIC's transmit collision rate
	 */
	@JMXAttribute(name="TXCollisions", description="The NIC's transmit collision rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getTXCollisions() {
		return txCollisionCounter.getLastValue();
	}
	

	/**
	 * Returns the NIC's transmit carrier rate
	 * @return the NIC's transmit carrier rate
	 */
	@JMXAttribute(name="TXCarrier", description="The NIC's transmit carrier rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getTXCarrier() {
		return txCarrierCounter.getLastValue();
	}
	

	/**
	 * Returns the NIC's received frame rate
	 * @return the NIC's received frame rate
	 */
	@JMXAttribute(name="RXFrames", description="The NIC's received frame rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getRXFrames() {
		return rxFrameCounter.getLastValue();
	}
	
	/**
	 * Returns the NIC's received drops rate
	 * @return the NIC's received drops rate
	 */
	@JMXAttribute(name="RXOverruns", description="The NIC's received drops rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getRXDrops() {
		return rxDropsCounter.getLastValue();
	}
	
	/**
	 * Returns the NIC's transmit drops rate
	 * @return the NIC's transmit drops rate
	 */
	@JMXAttribute(name="TXDrops", description="The NIC's transmit drops rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getTXDrops() {
		return txDropsCounter.getLastValue();
	}
	
	
	/**
	 * Returns the NIC's received overruns rate
	 * @return the NIC's received overruns rate
	 */
	@JMXAttribute(name="RXOverruns", description="The NIC's received overruns rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getRXOverruns() {
		return rxOversCounter.getLastValue();
	}
	
	/**
	 * Returns the NIC's sent overruns rate
	 * @return the NIC's sent overruns rate
	 */
	@JMXAttribute(name="TXOverruns", description="The NIC's transmit overruns rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getTXOverruns() {
		return txOversCounter.getLastValue();
	}
	
	
	/**
	 * Returns the NIC's received Error rate
	 * @return the NIC's received Error rate
	 */
	@JMXAttribute(name="RXErrors", description="The NIC's received Error rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getRXErrors() {
		return rxErrorsCounter.getLastValue();
	}
	
	/**
	 * Returns the NIC's sent Errors rate
	 * @return the NIC's sent Errors rate
	 */
	@JMXAttribute(name="TXErrors", description="The NIC's transmit Errors rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getTXErrors() {
		return txErrorsCounter.getLastValue();
	}
	
	
	/**
	 * Returns the NIC's received packets rate
	 * @return the NIC's received packets rate
	 */
	@JMXAttribute(name="RXPackets", description="The NIC's received packets rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getRXPackets() {
		return rxPacketsCounter.getLastValue();
	}
	
	/**
	 * Returns the NIC's sent Packets rate
	 * @return the NIC's sent Packets rate
	 */
	@JMXAttribute(name="TXPackets", description="The NIC's transmit Packets rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getTXPackets() {
		return txPacketsCounter.getLastValue();
	}
	
	
	/**
	 * Returns the NIC's received bytes rate
	 * @return the NIC's received bytes rate
	 */
	@JMXAttribute(name="RXBytes", description="The NIC's received bytes rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getRXBytes() {
		return rxBytesCounter.getLastValue();
	}
	
	/**
	 * Returns the NIC's sent bytes rate
	 * @return the NIC's sent bytes rate
	 */
	@JMXAttribute(name="TXBytes", description="The NIC's transmit bytes rate", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getTXBytes() {
		return txBytesCounter.getLastValue();
	}
	
	
	/**
	 * Returns the NIC's address
	 * @return the NIC's address
	 */
	@JMXAttribute(name="Address", description="The NIC's address", mutability=AttributeMutabilityOption.READ_ONLY)	
	public String getAddress() {
		return interfaceConfig.getAddress();
	}
	
	/**
	 * Returns the NIC's broadcast
	 * @return the NIC's broadcast
	 */
	@JMXAttribute(name="Broadcast", description="The NIC's broadcast", mutability=AttributeMutabilityOption.READ_ONLY)	
	public String getBroadcast() {
		return interfaceConfig.getBroadcast();
	}
	
	/**
	 * Returns the NIC's Description
	 * @return the NIC's Description
	 */
	@JMXAttribute(name="Description", description="The NIC's Description", mutability=AttributeMutabilityOption.READ_ONLY)	
	public String getDescription() {
		return interfaceConfig.getDescription();
	}
	
	/**
	 * Returns the NIC's Destination
	 * @return the NIC's Destination
	 */
	@JMXAttribute(name="Destination", description="The NIC's Destination", mutability=AttributeMutabilityOption.READ_ONLY)	
	public String getDestination() {
		return interfaceConfig.getDestination();
	}
	
	/**
	 * Returns the NIC's Speed
	 * @return the NIC's Speed
	 */
	@JMXAttribute(name="Speed", description="The NIC's Speed", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getSpeed() {
		return interfaceStat.getSpeed();
	}
	
	
	/**
	 * Returns the NIC's MAC Address
	 * @return the NIC's MAC Address
	 */
	@JMXAttribute(name="MACAddress", description="The NIC's MAC Address", mutability=AttributeMutabilityOption.READ_ONLY)	
	public String getMACAddress() {
		return interfaceConfig.getHwaddr();
	}
	
	/**
	 * Returns the NIC's NetMask
	 * @return the NIC's NetMask
	 */
	@JMXAttribute(name="NetMask", description="The NIC's NetMask", mutability=AttributeMutabilityOption.READ_ONLY)	
	public String getMACNetMask() {
		return interfaceConfig.getNetmask();
	}
	
	
	/**
	 * Returns the NIC's MTU
	 * @return the NIC's MTU
	 */
	@JMXAttribute(name="MTU", description="The NIC's Maximum Transfer Unit", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getMtu() {
		return interfaceConfig.getMtu();
	}
	
	/**
	 * Returns the NIC's Metric
	 * @return the NIC's Metric
	 */
	@JMXAttribute(name="Metric", description="The NIC's Metric", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getMetric() {
		return interfaceConfig.getMetric();
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Bootstraps this service.
	 */
	public static void boot() {
		HeliosSigar hsigar = HeliosSigar.getInstance();
		for(String s: hsigar.getNetInterfaceList()) {
			if(!"0.0.0.0".equals(hsigar.getNetInterfaceConfig(s).getAddress())) {
				new NetworkInterfaceService(s);
			}
		}		
	}

	/**
	 * The system name for this NIC
	 * @return the interfaceName
	 */
	@JMXAttribute(name="InterfaceName", description="The system name for this NIC", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getInterfaceName() {
		return interfaceName;
	}

	/**
	 * The NIC type name
	 * @return the interfaceName
	 */
	@JMXAttribute(name="InterfaceType", description="The NIC type name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getInterfaceType() {
		return interfaceType;
	}

	/**
	 * @return the rxBytesCounter
	 */
	public LongDeltaRollingCounter getRxBytesCounter() {
		return rxBytesCounter;
	}

}
