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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.IntegerRollingCounter;
import org.helios.jmxenabled.counters.LongDeltaRollingCounter;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.SigarException;

/**
 * <p>Title: CPUUtilizationService</p>
 * <p>Description: Service for publishing CPU info, utilization and stats</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.cpu.CPUUtilizationService</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class CPUUtilizationService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = -3611882914217230205L;
	/** The CPU Id */
	protected final int cpuId;
	/** The gatherer for info on this CPU */
	protected final CpuInfo cpuInfo;
	/** The gatherer for this CPU's percent stats */
	protected CpuPerc cpuPerc;
	/** The gatherer for this CPU's raw stats */
	protected final Cpu cpu;

	/** The CPU Model Name */
	protected final String model;
	/** The CPU Vendor */
	protected final String vendor;
	/** The CPU Clock Speed */
	protected final int mhz;
	/** The CPU Clock Size */
	protected final long cacheSize;
	/** The CPU's logical number of cores */
	protected final int totalCores;
	/** The System's physical number of sockets */
	protected final int totalSockets;
	/** The number of cores per socket */
	protected final int coresPerSocket;	
	
	/** This CPU's null utilization */
	protected final IntegerRollingCounter userPercentCounter;
	/** This CPU's null utilization */
	protected final IntegerRollingCounter sysPercentCounter;
	/** This CPU's null utilization */
	protected final IntegerRollingCounter nicePercentCounter;
	/** This CPU's null utilization */
	protected final IntegerRollingCounter idlePercentCounter;
	/** This CPU's null utilization */
	protected final IntegerRollingCounter waitPercentCounter;
	/** This CPU's null utilization */
	protected final IntegerRollingCounter irqPercentCounter;
	/** This CPU's null utilization */
	protected final IntegerRollingCounter softIrqPercentCounter;
	/** This CPU's null utilization */
	protected final IntegerRollingCounter stolenPercentCounter;
	/** This CPU's null utilization */
	protected final IntegerRollingCounter combinedPercentCounter;
	
	
	/** This CPU's Total utilization */
	protected final LongDeltaRollingCounter totalRateCounter;
	/** This CPU's User utilization */
	protected final LongDeltaRollingCounter userRateCounter;
	/** This CPU's Sys utilization */
	protected final LongDeltaRollingCounter sysRateCounter;
	/** This CPU's Nice utilization */
	protected final LongDeltaRollingCounter niceRateCounter;
	/** This CPU's Idle utilization */
	protected final LongDeltaRollingCounter idleRateCounter;
	/** This CPU's Wait utilization */
	protected final LongDeltaRollingCounter waitRateCounter;
	/** This CPU's Irq utilization */
	protected final LongDeltaRollingCounter irqRateCounter;
	/** This CPU's SoftIrq utilization */
	protected final LongDeltaRollingCounter softIrqRateCounter;
	/** This CPU's Stolen utilization */
	protected final LongDeltaRollingCounter stolenRateCounter;
	
	
	
	/**
	 * Creates a new CPUUtilizationService
	 * @param cpuId The CPU Id as a zero based index 
	 */
	public CPUUtilizationService(int cpuId) {
		super();
		this.cpuId = cpuId;
		try {
			cpuInfo = HeliosSigar.getInstance().getSigar().getCpuInfoList()[cpuId];
			cpuPerc = HeliosSigar.getInstance().getSigar().getCpuPercList()[cpuId];
			cpu = HeliosSigar.getInstance().getSigar().getCpuList()[cpuId];
			model = cpuInfo.getModel();
			vendor = cpuInfo.getVendor();
			mhz = cpuInfo.getMhz();
			cacheSize = cpuInfo.getCacheSize();
			totalCores = cpuInfo.getTotalCores();
			totalSockets = cpuInfo.getTotalSockets();
			coresPerSocket = cpuInfo.getCoresPerSocket();
			userPercentCounter = new IntegerRollingCounter("CPUUserSize" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			sysPercentCounter = new IntegerRollingCounter("CPUSysSize" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			nicePercentCounter = new IntegerRollingCounter("CPUNiceSize" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			idlePercentCounter = new IntegerRollingCounter("CPUIdleSize" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			waitPercentCounter = new IntegerRollingCounter("CPUWaitSize" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			irqPercentCounter = new IntegerRollingCounter("CPUIrqSize" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			softIrqPercentCounter = new IntegerRollingCounter("CPUSoftIrqSize" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			stolenPercentCounter = new IntegerRollingCounter("CPUStolenSize" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			combinedPercentCounter = new IntegerRollingCounter("CPUCombinedSize" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			totalRateCounter = new LongDeltaRollingCounter("CPUTotalUtilRate" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			userRateCounter = new LongDeltaRollingCounter("CPUUserUtilRate" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			sysRateCounter = new LongDeltaRollingCounter("CPUSysUtilRate" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			niceRateCounter = new LongDeltaRollingCounter("CPUNiceUtilRate" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			idleRateCounter = new LongDeltaRollingCounter("CPUIdleUtilRate" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			waitRateCounter = new LongDeltaRollingCounter("CPUWaitUtilRate" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			irqRateCounter = new LongDeltaRollingCounter("CPUIrqUtilRate" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			softIrqRateCounter = new LongDeltaRollingCounter("CPUSoftIrqUtilRate" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);
			stolenRateCounter = new LongDeltaRollingCounter("CPUStolenUtilRate" + cpuId, DEFAULT_ROLLING_SIZE, registerGroup);			
			run();
		} catch (SigarException ex) {
			throw new RuntimeException("Failed to get CPU Gathering Stubs" , ex);
		}
		this.scheduleSampling();
		registerCounterMBean("type", "CPUUtilizationService", "cpu", "" + cpuId);
		initPerfCounters();
		
	}
	
	/**
	 * Executes the CPU stats gathering
	 */
	@Override
	public void run() {
		try {
			
			cpu.gather(HeliosSigar.getInstance().getSigar());
			cpuPerc = HeliosSigar.getInstance().getSigar().getCpuPercList()[cpuId];
			
			
			userPercentCounter.put(doubleToIntPercent(cpuPerc.getUser()));
			sysPercentCounter.put(doubleToIntPercent(cpuPerc.getSys()));
			nicePercentCounter.put(doubleToIntPercent(cpuPerc.getNice()));
			idlePercentCounter.put(doubleToIntPercent(cpuPerc.getIdle()));
			waitPercentCounter.put(doubleToIntPercent(cpuPerc.getWait()));
			irqPercentCounter.put(doubleToIntPercent(cpuPerc.getIrq()));
			softIrqPercentCounter.put(doubleToIntPercent(cpuPerc.getSoftIrq()));
			stolenPercentCounter.put(doubleToIntPercent(cpuPerc.getStolen()));
			combinedPercentCounter.put(doubleToIntPercent(cpuPerc.getCombined()));			
			totalRateCounter.put(cpu.getTotal());
			userRateCounter.put(cpu.getUser());
			sysRateCounter.put(cpu.getSys());
			niceRateCounter.put(cpu.getNice());
			idleRateCounter.put(cpu.getIdle());
			waitRateCounter.put(cpu.getWait());
			irqRateCounter.put(cpu.getIrq());
			softIrqRateCounter.put(cpu.getSoftIrq());
			stolenRateCounter.put(cpu.getStolen());		
			
		} catch (Exception e) {
			log.error("Failed to gather on service [" + objectName + "]", e);
			throw new RuntimeException("Failed to gather on service [" + objectName + "]", e);
		}
	}
	
	protected Cpu serializedCopy(Cpu cpu) throws Exception {		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(cpu);
		oos.flush(); baos.flush();
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		return (Cpu)ois.readObject();
	}

	/**
	 * The id of this CPU
	 * @return the cpuId
	 */
	@JMXAttribute(name="CpuId", description="The id of this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getCpuId() {
		return cpuId;
	}

	/**
	 * The CPU Model Name
	 * @return the model
	 */
	@JMXAttribute(name="Model", description="The CPU Model Name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getModel() {
		return model;
	}

	/**
	 * The CPU Vendor
	 * @return the vendor
	 */
	@JMXAttribute(name="Vendor", description="The CPU Vendor Name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getVendor() {
		return vendor;
	}

	/**
	 * The CPU Clock Speed (Mhz)
	 * @return the mhz
	 */
	@JMXAttribute(name="ClockSpeed", description="The CPU Clock Speed (Mhz)", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getClockSpeed() {
		return mhz;
	}

	/**
	 * The CPU Cache Size (in bytes)
	 * @return the cacheSize
	 */
	@JMXAttribute(name="CacheSize", description="The CPU Cache Size (in bytes)", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCacheSize() {
		return cacheSize;
	}

	/**
	 * The logical number of cores in this CPU
	 * @return the totalCores
	 */
	@JMXAttribute(name="Cores", description="The logical number of cores in this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTotalCores() {
		return totalCores;
	}

	/**
	 * The physical number of sockets in this host
	 * @return the totalSockets
	 */
	@JMXAttribute(name="Sockets", description="The physical number of sockets in this host", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTotalSockets() {
		return totalSockets;
	}
	
	/**
	 * The numnber of cores per socket
	 * @return the coresPerSocket
	 */
	@JMXAttribute(name="CoresPerSocket", description="The numnber of cores per socket", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getCoresPerSocket() {
		return coresPerSocket;
	}
	
	/**
	 * Returns the percentage User CPU utilization for this CPU
	 * @return the percentage User CPU utilization
	 */
	@JMXAttribute(name="UserCPUPercent", description="The percentage User CPU utilization for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getUserCPUPercent() {
		return userPercentCounter.getLastValue();
	}

	/**
	 * Returns the percentage Sys CPU utilization for this CPU
	 * @return the percentage Sys CPU utilization
	 */
	@JMXAttribute(name="SysCPUPercent", description="The percentage Sys CPU utilization for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getSysCPUPercent() {
		return sysPercentCounter.getLastValue();
	}

	/**
	 * Returns the percentage Nice CPU utilization for this CPU
	 * @return the percentage Nice CPU utilization
	 */
	@JMXAttribute(name="NiceCPUPercent", description="The percentage Nice CPU utilization for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getNiceCPUPercent() {
		return nicePercentCounter.getLastValue();
	}

	/**
	 * Returns the percentage Idle CPU utilization for this CPU
	 * @return the percentage Idle CPU utilization
	 */
	@JMXAttribute(name="IdleCPUPercent", description="The percentage Idle CPU utilization for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getIdleCPUPercent() {
		return idlePercentCounter.getLastValue();
	}

	/**
	 * Returns the percentage Wait CPU utilization for this CPU
	 * @return the percentage Wait CPU utilization
	 */
	@JMXAttribute(name="WaitCPUPercent", description="The percentage Wait CPU utilization for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getWaitCPUPercent() {
		return waitPercentCounter.getLastValue();
	}

	/**
	 * Returns the percentage Irq CPU utilization for this CPU
	 * @return the percentage Irq CPU utilization
	 */
	@JMXAttribute(name="IrqCPUPercent", description="The percentage Irq CPU utilization for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getIrqCPUPercent() {
		return irqPercentCounter.getLastValue();
	}

	/**
	 * Returns the percentage SoftIrq CPU utilization for this CPU
	 * @return the percentage SoftIrq CPU utilization
	 */
	@JMXAttribute(name="SoftIrqCPUPercent", description="The percentage SoftIrq CPU utilization for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getSoftIrqCPUPercent() {
		return softIrqPercentCounter.getLastValue();
	}

	/**
	 * Returns the percentage Stolen CPU utilization for this CPU
	 * @return the percentage Stolen CPU utilization
	 */
	@JMXAttribute(name="StolenCPUPercent", description="The percentage Stolen CPU utilization for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getStolenCPUPercent() {
		return stolenPercentCounter.getLastValue();
	}

	/**
	 * Returns the percentage Combined CPU utilization for this CPU
	 * @return the percentage Combined CPU utilization
	 */
	@JMXAttribute(name="CombinedCPUPercent", description="The percentage Combined CPU utilization for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getCombinedCPUPercent() {
		return combinedPercentCounter.getLastValue();
	}

	/**
	 * Returns the percentage Total CPU utilization rate for this CPU
	 * @return the Total CPU utilization rate 
	 */
	@JMXAttribute(name="TotalCPURate", description="The Total CPU utilization rate for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTotalCPURate() {
		return totalRateCounter.getLastValue();
	}

	/**
	 * Returns the percentage User CPU utilization rate for this CPU
	 * @return the User CPU utilization rate 
	 */
	@JMXAttribute(name="UserCPURate", description="The User CPU utilization rate for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getUserCPURate() {
		return userRateCounter.getLastValue();
	}

	/**
	 * Returns the percentage Sys CPU utilization rate for this CPU
	 * @return the Sys CPU utilization rate 
	 */
	@JMXAttribute(name="SysCPURate", description="The Sys CPU utilization rate for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSysCPURate() {
		return sysRateCounter.getLastValue();
	}

	/**
	 * Returns the percentage Nice CPU utilization rate for this CPU
	 * @return the Nice CPU utilization rate 
	 */
	@JMXAttribute(name="NiceCPURate", description="The Nice CPU utilization rate for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getNiceCPURate() {
		return niceRateCounter.getLastValue();
	}

	/**
	 * Returns the percentage Idle CPU utilization rate for this CPU
	 * @return the Idle CPU utilization rate 
	 */
	@JMXAttribute(name="IdleCPURate", description="The Idle CPU utilization rate for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getIdleCPURate() {
		return idleRateCounter.getLastValue();
	}

	/**
	 * Returns the percentage Wait CPU utilization rate for this CPU
	 * @return the Wait CPU utilization rate 
	 */
	@JMXAttribute(name="WaitCPURate", description="The Wait CPU utilization rate for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getWaitCPURate() {
		return waitRateCounter.getLastValue();
	}

	/**
	 * Returns the percentage Irq CPU utilization rate for this CPU
	 * @return the Irq CPU utilization rate 
	 */
	@JMXAttribute(name="IrqCPURate", description="The Irq CPU utilization rate for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getIrqCPURate() {
		return irqRateCounter.getLastValue();
	}

	/**
	 * Returns the percentage SoftIrq CPU utilization rate for this CPU
	 * @return the SoftIrq CPU utilization rate 
	 */
	@JMXAttribute(name="SoftIrqCPURate", description="The SoftIrq CPU utilization rate for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSoftIrqCPURate() {
		return softIrqRateCounter.getLastValue();
	}

	/**
	 * Returns the percentage Stolen CPU utilization rate for this CPU
	 * @return the Stolen CPU utilization rate 
	 */
	@JMXAttribute(name="StolenCPURate", description="The Stolen CPU utilization rate for this CPU", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getStolenCPURate() {
		return stolenRateCounter.getLastValue();
	}

	
	
	/**
	 * Bootsraps this service
	 */
	public static void boot() {		
		try {
			for(int i = 0; i < HeliosSigar.getInstance().getCpuInfoList().length; i++) {
				new CPUUtilizationService(i);
			}
		} catch (SigarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
