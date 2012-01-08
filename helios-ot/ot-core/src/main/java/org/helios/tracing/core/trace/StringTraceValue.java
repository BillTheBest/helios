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

/**
 * <p>Title: StringTraceValue</p>
 * <p>Description: TraceValue implementation for a String </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.StringTraceValue</code></p>
 */

public class StringTraceValue extends TraceValue {
	/**  */
	private static final long serialVersionUID = -9216808984070639045L;
	/** The string value */
	protected String value;
	
	/**
	 * Creates a new StringTraceValue
	 * @param agentMetric The agent metric that this value is associated to
	 * @param timestamp The collection timestamp of the trace value
	 * @param value The value The value of the measurement
	 * @param urgent Indicates if the trace is urgent
	 * @param temporal Indicates if the trace is temporal
	 */
	public StringTraceValue(AgentMetric agentMetric, long timestamp, Object value, boolean urgent, boolean temporal) {
		super(agentMetric, timestamp, value, urgent, temporal);
		this.value = value.toString();
	}
	
	/**
	 * Creates a new StringTraceValue that is not urgent or temporal
	 * @param agentMetric The agent metric that this value is associated to
	 * @param timestamp The collection timestamp of the trace value
	 * @param value The value The value of the measurement
	 */
	public StringTraceValue(AgentMetric agentMetric, long timestamp, Object value) {
		this(agentMetric, timestamp, value, false, false);
	}
	
	/**
	 * Returns the trace instance value
	 * @return the trace instance value
	 */
	@Override
	public String getValue() {
		return value;
	}

}
