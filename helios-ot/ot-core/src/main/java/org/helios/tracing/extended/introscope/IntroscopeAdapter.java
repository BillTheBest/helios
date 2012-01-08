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
package org.helios.tracing.extended.introscope;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: IntroscopeAdapter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.introscope.IntroscopeAdapter</code></p>
 */

public interface IntroscopeAdapter {
	
	/**
	 * Returns a string array containing:<ul>
	 * <li>[0]: Host Name</li>
	 * <li>[1]: Process Name</li>
	 * <li>[2]: Agent Name</li>
	 * </ul>
	 * @return a string array containing the Host, Process, Agent triplet
	 */
	public String[] getHostProcessAgent();
	
	/**
	 * Returns the agent's host
	 * @return the agent's host
	 */
	public String getHost();
	
	/**
	 * Returns the agent's process
	 * @return the agent's process
	 */
	public String getProcess();
	
	/**
	 * Returns the agent's name
	 * @return the agent's name
	 */
	public String getName();
	
	
	/**
	 * Called when the agent connects
	 */
	public void connectionUp();

	/**
	 * Called when the agent disconnects
	 */
	public void connectionDown();

	/**
	 * Adds an Introscope Agent Connection Statsu Listener
	 * @param listener the listener to add
	 */
	public void addConnectionListener(
			IntroscopeAgentConnectionListener listener);

	/**
	 * Removes an Introscope Agent Connection Statsu Listener
	 * @param listener the listener to remove
	 */
	public void removeConnectionListener(
			IntroscopeAgentConnectionListener listener);

	public void setAgent();

	public boolean isAgentConnected();

	////@MethodDefinition(fieldGetter="agent")
	//@MethodDefinition(retType = @ReturnTypeDefinition(typeName = "com.wily.introscope.agent.IAgent"), bsrc = "return ($r)$0.agent;"
	public <T>  T getAgent();

	//@MethodDefinition(
	//bsrc="$0.connection = getAgent().IAgent_getIsengardServerConnection();"
	//bsrc = "{LOG.info(\"Getting Connection From Agent:\" + this.agent); connection = agent.IAgent_getIsengardServerConnection();}")
	public void setConnection();

	//@MethodDefinition(fieldGetter = "connection")
	public Object getConnection();

	public void connect();
	
	/**
	 * Directs the underlying agent to connect and waits for the connection confirm.
	 * @param timeout The timeout on the connect wait
	 * @param unit the timeout unit
	 * @return true if the connect confirm was received, false if the request timed out.
	 * @throws InterruptedException
	 */
	public boolean connectWithWait(long timeout, TimeUnit unit) throws InterruptedException;	
	
