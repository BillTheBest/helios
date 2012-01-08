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
package org.helios.collectors.synthetic;

import java.util.ArrayList;
import java.util.List;

import org.helios.collectors.AbstractCollector;
import org.helios.collectors.CollectionResult;
import org.helios.collectors.exceptions.CollectorStartException;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;

/**
 * <p>Title: FakeCollector</p>
 * <p>Description: Fake Collector that mimics an Introscope Agent to pump in hard coded metrics provided 
 * 					to it on regular intervals</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
@JMXManagedObject (declared=false, annotated=true)
public class FakeCollector extends AbstractCollector {

	/** FakeCollector collector version */
	private static final String FAKE_COLLECTOR_VERSION="0.1";
	
	protected int totalInterval = 0;
	private int currentInterval = 0;
	protected List<FakeMetric> metrics = new ArrayList<FakeMetric>();
	
	
	public CollectionResult collectCallback() {
		log.info("collectCallback for FakeCollector bean: "+this.getBeanName());
		long start = System.currentTimeMillis();
		CollectionResult result = new CollectionResult();
		try{
			for(FakeMetric fm: metrics){
				if(currentInterval < fm.values.length){
					tracer.traceSticky(fm.getValues()[currentInterval], fm.metricName, getTracingNameSpace());
				}else{
					// The number of values provided were less than the totalIntervals, so tracing fakeMetric's 
					// default value.
					tracer.traceSticky(fm.getDefaultValue(), fm.metricName, getTracingNameSpace());
				}
			}
			
		result.setResultForLastCollection(CollectionResult.Result.SUCCESSFUL);
		}catch(Exception ex){
			if(logErrors)
			log.error("An error occured during collect callback for FakeCollector: "+this.getBeanName(),ex);
			result.setResultForLastCollection(CollectionResult.Result.FAILURE);
			result.setAnyException(ex);
			return result;			
		}finally{
			tracer.traceSticky(System.currentTimeMillis()-start, "Elapsed Time", getTracingNameSpace());
		}
		return result;
	}
	
	/**
	 * Returns the Fake Collector version
	 */
	public String getCollectorVersion() {
		return "FakeCollector v. " + FAKE_COLLECTOR_VERSION;
	}
	
	public void startCollector() throws CollectorStartException {
		log.info("startCollector executed for FakeCollector bean: "+this.getBeanName());
	}

	public int getTotalInterval() {
		return totalInterval;
	}

	public void setTotalInterval(int totalInterval) {
		this.totalInterval = totalInterval;
	}

	public List<FakeMetric> getMetrics() {
		return metrics;
	}

	public void setMetrics(List<FakeMetric> metrics) {
		this.metrics = metrics;
	}
	
}
