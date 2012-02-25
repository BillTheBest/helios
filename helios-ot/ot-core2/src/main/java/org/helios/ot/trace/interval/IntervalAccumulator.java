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
package org.helios.ot.trace.interval;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
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
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.helios.jmxenabled.threads.HeliosThreadPoolExecutorImpl;
import org.helios.jmxenabled.threads.TaskRejectionPolicy;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.trace.IntervalTrace;
import org.helios.ot.trace.MetricId;
import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.helios.ot.type.MetricType;
import org.helios.time.SystemClock;

/**
 * <p>Title: IntervalAccumulator</p>
 * <p>Description: Queues interval trace submissions and aggregates them until the end of a flush interval when the aggregated traces are flushed out.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.interval.IntervalAccumulator</code></p>
 */
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating a flush process has started", types={
                @JMXNotificationType(type=IntervalAccumulator.FLUSH_START_NOTIF)
        }),
        @JMXNotification(description="Notification indicating a flush process has ended", types={
                @JMXNotificationType(type=IntervalAccumulator.FLUSH_END_NOTIF)
        }),
        @JMXNotification(description="Notification indicating a requested interval metric has been flushed", types={
                @JMXNotificationType(type=IntervalAccumulator.FLUSH_METRIC)
        }),
        @JMXNotification(description="Notification indicating that traces were dropped during submission in the last interval", types={
                @JMXNotificationType(type=IntervalAccumulator.SUB_DROPS)
        })        
        
})
@JMXManagedObject(declared=true, annotated=false)
public class IntervalAccumulator extends ManagedObjectDynamicMBean implements IntervalProcessor {
	/**  */
	private static final long serialVersionUID = 3599220513795229447L;
	/** The singleton instance */
	protected static volatile IntervalAccumulator instance = null;
	/** The state aware interval processor */
	protected static final StateAwareIntervalProcessor intervalProcessor = new StateAwareIntervalProcessor(); 
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();	
	/** The queue size for each mod queue */
	protected int queueSize;
	/** The submission rejection policy in the event the queue is full */
	protected TaskRejectionPolicy policy;
	/** The flush period in ms. -1 means manual. */
	protected long flushPeriod;
	/** The submission queue reader batch size */
	protected int subQueueBatchSize;
	/** The accumulator map size */
	protected int accumulatorMapSize;
	/** The open trace mod */
	protected final int mod;
	

	
	
	/** The notification type for the start of a flush process */
	public static final String FLUSH_START_NOTIF =  "org.helios.ot.trace.interval.flush.start";
	/** The notification type for the end of a flush process */
	public static final String FLUSH_END_NOTIF = "org.helios.ot.trace.interval.flush.end";
	/** The notification type for the flush of a requested interval metric */
	public static final String FLUSH_METRIC = "org.helios.ot.trace.interval.flush.metric";
	/** The notification type for indicating traces were dropped during submission in the last interval */
	public static final String SUB_DROPS = "org.helios.ot.trace.interval.sub.drops";
	
	/** A set of IA listeners */
	protected static final Set<IntervalAccumulatorListener> iaListeners = new CopyOnWriteArraySet<IntervalAccumulatorListener>();
	
