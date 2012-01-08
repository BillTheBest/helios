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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanOperationInfo;
import javax.management.Notification;

import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: HeliosThreadPoolBase</p>
 * <p>Description: Base class for HeliosThreadPools.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.HeliosThreadPoolBase</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating thread pool shutdown", types={
                @JMXNotificationType(type=HeliosThreadPoolBase.NOTIFICATION_SHUTDOWN)
        }),
        @JMXNotification(description="Notification indicating thread pool immediate shutdown", types={
                @JMXNotificationType(type=HeliosThreadPoolBase.NOTIFICATION_SHUTDOWN_IMMEDIATE)
        }),        
        @JMXNotification(description="Notification indicating a rejected task in the thread pool", types={
                @JMXNotificationType(type=HeliosThreadPoolBase.NOTIFICATION_TASK_REJECTED)
        })
})
public class HeliosThreadPoolBase implements ExecutorService {
	/**  */
	private static final long serialVersionUID = -4440387986555425915L;
	/** The managed thread pool */
	protected final ThreadPoolExecutor threadPool;
	/** The modb for publishing a management interface for this instance */
	protected final ManagedObjectDynamicMBean modb;
	/** The thread group that this executor's threads live in */
	protected final ThreadGroup threadGroup;
	/** A counter of rejected tasks */
	protected final AtomicLong rejectTaskCount = new AtomicLong(0L);
	/** A counter of uncaught exceptions */
	protected final AtomicLong uncaughtExceptionCount = new AtomicLong(0L);
	/** The thread factory that creates threads for this pool */
	protected final HeliosThreadFactory threadFactory;
	/** The pool description */
	protected final String description;
	
	/** The JMX notification type for thread pool shutdown */
	public static final String NOTIFICATION_SHUTDOWN = "org.helios.jmx.threadpool.shutdown";
	/** The JMX notification type for immediate thread pool shutdown */
	public static final String NOTIFICATION_SHUTDOWN_IMMEDIATE = "org.helios.jmx.threadpool.shutdown.immediate";
	/** The JMX notification type for thread pool rejected task */
	public static final String NOTIFICATION_TASK_REJECTED = "org.helios.jmx.threadpool.taskrejected";
	

	/**
	 * Creates a new HeliosThreadPoolBase
	 * @param threadPool The underlying thread pool
	 */
	protected HeliosThreadPoolBase(ThreadPoolExecutor threadPool) {
		this(threadPool, null);
	}
	
	
	/**
	 * Creates a new HeliosThreadPoolBase
	 * @param threadPool The underlying thread pool
	 * @param description An arbitrary description. If null, will simply use the class name
	 */
	protected HeliosThreadPoolBase(ThreadPoolExecutor threadPool, String description) {
		super();
		this.threadPool = threadPool;	
		this.threadFactory = (HeliosThreadFactory)this.threadPool.getThreadFactory();
		this.threadGroup = null;
		this.description = (description==null) ?  getClass().getSimpleName() : description;
		modb = new ManagedObjectDynamicMBean(this.description);
	}

	
	/**
	 * Executes the given command at some time in the future. The command may execute in a new thread, in a pooled thread, or in the calling thread, at the discretion of the Executor implementation. 
	 * @param command the runnable task 
	 */
	@Override
	@JMXOperation(name="execute", description="Executes the given command at some time in the future", impact=MBeanOperationInfo.ACTION)
	public void execute(@JMXParameter(name="command",description="The task to submit") Runnable command) {
		threadPool.execute(command);
	}

	/**
	 * Submits a Runnable task for execution and returns a Future representing that task. 
	 * @param task the task to submit
	 * @return  a Future representing pending completion of the task
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable)
	 */
	@JMXOperation(name="submit", description="Submits a Runnable task for execution and returns a Future representing that task", impact=MBeanOperationInfo.ACTION)
	public Future<?> submit(@JMXParameter(name="task",description="The task to submit") Runnable task) {
		return threadPool.submit(task);
	}


