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

import org.helios.helpers.ExternalizationHelper;
import org.helios.ot.trace.types.StringTraceValue;
import org.helios.ot.trace.types.TraceValueType;
import org.helios.ot.type.MetricType;

//import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: StringIntervalTraceValue</p>
 * <p>Description: Trace value for an interval of a string slot. That is, this value simply maintains the last provided string in the interval</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.interval.StringIntervalTraceValue</code></p>
 */
//@XStreamAlias("StringIntervalTraceValue")
public class StringIntervalTraceValue<T extends StringTraceValue> extends AbstractIntervalTraceValue<T> {
	/** The value of the last string traced in the interval */
	//@XStreamAlias("value")
	protected String message = null;
	
	/**
	 * Copy Constructor
	 * @param stringIntervalTraceValue a <code>StringIntervalTraceValue</code> object
	 */
	private StringIntervalTraceValue(StringIntervalTraceValue<T> stringIntervalTraceValue) {
		super(stringIntervalTraceValue);
		this.message = stringIntervalTraceValue.message;
	}
	
	/**
	 * Creates a new StringIntervalTraceValue 
	 * @param traces The initial traces to apply
	 */
	public StringIntervalTraceValue(T...traces) {
		super(TraceValueType.INTERVAL_STRING_TYPE);
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
	public StringIntervalTraceValue<T> cloneReset(MetricType metricType) {
		if(metricType==null) throw new IllegalArgumentException("Passed MetricType was null", new Throwable());		
		StringIntervalTraceValue<T> clone = new StringIntervalTraceValue<T>(this);
		super.cloneReset(metricType);
		message = "";
		return clone;
	}	
	
	/**
	 * Aggregates the passed ITraceValue into this interval trace value
	 * @param value The ITraceValue to apply
	 */
	@Override
	public void apply(T value) {		
		super.apply(value);
		message = value.getValue();
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
		message = ExternalizationHelper.unExternalizeString(in);
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		ExternalizationHelper.externalizeString(out, message);
	}
	
	
	
	


	/**
	 * Creates a new StringIntervalTraceValue
	 */
	public StringIntervalTraceValue() {
		super(TraceValueType.STRING_TYPE);
	}
	

	/**
	 * Returns the primary value of this| grep trace value
	 * @return the primary value of this trace value
	 */
	@Override
	public String getValue() {
		return message;
	}
}
