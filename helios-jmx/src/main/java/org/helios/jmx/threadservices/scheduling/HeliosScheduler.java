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
package org.helios.jmx.threadservices.scheduling;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.concurrency.JMXManagedLock;
import org.helios.jmx.threadservices.scheduling.quartz.FixedDelayTrigger;
import org.helios.jmx.threadservices.scheduling.quartz.JMXManagedQuartzThreadPoolService;
import org.helios.jmx.threadservices.scheduling.quartz.QuartzDeferredTaskResult;
import org.helios.jmx.threadservices.scheduling.quartz.QuartzExecutionTask;
import org.helios.jmx.threadservices.scheduling.quartz.QuartzExecutionTaskType;
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
 * <p>Title: HeliosScheduler</p>
 * <p>Description: The primary task scheduling service for Helios.</p> 
 * <p>The underluing scheduler implementation is the Quartz scheduler backed by a <code>JMXManagedQuartzThreadPoolService</code>.</p>
 * <p>Default configuration values and override properties read from system properties, and then environment are: 
 * <table border="1"><tr><th>Configuration Item</th><th>Default</th><th>Environment or System Property for Override</th></tr>
 * <tr><td>Task Submission Queue Size</td><td>10000</td><td>org.helios.scheduler.queuesize</td></tr>
 * <tr><td>Core Pool Size</td><td>The number of CPUs x 5 (from OperatingSystemMXBean) or 5 if this fails.</td><td>org.helios.scheduler.corepoolsize</td></tr>
 * <tr><td>Max Pool Size</td><td>The number of CPUs x 10 (from OperatingSystemMXBean) or 10 if this fails.</td><td>org.helios.scheduler.maxpoolsize</td></tr>
 * <tr><td>Thread Keep Alive Time</td><td>10000</td><td>org.helios.scheduler.threadkeepalive</td></tr>
 * <tr><td>ThreadGroup Name</td><td>&lt;JMX Object Name&gt;-ThreadGroup</td><td>Cannot be overriden.</td></tr>
 * <tr><td>Thread Name Prefix</td><td>&lt;JMX Object Name&gt;-Thread#</td><td>Cannot be overriden.</td></tr>
 * <tr><td>Daemon Threads</td><td>true</td><td>org.helios.scheduler.daemonthreads</td></tr>
 * <tr><td>Thread Stack Size</td><td>JVM Implementation Default</td><td>org.helios.scheduler.threadstacksize</td></tr>
 * <tr><td>Thread Priority</td><td>Thread.NORM_PRIORITY</td><td>org.helios.scheduler.threadpriority</td></tr>
 * <tr><td>ThreadPool Shutdown Time (ms)</td><td>1</td><td>org.helios.scheduler.shutdowntime</td></tr>
 * <tr><td>Number of threads to prestart</td><td>0</td><td>org.helios.scheduler.prestartthreads</td></tr>
 * <tr><td>Task hand offs to the pool should be queued, not executed</td><td>true</td><td>org.helios.scheduler.usequeue</td></tr>
 * <tr><td>Thread pool instrumentation enabled</td><td>false</td><td>org.helios.scheduler.instrumentation</td></tr>
 * </table></p> 

 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(declared=true, annotated=true)
public class HeliosScheduler extends ManagedObjectDynamicMBean implements IScheduledThreadPoolService {
	
	/** Key name to lookup the scheduler's task queue size override */
	public static final String TASK_QUEUE_SIZE = "org.helios.scheduler.queuesize";
	/** Key name to lookup the scheduler's core pool size override */
	public static final String CORE_POOL_SIZE = "org.helios.scheduler.corepoolsize";
	/** Key name to lookup the scheduler's max pool size override */
	public static final String MAX_POOL_SIZE = "org.helios.scheduler.maxpoolsize";
	/** Key name to lookup the scheduler's thread keep alive time override */
	public static final String KEEP_ALIVE_TIME = "org.helios.scheduler.threadkeepalive";
	/** Key name to lookup the scheduler's thread daemon override */
	public static final String DAEMON_THREADS = "org.helios.scheduler.daemonthreads";
	/** Key name to lookup the scheduler's thread priority */
	public static final String THREAD_PRIORITY = "org.helios.scheduler.threadpriority";
	/** Key name to lookup the scheduler's pool shutdown time */
	public static final String POOL_SHUTDOWN_TIME = "org.helios.scheduler.shutdowntime";
	/** Key name to lookup the scheduler's thread prestart count */
	public static final String THREAD_PRESTART = "org.helios.scheduler.prestartthreads";
	/** Key name to lookup the scheduler's queue or immediate exec. */
	public static final String THREAD_USEQUEUE = "org.helios.scheduler.usequeue";
	/** Key name to lookup the scheduler's thread pool instrumentation enabled */
	public static final String THREAD_POOL_INSTRUMENTED = "org.helios.scheduler.instrumentation";
	/** Key name to lookup the scheduler's task executor instrumentation enabled */
	public static final String TASK_EXECUTOR_INSTRUMENTED = "org.helios.scheduler.task.instrumentation";
	
