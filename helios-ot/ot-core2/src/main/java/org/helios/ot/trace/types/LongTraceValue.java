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
package org.helios.ot.trace.types;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.helios.ot.deltas.DeltaManager;
import org.helios.ot.type.MetricType;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: LongTraceValue</p>
 * <p>Description: TraceValue implementation for longs.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.LongTraceValue</code></p>
 */
@XStreamAlias("LongTraceValue")
public class LongTraceValue extends AbstractNumericTraceValue  {
	/** The value */
	@XStreamAlias("value")
	protected long value;

	/**
	 * Creates a new LongTraceValue
	 */
	public LongTraceValue() {
		super(TraceValueType.LONG_TYPE);
	}
	
	/**
	 * Creates a new LongTraceValue for the passed child type
	 * @param type The child TraceValueType
	 */
	public LongTraceValue(TraceValueType type) {
		super(type);
	}
	
	/**
	 * Applies a delta to this value using the passed metric name as the key
	 * @param fqn The fully qualified metric name
	 * @return The resulting numeric trace value
	 */
	public INumericTraceValue applyDelta(CharSequence fqn) {
		if(fqn==null) throw new IllegalArgumentException("The passed fqn was null", new Throwable());
		Number n = DeltaManager.getInstance().delta(fqn.toString(), value, MetricType.DELTA_LONG_AVG, DeltaManager.DEFAULT_DELTA_TYPE);
		if(n==null) return null;
		else {
			value = n.longValue();
			return this;
		}
	}
	
	
	
	/**
	 * Returns the primary value of this trace value
	 * @return the primary value of this trace value
	 */
	public Object getValue() {
		return value;
	}
	
	public long getLongValue() {
		return value;
	}
	
//	/**
//	 * Returns the numberic value of the numeric trace as a double
//	 * @return a double
//	 */
//	public double getDValue() {
//		return new Double(value);
//	}	
	
	/**
	 * Returns the long value
	 * @return the long value
	 */
	public double getNativeValue() {
		return value;
	}
	

	/**
	 * Creates a new LongTraceValue
	 * @param value The value
	 */
	public LongTraceValue(long value) {
		super(TraceValueType.LONG_TYPE.ordinal());
		this.value = value;
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
		value = in.readLong();
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);				
		out.writeLong(value);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (value ^ (value >>> 32));
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
		LongTraceValue other = (LongTraceValue) obj;
		if (value != other.value)
			return false;
		return true;
	}

}

