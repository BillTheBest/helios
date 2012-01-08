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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.interval.IntervalFactory;
import org.helios.tracing.trace.MetricId;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: DefaultAccumulatorImpl</p>
 * <p>Description: The default IAccumulator implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.accumulator.DefaultAccumulatorImpl</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class DefaultAccumulatorImpl extends ManagedObjectDynamicMBean implements IAccumulator , Runnable {
	/** A map of tracemetric intervals keyed by the designated internal metric ID. */
	protected final Map<Integer, IIntervalTrace> intervalTraces;
	/** The accumulator processing thread */
	protected final Thread processorThread;
	/** The modulo code that this accumulator is processing */
	protected final int modCode;
	/** The accumulator manager provided control context */
	protected final AccumulatorContext context;
	/** The accumulator manager configuration */
	protected final AccumulatorConfiguration accConfiug;
	
	/** The incoming trace queue */
	protected final BlockingQueue<Trace> feedQueue;
	/** instance logger */
	protected final Logger log;
	/** the number of flushes */
	protected final AtomicLong flushCount = new AtomicLong(0L);
	/** the elapsed time of the last flush */
	protected final AtomicLong lastFlushTime = new AtomicLong(0L);
	/** the number of intervals flushed in the last flush */
	protected final AtomicInteger lastFlushCount = new AtomicInteger(0);
	/** the number of intervals dropped in the last flush */
	protected final AtomicInteger lastFlushDropCount = new AtomicInteger(0);
	/** the number of traces processed in the last interval */
	protected final AtomicLong lastIntervalTracesProcessed = new AtomicLong(0);
	
	
	
	
	/**
	 * Creates a new DefaultAccumulatorImpl
	 * @param modCode The modulo code that this accumulator is processing
	 * @param context The accumulator manager provided control context
	 */
	public DefaultAccumulatorImpl(int modCode, AccumulatorContext context) {
		this.modCode = modCode;
		this.context = context;	
		accConfiug = context.getAccumulatorConfiguration();
		this.intervalTraces = new ConcurrentHashMap<Integer, IIntervalTrace>(accConfiug.getAccumulatorMapSize(), accConfiug.getAccumulatorMapLoadFactor());
		this.processorThread = threadFactory.newThread(this);
		this.feedQueue = new ArrayBlockingQueue<Trace>(accConfiug.getAccumulatorQueueSize(), accConfiug.isAccumulatorQueueFair());
		log = Logger.getLogger(DefaultAccumulatorImpl.class.getName() + "[" + this.modCode + "]");
		this.reflectObject(this);
		objectName = JMXHelper.objectName(new StringBuilder(getClass().getPackage().getName()).append(":service=").append(getClass().getSimpleName()).append(",mod=").append(this.modCode));
		try {
			if(JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
				JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName);
			}
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (Exception e) {
			log.warn("Failed to register management interface. Continuing without", e);
		}
		log.info("Created Accumulator [" + modCode + "]");
	}
	
	/**
	 * Defines the task that the processor thread runs to sink the trace queue. 
	 */
	public void run() {
		try {
			if(log.isDebugEnabled()) log.debug("Waiting on Start Latch");
			context.getAccumulatorLatch().get().await();
		} catch (Exception e) {
			throw new RuntimeException("Failed to wait for AccumulatorManager Startup Wait Coordinator", e);
		}
		while(true) {			
			if(log.isDebugEnabled()) log.debug("Starting processing loop");
			Trace trace = null;
			try {
				trace = feedQueue.take();
				if(trace!=null) processTrace(trace);
				if(context.isDoFlush()) {
					long start = System.currentTimeMillis();
					doFlush();
					lastFlushTime.set(System.currentTimeMillis()-start);
					flushCount.incrementAndGet();
				}				
			} catch (InterruptedException ie) {
				if(Thread.interrupted()) Thread.interrupted();
				if(context.isDoFlush()) {
					doFlush();
				}
			}
		}
	}
	
	/**
	 * Executes a close, clone and reset on each metric interval, and flushes the closed intervals.
	 */
	protected void doFlush() {
		final Collection<IIntervalTrace> closedIntervals = new HashSet<IIntervalTrace>(intervalTraces.size());
		if(log.isDebugEnabled()) log.debug("Starting flush on [" + intervalTraces.size() + "] interval metrics");		
		for(IIntervalTrace interval: intervalTraces.values()) {
			interval.close();
			closedIntervals.add(interval.clone());
			interval.reset();							
		}
		int[] counts = context.submit(closedIntervals);
		lastFlushCount.set(counts[0]);
		lastFlushDropCount.set(counts[1]);
		if(log.isDebugEnabled()) log.debug("Flush Complete. Waiting On Completion Latch");
		context.flushComplete();
		flushCount.incrementAndGet();
		lastIntervalTracesProcessed.set(0);
	}
	
	/**
	 * Applies the trace to the accumulator
	 * @param trace the trace to apply
	 */
	public void processTrace(Trace trace) {
		lastIntervalTracesProcessed.incrementAndGet();
		int serial = trace.getSerial();
		IIntervalTrace interval = intervalTraces.get(serial);
		if(interval==null) {
			synchronized(intervalTraces) {
				interval = intervalTraces.get(serial);
				if(interval==null) {
					MetricId id = trace.getMetricId();
					interval = IntervalFactory.createInterval(id);
					intervalTraces.put(serial, interval);					
				}
			}
		}
		interval.apply(trace);
	}
	
	/**
	 * Submits a trace for accumulation
	 * @param trace the trace to apply
	 * @return true if the trace was accepted, false if it was dropped because of a full queue.
	 */
	public boolean accumulateTrace(Trace trace) {
		boolean success =  feedQueue.offer(trace);
		if(!success) {
			// increment drops
		}
		return success;
	}
	
	
	/**
	 * Returns this accumulators processor thread
	 * @return this accumulators processor thread
	 */
	public Thread getProcessingThread() {
		return processorThread;
	}

	
	
	/**
	 * Returns the total number of flushes 
	 * @return the total number of flushes
	 */
	@JMXAttribute(name="FlushCount", description="The total number of flushes", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFlushCount() {
		return flushCount.get();
	}

	/**
	 * Returns the elapsed time of the last flush (ms.)
	 * @return the elapsed time of the last flush (ms.)
	 */
	@JMXAttribute(name="LastFlushTime", description="The elapsed time of the last flush (ms.)", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastFlushTime() {
		return lastFlushTime.get();
	}

	/**
	 * Returns the number of intervals flushed in the last flush
	 * @return the number of intervals flushed in the last flush
	 */
	@JMXAttribute(name="LastFlushCount", description="The number of intervals flushed in the last flush", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastFlushCount() {
		return lastFlushCount.get();
	}

	/**
	 * Returns the number of intervals dropped in the last flush
	 * @return the number of intervals dropped in the last flush
	 */
	@JMXAttribute(name="LastFlushDropCount", description="The number of intervals dropped in the last flush", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastFlushDropCount() {
		return lastFlushDropCount.get();
	}

	/**
	 * Returns the number of traces processed in the last interval 
	 * @return the number of traces processed in the last interval 
	 */
	@JMXAttribute(name="LastIntervalTracesProcessed", description="The number of traces processed in the last interval", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastIntervalTracesProcessed() {
		return lastIntervalTracesProcessed.get();
	}
	
	@JMXAttribute(name="IntervalCount", description="The number of traces processed in the last interval", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getIntervalCount() {
		return intervalTraces.size();
	}
	
	
}
