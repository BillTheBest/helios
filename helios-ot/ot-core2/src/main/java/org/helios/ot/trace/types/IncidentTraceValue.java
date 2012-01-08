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
package org.helios.ot.trace.types;

/**
 * <p>Title: IncidentTraceValue</p>
 * <p>Description: TraceValue implementation for a number of incidents</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.IncidentTraceValue</code></p>
 */

public class IncidentTraceValue extends IntTraceValue {

	/**
	 * Creates a new IncidentTraceValue
	 */
	public IncidentTraceValue() {
		super(TraceValueType.INCIDENT_TYPE);
	}

	/**
	 * Creates a new IncidentTraceValue
	 * @param incidentCount The number of incidents to record
	 */
	public IncidentTraceValue(int incidentCount) {
		super(TraceValueType.INCIDENT_TYPE);
		this.value = incidentCount;
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    return getValue().toString();
	}

}
