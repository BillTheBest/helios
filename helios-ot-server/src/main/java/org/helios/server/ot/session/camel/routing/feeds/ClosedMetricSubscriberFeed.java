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
package org.helios.server.ot.session.camel.routing.feeds;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.management.ObjectName;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.trace.ClosedTrace;
import org.helios.server.ot.session.camel.routing.AbstractSubscriberRoute;
import org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor;
import org.helios.server.ot.session.camel.routing.annotations.SubRoute;
import org.helios.server.ot.session.camel.routing.annotations.SubRouteConfig;
import org.helios.server.ot.session.camel.routing.http.TraceAggregationStrategy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: ClosedMetricSubscriberFeed</p>
 * <p>Description: A subscriber feed to subscribe to closed metric feeds and route the items to the target output processor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.feeds.ClosedMetricSubscriberFeed</code></p>
 * @param <T> The router key type
 */
@SubRoute(typeKey="metric-feed", configuration={
		@SubRouteConfig(name="mask", type=String.class),
		@SubRouteConfig(name="completionSize", type=int.class, defaultValue="10"),
		@SubRouteConfig(name="completionTimeout", type=long.class, defaultValue="5000")
})
public class ClosedMetricSubscriberFeed<T> extends AbstractSubscriberRoute<String> implements InitializingBean, ExceptionListener { 
	/** A map of JMS session/subscriber pairs keyed by the topic they're listening on messages for */
	protected final Map<String, MetricListener> listeners = new ConcurrentHashMap<String, MetricListener>();
	
	/** The JMS Connection Factory */
	protected ConnectionFactory connectionFactory = null;
	/** The JMS Connection */
	protected Connection connection = null;
	/** The ActiveMQ Component that provides the connection factory */
	@Autowired(required=true)
	protected ActiveMQComponent activeMq = null;
	
	/**
	 * Creates a new ClosedMetricSubscriberFeed
	 * @param outputProcessor The end point for items routed by this subscriber route
	 * @param sessionId The session ID that this subscriber route is owned by
	 */
	public ClosedMetricSubscriberFeed(SubscriptionOutputProcessor<?> outputProcessor, String sessionId) {
		super(outputProcessor, sessionId);
	}
	
