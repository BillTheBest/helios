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
package org.helios.ot.generic.translator;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.ot.generic.GenericMetric;
import org.helios.ot.generic.GenericMetricDef;
import org.helios.ot.generic.GenericMetricFactory;
import org.helios.ot.generic.IGenericMetric;
import org.helios.ot.generic.IGenericMetricTranslator;

/**
 * <p>Title: WilyGenericMetricTranslator</p>
 * <p>Description: Translator for Wily Introscope metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.generic.translator.WilyGenericMetricTranslator</code></p>
 */

public class WilyGenericMetricTranslator implements IGenericMetricTranslator {
	/** Indicates if the translator is valid */
	private boolean valid;
	/** The wily metric metric class */
	private static Class<?> ANumericalTimeslicedValue = null;
	/** The wily metric metric class loader */
	private static ClassLoader wilyClassLoader = null;
	/** The reflection method cache */
	private static final Map<Integer, Method> METHOD_CACHE = new ConcurrentHashMap<Integer, Method>();
	
	
	/** The wily metric metric class name */
	public static final String WILY_METRIC_CLASSNAME = "com.wily.introscope.stat.timeslice.ANumericalTimeslicedValue"; 
	
	/**
	 * A hint to the service loader as to where the wily metric class might be found
	 * @param cl the wily metric class classloader
	 */
	public static void setClassLoader(ClassLoader cl) {
		wilyClassLoader = cl;
		try {
			ANumericalTimeslicedValue = Class.forName("com.wily.introscope.stat.timeslice.ANumericalTimeslicedValue", true, wilyClassLoader);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to load class ANumericalTimeslicedValue", e);
		}
	}

	
	/**
	 * Creates a new WilyGenericMetricTranslator
	 */
	public WilyGenericMetricTranslator() {
		try {
			testWilyClass();
			valid = true;
		} catch (Exception e) {			
			valid = false;
		}
	}
	
	/**
	 * Loads the wily introscope metric class if it has not been loaded already
	 */
	protected void testWilyClass() {
		if(ANumericalTimeslicedValue==null) {
			synchronized(this) {
				if(ANumericalTimeslicedValue==null) {
					try {
						if(wilyClassLoader!=null) {
							ANumericalTimeslicedValue = Class.forName("com.wily.introscope.stat.timeslice.ANumericalTimeslicedValue", true, wilyClassLoader);
						} else {
							ANumericalTimeslicedValue = Class.forName("com.wily.introscope.stat.timeslice.ANumericalTimeslicedValue");
						}
					} catch (Exception e) {
						e.printStackTrace(System.err);
						throw new RuntimeException("Failed to load Wily Introscope Metric Class [" + WILY_METRIC_CLASSNAME + "]", e);
					}
				}
			}
		}

	}
	
	/**
	 * Creates a new WilyGenericMetricImpl array from the passed array of Introscope supplied MetricData instances
	 * @param metricDatas the Introscope supplied MetricData instances
	 * @return An array of WilyGenericMetricImpls (numerics only)
	 */
	public static IGenericMetric[] convert(Object metricDatas) {
		int arrayLength = metricDatas==null ? 0 : Array.getLength(metricDatas);
		if(arrayLength<1) return GenericMetricFactory.EMPTY_ARR;
		Set<IGenericMetric> metrics = new HashSet<IGenericMetric>(arrayLength);
		for(int i = 0; i < arrayLength; i++) {
			Object md = Array.get(metricDatas, i);
			Object dataValue = invoke(md, "getDataValue");
			boolean hasDataPoints = (Boolean)invoke(dataValue, "hasDataPoints");
			boolean numeric = ANumericalTimeslicedValue.isAssignableFrom(dataValue.getClass());
			if(hasDataPoints && numeric) {
				metrics.add(newGenericMetric(md, dataValue, invoke(md, "getAgentMetric"), invoke(md, "getAgentName")));
			}
		}
		return metrics.toArray(new IGenericMetric[metrics.size()]);
	}	
	
	/**
	 * Creates a new WilyGenericMetricImpl
	 * @param metricData The Introscope supplied MetricData instance
	 */
	private static GenericMetric newGenericMetric(Object metricData, Object dataValue, Object agentMetric, Object agentName) {
		int typeCode = (Integer)invoke(agentMetric, "getAttributeType");  //metricData.getAgentMetric().getAttributeType();
		String fullName = invoke(metricData, "getFullBindingURL");
		GenericMetricDef metricDef = GenericMetricDef.getInstance(fullName, typeCode);
		long intervalStart = (Long)invoke(dataValue, "getStartTimestampInMillis"); //metricData.getDataValue().getStartTimestampInMillis();
		long intervalEnd = (Long)invoke(dataValue, "getStopTimestampInMillis");  //metricData.getDataValue().getStopTimestampInMillis();
		long count = (Long)invoke(dataValue, "getDataPointCount");  //metricData.getDataValue().getDataPointCount();
		long avg = (Long)invoke(dataValue, "getValueAsLong");  //numericMetric.getValueAsLong();
		long min = (Long)invoke(dataValue, "getMinimumAsLong"); //numericMetric.getMinimumAsLong();
		long max = (Long)invoke(dataValue, "getMaximumAsLong"); //numericMetric.getMaximumAsLong();
		return new GenericMetric(intervalStart, intervalEnd, avg, max, min, count, metricDef);
		
	}
	

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetricTranslator#translate(java.lang.Object[])
	 */
	@Override
	public IGenericMetric[] translate(Object... metrics) {
		if(!valid) throw new RuntimeException("The translator is invalid because the Wily Introscope Metric class is not loaded. Call setClassLoader with the class loader for [" + WILY_METRIC_CLASSNAME + "]", new Throwable());		
		return convert(metrics);
	}
	
	/**
	 * Determines if this instance of the translator is valid for the current environment.
	 * Typically called after the service loader instantiates the class.
	 * @return true if the translator is in a valid state, false otherwise.
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Reflective invoker with method caching.
	 * @param target The target object to invoke on
	 * @param methodName The method name to invoke
	 * @return the return value of the invocation
	 */
	private static <T> T invoke(Object target, String methodName) {
		try {
			int key = (target.getClass().getName() + methodName).hashCode();
			Method m = METHOD_CACHE.get(key);
			if(m==null) {
				try {
					m = target.getClass().getDeclaredMethod(methodName);
				} catch (Exception e) {
					m = target.getClass().getMethod(methodName);
				}
				METHOD_CACHE.put(key, m);
			}
			return (T)m.invoke(target);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke [" + methodName + "] on instance of [" + target.getClass().getName() + "]", e);
		}
	}
	
	
}
