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
package org.helios.jmx.threadservices;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.pausable.PausablePool;
import org.helios.jmx.threadservices.pausable.PausableThreadPoolExecutor;
import org.helios.jmx.threadservices.pausable.SchedulablePausableThreadPoolExecutor;
import org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService;
import org.helios.jmx.threadservices.submission.InvocationArgumentProvider;
import org.helios.jmx.threadservices.submission.InvocationTask;


/**
 * <p>Title: JMXManagedThreadPoolService</p>
 * <p>Description: A JMX managed thread pool service used to submit tasks to be run asynchronously in a seperate worker thread.</p>
 * <p>Default configuration values: <ul>
 * <li><b>Task Submission Queue Size</b>: 10000</li>
 * <li><b>Core Pool Size</b>: The number of CPUs x 5 (from OperatingSystemMXBean) or 5 if this fails.</li>
 * <li><b>Max Pool Size</b>: The number of CPUs x 10 (from OperatingSystemMXBean) or 10 if this fails.</li>
 * <li><b>Thread Keep Alive Time</b>: 10000 ms.</li>
 * <li><b>Discard Oldest</b>: false</li>
 * <li><b>ThreadGroup Name</b>: <code>[JMX Object Name]-ThreadGroup</code>.</li>
 * <li><b>Thread Name Prefix</b>: <code>[JMX Object Name]-Thread#</code>.</li>
 * <li><b>Daemon Threads</b>: true</li>
 * <li><b>Thread Stack Size</b>: JVM Implementation Default</li>
 * <li><b>Thread Priority</b>: <code>Thread.NORM_PRIORITY</code></li>
 * <li><b>ThreadPool Shutdown Time</b>: 1</li>
 * <li><b>Number of threads to prestart</b>: 0</li>
 * </ul></p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(declared=false, annotated=true)
public class JMXManagedThreadPoolService extends ManagedObjectDynamicMBean implements IThreadPoolService, ThreadFactory, ExecutorService, UncaughtExceptionHandler {
	
	/** serial uid */
	private static final long serialVersionUID = 2999361021118266933L;
	/** Default Core Pool Size Multiplier */
	public static final int CORE_POOL_SIZE_FACTOR = 5;
	/** Default Max Pool Size Multiplier */
	public static final int MAX_POOL_SIZE_FACTOR = 5;
	
	/** The thread pool that handles the incoming traces from the data queue.*/
	protected AbstractExecutorService threadPool = null;
	/** The backing queue for the thread pool. */
	protected BlockingQueue<Runnable> requestQueue = null; 
	/** The capacity of the thread pool backing queue. */
	protected int queueCapacity = 10000;
	/** The core pool size. Defaults to 5 */
	protected int corePoolSize = 5;
	/** The maximum pool size. Defaults to 10 */
	protected int maxPoolSize = 10;
	/** Maximum Thread Keep Alive Time in ms. Defaults to 10000 */
	protected long threadKeepAliveTime = 10000;
	/** Specifies if rejected submissions on account of a full queue are discarded, or causes the oldest task to be discarded to allow the latest */
	protected boolean discardOldest = false;
	/** The name of the thread group backing the pool. Defaults to <JMX Object Name>-ThreadGroup */
	protected String threadGroupName = null;
	/** The thread name prefix for threads generated to back the thread pool. Defaults to  <JMX Object Name>-Thread# */
	protected String threadNamePrefix = null;
	/** Indicates if threads backing the thread pool should be daemon threads. Defaults to true. */
	protected boolean daemonThreads = true;
	/** Specifies a customized stack size for threads created in the thread pool. Uses JVM Default if not specified. */
	protected int threadStackSize = -1;
	/** The ThreadGroup that the pool's threads a grouped in. */
	protected ThreadGroup threadGroup = null;
	/** The priority of the threads in the thread pool. Defaults to <code>Thread.NORM_PRIORITY</code> */
	protected int threadPriority = Thread.NORM_PRIORITY;
	/** The threadPool shutdown time in ms. that allows tasks in the queue to finish before discarding them and finishing the stop */
	protected long shutdownTime = 1;
	/** The number of threads to prestart. Defaults to zero. */
	protected int prestartThreads = 0;
	/** Defines if the thread pool has task execution instrumentation enabled */
	protected AtomicBoolean instrumentationEnabled = new AtomicBoolean(false);
	/** Defines if the task executors have instrumentation enabled */
	protected AtomicBoolean taskInstrumentationEnabled = new AtomicBoolean(false);
	
	
	protected StringBuilder messages = new StringBuilder();
	
	protected Logger log = Logger.getLogger(getClass().getName());
	
	protected static AtomicLong threadSequence = new AtomicLong(0);
	protected static AtomicLong threadPoolSequence = new AtomicLong(0);
	
	
	/** The cumulative numbver of uncaught thread exceptions */
	protected AtomicLong uncaughtExceptionCount = new AtomicLong(0);
	
	protected boolean isRunning = false;
	
	protected RejectedExecutionHandler handler = null;
	
	

	/**
	 * Constructs a new JMXManagedThreadPoolService.
	 */
	public JMXManagedThreadPoolService() {
		setDefaultPoolSizes();
		reflectObject(this);
		log.info("Instantiated " + this.getClass().getName());
	}
	
	/**
	 * Sets the default pool sizes as follows: <ul>
	 * <li><b>Core Pool Size</b>: The number of CPUs x 5 (from OperatingSystemMXBean) or 5 if this fails.</li>
	 * <li><b>Max Pool Size</b>: The number of CPUs x 10 (from OperatingSystemMXBean) or 10 if this fails.</li> 
	 * </ul>
	 */
	protected void setDefaultPoolSizes() {
		corePoolSize = getDefaultCorePoolSize();
		maxPoolSize = getDefaultMaxPoolSize();
	}
	
	/**
	 * Determines the default core pool size for the number of CPUs.
	 * @return the default core pool size.
	 */
	public static int getDefaultCorePoolSize() {
		int cpuCount = 1;
		try {
			cpuCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		} catch (Exception e) {
			cpuCount = 1;
		}
		return CORE_POOL_SIZE_FACTOR * cpuCount;		
	}
	
	/**
	 * Determines the default max pool size for the number of CPUs.
	 * @return the default max pool size.
	 */
	public static int getDefaultMaxPoolSize() {
		int cpuCount = 1;
		try {
			cpuCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		} catch (Exception e) {
			cpuCount = 1;
		}
		return MAX_POOL_SIZE_FACTOR * cpuCount;		
	}
	
	
	/**
	 * Starts the JMXManagedThreadPoolService.
	 * @throws Exception
	 */
	@JMXOperation(description="Starts the Service", name="start")
	public void start() throws Exception {
		if(isRunning) return;
		log.info("\n\t================================\n\tStarting Service" + objectName + "\n\t================================");
		initializePool();
		log.info("\n\t================================\n\tStarted Service" + objectName + "\n\t================================");
	}
	


	/**
	 * Initializes the pool.
	 */
	protected void initializePool() {
		if(threadGroupName==null) {
			threadGroupName = objectName.toString() + "-ThreadGroup";
		}
		log.info("Thread Group Name:" + threadGroupName);
		threadGroup = new ThreadGroup(threadGroupName);
		if(threadNamePrefix==null) {
			threadNamePrefix = objectName.toString();
		}
		log.info("Thread Name Prefix:" + threadNamePrefix);
		setDiscardPolicy();
		log.info("Discard Policy:" + handler.getClass().getName());
		if(requestQueue==null) {
			requestQueue = new ArrayBlockingQueue<Runnable>(queueCapacity);
		}
		log.info("Work Queue Size:" + requestQueue.remainingCapacity());
		log.info("Thread Keep Alive Time:" + threadKeepAliveTime);
		log.info("Core Pool Size:" + corePoolSize);
		log.info("Maximum Pool Size:" + maxPoolSize);
		if(this instanceof IScheduledThreadPoolService) {
			threadPool = new SchedulablePausableThreadPoolExecutor(corePoolSize, this, handler);
		} else {
			threadPool = new PausableThreadPoolExecutor(corePoolSize, maxPoolSize, threadKeepAliveTime, TimeUnit.MILLISECONDS, requestQueue, this, handler);
			((PausableThreadPoolExecutor)threadPool).setInstrumented(instrumentationEnabled.get());			
			this.reflectObject(threadPool);
		}		
		((PausablePool)threadPool).setHandler(handler);
		int prestarted = 0;
		for(int i = 0; i < prestartThreads; i++) {
			if(((ThreadPoolExecutor)threadPool).prestartCoreThread()) prestarted++;
		}
		log.info("Prestarted " + prestarted + " Threads.");
		isRunning = true;
	}
	
	
	/**
	 * Sets the discard policy in the threadPool
	 */
	protected void setDiscardPolicy() {
		if(discardOldest) {
			handler = new ThreadPoolExecutor.DiscardOldestPolicy();
		} else {
			handler = new ThreadPoolExecutor.DiscardPolicy();
		}		
	}
	
	/**
	 * Stops the JMXManagedThreadPoolService.
	 */
	@JMXOperation(description="Stops the Service", name="stop")
	public void stop() {
		if(!isRunning) return;
		log.info("\n\t================================\n\tStopping Service" + objectName + "\n\t================================");
		boolean cleanShutdown = false;
		int discardedTasks = 0;
		try {
			threadPool.shutdown();
			try {
				log.info("\n\tWaiting " + shutdownTime + " ms. for tasks to complete.");
				if(threadPool.awaitTermination(shutdownTime, TimeUnit.MILLISECONDS)) {
					cleanShutdown = true;
				} else {
					cleanShutdown = false;
				}
			} catch (InterruptedException e) {
				cleanShutdown = false;
			}
		} finally {
			
		}
		if(threadPool.isTerminated() && cleanShutdown) {
			log.info("\n\tThreadPool Clean Shutdown.");
		} else {
			discardedTasks = threadPool.shutdownNow().size();
			log.info("\n\tThreadPool Shutdown Time Out. Discarded " + discardedTasks + " Tasks.");
		}
		
		isRunning = false;
		stopInner();
		log.info("\n\t================================\n\tStopped Service" + objectName + "\n\t================================");
	}
	
	protected void stopInner() {}
	

	
	/**
	 * Constructs a new Thread. Implementations may also initialize priority, name, daemon status, ThreadGroup, etc. 
	 * @param r a runnable to be executed by new thread instance 
	 * @return The constructed thread.
	 */
	public Thread newThread(Runnable r) {
		Thread t = null;
		if(threadGroup==null) {
			threadGroup = new ThreadGroup("JMXManagedThreadPoolService#" + threadPoolSequence.incrementAndGet());
		}
		t = new Thread(threadGroup, r, threadNamePrefix + "-Thread#" + threadSequence.incrementAndGet(), threadStackSize);
		t.setPriority(threadPriority);
		t.setDaemon(daemonThreads);
		if(threadPool != null) {
			t.setUncaughtExceptionHandler(this);
		}
		return t;
	}

	/**
	 * Returns the core number of threads. 
	 * @return the core number of threads
	 */
	@JMXAttribute(description="The Core Pool Size of the Thread Pool", name="CorePoolSize")
	public int getCorePoolSize() {
		return corePoolSize;
	}

	/**
	 * Sets the core number of threads. This overrides any value set in the constructor. 
	 * If the new value is smaller than the current value, excess existing threads will be terminated when they next become idle. 
	 * If larger, new threads will, if needed, be started to execute any queued tasks. 
	 * @param corePoolSize the new core size 
	 */
	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
		if(isRunning) ((ThreadPoolExecutor)threadPool).setCorePoolSize(corePoolSize);
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The pool backing threads daemon setting.", name="DaemonThreads")
	public boolean getDaemonThreads() {
		return daemonThreads;
	}

	/**
	 * @param daemonThreads
	 */
	public void setDaemonThreads(boolean daemonThreads) {
		this.daemonThreads = daemonThreads;
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The discard policy for a full work queue state.", name="DiscardOldest")
	public boolean getDiscardOldest() {
		return discardOldest;
	}

	/**
	 * @param discardOldest
	 */
	public void setDiscardOldest(boolean discardOldest) {
		this.discardOldest = discardOldest;
		if(isRunning) setDiscardPolicy();
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The Maximum Pool Size of the Thread Pool", name="MaxPoolSize", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	/**
	 * @param maxPoolSize
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
		if(isRunning) ((ThreadPoolExecutor)threadPool).setMaximumPoolSize(maxPoolSize);
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The Thread Pool Work Queue Capacity", name="WorkQueueCapacity")
	public int getQueueCapacity() {
		return queueCapacity;
	}

	/**
	 * @param queueCapacity
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The Thread Group Name", name="ThreadGroupName")
	public String getThreadGroupName() {
		return threadGroupName;
	}

	/**
	 * @param threadGroupName
	 */
	public void setThreadGroupName(String threadGroupName) {
		this.threadGroupName = threadGroupName;
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The Keep Alive Time for threads above the core count in ms.", name="ThreadKeepAlive")
	public long getThreadKeepAliveTime() {
		return threadKeepAliveTime;
	}

	/**
	 * @param threadKeepAliveTime
	 */	
	public void setThreadKeepAliveTime(long threadKeepAliveTime) {
		this.threadKeepAliveTime = threadKeepAliveTime;
		if(isRunning) ((ThreadPoolExecutor)threadPool).setKeepAliveTime(threadKeepAliveTime, TimeUnit.MILLISECONDS);
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The prefix to the thread names in the thread pool.", name="ThreadNamePrefix")
	public String getThreadNamePrefix() {
		return threadNamePrefix;
	}

	/**
	 * @param threadNamePrefix
	 */
	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The size in bytes of the stack allocated to threads created for the thread pool.", name="ThreadStackSize")
	public int getThreadStackSize() {
		return threadStackSize;
	}

	/**
	 * @param threadStackSize
	 */
	public void setThreadStackSize(int threadStackSize) {
		this.threadStackSize = threadStackSize;
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The thread pool backing the service.", name="ThreadPool")
	public ThreadPoolExecutor getThreadPool() {
		return ((ThreadPoolExecutor)threadPool);
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The priority of the threads backing the thread pool.", name="ThreadPriority")	
	public int getThreadPriority() {
		return threadPriority;
	}

	/**
	 * @param threadPriority
	 */
	public void setThreadPriority(int threadPriority) {
		this.threadPriority = threadPriority;
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="An estimate of the number of active threads in the thread group backing the thread pool.", name="ActiveThreadCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getActiveThreadCount() {
		if(isRunning) {
			return threadGroup.activeCount();
		} else {
			return 0;
		}
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The approximate total number of tasks that have completed execution.", name="CompletedTaskCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCompletedTaskCount() {
		if(isRunning) {
			return ((ThreadPoolExecutor)threadPool).getCompletedTaskCount();
		} else {
			return 0;
		}
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The largest number of threads that have ever simultaneously been in the pool.", name="LargestPoolSize", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getLargestPoolSize() {
		if(isRunning) {
			return ((ThreadPoolExecutor)threadPool).getLargestPoolSize();
		} else {
			return 0;
		}		
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The current number of threads in the pool.", name="CurrentPoolSize", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getCurrentPoolSize() {
		if(isRunning) {
			return ((ThreadPoolExecutor)threadPool).getPoolSize();
		} else {
			return 0;
		}				
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The approximate total number of tasks that have been scheduled for execution.", name="TaskCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTaskCount() {
		if(isRunning) {
			return ((ThreadPoolExecutor)threadPool).getTaskCount();
		} else {
			return 0;
		}						
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="True if the thread pool is running.", name="Running", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getRunning() {
		return isRunning;
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The number of tasks in the request queue.", name="QueuedTaskCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getQueuedTaskCount() {
		if(requestQueue!=null) {
			return requestQueue.size();
		} else {
			return 0;
		}
	}
	
	/**
	 * Returns an instance of the pool as an IThreadPoolService
	 * @return
	 */
	@JMXAttribute(description="The public interface for submiting tasks.", name="Instance", mutability=AttributeMutabilityOption.READ_ONLY)
	public IThreadPoolService getInstance() {
		return this;
	}
	
	/**
	 * 
	 */
	@JMXOperation(description="Purges all cancelled Future Tasks", name="purgeFutures")
	public void purgeFutures() {
		if(isRunning) {
			((ThreadPoolExecutor)threadPool).purge();
		}		
	}
	
	/**
	 * 
	 */
	@JMXOperation(description="Purges all scheduled tasks in the queue", name="purgeQueue")
	public void purgeQueuedTasks() {
		if(requestQueue != null) {
			requestQueue.clear();
		}
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The shutdown time for the thread pool.", name="ShutdownTime")
	public long getShutdownTime() {
		return shutdownTime;
	}

	/**
	 * @param shutdownTime
	 */
	public void setShutdownTime(long shutdownTime) {
		this.shutdownTime = shutdownTime;
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The number of threads to prestart.", name="PrestartThreads")
	public int getPrestartThreads() {
		return prestartThreads;
	}

	/**
	 * @param prestartThreads
	 */
	public void setPrestartThreads(int prestartThreads) {
		this.prestartThreads = prestartThreads;
	}

	/**
	 * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current thread is interrupted, whichever happens first.
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @return true if this executor terminated and false  if the timeout elapsed before termination
	 * @throws InterruptedException
	 * @see java.util.concurrent.ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
	 */
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return threadPool.awaitTermination(timeout, unit);		
	}


	/**
	 * @return
	 * @see java.util.concurrent.ExecutorService#isShutdown()
	 */
	@JMXAttribute(description="The shutdown state of the pool.", name="Shutdown", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getShutdownState() {
		return threadPool.isShutdown();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ExecutorService#isTerminated()
	 */
	@JMXAttribute(description="The termination state of the pool.", name="Terminated", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getTerminated() {
		return threadPool.isTerminated();
	}

	/**
	 * 
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 */
	public void shutdown() {
		threadPool.shutdown();		
	}

	/**
	 * @return
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	public List<Runnable> shutdownNow() {
		return threadPool.shutdownNow();
	}

	/**
	 * Submits a value-returning task for execution and returns a Future representing the pending results of the task.
	 * If you would like to immediately block waiting for a task, you can use constructions of the form result = exec.submit(aCallable).get();
	 * Note: The Executors class includes a set of methods that can convert some other common closure-like objects, for example, PrivilegedAction to Callable form so they can be submitted.
	 * @param task<T> the task to submit
	 * @return a Future representing pending completion of the task
	 * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
	 * @throws RejectedExecutionException - if task cannot be scheduled for execution
	 * @throws NullPointerException - if task null
	 */
	@JMXOperation(description="Submits a value-returning task for execution and returns a Future representing the pending results of the task.", name="submit")
	public <T> Future<T> submit(
			@JMXParameter(description="The task to submit.", name="task") Callable<T> task
			) {
		return threadPool.submit(task);
	}

	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
	 */
	public Future<?> submit(Runnable task) {
		return threadPool.submit(task);
	}

	/**
	 * @param <T>
	 * @param task
	 * @param result
	 * @return
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
	 */
	public <T> Future<T> submit(Runnable task, T result) {
		return threadPool.submit(task, result);
	}
	

	/**
	 * @param command
	 * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
	 */
	public void execute(Runnable command) {
		threadPool.execute(command);
	}

	/**
	 * Submits an invocation task for execution.
	 * @param target The target object
	 * @param method The target method
	 * @param arguments The parameters to the method.
	 * @return a Future representing pending completion of the task
	 * @see org.helios.jmx.threadservices.IThreadPoolService#submit(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public <T> Future<T> submit(Object target, Method method, Object[] arguments) {		
		return threadPool.submit(new InvocationTask<T>(target, method, arguments));
	}
	
	/**
	 * Submits an invocation task for execution.
	 * @param target The target object
	 * @param method The target method
	 * @param argProvider The parameter provider for the invocation.
	 * @return a Future representing pending completion of the task
	 * @see org.helios.jmx.threadservices.IThreadPoolService#submit(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public <T> Future<T> submit(Object target, Method method, InvocationArgumentProvider argProvider) {		
		return threadPool.submit(new InvocationTask<T>(target, method, argProvider));
	}
	
	/**
	 * Submits a named invocation task for execution.
	 * @param name The name of the invocation task.
	 * @param target The target object
	 * @param method The target method
	 * @param arguments The parameters to the method.
	 * @return a Future representing pending completion of the task
	 * @see org.helios.jmx.threadservices.IThreadPoolService#submit(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public <T> Future<T> submit(String name, Object target, Method method, Object[] arguments) {		
		return threadPool.submit(new InvocationTask<T>(target, method, arguments, name));
	}
	
	/**
	 * Submits a named invocation task for execution.
	 * @param name The name of the invocation task.
	 * @param target The target object
	 * @param method The target method
	 * @param argProvider The parameter provider for the invocation.
	 * @return a Future representing pending completion of the task
	 * @see org.helios.jmx.threadservices.IThreadPoolService#submit(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public <T> Future<T> submit(String name, Object target, Method method, InvocationArgumentProvider argProvider) {		
		return threadPool.submit(new InvocationTask<T>(target, method, argProvider, name));
	}
	
	

	/**
	 * @return
	 * @see org.helios.jmx.threadservices.IThreadPoolService#isPaused()
	 */
	@JMXAttribute(description="The paused state of the pool.", name="Paused", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getPaused() {
		if(threadPool==null) return false;
		return ((PausablePool)threadPool).isPaused();
	}

	/**
	 * @param paused
	 */
	public void setPaused(boolean paused) {
		if(paused) {
			if(!((PausablePool)threadPool).isPaused()) {
				((PausablePool)threadPool).pause();
			}
		} else {
			if(((PausablePool)threadPool).isPaused()) {
				((PausablePool)threadPool).resume();
			}			
		}
	}
	

	/**
	 * 
	 * @see org.helios.jmx.threadservices.IThreadPoolService#pause()
	 */
	public void pause() {
		if(!((PausablePool)threadPool).isPaused()) {
			((PausablePool)threadPool).pause();
		}		
	}

	/**
	 * 
	 * @see org.helios.jmx.threadservices.IThreadPoolService#resume()
	 */
	public void resume() {
		if(((PausablePool)threadPool).isPaused()) {
			((PausablePool)threadPool).resume();
		}		
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The size of the pool's backing queue", name="QueueSize", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getQueueSize() {
		if(threadPool==null) return 0;
		return ((ThreadPoolExecutor)threadPool).getQueue().size();
	}
	
	/**
	 * 
	 */
	public void reset() {
		((PausablePool)threadPool).reset();
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The number of tasks that have been run by the pool.", name="RunTaskCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRunTaskCount() {
		if(threadPool==null) return 0;
		return ((PausablePool)threadPool).getRunTasks();
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The number of tasks that are currently being run by the pool.", name="RunningTasksCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getRunningTasksCount() {
		if(threadPool==null) return 0;
		return ((PausablePool)threadPool).getRunningTasks();
	}

	
	/**
	 * @return
	 */
	@JMXAttribute(description="The number of threads waiting on the paused thread pool.", name="WaiterCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getWaiterCount() {
		if(threadPool==null) return 0;
		return ((PausablePool)threadPool).getWaiterCount();	
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The number of rejected tasks.", name="RejectedTasks", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRejectedTasks() {
		if(threadPool==null) return 0;
		return ((PausablePool)threadPool).getRejectedTasks();	
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The number of failed tasks", name="FailedTasks", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFailedTasks() {
		if(threadPool==null) return 0;
		return ((PausablePool)threadPool).getFailedTasks();	
	}

	/**
	 * Thread Pool Instrumentation State
	 * @return true of enabled, false if not.
	 */
	@JMXAttribute(description="Thread Pool Instrumentation State", name="InstrumentationEnabled", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getInstrumentationEnabled() {
		return instrumentationEnabled.get();
	}

	/**
	 * Sets the Thread Pool Instrumentation State
	 * @param instrumentationEnabled true enables the instrumentation, false disables it.
	 */	
	public void setInstrumentationEnabled(boolean instrumentationEnabled) {
		this.instrumentationEnabled.set(instrumentationEnabled);
		if(threadPool != null) {
			if(((PausableThreadPoolExecutor)threadPool).getInstrumented() != instrumentationEnabled) {
				((PausableThreadPoolExecutor)threadPool).setInstrumented(instrumentationEnabled);
			}
		}
	}
	
	/**
	 * Task Executor Instrumentation State
	 * @return true of enabled, false if not.
	 */
	@JMXAttribute(description="Task Executor Instrumentation State", name="TaskExecutorInstrumentationEnabled", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getTaskInstrumentationEnabled() {
		return taskInstrumentationEnabled.get();
	}
	
	/**
	 * Sets the Task Executor Instrumentation State
	 * @param enabled true to enable, false to disable.
	 */
	
	public void setTaskInstrumentationEnabled(boolean enabled) {
		taskInstrumentationEnabled.set(enabled);
	}	
	
	

	/**
	 * Returns true if this executor has been shut down.
	 * @return true if this executor has been shut down.
	 * @see java.util.concurrent.ExecutorService#isShutdown()
	 */
	@JMXAttribute(description="Returns true if this executor has been shut down.", name="Shutdown", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean isShutdown() {
		return threadPool.isShutdown();
	}

	/**
	 * Returns true if all tasks have completed following shut down.
	 * @return true if all tasks have completed following shut down
	 * @see java.util.concurrent.ExecutorService#isTerminated()
	 */
	@JMXAttribute(description="Returns true if all tasks have completed following shut down.", name="Terminated", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean isTerminated() {
		return threadPool.isTerminated();
	}
	
	/**
	 * Executes the given tasks, returning a list of Futures holding their status and results when all complete.
	 * @param tasks the collection of tasks 
	 * @return A list of Futures representing the tasks, in the same sequential order as produced by the iterator for the given task list, each of which has completed. 
	 * @throws InterruptedException
	 */
	@JMXOperation(name="invokeAll",description="Executes the given tasks, returning a list of Futures holding their status and results when all complete.")
	public <T> List<Future<T>> invokeAll(
			@JMXParameter(name="tasks",description="the collection of tasks")Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return threadPool.invokeAll((Collection<? extends Callable<T>>) tasks);
	}
	

	/**
	 * Executes the given tasks, returning a list of Futures holding their status and results when all complete or the timeout expires, whichever happens first. 
	 * @param tasks the collection of tasks
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument 
	 * @return A list of Futures representing the tasks, in the same sequential order as produced by the iterator for the given task list. 
	 * @throws InterruptedException
	 * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@JMXOperation(name="invokeAll",description="Executes the given tasks, returning a list of Futures holding their status and results when all complete.")
	public <T> List<Future<T>> invokeAll(
			@JMXParameter(name="tasks",description="the collection of tasks") Collection<? extends Callable<T>> tasks, 
			@JMXParameter(name="timeout",description="the maximum time to wait") long timeout, 
			@JMXParameter(name="unit",description="the time unit of the timeout argument ") TimeUnit unit)
			throws InterruptedException {
		return threadPool.invokeAll((Collection<? extends Callable<T>>) tasks, timeout, unit);
	}

	/**
	 * Executes the given tasks, returning the result of one that has completed successfully (i.e., without throwing an exception), if any do. 
	 * @param tasks the collection of tasks
	 * @return The result returned by one of the tasks.  
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
	 */
	@JMXOperation(name="invokeAny",description="Executes the given tasks, returning the result of one that has completed successfully (i.e., without throwing an exception), if any do.")
	public <T> T invokeAny(
			@JMXParameter(name="tasks",description="the collection of tasks") Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return threadPool.invokeAny(tasks);
	}

	/**
	 * Executes the given tasks, returning the result of one that has completed successfully (i.e., without throwing an exception), if any do.
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument 
	 * @return The result returned by one of the tasks.   
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@JMXOperation(name="invokeAny",description="Executes the given tasks, returning the result of one that has completed successfully (i.e., without throwing an exception), if any do.")
	public <T> T invokeAny(
			@JMXParameter(name="tasks",description="the collection of tasks") Collection<? extends Callable<T>> tasks, 
			@JMXParameter(name="timeout",description="the maximum time to wait") long timeout, 
			@JMXParameter(name="unit",description="the time unit of the timeout argument ") TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return threadPool.invokeAny(tasks, timeout, unit);
	}

	/**
	 * @return the requestQueue
	 */
	public BlockingQueue<Runnable> getRequestQueue() {
		return requestQueue;
	}

	/**
	 * @param requestQueue the requestQueue to set
	 */
	public void setRequestQueue(BlockingQueue<Runnable> requestQueue) {
		this.requestQueue = requestQueue;
	}

	/**
	 * Logs uncaught exceptions
	 * @param thread The threadx executing the exception block
	 * @param throwable the exception thrown from the thread
	 * TODO: Allow configurable override here.
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		uncaughtExceptionCount.incrementAndGet();
		log.warn("Uncaught exception in worker thread [" + thread + "] for pool [" + this.objectName + "]", throwable);		
	}
	
	/**
	 * Returns the cumulative count of uncaught thread exceptions
	 * @return the cumulative count of uncaught thread exceptions
	 */
	@JMXAttribute(description="The cumulative count of uncaught thread exceptions", name="UncaughtExceptionCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getUncaughtExceptionCount() {
		return uncaughtExceptionCount.get();
	}

}
