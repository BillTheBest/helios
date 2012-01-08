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
package org.helios.tracing.interval.accumulator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.helios.patterns.queues.LongBitMaskFactory;
import org.helios.patterns.queues.LongBitMaskFactory.LongBitMaskSequence;
import org.helios.tracing.AbstractTracerInstanceFactory;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.MetricId;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: AccumulatorManager</p>
 * <p>Description: The managing container for a set of accumulators</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.accumulator.AccumulatorManager</code></p>
 */

public class AccumulatorManager implements Runnable {
	/** The singleton instance */
	private static AtomicReference<AccumulatorManager> instance = new AtomicReference<AccumulatorManager>(null);
	/** The singleton ctor lock */
	private static final Object lock = new Object();
	/** The stack of accumulators */
	protected Map<Integer, IAccumulator> accumulators;
	/** The accumulator context passed into each accumulator in the accumulator group. */
	protected AccumulatorContext accumulatorContext = null;
	/** The submission queue . */
	protected BlockingQueue<IIntervalTrace> submissionQueue;
	/** The started indicator */
	protected final AtomicBoolean started = new AtomicBoolean(false);	
	/** The flush indicator */
	protected final AtomicBoolean doFlush = new AtomicBoolean(false);
	/** The post flush latch */
	protected final AtomicReference<CountDownLatch> postFlushLatch = new AtomicReference<CountDownLatch>(null);
	/** The accumulator manager thread scheduler */
	protected ScheduledThreadPoolExecutor scheduler = null;
	/** instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** A map of registered tracer factories keyed by factoryId */
	protected final Map<String, AbstractTracerInstanceFactory> tracerFactories = new ConcurrentHashMap<String, AbstractTracerInstanceFactory>(LongBitMaskSequence.MAX_KEYS, 0.75f);
	
	//===================================================
	//	Configuration Objects
	//===================================================
	/** Configuration for the individual accumulators */
	protected AccumulatorConfiguration accumulatorConfig = new AccumulatorConfiguration();
	/** Configuration accumulator manager */
	protected AccumulatorManagerConfiguration accumulatorMgrConfig = new AccumulatorManagerConfiguration();
	//===================================================
	
	/**
	 * Private ctor. 
	 */
	private AccumulatorManager(boolean start) {
		if(start) start();
	}
	
	/**
	 * Acquires the singleton instance of the AccumulatorManager
	 * @return the AccumulatorManager
	 */
	public static AtomicReference<AccumulatorManager> getInstance() {
		if(instance.get()==null) {
			synchronized(lock) {
				if(instance.get()==null) {
					instance.set(new AccumulatorManager(true));
				}
			}
		}
		return instance;
	}
	
	
	
