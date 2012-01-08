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
package org.helios.espercl;

import static org.helios.espercl.ConsoleOps.err;
import static org.helios.espercl.ConsoleOps.notif;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;

/**
 * <p>Title: EsperSessionImpl</p>
 * <p>Description: An implementation of a proxy for the remote esper session.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.EsperSessionImpl</code></p>
 */

/*
 * TODO: local clean up
 * TODO: remote cleanup
 * TODO: stop, restart and destroy statements
 * TODO: expose current statement name to client
 * TODO: suppress statement operations if statement is null.
 * TODO: Queue for output events
 * TODO: Options impl.
 * TODO: space to toggle stop/restart
 */

public class EsperSessionImpl implements EsperSession, NotificationListener, StatementProxy {
	/** The MBeanServerInvocationHandler proxy */
	protected final EsperSession esperSession;
	/** The MBeanServerConnection */
	protected final MBeanServerConnection mbeanServer;
	/** The remote esper session ObjectName */
	protected final ObjectName sessionObjectName;
	/** flag to indicate the client is quiting and suppres error logging */
	protected AtomicBoolean quitting = new AtomicBoolean(false);
	
	/** The current statement's metrics ObjectName */
	protected final AtomicReference<ObjectName> metricsObjectName = new AtomicReference<ObjectName>();
	/** The remote proxy for the statement's metrics MBean */
	protected final AtomicReference<StatementProxy> metricsProxy = new AtomicReference<StatementProxy>();
	/** A set of statement listeners to be notified of events from this proxy */
	protected final Set<StatementListener> listeners = new CopyOnWriteArraySet<StatementListener>();
	/** An executor to process notifications to release the notifier thread as quickly as possible */
	//protected final Executor executor = Executors.
	
	/** The current statement name */
	protected String currentStName = null;
	/** The current statement text */
	protected String currentStText = null;
	
	
	/**
	 * Creates a new EsperSessionImpl
	 * @param mbeanServer The MBeanServerConnection
	 * @param sessionObjectName The remote esper session ObjectName
	 * @param listener The initial listener to register
	 */
	public EsperSessionImpl(MBeanServerConnection mbeanServer, ObjectName sessionObjectName, StatementListener listener) {		
		this.mbeanServer = mbeanServer;
		this.sessionObjectName = sessionObjectName;
		this.esperSession = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, sessionObjectName, EsperSession.class, true);
		try {
			this.mbeanServer.addNotificationListener(this.sessionObjectName, this, null, null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register notificationListener", e);
		}
		addListener(listener);
	}
	
	/**
	 * Handles a notification from the remote statement
	 * @param notif The notification
	 * @param handback The notification handback
	 */
	public void handleNotification(Notification notif, Object handback) {
		String type = notif.getType();
		if(type==null) {
			err("Null notification type");
		}
		Map<String, String> stMap = null;
		String name = null;
		if(type.startsWith(STATEMENT_LIFECYCLE)) {
			stMap = (Map<String, String>)notif.getUserData();
			name = stMap.get(STATEMENT_NAME);
		}
		
		if(metricsObjectName.get()==null) {
			ConsoleOps.notif("Following event is expired since there is no statement created");
		}
		if(STATEMENT_EVENT.equals(type)) {
			//data(notif.getUserData());
			for(StatementListener l: listeners) {
				Object[] events = ((Set<Object>)notif.getUserData()).toArray(new Object[0]);
				l.onInEvents(notif.getMessage(), events); 
			}
		} else if(STATEMENT_CREATED.equals(type)) {
			notif("Statement Created [", name, "]");
			for(StatementListener l: listeners) { l.onCreate(name); }
		} else if(STATEMENT_FAILED.equals(type)) {
			err("Statement Failed [", name, "]");
			for(StatementListener l: listeners) { l.onFail(name); }
		} else if(STATEMENT_DESTROYED.equals(type)) {
			notif("Statement Terminated [", name, "]");
			for(StatementListener l: listeners) { l.onDestroy(name); }
		} else if(STATEMENT_STOPPED.equals(type)) {
			notif("Statement Paused [", name, "]");
			for(StatementListener l: listeners) { l.onStop(name); }
		} else if(STATEMENT_STARTED.equals(type)) {
			notif("Statement Started [", name, "]");
			for(StatementListener l: listeners) { l.onStart(name); }
		} else {
			err("Unrecognized notification type [" + type + "]");
		}		
	}	
	
/*
		Map<String, String> statementMap = new HashMap<String, String>(4);
		EPStatementImpl impl = (EPStatementImpl)statement;
		statementMap.put(STATEMENT_ID, impl.getStatementId());
		statementMap.put(STATEMENT_STATE, impl.getState().name());
		statementMap.put(STATEMENT_TYPE, impl.getStatementMetadata().getStatementType().name());
		statementMap.put(STATEMENT_NAME, impl.getName());
		statementMap.put(STATEMENT_EVENT_TYPE, impl.getEventType().getName());
		statementMap.put(STATEMENT_TEXT, impl.getText());
		statementMap.put(STATEMENT_LAST_CHANGE_TIME, "" + impl.getTimeLastStateChange());

 */

