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

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.helios.jmx.threadservices.metrics.ThreadResourceMonitor;
import org.helios.jmx.threadservices.metrics.ThreadResourceSnapshot;
import org.helios.jmx.threadservices.scheduling.ExecutionTask;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;

/**
 * <p>Title: QuartzExecutionTask</p>
 * <p>Description: A wrapper to contain a scheduled task that is compliant with the Quartz Job interface.</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 * TODO: Need to remove Future implementation methods as they are now obsolete.
 */
@SuppressWarnings("unchecked")
public class QuartzExecutionTask implements Job {
	/** The JobMap key for the ExecutionTask type */
	public static final String TASK_TYPE = "quartz.task.type";
	/** The JobMap key for the ExecutionTask type */
	public static final String TASK = "quartz.task";
	/** jobmap key for the type of task */
	public static final String TASK_TYPE_KEY = "TaskSchedulingType";
	/** jobmap key for ttask instrumentation enabled */
	public static final String TASK_INSTRUMENTATION_ENABLED = "TaskInstrumentation";


	/** The trigger scheduling a deferred task */
	protected Trigger trigger = null;
	/** The result of the call */
	protected AtomicReference result = new AtomicReference((Object)null);
	/** The call's exception */
	protected AtomicReference executionException = new AtomicReference((Throwable)null);
	/** The current task's state */
	protected AtomicInteger state = new AtomicInteger(QuartzExecutionTaskState.UNFIRED_ID);
	/** The task Id */
	protected String taskId = null;
	/** The task group */
	protected String groupId = null;
	/** The scheduler */
	protected Scheduler scheduler = null;
	/** State thread protection */
	protected Lock lock = new ReentrantLock(true);




	/**
	 * @param taskId the taskId to set
	 */
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}



	/**
	 * Executes a quartz job
	 * @param jobContext
	 * @throws JobExecutionException
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		boolean enableInstrumentation = false;
		try {
			lock.lock();
			if(state.get() != QuartzExecutionTaskState.COMPLETED_ID) {
				state.set(QuartzExecutionTaskState.FIRING_ID);
				ThreadResourceSnapshot tsr = null;
				try {
					JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
					QuartzExecutionTaskType taskType = (QuartzExecutionTaskType)dataMap.get(TASK_TYPE);
					Object tmpVal = dataMap.get(TASK_INSTRUMENTATION_ENABLED);
					if(tmpVal != null && tmpVal instanceof Boolean) {
						enableInstrumentation = (Boolean)tmpVal;
					}
					if(taskType==null) {
						executionException.set(new JobExecutionException("No QuartzExecutionTaskType defined"));
						throw (JobExecutionException)executionException.get();
					}
					Object task = dataMap.get(TASK);
					if(task==null) {
						executionException.set(new JobExecutionException("No QuartzExecutionTask defined"));
						throw (JobExecutionException)executionException.get();

					}
					int key = taskType.getId();
					switch (key) {
					case QuartzExecutionTaskType.CALLABLE_ID:
						try {
							if(enableInstrumentation) ThreadResourceMonitor.start();
							result.set(((Callable)task).call());
						} catch (Exception e) {
							executionException.set(new JobExecutionException("QuartzExecutionTask Callable Invocation Failed", e));
							throw (JobExecutionException)executionException.get();
						} finally {
							if(enableInstrumentation) tsr = ThreadResourceMonitor.stop();
//							String listenerName = jobContext.getTrigger().getGroup() + "/" + jobContext.getTrigger().getName();
//							try {
//								scheduler.removeTriggerListener(listenerName);
//							} catch (Exception e) {};
						}
						break;
					case QuartzExecutionTaskType.RUNNABLE_ID:
						try {
							if(enableInstrumentation) ThreadResourceMonitor.start();
							((Runnable)task).run();
						} catch (Exception e) {
							executionException.set(new JobExecutionException("QuartzExecutionTask Runnable Invocation Failed", e));
							throw (JobExecutionException)executionException.get();
						} finally {
							if(enableInstrumentation) tsr = ThreadResourceMonitor.stop();
						}
						break;
					case QuartzExecutionTaskType.EXECUTION_TASK_ID:
						try {
							if(enableInstrumentation) ThreadResourceMonitor.start();
							result.set(((ExecutionTask)task).executeTask());
						} catch (Throwable e) {
							executionException.set(new JobExecutionException("QuartzExecutionTask ExecutionTask Invocation Failed", e));
							throw (JobExecutionException)executionException.get();
						} finally {
							if(enableInstrumentation) tsr = ThreadResourceMonitor.stop();
						}
						break;
					default:
						executionException.set(new JobExecutionException("Unrecognized QuartzExecutionTask Type Id:" + key));
						throw (JobExecutionException)executionException.get();
					}
				} finally {
					state.set(QuartzExecutionTaskState.FIRED_ID);
					if(enableInstrumentation)  {
						jobContext.setResult(tsr);
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}


	/**
	 * Returns the remaining delay associated with this object, in the given time unit.
	 * @param unit the time unit
	 * @return the remaining delay; zero or negative values indicate that the delay has already elapsed
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Deprecated
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
	@Deprecated
	public int compareTo(Delayed delayed) {
		Long thatDelay = delayed.getDelay(TimeUnit.MILLISECONDS);
		Long thisDelay = this.getDelay(TimeUnit.MILLISECONDS);
		return thisDelay.compareTo(thatDelay);
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
	@Deprecated
	public boolean cancel(boolean mayInterruptIfRunning) {
		try {
			lock.lock();
			if(state.get()==QuartzExecutionTaskState.UNFIRED_ID) {
				scheduler.unscheduleJob(taskId, groupId);
				state.set(QuartzExecutionTaskState.COMPLETED_ID);
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		} finally {
			lock.unlock();
		}
	}









	/**
	 * Returns true if this task was cancelled before it completed normally.
	 * @return true if task was cancelled before it completed
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Deprecated
	public boolean isCancelled() {
		return state.get()==QuartzExecutionTaskState.CANCELLED_ID;
	}



	/**
	 * Returns true if this task completed. Completion may be due to normal termination, an exception, or cancellation -- in all of these cases, this method will return true.
	 * @return true if this task completed.
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Deprecated
	public boolean isDone() {
		return state.get()==QuartzExecutionTaskState.COMPLETED_ID;
	}



	/**
	 * @param trigger the trigger to set
	 */
	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}



	/**
	 * @param scheduler the scheduler to set
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}



	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}



	/**
	 * @return the result
	 */
	public Object getResult() {
		return result.get();
	}



	/**
	 * @return the executionException
	 */
	public Exception getExecutionException() {
		return (Exception)executionException.get();
	}

}
