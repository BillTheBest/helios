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

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.ot.trace.IntervalTrace;
import org.helios.ot.trace.Trace;

/**
 * <p>Title: JMXMetric</p>
 * <p>Description: An MBean container to manage an individual interval metric published from the interval accummulator.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.jmx.JMXMetric</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class JMXMetric extends ManagedObjectDynamicMBean {
	
	
	/** A cache of OpenMetricTypes keyed by the metric point */
	protected final Map<String, OpenMetricType> openMetricTypes = new ConcurrentHashMap<String, OpenMetricType>(128);
	/** Instance Logger */
	protected final Logger log = Logger.getLogger(getClass());

	
	/** A map of Metric MBean ObjectNames keyed by full metric name */
	protected static final Map<String, ObjectName> nameToMBean = new ConcurrentHashMap<String, ObjectName>(1024);
	/** A map of JMXMetrics keyed by ObjectName */
	protected static final Map<ObjectName, JMXMetric> mbeanToMetric = new ConcurrentHashMap<ObjectName, JMXMetric>(1024);
	/** The JMX domain where the OpenMetricTypes are registered */
	public static final String LOCAL_METRIC_DOMAIN = "org.helios.ot.metrics";
	/** The ObjectName property key prefix */
	public static final String LOCAL_METRIC_KEY_PREFIX = "v";
	
	
	
	
	
	
	/**
	 * Publishes an Interal Trace to an OpenMetricType
	 * @param intervalTrace
	 */
	public static void publish(IntervalTrace intervalTrace) {
		if(intervalTrace==null) throw new IllegalArgumentException("Passed interval trace was null", new Throwable());
		
		ObjectName on =  nameToMBean.get(intervalTrace.getFQN());
		if(on==null) {
			synchronized (nameToMBean) {
				on =  nameToMBean.get(intervalTrace.getFQN());
				if(on==null) {
					on = getCategoryObjectNameForMetric(intervalTrace);
					nameToMBean.put(intervalTrace.getFQN(), on);
				}
			}
		}
		JMXMetric jMetric = mbeanToMetric.get(on);
		if(jMetric==null) {
			synchronized(mbeanToMetric) {
				jMetric = mbeanToMetric.get(on);
				if(jMetric==null) {
					jMetric = new JMXMetric();
					mbeanToMetric.put(on, jMetric);
					try {
						if(JMXHelper.getHeliosMBeanServer().isRegistered(on)) {
							JMXHelper.getHeliosMBeanServer().unregisterMBean(on);
						}
						JMXHelper.getHeliosMBeanServer().registerMBean(jMetric, on);
					} catch (Exception e) {
						throw new RuntimeException("Failed to publish OpenMetricType Container [" + on + "]", e);
					}					
				}
			}
		}
		if(!jMetric.openMetricTypes.containsKey(intervalTrace.getMetricName())) {
			jMetric.create(intervalTrace);
		} else {
			jMetric.update(intervalTrace);
		}
	}
	
	
	
	
	
	protected void create(IntervalTrace intervalTrace) {
		try {
			OpenMetricType omt = new OpenMetricType(intervalTrace);
			openMetricTypes.put(intervalTrace.getMetricName(), omt);
			this.reflectObject(omt);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		
	}
	
	protected void update(IntervalTrace intervalTrace) {
		try {
			openMetricTypes.get(intervalTrace.getMetricName()).update(intervalTrace);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		
	}
	/**
	 * Creates an OpenMetricType ObjectName for the passed interval metric name
	 * @param intervalTrace The interval trace to get the ObjectName for
	 * @return An ObjectName
	 */
	public static ObjectName getObjectNameForMetric(IntervalTrace intervalTrace) {
		if(intervalTrace==null) throw new IllegalArgumentException("The passed trace was null", new Throwable());
		int keyIndex = 0;
		Hashtable<String, String> keyProps = new Hashtable<String, String>();
		for(String val: intervalTrace.getFQN().split(Trace.DELIM)) {
			String key = LOCAL_METRIC_KEY_PREFIX + keyIndex;
			keyProps.put(key, val);
		}
		return JMXHelper.objectName(LOCAL_METRIC_DOMAIN, keyProps);
	}
	
	/**
	 * Creates an OpenMetricType category ObjectName for the passed interval metric name.
	 * The category aggregates into a namespace one above the point so all points in the same namespace
	 * above the point will be represented in the same cointainer
	 * @param intervalTrace The interval trace to get the ObjectName for
	 * @return An ObjectName
	 */
	public static ObjectName getCategoryObjectNameForMetric(IntervalTrace intervalTrace) {
		if(intervalTrace==null) throw new IllegalArgumentException("The passed trace was null", new Throwable());
		int keyIndex = 0;
		Hashtable<String, String> keyProps = new Hashtable<String, String>();
		String[] frags = intervalTrace.getFQN().split(Trace.DELIM);
		for(int i = 0; i < frags.length-1; i++) {
			String key = LOCAL_METRIC_KEY_PREFIX + keyIndex;
			keyProps.put(key, frags[i]);			
		}
		return JMXHelper.objectName(LOCAL_METRIC_DOMAIN, keyProps);
	}
	
	
}
