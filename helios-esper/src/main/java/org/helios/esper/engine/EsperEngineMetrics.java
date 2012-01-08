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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.metric.EngineMetric;

/**
 * <p>Title: EsperEngineMetrics</p>
 * <p>Description: JMX MBean to expose an Esper engine's metrics.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 */
@JMXManagedObject(annotated=true, declared=true)
public class EsperEngineMetrics extends ManagedObjectDynamicMBean implements UpdateListener {
	/**  */
	private static final long serialVersionUID = 6613555454843291204L;
	protected AtomicLong timestamp = new AtomicLong(0);
	protected AtomicLong inputCount = new AtomicLong(0);
	protected AtomicLong scheduleDepth = new AtomicLong(0);
	protected AtomicLong deepSizeElapsedTime = new AtomicLong(0);
	protected String engineURI = null;
	protected Logger log = Logger.getLogger(getClass());
	protected EPServiceProvider provider;
	protected EPStatement statement = null; 

	/**
	 * @param newBeans
	 * @param oldBeans
	 * @see com.espertech.esper.client.UpdateListener#update(com.espertech.esper.client.EventBean[], com.espertech.esper.client.EventBean[])
	 */
	public void update(EventBean[] newBeans, EventBean[] oldBeans) {
		for(EventBean metric: newBeans) {
			Object m = metric.getUnderlying();
			if(m instanceof EngineMetric) {
				EngineMetric em = (EngineMetric)m;
				this.timestamp.set(em.getTimestamp());
				this.inputCount.set(em.getInputCount());
				this.scheduleDepth.set(em.getScheduleDepth());
				if(log.isDebugEnabled())log.debug(toString(em));
			}
		}
	}
	
	public String toString(EngineMetric metric) {
	    final String DELIM = "\n\t";
	    final String CR = "\n";
	    final String VALUE_OPEN = "[";
	    final String VALUE_CLOSE = "]";
	    StringBuilder retValue = new StringBuilder();    
	    retValue.append(CR).append("EngineMetric Name:[").append(metric.getEngineURI()).append("]  (")
	        .append(DELIM).append("timestamp:").append(VALUE_OPEN).append(metric.getTimestamp()).append(VALUE_CLOSE)
	        .append(DELIM).append("inputCount:").append(VALUE_OPEN).append(metric.getInputCount()).append(VALUE_CLOSE)
	        .append(DELIM).append("scheduleDepth:").append(VALUE_OPEN).append(metric.getScheduleDepth()).append(VALUE_CLOSE)
	        .append(CR).append(")");    
	    return retValue.toString();
	}	
	
	public void start() {
		statement = provider.getEPAdministrator().createEPL("select * from com.espertech.esper.client.metric.EngineMetric.win:time(5 sec) where engineURI = '" + engineURI + "'", "Monitor-" + engineURI);
		statement.addListener(this);
	}
	
	
	/**
	 * @param description
	 */
	public EsperEngineMetrics(EPServiceProvider provider) {		
		super("Esper Engine [" + provider.getURI() + "] Metrics");
		this.provider = provider;
		this.engineURI = provider.getURI();
		this.reflectObject(this);
	}

	/**
	 * @return the timestamp
	 */
	@JMXAttribute(description="The current engine time", name="Timestamp", mutability=AttributeMutabilityOption.READ_ONLY)
	public AtomicLong getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the inputCount
	 */
	@JMXAttribute(description="Cumulative number of input events since engine initialization time.", name="InputCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getInputCount() {
		return inputCount.get();
	}
	
	@JMXAttribute(description="Input event rate.", name="InputRate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getInputRate() {
		return inputCount.get();
	}
	

	/**
	 * @return the scheduleDepth
	 */
	@JMXAttribute(description="Number of outstanding schedules.", name="ScheduleDepth", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getScheduleDepth() {
		return scheduleDepth.get();
	}

	/**
	 * @return the engineURI
	 */
	@JMXAttribute(description="The URI of the engine instance.", name="EngineURI", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getEngineURI() {
		return engineURI;
	}



	@JMXAttribute(name="DeepSizeElapsedTime", description="The elapsed time to calc the memory size of the engine runtime.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getDeepSizeElapsedTime() {
		return deepSizeElapsedTime.get();
	}



}
