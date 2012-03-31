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
package org.helios.jmxenabled.threads;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;

/**
 * <p>Title: ExecutorBuilder</p>
 * <p>Description: Builder class for {@link java.util.concurrent.ThreadPoolExecutor}s and {@link java.util.concurrent.ScheduledThreadPoolExecutor}s.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.ExecutorBuilder</code></p>
 */

public class ExecutorBuilder {
	
	/** The number of available processors */
	public static final int PROCESSOR_COUNT = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	
	/** Serial number factory for auto generated thread group names */
	private static final AtomicLong serial = new AtomicLong(0L);
	/** Prefix for the pool's thread group and threads. Defaults to <b><code>HeliosExecutor</code></b>. */
	private String threadGroupName = null;
	/** The rejection policy for the thread pool. Defaults to <b><code>CALLERRUNS</code></b> */
	private TaskRejectionPolicy policy = TaskRejectionPolicy.CALLERRUNS;
	/** Indicates if the requested executor type. If true, returns a {@link java.util.concurrent.ThreadPoolExecutor} otherwise a {@link java.util.concurrent.ScheduledThreadPoolExecutor}. Defaults to true. */
	private boolean tpExecutor = true;
	/** The thread pool's uncaught exception handler. Defaults to {@org.helios.jmxenabled.threads.DefaultUncaughtExceptionHandler} */
	private UncaughtExceptionHandler eHandler = null;
	/** Indicates if the executor should have task metrics enabled on start. Defaults to false */
	private boolean taskMetricsEnabled = false;
	/** The maximum number of threads. Defaults to 2 X the number of processors */
	private int maxThreads = PROCESSOR_COUNT * 2;
	/** The core number of threads. Defaults to 2 X the number of processors */
	private int coreThreads = PROCESSOR_COUNT * 2;
	/** The number of core threads to prestart. Defaults to zero */
	private int prestartThreads = 0;
	/** The time limit in ms. for which threads may remain idle before being terminated. Defaults to 60000 ms. */
	private long keepAliveTime = 60000L;
	/** The priority of the threads that will be created for the pool. Defaults to Thread.NORM_PRIORITY */
	private int threadPriority = Thread.NORM_PRIORITY;	
	/** Indicates if the threads created for the thread pool should be daemon threads. Defaults to true */
	private boolean daemonThreads = true;
	/** Indicates if the thread pool will allow core threads to timeout. Defaults to false */
	private boolean coreThreadTimeout = false;
	/** The time limit in ms. that will be allowed for threads to complete processing after a shutdown call. Defaults to 0 ms. which is no time. */
	private long terminationTime = -1L;
	/** The size of the task submission queue. Defaults to 1000. */
	private int taskQueueSize = 1000;
	/** The fairness of the task submission queue. Defaults to fair */
	private boolean fairSubmissionQueue = true;
	/** Indicates if scheduled periodic tasks should keep running after the scheduled thread pool executor has been shut down. Defaults to false. If true, will force thread config to be daemons. */
	private boolean continuePeriodicTasks = false;
	/** Indicates if scheduled delayed tasks should keep running after the scheduled thread pool executor has been shut down. Defaults to false. If true, will force thread config to be daemons. */
	private boolean continueDelayedTasks = false;
	
	/** The ObjectName this pool should be published as */
	private ObjectName poolObjectName = null;
	/** The JMX MBeanServer domains this pool should be published in */
	private String[] jmxDomains = null;
	/** Class Logger */
	protected final static Logger LOG = Logger.getLogger(ExecutorBuilder.class);
	
	private ExecutorBuilder() {}
	
	/**
	 * Creates and returns a new ExecutorBuilder
	 * @return an ExecutorBuilder
	 */
	public static ExecutorBuilder newBuilder() {
		return new ExecutorBuilder();
	}
	
