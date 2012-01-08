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

import java.util.Date;

import javax.management.ObjectName;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture;
import org.helios.tracing.thread.TraceThreadInfoCapture;
import org.helios.tracing.trace.MetricType;
import org.helios.tracing.trace.Trace;
import org.helios.tracing.trace.Trace.Builder;

/**
 * <p>Title: ITracer</p>
 * <p>Description: Defines the tracing interface </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.ITracer</code></p>
 */

public interface ITracer {
	/**
	 * Sets the logging level
	 * @param level
	 */
	public void setLogging(String level);
	
	/**
	 * Returns the logging level
	 * @return the logging level
	 */	
	public String getLogging();
	
	/**
	 * Start a new Trace.Builder
	 * @param metricName The name of the metric.
	 * @param value The value of the metric.
	 * @param metricType The metric type.
	 */	
	public Trace.Builder trace(String metricName, Object value, MetricType metricType);

	/**
	 * Start a new Trace.Builder
	 * @param metricName The name of the metric.
	 * @param value The value of the metric.
	 * @param metricType The metric type.
	 */		
	public Trace.Builder trace(String metricName, Object value, String metricType);
	
	/**
	 * Start a new Trace.Builder
	 * @param metricName The name of the metric.
	 * @param value The value of the metric.
	 * @param metricType The metric type.
	 */		
	public Trace.Builder trace(String metricName, Object value, int metricType);
	
	/**
	 * Returns a virtual tracer wrapped around this tracing instance 
	 * @param host The virtual tracer's host
	 * @param agent The virtual tracer's agent name
	 * @return a virtual tracer
	 */
	public ITracer getVirtualTracer(String host, String agent);
	
	/**
	 * Creates a temporal tracer that uses this tracer for the mechanics but sets all traces generated to be temporal.
	 * Only one instance of a temporal tracer will be created per underlying tracer. Instances are cached so this method can be called
	 * multiple times on the same tracer and it will return the same temporal tracer instance each time.
	 * @return a temporal tracer.
	 */
	public ITracer getTemporalTracer();
	
	/**
	 * Returns a default tracer name.
	 * @return default tracer name.
	 */
	public String getDefaultName();
	
	/**
	 * Returns the current int in delta state for the passed namespace.
	 * @param namespace The metric namespace.
	 * @return The int in delta state or null if there is no delta for the passed namespace.
	 */
	public int getCurrentDeltaInt(String namespace);
	
	/**
	 * Returns the current long in delta state for the passed namespace.
	 * @param namespace The metric namespace.
	 * @return The long in delta state or null if there is no delta for the passed namespace.
	 */
	public long getCurrentDeltaLong(String namespace);
	
	/**
	 * Generates a string report of the contents of the delta states.
	 * @return A string report of deltas.
	 */
	public String dumpDeltas();
	
	
	/**
	 * Determines the delta of the passed value for the passed keys against the value in state and stores the passed value in state.
	 * If no value is held in state, or the in state value is greater than the new value, the new value is placed in state and a null is returned.
	 * @param namespace The fully qualified metric namespace.
	 * @param value The new int value
	 * @return The delta of the passed value against the value in state, or a null.
	 */
	public Integer deltaInt(String namespace, int value);
	
	/**
	 * Determines the delta of the passed value for the passed keys against the value in state and stores the passed value in state.
	 * If no value is held in state, or the in state value is greater than the new value, the new value is placed in state and a null is returned.
	 * @param namespace The fully qualified metric namespace.
	 * @param value The new long value
	 * @return The delta of the passed value against the value in state, or a null.
	 */
	public Long deltaLong(String namespace, long value); 		
	
	/**
	 * Returns the number entries held in state for deltas.
	 * @return The size of the delta state.
	 */
	public int getDeltaSize();
	
	/**
	 * Locates the range value for the measurement passed into the named range.
	 * @param rangeName The name of range to lookup in.
	 * @param value The value to pass in to the range lookup.
	 * @return The looked up range value.
	 * @see org.helios.tracing.ITracer#lookupRange(java.lang.String, long)
	 */
	public String lookupRange(String rangeName, long value);
	
	/**
	 * Returns the deep size of an object measured by an instance of <code>java.lang.Instrumentation</code>. 
	 * @param obj The object to measure the size of.
	 * @return The size of the passed object.
	 * @throws InstrumentationNotEnabled
	 * @see org.helios.tracing.ITracer#sizeOf(java.lang.Object)
	 */
	public long sizeOf(Object obj) throws InstrumentationNotEnabled;
	
