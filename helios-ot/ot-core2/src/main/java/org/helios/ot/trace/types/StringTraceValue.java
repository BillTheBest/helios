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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.helios.helpers.ExternalizationHelper;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: StringTraceValue</p>
 * <p>Description: TraceValue implementation for strings.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.StringTraceValue</code></p>
 */
@XStreamAlias("StringTraceValue")
public class StringTraceValue  extends AbstractTraceValue {
	/** The value */
	@XStreamAlias("value")
	protected String value = "";

	/**
	 * For extern only 
	 */
	public StringTraceValue() {
		super(TraceValueType.STRING_TYPE);
	}
	
	/**
	 * Creates a new StringTraceValue. Parent Ctor for child impls.
	 * @param type The type of the child impl.
	 */
	protected StringTraceValue(TraceValueType type) {
		super(type);
	}
	
	/**
	 * Creates a new StringTraceValue. This ctor is only used by the StringsTraceValue.
	 * @param value The string value to initialize the trace with
	 */
	public StringTraceValue(CharSequence value) {
		this();
		this.value = value==null ? null : value.toString();
	}
	
	/**
	 * Returns the primary value of this trace value
	 * @return the primary value of this trace value
	 */
	public String getValue() {
		return value;
	}
	


	/**
	 * Reads the state of this object in from the Object input stream
	 * @param in the stream to read data from in order to restore the object 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		value = ExternalizationHelper.unExternalizeString(in);
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);				
		ExternalizationHelper.externalizeString(out, value);	
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringTraceValue other = (StringTraceValue) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}