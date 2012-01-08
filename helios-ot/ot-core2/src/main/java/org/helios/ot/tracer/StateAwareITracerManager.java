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

import java.util.concurrent.atomic.AtomicLong;

import org.helios.ot.subtracer.IntervalTracer;
import org.helios.ot.subtracer.PhaseTriggerTracer;
import org.helios.ot.subtracer.TemporalTracer;
import org.helios.ot.subtracer.UrgentTracer;
import org.helios.ot.subtracer.VirtualTracer;
import org.helios.ot.subtracer.pipeline.IPhaseTrigger;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.helios.ot.tracer.disruptor.TraceCollection.OfflineTraceCollection;

/**
 * <p>Title: StateAwareITracerManager</p>
 * <p>Description: A wrapped TracerManager that delegates calls to the actual TracerManager if it is running
 * and makes a best effort to quietly discard requests when the actual TracerManager is down.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.tracer.StateAwareITracerManager</code></p>
 */

public class StateAwareITracerManager implements ITracerManager {
	/** The inner delegate TracerManager */
	private TracerManager3 innerTraceManager;
	/** Indicates if the tm is online */
	private boolean online = false;
	
	/** The singleton instance */
	private static volatile StateAwareITracerManager instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	protected final AtomicLong dropCount = new AtomicLong(0L);
	
	/** The trace collection returned when the tracer manager is offline */
	public static final OfflineTraceCollection OFF_LINE_TC = new OfflineTraceCollection();

	
	static StateAwareITracerManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new StateAwareITracerManager(); 
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new StateAwareITracerManager
	 */
	private StateAwareITracerManager() {
		
	}
	
	/**
	 * Updates the inner tracermanager delegate
	 * @param tm a new TracerManager or null if the tracer mamaner has shut down.
	 */
	void setTracerManager(TracerManager3 tm) {
		online = (tm!=null);
		innerTraceManager = tm;
		if(tm==null) {
			reset();
		}
	}

	/**
	 * Commits a trace collection for processing by endpoints.
	 * @param traceCollection the trace collection to commit.
	 */
	public void commit(TraceCollection traceCollection) {
		if(online) {
			traceCollection.submit();
		}
	}
	
	/**
	 * Returns the next open TraceCollection slot from the ring buffer
	 * @return the next open TraceCollection
	 */
	public TraceCollection getNextTraceCollectionSlot() {
		if(online) {
			TraceCollection tc = innerTraceManager.getNextTraceCollectionSlot();
			if(tc==null) {
				dropCount.incrementAndGet();
				return OFF_LINE_TC;
			}
			return tc;
		}
		return OFF_LINE_TC;
	}
	
	/**
	 * Returns the number of dropped submissions due to unavailable slots
	 * @return the number of dropped submissions due to unavailable slots
	 */
	public long getDropCount() {
		return dropCount.get();
	}
	
	/**
	 * Resets the metrics
	 */
	public void reset() {
		dropCount.set(0);
	}
	
	
	/**
	 * Returns a standard tracer
	 * @return an ITracer
	 */
	public TracerImpl getTracer() {
		return innerTraceManager.getTracer();
	}
	

	
	/**
	 * Stops the tracer manager, the disruptor and deallocates resources.
	 */
	public void shutdown() {
		if(online) {
			innerTraceManager.shutdown();
		}
	}


	

	/**
	 * Returns an interval tracer 
	 * @return an interval tracer.
	 */
	public IntervalTracer getIntervalTracer() {
		return innerTraceManager.getIntervalTracer();
	}


	/**
	 * Returns a phase trigger tracer
	 * @param triggers An array of phase triggers 
	 * @return a PhaseTriggerTracer 
	 */
	public PhaseTriggerTracer getPhaseTriggerTracer(IPhaseTrigger... triggers) {
		return innerTraceManager.getPhaseTriggerTracer(triggers);
	}

	/**
	 * Returns a temporal tracer 
	 * @return a temporal tracer.
	 */
	public TemporalTracer getTemporalTracer() {
		return innerTraceManager.getTemporalTracer();
	}


	/**
	 * Returns an urgent tracer 
	 * @return an urgent tracer.
	 */
	public UrgentTracer getUrgentTracer() {
		return innerTraceManager.getUrgentTracer();
	}

	/**
	 * Returns a virtual tracer 
	 * @param host The virtual tracer's host
	 * @param agent The virtual tracer's agent name
	 * @return a virtual tracer
	 */
	public VirtualTracer getVirtualTracer(String host, String agent) {
		return innerTraceManager.getVirtualTracer(host, agent);
	}

}
