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
package org.helios.server.ot.jms.pubsub;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.BaseCommand;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.RemoveInfo;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.jms.JmsMessage;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: SubListener</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.jms.pubsub.SubListener</code></p>
 */
@ManagedResource(objectName="org.helios.server.ot.jms.pubsub:service=SubListener")
public class SubListener implements Processor {

	/** The subscription pattern cache */
	@Autowired(required=true)
	@Qualifier("subCache")	
	protected Cache subCache;
	/** The consumer to subscription pattern cache */
	@Autowired(required=true)
	@Qualifier("consumerCache")	
	protected Cache consumerCache;
	
	/** The consumer tabular type */
	protected final TabularType consumerType = getConsumerEntryType();
	/** The sub pattern tabular type */
	protected final TabularType subPatternType = getMetricSubPatternType();

	
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
	 */
	@Override
	public void process(Exchange exchange) throws Exception {		
		BaseCommand bc = (BaseCommand)((ActiveMQMessage)((JmsMessage)exchange.getIn()).getJmsMessage()).getDataStructure();
		if(bc instanceof ConsumerInfo) {
			ConsumerInfo ci = (ConsumerInfo)bc;ci.getDestination().toString().replace("topic://helios.metrictree.", "").replaceAll(">$", "*");
			SubscriptionPattern sp = SubscriptionPattern.newInstance(ci.getDestination().toString()); 
			String subPattern = sp.getPatternValue();
			Element element = subCache.get(subPattern);			
			if(element!=null) {
				((SubscriptionPattern)element.getObjectValue()).incr();
			} else {
				sp.incr();
				subCache.put(sp.getElement());
			}
			consumerCache.put(new Element(ci.getConsumerId().toString(), subPattern));
			log.info("Processing Sub Start on [" + subPattern + "]  ConsumerId: [" + ci.getConsumerId().toString() + "]" );
		} else if (bc instanceof RemoveInfo) {
			RemoveInfo ri = (RemoveInfo)bc;
			Element element = consumerCache.get(ri.getObjectId().toString());
			if(element!=null) {
				log.info("Processing Sub Stop on ObjectId: [" + ri.getObjectId() + "] with sub pattern [" + element.getValue() + "]");
				String subPattern = (String)element.getValue();
				if(consumerCache.remove(ri.getObjectId().toString())) {
					log.info("Cleared Consumer Cache of [" + ri.getObjectId().toString() + "]");
				} else {
					log.warn("Failed to remove item with Key [" + ri.getObjectId().toString() + "]");
				}				
				Element subElement = subCache.get(subPattern);			
				if(subElement!=null) {
					SubscriptionPattern sp = ((SubscriptionPattern)subElement.getObjectValue()).decr();
					if(sp.getSubCount()<1) {
						log.info("Sub Pattern [" + subPattern + "] ticked to zero and removed");
						subCache.remove(subPattern);
					} else {
						log.info("Sub Pattern [" + subPattern + "] ticked to [" + sp.getSubCount() + "]");
					}
				} else {
					log.warn("No Sub Pattern found for [" + subPattern + "] on consumer removal");
				}
			} else {
				log.warn("No consumer cache entry found for [" + ri.getObjectId().toString() + "] on RemovInfo");
			}
		} else {
			log.info("Unrecognized BaseCommand Type [" + bc.getClass().getName() + "]");
		}
	}
	
	
	/**
	 * Processes the sub pattern
	 * @param pattern The subscription pattern being processes
	 * @param add true if the pattern is being added, false if it is being removed
	 * @return true if the process was a remove and resulted in a terminated subPattern, false otherwise
	 */
	protected boolean processSubPattern(String pattern, boolean add) {
		AtomicInteger ai = null;
		Element element = subCache.get(pattern);
		if(element==null) {
			synchronized(subCache) {
				element = subCache.get(pattern);
				if(element==null) {
					element = new Element(pattern, new AtomicInteger(0));
					subCache.put(element);
				}
			}
		}
		ai = (AtomicInteger)element.getObjectValue();
		if(add) {
			int subCount = ai.incrementAndGet();
			log.info("Cached SubPattern [" + pattern + "] with count [" + subCount + "]");
			return false;
		}
		int subCount = ai.decrementAndGet();
		if(subCount<1) {
			subCache.remove(pattern);
			return true;
		}
		return false;
	}

