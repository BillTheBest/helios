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
package org.helios.tracing.bridge;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmxenabled.queues.FlushQueueReceiver;
import org.helios.jmxenabled.queues.TimeSizeFlushQueue;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: AbstractTracingBridge</p>
 * <p>Description: An abstract base class for TracingBridge implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.bridge.AbstractTracingBridge</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public abstract class AbstractTracingBridge extends ManagedObjectDynamicMBean implements ITracingBridge, FlushQueueReceiver<Trace> {
	/** The configured bridge executor */
	protected Executor threadPool = null;
	/** The trace flush queue */
	protected final TimeSizeFlushQueue<Trace> traceFlushQueue;
	/** The trace interval flush queue */
	protected TimeSizeFlushQueue<? extends IIntervalTrace> traceIntervalFlushQueue = null;
	/** Indicates if the bridge is interval capable */
	protected final boolean intervalCapable;
	/** The tracing bridge name */
	protected final String name;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/**
	 * Creates a new AbstractTracingBridge
	 * @param name The tracing bridge name
	 * @param bufferSize The maximum size of the trace bufffer before it is flushed. 0 will disable size triggered flushes.
	 * @param frequency The frequency on which the trace buffer is flushed (ms.) 0 will disable time triggered flushes.
	 * @param intervalCapable Indicates if the bridge is interval capable
	 */
	public AbstractTracingBridge(String name, int bufferSize, long frequency, boolean intervalCapable) {
		this.name = name==null ? getClass().getSimpleName() : name;
		this.intervalCapable = intervalCapable;
		traceFlushQueue = new TimeSizeFlushQueue<Trace>(name, bufferSize, frequency, this);
		if(this.intervalCapable) {
			traceIntervalFlushQueue = new TimeSizeFlushQueue<IIntervalTrace>(name, bufferSize, frequency, new FlushQueueReceiver<IIntervalTrace>(){
				public void flushTo(Collection<IIntervalTrace> flushedItems) {
					flushIntervalsTo(flushedItems);
				}
			});
		} else {
			traceIntervalFlushQueue=null;
		}
		log.info("Created [" + getClass().getSimpleName() + "]");
	}
	
	
	/**
	 * Returns true if the bridge is stateful and can be up or down.
	 * @return true if the bridge is stateful and can be up or down, false if it is stateless.
	 */
	public boolean isStateful() {
		return this.intervalCapable;
	}
	
	/**
	 * Sets the thread pool used by this tracing bridge for reconnect polling and flushes.
	 * @param threadPool an executor	 
	 */
	public void setExecutor(Executor threadPool) {
		this.threadPool = threadPool;
	}
	
	/**
	 * Returns the configured executor
	 * @return the configured executor
	 */
	public Executor getExecutor() {
		return threadPool;
	}


	/**
	 * Indicates if the bridge is interval capable
	 * @return true if the bridge is interval capable
	 */
	@Override
	public boolean isIntervalCapable() {
		return intervalCapable;
	}


	/**
	 * Submits traces to the tracing endpoint
	 * @param traces the traces to submit
	 */
	public abstract void submitTraces(Collection<Trace> traces);
	
	/**
	 * Submits traces to the tracing endpoint
	 * @param traces the traces to submit
	 */
	public void submitTraces(Trace... traces) {
		if(traces!=null) {
			submitTraces(Arrays.asList(traces));
		}
	}
	
	
	/**
	 * Submits trace intervals to the tracing endpoint
	 * @param intervalTraces the trace intervals to submit
	 */
	public void submitIntervalTraces(IIntervalTrace... traceIntervals) {
		if(traceIntervals!=null) {
			submitIntervalTraces(Arrays.asList(traceIntervals));
		}		
	}
	
//	{
//		StringBuilder b = new StringBuilder("\nTrace Submission[");
//		if(traces!=null && traces.length>0) {
//			for(Trace trace: traces) {
//				b.append("\n\t").append(trace);
//			}
//		}		
//		b.append("\n]");
//		log.info(b.toString());
//	}
	
	/**
	 * Submits trace intervals to the tracing endpoint
	 * @param intervalTraces the trace intervals to submit
	 */
	public abstract void submitIntervalTraces(Collection<IIntervalTrace> intervalTraces);
//	{
//		StringBuilder b = new StringBuilder("\nTraceInterval Submission[");
//		if(intervalTraces!=null && intervalTraces.length>0) {
//			for(IIntervalTrace traceInterval: intervalTraces) {
//				b.append("\n\t").append(traceInterval);
//			}
//		}		
//		b.append("\n]");
//		log.info(b.toString());		
//	}



	/**
	 * Receives a batch of intervalTraces from the flushQueue
	 * @param flushedItems an array of TraceIntervals
	 */
	public void flushIntervalsTo(Collection<IIntervalTrace> flushedItems) {
		submitIntervalTraces(flushedItems);
	}



}


/**
	@TODO:
	Drop Count
	Send Count
	Interval Filtering and Config
	Buffer/Flush
*/