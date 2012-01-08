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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: TraceInstance</p>
 * <p>Description: An instance of one measured value from an agent for a metric</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.TraceInstance</code></p>
 */

public class TraceInstance implements Serializable {
	/**  */
	private static final long serialVersionUID = -4542837021496248265L;
	/** The host where the agent is operating */
	protected final Host host;
	/** The agent that produced the trace */
	protected final Agent agent;
	/** The metric type of the trace */
	protected final Metric metric;
	/** The agent's mapping to the metric type */
	protected final AgentMetric agentMetric;
	/** The metric produced by the agent */
	protected final TraceValue traceValue;
	
	/** The delimeter between namespace entries */
	public static final String DELIM = "/";
	/** The delimeter before the metric value */
	public static final String VALUE_DELIM = ":";
	/** The application name property Id */
	public static final String APPLICATION_ID = "org.helios.application.name";
	
	/** The header constant name for the Trace fully qualified name */
	public static final String TRACE_FQN = "fqn";
	/** The header constant name for the Trace point */
	public static final String TRACE_POINT = "point";
	/** The header constant name for the Trace full name (fqn minus host and agent) */
	public static final String TRACE_FULLNAME = "fullname";
	/** The header constant name for the Trace name space segments */
	public static final String TRACE_NAMESPACE = "namespace";
	/** The header constant name for the Trace local namespace (namespace minust host and agent) */
	public static final String TRACE_LNAMESPACE = "lnamespace";
	/** The header constant name for the Trace agent name */
	public static final String TRACE_APP_ID = "agent";
	/** The header constant name for the Trace host */
	public static final String TRACE_HOST = "host";
	/** The header constant name for the Trace type  */
	public static final String TRACE_TYPE = "type";	
	/** The header constant name for the Trace type name */
	public static final String TRACE_TYPE_NAME = "typename";
	/** The header constant name for the Trace type code */
	public static final String TRACE_TYPE_CODE = "typecode";
	/** The header constant name for the Trace timestamp */
	public static final String TRACE_TS = "timestamp";
	/** The header constant name for the Trace date */
	public static final String TRACE_DATE = "date";
	/** The header constant name for the Trace value as a string */
	public static final String TRACE_SVALUE = "svalue";
	/** The header constant name for the Trace value in native type */
	public static final String TRACE_VALUE = "value";	
	/** The header constant name for the Trace temporal flag */
	public static final String TRACE_TEMPORAL = "temporal";
	/** The header constant name for the Trace urgent flag */
	public static final String TRACE_URGENT = "urgent";
	/** The header constant name for the Trace model flag */
	public static final String TRACE_MODEL = "model";
	
	/**
	 * Renders the traceInstance as a name/value map
	 * @return A map of traceInstance attributes keyed by header constant name
	 */
	public Map<String, Object> getTraceMap() {
		Map<String, Object> map = new HashMap<String, Object>(16);
		map.put(TRACE_FQN, agentMetric.getName());
		map.put(TRACE_POINT, metric.getName());
		map.put(TRACE_FULLNAME, metric.getFullName());
		map.put(TRACE_NAMESPACE, agentMetric.getNamespace());
		map.put(TRACE_LNAMESPACE, metric.getNamespace());
		map.put(TRACE_APP_ID, agent.getName());
		map.put(TRACE_HOST, agent.getHost().getName());
		map.put(TRACE_TYPE_NAME, metric.getType().name());
		map.put(TRACE_TYPE_CODE, metric.getType().getCode());
		map.put(TRACE_TYPE, metric.getType());
		map.put(TRACE_TS, traceValue.getValueTimestampUDT());
		map.put(TRACE_DATE, traceValue.getValueTimestamp());
		map.put(TRACE_SVALUE, "" + traceValue.getValue());
		map.put(TRACE_VALUE, traceValue.getValue());
		map.put(TRACE_TEMPORAL, traceValue.isTemporal());
		map.put(TRACE_URGENT, traceValue.isUrgent());
		map.put(TRACE_MODEL, true);
		return map;
	}
	
