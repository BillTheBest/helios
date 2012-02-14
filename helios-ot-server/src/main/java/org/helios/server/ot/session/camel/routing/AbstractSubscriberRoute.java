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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ObjectName;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.server.ot.session.SessionSubscriptionTerminator;
import org.helios.server.ot.session.camel.routing.annotations.SubRoute;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: AbstractSubscriberRoute</p>
 * <p>Description: An abstract base class for ISubscriberRoute concrete implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.AbstractSubscriberRoute</code></p>
 * @param <T> The type of the subscriber route's unique key per session 
 */
@JMXManagedObject(annotated=true, declared=false)
public abstract class AbstractSubscriberRoute<T> extends RouteBuilder implements SessionSubscriptionTerminator, ISubscriberRoute<T>, CamelContextAware {
	/** The camel route */
	protected Route route;
	/** The session ID that this subscriber route is owned by */
	protected final String sessionId;
	/** The subscriber route type the uniquely identifies the type of items being sent to the output processor */
	protected final String typeKey;
	/** The end point for items routed by this subscriber route */
	protected final SubscriptionOutputProcessor<?> outputProcessor;
	/** The injected Camel Context */
	protected CamelContext camelContext = null;
	/** Instance logger */
	protected final Logger log;
	/** The subscriber route's starting timestamp */
	protected final long createdTimestamp;
	/** A map of active feed sub feed IDs keyed by the router key */
	protected final Map<T, SubscribedFeedSubKey> subscribedKeys = new ConcurrentHashMap<T, SubscribedFeedSubKey>();
	/** A map of active feed router keys keyed by the sub feed key */
	protected final Map<String, T> subFeedKeys = new ConcurrentHashMap<String, T>();
	/** A string set of the subscribed ObjectName patterns */
	protected final AtomicReference<Set<String>> routerKeyPatterns = new AtomicReference<Set<String>>(new CopyOnWriteArraySet<String>());
	/** The direct endpoint to send notifications to */
	protected Endpoint endpoint = null;	
	/** The producer template to inject exchanges */	
	protected ProducerTemplate producer = null;
	/** The size of the producer template cache. If < 0, uses the default */
	protected int producerTemplateCacheSize = -1;
	/** The MODB Container */
	protected final ManagedObjectDynamicMBean modb = new ManagedObjectDynamicMBean(getClass().getSimpleName());

	/** The router registry that manages the creation and cleanup of created routes */
	@Autowired(required=true)
	protected SubscriberRouteRegistry routeRegistry;
	
	/** The completion size of the aggregating strategy. Defaults to 10. */
	protected int completionSize = 10;
	/** The completion timeout of the aggregating strategy. Defaults to 5000. */
	protected long completionTimeout = 5000;
	/** Serial number factory for feed instances */
	private final AtomicLong feedSerial = new AtomicLong(0L);
	
