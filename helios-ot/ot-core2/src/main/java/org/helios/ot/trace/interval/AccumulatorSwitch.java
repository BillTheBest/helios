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

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.trace.IntervalTrace;
import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.TracerManager3;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.helios.time.SystemClock;

/**
 * <p>Title: AccumulatorSwitch</p>
 * <p>Description: The main entry point for {@link SubmissionProcessor}s to apply traces to intervals in the current {@link AccumulatorMapChannel}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.interval.AccumulatorSwitch</code></p>
 */

public class AccumulatorSwitch {
	/** Switch control that determines if submission processors are in submit or flush mode */
	protected final AtomicBoolean accSwitch = new AtomicBoolean(true);
	/** A countdown latch, counted down by submission processors as they finish processing flush signals */
	protected AtomicReference<CountDownLatch> submissionProcessorLatch = new AtomicReference<CountDownLatch>(null);
	/** A countdown latch, counted down by the accumulator switch when the flush is complete that the submission processors wait on once they complete their flush */
	protected AtomicReference<CountDownLatch> flushCompleteLatch = new AtomicReference<CountDownLatch>(null);
//	/** The flush set that submission processors flush their cloned IntervalTraces into */
//	protected final Set<IntervalTrace>[] intervalTraces;
	/** The thread pool containing the submission processor threads */
	protected final ThreadGroup submissionProcessors;
	/** The current flush period timestamp */
	protected volatile long flushTimestamp = 0L;
	/** The switched channel */
	protected final AccumulatorMapChannel channel;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The accumulator map size */
	protected final int accMapSize;
	/** The OpenTrace Mod */
	protected final int mod;
	/** The group flush counter */
	protected final AtomicLong flushCounter = new AtomicLong(0L);
	
	/**
	 * Creates a new AccumulatorSwitch
	 * @param mod The OpenTrace Mod
	 * @param accMapSize The channel map size
	 * @param submissionProcessors The submission processor thread group
	 */
	public AccumulatorSwitch(int mod, int accMapSize, ThreadGroup submissionProcessors) {
		this.accMapSize = accMapSize;
		this.mod = mod;
		this.submissionProcessors = submissionProcessors;
		channel = new AccumulatorMapChannel(mod, accMapSize);
//		intervalTraces = new HashSet[mod];
//		for(int i = 0; i < mod; i++) {
//			intervalTraces[i] =  new HashSet<IntervalTrace>(512);
//		}
	}
	
	/**
	 * Delegates the apply operation to the currently focused channel
	 * @param mod The mod of the submitter
	 * @param traces The traces to apply
	 */
	void apply(int mod, Collection<Trace> traces) {
		channel.apply(mod, traces);
	}
	
	/**
	 * Informs the submission processor if it should be processing submissions or flushing
	 * @return true if it should be processing submissions, false if it should flushing
	 */
	boolean processSubmissions() {
		return accSwitch.get();
	}
	
	/**
	 * The submission processors wait on this after they flush
	 * @param mod The mod of the thread waiting
	 */
	void waitForFlushComplete(int mod) {
		try {
			flushCompleteLatch.get().await();
		} catch (InterruptedException ie) {
			log.error("SubmissionProcessor#" + mod + " Interrupted Waiting on FlushComplete Latch", ie);
			// TODO: What do we do here ? 
		}
	}
	
	
	/**
	 * The submission processor threads call a flush on their interval traces here.
	 * @param mod The mod of the interval traces to flush
	 */
	@SuppressWarnings("unchecked")
	void flush(int mod) {
		SystemClock.startTimer();
		Collection<IntervalTrace> flushed = channel.cloneReset(mod, flushTimestamp);
		final int metricCount = flushed.size();
		TraceCollection tc = TracerManager3.getInstance().getNextTraceCollectionSlot();
		if(tc!=null) {
			tc.load(flushed);
			TracerManager3.getInstance().commit(tc);
		}
		for(IntervalTrace it: flushed) {
			if(it.hasAnyPhaseTriggers() && it.hasTriggersFor(Phase.ATTACHED)) {
				it.runPhaseTriggers(Phase.ATTACHED);
			}
		}
		log.info("Submission Processor#" + mod + " Flushed [" + metricCount + "] traces in " + SystemClock.endTimer());
		submissionProcessorLatch.get().countDown();
	}
	
