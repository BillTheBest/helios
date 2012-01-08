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
package org.helios.server.ot.session.camel.routing.http;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.log4j.Logger;
import org.helios.server.ot.session.camel.routing.ISubscriberRoute;

/**
 * <p>Title: TraceAggregationStrategy</p>
 * <p>Description: A camel aggregation strategy to aggregate multiple traces into a single collection of traces.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.http.TraceAggregationStrategy</code></p>
 */

public class TraceAggregationStrategy<T> implements AggregationStrategy {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/**
	 * {@inheritDoc}
	 * Currently:  <router-key> -->  Set<ClosedTrace>
	 * Modify To:  <router-key> -->  Map<SubFeedKey, ClosedTrace>
	 * @see org.apache.camel.processor.aggregate.AggregationStrategy#aggregate(org.apache.camel.Exchange, org.apache.camel.Exchange)
	 */
	@SuppressWarnings("unchecked")
	@Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		//logExchange(newExchange);
		Map<String, Map<String, Set<T>>> aggrBody = new HashMap<String, Map<String, Set<T>>>();
		HashSet<T> newBody = null;
		try {
			if(oldExchange==null) {
				Message msg = newExchange.hasOut() ? newExchange.getOut() : newExchange.getIn();
				newBody = msg.getBody(HashSet.class);
				String typeKey = msg.getHeader(ISubscriberRoute.HEADER_TYPE_KEY, String.class);
				String subFeedKey = msg.getHeader(ISubscriberRoute.HEADER_SUB_FEED_KEY, String.class);
				if(subFeedKey!=null) {
					aggrBody.put(typeKey, newSubFeedKeyMap(subFeedKey, newBody));
					msg.setBody(aggrBody);					
				}
				return newExchange;
			} 
			Message oldMsg = oldExchange.getIn();
			Message newMsg = newExchange.getIn();
			aggrBody = oldMsg.getBody(HashMap.class);
			newBody = newMsg.getBody(HashSet.class);
			String typeKey = newMsg.getHeader(ISubscriberRoute.HEADER_TYPE_KEY, String.class);
			String subFeedKey = newMsg.getHeader(ISubscriberRoute.HEADER_SUB_FEED_KEY, String.class);
			if(subFeedKey!=null) {
				Map<String, Set<T>> oldMap = (Map<String, Set<T>>) aggrBody.get(typeKey);
				updateOldMap(oldMap, subFeedKey, newBody);
			}
			//oldSet.addAll(newBody);
			return oldExchange;
		} catch(Exception e) {
			log.error("Failed to aggregate ClosedTrace Exchanges", e);
			throw new RuntimeException("Failed to aggregate ClosedTrace Exchanges", e);
		}
    }
	
	
	/**
	 * Adds new items to the old body
	 * @param oldMap The old map
	 * @param subFeedKey The new subFeedKey
	 * @param items the new items to add
	 */
	protected void updateOldMap(final Map<String, Set<T>> oldMap, String subFeedKey, final Set<T> items) {
		Set<T> oldItems = oldMap.get(subFeedKey);
		if(oldItems==null) {
			oldItems = new HashSet<T>();
			oldMap.put(subFeedKey, oldItems);
		}
		oldItems.addAll(items);
	}
	
	/**
	 * Creates a new SubFeedKey Map
	 * @param subFeedKey The new subFeedKey
	 * @param items The first items to add
	 * @return the new SubFeedKey Map
	 */
	protected Map<String, Set<T>> newSubFeedKeyMap(String subFeedKey, Set<T> items) {
		Map<String, Set<T>> map = new HashMap<String, Set<T>>();
		map.put(subFeedKey, items);
		return map;
	}
	
	
/*
	@SuppressWarnings("unchecked")
	@Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		//logExchange(newExchange);
		Map<String, Set<T>> aggrBody = new HashMap<String, Set<T>>();
		HashSet<T> newBody = null;
		try {
			if(oldExchange==null) {
				Message msg = newExchange.hasOut() ? newExchange.getOut() : newExchange.getIn();
				newBody = msg.getBody(HashSet.class);
				String typeKey = msg.getHeader(ISubscriberRoute.HEADER_SUB_ROUTER_KEY, String.class);
				aggrBody.put(typeKey, newBody);
				msg.setBody(aggrBody);
				//newExchange.setIn(msg);
				return newExchange;
			} 
			Message oldMsg = oldExchange.getIn();
			Message newMsg = newExchange.getIn();
			aggrBody = oldMsg.getBody(HashMap.class);
			newBody = newMsg.getBody(HashSet.class);
			String typeKey = newMsg.getHeader(ISubscriberRoute.HEADER_SUB_ROUTER_KEY, String.class);
			HashSet<T> oldSet = (HashSet<T>) aggrBody.get(typeKey);
			oldSet.addAll(newBody);
			//oldSet.addAll(newBody);
			return oldExchange;
		} catch(Exception e) {
			log.error("Failed to aggregate ClosedTrace Exchanges", e);
			throw new RuntimeException("Failed to aggregate ClosedTrace Exchanges", e);
		}
    } 
 */
	
	
	protected void logExchange(Exchange exchange) {
		StringBuilder b = new StringBuilder("Exchange to be aggregated:");
		b.append("\n\tExchange Properties:");
		for(Map.Entry<String, Object> entry: exchange.getProperties().entrySet()) {
			b.append("\n\t\t").append(entry.getKey()).append(":").append(entry.getValue());
		}
		Message msg = exchange.getIn();
		b.append("\n\tMessage Headers:");
		for(Map.Entry<String, Object> entry: msg.getHeaders().entrySet()) {
			b.append("\n\t\t").append(entry.getKey()).append(":").append(entry.getValue());
		}
		log.info(b.toString());
	}
	

}
