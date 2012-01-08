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
package org.helios.tracing.core.trace;

import java.lang.reflect.Array;

/**
 * <p>Title: StringsTraceValue</p>
 * <p>Description: TraceValue implementation for a String array</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.StringsTraceValue</code></p>
 */

public class StringsTraceValue extends TraceValue {
	/**  */
	private static final long serialVersionUID = -7414735969982830424L;
	/** The string value */
	protected String[] value;
	
	/**
	 * Creates a new StringsTraceValue
	 * @param agentMetric The agent metric that this value is associated to
	 * @param timestamp The collection timestamp of the trace value
	 * @param value The value The value of the measurement
	 * @param urgent Indicates if the trace is urgent
	 * @param temporal Indicates if the trace is temporal
	 */
	public StringsTraceValue(AgentMetric agentMetric, long timestamp, Object value, boolean urgent, boolean temporal) {
		super(agentMetric, timestamp, value, urgent, temporal);
		if(value.getClass().isArray()) {
			int length = Array.getLength(value);
			this.value = new String[length];
			for(int i = 0; i < length; i++) {
				this.value[i] = Array.get(value, i).toString();
			}
			
		} else {
			this.value = new String[]{value.toString()};
		}		
	}
	
	/**
	 * Creates a new StringsTraceValue that is not urgent or temporal
	 * @param agentMetric The agent metric that this value is associated to
	 * @param timestamp The collection timestamp of the trace value
	 * @param value The value The value of the measurement
	 */
	public StringsTraceValue(AgentMetric agentMetric, long timestamp, Object value) {
		this(agentMetric, timestamp, value, false, false);
	}
	
	/**
	 * Returns the trace instance value
	 * @return the trace instance value
	 */
	@Override
	public String[] getValue() {
		return value;
	}

}
