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

/**
 * <p>Title: IncidentCounterInterval</p>
 * <p>Description: Interval event counter</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.opentrace.interval.IncidentCounterInterval</code></p>
 */

public class IncidentCounterInterval extends BaseTraceMetricInterval {

	/**
	 * For externalization only
	 */
	public IncidentCounterInterval() {
		super((MetricId)null);
	}
	
	/**
	 * Creates a new IncidentCounterInterval for the passed MetricId
	 * @param metricId the metricId for this interval
	 */
	public IncidentCounterInterval(MetricId metricId) {
		super(metricId);
	}
	
	/**
	 * Creates a new clone of this IncidentCounterInterval
	 * @param incidentCounterInterval the cloned IncidentCounterInterval
	 */
	public IncidentCounterInterval(IncidentCounterInterval incidentCounterInterval) {
		super(incidentCounterInterval);
	}
	

	/**
	 * Returns a clone of this incidentCounterInterval
	 * @return a clone of this incidentCounterInterval
	 */
	@Override
	public IncidentCounterInterval clone() {
		return new IncidentCounterInterval(this);
	}
	
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    StringBuilder retValue = new StringBuilder(metricId.toString()).append("[");
	    super.toString(retValue);
	    retValue.append("\n]");
	    return retValue.toString();
	}
	

}