	/**
	 * Builds and returns the configured ThreadPoolExecutor
	 * @return the configured ThreadPoolExecutor
	 */
	public ThreadPoolExecutor build() {
		ThreadPoolExecutor tpe = null;
		if(threadGroupName==null) {
			threadGroupName = "HeliosExecutor" + serial.incrementAndGet();
		}
		if(this.tpExecutor) {
			tpe = new HeliosThreadPoolExecutorImpl(coreThreads, maxThreads, keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(taskQueueSize, fairSubmissionQueue), 
					new HeliosThreadFactory(threadGroupName, 0, threadPriority, eHandler,daemonThreads), policy.getPolicy(), terminationTime, taskMetricsEnabled);
			if(prestartThreads>0) {
				for(int i = 0; i < prestartThreads; i++) {
					if(!tpe.prestartCoreThread()) break;
				}
			}
			if(terminationTime>-1) {
				((HeliosThreadPoolExecutorImpl)tpe).setTerminationTime(terminationTime);
			}
		} else {
			tpe = new HeliosScheduledThreadPoolExecutorImpl(coreThreads, new HeliosThreadFactory(threadGroupName, 0, threadPriority, eHandler,daemonThreads), policy.getPolicy(), terminationTime, taskMetricsEnabled);
			((HeliosScheduledThreadPoolExecutorImpl)tpe).setContinueExistingPeriodicTasksAfterShutdownPolicy(continuePeriodicTasks);
			((HeliosScheduledThreadPoolExecutorImpl)tpe).setExecuteExistingDelayedTasksAfterShutdownPolicy(continueDelayedTasks);
		}
		tpe.setKeepAliveTime(keepAliveTime, TimeUnit.MILLISECONDS);
		tpe.allowCoreThreadTimeOut(coreThreadTimeout);		
		if(poolObjectName!=null) {
			try {
				if(jmxDomains!=null) {
					for(String domain: jmxDomains) {
						try {
							ExecutorMBeanPublisher.register(tpe, poolObjectName, domain);							
						} catch (Exception e) {
							LOG.warn("Failed to register ThreadPool with requested ObjectName [" + poolObjectName + "] in JMX Domain [" + domain + "]. Continuing.", e);
						}
					}
				} else {
					ExecutorMBeanPublisher.register(tpe, poolObjectName);
				}
			} catch (Exception e) {
				LOG.warn("Failed to register ThreadPool with requested ObjectName [" + poolObjectName + "] in default JMX Domain. Continuing.", e);
			}
		}
		return tpe;		
	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		LOG.info("Testing ExecutorBuilder");
		ExecutorBuilder.newBuilder()
			.setCoreThreads(5)
			.setMaxThreads(10)
			.setThreadGroupName("MyTestThreadGroup")
			.setPrestartThreads(3)
			.setKeepAliveTime(10000)
			.setPoolObjectName("org.helios.jmx:service=MyTestThreadPool")
			.build().execute(new Runnable(){
				public void run() {
					LOG.info("Thread:[" + Thread.currentThread().getClass().getName() + "]" + Thread.currentThread());
					LOG.info("ThreadGroup:[" + Thread.currentThread().getThreadGroup().getClass().getName() + "]" + Thread.currentThread().getThreadGroup());
				}
			});
		
		try { Thread.currentThread().join(); } catch (Exception e) {}
			
	}

