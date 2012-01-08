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

import java.rmi.RemoteException;
import java.util.Map;

import javax.management.ObjectName;




/**
 * <p>Title: EsperSession</p>
 * <p>Description: Defines the remote interface to the Esper client session proxy. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.EsperSession</code></p>
 */

public interface EsperSession {
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
	/** JMX notification type prefix for statement lifecycle events */
	public static final String STATEMENT_LIFECYCLE = "org.helios.esper.statement.state";
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
	
	
	/*
	 * TODO: local clean up
	 * TODO: remote cleanup
	 * TODO: stop, restart and destroy statements
	 * TODO: expose current statement name to client
	 * TODO: suppress statement operations if statement is null.
	 * TODO: Queue for output events
	 * TODO: Options impl.
	 */

	/**
	 * Executes an on demmand epl query and returns the rednered results.
	 * @param epl the epl on demmand query
	 */
	public Object query(String epl) throws Exception;
	
	/**
	 * Creates and subscribes to an epl statement which will broadcast the events as JMX notifications.
	 * @param epl the epl statement
	 * @return the statement's metrics ObjectName
	 */
	public ObjectName subscribe(String epl) throws Exception;

	/**
	 * Creates and subscribes to an epl statement which will broadcast the events as JMX notifications.
	 * @param epl the epl statement
	 * @param epl the epl statement name.
	 * @return the statement's metrics ObjectName
	 */	
	public ObjectName subscribe(String epl, String name) throws Exception;
	
	/**
	 * Retrieves the value of an option
	 * @param name the option name
	 * @return the option value or null if the name is not bound
	 */
	public Object getOption( String name);

	/**
	 * Returns an array of the bound option names
	 * @return an array of the bound option names
	 */
	public String[] getOptionNames();

	/**
	 * Returns the option map
	 * @return the option map
	 */
	public Map<String, Object> getOptions();

	/**
	 * Sets an option value
	 * @param name The option name
	 * @param value The option value
	 */
	public void setOption(String name, Object value);
	
	/**
	 * Returns all the registered statement names
	 * @return all the registered statement names
	 */
	public String[] getStatementNames();
	
	/**
	 * Returns all the registered SELECT statement names
	 * @return all the registered SELECT statement names
	 */
	public String[] getSelectStatementNames();
	
	
	/**
	 * Returns all the registered statement names that have a type that matches the passed regex
	 * @param typeMatch
	 * @return all the registered statement names that have a type that matches the passed regex
	 */
	public String[] getStatementNames(String typeMatch);
	
	/**
	 * Returns the value of the named variable
	 * @param name the variable name
	 * @return the variable value
	 */
	public Object getVariableValue(String name);
	
	/**
	 * Sets the value of the named variable
	 * @param name the name of the variable
	 * @param value the value of the variable
	 */
	public void setVariableValue(String name, Object value);
	
	/**
	 * Returns all the variables name declared.
	 * @return an array of variable names
	 */
	public String[] getVariableNames();	
	
	/**
	 * Stops the currently running statement
	 */
	public void stopSubscription();
	
	/**
	 * Destroys the currently registered statement
	 */
	public void destroySubscription();
	
	/**
	 * Cleans up this session when disconnecting
	 */
	public void destroySession();
	
	/**
	 * Determines if there is a statement is currently registered
	 * @return true if a statement is currently registered, false if not
	 */
	public boolean isStatementRegistered();
	
	/**
	 * Restarts a stopped subscription
	 */
	public void startSubscription();
	
	/**
	 * Returns the last exception thrown from a remote invocation
	 * @return the last exception thrown from a remote invocation
	 */

	public Exception getLastException();
	
	
	
}
