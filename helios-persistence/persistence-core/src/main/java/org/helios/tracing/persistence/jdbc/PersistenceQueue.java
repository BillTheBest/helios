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
package org.helios.tracing.persistence.jdbc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.tracing.core.trace.Agent;
import org.helios.tracing.core.trace.AgentMetric;
import org.helios.tracing.core.trace.Host;
import org.helios.tracing.core.trace.Metric;
import org.helios.tracing.core.trace.TraceInstance;
import org.helios.tracing.core.trace.TraceValue;
import org.helios.tracing.core.trace.cache.PersistedIndicators;
import org.helios.tracing.core.trace.cache.TraceModelCache;

/**
 * <p>Title: PersistenceQueue</p>
 * <p>Description: A strongly typed multi-queue for pending persistence.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.persistence.jdbc.PersistenceQueue</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class PersistenceQueue {
	protected final TraceModelCache traceModelCache;
	protected final PersistedIndicators pi;
	
	protected final ArrayBlockingQueue<Host> hosts = new ArrayBlockingQueue<Host>(10);
	protected final ArrayBlockingQueue<Agent> agents = new ArrayBlockingQueue<Agent>(100);
	protected final ArrayBlockingQueue<Metric> metrics = new ArrayBlockingQueue<Metric>(1000);
	protected final ArrayBlockingQueue<AgentMetric> agentMetrics = new ArrayBlockingQueue<AgentMetric>(1000);
	protected final ArrayBlockingQueue<TraceValue> traceValues = new ArrayBlockingQueue<TraceValue>(5000);
	
	public static final String HASH_HOST = Host.class.getSimpleName();
	public static final String HASH_AGENT = Agent.class.getSimpleName();
	public static final String HASH_METRIC = Metric.class.getSimpleName();
	public static final String HASH_AGENT_METRIC = AgentMetric.class.getSimpleName();
	
	/**
	 * Creates a new PersistenceQueue 
	 * @param traceModelCache
	 */
	public PersistenceQueue(TraceModelCache traceModelCache) {
		this.traceModelCache = traceModelCache;
		this.pi = traceModelCache.getPersistedIndicators();
	}

	/**
	 * Adds the TraceInstance elements to be persisted to the work queue
	 * @param trace The trace instance to add
	 * @throws InterruptedException 
	 */
	public void process(TraceInstance trace) throws InterruptedException {
		if(trace==null) return;
		if(!pi.contains(HASH_HOST, trace.getHost().getId())) {
			hosts.put(trace.getHost());
		}
		if(!pi.contains(HASH_AGENT, trace.getAgent().getId())) {
			agents.put(trace.getAgent());
		}
		if(!pi.contains(HASH_METRIC, trace.getMetric().getId())) {
			metrics.put(trace.getMetric());
		}
		if(!pi.contains(HASH_AGENT_METRIC, trace.getAgentMetric().getId())) {
			agentMetrics.put(trace.getAgentMetric());
		}
		if(trace.getMetric().getType().isNumber()) {
			traceValues.put(trace.getTraceValue());
		}
	}
	
	/**
	 * Returns the total number of elements waiting to be persisted
	 * @return the total number of elements waiting to be persisted
	 */
	@JMXAttribute(name="QueueSize", description="The total number of elements waiting to be persisted", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getQueueSize() {
		return (hosts.size() + agents.size() + metrics.size() + agentMetrics.size() + traceValues.size());
	}
	
	/**
	 * Flushes the pending hosts to be persisted and returns them
	 * @return a set of the pending hosts to be persisted 
	 */
	public Collection<Host> flushHosts() {
		Set<Host> drain = new HashSet<Host>(hosts.size());
		hosts.drainTo(drain);
		return drain;		
	}
	
	/**
	 * Flushes the pending agents to be persisted and returns them
	 * @return a set of the pending agents to be persisted 
	 */
	public Collection<Agent> flushAgents() {
		Set<Agent> drain = new HashSet<Agent>(agents.size());
		agents.drainTo(drain);
		return drain;		
	}
	
	/**
	 * Flushes the pending metrics to be persisted and returns them
	 * @return a set of the pending metrics to be persisted 
	 */
	public Collection<Metric> flushMetrics() {
		Set<Metric> drain = new HashSet<Metric>(metrics.size());
		metrics.drainTo(drain);
		return drain;		
	}
	
	/**
	 * Flushes the pending agentMetrics to be persisted and returns them
	 * @return a set of the pending agentMetrics to be persisted 
	 */
	public Collection<AgentMetric> flushAgentMetrics() {
		Set<AgentMetric> drain = new HashSet<AgentMetric>(agentMetrics.size());
		agentMetrics.drainTo(drain);
		return drain;		
	}
	
	/**
	 * Flushes the pending traceValues to be persisted and returns them
	 * @return a set of the pending traceValues to be persisted 
	 */
	public Collection<TraceValue> flushTraceValues() {
		Set<TraceValue> drain = new HashSet<TraceValue>(traceValues.size());
		traceValues.drainTo(drain);
		return drain;		
	}
	
	
	
}
