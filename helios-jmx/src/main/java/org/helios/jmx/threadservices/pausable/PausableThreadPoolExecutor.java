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
package org.helios.jmx.threadservices.pausable;

import java.lang.ref.SoftReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.instrumentation.ThreadTaskInstrumentation;
/**
 * <p>
 * Title: PausableThreadPoolExecutor
 * </p>
 * <p>
 * Description: A pausable ThreadPoolExecutor
 * </p>
 * <p>
 * Company: Helios Development Group
 * </p>
 * 
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$ $HeadURL$ $Id$
 */
@JMXManagedObject(declared=true, annotated=true)
public class PausableThreadPoolExecutor extends ThreadPoolExecutor implements RejectedExecutionHandler, Thread.UncaughtExceptionHandler, PausablePool {
	private boolean isPaused;
	private ReentrantLock pauseLock = new ReentrantLock();
	private Condition unpaused = pauseLock.newCondition();
	protected AtomicInteger runningTasks = new AtomicInteger(0);
	protected AtomicLong runTasks = new AtomicLong(0);
	protected AtomicLong rejectedTasks = new AtomicLong(0);
	protected AtomicLong failedTasks = new AtomicLong(0);
	protected AtomicBoolean instrument = new AtomicBoolean(false);
	protected RejectedExecutionHandler handler = null;
	protected ThreadLocal<ThreadTaskInstrumentation> instrumentation = new ThreadLocal<ThreadTaskInstrumentation>();
	protected ReentrantLock statsLock = new ReentrantLock();
	protected long totalElapsedTime = 0L;
	protected long totalCpuTime = 0L;
	protected long totalWaitTime = 0L;
	protected long totalBlockTime = 0L;
	protected long averageElapsedTime = 0L;
	protected long averageCpuTime = 0L;
	protected long averageWaitTime = 0L;
	protected long averageBlockTime = 0L;
	protected int totalBasedUtilization = 0;
	protected int averageBasedUtilization = 0;
	protected AtomicLong statsUpdateCount = new AtomicLong(0);
	protected AtomicLong statsUpdateTotalTime = new AtomicLong(0);
	protected AtomicLong averageStatsUpdateTime = new AtomicLong(0);
	
	/**
	 * Updates the accumulating instrumentation statistics based on the passed ThreadTaskInstrumentation. 
	 * @param tti A ThreadTaskInstrumentation generated from an executed task.
	 */
	protected void updateStats(ThreadTaskInstrumentation tti) {
		long start = System.currentTimeMillis();
		statsLock.lock();
		try {
			long t = runTasks.incrementAndGet();
			totalElapsedTime += tti.getElapsedTime();
			if(ThreadTaskInstrumentation.isCpuTimesAvailable()) {
				totalCpuTime += tti.getElapsedCpuTime();
				averageCpuTime = calcAvg(totalCpuTime,t);
			}
			if(ThreadTaskInstrumentation.isContentionTimesAvailable()) {
				totalWaitTime += tti.getWaitTime();
				totalBlockTime += tti.getBlockTime();
				averageWaitTime = calcAvg(totalWaitTime,t);
				averageBlockTime = calcAvg(totalBlockTime,t);				
			}
			averageElapsedTime = calcAvg(totalElapsedTime,t); 
			if(ThreadTaskInstrumentation.isContentionTimesAvailable() && ThreadTaskInstrumentation.isCpuTimesAvailable()) {
				averageBasedUtilization = (int)(((float)(float)averageCpuTime/(float)(averageElapsedTime-(averageWaitTime+averageBlockTime)))*100);
				totalBasedUtilization = (int)(((float)(float)totalCpuTime/(float)(totalElapsedTime-(totalWaitTime+totalBlockTime)))*100);
			}			
		} finally {
			long elapsed = System.currentTimeMillis()-start;
			averageStatsUpdateTime.set(calcAvg(statsUpdateTotalTime.addAndGet(elapsed), statsUpdateCount.incrementAndGet()));
			statsLock.unlock();
		}
	}
	
