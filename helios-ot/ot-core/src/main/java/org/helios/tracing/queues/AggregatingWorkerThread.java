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
package org.helios.tracing.queues;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;



/**
 * <p>Title: AggregatingWorkerThread</p>
 * <p>Description: Worker thread that funnels queued items from subqueues into an ExecutorService .</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1647 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/queues/AggregatingWorkerThread.java $
 * $Id: AggregatingWorkerThread.java 1647 2009-10-24 21:52:31Z nwhitehead $
 */
@JMXManagedObject(annotated=true, declared=true)
public class AggregatingWorkerThread<T> extends Thread implements UncaughtExceptionHandler {
	/** Object instance logger */
	protected Logger log = null;
	/** The subqueues to pull from */
	protected BlockingQueue<T>[] queues = null;
	/** The ExecutorService tasks are funneled to */
	protected ExecutorService executor = null;
	/** A sleep period taken by the thread if all subqueues are empty */
	protected long zeroQueueSleepTime = 0L;
	/** The maximum number of items to pull from the subqueue */
	protected AtomicInteger queuePullMaxSize = null;
	/** the thread name */
	protected String threadName = null;
	/** the thread priority */
	protected int threadPriority = Thread.NORM_PRIORITY;
	/** the daemon state of the thread */
	protected boolean daemon = true;
	/** a serial number generator for naming un-named threads */
	protected static final AtomicInteger serial = new AtomicInteger(0);
	/** the run signal for the thread */
	protected AtomicBoolean run = new AtomicBoolean(true);
	/** Indicates if current in process flush should be completed when the thread is stopped */
	protected boolean completeFlush = true;
	/** The created ObjectName for the management interface */
	protected ObjectName objectName = null;
	/** MBean Wrapper */
	protected ManagedObjectDynamicMBean modb = null;
	/** MBeanServer where management interface is registered */
	protected MBeanServer server = null;
	//========
	// Instrumentation
	//========
	protected long sleepCount = 0L;	
	protected long itemsDrained = 0L;
	protected long itemsSubmitted = 0L;
	protected long totalLoopTime = 0L;
	protected long totalLoopCount = 0L;
	
	//========
	// baselines
	//========
	protected long startTime = 0L;
	protected long blockTime = 0L;
	protected long waitTime = 0L;
	
	
	/** The property name where the jmx default domain is referenced */
	public static final String JMX_DOMAIN_PROPERTY = "helios.opentrace.config.jmx.domain";
	/** The default jmx default domain is referenced */
	public static final String JMX_DOMAIN_DEFAULT = "DefaultDomain";
	/** The OpenTraceManager's JMX ObjectName */
	public static final String OPEN_TRACE_MANAGER_OBJECT_NAME = "org.helios.opentrace:service=TracerQueueWorkerThread,name=";
	
	protected static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	protected static final boolean threadContentionEnabled = (threadMXBean.isThreadContentionMonitoringSupported() && threadMXBean.isThreadContentionMonitoringEnabled());
	
	
	/**
	 * Creates a new AggregatingWorkerThread
	 * @param queues The subqueues to pull from
	 * @param executor The ExecutorService tasks are funneled to
	 * @param zeroQueueSleepTime A sleep period taken by the thread if all subqueues are empty
	 * @param queuePullMaxSize The maximum number of items to pull from the subqueue
	 * @param threadName The name assigned to the thread.
	 * @param daemon The daemon state of the thread
	 * @param priority The thread priority
	 */
	public AggregatingWorkerThread(BlockingQueue<T>[] queues,
			ExecutorService executor, long zeroQueueSleepTime,
			AtomicInteger queuePullMaxSize, String threadName, boolean daemon, int priority) {
		super();
		this.queues = queues==null ? new BlockingQueue[]{} : queues.clone();
		this.executor = executor;
		this.zeroQueueSleepTime = zeroQueueSleepTime;
		this.queuePullMaxSize = queuePullMaxSize;
		this.setDaemon(daemon);
		this.setName(threadName);
		this.setPriority(threadPriority);
		this.setUncaughtExceptionHandler(this);
		this.log = Logger.getLogger(getClass() + "." + threadName);
		
	}
	
	/**
	 * Creates a new AggregatingWorkerThread.
	 * Thread name is autogenerated.
	 * Thread is daemon.
	 * Thread priority is <code>Thread.NORM_PRIORITY</code>.
	 * @param queues The subqueues to pull from
	 * @param executor The ExecutorService tasks are funneled to
	 * @param zeroQueueSleepTime A sleep period taken by the thread if all subqueues are empty
	 * @param queuePullMaxSize The maximum number of items to pull from the subqueue
	 */
	public AggregatingWorkerThread(BlockingQueue<T>[] queues,
			ExecutorService executor, long zeroQueueSleepTime,
			AtomicInteger queuePullMaxSize) {
		this(queues, executor, zeroQueueSleepTime, queuePullMaxSize, "AggregatingWorkerThread#" + serial.incrementAndGet(), true, Thread.NORM_PRIORITY);
	}

	/**
	 * Method invoked when the given thread terminates due to the given uncaught exception.
	 * @param t the thread
	 * @param e the exception
	 * @see java.lang.Thread$UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	public void uncaughtException(Thread t, Throwable e) {
		log.warn("AggregatingWorkerThread [" + t.getName() + "/" + t.getId() + "] encountered exception", e);
	}
	
	/**
	 * Captures baseline metrics and starts the thread. 
	 * @see java.lang.Thread#start()
	 */
	public void start() {
		startTime = System.currentTimeMillis();
		registerMBean();
		super.start();
		if(threadContentionEnabled) {
			blockTime = threadMXBean.getThreadInfo(this.getId()).getBlockedTime();
			waitTime = threadMXBean.getThreadInfo(this.getId()).getWaitedTime();
		}
		
	}
	
