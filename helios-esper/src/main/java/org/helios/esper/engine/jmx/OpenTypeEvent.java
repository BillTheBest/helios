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
package org.helios.esper.engine.jmx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.helios.jmx.opentypes.OpenTypeManager;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
/**
 * <p>Title: OpenTypeEvent</p>
 * <p>Description: Static utilities to handle event beans as open types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.esper.engine.jmx.OpenTypeEvent</code></p>
 */

public class OpenTypeEvent {
	
	/** A cache of esper event types translated to composite types keyed by the event type ID */
	protected static final Map<Integer, CompositeType> ctypes = new ConcurrentHashMap<Integer, CompositeType>();
	/** A cache of esper event array types translated to tabular types keyed by the event type ID */
	protected static final Map<Integer, TabularType> ttypes = new ConcurrentHashMap<Integer, TabularType>();
	
	/**
	 * Builds and returns a {@link CompositeType} for the passed {@link EventType}.
	 * @param eventType The Esper event bean event type
	 * @return a composite type
	 */
	public static CompositeType getType(EventType eventType) {
		if(eventType==null) throw new IllegalArgumentException("The passed eventType was null", new Throwable());
		CompositeType ctype = ctypes.get(eventType.getEventTypeId());
		if(ctype==null) {
			synchronized(ctypes) {
				ctype = ctypes.get(eventType.getEventTypeId());
				if(ctype==null) {
					EventPropertyDescriptor[] descriptors = eventType.getPropertyDescriptors();
					List<String> propNames = new ArrayList<String>(descriptors.length);
					List<String> propDescs = new ArrayList<String>(descriptors.length);
					List<OpenType<?>> propOpenTypes = new ArrayList<OpenType<?>>(descriptors.length);
					for(EventPropertyDescriptor descriptor: descriptors) {
						propNames.add(descriptor.getPropertyName());
						propDescs.add(descriptor.getPropertyName());
						OpenType<?> otype = OpenTypeManager.getInstance().getOpenTypeOrNull(descriptor.getPropertyType().getName());
						if(otype!=null) {
							propOpenTypes.add(otype);
						} else {
							propOpenTypes.add(SimpleType.STRING);
						}
					}
					try {
						ctype = new CompositeType(
								eventType.getName(), 
								"Composite Type for " + eventType.getName(), 
								propNames.toArray(new String[propNames.size()]),
								propDescs.toArray(new String[propDescs.size()]),
								propOpenTypes.toArray(new OpenType[propOpenTypes.size()]));
						TabularType ttype = ttypes.get(eventType.getEventTypeId());
						if(ttype==null) {
							synchronized(ttypes) {
								ttype = ttypes.get(eventType.getEventTypeId());
								if(ttype==null) {
									ttype = new TabularType(eventType.getName(), eventType.getName() + " Tabular Type", ctype, propNames.toArray(new String[propNames.size()]));
									ttypes.put(eventType.getEventTypeId(), ttype);
								}
							}
						}
					} catch (OpenDataException e) {
						throw new RuntimeException("Failed to build CompositeType for event type [" + eventType.getName() + "]", e);
					}					
				}
			}
		}
		return ctype;
	}
	
	
	
	/**
	 * Returns an array of CompositeData instances translated from the passed esper event beans
	 * @param beans An array of esper event beans
	 * @return an array of CompositeData instances 
	 */
	public static CompositeData[] getCompositeDatas(EventBean[] beans) {
		if(beans==null || beans.length<1) return new CompositeData[0];
		Set<CompositeData> cdatas = new HashSet<CompositeData>(beans.length);
		EventType et = beans[0].getEventType();
		int propCount = et.getPropertyDescriptors().length;
		CompositeType ctype = getType(et);
		try {
		for(EventBean bean: beans) {
			Map<String, Object> data = new HashMap<String, Object>(propCount);
			for(String itemName: ctype.keySet()) {
				if(ctype.getType(itemName).equals(SimpleType.STRING)) {
					data.put(itemName, bean.get(itemName).toString());
				} else {
					data.put(itemName, bean.get(itemName));
				}
			}
			cdatas.add(new CompositeDataSupport(ctype, data)); 
		}
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert an EventBean to a CompositeData instance", e);
		}		
		return cdatas.toArray(new CompositeData[cdatas.size()]);
	}
	
	/**
	 * Returns a tabular data translation of the passed esper event bean array
	 * @param beans The esper event bean array
	 * @return a tabular data instance
	 */
	public static TabularData getTabularData(EventBean[] beans) {
		if(beans==null || beans.length<1) throw new IllegalArgumentException("Passed event bean array must not be null and must have at least 1 bean", new Throwable());
		CompositeData[] cds = getCompositeDatas(beans);
		TabularDataSupport tds = new TabularDataSupport(ttypes.get(beans[0].getEventType().getEventTypeId()), beans.length, 0.75f);
		tds.putAll(cds);
		return tds;
		
	}
}
