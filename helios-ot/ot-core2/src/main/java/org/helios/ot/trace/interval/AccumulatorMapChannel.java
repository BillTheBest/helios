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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.trace.IntervalTrace;
import org.helios.ot.trace.Trace;
import org.helios.time.SystemClock;


/**
 * <p>Title: AccumulatorMapChannel</p>
 * <p>Description: Manages one stacks of AccumulatorMaps. It is either <i>OffLine</i> which is being flushed and reset or <i>OnLine</i> which has active submission processors applying traces.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.interval.AccumulatorMapChannel</code></p>
 */

public class AccumulatorMapChannel {
	/** An alternating map stack */
	protected final Map<String, IntervalTrace>[] accumulatorMap;
	/** The concurrency count of submission processors */
	protected final AtomicInteger concurrency = new AtomicInteger(0);
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	
	
	/**
	 * Creates a new AccumulatorMapChannel
	 * @param mod The OpenTrace mod
	 * @param accMapSize The accumulator map size
	 */
	@SuppressWarnings("unchecked")
	public AccumulatorMapChannel(int mod, int accMapSize) {
		accumulatorMap = new HashMap[mod];
		for(int i = 0; i < mod; i++) {
			accumulatorMap[i] = new HashMap<String, IntervalTrace>(accMapSize);
		}
	}
	
	/**
	 * Clears all registered interval traces from the accumulator map channel
	 */
	public void resetMapChannel() {
		for(Map<String, IntervalTrace> accMap: accumulatorMap) {
			accMap.clear();
		}
	}
	
	
	/**
	 * Applies the passed collection of traces to the accumulator map for the passed mod
	 * @param mod The mod of the map to apply to
	 * @param traces The collection of traces to apply
	 */
	@SuppressWarnings("unchecked")
	public void apply(int mod, Collection<Trace> traces) {
		if(traces==null || traces.isEmpty()) return;
		concurrency.incrementAndGet();		
		try {
			Map<String, IntervalTrace> map = accumulatorMap[mod];
			long currentTime = SystemClock.currentClock().getTime();
			for(Trace trace: traces) {
				String fqn = trace.getFQN();
				IntervalTrace intervalTrace =  map.get(fqn);
				if(intervalTrace==null) {
					intervalTrace = IntervalTrace.intervalTrace(trace, currentTime);
					map.put(fqn, intervalTrace);					
				}
				intervalTrace.apply(trace);
				if(trace.hasAnyPhaseTriggers() && trace.hasTriggersFor(Phase.APPLIED)) {
					trace.runPhaseTriggers(Phase.APPLIED);
				}
			}
		} finally {
			concurrency.decrementAndGet();
		}
	}
	
	/**
	 * Closes the current interval and returns a set of the closed interval traces
	 * @param mod The mod of the map to close
	 * @param intervalEndTimestamp The end timestamp of this interval and the start of the next one
	 * @return A collection of closed interval traces
	 */
	@SuppressWarnings("unchecked")
	public Collection<IntervalTrace> cloneReset(int mod, long intervalEndTimestamp) {		
		Map<String, IntervalTrace> map = accumulatorMap[mod];
		Set<IntervalTrace> closed = new HashSet<IntervalTrace>(map.size());
		IntervalAccumulator ia = IntervalAccumulator.getInstance(); 
		for(IntervalTrace it: map.values()) {
			//log.info("Flushing interval trace [" + it.getFQN() + "/" + System.identityHashCode(it) + "]");
			//if(log.isTraceEnabled()) log.trace(it);
			IntervalTrace closedInterval = it.cloneReset(intervalEndTimestamp);
			if(closedInterval.hasAnyPhaseTriggers() && closedInterval.hasTriggersFor(Phase.FLUSHED)) {
				closedInterval.runPhaseTriggers(Phase.FLUSHED);
			}			
			try { ia.fireFlushIntervalTraceEvent(closedInterval); } catch (Exception e) {e.printStackTrace(System.err);}
			closed.add(closedInterval);
		}
		return closed;
	}
	
	/**
	 * Returns the number of submission threads currently processing in this channel
	 * @return the number of submission threads currently processing in this channel
	 */
	public int getConcurrency() {
		return concurrency.get();
	}

}
