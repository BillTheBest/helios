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

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.helios.ot.trace.types.StringsTraceValue;
import org.helios.ot.trace.types.TraceValueType;
import org.helios.ot.type.MetricType;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: StringsIntervalTraceValue</p>
 * <p>Description: Trace value for an interval of strings. The value and count of each distinct message during the interval is accumulated.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.interval.StringsIntervalTraceValue</code></p>
 */
@XStreamAlias("StringsIntervalTraceValue")
public class StringsIntervalTraceValue<T extends StringsTraceValue> extends AbstractIntervalTraceValue<T> {
	/** The distinct string messages traced during the interval and a count of each */
	@XStreamAlias("values")
	protected final TObjectIntHashMap<String> messages;

	/**
	 * Creates a new StringsIntervalTraceValue 
	 */
	public StringsIntervalTraceValue() {
		super(TraceValueType.INTERVAL_STRINGS_TYPE);
		messages = new TObjectIntHashMap<String>();
	}
	
	
	/**
	 * Creates a new StringsIntervalTraceValue 
	 * @param traces The initial traces to apply to this interval
	 */
	public StringsIntervalTraceValue(T...traces) {
		super(TraceValueType.INTERVAL_STRINGS_TYPE);
		messages = new TObjectIntHashMap<String>();
		if(traces!=null) {
			for(T trace: traces) {
				apply(trace);
			}
		}
	}
	
	/**
	 * Copy Constructor 
	 * @param stringsIntervalTraceValue The interval trace to clone from
	 */
	private StringsIntervalTraceValue(StringsIntervalTraceValue<T> stringsIntervalTraceValue) {
		super(TraceValueType.INTERVAL_STRINGS_TYPE);
		this.messages = new TObjectIntHashMap<String>(stringsIntervalTraceValue.messages);
	}
	

	/**
	 * Returns a string array of the distinct messages traced during this interval
	 * @return a string array
	 */
	@Override
	public String[] getValue() {
		int size = messages.size();
		return new TreeSet<String>(
				Arrays.asList(
						messages.keys(new String[size]))
				).toArray(new String[size]);
	}

	/**
	 * Aggregates the passed ITraceValue into this interval trace value
	 * @param value The ITraceValue to apply
	 */
	@Override
	public void apply(T value) {		
		super.apply(value);
		messages.adjustOrPutValue(value.getValue(), 1, 1);
	}
	
	/**
	 * Clones the state of this interval trace value and then resets it's state for the next interval
	 * @param metricType The metric type of the owning trace passed so that the interval value 
	 * can execute the reset with the correct semantics.
	 * @return A clone of this object prior to reset.
	 */
	public StringsIntervalTraceValue<T> cloneReset(MetricType metricType) {
		if(metricType==null) throw new IllegalArgumentException("Passed MetricType was null", new Throwable());		
		StringsIntervalTraceValue<T> clone = new StringsIntervalTraceValue<T>(this);
		super.cloneReset(metricType);
		messages.clear();
		return clone;
	}		
	
	/**
	 * Reads the state of this object in from the Object input stream
	 * @param in the stream to read data from in order to restore the object 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
//	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		messages.readExternal(in);
//		TObjectIntHashMap<String> msgs = (TObjectIntHashMap<String>)in.readObject();
//		msgs.forEachEntry(new TObjectIntProcedure<String>(){
//			public boolean execute(String message, int cnt) {
//				messages.put(message, cnt);
//				return true;
//			}			
//		});

	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		messages.writeExternal(out);
	}
		
	
}
