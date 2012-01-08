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
package org.helios.ot.tracer.fixtures.thread;

import java.util.ArrayList;
import java.util.List;

import org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture;
import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: TraceThreadInfoCapture</p>
 * <p>Description: A container class for captured metrics representing a thread's CPU, Block and Wait stats.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1058 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/thread/TraceThreadInfoCapture.java $
 * $Id: TraceThreadInfoCapture.java 1058 2009-02-18 17:33:54Z nwhitehead $
 */
public class TraceThreadInfoCapture  {
	
	public static final int CPU      = 1 << 0;  
	public static final int WAIT      = 1 << 1; 
	public static final int BLOCK    = 1 << 2;  
	
	
	/**
	 * Traces the current values.
	 * Used when a TraceThreadInfoCapture needs to be traced to two name spaces.
	 * @param A Closed ThreadInfoCapture 
	 * @param metricPrefix The full metric name minus the name itself which is provided by this method.
	 * @param tracer The ITracer to use for tracing.
	 */
	public static void trace(ThreadInfoCapture tic, String[] namespace, ITracer tracer) {
		trace(tic, null, tracer, namespace);
		
	}
	
	/**
	 * Traces the current values.
	 * Used when a TraceThreadInfoCapture needs to be traced to two name spaces.
	 * @param A Closed ThreadInfoCapture
	 * @param metricPrefix
	 * @param tracer
	 * @param namespace
	 * TODO: This needs to call a batched trace cmd.
	 */
	public static void trace(ThreadInfoCapture tic, String[] metricPrefix, ITracer tracer, String...namespace) {
		if(tracer==null) return;
		
		List<Trace> traces = new ArrayList<Trace>(8);
		
		
		traces.add(Trace.build(
				tic.getElapsedTime(), MetricType.LONG_AVG, 
				tic.isNanoTime() ? "Elapsed Time (ns)" : "Elapsed Time (ms)")
				.prefix(metricPrefix)
				.segment(namespace).format(tracer).build()
		);
		if(tic.getMetricOption()>0) {
			if(((tic.getMetricOption() & CPU) == CPU)) {
				traces.add(Trace.build(
						tic.getTotalCpuTime(), MetricType.LONG_AVG,	"Total CPU Time (ns)")
						.prefix(metricPrefix)
						.segment(namespace).format(tracer).build()
				);				
			}
			if(((tic.getMetricOption() & BLOCK) == BLOCK)) {
				traces.add(Trace.build(
						tic.getBlockedCount(), MetricType.LONG_AVG, "Total Blocks")
						.prefix(metricPrefix)
						.segment(namespace).format(tracer).build()
				);				
				traces.add(Trace.build(
						tic.getBlockedTime(), MetricType.LONG_AVG, "Total Block Time (ms)")
						.prefix(metricPrefix)
						.segment(namespace).format(tracer).build()
				);								
			}
			if(((tic.getMetricOption() & WAIT) == WAIT)) {
				traces.add(Trace.build(
						tic.getWaitCount(), MetricType.LONG_AVG, "Total Waits")
						.prefix(metricPrefix)
						.segment(namespace).format(tracer).build()
				);				
				traces.add(Trace.build(
						tic.getWaitTime(), MetricType.LONG_AVG, "Total Wait Time (ms)")
						.prefix(metricPrefix)
						.segment(namespace).format(tracer).build()
				);												
			}
		}
		tracer.traceTrace(traces);
		
	}
	
//	/**
//	 * Traces the current values but uses an incident trace for waits and blocks.
//	 * Used when a TraceThreadInfoCapture needs to be traced to two name spaces 
//	 * and waits and blocks should be aggregated for the interval. 
//	 * @param metricPrefix The full metric name minus the name itself which is provided by this method.
//	 * @param tracer The ITracer to use for tracing.
//	 */
//	public void traceIncident(String metricPrefix, ITracer tracer) {
//		if(tracer==null) return;
//		if(metricOption<0) return;
//		if(((metricOption & CPU) == CPU)) {
//			tracer.trace(totalCpuTime, metricPrefix, "Total CPU Time (ns)");
//		}
//		if(((metricOption & BLOCK) == BLOCK)) {
//			tracer.traceIncident((int)blockedCount, metricPrefix, "Total Blocks");
//			tracer.trace(blockedTime, metricPrefix, "Total Block Time (ms)");
//		}
//		if(((metricOption & WAIT) == WAIT)) {
//			tracer.traceIncident((int)waitCount, metricPrefix, "Total Waits");
//			tracer.trace(waitTime, metricPrefix, "Total Wait Time (ms)");
//		}			
//	}
	

	
	
//	/**
//	 * Calls TraceThreadInfoCapture end and traces the captured measurements.
//	 * @param name The compound name fragments.
//	 * @param tracer The tracer to trace to.
//	 */
//	public static TraceThreadInfoCapture traceEnd(String[] name, ITracer tracer) {
//		if(tracer==null) return null;
//		TraceThreadInfoCapture tic = TraceThreadInfoCapture.end();
//		int metricOption = tic.getMetricOption();
//		if(tic!=null) {
//			traceEnd(name, "Elapsed Time (ms)", tic.getElapsedTime(), tracer);
//			if(metricOption<0) return tic;
//			if(((metricOption & CPU) == CPU)) {
//				traceEnd(name, "Total CPU Time (ns)", tic.getTotalCpuTime(), tracer);
//			}
//			if(((metricOption & BLOCK) == BLOCK)) {
//				traceEnd(name, "Total Blocks", tic.getBlockedCount(), tracer);
//				traceEnd(name, "Total Block Time (ms)", tic.getBlockedTime(), tracer);
//			}
//			if(((metricOption & WAIT) == WAIT)) {
//				traceEnd(name, "Total Waits", tic.getWaitCount(), tracer);			
//				traceEnd(name, "Total Wait Time (ms)", tic.getWaitTime(), tracer);							
//			}			
//		}
//		return tic;
//	}
	
	
	
	/**
	 * Executes tracing on the passed measurement.
	 * @param name
	 * @param metricName
	 * @param value
	 * @param tracer the tracer to use.
	 */
	protected static void traceEnd(String[] name, String metricName, long value, ITracer tracer) {
		if(tracer==null || value==-1) return;
		tracer.trace(value, metricName, name);
	}
	

}