	/**
	 * Clears all registered interval traces from the accumulator map channel
	 */
	public void resetMapChannel() {
		channel.resetMapChannel();
	}
	
	/**
	 * When a submission processor has nothing to accumulate on a flush signal, it will call this instead of flush(mod).
	 * Since there is no work, there's no point in firing off an empty trace collection,
	 * BUT, WE NEED TO DROP THE LATCH.
	 * @param mod
	 */
	void nullFlush(int mod) {
		submissionProcessorLatch.get().countDown();
	}
	
	private final AtomicInteger concurrency = new AtomicInteger(0);
	
	/**
	 * Issues a channel switch at the end of an interval
	 * @param intervalEndTimestamp The end timestamp of this interval and the start of the next one
	 */	
	void switchChannel(long intervalEndTimestamp) {
		int threads = concurrency.incrementAndGet();
		if(threads>1) {
			log.error("\n\t! ! ! ! ! ! !\n\tMultiple Threads in SwitchChannel:" + threads  + "\n\t! ! ! ! ! ! !");
		}
		submissionProcessorLatch.set(new CountDownLatch(mod));
		flushCompleteLatch.set(new CountDownLatch(1));
		// Switch the submission processors to flush mode
		accSwitch.set(false);
		// Set the current flush period
		flushTimestamp = SystemClock.time();
		// Interrupt the submission processors in case they're waiting on a trace
		submissionProcessors.interrupt();
		try {
			long start = System.nanoTime();
			// Waits for submission processors to complete their flush
			submissionProcessorLatch.get().await();
			long elapsed = System.nanoTime()-start;
			// Switch the submission processors back to submit mode
			accSwitch.set(true);
			// the flush is complete so drop the flush complete latch and let the submission processors continue
			flushCompleteLatch.get().countDown();
			log.info("Submission Processors Flushed interval traces in [" + elapsed + "] ns. [" + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS) + "] ms.");
			int i = 0;
			SystemClock.startTimer();
			// This needs to go in a seperate thread.
//			IntervalAccumulator ia = IntervalAccumulator.getInstance(); 
//			for(Set<IntervalTrace> traceSet: intervalTraces) {
//				for(IntervalTrace trace: traceSet) {
//					i++;
//					try { JMXMetric.publish(trace); } catch (Exception e) {e.printStackTrace(System.err);}
//					try { ia.fireFlushIntervalTraceEvent(trace); } catch (Exception e) {e.printStackTrace(System.err);}
//				}
//			}
//			ElapsedTime et = SystemClock.endTimer();
//			log.info("Updated [" + i + "] OpenMetricTypes in " + et);
			
		} catch (InterruptedException ie) {
			log.error("FlushProcessor Interrupted Waiting on SubmissionProcessor Latch", ie);
			// TODO: What do we do here ? 			
		} finally {
			concurrency.decrementAndGet();
		}
		
//		AccumulatorMapChannel closingChannel = channels[accSwitch.get() ? 0 : 1];
//		accSwitch.set(!accSwitch.get());
//		long start = System.nanoTime();
//		while(closingChannel.getConcurrency()>0) {
//			Thread.yield();
//		}
//		long elapsed = System.nanoTime() - start;
//		log.info("AccSwitch Waited [" + elapsed + "] ns. for closing channel zero concurrency");
//		Set<IntervalTrace> intervalTraces = new HashSet<IntervalTrace>(mod*accMapSize);
//		for(int i = 0; i < mod; i++) {
//			intervalTraces.addAll(closingChannel.cloneReset(i, intervalEndTimestamp));
//		}
	}
}
