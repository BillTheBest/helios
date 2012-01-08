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
package org.helios.tracing.core.trace.cache;

import org.helios.sequence.SequenceManager;
import org.helios.tracing.core.trace.Agent;
import org.helios.tracing.core.trace.AgentMetric;
import org.helios.tracing.core.trace.Host;
import org.helios.tracing.core.trace.Metric;
import org.helios.tracing.trace.MetricType;

/**
 * <p>Title: TraceModelCache</p>
 * <p>Description: Defines a standard caching interface for actively cached trace model instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.cache.TraceModelCache</code></p>
 */

public interface TraceModelCache {
	/** Compound Key Delimeter  */
	public static final String DELIM = "/";	
	/** Format specifier for AgentMetric key */
	public static final String AGENT_METRIC_KEY_FORMAT = "{0,number,integer}" + DELIM + "{1,number,integer}";
	
	
	/**
	 * Returns this cache's sequence manager
	 * @return the sequenceManager
	 */
	public SequenceManager getSequenceManager();
	
	/**
	 * Returns this cache's persisted indicators
	 * @return this cache's persisted indicators
	 */
	public PersistedIndicators getPersistedIndicators();
	
	/**
	 * Returns the host keyed by the passed host name
	 * @param hostName the name of host to retrieve from cache
	 * @return the cached host or null if one was not found
	 */
	public Host getHost(String hostName);
	
	
	/**
	 * Caches the passed host
	 * @param host The host to cache
	 */
	public void putHost(Host host);
	/**
	 * Resolves the passed host name, creates a host and caches it.
	 * @param hostName The host name
	 * @return the created and cached host
	 */
	public Host putHost(String hostName);
	
	/**
	 * Returns the agent keyed by the passed host name and agent name
	 * @param hostName The host name where the agent resides 
	 * @param agentName The name of the agent
	 * @return the cached agent or null if one was not found
	 */
	public Agent getAgent(String hostName, String agentName);
	
	
	/**
	 * Caches the passed agent
	 * @param agent the agent to cache
	 */
	public void putAgent(Agent agent);
	
	/**
	 * Builds, caches and returns an agent from the passed host and agent name.
	 * @param hostName The name of the host
	 * @param agentName The name of the agent
	 */
	public Agent putAgent(String hostName, String agentName);
	
	/**
	 * Returns the metric keyed by the passed full metric name
	 * @param fullName The full name of the metric
	 * @return the cached metric or null if one was not found
	 */
	public Metric getMetric(String fullName);
	/**
	 * Caches the passed metric
	 * @param metric The metric to cache
	 */
	public void putMetric(Metric metric);
	
	/**
	 * Builds, caches and returns a metric from the passed fullName and metric type.
	 * @param fullName The metric full name
	 * @param type The metric type
	 * @return the build metric
	 */
	public Metric putMetric(String fullName, MetricType type);
	
	/**
	 * Returns the AgentMetric for the passed Agent and Metric.
	 * @param agent The agent associated to the AgentMetric
	 * @param metric The metric associated to the AgentMetric
	 * @return the cached AgentMetric or null if one was not found
	 */
	public AgentMetric getAgentMetric(Agent agent, Metric metric);
	/**
	 * Caches the passed AgentMetric
	 * @param agentMetric The AgentMetric to cache
	 */
	public void putAgentMetric(AgentMetric agentMetric);
	
	/**
	 * Builds, caches and returns an AgentMetric from the passed agent and metric.
	 * @param agent The agent that produced the agentmetric
	 * @param metric the metric that the agent produced
	 * @return the created agentmetric
	 */
	public AgentMetric putAgentMetric(Agent agent, Metric metric);	
	
	/**
	 * Creates a new hash
	 * @param name The name of the hash
	 * @param size The initial capacity
	 */
	public void addHash(String name, int size);
	/**
	 * Creates a new hash
	 * @param name The name of the hash
	 */
	public void addHash(String name);
	/**
	 * Adds a value to the named hash. If the named hash does not exist, it will be created.
	 * @param name The hash name
	 * @param value The value to add
	 */
	public void addValue(String name, long value);
	/**
	 * Determines if the named hash contains the passed value
	 * @param name The name of the hash
	 * @param value The value to check for 
	 * @return true if the named hash contains the passed value
	 */
	public boolean contains(String name, long value);
	/**
	 * Determines if the passed hash name exists in the hashSet
	 * @param name The hash name
	 * @return true if the named hash exists, false if it does not
	 */
	public boolean exists(String name);
	
	/**
	 * Returns the host with the passed id
	 * @param id 
	 * @return the requested object or null if it was not in cache
	 */
	public Host getHost(long id);

	/**
	 * Returns the agent with the passed id
	 * @param id 
	 * @return the requested object or null if it was not in cache
	 */
	public Agent getAgent(long id);
	
	/**
	 * Returns the metric with the passed id
	 * @param id 
	 * @return the requested object or null if it was not in cache
	 */
	public Metric getMetric(long id);

	/**
	 * Returns the agentMetric with the passed id
	 * @param id 
	 * @return the requested object or null if it was not in cache
	 */
	public AgentMetric getAgentMetric(long id);
	
	/**
	 * Clears the trace model cache
	 */
	public void clear();
	
	public void flagAsSaved(Object obj);

	
}
