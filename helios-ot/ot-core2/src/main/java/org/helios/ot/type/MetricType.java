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
package org.helios.ot.type;

import static org.helios.ot.trace.types.TraceValueType.BYTES_TYPE;
import static org.helios.ot.trace.types.TraceValueType.INTERVAL_BYTES_TYPE;
import static org.helios.ot.trace.types.TraceValueType.INCIDENT_TYPE;
import static org.helios.ot.trace.types.TraceValueType.INTERVAL_INCIDENT_TYPE;
import static org.helios.ot.trace.types.TraceValueType.INT_TYPE;
import static org.helios.ot.trace.types.TraceValueType.INTERVAL_INT_TYPE;
import static org.helios.ot.trace.types.TraceValueType.LONG_TYPE;
import static org.helios.ot.trace.types.TraceValueType.INTERVAL_LONG_TYPE;
import static org.helios.ot.trace.types.TraceValueType.STRINGS_TYPE;
import static org.helios.ot.trace.types.TraceValueType.INTERVAL_STRINGS_TYPE;
import static org.helios.ot.trace.types.TraceValueType.STRING_TYPE;
import static org.helios.ot.trace.types.TraceValueType.INTERVAL_STRING_TYPE;
import static org.helios.ot.trace.types.TraceValueType.TIMESTAMP_TYPE;
import static org.helios.ot.trace.types.TraceValueType.INTERVAL_TIMESTAMP_TYPE;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;

import org.helios.helpers.ExternalizationHelper;
import org.helios.helpers.StreamHelper;
import org.helios.ot.trace.types.ByteArrayTraceValue;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.trace.types.ITraceValueFactory;
import org.helios.ot.trace.types.IntTraceValue;
import org.helios.ot.trace.types.LongTraceValue;
import org.helios.ot.trace.types.StringTraceValue;
import org.helios.ot.trace.types.TimestampTraceValue;
import org.helios.ot.trace.types.TraceValueType;
import org.helios.ot.trace.types.interval.IIntervalTraceValue;

/**
 * <p>Title: MetricType</p>
 * <p>Description: Enumerates the different metric types supported by <code>ITracer</code>s.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.type.MetricType</code></p>
 */
@SuppressWarnings("unchecked")
public enum MetricType {
	/** Interval averaged integer */
	INT_AVG (false, false, true, INT_TYPE, INTERVAL_INT_TYPE),
	/** Interval averaged long */
	LONG_AVG (false, false, true, LONG_TYPE, INTERVAL_LONG_TYPE),
	/** Sticky averaged integer */
	STICKY_INT_AVG (true, false, true, INT_TYPE, INTERVAL_INT_TYPE),
	/** Sticky averaged long */
	STICKY_LONG_AVG (true, false, true, LONG_TYPE, INTERVAL_LONG_TYPE),
	/** Delta integer */
	DELTA_INT_AVG (false, true, true, INT_TYPE, INTERVAL_INT_TYPE),
	/** Delta long */
	DELTA_LONG_AVG (false, true, true, LONG_TYPE, INTERVAL_LONG_TYPE),
	/** Sticky Delta integer */
	STICKY_DELTA_INT_AVG (true, true, true, INT_TYPE, INTERVAL_INT_TYPE),
	/** Sticky Delta long */
	STICKY_DELTA_LONG_AVG (true, true, true, LONG_TYPE, INTERVAL_LONG_TYPE),
	/** Interval incident count */
	INTERVAL_INCIDENT (false, false, false, INCIDENT_TYPE, INTERVAL_INCIDENT_TYPE),
	/** Timestamp */
	TIMESTAMP (true, false, false, TIMESTAMP_TYPE, INTERVAL_TIMESTAMP_TYPE),
	/** String message */
	STRING(true, false, false, STRING_TYPE, INTERVAL_STRING_TYPE),
	/** All distinct strings for an interval  */
	STRINGS(false, false, false, STRINGS_TYPE, INTERVAL_STRINGS_TYPE),
	/** A byte array  */
	BYTES(false, false, false, BYTES_TYPE, INTERVAL_BYTES_TYPE);
	