	/**
	 * Returns currently active subscrier consumer Ids
	 * @return the currently active subscrier consumer Ids
	 */
	@ManagedAttribute(description="The currently active subscriber consumer Ids")
	public TabularData getConsumerIdKeys() {
		try {
			TabularDataSupport tds = new TabularDataSupport(consumerType);
			CompositeType ct = tds.getTabularType().getRowType();
			for(Object key : consumerCache.getKeys()) {				
				Element element = consumerCache.get(key);
				if(element==null) continue;
				tds.put(new CompositeDataSupport(ct, 
					new String[]{"ConsumerID", "SubPattern"}, 
					new Object[]{key.toString(), element.getValue().toString()})
				);				
			}		
			return tds;
		} catch (Exception e) {
			log.error("Failed to render ConsumerIdKeys", e);
			throw new RuntimeException("Failed to render ConsumerIdKeys:" + e);
		}
	}
	
	/**
	 * Returns currently subscribed metric patterns 
	 * @return the currently subscribed metric patterns
	 */
	@ManagedAttribute(description="The currently subscribed metric patterns")
	public TabularData getSubscribedMetricPatterns() {
		try {
			TabularDataSupport tds = new TabularDataSupport(subPatternType);
			CompositeType ct = tds.getTabularType().getRowType();
			for(Object key : subCache.getKeys()) {				
				Element element = subCache.get(key);
				if(element==null) continue;
				tds.put(new CompositeDataSupport(ct, 
					new String[]{"Pattern", "SubscriberCount"}, 
					new Object[]{key.toString(), ((SubscriptionPattern)element.getValue()).getSubCount()})
				
				);				
			}			
			return tds;
		} catch (Exception e) {
			log.error("Failed to render ubscribedMetricPatterns", e);
			throw new RuntimeException("Failed to render ubscribedMetricPatterns:" + e);
		}
	}
	
	
//	/**
//	 * Returns currently active subscrier consumer Ids
//	 * @return the currently active subscrier consumer Ids
//	 */
//	@ManagedAttribute(description="The currently active subscrier consumer Ids")
//	public String[] getConsumerIdKeys() {
//		List keys = consumerCache.getKeys();
//		String[] strKeys = new String[keys.size()];
//		int cnt = 0;
//		for(Object key: keys) {
//			strKeys[cnt] = key.toString();
//			cnt++;
//		}
//		return strKeys;
//	}
	
	/**
	 * Creates the ConsumerEntryType Tabular type
	 * @return the ConsumerEntryType TabularType
	 */
	protected static TabularType getConsumerEntryType() {
		try {
			return new TabularType ("ConsumerEntriesType", "Describes the current consumer IDs and their mappings to Metric Sub Patterns", 
					new CompositeType(
					"ConsumerEntryType",
					"Describes a current consumer ID and its mappings to Metric Sub Patterns",
					new String[]{"ConsumerID", "SubPattern"},
					new String[]{"The JMS Consumer ID", "The Metric Subscription Pattern"},
					new OpenType[]{SimpleType.STRING, SimpleType.STRING}
			), new String[]{"ConsumerID"});
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ConsumerEntryType", e);
		}
	}
	
	/**
	 * Creates the Metric Sub Pattern Tabular type
	 * @return the Metric Sub Pattern  TabularType
	 */
	protected static TabularType getMetricSubPatternType() {
		try {
			return new TabularType("MetricSubPattern", "The current Metric Sub Patterns and the number of subscribers for each", new CompositeType(
					"MetricSubPattern",
					"A Metric Sub Pattern and the number of subscribers",
					new String[]{"Pattern", "SubscriberCount"},
					new String[]{"The metric pattern", "The number of subscribers"},
					new OpenType[]{SimpleType.STRING, SimpleType.INTEGER}
			), new String[]{"Pattern"});
		} catch (Exception e) {
			throw new RuntimeException("Failed to create MetricSubPattern", e);
		}
	}
	
}