	/**
	 * Starts the accumulator manager
	 */
	private void start() {
		MetricId.modSize.set(accumulatorMgrConfig.getAccumulatorMod());
		if(scheduler==null) {
			scheduler = new ScheduledThreadPoolExecutor(1, new ThreadFactory(){
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "AccumulatorManagerThread");
					t.setDaemon(true);
					return t;
				}				
			});
		}		
		submissionQueue =  new ArrayBlockingQueue<IIntervalTrace>(accumulatorMgrConfig.getSubmissionQueueSize(), accumulatorMgrConfig.isSubmissionQueueFair());
		accumulators = new HashMap<Integer, IAccumulator>(accumulatorMgrConfig.getAccumulatorMod());
		accumulatorContext = new AccumulatorContext(accumulatorConfig, doFlush,  postFlushLatch, submissionQueue);
		postFlushLatch.set(new CountDownLatch(1));
		for(int i = 0; i < accumulatorMgrConfig.getAccumulatorMod(); i++) {
			IAccumulator accumulator = new DefaultAccumulatorImpl(i, accumulatorContext);
			accumulators.put(i, accumulator);
			accumulator.getProcessingThread().start();
		}
		scheduler.scheduleAtFixedRate(this, accumulatorMgrConfig.getFlushPeriod(), accumulatorMgrConfig.getFlushPeriod(), TimeUnit.MILLISECONDS);
		postFlushLatch.get().countDown();
		started.set(true);
	}
	
	/**
	 * Need to clean up resources.
	 */
	private void stop() {
		
	}
	
	/** Generates up to 64 tracer bit masks used to filter intervals when flushed back to the tracer factory */
	protected final LongBitMaskSequence tracerMasks = LongBitMaskFactory.newSequence();
	
	/**
	 * Registers a tracerFactory for interval flushes.
	 * @param tracerFactory the tracer factory to register
	 * @return the designated bit mask for the registered tracer factory or -1 if the factory has already been registered.
	 */
	public long registerTracerFactory(AbstractTracerInstanceFactory tracerFactory) {
		if(tracerFactory!=null) {
			if(!tracerFactories.containsKey(tracerFactory.getFactoryId())) {
				synchronized(tracerFactories) {
					if(!tracerFactories.containsKey(tracerFactory.getFactoryId())) {
						long mask = -1;
						try {
							mask = tracerMasks.next();
						} catch (Exception e) {
							throw new RuntimeException("Cannot register tracer factory [" + tracerFactory.getClass().getName() + "] as the maximum number have already been registered", new Throwable());
						}
						tracerFactories.put(tracerFactory.getFactoryId(), tracerFactory);
						return mask;
					}
				}
			}
			return -1;
		} else {
			throw new IllegalArgumentException("Passed tracerFactory was null", new Throwable());
		}
		
	}
	
	/**
	 * Unregisters a tracer factory to stop it from receiving flushed interval traces
	 * @param tracerFactoryId The Id of the factory to unregister
	 */
	public void unregisterTracerFactory(String tracerFactoryId) {
		AbstractTracerInstanceFactory factory = tracerFactories.remove(tracerFactoryId);
	}


	/**
	 * Executes the flush every flush period.
	 */
	@Override
	public void run() {
		try {
			postFlushLatch.set(new CountDownLatch(accumulatorMgrConfig.getAccumulatorMod()-1));
			doFlush.set(true);
			IAccumulator.accumulatorThreadGroup.interrupt();
			postFlushLatch.get().await();
			doFlush.set(false);
			Set<IIntervalTrace> intervals = new HashSet<IIntervalTrace>(accumulatorMgrConfig.getFlushBatchSize());
			StringBuilder b = new StringBuilder("\n");
			while(true) {
				submissionQueue.drainTo(intervals, accumulatorMgrConfig.getFlushBatchSize());
				for(AbstractTracerInstanceFactory factory: tracerFactories.values()) {
					factory.submitIntervalTraces(intervals);
				}
				if(intervals.isEmpty()) {
					break;
				} else {
					if(log.isDebugEnabled()) {
						for(IIntervalTrace interval: intervals) {
							if(Arrays.toString(interval.getMetricId().getNamespace()).contains("TraceLoop")) {
								b.append(interval);
							}
						}			
						log.debug(b.toString());
					}
					intervals.clear();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	/** The number of traces dropped since the last reset due to a full accumulator queue */
	protected final AtomicLong dropCount = new AtomicLong(0L);
	
	/**
	 * Submits a trace and routes it through to the correct accumulator.
	 * @param trace The trace to process.
	 */
	public void processTrace(Trace trace) {
		if(trace!=null) {
			if(started.get()) {
				if(!accumulators.get(trace.getMetricId().getMod()).accumulateTrace(trace)) {
					dropCount.incrementAndGet();
				}
			} else {
				dropCount.incrementAndGet();
			}
		}
	}

	/**
	 * <p>Title: AccumulatorManagerFactory</p>
	 * <p>Description: A factory to support the re-configurable AccumulatorManager singleton</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tracing.interval.accumulator.AccumulatorManager.AccumulatorManagerFactory</code></p>
	 */
	public static class AccumulatorManagerFactory {
		
		/**
		 * Acquires the default or existing AccumulatorManager
		 * @return an AccumulatorManager
		 */
		public static AtomicReference<AccumulatorManager> getInstance() {
			return AccumulatorManager.getInstance();
		}
		
		/**
		 * Creates, configures and starts a new AccMgr, killing off the old singleton if it exists.
		 * Typically useful for a container config like Spring to bootstrap a new AccMgr instance. 
		 * @param accMgrConfig the new AccumulatorManager configuration
		 * @param accConfig the new Accumulator configuration
		 * @return an AccumulatorManager
		 */
		public static AtomicReference<AccumulatorManager> getInstanceForce(AccumulatorManagerConfiguration accMgrConfig, AccumulatorConfiguration accConfig) {
			AccumulatorManager accMgr = new AccumulatorManager(false);
			accMgr.accumulatorConfig = accConfig;
			accMgr.accumulatorMgrConfig = accMgrConfig;
			accMgr.start();						
			synchronized(lock) {
				AccumulatorManager oldAccMgr = instance.getAndSet(accMgr);				
				if(oldAccMgr!=null) {
					accMgr.stop();
				}
			}
			return instance;
		}		
	}
	
	
}
