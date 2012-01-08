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

import org.helios.ot.trace.types.INumericTraceValue;
import org.helios.ot.trace.types.TraceValueType;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: AbstractNumericIntervalTraceValue</p>
 * <p>Description: A base abstract class for all numeric interval trace values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.interval.AbstractNumericIntervalTraceValue</code></p>
 * @param <T>
 */


public abstract class AbstractNumericIntervalTraceValue<T extends INumericTraceValue> extends AbstractIntervalTraceValue<T>  { //implements INumericIntervalTraceValue<T> {
	/** The average value for the interval */
	protected double davg = 0D;
	/** The rolling total */
	protected double total = 0D;
	

	
	/**
	 * Returns the accumulated total as a {@link java.lang.Number}
	 * @return the total
	 */	
	public Number getTotal() {
		return total;
	}

	
	/**
	 * Copy Constructor
	 * @param abstractNumericIntervalTraceValue a <code>AbstractNumericIntervalTraceValue</code> object
	 */
	protected AbstractNumericIntervalTraceValue(AbstractNumericIntervalTraceValue<T> abstractNumericIntervalTraceValue) {
		super(abstractNumericIntervalTraceValue);
	    this.davg = abstractNumericIntervalTraceValue.davg;
	    this.total = abstractNumericIntervalTraceValue.total;
	}

	/**
	 * Excutes a reset on this instance. Abstract classes return null as the clone is completed by concrete instances.
	 * @param metricType The metric type of the owning trace passed so that the interval value 
	 * can execute the reset with the correct semantics.
	 * @return null
	 */
	public IIntervalTraceValue<T> cloneReset(MetricType metricType) {
		super.cloneReset(metricType);
		if(!metricType.isSticky()) {
			davg = 0D;			
		}
		total = 0D;
		return null;
	}
	/**
	 * Creates a new AbstractNumericIntervalTraceValue
	 * @param traceValueTypeId
	 */
	public AbstractNumericIntervalTraceValue(int traceValueTypeId) {
		super(traceValueTypeId);
	}

	/**
	 * Creates a new AbstractNumericIntervalTraceValue 
	 * @param traceValueType
	 */
	public AbstractNumericIntervalTraceValue(TraceValueType traceValueType) {
		super(traceValueType);
	}
	
	protected double _avg() {
		if(total==0 || count==0) return 0;
		else return total/count;
	}
	
//	/**
//	 * Calculates a rolling average
//	 * @param curr The current average
//	 * @param ne The new value to rollup
//	 * @return The new rolling average
//	 */
//	protected static double _avg(double curr, double ne) {
//		curr = curr/10;
//		ne = ne/10;
//		
//		double v = curr + ne;
//		if(v==0) return 0;
//		return Math.floor(((v/2)*10));
//		
//	}	
	

	
	/**
	 * Aggregates the passed ITraceValue into this interval trace value
	 * @param value The ITraceValue to apply
	 */
	@Override
	public void apply(T value) {
		super.apply(value);
		total += value.getNativeValue();
		davg = _avg();
	}
	
	/**
	 * Returns the average value as a double
	 * @return the average value as a double
	 */
	public double getDoubleAverage() {
		return davg;
	}
	
	/**
	 * Returns the average value as a Number
	 * @return the average value as a Number
	 */
	public Number getAverage() {
		return davg;
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
		davg = in.readDouble();
		total = in.readDouble();
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeDouble(davg);
		out.writeDouble(total);
	}





















	
	
}
