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
package org.helios.jmx.threadservices.instrumentation;

import static org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture.BLOCK;
import static org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture.CPU;
import static org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture.WAIT;

import java.util.concurrent.atomic.AtomicLong;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: TaskThreadInfoManager</p>
 * <p>Description: A named instance of a thread stats manager. 
 * </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(annotated=true, declared=true)
public class TaskThreadInfoManager {
	protected int collectionOption = BLOCK + CPU + WAIT;
	protected boolean timeInNanos = false;
	
	
	/** The name of the manager */
	protected String name = null;
	
	/** the total number of times the manager has been called */
	protected AtomicLong executionCount = new AtomicLong(0);
	
	/** the total execution time of the task */
	protected AtomicLong totalExecutionTime = new AtomicLong(0);
	/** the total cpu time of the task */
	protected AtomicLong totalCpuTime = new AtomicLong(0);
	/** the total block time of the task */
	protected AtomicLong totalBlockTime = new AtomicLong(0);
	/** the total wait time of the task */
	protected AtomicLong totalWaitTime = new AtomicLong(0);

	/** the last execution time of the task */
	protected AtomicLong lastExecutionTime = new AtomicLong(0);
	/** the last cpu time of the task */
	protected AtomicLong lastCpuTime = new AtomicLong(0);
	/** the last block time of the task */
	protected AtomicLong lastBlockTime = new AtomicLong(0);
	/** the last wait time of the task */
	protected AtomicLong lastWaitTime = new AtomicLong(0);
	
	/** indicates if the manager is enabled or disabled */
	protected boolean enabled = true;
	
	/** the elapsed time unit */
	protected String timeUnit = null;
	

	/**
	 * Creates a new TaskThreadInfoManager. Defaults instrumentation to 0, timeInNanos to false and enabled to true.
	 * @param name The context specific but arbitrary name for the task this manager represents.
	 */
	public TaskThreadInfoManager(String name) {
		this(name, CPU+BLOCK+WAIT, false, true);		
	}
	
	/**
	 * Creates a new TaskThreadInfoManager. 
	 * @param name The name of the task.
	 * @param instrumentationOption The instrumentation option. (eg. CPU+BLOCK)
	 * @param timeInNanos Reports elapsed times in ns. if true. Otherwise reports in ms.
	 * @param enabled Immediatelly enables the task manager if true.
	 */
	public TaskThreadInfoManager(String name, int instrumentationOption, boolean timeInNanos, boolean enabled) {
		super();
		this.name = name;
		this.collectionOption = instrumentationOption;
		this.timeInNanos = timeInNanos;
		this.enabled = enabled;
		timeUnit = (timeInNanos ? " (ns.)" : " (ms.)");
	}
	
	
	/**
	 * Starts a thread collection.
	 */
	public void start() {
		if(enabled) {
			ThreadInfoCapture.start(collectionOption, timeInNanos);
		}
	}
	
	/**
	 * Ends a thread collection.
	 */
	public void end() {
		if(enabled) {
			processUpdates(ThreadInfoCapture.end());
		}
	}
	
	/**
	 * Accumulates the results.
	 * @param tic The tic returned from end()
	 */
	protected void processUpdates(ThreadInfoCapture tic) {
		executionCount.incrementAndGet();
		totalExecutionTime.addAndGet(tic.elapsedTime);
		totalCpuTime.addAndGet(tic.totalCpuTime);
		totalBlockTime.addAndGet(tic.blockedTime);
		totalWaitTime.addAndGet(tic.waitTime);
		lastExecutionTime.set(tic.elapsedTime);
		lastCpuTime.set(tic.totalCpuTime);
		lastBlockTime.set(tic.blockedTime);
		lastWaitTime.set(tic.waitTime);		
	}

