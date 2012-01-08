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
package org.helios.tracing;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.patterns.queues.QueuedSubscriptionProvider;
import org.helios.tracing.bridge.ITracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: PublishingTracerInstanceFactory</p>
 * <p>Description: A factory facade for the PublishingTracer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.PublishingTracerInstanceFactory</code></p>
 * @TODO: Needs to be singleton.
 */

public class PublishingTracerInstanceFactory extends AbstractTracerInstanceFactory implements  ITracingBridge, ITracerInstanceFactory  {
	/** A set of traceSubscribers to traces generated from this tracer */
	protected final Set<QueuedSubscriptionProvider<Trace>> traceSubscribers = new CopyOnWriteArraySet<QueuedSubscriptionProvider<Trace>>();
	/** A set of intervalTraceSubscribers to traces generated from this tracer */
	protected final Set<QueuedSubscriptionProvider<IIntervalTrace>> intervalTraceSubscribers = new CopyOnWriteArraySet<QueuedSubscriptionProvider<IIntervalTrace>>();
	

	public static PublishingTracerInstanceFactory getInstance() {
		return new PublishingTracerInstanceFactory();
	}
	
	/**
	 * Creates a new PublishingTracerInstanceFactory 
	 */
	public PublishingTracerInstanceFactory() {
		super();
	}
	
	/**
	 * Adds a new trace subscriber
	 * @param subscriber the subscriber to add
	 */
	public void addTraceSubscriber(QueuedSubscriptionProvider<Trace> subscriber) {
		if(subscriber!=null) {
			if(subscriber.getSubscriptionQueue()!=null) {
				traceSubscribers.add(subscriber);
			} else {
				throw new RuntimeException("The passed trace subscriber presented a null queue [" + subscriber + "]");
			}
		}
	}
	
	/**
	 * Adds a new interval trace subscriber
	 * @param subscriber the subscriber to add
	 */
	public void addIntervalTraceSubscriber(QueuedSubscriptionProvider<IIntervalTrace> subscriber) {
		if(subscriber!=null) {
			if(subscriber.getSubscriptionQueue()!=null) {
				intervalTraceSubscribers.add(subscriber);
			} else {
				throw new RuntimeException("The passed interval trace subscriber presented a null queue [" + subscriber + "]");
			}
		}
	}
	
	
	/**
	 * Removes a registered trace subscriber
	 * @param subscriber the subscriber to remove
	 */	
	public void removeTraceSubscriber(QueuedSubscriptionProvider<Trace> subscriber) {
		if(subscriber!=null) {
			traceSubscribers.remove(subscriber);
		}
	}

	/**
	 * Removes a registered interval trace subscriber
	 * @param subscriber the subscriber to remove
	 */	
	public void removeIntervalTraceSubscriber(QueuedSubscriptionProvider<IIntervalTrace> subscriber) {
		if(subscriber!=null) {
			intervalTraceSubscribers.remove(subscriber);
		}
	}

	
	/**
	 * Returns a summary of the state of the traceSubscribers
	 * @return a summary of the state of the traceSubscribers
	 */
	@JMXAttribute (name="TraceSubscribers", description="A summary of the state of the traceSubscribers", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getTraceSubscribers() {
		Set<QueuedSubscriptionProvider<Trace>> tmpSet = new HashSet<QueuedSubscriptionProvider<Trace>>(traceSubscribers);
		String[] subs = new String[tmpSet.size()];
		int cntr = 0;
		for(QueuedSubscriptionProvider<Trace> q: tmpSet) {
			subs[cntr] = q.getName() + ":" + q.getSubscriptionQueue().size();
			cntr++;
		}
		return subs;
	}
	
	/**
	 * Returns a summary of the state of the interval trace Subscribers
	 * @return a summary of the state of the interval trace Subscribers
	 */
	@JMXAttribute (name="IntervalTraceSubscribers", description="A summary of the state of the interval trace Subscribers", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getIntervalTraceSubscribers() {
		Set<QueuedSubscriptionProvider<IIntervalTrace>> tmpSet = new HashSet<QueuedSubscriptionProvider<IIntervalTrace>>(intervalTraceSubscribers);
		String[] subs = new String[tmpSet.size()];
		int cntr = 0;
		for(QueuedSubscriptionProvider<IIntervalTrace> q: tmpSet) {
			subs[cntr] = q.getName() + ":" + q.getSubscriptionQueue().size();
			cntr++;
		}
		return subs;
	}
	
	
	

	/**
	 * @return
	 */
	@Override
	public String getEndPointName() {
		return "PublishingHub";
	}

	/**
	 * @return
	 */
	@Override
	public Executor getExecutor() {
		return this.executor;
	}

	/**
	 * @return
	 */
	@Override
	public boolean isIntervalCapable() {
		return true;
	}

	/**
	 * @return
	 */
	@Override
	public boolean isStateful() {
		return false;
	}

	/**
	 * Publishes passed interval traces to the subscribed queues
	 * @param intervalTraces
	 */
	@Override
	public void submitIntervalTraces(IIntervalTrace... traceIntervals) {
		if(traceIntervals!=null && traceIntervals.length>0) {
			for(IIntervalTrace trace: traceIntervals) {
				for(QueuedSubscriptionProvider<IIntervalTrace> q: intervalTraceSubscribers) {
					if(q.getSubscriptionQueue().offer(trace)) {
						intervalTraceSendCounter.incrementAndGet();
					} else {
						intervalTraceDropCounter.incrementAndGet();
						if(log.isDebugEnabled()) {
							log.debug("Dropped interval trace on full queue for [" + q.toString() + "]");
						}
					}
				}
			}
		}		
	}

	/**
	 * Publishes passed traces to the subscribed queues
	 * @param traces
	 */
	@Override
	public void submitTraces(Trace... traces) {
		if(traces!=null && traces.length>0) {
			for(Trace trace: traces) {
				for(QueuedSubscriptionProvider<Trace> q: traceSubscribers) {
					if(q.getSubscriptionQueue().offer(trace)) {
						traceSendCounter.incrementAndGet();
					} else {
						traceDropCounter.incrementAndGet();
						if(log.isDebugEnabled()) {
							log.debug("Dropped trace on full queue for [" + q.toString() + "]");
						}
					}
				}
			}
		}
	}
	

}
