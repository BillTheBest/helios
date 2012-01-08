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

import org.apache.camel.Route;
import org.apache.camel.RoutesBuilder;
import org.helios.server.ot.session.SessionSubscriptionTerminator;

/**
 * <p>Title: ISubscriberRoute</p>
 * <p>Description: Defines a wrapped camel route, created and started to route content from helios resources out to a subscribing client's output processor.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.ISubscriberRoute</code></p>
 * @param <T> The type of router key
 */

public interface ISubscriberRoute<T> extends RoutesBuilder, SessionSubscriptionTerminator {
	
	/** The exchange header representing the feed sub key */
	public static final String 	HEADER_TYPE_KEY = "typeKey";
	/** The exchange header representing the aggregation ID */
	public static final String 	HEADER_AGGRD_ID = "aggrId";
	/** The properties key for the router's appended router keys */
	public static final String 	HEADER_ROUTER_KEYS = "routerKeys";
	/** The properties key for the property added to the router's appended router keys to indicate the feed sub key of the just requested router key */
	public static final String 	HEADER_JUST_ADDED_FEED_KEY = "requestedSubFeedKey";
	/** The properties key for the subFeed request's router key */
	public static final String HEADER_SUB_ROUTER_KEY = "routerKey";
	/** The properties key for the subscriber sub feed key */
	public static final String HEADER_SUB_FEED_KEY = "subFeedKey";

	/**
	 * Returns the underlying camel route
	 * @return a camel route
	 */
	public Route getRoute();
	
	/**
	 * Sets the camel route of this subscriber route once it has been injected into the camel context. 
	 * @param route the route to set
	 */
	public void setRoute(Route route);	
	
	/**
	 * Returns The session ID that this subscriber route is owned by
	 * @return the sessionId
	 */
	public String getSessionId();

	/**
	 * Returns the subscriber route type the uniquely identifies the type of items being sent to the output processor
	 * @return the typeKey
	 */
	public String getTypeKey();

	/**
	 * Returns the subscriber route's starting timestamp
	 * @return the createdTimestamp
	 */
	public long getCreatedTimestamp();
	
	/**
	 * Returns the end point for items routed by this subscriber route 
	 * @return the outputProcessor
	 */
	public SubscriptionOutputProcessor<?> getOutputProcessor();
	
	/**
	 * Returns the completion size of the aggregating strategy
	 * @return the completionSize
	 */
	public int getCompletionSize();

	/**
	 * Sets the completion size of the aggregating strategy
	 * @param completionSize the completionSize to set
	 */
	public void setCompletionSize(int completionSize);

	/**
	 * Returns the completion timeout of the aggregating strategy
	 * @return the completionTimeout
	 */
	public long getCompletionTimeout();

	/**
	 * Sets the completion timeout of the aggregating strategy
	 * @param completionTimeout the completionTimeout to set
	 */
	public void setCompletionTimeout(long completionTimeout);
	
	/**
	 * Returns the route id defined as the type key plus the sessionId 
	 * @return the routeId
	 */
	public String getRouteId();	
	
	/**
	 * Returns a map of the subscriber route configuration and runtime properties.
	 * @return a map of the subscriber route configuration and runtime properties.
	 */
	public Map<String, Object> getProperties();
	
	/**
	 * Adds a router key representing a unique resource the caller is subscribing to 
	 * @param properties The caller supplied properties from which the subscriberroute implementation can determine the routerKey
	 * @return A map of subscriberroute properties that should include the resulting subFeedKey keyed by {@literal HEADER_JUST_ADDED_FEED_KEY} 
	 */
	public Map<String, Object> addRouterKey(Map<String, String> properties);
	
	/**
	 * Removes a router key representing a unique resource the caller is subscribed to. 
	 * @param properties The caller supplied properties from which the subscriberroute implementation can determine the routerKey or subFeedKey
	 * @return true of there are remaining router keys, false if not.
	 */
	public boolean removeRouterKey(Map<String, String> properties); 
	
	/**
	 * Returns a native router key for the passed properties
	 * @param properties The properties that a subscriber route can determine a router key from
	 * @return a router key
	 */
	public T extractRouterKey(Map<String, String> properties);
	
	
//	/**
//	 * Directs the subscriber route instance to rebuild the route considering the passed key removed.
//	 * i.e. what to do when the last subscriber unregisters interest in {@code subFeedKey}.
//	 * @param routerKey The routerKey key which just lost its last interested subscriber
//	 */
//	public void rebuildSubscriberRouteWithout(T routerKey); 
//	
//	/**
//	 * Directs the subscriber route instance to rebuild the route considering the new passed key added.
//	 * i.e. what to do when a subscriber registers interest in a new router key 
//	 * @param routerKey The router key which is new and unique to this subscriber route
//	 * @param feedSubKey The generated feed sub key that identifies the new subscription
//	 */
//	public void rebuildSubscriberRouteWith(T routerKey, String feedSubKey);
		
	
//	/**
//	 * Adds a new sub feed request to the subscriber route
//	 * @param subFeedProperties The properties of the new subfeed request which should contain a router key
//	 * @return The feed subkey generated by the subscrier route to uniquely identify the underying router key for this subscription request.
//	 * That is to say, if the passed router key is already registered with the subscriber route, the generated key for that router key wil be returned.
//	 */
//	public String addSubFeed(Map<String, String> subFeedProperties);
//	
//	/**
//	 * Removes the router key associated with the passed sub feed key, effectively cancelling the associated subscription.
//	 * @param subFeedKey The sub feed key designated to the subscription resource to cancel.
//	 */
//	public void removeSubFeed(String subFeedKey);
}
