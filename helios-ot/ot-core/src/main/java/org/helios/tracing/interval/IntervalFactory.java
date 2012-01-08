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
import org.helios.tracing.trace.MetricType;

/**
 * <p>Title: IntervalFactory</p>
 * <p>Description: Factory for creating TraceMetricIntervals of the correct type for the passed trace.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.interval.IntervalFactory</code></p>
 */

public class IntervalFactory {
	/**
	 * Creates a new metric interval for the passed trace.
	 * @param trace The trace to create an interval for
	 * @return a new IIntervalTrace
	 */
	public static IIntervalTrace createInterval(MetricId id) {
		if(id==null) return null;
		MetricType type = id.getType();
		if(type.isNumber()) {
			if(type.isIncident()) {
				return new IncidentCounterInterval(id);
			} else {
				if(type.isInt()) {
					return new HighLowAverageIntervalInt(id, type.isSticky());
				} else {
					return new HighLowAverageIntervalLong(id, type.isSticky());
				}
			}
		} else if(type.isString()) {
			if(type.isSticky()) {
				return new StringInterval(true, id);
			} else {
				return new StringInterval(false, id);
			}
		} else {
			return new TimestampInterval(id);
		}
	}
}
