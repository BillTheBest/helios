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

import org.helios.tracing.trace.MetricId;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: HighLowAverageIntervalInt</p>
 * <p>Description: Container class for handling numeric high/low/avg metrics for ints.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.interval.HighLowAverageIntervalInt</code></p>
 */

public class HighLowAverageIntervalInt extends BaseTraceMetricInterval  implements Externalizable {
	/** Indicates if values are sticky */
	protected boolean sticky;
	/** Indicates if any values have been applied since creation or the last snap */
	protected boolean snapped = false;
	/** The highest applied value for the current interval */
	protected int highValue = 0;
	/** The lowest applied value for the current interval */
	protected int lowValue = 0;
	/** The total of the interval applied values for the current interval */
	protected long totalValue = 0;
	
	/** The average applied value for the current interval */
	protected int avgValue = 0;
	
	/**
	 * For externalization only
	 */
	public HighLowAverageIntervalInt() {
		super((MetricId)null);
	}
	
	/**
	 * Copy Constructor
	 * @param highLowAverageIntervalInt a <code>HighLowAverageIntervalInt</code> object
	 */
	public HighLowAverageIntervalInt(HighLowAverageIntervalInt highLowAverageIntervalInt) {
		super(highLowAverageIntervalInt);
		this.sticky = highLowAverageIntervalInt.sticky;
		this.snapped = highLowAverageIntervalInt.snapped;
	    this.highValue = highLowAverageIntervalInt.highValue;
	    this.lowValue = highLowAverageIntervalInt.lowValue;
	    this.avgValue = highLowAverageIntervalInt.avgValue;
	}

	/**
	 * Returns a clone of this HLA
	 * @return a cloned HLA
	 */
	public HighLowAverageIntervalInt clone() {
		return new HighLowAverageIntervalInt(this);
	}

	/**
	 * Creates a new HighLowAverageIntervalInt
	 * @param metricId The metricId for this interval
	 * @param sticky If true, values are retained across snaps
	 */
	public HighLowAverageIntervalInt(MetricId metricId, boolean sticky) {
		super(metricId);
		this.sticky = sticky;
	}

	/**
	 * Closes the interval
	 */
	public void close() {
		super.close();
		avgValue = avg(totalValue, count);
	}
	
	
	/**
	 * Resets the HLA
	 */
	public void reset() {
		super.reset();
		snapped = false;
		totalValue = 0;
		if(!sticky) {
			highValue = 0;
			lowValue = 0;
			avgValue = 0;						
		}
	}
	
	/**
	 * Applies a new value to the HLA
	 * @param sample a traced value to be aggregated in the HLA
	 */
	public void apply(Trace trace) {
		super.apply(trace);

		int sample = trace.getIntValue();
		if(!snapped) {
			highValue = sample;
			lowValue = sample;
			totalValue = sample;
			avgValue = 0;
			snapped = true;
		} else {
			totalValue += sample;
			if(sample>highValue) highValue = sample;
			if(sample<lowValue) lowValue = sample;						
		}
	}
	
	
	/**
	 * Calculates the sequential average
	 * @param acc The cummulative average
	 * @param val the new value
	 * @return the new average
	 */
	protected int avg(double total, long cnt) {
		if(total<1 || cnt < 1) return 0;
		double d = total / cnt;
		return (int)d;
	}




	/**
	 * Indicates if the HLA is sticky
	 * @return if true, the HLA is sticky, otherwise it is interval averaged.
	 */
	public boolean isSticky() {
		return sticky;
	}




	/**
	 * Indicates if the HLA is in a snapped state
	 * @return if true, the HLA has been snapped this interval.
	 */
	public boolean isSnapped() {
		return snapped;
	}




	/**
	 * Returns the highest applied value this interval
	 * @return the highest applied value this interval
	 */
	public int getHighValue() {
		return highValue;
	}




	/**
	 * Returns the lowest applied value this interval
	 * @return the lowest applied value this interval
	 */
	public int getLowValue() {
		return lowValue;
	}




	/**
	 * Returns the average applied value this interval
	 * @return the average applied value this interval
	 */
	public int getAvgValue() {
		return avgValue;
	}

	/**
	 * Reads in this HLA
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		sticky = in.readBoolean();
		snapped = in.readBoolean();
		highValue = in.readInt();
		lowValue = in.readInt();
		avgValue = in.readInt();		
	}

	/**
	 * Writes out this HLA
	 * @param out
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeBoolean(sticky);
		out.writeBoolean(snapped);
		out.writeInt(highValue);
		out.writeInt(lowValue);
		out.writeInt(avgValue);
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder(metricId.toString()).append("[");
	    super.toString(retValue);
	    retValue.append(TAB).append("snapped = ").append(this.snapped)
	        .append(TAB).append("highValue = ").append(this.highValue)
	        .append(TAB).append("lowValue = ").append(this.lowValue)
	        .append(TAB).append("avgValue = ").append(this.avgValue)
	        .append(TAB).append("totalValue = ").append(this.totalValue)
	        .append("\n]");    
	    return retValue.toString();
	}

}
