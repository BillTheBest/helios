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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.concurrency.ContinuationBarrier;
import org.helios.jmx.threadservices.metrics.AggregatedMetricsManager;
import org.helios.jmx.threadservices.metrics.ThreadResourceSnapshot;
import org.helios.jmx.threadservices.scheduling.ScheduledTaskHandle;
import org.helios.jmx.threadservices.scheduling.ScheduledTaskListener;
import org.helios.jmx.threadservices.scheduling.ScheduledTaskListenerExecutor;
import org.helios.jmx.threadservices.scheduling.SchedulingType;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerListener;

/**
 * <p>Title: QuartzDeferredTaskResult</p>
 * <p>Description: A ScheduledFuture implementation for accessing and canceling Quartz deferred task jobs.</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 * @param <V>
 * @param <V>
 */
@SuppressWarnings("unchecked")
@JMXManagedObject(declared=true, annotated=true)
public class QuartzDeferredTaskResult<V> extends ManagedObjectDynamicMBean implements ScheduledTaskHandle<V>, TriggerListener {

	/** The task Id of the job */
	protected String taskId = null;
	/** The group Id of the job */
	protected String groupId = null;
	/** The scheduler where the job is registered */
	protected Scheduler scheduler = null;
	/** The trigger firing the job  */
	protected Trigger trigger = null;
	/** The constructed name of the trigger listener */
	protected String triggerListenerName = null;
	/** The Id of the task state */
	protected AtomicInteger state = new AtomicInteger(QuartzExecutionTaskState.UNFIRED_ID);
	/** the execute count of the task */
	protected AtomicInteger executeCount = new AtomicInteger(0);
	/** the fire count of the task */
	protected AtomicInteger fireCount = new AtomicInteger(0);

	/** the completed count of the task */
	protected AtomicInteger completeCount = new AtomicInteger(0);
	/** the failure count of the task */
	protected AtomicInteger failCount = new AtomicInteger(0);
	/** the veto count of the task */
	protected AtomicInteger vetoCount = new AtomicInteger(0);
	/** the last execution time */
	protected AtomicLong lastExecutionTime = new AtomicLong(0);
	/** the last start time */
	protected AtomicLong lastStartTime = new AtomicLong(0);


	/** a set of scheduled task listener */
	protected Map<ScheduledTaskListener, ScheduledTaskListener> listeners = new ConcurrentHashMap<ScheduledTaskListener, ScheduledTaskListener>();

	/** enables/disables the trigger veto. supports the task pause. */
	protected AtomicBoolean vetoEnabled = new AtomicBoolean(false);

	/** The result of the call */
	protected AtomicReference<V> result = new AtomicReference(null);
	/** The exception thrown from the call */
	protected AtomicReference executionException = new AtomicReference(null);
	/** A barrier that all get() calling threads must wait on until the task and result are complete */
	protected ContinuationBarrier resultAccessBarrier = new ContinuationBarrier("ResultAccessBarrier", true);
	/** A barrier that all get() calling threads must wait on until the task and result are complete */
	protected ContinuationBarrier nextExecutionBarrier = new ContinuationBarrier("NextExecutionBarrier", true);
	/** A thread pool that listener event invocations can be dispatched to */
	protected ExecutorService threadPool = null;
	/** An mbeanserver where this object will be registered */
	protected MBeanServer mbeanServer = null;
	/** ObjectName the task will be registered with */
	protected ObjectName objectName = null;
	/** The scheduling task type */
	protected SchedulingType schedType = null;
	/** Indicates if task execution instrumentation should be enabled */
	protected boolean enableInstrumentation = true;
	/** The metric aggregator for managing instrumentation generated stats */
	protected AggregatedMetricsManager agmm = null;


	/** A lock on accessing results to ensure that methods from conflicting threads act in an atomic fassion */
	protected ReentrantLock lock = new ReentrantLock();
	/** Instance logger */
	protected Logger log = null;

	/*
	public static final int UNFIRED_ID = 0;
	public static final int FIRING_ID = 1;
	public static final int FIRED_ID = 2;
	public static final int COMPLETED_ID = 3;
	public static final int EXCEPTION_ID = 4;
	public static final int COMPLETED_CONTINUING_ID = 5;
	public static final int EXCEPTION_CONTINUING_ID = 6;
	public static final int CANCELLED_ID = 7;
	*/



