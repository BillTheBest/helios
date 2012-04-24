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
package org.helios.collectors.scheduler.quartz;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;

import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.threadservices.JMXManagedThreadPoolService;
import org.quartz.SchedulerConfigException;

/**
 * <p>Title: JMXManagedQuartzThreadPoolService</p>
 * <p>Description: An extension of the JMXManagedThreadPoolService with a Quartz compatible interface and tuned default configuration.</p>
 * <p>Default configuration values: <ul>
 * <li><b>Task Submission Queue Size</b>: 10000</li>
 * <li><b>Core Pool Size</b>: The number of CPUs x 5 (from OperatingSystemMXBean) or 5 if this fails.</li>
 * <li><b>Max Pool Size</b>: The number of CPUs x 10 (from OperatingSystemMXBean) or 10 if this fails.</li>
 * <li><b>Thread Keep Alive Time</b>: 10000 ms.</li>
 * <li><b>Discard Oldest</b>: false</li>
 * <li><b>ThreadGroup Name</b>: <code><JMX Object Name>-ThreadGroup</code>.</li>
 * <li><b>Thread Name Prefix</b>: <code><JMX Object Name>-Thread#</code>.</li>
 * <li><b>Daemon Threads</b>: true</li>
 * <li><b>Thread Stack Size</b>: JVM Implementation Default</li>
 * <li><b>Thread Priority</b>: <code>Thread.NORM_PRIORITY</code></li>
 * <li><b>ThreadPool Shutdown Time</b>: 1</li>
 * <li><b>Number of threads to prestart</b>: 0</li>
 * <li><b>Task hand offs to the pool should be queued, not executed</b>: true</li>
 * </ul></p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(declared=false, annotated=true)
public class JMXManagedQuartzThreadPoolService extends
		JMXManagedThreadPoolService implements IQuartzThreadPoolService {

	/** Indicates if the task should be executed or queued */
	protected AtomicBoolean queueExecutions = new AtomicBoolean(true);
	/** The constant name of the threadpool ObjectName */
	public static final String SERVICE_NAME = "org.helios.scheduling:service=QuartzThreadPoolService";
	/** The constant ObjectName where the threadpool registered in the platform agent. */
	public static final ObjectName SERVICE_OBJECT_NAME = JMXHelperExtended.objectName(SERVICE_NAME);
	/** The description of the threadpool service */
	public static final String SERVICE_DESCRIPTION = "Helios JMX Managed Quartz Thread Pool Service";


	/**
	 *
	 */
	private static final long serialVersionUID = -7716076497358640316L;

	/**
	 * Creates a new JMXManagedQuartzThreadPoolService.
	 */
	public JMXManagedQuartzThreadPoolService() {
		super();
	}

	// ====================================================================
	// 		Quartz Thread Pool Interface
	// ====================================================================

	/**
	 * Determines the number of threads that are currently available in in the pool.
	 * Useful for determining the number of times runInThread(Runnable) can be called before returning false.
	 * The implementation of this method should block until there is at least one available thread.
	 * @return the number of currently available threads
	 * @see org.quartz.spi.ThreadPool#blockForAvailableThreads()
	 */
	public int blockForAvailableThreads() {
		if(threadPool==null) throw new RuntimeException("ThreadPool is null");
		return 1;
	}

	/**
	 * Returns the number of threads currently in the pool.
	 * @return the number of threads in the pool.
	 * @see org.quartz.spi.ThreadPool#getPoolSize()
	 */
	public int getPoolSize() {
		if(threadPool==null) return 0;
		return ((ThreadPoolExecutor)threadPool).getPoolSize();

	}

	/**
	 * Called by the QuartzScheduler before the ThreadPool is used, in order to give the it a chance to initialize.
	 * @throws SchedulerConfigException
	 * @see org.quartz.spi.ThreadPool#initialize()
	 */
	public void initialize() throws SchedulerConfigException {
		try {
			start();
		} catch (Exception e) {
			throw new SchedulerConfigException("Failed to call start on pool", e);
		}
	}

	/**
	 * Execute the given Runnable in the next available Thread.
     * The implementation of this interface should not throw exceptions unless there is a serious problem
     * (i.e. a serious misconfiguration). If there are no immediately available threads false should be returned.
	 * @param runnable the runnable to run.
	 * @return true, if the runnable was assigned to run on a Thread.
	 * @see org.quartz.spi.ThreadPool#runInThread(java.lang.Runnable)
	 */
	public boolean runInThread(Runnable runnable) {
		if(threadPool==null) return false;
		try {
			if(queueExecutions.get()) {
				threadPool.submit(runnable);
			} else {
				threadPool.execute(runnable);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Called by the QuartzScheduler to inform the ThreadPool  that it should free up all of it's resources because the scheduler is shutting down.
	 * @param waitForJobsToComplete If true, jobs currently in progress will complete.
	 * @see org.quartz.spi.ThreadPool#shutdown(boolean)
	 */
	public void shutdown(boolean waitForJobsToComplete) {
		if(threadPool==null) return;
		if(waitForJobsToComplete) {
			threadPool.shutdown();
		} else {
			threadPool.shutdownNow();
		}
	}

	/**
	 * @return the queueExecutions
	 */
	@JMXAttribute(description="Indicates if tasks are queued, not executed", name="QueueExecutions")
	public boolean getQueueExecutions() {
		return queueExecutions.get();
	}

	/**
	 * @param queueExecutions the queueExecutions to set
	 */
	public void setQueueExecutions(boolean queueExecutions) {
		this.queueExecutions.set(queueExecutions);
	}


}