	/**
	 * Ends the current TraceThreadInfoCapture, traces it and returns the result.
	 * @param namespace The metric namespace suffix.
	 * @return A closed TraceThreadInfoCapture.
	 * @see org.helios.tracing.ITracer#endThreadInfoCapture(java.lang.String[], java.lang.String[])
	 */
	public ThreadInfoCapture endThreadInfoCapture(String... namespace);
	
	/**
	 * Ends the current TraceThreadInfoCapture, traces it and returns the result.
	 * @param prefix The metric namespace prefix.
	 * @param namespace The metric namespace suffix.
	 * @return A closed TraceThreadInfoCapture.
	 * @see org.helios.tracing.ITracer#endThreadInfoCapture(java.lang.String[], java.lang.String[])
	 */
	public ThreadInfoCapture endThreadInfoCapture(String[] prefix, String... namespace);
	


	/**
	 * Starts a ThreadInfo capture of the current thread's CPU time, block time, block count, wait time and wait count. 
	 */
	public void startThreadInfoCapture();
	
	/**
	 * Starts a ThreadInfo capture one or more of the current thread's CPU time, block time, block count, wait time and wait count.
	 * @param options Mask of options. ints are CPU, WAIT and BLOCK. eg. start(CPU+WAIT)
	 */
	public void startThreadInfoCapture(int options);
	
	/**
	 * Starts a new ThreadInfoi capture for the current thread
	 * @param options Mask of options. ints are CPU, WAIT and BLOCK. eg. start(CPU+WAIT)
	 * @param nanoTime If true, elapsed time is captured in nanos. If false, elapsed time is captured in millis.
	 */
	public void startThreadInfoCapture(int options, boolean nanoTime);
	
	/**
	 * Ends a ThreadInfo capture and returns the elapsed results.
	 * @return The fully delta processed thread stats representing events and resource usage 
	 * for the current thread between the start and end threadinfo capture calls.
	 */	
	public ThreadInfoCapture endThreadInfoCapture();
	
	/**
	 * Traces a timestamp. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */
	public Trace trace(Date value, String metricName, String... nameSpace);
	
	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type's code that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */
	public Trace smartTrace(int type, String value, String metricName, String... nameSpace);
	
	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */	
	public Trace smartTrace(MetricType type, String value, String metricName, String... nameSpace);
	
	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */
	public Trace smartTrace( MetricType type, String value, String metricName, String[] prefix, String... nameSpace);			
	
	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type's code that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */		
	public Trace smartTrace(String type, String value, String metricName, String... nameSpace);

	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType. 
	 * @param type The metric type's code that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */
	public Trace smartTrace(String type, String value, String metricName, String[] prefix, String... nameSpace);
	
	
	/**
	 * Traces a parameterized metric type. The value is specified as a String and then converted to the type specified by the MetricType.
	 * @param type The metric type's code that the value should be translated to.
	 * @param value The trace value in string format compatible with the speficied metric type.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if there is an error in processing, or if returnTraces is false.
	 */
	public Trace smartTrace(int type, String value, String metricName, String[] prefix, String... nameSpace);
	
	/**
	 * Traces an interval averaged integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */
	public Trace trace(int value, String metricName, String... nameSpace);
	
	
	/**
	 * Traces an interval averaged long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */
	public Trace trace(long value, String metricName, String... nameSpace);
	
	/**
	 * Traces an Introscope string. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace trace(String value, String metricName, String... nameSpace);
	
	/**
	 * Traces a non sticky string with all distinct messages being retained for the interval.
	 * May have variable supportability among trace implementations. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace.
	 */
	public Trace traceMessage(String value, String metricName, String...nameSpace);
	
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
	public Trace traceMessage(String value, String metricName, String[] prefix, String...nameSpace);
	
	/**
	 * Traces a delta averaged interval integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false or if the delta was suppressed.
	 */
	public Trace traceDelta(int value, String metricName, String... nameSpace);
	
	
	/**
	 * Traces a delta averaged interval long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false or if the delta was suppressed.
	 */
	public Trace traceDelta(long value, String metricName, String... nameSpace);
	