	/**
	 * Creates a new MetricType
     * @param sticky Indicates if values are retained across intervals
     * @param delta Indicates if this type expects monotonically increasing values
     * @param traceValueType The underlying TraceValueType which represents the raw value type for this MetricType.
     * @param intervalTraceValueType The underlying Interval TraceValueType which represents the raw interval value type for this MetricType.
     */
    private MetricType(boolean sticky, boolean delta, boolean minMaxAvg, TraceValueType traceValueType, TraceValueType intervalTraceValueType) {
        this.sticky = sticky;
        this.delta = delta;
        this.minMaxAvg = minMaxAvg;
        this.traceValueType = traceValueType;
        this.intervalTraceValueType = intervalTraceValueType;
        factory = this.traceValueType.getFactory();
        fillIn = (ITraceValue[]) Array.newInstance(factory.getTraceValueClass(), 0);
    }
    
    /** Indicates if values are retained across intervals */
    private final boolean sticky;
    /** Indicates if this type expects monotonically increasing values */
    private final boolean delta;
    /** Indicates if the interval type is a Min/Max/Avg */
    private final boolean minMaxAvg;
    
    /** The underlying TraceValueType which represents the raw value type for this MetricType. */
    private final TraceValueType traceValueType;
    /** The underlying Interval TraceValueType which represents the raw interval value type for this MetricType. */
    private final TraceValueType intervalTraceValueType;
    /** The trace value factory */
    private final ITraceValueFactory factory;
    /** A zero length trace value array typed for the factory to avoid some generics issues */
    private final ITraceValue[] fillIn;
    
	
	public ITraceValue traceValue(Object val) {
		return factory.createTraceValue(val);
	}
    
	public ITraceValue traceValue(Number val) {
		return factory.createTraceValue(val);
	}	
	
	public IIntervalTraceValue intervalTraceValue(ITraceValue...val) {
		if(val==null || val.length==0) val = fillIn;
		return TRACER_FACTORIES.get(this).createIntervalTraceValue(val);  //factory.createIntervalTraceValue(val);
	}
    
	
	public static final int TYPE_INT_AVG = INT_AVG.ordinal();
	public static final int TYPE_LONG_AVG = LONG_AVG.ordinal();
	public static final int TYPE_STICKY_INT_AVG = STICKY_INT_AVG.ordinal();
	public static final int TYPE_STICKY_LONG_AVG = STICKY_LONG_AVG.ordinal();
	public static final int TYPE_DELTA_INT_AVG = DELTA_INT_AVG.ordinal();
	public static final int TYPE_DELTA_LONG_AVG = DELTA_LONG_AVG.ordinal();
	public static final int TYPE_STICKY_DELTA_INT_AVG = DELTA_INT_AVG.ordinal();
	public static final int TYPE_STICKY_DELTA_LONG_AVG = DELTA_LONG_AVG.ordinal();
	public static final int TYPE_INTERVAL_INCIDENT = INTERVAL_INCIDENT.ordinal();
	public static final int TYPE_TIMESTAMP = TIMESTAMP.ordinal();
	public static final int TYPE_STRING = STRING.ordinal();
	public static final int TYPE_STRINGS = STRINGS.ordinal();
	public static final int TYPE_BYTES = BYTES.ordinal();
	
	public static final String HIGH = "High";
	public static final String LOW = "Low";
	public static final String AVERAGE = "Avg";
	public static final String COUNT = "Count";
	public static final String EARLIEST = "Earliest";
	public static final String LATEST = "Latest";
	public static final String MEDIAN = "Median";
	public static final String LAST = "Last Message";
	
	
	

	
     
