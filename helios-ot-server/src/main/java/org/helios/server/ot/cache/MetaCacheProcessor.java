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
package org.helios.server.ot.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.helios.ot.trace.ClosedTrace;
import org.helios.ot.trace.Trace;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * <p>Title: MetaCacheProcessor</p>
 * <p>Description: Writes incoming metrics to the meta cache</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.cache.MetaCacheProcessor</code></p>
 */

public class MetaCacheProcessor implements Processor, InitializingBean  {
	/** The metric cache */
	@Autowired(required=true)
	@Qualifier("metricNameCache")	
	protected Cache metricNameCache;
	
	/** The last metric cache */
	@Autowired(required=true)
	@Qualifier("lastMetricCache")	
	protected Cache lastMetricCache;
	
	/** The metric tree cache */
	@Autowired(required=true)
	@Qualifier("metricTreeCache")	
	protected Cache metricTreeCache;
	
	
	/** The root entry */
	protected MetricTreeEntry rootEntry;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/** Delimeter and Root key */
	public static final String KEY_DELIM = "/";
	
	/**
	 * {@inheritDoc}
	 * <p>The incoming traces</p>
	 * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
	 */
	@Override
	public void process(Exchange exchange) throws Exception {
		if(exchange!=null) {
			Message msg = exchange.getIn();
			ClosedTrace trace = msg.getBody(ClosedTrace.class);
			processMetricName(trace);
		}
	}
	

	/**
	 * Caches the metric name if it is not in the cache
	 * @param trace The trace
	 */
	protected void processMetricName(ClosedTrace trace) {
		lastMetricCache.put(new Element(trace.getFQN(), trace));
		Element elem = metricNameCache.get(trace.getFQN());
		if(elem==null) {
			//putHierarchy(trace);
			elem = new Element(trace.getFQN(), trace.getMetricId());
			if(log.isDebugEnabled()) log.debug("Processing New Element:[" + trace.getFQN() + "]");
			metricNameCache.put(elem);
			MetricTreeEntry currentEntry = rootEntry;
			String[] segments = trace.getFQN().split("/");
			StringBuilder keyPref = new StringBuilder();
			String lastSegment = null;
			for(String segment: segments) {
				lastSegment = segment;
				if(keyPref.length()==0) {
					keyPref.append(segment);
				} else {
					keyPref.append(KEY_DELIM).append(segment);
				}
				currentEntry.addSubKey(segment, keyPref);
				currentEntry = MetricTreeEntry.getOrCreate(keyPref, getParentKey(keyPref), null, metricTreeCache);
			}			
			currentEntry.setMetricId(trace.getMetricId());
			Element parentElement = metricTreeCache.get(currentEntry.getParentKey());
			MetricTreeEntry mte = (MetricTreeEntry)parentElement.getValue();
			mte.addNode(lastSegment, trace.getMetricId());
			metricTreeCache.put(parentElement);			
		}
		//MetricTreeEntry entry = MetricTreeEntry.getOrCreate(trace.getFQN(), getParentKey(trace.getFQN()), trace.getMetricId(), metricTreeCache);
		
	}
	
	/**
	 * Adds the trace hierarchy to the cache
	 * @param trace The trace with the name to add
	 */
	protected void putHierarchy(ClosedTrace trace) {
		String[] segments = trace.getFQN().split(KEY_DELIM);
		int nameSize = trace.getFQN().length();
		int segSize = segments.length;
		StringBuilder nodeName = new StringBuilder(nameSize);
		StringBuilder parentNodeName = new StringBuilder(nameSize);
		MetricTreeEntry parentEntry = MetricTreeEntry.getOrCreate(segments[0], KEY_DELIM, null, metricTreeCache);
		
		parentNodeName.append(segments[0]);
		nodeName.append(segments[0]);
		if(rootEntry==null) rootEntry = MetricTreeEntry.getOrCreate("/", "", null, metricTreeCache);
		rootEntry.addSubKey(nodeName, nodeName);
		MetricTreeEntry currentEntry = null;
		for(int i = 1; i < segSize; i++) {
			nodeName.append(KEY_DELIM).append(segments[i]);
			if(i==(segSize-1)) {
				currentEntry = MetricTreeEntry.getOrCreate(nodeName, parentNodeName, trace.getMetricId(), metricTreeCache);
				parentEntry.addNode(segments[i], trace.getMetricId());
				currentEntry.addSubKey(segments[i], nodeName);
			} else {
				currentEntry = MetricTreeEntry.getOrCreate(nodeName, parentNodeName, null, metricTreeCache);
				parentEntry.addSubKey(segments[i], nodeName);
			}
			parentEntry = currentEntry;
			parentNodeName.append(KEY_DELIM).append(segments[i]);
		}
	}
	
	/**
	 * Returns the parent path of the passed path
	 * @param path The path to get the parent of
	 * @return The parent path
	 */
	public static String getParentKey(CharSequence path) {
		if(path==null) throw new IllegalArgumentException("The passed metric path was null", new Throwable());
		String sPath = path.toString().toString();
		if(sPath.equals(KEY_DELIM)) return "";
		String[] segments = sPath.split(KEY_DELIM);
		if(segments.length==1) return KEY_DELIM;
		StringBuilder b = new StringBuilder(sPath).reverse();
		b.delete(0, b.indexOf("/")+1);
		return b.reverse().toString();
	}
	
	/**
	 * Creates an indent of the passed number of tabs
	 * @param cnt the number of tabs
	 * @return an indent
	 */
	public static String indent(int cnt) {
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < cnt; i++) { b.append("\t"); }
		return b.toString();
	}

	
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		rootEntry = MetricTreeEntry.getOrCreate("/", "", null, metricTreeCache);
	}
	

}
