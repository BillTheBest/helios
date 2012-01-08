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
package org.helios.tracing.interval;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.helpers.ExternalizationHelper;
import org.helios.tracing.trace.MetricId;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: StringInterval</p>
 * <p>Description: Interval metric for a multi or single strings. If single, the last string of interval is submitted.
 * If multi, unique messages and the count of each is retained.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.interval.StringInterval</code></p>
 */

public class StringInterval extends BaseTraceMetricInterval implements Externalizable {
	/** The last traced string message of the interval, if any */
	protected final AtomicReference<CharSequence> message = new AtomicReference<CharSequence>(null);
	/** The unique traced string messages of the interval as keys with the value being the count of each */
	protected final Map<String, AtomicInteger> messages = new HashMap<String, AtomicInteger>();
	
	/** Indicates if the interval is single or multi string */
	protected boolean single = true;
	
	/**
	 * Creates a new StringInterval
	 * @param metricId
	 */
	public StringInterval(StringInterval stringInterval) {
		super(stringInterval);
		this.single = stringInterval.single;
		if(single) {
			this.message.set(stringInterval.message.get());
		} else {
			this.messages.putAll(stringInterval.messages);
		}
	}
	
	/**
	 * Accumulates a trace for the current interval
	 * @param trace a trace to apply
	 */
	public void apply(Trace trace) {
		super.apply(trace);
		String msg = trace.getValue().toString();
		if(single) {
			message.set(msg);
		} else {
			AtomicInteger ai = messages.get(msg);
			if(ai==null) {
				ai = new AtomicInteger(0);
				messages.put(msg, ai);
			}
			ai.incrementAndGet();
		}
	}
	
	/**
	 * Clears the message reference
	 */
	public void reset() {
		super.reset();
		if(!single) {
			messages.clear();
		}		
	}
	
	/**
	 * Returns a clone of this StringInterval
	 * @return a clone of this StringInterval
	 */
	public StringInterval clone() {		
		return new StringInterval(this);
	}

	
	/**
	 * Creates a new StringInterval
	 * @param single 
	 * @param metricId
	 */
	public StringInterval(boolean single, MetricId metricId) {
		super(metricId);
		this.single = single;
	}
	
	/**
	 * Reads in this Interval
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		single = in.readBoolean();
		if(single) {
			String s = ExternalizationHelper.unExternalizeString(in);
			message.set(s);			
		} else {
			int cnt = in.readInt();
			if(cnt>0) {
				for(int i = 0; i < cnt; i++) {
					String msg = ExternalizationHelper.unExternalizeString(in);
					int count = in.readInt();
					messages.put(msg, new AtomicInteger(count));
				}
			}
		}
	}

	/**
	 * Writes out this Interval
	 * @param out
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeBoolean(single);
		if(single) {
			CharSequence ch = message.get();
			if(ch!=null) {
				ExternalizationHelper.externalizeString(out, ch.toString());
			} else {
				ExternalizationHelper.externalizeString(out, null);
			}
		} else {
			Map<String, AtomicInteger> copy = new HashMap<String, AtomicInteger>(messages);
			out.writeInt(copy.size());
			for(Map.Entry<String, AtomicInteger> entry: copy.entrySet()) {
				ExternalizationHelper.externalizeString(out, entry.getKey());
				out.writeInt(entry.getValue().get());
			}
		}
		
	}
	
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder(metricId.toString()).append("[");
	    super.toString(retValue);
	    if(single) {
	    	retValue.append(TAB).append("Msg:").append(message.get());
	    } else {
	    	for(Map.Entry<String, AtomicInteger> entry: messages.entrySet()) {
	    		retValue.append(TAB).append(entry.getKey()).append(":").append(entry.getValue());
	    	}
	    }
	    retValue.append("\n]");
	    return retValue.toString();
	}

}
