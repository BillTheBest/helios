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
package org.helios.tracing.trace;

import org.helios.tracing.TraceValue;

/**
 * <p>Title: InvalidTypeRequestException</p>
 * <p>Description: Exception thrown when a request is made to a trace value for the incorrect type</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.InvalidTypeRequestException</code></p>
 */

public class InvalidTypeRequestException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = -6729788342225996419L;

	/**
	 * Creates a new InvalidTypeRequestException
	 * @param requested The requested type
	 * @param actual The actual type
	 */
	public InvalidTypeRequestException(int requested, int actual) {
		super("Requested value of type [" + TraceValue.TYPE_MAP.get(requested) + "] from a TraceValue of type [" + TraceValue.TYPE_MAP.get(actual) + "]");
	}

	/**
	 * Creates a new InvalidTypeRequestException
	 * @param requested The requested type
	 * @param actual The actual type
	 * @param cause A cause so we can provide a stack trace
	 */
	public InvalidTypeRequestException(int requested, int actual, Throwable cause) {
		super("Requested value of type [" + TraceValue.TYPE_MAP.get(requested) + "] from a TraceValue of type [" + TraceValue.TYPE_MAP.get(actual) + "]", cause);
	}

}
