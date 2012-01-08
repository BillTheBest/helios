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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.trace.types.TraceValueType;
import org.helios.ot.type.MetricType;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: AbstractIntervalTraceValue</p>
 * <p>Description: A base abstract class for all interval trace values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.interval.AbstractIntervalTraceValue</code></p>
 */

public abstract class AbstractIntervalTraceValue<T extends ITraceValue> implements IIntervalTraceValue<T>, Externalizable {
	/** The number of traces aggregated during the interval */
	@XStreamAlias("count")
	protected int count = 0;
	/** The TraceValueType of the type of this trace value */
	@XStreamAlias("valueType")
	protected TraceValueType traceValueType = null;
	
	/**
	 * Returns the trace value type of this instance
	 * @return the traceValueType
	 */
	public TraceValueType getTraceValueType() {
		return traceValueType;
	}
	
	
	/**
	 * Copy Constructor
	 * @param abstractIntervalTraceValue a <code>AbstractIntervalTraceValue</code> object
	 */
	protected AbstractIntervalTraceValue(AbstractIntervalTraceValue<T> abstractIntervalTraceValue) {
	    this.count = abstractIntervalTraceValue.count;
	    this.traceValueType = abstractIntervalTraceValue.traceValueType;
	}
	
	/**
	 * Excutes a reset on this instance. Abstract classes return null as the clone is completed by concrete instances.
	 * @param metricType The metric type of the owning trace passed so that the interval value 
	 * can execute the reset with the correct semantics.
	 * @return null
	 */
	public IIntervalTraceValue<T> cloneReset(MetricType metricType) {
		count = 0;
		return null;
	}
	
	/**
	 * Determines if any traces have een applied to this interval
	 * @return true if any traces have een applied to this interval, false otherwise
	 */
	public boolean isTouched() {
		return count > 0;
	}

	/**
	 * Returns the number of traces aggregated during the interval
	 * @return the number of traces aggregated during the interval
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * Increments the count by 1
	 * @return the updated count
	 */
	public int increment() {
		count++;
		return count;
	}
	

	
	/**
	 * Increments the count by the passed value
	 * @param value the value to increment the cont by
	 * @return the updated count
	 */
	public int increment(int value) {
		count = count + value;
		return count;
	}
	
	/**
	 * Aggregates the passed ITraceValue into this interval trace value
	 * @param value The ITraceValue to apply
	 */
	public void apply(T value) {
		count++;		
	}
	

	/**
	 * Creates a new AbstractIntervalTraceValue
	 * @param traceValueTypeId the type ID
	 */
	protected AbstractIntervalTraceValue(int traceValueTypeId) {
		TraceValueType tvt = TraceValueType.forCode(traceValueTypeId);
		if(tvt==null) throw new IllegalArgumentException("Passed TraceValueTypeId [" + traceValueTypeId + "] is not a valid TraceValueType ordinal", new Throwable());
		this.traceValueType = tvt;
	}

	/**
	 * Creates a new AbstractIntervalTraceValue
	 * @param traceValueType the type ID
	 */
	protected AbstractIntervalTraceValue(TraceValueType traceValueType) {
		if(traceValueType==null) throw new IllegalArgumentException("Passed TraceValueType was null", new Throwable());
		this.traceValueType = traceValueType;
	}
	
	
	
	/**
	 * Indicates if this is an interval type
	 * @return true if this is an interval type
	 */
	public boolean isInterval() {
		return true;
	}	
	
	/**
	 * Reads the state of this object in from the Object input stream
	 * @param in the stream to read data from in order to restore the object 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		count = in.readInt();		
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(count);		
	}



	
	
	

}