    private static final int innerSize = MetricType.values().length;
    private static final TIntObjectHashMap<MetricType> CODE2TYPE = new TIntObjectHashMap<MetricType>(innerSize);
    private static final TIntObjectHashMap<String> DESCRIPTIONS = new TIntObjectHashMap<String>(11);
    private static final TIntObjectHashMap<Class<?>> INTERVAL_DATA_TYPE = new TIntObjectHashMap<Class<?>>(innerSize);
    private static final Map<MetricType, ITraceValueFactory> TRACER_FACTORIES = new HashMap<MetricType, ITraceValueFactory>(innerSize);
    
    
    static {
    	
    	DESCRIPTIONS.put(TYPE_INT_AVG, "Interval averaged integer");
    	DESCRIPTIONS.put(TYPE_LONG_AVG, "Interval averaged long");
    	DESCRIPTIONS.put(TYPE_STICKY_INT_AVG, "Sticky averaged integer");
    	DESCRIPTIONS.put(TYPE_STICKY_LONG_AVG, "Sticky averaged long");
    	DESCRIPTIONS.put(TYPE_DELTA_INT_AVG, "Delta integer");
    	DESCRIPTIONS.put(TYPE_DELTA_LONG_AVG, "Delta long");
    	DESCRIPTIONS.put(TYPE_STICKY_DELTA_INT_AVG, "Sticky Delta integer");
    	DESCRIPTIONS.put(TYPE_STICKY_DELTA_LONG_AVG, "Sticky Delta long");
    	DESCRIPTIONS.put(TYPE_INTERVAL_INCIDENT, "Interval incident count");
    	DESCRIPTIONS.put(TYPE_TIMESTAMP, "Interval Timestamp Range");    	
    	DESCRIPTIONS.put(TYPE_STRING, "Last message of the interval");
    	DESCRIPTIONS.put(TYPE_STRINGS, "All messages in the interval");
    	DESCRIPTIONS.put(TYPE_BYTES, "All byte arrays in the interval");
    	
    	INTERVAL_DATA_TYPE.put(TYPE_INT_AVG, CompositeData.class);
    	INTERVAL_DATA_TYPE.put(TYPE_LONG_AVG, CompositeData.class);
    	INTERVAL_DATA_TYPE.put(TYPE_STICKY_INT_AVG, CompositeData.class);
    	INTERVAL_DATA_TYPE.put(TYPE_STICKY_LONG_AVG, CompositeData.class);
    	INTERVAL_DATA_TYPE.put(TYPE_DELTA_INT_AVG, CompositeData.class);
    	INTERVAL_DATA_TYPE.put(TYPE_DELTA_LONG_AVG, CompositeData.class);
    	INTERVAL_DATA_TYPE.put(TYPE_STICKY_DELTA_INT_AVG, CompositeData.class);
    	INTERVAL_DATA_TYPE.put(TYPE_STICKY_DELTA_LONG_AVG, CompositeData.class);
    	INTERVAL_DATA_TYPE.put(TYPE_INTERVAL_INCIDENT, Long.class);
    	INTERVAL_DATA_TYPE.put(TYPE_TIMESTAMP, CompositeData.class);    	
    	INTERVAL_DATA_TYPE.put(TYPE_STRING, String.class);
    	INTERVAL_DATA_TYPE.put(TYPE_STRINGS, new String[0].getClass());

    	
    	for(MetricType mt: MetricType.values()) {
        	CODE2TYPE.put(mt.ordinal(), mt);
        	TRACER_FACTORIES.put(mt, mt.factory);
    	}
    	

    }

    /**
     * Determines if this MetricType is numeric (int or long)
     * @return true if the data type is numeric.
     */    
    public boolean isNumber() {
    	return (this.isLong() || this.isInt() || this.isIncident() || this.equals(TIMESTAMP));
    }
    
    /**
     * Returns true if this is a timestamp type
     * @return true if this is a timestamp type
     */
    public boolean isTimestamp() {
    	return this.equals(TIMESTAMP);
    }
    
    /**
     * Determines if this MetricType is of the Long data type.
     * @return true if the data type is long.
     */
    public boolean isLong() {
    	return this.name().contains("LONG_");
    }
    
    /**
     * Determines if this MetricType is of the Int data type.
     * @return true if the data type is int.
     */
    public boolean isInt() {
    	return this.name().contains("INT_") || this.equals(MetricType.INTERVAL_INCIDENT);
    }
    
    
    
        
    
    
    /**
     * Returns the description of the metric type.
     * @return the description of the metric type.
     */
    public String getDescription() {
    	return DESCRIPTIONS.get(ordinal());
    }
    
    public Class<?> getIntervalDataReturnType() {
    	return INTERVAL_DATA_TYPE.get(ordinal());
    }
    
