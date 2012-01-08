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
package org.helios.ot.jmx;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.InvalidKeyException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.helios.helpers.ClassHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.opentypes.OpenTypeManager;
import org.helios.ot.trace.IntervalTrace;
import org.helios.ot.trace.types.interval.IMinMaxAvgIntervalTraceValue;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: OpenMetricType</p>
 * <p>Description: A CompositeData type that represents one OpenTrace published interval metric</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.jmx.OpenMetricType</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class OpenMetricType implements CompositeData, Serializable {
	/**  */
	private static final long serialVersionUID = 6971131010472112886L;
	
	/** The number of published fields in the composite type. THIS NEEDS TO BE UPDATED IF FIELDS ARE ADDED OR REMOVED */
	public static final int FIELD_CNT = 9;
	
	/** The values of the OpenMetricType */
	protected final Object[] values = new Object[FIELD_CNT];
	/** An ordered array of the keys of the composite type */
	private static String[] keys = new String[FIELD_CNT];
	
	/** The composite type for OpenMetricType */
	protected static CompositeType compositeType = null;
	/** Flag for an MinMaxAvg type */
	protected boolean minMaxAvg;
	
	/** The metric point */
	protected final String point;
		
	/** Decodes from the field key to the values array index */
	protected static final TObjectIntHashMap<String> INDEX = new TObjectIntHashMap<String>(9);
	/** Decode from index to key name */
	protected static final TIntObjectHashMap<String> NAMES = new TIntObjectHashMap<String>(9);
	/** Decode from index to description */
	protected static final TIntObjectHashMap<String> DESCRIPTIONS = new TIntObjectHashMap<String>(9);
	/** Decode from index to open type */
	protected static final TIntObjectHashMap<OpenType<?>> TYPES = new TIntObjectHashMap<OpenType<?>>(9);
	
	
	
	static {
		try {
			int i = 0;
			for(Method m: OpenMetricType.class.getDeclaredMethods()) {
				JMXAttribute attr = m.getAnnotation(JMXAttribute.class);				
				if(attr!=null && !attr.expose()) {
					INDEX.put(attr.name(), i);
					NAMES.put(i, attr.name());
					DESCRIPTIONS.put(i, attr.description());
					TYPES.put(i, OpenTypeManager.getInstance().getOpenType(m.getReturnType().getName()));
					i++;
				}
			}
			
			compositeType = new CompositeType(
					"OpenMetricType", 
					"OpenTrace published interval metric", 
					NAMES.values(new String[i]), 
					DESCRIPTIONS.values(new String[i]), 
					TYPES.values(new OpenType[i]));
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize OpenMetricType MetaData", e);
		}
		//NAMES.values(new String[NAMES.size()]);
		for(int x = 0; x < FIELD_CNT; x++) {
			keys[x] = NAMES.get(x);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public static void main(String[] args) {
		log(compositeType);
		log(INDEX);
		log(NAMES);
		
		OpenMetricType omt = new OpenMetricType("FOO", MetricType.INT_AVG.name());
		omt.set("StartTime", 0);
		omt.set("EndTime", 1);
		omt.set("Value", "3");
		omt.set("Count", 5);
		omt.set("Average", 10);
		omt.set("Maximum", 20);
		omt.set("Minimum", 2);
		
		log(Arrays.toString(omt.values));
		try {
			log(omt.writeReplace());
		} catch (ObjectStreamException e) {
			e.printStackTrace(System.err);
		}
		
	}
	
	
	
	/**
	 * Creates a new OpenMetricType 
	 * @param intervalTrace The interval trace to create the OpenMetricType for
	 */
	public OpenMetricType(IntervalTrace intervalTrace) {
		this(ClassHelper.nvl(intervalTrace.getFQN(), "The passed interval trace was null"), intervalTrace.getMetricType().name());
		minMaxAvg = (intervalTrace.getIntervalTraceValue() instanceof IMinMaxAvgIntervalTraceValue);
		update(intervalTrace);
	}
	
	/**
	 * Updates the values in this OpenMetricType with the values from the passed interval trace
	 * @param intervalTrace the interval trace to update this OpenMetricType with
	 */
	public void update(IntervalTrace intervalTrace) {
		if(intervalTrace==null) throw new IllegalArgumentException("The passed interval trace was null", new Throwable());
		set("StartTime", intervalTrace.getTimeStamp());
		set("EndTime", intervalTrace.getEndTimeStamp());
		set("Value", intervalTrace.getValue().toString());
		set("Count", intervalTrace.getIntervalTraceValue().getCount());
		if(minMaxAvg) {
			IMinMaxAvgIntervalTraceValue mma = (IMinMaxAvgIntervalTraceValue)intervalTrace.getIntervalTraceValue();
			set("Average", mma.getAverage().longValue());
			set("Maximum", mma.getMaximum().longValue());
			set("Minumum", mma.getMinimum().longValue());
		} else {
			setNull("Average");
			setNull("Maximum");
			setNull("Minumum");						
//			set("Average", -1L);
//			set("Maximum", -1L);
//			set("Minumum", -1L);			
		}
	}
	
	
	
	/**
	 * Creates a new OpenMetricType 
	 * @param metricName The full metric name
	 * @param metricType The name of the metric type
	 */
	protected OpenMetricType(String metricName, String metricType) {
		set("MetricName", metricName, metricName);
		set("MetricType", metricType, metricType);		
		String[] frags = metricName.split("/");
		point = frags[frags.length-1];
	}

	/**
	 * Sets the value of the named field
	 * @param key The field key
	 * @param value The value to set
	 * @param defaultValue A default value to set if the value is null
	 */
	protected void set(String key, Object value, Object defaultValue) {
		if(key==null) throw new IllegalArgumentException("The passed key was null", new Throwable());
		if(value==null && defaultValue==null) throw new IllegalArgumentException("The passed value and defaultValue were null", new Throwable());
		int offset = INDEX.get(key);
		if(TYPES.get(offset).equals(SimpleType.STRING)) {
			value = value==null ? "" + defaultValue : "" + value;
		}
		values[offset] = value==null ? defaultValue : (value instanceof Number) ? ((Number)value).longValue(): value;
	}
	
	/**
	 * Sets the named value to null.
	 * Only used when a specific field type is not applicable for the metric type
	 * @param key The field key
	 */
	protected void setNull(String key) {
		if(key==null) throw new IllegalArgumentException("The passed key was null", new Throwable());
		int offset = INDEX.get(key);
		values[offset] = null;
	}
	
	/**
	 * Sets the value of the named field
	 * @param key The field key
	 * @param value The value to set
	 */
	protected void set(String key, Object value) {
		set(key, value, null);
	}


	/**
	 * A metric instance for this category mbean
	 * @return An OpenMetricType
	 */
	@JMXAttribute(name="{f:point}", description="An OpenMetricType", mutability=AttributeMutabilityOption.READ_ONLY)
	public OpenMetricType getThis() {
		return this;
	}

	
	/**
	 * Returns the full metric name
	 * @return the metricName
	 */
	@JMXAttribute(expose=false, name="MetricName", description="The fully qualified metric name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getMetricName() {
		return (String)get("MetricName");
	}

	/**
	 * Returns the name of the Metric type
	 * @return the metricType
	 */
	@JMXAttribute(expose=false, name="MetricType", description="The name of the metric type", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getMetricType() {
		return (String)get("MetricType");
	}

	/**
	 * The start time of the interval
	 * @return the startTime
	 */
	@JMXAttribute(expose=false, name="StartTime", description="The start time of the interval", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getStartTime() {
		return (Long)get("StartTime");
	}

	/**
	 * The end time of the interval
	 * @return the endTime
	 */
	@JMXAttribute(expose=false, name="EndTime", description="The end time of the interval", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getEndTime() {
		return (Long)get("EndTime");
	}

	/**
	 * The primary value of the metric as a string
	 * @return the value
	 */
	@JMXAttribute(expose=false, name="Value", description="The primary value of the metric as a string", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getValue() {
		return (String)get("Value");
	}

	/**
	 * The number of traces for this metric name aggregated into this interval
	 * @return the metric count
	 */
	@JMXAttribute(expose=false, name="Count", description="The number of traces for this metric name aggregated into this interval", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCount() {
		return ((Number)get("Count")).longValue();
	}

	/**
	 * The average metric value for the interval. Null if not a MinMaxAvg metric type.
	 * @return the average metric value 
	 */
	@JMXAttribute(expose=false, name="Average", description="The average metric value for the interval. Null if not a MinMaxAvg metric type", mutability=AttributeMutabilityOption.READ_ONLY)
	public Long getAverage() {
		return (Long)get("Average");
	}

	/**
	 * The maximum metric value for the interval. Null if not a MinMaxAvg metric type.
	 * @return the maximum metric value 
	 */
	@JMXAttribute(expose=false, name="Maximum", description="The maximum metric value for the interval. Null if not a MinMaxAvg metric type", mutability=AttributeMutabilityOption.READ_ONLY)
	public Long getMaximum() {
		return (Long)get("Maximum");
	}

	/**
	 * The minimum metric value for the interval. Null if not a MinMaxAvg metric type.
	 * @return the minimum metric value 
	 */
	@JMXAttribute(expose=false, name="Minimum", description="The minimum metric value for the interval. Null if not a MinMaxAvg metric type", mutability=AttributeMutabilityOption.READ_ONLY)
	public Long getMinumum() {
		return (Long)get("Minimum");
	}
	
	// =====================================================================================
	//  CompositeData Operations
	// =====================================================================================
	
    /**
     * Returns the <i>composite type </i> of this <i>composite data</i> instance.
     *
     * @return the type of this CompositeData.
     */
    public CompositeType getCompositeType() {
    	return compositeType;
    }

    /**
     * Returns the value of the item whose name is <tt>key</tt>.
     *
     * @param key the name of the item.
     *
     * @return the value associated with this key.
     *
     * @throws IllegalArgumentException  if <tt>key</tt> is a null or empty String.
     *
     * @throws InvalidKeyException  if <tt>key</tt> is not an existing item name for this <tt>CompositeData</tt> instance.
     */
    public Object get(String key) {
    	if(key==null) throw new IllegalArgumentException("Passed key was null", new Throwable());
    	try {
    		int index = INDEX.get(key);
    		return values[index];
    	} catch (Exception e) {
    		throw new InvalidKeyException("Passed key [" + key + "] was invalid:" + e);
    	}
    }

    /**
     * Returns an array of the values of the items whose names are specified by <tt>keys</tt>, in the same order as <tt>keys</tt>.
     *
     * @param keys the names of the items.
     *
     * @return the values corresponding to the keys.
     *
     * @throws IllegalArgumentException  if an element in <tt>keys</tt> is a null or empty String.
     *
     * @throws InvalidKeyException  if an element in <tt>keys</tt> is not an existing item name for this <tt>CompositeData</tt> instance.
     */
    public Object[] getAll(String[] keys) {
    	if(keys==null) throw new IllegalArgumentException("Passed keys were null", new Throwable());
    	Object[] vals = new Object[keys.length];
    	for(int i = 0; i < keys.length; i++) {
    		vals[i] = get(keys[i]);
    	}
    	return vals;
    }

    /**
     * Returns <tt>true</tt> if and only if this <tt>CompositeData</tt> instance contains 
     * an item whose name is <tt>key</tt>. 
     * If <tt>key</tt> is a null or empty String, this method simply returns false.
     *
     * @param key the key to be tested.
     *
     * @return true if this <tt>CompositeData</tt> contains the key.
     */
    public boolean containsKey(String key)  {
    	if(key==null) throw new IllegalArgumentException("Passed key was null", new Throwable());
    	return INDEX.containsKey(key);
    }

    /**
     * Returns <tt>true</tt> if and only if this <tt>CompositeData</tt> instance contains an item 
     * whose value is <tt>value</tt>.
     *
     * @param value the value to be tested.
     *
     * @return true if this <tt>CompositeData</tt> contains the value.
     */
    public boolean containsValue(Object value) {
    	if(value==null) return false;
    	for(Object val: values) {
    		if(value.equals(val)) return true;
    	}
    	return false;
    }

    /**
     * Returns an unmodifiable Collection view of the item values contained in this <tt>CompositeData</tt> instance.
     * The returned collection's iterator will return the values in the ascending lexicographic order of the corresponding 
     * item names. 
     *
     * @return the values.
     */
    public Collection<?> values()  {
    	List<Object> collection = new ArrayList<Object>(values.length);
    	Collections.addAll(collection, values);
    	return collection;
    }

	
    protected Object writeReplace() throws ObjectStreamException {    	
    	try {
			return new CompositeDataSupport(getCompositeType(), keys, values);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			if(e instanceof ObjectStreamException) {
				throw (ObjectStreamException)e;
			}
			throw new RuntimeException("Failed to write replace OpenMetricType", e);
		}
    }


}
