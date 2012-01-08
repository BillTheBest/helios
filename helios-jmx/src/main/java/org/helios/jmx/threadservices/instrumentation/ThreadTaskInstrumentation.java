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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * <p>Title: ThreadTaskInstrumentation</p>
 * <p>Description: A container and helper class for capturing resource averageBasedUtilization and timings for thread pool executed tasks.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ThreadTaskInstrumentation {
	/** A refrence to the ThreadMXBean */
	public static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	/** The number of CPUs reported by the JVM's OperatingSystemMXBean */
	public static final int cpuCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** Indicates if CPU Timings are accessible */
	public static boolean cpuTimesAvailable = false;
	/** Indicates if Thread contention monitoring is accessible */
	protected static boolean contentionTimesAvailable = false;
	/** elapsed nanoseconds for the thread task */
	protected long elapsedTime = 0L;
	/** elapsed cpu time in nanoseconds for the thread task */
	protected long elapsedCpuTime = 0L;
	/** elapsed wait time for the thread task */
	protected long waitTime = 0L;
	/** elapsed block time for the thread task */
	protected long blockTime = 0L;	
	/** Indicates if the tti is closed */
	protected boolean closed = false;
		
	
	/**
	 * Copy Constructor
	 *
	 * @param threadTaskInstrumentation a <code>ThreadTaskInstrumentation</code> object
	 */
	public ThreadTaskInstrumentation(ThreadTaskInstrumentation threadTaskInstrumentation) 
	{
	    this.elapsedTime = threadTaskInstrumentation.elapsedTime;
	    this.elapsedCpuTime = threadTaskInstrumentation.elapsedCpuTime;
	    this.waitTime = threadTaskInstrumentation.waitTime;
	    this.blockTime = threadTaskInstrumentation.blockTime;
	    this.closed = threadTaskInstrumentation.closed;
	}

	public ThreadTaskInstrumentation() {
		collect();
	}
	
	public ThreadTaskInstrumentation close() {
		ThreadTaskInstrumentation tti = new ThreadTaskInstrumentation();
		elapsedTime = tti.getElapsedTime() - elapsedTime;
		elapsedCpuTime = tti.getElapsedCpuTime() - elapsedCpuTime;
		waitTime = tti.getWaitTime() - waitTime;
		blockTime = tti.getBlockTime() - blockTime;
		closed = true;
		return new ThreadTaskInstrumentation(this);
	}
	
	protected void collect() {
		elapsedTime = System.nanoTime();		
		if(cpuTimesAvailable) {
			elapsedCpuTime = threadMXBean.getThreadCpuTime(Thread.currentThread().getId());
		}
		if(contentionTimesAvailable) {
			ThreadInfo threadInfo = threadMXBean.getThreadInfo(Thread.currentThread().getId());
			waitTime = threadInfo.getWaitedTime();
			blockTime = threadInfo.getBlockedTime();
		}		
	}
	
	
	static {
		if(ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported()) {
			if(!ManagementFactory.getThreadMXBean().isThreadCpuTimeEnabled()) {
				try {
					ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
					cpuTimesAvailable = true;
				} catch (Throwable t) {
					cpuTimesAvailable = false;
				}
			} else {
				cpuTimesAvailable = true;
			}
		}
		if(ManagementFactory.getThreadMXBean().isThreadContentionMonitoringSupported()) {
			if(!ManagementFactory.getThreadMXBean().isThreadContentionMonitoringEnabled()) {
				try {
					ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(true);
					contentionTimesAvailable = true;
				} catch (Throwable t) {
					contentionTimesAvailable = false;
				}
			} else {
				contentionTimesAvailable = true;
			}
		}		
	}


	/**
	 * @return the cpuTimesAvailable
	 */
	public static boolean isCpuTimesAvailable() {
		return cpuTimesAvailable;
	}

	/**
	 * @return the contentionTimesAvailable
	 */
	public static boolean isContentionTimesAvailable() {
		return contentionTimesAvailable;
	}

	/**
	 * @return the elapsedTime
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * @return the elapsedCpuTime
	 */
	public long getElapsedCpuTime() {
		return elapsedCpuTime;
	}

	/**
	 * @return the waitTime
	 */
	public long getWaitTime() {
		return waitTime;
	}

	/**
	 * @return the blockTime
	 */
	public long getBlockTime() {
		return blockTime;
	}

	/**
	 * @return the closed
	 */
	public boolean isClosed() {
		return closed;
	}

	public void reset() {
		collect();
		closed = false;
		
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	
	    StringBuilder retValue = new StringBuilder();
	    
	    retValue.append("ThreadTaskInstrumentation ( ")
	        .append("elapsedTime = ").append(this.elapsedTime).append(TAB)
	        .append("elapsedCpuTime = ").append(this.elapsedCpuTime).append(TAB)
	        .append("waitTime = ").append(this.waitTime).append(TAB)
	        .append("blockTime = ").append(this.blockTime).append(TAB)
	        .append("closed = ").append(this.closed).append(TAB)
	        .append(" )");
	    
	    return retValue.toString();
	}

}