	/**
	 * Executes an on demmand epl query and returns the rednered results.
	 * @param epl the epl on demmand query
	 */
	public Object query(String epl) throws Exception {
		try {
			return esperSession.query(epl);
		} catch (Exception e) {
			throw processException("query", e);
		}
	}
	
	/**
	 * Creates and subscribes to an epl statement which will broadcast the events as JMX notifications.
	 * @param epl the epl statement
	 * @return the statement's metrics ObjectName
	 */
	public ObjectName subscribe(String epl) throws Exception {
		try {
			metricsObjectName.set(esperSession.subscribe(epl));
			metricsProxy.set(MBeanServerInvocationHandler.newProxyInstance(mbeanServer, metricsObjectName.get(), StatementProxy.class, false));
			currentStName = metricsProxy.get().getStatementName();
			currentStText = epl;
			notif("Started subscription [", currentStName, "]");
			return metricsObjectName.get();
		} catch (Exception e) {
			throw processException("subscribe", e);
		}
	}
	
	
	/**
	 * Builds an exception from the passed operation name and default exception. Attempts to retrieve the remote actual exception if available.
	 * @param operation The operation name that failed
	 * @param defaultException The default exception
	 * @return a new Exception wrapping the actual exception or the default.
	 */
	protected Exception processException(String operation, Exception defaultException) {
		Exception serverSideExc = null;
		try{ serverSideExc = getLastException(); } catch (Exception x) {}
		if(serverSideExc!=null) {
			return new Exception("Failed to process request [" + operation + "]", serverSideExc);
		} else {
			return new Exception("Failed to process request [" + operation + "]", defaultException);
		}
		
	}
	

	/**
	 * Creates and subscribes to an epl statement which will broadcast the events as JMX notifications.
	 * @param epl the epl statement
	 * @param epl the epl statement name.
	 * @return the statement's metrics ObjectName
	 */	
	public ObjectName subscribe(String epl, String name) throws Exception {
		try {
			metricsObjectName.set(esperSession.subscribe(epl, name));
			metricsProxy.set(MBeanServerInvocationHandler.newProxyInstance(mbeanServer, metricsObjectName.get(), StatementProxy.class, false));
			currentStName = name;
			currentStText = epl;
			notif("Started subscription [", currentStName, "]");
			return metricsObjectName.get();
		} catch (Exception e) {
			throw processException("subscribe", e);
		}
	}
	
	/**
	 * Cleans up this session when disconnecting
	 */
	public void destroySession() {
		quitting.set(true);
		cleanupStatement();
		try {
			mbeanServer.removeNotificationListener(sessionObjectName, this);
		} catch (Exception e) {}
		listeners.clear();
	}
	
	/**
	 * Cleans up a subscription statement when it is destroyed
	 */
	protected void cleanupStatement() {
		if(metricsProxy.get()!=null) {
			synchronized(metricsProxy) {
				if(metricsProxy.get()!=null) {
					try { esperSession.stopSubscription(); } catch (Exception e) {}
					try { esperSession.destroySubscription(); } catch (Exception e) {}
					metricsProxy.set(null);
					metricsObjectName.set(null);
				}
			}
		}		
	}
	
