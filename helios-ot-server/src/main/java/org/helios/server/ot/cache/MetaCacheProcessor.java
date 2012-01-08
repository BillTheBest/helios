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
			elem = new Element(trace.getFQN(), trace.getMetricId());
			log.info("Processing New Element:[" + trace.getFQN() + "]");
			metricNameCache.put(elem);
		}
	}
	
	/**
	 * Returns the parent path of the passed path
	 * @param path The path to get the parent of
	 * @return The parent path
	 */
	public static String getParentKey(CharSequence path) {
		if(path==null) throw new IllegalArgumentException("The passed metric path was null", new Throwable());
		String sPath = path.toString();
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
				
	}
	

}
