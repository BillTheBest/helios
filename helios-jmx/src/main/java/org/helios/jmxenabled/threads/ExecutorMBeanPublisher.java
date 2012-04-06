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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.reflection.PrivateAccessor;

/**
 * <p>Title: ExecutorMBeanPublisher</p>
 * <p>Description: Publishes a management MBean for an existing ExecutorThreadPool</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.ExecutorMBeanPublisher</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating thread pool shutdown", types={
                @JMXNotificationType(type=ExecutorMBeanPublisher.NOTIFICATION_SHUTDOWN)
        }),
        @JMXNotification(description="Notification indicating thread pool immediate shutdown", types={
                @JMXNotificationType(type=ExecutorMBeanPublisher.NOTIFICATION_SHUTDOWN_IMMEDIATE)
        }),        
        @JMXNotification(description="Notification indicating a rejected task in the thread pool", types={
                @JMXNotificationType(type=ExecutorMBeanPublisher.NOTIFICATION_TASK_REJECTED)
        })
})
public class ExecutorMBeanPublisher extends ManagedObjectDynamicMBean implements RejectedExecutionHandler {	
	/**  */
	private static final long serialVersionUID = 5749664980497217642L;
	/** The managed thread pool */
	protected final ThreadPoolExecutor threadPool;
	/** The class name of the executor's thread factory */
	protected final String threadFactoryName;
	/** The thread group of the executor's threads  */
	protected final String threadGroupName; 
	/** Inner rejected execution handler */
	protected final RejectedExecutionHandler rejectionHandler;
	/** A counter of rejected tasks */
	protected final AtomicLong rejectTaskCount = new AtomicLong(0);
//	/** A counter of unhandled exceptions */
//	protected final AtomicLong unhandledExceptionCount = new AtomicLong(0);

	/** The MBean's object name */
	protected ObjectName threadPoolObjectName = null;
	/** The MBeanServer where the MBean is registered */
	protected MBeanServer server = null;
	/** Instance logger */
	protected Logger log = Logger.getLogger(getClass());
