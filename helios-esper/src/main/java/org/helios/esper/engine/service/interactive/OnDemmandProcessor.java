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
package org.helios.esper.engine.service.interactive;

import java.util.Collection;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.esper.engine.Engine;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.EventRenderer;

/**
 * <p>Title: OnDemmandProcessor</p>
 * <p>Description: Processor for executing on demmand esper queries.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.service.interactive.OnDemmandProcessor</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class OnDemmandProcessor extends ManagedObjectDynamicMBean {
	/**  */
	private static final long serialVersionUID = -5639152914848615513L;
	/** the esper engine */
	protected Engine engine = null;
	/** the XML and JSON renderer */
	protected EventRenderer render = null;
	/** instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	
	public static ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.esper.services:name=OnDemmandProcessor");
	
	/**
	 * Creates a new OnDemmandProcessor
	 * @param engine the helios esper engine
	 */
	public OnDemmandProcessor(Engine engine) {
		super();
		this.engine = engine;
	}
	
	/**
	 * Initializes the onDemmand processor
	 * @throws Exception
	 */
	public void init() throws Exception {
		log.info("\n\t==============================\n\tStarting OnDemmandProcessor\n\t==============================\n");
		if(engine==null) throw new Exception("Helios Esper Engine was null");
		if(JMXHelper.getHeliosMBeanServer().isRegistered(OBJECT_NAME)) {
			throw new Exception("Helios Esper Engine OnDemmandProcessor already registered");
		}
		render = engine.getEsperRuntime().getEventRenderer();
		this.reflectObject(this);
		JMXHelper.getHeliosMBeanServer().registerMBean(this, OBJECT_NAME);
		log.info("\n\t==============================\n\tStarted OnDemmandProcessor\n\t==============================\n");
	}
	
	
	/**
	 * Executes an on demmand epl query and returns the results in JSON or XML
	 * @param title the element title
	 * @param epl the epl on demmand query
	 * @param inXml if true, results are returned in XML, if false, returned in JSON
	 * @return the results of the on demmand query rendered in a text format
	 */
	@JMXOperation(name="query", description="Executes an on demmand epl query and returns the results in JSON or XML")
	public String query(
			@JMXParameter(name="title", description="The title for each element rendered") String title, 
			@JMXParameter(name="epl", description="The epl statement to execute") String epl, 
			@JMXParameter(name="isXml", description="If true, renders in XML, else renders in JSON") boolean inXml) throws Exception {
		StringBuilder b = new StringBuilder();
		try {
			if(log.isDebugEnabled()) log.debug("Executing OnDemmand EPL [" + epl + "]");
			EPOnDemandQueryResult result = engine.getEsperRuntime().executeQuery(epl);
			EventBean[] results = result.getArray();
			if(log.isDebugEnabled()) log.debug("Processed EPL for [" + results.length + "] results");
			
			for(EventBean eb: results) {
				if(inXml) {
					b.append(render.renderXML(title, eb));
				} else {
					b.append(render.renderJSON(title, eb));
				}
				
			}
			b.append("\n\t<!--").append("Event Type:").append(result.getEventType().getName()).append("  Count:").append(results.length).append("  -->");
			return b.toString();
		} catch (Exception e) {
			log.error("Failed to execute epl [" + epl + "]", e);
			throw new Exception("Failed to execute epl [" + epl + "]", e);
		}
		
		
	}
	
	/**
	 * @param epl
	 * @return
	 */
	public Collection<? extends Object> query(String epl) {
		return null;
	}
	
	/**
	 * Returns the helios esper engine
	 * @return the engine
	 */
	public Engine getEngine() {
		return engine;
	}
	
	/**
	 * Sets the helios esper engine
	 * @param engine the engine to set
	 */
	public void setEngine(Engine engine) {
		this.engine = engine;
	}
}