	/**
	 * Traces multiple incidents. 
	 * @param incidents The number of incidents.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceIncident(int value, String metricName, String... nameSpace);
	
	/**
	 * Traces a single incident. 
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceIncident(String metricName, String... nameSpace);
	
	
	/**
	 * Traces a sticky interval integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null is returnTraces is false.
	 */
	public Trace traceSticky(int value, String metricName, String... nameSpace);
	

	/**
	 * Traces a sticky interval long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null is returnTraces is false.
	 */
	public Trace traceSticky(long value, String metricName, String... nameSpace);
	
	/**
	 * Traces a sticky delta integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace  or null if returnTraces is false or if the delta was suppressed.
	 */
	public Trace traceStickyDelta(int value, String metricName,	String... nameSpace);
	
	
	/**
	 * Traces a sticky delta long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace  or null if returnTraces is false or if the delta was suppressed.
	 */
	public Trace traceStickyDelta(long value, String metricName, String... nameSpace);
	
	/**
	 * Traces a sticky timestamp. 
	 * @param value The trace value, converted into a date.
	 * @param metricName The metric name.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null returnTraces is false.
	 */
	public Trace traceTimestamp(long value, String metricName, String... nameSpace);



	/**
	 * Returns the assigned tracer name.
	 * @return the tracerName
	 */
	public String getTracerName();
	
	
	/**
	 * the ObjectName used to register the tracer's management interface
	 * @return the objectName
	 */
	public ObjectName getTracerObjectName();

	/**
	 * Traces an interval averaged integer. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */
	public Trace trace(int value, String metricName, String[] prefix, String... nameSpace);


	/**
	 * Traces an interval averaged long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace trace(long value, String metricName, String[] prefix, String... nameSpace);


	/**
	 * Traces a timestamp. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */		
	public Trace trace(Date value, String metricName, String[] prefix, String... nameSpace);
	
	/**
	 * Traces a string message. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */		
	public Trace trace(String value, String metricName, String[] prefix, String... nameSpace);


	/**
	 * Traces a delta int. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceDelta(int value, String metricName, String[] prefix, String... nameSpace);


	/**
	 * Traces a delta long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceDelta(long value, String metricName, String[] prefix, String... nameSpace);
	
	/**
	 * Traces one interval incident. 
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceIncident(String metricName, String[] prefix, String... nameSpace);

	/**
	 * Traces the specified number of interval incidents. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceIncident(int value, String metricName, String[] prefix, String... nameSpace);
	
	/**
	 * Traces a sticky int. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceSticky(int value, String metricName, String[] prefix, String... nameSpace);

	/**
	 * Traces a sticky long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceSticky(long value, String metricName, String[] prefix, String... nameSpace);

	/**
	 * Traces a sticky delta int. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceStickyDelta(int value, String metricName, String[] prefix, String... nameSpace);

	/**
	 * Traces a sticky delta long. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceStickyDelta(long value, String metricName, String[] prefix, String... nameSpace);

	/**
	 * Traces a timestamp. 
	 * @param value The trace value.
	 * @param metricName The metric name.
	 * @param prefix An additional namespace prefix.
	 * @param nameSpace The metric namespace suffix.
	 * @return The generated Trace or null if returnTraces is false.
	 */	
	public Trace traceTimestamp(long value, String metricName, String[] prefix, String... nameSpace);
	
	/**
	 * Builds a trace instance
	 * @param metricType
	 * @param value
	 * @param metricName
	 * @param nameSpace
	 * @return
	 * @see org.helios.tracing.ITracer#getInstance(org.helios.tracing.trace.MetricType, java.lang.Object, java.lang.String, java.lang.String[])
	 */
	public Trace getInstance(MetricType metricType, Object value, String metricName, String... nameSpace);

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
	public Trace getInstance(MetricType metricType, Object value, String metricName, String[] prefix, String... nameSpace);
	
	/**
	 * Implemented by concrete Tracer implementations.
	 * @param trace
	 * @return
	 * @see org.helios.tracing.ITracer#traceTrace(org.helios.tracing.trace.Trace)
	 */
	public Trace traceTrace(Trace trace);
	
	/**
	 * This method can be overriden by concrete implementations to customize the output of the builder.
	 * @param builder A reference to the builder just prior to generating the trace.
	 * @return The builder.
	 */
	public Builder format(Builder builder);
	
	
	
	
	
}
