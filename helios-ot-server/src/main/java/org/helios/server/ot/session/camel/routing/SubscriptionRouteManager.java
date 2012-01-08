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
package org.helios.server.ot.session.camel.routing;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.log4j.Logger;
import org.eclipse.jetty.continuation.Continuation;
import org.helios.server.ot.session.OutputFormat;
import org.helios.server.ot.session.SessionSubscriptionTerminator;
import org.helios.spring.container.jmx.ApplicationContextService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>Title: SubscriptionRouteManager</p>
 * <p>Description: Manages subscription routes on behalf of a subscriber client.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.SubscriptionRouteManager</code></p>
 */

public class SubscriptionRouteManager implements ApplicationContextAware, CamelContextAware, SessionSubscriptionTerminator {


	/** The Spring application context */
	protected ApplicationContext applicationContext;
	/** The camel helios context */
	protected CamelContext camelContext;
	/** Instance logger */
	protected final Logger log;
	
	/** The subscribing sessionId */
	protected final String sessionId;
	/** The session's OutputFormat */
	protected final OutputFormat outputFormat;
	/** The output processor type */
	protected final String processorType;
	/** The processor created for this router */
	protected final SubscriptionOutputProcessor<?> outputProcessor;
	/** Indicates if the router has been started */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** A map of active subscriber routes in this context keyed by the route id*/
	protected final Map<String, ISubscriberRoute<?>> routes = new ConcurrentHashMap<String, ISubscriberRoute<?>>();
	/** The route manager's starting timestamp */
	protected final long createdTimestamp;
	/** The completion size of the aggregating strategy */
	protected int completionSize = 10;
	/** The completion timeout of the aggregating strategy */
	protected long completionTimeout = 5000;
	/** The subscriber route registry to build route instances */
	protected SubscriberRouteRegistry routeRegistry = null;
	
	
//	/** Serial number factory */
//	private static final AtomicLong serial= new AtomicLong(0L);
	
	/**
	 * Creates a new SubscriptionRouteManager
	 * @param sessionId The subscribing sessionId
	 * @param outputFormat The session's OutputFormat
	 * @param processorType The output processor type name  (e.g. PollingHttpSubscriptionProcessor)
	 */
	public SubscriptionRouteManager(String sessionId, OutputFormat outputFormat, String processorType) {			
		if(sessionId==null) throw new IllegalArgumentException("The passed sessionId was null", new Throwable());
		if(outputFormat==null) throw new IllegalArgumentException("The passed outputFormat was null", new Throwable());
		this.sessionId = sessionId;
		this.outputFormat = outputFormat;
		this.processorType = processorType;
		log = Logger.getLogger(getClass().getName() + "." + sessionId);
		createdTimestamp = System.currentTimeMillis();		
		if(processorType!=null && !"".equals(processorType)) {			
			log.info("Created SubscriptionRouteManager for session [" + sessionId + "]");
			// We have to use the static helios app ctx to get the spring application context
			// because the app context is not injected until after the ctor is executed.			
			outputProcessor = (SubscriptionOutputProcessor<?>)ApplicationContextService.get().getBean("PollingHttpSubscriptionProcessor", sessionId, outputFormat);
		} else {
			outputProcessor = null;
			log.info("Created SubscriptionRouteManager Prototype");
		}		
	}
	
	/**
	 * Creates a new SubscriptionRouteManager
	 * @param sessionId The subscribing sessionId
	 * @param outputFormat The session's OutputFormat
	 * @param processorType The output processor type name  (e.g. PollingHttpSubscriptionProcessor)
	 * @return a new SubscriptionRouteManager
	 */
	public static SubscriptionRouteManager getInstance(String sessionId, OutputFormat outputFormat, String processorType) {
		return new SubscriptionRouteManager(sessionId, outputFormat, processorType);
	}
	
	
	/**
	 * Starts a new Subscription Route
	 * @param sessionId The subscriber session id.
	 * @param routeType The subscriber route type key
	 * @param subscriberParams The subscrier route parameters
	 * @return a new ISubscriberRoute
	 */
	public Map<String, Object> startSubscriptionRoute(String sessionId, String routeType, Map<String, String> subscriberParams) {
		try {			
			return routeRegistry.getInstance(routeType, sessionId, outputProcessor, subscriberParams);
		} catch (Exception e) {
			log.error("Failed to startSubscriptionRoute", e);
			throw new RuntimeException("Failed to startSubscriptionRoute", e);
		}
	}
	
	/**
	 * Stops a subscription subFeedKey
	 * @param routeType The router type key
	 * @param sessionId The session Id
	 * @param subscriberParams The subFeed properties
	 */
	public void stopSubscriptionRoute(String routeType, String sessionId, Map<String, String> subscriberParams) {
		log.info("Stopping SubKey\n\tType [" + routeType + "] \n\tSession [" + sessionId + "]\n\tSubKey [" + subscriberParams.get(ISubscriberRoute.HEADER_SUB_FEED_KEY) + "]");
		routeRegistry.terminateSubFeed(routeType, sessionId, subscriberParams);
	}
	
	/**
	 * Polls for item delivery
	 * @param atATime The maximum number of items to deliver at a time
	 * @param timeout The period of time in ms. to wait if there are no items for delivery
	 * @return A [possibly empty] set of polled items
	 */
	public Set<?> poll(int atATime, long timeout) {
		return outputProcessor.poll(atATime, timeout);
	}
	
	/**
	 * Registers a continuation that the processor will resume when a new item is published
	 * @param continuation a jetty continuation
	 */
	public void registerContinuation(Continuation continuation) {
		outputProcessor.registerContinuation(continuation);
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
	 * <p>Terminates the output processor and any active subscription routes 
	 * @see org.helios.server.ot.session.SessionSubscriptionTerminator#terminate()
	 */
	@Override
	public void terminate() {
		if(outputProcessor!=null) outputProcessor.terminate();
		routeRegistry.terminateSession(sessionId); 
		routes.clear();		
	}

	/**
	 * Returns the subscriber route registry to build route instances
	 * @return the routeRegistry
	 */
	public SubscriberRouteRegistry getRouteRegistry() {
		return routeRegistry;
	}

	/**
	 * Sets the subscriber route registry to build route instances
	 * @param routeRegistry the routeRegistry to set
	 */
	public void setRouteRegistry(SubscriberRouteRegistry routeRegistry) {
		this.routeRegistry = routeRegistry;
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Returns the output processor
	 * @return the outputProcessor
	 */
	public SubscriptionOutputProcessor<?> getOutputProcessor() {
		return outputProcessor;
	}
	

}
