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
package org.helios.ot.trace;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

//import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: ClosedIntervalTrace</p>
 * <p>Description: A closed interval trace</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.ClosedIntervalTrace</code></p>
 */
@SuppressWarnings("unchecked")
//@XStreamAlias("closedTrace")
@XmlRootElement(name="closedTrace")
public class ClosedIntervalTrace extends ClosedTrace {
	/** The hit count for this metric */
	//@XStreamAlias("count")
	@XmlElement(name="count")
	protected int count;
	/** The interval end timestamp */
	//@XStreamAlias("endTimestamp")
	@XmlElement(name="endTimestamp")
	protected long endTimestamp;
	
	/**
	 * Creates a new ClosedIntervalTrace. For externalization only
	 */
	public ClosedIntervalTrace() {
		
	}

	/**
	 * Creates a new ClosedIntervalTrace
	 * @param trace An interval trace
	 */
	protected ClosedIntervalTrace(IntervalTrace trace) {
		super(nvl(trace));
		count = trace.intervalTraceValue.getCount();
		endTimestamp = trace.endTimeStamp;
		traceValue = trace.getIntervalTraceValue();
		value = traceValue.getValue();
	}
	
	/**
	 * Returns the metric hit count for this interval
	 * @return the metric hit count for this interval
	 */
	public int getCount() {
		return count;
	}


	/**
	 * The ending timestamp for this interval
	 * @return the ending timestamp for this interval
	 */
	public long getEndTimestamp() {
		return endTimestamp;
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		try {
			count = in.readInt();
			endTimestamp = in.readLong();
		} catch (Exception e) {
			String msg = "Failed to read externalized ClosedTrace";
			System.err.println(msg);
			throw new RuntimeException(msg, e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {	
		super.writeExternal(out);
		try {
			out.writeInt(count);
			out.writeLong(endTimestamp);
		} catch (Exception e) {
			String msg = "Failed to write ClosedTrace to external";
			System.err.println(msg);
			throw new RuntimeException(msg, e);
		}		
	}


	

}
