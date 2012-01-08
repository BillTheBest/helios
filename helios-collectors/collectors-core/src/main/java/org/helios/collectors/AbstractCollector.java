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
package org.helios.collectors;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.ObjectName;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.collectors.exceptions.CollectorException;
import org.helios.collectors.exceptions.CollectorInitException;
import org.helios.collectors.exceptions.CollectorStartException;
import org.helios.collectors.jmx.AbstractObjectTracer;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.SystemEnvironmentHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.scheduling.HeliosScheduler;
import org.helios.jmx.threadservices.scheduling.NamedTask;
import org.helios.jmx.threadservices.scheduling.ObjectExecutionTask;
import org.helios.jmx.threadservices.scheduling.ScheduledTaskHandle;
import org.helios.logging.appenders.LinkedListAppender;
import org.helios.misc.BlackoutInfo;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.helios.scripting.ScriptBeanException;
import org.helios.scripting.ScriptBeanWrapper;
import org.springframework.beans.factory.BeanNameAware;


/**
 * <p>Title: AbstractCollector</p>
 * <p>Description: Abstract Base class for each Helios Collector.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
@JMXManagedObject (declared=true, annotated=true)
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating exception during collect callback for any collector", types={
                @JMXNotificationType(type="org.helios.collectors.exception.notification")
        }),
        @JMXNotification(description="Notification indicating change in CollectorState", types={
                @JMXNotificationType(type="org.helios.collectors.AbstractCollector.CollectorState")
        })       
})
public abstract class AbstractCollector extends ManagedObjectDynamicMBean implements Callable<CollectionResult>, ICollector, NamedTask, BeanNameAware {
	
	private static final long serialVersionUID = 8420908979451738697L;

	/**
	 * Indicates that a collection should be executed as soon as the collector 
	 * starts. Otherwise, the first collection will occur in accordance with 
	 * the schedule and/or frequency.
	 */
	protected boolean immediateCollect=false;
	
	/** The root tracing namespace where all collected metrics will be traced to */
	protected String[] tracingNameSpace;
	
	/** 
	 * An instance level logger which is set to <flattened namespace>.<bean name> 
	 * so that the collectors logging statements are distinguishable in a 
	 * shared log file.
	 */
	protected Logger log;
	
	/**
	 * An logging facility internal to and exclusively for the current instance 
	 * of the collector. These are operational messages that are written to an 
	 * internal fixed size circular buffer. The contents of the internal log can 
	 * be viewed from the management interface.
	 */
	protected LinkedListAppender llAppender;
	
	
	/**Logger to store internal debugging messages*/
	protected Logger internalLog;
	
	/** Indicates if details of a collector failures should be logged. */
	protected boolean logErrors=false;
	
	/** 
	 * Indicates if the base class should trace the status and summary 
	 * results (collection instance metadata) after each collection.
	 */	
	protected boolean logCollectionResult=false;
	
	/**
	 * The frequency of collections in milliseconds. A value of zero 
	 * means there are no frequency based collection callbacks.
	 */
	protected long frequency = 15000L;
	
	/**
	 * A cron expression indicating a schedule of collections. 
	 * A null value means there are no scheduled collection callbacks.
	 */
	protected String schedule;
	
	/**
	 * A delay period between the collector start and the beginning of 
	 * the scheduled frequency. For example, if a collector is configured 
	 * with immediateCollect=false, frequency=10000, initialDelay=15000, 
	 * the first collection will occur 25000 ms after start.
	 */
	protected long initialDelay = 0L;
	
	/**
	 * Indicates that the collector should be reset on the collect 
	 * callback if this amount of time (in ms) has elapsed since the last reset.
	 */
	protected long resetFrequency = 0L;

	/**
	 * Indicates that the collector should be reset on the collect 
	 * callback based on this cron schedule.
	 */
	protected String resetSchedule = null;
	
	/** Indicates that the collector should be reset every resetCount collections. */
	protected int resetCount;
	
	/** Flag to indicate whether reset is called on this collector */
	protected AtomicBoolean resetFlag = new AtomicBoolean(false);	
	
	/**
	 * An instance of a Helios OpenTrace Factory to get a handle of TracerImpl instance */
	protected TracerManager3 tracerFactory;
	
	/**
	 * An instance of a Helios OpenTrace TracerImpl to be used for tracing in the 
	 * collector. If the tracer is null at init, the collector should call OpenTrace 
	 * TraceFactory.getInstance().getTracer() to get the default tracer.
	 */
	protected ITracer tracer;
	
	/**
	 * An exception to the above rule. At init, if the tracer is null, but tracerName is 
	 * not null, the collector should call OpenTrace TraceFactory.getInstance(tracerName)
	 * to get the named tracer.
	 */
	protected String tracerName;
	
	/**
	 * Name with which this collector's MBean will be registered in an MBeanServer.  If this property
	 * is not provided, a default value will be created.  The domain name will be set to the package name
	 * of AbstractCollector, type will be the name of implementation class of this AbstractCollector and   
	 * name will be set to the Bean Id provided through the configuration file
	 *  
	 * Default Value: <package_name>:type=<Class_name>,name=<Bean_Id>
	 * eg: org.helios.collectors:type=URLCollector,name=LoginPage
	 */
	protected ObjectName objectName;
	
	/**  
	 * An enum that defines the state of the collector. Lifecycle operations 
	 * should automatically set this state in each case.
	 */
	protected enum CollectorState {
		NULL, 
		CONSTRUCTED, 
		INITIALIZING, 
		INITIALIZED,
		INIT_FAILED,
		STARTING, 
		STARTED, 
		START_FAILED, 
		STOPPING, 
		STOPPED, 
		COLLECTING,
		RESETTING
	}
	
	/** Indicates the current state of the collector. */
	private CollectorState state;
	
	/** Last time a collection was started. */
	protected long lastTimeCollectionStarted = 0L;
	
	/** Last time a collection finished. */
	protected long lastTimeCollectionCompleted = 0L;
	
	/** Last time a collection successfully completed. */
	protected long lastTimeCollectionSucceeded = 0L;
	
	/** Last time a collection failed. */
	protected long lastTimeCollectionFailed = 0L;
	
	/** Spring oriented identity for this collector */
	protected String beanName;

	/** Reference of a POJO that stores information about the last collection result  */
	protected CollectionResult collectionResult;
	
	/** Used during reset  */
	protected final ReentrantLock collectorLock = new ReentrantLock();
	
	/**
	 * Scheduled collect call should wait for this period if the current  
	 * CollectorState is RESETTING
	 */
	protected long waitPeriodIfResetting = 0L;
	
	/** Total number of times a collection is done. */
	protected int totalCollectionCount=0;
	
	/** Number of time a collector succeed. */
	protected int totalSuccessCount=0;
	
	/** Number of time the collector failed. */
	protected int totalFailureCount=0;	
	
	/** Total number of collectors running at this time time in this JVM. */
	protected static AtomicInteger numberOfCollectorsRunning = new AtomicInteger();
	
	/**
	 * If saveStackTraces is true, the 
	 * collector will retain a copy of the thread stack trace after the 
	 * last init, start and collect methods. The management interface 
	 * provides an operation to display the stack traces in string format 
	 */
	protected boolean saveStackTraces = false;
	
	/** 
	 * Snapshot of thread's stack trace after the completion 
	 * of init lifecycle method.  Used for debugging purpose. 
	 */
	protected StackTraceElement[] initStackTrace;
	
	/** 
	 * Snapshot of thread's stack trace after the completion 
	 * of start lifecycle method.  Used for debugging purpose. 
	 */	
	protected StackTraceElement[] startStackTrace;
	
	/** 
	 * Snapshot of thread's stack trace after the completion 
	 * of last collect method.  Used for debugging purpose. 
	 */	
	protected StackTraceElement[] lastCollectStackTrace;
	
	/** The MBeanServer's default domnain */
	protected static String defaultDomain = null;	
	
	protected MBeanServer server = null;

	/** TODO Spring Bean Factory Reference
	protected BeanFactory beanFactory;	
	*/	
	
	/** Reference to the primary task scheduling service for Helios */
	protected HeliosScheduler hScheduler = null;
	
	/** Handle to a scheduled frequency task */
	protected ScheduledTaskHandle<CollectionResult> frequencyTaskHandle = null;
	
	/** Handle to a scheduled cron task */
	protected ScheduledTaskHandle<CollectionResult> cronTaskHandle = null;
	
	/** Handle to a scheduled frequency based resets */
	protected ScheduledTaskHandle<AbstractCollector> frequencyResetTaskHandle = null;
	
	/** Handle to a scheduled cron based resets */
	protected ScheduledTaskHandle<AbstractCollector> cronResetTaskHandle = null;	
	
	/** The property name where the jmx default domain is referenced */
	public static final String JMX_DOMAIN_PROPERTY = "helios.opentrace.config.jmx.domain";
	/** The default jmx default domain is referenced */
	public static final String JMX_DOMAIN_DEFAULT = "DefaultDomain";
	
	protected static final Pattern namePattern = Pattern.compile("\\{(\\d++)\\}");
	protected static final Pattern thisPattern = Pattern.compile("\\{THIS-PROPERTY:([a-zA-Z\\(\\)\\s-]+)}");
	protected static final Pattern thisDomainPattern = Pattern.compile("\\{THIS-DOMAIN:([\\d+])}");	
	protected static final Pattern segmentPattern = Pattern.compile("\\{SEGMENT:([\\d+])}");	
	protected static final Pattern targetDomainPattern = Pattern.compile("\\{TARGET-DOMAIN:([\\d+])}");	
	protected static final Pattern targetPattern = Pattern.compile("\\{TARGET-PROPERTY:([a-zA-Z\\(\\)\\s-]+)}");	
	
	/** The property name where the JMX domain name for all collectors would be picked */
	protected static final String COLLECTORS_DOMAIN_PROPERTY="helios.collectors.jmx.domain";
	/** Default JMX domain name for all collectors in case COLLECTORS_DOMAIN_PROPERTY is not specified*/
	protected static final String COLLECTORS_DOMAIN_DEFAULT="org.helios.collectors";
	
	
	private static AtomicLong notificationSerialNumber = new AtomicLong(0);	
	
	/** A label to be displayed on Helios dashboard for Availability of this collector */
	protected String defaultAvailabilityLabel="Availability";
	/** Number of failures during collect before Helios switches to a alternate (slower) frequency for this collector */
	protected int failureThreshold = 5;
	/** Number of consecutive errors produced by this collector so far */
	protected int consecutiveFailureCount = 0;
	/** Flag that indicates whether the alternate frequency is in effect due to recent error while collection. */
	protected boolean fallbackFrequencyActivated=false;
	/** Number of collections to skip when fallbackFrequencyActivated is true */
	protected int actualSkipped=0;
	/** 
	 *  Number of collect iterations to skip when fallbackFrequencyActivated is true.
	 *  Set this parameter to -1 to explicitly disable dormant mode.  This is usually desired when the frequency 
	 *  or schedule (cron expression) for this collector is set high.  For example (collectors that runs 
	 *  once every hour or daily etc...) 
	 * 
	 */
	protected int iterationsToSkip = 5;
	
	/** Number of attempts made so far to restart this collector after the initial start failed. */
	protected int restartAttempts = 0;
	/** 
	 * Maximum number of attempts that will be made to restart a collector whose initial start has failed. 
	 * Though not recommended but if you want the restart to be tried indefinitely, then set 
	 * this parameter to -1.
	 */
	protected int maxRestartAttempts = 3;
	
	/** Delay before another attempt is made to restart a collector.  Default is 2 minutes. */
	protected long restartAttemptDelayFrequency = 120000L;
	
	/** Handle to a scheduled cron task */
	protected ScheduledTaskHandle<AbstractCollector> restartTaskHandle = null;
	
	protected ScriptBeanWrapper startExceptionScript = null;
	
	protected ScriptBeanWrapper collectExceptionScript = null;
	
	/** Start of blackout period - expressed in HH:MM format.  HH is in the 24 hour format **/
	protected String blackoutStart = null;
	/** End of blackout period - expressed in HH:MM format.  HH is in the 24 hour format **/
	protected String blackoutEnd = null;
	/** Object to hold detailed blackout information */
	private BlackoutInfo blackoutInfo = null;	
	

	/**
	 * Classes extending AbstractCollector should call this constructor using super() 
	 */
	public AbstractCollector() {
		initializeLogging();
		setState(CollectorState.CONSTRUCTED);
	}
	
	/**
	 * Returns version of Helios Core Collector
	 * 
	 * @return version of AbstractCollector
	 */
	@JMXAttribute (name="Version", description="Displays Helios core collector version", mutability=AttributeMutabilityOption.READ_ONLY)
	public static String getVersion() {
		return "Helios Collectors v. @VERSION@";
	}	
	
	/**
	 * Returns version of concrete implementation of AbstractCollector.
	 * Implementation of this method should be provided by all concrete collector classes
	 * 
	 * @return version of concrete implementation classes
	 */
	public abstract String getCollectorVersion();

	/**
	 * Register this collector as an MBean to the MBean Server using objectName
	 *  
	 * @param collector
	 */
	protected void registerInMBeanServer(AbstractCollector collector){
		defaultDomain = SystemEnvironmentHelper.getSystemPropertyThenEnv(JMX_DOMAIN_PROPERTY, JMX_DOMAIN_DEFAULT);
		server = JMXHelper.getLocalMBeanServer(defaultDomain);
		try{
			server.registerMBean(collector, objectName);
		}catch (InstanceAlreadyExistsException iaex) {
			if(logErrors)
				log.error("Failed to register MBean with objectName: [" +objectName+ "] as it is already registered.", iaex);			
		} catch (Exception ex) {
			if(logErrors)
				log.error("An unexpected error occured while registering MBean with objectName: [" +objectName+ "].", ex);			
		}
	}
		
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre initialization tasks that needs to be done.
	 */
	public void preInit() throws CollectorInitException {}	
	
	/**
	 * Initializes basic resources that are critical for any collector to work properly.
	 * This method cannot be overriden by concrete implementation classes.
	 *  
	 * @throws CollectorInitException
	 */
	public final void init() throws CollectorInitException {
		setState(CollectorState.INITIALIZING);
		try{
			if(tracer==null){
				if(tracerFactory!=null){
						tracer = tracerFactory.getTracer();
				}else{
					log.error("Tracer Factory not initialized properly for collector bean: "+this.getBeanName());				
				}
			}
			preInit();
			initCollector();
			postInit();
			if(objectName==null){
				try {
					this.objectName = ObjectName.getInstance(SystemEnvironmentHelper.getSystemPropertyThenEnv(COLLECTORS_DOMAIN_PROPERTY, COLLECTORS_DOMAIN_DEFAULT)+":type="+this.getClass().getName().substring( this.getClass().getName().lastIndexOf( '.' ) + 1 )+",name="+this.getBeanName());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(blackoutStart!=null && blackoutEnd!=null){
				blackoutInfo = new BlackoutInfo(blackoutStart, blackoutEnd, beanName);
				if(!blackoutInfo.isValidRange()){
					blackoutInfo = null;
				}
			}
			if(saveStackTraces){
				initStackTrace=Thread.currentThread().getStackTrace();
			}
			setState(CollectorState.INITIALIZED);
		} catch(CollectorInitException ciex){
			setState(CollectorState.INIT_FAILED);
			if(logErrors)
				log.error("An error occured while initializing the collector bean: "+this.getBeanName(),ciex);
		} catch(Exception ex){
			setState(CollectorState.INIT_FAILED);
			if(logErrors)
				log.error("An error occured while initializing the collector bean: "+this.getBeanName(),ex);
		}		
	}
	
	/**
	 * An additional convenience method provided for implementing task that needs to be 
	 * performed for initialization of this collector
	 */
	public void initCollector() throws CollectorInitException{}		
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom post initialization tasks that needs to be done.
	 */
	public void postInit() throws CollectorInitException {}	
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre startup tasks that needs to be done.
	 */
	public void preStart() throws CollectorStartException{}
	
	/**
	 * This method is an entry point to kickstart any collectors' lifecycle.  It initializes and 
	 * performs startup tasks before this collector is scheduled with HeliosScheduler.
	 * This method cannot be overriden by concrete implementation classes.  
	 *  
	 * @throws CollectorStartException
	 */
	@JMXOperation (name="start", description="Start this collector")
	public final void start() throws CollectorStartException{
		try {
			init();
			if(getState() != CollectorState.INITIALIZED){
				log.error("Initialization error for bean: "+this.getBeanName() + ", so no further attempts would be made to start it.");
				executeExceptionScript();
				return;
			}
			setState(CollectorState.STARTING);
			preStart();
			startCollector();
			postStart();	
			if(immediateCollect) {
				log.info("About to trigger immediate collect for collector bean: " + this.getBeanName());
				if(hScheduler==null)
					hScheduler = HeliosScheduler.getInstance();
				hScheduler.scheduleDeferred(this, 5, TimeUnit.MILLISECONDS);
			}
			registerInMBeanServer(this);
			scheduleCollect();
			scheduleReset();
			setState(CollectorState.STARTED);
			logBanner("Collector ", this.getBeanName(), " Started");
		} catch (CollectorStartException csex){
			setState(CollectorState.START_FAILED);
			if(logErrors)
				log.error("An error occured while starting the collector bean: "+this.getBeanName(),csex);
			else
				log.error("An error occured while starting the collector bean: "+this.getBeanName());
			executeExceptionScript();
			scheduleRestart();
		} catch (Exception ex){
			setState(CollectorState.START_FAILED);
			if(logErrors)
				log.error("An error occured while starting the collector bean: "+this.getBeanName(),ex);
			else
				log.error("An error occured while starting the collector bean: "+this.getBeanName());
			scheduleRestart();
			executeExceptionScript();
		} finally {
			if(saveStackTraces){
				startStackTrace=Thread.currentThread().getStackTrace();
			}			
		}
	}
	
	protected void executeExceptionScript(){
		Map<String, Object> bindings=new HashMap<String, Object>();
		if(log==null)
			log=Logger.getLogger(AbstractObjectTracer.class);
		bindings.put("log",log);
		try{
			if(getState() == CollectorState.START_FAILED || getState() == CollectorState.INIT_FAILED){
				if(startExceptionScript != null){
					startExceptionScript.getScriptBindings().putAll(bindings);
					((IScriptAction)startExceptionScript.getScriptBeanInterface()).executeScript();
				}
			}else{
				if(collectExceptionScript != null){
					collectExceptionScript.getScriptBindings().putAll(bindings);
					((IScriptAction)collectExceptionScript.getScriptBeanInterface()).executeScript();
				}
			}
		}catch(ScriptBeanException sbex){
			log.error("An error occured while executing exception script for collector bean: "+ this.getBeanName(), sbex);
		}
	}
	
	protected void scheduleRestart(){
		if(!(maxRestartAttempts == -1) && restartAttempts >= maxRestartAttempts){
			log.error("Restart attempts for bean: "+this.getBeanName() + " is exhausted, so no further attempts will be made.");
			return;
		}
		restartAttempts++;
		if(hScheduler==null)
			hScheduler = HeliosScheduler.getInstance();		
		if(restartAttemptDelayFrequency > 0L){
			Object[] arguments = {};
			try{
				restartTaskHandle = hScheduler.scheduleDeferred(new ObjectExecutionTask<AbstractCollector>(this,this.getClass().getMethod("start",null),arguments), restartAttemptDelayFrequency, TimeUnit.MILLISECONDS);
				log.info("Another attempt to start bean: "+this.getBeanName() + " will be made in "+ restartAttemptDelayFrequency + " milliseconds.");
			} catch(NoSuchMethodException ncmex){}
		}
	}
	
	/**
	 * Abstract method for which implementation is provide by concrete collector 
	 * classes. 
	 */
	public abstract void startCollector() throws CollectorStartException;

	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom post startup tasks that needs to be done.
	 */
	public void postStart() throws CollectorStartException{}


	/**
	 * Callback method for HeliosScheduler to trigger the start of a collection. 
	 */
	public CollectionResult call() throws CollectorException {
		collect();
		return collectionResult;
	}	
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre collect tasks that needs to be done.
	 */
	public void preCollect() {}
	
	/**
	 * This method ties up the functionality and sequencing of pre, post and collectCallback methods.  It cannot be 
	 * overridden by concrete collector classes
	 */
	public final void collect() throws CollectorException{
		//Check whether blackout period specified for this collector and whether it's active
		if(blackoutInfo!=null && blackoutInfo.isBlackoutActive()){
			log.debug("*** Skipping collection as blackout period is active...");
			return;
		}
		
		//Check whether this collector is running in a dormant mode
		if(fallbackFrequencyActivated && iterationsToSkip!=-1){
			if(actualSkipped < iterationsToSkip ){
				actualSkipped++;
				log.debug("*** Skipping iteration " + actualSkipped + " for collector " +this.getBeanName()+" as it is in fallbackFrequencyActivated mode");
				return;
			}else{
				actualSkipped=0;
			}
		}
		CollectorState currState = getState();
		boolean errors=false;
		boolean gotLock=false;
		
		if(collectorLock.isLocked()){
			try{
				gotLock = collectorLock.tryLock(waitPeriodIfResetting, TimeUnit.MILLISECONDS);
				if(!gotLock){
					log.error("Unable to obtain a lock on collector bean: "+this.getBeanName());
					return;
				}
			}catch(InterruptedException ex){}
		}
		if(resetFlag.get()==true || (resetCount>0 && totalCollectionCount!=0 && totalCollectionCount%resetCount==0)){
			try{
				this.reset();
				resetFlag.getAndSet(false);
			}catch(Exception ex){
				if(logErrors)
					log.error("An exception occured while resetting the collector bean: "+this.getBeanName(),ex);
			}
		}
		if(getState() == CollectorState.STARTED){
			try {
				lastTimeCollectionStarted=System.currentTimeMillis();
				setState(CollectorState.COLLECTING);
				numberOfCollectorsRunning.incrementAndGet();
				preCollect();
				collectionResult = collectCallback();
				if(collectionResult.getResultForLastCollection() == CollectionResult.Result.FAILURE){
					throw new CollectorException(collectionResult.getAnyException());
				}
				lastTimeCollectionSucceeded=System.currentTimeMillis();
				totalSuccessCount++;
				if(fallbackFrequencyActivated){
					/* This collector is running on a fallback frequency.  Since this collect call was
					 * successful, switch the collect frequency back to normal schedule now. */
					fallbackFrequencyActivated=false;
					consecutiveFailureCount=0;
					actualSkipped=0;
					log.info("*** Frequency for collector: " + this.getBeanName() +" is switched back to normal now.");
				}
				logBanner("Completed Collect for", this.getBeanName());
			} catch (Exception ex){
				lastTimeCollectionFailed=System.currentTimeMillis();
				errors=true;
				totalFailureCount++;
				consecutiveFailureCount++;
				if(consecutiveFailureCount>=failureThreshold && !fallbackFrequencyActivated){
					log.info("*** Slowing down the collect frequency for bean: " + this.getBeanName() +" as it has exceeded the collectFailureThreshold parameter.");
					fallbackFrequencyActivated=true;
				}				
				this.sendNotification(new Notification("org.helios.collectors.exception.notification",this,notificationSerialNumber.incrementAndGet(),lastTimeCollectionFailed,this.getBeanName()));
				internalLog.info("Last Failed Collection Elapsed Time: " + (lastTimeCollectionFailed - lastTimeCollectionStarted)+" milliseconds");
				if(logErrors)
					log.error("Collection failed for bean collector: "+this.getBeanName(),ex);
				executeExceptionScript();
			}finally {
				totalCollectionCount++;
				setState(currState);
				if(!errors){
					postCollect();
					lastTimeCollectionCompleted=System.currentTimeMillis();
					log.debug("Last Collection Elapsed Time: " + (lastTimeCollectionCompleted - lastTimeCollectionStarted)+ " milliseconds");
				}
				if(logCollectionResult) 
					logCollectionResultDetails();
				if(saveStackTraces) 
					lastCollectStackTrace=Thread.currentThread().getStackTrace();
				if(collectorLock.isLocked())
					collectorLock.unlock();
				numberOfCollectorsRunning.decrementAndGet();
			}	
		} else {
			log.trace("Not executing collect method as the collector state is not STARTED.");
		}
	}

	/**
	 * 
	 */
	private void logCollectionResultDetails() {
		internalLog.info("Last Collection Result: " + collectionResult.getResultForLastCollection());
		internalLog.info("Last Collection Started at: " + returnDate(lastTimeCollectionStarted));
		internalLog.info("Last Collection Succeeded at: " + returnDate(lastTimeCollectionSucceeded));
		internalLog.info("Last Collection Completed at: " + returnDate(lastTimeCollectionCompleted));
		internalLog.info("Last Collection Failed at: " + returnDate(lastTimeCollectionFailed));
		internalLog.info("Total number of collections so far: "+totalCollectionCount);
		internalLog.info("Successful collections: "+totalSuccessCount);
		internalLog.info("Failed collections: "+totalFailureCount);
		internalLog.info("Consecutive failed collections: "+consecutiveFailureCount);
	}
	
	private String returnDate(long millis){
		if(millis!=0)
			return new Date(millis).toString();
		else
			return "";
	}
	
	/**
	 * Collector specific collection tasks that should be implemented by concrete collector classes
	 */
	public abstract CollectionResult collectCallback();
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre collect tasks that needs to be done.
	 */
	public void postCollect() {}
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre stop tasks that needs to be done.
	 */
	public void preStop() {}
	
	/**
	 * This method ties up the functionality and sequencing of pre, post and stopCollector methods.  It cannot be 
	 * overridden by concrete collector classes
	 */
	@JMXOperation (name="stop", description="Stop this collector")
	public final void stop() throws CollectorException{
		if(getState()==CollectorState.STOPPED || getState()==CollectorState.STOPPING)
			return;
		setState(CollectorState.STOPPING);
		try {
			preStop();
			unScheduleCollect();
			unScheduleReset();			
			stopCollector();
			postStop();
			setState(CollectorState.STOPPED);
			logBanner("Collector", this.getBeanName()+" Stopped");
		} catch (Exception ex){
			throw new CollectorException("An error occured while stopping collector bean: " + this.getBeanName(),ex); 
		} 
	}
	
	/**
	 * An additional convenience method provided for implementing task that needs to be 
	 * performed for stopping this collector
	 */
	public void stopCollector() {}
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom post stop tasks that needs to be done.
	 */
	public void postStop() {
		
	}
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre reset tasks that needs to be done.
	 */
	public void preReset() {}
	
	/**
	 * This method ties up the functionality and sequencing of pre, post and resetCollector methods.  It cannot be 
	 * overridden by concrete collector classes
	 */
	@JMXOperation (name="reset", description="Reset this collector")
	public final void reset() throws CollectorException{
		CollectorState currState = getState();
		setState(CollectorState.RESETTING);
		// acquires a re-enterent lock to prevent collectorCallback from 
		// executing while reset is happening.
		collectorLock.lock();
		try {
			preReset();
			resetCollector();
			postReset();
			setState(currState);
			logBanner("Reset Completed for Collector", this.getBeanName());
		} catch (Exception ex){
			throw new CollectorException("An error occured while resetting the collector bean: " + this.getBeanName(),ex);
		} finally {
			collectorLock.unlock();
		}		
	}

	/**
	 * An additional convenience method provided for implementing task that needs to be 
	 * performed for resetting this collector
	 */
	public void resetCollector() {}
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom post reset tasks that needs to be done.
	 */
	public void postReset() {}	
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre destroy tasks that needs to be done.
	 */
	public void preDestroy() {}
	
	/**
	 * This method ties up the functionality and sequencing of pre, post and destroyCollector methods.  It cannot be 
	 * overridden by concrete collector classes
	 */
	public final void destroy() throws CollectorException{
		try {
			preDestroy();
			destroyCollector();
			unregisterFromMBeanServer(objectName);
			unScheduleCollect();
			unScheduleReset();
			postDestroy();
			logBanner("Collector", this.getBeanName()," Destroyed");
		} catch(Exception ex){
			throw new CollectorException("An error occured while destroying collector bean: " + this.getBeanName(),ex);
		}	
	}

	protected void unregisterFromMBeanServer(ObjectName objName)
			throws InstanceNotFoundException, MBeanRegistrationException {
		if(server!=null && server.isRegistered(objName)){
			server.unregisterMBean(objName);
			logBanner("Collector: ", this.getBeanName()," is unregistered from MBean server.");
		}
	}
	
	/**
	 * An additional convenience method provided for implementing task that needs to be 
	 * performed for destroying this collector
	 */
	public void destroyCollector() {}	
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom post destroy tasks that needs to be done.
	 */
	public void postDestroy() {}	
	
	/**
	 * @return the immediateCollect
	 */
	@JMXAttribute (name="ImmediateCollect", description="Flag to indicate the collector should start as soon as it's instantiated")
	public boolean getImmediateCollect() {
		return immediateCollect;
	}

	/**
	 * @param immediateCollect the immediateCollect to set
	 */
	public void setImmediateCollect(boolean immediateCollect) {
		this.immediateCollect = immediateCollect;
	}

	/**
	 * @return the tracingNameSpace
	 */
	@JMXAttribute (name="TracingNameSpace", description="Displays the namespace under which this collectors' statistics should be traced.")	
	public String[] getTracingNameSpace() {
		return tracingNameSpace.clone();
	}

	/**
	 * @param tracingNameSpace the tracingNameSpace to set
	 */
	public void setTracingNameSpace(String[] tracingNameSpace) {
		this.tracingNameSpace = tracingNameSpace;
	}

	/**
	 * @return the log
	 */
	public Logger getLog() {
		return log;
	}

	/**
	 * @param log the log to set
	 */
	public void setLog(Logger log) {
		this.log = log;
	}

	/**
	 * @return the internalLog
	 */
	@JMXAttribute (name="InternalLog", description="Displays messages from internal log.", mutability=AttributeMutabilityOption.READ_ONLY)
	public Logger getInternalLog() {
		return internalLog;
	}

	/**
	 * @param internalLog the internalLog to set
	 */
	public void setInternalLog(Logger internalLog) {
		this.internalLog = internalLog;
	}

	/**
	 * @return the logErrors
	 */
	@JMXAttribute (name="LogErrors", description="Indicates whether errors should be logged to the collectors' log")
	public boolean getLogErrors() {
		return logErrors;
	}

	/**
	 * @param logErrors the logErrors to set
	 */
	public void setLogErrors(boolean logErrors) {
		this.logErrors = logErrors;
	}

	/**
	 * @return the logCollectionState
	 */
	@JMXAttribute (name="LogCollectionResult", description="Indicates whether results from last collection should be displayed in the log")
	public boolean getLogCollectionResult() {
		return logCollectionResult;
	}

	/**
	 * @param logCollectionState the logCollectionState to set
	 */
	public void setLogCollectionResult(boolean logCollectionResult) {
		this.logCollectionResult = logCollectionResult;
	}

	/**
	 * @return the frequency
	 */
	@JMXAttribute (name="Frequency", description="Frequency at which this collector is executed.")	
	public long getFrequency() {
		return frequency;
	}

	/**
	 * @param frequency the frequency to set
	 */
	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	/**
	 * @return the schedule
	 */
	@JMXAttribute (name="Schedule", description="Cron like Schedule at which this collector is executed.")
	public String getSchedule() {
		return schedule;
	}

	/**
	 * @param schedule the schedule to set
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * @return the resetFrequency
	 */
	@JMXAttribute (name="ResetFrequency", description="Frequency at which the statistics should be reset.")
	public long getResetFrequency() {
		return resetFrequency;
	}

	/**
	 * @param resetFrequency the resetFrequency to set
	 */
	public void setResetFrequency(long resetFrequency) {
		this.resetFrequency = resetFrequency;
	}

	/**
	 * @return the resetCount
	 */
	@JMXAttribute (name="ResetCount", description="Number of times this collector is reset since last startup.")
	public int getResetCount() {
		return resetCount;
	}

	/**
	 * @param resetCount the resetCount to set
	 */
	public void setResetCount(int resetCount) {
		logBanner("Reset Called");
		this.resetCount = resetCount;
	}

	/**
	 * @return the tracer
	 */
	@JMXAttribute (name="Tracer", description="Tracer that will trace all statistics for this collector.", mutability=AttributeMutabilityOption.READ_ONLY)
	public ITracer getTracer() {
		return tracer;
	}

	/**
	 * @param tracer the tracer to set
	 */
	public void setTracer(ITracer tracer) {
		this.tracer = tracer;
	}

	/**
	 * @return the tracerName
	 */
	@JMXAttribute (name="TracerName", description="Tracer name")
	public String getTracerName() {
		return tracerName;
	}

	/**
	 * @param tracerName the tracerName to set
	 */
	public void setTracerName(String tracerName) {
		this.tracerName = tracerName;
	}

	/**
	 * @return the objectName
	 */
	@JMXAttribute (name="ObjectName", description="Object name with which this collector is registered as an MBean.", mutability=AttributeMutabilityOption.READ_ONLY)
	public ObjectName getObjectName() {
		return objectName; 
	}

	/**
	 * @param objectName the objectName to set
	 */
	public void setObjectName(ObjectName oName) {
//		try {
//			this.objectName = ObjectName.getInstance(oName);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		objectName=oName;
	}

	/**
	 * @return the beanName
	 */
	@JMXAttribute (name="BeanName", description="Name of this bean defined in the configuration file.", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getBeanName() {
		return beanName;
	}

	/**
	 * @param beanName the beanName to set
	 */
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
	
	/**
	 * @return the collectorState
	 */
	public synchronized CollectorState getState() {
		return state;
	}

	/**
	 * @param collectorState the collectorState to set
	 */
	public synchronized void setState(CollectorState s) {
		CollectorState oldState = getState();
		state = s;
		if( 	(getState()==CollectorState.INIT_FAILED) 
			 || (getState()==CollectorState.START_FAILED) 
			 || (getState()==CollectorState.STOPPED)
			 || (getState()==CollectorState.STARTED && oldState==CollectorState.STARTING  )  )
		this.sendNotification(new Notification("org.helios.collectors.AbstractCollector.CollectorState",this,notificationSerialNumber.incrementAndGet(),System.currentTimeMillis(),getState().toString()));
	}

	/**
	 * @return the lastTimeCollectionStarted
	 */
	@JMXAttribute (name="LastTimeCollectionStarted", description="Timestamp when the last collection started.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastTimeCollectionStarted() {
		return lastTimeCollectionStarted;
	}

	/**
	 * @param lastTimeCollectionStarted the lastTimeCollectionStarted to set
	 */
	public void setLastTimeCollectionStarted(long lastTimeCollectionStarted) {
		this.lastTimeCollectionStarted = lastTimeCollectionStarted;
	}

	/**
	 * @return the lastTimeCollectionCompleted
	 */
	@JMXAttribute (name="LastTimeCollectionCompleted", description="Timestamp when the last collection completed.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastTimeCollectionCompleted() {
		return lastTimeCollectionCompleted;
	}

	/**
	 * @param lastTimeCollectionCompleted the lastTimeCollectionCompleted to set
	 */
	public void setLastTimeCollectionCompleted(long lastTimeCollectionCompleted) {
		this.lastTimeCollectionCompleted = lastTimeCollectionCompleted;
	}

	/**
	 * @return the lastTimeCollectionSucceeded
	 */
	@JMXAttribute (name="LastTimeCollectionSucceeded", description="Timestamp when the last collection completed successfully.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastTimeCollectionSucceeded() {
		return lastTimeCollectionSucceeded;
	}

	/**
	 * @param lastTimeCollectionSucceeded the lastTimeCollectionSucceeded to set
	 */
	public void setLastTimeCollectionSucceeded(long lastTimeCollectionSucceeded) {
		this.lastTimeCollectionSucceeded = lastTimeCollectionSucceeded;
	}

	/**
	 * @return the lastTimeCollectionFailed
	 */
	@JMXAttribute (name="LastTimeCollectionFailed", description="Timestamp when the last collection failed.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastTimeCollectionFailed() {
		return lastTimeCollectionFailed;
	}
	
	/**
	 * @param lastTimeCollectionFailed the lastTimeCollectionFailed to set
	 */
	public void setLastTimeCollectionFailed(long lastTimeCollectionFailed) {
		this.lastTimeCollectionFailed = lastTimeCollectionFailed;
	}

	/**
	 * @return the waitPeriodIfResetting
	 */
	public long getWaitPeriodIfResetting() {
		return waitPeriodIfResetting;
	}

	/**
	 * @param waitPeriodIfResetting the waitPeriodIfResetting to set
	 */
	public void setWaitPeriodIfResetting(long waitPeriodIfResetting) {
		this.waitPeriodIfResetting = waitPeriodIfResetting;
	}

	/**
	 * @return the successCount
	 */
	@JMXAttribute (name="TotalSuccessCount", description="Number of times this collector was successfull.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTotalSuccessCount() {
		return totalSuccessCount;
	}

	/**
	 * @return the failureCount
	 */
	@JMXAttribute (name="TotalFailureCount", description="Number of times this collector failed.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTotalFailureCount() {
		return totalFailureCount;
	}

	/**
	 * @return the checkCount
	 */
	@JMXAttribute (name="TotalCheckCount", description="Total number of times this collector was executed.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTotalCheckCount() {
		return totalCollectionCount;
	}

	/**
	 * @return the tracerFactory
	 */
	public TracerManager3 getTracerFactory() {
		return tracerFactory;
	}

	/**
	 * @param tracerFactory the tracerFactory to set
	 */
	public void setTracerFactory(TracerManager3 tracerFactory) {
		this.tracerFactory = tracerFactory;
	}

	@JMXOperation (name="displayInternalLog", description="Displays content of internal log.")
	public String displayInternalLog(){
		if(llAppender!=null){
			internalLog.info(llAppender.displayEvents());
			return llAppender.displayEvents();
		}
		return "";
	}		

	@JMXOperation (name="displayInitStackTrace", description="Displays the thread stack trace of init method for this collector.")
	public String displayInitStackTrace(){
		StringBuilder result = new StringBuilder("");
		if(initStackTrace!=null && initStackTrace.length>0){
			for(StackTraceElement element: initStackTrace){
				result.append(element.toString());
			}
		}
		return result.toString();
	}
	
	@JMXOperation (name="displayStartStackTrace", description="Displays the thread stack trace of start method for this collector.")
	public String displayStartStackTrace(){
		StringBuilder result = new StringBuilder("");
		if(startStackTrace!=null && startStackTrace.length>0){
			for(StackTraceElement element: startStackTrace){
				result.append(element.toString());
			}
		}
		return result.toString();
	}	

	@JMXOperation (name="displayLastCollectStackTrace", description="Displays the thread stack trace for last collect method for this collector.")
	public String displayLastCollectStackTrace(){
		StringBuilder result = new StringBuilder("");
		if(lastCollectStackTrace!=null && lastCollectStackTrace.length>0){
			for(StackTraceElement element: lastCollectStackTrace){
				result.append(element.toString());
			}
		}
		return result.toString();
	}	
	
	/**
	 * @return the current state of this collector
	 */
	@JMXOperation (name="currentState", description="Returns the current state of this collector.")
	public String currentState() {
		if(getState()==CollectorState.STARTED) return "Started";
        else if(getState()==CollectorState.COLLECTING) return "Collecting";
		else if(getState()==CollectorState.STOPPED) return "Stopped";
		else if(getState()==CollectorState.INITIALIZED) return "Initialized";
		else if(getState()==CollectorState.INITIALIZING) return "Initializing";
        else if(getState()==CollectorState.INIT_FAILED) return "Initialization Failed";
        else if(getState()==CollectorState.STARTING) return "Starting";
        else if(getState()==CollectorState.START_FAILED) return "Start Failed";
        else if(getState()==CollectorState.STOPPING) return "Stopping";
        else if(getState()==CollectorState.CONSTRUCTED) return "Constructed";
        else if(getState()==CollectorState.RESETTING) return "Resetting";
        else return "Unknown";
	}	
	
	public boolean isRunning() {
		boolean isRunning=true;
		if( 	getState() == CollectorState.STOPPED ||
				getState() == CollectorState.INIT_FAILED ||
				getState() == CollectorState.START_FAILED || 
				getState() == CollectorState.STOPPING ||
				getState() == CollectorState.NULL ){  
			isRunning=false;
		}
		return isRunning;
	}
	
	/**
	 * Formats one field.
	 * @param template
	 * @param data
	 * @return formatted field
	 */
	protected String formatField(final String template, String[] data) {
		String newTemplate = template;
		if(template==null) return "";
		if(template.startsWith("{") && template.endsWith("}")) {
			if(template.equalsIgnoreCase("{beanName}")) {
				return beanName;
			}
			Matcher m = namePattern.matcher(template);
			if(m.matches()) {
				m.group();
				try {
					int index = Integer.parseInt(m.group(1));
					newTemplate = data[index];
				} catch (Exception e) {
					newTemplate = "NameSpace Error";
				}				
			} else {
				newTemplate = "NameSpace Token Error";
			}
		}
		return newTemplate;
	}	
	
	/**
	 * Format segments of traces for this collector.
	 * @param String array of fragments
	 */
	public void formatSegments(String[] fragments) {
		if(fragments==null || fragments.length < 1) return;
		for(int i = 0; i < fragments.length; i++) {
			fragments[i] = formatField(fragments[i], null);
		}
	}	
	
	/**
	 * Add all String parameters passed to this method with a delimiter "|"
	 * @param variable number of String arguments
	 * @return combined string
	 */
	protected String format(String...tokens){
		StringBuilder sBuilder = new StringBuilder();
		for(int i=0;i<tokens.length;i++){
			sBuilder.append(tokens[i]+"|");
		}
		return sBuilder.toString();
	}
	
	/**
	 * Initializes resources used for logging the behavior of this collector
	 */
	public void initializeLogging(){
		log = Logger.getLogger(this.getClass());
		
		// Set internal appender for debugging purpose
		llAppender = new LinkedListAppender();
		internalLog = Logger.getLogger("internal.logger");
		internalLog.setLevel(Level.DEBUG); 
		internalLog.setAdditivity(false);  
		internalLog.addAppender(llAppender);  
	}
	
	/**
	 * Utility method for logging boundries of various events
	 * @param tokens
	 */
	public void logBanner(String...tokens){
		//log.debug("=================================================");
		StringBuilder sBuilder = new StringBuilder("");
		for(int i=0;i<tokens.length;i++){
			sBuilder.append(tokens[i]+" ");
		}
		log.debug(sBuilder.toString());
		//log.debug("=================================================");
	}	
	/**
	 * Applies pattern substitutions to the passed string for target properties from this MBean.
	 * @param name A value to be formatted.
	 * @return A formatted name.
	 */
	protected String formatName(String name) {
		if(name.contains("{THIS-PROPERTY")) {
			name = bindTokens(objectName, name, thisPattern);
		}
		if(name.contains("{THIS-DOMAIN")) {
			name = bindTokens(objectName, name, thisDomainPattern);
		}
		if(name.contains("{SEGMENT")) {
			name = bindTokens(objectName, name, segmentPattern);
		}				
		return name;
	}
	
	/**
	 * Applies pattern substitutions to the passed string for target properties from the target mbean.
	 * @param name A value to be formatted.
	 * @return A formatted name.
	 */
	protected String formatName(String name, ObjectName remoteMBean) {
		if(name.contains("{TARGET-PROPERTY")) {
			name = bindTokens(remoteMBean, name, targetPattern);
		}
		if(name.contains("{THIS-DOMAIN")) {
			name = bindTokens(objectName, name, targetDomainPattern);
		}				
		return name;
	}
	
	/**
	 * Takes the text passed and replaces tokens in accordance with the pattern 
	 * supplied taking the substitution vale from properties in the passed object name.
	 * @param targetObjectName The substitution values come from this object name.
	 * @param text The original text that will be substituted.
	 * @param p The pattern matcher to locate substitution tokens.
	 * @return The substituted string.
	 */
	public String bindTokens(ObjectName targetObjectName, String text, Pattern p) {
		Matcher matcher = p.matcher(text);
		String token = null;
		String property = null;
		String propertyValue = null;
		int pos = -1;
		while(matcher.find()) {
			token = matcher.group(0);
			property = matcher.group(1);
			propertyValue = targetObjectName.getKeyProperty(property);
            if(token.toUpperCase().contains("DOMAIN")) {
                pos = Integer.parseInt(property);
                propertyValue = targetObjectName.getDomain().split("\\.")[pos];
            } else if(token.toUpperCase().contains("SEGMENT")) {
            	pos = Integer.parseInt(property);
            	propertyValue = tracingNameSpace[pos];
            } else {
                propertyValue = targetObjectName.getKeyProperty(property);
            }			
			text = text.replace(token, propertyValue);
		}
		return text;
	}	

	/**
	 * Scheduled a frequency or a cron based task with HeliosScheduler
	 */
	protected void scheduleCollect(){
		if(hScheduler==null)
			hScheduler = HeliosScheduler.getInstance();		
		if(frequency <= 0L)
			log.trace("No frequency provided for collector bean: " + this.getBeanName());
		else {
			frequencyTaskHandle = hScheduler.scheduleAtFrequency(false, this, initialDelay, frequency, TimeUnit.MILLISECONDS);
		}

		if(schedule==null)
			log.trace("No schedule provided for collector bean: " + this.getBeanName());
		else {
			if(HeliosScheduler.isValidCron(schedule)){
				cronTaskHandle = hScheduler.scheduleWithCron(this, schedule);
			}else{
				log.error("Ignoring collect schedule as invalid cron is provide for collector bean: " + this.getBeanName());
			}
		}
	}

	protected void unScheduleCollect(){
		if(frequencyTaskHandle!=null){
			frequencyTaskHandle.cancel(true);
		}
		if(cronTaskHandle!=null){
			cronTaskHandle.cancel(true);
		}
	}
	
	/**
	 * Scheduled a frequency or a cron based task with HeliosScheduler
	 */
	protected void scheduleReset() throws Exception{
		if(hScheduler==null)
			hScheduler = HeliosScheduler.getInstance();	
		Object[] arguments = {new AtomicBoolean(true)};
		if(resetFrequency > 0L)
			frequencyResetTaskHandle = hScheduler.scheduleAtFrequency(false, new ObjectExecutionTask<AbstractCollector>(this,this.getClass().getMethod("setResetFlag",AtomicBoolean.class),arguments), initialDelay, resetFrequency, TimeUnit.MILLISECONDS);
		if(resetSchedule!=null){
			if(HeliosScheduler.isValidCron(resetSchedule)){
				cronResetTaskHandle = hScheduler.scheduleWithCron(new ObjectExecutionTask<AbstractCollector>(this,this.getClass().getMethod("setResetFlag",AtomicBoolean.class),arguments), resetSchedule);
			}else{
				log.error("Ignoring reset schedule as invalid cron is provide for collector bean: " + this.getBeanName());
			}
		}
	}	

	protected void unScheduleReset(){
		if(frequencyResetTaskHandle!=null){
			frequencyResetTaskHandle.cancel(true);
		}
		if(cronResetTaskHandle!=null){
			cronResetTaskHandle.cancel(true);
		}
	}		
	
	/**
	 * Implemented method for NamedTask interface to provide a more meaningful name for 
	 * Scheduled Tasks
	 */
	@JMXAttribute (name="Name", description="Name of this collector bean", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getName() {
		return getBeanName();
	}
	
	public void setResetFlag(AtomicBoolean flag){
		resetFlag = flag;
	}
	
	/**
	 * @return the resetSchedule
	 */
	@JMXAttribute (name="ResetSchedule", description="Cron schedule for this collector bean", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getResetSchedule() {
		return resetSchedule;
	}

	/**
	 * @param resetSchedule the resetSchedule to set
	 */
	public void setResetSchedule(String resetSchedule) {
		this.resetSchedule = resetSchedule;
	}

	/**
	 * @return the defaultAvailabilityLabel
	 */
	@JMXAttribute (name="DefaultAvailabilityLabel", description="Default Availability Label to be traced", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDefaultAvailabilityLabel() {
		return defaultAvailabilityLabel;
	}

	/**
	 * @param defaultAvailabilityLabel the defaultAvailabilityLabel to set
	 */
	public void setDefaultAvailabilityLabel(String defaultAvailabilityLabel) {
		this.defaultAvailabilityLabel = defaultAvailabilityLabel;
	}

	/**
	 * @return the failureThreshold
	 */
	@JMXAttribute (name="FailureThreshold", description="Number of failure before the collector slows down the schedule", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getFailureThreshold() {
		return failureThreshold;
	}

	/**
	 * @param failureThreshold the failureThreshold to set
	 */
	public void setFailureThreshold(int failureThreshold) {
		this.failureThreshold = failureThreshold;
	}

	/**
	 * @return the iterationsToSkip
	 */
	@JMXAttribute (name="IterationToSkip", description="Number of collect iterations to skip when running on alternate frequency", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getIterationsToSkip() {
		return iterationsToSkip;
	}

	/**
	 * @param iterationsToSkip the iterationsToSkip to set
	 */
	public void setIterationsToSkip(int iterationsToSkip) {
		this.iterationsToSkip = iterationsToSkip;
	}

	@JMXAttribute (name="InitialDelay", description="Number of Milliseconds to wait before the scheduler kicks in for this collector", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getInitialDelay() {
		return initialDelay;
	}

	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}
	
	/**
	 * @return the maxRestartAttempts
	 */
	@JMXAttribute (name="MaxRestartAttempts", description="Maximum restart attempts in case of failure during startup", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getMaxRestartAttempts() {
		return maxRestartAttempts;
	}

	/**
	 * @param maxRestartAttempts the maxRestartAttempts to set
	 */
	public void setMaxRestartAttempts(int maxRestartAttempts) {
		this.maxRestartAttempts = maxRestartAttempts;
	}

	/**
	 * @return the restartAttemptDelayFrequency
	 */
	@JMXAttribute (name="RestartAttemptDelayFrequency", description="Frequency at which restart attempts should be made", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRestartAttemptDelayFrequency() {
		return restartAttemptDelayFrequency;
	}

	/**
	 * @param restartAttemptDelayFrequency the restartAttemptDelayFrequency to set
	 */
	public void setRestartAttemptDelayFrequency(long restartAttemptDelayFrequency) {
		this.restartAttemptDelayFrequency = restartAttemptDelayFrequency;
	}

	/**
	 * Constructs a <code>StringBuilder</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	    StringBuilder retValue = new StringBuilder("");
	    retValue.append("AbstractCollector ( " + super.toString() + TAB);
	    retValue.append("immediateCollect = " + this.immediateCollect + TAB);
	    retValue.append("log = " + this.log + TAB);
	    retValue.append("llAppender = " + this.llAppender + TAB);
	    retValue.append("internalLog = " + this.internalLog + TAB);
	    retValue.append("logErrors = " + this.logErrors + TAB);
	    retValue.append("logCollectionResult = " + this.logCollectionResult + TAB);
	    retValue.append("frequency = " + this.frequency + TAB);
	    retValue.append("schedule = " + this.schedule + TAB);
	    retValue.append("initialDelay = " + this.initialDelay + TAB);
	    retValue.append("resetFrequency = " + this.resetFrequency + TAB);
	    retValue.append("resetSchedule = " + this.resetSchedule + TAB);
	    retValue.append("resetCount = " + this.resetCount + TAB);
	    retValue.append("resetFlag = " + this.resetFlag + TAB);
	    retValue.append("tracerFactory = " + this.tracerFactory + TAB);
	    retValue.append("tracer = " + this.tracer + TAB);
	    retValue.append("tracerName = " + this.tracerName + TAB);
	    retValue.append("objectName = " + this.objectName + TAB);
	    retValue.append("state = " + getState() + TAB);
	    retValue.append("lastTimeCollectionStarted = " + this.lastTimeCollectionStarted + TAB);
	    retValue.append("lastTimeCollectionCompleted = " + this.lastTimeCollectionCompleted + TAB);
	    retValue.append("lastTimeCollectionSucceeded = " + this.lastTimeCollectionSucceeded + TAB);
	    retValue.append("lastTimeCollectionFailed = " + this.lastTimeCollectionFailed + TAB);
	    retValue.append("beanName = " + this.beanName + TAB);
	    retValue.append("collectionResult = " + this.collectionResult + TAB);
	    retValue.append("collectorLock = " + this.collectorLock + TAB);
	    retValue.append("waitPeriodIfResetting = " + this.waitPeriodIfResetting + TAB);
	    retValue.append("totalCollectionCount = " + this.totalCollectionCount + TAB);
	    retValue.append("totalSuccessCount = " + this.totalSuccessCount + TAB);
	    retValue.append("totalFailureCount = " + this.totalFailureCount + TAB);
	    retValue.append("saveStackTraces = " + this.saveStackTraces + TAB);
	    retValue.append("server = " + this.server + TAB);
	    retValue.append("hScheduler = " + this.hScheduler + TAB);
	    retValue.append("frequencyTaskHandle = " + this.frequencyTaskHandle + TAB);
	    retValue.append("cronTaskHandle = " + this.cronTaskHandle + TAB);
	    retValue.append("frequencyResetTaskHandle = " + this.frequencyResetTaskHandle + TAB);
	    retValue.append("cronResetTaskHandle = " + this.cronResetTaskHandle + TAB);
	    retValue.append("defaultAvailabilityLabel = " + this.defaultAvailabilityLabel + TAB);
	    retValue.append("failureThreshold = " + this.failureThreshold + TAB);
	    retValue.append("consecutiveFailureCount = " + this.consecutiveFailureCount + TAB);
	    retValue.append("fallbackFrequencyActivated = " + this.fallbackFrequencyActivated + TAB);
	    retValue.append("actualSkipped = " + this.actualSkipped + TAB);
	    retValue.append("iterationsToSkip = " + this.iterationsToSkip + TAB);
	    retValue.append("restartAttempts = " + this.restartAttempts + TAB);
	    retValue.append("maxRestartAttempts = " + this.maxRestartAttempts + TAB);
	    retValue.append("restartAttemptDelayFrequency = " + this.restartAttemptDelayFrequency + TAB);
	    retValue.append("restartTaskHandle = " + this.restartTaskHandle + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}

	/**
	 * @return the startExceptionScript
	 */
	public ScriptBeanWrapper getStartExceptionScript() {
		return startExceptionScript;
	}

	/**
	 * @param startExceptionScript the startExceptionScript to set
	 */
	public void setStartExceptionScript(ScriptBeanWrapper startExceptionScript) {
		this.startExceptionScript = startExceptionScript;
	}

	/**
	 * @return the collectExceptionScript
	 */
	public ScriptBeanWrapper getCollectExceptionScript() {
		return collectExceptionScript;
	}

	/**
	 * @param collectExceptionScript the collectExceptionScript to set
	 */
	public void setCollectExceptionScript(ScriptBeanWrapper collectExceptionScript) {
		this.collectExceptionScript = collectExceptionScript;
	}

	/**
	 * @return the numberOfCollectorsRunning
	 */
	@JMXAttribute (name="NumberOfCollectorsRunning", description="Total number of collectors running at this time time in this JVM", mutability=AttributeMutabilityOption.READ_ONLY)
	public static AtomicInteger getNumberOfCollectorsRunning() {
		return numberOfCollectorsRunning;
	}
	
	/**
	 * @return cron expression that defines start of blackout period
	 */
	public String getBlackoutStart() {
		return blackoutStart;
	}

	/**
	 * @param blackoutStart - cron expression that defines start of blackout period
	 */
	public void setBlackoutStart(String blackoutStart) {
		this.blackoutStart = blackoutStart;
	}
	
	/**
	 * 
	 * @return cron expression that defines end of blackout period
	 */
	public String getBlackoutEnd() {
		return blackoutEnd;
	}
	
	/**
	 * 
	 * @param blackoutEnd - cron expression that defines end of blackout period
	 */
	public void setBlackoutEnd(String blackoutEnd) {
		this.blackoutEnd = blackoutEnd;
	}    
    
}
