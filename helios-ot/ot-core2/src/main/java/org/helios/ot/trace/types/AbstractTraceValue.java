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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * <p>Title: AbstractTraceValue</p>
 * <p>Description: The base abstract class for all trace values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.AbstractTraceValue</code></p>
 */
//@XmlRootElement(name="value")
public abstract class AbstractTraceValue implements ITraceValue, Externalizable {
	/** The TraceValueType of the type of this trace value */
	//@XStreamAlias("traceValueType")
	@XStreamOmitField
	protected TraceValueType traceValueType;
	/** Constant to indicate this is not an interval trace */
	@XStreamAlias("interval")
	protected final boolean interval = false;
	
	/**
	 * Indicates if this is an interval type
	 * @return true if this is an interval type
	 */
	@XmlElement(name="interval")	
	public boolean isInterval() {
		return interval;
	}
	
	
	/**
	 * Creates a new AbstractTraceValue
	 * @param traceValueTypeId The TraceValueType ordinal of the type of this trace value
	 */
	protected AbstractTraceValue(int traceValueTypeId) {
		TraceValueType tvt = TraceValueType.forCode(traceValueTypeId);
		if(tvt==null) throw new IllegalArgumentException("Passed TraceValueTypeId [" + traceValueTypeId + "] is not a valid TraceValueType ordinal", new Throwable());
		this.traceValueType = tvt;
	}
	
	/**
	 * Creates a new AbstractTraceValue
	 * @param traceValueType The TraceValueType type of this trace value
	 */
	protected AbstractTraceValue(TraceValueType traceValueType) {
		if(traceValueType==null) throw new IllegalArgumentException("Passed TraceValueType was null", new Throwable());
		this.traceValueType = traceValueType;
	}
	
	
	/**
	 * Reads the state of this object in from the Object input stream
	 * @param in the stream to read data from in order to restore the object 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	//@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int typeId = in.readInt();
		traceValueType = TraceValueType.forCode(typeId);
		if(traceValueType==null) {
			throw new IOException("The int value read [" + typeId + "] is not a valid ordinal for the TraceValueType", new Throwable());
		}
	}

	/**
	 * Writes this object out to the Object output stream
	 * @param out the stream to write the object to 
	 * @throws IOException
	 */
	//@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(traceValueType.ordinal());
	}


	/**
	 * Returns the trace value type of this instance
	 * @return the traceValueType
	 */
	@XmlElement(name="traceValueType")
	public TraceValueType getTraceValueType() {
		return traceValueType;
	}
	
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    return getValue().toString();
	}
	

}
