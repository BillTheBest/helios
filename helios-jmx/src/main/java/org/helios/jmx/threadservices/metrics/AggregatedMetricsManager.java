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
package org.helios.jmx.threadservices.metrics;

import java.util.Date;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: AggregatedMetricsManager</p>
 * <p>Description: Maintains an infinitely rolling aggregate of metrics supplied in closed ThreadResourceSnapshots.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject (annotated=true, declared=true)
public class AggregatedMetricsManager {
	protected long executions = 0;
	protected long successfulExecutions = 0;
	protected long failedExecutions = 0;
	protected boolean lastExecutionSuccessful = false;
	protected Date lastExecutionTimestamp = null;
	protected Date lastSuccessfulExecutionTimestamp = null;
	protected Date lastFailedExecutionTimestamp = null;
	
	protected long lastExecutionTime = 0;
	protected long lastCpuTime = 0;
	protected long lastStoppedTime = 0;
	protected long lastWaitTime = 0;
	protected long lastBlockTime = 0;
	protected long lastWaitCount = 0;
	protected long lastBlockCount = 0;
	protected int lastCpuPercent = 0;
	protected int lastStoppedPercent = 0;

	
	protected long averageExecutionTime = 0;	
	protected long averageCpuTime = 0;	
	protected long averageStoppedTime = 0;	
	protected long averageWaitTime = 0;	
	protected long averageBlockTime = 0;	
	protected long averageWaitCount = 0;	
	protected long averageBlockCount = 0;
	protected int averageCpuPercent = 0;	
	protected int averageStoppedPercent = 0;	

	
	
	protected boolean trackStatsOnFailed = false;
	
	/**
	 * Creates a new AggregatedMetricsManager
	 * @param trackStatsOnFailed If true, stats will aggregated for failed and successful executions. If false, only successful thread stats will be aggregated.
	 */
	public AggregatedMetricsManager(boolean trackStatsOnFailed) {
		this.trackStatsOnFailed = trackStatsOnFailed;
	}
	
	
	/**
	 * Updates the aggregated metrics and status.
	 * @param trs The ThreadResourceSnapshot to be aggregated into this instance of a AggregatedMetricsManager 
	 * @param successful True if the task was successful, false if it was not.
	 */
	public synchronized void update(ThreadResourceSnapshot trs, boolean successful) {
		lastExecutionTimestamp = new Date();
		executions++;
		if(successful) {
			successfulExecutions++;
			lastSuccessfulExecutionTimestamp = lastExecutionTimestamp;
		} else {
			failedExecutions++;
			lastFailedExecutionTimestamp = lastExecutionTimestamp;
		}
		if(!successful && !trackStatsOnFailed) return;
		
		lastExecutionTime = trs.getElapsedTimeMs();
		lastCpuTime = trs.getCpuTime();
		lastBlockTime = trs.getBlockTime();
		lastBlockCount = trs.getBlockCount();
		lastWaitTime = trs.getWaitTime();
		lastWaitCount = trs.getWaitCount();
		lastCpuPercent = trs.getPercentageCpu();
		lastStoppedPercent = trs.getPercentageStopped();
		lastStoppedTime = lastWaitTime + lastBlockTime;
		
		averageExecutionTime = calcAverage(averageExecutionTime,lastExecutionTime); 	
		averageCpuTime = calcAverage(averageCpuTime,lastCpuTime);
		averageCpuPercent = calcAverage(averageCpuPercent,lastCpuPercent);
		averageStoppedPercent = calcAverage(averageStoppedPercent,lastStoppedPercent);
		averageStoppedTime = calcAverage(averageStoppedTime,lastStoppedTime);
		averageWaitTime = calcAverage(averageWaitTime,lastWaitTime);	
		averageBlockTime = calcAverage(averageBlockTime,lastBlockTime);
		averageWaitCount = calcAverage(averageWaitCount,lastWaitCount);
		averageBlockCount = calcAverage(averageBlockCount,lastBlockCount);
		
		
	}
	
	protected long calcAverage(long rollingAverage, long last) {
		if(executions<2) return last;
		long tmp = last + rollingAverage;
		if(tmp==0) return 0;
		return tmp/2;
	}
	