    /**
     * Indicates if this metric type is a string or strings type
     * @return true if metric type is a string or strings type, false otherwise
     */
    public boolean isString() {
    	return this.equals(STRING) || this.equals(STRINGS); 
    }
    
 
    /**
     * Decodes an int type code to a MetricType.
     * @param i A MetricType int code.
     * @return The corresponding MetricType.
     */
    public static MetricType typeForCode(int i) {
    	if(!CODE2TYPE.containsKey(i)) {
    		throw new RuntimeException("The code [" + i + "] is not a valid MetricType Code.");
    	}
    	return CODE2TYPE.get(i);
    }
    
    /**
     * Decodes an string type code to a MetricType.
     * @param name
     * @return
     */
    public static MetricType typeForCode(String name) {
    	return MetricType.valueOf(name);
    }
    
    /**
     * Decodes an int type code to a MetricType.
     * @param i
     * @return
     */
    public static MetricType valueOf(int i) {
    	return typeForCode(i);
    }

	/**
	 * The internal code of the metric type.
	 * @return the code
	 */
	public int getCode() {
		return ordinal();
	}
	
	/**
	 * The name of the metric type.
	 * Delegates to <code>name()</code>
	 * @return the name of the metric type.
	 */
	public String getName() {
		return name();
	}

	/**
	 * Indicates if the metric type is sticky.
	 * @return the sticky
	 */
	public boolean isSticky() {
		return sticky;
	}
	
	/**
	 * Indicates if this is an incident type.
	 * @return
	 */
	public boolean isIncident() {
		return(INTERVAL_INCIDENT.equals(this));
	}

	/**
	 * Indicates if the metric type is delta.
	 * @return the delta
	 */
	public boolean isDelta() {
		return delta;
	}

	/**
	 * The type of the MetricType's TraceValueType.
	 * @return the traceValueType
	 */
	public TraceValueType getValueType() {
		return traceValueType;
	}
	
	/**
	 * Reads a value from the input stream and attempts to decode to an ITraceValue
	 * @param is The input stream to read from
	 * @return An ITraceValue
	 */
	public static ITraceValue traceValue(InputStream is) {
		ITraceValue value = null;
		try {
			Object obj = null;
			byte[] bytes = StreamHelper.readByteArrayFromStream(is);
			try {
				obj = ExternalizationHelper.deserialize(bytes);
				if(obj instanceof ITraceValue) {
					return (ITraceValue)obj;
				} else if(obj instanceof CharSequence) {
					return new StringTraceValue(obj.toString());
				} else if(obj instanceof Number) {
					Number n = (Number)obj;
					Integer i = isInt(n);
					if(i!=null) return new IntTraceValue(i);
					Long l = isLong(n);
					if(l!=null) {
						return new LongTraceValue(l);
					} else {
						throw new RuntimeException("Number was out of range for an ITraceInterval [" + n + "]", new Throwable());
					}
				} else if(obj instanceof Date) {
					return new TimestampTraceValue(((Date)obj).getTime());
				}
				return null;
			} catch (Exception e) {
				return new ByteArrayTraceValue(bytes);
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ITraceValue from input stream", e);
		}
	}
	
	/**
	 * Reads a value from the input stream and attempts to decode to an ITraceValue
	 * @param is The input stream to read from
	 * @param type The expected metric type of the object in the stream 
	 * @return An ITraceValue
	 */
	public static ITraceValue traceValue(InputStream is, MetricType type) {
		Object obj = null;
		byte[] bytes = null;
		try {
			bytes = StreamHelper.readByteArrayFromStream(is);
		} catch (Exception e) {
			throw new RuntimeException("Failed to read bytes from InputStream", new Throwable());
		}
		try {
			obj = ExternalizationHelper.deserialize(bytes);
			return type.traceValue(obj);
		} catch (Exception e) {
			return new ByteArrayTraceValue(bytes);
		}
		
	}
	
	public static Integer isInt(Number number) {
		try {
			int i = number.intValue();
			return i;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static Long isLong(Number number) {
		try {
			long i = number.longValue();
			return i;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Returns the underlying Interval TraceValueType which represents the raw interval value type for this MetricType.
	 * @return the intervalTraceValueType
	 */
	public TraceValueType getIntervalTraceValueType() {
		return intervalTraceValueType;
	}

	/**
	 * Determines if this metric type is a Min/Max/Avg
	 * @return the minMaxAvg
	 */
	public boolean isMinMaxAvg() {
		return minMaxAvg;
	}


	
		
}

