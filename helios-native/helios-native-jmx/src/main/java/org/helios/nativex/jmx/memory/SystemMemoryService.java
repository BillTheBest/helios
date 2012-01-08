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
package org.helios.nativex.jmx.memory;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.IntegerRollingCounter;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.SigarException;

/**
 * <p>Title: SystemMemoryService</p>
 * <p>Description: Monitor service for aggregate memory info and utilization metrics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.memory.SystemMemoryService</code></p>
 */

public class SystemMemoryService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = 5983103755810715643L;
	/** The system memory gatherer */
	protected final Mem mem;	
	/** The System Total memory */
	protected final LongRollingCounter totalCounter = new LongRollingCounter("SystemMemoryTotalCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Ram memory */
	protected final LongRollingCounter ramCounter = new LongRollingCounter("SystemMemoryRamCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Used memory */
	protected final LongRollingCounter usedCounter = new LongRollingCounter("SystemMemoryUsedCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Free memory */
	protected final LongRollingCounter freeCounter = new LongRollingCounter("SystemMemoryFreeCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System ActualUsed memory */
	protected final LongRollingCounter actualUsedCounter = new LongRollingCounter("SystemMemoryActualUsedCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System ActualFree memory */
	protected final LongRollingCounter actualFreeCounter = new LongRollingCounter("SystemMemoryActualFreeCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System UsedPercent memory */
	protected final IntegerRollingCounter usedPercentCounter = new IntegerRollingCounter("SystemMemoryUsedPercentCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System FreePercent memory */
	protected final IntegerRollingCounter freePercentCounter = new IntegerRollingCounter("SystemMemoryFreePercentCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	
	/**
	 * Creates a new SystemMemoryService
	 */
	public SystemMemoryService() {
		super();
		try {
			mem = HeliosSigar.getInstance().getSigar().getMem();		
		} catch (SigarException ex) {
			throw new RuntimeException("Failed to get System memory Gathering Stubs" , ex);
		}
		this.scheduleSampling();
		registerCounterMBean("type", "SystemMemoryService");
		initPerfCounters();
		run();
	}
	/**
	 * Bootstraps this service 
	 */
	public static void boot() {
		new SystemMemoryService();
	}
	
	
	/**
	 * Executes the System memory stats gathering
	 */
	@Override
	public void run() {
		try {
			mem.gather(HeliosSigar.getInstance().getSigar());
			//if(log.isDebugEnabled()) log.debug("[Mem]:" + mem + " Free%:" + mem.getFreePercent());
			totalCounter.put(mem.getTotal());
			ramCounter.put(mem.getRam());
			usedCounter.put(mem.getUsed());
			freeCounter.put(mem.getFree());
			actualUsedCounter.put(mem.getActualUsed());
			actualFreeCounter.put(mem.getActualFree());
			usedPercentCounter.put((int)mem.getUsedPercent());
			freePercentCounter.put((int)mem.getFreePercent());			
		} catch (Exception e) {
			log.error("Failed to gather on service [" + objectName + "]", e);
			throw new RuntimeException("Failed to gather on service [" + objectName + "]", e);
		}
	}
	
	
	/**
	 * Returns the system Total memory 
	 * @return the system aggregate Total memory  utilization 
	 */
	@JMXAttribute(name="Total", description="The system Total memory", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTotal() {
		return totalCounter.getLastValue();
	}

	/**
	 * Returns the system Ram memory in MB
	 * @return the system aggregate Ram memory  utilization 
	 */
	@JMXAttribute(name="Ram", description="The system Ram memory in MB", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRam() {
		return ramCounter.getLastValue();
	}

	/**
	 * Returns the system Used memory 
	 * @return the system aggregate Used memory  utilization 
	 */
	@JMXAttribute(name="Used", description="The system Used memory", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getUsed() {
		return usedCounter.getLastValue();
	}

	/**
	 * Returns the system Free memory 
	 * @return the system aggregate Free memory  utilization 
	 */
	@JMXAttribute(name="Free", description="The system Free memory", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFree() {
		return freeCounter.getLastValue();
	}

	/**
	 * Returns the system ActualUsed memory 
	 * @return the system aggregate ActualUsed memory  utilization 
	 */
	@JMXAttribute(name="ActualUsed", description="The system ActualUsed memory", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getActualUsed() {
		return actualUsedCounter.getLastValue();
	}

	/**
	 * Returns the system ActualFree memory 
	 * @return the system aggregate ActualFree memory  utilization 
	 */
	@JMXAttribute(name="ActualFree", description="The system ActualFree memory", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getActualFree() {
		return actualFreeCounter.getLastValue();
	}

	/**
	 * Returns the system UsedPercent memory 
	 * @return the system aggregate UsedPercent memory  utilization 
	 */
	@JMXAttribute(name="UsedPercent", description="The system UsedPercent memory", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getUsedPercent() {
		return usedPercentCounter.getLastValue();
	}

	/**
	 * Returns the system FreePercent memory 
	 * @return the system aggregate FreePercent memory  utilization 
	 */
	@JMXAttribute(name="FreePercent", description="The system FreePercent memory", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getFreePercent() {
		return freePercentCounter.getLastValue();
	}

	

}
