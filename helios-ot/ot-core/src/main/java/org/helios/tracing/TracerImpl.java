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
package org.helios.tracing;

import static org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture.BLOCK;
import static org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture.CPU;
import static org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture.WAIT;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.containers.ObjectHierarchyTree;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.SystemEnvironmentHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture;
import org.helios.tracing.deltas.DeltaManager;
import org.helios.tracing.lifecycle.TracerLifeCycles;
import org.helios.tracing.subtracer.ITemporalTracer;
import org.helios.tracing.subtracer.TemporalTracer;
import org.helios.tracing.subtracer.VirtualTracer;
import org.helios.tracing.thread.TraceThreadInfoCapture;
import org.helios.tracing.trace.MetricType;
import org.helios.tracing.trace.Trace;
import org.helios.tracing.trace.Trace.Builder;
import org.helios.tracing.userlocator.UserIdLocator;

/**
 * <p>Title: TracerImpl</p>
 * <p>Description: The default main tracer implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.TracerImpl</code></p>
 */
@JMXManagedObject (declared=true, annotated=true)
public class TracerImpl extends ManagedObjectDynamicMBean implements ITracer {
	

	/**  */
	private static final long serialVersionUID = 1901733767886784597L;
	/** The static class hierarchy logger */
	protected static final Logger LOG = Logger.getLogger(TracerImpl.class.getPackage().getName() + ".Tracer");
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** A reference to the JVM's ThreaqdMXBean for thread statistics */
	protected static final ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
	/** Indicates if CPU Timings are enabled for the JVM */
	protected static boolean cpuTimeEnabled = false;
	/** Indicates if CPU Contention Monitoring is enabled for the JVM */
	protected static boolean contentionEnabled = false;
	/** A thread local to hold a StringBuilder for thread safe high speed string appending */
	protected static final ThreadLocal<StringBuilder> buffer = new ThreadLocal<StringBuilder>();
	/** Holds the name of the UserId Locator Class Name */
	protected UserIdLocator userIdLocator = null;
	/** Pattern for parsing trace statements */
	protected Pattern pattern = null;
	/** A reference back to the creating TracingFactory */
	protected final ITracerInstanceFactory tracerFactory;
	/** Indicates if tracer returns traces */
	protected boolean returnTraces = false;
	/** the name of the tracer */
	protected String tracerName = null;
	/** the JMX ObjectName name of the tracer */
	protected ObjectName tracerObjectName = null;
	/** Tracer lifecycle stats management */
	protected static final TracerLifeCycles tracerLifeCycles = new TracerLifeCycles();
	/** Instance reset barrier lock */
	protected Object resetLock = new Object();
	/** The delta processor */
	protected DeltaManager deltaManager = DeltaManager.getInstance();
	
	

	
	
	/** Enabled/Disabled Flags */
	protected static final ObjectHierarchyTree<Boolean> tracingState = new ObjectHierarchyTree<Boolean>("/", "*", false, true);
	/** Average Enabled/Disabled Lookup Time */
	protected static final AtomicLong averageLookupTime = new AtomicLong(0L);
	/** Enabled/Disabled Trip Flag */
	protected final static AtomicBoolean oneReading = new AtomicBoolean(false);

	/** The property name where the jmx default domain is referenced */
	public static final String JMX_DOMAIN_PROPERTY = "helios.opentrace.config.jmx.domain";
	/** The default jmx default domain is referenced */
	public static final String JMX_DOMAIN_DEFAULT = "DefaultDomain";

	
	/** The MBeanServer's default domnain */
	protected final static String defaultDomain = SystemEnvironmentHelper.getSystemPropertyThenEnv(JMX_DOMAIN_PROPERTY, JMX_DOMAIN_DEFAULT);

	
	/** The MBeanServer where the factory is registered */
	protected static final  MBeanServer mbeanServer = JMXHelper.getLocalMBeanServer(defaultDomain);
	
	
	
	
	protected static final String ENABLED_METRIC_PROPERTY_NAME_PREFIX = "helios.opentrace.metric.enablestate.";
	
	
	
	/** testing hook for concurrency tests */
	public AtomicLong ctorCalls = new AtomicLong(0);


	static {
		
		if(tmx.isCurrentThreadCpuTimeSupported()) {
			tmx.setThreadCpuTimeEnabled(true);
			cpuTimeEnabled = true;
		} else {
			cpuTimeEnabled = false;
		}
		
		if(tmx.isThreadContentionMonitoringSupported()) {
			tmx.setThreadContentionMonitoringEnabled(true);
			contentionEnabled = true;
		} else {
			contentionEnabled = false;
		}		
		
		bootStrapEnabledMetricStates();
	}
	
	
	
