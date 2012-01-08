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

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.sequence.SequenceManager;
import org.helios.tracing.core.trace.Agent;
import org.helios.tracing.core.trace.AgentMetric;
import org.helios.tracing.core.trace.Host;
import org.helios.tracing.core.trace.Metric;
import org.helios.tracing.trace.MetricType;

/**
 * <p>Title: DefaultTraceModelCacheImpl</p>
 * <p>Description: A default TraceModelCache implementation based on simple concurrent hash maps.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.cache.DefaultTraceModelCacheImpl</code></p>
 */

public class DefaultTraceModelCacheImpl extends AbstractTraceModelCache {
	/** A cache of hosts keyed by host name */
	protected final Map<String, Host> hosts = new ConcurrentHashMap<String, Host>(128);
	/** A cache of agents keyed by host/agent name */
	protected final Map<String, Agent> agents = new ConcurrentHashMap<String, Agent>(256);
	/** A cache of metrics keyed by metric name */
	protected final Map<String, Metric> metrics = new ConcurrentHashMap<String, Metric>(1024);	
	/** A cache of agent-metrics keyed by agent/metric name */
	protected final Map<String, AgentMetric> agentMetrics = new ConcurrentHashMap<String, AgentMetric>(1024);
	/** A cache of hosts keyed by host id */
	protected final Map<Long, Host> hostIds = new ConcurrentHashMap<Long, Host>(128);
	/** A cache of agents keyed by host/agent id */
	protected final Map<Long, Agent> agentIds = new ConcurrentHashMap<Long, Agent>(256);
	/** A cache of metrics keyed by metric id */
	protected final Map<Long, Metric> metricIds = new ConcurrentHashMap<Long, Metric>(1024);	
	/** A cache of agent-metrics keyed by agent/metric id */
	protected final Map<Long, AgentMetric> agentMetricIds = new ConcurrentHashMap<Long, AgentMetric>(1024);
	
	
	
	/**
	 * Clears the trace model cache
	 */
	public void clear() {
		hosts.clear();
		agents.clear();
		metrics.clear();
		agentMetrics.clear();
		hostIds.clear();
		agentIds.clear();
		metricIds.clear();
		agentMetricIds.clear();
		persisted.clear();
	}
	
	
	/**
	 * Creates a new DefaultTraceModelCacheImpl
	 * @param sequenceManager The sequence manager that will generate ID keys for new entities added to cache
	 */
	public DefaultTraceModelCacheImpl(SequenceManager sequenceManager) {
		super(sequenceManager);
	}
	
	public void flagAsSaved(Object obj) {
		if(obj!=null) {
			Class<?> clazz = obj.getClass();
			if(AgentMetric.class.equals(clazz)) {
				putAgentMetric((AgentMetric)obj);
				persisted.addValue("AgentMetric", ((AgentMetric)obj).getId());
			} else if(Metric.class.equals(clazz)) {
				putMetric((Metric)obj);
				persisted.addValue("Metric", ((Metric)obj).getId());				
			} else if(Agent.class.equals(clazz)) {
				putAgent((Agent)obj);
				persisted.addValue("Agent", ((Agent)obj).getId());				
			} else if(Host.class.equals(clazz)) {
				putHost((Host)obj);
				persisted.addValue("Host", ((Host)obj).getId());								
			}
		}
	}
	
	
	
	/**
	 * Returns the host keyed by the passed host name
	 * @param hostName the name of host to retrieve from cache
	 * @return the cached host or null if one was not found
	 */
	public Host getHost(String hostName) {
		if(hostName==null||hostName.trim().length()<1) return null;
		return hosts.get(hostName.trim());
	}
	/**
	 * Caches the passed host
	 * @param host The host to cache
	 */
	public void putHost(Host host) {
		hosts.put(host.getName(), host);
		hostIds.put(host.getId(), host);
	}
	
	/**
	 * Resolves the passed host name, creates a host and caches it.
	 * @param hostName The host name
	 * @return the created and cached host
	 */
	public Host putHost(String hostName) {
		if(hostName==null||hostName.trim().length()<1) return null;
		if(hosts.containsKey(hostName.trim())) return getHost(hostName);
		synchronized(hosts) {
			if(hosts.containsKey(hostName.trim())) return getHost(hostName);
			Host host = 
				new Host(sequenceManager.getSequence(Host.SEQUENCE_KEY).next(), 
				this.resolveHostName(hostName), 
				this.resolveHostAddress(hostName), 
				this.resolveHostFullName(hostName));
			putHost(host);
			return host;
		}		
	}
	/**
	 * Returns the agent keyed by the passed host name and agent name
	 * @param hostName The host name where the agent resides 
	 * @param agentName The name of the agent
	 * @return the cached agent or null if one was not found
	 */
	public Agent getAgent(String hostName, String agentName) {
		if(hostName==null||hostName.trim().length()<1) return null;
		if(agentName==null||agentName.trim().length()<1) return null;
		return agents.get(new StringBuilder(hostName.trim()).append(DELIM).append(agentName.trim()).toString());
	}
	/**
	 * Caches the passed agent
	 * @param agent the agent to cache
	 */
	public void putAgent(Agent agent) {
		if(agent!=null) {
			agents.put(new StringBuilder(agent.getHost().getName()).append(DELIM).append(agent.getName()).toString(), agent);
		}
		agentIds.put(agent.getId(), agent);
	}
	
