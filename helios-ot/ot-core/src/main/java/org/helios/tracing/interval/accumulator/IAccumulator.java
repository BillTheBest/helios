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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.tracing.trace.Trace;

/**
 * <p>Title: IAccumulator</p>
 * <p>Description: Defines an accumulator that manages the accumulation of interval metrics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.accumulator.IAccumulator</code></p>
 */

public interface IAccumulator extends Runnable {
	/** A serial number factory for all accumulator threads */
	static final AtomicInteger serial = new AtomicInteger(0);
	/** The thread group for all accumulator threads */
	static final ThreadGroup accumulatorThreadGroup = new ThreadGroup(DefaultAccumulatorImpl.class.getSimpleName() + "ThreadGroup");
	/** The thread factory that creates the accumulator threads */
	static final ThreadFactory threadFactory = new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(accumulatorThreadGroup, r, DefaultAccumulatorImpl.class.getSimpleName() + "Thread#" + serial.incrementAndGet());
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}		
	};
	
	
	/**
	 * Returns this accumulators processor thread
	 * @return this accumulators processor thread
	 */
	Thread getProcessingThread();
	
	/**
	 * Applies the trace to the accumulator
	 * @param trace the trace to apply
	 */
	public void processTrace(Trace trace);
	
	/**
	 * Submits a trace for accumulation
	 * @param trace the trace to apply
	 * @return true if the trace was accepted, false if it was dropped because of a full queue.
	 */
	public boolean accumulateTrace(Trace trace);

}
