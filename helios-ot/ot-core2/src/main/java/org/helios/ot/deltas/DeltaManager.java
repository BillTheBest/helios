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
package org.helios.ot.deltas;

import gnu.trove.map.hash.TObjectLongHashMap;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.SystemEnvironmentHelper;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: DeltaManager</p>
 * <p>Description: A utility class for managing deltas.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class DeltaManager {
	/** The delta manager singleton */
	protected volatile static DeltaManager deltaManager = null;
	/** The container for tracking deltas */
	protected TObjectLongHashMap<java.lang.String> longDeltas = null;
	/** The configured default delta type for this JVM instance */
	static DeltaType deltaType = null;
	/** The class logger */
	protected static Logger LOG = Logger.getLogger(DeltaManager.class);
	/** The DeltaManager JMX ObjectName */
	public static final ObjectName objectName = JMXHelper.objectName("org.helios.tracing:service=DeltaManager");
	
	
	/** The name of the system property to override the configured initial capacity of the delta container */
	protected static final String DELTA_CAPACITY = "helios.opentrace.deltas.initialcapacity";
	/** The name of the system property to override the configured load capacity of the delta container */
	protected static final String DELTA_LOAD_FACTOR = "helios.opentrace.deltas.loadfactor";
	/** The name of the system property to override the configured default delta type of the delta container */
	protected static final String DELTA_TYPE = "helios.opentrace.deltas.type";
	/** The default initial capacity of the delta container */
	protected static final int DELTA_CAPACITY_DEFAULT = 100;	
	/** The default load factor of the delta container */
	protected static final float DELTA_LOAD_FACTOR_DEFAULT = 0.5F;
	/** The default delta type of the delta container */
	public static final DeltaType DEFAULT_DELTA_TYPE = DeltaType.REBASE;

	
	/**
	 * Creates a new singleton instance of the DeltaManager.
	 */
	private DeltaManager() {
		int initialDeltaCapacity = DELTA_CAPACITY_DEFAULT;
		float initialDeltaLoadFactor = DELTA_LOAD_FACTOR_DEFAULT;
		try { initialDeltaCapacity = Integer.parseInt(SystemEnvironmentHelper.getEnvThenSystemProperty(DELTA_CAPACITY)); } catch (Exception e) {}
		try { initialDeltaLoadFactor = Float.parseFloat(SystemEnvironmentHelper.getEnvThenSystemProperty(DELTA_LOAD_FACTOR)); } catch (Exception e) {}
		try { deltaType = DeltaType.valueOf(SystemEnvironmentHelper.getEnvThenSystemProperty(DELTA_TYPE, DEFAULT_DELTA_TYPE.name())); } catch (Exception e) {}
		longDeltas = new TObjectLongHashMap<java.lang.String>(initialDeltaCapacity, initialDeltaLoadFactor);		
	}
	
	/**
	 * Returns the singleton instance of the DeltaManager.
	 * @return the singleton instance of the DeltaManager.
	 */
	public static DeltaManager getInstance() {
		if(deltaManager!=null) return deltaManager;
		synchronized(DeltaManager.class) {
			if(deltaManager!=null) return deltaManager;
			deltaManager = new DeltaManager();
			return deltaManager;
		}
	}
	
	/**
	 * Processes a delta request for the passed name, value, metric type for the default delta type.
	 * @param name
	 * @param value
	 * @param type
	 * @return
	 */
	public synchronized Number delta(String name, Number value, MetricType type) {
		return delta(name, value, type, deltaType);
	}
	
	/**
	 * Processes a delta request for the passed name, value, metric type and delta type.
	 * @param name
	 * @param value
	 * @param type
	 * @param dt
	 * @return
	 */
	public synchronized Number delta(String name, Number value, MetricType type, DeltaType dt) {
		Number number = null;
		if(name==null || value==null || type==null || dt==null) throw new RuntimeException("Null parameter passed");
		if(!type.isDelta()) throw new RuntimeException("Metric Type [" + type.getName() + "] is not delta");
		long inValue = value.longValue();
		if(longDeltas.containsKey(name)) {
			long state = longDeltas.get(name);
			longDeltas.put(name, inValue);
			if(inValue < state) {
				number = processBrokenMonotonic(name, inValue, state, dt);
			} else {
				number = inValue - state;				
			}
		} else {
			longDeltas.put(name, inValue);
		}
		if(number==null) return null;
		if(type.isInt()) {
			return number.intValue();
		} else {
			return number.longValue();
		}
	}
	
	/**
	 * Retrieves the value in state for the passed name.
	 * @param name The name to retrieve state for.
	 * @return The number in state or null.
	 */
	public synchronized Long getState(String name) {
		if(longDeltas.containsKey(name)) {
			return longDeltas.get(name);
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the number of entries in state.
	 * @return the number of entries in state.
	 */
	public synchronized int getStateSize() {		
		return longDeltas.size();
	}
	
	/**
	 * Returns a formatted string displaying the names and values in state.
	 * @return a formatted string displaying the names and values in state.
	 */
	public synchronized String dumpState() {
		StringBuilder b = new StringBuilder("DeltaManager States:");
		for(Object key: longDeltas.keys()) {
			b.append("\n\t").append(key).append(":").append(longDeltas.get(key.toString()));
		}
		return b.toString();
	}
	
	/**
	 * Calculates the return value for the delta request in accordance with the deltaType.
	 * @param name
	 * @param newValue
	 * @param state
	 * @return
	 */
	protected Long processBrokenMonotonic(String name, long newValue, long state, DeltaType dt) {
		longDeltas.put(name, newValue);
		if(dt.equals(DeltaType.REBASE)) {			
			return null;
		} 
		long d = newValue - state;
		if(dt.equals(DeltaType.RELATIVE)) {
			return d;
		} else {
			return Math.abs(d);
		}
	}
	
	/**
	 * Clears all deltas in state.
	 */
	public synchronized void reset() {
		longDeltas.clear();
	}
}