	protected int calcAverage(int rollingAverage, int last) {
		if(executions<2) return last;
		int tmp = last + rollingAverage;
		if(tmp==0) return 0;
		return tmp/2;
	}
	
	
	/**
	 * @return the executions
	 */
	@JMXAttribute (name="Executions", description="The number of executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getExecutions() {
		return executions;
	}
	/**
	 * @return the successfulExecutions
	 */
	@JMXAttribute (name="SuccessfulExecutions", description="The number of successful executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getSuccessfulExecutions() {
		return successfulExecutions;
	}
	/**
	 * @return the failedExecutions
	 */
	@JMXAttribute (name="FailedExecutions", description="The number of failed executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getFailedExecutions() {
		return failedExecutions;
	}
	/**
	 * @return the lastExecutionSuccessful
	 */
	@JMXAttribute (name="LastExecutionSuccessful", description="Indicates if the last execution was successful", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized boolean getLastExecutionSuccessful() {
		return lastExecutionSuccessful;
	}
	/**
	 * @return the lastExecutionTimestamp
	 */
	@JMXAttribute (name="LastExecutionTimestamp", description="The timestamp of the last execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized Date getLastExecutionTimestamp() {
		return lastExecutionTimestamp;
	}
	
	/**
	 * @return the lastSuccessfulExecutionTimestamp
	 */
	@JMXAttribute (name="LastSuccessfulExecutionTimestamp", description="The timestamp of the last successful execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized Date getLastSuccessfulExecutionTimestamp() {
		return lastSuccessfulExecutionTimestamp;
	}

	/**
	 * @return the lastFailedExecutionTimestamp
	 */
	@JMXAttribute (name="LastFailedExecutionTimestamp", description="The timestamp of the last failed execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized Date getLastFailedExecutionTimestamp() {
		return lastFailedExecutionTimestamp;
	}
	
	/**
	 * @return the averageExecutionTime
	 */
	@JMXAttribute (name="AverageExecutionTime", description="The average elapsed time of executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getAverageExecutionTime() {
		return averageExecutionTime;
	}
	/**
	 * @return the lastExecutionTime
	 */
	@JMXAttribute (name="LastExecutionTime", description="The last elapsed time of an execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getLastExecutionTime() {
		return lastExecutionTime;
	}
	/**
	 * @return the averageCpuTime
	 */
	@JMXAttribute (name="AverageCpuTime", description="The average cpu time of executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getAverageCpuTime() {
		return averageCpuTime;
	}
	/**
	 * @return the lastCpuTime
	 */
	@JMXAttribute (name="LastCpuTime", description="The last cpu time of an execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getLastCpuTime() {
		return lastCpuTime;
	}
	/**
	 * @return the averageCpuPercent
	 */
	@JMXAttribute (name="AverageCpuPercent", description="The average cpu usage percentage of executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized int getAverageCpuPercent() {
		return averageCpuPercent;
	}
	/**
	 * @return the lastCpuPercent
	 */
	@JMXAttribute (name="LastCpuPercent", description="The last cpu usage percentage of an execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized int getLastCpuPercent() {
		return lastCpuPercent;
	}
	/**
	 * @return the averageStoppedPercent
	 */
	@JMXAttribute (name="AverageStoppedPercent", description="The average stopped time percentage during executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized int getAverageStoppedPercent() {
		return averageStoppedPercent;
	}
	/**
	 * @return the lastStoppedPercent
	 */
	@JMXAttribute (name="LastStoppedPercent", description="The last stopped time percentage during an execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized int getLastStoppedPercent() {
		return lastStoppedPercent;
	}
	/**
	 * @return the averageStoppedTime
	 */
	@JMXAttribute (name="AverageStoppedTime", description="The average stopped time during executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getAverageStoppedTime() {
		return averageStoppedTime;
	}
	/**
	 * @return the lastStoppedTime
	 */
	@JMXAttribute (name="LastStoppedTime", description="The last stopped time during an execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getLastStoppedTime() {
		return lastStoppedTime;
	}
	/**
	 * @return the averageWaitTime
	 */
	@JMXAttribute (name="AverageWaitTime", description="The average wait time during executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getAverageWaitTime() {
		return averageWaitTime;
	}
	/**
	 * @return the lastWaitTime
	 */
	@JMXAttribute (name="LastWaitTime", description="The total wait time of the last execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getLastWaitTime() {
		return lastWaitTime;
	}
	/**
	 * @return the averageBlockTime
	 */
	@JMXAttribute (name="AverageBlockTime", description="The average block time during executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getAverageBlockTime() {
		return averageBlockTime;
	}
	/**
	 * @return the lastBlockTime
	 */
	@JMXAttribute (name="LastBlockTime", description="The total block time of the last execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getLastBlockTime() {
		return lastBlockTime;
	}
	/**
	 * @return the averageWaitCount
	 */
	@JMXAttribute (name="AverageWaitCount", description="The average wait count during executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getAverageWaitCount() {
		return averageWaitCount;
	}
	/**
	 * @return the lastWaitCount
	 */
	@JMXAttribute (name="LastWaitCount", description="The total wait count of the last execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getLastWaitCount() {
		return lastWaitCount;
	}
	/**
	 * @return the averageBlockCount
	 */
	@JMXAttribute (name="AverageBlockTime", description="The average block count during executions", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getAverageBlockCount() {
		return averageBlockCount;
	}
	/**
	 * @return the lastBlockCount
	 */
	@JMXAttribute (name="LastBlockCount", description="The total block count of the last execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized long getLastBlockCount() {
		return lastBlockCount;
	}

	
	
	
	
}