	/** Key name to lookup the additional domain to register the scheduler in */
	public static final String JMX_REGISTRATION_DOMAIN = "org.helios.scheduler.jmxdomain";
	
	
	/** The default queue size */
	public static int DEFAULT_QUEUE_SIZE = 10000;
	/** The default thread keep alive time (ms)*/
	public static long DEFAULT_KEEP_ALIVE_TIME = 10000;
	/** The default thread pool shutdown time (ms)*/
	public static long DEFAULT_POOL_SHUTDOWN_TIME = 1000;	
	
    
    /** A counter to generate unique scheduled task Ids */
    protected AtomicLong taskIdFactory = new AtomicLong(0);    
	/** A pause state atomic boolean */
    protected AtomicBoolean schedulerPaused = new AtomicBoolean(false);
	
	
	/** The constant name of the scheduler ObjectName */
	public static final String SERVICE_NAME = "org.helios.scheduling:service=HeliosScheduler";
	/** The constant ObjectName where the scheduler registered in the platform agent. */
	public static final ObjectName SERVICE_OBJECT_NAME = JMXHelperExtended.objectName(SERVICE_NAME);
	/** The description of the scheduler service */
	public static final String SERVICE_DESCRIPTION = "Helios VM Centralized JMX Managed Scheduling Service";

	/** The constant name of the quartz scheduler ObjectName */
	public static final String Q_SERVICE_NAME = "org.helios.scheduling:service=QuartzScheduler";
	/** The constant ObjectName where the scheduler registered in the platform agent. */
	public static final ObjectName Q_SERVICE_OBJECT_NAME = JMXHelperExtended.objectName(Q_SERVICE_NAME);
	/** The description of the scheduler service */
	public static final String Q_SERVICE_DESCRIPTION = "Helios Quartz Scheduling Engine";
	
	
	
	/** The platform MBeanServer */
	private static MBeanServer platformAgent = ManagementFactory.getPlatformMBeanServer();
	/** Optional Secondary MBeanServer */
	protected MBeanServer mbeanServer = null;
	/** The internal quartz scheduler */
	private QuartzScheduler quartzScheduler = null;
	/** boot strap synchronization lock */
	private static Object lock = new Object();
	/** Quartz scheduler factory */
	protected DirectSchedulerFactory schedulerFactory = null;
	/** A static class logger */
	protected static Logger log = Logger.getLogger(HeliosScheduler.class);
	/** task instrumentation enabled flag */
	protected boolean taskExecutorInstrumentationEnabled = false;
	/** A reference to the Quartz Job Store */
	protected RAMJobStore jobStore = null;
	
	/** The internal static singleton scheduler instance */ 
	private static volatile HeliosScheduler scheduler = null;
	/** The quartz scheduler */
	private Scheduler innerScheduler = null;
	/** The scheduler's thread pool */
	protected JMXManagedQuartzThreadPoolService threadPool = null;
	
	
	/**
	 * Acquires a reference to the single in VM instance of the HeliosScheduler.
	 * If this is the first call in this VM, a new HeliosScheduler will be created.
	 * If this singleton has already been created, a reference to the existing one will be returned.
	 * @return a reference to the HeliosScheduler.
	 */
	public static HeliosScheduler getInstance() {
		return getInstance(null);
	}
	
	/**
	 * Acquires a reference to the single in VM instance of the HeliosScheduler.
	 * If this is the first call in this VM, a new HeliosScheduler will be created.
	 * If this singleton has already been created, a reference to the existing one will be returned, so the passed properties will be ignored. 
	 * @param properties Configuration properties for the scheduler.
	 * @return a reference to the HeliosScheduler.
	 */
	public static HeliosScheduler getInstance(Properties properties) {
		if(scheduler!=null) {
			return scheduler;
		} else {
			try {
				JMXManagedLock.getInstance().lock(SERVICE_NAME);
				if(platformAgent.isRegistered(SERVICE_OBJECT_NAME)) {
					scheduler = (HeliosScheduler)platformAgent.getAttribute(SERVICE_OBJECT_NAME, "Reference");					
				} else {
					scheduler = new HeliosScheduler(properties);
					if(!platformAgent.isRegistered(SERVICE_OBJECT_NAME)) {
						platformAgent.registerMBean(scheduler, SERVICE_OBJECT_NAME);
					}
				}
				return scheduler;
			} catch (Exception e) {
				log.fatal("Failed to instantiate the HeliosScheduler", e);
				throw new RuntimeException("Failed to instantiate the HeliosScheduler", e);
			} finally {
				JMXManagedLock.getInstance().unlock(SERVICE_NAME);
			}
		}
	}
	