	/**
	 * Creates a new QuartzDeferredTaskResult.
	 * @param trigger The trigger firing the job
	 * @param scheduler The scheduler where the job is registered
	 * @param threadPool A thread pool that listener event invocations can be dispatched to
	 * @param mbeanServer An mbeanserver where this object will be registered. Ignored if null.
	 * @throws SchedulerException Thrown if trigger listener cannot be registered with the scheduler.
	 */
	public QuartzDeferredTaskResult(Trigger trigger, Scheduler scheduler, ExecutorService threadPool, MBeanServer mbeanServer, SchedulingType schedType) throws SchedulerException {
		super();
		log = Logger.getLogger(getClass().getName() + "." + trigger.getJobName());
		log.info("Created QuartzDeferredTaskResult");
		this.threadPool = threadPool;
		this.mbeanServer = mbeanServer;
		this.taskId = trigger.getJobName();
		this.groupId = trigger.getJobGroup();
		this.scheduler = scheduler;
		this.trigger = trigger;
		triggerListenerName = groupId + "/" + taskId;
		scheduler.addTriggerListener(this);
		this.trigger.addTriggerListener(triggerListenerName);
		this.schedType = schedType;
		if(schedType!=SchedulingType.DEFERRED) {
			this.reflectObject(this);
			try {
				objectName = JMXHelperExtended.objectName("org.helios.scheduling.tasks:name=" + taskId.replace(":", "@").replace("=", "-").replace(",", ".") + ",type=" + schedType.name());
				if(mbeanServer.isRegistered(objectName)) {
					mbeanServer.unregisterMBean(objectName);
				}
				mbeanServer.registerMBean(this, objectName);
			} catch (Exception e) {
				log.warn("Task[" + triggerListenerName + "] Could not register management interface", e);
			}
		}
		if(enableInstrumentation) {
			agmm = new AggregatedMetricsManager(false);
			this.reflectObject(agmm);
		}

//		resultAccessBarrier = new ContinuationBarrier(true);
//		nextExecutionBarrier= new ContinuationBarrier(false);
//		if(taskId.startsWith(HeliosScheduler.DEFERRED_TASK)) {
//			nextExecutionBarrier= new ContinuationBarrier(true);
//		} else {
//			nextExecutionBarrier= new ContinuationBarrier(false);
//		}
	}

	/**
	 * Renders the taskResult into a readable string.
	 * @return A string describing the taskResult.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder("QuartzDeferredTaskResult:");
		buff.append("\n\tState:").append(QuartzExecutionTaskState.valueOf(state.get()));
		buff.append("\n\tTriggerListenerName:").append(triggerListenerName);
		buff.append("\n\tJobName:").append(taskId);
		buff.append("\n\tJobGroup:").append(groupId);
		buff.append("\n\tNextFireTime:").append(trigger.getNextFireTime());
		buff.append("\n\tPreviousFireTime:").append(trigger.getPreviousFireTime());
		return buff.toString();
	}

	/**
	 * Returns the remaining delay associated with this object, in the given time unit.
	 * @param unit the time unit
	 * @return the remaining delay; zero or negative values indicate that the delay has already elapsed
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	public long getDelay(TimeUnit unit) {
		long msDelay = trigger.getNextFireTime().getTime()-System.currentTimeMillis();
		return unit.convert(msDelay, TimeUnit.MILLISECONDS);
	}



	/**
	 * Compares this object with the specified object for order. Returns a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
	 * @param delayed
	 * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Delayed delayed) {
		Long thatDelay = delayed.getDelay(TimeUnit.MILLISECONDS);
		Long thisDelay = this.getDelay(TimeUnit.MILLISECONDS);
		return thisDelay.compareTo(thatDelay);
	}


	/**
	 * Causes the current thead to wait on the next execution of the trigger.
	 * @throws InterruptedException
	 */
	public void waitOnNextExecution() throws InterruptedException {
		if(state.get()==QuartzExecutionTaskState.CANCELLED_ID) {
			return;
		} else {
			if(log.isDebugEnabled())log.debug("Waiting on next execution:" + nextExecutionBarrier);
			nextExecutionBarrier.await();
			if(log.isDebugEnabled())log.debug("Next Execution Barrier Dropped");
		}
	}

	/**
	 * Causes the current thead to wait on the next execution of the trigger for the specified period.
	 * @param wait The timeout period.
	 * @param unit The unit of the period.
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public void waitOnNextExecution(long wait, TimeUnit unit) throws InterruptedException, TimeoutException {
		if(state.get()==QuartzExecutionTaskState.CANCELLED_ID) {
			return;
		} else {
			//nextExecutionBarrier.await(wait, unit);
			nextExecutionBarrier.await();

		}
	}


	/**
	 * Drops the nextExecutionBarrier and raises the resultAccessBarrier.
	 * Executed by <code>triggerFired</code>.
	 */
	protected void taskStarting() {
		if(log.isDebugEnabled()) log.debug("Raising Result Access Barrier");
		resultAccessBarrier.raiseBarrier();
		if(log.isDebugEnabled()) log.debug("Dropping Next Execution Barrier");
		nextExecutionBarrier.dropBarrier();
	}

