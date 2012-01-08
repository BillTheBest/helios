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
package org.helios.esper.engine.service.interactive;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;

import org.apache.log4j.Logger;
import org.helios.esper.engine.Engine;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.client.EPStatementStateListener;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.core.EPStatementImpl;

/**
 * <p>Title: RemoteStatementSession</p>
 * <p>Description: A stateful session proxy for the helios esper command line app.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.service.interactive.RemoteStatementSession</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
@JMXNotifications(notifications={
		@JMXNotification(description="Event emmited notification", types={
				@JMXNotificationType(type=RemoteStatementSession.STATEMENT_EVENT)
		}),
		@JMXNotification(description="Subscribed Statement Created", types={
				@JMXNotificationType(type=RemoteStatementSession.STATEMENT_CREATED)
		}),		
		@JMXNotification(description="Subscribed Statement Failed", types={
				@JMXNotificationType(type=RemoteStatementSession.STATEMENT_FAILED)
		}),
		@JMXNotification(description="Subscribed Statement Started", types={
				@JMXNotificationType(type=RemoteStatementSession.STATEMENT_STARTED)
		}),
		@JMXNotification(description="Subscribed Statement Stopped", types={
				@JMXNotificationType(type=RemoteStatementSession.STATEMENT_STOPPED)
		}),
		@JMXNotification(description="Subscribed Statement Destroyed", types={
				@JMXNotificationType(type=RemoteStatementSession.STATEMENT_DESTROYED)
		})		
})

public class RemoteStatementSession extends ManagedObjectDynamicMBean implements NotificationListener, NotificationFilter, StatementAwareUpdateListener, EPStatementStateListener {
	/** JMX notification type for statement created */
	public static final String STATEMENT_CREATED = "org.helios.esper.statement.state.created";	
	/** JMX notification type for statement failed */
	public static final String STATEMENT_FAILED = "org.helios.esper.statement.state.failed";
	/** JMX notification type for statement stopped */
	public static final String STATEMENT_STOPPED = "org.helios.esper.statement.state.stopped";
	/** JMX notification type for statement started */
	public static final String STATEMENT_STARTED = "org.helios.esper.statement.state.started";
	/** JMX notification type for statement destroyed */
	public static final String STATEMENT_DESTROYED = "org.helios.esper.statement.state.destroyed";
	/** JMX notification type for emitted event*/
	public static final String STATEMENT_EVENT = "org.helios.esper.event";
	/** Statement meta-data key for the statement Id */
	public static final String STATEMENT_ID = "statement.id";
	/** Statement meta-data key for the statement state */
	public static final String STATEMENT_STATE = "statement.state";
	/** Statement meta-data key for the statement type */
	public static final String STATEMENT_TYPE = "statement.type";
	/** Statement meta-data key for the statement name */
	public static final String STATEMENT_NAME = "statement.name";
	/** Statement meta-data key for the statement event type name */
	public static final String STATEMENT_EVENT_TYPE = "statement.event.type";
	/** Statement meta-data key for the statement EPL text */
	public static final String STATEMENT_TEXT = "statement.text";
	/** Statement meta-data key for the statement last time changed */
	public static final String STATEMENT_LAST_CHANGE_TIME = "statement.lastchangetime";
	
	
	/** The JMX remoting connection Id */
	protected final String connectionId;
	/** the esper service provider */
	protected final EPServiceProvider esperProvider;
	/** the session options */
	protected final StatementOptions options = new StatementOptions();
	/** The current esper subscription statement */
	protected final AtomicReference<EPStatement> subscribedStatement = new AtomicReference<EPStatement>(null);
	/** The statement metrics MBean ObjectName for the current subscribed statement */
	protected final AtomicReference<ObjectName> metricsObjectName = new AtomicReference<ObjectName>(null);
	/** The current statement map provided to the client on statement state change */
	protected Map<String, String> statementMap = null;	
	/** The JMX Server's ObjectName */
	public static final ObjectName JMX_SERVER_OBJECT_NAME = JMXHelper.objectName("org.helios.jmx:service=JMXConnectorServer");
	/** The RemoteStatementSession instance's object name template */
	public static final String SESSION_OBJECT_NAME = "org.helios.esper.remote:connectionid=";
	/** Instance logger */
	protected final Logger log;
	/**  */
	private static final long serialVersionUID = 3485040176531579701L;
	/** Serial number generator for generated statement names */
	protected final AtomicInteger statementId = new AtomicInteger(0);
	
	/** The thread pool for handling outboud notifications */
	protected final ExecutorService notificationThreadPool;
	
	/** The last exception thrown from a remote invocation */
	protected final AtomicReference<Exception> lastException  = new AtomicReference<Exception>(); 
	
	
	/**
	 * Creates a new RemoteStatementSession 
	 * @param connectionId The JMX remoting connectionId
	 * @param esperProvider The esper service provider
	 * @param notificationThreadPool The engine supplied thread pool for handling outbound notifications
	 */
	public RemoteStatementSession(String connectionId, EPServiceProvider esperProvider, ExecutorService notificationThreadPool) {
		log = Logger.getLogger(getClass().getName() + "." + connectionId.replace('.', '_'));
		this.connectionId = connectionId;
		this.esperProvider = esperProvider;
		this.notificationThreadPool = notificationThreadPool;
		objectName = JMXHelper.objectName(SESSION_OBJECT_NAME + ObjectName.quote(connectionId));
		log.info("Creating remote session for [" + connectionId + "]");
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
			JMXHelper.getHeliosMBeanServer().addNotificationListener(JMX_SERVER_OBJECT_NAME, this, this, null);
			log.info("Registered session MBean [" + objectName + "]");
		} catch (Exception e) {
			try { JMXHelper.getHeliosMBeanServer().removeNotificationListener(JMX_SERVER_OBJECT_NAME, this); } catch (Exception ex) {}
			try { JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName); } catch (Exception ex) {}
			throw new RuntimeException("Failed to register remote statement session for connection id [" + connectionId + "]", e);
		}		
	}
	
	/**
	 * Executes an on demmand epl query and returns the rednered results.
	 * @param epl the epl on demmand query
	 * @return the rendering of the on demmand query result.
	 */
	@JMXOperation(name="query", description="Executes an on demmand epl query and returns the results")
	public Object query(@JMXParameter(name="epl", description="The EPL OnDemmand Query") String epl) throws Exception {
		try {
			if(log.isDebugEnabled()) log.debug("Executing OnDemmand EPL [" + epl + "]");
			EPOnDemandQueryResult result = esperProvider.getEPRuntime().executeQuery(epl);
			EventBean[] results = result.getArray();
			if(log.isDebugEnabled()) log.debug("Processed EPL for [" + results.length + "] results");
			return ((Render)options.getOption(StatementOptions.RENDER)).render(esperProvider.getEPRuntime(), results);
		} catch (Exception e) {
			log.error("Failed to execute epl [" + epl + "]", e);
			lastException(e);
			throw new Exception("Failed to execute epl [" + epl + "]", e);
		}
	}
	
	/**
	 * Returns all the registered statement names
	 * @return all the registered statement names
	 */
	@JMXAttribute(name="StatementNames", description="Returns all the statement names known to the administrator", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getStatementNames() {
		Set<String> names = new HashSet<String>();
		for(String name: esperProvider.getEPAdministrator().getStatementNames()) {
			if(name!=null && !name.startsWith("Monitor-")) {
				names.add(name);
			}
		}
		return names.toArray(new String[names.size()]);
	}
	
	/**
	 * Returns all the registered SELECT statement names
	 * @return all the registered SELECT statement names
	 */
	@JMXAttribute(name="SelectStatementNames", description="Returns all the SELECT statement names known to the administrator", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getSelectStatementNames() {
		return getStatementNames("SELECT");
	}
	
	
	/**
	 * Returns all the registered statement names that have a type that matches the passed regex
	 * @param typeMatch
	 * @return all the registered statement names that have a type that matches the passed regex
	 */
	@JMXOperation(name="getStatementNames", description="Returns all the statement names known to the administrator that have a type that matches the passed regex")
	public String[] getStatementNames(@JMXParameter(name="typeMatch", description="A regex to match to the statement type") String typeMatch) {
		Set<String> names = new HashSet<String>();
		Pattern p = Pattern.compile(typeMatch, Pattern.CASE_INSENSITIVE);
		for(String name: esperProvider.getEPAdministrator().getStatementNames()) {
			if(name==null) continue;
			String type = ((EPStatementImpl)esperProvider.getEPAdministrator().getStatement(name)).getStatementMetadata().getStatementType().name();
			if(p.matcher(type).matches() && !name.startsWith("Monitor-")) {
				names.add(name);
			}
		}
		return  names.toArray(new String[names.size()]);
	}
	
	
	/**
	 * Creates and subscribes to an epl statement which will broadcast the events as JMX notifications.
	 * @param epl the epl statement
	 * @param name the epl statement name
	 * @return this session's metrics ObjectName
	 */
	@JMXOperation(name="subscribe", description="Creates and subscribes to an epl statement which will broadcast the events as JMX notifications.")
	public ObjectName subscribe(
			@JMXParameter(name="epl", description="The EPL Statement") String epl,
			@JMXParameter(name="name", description="The EPL Statement name") String name) throws Exception {
		try {
			EPStatement st = this.subscribedStatement.get();
			if(st!=null) {			
				throw new RuntimeException("Exception subscribing. Subscription is already running. [" + st.getName() + "/" + st.getState().name() + "]");
			}			
			if(log.isDebugEnabled()) log.debug("Subscribing to EPL Statement [" + epl + "]");
			st = esperProvider.getEPAdministrator().createEPL(epl, name.replace(':', ';')); 
			this.subscribedStatement.set(st);
			this.metricsObjectName.set(Engine.createMetricsObjectName(st));
			st.addListener(this);
			if(log.isDebugEnabled()) log.debug("Subscribed to EPL Statement [" + epl + "] results");
			return this.metricsObjectName.get();
		} catch (Exception e) {
			throw lastException(new RuntimeException("Failed to create epl  statement [" + epl + "]", e));
		}
	}
	
	
	
	/**
	 * Stops the currently running statement
	 */
	@JMXOperation(name="stopSubscription", description="Stops the currently running statement.")
	public void stopSubscription() {
		EPStatement st = this.subscribedStatement.get();
		if(st==null) {			
			throw new RuntimeException("Exception stopping. No subscription is running.");
		} else {
			this.subscribedStatement.get().stop();
		}		
	}
	
	/**
	 * Returns the current statement name, or null if one is not registered
	 * @return the statement name or null
	 */
	@JMXAttribute(name="CurrentStatementName", description="The current statement name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getCurrentStatementName() {
		EPStatement st = this.subscribedStatement.get();
		if(st==null) {			
			return null;
		} else {
			return st.getName();			
		}		
	}
	
	/**
	 * Returns the current statement state, or null if one is not registered
	 * @return the statement name or null
	 */
	@JMXAttribute(name="CurrentStatementState", description="The current statement state", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getCurrentStatementState() {
		EPStatement st = this.subscribedStatement.get();
		if(st==null) {			
			return null;
		} else {
			return st.getState().name();			
		}		
	}
	
	/**
	 * Returns the current statement text, or null if one is not registered
	 * @return the statement text or null
	 */
	@JMXAttribute(name="CurrentStatementText", description="The current statement text", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getCurrentStatementText() {
		EPStatement st = this.subscribedStatement.get();
		if(st==null) {			
			return null;
		} else {
			return st.getText();			
		}		
	}
	
	/**
	 * Destroys the currently registered statement
	 */
	@JMXOperation(name="destroySubscription", description="Destroys the currently registered statement.")
	public void destroySubscription() {
		EPStatement st = this.subscribedStatement.get();
		if(st==null) {			
			throw new RuntimeException("Exception destroying. No subscription is running.");
		} else {
			try { this.subscribedStatement.get().stop(); } catch (Exception e) {}
			this.subscribedStatement.get().destroy();
		}		
	}
	
	
	/**
	 * Creates and subscribes to an epl statement which will broadcast the events as JMX notifications.
	 * @param epl the epl statement
	 * @param name the epl statement name
	 * @return this session's metrics ObjectName
	 */
	@JMXOperation(name="subscribe", description="Creates and subscribes to an epl statement which will broadcast the events as JMX notifications.")
	public ObjectName subscribe(@JMXParameter(name="epl", description="The EPL Statement") String epl) throws Exception {
		EPStatement st = this.subscribedStatement.get();
		if(st!=null) {
			throw lastException(new RuntimeException("Exception subscribing. Subscription is already running. [" + st.getName() + "/" + st.getState().name() + "]"));
		}
		return subscribe(epl, "Statement for [" + connectionId + "]#" + statementId.incrementAndGet());
	}
	
	
	
	/**
	 * Returns the ObjectName of the metrics MBean for the currently executing statement
	 * @return  the ObjectName  or null if no statement is running.
	 */
	@JMXAttribute(name="StatementMetrics", description="The ObjectName of the metrics MBean for the currently executing statement", mutability=AttributeMutabilityOption.READ_ONLY)
	public ObjectName getStatementMetrics() {
		return this.metricsObjectName.get();
	}
	
	
	
	/**
	 * Returns the value of the named variable
	 * @param name the variable name
	 * @return the variable value
	 */
	@JMXOperation(name="getVariableValue", description="Returns the value of the named variable")
	public Object getVariableValue(@JMXParameter(name="name", description="The variable name") String name) {
		return esperProvider.getEPRuntime().getVariableValue(name);
	}
	
	/**
	 * Sets the value of the named variable
	 * @param name the name of the variable
	 * @param value the value of the variable
	 */
	@JMXOperation(name="setVariableValue", description="Sets the value of the named variable")
	public void setVariableValue(
			@JMXParameter(name="name", description="The variable name") String name, 
			@JMXParameter(name="value", description="The variable value")Object value) {
		esperProvider.getEPRuntime().setVariableValue(name, value);
	}
	
	/**
	 * Returns all the variables name declared.
	 * @return an array of variable names
	 */
	@JMXAttribute(name="VariableNames", description="Returns all the variables name declared", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getVariableNames() {
		return esperProvider.getEPRuntime().getVariableValueAll().keySet().toArray(new String[0]);
	}

	/**
	 * JMX remoting session disconnect listener. 
	 * @param notification The notification from the JMXServer
	 * @param handback The notification handback
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		if(subscribedStatement.get()!=null) {
			subscribedStatement.get().removeAllListeners();
			try { subscribedStatement.get().stop(); } catch (Exception e) {}
			try { subscribedStatement.get().destroy(); } catch (Exception e) {}			
		}
		try { JMXHelper.getHeliosMBeanServer().removeNotificationListener(JMX_SERVER_OBJECT_NAME, this); } catch (Exception ex) {}
		try { JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName); } catch (Exception ex) {}
		log.info("Handled disconnect for [" + connectionId + "]");

	}
	
//	public void addNotificationListener(NotificationListener listener,
//            NotificationFilter filter,
//            Object handback)
//            throws IllegalArgumentException {
//		log.info("Adding notification listener [" + listener.getClass().getName() + "]");
//		super.addNotificationListener(listener, filter, handback);
//	}

	/**
	 * Implements a filter for this session's JMX remoting session
	 * @param notification The npotification to inspect
	 * @return true to accept the notification
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		return (
				notification != null && 
				notification instanceof JMXConnectionNotification &&
				(
						JMXConnectionNotification.FAILED.equals(notification.getType()) ||
						JMXConnectionNotification.CLOSED.equals(notification.getType())
				) && connectionId.equals(((JMXConnectionNotification)notification).getConnectionId())				
		);
	}

	/**
	 * Callback from the statement when an event is fired.
	 * @param inEvents The events entering the stream
	 * @param outEvents The events leaving  the stream
	 * @param statement The statement the events are provided by
	 * @param provider the statement's provider
	 */
	@Override
	public void update(final EventBean[] inEvents, final EventBean[] outEvents, final EPStatement statement, final EPServiceProvider provider) {
		notificationThreadPool.execute(new Runnable(){
			public void run() {
				
				if(log.isDebugEnabled()) {
					StringBuilder b = new StringBuilder("Handling notification for statement [");
					b.append(statement.getName()).append("]");
					b.append("\n\tIn Events:").append(inEvents==null ? 0 : inEvents.length);
					b.append("\n\tOut Events:").append(outEvents==null ? 0 : outEvents.length);
					log.debug("");
				}
				Notification n = new Notification(STATEMENT_EVENT, objectName, nextNotificationSequence(), System.currentTimeMillis(), statement.getName());
				n.setUserData(Render.valueOf(options.getOption(StatementOptions.RENDER).toString()).render(esperProvider.getEPRuntime(), inEvents));
				sendNotification(n);				
			}
		});
	}

	/**
	 * Retrieves the value of an option
	 * @param name the option name
	 * @return the option value or null if the name is not bound
	 */
	@JMXOperation(name="getOption", description="Retrieves the value of an option")
	public Object getOption(@JMXParameter(name="name", description="The option name") String name) {
		return options.getOption(name);
	}

	/**
	 * Returns an array of the bound option names
	 * @return an array of the bound option names
	 */
	@JMXAttribute(name="OptionNames", description="An array of the bound option names", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getOptionNames() {
		return options.getOptionNames();
	}

	/**
	 * Returns the option map
	 * @return the option map
	 */
	@JMXAttribute(name="Options", description="The map of session options", mutability=AttributeMutabilityOption.READ_ONLY)
	public Map<String, Object> getOptions() {
		return options.getOptions();
	}

	/**
	 * Sets an option value
	 * @param name The option name
	 * @param value The option value
	 */
	@JMXOperation(name="setOption", description="Sets the value of an option")
	public void setOption(
			@JMXParameter(name="name", description="The option name") String name, 
			@JMXParameter(name="value", description="The option value") Object value) {
		options.setOption(name, value);
	}

	/**
	 * Called when this session's subscribed statement starts
	 * @param provider the esper provider
	 * @param statement this session's subscribed statement 
	 */
	@Override
	public void onStatementCreate(EPServiceProvider provider, EPStatement statement) {
		log.info("Statement started [" + statement.getText() + "] for connectionId [" + connectionId + "]");
		fireStatementNotification(statement, STATEMENT_CREATED, "Statement Created [" + statement.getName() + "]");
	}
	
	/**
	 * Fires a statement notification to registered listeners
	 * @param statement The statement
	 * @param type the notification type
	 * @param message the message
	 */
	protected void fireStatementNotification(EPStatement statement, String type, String message) {
		Notification n = new Notification(type, objectName, this.nextNotificationSequence(), System.currentTimeMillis(), message);
		Map<String, String> statementMap = new HashMap<String, String>(4);
		EPStatementImpl impl = (EPStatementImpl)statement;
		statementMap.put(STATEMENT_ID, impl.getStatementId());
		statementMap.put(STATEMENT_STATE, impl.getState().name());
		statementMap.put(STATEMENT_TYPE, impl.getStatementMetadata().getStatementType().name());
		statementMap.put(STATEMENT_NAME, impl.getName());
		statementMap.put(STATEMENT_EVENT_TYPE, impl.getEventType().getName());
		statementMap.put(STATEMENT_TEXT, impl.getText());
		statementMap.put(STATEMENT_LAST_CHANGE_TIME, "" + impl.getTimeLastStateChange());
		n.setUserData(statementMap);
		this.sendNotification(n);
	}

	/**
	 * Called when this session's subscribed statement changes state
	 * @param provider the esper provider
	 * @param statement the esper statement
	 */
	@Override
	public void onStatementStateChange(EPServiceProvider provider, EPStatement statement) {
		log.info("Statement changed state [" + statement.getState().name() + "]");
		if(this.subscribedStatement.get()!=null) {
			EPStatementState state = statement.getState();
			if(state.equals(EPStatementState.DESTROYED)) { // || state.equals(EPStatementState.STOPPED) || state.equals(EPStatementState.FAILED)) {
				fireStatementNotification(statement, STATEMENT_DESTROYED, "Statement DESTROYED [" + statement.getName() + "]");
				statementCleanup();
			} else if(state.equals(EPStatementState.STOPPED)) {
				fireStatementNotification(statement, STATEMENT_DESTROYED, "Statement STOPPED [" + statement.getName() + "]");
			} else if(state.equals(EPStatementState.STARTED)) {
				fireStatementNotification(statement, STATEMENT_STARTED, "Statement STARTED [" + statement.getName() + "]");
			} else if(state.equals(EPStatementState.FAILED)) {
				fireStatementNotification(statement, STATEMENT_FAILED, "Statement FAILED [" + statement.getName() + "]");
				statementCleanup();
			} else {
				log.error("Uknown statement state [" + statement + "]");
			}
		}
	}
	
	/**
	 * Cleans up a statement after a failure or destroy
	 */
	protected void statementCleanup() {
		EPStatement st = this.subscribedStatement.get();
		if(st!=null) {
			try { st.removeAllListeners(); } catch (Exception e) {}
			if(!st.isDestroyed()) {
				try { st.destroy(); } catch (Exception e) {}
			}			
			this.subscribedStatement.set(null);
			this.metricsObjectName.set(null);
		}
	}
	
	/**
	 * Sets and returns the last exception
	 * @param exc The thrown exeption
	 * @return The thrown exeption
	 */
	protected Exception lastException(Exception exc) {
		lastException.set(exc);
		return exc;
	}

	/**
	 * Returns the last exception thrown from a remote invocation
	 * @return the last exception thrown from a remote invocation
	 */
	@JMXAttribute(name="LastException", description="The last exception thrown from a remote invocation", mutability=AttributeMutabilityOption.READ_ONLY)
	public Exception getLastException() {
		return lastException.getAndSet(null);
	}

}
