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
 * <p>Title: LongTraceValue</p>
 * <p>Description: A long type trace value implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.LongTraceValue</code></p>
 */

public class LongTraceValue extends TraceValue {
	/**  */
	private static final long serialVersionUID = 6490818499107017089L;
	/** The trace long value */
	protected long value;
	
	/**
	 * Creates a new LongTraceValue
	 * @param agentMetric The agent metric that this value is associated to
	 * @param timestamp The collection timestamp of the trace value
	 * @param value The value The value of the measurement
	 * @param urgent Indicates if the trace is urgent
	 * @param temporal Indicates if the trace is temporal
	 */
	public LongTraceValue(AgentMetric agentMetric, long timestamp, Object value, boolean urgent, boolean temporal) {
		super(agentMetric, timestamp, value, urgent, temporal);
		if(value instanceof Number) {
			this.value = ((Number)value).longValue();
		} else {
			this.value = Long.parseLong(value.toString());
		}
	}
	
	/**
	 * Creates a new LongTraceValue that is not urgent or temporal
	 * @param agentMetric The agent metric that this value is associated to
	 * @param timestamp The collection timestamp of the trace value
	 * @param value The value The value of the measurement
	 */
	public LongTraceValue(AgentMetric agentMetric, long timestamp, Object value) {
		this(agentMetric, timestamp, value, false, false);
	}
	
	/**
	 * Returns the trace long value
	 * @return the trace long value
	 */
	@Override
	public Long getValue() {
		return value;
	}

}
