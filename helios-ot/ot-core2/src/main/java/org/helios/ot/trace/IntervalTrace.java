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


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.xml.bind.annotation.XmlRootElement;

import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.trace.types.interval.IIntervalTraceValue;
import org.helios.ot.type.MetricType;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamInclude;


/**
 * <p>Title: IntervalTrace</p>
 * <p>Description: Extension of {@link Trace} to capture interval aggregated detail.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.IntervalTrace</code></p>
 */
@XStreamAlias("intervalTrace")
@XmlRootElement(name="intervalTrace")
public class IntervalTrace<I extends IIntervalTraceValue<T>, T extends ITraceValue> extends Trace<T> {
	/** The interval trace value */
	@XStreamAlias("value")
	protected I intervalTraceValue = null;
	/** The timestamp of the end of the interval */
	@XStreamAlias("endTimeStamp")
	protected long endTimeStamp = -1L;
	/** The number of intervals this object has been flushed in */
	@XStreamAlias("flushCount")
	protected long flushCount = 0;
	

	/**
	 * Creates a new Trace with an option to make it an interval trace
	 * @param interval true if this is an interval trace,
	 */
	public IntervalTrace() {
		super();		
	}
	
	/**
	 * Retrieves the primary value
	 * @return the primary value 
	 */
	public Object getValue() {
		return intervalTraceValue.getValue();
	}
	
	/**
	 * Retrieves the TraceValue
	 * @return the TraceValue
	 */
	public I getIntervalTraceValue() {
		return intervalTraceValue;
	}
	
	
	
	/**
	 * Retrieves the value as a string
	 * @return the value as a string
	 */
	public String getStringValue() {
		return intervalTraceValue.getValue().toString();
	}


	
	/**
	 * Creates a new interval trace for accumulating intervals of traces.
	 * @param type The MetricType of the interval trace
	 * @param fullMetricName The full metric name of the interval trace
	 * @param The start time of the current interval
	 * @return a new interval trace
	 */
	@SuppressWarnings("unchecked")
	public static IntervalTrace<?, IIntervalTraceValue<?>> intervalTrace(Trace trace, long startTime) {
		MetricType type = trace.getMetricType();
		String fullMetricName = trace.getFQN();		
		IntervalTrace<?, IIntervalTraceValue<?>> intervalTrace = new IntervalTrace();
		intervalTrace.metricId = MetricId.getInstance(type, fullMetricName);
		intervalTrace.timeStamp = startTime;
		intervalTrace.anyPhaseTriggers = trace.anyPhaseTriggers;
		if(intervalTrace.anyPhaseTriggers) {
			intervalTrace.phaseTriggerSignature = trace.phaseTriggerSignature;
			intervalTrace.phaseTriggers.putAll(trace.getPhaseTriggers());
		}
		
		//trace.intervalTraceValue = (?)type.intervalTraceValue();
		return intervalTrace;
	}
	
	
	/**
	 * Aggregates a trace instance into this interval trace
	 * @param trace The trace to apply
	 */
	public void apply(Trace<T> trace) {
		if(trace==null) throw new IllegalArgumentException("The passed trace was null", new Throwable());
		if(intervalTraceValue==null) {
			intervalTraceValue = (I)this.metricId.type.intervalTraceValue();
		}
		intervalTraceValue.apply(trace.getTraceValue());
		if(phaseTriggerSignature != trace.phaseTriggerSignature) {
			phaseTriggers.clear();
			anyPhaseTriggers = trace.anyPhaseTriggers;
			if(anyPhaseTriggers) {
				phaseTriggerSignature = trace.phaseTriggerSignature;
				phaseTriggers.putAll(trace.getPhaseTriggers());
			}			
		}
	}
	
	/**
	 * At the end of each interval, this is called to close, clone and reset the Interval Trace
	 * @param intervalEndTimestamp The timestamp passed by the flusher indicating the end of the current interval
	 * @return the cloned Trace
	 */
	public IntervalTrace<I, T> cloneReset(long intervalEndTimestamp) {
		endTimeStamp = intervalEndTimestamp;
		IntervalTrace<I, T> trace = new IntervalTrace(this);
		timeStamp = intervalEndTimestamp;
		endTimeStamp = -1L;
		
		return trace;
	}

	/**
	 * The timestamp of the end of the interval
	 * @return the endTimeStamp
	 */
	public long getEndTimeStamp() {
		return endTimeStamp;
	}

	/**
	 * Copy Constructor
	 * @param intervalTrace a <code>IntervalTrace</code> object
	 */
	private IntervalTrace(IntervalTrace intervalTrace) {
		super(intervalTrace);
	    this.intervalTraceValue = (I) intervalTrace.intervalTraceValue.cloneReset(metricId.getType());
	    this.endTimeStamp = intervalTrace.endTimeStamp;
		anyPhaseTriggers = intervalTrace.anyPhaseTriggers;
		if(anyPhaseTriggers) {
			phaseTriggerSignature = intervalTrace.phaseTriggerSignature;
			phaseTriggers.putAll(intervalTrace.getPhaseTriggers());
		}

	}
	
	/**
	 * Generates a String representation of the Trace.
	 * @return A string.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder(metricId.toString());
		buff.append(VALUE_DELIM).append(intervalTraceValue.toString());
		buff.append("(").append(timeStamp).append(")");
		return buff.toString();		
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(intervalTraceValue);
		out.writeLong(endTimeStamp);
		out.writeLong(flushCount);
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		intervalTraceValue = (I)in.readObject();
		endTimeStamp = in.readLong();
		flushCount = in.readLong();
	}
	

}
