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
package org.helios.nativex.jmx.cpu;

import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.IntegerRollingCounter;
import org.helios.jmxenabled.counters.LongDeltaRollingCounter;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.SigarException;

/**
 * <p>Title: SystemCPUUtilizationService</p>
 * <p>Description: Monitor service for aggregate CPU utilization metrics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.cpu.SystemCPUUtilizationService</code></p>
 */

public class SystemCPUUtilizationService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = -355179519686790538L;
	/** The gatherer for the system aggregate CPU's raw stats */
	protected final Cpu cpu;
	/** The gatherer for the system aggregate CPU's percent stats */
	protected CpuPerc cpuPerc;
	
	/** The System Aggregate CPU Total utilization rate */
	protected final LongDeltaRollingCounter totalRateCounter = new LongDeltaRollingCounter("SystemCPUTotalUtilRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU User utilization rate */
	protected final LongDeltaRollingCounter userRateCounter = new LongDeltaRollingCounter("SystemCPUUserUtilRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Sys utilization rate */
	protected final LongDeltaRollingCounter sysRateCounter = new LongDeltaRollingCounter("SystemCPUSysUtilRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Nice utilization rate */
	protected final LongDeltaRollingCounter niceRateCounter = new LongDeltaRollingCounter("SystemCPUNiceUtilRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Idle utilization rate */
	protected final LongDeltaRollingCounter idleRateCounter = new LongDeltaRollingCounter("SystemCPUIdleUtilRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Wait utilization rate */
	protected final LongDeltaRollingCounter waitRateCounter = new LongDeltaRollingCounter("SystemCPUWaitUtilRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Irq utilization rate */
	protected final LongDeltaRollingCounter irqRateCounter = new LongDeltaRollingCounter("SystemCPUIrqUtilRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU SoftIrq utilization rate */
	protected final LongDeltaRollingCounter softIrqRateCounter = new LongDeltaRollingCounter("SystemCPUSoftIrqUtilRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Stolen utilization rate */
	protected final LongDeltaRollingCounter stolenRateCounter = new LongDeltaRollingCounter("SystemCPUStolenUtilRate", DEFAULT_ROLLING_SIZE, registerGroup);
	
	/** The System Aggregate CPU Total utilization percent */
	protected final IntegerRollingCounter totalPercentCounter = new IntegerRollingCounter("SystemCPUTotalUtilPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU User utilization percent */
	protected final IntegerRollingCounter userPercentCounter = new IntegerRollingCounter("SystemCPUUserUtilPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Sys utilization percent */
	protected final IntegerRollingCounter sysPercentCounter = new IntegerRollingCounter("SystemCPUSysUtilPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Nice utilization percent */
	protected final IntegerRollingCounter nicePercentCounter = new IntegerRollingCounter("SystemCPUNiceUtilPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Idle utilization percent */
	protected final IntegerRollingCounter idlePercentCounter = new IntegerRollingCounter("SystemCPUIdleUtilPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Wait utilization percent */
	protected final IntegerRollingCounter waitPercentCounter = new IntegerRollingCounter("SystemCPUWaitUtilPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Irq utilization percent */
	protected final IntegerRollingCounter irqPercentCounter = new IntegerRollingCounter("SystemCPUIrqUtilPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU SoftIrq utilization percent */
	protected final IntegerRollingCounter softIrqPercentCounter = new IntegerRollingCounter("SystemCPUSoftIrqUtilPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The System Aggregate CPU Stolen utilization percent */
	protected final IntegerRollingCounter stolenPercentCounter = new IntegerRollingCounter("SystemCPUStolenUtilPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	
	/**
	 * Creates a new SystemCPUUtilizationService
	 */
	public SystemCPUUtilizationService() {
		super();
		try {
			cpuPerc = HeliosSigar.getInstance().getSigar().getCpuPerc();
			cpu = HeliosSigar.getInstance().getSigar().getCpu();				
		} catch (SigarException ex) {
			throw new RuntimeException("Failed to get CPU Gathering Stubs" , ex);
		}
		this.scheduleSampling();
		registerCounterMBean("type", "SystemCPUUtilizationService");
		initPerfCounters();
		run();
	}
	
	/**
	 * Bootstraps this service 
	 */
	public static void boot() {
		new SystemCPUUtilizationService();
	}

	
	private final AtomicBoolean firstRun = new AtomicBoolean(false);
	
	/**
	 * Executes the CPU stats gathering
	 */
	@Override
	public void run() {
		try {			
			// The first reading sometimes returns NaNs, so we only process the second+ gatherings.
			cpu.gather(HeliosSigar.getInstance().getSigar());
			cpuPerc = HeliosSigar.getInstance().getSigar().getCpuPerc();
			if(firstRun.get()) {							
				//if(log.isDebugEnabled()) log.debug("[" + objectName + "] CPU:" + cpu);
				totalRateCounter.put(cpu.getTotal());
				userRateCounter.put(cpu.getUser());
				sysRateCounter.put(cpu.getSys());
				niceRateCounter.put(cpu.getNice());
				idleRateCounter.put(cpu.getIdle());
				waitRateCounter.put(cpu.getWait());
				irqRateCounter.put(cpu.getIrq());
				softIrqRateCounter.put(cpu.getSoftIrq());
				stolenRateCounter.put(cpu.getStolen());
				totalPercentCounter.put(doubleToIntPercent(cpuPerc.getCombined()));
				userPercentCounter.put(doubleToIntPercent(cpuPerc.getUser()));
				sysPercentCounter.put(doubleToIntPercent(cpuPerc.getSys()));
				nicePercentCounter.put(doubleToIntPercent(cpuPerc.getNice()));
				idlePercentCounter.put(doubleToIntPercent(cpuPerc.getIdle()));
				waitPercentCounter.put(doubleToIntPercent(cpuPerc.getWait()));
				irqPercentCounter.put(doubleToIntPercent(cpuPerc.getIrq()));
				softIrqPercentCounter.put(doubleToIntPercent(cpuPerc.getSoftIrq()));
				stolenPercentCounter.put(doubleToIntPercent(cpuPerc.getStolen()));
			}
		} catch (Exception e) {
			log.error("Failed to gather on service [" + objectName + "]", e);
			throw new RuntimeException("Failed to gather on service [" + objectName + "]", e);
		} finally {
			firstRun.set(true);
		}
	}
	
	
	/**
	 * Returns the system aggregate Total CPU utilization rate
	 * @return the system aggregate Total CPU utilization rate 
	 */
	@JMXAttribute(name="TotalCPURate", description="The system Total CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTotalCPURate() {
		return totalRateCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate User CPU utilization rate
	 * @return the system aggregate User CPU utilization rate 
	 */
	@JMXAttribute(name="UserCPURate", description="The system User CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getUserCPURate() {
		return userRateCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Sys CPU utilization rate
	 * @return the system aggregate Sys CPU utilization rate 
	 */
	@JMXAttribute(name="SysCPURate", description="The system Sys CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSysCPURate() {
		return sysRateCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Nice CPU utilization rate
	 * @return the system aggregate Nice CPU utilization rate 
	 */
	@JMXAttribute(name="NiceCPURate", description="The system Nice CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getNiceCPURate() {
		return niceRateCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Idle CPU utilization rate
	 * @return the system aggregate Idle CPU utilization rate 
	 */
	@JMXAttribute(name="IdleCPURate", description="The system Idle CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getIdleCPURate() {
		return idleRateCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Wait CPU utilization rate
	 * @return the system aggregate Wait CPU utilization rate 
	 */
	@JMXAttribute(name="WaitCPURate", description="The system Wait CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getWaitCPURate() {
		return waitRateCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Irq CPU utilization rate
	 * @return the system aggregate Irq CPU utilization rate 
	 */
	@JMXAttribute(name="IrqCPURate", description="The system Irq CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getIrqCPURate() {
		return irqRateCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate SoftIrq CPU utilization rate
	 * @return the system aggregate SoftIrq CPU utilization rate 
	 */
	@JMXAttribute(name="SoftIrqCPURate", description="The system SoftIrq CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSoftIrqCPURate() {
		return softIrqRateCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Stolen CPU utilization rate
	 * @return the system aggregate Stolen CPU utilization rate 
	 */
	@JMXAttribute(name="StolenCPURate", description="The system Stolen CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getStolenCPURate() {
		return stolenRateCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Total CPU utilization percentage
	 * @return the system aggregate Total CPU utilization percentage 
	 */
	@JMXAttribute(name="TotalCPUPercent", description="The system Total CPU utilization %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTotalCPUPercent() {
		return totalPercentCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate User CPU utilization percentage
	 * @return the system aggregate User CPU utilization percentage 
	 */
	@JMXAttribute(name="UserCPUPercent", description="The system User CPU utilization %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getUserCPUPercent() {
		return userPercentCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Sys CPU utilization percentage
	 * @return the system aggregate Sys CPU utilization percentage 
	 */
	@JMXAttribute(name="SysCPUPercent", description="The system Sys CPU utilization %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getSysCPUPercent() {
		return sysPercentCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Nice CPU utilization percentage
	 * @return the system aggregate Nice CPU utilization percentage 
	 */
	@JMXAttribute(name="NiceCPUPercent", description="The system Nice CPU utilization %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getNiceCPUPercent() {
		return nicePercentCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Idle CPU utilization percentage
	 * @return the system aggregate Idle CPU utilization percentage 
	 */
	@JMXAttribute(name="IdleCPUPercent", description="The system Idle CPU utilization %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getIdleCPUPercent() {
		return idlePercentCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Wait CPU utilization percentage
	 * @return the system aggregate Wait CPU utilization percentage 
	 */
	@JMXAttribute(name="WaitCPUPercent", description="The system Wait CPU utilization %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getWaitCPUPercent() {
		return waitPercentCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Irq CPU utilization percentage
	 * @return the system aggregate Irq CPU utilization percentage 
	 */
	@JMXAttribute(name="IrqCPUPercent", description="The system Irq CPU utilization %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getIrqCPUPercent() {
		return irqPercentCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate SoftIrq CPU utilization percentage
	 * @return the system aggregate SoftIrq CPU utilization percentage 
	 */
	@JMXAttribute(name="SoftIrqCPUPercent", description="The system SoftIrq CPU utilization %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getSoftIrqCPUPercent() {
		return softIrqPercentCounter.getLastValue();
	}

	/**
	 * Returns the system aggregate Stolen CPU utilization percentage
	 * @return the system aggregate Stolen CPU utilization percentage 
	 */
	@JMXAttribute(name="StolenCPUPercent", description="The system Stolen CPU utilization %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getStolenCPUPercent() {
		return stolenPercentCounter.getLastValue();
	}

	

}
