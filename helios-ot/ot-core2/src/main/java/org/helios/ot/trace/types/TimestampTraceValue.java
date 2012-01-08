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

import java.util.Date;

/**
 * <p>Title: TimestampTraceValue</p>
 * <p>Description: TraceValue implementation for timestamps</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.TimestampTraceValue</code></p>
 */

public class TimestampTraceValue extends LongTraceValue {

	/**
	 * Creates a new TimestampTraceValue
	 */
	public TimestampTraceValue() {
		super(TraceValueType.TIMESTAMP_TYPE);
	}

	/**
	 * Creates a new TimestampTraceValue
	 * @param timestamp The timestamp value for this trace
	 */
	public TimestampTraceValue(long timestamp) {
		super(TraceValueType.TIMESTAMP_TYPE);
		this.value = timestamp;
	}

	/**
	 * Returns the UTC timestamp as a double
	 * @return the UTC timestamp as a double
	 */
	@Override
	public double getNativeValue() {
		return value;
	}

	/**
	 * Returns the UTC timestamp 
	 * @return the UTC timestamp 
	 */
	@Override
	public Object getValue() {
		return value;
	}
	
	/**
	 * Returns the UTC timestamp 
	 * @return the UTC timestamp 
	 */
	public long getTimestamp() {
		return value;
	}
	
	/**
	 * Returns the java Date of the timestamp
	 * @return the java Date of the timestamp
	 */
	public Date getDate() {
		return new Date(value);
	}

}
