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

import org.helios.ot.trace.types.interval.IMinMaxAvgIntervalTraceValue;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: ClosedMinMaxAvgTrace</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.ClosedMinMaxAvgTrace</code></p>
 */
@SuppressWarnings("unchecked")
@XStreamAlias("closedTrace")
@XmlRootElement(name="closedTrace")
public class ClosedMinMaxAvgTrace extends ClosedIntervalTrace {
	/** The maximum value for the interval */
	@XStreamAlias("max")
	@XmlElement(name="max")
	protected long max;
	/** The minimum value for the interval */
	@XStreamAlias("min")
	@XmlElement(name="min")
	protected long min;
	/** The average value for the interval */
	@XStreamAlias("avg")
	@XmlElement(name="avg")
	protected long avg;

	/**
	 * Creates a new ClosedMinMaxAvgTrace. For externalization only
	 */
	public ClosedMinMaxAvgTrace() {
		
	}
	
	
	/**
	 * Creates a new ClosedMinMaxAvgTrace
	 * @param trace An IntervalTrace trace
	 */
	protected ClosedMinMaxAvgTrace(IntervalTrace trace) {		
		super(nvl(trace));
		IMinMaxAvgIntervalTraceValue minMaxAvg = (IMinMaxAvgIntervalTraceValue)trace.getIntervalTraceValue();
		max = minMaxAvg.getMaximum().longValue();
		min = minMaxAvg.getMinimum().longValue();
		avg = minMaxAvg.getAverage().longValue();
	}

	/**
	 * Returns the maximum value for the interval
	 * @return the maximum value for the interval
	 */
	public long getMax() {
		return max;
	}

	/**
	 * Returns the minimum value for the interval
	 * @return the minimum value for the interval
	 */
	public long getMin() {
		return min;
	}

	/**
	 * Returns the average value for the interval
	 * @return the average value for the interval
	 */
	public long getAvg() {
		return avg;
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		try {
			min = in.readLong();
			max = in.readLong();
			avg = in.readLong();
		} catch (Exception e) {
			String msg = "Failed to read externalized ClosedMinMaxAvgTrace";
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
			out.writeLong(min);
			out.writeLong(max);
			out.writeLong(avg);
		} catch (Exception e) {
			String msg = "Failed to write ClosedMinMaxAvgTrace to external";
			System.err.println(msg);
			throw new RuntimeException(msg, e);
		}		
	}

	

}