	/**
	 * Returns the number of registered closed metric listeners registered on behalf of this route
	 * @return the number of registered closed metric listeners registered on behalf of this route
	 */
	@JMXAttribute(name="ListenerCount", description="The number of registered closed metric listeners registered on behalf of this route", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getListenerCount() {
		return listeners.size();
	}
	
	/**
	 * Returns the topic names that this route is subscribed to 
	 * @return the topic names that this route is subscribed to 
	 */
	@JMXAttribute(name="ListenerTopicNames", description="The Topics that this route is subscribed to", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getListenerTopicNames() {
		return listeners.keySet().toArray(new String[listeners.size()]);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("Initializing JMS Constructs");
		connectionFactory = activeMq.getConfiguration().getConnectionFactory();
		connection = connectionFactory.createConnection();
		connection.setExceptionListener(this);
		connection.start();
		log.info("JMS Initialized");
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
	 */
	@Override
	public void onException(JMSException exception) {
		log.warn("JMS Connection Error", exception);
		try { connection.close(); } catch (Exception e) {}
		// Add asynch reconnect process unless shutdown is in process
	}

	
	/**
	 * <p>Title: MetricListener</p>
	 * <p>Description: A JMS listener allocated for each unique router key</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.camel.routing.feeds.ClosedMetricSubscriberFeed.MetricListener</code></p>
	 */
	public class MetricListener implements MessageListener {
		/** The JMS Connection */
		protected Connection connection;
		/** The JMS Session */
		protected Session session;
		/** The JMS Consumer */
		protected MessageConsumer consumer;
		/** The JMS Destination */
		protected final Destination destination;
		/** The detination name */
		protected final String routerKey;
		/** The subFeedKey this listener is registered for */
		protected final String subFeedKey;
		/** The producer to forward the message on */
		protected ProducerTemplate producer = null;
		/** The endpoint to forward the message to */
		protected Endpoint endpoint = null;
		/** Static class logger */
		protected final Logger LOG = Logger.getLogger(MetricListener.class);
		
		
		/**
		 * Creates a new MetricListener
		 * @param conn The JMS Connection
		 * @param routerKey The routerKey representing the destination
		 * @param subFeedKey The callback subKey
		 * @param producer The producer to forward the event on
		 * @param endpoint The endpoint to forward the message to
		 * @throws JMSException thrown on any JMSException
		 */
		public MetricListener(Connection conn, String routerKey, String subFeedKey, ProducerTemplate producer, Endpoint endpoint) throws JMSException {
			this.subFeedKey = subFeedKey;
			this.routerKey = routerKey;
			session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = session.createTopic(routerKey);
			consumer = session.createConsumer(destination);
			consumer.setMessageListener(this);
			this.producer = producer;
			this.endpoint = endpoint;
			LOG.info("Started MetricListener for [" + destination + "]");
		}

		/**
		 * {@inheritDoc}
		 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
		 */
		@Override
		public void onMessage(javax.jms.Message message) {
			try {
				producer.sendBodyAndHeader(endpoint, ((ObjectMessage)message).getObject(), HEADER_SUB_FEED_KEY, subFeedKey);
				eventCount.incrementAndGet();
			} catch (IllegalStateException ise) {
				// Producer is closed.
				final MetricListener ml = this;
				LOG.warn("MetricListener found Producer Closed. Issuing Asynch Stop for Session [" + session + "] Destination [" + destination + "]");
				new Thread("MetricListener AsynchTerminator [" + session + "]/[" + destination + "]") {
					@Override
					public void run() {
						try {
							ml.terminate();
							LOG.info("Terminated MetricListener [" + session + "]/[" + destination + "]");
						} catch (Exception e) {
							LOG.error("Failed to stop MetricListener [" + session + "]/[" + destination + "]", e);
						}
					}
				}.start();
				
			} catch (CamelExecutionException e) {				
				e.printStackTrace();
			} catch (JMSException e) {				
				e.printStackTrace();
			}			
		}
		
		/**
		 * Terminates this MetricListener
		 */
		public void terminate() {
			LOG.info("Terminating MetricListener for [" + destination + "]");
			
			
			try { producer.stop(); } catch (Exception e) {}
			try { consumer.close(); } catch (Exception e) {}
			try { session.close(); } catch (Exception e) {}
		}
		
		/**
		 * Refreshes the listener after a connection failure
		 * @param conn The new connection
		 * @throws JMSException thrown on a failure to refresh
		 */
		public void refreshConnection(Connection conn) throws JMSException {
			terminate();
			session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);			
			consumer = session.createConsumer(destination);
			consumer.setMessageListener(this);	
			LOG.info("Restarted MetricListener for [" + destination + "]");
		}
		
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.routing.AbstractSubscriberRoute#addRouterKey(java.lang.Object, java.lang.String)
	 */
	@Override
	protected void addRouterKey(String routerKey, String subFeedKey, Map<String, String> properties ) {
		log.info("Adding Router Key [" + routerKey + "] with SubFeedKey [" + subFeedKey + "]");
		try {
			MetricListener ml = new MetricListener(connection, routerKey, subFeedKey, producer, endpoint);
			listeners.put(routerKey, ml);
		} catch (Exception e) {			
			log.error("Failed to add router/subFeed key [" + routerKey + "/" + subFeedKey + "]", e);
			throw new RuntimeException("Failed to add router/subFeed key [" + routerKey + "/" + subFeedKey + "]", e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.routing.AbstractSubscriberRoute#extractRouterKey(java.util.Map)
	 */
	@Override
	public String extractRouterKey(Map<String, String> properties) {
		if(properties==null) throw new IllegalArgumentException("The passed properties was null", new Throwable());
		return properties.get(HEADER_SUB_ROUTER_KEY);
	}

	
	/**
	 * Removes a router key representing a unique resource the caller is subscribed to. 
	 * @param routerKey The router key to remove the subscription for
	 */	
	@Override
	protected void removeRouterKey(String routerKey) {
		log.info("Removing Router Key [" + routerKey + "]");
		MetricListener ml = listeners.remove(routerKey);
		if(ml!=null) {
			ml.terminate();
		}		
		
	}
	
	
	/**
	 * Builds the compound topic from the router keys
	 * @return The compound topic
	 */
	protected String buildCompoundTopic() {
		StringBuilder b = new StringBuilder();
		Set<String> keys = routerKeyPatterns.get();
		if(keys!=null && !keys.isEmpty()) {
			for(String key: keys) {
				b.append(key).append(",");
			}
			b.deleteCharAt(b.length()-1);
		}		
		return b.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.builder.RouteBuilder#configure()
	 */
	@Override
	public void configure() throws Exception {
		from("direct:" + routeId)   
		.routeId(routeId)
		.process(new Processor(){
			public void process(Exchange exchange) throws Exception {			
				Message in = exchange.getIn();
				ClosedTrace trace = in.getBody(ClosedTrace.class);
				in.setBody(new HashSet<ClosedTrace>(Arrays.asList(trace)));
				in.setHeader("aggrId", routeId);
				in.setHeader("typeKey", typeKey);
			}
		})
		.aggregate(header("aggrId"), new TraceAggregationStrategy<ClosedTrace>())
			.completionSize(completionSize)
			.completionTimeout(completionTimeout)
		.marshal(outputProcessor.getOutputFormat().getBeanName())
		.process(outputProcessor)		
		.setId(routeId + "-OutputProcessor" );
        
		
		endpoint = this.endpoint("direct:" + routeId);
		log.info("Created Processor [" + routeId + "]");
		
	}
	
	

	
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("ClosedMetricSubscriberFeed [")
	        .append(TAB).append("route = ").append(this.route)
	        .append(TAB).append("sessionId = ").append(this.sessionId)
	        .append(TAB).append("typeKey = ").append(this.typeKey)
	        .append(TAB).append("outputProcessor = ").append(this.outputProcessor)
	        .append(TAB).append("createdTimestamp = ").append(this.createdTimestamp)
	        .append(TAB).append("completionSize = ").append(this.completionSize)
	        .append(TAB).append("completionTimeout = ").append(this.completionTimeout)
	        .append("\n]");    
	    return retValue.toString();
	}

	/**
	 * ClosedMetricSubscriberFeed pre-termination cleanup.
	 */
	@Override
	public void preTerminationCleanup() {
		log.info("Terminating ClosedMetricSubscriber for [" + sessionId + "]");
		for(MetricListener listener: listeners.values()) {
			try { listener.terminate(); } catch (Exception e) {}
		}
		listeners.clear();		
	}
	
	/**
	 * ClosedMetricSubscriberFeed post-termination cleanup.
	 */
	@Override
	public void postTerminationCleanup() {
		
	}









}