	/**
	 * Prefix for the pool's thread group and threads.
	 * @param threadGroupName the threadGroupName to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setThreadGroupName(String threadGroupName) {
		this.threadGroupName = threadGroupName;
		return this;
	}

	/**
	 * The rejection policy for the thread pool.
	 * @param policy the policy to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setPolicy(TaskRejectionPolicy policy) {
		this.policy = policy;
		return this;
	}

	/**
	 * Indicates if the requested executor type. If true, returns a {@link java.util.concurrent.ThreadPoolExecutor} otherwise a {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
	 * @param tpExecutor true for a thread pool executor, false for a scheduler
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setExecutorType(boolean tpExecutor) {
		this.tpExecutor = tpExecutor;
		return this;
	}

	/**
	 * The thread pool's uncaught exception handler.
	 * @param eHandler the uncaught exception handler to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setUncaughtExceptionHandler(UncaughtExceptionHandler eHandler) {
		this.eHandler = eHandler;
		return this;
	}

	/**
	 *  Indicates if the executor should have task metrics enabled on start.
	 * @param taskMetricsEnabled the taskMetricsEnabled to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setTaskMetricsEnabled(boolean taskMetricsEnabled) {
		this.taskMetricsEnabled = taskMetricsEnabled;
		return this;
		
	}

	/**
	 * Sets the pool's maximum number of threads
	 * @param maxThreads the maxThreads to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return this;
	}

	/**
	 * Sets the pool's core number of threads
	 * @param coreThreads the coreThreads to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setCoreThreads(int coreThreads) {
		this.coreThreads = coreThreads;
		return this;
	}

	/**
	 * Sets the number of core threads to prestart
	 * @param prestartThreads the prestartThreads to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setPrestartThreads(int prestartThreads) {
		this.prestartThreads = prestartThreads;
		return this;
	}

	/**
	 * Sets the maximum keep alive time for idle threads
	 * @param keepAliveTime the keepAliveTime to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setKeepAliveTime(long keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
		return this;
	}

	/**
	 * Sets the thread priority of the thread pool's threads
	 * @param threadPriority the threadPriority to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setThreadPriority(int threadPriority) {
		this.threadPriority = threadPriority;
		return this;
	}

	/**
	 * Indicates if the thread pool's threads should be daemon threads
	 * @param daemonThreads true to set daemon threads
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setDaemonThreads(boolean daemonThreads) {
		this.daemonThreads = daemonThreads;
		return this;
	}

	/**
	 * Indicates if the thread pool's core threads should be allowed to timeout
	 * @param coreThreadTimeout the coreThreadTimeout to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setCoreThreadTimeout(boolean coreThreadTimeout) {
		this.coreThreadTimeout = coreThreadTimeout;
		return this;
	}

	/**
	 * Specifies a period of time to allow threads to complete processing on a shutdown call.
	 * @param terminationTime the terminationTime to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setTerminationTime(long terminationTime) {
		this.terminationTime = terminationTime;
		return this;
	}

	/**
	 * Sets the size of the thread pool's task queue
	 * @param taskQueueSize the taskQueueSize to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setTaskQueueSize(int taskQueueSize) {		
		this.taskQueueSize = taskQueueSize;
		return this;
	}

	/**
	 * Indicates if the thread pool's task queue should be fair
	 * @param fairSubmissionQueue the fairSubmissionQueue to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setFairSubmissionQueue(boolean fairSubmissionQueue) {
		this.fairSubmissionQueue = fairSubmissionQueue;
		return this;
	}

	/**
	 * Indicates if scheduled periodic tasks should keep running after the scheduled thread pool executor has been shut down.
	 * @param continuePeriodicTasks the continuePeriodicTasks to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setContinuePeriodicTasks(boolean continuePeriodicTasks) {
		this.continuePeriodicTasks = continuePeriodicTasks;
		return this;
	}

	/**
	 * Indicates if scheduled delayed tasks should keep running after the scheduled thread pool executor has been shut down
	 * @param continueDelayedTasks the continueDelayedTasks to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setContinueDelayedTasks(boolean continueDelayedTasks) {
		this.continueDelayedTasks = continueDelayedTasks;
		return this;
	}

	/**
	 * Sets the ObjectName this pool should be published as
	 * @param poolObjectName the ObjectName this pool should be published as
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setPoolObjectName(ObjectName poolObjectName) {
		if(this.poolObjectName==null) {
			this.poolObjectName = poolObjectName;
		}
		return this;
	}
	
	/**
	 * Sets the ObjectName this pool should be published as
	 * @param poolObjectName the ObjectName this pool should be published as
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setPoolObjectName(CharSequence poolObjectName) {
		if(this.poolObjectName==null) {
			this.poolObjectName = JMXHelper.objectName(poolObjectName);
		}
		return this;
	}
	
	/**
	 * Sets the ObjectName this pool should be published as
	 * @param domain The domain portion of the ObjectName
	 * @param keys The ObjectName keys
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setPoolObjectName(CharSequence domain, CharSequence...keys) {
		if(this.poolObjectName==null) {
			this.poolObjectName = JMXHelper.objectName(domain, keys);
		}
		return this;
	}
	
	

	/**
	 * Sets the JMX MBeanServer domains this pool should be published in 
	 * @param jmxDomains the jmxDomains to set
	 * @return this ExecutorBuilder
	 */
	public ExecutorBuilder setJmxDomains(String...jmxDomains) {
		if(this.jmxDomains==null) {
			this.jmxDomains = jmxDomains;
		}
		return this;
	}
	
	
	
	
}
