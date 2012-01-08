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
import java.util.Date;

import org.helios.patterns.queues.Filterable;
import org.helios.patterns.queues.LongBitMaskFactory.LongBitMaskSequence;
import org.helios.tracing.trace.MetricId;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: BaseTraceMetricInterval</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.interval.BaseTraceMetricInterval</code></p>
 * @TODO: Add temporal
 * @TODO: Add urgent  
 */

public abstract class BaseTraceMetricInterval implements Filterable<Long>, Externalizable, IIntervalTrace {
	/** The metric interval's metric Id */
	protected MetricId metricId = null;
	/** The start time of this interval */
	protected long startTime;
	/** The end time of this interval */
	protected long endTime;
	/** The interval incident count */
	protected long count;
	
	private static long ctorCount = 0;
	
	/**
	 * Copy Constructor
	 * @param baseTraceMetricInterval a <code>BaseTraceMetricInterval</code> object
	 */
	public BaseTraceMetricInterval(BaseTraceMetricInterval baseTraceMetricInterval) 
	{
	    this.metricId = baseTraceMetricInterval.metricId;
	    this.startTime = baseTraceMetricInterval.startTime;
	    this.endTime = baseTraceMetricInterval.endTime;
	    this.count = baseTraceMetricInterval.count;
	}

	/**
	 * Creates a new BaseTraceMetricInterval 
	 * @param metricId The metridIc of the interval
	 */
	public BaseTraceMetricInterval(MetricId metricId) {
		ctorCount++;
		this.metricId = metricId;
		startTime = System.currentTimeMillis();
		System.out.println("Ctors Called:" + ctorCount);
	}
	
	/**
	 * Returns the MetricId for this trace interval
	 * @return the MetricId for this trace interval
	 */
	public MetricId getMetricId() {
		return metricId;
	}
	
	/**
	 * Returns the start time of the interval
	 * @return the start time of the interval
	 */
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * Returns the end time of the interval
	 * @return the end time of the interval
	 */
	public long getEndTime() {
		return endTime;
	}
	
	/**
	 * Returns the incident count for this metric during the interval
	 * @return the incident count 
	 */
	public long getCount() {
		return count;
	}
	
	/**
	 * Closes out the interval
	 */
	public void close() {
		endTime = System.currentTimeMillis();
	}
	
	/**
	 * Returns a copy of this interval
	 * @return a copy of this interval
	 */
	public abstract IIntervalTrace clone();
	
	/**
	 * Resets the interval
	 */
	public void reset() {
		startTime = System.currentTimeMillis();
		count = 0;
	}
	
	/**
	 * Accumulates a trace for the current interval
	 * @param trace a trace to apply
	 */
	public void apply(Trace trace) {
		count++;
	}
	
	/**
	 * Reads in this Interval
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		startTime = in.readLong();
		endTime = in.readLong();
		count = in.readLong();
		metricId = (MetricId)in.readObject();
	}

	/**
	 * Writes out this HLA
	 * @param out
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(startTime);
		out.writeLong(endTime);
		out.writeLong(count);
		out.writeObject(metricId);
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("BaseTraceMetricInterval [")
	        .append(TAB).append("metricId = ").append(this.metricId)
	        .append(TAB).append("startTime = ").append(this.startTime)
	        .append(TAB).append("endTime = ").append(this.endTime)
	        .append(TAB).append("count = ").append(this.count)
	        .append("\n]");    
	    return retValue.toString();
	}

	/**
	 * Appends to a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public void toString(StringBuilder b) {
	    final String TAB = "\n\t";
	        b.append(TAB).append("startTime = ").append(new Date(startTime))
	        .append(TAB).append("endTime = ").append(new Date(this.endTime))
	        .append(TAB).append("count = ").append(this.count);
	}
	
	
	/**
	 * Determines if the passed bit is enabled in the metricId's mask
	 * @param filterKey The mask bit of a FilteredBlockingQueue
	 * @return true if this item should be dropped from the FilteredBlockingQueue  (i.e. the bitmask is not enabled)
	 */
	@Override
	public boolean drop(Long filterKey) {		
		return metricId.drop(filterKey);
	}
		

}
