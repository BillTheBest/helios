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
import java.util.Stack;
import java.util.concurrent.TimeUnit;


/**
 * <p>Title: ThreadInfoCapture</p>
 * <p>Description: A container class for captured metrics representing a thread's CPU, Block and Wait stats.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ThreadInfoCapture {
	public static final int CPU      = 1 << 0;  
	public static final int WAIT      = 1 << 1; 
	public static final int BLOCK    = 1 << 2;  

	
	protected static boolean contEnabled = false;
	protected static boolean cpuEnabled = false;
	protected static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	
	protected int metricOption = CPU+WAIT+BLOCK;
	protected long elapsedTime = 0;
	protected long totalCpuTime = -1;
	protected long blockedCount = -1;
	protected long blockedTime = -1;
	protected long waitCount = -1;
	protected long waitTime = -1;
	protected boolean nanoTime = true;
	
	protected static ThreadLocal<Stack<ThreadInfoCapture>> threadInfoLocal = new ThreadLocal<Stack<ThreadInfoCapture>>() {
		protected synchronized Stack<ThreadInfoCapture> initialValue() {
            return new Stack<ThreadInfoCapture>();
        }
	};
	
 
	
	protected ThreadInfoCapture() {}
	
	static {
		if(threadMXBean.isCurrentThreadCpuTimeSupported()) {			
			cpuEnabled = threadMXBean.isThreadCpuTimeEnabled();
		}
		if(threadMXBean.isThreadContentionMonitoringSupported()) {
			contEnabled  = threadMXBean.isThreadContentionMonitoringEnabled();
		}
	}
	
	/**
	 * Starts a ThreadInfo snapshot.
	 * By default captures all enabled metrics and ms. elapsed time.
	 */
	public static void start() {		
		start(true);
	}
	
 	
	/**
	 * Starts a ThreadInfo snapshot.
	 * By default captures all enabled metrics.
	 * @param nanoTime If true, elapsed time will be captured in nanos. If false, ms.
	 */
	public static void start(boolean nanoTime) {			
		threadInfoLocal.get().push(new ThreadInfoCapture(CPU+WAIT+BLOCK));
	}
	
	public static ThreadInfoCapture emptyTic() {
		return new ThreadInfoCapture();
	}
	
	
	/**
	 * Starts a ThreadInfo snapshot.
	 * @param options Mask of options. ints are CPU, WAIT and BLOCK.
	 * eg. start(CPU+WAIT) 
	 */
	public static void start(int options) {
		start(options, false);
	}
	
	/**
	 * Starts a ThreadInfo snapshot.
	 * @param options Mask of options. ints are CPU, WAIT and BLOCK. eg. start(CPU+WAIT) 
	 * @param nanoTime If true, elapsed time will be captured in nanos. If false, ms.
	 */
	public static void start(int options, boolean nanoTime) {
		threadInfoLocal.get().push(new ThreadInfoCapture(options, nanoTime));
	}
	
	
	
	/**
	 * Ends a ThreadInfo snapshot and returns a ThreadInfoCapture containing the delta values.
	 * @return A ThreadInfoCapture or null if the starting snapshot was not found.
	 */
	public static ThreadInfoCapture end() {
		if(threadInfoLocal.get().isEmpty()) return null;
		ThreadInfoCapture snapshot = threadInfoLocal.get().pop();
		int metricOption = snapshot.metricOption;
		ThreadInfoCapture tic = new ThreadInfoCapture(metricOption);
		tic.nanoTime = snapshot.nanoTime;
		tic.elapsedTime = (snapshot.nanoTime ? System.nanoTime() : System.currentTimeMillis()) - snapshot.elapsedTime;
		ThreadInfo threadInfo = threadMXBean.getThreadInfo(Thread.currentThread().getId());		
		if(cpuEnabled && ((metricOption & CPU) == CPU)) {
			tic.totalCpuTime = threadMXBean.getCurrentThreadCpuTime()-snapshot.totalCpuTime;
		}
		if(contEnabled) {
			if(((metricOption & BLOCK) == BLOCK)) {
				tic.blockedCount = threadInfo.getBlockedCount()-snapshot.blockedCount;
				tic.blockedTime = threadInfo.getBlockedTime()-snapshot.blockedTime;
			}
			if(((metricOption & WAIT) == WAIT)) {
				tic.waitCount = threadInfo.getWaitedCount()-snapshot.waitCount;
				tic.waitTime = threadInfo.getWaitedTime()-snapshot.waitTime;
			}
		}				
		return tic;
	}
	
	/**
	 * Creates a new ThreadInfoCapture snapshot and populates it.
	 * @param options Mask for collection options.
	 */
	protected ThreadInfoCapture(int options) {
		this(options, false);
	}
	
	/**
	 * Creates a new ThreadInfoCapture snapshot and populates it.
	 * @param options Mask for collection options.
	 * @param nanoTime if true, elapsed time will be in nanos. If false, it will be in ms.
	 */
	protected ThreadInfoCapture(int options, boolean nanoTime) {
		this.nanoTime = nanoTime;
		ThreadInfo threadInfo = threadMXBean.getThreadInfo(Thread.currentThread().getId());		
		metricOption = options;
		
		elapsedTime = nanoTime ? System.nanoTime() : System.currentTimeMillis();
		if(metricOption<1) return;
		if(cpuEnabled && ((metricOption & CPU) == CPU)) {
			totalCpuTime = threadMXBean.getCurrentThreadCpuTime();
		}
		if(contEnabled) {
			if(((metricOption & BLOCK) == BLOCK)) {
				blockedCount = threadInfo.getBlockedCount();
				blockedTime = threadInfo.getBlockedTime();
			}
			if(((metricOption & WAIT) == WAIT)) {
				waitCount = threadInfo.getWaitedCount();
				waitTime = threadInfo.getWaitedTime();
			}
		}		
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
	    
	    retValue.append("ThreadInfoCapture ( ")	        
	        .append("metricOption = ").append(this.metricOption).append(TAB)
	        .append("elapsedTime(ns) = ").append(nanoTime ? elapsedTime : TimeUnit.NANOSECONDS.convert(elapsedTime, TimeUnit.MILLISECONDS)).append(TAB)
	        .append("elapsedTime(ms) = ").append(nanoTime ? TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) : elapsedTime).append(TAB)
	        .append("totalCpuTime = ").append(this.totalCpuTime).append(TAB)
	        .append("blockedCount = ").append(this.blockedCount).append(TAB)
	        .append("blockedTime = ").append(this.blockedTime).append(TAB)
	        .append("waitCount = ").append(this.waitCount).append(TAB)
	        .append("waitTime = ").append(this.waitTime).append(TAB)
	        .append("nanoTime = ").append(this.nanoTime).append(TAB)
	        .append(" )");
	    
	    return retValue.toString();
	}


	/**
	 * @return the metricOption
	 */
	public int getMetricOption() {
		return metricOption;
	}


	/**
	 * @return the elapsedTime
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}


	/**
	 * @return the blockedCount
	 */
	public long getBlockedCount() {
		return blockedCount;
	}


	/**
	 * @return the blockedTime
	 */
	public long getBlockedTime() {
		return blockedTime;
	}


	/**
	 * @return the waitCount
	 */
	public long getWaitCount() {
		return waitCount;
	}


	/**
	 * @return the waitTime
	 */
	public long getWaitTime() {
		return waitTime;
	}


	/**
	 * @return the nanoTime
	 */
	public boolean isNanoTime() {
		return nanoTime;
	}


	/**
	 * @return the totalCpuTime
	 */
	public long getTotalCpuTime() {
		return totalCpuTime;
	}

}
