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

import org.helios.ot.trace.types.IncidentTraceValue;
import org.helios.ot.trace.types.TraceValueType;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: IncidentIntervalTraceValue</p>
 * <p>Description: Trace value for an interval of incident traces</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.interval.IncidentIntervalTraceValue</code></p>
 */
////@XStreamAlias("IncidentIntervalTraceValue")
public class IncidentIntervalTraceValue<T extends IncidentTraceValue> extends AbstractNumericIntervalTraceValue<T> {
	
	/**
	 * Creates a new IncidentIntervalTraceValue
	 * @param abstractNumericIntervalTraceValue
	 */
	private IncidentIntervalTraceValue(IncidentIntervalTraceValue<T> incidentIntervalTraceValue) {
		super(incidentIntervalTraceValue);
		
	}

	/**
	 * Creates a new IncidentIntervalTraceValue
	 * @param traceValueTypeId
	 */
	public IncidentIntervalTraceValue() {
		super(TraceValueType.INTERVAL_INCIDENT_TYPE);
	}
	
	/**
	 * Creates a new IncidentIntervalTraceValue
	 * @param traces The initial traces to apply
	 */
	public IncidentIntervalTraceValue(T...traces) {
		super(TraceValueType.INTERVAL_INCIDENT_TYPE);
		if(traces!=null) {
			for(T trace: traces) {
				apply(trace);
			}
		}		
	}	
	
	/**
	 * Aggregates the passed ITraceValue into this interval trace value
	 * @param value The ITraceValue to apply
	 */
	@Override
	public void apply(T value) {		
		super.apply(value);		
		//this.value  += value.getIntValue();
	}
		
	/**
	 * Clones the state of this interval trace value and then resets it's state for the next interval
	 * @param metricType The metric type of the owning trace passed so that the interval value 
	 * can execute the reset with the correct semantics.
	 * @return A clone of this object prior to reset.
	 */
	public IncidentIntervalTraceValue<T> cloneReset(MetricType metricType) {
		if(metricType==null) throw new IllegalArgumentException("Passed MetricType was null", new Throwable());		
		IncidentIntervalTraceValue<T> clone = new IncidentIntervalTraceValue<T>(this);
		super.cloneReset(metricType);		
		return clone;
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
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
	}	

	/**
	 * Returns the number of incidents for this interval
	 * @return the number of incidents for this interval
	 */
	@Override
	public Object getValue() {
		return (long)total;
	}
	
	/**
	 * Returns the total number of incidents for the interval
	 * @return the total number of incidents for the interval
	 */
	public long getLongValue() {
		return (long)total;
	}
	
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {	    
	    StringBuilder retValue = new StringBuilder("\n\tIncidentIntervalTraceValue [")
	        .append(TAB).append("total = ").append(getValue())
	        .append(TAB).append("cnt = ").append(this.count)
	        .append("\n\t]");    
	    return retValue.toString();
	}	

}
