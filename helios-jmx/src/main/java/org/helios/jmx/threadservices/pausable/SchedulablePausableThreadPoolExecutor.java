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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Title: SchedulablePausableThreadPoolExecutor</p>
 * <p>Description: A pausable scheduled thread pool executor.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SchedulablePausableThreadPoolExecutor extends
		ScheduledThreadPoolExecutor implements RejectedExecutionHandler, Thread.UncaughtExceptionHandler, PausablePool {

	private boolean isPaused;
	private ReentrantLock pauseLock = new ReentrantLock();
	private Condition unpaused = pauseLock.newCondition();
	protected AtomicInteger runningTasks = new AtomicInteger(0);
	protected AtomicLong runTasks = new AtomicLong(0);
	protected AtomicLong rejectedTasks = new AtomicLong(0);
	protected AtomicLong failedTasks = new AtomicLong(0);
	protected RejectedExecutionHandler handler = null;

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
	 * @param corePoolSize the number of threads to keep in the pool, even if they are idle. 
	 */
	public SchedulablePausableThreadPoolExecutor(int corePoolSize) {
		super(corePoolSize);
	}

	/**
	 * @param corePoolSize the number of threads to keep in the pool, even if they are idle. 
	 * @param threadFactory the factory to use when the executor creates a new thread. 
	 */
	public SchedulablePausableThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
		super(corePoolSize, threadFactory);
	}

	
	/**
	 * @param corePoolSize the number of threads to keep in the pool, even if they are idle. 
	 * @param threadFactory the factory to use when the executor creates a new thread. 
	 * @param rejectedExecutionHandler the handler to use when execution is blocked because the thread bounds and queue capacities are reached. 
	 */
	public SchedulablePausableThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
		super(corePoolSize, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * @param corePoolSize the number of threads to keep in the pool, even if they are idle. 
	 * @param rejectedExecutionHandler the handler to use when execution is blocked because the thread bounds and queue capacities are reached.
	 */
	public SchedulablePausableThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler rejectedExecutionHandler) {
		super(corePoolSize, rejectedExecutionHandler);
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
		super.afterExecute(r,t);
		runningTasks.decrementAndGet();
		runTasks.incrementAndGet();		
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


}
