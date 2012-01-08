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
import java.sql.Types;
import java.util.Date;

import org.helios.tracing.core.trace.annotations.persistence.Store;
import org.helios.tracing.core.trace.annotations.persistence.StoreField;

/**
 * <p>Title: TraceValue</p>
 * <p>Description: TraceModel representation of a unique instance measurement for an agent metric</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.TraceValue</code></p>
 */
@Store(store="TRACE_VALUE")
public abstract class TraceValue implements Serializable {
	/**  */
	private static final long serialVersionUID = -4735553724367866842L;
	/** The agent metric of the trace value */
	protected final AgentMetric agentMetric;
	/** The collection timestamp of the trace value */
	protected final Date valueTimestamp;
	/** Indicates if the trace is urgent */
	protected boolean urgent = false;
	/** Indicates if the trace is temporal */
	protected boolean temporal = false;
	
	/**
	 * Creates a new TraceValue that is not urgent or temporal
	 * @param agentMetric The agent metric that this value is associated to
	 * @param timestamp The collection timestamp of the trace value
	 * @param value The value The value of the measurement
	 */
	public TraceValue(AgentMetric agentMetric, long timestamp, Object value) {
		this(agentMetric, timestamp, value, false, false);
	}
	
	/**
	 * Creates a new TraceValue
	 * @param agentMetric The agent metric that this value is associated to
	 * @param timestamp The collection timestamp of the trace value
	 * @param value The value The value of the measurement
	 * @param urgent Indicates if the trace is urgent
	 * @param temporal Indicates if the trace is temporal
	 */
	public TraceValue(AgentMetric agentMetric, long timestamp, Object value, boolean urgent, boolean temporal) {
		if(agentMetric==null) throw new RuntimeException("Passed agentMetric was null", new Throwable());
		if(value==null) throw new RuntimeException("Passed value was null", new Throwable());
		this.agentMetric = agentMetric;
		this.valueTimestamp = new Date(timestamp);
		this.urgent = urgent;
		this.temporal = temporal;
	}
	
	// SELECT AGENT_METRIC_ID,VALUE,VALUE_DATE FROM TRACE_VALUE
	
	/**
	 * The agent metric of the trace value
	 * @return the agentMetric
	 */
	@StoreField(name="AGENT_METRIC_ID", type=Types.BIGINT, fk=true)
	public AgentMetric getAgentMetric() {
		return agentMetric;
	}
	/**
	 * The collection timestamp of the trace value
	 * @return the valueTimestamp
	 */
	@StoreField(name="VALUE_DATE", type=Types.TIMESTAMP)
	public Date getValueTimestamp() {
		return valueTimestamp;
	}
	
	/**
	 * Returns the collection timestamp of the trace value as a long UDT
	 * @return the collection timestamp of the trace value as a long UDT
	 */
	public long getValueTimestampUDT() {
		return valueTimestamp.getTime();
	}
	
	/**
	 * Returns the value of the trace
	 * @return the value of the trace
	 */
	@StoreField(name="VALUE", type=Types.NUMERIC)
	public abstract Object getValue();
	/**
	 * @return the urgent
	 */
	public boolean isUrgent() {
		return urgent;
	}
	/**
	 * @param urgent the urgent to set
	 */
	public void setUrgent(boolean urgent) {
		this.urgent = urgent;
	}
	/**
	 * @return the temporal
	 */
	public boolean isTemporal() {
		return temporal;
	}
	/**
	 * @param temporal the temporal to set
	 */
	public void setTemporal(boolean temporal) {
		this.temporal = temporal;
	}

	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((agentMetric == null) ? 0 : agentMetric.hashCode());
		result = prime * result
				+ ((valueTimestamp == null) ? 0 : valueTimestamp.hashCode());
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
		TraceValue other = (TraceValue) obj;
		if (agentMetric == null) {
			if (other.agentMetric != null)
				return false;
		} else if (!agentMetric.equals(other.agentMetric))
			return false;
		if (valueTimestamp == null) {
			if (other.valueTimestamp != null)
				return false;
		} else if (!valueTimestamp.equals(other.valueTimestamp))
			return false;
		return true;
	}
	
	
}
