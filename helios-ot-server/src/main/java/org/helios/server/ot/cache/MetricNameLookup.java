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

import java.util.HashSet;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;

import org.helios.ot.trace.MetricId;
import org.helios.server.ot.jms.pubsub.PatternMatchesTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * <p>Title: MetricNameLookup</p>
 * <p>Description: Service to retrieve MetricIds that match a given expression</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.cache.MetricNameLookup</code></p>
 */

public class MetricNameLookup {

	/** The metric cache */
	@Autowired(required=true)
	@Qualifier("metricNameCache")	
	protected Cache metricCache;

	/** Empty result constant */
	public static final MetricId[] EMPTY_RESULTS = new MetricId[0];
	
	/**
	 * Returns MetricIds from the metric name cache where the FQN matches the passed expression
	 * @param expression The expression to match to MetricId FQNs
	 * @return An array of matching MetricIds
	 */
	public MetricId[] search(String expression) {
		if(expression==null) return EMPTY_RESULTS;
		Set<MetricId> ids = new HashSet<MetricId>();
		Results results = null;
		try {
			results = metricCache.createQuery()
			//.addCriteria(new PatternMatchesTo("fqn", expression))
			.addCriteria(Query.KEY.ilike(expression))
			.includeValues().execute();
			if(results.size()<1) return EMPTY_RESULTS;
			for(Result r: results.all()) {
				ids.add((MetricId)r.getValue());
			}
			
		} finally {
			if(results!=null) results.discard();
		}
		
		
		return ids.toArray(new MetricId[ids.size()]);
	}
	
	
//	public void send(Exchange exchange) {
//		Message msg = exchange.getIn();
//		ClosedTrace trace = msg.getBody(ClosedTrace.class);
//		int matches = subCache.createQuery()
//		.addCriteria(new PatternMatchesTo("pattern", trace.getFQN()))
//		.maxResults(1)
//		.includeKeys()
//		.execute().size();
//		if(matches > 0) {
//			msg.setBody(trace);
//			msg.setHeaders(trace.getTraceMap());					
//			template.asyncSend(getEndpoint(String.format("%s:%s.%s", providerPrefix, destinationPrefix, trace.getFQN()).replace('/', '.')), exchange);
//			publishedCount.incrementAndGet();
//		} else {
//			dropCount.incrementAndGet();
//		}
//	}
	
	
}
