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
package org.helios.ot.tracer;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.helpers.Banner;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.service.ServiceState;
import org.helios.jmxenabled.service.ServiceState.ServiceStateController;
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.helios.jmxenabled.threads.ExecutorMBeanPublisher;
import org.helios.jmxenabled.threads.TaskRejectionPolicy;
import org.helios.ot.endpoint.DefaultEndpoint;
import org.helios.ot.endpoint.IEndPoint;
import org.helios.ot.endpoint.InstrumentedEndPointWrapper;
import org.helios.ot.endpoint.LifecycleAwareIEndPoint;
import org.helios.ot.instrumentation.AgentJVMMonitor;
import org.helios.ot.instrumentation.InstrumentationProfile;
import org.helios.ot.subtracer.IntervalTracer;
import org.helios.ot.subtracer.PhaseTriggerTracer;
import org.helios.ot.subtracer.TemporalTracer;
import org.helios.ot.subtracer.UrgentTracer;
import org.helios.ot.subtracer.VirtualTracer;
import org.helios.ot.subtracer.pipeline.IPhaseTrigger;
import org.helios.ot.trace.interval.IntervalAccumulator;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.helios.ot.tracer.disruptor.TraceCollectionCloser;
import org.helios.time.SystemClock;

/**
 * <p>Title: TracerManager</p>
 * <p>Description: The primary ot-core engine that manages all threaded services in the core and is the accessor and factory for the main tracer.</p>
 * <p>The TracerManager is a "configurable singleton" meaning that:<ul>
 * 	<li>It is accessible globally and statically through {@code getInstance()} </li>
 * 	<li>When {@code getInstance()} is called, if the singleton instance does not exist, it will be created. </li>
 * 	<li>When {@code getInstance()} is called, if the singleton instance does exist, that instance will be returned.</li>
 * 	<li>When {@code getInstance()} is called, a {@code Configuration} instance is used to provide confuguration details for the singleton to be created.</li>
 * 	<li>When a {@code getInstance()} call results in the creation of the singleton, the {@code Configuration} uses {@link org.helios.helpers.ConfigurationHelper#getSystemThenEnvProperty} to acquire
 * system property or environment driven configuration, using defaults in the absence of said.</li>    
 * 	<li>The TracerManager configuration and {@code start()} are not publicly accessible. These are called by the {@code Configuration} instance.</li>
 * 	<li>In some scenarios, a DI container may want to apply a specific configuration to the TracerManager which can be done by configuring an instance of {@code Configuration} and calling {@code getInstance(Configuration)}.
 * 	If another caller has previously and eagerly called {@code getInstance()}, resulting in the default configuration, the container call will shut down the defaulot created instance and recreate a new instance with the 
 * container specified configuration.</li>
 * 	<li>In order to avoid stale references, this singleton swapout must maintain the existing references that may be held by other objects.</li>
 * 	<li>The {@code Configuration} class supports two sets of almost identical setters, one being the tradiitonal java bean style setters and the other a fluent builder style set of configurators.</li>
 * </ul>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.tracer.TracerManager3</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class TracerManager3 extends ManagedObjectDynamicMBean implements Runnable, ITracerManager, TraceCollectionCloser { 
	// ===================================
	// Static fields
	// ===================================		
	/** The singleton instance, initially unconfigured. */
	private static final TracerManager3 instance = new TracerManager3();
	
	/** The class name of the default endpoint in the event that no other endpoints are defined */
	public static final IEndPoint DEFAULT_ENDPOINT = new DefaultEndpoint();
	/** The state aware wrapper for the TracerManager */
	private static final StateAwareITracerManager iTracerManager = StateAwareITracerManager.getInstance();
	/** The tracer impl root */
	private static final TracerImpl tracerImpl = TracerImpl.getInstance(iTracerManager);
	/** Indicates if any interval tracers have been requested */
	private static final AtomicBoolean intervalTracerRequested = new AtomicBoolean(false);
	/** Static class logger */
	private static final Logger LOG = Logger.getLogger(TracerManager3.class);

	// ===================================
	// Instance fields
	// ===================================
	/** The current in-use configuration */
	protected final AtomicReference<Configuration> configuration = new AtomicReference<Configuration>(null);
	/** A set of registered state listeners */
	protected final Set<ITracerManagerListener> listeners = new CopyOnWriteArraySet<ITracerManagerListener>();
	/** The endpoint execution thread pool */
	protected volatile ThreadPoolExecutor executor = null;
	/** The state controller of the tracer manager */
	protected final ServiceStateController state = ServiceState.newController("TracerManager");
	/** The slot acquisition wait implementation */
	protected SlotWaitStrategy.ISlotWaitStrategy waitStrategyImpl = SlotWaitStrategy.DEFAULT.getStrategy();
	/** The slot queue */
	protected BlockingQueue<TraceCollection> slotQueue;
	/** The shared submission context passed to all TraceCollections to provide it the resources to submit. */
	protected SubmissionContext submissionContext;
	/** The slot acquisition wait time in ms. If a slot is not acquired in this time, the submission is dropped */
	protected long slotWaitTime = -1L;
	/** The configured instrumentation profile rolling counter size */
	protected int rollingCounterSize = Configuration.DEFAULT_ROLLING_COUNTER_SIZE;
	/** The interval accumulator service */
	protected volatile IntervalAccumulator intervalAccumulator = null; 
	/** An IntervalAccumulator builder which needs to be configured here */
	protected IntervalAccumulator.Builder iaBuilder = null;


	// ===================================
	// Public static constants
	// ===================================	
	/** A serial number to identify the generation of the executor */
	private static final AtomicLong executorGenerationSerial = new AtomicLong(0L);
	/** The ObjectName of the TracerManager */
	public static final ObjectName TM_ON = JMXHelper.objectName("org.helios.ot.tm:service=TracerManager");
	/** The ObjectName of the TM's endpoint executor */
	public static final ObjectName TM_EXECUTOR_ON = JMXHelper.objectName("org.helios.ot.tm:service=ThreadPool");
	
	
	private TracerManager3() {
		final TracerManager3 tm = this;
		Runtime.getRuntime().addShutdownHook(new Thread("TracerManagerShutdownHook"){
			public void run() {
				tm.shutdown();
			}
		});
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger.getLogger(DefaultEndpoint.class).setLevel(Level.INFO);
//		ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(true);
//		ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
		//EventHandlerAppender app = new EventHandlerAppender("TracerManager3.main");
		//Logger.getLogger(DefaultEndpoint.class).addAppender(app);
		LOG.info("TracerManager Test");
		TracerManager3 tm = null;
		Map<String, Long> dropCounts = new TreeMap<String, Long>(); 
		Map<String, Integer> queueDepth = new TreeMap<String, Integer>();
		Map<String, String> elapsedTime = new TreeMap<String, String>();
		Map<String, String> telapsedTime = new TreeMap<String, String>();
		Set<String> captures = new HashSet<String>();
		for(int x = 0; x < 10; x++) {
			int state = -1; 
			tm = TracerManager3.getInstance(
					Configuration.getDefaultConfiguration().endPointInstrumentation(InstrumentationProfile.PERF)
			);
			tm.getIntervalTracer();
			LOG.info("Running Iteration:" + x + " with starting drop count:" + StateAwareITracerManager.getInstance().getDropCount());
			SystemClock.startTimer();
			int i = 0;
			for(;i < 10000; i++) {
				if(i%1000==0) {
					tm.getTracer().startThreadInfoCapture();
					state = i+1000-1;
				}
				tm.getTracer().trace(i, "Bar", "Foo");
				if(i==state) {
					captures.add(tm.getTracer().endThreadInfoCapture("Metrics").toString());
				}
				
			}	
			assert i==10000;
			telapsedTime.put("" + x, SystemClock.lapTimer().toString());
			queueDepth.put("" + x, instance.executor.getQueue().size());
			while(!instance.executor.getQueue().isEmpty()) {
				//try { Thread.currentThread().join(5); } catch (Exception e) {}
				Thread.yield();
			}
			elapsedTime.put("" + x, SystemClock.endTimer().toString());
			dropCounts.put("" + x, StateAwareITracerManager.getInstance().getDropCount());
			//try { Thread.sleep(30000); } catch (Exception e) {}
			instance.shutdown();		
			
		}
		for(String tic: captures) {
			LOG.info(tic);
		}
		for(Map.Entry<String, Long> entry: dropCounts.entrySet()) {
			LOG.info("DropCount#" + entry.getKey() + ":" + entry.getValue() 
//					+ "\n\tTracing Elapsed:" + telapsedTime.get(entry.getKey())  
//					+ "\n\tWait For Queue Elapsed:" + elapsedTime.get(entry.getKey())  
//					+ "\n\tQueue Depth:" + queueDepth.get(entry.getKey())
			);
		}
	}

	/**
	 * Returns the TM instance [re-]configured with the passed configuration
	 * @param config A customized configuration
	 * @return the TM instance.
	 * TODO: Need to stop an existing instance if it is already configured.
	 */
	public static TracerManager3 getInstance(Configuration config) {
		instance.configuration.set(config);
		if(instance.state.getState().equals(ServiceState.STARTING)) {
			instance.state.waitForStates(ServiceState.STARTED);
			instance.shutdown();			
		}
		if(instance.state.getState().equals(ServiceState.STARTED)) {
			instance.shutdown();
		}
		return getInstance();
	}
	
	/**
	 * Returns the TracerManager. If an instance has not be configured yet, a default configuration will be used to configure one.
	 * @return the TracerManager
	 */
	public static TracerManager3 getInstance() {
		if(instance.state.getState().equals(ServiceState.STARTED)) {
			return instance;
		}
		if(instance.state.getState().equals(ServiceState.STARTING)) {
			ServiceState st = instance.state.waitForStates(ServiceState.STARTED, ServiceState.STARTFAILED);
			if(st==null) {
				throw new RuntimeException("TracerManager Failed to start on time. Retry.",  new Throwable());
			} else if(ServiceState.STARTFAILED.equals(st)) {				
				throw new RuntimeException("TracerManager Failed to start",  new Throwable());
			} else {
				return instance;
			}
		}
		if(instance.state.getState().equals(ServiceState.STARTFAILED)) {
			instance.shutdown();			
		}
		if(instance.state.getState().equals(ServiceState.STOPPING)) {
			instance.state.waitForStates(ServiceState.STOPPED);
		}
		
		if(instance.state.managedServiceStart(instance, 30000, TimeUnit.SECONDS)) {
			return instance;
		} 
		throw new RuntimeException("TracerManager Failed to start",  new Throwable());		
	}
	
	/**
	 * Asynch start task
	 */
	public void run() {
		Configuration config = instance.configuration.get();
		if(config==null) {
			config = Configuration.getDefaultConfiguration();
			instance.configuration.set(config);
		}
		config.build();
		slotWaitTime = config.slotWaitTime;
		instance.executor = config.executor;
		instance.slotQueue = config.slotQueue;
		instance.waitStrategyImpl = config.waitStrategyImpl;
		submissionContext = config.submissionContext;
		for(IEndPoint ep: submissionContext.endPoints) {
			if(ep instanceof LifecycleAwareIEndPoint) {
				((LifecycleAwareIEndPoint)ep).onTracerManagerStartup();
			}
		}
		rollingCounterSize = config.rollingCounterSize;
		iTracerManager.setTracerManager(instance);		
		iaBuilder = config.iaBuilder;
		if(intervalTracerRequested.get()) {
			intervalAccumulator = iaBuilder.build();
			IntervalTracer.setIntervalAccumulator(intervalAccumulator);
		}
//		registerDisruptorMBean();
		LOG.info(Banner.banner("TracerManager Started"));		
		instance.state.setStarted();	
		executor.execute(new Runnable(){
			public void run() {
				AgentJVMMonitor.getInstance();
			}
		});
	}
	
	/**
	 * Returns the TM state
	 * @return the TM state
	 */
	public ServiceState getState() {
		return state.getState();
	}
	
	

	/**
	 * Stops the TracerManager and all dependent resources.
	 */
	public void shutdown() {
		if(instance.state.getState().equals(ServiceState.STOPPED)) {
			return;
		}
		instance.state.setStopping();
		if(intervalAccumulator!=null) {
			IntervalTracer.setIntervalAccumulator(null);
			intervalAccumulator.stop();
			intervalAccumulator=null;			
		}
		StateAwareITracerManager.getInstance().setTracerManager(null);
		Configuration config = configuration.get();
		if(config==null) {
			LOG.warn("Shutdown found the TracerManager configuration null");			
		}
		if(instance.executor!=null) {
			instance.executor.shutdown();
			instance.executor = null;
		}
		for(IEndPoint ep: submissionContext.endPoints) {
			if(ep instanceof LifecycleAwareIEndPoint) {
				((LifecycleAwareIEndPoint)ep).onTracerManagerShutdown();
			}
		}
		submissionContext.endPoints.clear();
		submissionContext = null;
		slotQueue.clear();
		slotQueue = null;
		waitStrategyImpl = null;
		if(JMXHelper.getRuntimeHeliosMBeanServer().isRegistered(TM_EXECUTOR_ON)) {
			JMXHelper.getRuntimeHeliosMBeanServer().unregisterMBean(TM_EXECUTOR_ON);
		}		
		instance.state.setStopped();
		LOG.info(Banner.banner("TracerManager Stopped"));
	}
	
	/**
	 * Returns the next open TraceCollection slot from the ring buffer
	 * @return the next open TraceCollection
	 */
	public TraceCollection getNextTraceCollectionSlot() {
		if(slotQueue==null) throw new IllegalStateException("The TracerManager is not in a valid started state", new Throwable());
		TraceCollection tc = this.waitStrategyImpl
			.getSlot(slotQueue, slotWaitTime);
		//assert 1==2;
		return tc;
	}

	/**
	 * Submits a TraceCollection for processing
	 * @param tc the trace collection to process
	 */
	public void commit(TraceCollection tc) {
		if(tc!=null && tc!=StateAwareITracerManager.OFF_LINE_TC) executor.submit(tc);
	}	

	
	/**
	 * <p>Title: Configuration</p>
	 * <p>Description: A container class for managing the configuration of the TracerManager seperately from the instance itself.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.TracerManager.Configuration</code></p>
	 * TODO: sysprop read configs
	 */
	@SuppressWarnings("unchecked")
	public static class Configuration {
		/** The configured size of the TraceCollection slot queue. */
		protected  int slotQueueSize = DEFAULT_SLOTQUEUE_SIZE;
		/** The slot acquisition wait strategy, defaults to {@link SlotWaitStrategy#DEFAULT} */
		protected  SlotWaitStrategy waitStrategy = SlotWaitStrategy.DEFAULT;
		/** The slot acquisition wait implementation */
		protected  SlotWaitStrategy.ISlotWaitStrategy waitStrategyImpl = SlotWaitStrategy.DEFAULT.getStrategy();
		/** The slot acquisition wait time in ms. */
		protected long slotWaitTime = DEFAULT_SLOT_WAIT_TIME;
		/** The endpoint execution thread pool */
		protected ThreadPoolExecutor executor = null;
		/** The configured endpoints */
		protected final LinkedHashSet<IEndPoint> endPoints = new LinkedHashSet<IEndPoint>(); 
		/** Indicates if endpoints should be invoked by one thread each (true) or serially by one thread (false) per TraceCollection. Defaults to false */
		protected boolean multiThreadEndpoints = false; 
		/** The slot queue */
		protected BlockingQueue<TraceCollection> slotQueue;
		/** The shared submission context passed to all TraceCollections to provide it the resources to submit. */
		protected SubmissionContext submissionContext;
		/** The configured instrumentation profile */
		protected InstrumentationProfile instrumentationProfile = DEFAULT_INSTRUMENTATION;		
		/** The configured instrumentation profile rolling counter size */
		protected int rollingCounterSize = DEFAULT_ROLLING_COUNTER_SIZE;
		/** An IntervalAccumulator builder which needs to be configured here */
		protected IntervalAccumulator.Builder iaBuilder = IntervalAccumulator.getBuilder();
		
		
		
		/** The default size of the TraceCollection slot queue. Defaults to <b><code>number of cores * 10</code></b> */
		public static final int DEFAULT_SLOTQUEUE_SIZE = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() * 10;
		/** The default slot wait time in ms. */
		public static final long DEFAULT_SLOT_WAIT_TIME  = 1;		
		/** The default instrumentation profile */
		public static final InstrumentationProfile DEFAULT_INSTRUMENTATION = InstrumentationProfile.OFF;		
		/** The default rolling counter size for rolling instrumented endpoints */
		public static final int DEFAULT_ROLLING_COUNTER_SIZE  = 10;		
		
		
		/**
		 * Returns a default configuration
		 * @return a default configuration
		 */
		public static Configuration getDefaultConfiguration() {
			return new Configuration();
		}
		
		/**
		 * Creates a new empty Configuration
		 */
		public Configuration() {
		}
		
		/**
		 * Creates a new Configuration 
		 * @param slotQueueSize The size of the TraceCollection slot queue
		 */
		public Configuration(/** Add params for all configurable points */ int slotQueueSize) {
			this();
		}
		

		
		
		
		/**
		 * Builds the disruptor configuration.
		 * This method should be called after all the event handlers and exception handlers have been set.
		 * @return this Configuration
		 */
		
		//=============================================================
		//  We need a build/no-activate for testing
		//  so accordingly, when start() is called, 
		//  we need a way of determining if conf has been activated
		//  and if not, activate.
		//=============================================================
		
		public Configuration build() {
			ExecutorMBeanPublisher.terminate(TM_EXECUTOR_ON, true, JMXHelper.getHeliosMBeanServer().getDefaultDomain());
			executor = ExecutorBuilder.newBuilder()
//			.setCoreThreads(maxConsumerCount)
//			.setMaxThreads(totalConsumerCount+2)
			.setCoreThreads(slotQueueSize+1)
			.setMaxThreads(multiThreadEndpoints ? slotQueueSize*2 : slotQueueSize+1 )
			
			.setDaemonThreads(true)
			.setExecutorType(true)
			.setFairSubmissionQueue(false)
			.setPrestartThreads(slotQueueSize+1)
			.setTaskQueueSize(multiThreadEndpoints ? slotQueueSize*2 : slotQueueSize+1)
			.setThreadGroupName("OpenTrace.TracerManagerThreadPool.Gen#" + executorGenerationSerial.incrementAndGet())
			.setPoolObjectName(TM_EXECUTOR_ON)
			.setJmxDomains(JMXHelper.JMX_DOMAIN_DEFAULT)
			.build();	
			
			slotQueue = new ArrayBlockingQueue<TraceCollection>(slotQueueSize, false);
			if(endPoints.isEmpty()) {				
						if(instrumentationProfile==null || instrumentationProfile.equals(InstrumentationProfile.OFF)) {
							endPoints.add(
									new DefaultEndpoint()
							);
						} else {
							endPoints.add(
									new InstrumentedEndPointWrapper(new DefaultEndpoint(), instrumentationProfile, rollingCounterSize)	
							);
						}
			} else {
				if(instrumentationProfile!=null && !instrumentationProfile.equals(InstrumentationProfile.OFF)) {
					LinkedHashSet<IEndPoint> instrumentedEndPoints = new LinkedHashSet<IEndPoint>();
					for(IEndPoint ep: endPoints) {
						instrumentedEndPoints.add(
								new InstrumentedEndPointWrapper(ep, instrumentationProfile, rollingCounterSize)
						);
					}
					endPoints.clear();
					endPoints.addAll(instrumentedEndPoints);
				}
			}
			submissionContext = new SubmissionContext(endPoints, multiThreadEndpoints, executor, instance); 			
			for(int i = 0; i < slotQueueSize; i++) {
				TraceCollection tc = new TraceCollection();
				tc.setSubmissionContext(submissionContext);						
				slotQueue.add(tc);
			}
			return this;
		}
		
		
		/**
		 * Sets the slot queue size
		 * @param size the size to set the slot queue to
		 */
		public void setSlotQueueSize(int size) {
			if(size<1) throw new IllegalArgumentException("Invalid slot queue size [" + size + "]", new Throwable());
			this.slotQueueSize = size;
		}
		
		/**
		 * Sets the slot queue size
		 * @param size the size to set the slot queue to
		 * @return this Configuration
		 */
		public Configuration slotQueueSize(int size) {
			if(size<1) throw new IllegalArgumentException("Invalid slot queue size [" + size + "]", new Throwable());
			this.slotQueueSize = size;
			return this;
		}
		
		/**
		 * Sets the rolling counter size for instrumented endpoints
		 * @param size The size of the rolling counters to create
		 */
		public void setRollingCounterSize(int size) {
			rollingCounterSize = size;
		}
		
		/**
		 * Sets the rolling counter size for instrumented endpoints
		 * @param size The size of the rolling counters to create
		 * @return this configuration
		 */
		public Configuration rollingCounterSize(int size) {
			rollingCounterSize = size;
			return this;
		}
		
		
		/**
		 * Sets the amount of time the tracer manager will wait for a slot from the slot queue
		 * @param time The wait time in ms.
		 * @return this Configuration
		 */
		public Configuration slotWaitTime(long time) {
			this.slotWaitTime = time;
			return this;
		}
		
		/**
		 * Sets the amount of time the tracer manager will wait for a slot from the slot queue
		 * @param time The wait time in ms.
		 */
		public void setSlotWaitTime(long time) {
			this.slotWaitTime = time;
		}
		
		/**
		 * Sets the instrumentation profile to apply to the end points
		 * @param ip an instrumentation profile
		 */
		public void setEndPointInstrumentation(InstrumentationProfile ip) {
			instrumentationProfile = ip;
		}
		
		/**
		 * Sets the instrumentation profile to apply to the end points
		 * @param ip an instrumentation profile
		 */
		public void setEndPointInstrumentation(String ip) {
			instrumentationProfile = InstrumentationProfile.valueOf(ip);
		}
		

		/**
		 * Sets the instrumentation profile to apply to the end points
		 * @param ip an instrumentation profile
		 * @return this configuration
		 */		
		public Configuration endPointInstrumentation(InstrumentationProfile ip) {
			instrumentationProfile = ip;
			return this;
		}
		
		/**
		 * Sets the instrumentation profile to apply to the end points
		 * @param ip an instrumentation profile
		 * @return this configuration
		 */		
		public Configuration endPointInstrumentation(String ip) {
			instrumentationProfile = InstrumentationProfile.valueOf(ip);
			return this;
		}
		
		
		/**
		 * Sets the slot wait strategy
		 * @param waitStrategy the waitStrategy to set
		 */
		public void setSlotWaitStrategy(SlotWaitStrategy waitStrategy) {
			if(waitStrategy==null) throw new IllegalArgumentException("SlotWaitStrategy was null", new Throwable());
			this.waitStrategy = waitStrategy;
		}
		
		/**
		 * Sets the slot wait strategy by name
		 * @param waitStrategy the name of the waitStrategy to set
		 */
		public void setSlotWaitStrategy(String waitStrategy) {
			if(waitStrategy==null) throw new IllegalArgumentException("SlotWaitStrategy was null", new Throwable());
			this.waitStrategy = SlotWaitStrategy.valueOf(waitStrategy.trim().toUpperCase());
		}
		
		
		/**
		 * Sets the slot wait strategy
		 * @param waitStrategy the waitStrategy to set
		 * @return this Configuration
		 */
		public Configuration waitStrategy(SlotWaitStrategy waitStrategy) {
			if(waitStrategy==null) throw new IllegalArgumentException("SlotWaitStrategy was null", new Throwable());
			this.waitStrategy = waitStrategy;
			return this;
		}
		
		/**
		 * Sets the wait strategy by name
		 * @param waitStrategy the name of the slot waitStrategy to set
		 * @return this Configuration
		 */
		public Configuration waitStrategy(String waitStrategy) {
			if(waitStrategy==null) throw new IllegalArgumentException("SlotWaitStrategy was null", new Throwable());
			this.waitStrategy = SlotWaitStrategy.valueOf(waitStrategy.trim().toUpperCase());
			return this;
		}
		
		
		
		//===============================================================================
		//		IEndPoint Handler Configs
		//===============================================================================
		
		/**
		 * Adds an endPoint to the end of this configuration's end point stack
		 * @param endPoint the end point to append
		 * @return this configuration
		 */
		public Configuration appendEndPoint(IEndPoint endPoint) {
			if(endPoint==null) throw new IllegalArgumentException("IEndPoint was null", new Throwable());
			endPoints.add(endPoint);
			return this;
		}
		
		/**
		 * Adds an endPoint to the beginning of this configuration's end point stack
		 * @param endPoint the end point to insert
		 * @return this configuration
		 */
		public Configuration insertEndPoint(IEndPoint endPoint) {
			if(endPoint==null) throw new IllegalArgumentException("IEndPoint was null", new Throwable());
			endPoints.add(endPoint);
			return this;
		}
		
		/**
		 * Adds a collection of IEndPoint instances to this configuration's end point stack
		 * @param endPoints a collection of IEndPoints.
		 */
		public void setEndPoints(Collection<IEndPoint> endPoints) {
			if(endPoints==null) throw new IllegalArgumentException("IEndPoint Collection was null", new Throwable());
			this.endPoints.addAll(endPoints);
		}
		
		
		/**
		 * Sets the Interval Accumulator's flush period
		 * @param flushPeriod the Interval Accumulator's flush period in ms.
		 */
		public void setIntervalFlushPeriod(long flushPeriod) {
			iaBuilder.flushPeriod(flushPeriod);
		}
		
		/**
		 * Sets the Interval Accumulator's flush period
		 * @param flushPeriod the Interval Accumulator's flush period in ms.
		 * @return this configuration
		 */
		public Configuration intervalFlushPeriod(long flushPeriod) {
			iaBuilder.flushPeriod(flushPeriod);
			return this;
		}
		
		/**
		 * Sets the size of the Interval Accumulator's accumulation maps
		 * @param accumulatorMapSize the size of the Interval Accumulator's accumulation maps
		 */
		public void setAccumulatorMapSize(int accumulatorMapSize) {
			iaBuilder.accumulatorMapSize(accumulatorMapSize);
		}
		
		/**
		 * Sets the size of the Interval Accumulator's accumulation maps
		 * @param accumulatorMapSize the size of the Interval Accumulator's accumulation maps
		 * @return this Configuration
		 */
		public Configuration accumulatorMapSize(int accumulatorMapSize) {
			iaBuilder.accumulatorMapSize(accumulatorMapSize);			
			return this;
		}
		
		/**
		 * Sets the Interval Accumulator's task rejection policy
		 * @param policy the Interval Accumulator's task rejection policy
		 */
		public void setAccumulatorTaskRejectionPolicy(TaskRejectionPolicy policy) {
			iaBuilder.policy(policy);
		}
		
		/**
		 * Sets the Interval Accumulator's task rejection policy
		 * @param policy the Interval Accumulator's task rejection policy
		 * @return this Configuration
		 */
		public Configuration accumulatorTaskRejectionPolicy(TaskRejectionPolicy policy) {
			iaBuilder.policy(policy);
			return this;
		}
		
		/**
		 * Sets the Interval Accumulator's submission queue size
		 * @param queueSize the Interval Accumulator's submission queue size
		 */
		public void setAccumulatorQueueSize(int queueSize) {
			iaBuilder.queueSize(queueSize);
		}
		
		/**
		 * Sets the Interval Accumulator's submission queue size
		 * @param queueSize the Interval Accumulator's submission queue size
		 * @return this Configuration
		 */
		public Configuration accumulatorQueueSize(int queueSize) {
			iaBuilder.queueSize(queueSize);
			return this;
		}

		
		//===============================================================================
		
		
	}
	
	/**
	 * <p>Title: SubmissionContext</p>
	 * <p>Description: A container class for the costructs required to execute a TraceCollection submission</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.disruptor.TraceCollection.SubmissionContext</code></p>
	 */
	@JMXManagedObject(annotated=true, declared=false)
	public static class SubmissionContext {
		/** The IEndPoints to be called in the callable when this TraceCollection is submitted */
		protected final Set<IEndPoint> endPoints = new CopyOnWriteArraySet<IEndPoint>();
		/** Indicates if the endpoint invocations should be delegated out to multiple threads */
		protected boolean multiThreaded = false;
		/** The executor to use for multi-threaded endpoint execution */
		protected final ExecutorService executor;
		/** The closer to call once all endpoints have been executed */
		protected TraceCollectionCloser closer = null;
		
		
		/**
		 * Creates a new SubmissionContext
		 * @param multiThreaded Indicates if the endpoint invocations should be delegated out to multiple threads
		 * @param executor The executor to use for multi-threaded endpoint execution
		 * @param closer The closer to call once all endpoints have been executed
		 */
		public SubmissionContext(Set<IEndPoint> endPoints, boolean multiThreaded, ExecutorService executor, TraceCollectionCloser closer) {
			super();
			this.endPoints.addAll(endPoints);
			this.multiThreaded = multiThreaded;
			this.executor = executor;
			this.closer = closer;
		}

		/**
		 * Indicates if the endpoint exections should be delegated out to multiple threads
		 * @return the multiThreaded true if the endpoint exections should be delegated out to multiple threads, 
		 * false if they run serially in a single thread 
		 */
		public boolean isMultiThreaded() {			
			return multiThreaded;
		}
		
		/**
		 * Delegates call to {@link SubmissionContext#isMultiThreaded()}
		 * @return the multiThreaded true if the endpoint exections should be delegated out to multiple threads, 
		 * false if they run serially in a single thread
		 * @see  {@link SubmissionContext#isMultiThreaded()}
		 */
		@JMXAttribute(name="MultiThreaded", description="Indicates if the endpoint exections should be delegated out to multiple threads", mutability=AttributeMutabilityOption.READ_WRITE)
		public boolean getMultiThreaded() {			
			return multiThreaded;
		}
		

		/**
		 * Sets the multiThreaded
		 * @param multiThreaded true if the endpoint exections should be delegated out to multiple threads,
		 * false if they run serially in a single thread 
		 */
		public void setMultiThreaded(boolean multiThreaded) {
			this.multiThreaded = multiThreaded;
		}

		/**
		 * Returns the IEndPoints to be called in the callable when this TraceCollection is submitted
		 * @return a set of endpoints
		 */
		public Set<IEndPoint> getEndPoints() {
			return Collections.unmodifiableSet(endPoints);
		}
		
		/**
		 * Returns a string representation of the IEndPoints to be called in the callable when this TraceCollection is submitted
		 * @return a string array
		 */
		@JMXAttribute(name="EndPointNames", description="A string representation of the EndPoints to be called in the callable when this TraceCollection is submitted", mutability=AttributeMutabilityOption.READ_ONLY)
		public String[] getEndPointNames() {
			List<String> list = new ArrayList<String>(endPoints.size());
			for(IEndPoint endpoint: endPoints) {
				list.add(endpoint.toString());
			}
			return list.toArray(new String[list.size()]);
		}
		

		/**
		 * Returns the executor used to multi-thread endpoint executions
		 * @return an executor
		 */
		public ExecutorService getExecutor() {
			return executor;
		}

		/**
		 * The closer for a TraceCollection submission, executed after all endpoints have been executed for one TraceCollection submissio n
		 * @return the closer
		 */
		public TraceCollectionCloser getCloser() {
			return closer;
		}
		
		
	}



	
	/**
	 * Returns a virtual tracer 
	 * @param host The virtual tracer's host
	 * @param agent The virtual tracer's agent name
	 * @return a virtual tracer
	 */
	public VirtualTracer getVirtualTracer(String host, String agent) {
		return getTracer().getVirtualTracer(host, agent);
	}
	
	/**
	 * Returns a temporal tracer that sets all traces generated to be temporal.
	 * @return a temporal tracer.
	 */
	public TemporalTracer getTemporalTracer() {
		return getTracer().getTemporalTracer();
	}
	
	
	
	/**
	 * Creates an interval tracer 
	 * @return a interval tracer.
	 */
	public synchronized IntervalTracer getIntervalTracer() {
		// TODO:
		// Start the IntervalAccumulator if not started already
		intervalTracerRequested.set(true);
		if(intervalAccumulator==null) {
			intervalAccumulator = iaBuilder.build();
			IntervalTracer.setIntervalAccumulator(intervalAccumulator);
		}
		return IntervalTracer.getInstance(tracerImpl);
	}
	
	/**
	 * Creates an urgent tracer 
	 * @return a temporal tracer.
	 */
	public UrgentTracer getUrgentTracer() {
		return getTracer().getUrgentTracer();
	}
	
	/**
	 * Creates a phase trigger tracer
	 * @param triggers An array of phase triggers 
	 * @return a PhaseTriggerTracer 
	 */
	public PhaseTriggerTracer getPhaseTriggerTracer(IPhaseTrigger...triggers) {
		return getTracer().getPhaseTriggerTracer(triggers);
	}
	
	/**
	 * Acquires the single root tracer instance
	 * @return the root tracer
	 */
	@Override
	public TracerImpl getTracer() {
		return tracerImpl;
	}


	/**
	 * Callback from TraceCollection when processing has completed by all endpoints.
	 * @param traceCollection The closing trace collection
	 */
	@Override
	public void close(TraceCollection traceCollection) {
		traceCollection.reset();
		if(!slotQueue.offer(traceCollection)) {
			LOG.fatal("Failed to enqueue traceCollection back into slot queue");
		}
		
	}


	/**
	 * Callback when a trace collection starts processing
	 * @param traceCollection the trace collection
	 * @param args arbitrary parms
	 */
	@Override
	public void init(TraceCollection traceCollection, Object... args) {

		
	}	






}
