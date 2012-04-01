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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import org.apache.log4j.Logger;
import org.helios.ot.trace.Trace;

/**
 * <p>Title: SubmissionProcessor</p>
 * <p>Description: The runnable assigned to read trace submissions from a designated submission queue and apply them to the designated accumulator map</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.interval.SubmissionProcessor</code></p>
 */

public class SubmissionProcessor implements Runnable {
	/** The assigned mod for this thread to process */
	protected final int mod;
	/** The submission queue reader batch size */
	protected final int readerBatchSize;
	
	/** The assigned queue for this thread to read from */
	protected final BlockingQueue<Trace> queue;
	/** The thread running this processor */
	protected Thread processorThread = null;
	/** The continue running flag */
	protected final AtomicBoolean run = new AtomicBoolean(true);
	/** The accumulator switch to which submissions are applied */
	protected final AccumulatorSwitch accSwitch;
	/** Instance logger */
	protected final Logger log;
	
	
	
	/**
	 * Creates a new SubmissionProcessor
	 * @param mod The assigned mod for this thread to process
	 * @param readerBatchSize The maximum number of non-waited reads to make from the submission queue at one time
	 * @param queue The assigned queue for this thread to read from
	 * @param accSwitch 
	 */
	public SubmissionProcessor(int mod, int readerBatchSize, BlockingQueue<Trace> queue, AccumulatorSwitch accSwitch) {
		this.mod = mod;
		this.readerBatchSize = readerBatchSize;
		this.queue = queue;
		this.accSwitch = accSwitch;
		log = Logger.getLogger(getClass().getName() + "#" + mod);
		if(log.isDebugEnabled()) log.debug("Created SubmissionProcessor");
	}
	
	/**
	 * Stops the processor
	 */
	void stopProcessor() {
		if(log.isDebugEnabled()) log.debug("Stopping SubmissionProcessor");
		run.set(false);
		if(processorThread!=null) {
			processorThread.interrupt();
		}
	}



	/**
	 * Submission processing loop. The sequence is:<ol>
	 * 		<li>Read a batch of traces off the submission queue</li>
	 * 		<li>Acquire the submission lock</li>
	 * 		<li>Apply the batch of traces to the accumulator map</li>
	 * 		<li>Release the submission lock</li>
	 *  </ol>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		processorThread = Thread.currentThread();
		if(log.isDebugEnabled()) log.debug("SubmissionProcessor Started Processing");
		boolean intervalHasAction = false;
		while(run.get()) {
			try {					
				if(accSwitch.processSubmissions()) {
					// We're in SUBMIT mode					
					List<Trace> batch = new ArrayList<Trace>(readerBatchSize);
					int traceCount = 1;
					Trace trace = queue.take();
					batch.add(trace);					
					while(traceCount<readerBatchSize) {
						trace = queue.poll();
						if(trace==null) break;
						batch.add(trace);
						traceCount++;
					}
					if(log.isTraceEnabled()) log.trace("Acquired [" + traceCount + "] Traces to Apply");
					if(traceCount > 0) {
						intervalHasAction = true;
						accSwitch.apply(mod, batch);
					}									
				} else {
					// We're in FLUSH mode
//					if(intervalHasAction) {
//						accSwitch.flush(mod);						
//					} else {
//						accSwitch.nullFlush(mod);
//					}
					// ===============================================
					// It seems wasteful to ALWAYS flush items even
					// when we know there was no action in this mod.
					// This is a future optimization where some endpoint
					// will account for missing interval traces and 
					// assume they are zero-action traces.
					// ===============================================
					accSwitch.flush(mod);
					accSwitch.waitForFlushComplete(mod);
					intervalHasAction = false;
					
				}
			} catch (InterruptedException ie) {				
				if(run.get()) {
					//if(log.isDebugEnabled()) log.debug("SubmissionProcessor Interrupted");
					// Reset the interrupted state
					Thread.interrupted();
				}
			} catch (Exception e) {
				log.error("SubmissionProcessor Unexpected Exception", e);
			}
		}
		log.info("SubmissionProcessor Stopped");
	}

}
