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
package org.helios.ot.trace;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.trace.types.TraceValueType;
import org.helios.ot.type.MetricType;

//import com.thoughtworks.xstream.annotations.XStreamAlias;
//import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * <p>Title: ClosedTrace</p>
 * <p>Description: Represents a closed and read only Trace.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.ClosedTrace</code></p>
 */
//@XStreamAlias("closedTrace")
@XmlRootElement(name="closedTrace")
@SuppressWarnings("unchecked")
public class ClosedTrace implements Externalizable {
	/** The metric identifier */
	//@XStreamAlias("metricId")
	@XmlElement(name="metricId")
	protected MetricId metricId;
	/** The original trace start timestamp */
	//@XStreamAlias("startTimestamp")
	@XmlElement(name="startTimestamp")
	protected long startTimestamp;
	
	/** The type of the trace value */
	//@XStreamAlias("traceValueType")
	@XmlElement(name="traceValueType")
	protected TraceValueType traceValueType;
	/** Indicates if the trace is an interval trace */
	//@XStreamAlias("interval")
	@XmlElement(name="interval")
	protected boolean interval;
	/** Indicates if the trace is a MinMaxAvg trace */
	//@XStreamAlias("minmaxavg")
	@XmlElement(name="minmaxavg")
	protected boolean minMaxAvg;
	
	/** The value recorded in the trace */
	//@XStreamAlias("value")
	@XmlElement(name="value")
	protected Object value;
	/** The trace urgent flag */
	//@XStreamAlias("urgent")
	@XmlElement(name="urgent")
	protected boolean urgent;
	/** The trace temporal flag */
	//@XStreamAlias("temporal")
	@XmlElement(name="temporal")
	protected boolean temporal;
	
	/** The generic trace value */
	//@XStreamOmitField
	protected ITraceValue traceValue;
	
	/**
	 * Creates a new ClosedTrace. For externalization only.
	 */
	public ClosedTrace() {
		
	}
	
	
	/**
	 * Creates a new ClosedTrace
	 * @param trace The trace to create the closed trace from
	 */
	
	protected ClosedTrace(Trace trace) {
		if(trace==null) throw new IllegalArgumentException("The passed trace was null", new Throwable());
		metricId = trace.getMetricId();
		startTimestamp = trace.getTimeStamp();
		traceValue = trace.getTraceValue();
		interval = traceValue.isInterval();
		traceValueType = traceValue.getTraceValueType();
		minMaxAvg = traceValue.getTraceValueType().isMinMaxAvg();
		value = traceValue.getValue();
		temporal = trace.isTemporal();
		urgent = trace.isUrgent();
	}

	
	
	/**
	 * Creates a new ClosedTrace
	 * @param trace The trace to create the closed trace from
	 * @return a new ClosedTrace
	 */
	public static ClosedTrace newClosedTrace(Trace trace) {
		if(nvl(trace).isInterval()) {
			if(trace.getTraceValue().getTraceValueType().isMinMaxAvg()) {
				return new ClosedMinMaxAvgTrace((IntervalTrace)trace);
			}
			return new ClosedIntervalTrace((IntervalTrace)trace);
		}
		return new ClosedTrace(trace);
	}
	
	/**
	 * Creates a collection of new ClosedTraces
	 * @param traces The traces to create the closed trace collection from
	 * @return a collection of new ClosedTraces
	 */ 
	public static Collection<ClosedTrace> newClosedTrace(Trace...traces) {
		if(traces==null || traces.length<1) return Collections.emptySet();
		List<ClosedTrace> list = new ArrayList<ClosedTrace>(traces.length);
		for(Trace trace: traces) {
			if(trace==null) continue;
			list.add(newClosedTrace(trace));
		}
		return list;
	}
	
	/**
	 * Creates an array of new ClosedTraces
	 * @param traces The traces to create the closed trace collection from
	 * @return an array of new ClosedTraces
	 */ 
	public static ClosedTrace[] newClosedTraceArray(Trace...traces) {
		Collection<ClosedTrace> cts = newClosedTrace(traces);
		return cts.toArray(new ClosedTrace[cts.size()]);
	}
	
	/**
	 * Creates an array of new ClosedTraces
	 * @param traces The traces to create the closed trace collection from
	 * @return an array of new ClosedTraces
	 */ 
	public static ClosedTrace[] newClosedTraceArray(Set<? extends Trace<? extends ITraceValue>> traces) {
		if(traces==null || traces.isEmpty()) return new ClosedTrace[0];
		Collection<ClosedTrace> cts = newClosedTrace(traces.toArray(new Trace[traces.size()]));
		return cts.toArray(new ClosedTrace[cts.size()]);
	}
	
	/**
	 * Renders the trace as a name/value map
	 * @return A map of trace attributes keyed by header constant name
	 */
	@XmlTransient
	public Map<String, Object> getTraceMap() {
		Map<String, Object> map = new HashMap<String, Object>(16);
		map.putAll(metricId.getTraceMap());
		map.put(Trace.TRACE_TS, startTimestamp);
		map.put(Trace.TRACE_DATE, new Date(startTimestamp).toString());
		map.put(Trace.TRACE_SVALUE, traceValue.toString());
		map.put(Trace.TRACE_VALUE, traceValue.getValue());
		map.put(Trace.TRACE_TEMPORAL, temporal);
		map.put(Trace.TRACE_URGENT, urgent);
		map.put(Trace.TRACE_MODEL, false);
		return map;
	}


