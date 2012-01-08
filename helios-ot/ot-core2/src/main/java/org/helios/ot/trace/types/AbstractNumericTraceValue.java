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

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: AbstractNumericTraceValue</p>
 * <p>Description: The base abstract class for all numeric trace values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.AbstractNumericTraceValue</code></p>
 */
//@XStreamAlias("value")
public abstract class AbstractNumericTraceValue extends AbstractTraceValue implements INumericTraceValue {


	/**
	 * Creates a new AbstractNumericTraceValue
	 * @param traceValueTypeId
	 */
	public AbstractNumericTraceValue(int traceValueTypeId) {
		super(traceValueTypeId);
	}

	/**
	 * Creates a new AbstractNumericTraceValue
	 * @param traceValueType
	 */
	public AbstractNumericTraceValue(TraceValueType traceValueType) {
		super(traceValueType);
	}


}