	/**
	 * Private constructor for singleton scheduler. 
	 */
	private HeliosScheduler(Properties...properties) {		
		//=====================================================================
		//  Configure Thread Pool
		//=====================================================================		
		threadPool = new JMXManagedQuartzThreadPoolService();
		if(ConfigurationHelper.isIntDefined(TASK_QUEUE_SIZE, properties)) {
			threadPool.setQueueCapacity(ConfigurationHelper.getIntSystemThenEnvProperty(TASK_QUEUE_SIZE, DEFAULT_QUEUE_SIZE, properties));
		}
		if(ConfigurationHelper.isIntDefined(CORE_POOL_SIZE, properties)) {
			threadPool.setCorePoolSize(ConfigurationHelper.getIntSystemThenEnvProperty(CORE_POOL_SIZE, JMXManagedQuartzThreadPoolService.getDefaultCorePoolSize(), properties));
		}
		if(ConfigurationHelper.isIntDefined(MAX_POOL_SIZE, properties)) {
			threadPool.setMaxPoolSize(ConfigurationHelper.getIntSystemThenEnvProperty(MAX_POOL_SIZE, JMXManagedQuartzThreadPoolService.getDefaultMaxPoolSize(), properties));
		}
		if(ConfigurationHelper.isIntDefined(KEEP_ALIVE_TIME, properties)) {
			threadPool.setThreadKeepAliveTime(ConfigurationHelper.getLongSystemThenEnvProperty(KEEP_ALIVE_TIME, DEFAULT_KEEP_ALIVE_TIME, properties));
		}
		if(ConfigurationHelper.isBooleanDefined(DAEMON_THREADS, properties)) {
			threadPool.setDaemonThreads(ConfigurationHelper.getBooleanSystemThenEnvProperty(DAEMON_THREADS, true, properties));
		}
		if(ConfigurationHelper.isIntDefined(THREAD_PRIORITY, properties)) {
			int priority = ConfigurationHelper.getIntSystemThenEnvProperty(THREAD_PRIORITY, Thread.NORM_PRIORITY, properties);
			if(priority >= Thread.MIN_PRIORITY && priority <= Thread.MAX_PRIORITY) {
				threadPool.setThreadPriority(priority);
			} else {
				threadPool.setThreadPriority(Thread.NORM_PRIORITY);
			}
		}
		if(ConfigurationHelper.isLongDefined(POOL_SHUTDOWN_TIME, properties)) {
			threadPool.setShutdownTime(ConfigurationHelper.getLongSystemThenEnvProperty(POOL_SHUTDOWN_TIME, DEFAULT_POOL_SHUTDOWN_TIME, properties));
		}
		if(ConfigurationHelper.isIntDefined(THREAD_PRESTART, properties)) {
			threadPool.setPrestartThreads(ConfigurationHelper.getIntSystemThenEnvProperty(THREAD_PRESTART, 0, properties));
		}
		if(ConfigurationHelper.isBooleanDefined(THREAD_USEQUEUE, properties)) {
			threadPool.setQueueExecutions(ConfigurationHelper.getBooleanSystemThenEnvProperty(THREAD_USEQUEUE, true, properties));
		}
		if(ConfigurationHelper.isBooleanDefined(THREAD_POOL_INSTRUMENTED, properties)) {
			threadPool.setInstrumentationEnabled(ConfigurationHelper.getBooleanSystemThenEnvProperty(THREAD_POOL_INSTRUMENTED, false, properties));
		}
		if(ConfigurationHelper.isBooleanDefined(TASK_EXECUTOR_INSTRUMENTED, properties)) {
			taskExecutorInstrumentationEnabled = ConfigurationHelper.getBooleanSystemThenEnvProperty(TASK_EXECUTOR_INSTRUMENTED, false, properties);
		}
		
		//=====================================================================
		//  Configure and register scheduler resources
		//=====================================================================		
		try {			
			platformAgent.registerMBean(threadPool, JMXManagedQuartzThreadPoolService.SERVICE_OBJECT_NAME);
			//threadPool.start();
			try {
				jobStore = new RAMJobStore();
				SchedulingContext schedulingContext = new SchedulingContext();
				schedulingContext.setInstanceId("SIMPLE_NON_CLUSTERED");
				QuartzSchedulerResources quartzSchedulerResources = new QuartzSchedulerResources();
				quartzSchedulerResources.setInstanceId("SIMPLE_NON_CLUSTERED");
				quartzSchedulerResources.setJMXExport(true);
				quartzSchedulerResources.setJMXObjectName(Q_SERVICE_NAME);
				quartzSchedulerResources.setJobRunShellFactory(new StdJobRunShellFactory());
				quartzSchedulerResources.setJobStore(jobStore);
				quartzSchedulerResources.setMakeSchedulerThreadDaemon(true);
				quartzSchedulerResources.setName("HeliosQuartzScheduler");
				quartzSchedulerResources.setThreadName("HeliosQuartzSchedulerThread");
				quartzSchedulerResources.setThreadPool(threadPool);
				quartzScheduler = new QuartzScheduler(quartzSchedulerResources, schedulingContext, 0, 0);
				innerScheduler = new StdScheduler(quartzScheduler, schedulingContext);							
			} catch (Throwable e) {
				e.printStackTrace();
				schedulerFactory = DirectSchedulerFactory.getInstance();				
				schedulerFactory.createScheduler(threadPool, jobStore);			
				innerScheduler = schedulerFactory.getScheduler();					
			}				
			threadPool.start();
			jobStore.initialize(null, quartzScheduler.getSchedulerSignaler());
			quartzScheduler.start();			
			innerScheduler.start();
		} catch (Exception e) {
			scheduler = null;
			log.error("Failed to start Quartz scheduler factory. Cron schedules will not be supported.",  e);
		}
		////////////////
		// JMX Registration
		///////////////
		this.reflectObject(this);
		
		if(ConfigurationHelper.isDefined(JMX_REGISTRATION_DOMAIN)) {
			mbeanServer = JMXHelperExtended.getLocalMBeanServer(ConfigurationHelper.getSystemThenEnvProperty(JMX_REGISTRATION_DOMAIN, "DefaultDomain"));
			if(mbeanServer != null && !mbeanServer.isRegistered(SERVICE_OBJECT_NAME)) {
				try { mbeanServer.registerMBean(this, SERVICE_OBJECT_NAME); } catch (Exception e) {log.warn("HeliosScheduler could not be registered in domain:" + mbeanServer.getDefaultDomain(), e);}
			}
			if(mbeanServer != null && !mbeanServer.isRegistered(JMXManagedQuartzThreadPoolService.SERVICE_OBJECT_NAME)) {
				try { mbeanServer.registerMBean(threadPool, JMXManagedQuartzThreadPoolService.SERVICE_OBJECT_NAME); } catch (Exception e) {log.warn("Helios Quartz Thread Pool could not be registered in domain:" + mbeanServer.getDefaultDomain(), e);}
			}
			org.apache.commons.modeler.Registry registry = org.apache.commons.modeler.Registry.getRegistry(null, null);
			MBeanServer modellerMbeanServer = registry.getMBeanServer();
			try {
				if(mbeanServer != null && !mbeanServer.isRegistered(Q_SERVICE_OBJECT_NAME)) {				
					try {
						registry.setMBeanServer(mbeanServer);
						registry.registerComponent(quartzScheduler, Q_SERVICE_OBJECT_NAME, null);
					} catch (Exception e) {
						log.warn("Helios Quartz Thread Pool could not be registered in domain:" + mbeanServer.getDefaultDomain(), e);
					}
				}
			} finally {
				registry.setMBeanServer(modellerMbeanServer);
			}
		}
	}
	