	/**
	 * Creates a new TraceInstance
	 * @param host The host where the agent is operating
	 * @param agent The agent that produced the trace
	 * @param metric The metric type of the trace
	 * @param agentMetric The agent's mapping to the metric type
	 * @param traceValue The metric produced by the agent
	 */
	private TraceInstance(Host host, Agent agent, Metric metric, AgentMetric agentMetric, TraceValue traceValue) {
		this.host = host;
		this.agent = agent;
		this.metric = metric;
		this.agentMetric = agentMetric;
		this.traceValue = traceValue;
	}
	
	
	
	/**
	 * Returns a new TraceInstance Builder
	 * @return a new TraceInstance Builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}
	
	/**
	 * <p>Title: Builder</p>
	 * <p>Description: A builder class for the TraceInstance</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.tracing.core.trace.TraceInstance.Builder</code></p>
	 */
	public static class Builder {
		/** The host where the agent is operating */
		protected Host host;
		/** The agent that produced the trace */
		protected Agent agent;
		/** The metric type of the trace */
		protected Metric metric;
		/** The agent's mapping to the metric type */
		protected AgentMetric agentMetric;
		/** The metric produced by the agent */
		protected TraceValue traceValue;
		/** Indicates if the trace is urgent */
		protected boolean urgent = false;
		/** Indicates if the trace is temporal */
		protected boolean temporal = false;
		
		
		/**
		 * Builds and returns the TraceInstance
		 * @return the built TraceInstance
		 */
		public TraceInstance build() {
			TraceInstance ti = new TraceInstance(host, agent, metric, agentMetric, traceValue);
			return ti;
		}
		
		/**
		 * Sets the builder trace value and all the associated parent fields
		 * @param traceValue the traceValue to set
		 * @return this builder
		 */
		public Builder traceValue(TraceValue traceValue) {
			this.host = traceValue.getAgentMetric().getAgent().getHost();
			this.agent = traceValue.getAgentMetric().getAgent();
			this.metric = traceValue.getAgentMetric().getMetric();
			this.agentMetric = traceValue.getAgentMetric();
			this.traceValue = traceValue;
			return this;
		}
		
		/**
		 * Sets the urgent flag for the trace instance
		 * @param urgent true if the trace is urgent
		 * @return this builder
		 */
		public Builder urgent(boolean urgent) {
			this.urgent = urgent;
			return this;
		}
		

		/**
		 * Sets the temporal flag for the trace instance
		 * @param urgent true if the trace is temporal
		 * @return this builder
		 */
		public Builder temporal(boolean temporal) {
			this.temporal= temporal;
			return this;
		}
		
		/**
		 * Sets the builder host
		 * @param host the host to set
		 * @return this builder
		 */
		public Builder host(Host host) {
			this.host = host;
			return this;
		}
		
	}

	/**
	 * @return the host
	 */
	public Host getHost() {
		return host;
	}

	/**
	 * @return the agent
	 */
	public Agent getAgent() {
		return agent;
	}

	/**
	 * @return the metric
	 */
	public Metric getMetric() {
		return metric;
	}

	/**
	 * @return the agentMetric
	 */
	public AgentMetric getAgentMetric() {
		return agentMetric;
	}

	/**
	 * @return the traceValue
	 */
	public TraceValue getTraceValue() {
		return traceValue;
	}

	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agent == null) ? 0 : agent.hashCode());
		result = prime * result
				+ ((agentMetric == null) ? 0 : agentMetric.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((metric == null) ? 0 : metric.hashCode());
		result = prime * result
				+ ((traceValue == null) ? 0 : traceValue.hashCode());
		return result;
	}

	/**
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TraceInstance other = (TraceInstance) obj;
		if (agent == null) {
			if (other.agent != null)
				return false;
		} else if (!agent.equals(other.agent))
			return false;
		if (agentMetric == null) {
			if (other.agentMetric != null)
				return false;
		} else if (!agentMetric.equals(other.agentMetric))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (metric == null) {
			if (other.metric != null)
				return false;
		} else if (!metric.equals(other.metric))
			return false;
		if (traceValue == null) {
			if (other.traceValue != null)
				return false;
		} else if (!traceValue.equals(other.traceValue))
			return false;
		return true;
	}
}