	/**
	 * Submits a Runnable task for execution and returns a Future representing that task. 
	 * @param task the task to submit
	 * @param result the result to return 
	 * @return a Future representing pending completion of the task
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable, java.lang.Object)
	 */
	@JMXOperation(name="submit", description="Submits a Runnable task for execution and returns a Future representing that task", impact=MBeanOperationInfo.ACTION)
	public <T> Future<T> submit(@JMXParameter(name="task",description="The task to submit") Runnable task, @JMXParameter(name="result",description="The result to return")T result) {
		return threadPool.submit(task, result);
	}


	/**
	 * Submits a value-returning task for execution and returns a Future representing the pending results of the task.
	 * @param task the task to submit 
	 * @return a Future representing pending completion of the task
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.util.concurrent.Callable)
	 */
	@JMXOperation(name="submit", description="Submits a value-returning task for execution and returns a Future representing the pending results of the task", impact=MBeanOperationInfo.ACTION)
	public <T> Future<T> submit(@JMXParameter(name="task",description="The task to submit")Callable<T> task) {
		return threadPool.submit(task);
	}


	/**
	 * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdown()
	 */
	@JMXOperation(name="shutdown", description="Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted", impact=MBeanOperationInfo.ACTION)
	public void shutdown() {
		threadPool.shutdown();
		modb.sendNotification(new Notification(NOTIFICATION_SHUTDOWN, getPoolSource(), modb.nextNotificationSequence(), "Thread Pool [" + description + "] Shutdown"));
	}


	/**
	 * Attempts to stop all actively executing tasks, halts the processing of waiting tasks, and returns the number of tasks that were awaiting execution.
	 * @return the number of tasks that were awaiting execution.
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdownNow()
	 */
	@JMXOperation(name="shutdownNow", description="Attempts to stop all actively executing tasks, halts the processing of waiting tasks, and returns the number of tasks that were awaiting execution", impact=MBeanOperationInfo.ACTION)
	public List<Runnable> shutdownNow() {
		List<Runnable> list = threadPool.shutdownNow();
		int taskCount = list==null ? 0 : list.size();
		if(modb!=null) {
			Notification notif = new Notification(NOTIFICATION_TASK_REJECTED, getPoolSource(), modb.nextNotificationSequence(), "Thread Pool [" + description + "] Task Rejected");
			notif.setUserData(taskCount);
			modb.sendNotification(notif);
		}
		return list;
	}
	
	/**
	 * Creates an identifiable source for notifications
	 * @return the source for all notifications sent from this object
	 */
	protected Object getPoolSource() {
		if(modb.getObjectName()!=null) {
			return modb.getObjectName();
		} else {
			return description;
		}
	}


