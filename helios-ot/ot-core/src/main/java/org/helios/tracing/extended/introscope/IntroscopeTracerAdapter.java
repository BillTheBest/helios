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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.helios.aop.ClassDefinition;
import org.helios.aop.ConstructorDefinition;
import org.helios.aop.FieldDefinition;
import org.helios.aop.HModifier;
import org.helios.aop.MethodDefinition;
import org.helios.aop.ReturnTypeDefinition;

/**
 * <p>Title: IntroscopeTracerAdapter</p>
 * <p>Description: Interface defining the tracing operation set of the CA/Wily Introscope Data API version 5.0. 
 * The <code>IntroscopeTracerFactory</code> dynamically creates a class that implements this interface to
 * avoid a compile time dependency on Introscope libraries without the performance hit of using reflection. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1058 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/extended/introscope/IntroscopeTracerAdapter.java $
 * $Id: IntroscopeTracerAdapter.java 1058 2009-02-18 17:33:54Z nwhitehead $
 */
//@SourceAttachment (src="/aoptracermapping/*", urlprop=IntroscopeTracerAdapter.URL_PROP)
@ClassDefinition(
		imports={"com.wily.introscope.agent.api", "com.wily.introscope.agent", "com.wily.introscope.agent.connection"},
		fields={@FieldDefinition(
					name="agent", 
					typeName="com.wily.introscope.agent.IAgent",
					modifiers={HModifier.PRIVATE}
					//,initializer="null;"
					,initializer="com.wily.introscope.agent.AgentShim.getAgent();"
				),
				@FieldDefinition(
					name="connection", 
					typeName="com.wily.introscope.agent.connection.IsengardServerConnectionManager",
					modifiers={HModifier.PRIVATE},
					initializer="null;"
				)				
		},
		interfaces={"com.wily.introscope.agent.connection.IServerConnectionNotification"}
)
public abstract class IntroscopeTracerAdapter implements IntroscopeAdapter {
	// public abstract com.wily.introscope.agent.connection.IsengardServerConnectionManager com.wily.introscope.agent.IAgent.IAgent_getIsengardServerConnection()
	/** The key of the system property to get the source node URL for this class */
	public static final String URL_PROP = "helios.traceraop.mapping.introscope";
	protected final Logger LOG = Logger.getLogger(getClass());
	/** A set of agent connection state listeners */
	protected final Set<IntroscopeAgentConnectionListener> listeners = new CopyOnWriteArraySet<IntroscopeAgentConnectionListener>();
	/** Connection indicator */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** Connection wait lock */
	protected volatile CountDownLatch connectWaitLatch = null; 
	/** Connection wait lock create guard*/
	protected final Object connectWaitLock = new Object(); 
	
	/** The configuration prefix */
	public static final String CONFIG_PREFIX = IntroscopeTracerAdapter.class.getPackage().getName() + ".adapter.";
	
	/**
	 * Called when the agent connects
	 */
	public void connectionUp() {
		LOG.info("Connection Is Up");
		connected.set(true);
		if(connectWaitLatch!=null) {
			connectWaitLatch.countDown();
			connectWaitLatch=null;
		}
		for(IntroscopeAgentConnectionListener listener: listeners) {
			listener.connectionUp();
		}		
	}
		
	/**
	 * Called when the agent disconnects
	 */
	public void connectionDown() {
		LOG.info("Connection Is Down");
		connected.set(false);
		for(IntroscopeAgentConnectionListener listener: listeners) {
			listener.connectionDown();
		}
	}
	
