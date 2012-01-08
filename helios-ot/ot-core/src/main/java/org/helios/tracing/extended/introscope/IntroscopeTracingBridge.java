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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.helios.aop.DynaClassFactory;
import org.helios.cache.softref.SoftReferenceCache;
import org.helios.cache.softref.SoftReferenceCacheService;
import org.helios.helpers.URLHelper;
import org.helios.tracing.bridge.AbstractStatefulTracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.MetricId;
import org.helios.tracing.trace.MetricType;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: IntroscopeTracingBridge</p>
 * <p>Description: TracingBridge for Introscope.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.introscope.IntroscopeTracingBridge</code></p>
 */

@SuppressWarnings("unchecked")
public class IntroscopeTracingBridge extends AbstractStatefulTracingBridge {
	/** The introscope agent adapter instance */
	protected final AtomicReference<IntroscopeAdapter> adapter = new AtomicReference<IntroscopeAdapter>(null);
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(IntroscopeTracingBridge.class);
	/** SoftRef Cache for mapping helios metricIds to created Introscope metric names */
	protected static final SoftReferenceCache<String, String> metricCache =  SoftReferenceCacheService.getInstance().createCache();
	
	/** The IntroscopeAgent classloader */
	protected final ClassLoader agentLoader;
	
	/** The Introscope Agent's reported host name */
	protected static final AtomicReference<String> host = new AtomicReference<String>(null);
	/** The Introscope Agent's reported agent name */
	protected static final AtomicReference<String> agent = new AtomicReference<String>(null);
	

	
	/**
	 * Creates a new IntroscopeTracingBridge 
	 * @param name
	 * @param bufferSize
	 * @param frequency
	 * @param intervalCapable
	 */
	public IntroscopeTracingBridge(IntroscopeAdapterConfiguration config) {
		super(IntroscopeTracingBridge.class.getSimpleName(), 0, 0, true);
		ClassLoader[] cls = null;		
		if(isAgentAccessible()) {
			cls = new ClassLoader[]{};
			agentLoader = Thread.currentThread().getContextClassLoader();
		} else {
			agentLoader = getAgentClassLoader(config.getAgentJarName());
			if(agentLoader==null) {
				LOG.warn("The Introscope Agent is not in the class path and could not be located from the config [" + config.getAgentJarName() + "]. The IntroscopeTracingBridge is inactive");
				return;
			}
			cls = new ClassLoader[]{agentLoader};
		}
		ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(agentLoader);
		try {
			adapter.set((IntroscopeAdapter)DynaClassFactory.generateClassInstance(
					IntroscopeTracerAdapter.class.getPackage().getName() + ".TracerInstance", 
					IntroscopeTracerAdapter.class, cls));
		} finally {
			Thread.currentThread().setContextClassLoader(currentCl);
		}
		LOG.info("Created IntroscopeTracingBridge [" + adapter.get() + "]");
		
	}
	
	
	/**
	 * Determines if the Introscope agent is accessible in the current classpath
	 * @return true if it is, false otherwise
	 */
	public static boolean isAgentAccessible() {
		try {
			Class.forName("com.wily.introscope.agent.IAgent");
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Creates a classloader for the agent at the passed location
	 * @param agentLocation The file name of the agent
	 * @return An Introscope agent jar classloader or null if it could not be created
	 */
	protected ClassLoader getAgentClassLoader(String agentLocation) {
		if(agentLocation==null) return null;
		URL agentUrl = null;
		File agentFile = null;
		if(URLHelper.isValidURL(agentLocation)) {
			try { agentFile =new File(new URL(agentLocation).getFile()); } catch (Exception e) { 
				LOG.warn("Unexpected error creating URL/File from [" + agentLocation + "]", e);
				return null;
			} 
		} else {
			agentFile = new File(agentLocation);
		}
		if(agentFile.exists() && IntroscopeAdapterConfiguration.isWilyJar(agentFile.getAbsolutePath())) {
			try { 
				agentUrl = agentFile.toURI().toURL();
				return new URLClassLoader(new URL[]{agentUrl}, Thread.currentThread().getContextClassLoader());
			} catch (Exception e) {
				LOG.warn("Unexpected error creating URL/File from [" + agentLocation + "]", e);
				return null;
			}
		} else {
			LOG.warn("Failed to resolve Introscope Agent JAR from [" + agentLocation + "]", new Throwable());
			return null;
		}
	}
	
	
	/**
	 * Determines if the passed metricId has the same host/agent name as the activated Introscope agent.
	 * @param metricId the metricId to test
	 * @return true if they match
	 */
	public static boolean isMetricLocal(MetricId metricId) {
		if(metricId==null) throw new IllegalArgumentException("Passed metricId was null", new Throwable());
		return metricId.getHostName().equals(host.get()) && metricId.getAgentName().equals(agent.get());
	}
	
	/**
	 * Transforms the Helios Tracing API metric name arguments into an Introscope metric name.
	 * @param metricName The metric name.
	 * @param prefix The full metric name prefix.
	 * @param nameSpace The full metric name suffix.
	 * @return An introscope metric name.
	 */
	public static String getIntroscopeMetric(MetricId metricId) {
		String metricName = metricCache.get(metricId.getFQN());
		if(metricName==null) {
			synchronized(metricCache) {
				
				metricName = metricCache.get(metricId.getFQN());
				if(metricName==null) {
					StringBuilder buff = new StringBuilder();
					if(!isMetricLocal(metricId)) {
						buff.append(metricId.getHostName()).append("|").append(metricId.getAgentName());
					}
					// ============ HERE =================
					buff.append("|").append(metricId.getAgentName());			
					if(metricId.getNamespace()!=null) {						
						for(String s: metricId.getNamespace()) {
							if(s!=null && s.length() > 0) {
								buff.append("|").append(s);
							}
						}
					}
					if(buff.length() > 0) {
						buff.append(":");
					}
					buff.append(metricId.getMetricName());
					metricName = buff.toString();
					metricCache.put(metricId.getFQN(), metricName);
				}
			}		
		}
		return metricName;
	}
	
	

	/**
	 * Submits a collection of traces to Introscope
	 * @param traces a collection of traces
	 * @TODO: Can we optimize out the switch/case ?
	 */
	@Override
	public void submitTraces(Collection<Trace> traces) {
		IntroscopeAdapter agent = adapter.get();
		if(agent==null) return;
		if(traces!=null && !traces.isEmpty()) {		
			for(Trace trace: traces) {
				String iMetricName = getIntroscopeMetric(trace.getMetricId());
				MetricType type = trace.getMetricType();
				int typeCode = type.getCode();
				String value = trace.getValue().toString();
				switch (typeCode) {
					case MetricType.TYPE_INT_AVG:
						agent.recordDataPoint(iMetricName, Double.valueOf(value).intValue());
						break;
					case MetricType.TYPE_LONG_AVG:
						agent.recordDataPoint(iMetricName, Double.valueOf(value).longValue());
						break;
					case MetricType.TYPE_STICKY_INT_AVG:
						agent.recordCurrentValue(iMetricName, Double.valueOf(value).intValue());
						break;
					case MetricType.TYPE_STICKY_LONG_AVG:
						agent.recordCurrentValue(iMetricName, Double.valueOf(value).longValue());
						break;					
					case MetricType.TYPE_DELTA_INT_AVG:
						agent.recordDataPoint(iMetricName, Double.valueOf(value).intValue());
						break;
					case MetricType.TYPE_DELTA_LONG_AVG:
						agent.recordDataPoint(iMetricName, Double.valueOf(value).longValue());
						break;
					case MetricType.TYPE_STICKY_DELTA_INT_AVG:
						agent.recordCurrentValue(iMetricName, Double.valueOf(value).intValue());
						break;
					case MetricType.TYPE_STICKY_DELTA_LONG_AVG:
						agent.recordCurrentValue(iMetricName, Double.valueOf(value).longValue());
						break;
					case MetricType.TYPE_INTERVAL_INCIDENT:
						agent.recordIntervalIncident(iMetricName, Double.valueOf(value).intValue());	
						break;					
					case MetricType.TYPE_STRING:
						agent.recordDataPoint(iMetricName, value);
						break;
					case MetricType.TYPE_TIMESTAMP:
						agent.recordTimeStamp(iMetricName, Double.valueOf(value).longValue());
						break;
					default:
						if(log.isTraceEnabled()) log.trace("Introscope Tracer Error: Metric Type Code [" + type + "] not recognized for metric:" + iMetricName);
				}
				
			}
		}		
	}

	/**
	 * @param intervalTraces
	 */
	@Override
	public void submitIntervalTraces(Collection<IIntervalTrace> intervalTraces) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Determines if the bridge is connected. Stateless bridges should return true.
	 * @return true if the brige is connected or the bridge is stateless, false if the bridge is not connected.
	 */
	public boolean isConnected() {
		return adapter.get()!=null ? adapter.get().isAgentConnected() : false;
	}	

	/**
	 * Returns the Host/Pr
	 * @return
	 */
	@Override
	public String getEndPointName() {		
		return adapter.get()!=null ? Arrays.toString(adapter.get().getHostProcessAgent()) : null;
	}

	/**
	 * Not used. No queueing for Introscope agent.
	 * @param flushedItems
	 */
	@Override
	public void flushTo(Collection<Trace> flushedItems) {

	}

	/**
	 * 
	 */
	@Override
	protected void doConnect() {
		if(adapter.get()!=null) {
			adapter.get().connect();
		}
	}

	/**
	 * 
	 */
	@Override
	protected void doDisconnect() {
		if(adapter.get()!=null) {
			adapter.get().disconnect();
		}		
	}

	/**
	 * Not used
	 */
	@Override
	public void startReconnectPoll() {
	}

	/**
	 * Not used
	 */
	@Override
	public void stopReconnectPoll() {
		
	}

}