	/**
	 * Returns the Agent name
	 * @return the Agent name
	 * @see org.helios.ot.trace.MetricId#getAgentName()
	 */
	public String getAgentName() {
		return metricId.getAgentName();
	}

	/**
	 * Returns the fully qualified metric name
	 * @return the fully qualified metric name
	 * @see org.helios.ot.trace.MetricId#getFQN()
	 */
	public String getFQN() {
		return metricId.getFQN();
	}

	/**
	 * Returns the host name
	 * @return the host name
	 * @see org.helios.ot.trace.MetricId#getHostName()
	 */
	public String getHostName() {
		return metricId.getHostName();
	}

	/**
	 * Returns the local metric name (the metric name without the host/agent)
	 * @return the local metric name
	 * @see org.helios.ot.trace.MetricId#getLocalName()
	 */
	public String getLocalName() {
		return metricId.getLocalName();
	}


	/**
	 * Returns the metric point
	 * @return the metric point
	 * @see org.helios.ot.trace.MetricId#getMetricName()
	 */
	public String getMetricName() {
		return metricId.getMetricName();
	}

	/**
	 * Returns the metric namespace
	 * @return the metric namespace
	 * @see org.helios.ot.trace.MetricId#getNamespace()
	 */
	public String[] getNamespace() {
		return metricId.getNamespace();
	}


	/**
	 * Returns the metric type
	 * @return the metric type
	 * @see org.helios.ot.trace.MetricId#getType()
	 */
	public MetricType getType() {
		return metricId.getType();
	}

	

	/**
	 * Returns the trace value type
	 * @return the trace value type
	 * @see org.helios.ot.trace.types.ITraceValue#getTraceValueType()
	 */
	public TraceValueType getTraceValueType() {
		return traceValueType;
	}

	/**
	 * Returns the generic value of this trace
	 * @return the generic value of this trace
	 * @see org.helios.ot.trace.types.ITraceValue#getValue()
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Indicates if this is an interval trace
	 * @return true if this is an interval trace, false otherwise
	 * @see org.helios.ot.trace.types.ITraceValue#isInterval()
	 */
	public boolean isInterval() {
		return interval;
	}
	


	/**
	 * Returns the metricID
	 * @return the metricId
	 */
	public MetricId getMetricId() {
		return metricId;
	}


	/**
	 * Returns the trace timestamp, or if an interval, the start time of the interval
	 * @return the trace timestamp
	 */
	public long getStartTimestamp() {
		return startTimestamp;
	}


	/**
	 * Indicates if this is a minMaxAvg trace
	 * @return true if this is a minMaxAvg trace, false otherwise
	 */
	public boolean isMinMaxAvg() {
		return minMaxAvg;
	}



	/**
	 * Indicates if this is an urgent trace
	 * @return true if this is an urgent trace, false otherwise
	 */
	public boolean isUrgent() {
		return urgent;
	}


	/**
	 * Indicates if this is a temporal trace
	 * @return true if this is a temporal trace, false otherwise
	 */
	public boolean isTemporal() {
		return temporal;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		try {
			temporal = in.readBoolean();
			urgent = in.readBoolean();
			startTimestamp = in.readLong();
			metricId = (MetricId)in.readObject();
			traceValue = (ITraceValue)in.readObject();
			
			
			minMaxAvg = traceValue.getTraceValueType().isMinMaxAvg();
			traceValueType = traceValue.getTraceValueType();
			interval = traceValue.isInterval();
			value = traceValue.getValue();
		} catch (Exception e) {
			String msg = "Failed to read externalized ClosedTrace";
			System.err.println(msg);
			throw new RuntimeException(msg, e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {		
		try {
			out.writeBoolean(temporal);
			out.writeBoolean(urgent);			
			out.writeLong(startTimestamp);
			out.writeObject(metricId);
			out.writeObject(traceValue);
		} catch (Exception e) {
			String msg = "Failed to write ClosedTrace to external";
			System.err.println(msg);
			throw new RuntimeException(msg, e);
		}		
	}
	
	/**
	 * Null checks the passed value
	 * @param value The value to check for null
	 * @return The value if not null
	 * @param <T> The type of T
	 */
	protected static <T> T nvl(T value) {
		if(value==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
		return value;
	}


	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("ClosedTrace [")
	        .append(TAB).append("metricId = ").append(this.metricId)
	        .append(TAB).append("startTimestamp = ").append(this.startTimestamp)
	        .append(TAB).append("traceValueType = ").append(this.traceValueType)
	        .append(TAB).append("interval = ").append(this.interval)
	        .append(TAB).append("minMaxAvg = ").append(this.minMaxAvg)
	        .append(TAB).append("value = ").append(this.value)
	        .append(TAB).append("urgent = ").append(this.urgent)
	        .append(TAB).append("temporal = ").append(this.temporal)
	        .append(TAB).append("traceValue = ").append(this.traceValue)
	        .append("\n]");    
	    return retValue.toString();
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((metricId == null) ? 0 : metricId.hashCode());
		result = prime * result
				+ (int) (startTimestamp ^ (startTimestamp >>> 32));
		return result;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClosedTrace other = (ClosedTrace) obj;
		if (metricId == null) {
			if (other.metricId != null)
				return false;
		} else if (!metricId.equals(other.metricId))
			return false;
		if (startTimestamp != other.startTimestamp)
			return false;
		return true;
	}
	
	
}