	protected void registerMBean() {
		try {
			objectName = new ObjectName(OPEN_TRACE_MANAGER_OBJECT_NAME + getName());
			modb = new ManagedObjectDynamicMBean(this);
			server = JMXHelperExtended.getLocalMBeanServer(ConfigurationHelper.getEnvThenSystemProperty(JMX_DOMAIN_PROPERTY, JMX_DOMAIN_DEFAULT));
			server.registerMBean(modb, objectName);
		} catch (Exception e) {
			log.warn("Could not register MBean for [" + getName() + "/" + getId() + "]", e);
		}
	}
	
	/**
	 * Stops the thread.
	 * @param completeFlush Indicates if the current flush should be completed.
	 * @param unregisterInterface Indicates if the management interface should be unregistered.
	 */
	public void stopMe(boolean completeFlush, boolean unregisterInterface) {
		this.completeFlush = completeFlush;
		run.set(false);
		this.interrupt();
		try {
			if(unregisterInterface && server != null) {
				server.unregisterMBean(objectName);
			}
		} catch (Exception e) {}
	}
	
	/**
	 * Stops the thread.
	 * @param completeFlush Indicates if the current flush should be completed.
	 */
	public void stopMe(boolean completeFlush) {
		this.stopMe(completeFlush, true);
	}
	
	
	/**
	 * Pulls items from each subqueue and submits them to the executor.
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		Set<T> drain = new HashSet<T>();
		int drainCount = 0;
		int maxPull = 0;		
		while(run.get()) {
			long startTime = System.currentTimeMillis();
			try {
				drainCount = 0;
				maxPull = queuePullMaxSize.get();
				drain.clear();
				drainCount = flushCycle(drain, drainCount, maxPull);
				if(drainCount<1) {
					sleepCount++;
					Thread.currentThread().join(zeroQueueSleepTime);					
				} else {
					itemsDrained += drainCount;
				}
			} catch (InterruptedException tie) {
				interrupted();
				if(completeFlush) {
					flushCycle(drain, drainCount, maxPull);
				} else {
					break;
				}
			} finally {
				totalLoopCount++;
				totalLoopTime += System.currentTimeMillis()-startTime;
			}
		}
	}
	

	/**
	 * Executes one loop of queue draining.
	 * @param drain
	 * @param drainCount
	 * @param maxPull
	 * @return
	 */
	private int flushCycle(Set<T> drain, int drainCount, int maxPull) {
		for(BlockingQueue<T> q: queues) {
			drainCount += q.drainTo(drain, maxPull);
			for(T o: drain) {
				try {
					if(o instanceof Runnable) {
						executor.submit((Runnable)o);
						itemsSubmitted++;
					} else if(o instanceof Callable) {
						executor.submit((Callable)o);
						itemsSubmitted++;
					} else {
						log.warn("Dropping retrieved item which is not a runnable or callable");
					}
				} catch (RejectedExecutionException  ree) {
					
				}
			}
		}
		return drainCount;
	}

	/**
	 * @return the sleepCount
	 */
	@JMXAttribute(name="SleepCount", description="The number of times the thread slept because all subqueues were empty", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSleepCount() {
		return sleepCount;
	}

	/**
	 * @return the itemsDrained
	 */
	@JMXAttribute(name="ItemsDrained", description="The number of items drained from the subqueues", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getItemsDrained() {
		return itemsDrained;
	}

	/**
	 * @return the itemsSubmitted
	 */
	@JMXAttribute(name="ItemsSubmitted", description="The number of items successfully submitted to the executor service", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getItemsSubmitted() {
		return itemsSubmitted;
	}
	
	/**
	 * the average loop time
	 * @return
	 */
	@JMXAttribute(name="AverageLoopTime", description="The average elapsed time for one drain loop", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageLoopTime() {
		return avg(totalLoopTime, totalLoopCount);
	}
	
	/**
	 * the average DrainCount
	 * @return
	 */
	@JMXAttribute(name="AverageDrainCount", description="The average number of items pulled per drain loop", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getDrainCount() {
		return avg(itemsDrained, totalLoopCount);
	}
	
	/**
	 * The average block time per loop
	 * @return
	 */
	@JMXAttribute(name="AverageBlockTime", description="The average thread block time per drain loop", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageBlockTime() {
		if(threadContentionEnabled) {
			long currentBlockTime = threadMXBean.getThreadInfo(getId()).getBlockedTime();
			return avg((currentBlockTime-blockTime), totalLoopCount);
		} else {
			return -1L;
		}
	}
	
	/**
	 * The average wait time per loop
	 * @return
	 */
	@JMXAttribute(name="AverageWaitTime", description="The average thread wait time per drain loop", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageWaitTime() {
		if(threadContentionEnabled) {
			long currentWaitTime = threadMXBean.getThreadInfo(getId()).getWaitedTime();
			return avg((currentWaitTime-waitTime), totalLoopCount);
		} else {
			return -1L;
		}
	}
	
	
	
	private long avg(float time, float count ) {
		if(count==0 || time==0) return 0;
		float avg = time/count;
		return (long)avg;
	}

	/**
	 * @return the totalLoopTime
	 */
	public long getTotalLoopTime() {
		return totalLoopTime;
	}

	/**
	 * @return the totalLoopCount
	 */
	public long getTotalLoopCount() {
		return totalLoopCount;
	}
	
	
	
	
	
	
}
