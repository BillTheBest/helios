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
 * <p>Title: StringsTraceValue</p>
 * <p>Description: Marker class that is useful only because the interval trace needs to distinguish between String and Strings values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.StringsTraceValue</code></p>
 */
@XStreamAlias("StringsTraceValue")
public class StringsTraceValue extends StringTraceValue {

	/**
	 * Creates a new StringsTraceValue. For extern only 
	 */
	public StringsTraceValue() {
		super(TraceValueType.STRINGS_TYPE);
	}
	
	/**
	 * Creates a new StringsTraceValue
	 * @param value the initial value
	 */
	public StringsTraceValue(CharSequence value) {
		super(TraceValueType.STRINGS_TYPE);
		this.value = value==null ? "" : value.toString();
	}
	
}