	/**
	 * Resets the accumulated instrumentation and task counts.
	 */
	@JMXOperation (name="resetInstrumentation", description="Resets the accumulated instrumentation and task counts")
	public void resetInstrumentation() {
		statsLock.lock();
		try {
			totalElapsedTime = 0L;
			totalCpuTime = 0L;
			totalWaitTime = 0L;
			totalBlockTime = 0L;
			averageElapsedTime = 0L;
			averageCpuTime = 0L;
			averageWaitTime = 0L;
			averageBlockTime = 0L;			
			averageBasedUtilization = 0;	
			totalBasedUtilization = 0;
			averageStatsUpdateTime.set(0);
			statsUpdateTotalTime.set(0);
			statsUpdateCount.set(0);
			runTasks.set(0);
			rejectedTasks.set(0);
			failedTasks.set(0);					
		} finally {
			statsLock.unlock();
		}		
	}
	
	/**
	 * Calculates an average.
	 * @param total The accumulated value of events.
	 * @param count The total number of events.
	 * @return The average.
	 */
	protected long calcAvg(long total, long count) {
		if(total==0 || count==0) return 0;
		float f = total/count;
		return (long)f;
	}

	/**
	 * @return the handler
	 */
	public RejectedExecutionHandler getHandler() {
		return handler;
	}

	/**
	 * @param handler the handler to set
	 */
	public void setHandler(RejectedExecutionHandler handler) {
		this.handler = handler;
	}
	
	/**
	 * Starts an instrumentation measurement.
	 */
	protected void openInstrumentation() {
		ThreadTaskInstrumentation tti = instrumentation.get();
		if(tti==null) {
			instrumentation.set(new ThreadTaskInstrumentation());
		} else {
			tti.reset();
		}
	}
	