	/**
	 * Raises the nextExecutionBarrier and drops the resultAccessBarrier.
	 * Executed by <code>triggerComplete</code> and <code>cancel</code>.
	 */
	protected void taskComplete() {
		if(log.isDebugEnabled()) log.debug("Raising Next Execution Barrier");
		nextExecutionBarrier.raiseBarrier();
		if(log.isDebugEnabled()) log.debug("Dropping Result Access Barrier");
		resultAccessBarrier.dropBarrier();
	}



	/**
	 * Attempts to cancel execution of this task.
	 * This attempt will fail if the task has already completed, already been cancelled, or could not be cancelled for some other reason.
	 * If successful, and this task has not started when cancel is called, this task should never run.
	 * If the task has already started, then the mayInterruptIfRunning parameter determines whether the thread executing this task should be interrupted in an attempt to stop the task.
	 * @param mayInterruptIfRunning true if the thread executing this task should be interrupted; otherwise, in-progress tasks are allowed to complete
	 * @return false if the task could not be cancelled, typically because it has already completed normally; true otherwise
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	public boolean cancel(boolean mayInterruptIfRunning) {
		if(log.isDebugEnabled())log.debug("Cancelling....");
		lock.lock();
		try {
			if(log.isDebugEnabled())log.debug("Cancel Request Acquired Lock");
			if(schedType==SchedulingType.DEFERRED) {
				if(state.get()==QuartzExecutionTaskState.UNFIRED_ID) {
					scheduler.unscheduleJob(taskId, groupId);
					if(log.isDebugEnabled())log.debug("Job Unscheduled");
					try {
						scheduler.removeTriggerListener(((TriggerListener)this).getName());
					} catch (Exception e) {}
					state.set(QuartzExecutionTaskState.CANCELLED_ID);
					return true;
				} else {
					return false;
				}
			} else {
				scheduler.unscheduleJob(taskId, groupId);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if(log.isDebugEnabled())log.debug("Unlocking...");
			try {
				if(mbeanServer != null) {
					mbeanServer.unregisterMBean(objectName);
				}
			} catch (Exception e) {}
			lock.unlock();
			taskComplete();
			for(ScheduledTaskListener listener: listeners.values()) {
				listener.onTaskCancelled(taskId);
			}
			if(log.isDebugEnabled())log.debug("Unlocked");
		}
	}

	/**
	 * Returns true if this task was cancelled before it completed normally.
	 * @return true if task was cancelled before it completed
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	public boolean isCancelled() {
		return state.get()==QuartzExecutionTaskState.CANCELLED_ID;
	}



	/**
	 * Returns true if this task completed. Completion may be due to normal termination, an exception, or cancellation -- in all of these cases, this method will return true.
	 * @return true if this task completed.
	 * @see java.util.concurrent.Future#isDone()
	 */
	public boolean isDone() {
		return QuartzExecutionTaskState.valueOf(state.get()).isDone();
	}


	/**
	 * Waits if necessary for the computation to complete, and then retrieves its result.
	 * @return the computed result. If the task was a Runnnable, there is no result and a null will be returned.
	 * If an exception was thrown in the task execution, it will be rethrown here.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @see java.util.concurrent.Future#get()
	 */
	public V get() throws InterruptedException, ExecutionException {
		try {
			if(log.isDebugEnabled())log.debug("Waiting on get()");
			//lock.lockInterruptibly();
			if(log.isDebugEnabled())log.debug("Passed get() lock. State:" + QuartzExecutionTaskState.valueOf(state.get()));
			resultAccessBarrier.await();
			return processReturnResult();
//			if(!isDone()) {
//				if(log.isDebugEnabled())log.debug("get() found task not done");
//				lock.unlock();
//				// Could we miss a thread synch here ?
//				if(log.isDebugEnabled())log.debug("Waiting on get() resultAccessBarrier");
//				resultAccessBarrier.await();
//				if(log.isDebugEnabled())log.debug("Passed get() resultAccessBarrier");
//				return processReturnResult();
//			} else {
//				if(log.isDebugEnabled())log.debug("get() found task done");
//				return processReturnResult();
//			}
		} finally {
			if(lock.isHeldByCurrentThread()) {
				lock.unlock();
				if(log.isDebugEnabled())log.debug("get() unlocked");
			}
		}
	}

