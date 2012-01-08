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

import org.helios.tracing.trace.MetricId;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: IIntervalTrace</p>
 * <p>Description: Defines a class that accumulates multiple traces of the same unique metric for the duration of one interval.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.accumulator.ITraceMetricInterval</code></p>
 */

public interface IIntervalTrace {
	/**
	 * Returns the MetricId for this trace interval
	 * @return the MetricId for this trace interval
	 */
	public MetricId getMetricId();
	
	/**
	 * Returns the start time of the interval
	 * @return the start time of the interval
	 */
	public long getStartTime();
	/**
	 * Returns the end time of the interval
	 * @return the end time of the interval
	 */
	public long getEndTime();
	/**
	 * Returns the incident count for this metric during the interval
	 * @return the incident count 
	 */
	public long getCount();
	
	/**
	 * Closes out the interval
	 */
	public void close();
	/**
	 * Returns a copy of this interval
	 * @return a copy of this interval
	 */
	public IIntervalTrace clone();
	
	/**
	 * Resets the interval
	 */
	public void reset();
	
	/**
	 * Accumulates a trace for the current interval
	 * @param trace a trace to apply
	 */
	public void apply(Trace trace);
}
