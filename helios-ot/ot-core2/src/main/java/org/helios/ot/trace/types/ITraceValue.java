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
 * <p>Title: ITraceValue</p>
 * <p>Description: An abstraction of the value of an individual trace.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.ITraceValue</code></p>
 */

public interface ITraceValue {
	
	/** A toString() formatter for ITraceValues */
	public static final String TAB = "\n\t\t";
	
	/**
	 * Indicates if this is an interval type
	 * @return true if this is an interval type
	 */
	public boolean isInterval();
	
	/**
	 * Returns the primary value of this trace value
	 * @return the primary value of this trace value
	 */
	public Object getValue();
	
	/**
	 * Returns the trace value type of this instance
	 * @return the traceValueType
	 */
	public TraceValueType getTraceValueType();	
}
