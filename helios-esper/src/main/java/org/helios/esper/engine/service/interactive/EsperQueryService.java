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

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;

/**
 * <p>Title: EsperQueryService</p>
 * <p>Description: Provides esper on demmand and query subscription services.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.service.interactive.EsperQueryService</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class EsperQueryService extends ManagedObjectDynamicMBean {
	/**  */
	private static final long serialVersionUID = -9074844961471439832L;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
//	/**
//	 * Executes an on demmand epl query and returns the rednered results.
//	 * @param title the element title
//	 * @param epl the epl on demmand query
//	 * @param inXml if true, results are returned in XML, if false, returned in JSON
//	 * @return the results of the on demmand query rendered in a text format
//	 */
//	@JMXOperation(name="query", description="Executes an on demmand epl query and returns the results in JSON or XML")
//	public String query(String title, String epl, boolean inXml) throws Exception {
//		StringBuilder b = new StringBuilder();
//		try {
//			if(log.isDebugEnabled()) log.debug("Executing OnDemmand EPL [" + epl + "]");
//			EPOnDemandQueryResult result = engine.getEsperRuntime().executeQuery(epl);
//			EventBean[] results = result.getArray();
//			if(log.isDebugEnabled()) log.debug("Processed EPL for [" + results.length + "] results");
//			
//			for(EventBean eb: results) {
//				if(inXml) {
//					b.append(render.renderXML(title, eb));
//				} else {
//					b.append(render.renderJSON(title, eb));
//				}
//				
//			}
//			b.append("\n\t<!--").append("Event Type:").append(result.getEventType().getName()).append("  Count:").append(results.length).append("  -->");
//			return b.toString();
//		} catch (Exception e) {
//			log.error("Failed to execute epl [" + epl + "]", e);
//			throw new Exception("Failed to execute epl [" + epl + "]", e);
//		}
//		
//		
//	}
}