	/**
	 * Creates a new TracerImpl
	 * @param tracerFactory the tracer's factory
	 */
	public TracerImpl(ITracerInstanceFactory tracerFactory) {		
		this.tracerFactory = tracerFactory;
		tracerName = getDefaultName();
		tracerObjectName = getDefaultObjectName();
		TracerLifeCycles.incrementConstructors(tracerName);
		reflectObject(this);
		TracerFactory.getInstance().reflectObject(tracerLifeCycles);		
	}

	/**
	 * The release version banner.
	 * @return release.
	 */
	@JMXAttribute (name="Version", description="The Helios OpenTrace Package Version.", mutability=AttributeMutabilityOption.READ_ONLY)
	public static String getVersion() {
		return "Helios OpenTrace v. @VERSION@";
	}
	
	/**
	 * Sets the logging level
	 * @param level
	 */
	public void setLogging(String level) {
		org.apache.log4j.Level newlevel = Level.toLevel(level);
		log.setLevel(newlevel);		
	}
	
	/**
	 * Returns the logging level
	 * @return the logging level
	 */
	@JMXAttribute (name="Logging", description="The logging level of this tracer", mutability=AttributeMutabilityOption.READ_WRITE)
	public String getLogging() {
		Level level = log.getLevel();
		if(level==null) level = log.getEffectiveLevel();
		return level.toString();
	}	
	/**
	 * Start a new Trace.Builder
	 * @param metricName The name of the metric.
	 * @param value The value of the metric.
	 * @param metricType The metric type.
	 */	
	public Trace.Builder trace(String metricName, Object value, MetricType metricType) {
		Builder b = new Builder(value, metricType, metricName);
		b.setITracer(this);
		return b;
	}
	/**
	 * Start a new Trace.Builder
	 * @param metricName The name of the metric.
	 * @param value The value of the metric.
	 * @param metricType The metric type.
	 */		
	public Trace.Builder trace(String metricName, Object value, String metricType) {
		Builder b = new Builder(value, MetricType.valueOf(metricType), metricName);
		b.setITracer(this);
		return b;
	}
	/**
	 * Start a new Trace.Builder
	 * @param metricName The name of the metric.
	 * @param value The value of the metric.
	 * @param metricType The metric type.
	 */		
	public Trace.Builder trace(String metricName, Object value, int metricType) {
		Builder b = new Builder(value, MetricType.valueOf(metricType), metricName);
		b.setITracer(this);
		return b;
	}
		

	/**
	 * Returns a virtual tracer wrapped around this tracing instance 
	 * @param host The virtual tracer's host
	 * @param agent The virtual tracer's agent name
	 * @return a virtual tracer
	 */
	public ITracer getVirtualTracer(String host, String agent) {
		return tracerFactory.getVirtualTracer(host, agent);
	}
	
	/**
	 * Creates a temporal tracer that uses this tracer for the mechanics but sets all traces generated to be temporal.
	 * Only one instance of a temporal tracer will be created per underlying tracer. Instances are cached so this method can be called
	 * multiple times on the same tracer and it will return the same temporal tracer instance each time.
	 * @return a temporal tracer.
	 */
	public ITracer getTemporalTracer() {
		return tracerFactory.getTemporalTracer();
	}
	
	
	
	/**
	 * Returns a default tracer name the passed tracer class.
	 * @param the class of the tracer.
	 * @return default tracer name.
	 */
	protected static String getDefaultName(Class<?> clazz) {
		return clazz.getSimpleName();
	}
	
	/**
	 * Returns the default JMX ObjectName for the passed tracer class.
	 * @param the class of the tracer.
	 * @return A JMX ObjectName.
	 */
	protected static ObjectName getDefaultObjectName(Class<?> clazz) {
		return JMXHelper.objectName("org.helios.tracing.tracer:type=" + clazz.getSimpleName());
		
	}
	