	/**
	 * Stops the currently running statement
	 */
	public void stopSubscription() {
		if(this.metricsProxy.get()!=null) {
			String state = metricsProxy.get().getState();
			if(EPStatementState.STARTED.name().equals(state)) {
				try {
					esperSession.stopSubscription();
				} catch (Exception e) {
					throw new RuntimeException(processException("stop", e));
				}
			} else {
				ConsoleOps.err("Subscription is not started so cannot be stopped [==>", state, "]");
			}
		} else {
			if(!quitting.get()) {
				ConsoleOps.err("There is no subscription active to stop");
			}
		}
	}
	
	/**
	 * Destroys the currently registered statement
	 */
	public void destroySubscription() {
		if(this.metricsProxy.get()!=null) {
			cleanupStatement();
		} else {
			if(!quitting.get()) {
				ConsoleOps.err("There is no subscription to destroy");
			}
		}		
	}
	
	/**
	 * Restarts a stopped subscription
	 */
	public void startSubscription() {
		if(this.metricsProxy.get()!=null) {
			String state = metricsProxy.get().getState();
			if(EPStatementState.STOPPED.name().equals(state)) {
				esperSession.startSubscription();
			} else {
				ConsoleOps.err("Subscription is not stopped so cannot be started [==>", state, "]");
			}
		} else {
			ConsoleOps.err("There is no subscription to start");
		}				
	}
	
	
	/**
	 * Determines if there is a statement is currently registered
	 * @return true if a statement is currently registered, false if not
	 */
	public boolean isStatementRegistered() {
		return this.metricsProxy.get()!=null; 
	}
	
	
	
	/**
	 * Retrieves the value of an option
	 * @param name the option name
	 * @return the option value or null if the name is not bound
	 */
	public Object getOption( String name) {
		return esperSession.getOption(name);
	}

	/**
	 * Returns an array of the bound option names
	 * @return an array of the bound option names
	 */
	public String[] getOptionNames() {
		return esperSession.getOptionNames();
	}

	/**
	 * Returns the option map
	 * @return the option map
	 */
	public Map<String, Object> getOptions() {
		return esperSession.getOptions();
	}

	/**
	 * Sets an option value
	 * @param name The option name
	 * @param value The option value
	 */
	public void setOption(String name, Object value) {
		esperSession.setOption(name, value);
	}
	
	/**
	 * Returns all the registered statement names
	 * @return all the registered statement names
	 */
	public String[] getStatementNames() {
		return esperSession.getStatementNames();
	}
	
	/**
	 * Returns all the registered SELECT statement names
	 * @return all the registered SELECT statement names
	 */
	public String[] getSelectStatementNames() {
		return esperSession.getSelectStatementNames();
	}
	
	
	/**
	 * Returns all the registered statement names that have a type that matches the passed regex
	 * @param typeMatch
	 * @return all the registered statement names that have a type that matches the passed regex
	 */
	public String[] getStatementNames(String typeMatch) {
		return esperSession.getStatementNames(typeMatch);
	}
	
	/**
	 * Returns the value of the named variable
	 * @param name the variable name
	 * @return the variable value
	 */
	public Object getVariableValue(String name) {
		return esperSession.getVariableValue(name);
	}
	
	/**
	 * Sets the value of the named variable
	 * @param name the name of the variable
	 * @param value the value of the variable
	 */
	public void setVariableValue(String name, Object value) {
		esperSession.setVariableValue(name, value);
	}
	
	/**
	 * Returns all the variables name declared.
	 * @return an array of variable names
	 */
	public String[] getVariableNames() {
		return esperSession.getVariableNames();
	}
	

	/**
	 * Adds a listener to the proxy
	 * @param listener the listener to add
	 */
	public void addListener(StatementListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Removes a listener from the proxy
	 * @param listener the listener to remove
	 */
	public void removeListener(StatementListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}
	}

