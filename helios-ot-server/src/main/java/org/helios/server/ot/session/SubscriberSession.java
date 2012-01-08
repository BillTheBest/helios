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
package org.helios.server.ot.session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.eclipse.jetty.continuation.Continuation;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.server.ot.session.camel.routing.ISubscriberRoute;
import org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor;
import org.helios.server.ot.session.camel.routing.SubscriptionRouteManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>Title: SubscriberSession</p>
 * <p>Description: Represents the state of a metric subscriber.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.SubscriberSession</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class SubscriberSession extends ManagedObjectDynamicMBean implements ApplicationContextAware  {
	/** The unique session identifier for this Helios Subscriber */
	protected final String sessionId;
	/** The output processor type name */
	protected final String outputProcessorType;
	/** The spring application context */
	protected volatile ApplicationContext applicationContext = null;
	/** The output format for this session */
	protected OutputFormat outputFormat = null;
	/** The subscription manager for this session */
	protected SubscriptionRouteManager subscriptionRouteManager = null; 
	/** A map of subscriber routes keyed by the subscriber route type key */
	//protected final Map<String, ISubscriberRoute> subscriberRoutes = new ConcurrentHashMap<String, ISubscriberRoute>();
	/** Instance logger */
	protected final Logger log;
	
	
	
	/** A cache of sessions */
	protected static final Map<String, SubscriberSession> sessions = new ConcurrentHashMap<String, SubscriberSession>();
	/** The JMX ObjectName template for SubscriberSession instances */
	public static final String OBJECT_NAME_TEMPLATE = SubscriberSession.class.getPackage().getName() + ":type=" + SubscriberSession.class.getSimpleName() + ",id=%s";
	

	/**
	 * Acquires the SubscriberSession identified by the passed session Id.
	 * @param sessionId The session's unique identifier
	 * @return a SubscriberSession or null if one does not exist for the passed session Id.
	 */
	public static SubscriberSession getInstance(String sessionId) {
		if(sessionId==null) throw new IllegalArgumentException("The passed sessionId was null", new Throwable());
		return sessions.get(sessionId);		
	}
	/**
	 * Creates and caches a new SubscriberSession
	 * @param sessionId The session's unique identifier
	 * @param outputFormat The name of the output format for this session
	 * @param outputProcessorType The output processor bean name
	 * @return a SubscriberSession
	 */
	public static SubscriberSession getInstance(String sessionId, String outputFormat, String outputProcessorType) {
		return getInstance(sessionId, OutputFormat.forName(outputFormat), outputProcessorType);
	}

	
	/**
	 * Creates and caches a new SubscriberSession
	 * @param sessionId The session's unique identifier
	 * @param outputFormat The output format for this session
	 * @param outputProcessorType The output processor bean name
	 * @return a SubscriberSession
	 */
	public static SubscriberSession getInstance(String sessionId, OutputFormat outputFormat, String outputProcessorType) {
		if(sessionId==null || "".equals(sessionId)) throw new IllegalArgumentException("The passed sessionId was null or zero length", new Throwable());
		if(outputFormat==null) throw new IllegalArgumentException("The passed outputFormat was null", new Throwable());
		SubscriberSession session = sessions.get(sessionId);
		if(session==null) {
			synchronized(sessions) {
				session = sessions.get(sessionId);
				if(session==null) {
					session = new SubscriberSession(sessionId, outputFormat, outputProcessorType);
					sessions.put(sessionId, session);
					session.reflectObject(session);					
					JMXHelper.getRuntimeHeliosMBeanServer().registerMBean(session, getSubscriberSessionObjectName(sessionId));
				}
			}
		}
		return session;
	}
	
	/**
	 * Determines if the passed sessionId represents a created SubscriberSession
	 * @param sessionId The session id to interogate for
	 * @return true if the passed sessionId represents a created SubscriberSession, false otherwise
	 */
	public static boolean isSessionCreated(String sessionId) {
		if(sessionId==null) throw new IllegalArgumentException("The passed sessionId was null", new Throwable());
		return sessions.containsKey(sessionId);
	}
	
	/**
	 * Returns the JMX ObjectName for the passed session Id.
	 * @param sessionId The session Id to get the JMX ObjectName for
	 * @return an ObjectName
	 */
	public static ObjectName getSubscriberSessionObjectName(CharSequence sessionId) {
		if(sessionId==null) throw new IllegalArgumentException("The passed sessionId was null", new Throwable());
		return JMXHelper.objectName(String.format(OBJECT_NAME_TEMPLATE, sessionId.toString()));
	}
	
	/**
	 * Terminates the session subscriptions and this session.
	 */
	
	public void terminate() {
		subscriptionRouteManager.terminate();
		ObjectName on = getSubscriberSessionObjectName(sessionId);
		if(JMXHelper.getRuntimeHeliosMBeanServer().isRegistered(on)) {
			JMXHelper.getRuntimeHeliosMBeanServer().unregisterMBean(on);
		}
		sessions.remove(sessionId);
	}
	
	/**
	 * Starts a new Subscription Route of the passed type. If a route of the same type is already registered, it will be terminated.
	 * @param routeType The subscriber route type key
	 * @param subscriberParams The subscrier route parameters
	 * @return The new subscriber route cofiguration and runtime properties
	 */
	public Map<String, Object> startSubscriptionRoute(String routeType, Map<String, String> subscriberParams) {
		return subscriptionRouteManager.startSubscriptionRoute(sessionId, routeType, subscriberParams);
	}
	
	/**
	 * Stops a subscription subKey
	 * @param routeType The route typeKey
	 * @param subscriberParams The client supplied params
	 */
	public void stopSubscriptionRoute(String routeType, Map<String, String> subscriberParams) {
		subscriptionRouteManager.stopSubscriptionRoute(routeType, sessionId, subscriberParams);
	}
	
//	/**
//	 * Stops the Subscription Route of the passed type.
//	 * @param routeType The subscriber route type key
//	 * @return true if the sub route was stopped, false if it did not exist.
//	 */
//	public boolean stopSubscriptionRoute(String routeType) {
//		subscriptionRouteManager.
//		ISubscriberRoute subRoute = subscriberRoutes.get(routeType); 
//		if(subRoute!=null) {
//			subRoute.terminate();
//			return true;
//		}
//		return false;
//	}
	
	
	/**
	 * Polls for item delivery
	 * @param atATime The maximum number of items to deliver at a time
	 * @param timeout The period of time in ms. to wait if there are no items for delivery
	 */
	public Set<?> poll(int atATime, long timeout) {
		return subscriptionRouteManager.getOutputProcessor().poll(atATime, timeout);
	}	
	
	/**
	 * Registers a continuation that the processor will resume when a new item is published
	 * @param continuation a jetty continuation
	 */
	public void registerContinuation(Continuation continuation) {
		subscriptionRouteManager.registerContinuation(continuation);
	}
	
	
	/**
	 * Creates a new SubscriberSession
	 * @param sessionId The unique session identifier
	 * @param outputFormat The output format for this session
	 * @param outputProcessorType The output processor bean name
	 */
	private SubscriberSession(String sessionId, OutputFormat outputFormat, String outputProcessorType) {
		this.sessionId = sessionId;
		this.outputFormat = outputFormat;
		this.outputProcessorType = outputProcessorType;
		log = Logger.getLogger(getClass().getName() + "." + this.sessionId);
	}
	
	/**
	 * Returns the output format
	 * @return the output format
	 */
	public OutputFormat getOutputFormat() {
		return outputFormat;
	}
	/**
	 * Sets the output format
	 * @param outputFormat the output format
	 */
	public void setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = outputFormat;
	}


	/**
	 * The output format name for this session
	 * @return the outputFormat
	 */
	@JMXAttribute(name="OutputFormat", description="The output format name for this session", mutability=AttributeMutabilityOption.READ_WRITE)
	public String getOutputFormatName() {
		return outputFormat.name();
	}

	/**
	 * Sets the output format name for this session
	 * @param outputFormat the outputFormat to set
	 */
	public void setOutputFormatName(String outputFormat) {
		this.outputFormat = OutputFormat.forName(outputFormat);
	}
	/**
	 * @param applicationContext the applicationContext to set
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		subscriptionRouteManager =  (SubscriptionRouteManager)applicationContext.getBean("SubscriptionRouteManager", this.sessionId, this.outputFormat, outputProcessorType);
		SubscriptionOutputProcessor<?> sop = subscriptionRouteManager.getOutputProcessor();
		if(sop!=null) {
			this.reflectObject(sop);
		}
	}
	/**
	 * @return the subscriptionRouteManager
	 */
	public SubscriptionRouteManager getSubscriptionRouteManager() {
		return subscriptionRouteManager;
	}
	/**
	 * @param subscriptionRouteManager the subscriptionRouteManager to set
	 */
	public void setSubscriptionRouteManager(SubscriptionRouteManager subscriptionRouteManager) {
		this.subscriptionRouteManager = subscriptionRouteManager;
	}
	/**
	 * @return the outputProcessorType
	 */
	public String getOutputProcessorType() {
		return outputProcessorType;
	}
	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}
}
