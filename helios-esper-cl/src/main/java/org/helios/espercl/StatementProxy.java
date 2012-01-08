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

import com.espertech.esper.client.EventType;


/**
 * <p>Title: StatementProxy</p>
 * <p>Description: An interface to provide an MBeanServerInvocation handler to an Esper statement MBean</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.StatementProxy</code></p>
 */

public interface StatementProxy {
	/**
	 * Returns the CPU time in ns. consumed by this statement.
	 * @return the CPU time in ns. consumed by this statement.
	 * @see com.espertech.esper.client.metric.StatementMetric#getCpuTime()
	 */
	public long getCpuTime();
	
	/**
	 * Returns the statement event type name
	 * @return the statement event type name
	 */
	public String getEventTypeName();
	
	/**
	 * Returns the statement event type
	 * @return the statement event type
	 */
	public EventType getEventType();
	
	
	/**
	 * Returns the statement property names
	 * @return the statement property names
	 */
	public String[] getPropertyNames();
	
	/**
	 * Returns the timetsamp of the last statement change
	 * @return the timetsamp of the last statement change
	 */
	public long getLastTimeChange();
	
	
	/**
	 * Returns the state of the statement.
	 * @return the state of the statement.
	 */
	public String getState();
	
	/**
	 * Returns the engine URI
	 * @return the engine URI
	 * @see com.espertech.esper.client.metric.MetricEvent#getEngineURI()
	 */
	public String getEngineURI();

	/**
	 * Returns the number of output rows in insert stream.
	 * @return the number of output rows in insert stream.
	 * @see com.espertech.esper.client.metric.StatementMetric#getNumOutputIStream()
	 */
	public long getNumOutputIStream();

	/**
	 * Returns the number of output rows in remove stream.
	 * @return the number of output rows in remove stream.
	 * @see com.espertech.esper.client.metric.StatementMetric#getNumOutputRStream()
	 */
	public long getNumOutputRStream();

	/**
	 * Returns the statement name
	 * @return the statement name
	 * @see com.espertech.esper.client.metric.StatementMetric#getStatementName()
	 */
	public String getStatementName();

	/**
	 * Returns the engine timestamp.
	 * @return the engine timestamp.
	 * @see com.espertech.esper.client.metric.StatementMetric#getTimestamp()
	 */
	public long getTimestamp();

	/**
	 * Returns the statement wall time in nanoseconds. 
	 * @return the statement wall time in nanoseconds.
	 * @see com.espertech.esper.client.metric.StatementMetric#getWallTime()
	 */
	public long getWallTime();

	/**
	 * Returns the statement text
	 * @return the statementText
	 */
	public String getStatementText();

	/**
	 * Returns the number of listeners registered
	 * @return the number of listeners registered
	 */
	public int getListenerCount();
	
	/**
	 * Returns the number of statement aware listeners registered
	 * @return the number of statement aware listeners registered
	 */
	public int getStatementAwareListenerCount();
	
	/**
	 * Returns the statement type
	 * @return the statement type
	 */
	public String getStatementType();

}
