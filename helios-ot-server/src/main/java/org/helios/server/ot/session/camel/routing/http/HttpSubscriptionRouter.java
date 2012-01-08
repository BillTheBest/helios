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

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.log4j.Logger;
import org.helios.ot.trace.ClosedTrace;
import org.helios.server.ot.session.OutputFormat;
import org.helios.server.ot.session.SessionSubscriptionTerminator;
import org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>Title: HttpSubscriptionRouter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.http.HttpSubscriptionRouter</code></p>
 */

public class HttpSubscriptionRouter extends RouteBuilder implements ApplicationContextAware, CamelContextAware, SessionSubscriptionTerminator {
	/** The Spring application context */
	protected ApplicationContext applicationContext;
	/** The camel helios context */
	protected CamelContext camelContext;
	/** Instance logger */
	protected final Logger log;
	
	/** The subscription mask */
	protected final String mask;
	/** The subscribing sessionId */
	protected final String sessionId;
	/** The session's OutputFormat */
	protected final OutputFormat outputFormat;
	/** The processor created for this router */
	protected SubscriptionOutputProcessor processor = null;
	/** Indicates if the router has been started */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The id of the route created */
	protected String routeId = null;
	
	/** The completion size of the aggregating strategy */
	protected int completionSize = 10;
	/** The completion timeout of the aggregating strategy */
	protected long completionTimeout = 5000;
	
	
	/** Serial number factory */
	private static final AtomicLong serial= new AtomicLong(0L);
	
	/**
	 * Creates a new HttpSubscriptionRouter
	 * @param mask The subscription mask
	 * @param sessionId The subscribing sessionId
	 * @param outputFormat The session's OutputFormat 
	 */
	public HttpSubscriptionRouter(String mask, String sessionId, OutputFormat outputFormat) {
		this.mask = mask;
		this.sessionId = sessionId;
		this.outputFormat = outputFormat;
		log = Logger.getLogger(getClass().getName() + "." + sessionId);
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.CamelContextAware#getCamelContext()
	 */
	@Override
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

	}


	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.builder.RouteBuilder#configure()
	 */
	@Override
	public void configure() throws Exception {
		processor = (SubscriptionOutputProcessor)applicationContext.getBean("PollingHttpSubscriptionProcessor", sessionId, outputFormat);
		routeId = processor.getClass().getSimpleName() + "-" + sessionId + "-" + serial.incrementAndGet();
		from("activemq:topic:" + mask)
		.process(new Processor(){
			public void process(Exchange exchange) throws Exception {			
				Message in = exchange.getIn();
				ClosedTrace trace = in.getBody(ClosedTrace.class);
				in.setBody(new HashSet<ClosedTrace>(Arrays.asList(trace)));
				in.setHeader("aggrId", routeId);
			}
		})
		.aggregate(header("aggrId"), new TraceAggregationStrategy())
			.completionSize(completionSize)
			.completionTimeout(completionTimeout)
		.marshal(outputFormat.getBeanName())
		.process(processor)		
		.setId(routeId);
        from("jmx:platform?objectDomain=org.helios.server.ot.cache&key.service=CacheEventManager&format=raw")
        .marshal(outputFormat.getBeanName())
        .process(processor);
        
/*

format 	  	xml 	Format for the message body. Either "xml" or "raw". If xml, the notification is serialized to xml. If raw, then the raw java object is set as the body.
user 	  	  	Credentials for making a remote connection.
password 	  	  	Credentials for making a remote connection.
objectDomain 	yes 	  	The domain for the mbean you're connecting to.
objectName 	  	  	The name key for the mbean you're connecting to. This value is mutually exclusive with the object properties that get passed. (see below)
notificationFilter 	  	  	Reference to a bean that implements the NotificationFilter. The #ref syntax should be used to reference the bean via the Registry.
handback 	  	  	Value to handback to the listener when a notification is received. This value will be put in the message header with the key "jmx.handback" 

 */
        

		log.info("Created Processor [" + routeId + "]");
		
	}
	
	/**
	 * Starts the router
	 */
	public void start() {
		try {
			camelContext.addRoutes(this);			
			camelContext.startRoute(routeId);
			log.info("Started Processor [" + routeId + "]");
		} catch (Exception e) {
			log.error("Failed to start HttpSubscriptionRouter", e);
			throw new RuntimeException("Failed to start HttpSubscriptionRouter", e);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>Stops the router</p>
	 * @see org.helios.server.ot.session.SessionSubscriptionTerminator#terminate()
	 */
	@Override
	public void terminate() {
		if(processor!=null) processor.terminate();
	}

	/**
	 * Returns the ID of the Camel route created for this router
	 * @return the routeId
	 */
	public String getRouteId() {
		return routeId;
	}

	/**
	 * @return the processor
	 */
	public SubscriptionOutputProcessor getProcessor() {
		return processor;
	}

	/**
	 * The aggregating strategy completion size
	 * @return the completionSize
	 */
	public int getCompletionSize() {
		return completionSize;
	}

	/**
	 * Sets the aggregating strategy completion size
	 * @param completionSize the completionSize to set
	 */
	public void setCompletionSize(int completionSize) {
		this.completionSize = completionSize;
	}

	/**
	 * The aggregating strategy timeout completion in ms.
	 * @return the completionTimeout
	 */
	public long getCompletionTimeout() {
		return completionTimeout;
	}

	/**
	 * Sets the aggregating strategy timeout completion in ms.
	 * @param completionTimeout the completionTimeout to set
	 */
	public void setCompletionTimeout(long completionTimeout) {
		this.completionTimeout = completionTimeout;
	}

}