	/**
	 * Adds an Introscope Agent Connection Statsu Listener
	 * @param listener the listener to add
	 */
	public void addConnectionListener(IntroscopeAgentConnectionListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Removes an Introscope Agent Connection Statsu Listener
	 * @param listener the listener to remove
	 */
	public void removeConnectionListener(IntroscopeAgentConnectionListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}
	}
	
	
	@ConstructorDefinition()
	public IntroscopeTracerAdapter() {	
		setAgent();
		final IntroscopeTracerAdapter ad = this;
		Thread t = new Thread("IntroscopeAgent Connection Waiter") {			
			public void run() {
				log("Starting Conn Getter Thread for [" + ad + "]");
				try {
					ad.getClass().getMethod("setConnection").invoke(ad);
					while(ad.getClass().getMethod("getConnection").invoke(ad)==null) {
						log("Calling setConnection");
						ad.getClass().getMethod("setConnection").invoke(ad);
						if(ad.getClass().getMethod("getConnection").invoke(ad)!=null) {
							log("Connection Set. Breaking.");
							break;
						}
						log("Sleeping");
						try { Thread.currentThread().join(500);} catch (Exception e) {}
						ad.getClass().getMethod("setConnection").invoke(ad);
					}
					log("Acquired Connection [" + ad.getClass().getMethod("getConnection").invoke(ad)+ "]. Exiting Conn Getter Thread.");
					ad.registerConnectionListener();
					//getConnection().getClass().getMethod("addConnectionObserver", Class.forName("com.wily.introscope.agent.connection.IServerConnectionNotification")).invoke(getConnection(), ad);
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		};
		t.setDaemon(false);
		t.start();
	}
	
	public static void log(Object msg) {
		System.out.println("[" + Thread.currentThread().getName() + "]" + msg);
	}

	@MethodDefinition(
			bsrc="$0.connection.connect();"
	)
	public abstract void connect();
	
	/**
	 * Directs the underlying agent to connect and waits for the connection confirm.
	 * @param timeout The timeout on the connect wait
	 * @param unit the timeout unit
	 * @return true if the connect confirm was received, false if the request timed out.
	 * @throws InterruptedException
	 */
	public boolean connectWithWait(long timeout, TimeUnit unit) throws InterruptedException {
		if(!connected.get()) {
			if(connectWaitLatch==null) {
				synchronized(connectWaitLock) {
					if(connected.get()) return true;
					if(connectWaitLatch==null) {
						connectWaitLatch = new CountDownLatch(1);
					}
				}
			}
			if(!connected.get()) {
				return connectWaitLatch.await(timeout, unit);				
			}			
		}
		return true;
	}
	
	@MethodDefinition(
			bsrc="$0.connection.disconnect();"
	)
	public abstract void disconnect();

	
	/**
	 * Returns a string array containing:<ul>
	 * <li>[0]: Host Name</li>
	 * <li>[1]: Process Name</li>
	 * <li>[2]: Agent Name</li>
	 * </ul>
	 * @return a string array containing the Host, Process, Agent triplet
	 */	
	@MethodDefinition(
			bsrc="return $0.agent.IAgent_getHostProcessAgentTriplet();"
	)	
	public abstract String[] getHostProcessAgent();
	
	/**
	 * Returns the agent's host
	 * @return the agent's host
	 */
	public String getHost() {
		return getHostProcessAgent()[0];
	}
	
	/**
	 * Returns the agent's process
	 * @return the agent's process
	 */
	public String getProcess() {
		return getHostProcessAgent()[1];
	}
	
	/**
	 * Returns the agent's name
	 * @return the agent's name
	 */
	public String getName() {
		return getHostProcessAgent()[2];
	}
	
	

	@MethodDefinition(
			//bsrc="$0.connection = getAgent().IAgent_getIsengardServerConnection();"
			bsrc="{LOG.info(\"Setting Agent on [\" +  $0 + \"]:\" + com.wily.introscope.agent.AgentShim.getAgent()); $0.agent = com.wily.introscope.agent.AgentShim.getAgent(); LOG.info(\"Agent Set To:\" + $0.agent);}"
	)
	public abstract void setAgent();
	
	@MethodDefinition(
			bsrc="return connection.isConnected();",
			modifiers={HModifier.NOT_ABSTRACT}
	)
	protected abstract boolean _isAgentConnected();
	
	public boolean isAgentConnected() {
		if(getConnection()==null) return false;
		else return _isAgentConnected();
	}
	
	
//	@MethodDefinition(fieldGetter="agent", modifiers={HModifier.NOT_ABSTRACT})
	@MethodDefinition(
			//retType=@ReturnTypeDefinition(typeName="com.wily.introscope.agent.IAgent"),
			retType=@ReturnTypeDefinition(type=Object.class),
			bsrc="return $0.agent;",
			modifiers={HModifier.NOT_ABSTRACT},
			replace=true
	)
	public abstract Object getAgent();
//	{
//		return null;
//	}
	
	@MethodDefinition(
			//bsrc="$0.connection = getAgent().IAgent_getIsengardServerConnection();"
			bsrc="{LOG.info(\"Getting Connection From Agent:\" + this.agent); connection = agent.IAgent_getIsengardServerConnection();}"
	)
	public abstract void setConnection();
	
	
//	@MethodDefinition(fieldGetter="connection", modifiers={HModifier.NOT_ABSTRACT})
	@MethodDefinition(
			//retType=@ReturnTypeDefinition(typeName="com.wily.introscope.agent.IAgent"),
			retType=@ReturnTypeDefinition(type=Object.class),
			bsrc="return $0.connection;",
			modifiers={HModifier.NOT_ABSTRACT},
			replace=true
	)
	
	public abstract Object getConnection();
	
	@MethodDefinition(
			bsrc="$0.connection.addConnectionObserver($0);"
	)
	protected abstract void registerConnectionListener();
	
	
	/**
	 * Records a single data point and rolls it into the current average.
	 * An IntAverageDataRecorder represents a data source which generates periodic values suitable for averaging over a short time period (in the seconds timeframe). The IntAverageDataRecorder would be appropriate for something which measures response times. 
	 * @param metricName The fully qualified metric name.
	 * @param value A single data value
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createIntAverageDataRecorder($1).recordDataPoint($2);")
	public abstract void recordDataPoint(String metricName, int value);
	/**
	 * Adds the given delta to the current value.
	 * An IntCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an IntCounterDataRecorder. Counters are always positive. The counter will reset if Integer.MAX_VALUE is exceeded. 
	 * @param metricName The fully qualified metric name.
	 * @param value amount to add to the current value
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createIntCounterDataRecorder($1).add($2);")
	public abstract void addDataPoint(String metricName, int value);	
	/**
	 * Subtracts the given delta from the current value.
	 * An IntCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an IntCounterDataRecorder. Counters are always positive. The counter will reset if Integer.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param value amount to subtract from the current value
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createIntCounterDataRecorder($1).subtract($2);")
	public abstract void subtractDataPoint(String metricName, int value);
	/**
	 * Records the current data value.
	 * An IntCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an IntCounterDataRecorder. Counters are always positive. The counter will reset if Integer.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param value The current data value
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createIntCounterDataRecorder($1).recordCurrentValue($2);")
	public abstract void recordCurrentValue(String metricName, int value);
	
	
	
	/**
	 * Records a single data point and rolls it into the current average.
	 * A LongAverageDataRecorder represents a data source which generates periodic values suitable for averaging over a short time period (in the seconds timeframe). The LongAverageDataRecorder would be appropriate for something which measures response times. 
	 * @param metricName The fully qualified metric name.
	 * @param value A single data value
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createLongAverageDataRecorder($1).recordDataPoint($2);")
	public abstract void recordDataPoint(String metricName, long value);
	/**
	 * Adds the given delta to the current value.
	 * A LongCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an LongCounterDataRecorder (if the temperature was too big to fit into an int). Counters are always positive. The counter will reset if Long.MAX_VALUE is exceeded. 
	 * @param metricName The fully qualified metric name.
	 * @param value amount to add to the current value
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createLongCounterDataRecorder($1).add($2);")
	public abstract void addDataPoint(String metricName, long value);
	/**
	 * Subtracts the given delta from the current value.
	 * A LongCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an LongCounterDataRecorder (if the temperature was too big to fit into an int). Counters are always positive. The counter will reset if Long.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param value amount to subtract from the current value
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createLongCounterDataRecorder($1).subtract($2);")
	public abstract void subtractDataPoint(String metricName, long value);
	/**
	 * Records the current data value.
	 * A LongCounterDataRecorder represents a data source for which the most-recent value is the most important. For example, a temperature sensor would be well-suited to an LongCounterDataRecorder (if the temperature was too big to fit into an int). Counters are always positive. The counter will reset if Long.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param value The current data value
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createLongCounterDataRecorder($1).recordCurrentValue($2);")
	public abstract void recordCurrentValue(String metricName, long value);
	
	
	/**
	 * Records a single incident.
	 * A RateDataRecorder represents a data source which generates values based on time. The RateDataRecorder would be appropriate for something which measures responses per second. The incident counter will reset if Integer.MAX_VALUE is exceeded. 
	 * @param metricName The fully qualified metric name.
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createIntRateDataRecorder($1).recordIncident();")
	public abstract void recordIncident(String metricName);
	/**
	 * Record multiple incidents. The number of incidents must be positive.
	 * A RateDataRecorder represents a data source which generates values based on time. The RateDataRecorder would be appropriate for something which measures responses per second. The incident counter will reset if Integer.MAX_VALUE is exceeded.  
	 * @param metricName The fully qualified metric name.
	 * @param incidentCount The number of incidents.
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createIntRateDataRecorder($1).recordMultipleIncidents($2);")
	public abstract void recordIncident(String metricName, int incidentCount);
	/**
	 * Records a single incident.
	 * A PerIntervalCounterDataRecorder represents a data source for which the value within a sampling interval is important. Used to count number of incidents per sampling interval. 
	 * @param metricName The fully qualified metric name.
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createPerIntervalCounterDataRecorder($1).recordIncident(); ")
	public abstract void recordIntervalIncident(String metricName);
	/**
	 * Record multiple incidents. The number of incidents must be positive. 
	 * A PerIntervalCounterDataRecorder represents a data source for which the value within a sampling interval is important. Used to count number of incidents per sampling interval. 
	 * @param metricName The fully qualified metric name.
	 * @param incidentCount The number of incidents.
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createPerIntervalCounterDataRecorder($1).recordMultipleIncidents($2);")
	public abstract void recordIntervalIncident(String metricName, int incidentCount);
	
	/**
	 * Records a single String data point.
	 * A StringEventDataRecorder represents a log-like or stream-like data source which periodically generates Strings. This recorder does not have a notion of current value; it merely reports events in the order in which they are reported to it. 
	 * @param metricName The fully qualified metric name.
	 * @param value A String. If null, nothing will be recorded.
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createStringEventDataRecorder($1).recordDataPoint($2);")
	public abstract void recordDataPoint(String metricName, String value);
	/**
	 * Records a single timestamp.
	 * A TimestampDataRecorder represents a data source which generates successively increasin timestamps. The TimestampDataRecorder would be appropriate for something which reports date and time. 
	 * @param metricName The fully qualified metric name.
	 * @param value The timestamp to record in the form of a <code>java.util.Date</code>.
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createTimestampDataRecorder($1).recordTimestamp($2.getTime());")
	public abstract void recordDataPoint(String metricName, Date value);
	/**
	 * Records a single timestamp.
	 * A TimestampDataRecorder represents a data source which generates successively increasin timestamps. The TimestampDataRecorder would be appropriate for something which reports date and time. 
	 * @param metricName The fully qualified metric name.
	 * @param value The timestamp to record in the form of a long.
	 */
	@MethodDefinition(bsrc="DataRecorderFactory.createTimestampDataRecorder($1).recordTimestamp($2);")
	public abstract void recordTimeStamp(String metricName, long value);

	/**
	 * Indicates if the underlying agent is connected
	 * @return true if the underlying agent is connected
	 */
	public boolean getConnected() {
		return connected.get();
	}
	
	
	
	/*
	 * Deferring implementation of these management methods.
	 * The issue that needs to be resolved is that there is currently
	 * no way to determine the metric type, and therefore the DataRecorder type
	 * from the metric name.
	 * This will be fixed by exposing the Hashtable containg the DataRecorders
	 * so that a generically typed DataRecorder can be retrieved by name.
	 
	public boolean isShutoff(String metricName);
	public abstract void setShutoffState(String metricName, boolean isShutoff);
	public abstract void reinitialize(String metricName);
	
	*/
	
	
	
}


/**
<aoptracermapping name="Introscope">
	<!-- 
			Integer Recording Operations
	 -->
	<recordDataPointInt>
		DataRecorderFactory.createIntAverageDataRecorder($1).recordDataPoint($2);  
	</recordDataPointInt>
	<addDataPointInt>
		DataRecorderFactory.createIntCounterDataRecorder($1).add($2);  
	</addDataPointInt>
	<subtractDataPointInt>
		DataRecorderFactory.createIntCounterDataRecorder($1).subtract($2);  
	</subtractDataPointInt>
	<recordCurentDataPointInt>
		DataRecorderFactory.createIntCounterDataRecorder($1).recordCurrentValue($2);  
	</recordCurentDataPointInt>
	<!-- 
			Long Recording Operations
	 -->
	<recordDataPointLong>
		DataRecorderFactory.createLongAverageDataRecorder($1).recordDataPoint($2);  
	</recordDataPointLong>
	<addDataPointLong>
		DataRecorderFactory.createLongCounterDataRecorder($1).add($2);  
	</addDataPointLong>
	<subtractDataPointLong>
		DataRecorderFactory.createLongCounterDataRecorder($1).subtract($2);  
	</subtractDataPointLong>
	<recordCurentDataPointLong>
		DataRecorderFactory.createLongCounterDataRecorder($1).recordCurrentValue($2);  
	</recordCurentDataPointLong>
	<!-- 
			Incident Recording Operations
	 -->
	<recordIncident>
		DataRecorderFactory.createIntRateDataRecorder($1).recordIncident();  
	</recordIncident>
	<recordIncidents>
		DataRecorderFactory.createIntRateDataRecorder($1).recordMultipleIncidents($2);  
	</recordIncidents>
	<recordIntervalIncident>
		DataRecorderFactory.createPerIntervalCounterDataRecorder($1).recordIncident();  
	</recordIntervalIncident>
	<recordIntervalIncidents>
		DataRecorderFactory.createPerIntervalCounterDataRecorder($1).recordMultipleIncidents($2);  
	</recordIntervalIncidents>
	<!-- 
			Misc. Recording Options
	 -->
	<recordString>
		DataRecorderFactory.createStringEventDataRecorder($1).recordDataPoint($2);  
	</recordString>
	<recordDate>
		DataRecorderFactory.createTimestampDataRecorder($1).recordTimestamp($2.getTime());  
	</recordDate>
	<recordTimestamp>
		DataRecorderFactory.createTimestampDataRecorder($1).recordTimestamp($2);  
	</recordTimestamp>
	
</aoptracermapping>


*/