//	/** static class logger */
//	protected static Logger LOG = Logger.getLogger(ExecutorMBeanPublisher.class);
	
	/** A cache of registered mbeans keyed by the ThreadPoolExecutor that is wrapped */
	private static final Map<ThreadPoolExecutor, ObjectName> registered = new ConcurrentHashMap<ThreadPoolExecutor, ObjectName>();
	/** The JMX notification type for thread pool shutdown */
	public static final String NOTIFICATION_SHUTDOWN = "org.helios.jmx.threadpool.shutdown";
	/** The JMX notification type for immediate thread pool shutdown */
	public static final String NOTIFICATION_SHUTDOWN_IMMEDIATE = "org.helios.jmx.threadpool.shutdown.immediate";
	/** The JMX notification type for thread pool rejected task */
	public static final String NOTIFICATION_TASK_REJECTED = "org.helios.jmx.threadpool.taskrejected";
	
	/**
	 * Creates a new ExecutorMBeanPublisher
	 * @param threadPool The ThreadPoolExecutor to manage
	 */
	public ExecutorMBeanPublisher(ThreadPoolExecutor threadPool) {		
		super("ThreadPoolExecutor JMX Management Service");
		this.threadPool = threadPool;
		rejectionHandler = this.threadPool.getRejectedExecutionHandler();
		threadFactoryName = threadPool.getThreadFactory().getClass().getName();		
		Thread thread = threadPool.getThreadFactory().newThread(new Runnable(){
			public void run() {}
		});
		threadGroupName = thread.getThreadGroup()==null ? "None" : thread.getThreadGroup().getName(); 		
	}
	
	/**
	 * Creates a publishes an MBean management wrapper around the provided ThreadPoolExecutor into the platform MBeanServer
	 * @param threadPoolExecutor The threadPoolExecutor to create the MBean for. If the class of this object is not an instance of ThreadPoolExecutor, a runtime exception will be thrown.
	 * @param objectName The ObjectName that the MBean should be registered with
	 * @return The ObjectName of the MBean that the threadPoolExecutor is managed by. This may be the passed ObjectName, or it may the ObjectName of an existing MBean that was already created for the threadPoolExecutor. 
	 */
	public static ObjectName register(Executor threadPoolExecutor, ObjectName objectName) {
		return register(threadPoolExecutor, objectName, JMXHelper.JMX_DOMAIN_DEFAULT);
	}

	
	/**
	 * Convenience method to shutdown an existing thread pool.
	 * @param poolObjectName The object name of the published pool
	 * @param immediate If true, will execute a {@code shutdownNow()}, otherwise, executes {@code shutdown()}. 
	 * @param jmxDomains The JMX MBeanServer domains where the pool may have been published.
	 * @return the number of located instances that were terminated.
	 */
	public static int terminate(ObjectName poolObjectName, boolean immediate, String...jmxDomains) {
		if(poolObjectName==null) throw new IllegalArgumentException("Passed pool ObjectName was null", new Throwable());
		if(jmxDomains==null || jmxDomains.length<1) throw new IllegalArgumentException("Passed JMX MBeanServer Domains was null or zero length", new Throwable());
		int count = 0;
		for(String jmxDomain: jmxDomains) {
			if(jmxDomain==null) continue;
			try {
				MBeanServer server = JMXHelper.getLocalMBeanServer(jmxDomain);
				if(server==null) continue;
				if(server.isRegistered(poolObjectName) && server.isInstanceOf(poolObjectName, ExecutorMBeanPublisher.class.getName())) {
					Boolean terminated = (Boolean)server.getAttribute(poolObjectName, "Terminated");
					Boolean terminating = (Boolean)server.getAttribute(poolObjectName, "Terminating");
					Boolean shutdown = (Boolean)server.getAttribute(poolObjectName, "Shutdown");
					if(!terminated && !terminating && !shutdown) {
						server.invoke(poolObjectName, immediate ? "shutdownNow" : "shutdown", new Object[]{}, new String[]{});
					}
					server.unregisterMBean(poolObjectName);
					count++;
				}
			} catch (Exception e) {
				//LOG.warn("Failed to terminate Published Thread Pool [" + poolObjectName + "] in JMX MBeanServer Domain [" + jmxDomain + "]:" + e);
			}
		}
		return count;
	}
			
	
	/**
	 * Creates a publishes an MBean management wrapper around the provided ThreadPoolExecutor
	 * @param threadPoolExecutor The threadPoolExecutor to create the MBean for. If the class of this object is not an instance of ThreadPoolExecutor, a runtime exception will be thrown.
	 * @param objectName The ObjectName that the MBean should be registered with
	 * @param jmxDomain The default domain name of the MBeanServer where the MBean should be registered 
	 * @return The ObjectName of the MBean that the threadPoolExecutor is managed by. This may be the passed ObjectName, or it may the ObjectName of an existing MBean that was already created for the threadPoolExecutor. 
	 */
	public static ObjectName register(Executor threadPoolExecutor, ObjectName objectName, String jmxDomain) {
		if(threadPoolExecutor==null) throw new IllegalArgumentException("Passed ThreadPoolExecutor was null", new Throwable());
		if(!(threadPoolExecutor instanceof ThreadPoolExecutor)) throw new IllegalArgumentException("Passed Executor is not an instance of javax.util.concurrent.ThreadPoolExecutor. It is a [" + threadPoolExecutor.getClass().getName() + "]"  , new Throwable());
		ObjectName on = registered.get(threadPoolExecutor);		
		if(on==null) {
			synchronized(registered) {
				on = registered.get(threadPoolExecutor);
				if(on==null) {
					if(objectName==null) throw new IllegalArgumentException("Passed ObjectName was null", new Throwable());
					if(jmxDomain==null) jmxDomain = JMXHelper.JMX_DOMAIN_DEFAULT;
					MBeanServer server = JMXHelper.getLocalMBeanServer(jmxDomain, true);
					if(server==null) throw new RuntimeException("Failed to resolve MBeanServer for default domain [" + jmxDomain + "]", new Throwable());
					on = objectName;
					ExecutorMBeanPublisher mbean = new ExecutorMBeanPublisher((ThreadPoolExecutor)threadPoolExecutor);
					mbean.reflectObject(mbean);
					if(threadPoolExecutor instanceof HeliosThreadPoolExecutorImpl) {
						
						mbean.reflectObject(((HeliosThreadPoolExecutorImpl) threadPoolExecutor).getThreadFactory());
						mbean.reflectObject(((HeliosThreadFactory)((HeliosThreadPoolExecutorImpl) threadPoolExecutor).getThreadFactory()).threadGroup);
					}
					try {
						server.registerMBean(mbean, on);
					} catch (Exception e) {
						throw new RuntimeException("Failed to register MBean with ObjectName [" + on + "] in JMX Domain [" + jmxDomain + "]", e);
					}					
				}
			}			
		}		
		return on;
	}


	/**
	 * Injects the MBeanServer where the MBean is registered and the ObjectName it was registered under.
	 * @param server the MBeanServer where the MBean is registered
	 * @param name the ObjectName it was registered under
	 * @return the ObjectName it was registered under
	 * @throws Exception
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		super.preRegister(server, name);
		this.threadPoolObjectName = name;
		StringBuilder b = new StringBuilder(); 
		String serviceName = name.getKeyProperty("service");
		String typeName = name.getKeyProperty("type");
		if(serviceName!=null) {
			b.append(".").append(serviceName);
		}
		if(typeName!=null) {
			b.append(".").append(typeName);
		}
		return name;
	}

	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		final Logger LOG = Logger.getLogger(ExecutorMBeanPublisher.class);
		LOG.info("ExecutorMBeanPublisher Test");
		Executor exec = Executors.newFixedThreadPool(3);
		register(exec, JMXHelper.objectName("org.helios.jmx.threads:service=TestExecutor"));
		final Random r = new Random(System.nanoTime());
		while(true) {
			int tasks = r.nextInt(100);
			LOG.info("Creating [" + tasks + "] tasks");
			for(int i = 0; i < tasks; i++) {
				exec.execute(new Runnable(){
					public void run() {
						if(r.nextInt(5)==1) {
							throw new RuntimeException();
						}
						try { Thread.sleep(r.nextInt(1000)); } catch (Exception e) {}
					}
				});
			}
			try { Thread.sleep(10000); } catch (Exception e) {}
		}
		
	}

	/**
	 * No Op
	 * @param registrationDone
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(Boolean registrationDone) {}


	/**
	 * No Op
	 * @throws Exception
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {}


	/**
	 * No Op
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	@Override
	public void postDeregister() {}
	
	/**
	 * Delegates rejection handling to the pool's rejection handler.
	 * @param r The runnable
	 * @param executor The pool 
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {	
		rejectTaskCount.incrementAndGet();
		if(rejectionHandler!=null) {
			rejectionHandler.rejectedExecution(r, executor);
		} else {
			log.warn("Detected rejection execution of a [" + r + "] task from this executor but no rejected execution handler is installed" );
		}		
	}	

	
	// ======================================================
	// JMX Ops and Attributes

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
		this.sendNotification(new Notification(NOTIFICATION_SHUTDOWN, this.objectName, this.nextNotificationSequence(), "Thread Pool Shutdown"));
	}


	/**
	 * Attempts to stop all actively executing tasks, halts the processing of waiting tasks, and returns the number of tasks that were awaiting execution.
	 * @return the number of tasks that were awaiting execution.
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdownNow()
	 */
	@JMXOperation(name="shutdownNow", description="Attempts to stop all actively executing tasks, halts the processing of waiting tasks, and returns the number of tasks that were awaiting execution", impact=MBeanOperationInfo.ACTION)
	public int shutdownNow() {
		List<Runnable> list = threadPool.shutdownNow();
		int taskCount = list==null ? 0 : list.size();
		Notification notif = new Notification(NOTIFICATION_TASK_REJECTED, this.objectName, this.nextNotificationSequence(), "Thread Pool Task Rejected");
		notif.setUserData(taskCount);
		return taskCount;
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
	public boolean getTerminated() {
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
	 * Returns the name of the runstate of this thread pool
	 * @return the name of the runstate of this thread pool
	 */
	@JMXAttribute(name="RunState", description="The run state of the thread pool", mutability=AttributeMutabilityOption.READ_WRITE)
	public String getRunState() {
		int code = (Integer)PrivateAccessor.getFieldValue(threadPool, "runState");
		return RunState.valueOf(code).name();
	}
	
	/**
	 * <p>Title: RunState</p>
	 * <p>Description: Enumerates the runstate of the thread pool executor</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.jmxenabled.threads.ExecutorMBeanPublisher.RunState</code></p>
	 */
	public static enum RunState {
	    RUNNING,
	    SHUTDOWN,
	    STOP,
	    TERMINATED,
	    UNKNOWN;
	    
	    private static final Map<Integer, RunState> CODE2NAME = new HashMap<Integer, RunState>(RunState.values().length);
	    
	    static {
	    	for(RunState r: RunState.values()) {
	    		CODE2NAME.put(r.ordinal(), r);
	    	}
	    }
	    
	    /**
	     * Returns the RunState for the passed ordinal
	     * @param code The ordinal of the runstate
	     * @return The runstate or UNKNOWN if the code did not match.
	     */
	    public static RunState valueOf(int code) {
	    	RunState r = CODE2NAME.get(code);
	    	return r==null ? UNKNOWN : r;
	    }
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
	public boolean getsAllowsCoreThreadTimeOut() {
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
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
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
	 * Sets the termination time
	 * @param terminationWaitTime the amount of time (in ms.) to wait for the pool to shutdown
	 */
	public void setTerminationTime(long terminationWaitTime) {
		if(threadPool instanceof HeliosThreadPoolExecutorImpl) {
			if(terminationWaitTime<1L) throw new IllegalArgumentException("Invalid value for termination time [" + terminationWaitTime + "]", new Throwable());
			((HeliosThreadPoolExecutorImpl)threadPool).setTerminationTime(terminationWaitTime);
		}
	}

	/**
	 * Returns the termination time (in ms.)
	 * @return The amount of time (in ms.) to wait for the pool to shutdown
	 */
	@JMXAttribute(name="TerminationTime", description="The amount of time (in ms.) to wait for the pool to shutdown",  mutability=AttributeMutabilityOption.READ_WRITE)
	public long getTerminationTime() {
		if(threadPool instanceof HeliosThreadPoolExecutorImpl) {			
			return ((HeliosThreadPoolExecutorImpl)threadPool).getTerminationTime();
		}
		return -1;
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
		return threadFactoryName;
	}

	/**
	 * The name of the executor's thread's thread group
	 * @return the threadGroupName
	 */
	@JMXAttribute(name="ThreadGroupName", description="The name of the thread pool's thread group", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getThreadGroupName() {
		return threadGroupName;
	}

	/**
	 * Returns the number of rejected tasks
	 * @return the rejectTaskCount
	 */
	@JMXAttribute(name="RejectedTaskCount", description="The number of rejected tasks", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRejectedTaskCount() {
		return rejectTaskCount.get();
	}

}
