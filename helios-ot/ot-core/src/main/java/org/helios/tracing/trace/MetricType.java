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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.openmbean.CompositeData;


/**
 * <p>Title: MetricType</p>
 * <p>Description: Enumerates the different metric types supported by <code>ITracer</code>s.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1633 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/MetricType.java $
 * $Id: MetricType.java 1633 2009-10-19 23:01:15Z nwhitehead $ 
 */
public enum MetricType {
	/** Interval averaged integer */
	INT_AVG (false, false, 1, Integer.TYPE),
	/** Interval averaged long */
	LONG_AVG (false, false, 2, Long.TYPE),
	/** Sticky averaged integer */
	STICKY_INT_AVG (true, false, 3, Integer.TYPE),
	/** Sticky averaged long */
	STICKY_LONG_AVG (true, false, 4, Long.TYPE),
	/** Delta integer */
	DELTA_INT_AVG (false, true, 5, Integer.TYPE),
	/** Delta long */
	DELTA_LONG_AVG (false, true, 6, Long.TYPE),
	/** Sticky Delta integer */
	STICKY_DELTA_INT_AVG (true, true, 7, Integer.TYPE),
	/** Sticky Delta long */
	STICKY_DELTA_LONG_AVG (true, true, 8, Long.TYPE),
	/** Interval incident count */
	INTERVAL_INCIDENT (false, false, 9, Long.TYPE),
	/** Timestamp */
	TIMESTAMP (true, false, 10, Date.class),
	/** String message */
	STRING(true, false, 11, String.class),
	/** All distinct strings for an interval  */
	STRINGS(false, false, 12, String.class);
	
	
	
	private static AtomicInteger counter = new AtomicInteger(0);
	
	public static final int TYPE_INT_AVG = 1;
	public static final int TYPE_LONG_AVG = 2;
	public static final int TYPE_STICKY_INT_AVG = 3;
	public static final int TYPE_STICKY_LONG_AVG = 4;
	public static final int TYPE_DELTA_INT_AVG = 5;
	public static final int TYPE_DELTA_LONG_AVG = 6;
	public static final int TYPE_STICKY_DELTA_INT_AVG = 7;
	public static final int TYPE_STICKY_DELTA_LONG_AVG = 8;
	public static final int TYPE_INTERVAL_INCIDENT = 9;
	public static final int TYPE_TIMESTAMP = 10;
	public static final int TYPE_STRING = 11;
	public static final int TYPE_STRINGS = 12;
	
	public static final String HIGH = "High";
	public static final String LOW = "Low";
	public static final String AVERAGE = "Avg";
	public static final String COUNT = "Count";
	public static final String EARLIEST = "Earliest";
	public static final String LATEST = "Latest";
	public static final String MEDIAN = "Median";
	public static final String LAST = "Last Message";
	
	
	

	
    private int code; 
    private boolean sticky = false;
    private boolean delta = false;
    private Class<?> valueType = null;
    private static Map<Integer, MetricType> CODE2TYPE = new HashMap<Integer, MetricType>(11);
    private static Map<Integer, String> DESCRIPTIONS = new HashMap<Integer, String>(11);
    private static Map<Integer, Class<?>> INTERVAL_DATA_TYPE = new HashMap<Integer, Class<?>>(11);
    
    
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
    	
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.INT_AVG);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.LONG_AVG);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.STICKY_INT_AVG);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.STICKY_LONG_AVG);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.DELTA_INT_AVG);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.DELTA_LONG_AVG);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.STICKY_DELTA_INT_AVG);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.STICKY_DELTA_LONG_AVG);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.INTERVAL_INCIDENT);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.TIMESTAMP);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.STRING);
    	CODE2TYPE.put(counter.incrementAndGet(), MetricType.STRINGS);
    	
    	
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


    }

    /**
     * Determines if this MetricType is numeric (int or long)
     * @return true if the data type is numeric.
     */    
    public boolean isNumber() {
    	return (this.isLong() || this.isInt() || this.isIncident());
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
     * Determines if this MetricType is of the String data type.
     * @return true if the data type is String.
     */
    public boolean isString() {
    	return this.name().contains("STRING");
    }
    
    
        
    
    
    /**
     * Returns the description of the metric type.
     * @return the description of the metric type.
     */
    public String getDescription() {
    	return DESCRIPTIONS.get(code);
    }
    
    public Class<?> getIntervalDataReturnType() {
    	return INTERVAL_DATA_TYPE.get(code);
    }
    
    /**
     * @param sticky
     * @param delta
     * @param code
     * @param valueType
     */
    private MetricType(boolean sticky, boolean delta, int code, Class<?> valueType) {
        this.sticky = sticky;
        this.delta = delta;
        this.code = code;
        this.valueType = valueType;
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
		return code;
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
		return(code==9);
	}

	/**
	 * Indicates if the metric type is delta.
	 * @return the delta
	 */
	public boolean isDelta() {
		return delta;
	}

	/**
	 * The type of the MetricType's value.
	 * @return the valueType
	 */
	public Class<?> getValueType() {
		return valueType;
	}
	
	
		
}

