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
import java.util.Arrays;

//import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: ByteArrayTraceValue</p>
 * <p>Description: TraceValue implementation for byte arrays</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.ByteArrayTraceValue</code></p>
 */
//@XStreamAlias("ByteArrayTraceValue")
public class ByteArrayTraceValue extends AbstractTraceValue {
	//@XStreamAlias("value")
	protected byte[] value = null;
	/**
	 * Creates a new ByteArrayTraceValue
	 */
	public ByteArrayTraceValue() {
		super(TraceValueType.BYTES_TYPE);
	}

	/**
	 * Creates a new ByteArrayTraceValue
	 * @param byte[] the trace value
	 */
	public ByteArrayTraceValue(byte...value) {
		this();
		this.value = value;
	}

	/**
	 * Returns the primary value of this trace value
	 * @return the primary value of this trace value
	 */
	public byte[] getValue() {
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
		int size = in.readInt();
		value = new byte[size];
		if(size>0) {
			in.read(value);
		}
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(value==null ? 0 : value.length);
		if(value!=null) out.write(value);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(value);
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
		ByteArrayTraceValue other = (ByteArrayTraceValue) obj;
		if (!Arrays.equals(value, other.value))
			return false;
		return true;
	}
	

}