	/**
	 * Registers an IntervalAccumulatorListener
	 * @param listener the IntervalAccumulatorListener to register
	 */
	public static void addListener(IntervalAccumulatorListener listener) {
		if(listener!=null) {
			iaListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters an IntervalAccumulatorListener
	 * @param listener the IntervalAccumulatorListener to unregister
	 */
	public static void removeListener(IntervalAccumulatorListener listener) {
		if(listener!=null) {
			iaListeners.remove(listener);
		}
	}
	
	
	/** The started indicator */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	
	/** An array of accumulator hash maps, one for each mod */
	protected volatile AccumulatorSwitch accSwitch;	
	/** An array of submission queues, one for each mod */
	protected final BlockingQueue<Trace>[] submissionQueues;
	/** The submission queue processing thread pool */
	protected final HeliosThreadPoolExecutorImpl executor;
	/** The flush scheduler */
	protected final ScheduledThreadPoolExecutor scheduler;
	/** The flush schedule handle */
	protected ScheduledFuture<?> schedule;
	
	/** Flush listeners */
	protected final Set<FlushListener> flushListeners = new CopyOnWriteArraySet<FlushListener>();
	/** The flush processor that flushes the current interval */
	protected final FlushProcessor flushProcessor;
	/** A set of submission processors */
	protected final Set<SubmissionProcessor> submissionProcessors = new HashSet<SubmissionProcessor>();
	/** A map of subscribed metric names that have JMX notification requests */
	protected final Map<String, Integer> jmxSubscriptions = new ConcurrentHashMap<String, Integer>();
	/** Total submission drop counter */
	protected final AtomicLong dropCounter = new AtomicLong(0L);
	/** Interval submission drop counter */
	protected final AtomicLong intervalDropCounter = new AtomicLong(0L);
	/** Total offline submission counter */
	protected final AtomicLong offlineCounter = new AtomicLong(0L);
	
	/** Counter last reset time */
	protected final AtomicLong lastReset = new AtomicLong(System.currentTimeMillis());
	
	
	/** Instance Logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	
	/** The default flush period {@value DEFAULT_FLUSH_PERIOD} */
	public static final long DEFAULT_FLUSH_PERIOD = 15000;
	/** The default submission queue reader batch size {@value DEFAULT_SUBQ_BATCH_SIZE} */
	public static final int DEFAULT_SUBQ_BATCH_SIZE = 10;
	/** The default accumulator map size {@value DEFAULT_ACC_MAP_SIZE} */
	public static final int DEFAULT_ACC_MAP_SIZE = 512;
	
	/** The default flush queue size {@value DEFAULT_QUEUE_SIZE} */
	public static final int DEFAULT_QUEUE_SIZE = 1000;
	/** The default full queue rejection policy {@value DEFAULT_REJ_POLICY} */
	public static final TaskRejectionPolicy DEFAULT_REJ_POLICY = TaskRejectionPolicy.DISCARD;
	
	/** The system property or env-var that designates the flush period for this VM. */
	public static final String FLUSH_PERIOD_PROP = "org.helios.ot.flush";
	/** The system property or env-var that designates the flush queue size for this VM. */
	public static final String FLUSHQ_SIZE_PROP = "org.helios.ot.qsize";	
	/** The system property or env-var that designates the rejection policy for this VM. */
	public static final String REJ_POLICY_PROP = "org.helios.ot.policy";
	/** The system property or env-var that designates the submission queue reader batch size for this VM. */
	public static final String SUBQ_BATCH_SIZE_PROP = "org.helios.ot.qreader";
	/** The system property or env-var that designates the accumulator map size for this VM. */
	public static final String ACC_MAP_SIZE_PROP = "org.helios.ot.accmap";
	
	
	
	/** The interval accumulatpr ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.ot.trace.interval:service=Accumulator");
	/** The interval accumulator submission thread pool ObjectName */
	public static final ObjectName EXEC_OBJECT_NAME = JMXHelper.objectName("org.helios.ot.trace.interval:service=ThreadPool");
	/** The interval accumulator flush scheduler ObjectName */
	public static final ObjectName SCHED_OBJECT_NAME = JMXHelper.objectName("org.helios.ot.trace.interval:service=Scheduler");
	
	
	/** A serial number to identify the generation of the executor */
	private static final AtomicLong executorGenerationSerial = new AtomicLong(0L);
	
	/**
	 * <p>Title: StateAwareIntervalProcessor</p>
	 * <p>Description: </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.trace.interval.IntervalAccumulator.StateAwareIntervalProcessor</code></p>
	 */
	protected static class StateAwareIntervalProcessor implements IntervalProcessor {
		/** Total offline submission counter */
		protected final AtomicLong offlineCounter = new AtomicLong(0L);

		/**
		 * {@inheritDoc}
		 * @see org.helios.ot.trace.interval.IntervalProcessor#submit(org.helios.ot.trace.Trace)
		 */
		@Override
		public void submit(Trace trace) { 
			if(instance==null) {
				offlineCounter.incrementAndGet();
			} else {
				instance.submit(trace);
			}
		}
		
		/**
		 * Resets the offline submission count
		 */
		public void reset() {
			offlineCounter.set(0L);
		}
		
		/**
		 * Returns the offline submission count
		 * @return the offline submission count
		 */
		public long getOfflineSubmissionCount() {
			return offlineCounter.get();
		}
		
	}
	
	/**
	 * Creates a new IntervalAccumulator
	 * @param builder The configuring builder
	 */
	@SuppressWarnings("unchecked")
	private IntervalAccumulator(Builder builder) {
		super("The OpenTrace IntervalAccumulator");
		builder.configure(this);
		mod = MetricId.getMod();
		submissionQueues = new ArrayBlockingQueue[mod];
		for(int i = 0; i < mod; i++) {
			submissionQueues[i] = new ArrayBlockingQueue<Trace>(builder.queueSize);
		}
		
		executor = (HeliosThreadPoolExecutorImpl) ExecutorBuilder.newBuilder()
			.setCoreThreads(mod)
			.setMaxThreads(mod)
			.setDaemonThreads(true)
			.setExecutorType(true)
			.setFairSubmissionQueue(false)
			.setPolicy(policy)
			.setPrestartThreads(mod)
			.setTaskQueueSize(mod)
			.setThreadGroupName("OpenTrace.SubmissionProcessor.Gen#" + executorGenerationSerial.incrementAndGet())
			.setPoolObjectName(EXEC_OBJECT_NAME)
			.setJmxDomains("DefaultDomain")
			.build();
		
		scheduler = (ScheduledThreadPoolExecutor) ExecutorBuilder.newBuilder()
			.setCoreThreads(1)
			.setMaxThreads(1)
			.setDaemonThreads(true)
			.setExecutorType(false)
			.setFairSubmissionQueue(false)
			.setPolicy(policy)
			.setPrestartThreads(1)
			.setTaskQueueSize(1)
			.setThreadGroupName("OpenTrace.FlushScheduler")
			.setPoolObjectName(SCHED_OBJECT_NAME)  // EXEC_OBJECT_NAME,  SCHED_OBJECT_NAME 
			.setJmxDomains("DefaultDomain")
			.build();
		accSwitch = new AccumulatorSwitch(mod, accumulatorMapSize, executor.getThreadGroup());
		flushProcessor = new FlushProcessor(accSwitch, this);
		try {
			
			try {
				if(JMXHelper.getHeliosMBeanServer().isRegistered(OBJECT_NAME)) {
					if((Boolean) JMXHelper.getHeliosMBeanServer().getAttribute(OBJECT_NAME, "Started")) {
						JMXHelper.getHeliosMBeanServer().invoke(OBJECT_NAME, "stop", NO_ARGS, NO_STR_SIG);
					}
					JMXHelper.getHeliosMBeanServer().unregisterMBean(OBJECT_NAME);
				}
			} catch (Exception e) {}
			this.reflectObject(this);
			JMXHelper.getHeliosMBeanServer().registerMBean(this, OBJECT_NAME);
		} catch (Exception e) {
			log.warn("Failed to register the IntervalAccumulator mamagement interface. Continuing without");
		}
		log.info("Created IntervalAccumulator");
	}
	
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		//System.setProperty(MetricId.MOD_PROP, "10");
		Logger.getLogger(IntervalAccumulator.class).setLevel(Level.INFO);
		Logger.getLogger(SubmissionProcessor.class).setLevel(Level.INFO);
		Logger.getLogger(AccumulatorMapChannel.class).setLevel(Level.INFO);
		
		final IntervalAccumulator ia = IntervalAccumulator.getInstance();
		final ITracer tracer = TracerManager3.getInstance().getTracer();
		final Random r = new Random(System.nanoTime());
		
		//ia.start();
		for(int x = 0; x < 2; x++) {
			ia.submit(tracer.getInstance(MetricType.INT_AVG, r.nextInt(), "Foo", "Bar"));
			ia.submit(tracer.getInstance(MetricType.INT_AVG, r.nextInt(), "A", "B"));
			ia.submit(tracer.getInstance(MetricType.INT_AVG, r.nextInt(), "X", "Y"));
			for(int g = 0; g < 100; g++) {
				ia.submit(tracer.getInstance(MetricType.INT_AVG, r.nextInt(), "NmericCode" + g, "NumericCodes"));
			}
		}
		ia.registerFlushListener(new FlushListener(){
			public void onFlushEnd(long flushSerial) {
				new Thread() {
					public void run() {
						for(int x = 0; x < 2; x++) {
							ia.submit(tracer.getInstance(MetricType.INT_AVG, Math.abs(r.nextInt()), "Foo", "Bar"));
							ia.submit(tracer.getInstance(MetricType.INT_AVG, Math.abs(r.nextInt()), "A", "B"));
							ia.submit(tracer.getInstance(MetricType.INT_AVG, Math.abs(r.nextInt()), "X", "Y"));
							for(int g = 0; g < 10; g++) {
								ia.submit(tracer.getInstance(MetricType.INT_AVG, Math.abs(r.nextInt()), "NumericCode" + g, "NumericCodes"));
							}

						}						
					}
				}.start();
			}
			public void onFlushStart(long flushSerial) {
			}
		});
		
		//IntervalTracer tracer = new IntervalTracer(new TracerImpl(null), 0L);
//		for(int i = 0; i < 100; i++) {
//			for(int x = 0; x < 2; x++) {
//				ia.submit(tracer.getInstance(MetricType.INT_AVG, r.nextInt(), "Foo", "Bar"));
//				ia.submit(tracer.getInstance(MetricType.INT_AVG, r.nextInt(), "A", "B"));
//				ia.submit(tracer.getInstance(MetricType.INT_AVG, r.nextInt(), "X", "Y"));
//			}
//			try { Thread.currentThread().join(1000); } catch (Exception e) {}
//		}
		
		try { Thread.currentThread().join(); } catch (Exception e) {}
	}
	
	/**
	 * Returns the interval accumulator
	 * @return the interval accumulator
	 */
	public static IntervalAccumulator getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new IntervalAccumulator(new Builder());
					instance.start();					
				}
			}
		}
		return instance;
	}
	
	/**
	 * Acquires a new builder to builder a new interval accumulator from
	 * @return a new builder
	 */
	public static Builder getBuilder() {
		return new Builder();
	}
	
	/**
	 * Returns the interval accumulator
	 * @param builder The builder with the config for this new instance
	 * @return the interval accumulator
	 */
	private static IntervalAccumulator getInstance(Builder builder) {
		synchronized(lock) {
			if(instance!=null) {
				instance.stop();
			}
			instance = new IntervalAccumulator(builder);
			instance.start();			
		}
		return instance;
	}
	
	/**
	 * <p>Adds a notification listener, making a special JMXMetric cache entry if the filter indicates a interval metric subscription.
	 * <p>In order to register a subscription for a flushed interval metric, the following requirements apply: <ol>
	 * 	<li>The notification filter must be an instance of {@link javax.management.NotificationFilterSupport}</li>
	 * 	<li>The metric name should be appended to the {@value org.helios.ot.trace.interval.IntervalAccumulator#FLUSH_METRIC} JMX notification type, with a delimeting <b><code>.</code></b>.</li>
	 * 	<li></li>
	 * </ol>
	 * <p>Example:
	 * <pre>
	 *		NotificationFilterSupport myFilter = new NotificationFilterSupport();
 	 *		myFilter.enableType(IntervalAccumulator.FLUSH_METRIC + "." + "myhost/myagent/os/cpu1/usage");
 	 *		myFilter.enableType(IntervalAccumulator.FLUSH_METRIC + "." + "myhost/myagent/os/cpu2/usage");
  	 *		IntervalAccumulator.getInstance().addNotificationListener(myListener, myFilter, null);
	 * </pre>
	 * <p>The same rules apply to removing listeners
	 * <p>Wildcards will supported in the future.
	 * @param listener The listener to callback against
	 * @param filter The notification filter
	 * @param handback The handback object
	 */
	@Override 
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		super.addNotificationListener(listener, filter, handback);
		if(filter!=null && (filter instanceof NotificationFilterSupport)) {
			NotificationFilterSupport nfs = (NotificationFilterSupport)filter;		
			for(String enabled: nfs.getEnabledTypes()) {
				if(enabled.startsWith(FLUSH_METRIC) && enabled.length()>FLUSH_METRIC.length()+1) {
					String metricName = enabled.replace(FLUSH_METRIC + ".", "").trim();
					Integer subscriberCount = jmxSubscriptions.get(metricName);
					if(subscriberCount==null) {
						synchronized(jmxSubscriptions) {
							subscriberCount = jmxSubscriptions.get(metricName);
							if(subscriberCount==null) {
								jmxSubscriptions.put(metricName, 1);
							}
						}
					} else {
						jmxSubscriptions.put(metricName, subscriberCount+1);
					}
				}
			}
		}
	}
	
	/**
	 * Registers a notification listener for flushed interval metrics for the passed metric name.
	 * The created notification filter will be returned to the listener in the handback.
	 * @param listener The listener to register
	 * @param metricId The metric name to listen for
	 */
	public void addFlushedTraceListener(NotificationListener listener, String metricId) {
		 NotificationFilterSupport metricFilter = new NotificationFilterSupport();
	 	 metricFilter.enableType(IntervalAccumulator.FLUSH_METRIC + "." + metricId);	 	 
	  	 addNotificationListener(listener, metricFilter, metricFilter);		
	}
	
	/**
	 * Removes a notification listener.
	 * Removing a flushed interval metric notification has some special requirements outlined in {@link org.helios.ot.trace.interval.IntervalAccumulator.addNotificationListener}
	 * @param listener The listener subscribed 
	 * @param filter the notification filter
	 * @param handback The handback object
	 */
	@Override 
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		super.addNotificationListener(listener, filter, handback);
		if(filter!=null && (filter instanceof NotificationFilterSupport)) {
			NotificationFilterSupport nfs = (NotificationFilterSupport)filter;		
			for(String enabled: nfs.getEnabledTypes()) {
				if(enabled.startsWith(FLUSH_METRIC) && enabled.length()>FLUSH_METRIC.length()+1) {
					String metricName = enabled.replace(FLUSH_METRIC + ".", "").trim();
					if(jmxSubscriptions.containsKey(metricName)) {
						synchronized(jmxSubscriptions) {
							Integer subscriberCount = jmxSubscriptions.get(metricName);
							if(subscriberCount==null || subscriberCount<2) {
								jmxSubscriptions.remove(metricName);
							} else {
								subscriberCount--;
								jmxSubscriptions.put(metricName, subscriberCount);
							}
						}
					}
				}
			}
		}
	}
	
	
	
	/**
	 * Registers a flush listener
	 * @param listener
	 */
	public void registerFlushListener(FlushListener listener) {
		if(listener!=null) {
			flushListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a flush listener
	 * @param listener
	 */
	public void unRegisterFlushListener(FlushListener listener) {
		if(listener!=null) {
			flushListeners.remove(listener);
		}
	}
	
	/**
	 * Returns the number of flush signal listeners
	 * @return the number of flush signal listeners
	 */
	@JMXAttribute(name="FlushListenerCount", description="The number of flush signal listeners", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getFlushListenerCount() {
		return flushListeners.size();
	}
	
	/**
	 * Fires a flush start callback to all flush listeners
	 * @param flushSerial The serial number of the flush
	 */
	void fireFlushSignalStart(final long flushSerial) {
		Notification notif = new Notification(FLUSH_START_NOTIF, OBJECT_NAME, nextNotificationSequence(), SystemClock.time(), "FlushSignal#" + flushSerial);
		notif.setUserData(flushSerial);
		sendNotification(notif);
		defaultThreadPool.execute(new Runnable(){
			public void run() {
				for(FlushListener listener: flushListeners) {
					listener.onFlushStart(flushSerial);
				}
			}
		});
	}
	
	/**
	 * Fires a flush end callback to all flush listeners
	 * @param flushSerial The serial number of the flush
	 */
	void fireFlushSignalEnd(final long flushSerial) {
		long drops = intervalDropCounter.get();
		intervalDropCounter.set(0L);
		if(drops>0) {
			Notification notif = new Notification(SUB_DROPS, OBJECT_NAME, nextNotificationSequence(), SystemClock.time(), "Interval Submission Drops in Flush#" + flushSerial + ": " + drops);
			notif.setUserData(drops);
			sendNotification(notif);					
		}
		Notification notif = new Notification(FLUSH_END_NOTIF, OBJECT_NAME, nextNotificationSequence(), SystemClock.time(), "FlushSignal#" + flushSerial);
		notif.setUserData(flushSerial);
		sendNotification(notif);		
		defaultThreadPool.execute(new Runnable(){
			public void run() {
				for(FlushListener listener: flushListeners) {
					listener.onFlushEnd(flushSerial);
				}				
			}
		});
	}
	
	
	/**
	 * Fires a flushed interval JMX notification to subscribed listeners
	 * @param intervalTrace The interval trace that a listener might be interested in
	 */
	public void fireFlushIntervalTraceEvent(final IntervalTrace intervalTrace) {
		if(intervalTrace==null) throw new IllegalArgumentException("Passed interval trace was null", new Throwable());
		if(!jmxSubscriptions.containsKey(intervalTrace.getFQN())) return; 

		Notification notif = new Notification(FLUSH_METRIC + "." + intervalTrace.getFQN(), OBJECT_NAME, nextNotificationSequence(), SystemClock.time(), intervalTrace.toString());
		notif.setUserData(intervalTrace);
		sendNotification(notif);		
	}
	
	
	/**
	 * Starts all the accumulator threads
	 */
	@JMXOperation(name="start", description="Starts the interval accumulator")
	private void start() {
		if(!started.get()) {
			for(int i = 0; i < mod; i++) {			
				SubmissionProcessor processor = new SubmissionProcessor(i, subQueueBatchSize, submissionQueues[i], accSwitch); 
				executor.submit(processor);
				submissionProcessors.add(processor);
			}
			if(flushPeriod>1) {
				schedule = scheduler.scheduleAtFixedRate(flushProcessor, flushPeriod - (System.currentTimeMillis()-ManagementFactory.getRuntimeMXBean().getStartTime()), flushPeriod, TimeUnit.MILLISECONDS);
			}
//			if(!TracerManager.getInstance().isConfigured()) {
//				TracerManager.getInstance().getConfigurator().configure();
//			}
			started.set(true);
			for(IntervalAccumulatorListener listener: iaListeners) {
				listener.onIntervalAccumulatorStart(getInstance());
			}
			log.info("\n\t==========================\n\tIntervalAccumulator started.\n\tFlush Period:" + flushPeriod + " ms.\n\tOT Mod:" + mod + "\n\t==========================\n");
		} else {
			log.warn("Start called after IA was started", new Throwable());
		}
	}
	
	/**
	 * Stops all the accumulator threads
	 */
	@JMXOperation(name="stop", description="Stops the interval accumulator")
	public void stop() {		
		if(started.get()) {
			log.info("\n\t==========================\n\tStopping IntervalAccumulator\n\t==========================\n");
			for(IntervalAccumulatorListener listener: iaListeners) {
				listener.onIntervalAccumulatorStop();
			}
			if(schedule!=null) {
				schedule.cancel(true);				
			}
			
			int tasksRunning = 0;
			try { 
				tasksRunning = scheduler.shutdownNow().size();
				log.info("Stopped IntervalAccumulator Scheduler with [" + tasksRunning + "] tasks still in flight.");
			} catch (Exception e) {}
			for(SubmissionProcessor processor: submissionProcessors) {
				processor.stopProcessor();
			}
			submissionProcessors.clear();
			tasksRunning = 0;
			try { 
//				executor.shutdown();
				tasksRunning = executor.shutdownNow().size();
				log.info("Stopped IntervalAccumulator ThreadPool with [" + tasksRunning + "] tasks still in flight.");
			} catch (Exception e) {}
			//TracerManager3.getInstance().shutdown();
			try { JMXHelper.getHeliosMBeanServer().unregisterMBean(EXEC_OBJECT_NAME); } catch (Exception e) {}
			try { JMXHelper.getHeliosMBeanServer().unregisterMBean(SCHED_OBJECT_NAME); } catch (Exception e) {}
			try { JMXHelper.getHeliosMBeanServer().unregisterMBean(OBJECT_NAME); } catch (Exception e) {}
			instance=null;
			log.info("\n\t==========================\n\tStopped IntervalAccumulator\n\t==========================\n");
		}
	}
	
	/**
	 * Manually flushes the current interval.
	 * Throws a RuntimeException if the flush period is set to more than 0.
	 */
	@JMXOperation(name="flush", description="Manually flushes the current interval")
	public void flush() {
		if(flushPeriod>0) {
			throw new RuntimeException("Manual flush operation prohibited. Flushes are in automatic mode every [" + flushPeriod + "] ms.", new Throwable());
		}
		flushProcessor.run();
	}
	
	/**
	 * Submits a trace to be accumulated
	 * @param trace the trace to submit
	 */
	@JMXOperation(name="submit", description="Submits a trace to be accumulated")
	public void submit(@JMXParameter(name="trace", description="The trace to submit") Trace trace) {
		if(trace==null) return;
		if(log.isTraceEnabled()) log.trace("Submitting Trace [" + trace.getFQN() + "][" + trace.getMod() + "]");
		if(submissionQueues[trace.getMod()].offer(trace)) {
			if(trace.hasAnyPhaseTriggers() && trace.hasTriggersFor(Phase.SUBQ)) {
				trace.runPhaseTriggers(Phase.SUBQ);
			}
		} else {
			intervalDropCounter.incrementAndGet();
			dropCounter.incrementAndGet();
		}
		
	}
	
	/**
	 * Indicates if the interval accumulator is started
	 * @return true if the interval accumulator is started
	 */
	@JMXAttribute(name="Started", description="Indicates if the interval accumulator is started", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getStarted() {
		return started.get();
	}
	
	
	/**
	 * Returns the submission queue rejection policy
	 * @return the submission queue rejection policy
	 */
	@JMXAttribute(name="SubmissionRejectionPolicy", description="The submission queue rejection policy", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getSubmissionRejectionPolicy() {
		return policy.name();
	}
	
	
	/**
	 * Returns the submission queue size
	 * @return the submission queue size
	 */
	@JMXAttribute(name="SubmissionQueueSize", description="The submission queue size", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getSubmissionQueueSize() {
		return queueSize;
	}

	/**
	 * Returns the submission queue reader batch size
	 * @return the submission queue reader batch size
	 */
	@JMXAttribute(name="SubQReaderBatchSize", description="The the submission queue reader batch size", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getSubQReaderBatchSize() {
		return subQueueBatchSize;
	}
	

	/**
	 * Returns the accumulator map size
	 * @return the accumulator map size
	 */
	@JMXAttribute(name="AccumulatorMapSize", description="The accumulator map size", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getAccumulatorMapSize() {
		return accumulatorMapSize;
	}
	
	/**
	 * Clears all registered interval traces from the accumulator map channel
	 */
	@JMXOperation(name="resetMapChannel", description="Clears all registered interval traces from the accumulator map channel")
	public void resetMapChannel() {
		accSwitch.resetMapChannel();
	}
	
	
	/**
	 * Returns the number of offline submissions
	 * @return the number of offline submissions
	 */
	@JMXAttribute(name="OfflineSubmissionCount", description="The number of offline submissions", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getOfflineSubmissionCount() {
		return intervalProcessor.getOfflineSubmissionCount();
	}
	
	/**
	 * Returns the accumulator flush period (ms.)
	 * @return the accumulator flush period
	 */
	@JMXAttribute(name="FlushPeriod", description="The accumulator flush period (ms.)", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFlushPeriod() {
		return flushPeriod;
	}
	
	/**
	 * Returns the configured OpenTrace mod
	 * @return the configured OpenTrace mod
	 */
	@JMXAttribute(name="OpenTraceMod", description="The configured OpenTrace mod", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getOpenTraceMod() {
		return mod;
	}
	
	/**
	 * Returns the total number of traces queued for submission across all mod submission queues
	 * @return the total number of traces queued 
	 */
	@JMXAttribute(name="SubmissionQueueDepth", description="The total number of traces queued for submission across all mod submission queues", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSubmissionQueueDepth() {
		long total = 0;
		for(BlockingQueue<Trace> q: submissionQueues) {
			total += q.size();
		}
		return total;
	}
	
	/**
	 * Resets the IntervalAccumulator stats.
	 */
	@JMXOperation(name="reset", description="Resets the IntervalAccumulator stats")
	public void reset() {
		dropCounter.set(0L);
		intervalProcessor.reset();
		lastReset.set(System.currentTimeMillis());
	}
	
	/**
	 * Returns the total number of submission drops in the last interval
	 * @return the total number of submission drops in the last interval
	 */
	@JMXAttribute(name="IntervalDropCount", description="The total number of submission drops in the last interval", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getIntervalDropCount() {
		return intervalDropCounter.get();
	}
	
	/**
	 * Returns the total number of submission drops since the last reset
	 * @return the total number of submission drops since the last reset
	 */
	@JMXAttribute(name="SubmissionDropCount", description="The total number of submission drops since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSubmissionDropCount() {
		return dropCounter.get();
	}
	
	/**
	 * Returns the UTC timestamp of the last reset
	 * @return the UTC timestamp of the last reset
	 */
	@JMXAttribute(name="LastResetTime", description="The UTC timestamp of the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastResetTime() {
		return lastReset.get();
	}
	
	/**
	 * Returns the Date of the last reset
	 * @return the Date of the last reset
	 */
	@JMXAttribute(name="LastResetDate", description="The Date of the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getLastResetDate() {
		return new Date(lastReset.get());
	}
	

	
	/**
	 * Returns an array of the submission queue depths for all mod submission queues
	 * @return an array of the submission queue depths for all mod submission queues
	 */
	@JMXAttribute(name="SubmissionQueueDepths", description="The submission queue depths for all mod submission queues", mutability=AttributeMutabilityOption.READ_ONLY)
	public int[] getSubmissionQueueDepths() {
		int[] depths = new int[mod];
		for(int i = 0; i < mod; i++) {
			depths[i] = submissionQueues[i].size();
		}
		return depths;
	}
	
	

	/**
	 * <p>Title: Builder</p>
	 * <p>Description: Builder class for the IntervalAccumulator</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.trace.interval.IntervalAccumulator.Builder</code></p>
	 */
	public static class Builder {
		/** The queue size for each mod queue */
		private int queueSize = ConfigurationHelper.getIntSystemThenEnvProperty(FLUSHQ_SIZE_PROP, DEFAULT_QUEUE_SIZE);
		/** The submission rejection policy in the event the queue is full */
		private TaskRejectionPolicy policy;
		/** The flush period in ms. -1 means manual. */
		private long flushPeriod = ConfigurationHelper.getLongSystemThenEnvProperty(FLUSH_PERIOD_PROP, DEFAULT_FLUSH_PERIOD);
		/** The submission queue reader batch size */
		protected int subQueueBatchSize = ConfigurationHelper.getIntSystemThenEnvProperty(SUBQ_BATCH_SIZE_PROP, DEFAULT_SUBQ_BATCH_SIZE);
		/** The accumulator map size */
		protected int accumulatorMapSize = ConfigurationHelper.getIntSystemThenEnvProperty(ACC_MAP_SIZE_PROP, DEFAULT_ACC_MAP_SIZE);
		
		
		private Builder() {
			String tmp = ConfigurationHelper.getSystemThenEnvProperty(REJ_POLICY_PROP, DEFAULT_REJ_POLICY.name());
			try {
				policy = TaskRejectionPolicy.valueOf(tmp.toUpperCase().trim());
			} catch (Exception e) {
				policy = DEFAULT_REJ_POLICY;
			}
		}

		/**
		 * Creates a new IntervalAccumulator, destroying the old one if it exists
		 * @param queueSize The queue size for each mod queue
		 * @param policy The submission rejection policy in the event the queue is full
		 * @param flushPeriod The flush period in ms. -1 means manual.
		 * @return the IntervalAccumulator singleton 
		 */
		public static IntervalAccumulator build(int queueSize, TaskRejectionPolicy policy, long flushPeriod) {
			return new Builder().queueSize(queueSize).policy(policy).flushPeriod(flushPeriod).build();
		}

		/**
		 * Configures the passed IntervalAccumulator
		 * @param ia an IntervalAccumulator to configure
		 */
		void configure(IntervalAccumulator ia) {
			ia.queueSize = queueSize;
			ia.policy = policy;
			ia.flushPeriod = flushPeriod;
			ia.subQueueBatchSize = subQueueBatchSize;
			ia.accumulatorMapSize = accumulatorMapSize; 
		}
		
		/**
		 * Builds a new IntervalAccumulator, destroying the old one if one exists
		 * @return the IntervalAccumulator singleton 
		 */
		public IntervalAccumulator build() {
			return IntervalAccumulator.getInstance(this);
		}
		
		
		/**
		 * Sets the queue size for each mod queue
		 * @param queueSize the queueSize to set
		 */
		public void setQueueSize(int queueSize) {
			this.queueSize = queueSize;
		}
		
		/**
		 * Sets the submission rejection policy in the event the queue is full
		 * @param policy the policy to set
		 */
		public void setPolicy(TaskRejectionPolicy policy) {
			this.policy = policy;
		}
		
		/**
		 * Sets the flush period in ms. -1 means manual.
		 * @param flushPeriod the flushPeriod to set
		 */
		public void setFlushPeriod(long flushPeriod) {
			this.flushPeriod = flushPeriod;
		}
		
		/**
		 * Sets the queue size for each mod queue
		 * @param queueSize the queueSize to set
		 * @return this builder
		 */
		public Builder queueSize(int queueSize) {
			this.queueSize = queueSize;
			return this;
		}
		
		/**
		 * Sets the submission rejection policy in the event the queue is full
		 * @param policy the policy to set
		 * @return this builder
		 */
		public Builder policy(TaskRejectionPolicy policy) {
			this.policy = policy;
			return this;
		}
		
		/**
		 * Sets the flush period in ms. -1 means manual.
		 * @param flushPeriod the flushPeriod to set
		 * @return this builder
		 */
		public Builder flushPeriod(long flushPeriod) {
			this.flushPeriod = flushPeriod;
			return this;
		}

		/**
		 * The submission queue reader batch size
		 * @param subQueueBatchSize the subQueueBatchSize to set
		 */
		public void setSubQueueBatchSize(int subQueueBatchSize) {
			this.subQueueBatchSize = subQueueBatchSize;
		}
		
		/**
		 * The submission queue reader batch size
		 * @param subQueueBatchSize the subQueueBatchSize to set
		 * @return this builder
		 */
		public Builder subQueueBatchSize(int subQueueBatchSize) {
			this.subQueueBatchSize = subQueueBatchSize;
			return this;
		}
		

		/**
		 * The accumulator map size
		 * @param accumulatorMapSize the accumulatorMapSize to set
		 */
		public void setAccumulatorMapSize(int accumulatorMapSize) {
			this.accumulatorMapSize = accumulatorMapSize;
		}
		
		/**
		 * The accumulator map size
		 * @param accumulatorMapSize the accumulatorMapSize to set
		 * @return this builder
		 */
		public Builder accumulatorMapSize(int accumulatorMapSize) {
			this.accumulatorMapSize = accumulatorMapSize;
			return this;
		}
	}
	
}