	/**
	 * Returns the completed task's result or throws an exception if the task threw an exception.
	 * @return The result of the task. May be null if the task was a runnable.
	 * @throws ExecutionException
	 */
	protected V processReturnResult() throws ExecutionException {
		if(executionException.get() != null) {
			throw new ExecutionException("Deferred Task Encountered an Exception", (Throwable)executionException.get());
		} else {
			return result.get();
		}
	}

	/**
	 * Waits the defined timeout period if necessary for the computation to complete, and then retrieves its result.
	 * @param timeout the period to wait for timeout.
	 * @param unit The unit of the timeout period
	 * @return the computed result. If the task was a Runnnable, there is no result and a null will be returned.
	 * If an exception was thrown in the task execution, it will be rethrown here.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	public V get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		try {
			lock.lockInterruptibly();
			if(!isDone()) {
				lock.unlock();
				// Could we miss a thread synch here ?
				Thread.interrupted();
				resultAccessBarrier.await(timeout, unit);
				//resultAccessBarrier.await();
				if(Thread.currentThread().isInterrupted()) {
					throw new InterruptedException("Task was not complete when thread was interrupted.");
				}
				if(!isDone()) {
					throw new TimeoutException("Task was not complete when timeout period expired.");
				}
				return processReturnResult();
			} else {
				return processReturnResult();
			}
		} finally {
			if(lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}


	/**
	 * Gets the name of this trigger listener.
	 * @return The name of this trigger listener.
	 * @see org.quartz.TriggerListener#getName()
	 */
	public String getName() {
		return triggerListenerName;
	}

	/**
	 * @param trigger
	 * @param context
	 * @param triggerInstructionCode
	 * @see org.quartz.TriggerListener#triggerComplete(org.quartz.Trigger, org.quartz.JobExecutionContext, int)
	 */
	public void triggerComplete(Trigger trigger, JobExecutionContext context, int triggerInstructionCode) {
		lastExecutionTime.set(System.currentTimeMillis()-lastStartTime.get());
		if(log.isDebugEnabled())log.debug("TRIGGER EVENT: Trigger Complete");
		state.set(QuartzExecutionTaskState.FIRED_ID);
		executeCount.incrementAndGet();
		Job job = context.getJobInstance();
		if(job instanceof QuartzExecutionTask) {
			QuartzExecutionTask quartzExecutionTask = (QuartzExecutionTask)job;
			result.set((V)quartzExecutionTask.getResult());
			executionException.set(quartzExecutionTask.getExecutionException());
			int fCount = 0;
			if(executionException.get()==null) {
				fCount = completeCount.incrementAndGet();
				for(ScheduledTaskListener listener: listeners.values()) {
					listener.onTaskComplete(taskId, result.get(), fCount);
				}
			} else {
				fCount = failCount.incrementAndGet();
				for(ScheduledTaskListener listener: listeners.values()) {
					listener.onTaskFailed(taskId, fCount);
				}

			}
		}
		taskComplete();

		if(trigger.getNextFireTime()==null) {
			state.set(QuartzExecutionTaskState.COMPLETED_ID);
			try {
				scheduler.removeTriggerListener(((TriggerListener)this).getName());
			} catch (Exception e) {}
			try {
				if(mbeanServer != null) {
					mbeanServer.unregisterMBean(objectName);
				}
			} catch (Exception e) {}
			for(ScheduledTaskListener listener: listeners.values()) {
				listener.onTaskCancelled(taskId);
			}

		} else {
			state.set(QuartzExecutionTaskState.COMPLETED_CONTINUING_ID);
		}
		Object result = context.getResult();
		if(result != null && result instanceof ThreadResourceSnapshot) {
			processThreadResourceSnapshot((ThreadResourceSnapshot)result, (executionException.get()==null));
		}
	}

	/**
	 * Aggregates the metrics from these metric stats.
	 * @param tsr The ThreadResourceSnapshot generated from the job execution.
	 */
	protected void processThreadResourceSnapshot(ThreadResourceSnapshot trs, boolean successful) {
		if(agmm!=null) {
			agmm.update(trs, successful);
		}
	}

