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
package org.helios.esper.engine;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.esper.engine.service.interactive.RemoteStatementSession;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.spring.container.HeliosApplicationContext;
import org.helios.threads.DroppedThreadPoolTaskCounter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPServiceStateListener;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.client.EPStatementStateListener;
import com.espertech.esper.client.EventSender;
import com.espertech.esper.core.EPRuntimeImpl;
import com.espertech.esper.core.EPStatementImpl;

/**
 * <p>Title: Engine</p>
 * <p>Description: </p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.Engine</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class Engine implements ApplicationContextAware, BeanNameAware, EPServiceStateListener, EPStatementStateListener {
	/** The bean name of the esper engine */
	protected String beanName = null;
	/** The spring application context */
	protected HeliosApplicationContext appContext = null;
	/** The esper configuration */
	protected Configuration configuration = null;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The esper service provider */
	protected EPServiceProvider provider = null;
	/** The esper administrator */
	protected EPAdministrator esperAdmin = null;
	/** The esper runtime engine */
	protected EPRuntime esperRuntime = null;
	/** Predefined esper aliases */
	protected Map<String, String> aliases = new HashMap<String, String>();
	/** Predefined esper statements */
	protected Map<String, String> statements = new HashMap<String, String>();
	/** Predefined esper variables */
	protected Map<String, Object> variables = new HashMap<String, Object>();	
	/** Names of pre-registered class names */
	protected Set<String> mappedClasses = new CopyOnWriteArraySet<String>();
	/** Pre-compiled esper statements */
	protected Map<String, EPStatement> compiledStatements = new ConcurrentHashMap<String, EPStatement>();
	/** Registered listeners */
	protected Set<ListenerRegistration> listeners = new HashSet<ListenerRegistration>();
	/** The Helios Esper Engine JMX ObjectName */
	protected ObjectName objectName = null;
	/** Indicates and sets metric enablement for the engine statements */
	protected AtomicBoolean metricsEnabled = new AtomicBoolean(false);
	/** The thread pool for handling outboud notifications */
	protected ExecutorService notificationThreadPool = null;
	/** The counter for saturation dropped outboud notifications */
	protected final AtomicLong notificationDrops = new AtomicLong(0L);
	

	/** The default Helios Esper Engine JMX ObjectName */
	public static final ObjectName DEFAULT_OBJECT_NAME = JMXHelperExtended.objectName("org.helios.server.core:service=EsperEngine,type=Core");
	
	/**
	 * Determines if the passed class has been registered as an alias.
	 * @param clazz
	 * @return
	 */
	public boolean isEventTypeRegistered(Class<?> clazz) {
		return mappedClasses.contains(clazz.getName());
	}
	
	/**
	 * Adds a new simple event alias.
	 * @param clazz
	 */
	public void addSimpleEventAlias(Class<?> clazz) {
		configuration.addEventType(clazz);
	}

	/**
	 * Add an name for an event type represented by Java-bean plain-old Java object events. 
	 * @param name the name for the event type
	 * @param clazz the Java event class for which to add the name
	 */
	public void addSimpleEventAlias(String name, Class<?> clazz) {
		configuration.addEventType(name, clazz);
	}

	/**
	 * Returns an event sender for the passed class
	 * @param name the class to get an event sender for
	 * @return an event sender
	 */
	public EventSender getEventSender(Class<?> clazz) {
		if(clazz==null) throw new RuntimeException("Passed class was null");
		if(!isEventTypeRegistered(clazz)) {
			configuration.addEventType(clazz);
			configuration.addEventType(clazz.getSimpleName(), clazz);
		}
		return esperRuntime.getEventSender(clazz.getName());					
	}
	
	
	/**
	 * Returns an event sender for the passed class name
	 * @param name the name of the class to get an event sender for
	 * @return an event sender
	 */
	public EventSender getEventSender(String name) {
		
		try {
			if(name==null) throw new Exception("Passed name was null");
			return getEventSender(Class.forName(name));
		} catch (Exception e) {
			log.error("Failed to get event sender for [" + name + "]", e);
			throw new RuntimeException("Failed to get event sender for [" + name + "]", e);
		}
	}
	
	/**
	 * Injects an aray of events.
	 * @param events
	 */
	public void injectEvents(Object...events) {
		if(events!=null && events.length > 0) {
			if(!isEventTypeRegistered(events[0].getClass())) {
				configuration.addEventType(events[0].getClass());
			}
			for(Object o: events) {
				esperRuntime.sendEvent(o);
			}
		}
	}
	
	/**
	 * Stops the helios esper engine
	 */
	public void stop() {
		try {
			log.info("\n\t====\n\tStopping all Esper Statements\n\t====\n");
			esperAdmin.stopAllStatements();
		} catch (Exception e) {}

		try {
			log.info("\n\t====\n\tDestroying all Esper Statements\n\t====\n");
			esperAdmin.destroyAllStatements();
		} catch (Exception e) {}
		
		try {
			log.info("\n\t====\n\tDestroying Esper Provider\n\t====\n");
			provider.destroy();
		} catch (Exception e) {}
	}

	/**
	 * Starts the helios esper engine
	 * @throws Exception
	 */
	public void start() throws Exception {
		try {
			log.info("Starting EsperEngine [" + beanName + "]");
			if(objectName==null) objectName = DEFAULT_OBJECT_NAME;

			if(this.metricsEnabled.get()) {
				configuration.setMetricsReportingEnabled(); 
			} else {
				configuration.setMetricsReportingDisabled();
			}
			
			
			for(Entry<String, String> alias: aliases.entrySet()) {
				log.info("\t Loading Alias [" + alias.getKey() +"]");
				configuration.addEventType(alias.getKey(), alias.getValue());
				mappedClasses.add(alias.getValue());
			}
			
			StringBuilder b = new StringBuilder("\nEngine Variables:");
			for(Map.Entry<String, Object> variable: variables.entrySet()) {
				if(variable.getValue()!=null) {
					configuration.addVariable(variable.getKey(), variable.getValue().getClass(), variable.getValue());
					b.append("\n\t[").append(variable.getKey()).append("]:[").append(variable.getValue()).append("]");
				}
			}
			b.append("\n");
			log.info(b);
			
			
			provider = EPServiceProviderManager.getProvider(beanName,configuration);
			provider.addStatementStateListener(this);
			
			EsperEngineMetrics providerMetrics = new EsperEngineMetrics(provider);
			providerMetrics.start();
			JMXHelperExtended.getHeliosMBeanServer().registerMBean(providerMetrics, new ObjectName("org.helios.esper:type=EngineMetrics,name=" + provider.getURI()));
			if(notificationThreadPool==null) {
				notificationThreadPool = new ThreadPoolExecutor(2, 5, 15000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(500, true));
			}
			if(notificationThreadPool instanceof ThreadPoolExecutor) {
				((ThreadPoolExecutor)notificationThreadPool).setRejectedExecutionHandler(new DroppedThreadPoolTaskCounter(this.notificationDrops));
			}

			
			esperAdmin = provider.getEPAdministrator();
			esperRuntime = provider.getEPRuntime();
			for(Entry<String, String> statement: statements.entrySet()) {			
				EPStatement st = esperAdmin.createEPL(statement.getValue(), statement.getKey());
				compiledStatements.put(statement.getKey(), st);
				log.info("\t Compiled Statement [" + statement.getKey() +"]");
			}
			for(ListenerRegistration listener: listeners) {
				int i = 0;
				for(String stName: listener.getTargetStatements()) {
					EPStatement st = compiledStatements.get(stName);
					if(st!=null) {
						st.addListener(listener);
						i++;
					} else {
						log.warn("Listener Requested Non-Existent Statement:" + stName);
					}
				}
				log.info("Registered Listener [" +listener.getName() + "] for [" + i + "] Statements.");
			}		
			try {
				ManagedObjectDynamicMBean modb = new ManagedObjectDynamicMBean("Helios Core Metric Engine", this); 
				JMXHelperExtended.getHeliosMBeanServer().registerMBean(modb, objectName);
			} catch (Exception e) {
				log.warn("Failed to register JMX management interface:" + e);
			}
			log.info("Started EsperEngine [" + beanName + "]");
		} catch (Throwable e) {
			log.error("Failed to start esper engine", e);
			throw new Exception("Failed to start esper engine", e);
		}
	}
	
	/**
	 * @return the provider
	 */
	public EPServiceProvider getProvider() {
		return provider;
	}

	/**
	 * @return the runtime
	 */
	public EPRuntime getEsperRuntime() {
		return esperRuntime;
	}
	

	/**
	 * @param configuration the configuration to set
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * @param aliases the aliases to set
	 */
	public void setAliases(Map<String, String> aliases) {
		this.aliases = aliases;
	}
	

	/**
	 * Sets the Helios Spring ApplicationContext
	 * @param ac the injected ApplicationContext
	 * @throws BeansException
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext ac) throws BeansException {
		appContext = (HeliosApplicationContext)ac;
	}

	/**
	 * Sets the bean name
	 * @param name
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		beanName = name;
	}

	/**
	 * @return the esperAdmin
	 */
	public EPAdministrator getEsperAdmin() {
		return esperAdmin;
	}

	/**
	 * @param statements the statements to set
	 */
	public void setStatements(Map<String, String> statements) {
		this.statements = statements;
	}

	/**
	 * @param listeners the listeners to set
	 */
	public void setListeners(Set<ListenerRegistration> listeners) {
		this.listeners = listeners;
	}
	
	/**
	 * Configures the initial engine variables
	 * @param variables
	 */
	public void setVariables(Map<String, Object> variables) {
		if(variables!=null && variables.size() > 0) {
			this.variables.putAll(variables);
			if(esperRuntime!=null) {
				esperRuntime.setVariableValue(variables);
			}
		}
		
	}
	
	/**
	 * Sets a variable value in the engine
	 * @param name the variable name
	 * @param value the variable value
	 */
	@JMXOperation(name="setVariable",description="Sets a variable value in the engine")
	public void setVariable(
			@JMXParameter(name="name", description="The name of the variable to set") String name, 
			@JMXParameter(name="value", description="The value of the variable to get") Object value) {
		if(name!=null && value!=null) {
			this.variables.put(name, value);
			if(esperRuntime!=null) {
				esperRuntime.setVariableValue(name, value);
			}
		}
	}
	
	/**
	 * Returns the engine variable key names
	 * @return an array of variable names
	 */
	@JMXAttribute(name="VariableNames", description="The engine variable key names", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getVariableNames() {
		if(esperRuntime!=null) {
			return esperRuntime.getVariableValueAll().keySet().toArray(new String[0]);
		} else {
			return this.variables.keySet().toArray(new String[this.variables.size()]);
		}
	}
	
	/**
	 * Returns the value of the named variable
	 * @param name the variable name
	 * @return the variable value or null.
	 */
	@JMXOperation(name="getVariable",description="Returns the value of the named variable")
	public Object getVariable(@JMXParameter(name="name", description="The name of the variable to get") String name) {
		if(name==null) return null;
		if(this.esperRuntime!=null) {
			return variables.get(name);
		} else {
			return this.esperRuntime.getVariableValue(name);
		}		
	}
	
	/**
	 * The number of input metrics since start or the last reset.
	 * @return number of input metrics since start or the last reset.
	 */
	@JMXAttribute (
			description="The number of input metrics since start or the last reset.",
			expose=true,
			name="InCount",
			mutability=AttributeMutabilityOption.READ_ONLY
	)
	public long getInCount() {
		return esperRuntime.getNumEventsEvaluated();
	}
	
	/**
	 * The number of metrics routed internally since start or the last reset.
	 * @return number of metrics routed internally since start or the last reset.
	 */
	@JMXAttribute (
			description="The number of metrics routed internally since start or the last reset.",
			expose=true,
			name="RoutedInternalCount",
			mutability=AttributeMutabilityOption.READ_ONLY
	)
	public long getRoutedInternalCount() {
		return ((EPRuntimeImpl)esperRuntime).getRoutedInternal();
	}
	
	/**
	 * The number of metrics routed externally since start or the last reset.
	 * @return number of metrics routed exnternally since start or the last reset.
	 */
	@JMXAttribute (
			description="The number of metrics routed externally since start or the last reset.",
			expose=true,
			name="RoutedExternalCount",
			mutability=AttributeMutabilityOption.READ_ONLY
	)
	public long getRoutedExternalCount() {
		return ((EPRuntimeImpl)esperRuntime).getRoutedExternal();
	}
	
	/**
	 * Resets the internal stats.
	 */
	@JMXOperation (description="Resets the internal stats", name="resetStats")
	public void resetStats() {
		esperRuntime.resetStats();
	}

	/**
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * @param objectName the objectName to set
	 */
	public void setObjectName(ObjectName objectName) {
		this.objectName = objectName;
	}
	
	/**
	 * Indicates if engine statement metrics are enabled.
	 * @return true if metric are enabled, false if not.
	 */
	@JMXAttribute (name="MetricsEnabled", description="Indicates if engine statement metrics are enabled.", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getMetricsEnabled() {
		return metricsEnabled.get();
	}

	/**
	 * Enables or disables engine statement metrics.
	 * @param metricsEnabled true to enable, false to disable
	 */
	public void setMetricsEnabled(boolean metricsEnabled) {
		this.metricsEnabled.set(metricsEnabled);
		if(esperAdmin!=null) {			
			if(this.metricsEnabled.get()) {
				esperAdmin.getConfiguration().setMetricsReportingEnabled(); 
			} else {
				esperAdmin.getConfiguration().setMetricsReportingDisabled();
			}
		}
	}

	/**
	 * Callback when esper service is destroyed
	 * @param serviceProvider the service provider being destroyed
	 */
	public void onEPServiceDestroyRequested(EPServiceProvider serviceProvider) {
		log.info("Destroying Esper Service Provider [" + serviceProvider.getURI() + "]");
	}

	/**
	 * Callback when esper service is initialized
	 * @param serviceProvider the service provider being initialized
	 */
	public void onEPServiceInitialized(EPServiceProvider serviceProvider) {
		log.info("Initializing Esper Service Provider [" + serviceProvider.getURI() + "]");		
	}

	/**
	 * @param provider
	 * @param statement
	 * @see com.espertech.esper.client.EPStatementStateListener#onStatementCreate(com.espertech.esper.client.EPServiceProvider, com.espertech.esper.client.EPStatement)
	 */
	@Override
	public void onStatementCreate(EPServiceProvider provider, EPStatement statement) {
		if(!statement.getName().startsWith("Monitor-")) {
			log.info("Starting Statement Monitor for [" + statement.getName() + "]");
			EsperStatementMetrics esm = new EsperStatementMetrics(statement, provider);
			try {
				JMXHelperExtended.getHeliosMBeanServer().registerMBean(esm, createMetricsObjectName(statement));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}		
	}

	/**
	 * @param provider
	 * @param statement
	 * @see com.espertech.esper.client.EPStatementStateListener#onStatementStateChange(com.espertech.esper.client.EPServiceProvider, com.espertech.esper.client.EPStatement)
	 */
	@Override
	public void onStatementStateChange(EPServiceProvider provider, EPStatement statement) {
		if(statement.getState().equals(EPStatementState.DESTROYED)) {
			try {
				JMXHelperExtended.getHeliosMBeanServer().unregisterMBean(createMetricsObjectName(statement));				
			} catch (Exception e) {
				//e.printStackTrace();
			}			
		}
	}
	
	/** The format template for a metric MBean ObjectName  */
	public static final String METRIC_ON_PREFIX = "org.helios.engine.metrics:metrictype={0},name={1},type={2}";
	
	/**
	 * Creates a metric MBean ObjectName for the passed statement
	 * @param statement The statement to create ObjectName for
	 * @return an ObjectName
	 */
	public static ObjectName createMetricsObjectName(EPStatement statement) {
		return JMXHelper.objectName(MessageFormat.format(METRIC_ON_PREFIX, "Statement", statement.getName(), ((EPStatementImpl)statement).getStatementMetadata().getStatementType().name()));
	}
	
	/**
	 * Starts a remote statement session
	 * @param connectionId the connection id of the session
	 * @return the session's designated JMX ObjectName.
	 */
	@JMXOperation (description="Starts a remote statement session. Returns the session's JMX ObjectName.", name="startRemote")
	public ObjectName startRemote(@JMXParameter(name="connectionId", description="The connection id of the session") String connectionId) {
		RemoteStatementSession rss = new RemoteStatementSession(connectionId, this.provider, notificationThreadPool);
		return rss.getObjectName();
	}

	/**
	 * Returns the outbound JMX notification handling thread pool
	 * @return the notificationThreadPool
	 */
	@JMXAttribute (name="NotificationThreadPool", description="The outbound JMX notification handling thread pool", mutability=AttributeMutabilityOption.READ_WRITE)
	public ExecutorService getNotificationThreadPool() {
		return notificationThreadPool;
	}

	/**
	 * Sets the outbound JMX notification handling thread pool
	 * @param notificationThreadPool the notificationThreadPool to set
	 */
	public void setNotificationThreadPool(ExecutorService notificationThreadPool) {
		this.notificationThreadPool = notificationThreadPool;
	}

	/**
	 * Returns the number of saturation dropped outboud notifications
	 * @return the number of saturation dropped outboud notifications
	 */
	@JMXAttribute (name="NotificationDrops", description="The number of saturation dropped outboud notifications", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getNotificationDrops() {
		return notificationDrops.get();
	}
	
	/**
	 * Resets the count of saturation dropped outboud notifications
	 */
	@JMXOperation (description="Resets the count of saturation dropped outboud notifications", name="resetNotificationDrops")
	public void resetNotificationDrops() {
		notificationDrops.set(0L);
	}
}