	/**
	 * Returns a default tracer name.
	 * @return default tracer name.
	 */
	@JMXAttribute (name="DefaultName", description="The default name of this tracer", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDefaultName() {
		return this.getClass().getSimpleName();
	}
	
	/**
	 * Returns the default JMX ObjectName for this tracer instance.
	 * @return A JMX ObjectName.
	 */
	@JMXAttribute (name="DefaultObjectName", description="The default JMX ObjectName of this tracer", mutability=AttributeMutabilityOption.READ_ONLY)
	protected ObjectName getDefaultObjectName() {
		return JMXHelper.objectName(getStringBuilder().append(this.getClass().getPackage().getName()).append(":").append("type=Tracer,name=").append(getDefaultName(this.getClass())).toString());
		
	}
	
	
	
	protected void finalize() throws Throwable {
		TracerLifeCycles.incrementFinalizers(tracerName);
		super.finalize();		
	}
	
	
	/**
	 * Checks the default MBeanServer to see if the tracer instance represented by the passed class has been registered.
	 * If it has, it returns the reference. If not, or an error occurs getting the reference, a null is returned.
	 * @param clazz An ITracer class 
	 * @return the located ITracer or null.
	 */
	protected static TracerImpl jmxSingletonCheck(Class<? extends TracerImpl> clazz) {
		if(mbeanServer.isRegistered(getDefaultObjectName(clazz))) {
			try {
				return (TracerImpl)mbeanServer.getAttribute(getDefaultObjectName(clazz), "Reference");
			} catch (Exception e) {
				LOG.fatal("Failed to acquire registered ITracer reference from JMX [" + clazz.getName() + "] / [" + getDefaultObjectName(clazz) + "]", e);
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Registers the jmxSingleton tracer instance.
	 * @param atracer The tracer to register.
	 * @return The registered tracer, or null if there is an exception.
	 */
	protected static TracerImpl jmxSingletonRegister(TracerImpl atracer) {
		try {
			mbeanServer.registerMBean(atracer, getDefaultObjectName(atracer.getClass()));
			return atracer;
		} catch (InstanceAlreadyExistsException iea) {
			try {
				return (TracerImpl)mbeanServer.getAttribute(getDefaultObjectName(atracer.getClass()), "Reference");
			} catch (Exception e) {
				LOG.fatal("Failed to acquire registered ITracer reference from JMX [" + atracer.getClass().getName() + "] / [" + getDefaultObjectName(atracer.getClass()) + "]", e);
				return null;
			}			
		} catch (Exception e) {
			LOG.fatal("Unexpected error registering ITracer in JMX Agent [" + atracer.getClass().getName() + "] / [" + getDefaultObjectName(atracer.getClass()) + "]", e);
			return null;
		}
	}
	
	
	/**
	 * Indicates if the tracer returns traces.
	 * @return the returnTraces
	 */
	@JMXAttribute (name="ReturnTraces", description="Indicates if the tracer returns traces.", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean isReturnTraces() {
		return returnTraces;
	}

	/**
	 * Enables or disables the return trace.
	 * @param returnTraces the returnTraces to set
	 */
	public void setReturnTraces(boolean returnTraces) {
		this.returnTraces = returnTraces;
	}
	
	
	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder() {
		StringBuilder sb = buffer.get();
		if(sb==null) {
			sb = new StringBuilder();
			buffer.set(sb);
		}
		sb.setLength(0);
		return sb;
	}
	
	/**
	 * Reads the system properties and sets the name space verbosity for any property prefixed with <code>ENABLED_METRIC_PROPERTY_NAME_PREFIX</code>.
	 */
	protected static void bootStrapEnabledMetricStates() {
		Properties p = System.getProperties();
		String name = null;
		String value = null;
		for(Entry<Object, Object> entry : p.entrySet()) {
			name = entry.getKey().toString();
			value = entry.getValue().toString();
			if(name.toUpperCase().startsWith(ENABLED_METRIC_PROPERTY_NAME_PREFIX)) {				
				setCategoryTracing(name.substring(ENABLED_METRIC_PROPERTY_NAME_PREFIX.length()), value.equalsIgnoreCase("true"));
			}
		}
	}
	
	
	/**
	 * Determines if trace is enabled for the passed category.
	 * @param metricName The fully qualified metric name.
	 * @return true if trace is enabled.
	 */
	@JMXOperation (name="isMetricEnabled", description="Determines if the passed metric name is enabled.")
	public static boolean isMetricEnabled(
			@JMXParameter(name="MetricName", description="The metric name to get the enabled state of.") String metricName) {
		long start = System.currentTimeMillis();		
		boolean b = tracingState.getValue(metricName);
		long elapsed = System.currentTimeMillis()-start;
		if(!oneReading.get()) {
			oneReading.set(true);
			averageLookupTime.set(elapsed);
		} else {
			long currentAvg = averageLookupTime.get();			
			if((currentAvg + elapsed)==0L) {
				averageLookupTime.set(0L);
			} else {
				averageLookupTime.set((currentAvg + elapsed)/2);
			}						
		}		
		return b;
	}
	
	/**
	 * Sets the enabled state of the category's tracing logger.
	 * @param category The category to set.
	 * @param enabled true if tracing should be enabled. if false, the logger is set to OFF.
	 * @return A string describing the state of the tracing logger.
	 */	
	@JMXOperation (name="setCategoryTracing", description="Sets the enabled state of the passed metric name.")
	public static String setCategoryTracing(
			@JMXParameter(name="MetricName", description="The metric name to set the enabled state of.") String metricName, 
			@JMXParameter(name="Enabled", description="True for enabled, False for disabled.") boolean enabled) {		
		tracingState.setMember(metricName, enabled);
		return metricName + ":" + enabled;
		
	}
	

	
	/**
	 * Returns the current int in delta state for the passed namespace.
	 * @param namespace The metric namespace.
	 * @return The int in delta state or null if there is no delta for the passed namespace.
	 */
	@JMXOperation (name="getCurrentDeltaInt", description="Returns the current int in delta state for the passed namespace.")
	public int getCurrentDeltaInt(
			@JMXParameter(name="namespace", description="The fully qualified metric namespace.") String namespace) {
		Number n = deltaManager.getState(namespace);
		if(n==null) return -0;
		else return n.intValue();
	}
	
	/**
	 * Returns the current long in delta state for the passed namespace.
	 * @param namespace The metric namespace.
	 * @return The long in delta state or null if there is no delta for the passed namespace.
	 */
	@JMXOperation (name="getCurrentDeltaLong", description="Returns the current long in delta state for the passed namespace.")
	public long getCurrentDeltaLong(
			@JMXParameter(name="namespace", description="The fully qualified metric namespace.") String namespace) {
		Number n = deltaManager.getState(namespace);
		if(n==null) return -0;
		else return n.longValue();
		
	}
	
	/**
	 * Generates a string report of the contents of the delta states.
	 * @return A string report of deltas.
	 */
	@JMXOperation (name="dumpDeltas", description="Returns a string report of the contents of the delta states.")
	public String dumpDeltas() {
		return deltaManager.dumpState();
	}
	
	
	/**
	 * Determines the delta of the passed value for the passed keys against the value in state and stores the passed value in state.
	 * If no value is held in state, or the in state value is greater than the new value, the new value is placed in state and a null is returned.
	 * @param namespace The fully qualified metric namespace.
	 * @param value The new int value
	 * @return The delta of the passed value against the value in state, or a null.
	 */
	@JMXOperation (name="deltaInt", description="Processes an int delta.")
	public synchronized Integer deltaInt(
			@JMXParameter(name="namespace", description="The fully qualified metric namespace.") String namespace, 
			@JMXParameter(name="value", description="The new int value") int value) { 		
		Number n = deltaManager.delta(namespace, value, MetricType.DELTA_INT_AVG);
		if(n==null) return null;
		else return n.intValue();
	}
	
	/**
	 * Determines the delta of the passed value for the passed keys against the value in state and stores the passed value in state.
	 * If no value is held in state, or the in state value is greater than the new value, the new value is placed in state and a null is returned.
	 * @param namespace The fully qualified metric namespace.
	 * @param value The new long value
	 * @return The delta of the passed value against the value in state, or a null.
	 */
	@JMXOperation (name="deltaLong", description="Processes a long delta.")
	public synchronized Long deltaLong(
			@JMXParameter(name="namespace", description="The fully qualified metric namespace.") String namespace, 
			@JMXParameter(name="value", description="The new long value") long value) { 		
		Number n = deltaManager.delta(namespace, value, MetricType.DELTA_LONG_AVG);
		if(n==null) return null;
		else return n.longValue();
	}
	
	/**
	 * Returns the number entries held in state for deltas.
	 * @return The size of the delta state.
	 */
	@JMXAttribute (name="DeltaSize", description="The number of entries in the integer delta buffer.", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getDeltaSize() {
		return deltaManager.getStateSize();
	}
	
	
	
	/**
	 * Locates the range value for the measurement passed into the named range.
	 * @param rangeName The name of range to lookup in.
	 * @param value The value to pass in to the range lookup.
	 * @return The looked up range value.
	 * @see org.helios.tracing.ITracer#lookupRange(java.lang.String, long)
	 */
	@JMXOperation (name="lookupRange", description="Looks up the name of the range value that the passed value falls into for the passed range name.")
	public String lookupRange(
			@JMXParameter(name="RangeName", description="The name of the range to look up in.") String rangeName, 
			@JMXParameter(name="Value", description="The value to lookup a range value for.") long value) {
		throw new UnsupportedOperationException("lookupRange Not Implemented");
	}
	
	/**
	 * Returns the deep size of an object measured by an instance of <code>java.lang.Instrumentation</code>. 
	 * @param obj The object to measure the size of.
	 * @return The size of the passed object.
	 * @throws InstrumentationNotEnabled
	 * @see org.helios.tracing.ITracer#sizeOf(java.lang.Object)
	 */
	@JMXOperation (name="deepSize", description="Calulates the deep size of the passed object.")
	public long sizeOf(
			@JMXParameter(name="Object", description="The object to calculate the deep size of.") Object obj) throws InstrumentationNotEnabled {
		return TracerFactory.getInstance().deepSize(obj);		
	}
	
	/**
	 * Ends the current TraceThreadInfoCapture, traces it and returns the result.
	 * @param namespace The metric namespace suffix.
	 * @return A closed TraceThreadInfoCapture.
	 * @see org.helios.tracing.ITracer#endThreadInfoCapture(java.lang.String[], java.lang.String[])
	 */
	@JMXOperation (name="endThreadInfoCapture", description="Ends the current TraceThreadInfoCapture, traces it and returns the result.")
	public ThreadInfoCapture endThreadInfoCapture(
			@JMXParameter(name="namespace", description="The metric namespace suffix.") String... namespace) {
		return endThreadInfoCapture(null, namespace);
	}	
	
	/**
	 * Ends the current TraceThreadInfoCapture, traces it and returns the result.
	 * @param prefix The metric namespace prefix.
	 * @param namespace The metric namespace suffix.
	 * @return A closed TraceThreadInfoCapture.
	 * @see org.helios.tracing.ITracer#endThreadInfoCapture(java.lang.String[], java.lang.String[])
	 */
	@JMXOperation (name="endThreadInfoCapture", description="Ends the current TraceThreadInfoCapture, traces it and returns the result.")
	public ThreadInfoCapture endThreadInfoCapture(
			@JMXParameter(name="prefix", description="The metric namespace prefix.") String[] prefix, 
			@JMXParameter(name="namespace", description="The metric namespace suffix.") String... namespace) {
		ThreadInfoCapture tic = ThreadInfoCapture.end();
		TraceThreadInfoCapture.trace(tic, namespace, this);
		return tic;		
	}
	


	/**
	 * Starts a ThreadInfo capture of the current thread's CPU time, block time, block count, wait time and wait count. 
	 */
	@JMXOperation (name="startThreadInfoCapture", description="Starts a TraceThreadInfoCapture on the current thread.")
	public void startThreadInfoCapture() {
		ThreadInfoCapture.start(CPU+BLOCK+WAIT, false);
	}
	
	/**
	 * Starts a ThreadInfo capture one or more of the current thread's CPU time, block time, block count, wait time and wait count.
	 * @param options Mask of options. ints are CPU, WAIT and BLOCK. eg. start(CPU+WAIT)
	 */
	@JMXOperation (name="startThreadInfoCapture", description="Starts a TraceThreadInfoCapture on the current thread.")
	public void startThreadInfoCapture(
			@JMXParameter(name="Options", description="The thread stats to caputre. ints are CPU, WAIT and BLOCK. eg. start(CPU+WAIT)") int options) {
		ThreadInfoCapture.start(options, false);
	}
	
	
	/**
	 * Starts a new ThreadInfoi capture for the current thread
	 * @param options Mask of options. ints are CPU, WAIT and BLOCK. eg. start(CPU+WAIT)
	 * @param nanoTime If true, elapsed time is captured in nanos. If false, elapsed time is captured in millis.
	 */
	@JMXOperation (name="startThreadInfoCapture", description="Starts a TraceThreadInfoCapture on the current thread.")
	public void startThreadInfoCapture(
			@JMXParameter(name="Options", description="The thread stats to caputre. ints are CPU, WAIT and BLOCK. eg. start(CPU+WAIT)") int options, 
			@JMXParameter(name="NanoTime", description="The unit of the elapsed time. True means nanoseconds. False means milliseconds.") boolean nanoTime) {
		ThreadInfoCapture.start(options, nanoTime);
	}
	
	/**
	 * Ends a ThreadInfo capture and returns the elapsed results.
	 * @return The fully delta processed thread stats representing events and resource usage 
	 * for the current thread between the start and end threadinfo capture calls.
	 */
	@JMXOperation (name="endThreadInfoCapture", description="Ends a TraceThreadInfoCapture on the current thread.")
	public ThreadInfoCapture endThreadInfoCapture() {
		return ThreadInfoCapture.end();
	}




		
	
	/**
	 * Determines if tracing is enabled for the passed metric name.
	 * @param metricName A full or partial metric name. Partial names can only be truncated from the right. (ie. the most significant end must be supplied).
	 * @return true if metric is enabled. false if it is not.
	 */
/*	
	public static boolean isTraceEnabled(String metricName) {
		long start = System.currentTimeMillis();		
		boolean b = tracingState.getValue(getTracerLogger(tracerCategory));
		long elapsed = System.currentTimeMillis()-start;
		if(!oneReading) {
			oneReading = true;
			averageLookupTime.set(elapsed);
		} else {
			long currentAvg = averageLookupTime.get();			
			if((currentAvg + elapsed)==0L) {
				averageLookupTime.set(0L);
			} else {
				averageLookupTime.set((currentAvg + elapsed)/2);
			}						
		}		
		return b;
	}
*/	
	
	/**
	 * Traces a timestamp. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */
	@JMXOperation (name="trace", description="Traces a timestamp.")
	public Trace trace(
			@JMXParameter(name="value", description="The trace value") Date value, 
			@JMXParameter(name="metricName", description="The metric name") String metricName, 
			@JMXParameter(name="nameSpace", description="The metric namespace suffix.") String... nameSpace) {
		return traceTrace(
				Trace.build(value.getTime(), MetricType.TIMESTAMP, metricName).segment(nameSpace).format(this).build()
		);
	}
	
	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type's code that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */
	@JMXOperation (name="smartTrace", description="Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType.")
	public Trace smartTrace(
			@JMXParameter(name="type", description="The metric type's code that the value should be translated to.") int type, 
			@JMXParameter(name="value", description="The trace value") String value, 
			@JMXParameter(name="metricName", description="The metric name") String metricName, 
			@JMXParameter(name="nameSpace", description="The metric namespace suffix.") String... nameSpace) {
		return traceTrace(
				Trace.buildFromObject(value, MetricType.valueOf(type), metricName).segment(nameSpace).format(this).build()
		);		
	}
	
	
	/**
	 * Returns a granularly typed value for the passed value in accordance with the passed MetricType.
	 * @param type
	 * @param value
	 * @return
	 */
	public static Object deriveValue(MetricType type, Object value) {
		if(value==null) {
			logDeriveError(type, "null", Void.class, "Object value was null.");
			return null;
		}
		if(type==null) {
			logDeriveError(type, value, value.getClass(), "Type was null.");
			return null;
		} 
		
		Class<?> valueClass = value.getClass();
		if(valueClass.isPrimitive() || value instanceof Number) {
			if(!(value instanceof Number) || (!type.isNumber() && !type.equals(MetricType.TIMESTAMP))) {
				logDeriveError(type, value, valueClass, "Class type was primitive but was not an int or a long."); 
				return null;  // only primitives expected are int and long.
			} else {
				return type.isLong() ? ((Number)value).longValue() : ((Number)value).intValue();
			}
		}
		if(value instanceof CharSequence) {
			String strValue = value.toString();
			if(type.isNumber()) {
				try {
					Double d = new Double(strValue);
					return type.isLong() ? d.longValue() : d.intValue();
				} catch (Exception e) {
					logDeriveError(type, value, valueClass, "MetricType was Number but String value could not be converted.");
					return null; // type was number, but string could not be converted.
				}
			} else if(type.equals(MetricType.STRING)) {
				return strValue;
			} else {
				logDeriveError(type, value, valueClass, "A String value can only be a stringed number or string type but was neither.");
				return null; // a passed in String can only be a stringed number or string type.
			}
		}
		if(value instanceof Date) {
			if(type.equals(MetricType.TIMESTAMP)) {
				return ((Date)value).getTime();
			} else {
				logDeriveError(type, value, valueClass, "Value was a Date but type was not timestamp.");
				return null;
			}
		}
		logDeriveError(type, value, valueClass, "No conversion found.");
		return null;
	}
	
	
	/**
	 * Log warns a type derivation failure.
	 * @param type The metric type requested.
	 * @param value The value passed.
	 * @param valueClass The class name of the passed value.
	 * @param message Explanatory message.
	 */
	protected static void logDeriveError(MetricType type, Object value, Class<?> valueClass, String...message) {
		StringBuilder b = new StringBuilder("OpenTrace Type Derivation Failure:[");
		b.append(type==null ? "null": type.name()).append("]-[").append(value.toString()).append("(").append(valueClass.getName()).append(")]");
		if(message!=null) {
			for(String s: message) {
				b.append(s);
			}
		}
		LOG.warn(b.toString());
	}
	
	
	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */	
	@JMXOperation (name="smartTrace", description="Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType.")
	public Trace smartTrace(
			@JMXParameter(name="type", description="The metric type's enum that the value should be translated to.") MetricType type, 
			@JMXParameter(name="value", description="The trace value") String value, 
			@JMXParameter(name="metricName", description="The metric name") String metricName, 
			@JMXParameter(name="nameSpace", description="The metric namespace suffix.") String... nameSpace) {
		return traceTrace(
				Trace.buildFromObject(value, type, metricName).segment(nameSpace).format(this).build()
		);
	}

	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */
	@JMXOperation (name="smartTrace", description="Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType.")
	public Trace smartTrace(
			@JMXParameter(name="type", description="The metric type's enum that the value should be translated to.") MetricType type, 
			@JMXParameter(name="value", description="The trace value") String value, 
			@JMXParameter(name="metricName", description="The metric name") String metricName,
			@JMXParameter(name="prefix", description="An additional namespace prefix.") String[] prefix, 
			@JMXParameter(name="nameSpace", description="The metric namespace suffix.") String... nameSpace) {			
		return traceTrace(
				Trace.buildFromObject(value, type, metricName).segment(nameSpace).prefix(prefix).format(this).build()				
		);

	}
	
	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type's code that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */
	@JMXOperation (name="smartTrace", description="Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType.")	
	public Trace smartTrace(
			@JMXParameter(name="type", description="The metric type's name that the value should be translated to.") String type, 
			@JMXParameter(name="value", description="The trace value") String value, 
			@JMXParameter(name="metricName", description="The metric name") String metricName,
			@JMXParameter(name="nameSpace", description="The metric namespace suffix.") String... nameSpace) {			
		return traceTrace(
				Trace.buildFromObject(value, MetricType.valueOf(type), metricName).segment(nameSpace).format(this).build()
		);

	}

	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type's code that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */
	@JMXOperation (name="smartTrace", description="Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType.")
	public Trace smartTrace(
			@JMXParameter(name="type", description="The metric type's name that the value should be translated to.") String type, 
			@JMXParameter(name="value", description="The trace value") String value, 
			@JMXParameter(name="metricName", description="The metric name") String metricName,
			@JMXParameter(name="prefix", description="An additional namespace prefix.") String[] prefix, 
			@JMXParameter(name="nameSpace", description="The metric namespace suffix.") String... nameSpace) {			
		return traceTrace(
				Trace.buildFromObject(value, MetricType.valueOf(type), metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);
	}
	
	
	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType.
	 * @param type The metric type's code that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */
	@JMXOperation (name="smartTrace", description="Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType.")	
	public Trace smartTrace(
			@JMXParameter(name="type", description="The metric type's int code that the value should be translated to.") int type, 
			@JMXParameter(name="value", description="The trace value") String value, 
			@JMXParameter(name="metricName", description="The metric name") String metricName,
			@JMXParameter(name="prefix", description="An additional namespace prefix.") String[] prefix, 
			@JMXParameter(name="nameSpace", description="The metric namespace suffix.") String... nameSpace) {			
		return traceTrace(
				Trace.buildFromObject(value, MetricType.valueOf(type), metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);
	}

	
	
	/**
	 * Traces an interval averaged integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */
	public Trace trace(int value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.INT_AVG, metricName).segment(nameSpace).format(this).build()
				
		);
	}	
	
	/**
	 * Traces an interval averaged long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */
	public Trace trace(long value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.LONG_AVG, metricName).segment(nameSpace).format(this).build()
		);		
	}
	
	/**
	 * Traces an Introscope string. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace trace(String value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STRING, metricName).segment(nameSpace).format(this).build()
		);
	}
	
	/**
	 * Traces a non sticky string with all distinct messages being retained for the interval.
	 * May have variable supportability among trace implementations. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace.
	 */
	public Trace traceMessage(String value, String metricName, String...nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STRINGS, metricName).segment(nameSpace).format(this).build()
		);		
	}
	
	/**
	 * Traces a non sticky string with all distinct messages being retained for the interval.
	 * May have variable supportability among trace implementations.
	 * In cases where it is not directly supported, it will delegate to a standard <code>MetricType.STRING</code> trace. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace.
	 */
	public Trace traceMessage(String value, String metricName, String[] prefix, String...nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STRING, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);
	}
		
	
	
	/**
	 * Traces a delta averaged interval integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false or if the delta was suppressed.
	 */
	public Trace traceDelta(int value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.DELTA_INT_AVG, metricName).segment(nameSpace).format(this).build()
		);
	}
	
	
	/**
	 * Traces a delta averaged interval long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false or if the delta was suppressed.
	 */
	public Trace traceDelta(long value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.DELTA_LONG_AVG, metricName).segment(nameSpace).format(this).build()
		);
	}
	
	/**
	 * Traces multiple incidents. 
	 * @param incidents The number of incidents.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceIncident(int value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.INTERVAL_INCIDENT, metricName).segment(nameSpace).format(this).build()
		);
	}

	/**
	 * Traces a single incident. 
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceIncident(String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(1, MetricType.INTERVAL_INCIDENT, metricName).segment(nameSpace).format(this).build()
		);		
	}
	
	
	/**
	 * Traces a sticky interval integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null is returnTraces is false.
	 */
	public Trace traceSticky(int value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STICKY_INT_AVG, metricName).segment(nameSpace).format(this).build()
		);
	}
	

	/**
	 * Traces a sticky interval long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null is returnTraces is false.
	 */
	public Trace traceSticky(long value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STICKY_LONG_AVG, metricName).segment(nameSpace).format(this).build()
		);	
	}
	

	
	/**
	 * Traces a sticky delta integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace  or null if returnTraces is false or if the delta was suppressed.
	 */
	public Trace traceStickyDelta(int value, String metricName,	String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STICKY_DELTA_INT_AVG, metricName).segment(nameSpace).format(this).build()				
		);
	}
	
	
	/**
	 * Traces a sticky delta long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace  or null if returnTraces is false or if the delta was suppressed.
	 */
	public Trace traceStickyDelta(long value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STICKY_DELTA_LONG_AVG, metricName).segment(nameSpace).format(this).build()
		);
	}

	
	/**
	 * Traces a sticky timestamp. 
	 * @param value The trace value, converted into a date.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null returnTraces is false.
	 */
	public Trace traceTimestamp(long value, String metricName, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.TIMESTAMP, metricName).segment(nameSpace).format(this).build()				
		);		
	}



	/**
	 * Returns the assigned tracer name.
	 * @return the tracerName
	 */
	@JMXAttribute (name="TracerName", description="The assigned name of this tracer", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getTracerName() {
		return tracerName;
	}




	/**
	 * the ObjectName used to register the tracer's management interface
	 * @return the objectName
	 */
	@JMXAttribute (name="TracerObjectName", description="The assigned JMX ObjectName of this tracer", mutability=AttributeMutabilityOption.READ_ONLY)
	public ObjectName getTracerObjectName() {
		return tracerObjectName;
	}
	

	


	/**
	 * Traces an interval averaged integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */
	public Trace trace(int value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.INT_AVG, metricName).segment(nameSpace).prefix(prefix).format(this).build()				
		);				
	}


	/**
	 * Traces an interval averaged long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace trace(long value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.LONG_AVG, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);				
	}


	/**
	 * Traces a timestamp. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */		
	public Trace trace(Date value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value.getTime(), MetricType.TIMESTAMP, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);				
	}


	/**
	 * Traces a string message. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */		
	public Trace trace(String value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STRING, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);				
	}


	/**
	 * Traces a delta int. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceDelta(int value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.DELTA_INT_AVG, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);				
	}


	/**
	 * Traces a delta long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceDelta(long value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.DELTA_LONG_AVG, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);				
	}

	/**
	 * Traces one interval incident. 
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceIncident(String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(1, MetricType.INTERVAL_INCIDENT, metricName).segment(nameSpace).prefix(prefix).format(this).build()				
		);						
	}

	/**
	 * Traces the specified number of interval incidents. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceIncident(int value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.INTERVAL_INCIDENT, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);						
	}

	/**
	 * Traces a sticky int. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceSticky(int value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STICKY_INT_AVG, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);						
	}

	/**
	 * Traces a sticky long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceSticky(long value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STICKY_LONG_AVG, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);						
	}

	/**
	 * Traces a sticky delta int. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceStickyDelta(int value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STICKY_DELTA_INT_AVG, metricName).segment(nameSpace).prefix(prefix).format(this).build()				
		);						
	}

	/**
	 * Traces a sticky delta long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceStickyDelta(long value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.STICKY_DELTA_LONG_AVG, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);						
	}

	/**
	 * Traces a timestamp. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceTimestamp(long value, String metricName, String[] prefix, String... nameSpace) {
		return traceTrace(
				Trace.build(value, MetricType.TIMESTAMP, metricName).segment(nameSpace).prefix(prefix).format(this).build()
		);						
	}
	

	/**
	 * Builds a trace instance
	 * @param metricType
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#getInstance(org.helios.tracing.trace.MetricType, java.lang.Object, java.lang.String, java.lang.String[])
	 */
	public Trace getInstance(MetricType metricType, Object value, String metricName, String... nameSpace) {
		return Trace.buildFromObject(value, metricType, metricName).segment(nameSpace).format(this).build();
	}

	/**
	 * Builds a trace instance
	 * @param metricType
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#getInstance(org.helios.tracing.trace.MetricType, java.lang.Object, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace getInstance(MetricType metricType, Object value, String metricName, String[] prefix, String... nameSpace) {
		return Trace.buildFromObject(value, metricType, metricName).segment(nameSpace).prefix(prefix).format(this).build();
	}
	

	/**
	 * Implemented by concrete Tracer implementations.
	 * @param trace
	 * @return
	 * @see org.helios.tracing.ITracer#traceTrace(org.helios.tracing.trace.Trace)
	 */
	public Trace traceTrace(Trace trace) {
		this.tracerFactory.submitTraces(trace);
		return trace;
	}

	
	/**
	 * This method can be overriden by concrete implementations to customize the output of the builder.
	 * @param builder A reference to the builder just prior to generating the trace.
	 * @return The builder.
	 */
	public Builder format(Builder builder) {
		return builder;
	}
	
		
	
}
