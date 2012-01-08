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
package org.helios.tracing;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Map;

import org.helios.helpers.CollectionHelper;
import org.helios.helpers.ExternalizationHelper;
import org.helios.tracing.trace.InvalidTypeRequestException;
import org.helios.tracing.trace.MetricType;

/**
 * <p>Title: TraceValue</p>
 * <p>Description: Container class for the Trace value </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.TraceValue</code></p>
 */

public class TraceValue implements Externalizable {
	
	/** The type code */
	private int type;
	/** An int type value */
	private int intValue;
	/** A long type value */
	private long longValue;
	/** A string type value */
	private String stringValue;
	/** A byte array type value */
	private byte[] byteArrValue;
	
	/** Int type constant */
	public static final int INT_TYPE = 0;
	/** Long type constant */
	public static final int LONG_TYPE = 1;
	/** String type constant */
	public static final int STRING_TYPE = 2;
	/** Byte Array type constant */
	public static final int BYTE_TYPE = 3;
	
	/** Name decodes for type constants */
	public static final Map<Object, Object> TYPE_MAP = Collections.unmodifiableMap(CollectionHelper.createMap(INT_TYPE, "INT_TYPE", LONG_TYPE, "LONG_TYPE", STRING_TYPE, "STRING_TYPE", BYTE_TYPE, "BYTE_TYPE"));
	
	/**
	 * For externalization only
	 */
	public TraceValue() {}
	
	/**
	 * Creates a new TraceValue
	 * @param type The MetricType
	 * @param value The value
	 * @return a new TraceValue
	 */
	public static TraceValue create(MetricType type, Object value) {
		if(type==null) throw new IllegalArgumentException("Passed type was null", new Throwable());
		if(value==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
		if(type.isString()) {
			return new TraceValue(value.toString());
		} else if(type.isNumber()) {			
			if(type.isInt()) {
				if(value instanceof CharSequence) {
					return new TraceValue(Integer.parseInt(value.toString()));
				} else {
					return new TraceValue(((Number)value).intValue());
				}				
			} else {
				if(value instanceof CharSequence) {
					return new TraceValue(Long.parseLong(value.toString()));
				} else {
					return new TraceValue(((Number)value).longValue());
				}				
			}
		} else if(type.equals(MetricType.TIMESTAMP)) {
			return new TraceValue((Long)value);
		} else {
			throw new RuntimeException("Unsupported Metric Type:" + type, new Throwable());
		}
	}
	
	/**
	 * Creates a new int value TraceValue 
	 * @param value The int value
	 */
	public TraceValue(int value) {
		type = INT_TYPE;
		intValue = value;
		longValue = Long.MIN_VALUE;
		stringValue = null;
		byteArrValue = null;
	}
	
	/**
	 * Creates a new long value TraceValue 
	 * @param value The long value
	 */
	public TraceValue(long value) {
		type = LONG_TYPE;
		longValue = value;
		intValue = Integer.MIN_VALUE;		
		stringValue = null;
		byteArrValue = null;
	}
	
	/**
	 * Creates a new string value TraceValue 
	 * @param value The string value
	 */
	public TraceValue(CharSequence value) {
		type = INT_TYPE;
		stringValue = value.toString();
		longValue = Long.MIN_VALUE;
		intValue = Integer.MIN_VALUE;				
		byteArrValue = null;
	}
	
	/**
	 * Creates a new byte array value TraceValue 
	 * @param value The byte array
	 */
	public TraceValue(byte[] value) {
		type = INT_TYPE;
		byteArrValue = value;
		stringValue = null;
		longValue = Long.MIN_VALUE;
		intValue = Integer.MIN_VALUE;						
	}
	
	/**
	 * Creates a new byte array value TraceValue 
	 * @param is The byte array input stream
	 */
	public TraceValue(InputStream is) {
		type = BYTE_TYPE;		
		stringValue = null;
		longValue = Long.MIN_VALUE;
		intValue = Integer.MIN_VALUE;
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream(is.available());
			byte[] buffer = new byte[1024];
			int bytesRead = -1;
			while((bytesRead = is.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
			}
			byteArrValue = baos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read ByteArray from input stream", e);
		} finally {
			try { baos.close(); } catch (Exception e) {}
			try { is.close(); } catch (Exception e) {}
		}
	}
	

	/**
	 * Reads the TraceValue in
	 * @param in The ObjectInput
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		type = in.readInt();
		switch (type) {
		case INT_TYPE:
			intValue = in.readInt();
			break;
		case LONG_TYPE:
			longValue = in.readLong();
			break;
		case STRING_TYPE:
			stringValue = ExternalizationHelper.unExternalizeString(in);
			break;
		case BYTE_TYPE:
			byteArrValue = ExternalizationHelper.unExternalizeByteArray(in);
			break;
		}			
	}

	/**
	 * Writes the TraceValue out
	 * @param out The ObjectOutput
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(type);
		switch (type) {
		case INT_TYPE:
			out.writeInt(intValue);
			break;
		case LONG_TYPE:
			out.writeLong(longValue);
			break;
		case STRING_TYPE:
			ExternalizationHelper.externalizeString(out, stringValue);
			break;
		case BYTE_TYPE:
			ExternalizationHelper.externalizeByteArray(out, byteArrValue);
			break;
		}
	}
	
	/**
	 * Renders the value as a string
	 * @return the value rendered as a string
	 */
	public String toString() {
		switch (type) {
		case INT_TYPE:
			return "" + intValue;
		case LONG_TYPE:
			return "" + longValue;
		case STRING_TYPE:
			return stringValue;
		case BYTE_TYPE:
			return new String(byteArrValue);
		}	
		return null;
	}
	
	/**
	 * Returns the value as a generic object
	 * @return an object representing the value
	 */
	public Object getValue() {
		switch (type) {
		case INT_TYPE:
			return intValue;
		case LONG_TYPE:
			return longValue;
		case STRING_TYPE:
			return stringValue;
		case BYTE_TYPE:
			return byteArrValue;
		}	
		return null;		
	}
	
	/**
	 * Returns either an int or a long type as a long
	 * @return the numeric value expressed as a long
	 */
	public long getNumericValue() {
		if(type!=LONG_TYPE && type!=INT_TYPE) {
			throw new InvalidTypeRequestException(LONG_TYPE, type, new Throwable());
		}		
		if(type==INT_TYPE) return intValue;
		else return longValue;

	}

	/**
	 * Returns the type constant code
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Returns the int value
	 * @return the intValue
	 */
	public int getIntValue() {
		if(type!=INT_TYPE) {
			throw new InvalidTypeRequestException(INT_TYPE, type, new Throwable());
		}
		return intValue;
	}

	/**
	 * Returns the long value
	 * @return the longValue
	 */
	public long getLongValue() {
		if(type!=LONG_TYPE) {
			throw new InvalidTypeRequestException(LONG_TYPE, type, new Throwable());
		}		
		return longValue;
	}

	/**
	 * Returns the string value
	 * @return the stringValue
	 */
	public String getStringValue() {
		if(type!=STRING_TYPE) {
			throw new InvalidTypeRequestException(STRING_TYPE, type, new Throwable());
		}		
		return stringValue;
	}

	/**
	 * Returns the byte array value
	 * @return the byteArrValue
	 */
	public byte[] getByteArrValue() {
		if(type!=BYTE_TYPE) {
			throw new InvalidTypeRequestException(BYTE_TYPE, type, new Throwable());
		}		
		return byteArrValue;
	}

}