	/**
	 * @return the collectionOption
	 */
	@JMXAttribute(name="{f:name}CollectionOption", description="The level of metrics to collect", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getCollectionOption() {
		return collectionOption;
	}

	/**
	 * @return the timeInNanos
	 */
	@JMXAttribute(name="{f:name}TimeInNanos", description="Defines if times are reported in ns. or ms.", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getTimeInNanos() {
		return timeInNanos;
	}

	/**
	 * @return the name
	 */
	@JMXAttribute(name="{f:name}Name", description="The name assigned to the task", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getName() {
		return name;
	}

	/**
	 * @return the executionCount
	 */
	@JMXAttribute(name="{f:name}ExecutionCount", description="The number of times the task has been executed.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getExecutionCount() {
		if(!enabled) return -1;
		return executionCount.get();
	}

	/**
	 * @return the totalExecutionTime
	 */
	@JMXAttribute(name="{f:name}TotalExecutionTime", description="The total execution time of the task", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTotalExecutionTime() {
		if(!enabled) return -1;
		return totalExecutionTime.get();
	}

	/**
	 * @return the totalCpuTime
	 */
	public long getTotalCpuTime() {
		if(!enabled) return -1;
		return totalCpuTime.get();
	}

	/**
	 * @return the totalBlockTime
	 */
	public long getTotalBlockTime() {
		if(!enabled) return -1;
		return totalBlockTime.get();
	}

	/**
	 * @return the totalWaitTime
	 */
	public long getTotalWaitTime() {
		if(!enabled) return -1;
		return totalWaitTime.get();
	}

	/**
	 * @return the lastExecutionTime
	 */
	@JMXAttribute(name="{f:name}LastExecutionTime", description="The last execution time of the task", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastExecutionTime() {
		if(!enabled) return -1;
		return lastExecutionTime.get();
	}

	/**
	 * @return the lastCpuTime
	 */
	@JMXAttribute(name="{f:name}LastCpuTime", description="The last cpu time of the task", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastCpuTime() {
		if(!enabled) return -1;
		return lastCpuTime.get();
	}

	/**
	 * @return the lastBlockTime
	 */
	@JMXAttribute(name="{f:name}LastBlockTime", description="The last block time of the task", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getLastBlockTime() {
		if(!enabled) return -1;
		return lastBlockTime.get();
	}

	/**
	 * @return the lastWaitTime
	 */
	@JMXAttribute(name="{f:name}LastWaitTime", description="The last wait time of the task", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastWaitTime() {
		if(!enabled) return -1;
		return lastWaitTime.get();
	}

	/**
	 * @return the enabled
	 */
	@JMXAttribute(name="{f:name}Enabled", description="If task measurement is enabled", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getEnabled() {
		return enabled;
	}
	
	/**
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled; 
	}
	
	/**
	 * Calcs an average
	 * @param time
	 * @param count
	 * @return
	 */
	protected static long avg(float time, float count) {
		if(time==0 || count==0) return 0;
		float f = time/count;
		return (long)f;
	}
	

	

	
	/**
	 * @return the averageExecutionTime
	 */
	@JMXAttribute(name="{f:name}AverageExecutionTime", description="The average execution time of the task", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getAverageExecutionTime() {
		if(!enabled) return -1;
		return avg(totalExecutionTime.get(), executionCount.get());
	}

	/**
	 * @return the averageCpuTime
	 */
	@JMXAttribute(name="{f:name}AverageCpuTime", description="The average cpu time of the task", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getAverageCpuTime() {
		if(!enabled) return -1;
		return avg(totalCpuTime.get(), executionCount.get());
	}

	/**
	 * @return the averageBlockTime
	 */
	@JMXAttribute(name="{f:name}AverageBlockTime", description="The average block time of the task", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageBlockTime() {
		if(!enabled) return -1;
		return avg(totalBlockTime.get(), executionCount.get());
	}

	/**
	 * @return the averageWaitTime
	 */
	@JMXAttribute(name="{f:name}AverageWaitTime", description="The average wait time of the task", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getAverageWaitTime() {
		if(!enabled) return -1;
		return avg(totalWaitTime.get(), executionCount.get());
	}
	
	/**
	 * @return the averageStoppedTime
	 */
	@JMXAttribute(name="{f:name}AverageStoppedTime", description="The average stopped (wait+block) time of the task", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getAverageStoppedTime() {
		if(!enabled) return -1;
		return avg(totalWaitTime.get() + totalBlockTime.get(), executionCount.get());
	}
}
