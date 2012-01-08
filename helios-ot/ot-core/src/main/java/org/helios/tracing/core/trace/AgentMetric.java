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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.helios.tracing.core.trace.annotations.persistence.PK;
import org.helios.tracing.core.trace.annotations.persistence.Store;
import org.helios.tracing.core.trace.annotations.persistence.StoreField;
import org.helios.tracing.core.trace.cache.TraceModelCache;
import org.helios.tracing.trace.MetricType;

/**
 * <p>Title: AgentMetric</p>
 * <p>Description: TraceModel representation of a unique metric for a specific agent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.AgentMetric</code></p>
 */
@Store(store="AGENT_METRIC")
public class AgentMetric implements Serializable {
	/**  */
	private static final long serialVersionUID = -4589244702930529802L;
	/** The unique helios agentMetric Id */
	protected long id;
	/** The agent that produces this metric */
	protected Agent agent;
	/** The agent metric namespace */
	protected String[] namespace;
	/** The metric that the agent traces */
	protected Metric metric;
	/** The fully qualified metric */
	protected String name;
	/** The timestamp that this agent-metric was first seen */
	protected Date firstSeen;
	/** The timestamp that this agent-metric was last seen */
	protected Date lastSeen;
	
	/** The sequence name for the agent-metric id */
	public static final String SEQUENCE_KEY = "HELIOS_AGENTMETRIC_ID";

	/**
	 * Creates a new AgentMetric 
	 * @param id The designated unique Helios AgentMetric id.
	 * @param agent The agent that produced the AgentMetric
	 * @param metric The metric that the agent produced
	 */
	public AgentMetric(long id, Agent agent, Metric metric) {
		super();
		this.id = id;
		this.agent = agent;
		this.metric = metric;
		name = new StringBuilder(agent.getHost().getName()).append(Metric.NAME_DELIM).append(agent.getName()).append(Metric.NAME_DELIM).append(metric.getFullName()).toString();
		String[] frags = name.split(Metric.NAME_DELIM);
		namespace = new String[frags.length-1];
		System.arraycopy(frags, 0, namespace, 0, frags.length-1);
		firstSeen = new Date();
		lastSeen = firstSeen;
	}
	
	/**
	 * Creates a new AgentMetric from a result set and the sql <code>SELECT AGENT_ID,AGENT_METRIC_ID,FIRST_SEEN,LAST_SEEN,METRIC_ID FROM AGENT_METRIC</code>. 
	 * @param rset The pre-navigated result set
	 * @param cache The cache where the agent and metric will be retrieved from
	 * @throws SQLException 
	 */
	public AgentMetric(ResultSet rset, TraceModelCache cache) throws SQLException {
		int i = 0;
		long agentId = rset.getLong(++i);
		id = rset.getLong(++i);
		firstSeen= new Date(rset.getTimestamp(++i).getTime());
		lastSeen= new Date(rset.getTimestamp(++i).getTime());
		long metricId = rset.getLong(++i);
		agent = cache.getAgent(agentId);
		metric = cache.getMetric(metricId);
		cache.putAgentMetric(this);
		cache.addValue("AgentMetric", id);
		
	}
	
	/**
	 * The unique Helios agent-metric id 
	 * @return the id
	 */
	@PK
	@StoreField(name="AGENT_METRIC_ID", type=Types.BIGINT)
	public long getId() {
		return id;
	}

	


	/**
	 * The timestamp that this agent-metric was last seen
	 * @return the lastSeen
	 */
	@StoreField(name="LAST_SEEN", type=Types.TIMESTAMP)
	public Date getLastSeen() {
		return lastSeen;
	}
	/**
	 * Sets the last seen date
	 * @param lastSeen the lastSeen to set
	 */
	public void setLastSeen(Date lastSeen) {
		this.lastSeen = lastSeen;
	}
	
	/**
	 * The timestamp that this agent-metric was first seen
	 * @return the firstSeen
	 */
	@StoreField(name="FIRST_SEEN", type=Types.TIMESTAMP)
	public Date getFirstSeen() {
		return firstSeen;
	}
	/**
	 * The agent that produces this metric
	 * @return the agent
	 */
	@StoreField(name="AGENT_ID", type=Types.BIGINT, fk=true)
	public Agent getAgent() {
		return agent;
	}
	
	/**
	 * The metric that the agent traces 
	 * @return the metric
	 */
	@StoreField(name="METRIC_ID", type=Types.BIGINT, fk=true)
	public Metric getMetric() {
		return metric;
	}
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("AgentMetric [")
	        .append(TAB).append("agent = ").append(this.agent.getHost().getName()).append("/").append(this.agent.getName())
	        .append(TAB).append("metric = ").append(this.metric.getFullName()).append("/").append(this.metric.getTypeName())
	        .append(TAB).append("firstSeen = ").append(this.firstSeen)
	        .append(TAB).append("lastSeen = ").append(this.lastSeen)
	        .append("\n]");    
	    return retValue.toString();
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the namespace
	 */
	public String[] getNamespace() {
		return namespace;
	}
	
	/**
	 * Creates a new TraceValue for this AgentMetric that is not urgent or temporal
	 * @param timestamp The timestamp of the value
	 * @param value The value
	 * @return the new TraceValue
	 */
	public TraceValue getTrace(long timestamp, Object value) {
		return getTrace(timestamp, value, false, false);
	}

	/**
	 * Creates a new TraceValue for this AgentMetric
	 * @param timestamp The timestamp of the value
	 * @param value The value
	 * @param urgent true if the metric is urgent
	 * @param temporal true if the metric is temporal
	 * @return the new TraceValue
	 */	
	public TraceValue getTrace(long timestamp, Object value, boolean urgent, boolean temporal) {
		if(value==null) throw new RuntimeException("Passed value was null", new Throwable());
		MetricType type = metric.getType();
		switch (type.getCode()) {
			case MetricType.TYPE_DELTA_INT_AVG:
			case MetricType.TYPE_INT_AVG:
			case MetricType.TYPE_STICKY_DELTA_INT_AVG:
			case MetricType.TYPE_STICKY_INT_AVG:
			case MetricType.TYPE_INTERVAL_INCIDENT:
				return new IntTraceValue(this, timestamp, value, urgent, temporal);
			case MetricType.TYPE_DELTA_LONG_AVG:
			case MetricType.TYPE_LONG_AVG:
			case MetricType.TYPE_STICKY_DELTA_LONG_AVG:
			case MetricType.TYPE_STICKY_LONG_AVG:
				return new LongTraceValue(this, timestamp, value, urgent, temporal);
			case MetricType.TYPE_STRING:
				return new StringTraceValue(this, timestamp, value, urgent, temporal);
			case MetricType.TYPE_STRINGS:
				return new StringsTraceValue(this, timestamp, value, urgent, temporal);
			case MetricType.TYPE_TIMESTAMP:
				return new TimestampTraceValue(this, timestamp, value, urgent, temporal);
			default:
				throw new IllegalArgumentException("Programmer Error. The type [" + type + "] is not supported.", new Throwable());
		}
	}


	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
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
		AgentMetric other = (AgentMetric) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	
}
