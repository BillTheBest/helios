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
package org.helios.collectors.scheduler;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.helios.collectors.scheduler.quartz.*;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.CallableRunnable;
import org.helios.jmx.threadservices.JMXManagedThreadPoolService;
import org.helios.jmx.threadservices.WrappedScheduledFuture;
import org.helios.jmx.threadservices.pausable.SchedulablePausableThreadPoolExecutor;
import org.helios.jmx.threadservices.scheduling.ExecutionTask;
import org.helios.jmx.threadservices.scheduling.SchedulingType;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.core.SchedulingContext;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdJobRunShellFactory;
import org.quartz.impl.StdScheduler;
import org.quartz.simpl.RAMJobStore;

/**
 * <p>Title: JMXManagedScheduledThreadPoolService</p>
 * <p>Description: A JMX managed thread pool for managing deferred and scheduled tasks.</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(declared=false, annotated=true)
public class JMXManagedScheduledThreadPoolService extends
		JMXManagedThreadPoolService //implements IScheduledThreadPoolService
		{

	/**  */
	private static final long serialVersionUID = 6096250623215939612L;
	/** Quartz scheduler factory */
	protected DirectSchedulerFactory schedulerFactory = null;
    /** Quartz scheduler */
    protected Scheduler scheduler = null;
    /** the Quartz scheduler thread pool */
    protected IQuartzThreadPoolService quartzThreadPool = null;
    /** Indicates if the pool should support a cron service */
    protected boolean cronSupport = true;
    /** A counter to generate unique scheduled task Ids */
    protected AtomicLong taskIdFactory = new AtomicLong(0);
    /** A map of current frequency based tasks */
    protected Map<String, ScheduledFuture<?>> frequencyTasks = new ConcurrentHashMap<String, ScheduledFuture<?>>();
    /** A set of current cron based tasks */
    protected Set<String> scheduledTasks = new CopyOnWriteArraySet<String>();


    /** The task code prefix for frequency tasks */
    public static final String FREQUENCY_TASK = "F:";
    /** The task code prefix for scheduled (cron) tasks */
    public static final String SCHEDULE_TASK = "S:";

    /**
     * The number of frequency based scheduled tasks
     * @return
     */
    @JMXAttribute(description="The number of frequency based scheduled tasks", name="FrequencyTaskCount", mutability=AttributeMutabilityOption.READ_ONLY)
    public long getFrequencyTaskCount() {
    	return frequencyTasks.size();
    }

    /**
     * The number of cron based scheduled tasks
     * @return
     */
    @JMXAttribute(description="The number of cron based scheduled tasks", name="CronTaskCount", mutability=AttributeMutabilityOption.READ_ONLY)
    public long getCronTaskCount() {
    	return scheduledTasks.size();
    }


	/**
	 * Creates a new JMXManagedScheduledThreadPoolService with cron support.
	 */
	public JMXManagedScheduledThreadPoolService() {
		this(true);
	}

	/**
	 * Creates a new JMXManagedScheduledThreadPoolService
	 * @param cronSupport if true, a cron scheduler will be created.
	 */
	public JMXManagedScheduledThreadPoolService(boolean cronSupport) {
		super();
		this.cronSupport = cronSupport;
	}

	/**
	 * @param instrumentationEnabled the instrumentationEnabled to set
	 */
	public void setInstrumentationEnabled(boolean instrumentationEnabled) {
		super.setInstrumentationEnabled(instrumentationEnabled);
		if(quartzThreadPool != null) {
			if(((JMXManagedQuartzThreadPoolService)quartzThreadPool).getInstrumentationEnabled() != instrumentationEnabled) {
				((JMXManagedQuartzThreadPoolService)quartzThreadPool).setInstrumentationEnabled(instrumentationEnabled);
			}
		}
	}

	/**
	 * Initializes the pool and the cron scheduler if cronSupport is true.
	 * @see org.helios.jmx.threadservices.JMXManagedThreadPoolService#initializePool()
	 */
	@Override
	public void initializePool() {
		if(cronSupport) {
			try {
				if(quartzThreadPool==null) quartzThreadPool = new JMXManagedQuartzThreadPoolService();
				ObjectName quartzObjectName = new ObjectName(objectName.getDomain(), "service", "QuartzThreadPoolService");
				server.registerMBean(quartzThreadPool, quartzObjectName);
				((JMXManagedQuartzThreadPoolService)quartzThreadPool).setInstrumentationEnabled(this.instrumentationEnabled.get());
				((JMXManagedQuartzThreadPoolService)quartzThreadPool).start();
				try {
					SchedulingContext schedulingContext = new SchedulingContext();
					schedulingContext.setInstanceId("SIMPLE_NON_CLUSTERED");
					QuartzSchedulerResources quartzSchedulerResources = new QuartzSchedulerResources();
					quartzSchedulerResources.setInstanceId("SIMPLE_NON_CLUSTERED");
					quartzSchedulerResources.setJMXExport(true);
					quartzSchedulerResources.setJMXObjectName("org.helios:service=QuartzScheduler");
					quartzSchedulerResources.setJobRunShellFactory(new StdJobRunShellFactory());
					quartzSchedulerResources.setJobStore(new RAMJobStore());
					quartzSchedulerResources.setMakeSchedulerThreadDaemon(true);
					quartzSchedulerResources.setName("HeliosQuartzScheduler");
					quartzSchedulerResources.setThreadName("HeliosQuartzSchedulerThread");
					quartzSchedulerResources.setThreadPool(quartzThreadPool);
					QuartzScheduler quartzScheduler = new QuartzScheduler(quartzSchedulerResources, schedulingContext, 0, 0);
					StdScheduler stdScheduler = new StdScheduler(quartzScheduler, schedulingContext);
					scheduler  = stdScheduler;
				} catch (Throwable e) {
					e.printStackTrace();
					schedulerFactory = DirectSchedulerFactory.getInstance();
					schedulerFactory.createScheduler(quartzThreadPool, new RAMJobStore());
					scheduler = schedulerFactory.getScheduler();
				}


				scheduler.start();
			} catch (Exception e) {
				scheduler = null;
				log.error("Failed to start Quartz scheduler factory. Cron schedules will not be supported.",  e);
			}
		}
		super.initializePool();
	}

	/**
	 * Stops the thread pool and the quartz scheduler, if it was set up.
	 * @see org.helios.jmx.threadservices.JMXManagedThreadPoolService#stopInner()
	 */
	protected void stopInner() {
		if(scheduler!=null) {
			try {
				cronSupport = false;
				scheduler.shutdown();
			} catch (Exception e) {}
		}
	}




	/**
	 * Constructs a new Thread. Implementations may also initialize priority, name, daemon status, ThreadGroup, etc.
	 * @param r a runnable to be executed by new thread instance
	 * @return The constructed thread.
	 */
	public Thread newThread(Runnable r) {
		Thread t = null;
		t = new Thread(threadGroup, r, threadNamePrefix + "-Thread#" + threadSequence.incrementAndGet(), threadStackSize);
		t.setPriority(threadPriority);
		t.setDaemon(daemonThreads);
		if(threadPool != null) {
			t.setUncaughtExceptionHandler(((SchedulablePausableThreadPoolExecutor)threadPool));
		}
		return t;
	}

	/**
	 * Registers a new task and generates a task code for it.
	 * @param task The task to register.
	 * @param taskCode The task type code.
	 * @return The generated task key.
	 */
	protected String registerTask(ScheduledFuture<?> task, String taskCode) {
		String taskKey = taskCode + threadGroupName + this.taskIdFactory.incrementAndGet();
		frequencyTasks.put(taskKey, task);
		return taskKey;
	}

	/**
	 * Submits a callable for repeated time execution by the thread pool.
	 * @param fixedRate If true, task will be executed at a fixed rate, otherwise it will be executed at a fixed delay.
	 * @param callable The callable to schedule for repeated execution.
	 * @param executionCondition Optional execution condition that determines if the task should be executed at each period and if the task should be cancelled.
	 * @param delay The delay in the start of the schedule in ms.
	 * @param period The fixed delay or rate of the execution.
	 * @param timeUnit The unit of the period.
	 * @return A taskId which is the key to cancel the schedule.
	 */
	public <T> String scheduleAtFrequency(boolean fixedRate,
			Callable<T> callable,
			long delay, long period, TimeUnit timeUnit) {
		CallableRunnable<T> callableRunnable = new CallableRunnable<T>(callable);
		ScheduledFuture<T> innerScheduledFuture = null;
		if(fixedRate) {
			innerScheduledFuture =  (ScheduledFuture<T>) ((ScheduledThreadPoolExecutor)threadPool).scheduleAtFixedRate(callableRunnable, delay, period, timeUnit);
		} else {
			innerScheduledFuture = (ScheduledFuture<T>) ((ScheduledThreadPoolExecutor)threadPool).scheduleWithFixedDelay(callableRunnable, delay, period, timeUnit);
		}
		return registerTask(new WrappedScheduledFuture<T>(innerScheduledFuture, callableRunnable), FREQUENCY_TASK);
	}

	/**
	 * Submits a runnable for repeated time execution by the thread pool.
	 * @param fixedRate If true, task will be executed at a fixed rate, otherwise it will be executed at a fixed delay.
	 * @param runnable The runnable to schedule for repeated execution.
	 * @param executionCondition Optional execution condition that determines if the task should be executed at each period and if the task should be cancelled.
	 * @param delay The delay in the start of the schedule in ms.
	 * @param period The fixed delay or rate of the execution.
	 * @param timeUnit The unit of the period.
	 * @return A taskId which is the key to cancel the schedule.
	 */
	public <T> String scheduleAtFrequency(boolean fixedRate,
			Runnable runnable,
			long delay, long period, TimeUnit timeUnit) {
		ScheduledFuture<T> task = null;
		if(fixedRate) {
			task = (ScheduledFuture<T>) ((ScheduledThreadPoolExecutor)threadPool).scheduleAtFixedRate(runnable, delay, period, timeUnit);
		} else {
			task = (ScheduledFuture<T>) ((ScheduledThreadPoolExecutor)threadPool).scheduleWithFixedDelay(runnable, delay, period, timeUnit);
		}
		return registerTask(task, FREQUENCY_TASK);
	}

	/**
	 * Submits an execution task for repeated time execution by the thread pool.
	 * @param fixedRate If true, task will be executed at a fixed rate, otherwise it will be executed at a fixed delay.
	 * @param runnable The runnable to schedule for repeated execution.
	 * @param executionCondition Optional execution condition that determines if the task should be executed at each period and if the task should be cancelled.
	 * @param delay The delay in the start of the schedule in ms.
	 * @param period The fixed delay or rate of the execution.
	 * @param timeUnit The unit of the period.
	 * @return A taskId which is the key to cancel the schedule.
	 */
	public <T> String scheduleAtFrequency(boolean fixedRate,
			ExecutionTask<T> executionTask,
			long delay, long period, TimeUnit timeUnit) {
		CallableRunnable<T> callableRunnable = new CallableRunnable<T>(executionTask);
		ScheduledFuture<T> innerScheduledFuture = null;
		if(fixedRate) {
			innerScheduledFuture =  (ScheduledFuture<T>) ((ScheduledThreadPoolExecutor)threadPool).scheduleAtFixedRate(callableRunnable, delay, period, timeUnit);
		} else {
			innerScheduledFuture = (ScheduledFuture<T>) ((ScheduledThreadPoolExecutor)threadPool).scheduleWithFixedDelay(callableRunnable, delay, period, timeUnit);
		}
		return registerTask(new WrappedScheduledFuture<T>(innerScheduledFuture, callableRunnable), FREQUENCY_TASK);
	}

	/**
	 * Schedules a callable for deferred execution.
	 * @param callable The callable to schedule.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param delay The deferral period.
	 * @param timeUnit The unit of the deferral period.
	 * @return A scheduled future that can be used to cancel the task.
	 */
	public <T> ScheduledFuture<T> scheduleDeferred(Callable<T> callable, long delay, TimeUnit timeUnit) {
		return scheduleDeferred(callable, delay, timeUnit,  QuartzExecutionTaskType.CALLABLE);
	}

	/**
	 * Schedules a runnable for deferred execution.
	 * @param runnable The runnable to schedule.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param delay The deferral period.
	 * @param timeUnit The unit of the deferral period.
	 * @return A scheduled future that can be used to cancel the task.
	 */
	public <T> ScheduledFuture<T> scheduleDeferred(Runnable runnable,
			long delay, TimeUnit timeUnit) {
		return scheduleDeferred(runnable, delay, timeUnit,  QuartzExecutionTaskType.RUNNABLE);
	}

	/**
	 * Schedules an executionTask for deferred execution.
	 * @param executionTask The executionTask to schedule.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param delay The deferral period.
	 * @param timeUnit The unit of the deferral period.
	 * @return A scheduled future that can be used to cancel the task.
	 */
	public <T> ScheduledFuture<T> scheduleDeferred(ExecutionTask<T> executionTask,
			long delay, TimeUnit timeUnit) {
		return scheduleDeferred(executionTask, delay, timeUnit,  QuartzExecutionTaskType.EXECUTION_TASK);
	}

	/**
	 * Schedules an object for repeated invocation on a cron based schedule.
	 * @param task The object to be scheduled.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param cron A cron expression.
	 * @param type The type of execution task.
	 * @return A taskId which is the key to cancel the schedule.
	 * @see org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService#scheduleWithCron(java.util.concurrent.Callable, org.helios.jmx.threadservices.scheduling.ExecutionCondition, java.lang.String)
	 */
	protected <T> String schedule(Object task,
			String cron, QuartzExecutionTaskType type) {
		if(!isValidCron(cron)) {
			throw new RuntimeException("The expression [" + cron + "] is not a valid cron expression");
		}
		try {
			String taskKey = SCHEDULE_TASK + threadGroupName + this.taskIdFactory.incrementAndGet();
			CronTrigger cronTrigger = new CronTrigger(threadGroupName, taskKey, cron);
			JobDetail jobDetail = new JobDetail(threadGroupName, taskKey, QuartzExecutionTask.class, false, false, false);
			JobDataMap dataMap = jobDetail.getJobDataMap();
			dataMap.put(QuartzExecutionTask.TASK_TYPE, type);
			dataMap.put(QuartzExecutionTask.TASK, task);
			scheduler.scheduleJob(jobDetail, cronTrigger);
			scheduledTasks.add(taskKey);
			return taskKey;
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception scheduling cron task", e);
		}
	}

	/**
	 * Schedules a task for one time execution the defined period of time from now.
	 * @param task The QuartzExecutionTask
	 * @param delay The delay until the task is executed.
	 * @param timeUnit The unit for the delay value.
	 * @param type The type of the QuartzExecutionTask
	 * @return A scheduled future which can be used to acquire the result of the task or cancel the pending task.
	 */
	@SuppressWarnings("unchecked")
	protected <T> ScheduledFuture<T> scheduleDeferred(Object task, long delay, TimeUnit timeUnit,  QuartzExecutionTaskType type) {
		try {
			String taskKey = SCHEDULE_TASK + threadGroupName + this.taskIdFactory.incrementAndGet();
			Trigger trigger = new SimpleTrigger(threadGroupName, taskKey, new Date(System.currentTimeMillis() + timeUnit.convert(delay, TimeUnit.MILLISECONDS)));
			JobDetail jobDetail = new JobDetail(threadGroupName, taskKey, QuartzExecutionTask.class, false, false, false);
			JobDataMap dataMap = jobDetail.getJobDataMap();
			dataMap.put(QuartzExecutionTask.TASK_TYPE, type);
			dataMap.put(QuartzExecutionTask.TASK, task);
			ScheduledFuture<T> sf = (ScheduledFuture<T>)new QuartzDeferredTaskResult(trigger, scheduler, threadPool, server, SchedulingType.SCHEDULE);
			scheduler.scheduleJob(jobDetail, trigger);
			scheduledTasks.add(taskKey);
			return sf;
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception scheduling cron task", e);
		}

	}

	/**
	 * Schedules a callable for repeated invocation on a cron based schedule.
	 * @param callable The callable to be scheduled.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param cron A cron expression.
	 * @return A taskId which is the key to cancel the schedule.
	 * @see org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService#scheduleWithCron(java.util.concurrent.Callable, org.helios.jmx.threadservices.scheduling.ExecutionCondition, java.lang.String)
	 */
	public <T> String scheduleWithCron(Callable<T> callable,
			String cron) {
		return schedule(callable, cron, QuartzExecutionTaskType.CALLABLE);
	}

	/**
	 * Schedules a runnable for repeated invocation on a cron based schedule.
	 * @param runnable The runnable to be scheduled.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param cron A cron expression.
	 * @return A taskId which is the key to cancel the schedule.
	 * @see org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService#scheduleWithCron(java.util.concurrent.Callable, org.helios.jmx.threadservices.scheduling.ExecutionCondition, java.lang.String)
	 */
	public <T> String scheduleWithCron(Runnable runnable,
			String cron) {
		return schedule(runnable, cron, QuartzExecutionTaskType.RUNNABLE);
	}

	/**
	 * Schedules an ExecutionTask for repeated invocation on a cron based schedule.
	 * @param executionTask The ExecutionTask to be scheduled.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param cron A cron expression.
	 * @return A taskId which is the key to cancel the schedule.
	 * @see org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService#scheduleWithCron(java.util.concurrent.Callable, org.helios.jmx.threadservices.scheduling.ExecutionCondition, java.lang.String)
	 */
	public <T> String scheduleWithCron(ExecutionTask<T> executionTask,
			String cron) {
		return schedule(executionTask, cron, QuartzExecutionTaskType.EXECUTION_TASK);
	}

	/**
	 * Determines if the passed string is a valid cron expression.
	 * @param cron The expression to evaluate.
	 * @return true if the expression is valid.
	 */
	public static boolean isValidCron(String cron) {
		try {
			new CronExpression(cron);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Pauses the frequency, deferred and schedule firing of tasks.
	 * @see org.helios.jmx.threadservices.JMXManagedThreadPoolService#pause()
	 */
	public void pause() {
		super.pause();
		if(cronSupport) {
			try {
				if(!scheduler.isInStandbyMode()) {
					scheduler.standby();
				}
			} catch (Exception e) {
				log.warn("Cron Scheduler Failed to Pause", e);
				throw new RuntimeException("Cron Scheduler Failed to Pause", e);
			}
		}
	}

	/**
	 * Resumes the frequency, deferred and schedule firing of tasks.
	 * @see org.helios.jmx.threadservices.JMXManagedThreadPoolService#resume()
	 */
	public void resume() {
		super.resume();
		if(cronSupport) {
			try {
				if(scheduler.isInStandbyMode()) {
					scheduler.start();
				}
			} catch (Exception e) {
				log.warn("Cron Scheduler Failed to start", e);
				throw new RuntimeException("Cron Scheduler Failed to start", e);
			}
		}
	}

	/**
	 * @return
	 */
	@JMXAttribute(description="The thread pool backing the service.", name="ScheduledThreadPool", mutability=AttributeMutabilityOption.READ_ONLY)
	public ScheduledThreadPoolExecutor getThreadPool() {
		return ((ScheduledThreadPoolExecutor)threadPool);
	}

//	/**
//	 * Returns an instance of the pool as an IScheduledThreadPoolService
//	 * @return
//	 */
//	@JMXAttribute(description="The public interface for submiting tasks.", name="Instance", mutability=AttributeMutabilityOption.READ_ONLY)
//	public IScheduledThreadPoolService getInstance() {
//		return null;
//	}

	/**
	 * Cancels the scheduled task identified by the passed key.
	 * @param taskKey The identifier of the scheduled task to cancel.
	 * @param mayInterruptIfRunning true if the thread executing this task should be interrupted; otherwise, in-progress tasks are allowed to complete. Ignored if the task is a cron task.
	 * @return true if the task was cancelled, false if the task did not exist or failed to be cancelled.
	 * @see org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService#cancel(java.lang.String)
	 */
	public boolean cancel(String taskKey, boolean mayInterruptIfRunning) {
		if(!isTaskScheduled(taskKey)) return false;
		if(isKeyScheduledType(taskKey)) {
			try {
				scheduler.deleteJob(taskKey, threadGroup.getName());
				scheduledTasks.remove(taskKey);
				return true;
			} catch (Exception e) {
				return false;
			}
		} else {
			ScheduledFuture<?> scheduledFuture = frequencyTasks.remove(taskKey);
			if(scheduledFuture != null) {
				scheduledFuture.cancel(mayInterruptIfRunning);
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Determines if the passed key represents a task currently under scheduled management.
	 * @param taskKey The task key to test.
	 * @return true if the task is currently being managed.
	 */
	public boolean isTaskScheduled(String taskKey) {
		if(!isValidKey(taskKey)) return false;
		if(isKeyScheduledType(taskKey)) {
			return scheduledTasks.contains(taskKey);
		} else {
			return frequencyTasks.containsKey(taskKey);
		}

	}

	/**
	 * Determines if a task key has a valid format
	 * @param taskKey The key to validate
	 * @return true if it is valid.
	 */
	protected static boolean isValidKey(String taskKey) {
		return(taskKey != null && (taskKey.startsWith(FREQUENCY_TASK) || taskKey.startsWith(SCHEDULE_TASK)));
	}

	/**
	 * Determines if a taskKey represents a scheduled task.
	 * @param taskKey The key to evaluate.
	 * @return true if the task is a scheduled task.
	 */
	protected static boolean isKeyScheduledType(String taskKey) {
		return(taskKey != null && taskKey.startsWith(SCHEDULE_TASK));
	}

	/**
	 * Determines if a taskKey represents a frequency task.
	 * @param taskKey The key to evaluate.
	 * @return true if the task is a frequency task.
	 */
	protected static boolean isKeyFrequencyType(String taskKey) {
		return(taskKey != null && taskKey.startsWith(FREQUENCY_TASK));
	}

}
