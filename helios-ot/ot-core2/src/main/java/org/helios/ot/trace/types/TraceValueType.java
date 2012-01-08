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

import gnu.trove.map.hash.TIntObjectHashMap;

import org.helios.ot.trace.types.ITraceValueFactory.ByteArrayTraceValueFactory;
import org.helios.ot.trace.types.ITraceValueFactory.IncidentTraceValueFactory;
import org.helios.ot.trace.types.ITraceValueFactory.IntTraceValueFactory;
import org.helios.ot.trace.types.ITraceValueFactory.LongTraceValueFactory;
import org.helios.ot.trace.types.ITraceValueFactory.StringTraceValueFactory;
import org.helios.ot.trace.types.ITraceValueFactory.StringsTraceValueFactory;
import org.helios.ot.trace.types.ITraceValueFactory.TimestampTraceValueFactory;

/**
 * <p>Title: TraceValueType</p>
 * <p>Description: Enumerates the supported trace value types that can be represented in a Trace. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.TraceValueType</code></p>
 */

public enum TraceValueType  {
	// ============================
	// Single value types
	// ============================
	INT_TYPE(false, false, int.class, new IntTraceValueFactory<IntTraceValue>()),
	LONG_TYPE(false, false, long.class, new LongTraceValueFactory<LongTraceValue>()),
	STRING_TYPE(false, false, String.class, new StringTraceValueFactory<StringTraceValue>()),
	STRINGS_TYPE(false, false, String.class, new StringsTraceValueFactory<StringsTraceValue>()),
	INCIDENT_TYPE(false, false, int.class, new IncidentTraceValueFactory<IncidentTraceValue>()),
	TIMESTAMP_TYPE(false, false, long.class, new TimestampTraceValueFactory<TimestampTraceValue>()),
	BYTES_TYPE(false, false, byte[].class, new ByteArrayTraceValueFactory<ByteArrayTraceValue>()),
	// ============================
	// Interval value types
	// ============================
	INTERVAL_INT_TYPE(true, true, int.class, INT_TYPE.factory),
	INTERVAL_LONG_TYPE(true, true, long.class, LONG_TYPE.factory),
	INTERVAL_STRING_TYPE(true, false, String.class, STRING_TYPE.factory),
	INTERVAL_STRINGS_TYPE(true, false, String[].class, STRINGS_TYPE.factory),
	INTERVAL_INCIDENT_TYPE(true, false, long.class, INCIDENT_TYPE.factory),
	INTERVAL_TIMESTAMP_TYPE(true, true, long.class, TIMESTAMP_TYPE.factory),
	INTERVAL_BYTES_TYPE(true, false, byte[].class, BYTES_TYPE.factory);

	
	public Class<? extends ITraceValueFactory> getTraceValueClass() {
		return this.factory.getClass();
	}
	
	/**
	 * Creates a new TraceValueType
	 * @param interval Indicates if this is an interval type
	 * @param minMaxAvg Indicates if this is an interval minMaxAvg type 
	 * @param baseType The base data type class 
	 * @param factory The factory for TraceValues and IntervalTraceValues
	 */
	private TraceValueType(boolean interval, boolean minMaxAvg, Class<?> baseType, ITraceValueFactory<? extends ITraceValue> factory) {
		this.interval = interval;
		this.minMaxAvg = minMaxAvg;
		this.baseType = baseType;
		this.factory = factory;		
	}
	
	/** Indicates if this is an interval type */
	private final boolean interval;
	/** Indicates if this is an interval minMaxAvg type */
	private final boolean minMaxAvg;
	/** The base data type class */
	private final Class<?> baseType;
	/** The data type factory class */
	private final ITraceValueFactory<? extends ITraceValue> factory;
	
	
	//=======
	
	
	
	
	
	/** A decode map between the ordinal and the type  */
	private static final TIntObjectHashMap<TraceValueType> CODE2TYPE = new TIntObjectHashMap<TraceValueType>(TraceValueType.values().length);
	
	static {
		// Populate the CODE2TYPE decode map
		for(TraceValueType type: TraceValueType.values()) {
			CODE2TYPE.put(type.ordinal(), type);
		}
	}
	

	
	/**
	 * Returns the TraceValueType for the passed code
	 * @param code The code of the TraceValueType to decode 
	 * @return A TraceValueType or null if one did not map to the passed code
	 */
	public static TraceValueType forCode(int code) {
		return CODE2TYPE.get(code);
	}
	
	/**
	 * Returns the TraceValueType for the passed name
	 * @param name The name to decode
	 * @return a TraceValueType or null if one did not map to the passed name
	 */
	public static TraceValueType forName(String name) {
		if(name==null) return null;
		name = name.toUpperCase().trim();
		try {
			return TraceValueType.valueOf(name);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Indicates if this is an interval type
	 * @return true if this is an interval type
	 */
	public boolean isInterval() {
		return interval;
	}
	/**
	 * The base data type class
	 * @return the baseType
	 */
	public Class<?> getBaseType() {
		return baseType;
	}
	

	/**
	 * Returns the trace factory
	 * @return the factory
	 */
	public ITraceValueFactory<? extends ITraceValue> getFactory() {
		return factory;
	}

	/**
	 * @return the minMaxAvg
	 */
	public boolean isMinMaxAvg() {
		return minMaxAvg;
	}


	
}



///** Int type constant */
//public static final int INT_TYPE = 0;
///** Long type constant */
//public static final int LONG_TYPE = 1;
///** String type constant */
//public static final int STRING_TYPE = 2;
///** Byte Array type constant */
//public static final int BYTE_TYPE = 3;