	/**
	 * Completes an instrumentation measurement.
	 * @return the closed and delta processed ThreadTaskInstrumentation.
	 */
	protected ThreadTaskInstrumentation closeInstrumentation() {
		ThreadTaskInstrumentation tti = instrumentation.get();
		if(instrumentation.get()!=null) {
			tti = tti.close();
		} 
		return tti;
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 */
	public PausableThreadPoolExecutor(int arg0, int arg1, long arg2,
			TimeUnit arg3, BlockingQueue<Runnable> arg4) {
		super(arg0, arg1, arg2, arg3, arg4);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 */
	public PausableThreadPoolExecutor(int arg0, int arg1, long arg2,
			TimeUnit arg3, BlockingQueue<Runnable> arg4, ThreadFactory arg5) {
		super(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 */
	public PausableThreadPoolExecutor(int arg0, int arg1, long arg2,
			TimeUnit arg3, BlockingQueue<Runnable> arg4,
			RejectedExecutionHandler arg5) {
		super(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 * @param arg6
	 */
	public PausableThreadPoolExecutor(int arg0, int arg1, long arg2,
			TimeUnit arg3, BlockingQueue<Runnable> arg4, ThreadFactory arg5,
			RejectedExecutionHandler arg6) {
		super(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	/**
	 * @return
	 */
	public int getRunningTasks() {
		return runningTasks.get();
	}

	/**
	 * @param t
	 * @param r
	 */
	protected void afterExecute(Runnable r, Throwable t) {		
		if(instrument.get()) {			
			ThreadTaskInstrumentation tti = closeInstrumentation();
			this.updateStats(tti);
		} else {
			runTasks.incrementAndGet();
		}
		super.afterExecute(r,t);
		runningTasks.decrementAndGet();
				
	}
	
	/**
	 * @return
	 */
	public int getWaiterCount() {
		return pauseLock.getQueueLength();	
	}

	/**
	 * @param t
	 * @param r
	 * @see java.util.concurrent.ThreadPoolExecutor#beforeExecute(java.lang.Thread, java.lang.Runnable)
	 */
	protected void beforeExecute(Thread t, Runnable r) {		
		super.beforeExecute(t, r);
		runningTasks.incrementAndGet();
		pauseLock.lock();
		try {
			while (isPaused)
				unpaused.await();
		} catch (InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
		if(instrument.get()) {
			openInstrumentation();
		}
	}

	/**
	 * 
	 */
	public void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	/**
	 * 
	 */
	public void resume() {
		pauseLock.lock();
		try {
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}

	/**
	 * @return the isPaused
	 */
	public boolean isPaused() {
		return isPaused;
	}

	/**
	 * @return the runTasks
	 */
	public long getRunTasks() {
		return runTasks.get();
	}
	
	public long getRejectedTasks() {
		return rejectedTasks.get();
	}
	
	public void reset() {
		runTasks.set(0);
		rejectedTasks.set(0);
		failedTasks.set(0);
	}

	public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
		rejectedTasks.incrementAndGet();
		handler.rejectedExecution(runnable, executor);
		
	}

	public void uncaughtException(Thread thread, Throwable throwable) {
		failedTasks.incrementAndGet();		
	}
	
	public long getFailedTasks() {
		return failedTasks.get();
	}

	/**
	 * The state of thread pool task execution instrumentation.
	 * @return true if instrumentation is enabled.
	 */
	@JMXAttribute(description="True if task execution is instrumented, false if it is not.", name="Instrumented", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getInstrumented() {
		return instrument.get();
	}

	/**
	 * Sets the state of thread pool task execution instrumentation.
	 * If instrumentation is enabled and set to disabled, the instrumentation stats will be reset.
	 * @param instrument true enables instrumentation.
	 */
	public void setInstrumented(boolean instrument) {
		if(this.instrument.get() != instrument) {
			this.instrument.set(instrument);
			if(!instrument) {
				resetInstrumentation();
			}
		}
	}

	/**
	 * @return the averageElapsedTime
	 */
	@JMXAttribute(description="The average elapsed time of run task (ns)", name="AverageElapsedTime", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageElapsedTime() {
		statsLock.lock();
		try {
			return averageElapsedTime;
		} finally {
			statsLock.unlock();
		}						
		
	}

	/**
	 * @return the averageCpuTime
	 */
	@JMXAttribute(description="The average cpu time of run task (ns)", name="AverageCpuTime", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageCpuTime() {
		statsLock.lock();
		try {
			return averageCpuTime;
		} finally {
			statsLock.unlock();
		}				
	}

	/**
	 * @return the averageWaitTime
	 */
	@JMXAttribute(description="The average wait time of run task (ms)", name="AverageWaitTime", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageWaitTime() {
		statsLock.lock();
		try {
			return averageWaitTime;
		} finally {
			statsLock.unlock();
		}
	}

	/**
	 * @return the averageBlockTime
	 */
	@JMXAttribute(description="The average block time of run task (ms)", name="AverageBlockTime", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageBlockTime() {
		statsLock.lock();
		try {
			return averageBlockTime;
		} finally {
			statsLock.unlock();
		}
	}

	/**
	 * @return the averageBasedUtilization
	 */
	@JMXAttribute(description="The average percent CPU Utilization based on averages", name="AverageBasedUtilization", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getAverageBasedUtilization() {
		statsLock.lock();
		try {
			return averageBasedUtilization;
		} finally {
			statsLock.unlock();
		}
	}
	
	/**
	 * @return the totalBasedUtilization
	 */
	@JMXAttribute(description="The average percent CPU Utilization based on totals", name="TotalBasedUtilization", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTotalBasedUtilization() {
		statsLock.lock();
		try {
			return totalBasedUtilization;
		} finally {
			statsLock.unlock();
		}
	}

	/**
	 * @return the averageStatsUpdateTime
	 */
	@JMXAttribute(description="The average time to update instrumentation stats.", name="AverageStatsUpdateTime", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageStatsUpdateTime() {
		return averageStatsUpdateTime.get();
	}
	

}