	public void shutdown(boolean wait) {
		try {
			innerScheduler.shutdown(wait);
		} catch (Exception e) {}
		try {
			threadPool.shutdown(wait);
		} catch (Exception e) {}
		try {
			platformAgent.unregisterMBean(SERVICE_OBJECT_NAME);
		} catch (Exception e) {}						
		try {
			platformAgent.unregisterMBean(Q_SERVICE_OBJECT_NAME);
		} catch (Exception e) {}
		try {
			platformAgent.unregisterMBean(JMXManagedQuartzThreadPoolService.SERVICE_OBJECT_NAME);
		} catch (Exception e) {}
		
		
		if(mbeanServer!=null) {
			try {
				mbeanServer.unregisterMBean(SERVICE_OBJECT_NAME);
			} catch (Exception e) {}
			try {
				mbeanServer.unregisterMBean(Q_SERVICE_OBJECT_NAME);
			} catch (Exception e) {}
			try {
				mbeanServer.unregisterMBean(JMXManagedQuartzThreadPoolService.SERVICE_OBJECT_NAME);
			} catch (Exception e) {}										
		}
		scheduler = null;
	}

	/**
	 * Acquires a reference to a pre-existing HeliosScheduler from the platform agent.
	 * @return a reference to the HeliosScheduler.
	 */
	@JMXAttribute(name="Reference", description="a reference to the HeliosScheduler.", mutability=AttributeMutabilityOption.READ_ONLY)
	public HeliosScheduler getReference() {
		return scheduler;
	}
	