	/** Counter for the number of event emitted from this route */
	protected final AtomicLong eventCount = new AtomicLong(0L);
	
	
	/**
	 * Returns the number of events emitted from this router
	 * @return the number of events emitted from this router
	 */
	@JMXAttribute(name="EventCount", description="The number of events emitted from this router", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getEventCount() {
		return eventCount.get();
	}





	/** Serial number factory for route ids */
	protected static final AtomicLong routeSerial = new AtomicLong(0L);
	
	/** The route id defined as the type key plus the sessionId */
	protected final String routeId;
	/** This router's JMX ObjectName */
	protected final ObjectName objectName;
	
	/** The JMX ObjectName template for this subscriber route */
	public static final String OBJECT_NAME_TEMPLATE = "org.helios.server.ot.routing.subscribers:service=%s,session=%s";


	/**
	 * Removes a router key representing a unique resource the caller is subscribed to. 
	 * @param routerKey The router key to remove the subscription for
	 */	
	protected abstract void removeRouterKey(T routerKey);
	
	/**
	 * Adds a router key representing a unique resource the caller wants to subscribed to. 
	 * @param routerKey The router key to add a subscription for
	 * @param subFeedKey The subFeedKey designated to represent this subscription
	 * @param properties Additional optional properties for the router key 
	 */	
	protected abstract void addRouterKey(T routerKey, String subFeedKey, Map<String, String> properties);	
	
	
	/**
	 * Returns a native router key for the passed properties
	 * @param properties The properties that a subscriber route can determine a router key from
	 * @return a router key or null if one could not be built from the passed properties.
	 */
	public abstract T extractRouterKey(Map<String, String> properties);
	

	/**
	 * Subscriber route implementation specific pre-termination cleanup.
	 */
	public abstract void preTerminationCleanup();
	
	/**
	 * Subscriber route implementation specific post-termination cleanup.
	 */
	public abstract void postTerminationCleanup();
	

	/**
	 * {@inheritDoc}
	 * <p>Called when the session terminates or when the number of subscribed router keys drops to zero.
	 * @see org.helios.server.ot.session.SessionSubscriptionTerminator#terminate()
	 */
	@Override	
	@JMXOperation(name="terminate", description="Terminate this subscriber route" )
	public void terminate() {
		log.info("Terminating Subscriber Route [" + typeKey + "] for session [" + sessionId + "]");
		preTerminationCleanup();
		try { 
			endpoint.stop();
		} catch (Exception e) {
			log.warn("Failed to stop Endpoint [" + endpoint.getEndpointUri() + "] for [" + typeKey + "] for session [" + sessionId + "]");
		}
		
		try { 
			camelContext.stopRoute(routeId);
		} catch (Exception e) {
			log.warn("Failed to stop Subscriber Route [" + typeKey + "] for session [" + sessionId + "]");
		}
		try { 
			camelContext.removeRoute(routeId);
		} catch (Exception e) {
			log.warn("Failed to remove Subscriber Route [" + typeKey + "] for session [" + sessionId + "]");
		}
		routerKeyPatterns.set(null);
		postTerminationCleanup();
		subFeedKeys.clear();
		subscribedKeys.clear();
		JMXHelper.getRuntimeHeliosMBeanServer().unregisterMBean(objectName);
	}	


	/**
	 * Adds a router key representing a unique resource the caller is subscribing to 
	 * @param properties The caller supplied properties from which the subscriberroute implementation can determine the routerKey
	 * @return A map of subscriberroute properties that should include the resulting subFeedKey keyed by {@literal HEADER_JUST_ADDED_FEED_KEY} 
	 */
	@JMXOperation(name="addRouterKey", description="Adds a router key representing a unique resource the caller is subscribing to" )
	public Map<String, Object> addRouterKey(@JMXParameter(name="properties", description="A map of key/values defining a router key") Map<String, String> properties) {
		if(properties==null) throw new IllegalArgumentException("The passed properties was null", new Throwable());
		T routerKey = extractRouterKey(properties);
		if(routerKey==null) {
			throw new IllegalArgumentException("Failed to determine router key from passed properties [" + properties + "]", new Throwable());
		}		
		String subFeedKey = addSubscriptionRouterKey(routerKey, properties);
		Map<String, Object> retProps = getProperties();
		retProps.put(HEADER_JUST_ADDED_FEED_KEY, subFeedKey);
		log.info("Added SubscriptionRouterKey RouterKey:[" + routerKey +"] SubFeedKey:[" + subFeedKey + "]");
		return retProps;
	}
	
	/**
	 * Removes a sub feed key one instance of a subscription to a unique resource the caller is subscribed to. 
	 * @param properties The caller supplied properties from which the subscriberroute implementation can determine the routerKey or subFeedKey
	 * @return true of there are remaining router keys, false if not.
	 */
	@JMXOperation(name="removeRouterKey", description="Removes a sub feed key one instance of a subscription to a unique resource the caller is subscribed to" )
	public boolean removeRouterKey(@JMXParameter(name="properties", description="A map of key/values defining a router key") Map<String, String> properties) {
		if(properties==null) throw new IllegalArgumentException("The passed properties was null", new Throwable()); 
		T routerKey = null;
		String subFeedKey = properties.get(HEADER_SUB_FEED_KEY);
		if(subFeedKey!=null) {
			routerKey = subFeedKeys.get(subFeedKey);
		}
		if(routerKey==null) {
			routerKey = extractRouterKey(properties);
		}
		if(routerKey!=null && subFeedKey!=null) {
			log.info("Removing SubscriptionRouterKey RouterKey:[" + routerKey +"] SubFeedKey:[" + subFeedKey + "]");
			removeSubscriptionRouterKey(routerKey, subFeedKey);
		}
		return !subscribedKeys.isEmpty();
	}
	

	
	/**
	 * Inspects the subscription registry to see if the passed router key is already subscribed to.
	 * If it is, the subFeedKey representing that router key is returned.
	 * If not, the router key is added as a new subscription and the new subFeedKey is returned.
	 * @param routerKey The key that represents the resource the subscriber has subscribed to
	 * @param properties Additional properties for the router Key
	 * @return  the feed sub key representing the subscription to the specified router key
	 * 
	 */
	protected String addSubscriptionRouterKey(T routerKey, Map<String, String> properties) {
		if(routerKey==null) throw new IllegalArgumentException("The passed subFeedKey was null", new Throwable());
		SubscribedFeedSubKey routerKeySubFeed = subscribedKeys.get(routerKey);
		if(routerKeySubFeed==null) {
			synchronized(subscribedKeys) {
				routerKeySubFeed = subscribedKeys.get(routerKey);
				if(routerKeySubFeed==null) {
					routerKeySubFeed = new SubscribedFeedSubKey(routerKey, typeKey);					
					subscribedKeys.put(routerKey, routerKeySubFeed);
					routerKeyPatterns.get().add(routerKey.toString());
				}
			}
		}
		routerKeySubFeed.addSubFeedKey();		
		addRouterKey(routerKey, routerKeySubFeed.getSubFeedKey(), properties);
		return routerKeySubFeed.getSubFeedKey();
	}
	
	
	
	
	/**
	 * Unsubscribes one subscriber from the passed router key.
	 * If this decrements the subscriber count to zero, the router will remove the router key from the subscribed resources.
	 * @param routerKey The router key to decrement one subscriber for
	 * @param subFeedKey The subFeedKey to remove
	 * @return true of there are remaining router keys, false if not.
	 */
	protected boolean removeSubscriptionRouterKey(T routerKey, String subFeedKey) {
		if(routerKey==null) throw new IllegalArgumentException("The passed routerKey was null", new Throwable());
		if(subFeedKey==null) throw new IllegalArgumentException("The passed subFeedKey was null", new Throwable());
		SubscribedFeedSubKey routerKeySubFeed = subscribedKeys.get(routerKey);
		if(routerKeySubFeed!=null) {
			if(!routerKeySubFeed.removeSubFeedKey()) {
				// ================================================
				// SubKey counted down to zero.
				// ================================================
				removeRouterKey(routerKey);
				subFeedKeys.remove(subFeedKey);
				subscribedKeys.remove(routerKey);
				// ================================================
			}
		}
		return !subscribedKeys.isEmpty();
	}
	
	
	
	
	/**
	 * Creates a new AbstractSubscriberRoute
	 * @param outputProcessor The end point for items routed by this subscriber route
	 * @param sessionId The session ID that this subscriber route is owned by
	 * @param typeKey The subscriber route type the uniquely identifies the type of items being sent to the output processor
	 */
	public AbstractSubscriberRoute(SubscriptionOutputProcessor<?> outputProcessor, String sessionId, String typeKey) {
		if(outputProcessor==null) throw new IllegalArgumentException("The passed outputProcessor was null", new Throwable());
		if(sessionId==null) throw new IllegalArgumentException("The passed sessionId was null", new Throwable());		
		if(typeKey==null) {
			String annotatedTypeKey = getClassTypeKey(this); 
			if(annotatedTypeKey==null) {
				throw new IllegalArgumentException("The passed and annotated typeKeys were null", new Throwable());
			}
			this.typeKey = annotatedTypeKey;
		} else {
			this.typeKey = typeKey;
		}
		this.sessionId = sessionId;		
		this.outputProcessor = outputProcessor;
		routeId = String.format("Feed-%s-%s", this.typeKey, this.sessionId);
		log = Logger.getLogger(getClass().getName() + "." + sessionId);
		createdTimestamp = System.currentTimeMillis();
		log.info("Created SubscriberRoute [" + getClass().getName() + "] for session [" + sessionId + "]");
		objectName = JMXHelper.objectName(String.format(OBJECT_NAME_TEMPLATE, getClass().getSimpleName(), sessionId));
		if(!"PROTOTYPE".equals(sessionId)) {
			modb.reflectObject(this);
			JMXHelper.getRuntimeHeliosMBeanServer().registerMBean(modb, objectName);
		}
		
	}
	
	
	
	/**
	 * Creates a new AbstractSubscriberRoute
	 * @param outputProcessor The end point for items routed by this subscriber route
	 * @param sessionId The session ID that this subscriber route is owned by
	 */
	public AbstractSubscriberRoute(SubscriptionOutputProcessor<?> outputProcessor, String sessionId) {
		this(outputProcessor, sessionId, null);
	}
	
	/**
	 * Reads the type key from the class of the passed object
	 * @param obj The object to determine the type key for
	 * @return The type key or null if the class was not annotated with @SubRoute
	 */
	protected static String getClassTypeKey(Object obj) {
		if(obj==null) throw new IllegalArgumentException("The passed object was null", new Throwable());		
		SubRoute subRoute = obj.getClass().getAnnotation(SubRoute.class);
		if(subRoute!=null) return subRoute.typeKey();
		return null;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.routing.ISubscriberRoute#getRoute()
	 */
	public Route getRoute() {		
		return route;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.routing.ISubscriberRoute#getSessionId()
	 */
	@JMXAttribute(name="SessionId", description="The ID of the session that initiated this route", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.routing.ISubscriberRoute#getTypeKey()
	 */
	@JMXAttribute(name="TypeKey", description="The type key of this route", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getTypeKey() {
		return typeKey;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.routing.ISubscriberRoute#getCreatedTimestamp()
	 */
	@JMXAttribute(name="CreatedTimestamp", description="The UDT timestamp that this route was created at", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCreatedTimestamp() {
		return createdTimestamp;
	}
	
	/**
	 * Returns the date that this route was created on
	 * @return the date that this route was created on
	 */
	@JMXAttribute(name="CreatedDate", description="The date that this route was created at", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getCreatedDate() {
		return new Date(createdTimestamp);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.routing.ISubscriberRoute#getOutputProcessor()
	 */
	public SubscriptionOutputProcessor<?> getOutputProcessor() {
		return outputProcessor;
	}

	/**
	 * Returns the completion size of the aggregating strategy
	 * @return the completionSize
	 */
	@JMXAttribute(name="CompletionSize", description="The completion size of the aggregating strategy", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getCompletionSize() {
		return completionSize;
	}

	/**
	 * Sets the completion size of the aggregating strategy
	 * @param completionSize the completionSize to set
	 */
	public void setCompletionSize(int completionSize) {
		this.completionSize = completionSize;
	}

	/**
	 * Returns the completion timeout of the aggregating strategy
	 * @return the completionTimeout
	 */
	@JMXAttribute(name="CompletionTimeout", description="The completion timeout in ms. of the aggregating strategy", mutability=AttributeMutabilityOption.READ_WRITE)
	public long getCompletionTimeout() {
		return completionTimeout;
	}

	/**
	 * Sets the completion timeout of the aggregating strategy
	 * @param completionTimeout the completionTimeout to set
	 */
	public void setCompletionTimeout(long completionTimeout) {
		this.completionTimeout = completionTimeout;
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("AbstractSubscriberRoute [")
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
	 * Returns a map of the subscriber route configuration and runtime properties.
	 * @return a map of the subscriber route configuration and runtime properties.
	 */
	public Map<String, Object> getProperties() {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("type", typeKey);
		props.put("routeId", routeId);
		props.put("completionSize", completionSize);
		props.put("completionTimeout", completionTimeout);
		Set<String> keys = routerKeyPatterns.get();
		if(keys!=null) {
			props.put(HEADER_ROUTER_KEYS, keys.toArray(new String[keys.size()]));
		}
		return props;
	}
	
	

	/**
	 * Sets the camel route of this subscriber route once it has been injected into the camel context. 
	 * @param route the route to set
	 */
	public void setRoute(Route route) {
		this.route = route;
	}

	/**
	 * Returns the route id defined as the type key plus the sessionId 
	 * @return the routeId
	 */
	@JMXAttribute(name="RouteId", description="The ID of the underlying Camel Route", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getRouteId() {
		return routeId;
	}

	/**
	 * Returns the Camel Context
	 * @return the camelContext
	 */
	public CamelContext getCamelContext() {
		return camelContext;
	}

	/**
	 * Sets the Camel Context
	 * @param camelContext the camelContext to set
	 */
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
		if(producerTemplateCacheSize>0) {
			producer = this.camelContext.createProducerTemplate(producerTemplateCacheSize);
		} else {
			producer = this.camelContext.createProducerTemplate();
		}
		
	}
	
	
	
	/**
	 * Sets size of the producer template cache. If < 0, uses the default
	 * @param size The size of the producer template cache
	 */
	@JMXAttribute(name="ProducerTemplateCacheSize", description="The size of the producer template cache.", mutability=AttributeMutabilityOption.READ_WRITE)
	public void setProducerTemplateCacheSize(int size) {
		producerTemplateCacheSize = size;
	}
	
	/**
	 * Returns the configured size of the producer template cache.
	 * @return the configured size of the producer template cache
	 */
	@JMXAttribute(name="ProducerTemplateCacheSize", description="The configured size of the producer template cache.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getProducerTemplateCacheSize() {
		return producerTemplateCacheSize;
	}
	
	
	
	
	
	/**
	 * <p>Title: SubscribedFeedSubKey</p>
	 * <p>Description: Represents a feed sub key and the number of subscribers registered to it.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.camel.routing.AbstractSubscriberRoute.SubscribedFeedSubKey</code></p>
	 */
	public class SubscribedFeedSubKey {
		/** The subFeedKeys of the subscriber listening on this sub-key */
		private final String subFeedKey;
		/** The router key the subscribers registered for */
		private final T routerKey;
		/** The subKey Prefix */
		private final String prefix;
		/** The sub feed key subscriber count */
		private AtomicLong subscriberCount = new AtomicLong(0);
		
		/**
		 * Creates a new SubscribedFeedSubKey for the passed router Key
		 * @param routerKey The router key the subscribers registered for
		 * @param prefix The subFeedKey prefix
		 */
		public SubscribedFeedSubKey(T routerKey, String prefix) {
			this.routerKey = routerKey;	
			this.prefix = prefix;
			this.subFeedKey = this.prefix + "-" + feedSerial.incrementAndGet();
			subFeedKeys.put(subFeedKey, routerKey);
		}
		
		/**
		 * Adds a subFeedkey indicating that a component of the subscriber has requested a feed subscription on this routerKey 
		 * @return the number of subscribers against this key after adding this instance
		 */
		protected long addSubFeedKey() {			
			return subscriberCount.incrementAndGet();
		}
		
		/**
		 * Removes a subFeedkey indicating that a component of the subscriber has terminated a feed subscription on this routerKey
		 * @return true if there are still more than zero subFeedKey subscribers left, false otherwise (and the router key should be terminated)
		 */
		protected boolean removeSubFeedKey() {
			long subscriberCnt = subscriberCount.decrementAndGet();
			return subscriberCnt>0;
		}
		


		/**
		 * Returns the number of subFeedKeys for this router key
		 * @return the subscriber count
		 */
		public long getSubscriberCount() {
			return this.subscriberCount.get();
		}

		/**
		 * Returns the router key the subscribers registered for
		 * @return the routerKey
		 */
		public T getRouterKey() {
			return routerKey;
		}

		/**
		 * Returns the subFeedKey
		 * @return the subFeedKey
		 */
		public String getSubFeedKey() {
			return subFeedKey;
		}
	
	}
	
	

}
