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
import java.util.ArrayList;
import java.util.List;

import org.helios.helpers.ExternalizationHelper;
import org.helios.ot.trace.types.ByteArrayTraceValue;
import org.helios.ot.trace.types.TraceValueType;
import org.helios.ot.type.MetricType;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: ByteArrayIntervalTraceValue</p>
 * <p>Description: Trace value for an interval of byte arrays. That is, this value simply maintains a list of byte arrays traced in the interval</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.interval.ByteArrayIntervalTraceValue</code></p>
 */
@XStreamAlias("ByteArrayIntervalTraceValue")
public class ByteArrayIntervalTraceValue<T extends ByteArrayTraceValue> extends AbstractIntervalTraceValue<T> {
	/** The byte arrays accumulated in this interval */
	@XStreamAlias("values")
	protected final List<byte[]> value = new ArrayList<byte[]>();
	/**
	 * Creates a new ByteArrayIntervalTraceValue
	 * @param abstractIntervalTraceValue
	 */
	private ByteArrayIntervalTraceValue(ByteArrayIntervalTraceValue<T> byteArrayIntervalTraceValue) {
		this();
		value.addAll(byteArrayIntervalTraceValue.value);		
	}

	/**
	 * Creates a new ByteArrayIntervalTraceValue
	 */
	public ByteArrayIntervalTraceValue() {
		super(TraceValueType.INTERVAL_BYTES_TYPE);
	}

	/**
	 * Creates a new ByteArrayIntervalTraceValue 
	 * @param traces The traces to initialize the interval with
	 */
	public ByteArrayIntervalTraceValue(T...traces) {
		this();
		if(traces!=null) {
			for(T trace: traces) {
				apply(trace);
			}
		}
	}

	/**
	 * Returns a string array of the distinct messages traced during this interval
	 * @return a string array
	 */
	@Override
	public byte[][] getValue() {
		return value.toArray(new byte[value.size()][]);
	}

	/**
	 * Aggregates the passed ITraceValue into this interval trace value
	 * @param value The ITraceValue to apply
	 */
	@Override
	public void apply(T value) {		
		super.apply(value);
		this.value.add(value.getValue());
	}
	
	/**
	 * Clones the state of this interval trace value and then resets it's state for the next interval
	 * @param metricType The metric type of the owning trace passed so that the interval value 
	 * can execute the reset with the correct semantics.
	 * @return A clone of this object prior to reset.
	 */
	public ByteArrayIntervalTraceValue<T> cloneReset(MetricType metricType) {
		if(metricType==null) throw new IllegalArgumentException("Passed MetricType was null", new Throwable());		
		ByteArrayIntervalTraceValue<T> clone = new ByteArrayIntervalTraceValue<T>(this);
		super.cloneReset(metricType);
		value.clear();
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
		int size = in.readInt();
		for(int i = 0; i < size; i++) {
			value.add(ExternalizationHelper.unExternalizeByteArray(in));
		}
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(value.size());
		for(byte[] arr: value) {
			ExternalizationHelper.externalizeByteArray(out, arr);
		}
	}
		

}
