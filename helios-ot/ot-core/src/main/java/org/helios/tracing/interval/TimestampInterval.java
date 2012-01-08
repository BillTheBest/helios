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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.tracing.trace.MetricId;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: TimestampInterval</p>
 * <p>Description: Interval accumulator for timestamps. Retains the last timestamp per interval. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.interval.TimestampInterval</code></p>
 */

public class TimestampInterval extends BaseTraceMetricInterval {
	/** The last timestamp of the interval  */
	protected final AtomicLong timestamp = new AtomicLong(-1L);
	
	
	/**
	 * Creates a new clone TimestampInterval 
	 * @param timestampInterval The timestampInterval to clone from
	 */
	public TimestampInterval(TimestampInterval timestampInterval) {
		super(timestampInterval);
		this.timestamp.set(timestampInterval.timestamp.get());
	}
	
	/**
	 * Creates a new TimestampInterval
	 * @param metricId The metric Id for this interval metric
	 */
	public TimestampInterval(MetricId metricId) {
		super(metricId);
	}

	/**
	 * Resets the timestamp
	 */
	public void reset() {
		super.reset();
		timestamp.set(-1);
	}
	
	/**
	 * @return
	 */
	@Override
	public TimestampInterval clone() {
		return new TimestampInterval(this);
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
		timestamp.set(in.readLong());
	}
	
	/**
	 * Accumulates a trace for the current interval
	 * @param trace a trace to apply
	 */
	public void apply(Trace trace) {
		super.apply(trace);
		timestamp.set(trace.getLongValue());
		
	}

	/**
	 * Writes out this HLA
	 * @param out
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeLong(timestamp.get());
	}
	
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder(metricId.toString()).append("[");
	    super.toString(retValue);
	    retValue.append(TAB).append("Timestamp:").append(new Date(this.timestamp.get()))
	        .append("\n]");    
	    return retValue.toString();
	}
	
}
