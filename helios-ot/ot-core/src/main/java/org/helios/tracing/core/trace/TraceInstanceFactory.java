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
package org.helios.tracing.core.trace;

import java.util.Map;

import org.helios.sequence.SequenceManager;
import org.helios.tracing.core.trace.cache.TraceModelCache;
import org.helios.tracing.trace.MetricType;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: TraceInstanceFactory</p>
 * <p>Description:A factory for creating trace instances from maps, traces etc. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.TraceInstanceFactory</code></p>
 */

public class TraceInstanceFactory {
	/** The cache service for resolving hosts, agent, metrics and agent-metrics */
	protected final TraceModelCache traceModelCache;
	/** The sequence generator */
	protected final SequenceManager sequenceManager;

	/**
	 * Creates a new TraceInstanceFactory
	 * @param traceModelCache the cache to use
	 */
	public TraceInstanceFactory(TraceModelCache traceModelCache) {
		this.traceModelCache = traceModelCache;
		sequenceManager = traceModelCache.getSequenceManager();
	}
	

	/**
	 * Returns the injected TraceModelCache
	 * @return the injected TraceModelCache
	 */
	public TraceModelCache getTraceModelCache() {
		return traceModelCache;
	}

	
	
	/**
	 * Creates a new TraceInstance from the passed map
	 * @param traceMap A map of key/values to build a trace instance. The constants
	 * for the map keys are defined in <code>TraceInstance</code>.
	 * @return the built trace instance.
	 */
	public TraceInstance getInstance(Map<String, Object> traceMap) {
		Host host = traceModelCache.putHost((String)traceMap.get(TraceInstance.TRACE_HOST));
		Agent agent = traceModelCache.putAgent(host.getName(), (String)traceMap.get(TraceInstance.APPLICATION_ID));
		Metric metric = traceModelCache.putMetric((String)traceMap.get(TraceInstance.TRACE_FULLNAME), (MetricType)traceMap.get(TraceInstance.TRACE_TYPE));
		AgentMetric agentMetric = traceModelCache.putAgentMetric(agent, metric);
		TraceValue traceValue = agentMetric.getTrace(
				(Long)traceMap.get(TraceInstance.TRACE_TS), 
				traceMap.get(TraceInstance.TRACE_VALUE), 
				(Boolean)traceMap.get(TraceInstance.TRACE_URGENT), 
				(Boolean)traceMap.get(TraceInstance.TRACE_TEMPORAL));		
		return TraceInstance.newBuilder().traceValue(traceValue).build();
	}
	
	/**
	 * Creates a new TraceInstance from the passed trace
	 * @param trace The trace to create the TraceInstance from
	 * @return the built trace instance
	 */
	public TraceInstance getInstance(Trace trace) {
		Host host = traceModelCache.putHost(trace.getHostName());
		Agent agent = traceModelCache.putAgent(host.getName(), trace.getAgentName());
		Metric metric = traceModelCache.putMetric(trace.getLocalName(), trace.getMetricType());
		AgentMetric agentMetric = traceModelCache.putAgentMetric(agent, metric);
		TraceValue traceValue = agentMetric.getTrace(
				trace.getTimeStamp(), 
				trace.getValue(),
				trace.isUrgent(), 
				trace.isTemporal());		
		return TraceInstance.newBuilder().traceValue(traceValue).build();
	}
	
}