	/**
	 * Returns true if this executor has been shut down.
	 * @return true if this executor has been shut down.
	 * @see java.util.concurrent.ThreadPoolExecutor#isShutdown()
	 */
	@JMXAttribute(name="Shutdown", description="Returns true if this executor has been shut down", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getShutdown() {
		return threadPool.isShutdown();
	}


	/**
	 * Returns true if this executor is in the process of terminating after shutdown or shutdownNow but has not completely terminated.
	 * @return true if this executor is in the process of terminating
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminating()
	 */
	@JMXAttribute(name="Terminating", description="Returns true if this executor is in the process of terminating after shutdown or shutdownNow but has not completely terminated", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getTerminating() {
		return threadPool.isTerminating();
	}


	/**
	 * Returns true if all tasks have completed following shut down.
	 * @return true if all tasks have completed following shut down.
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	@JMXAttribute(name="Terminated", description="Returns true if all tasks have completed following shut down", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean isTerminated() {
		return threadPool.isTerminated();
	}

	/**
	 * Sets the core number of threads. If the new value is smaller than the current value, 
	 * excess existing threads will be terminated when they next become idle. 
	 * If larger, new threads will, if needed, be started to execute any queued tasks. 
	 * @param corePoolSize the new core size 
	 * @see java.util.concurrent.ThreadPoolExecutor#setCorePoolSize(int)
	 */
	public void setCorePoolSize(int corePoolSize) {
		threadPool.setCorePoolSize(corePoolSize);
	}


	/**
	 * Returns the core number of threads. 
	 * @return the core number of threads. 
	 * @see java.util.concurrent.ThreadPoolExecutor#getCorePoolSize()
	 */
	@JMXAttribute(name="CorePoolSize", description="The core number of threads", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getCorePoolSize() {
		return threadPool.getCorePoolSize();
	}


	/**
	 * Returns true if this pool allows core threads to time out and terminate if no tasks arrive within the keepAlive time 
	 * being replaced if needed when new tasks arrive. When true, 
	 * the same keep-alive policy applying to non-core threads applies also to core threads. 
	 * When false (the default), core threads are never terminated due to lack of incoming tasks. 
	 * @return true if core threads are allowed to time out, else false
	 * @see java.util.concurrent.ThreadPoolExecutor#allowsCoreThreadTimeOut()
	 */
	@JMXAttribute(name="AllowsCoreThreadTimeOut", description="Returns true if this pool allows core threads to time out and terminate if no tasks arrive within the keepAlive time", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getAllowsCoreThreadTimeOut() {
		return threadPool.allowsCoreThreadTimeOut();
	}
	
	/**
	 * Sets the policy governing whether core threads may time out and terminate if no tasks arrive within the keep-alive time, 
	 * being replaced if needed when new tasks arrive. When false, 
	 * core threads are never terminated due to lack of incoming tasks. 
	 * When true, the same keep-alive policy applying to non-core threads applies also to core threads. 
	 * To avoid continual thread replacement, the keep-alive time must be greater than zero when setting true. 
	 * This method should in general be called before the pool is actively used.
	 * @param allow true if core threads are allowed to time out, else false
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void setAllowsCoreThreadTimeOut(boolean allow) {
		threadPool.allowCoreThreadTimeOut(allow);
	}


	/**
	 * Sets the maximum allowed number of threads. 
	 * If the new value is smaller than the current value, excess existing threads will be terminated when they next become idle. 
	 * @param maximumPoolSize the new maximum 
	 * @see java.util.concurrent.ThreadPoolExecutor#setMaximumPoolSize(int)
	 */
	public void setMaximumPoolSize(int maximumPoolSize) {
		threadPool.setMaximumPoolSize(maximumPoolSize);
	}


	/**
	 * Returns the maximum allowed number of threads.  
	 * @return the maximum allowed number of threads. 
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	@JMXAttribute(name="MaximumPoolSize", description="The maximum number of threads", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getMaximumPoolSize() {
		return threadPool.getMaximumPoolSize();
	}


	/**
	 * Sets the time limit for which threads may remain idle before being terminated. If there are more than the core number of threads currently in the pool, after waiting this amount of time without processing a task, excess threads will be terminated. 
	 * @param time The keep alive time in ms.
	 * @see java.util.concurrent.ThreadPoolExecutor#setKeepAliveTime(long, java.util.concurrent.TimeUnit)
	 */
	public void setKeepAliveTime(long time) {
		threadPool.setKeepAliveTime(time, TimeUnit.MILLISECONDS);
	}


	/**
	 * Returns the keep alive time in ms.
	 * @return The keep alive time in ms.
	 * @see java.util.concurrent.ThreadPoolExecutor#getKeepAliveTime(java.util.concurrent.TimeUnit)
	 */
	@JMXAttribute(name="KeepAliveTime", description="The idle thread keep alive time in ms.", mutability=AttributeMutabilityOption.READ_WRITE)
	public long getKeepAliveTime() {
		return threadPool.getKeepAliveTime(TimeUnit.MILLISECONDS);
	}


	/**
	 * Returns the thread pools queue depth
	 * @return the thread pools queue depth
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
	 */
	@JMXAttribute(name="QueueDepth", description="The thread pool queue depth", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getQueueDepth() {
		return threadPool.getQueue().size();
	}
	
	/**
	 * Returns the thread pools queue remaining capacity
	 * @return the thread pools queue remaining capacity
	 */
	@JMXAttribute(name="QueueRemainingCapacity", description="The thread pool queue remaining capacity", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getQueueRemainingCapacity() {		
		return threadPool.getQueue().remainingCapacity();
	}
	
	/**
	 * Tries to remove from the work queue all Future tasks that have been cancelled.
	 * @see java.util.concurrent.ThreadPoolExecutor#purge()
	 */
	@JMXOperation(name="purge", description="Tries to remove from the work queue all Future tasks that have been cancelled.")
	public void purge() {
		threadPool.purge();
	}


	/**
	 * Returns the current number of threads in the pool.
	 * @return the current number of threads in the pool.
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	@JMXAttribute(name="PoolSize", description="The number of threads in the thread pool", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getPoolSize() {
		return threadPool.getPoolSize();
	}


	/**
	 * Returns the approximate number of threads that are actively executing tasks.
	 * @return the approximate number of threads that are actively executing tasks.
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	@JMXAttribute(name="ActiveCount", description="The approximate number of threads that are actively executing tasks", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getActiveCount() {
		return threadPool.getActiveCount();
	}


	/**
	 * Returns the largest number of threads that have ever simultaneously been in the pool.
	 * @return the largest number of threads that have ever simultaneously been in the pool.
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	@JMXAttribute(name="LargestPoolSize", description="The largest number of threads that have ever simultaneously been in the pool", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getLargestPoolSize() {
		return threadPool.getLargestPoolSize();
	}


	/**
	 * Returns the approximate total number of tasks that have ever been scheduled for execution.
	 * @return the approximate total number of tasks that have ever been scheduled for execution.
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	@JMXAttribute(name="TaskCount", description="The approximate total number of tasks that have ever been scheduled for execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTaskCount() {
		return threadPool.getTaskCount();
	}


	/**
	 * Returns the approximate total number of tasks that have completed execution.
	 * @return the approximate total number of tasks that have completed execution.
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	@JMXAttribute(name="CompletedTaskCount", description="The approximate total number of tasks that have completed execution", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCompletedTaskCount() {
		return threadPool.getCompletedTaskCount();
	}

	/**
	 * The class name of the executor's ThreadFactory
	 * @return the threadFactoryName
	 */
	@JMXAttribute(name="ThreadFactoryName", description="The class name of the thread pool's thread factory", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getThreadFactoryName() {
		return threadFactory.getClass().getName();
	}
	
	/**
	 * The pool's description
	 * @return the description
	 */
	@JMXAttribute(name="Description", description="The thread pool's description", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDescription() {
		return description;
	}
	

	/**
	 * The name of the executor's thread's thread group
	 * @return the threadGroupName
	 */
	@JMXAttribute(name="ThreadGroupName", description="The name of the thread pool's thread group", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getThreadGroupName() {
		return threadGroup.getName();
	}

	/**
	 * Returns the number of rejected tasks
	 * @return the rejectTaskCount
	 */
	@JMXAttribute(name="RejectedTaskCount", description="The number of rejected tasks", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRejectedTaskCount() {
		return rejectTaskCount.get();
	}


	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return threadPool.awaitTermination(timeout, unit);
	}


	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return threadPool.invokeAll(tasks);
	}


	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		return threadPool.invokeAll(tasks, timeout, unit);				
	}


	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return threadPool.invokeAny(tasks);
	}


	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return threadPool.invokeAny(tasks, timeout, unit);
	}


	@Override
	public boolean isShutdown() {
		return threadPool.isShutdown();
	}




}