	/**
	 * Returns the CPU time in ns. consumed by this statement.
	 * @return the CPU time in ns. consumed by this statement.
	 * @see com.espertech.esper.client.metric.StatementMetric#getCpuTime()
	 */
	public long getCpuTime() {
		return metricsProxy.get().getCpuTime();
	}
	
	
	/**
	 * Returns the statement event type name
	 * @return the statement event type name
	 */
	public String getEventTypeName() {
		return metricsProxy.get().getEventTypeName();
	}
	
	/**
	 * Returns the statement event type
	 * @return the statement event type
	 */
	public EventType getEventType() {
		return metricsProxy.get().getEventType();
	}
		
	
	/**
	 * Returns the statement property names
	 * @return the statement property names
	 */
	public String[] getPropertyNames() {
		return metricsProxy.get().getPropertyNames();
	}
	
	/**
	 * Returns the timetsamp of the last statement change
	 * @return the timetsamp of the last statement change
	 */
	public long getLastTimeChange() {
		return metricsProxy.get().getLastTimeChange();
	}
	
	
	/**
	 * Returns the state of the statement.
	 * @return the state of the statement.
	 */
	public String getState() {
		return metricsProxy.get().getState();
	}
	
	/**
	 * Returns the engine URI
	 * @return the engine URI
	 * @see com.espertech.esper.client.metric.MetricEvent#getEngineURI()
	 */
	public String getEngineURI() {
		return metricsProxy.get().getEngineURI();
	}

	/**
	 * Returns the number of output rows in insert stream.
	 * @return the number of output rows in insert stream.
	 * @see com.espertech.esper.client.metric.StatementMetric#getNumOutputIStream()
	 */
	public long getNumOutputIStream() {
		return metricsProxy.get().getNumOutputIStream();
	}

	/**
	 * Returns the number of output rows in remove stream.
	 * @return the number of output rows in remove stream.
	 * @see com.espertech.esper.client.metric.StatementMetric#getNumOutputRStream()
	 */
	public long getNumOutputRStream() {
		return metricsProxy.get().getNumOutputRStream();
	}

	/**
	 * Returns the statement name
	 * @return the statement name
	 * @see com.espertech.esper.client.metric.StatementMetric#getStatementName()
	 */
	public String getStatementName() {
		return metricsProxy.get().getStatementName();
	}

	/**
	 * Returns the engine timestamp.
	 * @return the engine timestamp.
	 * @see com.espertech.esper.client.metric.StatementMetric#getTimestamp()
	 */
	public long getTimestamp() {
		return metricsProxy.get().getTimestamp();
	}

	/**
	 * Returns the statement wall time in nanoseconds. 
	 * @return the statement wall time in nanoseconds.
	 * @see com.espertech.esper.client.metric.StatementMetric#getWallTime()
	 */
	public long getWallTime() {
		return metricsProxy.get().getWallTime();
	}

	/**
	 * Returns the statement text
	 * @return the statementText
	 */
	public String getStatementText() {
		return metricsProxy.get().getStatementText();
	}

	/**
	 * Returns the number of listeners registered
	 * @return the number of listeners registered
	 */
	public int getListenerCount() {
		return metricsProxy.get().getListenerCount();
	}
	
	/**
	 * Returns the number of statement aware listeners registered
	 * @return the number of statement aware listeners registered
	 */
	public int getStatementAwareListenerCount() {
		return metricsProxy.get().getStatementAwareListenerCount();
	}
	
	/**
	 * Returns the statement type
	 * @return the statement type
	 */
	public String getStatementType() {
		return metricsProxy.get().getStatementType();
	}

	/**
	 * @return the currentStName
	 */
	public String getCurrentStName() {
		return currentStName;
	}

	/**
	 * @return the currentStText
	 */
	public String getCurrentStText() {
		return currentStText;
	}
	
	/**
	 * Returns the last exception thrown from a remote invocation
	 * @return the last exception thrown from a remote invocation
	 */

	public Exception getLastException() {
		return esperSession.getLastException();
	}


}
