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
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.tracing.interval.IIntervalTrace;

/**
 * <p>Title: AccumulatorContext</p>
 * <p>Description: A context shared by all accumulators in an accumulator manager that provides directives to the accumulators.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.accumulator.AccumulatorContext</code></p>
 */

public class AccumulatorContext {
	/** The accumulator config */
	protected AccumulatorConfiguration accConfig = null;
	
	
	/** The flag indicating id processor threads should execute a flush */
	private final AtomicBoolean doFlush;
	/** A countdown latch reference to synchronize the end of the flush */
	private final AtomicReference<CountDownLatch> accumulatorLatch;
	
	/** The total number of dropped metrics due to submission timeouts */
	private final AtomicLong dropCount = new AtomicLong(0L);
	
	/** The submission queue */
	private final BlockingQueue<IIntervalTrace> submissionQueue;
	
	/**
	 * Creates a new AccumulatorContext 
	 * @param accConfig The accumulator configuiration
	 * @param doFlush The flag indicating id processor threads should execute a flush
	 * @param accumulatorLatch A countdown latch reference to synchronize the end of the flush
	 * @param submissionQueue The submission queue
	 */
	public AccumulatorContext(AccumulatorConfiguration accConfig , AtomicBoolean doFlush, AtomicReference<CountDownLatch> accumulatorLatch, BlockingQueue<IIntervalTrace> submissionQueue) {
		this.doFlush = doFlush;
		this.accumulatorLatch = accumulatorLatch;
		this.submissionQueue = submissionQueue;
	}
	
	/**
	 * Indicates that the accumulator processor thread should start a flush
	 * @return if true, the accumulator processor thread should start a flush
	 */
	public boolean isDoFlush() {
		return doFlush.get();
	}
	
	/**
	 * Accumulator processor threads call this when they complete their flush.
	 * The call will complete when all threads have completed the flush.
	 * If the waiting threads are interrupted while waiting on the latch, or the latch await time expires,
	 *   ----> then we need to figure out what to do.
	 */
	public void flushComplete() {
		CountDownLatch latch = accumulatorLatch.get();
		latch.countDown();
		boolean cleanComplete = false;
		try {
			cleanComplete = latch.await(accConfig.getAccumulatorLatchTimeout(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException ie) {
			cleanComplete = false;
		}
		if(!cleanComplete) {
			// what do we do here ?
		}
	}
	
	/**
	 * TraceInterval submission interface for accumulators to flush to
	 * @param intervals A collection of the accumulator's closed interval metrics.
	 * @return an int array where [0] is the number of submits, and [1] is the drop count
	 */
	public int[] submit(Collection<IIntervalTrace> intervals) {
		long _timeout = accConfig.getAccumulatorQueueTimeout();
		int pCount = 0, dCount = 0;
		boolean complete = true;
		for(Iterator<IIntervalTrace> iter = intervals.iterator(); iter.hasNext();) {
			try {
				if(!submissionQueue.offer(iter.next(), _timeout, TimeUnit.MILLISECONDS)) {					
					complete = false;
				} else {
					iter.remove();
					pCount++;
				}
			} catch (InterruptedException ie) {
				complete = false;
			}
		}
		if(!complete) {
			// flush did not complete
			dCount = intervals.size();
			dropCount.addAndGet(intervals.size());
			intervals.clear();
		}
		return new int[]{pCount, dCount};
	}


	/**
	 * Returns the total number of dropped metrics due to submission timeouts
	 * @return the dropCount
	 */
	public long getDropCount() {
		return dropCount.get();
	}

	

	/**
	 * Returns the context shared wait latch used to coordinate accumulator thread waits.
	 * @return the accumulatorLatch
	 */
	public AtomicReference<CountDownLatch> getAccumulatorLatch() {
		return accumulatorLatch;
	}

	/**
	 * Returns the accumulator configuration
	 * @return the accConfig
	 */
	public AccumulatorConfiguration getAccumulatorConfiguration() {
		return accConfig;
	}
	
	
}