	/**
	 * Builds, caches and returns an agent from the passed host and agent name.
	 * @param hostName The name of the host
	 * @param agentName The name of the agent
	 */
	public Agent putAgent(String hostName, String agentName) {
		if(hostName==null||hostName.trim().length()<1) return null;
		if(agentName==null||agentName.trim().length()<1) return null;
		Agent agent = getAgent(hostName, agentName);
		if(agent==null) {
			synchronized(agents) {
				agent = getAgent(hostName, agentName);
				Host host = getHost(hostName);
				if(agent==null) {
					agent = new Agent(sequenceManager.getSequence(Agent.SEQUENCE_KEY).next(), host, agentName);
					putAgent(agent);
				}
			}
		}
		return agent;
	}
	
	/**
	 * Returns the metric keyed by the passed full metric name
	 * @param fullName The full name of the metric
	 * @return the cached metric or null if one was not found
	 */
	public Metric getMetric(String fullName) {
		if(fullName==null||fullName.trim().length()<1) return null;
		return metrics.get(fullName.trim());
	}
	
	/**
	 * Caches the passed metric
	 * @param metric The metric to cache
	 */
	public void putMetric(Metric metric) {
		if(metric!=null) {
			metrics.put(metric.getFullName(), metric);
		}
		metricIds.put(metric.getId(), metric);
	}
	
	/**
	 * Builds, caches and returns a metric from the passed fullName and metric type.
	 * @param fullName The metric full name
	 * @param type The metric type
	 * @return the build metric
	 */
	public Metric putMetric(String fullName, MetricType type) {
		if(fullName==null||fullName.trim().length()<1) return null;
		Metric metric = metrics.get(fullName.trim());
		if(metric!=null) {
			if(type==null) {
				if(!metric.getType().equals(type)) throw new RuntimeException("Metric Type Collision for [" + fullName + "]. Registered as [" + metric.getType() + "] but received [" + type + "]", new Throwable());
			}
			return metric;
		} else {
			if(type==null) return null;
			synchronized(metrics) {
				metric = metrics.get(fullName.trim());
				if(metric==null) {
					metric = new Metric(sequenceManager.getSequence(Metric.SEQUENCE_KEY).next(), type.getCode(), fullName.trim());
					putMetric(metric);
				}
			}
		}
		return metric;
	}
	
	
	/**
	 * Returns the AgentMetric for the passed Agent and Metric.
	 * @param agent The agent associated to the AgentMetric
	 * @param metric The metric associated to the AgentMetric
	 * @return the cached AgentMetric or null if one was not found
	 */
	public AgentMetric getAgentMetric(Agent agent, Metric metric) {
		if(agent==null) throw new RuntimeException("Passed agent was null", new Throwable());
		if(metric==null) throw new RuntimeException("Passed metric was null", new Throwable());
		return agentMetrics.get(MessageFormat.format(AGENT_METRIC_KEY_FORMAT, agent.getId(), metric.getId()));
	}

	/**
	 * Caches the passed AgentMetric
	 * @param agentMetric The AgentMetric to cache
	 */
	public void putAgentMetric(AgentMetric agentMetric) {
		agentMetrics.put(MessageFormat.format(AGENT_METRIC_KEY_FORMAT, agentMetric.getAgent().getId(), agentMetric.getMetric().getId()), agentMetric);
		agentMetricIds.put(agentMetric.getId(), agentMetric);
	}
	
	/**
	 * Builds, caches and returns an AgentMetric from the passed agent and metric.
	 * @param agent The agent that produced the agentmetric
	 * @param metric the metric that the agent produced
	 * @return the created agentmetric
	 */
	public AgentMetric putAgentMetric(Agent agent, Metric metric) {
		if(agent==null) throw new RuntimeException("Passed agent was null", new Throwable());
		if(metric==null) throw new RuntimeException("Passed metric was null", new Throwable());
		String key = MessageFormat.format(AGENT_METRIC_KEY_FORMAT, agent.getId(), metric.getId());
		AgentMetric agentMetric = agentMetrics.get(key);
		if(agentMetric==null) {
			synchronized(agentMetrics) {
				agentMetric = agentMetrics.get(key);
				if(agentMetric==null) {
					agentMetric = new AgentMetric(sequenceManager.getSequence(AgentMetric.SEQUENCE_KEY).next(), agent, metric);
					putAgentMetric(agentMetric);
				}
			}
		}
		return agentMetric;
	}

	/**
	 * Returns the host with the passed id
	 * @param id 
	 * @return the requested object or null if it was not in cache
	 */
	public Host getHost(long id) {
		return hostIds.get(id);
	}

	/**
	 * Returns the agent with the passed id
	 * @param id 
	 * @return the requested object or null if it was not in cache
	 */
	public Agent getAgent(long id) {
		return agentIds.get(id);
	}
	
	/**
	 * Returns the metric with the passed id
	 * @param id 
	 * @return the requested object or null if it was not in cache
	 */
	public Metric getMetric(long id) {
		return metricIds.get(id);
	}

	/**
	 * Returns the agentMetric with the passed id
	 * @param id 
	 * @return the requested object or null if it was not in cache
	 */
	public AgentMetric getAgentMetric(long id) {
		return agentMetricIds.get(id);
	}

}
