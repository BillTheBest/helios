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
package org.helios.ot.tracer;

import java.util.Collection;
import java.util.Date;

import javax.management.ObjectName;

import org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture;
import org.helios.ot.subtracer.ISubTracerProvider;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.Trace.Builder;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: ITracer</p>
 * <p>Description: Defines the tracing interface </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.ITracer</code></p>
 */

public interface ITracer extends ISubTracerProvider {
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
	
//	/**
//	 * Returns the deep size of an object measured by an instance of <code>java.lang.Instrumentation</code>. 
//	 * @param obj The object to measure the size of.
//	 * @return The size of the passed object.
//	 * @throws InstrumentationNotEnabled
//	 * @see org.helios.tracing.ITracer#sizeOf(java.lang.Object)
//	 */
//	public long sizeOf(Object obj) throws InstrumentationNotEnabled;
	
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
	 * Dispatches the formatted trace to the process queue
	 * @param trace The undecorated trace
	 * @return the deorated trace
	 */
	public Trace traceTrace(Trace trace);
	
	/**
	 * Dispatches the passed traces to the process queue
	 * @param traces An array of traces to process
	 * @return the processed traces
	 */
	public Trace[] traceTrace(Trace ...traces);
	
	/**
	 * Dispatches the traces to the process queue
	 * @param traces A collection of traces to process
	 * @return the processed traces
	 */
	public Collection<Trace> traceTrace(Collection<Trace> traces);
	
	
	/**
	 * This method can be overriden by concrete implementations to customize the output of the builder.
	 * @param builder A reference to the builder just prior to generating the trace.
	 * @return The builder.
	 */
	public Builder format(Builder builder);
	
	/**
	 * Returns a reference to this tracer's tracer manager
	 * @return the tracerManager
	 */
	public ITracerManager getTracerManager();
	
	/**
	 * Builds a metric name from the passed fragments
	 * @param point The metric point
	 * @param nameSpace The metric name space
	 * @return The fully qualified metric name
	 */
	public String buildMetricName(CharSequence point, CharSequence...nameSpace);
	
	/**
	 * Builds a metric name from the passed fragments
	 * @param point The metric point
	 * @param prefix Prefixes for the namespace
	 * @param nameSpace The metric name space
	 * @return The fully qualified metric name
	 */
	public String buildMetricName(CharSequence point, CharSequence[] prefix, CharSequence...nameSpace);		
	
	
	
	
	
	
	
}