	public void disconnect();
	
	
	/**
	 * Records a single data point and rolls it into the current average.
	 * An IntAverageDataRecorder represents a data source which generates periodic values suitable for averaging over a short time period (in the seconds timeframe). The IntAverageDataRecorder would be appropriate for something which measures response times. 
	 * @param metricName The fully qualified metric name.
	 * @param value A single data value
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createIntAverageDataRecorder($1).recordDataPoint($2);")
	public void recordDataPoint(String metricName, int value);

	/**
	 * Adds the given delta to the current value.
	 * An IntCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an IntCounterDataRecorder. Counters are always positive. The counter will reset if Integer.MAX_VALUE is exceeded. 
	 * @param metricName The fully qualified metric name.
	 * @param value amount to add to the current value
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createIntCounterDataRecorder($1).add($2);")
	public void addDataPoint(String metricName, int value);

	/**
	 * Subtracts the given delta from the current value.
	 * An IntCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an IntCounterDataRecorder. Counters are always positive. The counter will reset if Integer.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param value amount to subtract from the current value
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createIntCounterDataRecorder($1).subtract($2);")
	public void subtractDataPoint(String metricName, int value);

	/**
	 * Records the current data value.
	 * An IntCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an IntCounterDataRecorder. Counters are always positive. The counter will reset if Integer.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param value The current data value
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createIntCounterDataRecorder($1).recordCurrentValue($2);")
	public void recordCurrentValue(String metricName, int value);

	/**
	 * Records a single data point and rolls it into the current average.
	 * A LongAverageDataRecorder represents a data source which generates periodic values suitable for averaging over a short time period (in the seconds timeframe). The LongAverageDataRecorder would be appropriate for something which measures response times. 
	 * @param metricName The fully qualified metric name.
	 * @param value A single data value
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createLongAverageDataRecorder($1).recordDataPoint($2);")
	public void recordDataPoint(String metricName, long value);

	/**
	 * Adds the given delta to the current value.
	 * A LongCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an LongCounterDataRecorder (if the temperature was too big to fit into an int). Counters are always positive. The counter will reset if Long.MAX_VALUE is exceeded. 
	 * @param metricName The fully qualified metric name.
	 * @param value amount to add to the current value
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createLongCounterDataRecorder($1).add($2);")
	public void addDataPoint(String metricName, long value);

	/**
	 * Subtracts the given delta from the current value.
	 * A LongCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an LongCounterDataRecorder (if the temperature was too big to fit into an int). Counters are always positive. The counter will reset if Long.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param value amount to subtract from the current value
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createLongCounterDataRecorder($1).subtract($2);")
	public void subtractDataPoint(String metricName, long value);

	/**
	 * Records the current data value.
	 * A LongCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an LongCounterDataRecorder (if the temperature was too big to fit into an int). Counters are always positive. The counter will reset if Long.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param value The current data value
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createLongCounterDataRecorder($1).recordCurrentValue($2);")
	public void recordCurrentValue(String metricName, long value);

	/**
	 * Records a single incident.
	 * A RateDataRecorder represents a data source which generates values based on time. The RateDataRecorder would be appropriate for something which measures responses per second. The incident counter will reset if Integer.MAX_VALUE is exceeded. 
	 * @param metricName The fully qualified metric name.
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createIntRateDataRecorder($1).recordIncident();")
	public void recordIncident(String metricName);

	/**
	 * Record multiple incidents. The number of incidents must be positive.
	 * A RateDataRecorder represents a data source which generates values based on time. The RateDataRecorder would be appropriate for something which measures responses per second. The incident counter will reset if Integer.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param incidentCount The number of incidents.
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createIntRateDataRecorder($1).recordMultipleIncidents($2);")
	public void recordIncident(String metricName, int incidentCount);

	/**
	 * Records a single incident.
	 * A PerIntervalCounterDataRecorder represents a data source for which the value within a sampling interval is important. Used to count number of incidents per sampling interval. 
	 * @param metricName The fully qualified metric name.
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createPerIntervalCounterDataRecorder($1).recordIncident(); ")
	public void recordIntervalIncident(String metricName);

	/**
	 * Record multiple incidents. The number of incidents must be positive. 
	 * A PerIntervalCounterDataRecorder represents a data source for which the value within a sampling interval is important. Used to count number of incidents per sampling interval. 
	 * @param metricName The fully qualified metric name.
	 * @param incidentCount The number of incidents.
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createPerIntervalCounterDataRecorder($1).recordMultipleIncidents($2);")
	public void recordIntervalIncident(String metricName,
			int incidentCount);

	/**
	 * Records a single String data point.
	 * A StringEventDataRecorder represents a log-like or stream-like data source which periodically generates Strings. This recorder does not have a notion of current value; it merely reports events in the order in which they are reported to it. 
	 * @param metricName The fully qualified metric name.
	 * @param value A String. If null, nothing will be recorded.
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createStringEventDataRecorder($1).recordDataPoint($2);")
	public void recordDataPoint(String metricName, String value);

	/**
	 * Records a single timestamp.
	 * A TimestampDataRecorder represents a data source which generates successively increasin timestamps. The TimestampDataRecorder would be appropriate for something which reports date and time. 
	 * @param metricName The fully qualified metric name.
	 * @param value The timestamp to record in the form of a <code>java.util.Date</code>.
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createTimestampDataRecorder($1).recordTimestamp($2.getTime());")
	public void recordDataPoint(String metricName, Date value);

	/**
	 * Records a single timestamp.
	 * A TimestampDataRecorder represents a data source which generates successively increasin timestamps. The TimestampDataRecorder would be appropriate for something which reports date and time. 
	 * @param metricName The fully qualified metric name.
	 * @param value The timestamp to record in the form of a long.
	 */
	//@MethodDefinition(bsrc = "DataRecorderFactory.createTimestampDataRecorder($1).recordTimestamp($2);")
	public void recordTimeStamp(String metricName, long value);

}