	/**
	 * Schedules an object for repeated invocation on a cron based schedule.
	 * @param task The object to be scheduled.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param cron A cron expression.
	 * @param type The type of execution task.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 * @see org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService#scheduleWithCron(java.util.concurrent.Callable, org.helios.jmx.threadservices.scheduling.ExecutionCondition, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	protected synchronized <T> ScheduledTaskHandle<T> schedule(Object task, String cron, QuartzExecutionTaskType type) {
		if(!isValidCron(cron)) {
			throw new RuntimeException("The expression [" + cron + "] is not a valid cron expression");
		}
		try {
			//StringBuilder taskKey = new StringBuilder(SCHEDULE_TASK);
			StringBuilder taskKey = new StringBuilder();
			taskKey.append(threadPool.getThreadGroupName()).append("#").append(taskIdFactory.incrementAndGet());
			if(task instanceof NamedTask)
				taskKey.append("-"+((NamedTask)task).getName());			
			CronTrigger cronTrigger = new CronTrigger(taskKey.toString(), threadPool.getThreadGroupName(), cron);
			JobDetail jobDetail = new JobDetail(taskKey.toString(), threadPool.getThreadGroupName(), QuartzExecutionTask.class, false, false, false);
			JobDataMap dataMap = jobDetail.getJobDataMap();
			dataMap.put(QuartzExecutionTask.TASK_TYPE, type);
			dataMap.put(QuartzExecutionTask.TASK, task);
			dataMap.put(QuartzExecutionTask.TASK_TYPE_KEY, SchedulingType.SCHEDULE);
			dataMap.put(QuartzExecutionTask.TASK_INSTRUMENTATION_ENABLED, taskExecutorInstrumentationEnabled);
			innerScheduler.scheduleJob(jobDetail, cronTrigger);
			ScheduledTaskHandle<T> sf = new QuartzDeferredTaskResult(cronTrigger, innerScheduler, threadPool, platformAgent, SchedulingType.SCHEDULE);
			return sf;			
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
	 * @return A ScheduledTaskHandle to the scheduled task.
	 */
	protected synchronized <T> ScheduledTaskHandle<T> scheduleDeferred(Object task, long delay, TimeUnit timeUnit,  QuartzExecutionTaskType type) {
		try {
			StringBuilder taskKey = new StringBuilder();

			if(task instanceof NamedTask) {
				taskKey.append(((NamedTask)task).getName());
			} else {
				taskKey.append(threadPool.getThreadGroupName()).append("#").append(taskIdFactory.incrementAndGet());
			}
			Date fireDate = new Date(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delay, timeUnit));
			// ==================
			// This SimpleTrigger constructor does not set a job name
			// Trigger trigger = new SimpleTrigger(taskKey.toString(), threadPool.getThreadGroupName(), fireDate);
			//===================			
			
			Trigger trigger = new SimpleTrigger(taskKey.toString(), threadPool.getThreadGroupName(), taskKey.toString(),
						threadPool.getThreadGroupName(), fireDate, null, 0, 1);
			
			long timeToFire = fireDate.getTime()-System.currentTimeMillis(); 
			log.info("===============>Message firing in " + timeToFire + " ms.");
			JobDetail jobDetail = new JobDetail(taskKey.toString(), threadPool.getThreadGroupName(), QuartzExecutionTask.class, false, false, false);
			JobDataMap dataMap = jobDetail.getJobDataMap();
			dataMap.put(QuartzExecutionTask.TASK_TYPE, type);
			dataMap.put(QuartzExecutionTask.TASK, task);
			dataMap.put(QuartzExecutionTask.TASK_TYPE_KEY, SchedulingType.DEFERRED);
			dataMap.put(QuartzExecutionTask.TASK_INSTRUMENTATION_ENABLED, taskExecutorInstrumentationEnabled);
			ScheduledTaskHandle<T> sf = new QuartzDeferredTaskResult(trigger, innerScheduler, threadPool, mbeanServer, SchedulingType.DEFERRED);
			innerScheduler.addTriggerListener((QuartzDeferredTaskResult)sf);
			innerScheduler.scheduleJob(jobDetail, trigger);
			return sf;			
		} catch (Exception e) {
			log.error("Unexpected exception scheduling cron task", e);
			throw new RuntimeException("Unexpected exception scheduling cron task", e);
		}		
	}

	/**
	 * Submits a callable for repeated time execution by the thread pool.
	 * @param fixedRate If true, task will be executed at a fixed rate, otherwise it will be executed at a fixed delay.
	 * @param callable The callable to schedule for repeated execution.
	 * @param executionCondition Optional execution condition that determines if the task should be executed at each period and if the task should be cancelled.
	 * @param delay The delay in the start of the schedule in ms.
	 * @param period The fixed delay or rate of the execution.
	 * @param timeUnit The unit of the period.
	 * @return   A ScheduledTaskHandle to the scheduled task.
	 */
	@SuppressWarnings("unchecked")
	protected synchronized <T> ScheduledTaskHandle<T> scheduleAtFrequency(boolean fixedRate, Object task, QuartzExecutionTaskType type, long delay, long period, TimeUnit timeUnit) {
		try {
			//StringBuilder taskKey = new StringBuilder(SCHEDULE_TASK);
			StringBuilder taskKey = new StringBuilder();

			if(task instanceof NamedTask) {
				taskKey.append(((NamedTask)task).getName());
			} else {
				taskKey.append(threadPool.getThreadGroupName()).append("#").append(taskIdFactory.incrementAndGet());
			}
			Trigger trigger = null;
			if(fixedRate) {
//				trigger = TriggerUtils.makeSecondlyTrigger(taskKey.toString(), (int)TimeUnit.SECONDS.convert(period, timeUnit), SimpleTrigger.REPEAT_INDEFINITELY);
				trigger = new SimpleTrigger(taskKey.toString(), threadPool.getThreadGroupName(), taskKey.toString(),
						threadPool.getThreadGroupName(), new Date(System.currentTimeMillis() + delay), null, SimpleTrigger.REPEAT_INDEFINITELY,
						TimeUnit.MILLISECONDS.convert(period, timeUnit));
			} else {
				trigger = new FixedDelayTrigger(taskKey.toString(), threadPool.getThreadGroupName(), taskKey.toString(),
						threadPool.getThreadGroupName(), new Date(System.currentTimeMillis() + delay), null, SimpleTrigger.REPEAT_INDEFINITELY,
						TimeUnit.MILLISECONDS.convert(period, timeUnit));	
				((FixedDelayTrigger)trigger).setOriginalScheduler(innerScheduler);
			}
			JobDetail jobDetail = new JobDetail(taskKey.toString(), threadPool.getThreadGroupName(), QuartzExecutionTask.class, false, false, false);
			/*
			 * Sometimes, there is still a task left with the same name.
			 * This can happen when a collector hot deploy/undeploy acts funny.
			 * To avoid issues, we will automatically remove the existing job, 
			 * if one exists with the same name/gropup
			 */
			innerScheduler.deleteJob(taskKey.toString(), threadPool.getThreadGroupName());
			JobDataMap dataMap = jobDetail.getJobDataMap();
			dataMap.put(QuartzExecutionTask.TASK_TYPE, type);
			dataMap.put(QuartzExecutionTask.TASK, task);
			dataMap.put(QuartzExecutionTask.TASK_TYPE_KEY, SchedulingType.FREQUENCY);
			dataMap.put(QuartzExecutionTask.TASK_INSTRUMENTATION_ENABLED, taskExecutorInstrumentationEnabled);
			ScheduledTaskHandle<T> sf = new QuartzDeferredTaskResult(trigger, innerScheduler, threadPool, platformAgent, SchedulingType.FREQUENCY);
			innerScheduler.addTriggerListener((QuartzDeferredTaskResult)sf);
			innerScheduler.scheduleJob(jobDetail, trigger);
			return sf;
		} catch (Exception e) {
			log.error("Unexpected exception scheduling frequency task", e);
			throw new RuntimeException("Unexpected exception scheduling frequency task", e);			
		}
	}	
	
	/**
	 * Schedules a callable for repeated invocation on a cron based schedule.
	 * @param callable The callable to be scheduled.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param cron A cron expression.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 * @see org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService#scheduleWithCron(java.util.concurrent.Callable, org.helios.jmx.threadservices.scheduling.ExecutionCondition, java.lang.String)
	 */
	public <T> ScheduledTaskHandle<T> scheduleWithCron(Callable<T> callable, String cron) {
		return schedule(callable, cron, QuartzExecutionTaskType.CALLABLE);
	}

	/**
	 * Schedules a runnable for repeated invocation on a cron based schedule.
	 * @param runnable The runnable to be scheduled.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param cron A cron expression.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 * @see org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService#scheduleWithCron(java.util.concurrent.Callable, org.helios.jmx.threadservices.scheduling.ExecutionCondition, java.lang.String)
	 */
	public <T> ScheduledTaskHandle<T> scheduleWithCron(Runnable runnable, String cron) {
		return schedule(runnable, cron, QuartzExecutionTaskType.RUNNABLE);
	}

	/**
	 * Schedules an ExecutionTask for repeated invocation on a cron based schedule.
	 * @param executionTask The ExecutionTask to be scheduled.
	 * @param executionCondition Optional execution condition that determines if the task should be executed once the deferral period has expired.
	 * @param cron A cron expression.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 * @see org.helios.jmx.threadservices.scheduling.IScheduledThreadPoolService#scheduleWithCron(java.util.concurrent.Callable, org.helios.jmx.threadservices.scheduling.ExecutionCondition, java.lang.String)
	 */
	public <T> ScheduledTaskHandle<T> scheduleWithCron(ExecutionTask<T> executionTask, String cron) {
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
	 * Submits a callable for repeated time execution by the thread pool.
	 * @param fixedRate If true, task will be executed at a fixed rate, otherwise it will be executed at a fixed delay.
	 * @param callable The callable to schedule for repeated execution.
	 * @param executionCondition Optional execution condition that determines if the task should be executed at each period and if the task should be cancelled.
	 * @param delay The delay in the start of the schedule in ms.
	 * @param period The fixed delay or rate of the execution.
	 * @param timeUnit The unit of the period.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 */
	public <T> ScheduledTaskHandle<T> scheduleAtFrequency(boolean fixedRate, Callable<T> callable, long delay, long period, TimeUnit timeUnit) {
		return scheduleAtFrequency(fixedRate, callable, QuartzExecutionTaskType.CALLABLE, delay, period, timeUnit);
	}

	/**
	 * Submits a runnable for repeated time execution by the thread pool.
	 * @param fixedRate If true, task will be executed at a fixed rate, otherwise it will be executed at a fixed delay.
	 * @param runnable The runnable to schedule for repeated execution.
	 * @param executionCondition Optional execution condition that determines if the task should be executed at each period and if the task should be cancelled.
	 * @param delay The delay in the start of the schedule in ms.
	 * @param period The fixed delay or rate of the execution.
	 * @param timeUnit The unit of the period.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 */
	public <T> ScheduledTaskHandle<T> scheduleAtFrequency(boolean fixedRate, Runnable runnable, long delay, long period, TimeUnit timeUnit) {
		return scheduleAtFrequency(fixedRate, runnable, QuartzExecutionTaskType.RUNNABLE, delay, period, timeUnit);
	}

	/**
	 * Submits an execution task for repeated time execution by the thread pool.
	 * @param fixedRate If true, task will be executed at a fixed rate, otherwise it will be executed at a fixed delay.
	 * @param runnable The runnable to schedule for repeated execution.
	 * @param executionCondition Optional execution condition that determines if the task should be executed at each period and if the task should be cancelled.
	 * @param delay The delay in the start of the schedule in ms.
	 * @param period The fixed delay or rate of the execution.
	 * @param timeUnit The unit of the period.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 */
	public <T> ScheduledTaskHandle<T>  scheduleAtFrequency(boolean fixedRate, ExecutionTask<T> executionTask, long delay, long period, TimeUnit timeUnit) {
		return scheduleAtFrequency(fixedRate, executionTask, QuartzExecutionTaskType.EXECUTION_TASK, delay, period, timeUnit);
	}
	
	/**
	 * Schedules a callable for deferred execution.
	 * @param callable The callable to defer.
	 * @param delay The deferral period.
	 * @param timeUnit The unit of the deferral period.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 */
	@JMXOperation(name="scheduleDeferred", description="Schedules a Callable for deferred execution.")
	public <T> ScheduledTaskHandle<T> scheduleDeferred(
			@JMXParameter(name="callable", description="The callable to defer.") Callable<T> callable,
			@JMXParameter(name="delay", description="The deferral period.") long delay, 
			@JMXParameter(name="timeUnit", description="The unit of the deferral period.") TimeUnit timeUnit) {
		return scheduleDeferred(callable, delay, timeUnit,  QuartzExecutionTaskType.CALLABLE);
	}

	/**
	 * Schedules a runnable for deferred execution.
	 * @param runnable The runnable to defer.
	 * @param delay The deferral period.
	 * @param timeUnit The unit of the deferral period.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 */
	@JMXOperation(name="scheduleDeferred", description="Schedules a Runnable for deferred execution.")
	public <T> ScheduledTaskHandle<T> scheduleDeferred(
			@JMXParameter(name="runnable", description="The runnable to defer.") Runnable runnable,
			@JMXParameter(name="delay", description="The deferral period.") long delay, 
			@JMXParameter(name="timeUnit", description="The unit of the deferral period.") TimeUnit timeUnit) {
		return scheduleDeferred(runnable, delay, timeUnit,  QuartzExecutionTaskType.RUNNABLE);
	}

	/**
	 * Schedules an executionTask for deferred execution.
	 * @param executionTask The executionTask to defer.
	 * @param delay The deferral period.
	 * @param timeUnit The unit of the deferral period.
	 * @return A ScheduledTaskHandle to the scheduled task.
	 */
	@JMXOperation(name="scheduleDeferred", description="Schedules an executionTask for deferred execution.")
	public <T> ScheduledTaskHandle<T> scheduleDeferred(
			@JMXParameter(name="executionTask", description="The executionTask to defer.") ExecutionTask<T> executionTask,
			@JMXParameter(name="delay", description="The deferral period.") long delay, 
			@JMXParameter(name="timeUnit", description="The unit of the deferral period.") TimeUnit timeUnit) {
		return scheduleDeferred(executionTask, delay, timeUnit,  QuartzExecutionTaskType.EXECUTION_TASK);
	}
	
	
	/**
	 * Attribute indicating if the scheduler is paused.
	 * @return if the scheduler is paused.
	 */
	@JMXAttribute(name="Paused", description="True if if the scheduler is paused.", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getPaused() {
		try {
			return schedulerPaused.get();
		} catch (Exception e) {
			log.error("Failed to determine scheduler's paused state", e);
			throw new RuntimeException("Failed to determine scheduler's paused state", e);
		}
	}
	
	/**
	 * Sets the pause state of the scheduler.
	 * @param paused true to pause the scheduler, false to resume.
	 */
	public void setPaused(boolean paused) {
		boolean isPaused = getPaused(); 
		if(isPaused != paused) {
			if(isPaused) {
				resume();
			} else {
				pause();
			}
		}
	}
	
	/**
	 * Retrieves the number of scheduled jobs.
	 * @return the number of scheduled jobs.
	 */
	@JMXAttribute(name="TaskCount", description="The number of scheduled jobs.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTaskCount() {
		try {
			return innerScheduler.getJobNames(threadPool.getThreadGroupName()).length;
		} catch (Exception e) {
			log.error("Failed to retrieve scheduled job count.", e);
			return -1;
		}
	}
	
	/**
	 * Retrieves the number of frequency based jobs.
	 * @return the number of frequency based jobs.
	 */
	@JMXAttribute(name="FrequencyTaskCount", description="The number of frequency based jobs.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getFrequencyTaskCount() {
		return getTypeJobCount(SchedulingType.FREQUENCY, "frequency");
	}
	
	/**
	 * Retrieves the number of cron based jobs.
	 * @return the number of cron based jobs.
	 */
	@JMXAttribute(name="CronTaskCount", description="The number of cron based jobs.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getCronTaskCount() {
		return getTypeJobCount(SchedulingType.SCHEDULE, "cron");
	}

	/**
	 * Retrieves the number of deferred jobs.
	 * @return the number of deferred jobs.
	 */
	@JMXAttribute(name="DeferredTaskCount", description="The number of deferred jobs.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getDeferredTaskCount() {
		return getTypeJobCount(SchedulingType.DEFERRED, "deferred");
	}
	
	
	/**
	 * Internal task by type counter.
	 * @param taskCode The SchedulingType. (FREQUENCY, SCHEDULE, DEFERRED)
	 * @param taskType The textual name of the task type (frequency, cron, deferred)
	 * @return The number of jobs of the passed type.
	 */
	protected int getTypeJobCount(SchedulingType taskCode, String taskType) {
		try {
			int count = 0;
			for(String jobName : innerScheduler.getJobNames(threadPool.getThreadGroupName())) {
				try {
					SchedulingType schedType  = (SchedulingType)innerScheduler.getJobDetail(jobName, threadPool.getThreadGroupName()).getJobDataMap().get(QuartzExecutionTask.TASK_TYPE_KEY);
					if(taskCode.equals(schedType)) count++;
				} catch (Exception e) {}
			}
			return count;
		} catch (Exception e) {
			log.error("Failed to retrieve " + taskType + " based job count.", e);
			return -1;
		}		
	}
	
	
	/**
	 * Pauses the frequency, deferred and schedule firing of tasks.
	 * @see org.helios.jmx.threadservices.JMXManagedThreadPoolService#pause()
	 */
	public void pause() {
		try {
			if(!schedulerPaused.get()) {
				//innerScheduler.pauseAll();
				innerScheduler.pauseJobGroup(threadPool.getThreadGroupName());
				schedulerPaused.set(true);
			}
		} catch (Exception e) {
			log.warn("Helios Scheduler Failed to Pause", e);
			throw new RuntimeException("Helios Scheduler Failed to Pause", e);
		}
	}	
	
	/**
	 * Toggles the scheduler's pause state.
	 * @return The final state of the scheduler: true if paused, false if not.
	 */
	@JMXOperation(name="togglePauseState", description="Toggles the scheduler's pause state.")
	public boolean togglePauseState() {
		if(schedulerPaused.get()) {
			resume();
		} else {
			pause();
		}
		return schedulerPaused.get();
	}
	
	/**
	 * Resumes the frequency, deferred and schedule firing of tasks.
	 * @see org.helios.jmx.threadservices.JMXManagedThreadPoolService#resume()
	 */
	public void resume() {
		try {
			if(schedulerPaused.get()) {
				//innerScheduler.resumeAll();
				innerScheduler.resumeJobGroup(threadPool.getThreadGroupName());
				schedulerPaused.set(false);
			}
		} catch (Exception e) {
			log.warn("Helios Scheduler Failed to start", e);
			throw new RuntimeException("Helios Scheduler Failed to start", e);
		}
	}
	

	public static void main(String[] args) {
		try {
			log("Test HeliosScheduler");
			HeliosScheduler scheduler = HeliosScheduler.getInstance();
			log("Scheduler Instance:" + scheduler);	
			//scheduler.scheduleAtFrequency(fixedRate, callable, delay, period, timeUnit)
			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void log(Object message) {
		System.out.println(message);
	}


	/**
	 * Enables or disables instrumentation on the underlying thread pool.
	 * @param enabled 
	 */
	public void setInstrumentationEnabled(boolean enabled) {
		threadPool.setInstrumentationEnabled(enabled);		
	}
	
	/**
	 * The enabled state of the thread pool instrumentation.
	 * @return true if instrumentation is enabled, false if it is not.
	 */
	@JMXAttribute(name="InstrumentationEnabled", description="True if instrumentation is enabled, false if it is not.", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getInstrumentationEnabled() {
		return threadPool.getInstrumentationEnabled();		
	}


	/**
	 * Indicates if task executors are instrumented.
	 * @return taskExecutorInstrumentationEnabled if true, task executors are instrumented.
	 */
	@JMXAttribute(name="TaskInstrumentationEnabled", description="True if task instrumentation is enabled, false if it is not.", mutability=AttributeMutabilityOption.READ_WRITE)
	public synchronized boolean getTaskExecutorInstrumentationEnabled() {
		return taskExecutorInstrumentationEnabled;
	}


	/**
	 * Sets if task executors are instrumented.
	 * @param taskExecutorInstrumentationEnabled if true, task executors will be instrumented.
	 */
	public synchronized void setTaskExecutorInstrumentationEnabled(
			boolean taskExecutorInstrumentationEnabled) {
		this.taskExecutorInstrumentationEnabled = taskExecutorInstrumentationEnabled;
	}


	
}