	/**
	 * @param trigger
	 * @param context
	 * @see org.quartz.TriggerListener#triggerFired(org.quartz.Trigger, org.quartz.JobExecutionContext)
	 */
	public void triggerFired(Trigger trigger, JobExecutionContext context) {
		lastStartTime.set(System.currentTimeMillis());
		if(log.isDebugEnabled())log.debug("TRIGGER EVENT: Trigger Fired");
		taskStarting();
		executionException.set(null);
		state.set(QuartzExecutionTaskState.FIRING_ID);
		int fCount = fireCount.incrementAndGet();
		for(ScheduledTaskListener listener: listeners.values()) {
			listener.onTaskFired(taskId, fCount);
		}

	}

	/**
	 * @param trigger
	 * @see org.quartz.TriggerListener#triggerMisfired(org.quartz.Trigger)
	 */
	public void triggerMisfired(Trigger trigger) {
		if(log.isDebugEnabled())log.debug("TRIGGER EVENT: Trigger Misfired");
		state.set(QuartzExecutionTaskState.FIRED_ID);
		if(trigger.getNextFireTime()==null) {
			state.set(QuartzExecutionTaskState.EXCEPTION_ID);
		} else {
			state.set(QuartzExecutionTaskState.EXCEPTION_CONTINUING_ID);
		}
		int fCount = failCount.incrementAndGet();
		for(ScheduledTaskListener listener: listeners.values()) {
			listener.onTaskFailed(taskId, fCount);
		}
	}

	/**
	 * @param trigger
	 * @param context
	 * @return
	 * @see org.quartz.TriggerListener#vetoJobExecution(org.quartz.Trigger, org.quartz.JobExecutionContext)
	 */
	public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
		state.set(QuartzExecutionTaskState.FIRING_ID);
		if(vetoEnabled.get()) {
			int vCount = vetoCount.incrementAndGet();
			for(ScheduledTaskListener listener: listeners.values()) {
				listener.onTaskVetoed(taskId, vCount);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * The number of times the task has been executed.
	 * @return the executionCount
	 */
	@JMXAttribute(name="ExecuteCount", description="The number of times the task has been executed.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getExecuteCount() {
		return executeCount.get();
	}

	/**
	 * Determines if the task trigger is being vetoed.
	 * If true, the trigger will be vetoed when fired.
	 * @return the veto Enabled state
	 */
	@JMXAttribute(name="Paused", description="The paused state of the task.", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getPaused() {
		return vetoEnabled.get();
	}

	/**
	 * Enables and disables the veto.
	 * If true, the trigger will be vetoed when fired.
	 * @param vetoEnabled the state to set veto Enabled to
	 */
	public void setPaused(boolean vetoEnabled) {
		this.vetoEnabled.set(vetoEnabled);
	}

	/**
	 * The number of times the task has executed and completed.
	 * @return the completeCount
	 */
	@JMXAttribute(name="CompleteCount", description="The number of times the task has executed and completed.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getCompleteCount() {
		return completeCount.get();
	}

	/**
	 * The number of times the task execution has thrown an exception.
	 * @return the failCount
	 */
	@JMXAttribute(name="FailCount", description="The number of times the task execution has thrown an exception.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getFailCount() {
		return failCount.get();
	}

	/**
	 * The number of times the task has been vetoed.
	 * @return the vetoCount
	 */
	@JMXAttribute(name="VetoCount", description="The number of times the task has been vetoed.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getVetoCount() {
		return vetoCount.get();
	}

	/**
	 * Registers a new listener on this scheduled task.
	 * @param scheduledTaskListener the listener to register.
	 */
	public void registerScheduledTaskListener(ScheduledTaskListener<V> scheduledTaskListener) {
		listeners.put(scheduledTaskListener, ScheduledTaskListenerExecutor.getListenerExecutorProxy(scheduledTaskListener, threadPool));
	}

	/**
	 * Unregisters a new listener on this scheduled task.
	 * @param scheduledTaskListener the listener to unregister.
	 */
	public void unRegisterScheduledTaskListener(ScheduledTaskListener<V> scheduledTaskListener) {
		listeners.remove(scheduledTaskListener);
	}

	/**
	 * The last elapsed execution time of the task in ms.
	 * @return the lastExecutionTime
	 */
	@JMXAttribute(name="LastExecutionTime", description="The last elapsed execution time of the task in ms.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastExecutionTime() {
		return lastExecutionTime.get();
	}

	/**
	 * The name of the scheduling type
	 * @return the schedType
	 */
	@JMXAttribute(name="SchedulingType", description="The scheduling type used for this task", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getSchedulingType() {
		if(schedType==null) return "UNKNOWN";
		return schedType.name();
	}


}
