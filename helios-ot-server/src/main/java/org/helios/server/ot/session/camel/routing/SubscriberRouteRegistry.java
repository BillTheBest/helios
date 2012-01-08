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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Route;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: SubscriberRouteRegistry</p>
 * <p>Description: A registry for SubscriberRoute types and factory for SubscriberRoute instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.SubscriberRouteRegistry</code></p>
 */
//@ManagedResource(
//		objectName="org.helios.server.ot.session.camel.routing:service=SubscriberRouteRegistry",
//		description="A registry for SubscriberRoute types and factory for SubscriberRoute instances"
//)
public class SubscriberRouteRegistry implements InitializingBean, ApplicationContextAware, CamelContextAware, SmartApplicationListener {
	/** The Spring application context */
	protected ApplicationContext applicationContext;
	/** The camel helios context */
	protected CamelContext camelContext;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** A map of SubscriberRoute prototype descriptors keyed by the type name */
	protected final Map<String, Descriptor> routeDescriptors = new ConcurrentHashMap<String, Descriptor>();
	/** A map of SubscriberRoutes keyed by route type in a map keyed by sessionId  */
	protected final Map<String, Map<String, ISubscriberRoute<?>>> routes = new ConcurrentHashMap<String, Map<String, ISubscriberRoute<?>>>();
	
	
	/**
	 * Returns the registered SubscriberRoute type keys
	 * @return an aray of SubscriberRoute type keys
	 */
	@ManagedAttribute(description="The registered SubscriberRoute type keys")
	public String[] getSubscriberRouteTypeKeys() {
		return routeDescriptors.keySet().toArray(new String[routeDescriptors.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		registerSubscriberRouters(applicationContext);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent appEvent) {
		if(appEvent instanceof ContextRefreshedEvent) {
			ContextRefreshedEvent appContextRefreshedEvent = (ContextRefreshedEvent)appEvent;
			registerSubscriberRouters(appContextRefreshedEvent.getApplicationContext());
		}
	}
	
	/**
	 * Looks for instances of ISubscriberRouters in the passed app context that are not already registered and registers them.
	 * @param context The app context to search
	 */
	protected void registerSubscriberRouters(ApplicationContext context) {
		if(!(context instanceof GenericApplicationContext)) return;
		GenericApplicationContext appCtx = (GenericApplicationContext)context; 
		for(Map.Entry<String, ISubscriberRoute> routeEntry : context.getBeansOfType(ISubscriberRoute.class).entrySet()) {			
			String beanName = routeEntry.getKey();
			if(!context.isPrototype(beanName)) continue;
			ISubscriberRoute route = routeEntry.getValue();
			String typeName = route.getTypeKey();
			if(!routeDescriptors.containsKey(typeName)) {
				synchronized(routeDescriptors) {
					if(!routeDescriptors.containsKey(typeName)) {
						Descriptor desc = new Descriptor(typeName, beanName, appCtx);
						routeDescriptors.put(typeName, desc);
					}
				}
			}
			
		}
	}
	
	/**
	 * Creates a new ISubscriberRoute instance
	 * @param typeName The route type name
	 * @param sessionId The session Id
	 * @param outputProcessor The session's output processor
	 * @param properties Optional properties to be applied to the prototype instance
	 * @return the router's properties 
	 * @throws Exception thrown on any exception
	 */
	public Map<String, Object> getInstance(String typeName, String sessionId, SubscriptionOutputProcessor<?> outputProcessor, Map<String, String> properties) throws Exception {
		if(typeName==null) throw new IllegalArgumentException("The passed type name was null", new Throwable());
		if(!routeDescriptors.containsKey(typeName)) throw new IllegalArgumentException("The passed type name [" + typeName + "] is not a registered route prototype", new Throwable());
		if(sessionId==null) throw new IllegalArgumentException("The passed sessionId was null", new Throwable());
		if(outputProcessor==null) throw new IllegalArgumentException("The passed outputProcessor was null", new Throwable());		
		ISubscriberRoute<?> route = getSessionRouter(sessionId, typeName);
		if(route==null) {
			synchronized(routes) {
				route = getSessionRouter(sessionId, typeName);
				if(route==null) {
					Descriptor desc = routeDescriptors.get(typeName);		
					route = (ISubscriberRoute)desc.getApplicationContext().getBean(desc.getBeanName(), outputProcessor, sessionId);
					getSessionRouters(sessionId).put(typeName, route);
					camelContext.addRoutes(route);
					Route camelRoute = camelContext.getRoute(route.getRouteId());
					route.setRoute(camelRoute);					
				}
			}
		}
		return route.addRouterKey(properties);
	}
	
	/**
	 * Decrements the number of subscribers for the subFeed or routerKey identified by the passed properties.
	 * @param typeName The subscriber route type key
	 * @param sessionId The subscriber session id
	 * @param properties The subscriber provided properties that the subscriber route can determine the router key or sub feed key
	 */
	public void terminateSubFeed(String typeName, String sessionId, Map<String, String> properties) {
		ISubscriberRoute<?> route = getSessionRouter(sessionId, typeName);
		log.info("Stopping SubKey\n\tType [" + typeName + "] \n\tSession [" + sessionId + "]\n\tSubKey [" + properties.get(ISubscriberRoute.HEADER_SUB_FEED_KEY) + "]");
		if(route!=null) {
			if(!route.removeRouterKey(properties)) {
				// this means that no router keys remain, so the router can be stopped.
			}
		}
		
	}
	
	/**
	 * Returns the subscriber route for the passed session id and router type key
	 * @param sessionId The session Id
	 * @param routerTypeKey The router type key
	 * @return An ISubscriberRoute or null if one was not found
	 */
	protected ISubscriberRoute getSessionRouter(String sessionId, String routerTypeKey) {
		return getSessionRouters(sessionId).get(routerTypeKey);
	}
	
	/**
	 * Retrieves the route map for the passed session Id, creating an empty one if necessary.
	 * @param sessionId The session id to get the route map for
	 * @return a session route map
	 */
	protected Map<String, ISubscriberRoute<?>> getSessionRouters(String sessionId) {
		Map<String, ISubscriberRoute<?>> routeMap = routes.get(sessionId);
		if(routeMap==null) {
			synchronized(routes) {
				routeMap = routes.get(sessionId);
				if(routeMap==null) {
					routeMap = new ConcurrentHashMap<String, ISubscriberRoute<?>>();
					routes.put(sessionId, routeMap);
				}
			}
		}
		return routeMap;
	}
	
	/**
	 * Terminates and unregisters all routers associated to the passed sessionId.
	 * @param sessionId The session id of the session that is terminating
	 */
	public void terminateSession(String sessionId) {
		if(sessionId==null) throw new IllegalArgumentException("The passed sessionId was null", new Throwable());
		log.info("Terminating Session [" + sessionId + "]");
		Map<String, ISubscriberRoute<?>> sessionRoutes = routes.get(sessionId);
		if(sessionRoutes!=null) {
			for(ISubscriberRoute<?> route: sessionRoutes.values()) {
				route.terminate();
			}
			sessionRoutes.clear();
			routes.remove(sessionId);
		}
	}
	
//	/**
//	 * Terminates one instance of a subscription in a session's router for the passed router key. If the number of subscribers for
//	 * the typ
//	 * @param sessionId The session id we're terminating the router for
//	 * @param typeKey The type key of the router
//	 */
//	public void terminateRouter(String sessionId, Object routerKey) {
//		if(sessionId==null) throw new IllegalArgumentException("The passed sessionId was null", new Throwable());
//		if(typeKey==null) throw new IllegalArgumentException("The passed typeKey was null", new Throwable());
//		log.info("Terminating Route [" + routerKey + "] for session [" + sessionId + "]");
//		Map<String, ISubscriberRoute> sessionRoutes = routes.get(sessionId);
//		if(sessionRoutes!=null) {
//			ISubscriberRoute route = sessionRoutes.remove(typeKey);
//			if(route!=null) {
//				route.terminate();
//			}
//		}
//	}
	
//	/**
//	 * Terminates a subfeed in a subscriber router.
//	 * @param sessionId The session id we're terminating the sub feed for
//	 * @param typeKey The type key of the router
//	 * @param subKey The sub feed identifier which uniquely identifies the sub feed to terminat
//	 */
//	public void terminateSubFeed(String sessionId, String typeKey, String subKey) {
//		if(sessionId==null) throw new IllegalArgumentException("The passed sessionId was null", new Throwable());
//		if(typeKey==null) throw new IllegalArgumentException("The passed typeKey was null", new Throwable());
//		if(subKey==null) throw new IllegalArgumentException("The passed subKey was null", new Throwable());
//		log.info("Terminating SubFeed [" + subKey + "] in Route [" + typeKey + "] for session [" + sessionId + "]");
//		Map<String, ISubscriberRoute> sessionRoutes = routes.get(sessionId);
//		if(sessionRoutes!=null) {
//			ISubscriberRoute route = sessionRoutes.remove(typeKey);
//			if(route!=null) {				
//				// remove subkey
//			}
//		}
//	}
	
	/**
	 * {@inheritDoc}
	 * <p>Returns true if the application event is assignable to a {@link ContextRefreshedEvent}.
	 * @see org.springframework.context.event.SmartApplicationListener#supportsEventType(java.lang.Class)
	 */
	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> clazz) {
		return clazz==null ? false : ContextRefreshedEvent.class.isAssignableFrom(clazz);
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.event.SmartApplicationListener#supportsSourceType(java.lang.Class)
	 */
	@Override
	public boolean supportsSourceType(Class<?> clazz) {
		return true;
	}


	/**
	 * {@inheritDoc}
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	@Override
	public int getOrder() {
		return 0;
	}
		
	
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		((AbstractApplicationContext)this.applicationContext).addApplicationListener(this);
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
	 * <p>Title: Descriptor</p>
	 * <p>Description: Wraps a SubscriberRoute entry meta-data set</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.camel.routing.SubscriberRouteRegistry.Descriptor</code></p>
	 */
	public static class Descriptor {
		private final String name;
		private final String beanName;
		private final String[] properties;
		private final GenericApplicationContext appContext;
		
		/**
		 * Creates a new Descriptor
		 * @param name The type name of the sub route
		 * @param beanName The bean name of the prototype
		 * @param appContext The application context containing the bean
		 */
		public Descriptor(String name, String beanName, GenericApplicationContext appContext) {
			this.beanName = beanName;
			this.name = name;
			this.appContext = appContext;
			Set<String> propNames = new HashSet<String>();
			BeanDefinition beanDef = appContext.getBeanDefinition(this.beanName);
			for(PropertyValue propVal: beanDef.getPropertyValues().getPropertyValueList()) {
				propNames.add(propVal.getName());
			}
			this.properties = propNames.toArray(new String[propNames.size()]);
		}
		
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the beanName
		 */
		public String getBeanName() {
			return beanName;
		}
		/**
		 * @return the properties
		 */
		public String[] getProperties() {
			return properties;
		}

		/**
		 * @return the appContext
		 */
		public GenericApplicationContext getApplicationContext() {
			return appContext;
		}

		
	}


}
