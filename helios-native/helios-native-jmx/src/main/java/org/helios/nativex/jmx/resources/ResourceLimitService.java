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
package org.helios.nativex.jmx.resources;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.ResourceLimit;

/**
 * <p>Title: ResourceLimitService</p>
 * <p>Description: System resource limit monitor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.resources.jmx.ResourceLimitService</code></p>
 */

public class ResourceLimitService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = -5558677184561321819L;
	/** The resource limit stats gatherer */
	protected final ResourceLimit resourceLimit;
	
	/** The CpuCur current system utilization */
	protected final LongRollingCounter cpuCurCounter = new LongRollingCounter("ResourceCpuCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The CpuMax system resource limit */
	protected final LongRollingCounter cpuMaxCounter = new LongRollingCounter("ResourceCpuMaxLimit", 1);
	/** The FileSizeCur current system utilization */
	protected final LongRollingCounter fileSizeCurCounter = new LongRollingCounter("ResourceFileSizeCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The FileSizeMax system resource limit */
	protected final LongRollingCounter fileSizeMaxCounter = new LongRollingCounter("ResourceFileSizeMaxLimit", 1);
	/** The PipeSizeMax system resource limit */
	protected final LongRollingCounter pipeSizeMaxCounter = new LongRollingCounter("ResourcePipeSizeMaxLimit", 1);
	/** The PipeSizeCur current system utilization */
	protected final LongRollingCounter pipeSizeCurCounter = new LongRollingCounter("ResourcePipeSizeCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The DataCur current system utilization */
	protected final LongRollingCounter dataCurCounter = new LongRollingCounter("ResourceDataCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The DataMax system resource limit */
	protected final LongRollingCounter dataMaxCounter = new LongRollingCounter("ResourceDataMaxLimit", 1);
	/** The StackCur current system utilization */
	protected final LongRollingCounter stackCurCounter = new LongRollingCounter("ResourceStackCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The StackMax system resource limit */
	protected final LongRollingCounter stackMaxCounter = new LongRollingCounter("ResourceStackMaxLimit", 1);
	/** The CoreCur current system utilization */
	protected final LongRollingCounter coreCurCounter = new LongRollingCounter("ResourceCoreCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The CoreMax system resource limit */
	protected final LongRollingCounter coreMaxCounter = new LongRollingCounter("ResourceCoreMaxLimit", 1);
	/** The MemoryCur current system utilization */
	protected final LongRollingCounter memoryCurCounter = new LongRollingCounter("ResourceMemoryCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The MemoryMax system resource limit */
	protected final LongRollingCounter memoryMaxCounter = new LongRollingCounter("ResourceMemoryMaxLimit", 1);
	/** The ProcessesCur current system utilization */
	protected final LongRollingCounter processesCurCounter = new LongRollingCounter("ResourceProcessesCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The ProcessesMax system resource limit */
	protected final LongRollingCounter processesMaxCounter = new LongRollingCounter("ResourceProcessesMaxLimit", 1);
	/** The OpenFilesCur current system utilization */
	protected final LongRollingCounter openFilesCurCounter = new LongRollingCounter("ResourceOpenFilesCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The OpenFilesMax system resource limit */
	protected final LongRollingCounter openFilesMaxCounter = new LongRollingCounter("ResourceOpenFilesMaxLimit", 1);
	/** The VirtualMemoryCur current system utilization */
	protected final LongRollingCounter virtualMemoryCurCounter = new LongRollingCounter("ResourceVirtualMemoryCurCurrentCounter", DEFAULT_ROLLING_SIZE);
	/** The VirtualMemoryMax system resource limit */
	protected final LongRollingCounter virtualMemoryMaxCounter = new LongRollingCounter("ResourceVirtualMemoryMaxLimit", 1);
	

	/**
	 * Creates a new ResourceLimitService
	 */
	public ResourceLimitService() {
		super();
		resourceLimit = HeliosSigar.getInstance().getResourceLimit();
		this.scheduleSampling();
		registerCounterMBean("service", "ResourceLimits");		
	}	
	
	/**
	 * Gathers and increments resource limit stats
	 */
	@Override
	public void run() {
		try {
			resourceLimit.gather(sigar);
			cpuCurCounter.put(resourceLimit.getCpuCur());
			cpuMaxCounter.put(resourceLimit.getCpuMax());
			fileSizeCurCounter.put(resourceLimit.getFileSizeCur());
			fileSizeMaxCounter.put(resourceLimit.getFileSizeMax());
			pipeSizeMaxCounter.put(resourceLimit.getPipeSizeMax());
			pipeSizeCurCounter.put(resourceLimit.getPipeSizeCur());
			dataCurCounter.put(resourceLimit.getDataCur());
			dataMaxCounter.put(resourceLimit.getDataMax());
			stackCurCounter.put(resourceLimit.getStackCur());
			stackMaxCounter.put(resourceLimit.getStackMax());
			coreCurCounter.put(resourceLimit.getCoreCur());
			coreMaxCounter.put(resourceLimit.getCoreMax());
			memoryCurCounter.put(resourceLimit.getMemoryCur());
			memoryMaxCounter.put(resourceLimit.getMemoryMax());
			processesCurCounter.put(resourceLimit.getProcessesCur());
			processesMaxCounter.put(resourceLimit.getProcessesMax());
			openFilesCurCounter.put(resourceLimit.getOpenFilesCur());
			openFilesMaxCounter.put(resourceLimit.getOpenFilesMax());
			virtualMemoryCurCounter.put(resourceLimit.getVirtualMemoryCur());
			virtualMemoryMaxCounter.put(resourceLimit.getVirtualMemoryMax());			
		} catch (Exception e) {
			log.error("Failed to gather on service [" + objectName + "]", e);
			throw new RuntimeException("Failed to gather on service [" + objectName + "]", e);
		}
	}	
	
	/**
	 * Bootstraps this service 
	 */
	public static void boot() {
		new ResourceLimitService();
	}
	
	/**
	 * Returns the current value for Cpu 
	 * @return the current value for Cpu
	 */
	@JMXAttribute(name="CpuCur", description="the current value for Cpu", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCpuCur() {
		return cpuCurCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for Cpu 
	 * @return The maximum value for Cpu
	 */
	@JMXAttribute(name="CpuMax", description="The maximum value for Cpu", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCpuMax() {
		return cpuMaxCounter.getLastValue();
	}

	/**
	 * Returns the current value for FileSize 
	 * @return the current value for FileSize
	 */
	@JMXAttribute(name="FileSizeCur", description="the current value for FileSize", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFileSizeCur() {
		return fileSizeCurCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for FileSize 
	 * @return The maximum value for FileSize
	 */
	@JMXAttribute(name="FileSizeMax", description="The maximum value for FileSize", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFileSizeMax() {
		return fileSizeMaxCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for PipeSize 
	 * @return The maximum value for PipeSize
	 */
	@JMXAttribute(name="PipeSizeMax", description="The maximum value for PipeSize", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getPipeSizeMax() {
		return pipeSizeMaxCounter.getLastValue();
	}

	/**
	 * Returns the current value for PipeSize 
	 * @return the current value for PipeSize
	 */
	@JMXAttribute(name="PipeSizeCur", description="the current value for PipeSize", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getPipeSizeCur() {
		return pipeSizeCurCounter.getLastValue();
	}

	/**
	 * Returns the current value for Data 
	 * @return the current value for Data
	 */
	@JMXAttribute(name="DataCur", description="the current value for Data", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getDataCur() {
		return dataCurCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for Data 
	 * @return The maximum value for Data
	 */
	@JMXAttribute(name="DataMax", description="The maximum value for Data", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getDataMax() {
		return dataMaxCounter.getLastValue();
	}

	/**
	 * Returns the current value for Stack 
	 * @return the current value for Stack
	 */
	@JMXAttribute(name="StackCur", description="the current value for Stack", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getStackCur() {
		return stackCurCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for Stack 
	 * @return The maximum value for Stack
	 */
	@JMXAttribute(name="StackMax", description="The maximum value for Stack", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getStackMax() {
		return stackMaxCounter.getLastValue();
	}

	/**
	 * Returns the current value for Core 
	 * @return the current value for Core
	 */
	@JMXAttribute(name="CoreCur", description="the current value for Core", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCoreCur() {
		return coreCurCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for Core 
	 * @return The maximum value for Core
	 */
	@JMXAttribute(name="CoreMax", description="The maximum value for Core", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCoreMax() {
		return coreMaxCounter.getLastValue();
	}

	/**
	 * Returns the current value for Memory 
	 * @return the current value for Memory
	 */
	@JMXAttribute(name="MemoryCur", description="the current value for Memory", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMemoryCur() {
		return memoryCurCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for Memory 
	 * @return The maximum value for Memory
	 */
	@JMXAttribute(name="MemoryMax", description="The maximum value for Memory", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMemoryMax() {
		return memoryMaxCounter.getLastValue();
	}

	/**
	 * Returns the current value for Processes 
	 * @return the current value for Processes
	 */
	@JMXAttribute(name="ProcessesCur", description="the current value for Processes", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getProcessesCur() {
		return processesCurCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for Processes 
	 * @return The maximum value for Processes
	 */
	@JMXAttribute(name="ProcessesMax", description="The maximum value for Processes", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getProcessesMax() {
		return processesMaxCounter.getLastValue();
	}

	/**
	 * Returns the current value for OpenFiles 
	 * @return the current value for OpenFiles
	 */
	@JMXAttribute(name="OpenFilesCur", description="the current value for OpenFiles", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getOpenFilesCur() {
		return openFilesCurCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for OpenFiles 
	 * @return The maximum value for OpenFiles
	 */
	@JMXAttribute(name="OpenFilesMax", description="The maximum value for OpenFiles", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getOpenFilesMax() {
		return openFilesMaxCounter.getLastValue();
	}

	/**
	 * Returns the current value for VirtualMemory 
	 * @return the current value for VirtualMemory
	 */
	@JMXAttribute(name="VirtualMemoryCur", description="the current value for VirtualMemory", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getVirtualMemoryCur() {
		return virtualMemoryCurCounter.getLastValue();
	}

	/**
	 * Returns The maximum value for VirtualMemory 
	 * @return The maximum value for VirtualMemory
	 */
	@JMXAttribute(name="VirtualMemoryMax", description="The maximum value for VirtualMemory", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getVirtualMemoryMax() {
		return virtualMemoryMaxCounter.getLastValue();
	}

	
}
