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

import com.espertech.esper.client.EventBean;

/**
 * <p>Title: StatementListener</p>
 * <p>Description: Defines a cl side listener registered with the <code>StatementProxy</code> that handles
 * data and lifecycle events from the remote statement.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.StatementListener</code></p>
 */

public interface StatementListener {
	/**
	 * Called when a statement is created
	 * @param statementName The name of the statement
	 */
	public void onCreate(String statementName);
	
	/**
	 * Called when a statement starts or restarts
	 * @param statementName The name of the statement
	 */
	public void onStart(String statementName);
	/**
	 * Called when a statement stops
	 * @param statementName The name of the statement
	 */
	public void onStop(String statementName);
	/**
	 * Called when a statement is destroyed
	 * @param statementName The name of the statement
	 */	
	public void onDestroy(String statementName);
	/**
	 * Called when a statement reports a failure state
	 * @param statementName The name of the statement
	 */	
	public void onFail(String statementName);
	/**
	 * Called when the statement returns events entering the event stream
	 * @param statementName The name of the statement
	 * @param events An array of events entering the event stream
	 */
	public void onInEvents(String statementName, Object...events);
	/**
	 * Called when the statement returns events leaving the event stream
	 * @param statementName The name of the statement
	 * @param events An array of events leaving the event stream
	 */
	public void onOutEvents(String statementName, EventBean...events);
}
