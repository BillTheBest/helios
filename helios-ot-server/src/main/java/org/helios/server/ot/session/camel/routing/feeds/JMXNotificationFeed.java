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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.helios.helpers.JMXHelper;
import org.helios.server.ot.session.camel.routing.AbstractSubscriberRoute;
import org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor;
import org.helios.server.ot.session.camel.routing.annotations.SubRoute;
import org.helios.server.ot.session.camel.routing.annotations.SubRouteConfig;
import org.helios.server.ot.session.camel.routing.http.TraceAggregationStrategy;

/**
 * <p>Title: JMXNotificationFeed</p>
 * <p>Description: A subscriber feed to subscribe to JMX notifications and route the items to the target output processor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.feeds.JMXNotificationFeed</code></p>
 * @param <T> The router key type
 */

@SubRoute(typeKey="jmx-feed", configuration={
		@SubRouteConfig(name="domain", type=String.class),
		@SubRouteConfig(name="keyProperties", type=Map.class),
		@SubRouteConfig(name="completionSize", type=int.class, defaultValue="10"),
		@SubRouteConfig(name="completionTimeout", type=long.class, defaultValue="5000")
})
public class JMXNotificationFeed<T> extends AbstractSubscriberRoute<ObjectName> {
	/** A map of notification listeners keyed by the ObjectName they're listening on notifications for */
	protected final Map<ObjectName, NotificationListener> listeners = new ConcurrentHashMap<ObjectName, NotificationListener>();
	
	/**
	 * Creates a new JMXNotificationFeed
	 * @param outputProcessor The end point for items routed by this subscriber route
	 * @param sessionId The session ID that this subscriber route is owned by
	 */
	public JMXNotificationFeed(SubscriptionOutputProcessor<?> outputProcessor, String sessionId) {
		super(outputProcessor, sessionId);
	}
	
	
	/**
	 * Adds a router key representing a unique resource the caller wants to subscribed to. 
	 * @param routerKey The router key to add a subscription for
	 * @param subFeedKey The subFeedKey designated to represent this subscription
	 * @param properties Additional optional properties for the router key
	 */	
	@Override
	protected void addRouterKey(ObjectName routerKey, String subFeedKey, Map<String, String> properties) {
		if(routerKey==null) throw new IllegalArgumentException("The passed routerKey was null", new Throwable());
		log.info("Adding router key [" + routerKey + "] with subFeedKey [" + subFeedKey + "]");
		String notificationType = properties.get("notification");
		newNotificationListener(routerKey, subFeedKey, notificationType); 
	}
	
	/**
	 * Removes a router key representing a unique resource the caller is subscribed to. 
	 * @param routerKey The router key to remove the subscription for
	 */
	@Override
	protected void removeRouterKey(ObjectName routerKey) {
		if(routerKey==null) throw new IllegalArgumentException("The passed router key was null", new Throwable());
		NotificationListener listener = listeners.remove(routerKey);		
		if(listener!=null) {
			JMXHelper.getRuntimeHeliosMBeanServer().removeNotificationListener(routerKey, listener);
		}

	}
	
	/**
	 * Returns a native router key for the passed properties
	 * @param properties The properties that a subscriber route can determine a router key from
	 * @return a router key or null if one could not be built from the passed properties.
	 */
	@Override
	public ObjectName extractRouterKey(Map<String, String> properties) {
		if(properties==null) throw new IllegalArgumentException("The passed properties was null", new Throwable());
		String objectNameStr = properties.get(HEADER_SUB_ROUTER_KEY);
		if(objectNameStr != null) {
			return JMXHelper.objectName(objectNameStr);
		}
		return null;
	}

	
	
	/**
	 * Creates a new notification listener for the passed ObjectName pattern and <ol>
	 * 		<li>Adds the object name and created listener to the listeners map</li>
	 * 		<li>Adds the ObjectName string to the patterns set</li>
	 * 		<li>Registers the created listener with the Helios MBeanServer.</li>
	 * </ol>
	 * @param objectName the ObjectName pattern to subscribe to
	 * @param feedSubKey The feed subkey to add as the handback object
	 * @param notificationType The optional notification type to filter on
	 * @return a JMX notification listener
	 */
	protected NotificationListener newNotificationListener(final ObjectName objectName, final String feedSubKey, final String notificationType) {
		if(objectName==null) throw new IllegalArgumentException("The passed objectName was null", new Throwable());
		NotificationListener listener =  new NotificationListener() {
			final Set<String> matches = new CopyOnWriteArraySet<String>();
			final Set<String> nonMatches = new CopyOnWriteArraySet<String>();
			public void handleNotification(Notification notification, Object handback) {
				if(notificationType!=null) {
					
					String notifType = notification.getType();
					if(nonMatches.contains(notifType)) return;
					if(!matches.contains(notifType)) {
						if(!notificationTypeMatch(notification, notificationType)) {
							nonMatches.add(notifType);
							return;
						}
						matches.add(notifType);
					}
				}
				Object source = notification.getSource();
				if(source instanceof ObjectName) {
					notification.setSource(source.toString());
				}
				producer.sendBodyAndHeader(endpoint, notification, HEADER_SUB_FEED_KEY, feedSubKey); 
			}
		};
		listeners.put(objectName, listener);
		
		JMXHelper.getRuntimeHeliosMBeanServer().addNotificationListener(objectName, listener, null, feedSubKey);
		return listener;
	}
	
	/**
	 * Determines if the passed notification matches the passed notification type string
	 * @param notification The notification to test
	 * @param notificationType The notification type string
	 * @return true for a match, false otherwise
	 */
	protected boolean notificationTypeMatch(Notification notification, String notificationType) {
		if(notificationType==null || notificationType.trim().equals("")) return true;
		ObjectName ref = JMXHelper.objectName(notification.getType() + ":foo=bar");
		ObjectName pattern = JMXHelper.objectName(notificationType + ":foo=bar");
		return pattern.apply(ref);
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
				Notification n = in.getBody(Notification.class);
				HashSet<Notification> set = new HashSet<Notification>();
				set.add(n);
				in.setBody(set);				
				//in.setHeader("typeKey", typeKey);  // Now set by the notification sender
				in.setHeader("aggrId", routeId);
				in.setHeader("typeKey", typeKey);
			}
		})
		.aggregate(header("aggrId"), new TraceAggregationStrategy<Notification>())
			.completionSize(completionSize)
			.completionTimeout(completionTimeout)

		.marshal(outputProcessor.getOutputFormat().getBeanName())
		.process(outputProcessor)		
		.setId(routeId + "-OutputProcessor" );
		log.info("Created Processor [" + routeId + "]");
		endpoint = this.endpoint("direct:" + routeId);
		
	}
	
	
	/**
	 * JMXNotificationFeed pre-termination cleanup.
	 */
	@Override
	public void preTerminationCleanup() {
		for(Entry<ObjectName, NotificationListener> jmxSub: listeners.entrySet()) {
			JMXHelper.getRuntimeHeliosMBeanServer().removeNotificationListener(jmxSub.getKey(), jmxSub.getValue());
		}
		listeners.clear();		
	}
	
	/**
	 * JMXNotificationFeed post-termination cleanup.
	 */
	@Override
	public void postTerminationCleanup() {		
		producer = null;
		endpoint = null;		
	}
	
	
//	/**		
//	 * Sets the Camel Context and acquires a producer template
//	 * @param camelContext the camelContext to set
//	 */
//	@Override
//	public void setCamelContext(CamelContext camelContext) {
//		super.setCamelContext(camelContext);
//		producer = camelContext.createProducerTemplate();		
//	}


}