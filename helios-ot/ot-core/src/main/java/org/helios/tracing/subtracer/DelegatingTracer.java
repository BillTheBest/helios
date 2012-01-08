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
package org.helios.tracing.subtracer;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture;
import org.helios.tracing.ITracer;
import org.helios.tracing.InstrumentationNotEnabled;
import org.helios.tracing.trace.MetricType;
import org.helios.tracing.trace.Trace;
import org.helios.tracing.trace.Trace.Builder;

/**
 * <p>Title: DelegatingTracer</p>
 * <p>Description: An abstract tracer that delegates all tracing calls to an inner concrete tracer. Provided for implementing subtracers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.subtracer.DelegatingTracer</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public abstract class DelegatingTracer extends ManagedObjectDynamicMBean implements ITracer {
	/** The inner wrapped concrete tracer */
	protected final ITracer vtracer;
	/** The virtual tracr name */
	protected final String tracerName;
	/** The tracer send counter */
	protected final AtomicLong sendCounter = new AtomicLong(0L);
	/** the JMX ObjectName name of the tracer */
	protected ObjectName tracerObjectName = null;
	/** The tracer's last counter reset timestamp */
	protected final AtomicLong lastResetTime = new AtomicLong(0L);
	/** The delegatring tracer's logger */
	protected final Logger log;
	/** This tracer's ObjectName */
	protected ObjectName objectName = null;
	/** The subtracer stack */
	protected final Set<Class<? extends ITracer>> stack = new HashSet<Class<? extends ITracer>>();
	
	/**
	 * Creates a new DelegatingTracer 
	 * @param vtracer The wrapped inner tracer
	 */
	public DelegatingTracer(ITracer vtracer, String tracerName) {		
		if(vtracer instanceof DelegatingTracer) {
			Set<Class<? extends ITracer>> vtracerStack = ((DelegatingTracer)vtracer).stack;
			if(vtracerStack.contains(getClass())) {
				throw new RuntimeException("SubTracer of type [" + getClass().getName() + "] found instance of same already in SubTracer stack.", new Throwable());
			}
			stack.addAll(vtracerStack);
		} else {
			stack.add(vtracer.getClass());
		}
		stack.add(getClass());
		this.vtracer = vtracer;
		this.tracerName = tracerName;
		log = Logger.getLogger(getClass().getName() + "." + tracerName);
		objectName = createObjectName(vtracer.getTracerObjectName());		
	}
	
	/**
	 * Creates a new ObjectName for this subTracer by appending its sequenced key to the parent's ObjectName. 
	 * @param parentObjectName The parent tracer's ObjectName
	 * @return a new ObjectName
	 */
	protected ObjectName createObjectName(ObjectName parentObjectName) {
		return JMXHelper.objectName(new StringBuilder(parentObjectName.toString())
				.append(",subTracer").append(stack.size()-1).append("=")
				.append(getClass().getSimpleName())
		);
	}
	
	/**
	 * Returns this tracer's instance name
	 * @return this tracer's instance name
	 */
	@JMXAttribute(name="TracerName", description="The delegate tracer name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getTracerName() {
		return tracerName;
	}
	
	
	//==========================================================
	
	/**
	 * @param builder
	 * @return
	 */
	@Override
	public abstract Builder format(Builder builder);
	
	
	
	/**
	 * Returns the date of the last time the tracer's counters were reset.
	 * @return
	 * @see org.helios.tracing.ITracer#getLastCounterResetDate()
	 */
	@JMXAttribute(name="LastCounterResetDate", description="The date of the last time the tracer's counters were reset.", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getLastCounterResetDate() {
		return new Date(lastResetTime.get());
	}

	/**
	 * Returns the timestamp of the last time the tracer's counters were reset.
	 * @return
	 * @see org.helios.tracing.ITracer#getLastCounterResetTime()
	 */
	@JMXAttribute(name="LastCounterResetTime", description="The last time the tracer's counters were reset.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastCounterResetTime() {
		return lastResetTime.get();
	}

	/**
	 * Returns this tracer's verbosity
	 * @return
	 * @see org.helios.tracing.ITracer#getLogging()
	 */
	@JMXAttribute (name="Logging", description="The logging level of this tracer", mutability=AttributeMutabilityOption.READ_WRITE)
	public String getLogging() {
		Level level = log.getLevel();
		if(level==null) level = log.getEffectiveLevel();
		return level.toString();		
	}

	/**
	 * The number of traces sent
	 * @return the number of traces sent
	 * @see org.helios.tracing.ITracer#getSendCounter()
	 */
	@JMXAttribute(name="SendCounter", description="The number of traces sent", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSendCounter() {
		return sendCounter.get();
	}
	
	/**
	 * Returns the ObjectName of this tracer's management interface
	 * @return
	 * @see org.helios.tracing.ITracer#getTracerObjectName()
	 */
	public ObjectName getTracerObjectName() {
		return this.tracerObjectName;
	}

	/**
	 * @param host
	 * @param agent
	 * @return
	 * @see org.helios.tracing.ITracer#getVirtualTracer(java.lang.String, java.lang.String)
	 */
	public ITracer getVirtualTracer(String host, String agent) {
		return vtracer.getVirtualTracer(host, agent);
	}
	
	/**
	 * Resets this tracer's counters 
	 * @see org.helios.tracing.ITracer#resetCounters()
	 */
	@JMXOperation(name="resetCounters", description="Resets this tracer's counters")
	public void resetCounters() {
		sendCounter.set(0L);
		lastResetTime.set(System.currentTimeMillis());		
	}

	/**
	 * Sets this tracer's logging level
	 * @param level
	 * @see org.helios.tracing.ITracer#setLogging(java.lang.String)
	 */
	public void setLogging(String level) {
		org.apache.log4j.Level newlevel = Level.toLevel(level);
		log.setLevel(newlevel);		
	}	
	
	
	//===========================================================
	

	/**
	 * Passes the trace instance through to the underlying trace instance's <code>traceTrace</code> method after setting the virtual agent overrides.
	 * @param trace The trace to be virtualized and passed.
	 * @return The trace that was virtualized and passed
	 * @see org.helios.tracing.TracerImpl#traceTrace(org.helios.tracing.trace.Trace)
	 */
	public Trace traceTrace(Trace trace) {
		if(trace.getValue()!=null) {
			vtracer.traceTrace(trace);
			sendCounter.incrementAndGet();
		}
		return trace;
	}


	/**
	 * Traces a Delta Int
	 * @param namespace
	 * @param value
	 * @return
	 * @see org.helios.tracing.ITracer#deltaInt(java.lang.String, int)
	 */
	public Integer deltaInt(String namespace, int value) {
		return vtracer.deltaInt(namespace, value);
	}

	/**
	 * Traced a delta long
	 * @param namespace
	 * @param value
	 * @return
	 * @see org.helios.tracing.ITracer#deltaLong(java.lang.String, long)
	 */
	public Long deltaLong(String namespace, long value) {
		return vtracer.deltaLong(namespace, value);
	}

	/**
	 * Dumps out the delta accumulators in a formated string
	 * @return
	 * @see org.helios.tracing.ITracer#dumpDeltas()
	 */
	public String dumpDeltas() {
		return vtracer.dumpDeltas();
	}

	/**
	 * Ends a thread info capture and returns the captured data
	 * @return
	 * @see org.helios.tracing.ITracer#endThreadInfoCapture()
	 */
	public ThreadInfoCapture endThreadInfoCapture() {
		return vtracer.endThreadInfoCapture();
	}

	/**
	 * Ends a thread info capture, traces and returns the captured data
	 * @param namespace
	 * @return
	 * @see org.helios.tracing.ITracer#endThreadInfoCapture(java.lang.String[])
	 */
	public ThreadInfoCapture endThreadInfoCapture(String... namespace) {
		return vtracer.endThreadInfoCapture(namespace);
	}

	/**
	 * Ends a thread info capture, traces and returns the captured data
	 * @param prefix
	 * @param namespace
	 * @return
	 * @see org.helios.tracing.ITracer#endThreadInfoCapture(java.lang.String[], java.lang.String[])
	 */
	public ThreadInfoCapture endThreadInfoCapture(String[] prefix,
			String... namespace) {
		return vtracer.endThreadInfoCapture(prefix, namespace);
	}

	/**
	 * Returns the current int delta state
	 * @param namespace
	 * @return
	 * @see org.helios.tracing.ITracer#getCurrentDeltaInt(java.lang.String)
	 */
	public int getCurrentDeltaInt(String namespace) {
		return vtracer.getCurrentDeltaInt(namespace);
	}

	/**
	 * Returns the current long delta state
	 * @param namespace
	 * @return
	 * @see org.helios.tracing.ITracer#getCurrentDeltaLong(java.lang.String)
	 */
	public long getCurrentDeltaLong(String namespace) {
		return vtracer.getCurrentDeltaLong(namespace);
	}

	/**
	 * Returns the tracer's default name
	 * @return
	 * @see org.helios.tracing.ITracer#getDefaultName()
	 */
	public String getDefaultName() {
		return vtracer.getDefaultName();
	}

	/**
	 * Returns the number of deltas in state
	 * @return
	 * @see org.helios.tracing.ITracer#getDeltaSize()
	 */
	public int getDeltaSize() {
		return vtracer.getDeltaSize();
	}

	/**
	 * Creates a trace instance
	 * @param metricType
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#getInstance(org.helios.tracing.trace.MetricType, java.lang.Object, java.lang.String, java.lang.String[])
	 */
	public Trace getInstance(MetricType metricType, Object value,
			String metricName, String... nameSpace) {
		return vtracer.getInstance(metricType, value, metricName, nameSpace);
	}

	/**
	 * Creates a trace instance
	 * @param metricType
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#getInstance(org.helios.tracing.trace.MetricType, java.lang.Object, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace getInstance(MetricType metricType, Object value,
			String metricName, String[] prefix, String... nameSpace) {
		return vtracer.getInstance(metricType, value, metricName, prefix,
				nameSpace);
	}


	/**
	 * Returns a temporal tracer that wraps this tracer.
	 * @return 
	 * @see org.helios.tracing.ITracer#getTemporalTracer()
	 */
	public ITracer getTemporalTracer() {
		return vtracer.getTemporalTracer();
	}

	/**
	 * Looks up a range qualifier
	 * @param rangeName
	 * @param value
	 * @return
	 * @see org.helios.tracing.ITracer#lookupRange(java.lang.String, long)
	 */
	public String lookupRange(String rangeName, long value) {
		return vtracer.lookupRange(rangeName, value);
	}


	/**
	 * Returns the deep size of the passed object in bytes
	 * @param obj
	 * @return
	 * @throws InstrumentationNotEnabled
	 * @see org.helios.tracing.ITracer#sizeOf(java.lang.Object)
	 */
	public long sizeOf(Object obj) throws InstrumentationNotEnabled {
		return vtracer.sizeOf(obj);
	}

	/**
	 * Smart Trace
	 * @param type
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#smartTrace(int, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public Trace smartTrace(int type, String value, String metricName,
			String... nameSpace) {
		return vtracer.smartTrace(type, value, metricName, nameSpace);
	}

	/**
	 * Smart Trace
	 * @param type
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#smartTrace(int, java.lang.String, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace smartTrace(int type, String value, String metricName,
			String[] prefix, String... nameSpace) {
		return vtracer.smartTrace(type, value, metricName, prefix, nameSpace);
	}

	/**
	 * Smart Trace
	 * @param type
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#smartTrace(org.helios.tracing.trace.MetricType, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public Trace smartTrace(MetricType type, String value, String metricName,
			String... nameSpace) {
		return vtracer.smartTrace(type, value, metricName, nameSpace);
	}

	/**
	 * Smart Trace
	 * @param type
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#smartTrace(org.helios.tracing.trace.MetricType, java.lang.String, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace smartTrace(MetricType type, String value, String metricName,
			String[] prefix, String... nameSpace) {
		return vtracer.smartTrace(type, value, metricName, prefix, nameSpace);
	}

	/**
	 * Smart Trace
	 * @param type
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#smartTrace(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public Trace smartTrace(String type, String value, String metricName,
			String... nameSpace) {
		return vtracer.smartTrace(type, value, metricName, nameSpace);
	}

	/**
	 * Smart Trace
	 * @param type
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#smartTrace(java.lang.String, java.lang.String, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace smartTrace(String type, String value, String metricName,
			String[] prefix, String... nameSpace) {
		return vtracer.smartTrace(type, value, metricName, prefix, nameSpace);
	}

	/**
	 * Starts a thread info capture
	 * @see org.helios.tracing.ITracer#startThreadInfoCapture()
	 */
	public void startThreadInfoCapture() {
		vtracer.startThreadInfoCapture();
	}

	/**
	 * Starts a thread info capture
	 * @param options
	 * @param nanoTime
	 * @see org.helios.tracing.ITracer#startThreadInfoCapture(int, boolean)
	 */
	public void startThreadInfoCapture(int options, boolean nanoTime) {
		vtracer.startThreadInfoCapture(options, nanoTime);
	}

	/**
	 * Starts a thread info capture
	 * @param options
	 * @see org.helios.tracing.ITracer#startThreadInfoCapture(int)
	 */
	public void startThreadInfoCapture(int options) {
		vtracer.startThreadInfoCapture(options);
	}

	/**
	 * Traces a timestamp
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#trace(java.util.Date, java.lang.String, java.lang.String[])
	 */
	public Trace trace(Date value, String metricName, String... nameSpace) {
		return vtracer.trace(value, metricName, nameSpace);
	}

	/**
	 * Traces a timestamp
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#trace(java.util.Date, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace trace(Date value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.trace(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces an int average
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#trace(int, java.lang.String, java.lang.String[])
	 */
	public Trace trace(int value, String metricName, String... nameSpace) {
		return vtracer.trace(value, metricName, nameSpace);
	}

	/**
	 * Traces an int average
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#trace(int, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace trace(int value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.trace(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces a long average
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tr
	 * acing.ITracer#trace(long, java.lang.String, java.lang.String[])
	 */
	public Trace trace(long value, String metricName, String... nameSpace) {
		return vtracer.trace(value, metricName, nameSpace);
	}

	/**
	 * Traces a long average
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#trace(long, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace trace(long value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.trace(value, metricName, prefix, nameSpace);
	}

	/**
	 * Returns a trace builder
	 * @param metricName
	 * @param value
	 * @param metricType
	 * @return
	 * @see org.helios.tracing.ITracer#trace(java.lang.String, java.lang.Object, int)
	 */
	public Builder trace(String metricName, Object value, int metricType) {
		return vtracer.trace(metricName, value, metricType);
	}

	/**
	 * Returns a trace builder
	 * @param metricName
	 * @param value
	 * @param metricType
	 * @return
	 * @see org.helios.tracing.ITracer#trace(java.lang.String, java.lang.Object, org.helios.tracing.trace.MetricType)
	 */
	public Builder trace(String metricName, Object value, MetricType metricType) {
		return vtracer.trace(metricName, value, metricType);
	}

	/**
	 * Returns a trace builder
	 * @param metricName
	 * @param value
	 * @param metricType
	 * @return
	 * @see org.helios.tracing.ITracer#trace(java.lang.String, java.lang.Object, java.lang.String)
	 */
	public Builder trace(String metricName, Object value, String metricType) {
		return vtracer.trace(metricName, value, metricType);
	}

	/**
	 * Traces a message
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#trace(java.lang.String, java.lang.String, java.lang.String[])
	 */
	public Trace trace(String value, String metricName, String... nameSpace) {
		return vtracer.trace(value, metricName, nameSpace);
	}

	/**
	 * Traces a message
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#trace(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace trace(String value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.trace(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces a delta int
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceDelta(int, java.lang.String, java.lang.String[])
	 */
	public Trace traceDelta(int value, String metricName, String... nameSpace) {
		return vtracer.traceDelta(value, metricName, nameSpace);
	}

	/**
	 * Traces a delta int
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceDelta(int, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceDelta(int value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.traceDelta(value, metricName, prefix, nameSpace);
	}
	

	/**
	 * Traces a delta long
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceDelta(long, java.lang.String, java.lang.String[])
	 */
	public Trace traceDelta(long value, String metricName, String... nameSpace) {
		return vtracer.traceDelta(value, metricName, nameSpace);
	}

	/**
	 * Traces a delta long
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceDelta(long, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceDelta(long value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.traceDelta(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces a number of incidents
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceIncident(int, java.lang.String, java.lang.String[])
	 */
	public Trace traceIncident(int value, String metricName,
			String... nameSpace) {
		return vtracer.traceIncident(value, metricName, nameSpace);
	}

	/**
	 * Traces a number of incidents
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceIncident(int, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceIncident(int value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.traceIncident(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces one incident
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceIncident(java.lang.String, java.lang.String[])
	 */
	public Trace traceIncident(String metricName, String... nameSpace) {
		return vtracer.traceIncident(metricName, nameSpace);
	}

	/**
	 * Traces one incident
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceIncident(java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceIncident(String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.traceIncident(metricName, prefix, nameSpace);
	}

	/**
	 * Traces a message
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceMessage(java.lang.String, java.lang.String, java.lang.String[])
	 */
	public Trace traceMessage(String value, String metricName,
			String... nameSpace) {
		return vtracer.traceMessage(value, metricName, nameSpace);
	}

	/**
	 * Traces a message
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceMessage(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceMessage(String value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.traceMessage(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces a sticky int
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceSticky(int, java.lang.String, java.lang.String[])
	 */
	public Trace traceSticky(int value, String metricName, String... nameSpace) {
		return vtracer.traceSticky(value, metricName, nameSpace);
	}

	/**
	 * Traces a sticky int
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceSticky(int, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceSticky(int value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.traceSticky(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces a sticky long
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceSticky(long, java.lang.String, java.lang.String[])
	 */
	public Trace traceSticky(long value, String metricName, String... nameSpace) {
		return vtracer.traceSticky(value, metricName, nameSpace);
	}

	/**
	 * Traces a sticky long
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceSticky(long, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceSticky(long value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.traceSticky(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces a sticky delta long
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceStickyDelta(int, java.lang.String, java.lang.String[])
	 */
	public Trace traceStickyDelta(int value, String metricName,
			String... nameSpace) {
		return vtracer.traceStickyDelta(value, metricName, nameSpace);
	}

	/**
	 * Traces a sticky delta long
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceStickyDelta(int, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceStickyDelta(int value, String metricName,
			String[] prefix, String... nameSpace) {
		return vtracer.traceStickyDelta(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces a sticky delta long
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceStickyDelta(long, java.lang.String, java.lang.String[])
	 */
	public Trace traceStickyDelta(long value, String metricName,
			String... nameSpace) {
		return vtracer.traceStickyDelta(value, metricName, nameSpace);
	}

	/**
	 * Traces a sticky delta long
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceStickyDelta(long, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceStickyDelta(long value, String metricName,
			String[] prefix, String... nameSpace) {
		return vtracer.traceStickyDelta(value, metricName, prefix, nameSpace);
	}

	/**
	 * Traces a  timestamp
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceTimestamp(long, java.lang.String, java.lang.String[])
	 */
	public Trace traceTimestamp(long value, String metricName,
			String... nameSpace) {
		return vtracer.traceTimestamp(value, metricName, nameSpace);
	}

	/**
	 * Traces a  timestamp
	 * @param value
	 * @param metricName
	 * @param prefix
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#traceTimestamp(long, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	public Trace traceTimestamp(long value, String metricName, String[] prefix,
			String... nameSpace) {
		return vtracer.traceTimestamp(value, metricName, prefix, nameSpace);
	}


	//==================================================

}
