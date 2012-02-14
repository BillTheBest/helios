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

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.ehcache.Cache;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.helios.ot.trace.ClosedTrace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: TopicTreeRepublisher</p>
 * <p>Description: Consumes aagent incoming metric messages and republishes them on the topic tree.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.jms.pubsub.TopicTreeRepublisher</code></p>
 */
@ManagedResource(objectName="org.helios.server.ot.jms.pubsub:service=TopicTreeRepublisher")
public class TopicTreeRepublisher implements Processor, CamelContextAware {
	/** The subscription pattern cache */
	@Autowired(required=true)
	@Qualifier("subCache")
	protected Cache subCache;
	
	/** The camel context */
	protected CamelContext camelContext = null;
	/** The endpoint lookup map */
	protected Map<String, Endpoint> endPointMap = null;
	
	/** The sender template */
	private final ProducerTemplate template;
	/** The topic tree destination prefix */
	private final String destinationPrefix;
	/** The topic tree provider prefix */
	private final String providerPrefix;
	
	/** The number of messages published into the metric tree */
	protected final AtomicLong publishedCount = new AtomicLong(0L);
	/** The number of messages dropped */
	protected final AtomicLong dropCount = new AtomicLong(0L);
	
	/**
	 * Creates a new TopicTreeRepublisher
	 * @param template The sender template
	 * @param destinationPrefix The topic tree destination prefix 
	 * @param providerPrefix The topic tree provider prefix
	 */
	public TopicTreeRepublisher(ProducerTemplate template, String destinationPrefix, String providerPrefix) {
		this.template = template;
		this.destinationPrefix = destinationPrefix;
		this.providerPrefix = providerPrefix;
		
		
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		if(exchange!=null) {
			Message msg = exchange.getIn();
			ClosedTrace trace = msg.getBody(ClosedTrace.class);
			msg.setBody(trace.toString());			
			msg.setHeaders(trace.getTraceMap());
			msg.setHeader("Dest", destinationPrefix + trace.getFQN().replace('/', '.'));
			exchange.setOut(msg);
		}
	}
	
	
	// "[^\\.]\\*"
	

	
	public void send(Exchange exchange) {
		Message msg = exchange.getIn();
		ClosedTrace trace = msg.getBody(ClosedTrace.class);
		int matches = subCache.createQuery()
		.addCriteria(new PatternMatchesTo("pattern", trace.getFQN()))
		.maxResults(1)
		.includeKeys()
		.execute().size();
		if(matches > 0) {
			msg.setBody(trace);
			msg.setHeaders(trace.getTraceMap());					
			template.asyncSend(getEndpoint(String.format("%s:%s.%s", providerPrefix, destinationPrefix, trace.getFQN()).replace('/', '.')), exchange);
			publishedCount.incrementAndGet();
		} else {
			dropCount.incrementAndGet();
		}
	}
	
	/**
	 * Acquires the endpoint for the passed name
	 * @param name The name of the endpoint
	 * @return the named endpoint
	 */
	protected Endpoint getEndpoint(String name) {
		Endpoint endpoint = endPointMap.get(name);
		if(endpoint==null) {
			synchronized(endPointMap) {
				endpoint = endPointMap.get(name);
				if(endpoint==null) {
					endpoint = camelContext.getEndpoint(name);
				}
			}
		}
		return endpoint;
	}

	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.CamelContextAware#getCamelContext()
	 */
	@Override
	@ManagedAttribute(description="The camel context")
	public CamelContext getCamelContext() {
		return camelContext;
	}

	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.CamelContextAware#setCamelContext(org.apache.camel.CamelContext)
	 */
	@Override
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;		
		endPointMap = this.camelContext.getEndpointMap(); 
	}

	/**
	 * Returns the number of messages published into the metric tree 
	 * @return the publishedCount
	 */
	@ManagedAttribute(description="The number of messages published into the metric tree")
	public long getPublishedCount() {
		return publishedCount.get();
	}
	
	/**
	 * Returns the number of messages dropped (ie. <i>not</i> published into the metric tree) 
	 * @return the drop Count
	 */
	@ManagedAttribute(description="The number of messages dropped")
	public long getDropCount() {
		return dropCount.get();
	}
	

	/**
	 * Returns the number of entries in the pattern cache
	 * @return the pattern cache size
	 */
	@ManagedAttribute(description="The number of entries in the pattern cache")
	public int getCacheSize() {
		return subCache.getSize();
	}

	/**
	 * Returns the pattern cache average search time in ms.
	 * @return the pattern cache average search time in ms.
	 */
	@ManagedAttribute(description="The pattern cache average search time (ms.)")
	public long getCacheAverageSearchTime() {
		return subCache.getAverageSearchTime();
	}
	
	/**
	 * Returns the pattern cache searches per second
	 * @return the pattern cache searches per second
	 */
	@ManagedAttribute(description="The pattern cache searches per second")
	public long getCacheSearchesPerSecond() {
		return subCache.getSearchesPerSecond();
	}

	/**
	 * The republisher's JMS destination prefix
	 * @return the destinationPrefix
	 */
	@ManagedAttribute(description="The republisher's JMS destination prefix")
	public String getDestinationPrefix() {
		return destinationPrefix;
	}

	/**
	 * The republisher's JMS provider prefix
	 * @return the providerPrefix
	 */
	@ManagedAttribute(description="The republisher's JMS provider prefix")
	public String getProviderPrefix() {
		return providerPrefix;
	}
	

}
