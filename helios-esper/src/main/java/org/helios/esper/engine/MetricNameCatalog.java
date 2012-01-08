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
package org.helios.esper.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.openmbean.TabularDataSupport;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.BeanNameAware;

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.sdicons.json.mapper.JSONMapper;





/**
 * <p>Title: MetricNameCatalog</p>
 * <p>Description: An esper statement listener that maintains a unique list of metric names</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.MetricNameCatalog</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating that new metric names have been added to the cache", types={
                @JMXNotificationType(type=MetricNameCatalog.NEW_METRIC_NOTIFICATION_TYPE)
        })       
})
public class MetricNameCatalog extends ManagedObjectDynamicMBean implements ListenerRegistration, BeanNameAware {
	protected Map<String, MetricName> metricNames = new ConcurrentHashMap<String, MetricName>();
	protected Map<String, Map<String, String>> hierarchy = new ConcurrentHashMap<String, Map<String, String>>(1000);
	protected Logger log = Logger.getLogger(getClass());
	protected AtomicLong serial = new AtomicLong(0);
	protected String[] targetStatements = new String[0];
	protected String beanName = null;
	protected EPStatement statement = null;
	protected EPServiceProvider serviceProvider = null;

	private static final long serialVersionUID = -1572906982342678315L;
	
	public static final String NEW_METRIC_NOTIFICATION_TYPE = "org.helios.server.engine.newmetrics";
	
	public static final String ROOT = "HELIOS_METRIC_ROOT";

	/**
	 * @param description
	 */
	public MetricNameCatalog(String description) {
		super(description);
		hierarchy.put(ROOT + "/", new ConcurrentHashMap<String, String>());
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(name="metricCount", description="The number of metric names in cache", mutability=AttributeMutabilityOption.READ_ONLY)
	public int metricCount() {
		return metricNames.size();
	}
	
	/**
	 * @param server
	 */
	public void setMBeanServer(MBeanServer server) {
		this.server = server;
	}
	
	public void start() throws Exception {
		log.info("\n\t==============================================\n\tStarting MetricNameCatalog\n\t==============================================\n");
		this.reflectObject(this);
		server.registerMBean(this, objectName);
		log.info("\n\t==============================================\n\tStarted MetricNameCatalog\n\t==============================================\n");
	}
	
	
	
	
	/**
	 * @return
	 */
	@JMXAttribute(name="Names", description="The metric names in cache", mutability=AttributeMutabilityOption.READ_ONLY)	
	public String[] getNames() {
		return metricNames.keySet().toArray(new String[metricNames.size()]);
	}
	
	/**
	 * @param fullName
	 * @return
	 */
	@JMXOperation(name="getMetricName", description="Retrieves the cached metric name for the passed full name")
	public MetricName getMetricName(@JMXParameter(name="fullName", description="The fullName of the metric to retrieve from cache.")String fullName) {
		return metricNames.get(fullName);
	}
	
	/**
	 * Returns an array of all the unique metrics
	 * @return an array of all the unique metrics
	 */
	@JMXAttribute(name="AllMetrics", description="The metric names in cache", mutability=AttributeMutabilityOption.READ_ONLY)
	public MetricName[] getAllMetrics() {
		return metricNames.values().toArray(new MetricName[metricNames.size()]);
	}
	
	@JMXAttribute(name="MetricTable", description="The metric names in cache as a Tabular Type", mutability=AttributeMutabilityOption.READ_ONLY)
	public TabularDataSupport getMetricTable() {
		TabularDataSupport tds = new TabularDataSupport(MetricName.METRIC_NAME_TABULAR_TYPE, metricNames.size(), 0.75f);
		tds.putAll(metricNames.values().toArray(new MetricName[metricNames.size()]));
		return tds;
	}
	

	
	@JMXOperation(name="getMetricNames", description="Retrieves the cached metric name for the passed full names")
	public MetricName[] getMetricNames(@JMXParameter(name="fullName", description="The fullNames of the metrics to retrieve from cache.")String...fullNames) {
		Set<MetricName> matches = new HashSet<MetricName>();
		if(fullNames!=null && fullNames.length>0) 
		for(String name: fullNames) {
			MetricName mn = metricNames.get(name);
			if(mn!=null) matches.add(mn);
		}		
		return matches.toArray(new MetricName[matches.size()]);
	}
	
	
	/**
	 * @param fullName
	 * @return
	 */
	@JMXOperation(name="getMetricsLike", description="Retrieves the cached metric names that match the passed regular expression")
	public MetricName[] getMetricsLike(@JMXParameter(name="regex", description="The regex of the metric fullName to match against and retrieve from cache.")String regex) {
		Set<MetricName> matches = new HashSet<MetricName>();
		Pattern p = Pattern.compile(regex);
		for(Map.Entry<String, MetricName> entry: metricNames.entrySet()) {
			if(p.matcher(entry.getKey()).matches()) {
				matches.add(entry.getValue());
			}
		}
		return matches.toArray(new MetricName[matches.size()]);
	}
	
	/**
	 * @return
	 */
	@JMXOperation(name="displayHierarchyInJSON", description="Renders the contents of the hierarchy in JSON")
	public String displayHierarchyInJSON() {
		try {
			return JSONMapper.toJSON(hierarchy).render(true);
		} catch (Exception e) {
			log.error("Failed to render hierarchy", e);
			return "Failed to render hierarchy:" + e;
		}
	}
	
	/**
	 * @param name
	 * @return
	 */
	@JMXOperation(name="getJSONDirectChildrenFor", description="Renders the direct children of the passed node in JSON")
	public String getJSONDirectChildrenFor(String name) {
		name = ROOT + "/" + name;
		Map<String, String> m = hierarchy.get(name);
		Map<String, Map> output = new HashMap<String, Map>();
		Map<String, String> segmentMap = new HashMap<String, String>();
		Map<String, MetricName> metricMap = new HashMap<String, MetricName>();
		output.put("segments", segmentMap);
		output.put("metrics", metricMap);
		
		if(m!=null) {
			for(Map.Entry<String, String> entry: m.entrySet()) {
				if(entry.getKey().endsWith("/")) {
					segmentMap.put(entry.getKey(), entry.getValue());
				} else {
					MetricName mn = metricNames.get(entry.getValue());
					if(mn!=null) {
						metricMap.put(entry.getKey(), mn);
					}
				}
			}
		}
		try {
			return JSONMapper.toJSON(output).render(true);
		} catch (Exception e) {
			log.error("Failed to render direct children for [" + name + "]", e);
			return "Failed to render direct children [" + name + "]:" + e;
		}
		
	}
	
	@JMXOperation(name="getXMLDirectChildrenFor", description="Renders the direct children of the passed node in JSON")
	public String getXMLDirectChildrenFor(String name) {
		String json = this.getJSONDirectChildrenFor(name);
		try {
			JSONObject obj = new JSONObject(json);
			return XML.toString(obj, "MetricNameChildren");
		} catch (Exception e) {
			log.error("Failed to render XML", e);
			return "Failed to render direct children [" + name + "]:" + e;
		}
	}
	
	
	/**
	 * @param fullName
	 */
	protected void updateHierarchy(String fullName) {
		if(fullName==null || fullName.length() < 1) return;
		String[] segments = fullName.split("/");
		int segCount = segments.length;
		int mseg = segments.length-1;
		StringBuilder incrName = new StringBuilder(ROOT + "/");
		String key = null;
		Map<String, String> items = null;
		for(int i = 0; i < segCount; i++) {
			key = segments[i];
			if(items==null) {
				items = hierarchy.get(incrName.toString());
			}
			// eg. key = "InVMAgent", incrName = ROOT/HOST/, items=children->ROOT/HOST/
			if(i==mseg) {
				incrName.append(key);
				items.put(key, incrName.toString().replace(ROOT + "/", ""));
				continue;
			} else {
				incrName.append(key).append("/");
				// add (InVMAgent/, HOST/InVMAgent/) to items
				items.put(key + "/", incrName.toString().replace(ROOT + "/", ""));
				// add (ROOT/HOST/InVMAgent/, new Map) to hierarchy
				items = hierarchy.get(incrName.toString());
				if(items==null) {
					items = new ConcurrentHashMap<String, String>();
					hierarchy.put(incrName.toString(), items);
				}					
			}									
		}
	}
	@JMXAttribute(name="Hierarchy", description="The metric names hierarchy map", mutability=AttributeMutabilityOption.READ_ONLY)
	public Map getHierarchy() {
		return Collections.unmodifiableMap(hierarchy);
	}
	
	/**
	 * @throws Exception
	 */
	@JMXOperation(name="reset", description="Resets and repopulates the catalog.")
	public void reset() throws Exception {
		try {
			if(this.serviceProvider != null && this.statement != null) {
				hierarchy.clear();
				metricNames.clear();
				hierarchy.put(ROOT + "/", new ConcurrentHashMap<String, String>());
				EPOnDemandQueryResult result = serviceProvider.getEPRuntime().executeQuery(statement.getText());
				this.update(result.getArray(), null, statement, serviceProvider);

			}
		} catch (Exception e) {
			log.error("Failed to reset metric catalog", e);
			throw e;
		}
	}

	/**
	 * @param newBeans
	 * @param oldBeans
	 * @param st
	 * @param sp
	 * @see com.espertech.esper.client.StatementAwareUpdateListener#update(com.espertech.esper.client.EventBean[], com.espertech.esper.client.EventBean[], com.espertech.esper.client.EPStatement, com.espertech.esper.client.EPServiceProvider)
	 */
	@SuppressWarnings("unchecked")
	public void update(EventBean[] newBeans, EventBean[] oldBeans, EPStatement st, EPServiceProvider sp) {
		int cnt = 0;
		if(st!=null) {
			this.statement = st;
		}
		if(serviceProvider!=null) {
			this.serviceProvider = sp;
		}
		if(oldBeans!=null && oldBeans.length > 0) {
			log.info("Deleted [" + oldBeans.length + "] instances");
		}
		Set<String> newNames = new HashSet<String>();
		if(!"UniqueMetricNameWindowSelect".equals(st.getName())) {
			//log.info("Processing results from [" + st.getName() + "]");
		}
		for(EventBean bean: newBeans) {
			if("InactiveMetrics".equals(st.getName())) {
				//log.info("Expired MetricName:" + bean.toString());
			} else {
				try {
					if(!metricNames.containsKey(((Map)bean.getUnderlying()).get(MetricName.FULL_NAME))) {
						MetricName mn = new MetricName((Map)bean.getUnderlying());
						metricNames.put(mn.getFullName(), mn);
						newNames.add(mn.getFullName());
						updateHierarchy(mn.getFullName());
						cnt++;
					}				
				} catch (Exception e) {
					log.error("Exception Updating Hierarchy", e);
				}
			}
		}
		if(cnt>0) {
			if(log.isDebugEnabled()) log.debug("Added [" + cnt + "] new MetricNames to cache");
			Notification notif = new Notification(NEW_METRIC_NOTIFICATION_TYPE, this.objectName, serial.incrementAndGet(), System.currentTimeMillis());
			notif.setUserData(newNames);
			sendNotification( notif );
		}
	}

	/**
	 * @return
	 * @see org.helios.server.core.esper.ListenerRegistration#getName()
	 */
	public String getName() {
		return beanName;
	}

	/**
	 * @return
	 * @see org.helios.server.core.esper.ListenerRegistration#getTargetStatements()
	 */
	public String[] getTargetStatements() {
		return targetStatements;
	}

	/**
	 * @return the beanName
	 */
	public String getBeanName() {
		return beanName;
	}

	/**
	 * @param beanName the beanName to set
	 */
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * @param targetStatements the targetStatements to set
	 */
	public void setTargetStatements(String[] targetStatements) {
		this.targetStatements = targetStatements;
	}

	/**
	 * @return the Metric Tree Root Name
	 */
	@JMXAttribute(name="Root", description="The Metric Tree Root Name", mutability=AttributeMutabilityOption.READ_ONLY)
	public static String getRoot() {
		return ROOT;
	}

//	@Override
//	public void update(EventBean[] inEvents, EventBean[] outEvents) {
//		update(inEvents, outEvents, null, null);
//		
//	}



}
