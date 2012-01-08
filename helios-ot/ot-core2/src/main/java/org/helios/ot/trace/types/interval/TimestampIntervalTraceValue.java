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
package org.helios.ot.trace.types.interval;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

import org.helios.ot.trace.types.TraceValueType;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: TimestampIntervalTraceValue</p>
 * <p>Description: Trace value for an interval of timestamp traces</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.interval.TimestampIntervalTraceValue</code></p>
 */

public class TimestampIntervalTraceValue<T extends org.helios.ot.trace.types.TimestampTraceValue> extends LongIntervalTraceValue<T> {
	/** The first timestamp of the interval */
	protected long first = 0L;
	/** The last timestamp of the interval */
	protected long last = 0L;
	
	
	/**
	 * Creates a new TimestampIntervalTraceValue 
	 * @param timestampIntervalTraceValue
	 */
	protected TimestampIntervalTraceValue(TimestampIntervalTraceValue<T> timestampIntervalTraceValue) {
		super(timestampIntervalTraceValue);
	    this.first = timestampIntervalTraceValue.first;
	    this.last = timestampIntervalTraceValue.last;
	}

	/**
	 * Creates a new TimestampIntervalTraceValue
	 * @param traces The initial traces to apply
	 */
	public TimestampIntervalTraceValue(T...traces) {
		super(TraceValueType.INTERVAL_TIMESTAMP_TYPE);
		if(traces!=null) {
			for(T trace: traces) {
				apply(trace);
			}
		}		
	}	
	
	/**
	 * Clones the state of this interval trace value and then resets it's state for the next interval
	 * @param metricType The metric type of the owning trace passed so that the interval value 
	 * can execute the reset with the correct semantics.
	 * @return A clone of this object prior to reset.
	 */
	public TimestampIntervalTraceValue<T> cloneReset(MetricType metricType) {
		if(metricType==null) throw new IllegalArgumentException("Passed MetricType was null", new Throwable());		
		TimestampIntervalTraceValue<T> clone = new TimestampIntervalTraceValue<T>(this);
		super.cloneReset(metricType);
		first = 0;
		last = 0;
		return clone;
	}		
	
	/**
	 * Creates a new TimestampIntervalTraceValue
	 */
	public TimestampIntervalTraceValue() {
		super(TraceValueType.INTERVAL_TIMESTAMP_TYPE);
	}

	/**
	 * Aggregates the passed ITraceValue into this interval trace value
	 * @param value The ITraceValue to apply
	 */
	@Override
	public void apply(T value) {		
		super.apply(value);
		long v = value.getLongValue();
		last = v;
		if(count==1) {
			first = v;
		}
	}
	
	/**
	 * Reads the state of this object in from the Object input stream
	 * @param in the stream to read data from in order to restore the object 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		first = in.readLong();
		last = in.readLong();
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeLong(first);
		out.writeLong(last);
	}
	

	/**
	 * Returns the median timestamp for the interval
	 * @return the median timestamp for the interval
	 */
	@Override
	public Object getValue() {
		return (long)davg;
	}
	
	/**
	 * Returns the median timestamp for the interval
	 * @return the median timestamp for the interval
	 */
	public long getMedianTimestamp() {
		return (long)davg;
	}
	
	/**
	 * Returns the median date for the interval
	 * @return the median date for the interval
	 */
	public Date getMedianDate() {
		return new Date((long)davg);
	}
	
	
	/**
	 * Returns the max timestamp for the interval
	 * @return the max timestamp for the interval
	 */
	public long getMaxTimestamp() {
		return max;
	}
	
	/**
	 * Returns the max date for the interval
	 * @return the max date for the interval
	 */
	public Date getMaxDate() {
		return new Date(max);
	}
	
	
	/**
	 * Returns the min timestamp for the interval
	 * @return the min timestamp for the interval
	 */
	public long getMinTimestamp() {
		return min;
	}
	
	/**
	 * Returns the min date for the interval
	 * @return the min date for the interval
	 */
	public Date getMinDate() {
		return new Date(min);
	}
	
	
	/**
	 * Returns the first timestamp for the interval
	 * @return the first timestamp for the interval
	 */
	public long getFirstTimestamp() {
		return first;
	}
	
	/**
	 * Returns the first date for the interval
	 * @return the first date for the interval
	 */
	public Date getFirstDate() {
		return new Date(first);
	}
	
	
	/**
	 * Returns the last timestamp for the interval
	 * @return the last timestamp for the interval
	 */
	public long getLastTimestamp() {
		return last;
	}

	/**
	 * Returns the last date for the interval
	 * @return the last date for the interval
	 */
	public Date getLastDate() {
		return new Date(last);
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("TimestampIntervalTraceValue [")
	    	.append(TAB).append("range = ").append(getMinDate()).append(" --> ").append(getMaxDate())
	        .append(TAB).append("median = ").append(getMedianDate())
	        .append(TAB).append("first = ").append(getFirstDate())
	        .append(TAB).append("min = ").append(getMinDate())
	        .append(TAB).append("last = ").append(getLastDate())
	        .append(TAB).append("max = ").append(getMaxDate())	        
	        .append(TAB).append("count = ").append(count)
	        .append("\n]");    
	    return retValue.toString();
	}
	

}
