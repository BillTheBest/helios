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

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.tracing.bridge.ITracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.subtracer.IIntervalTracer;
import org.helios.tracing.subtracer.ITemporalTracer;
import org.helios.tracing.subtracer.IUrgentTracer;
import org.helios.tracing.subtracer.IVirtualTracer;
import org.helios.tracing.trace.Trace;



/**
 * <p>Title: ITracerInstanceFactory</p>
 * <p>Description: Interface defining a factory to create an instance of a Tracer.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1058 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/ITracerInstanceFactory.java $
 * $Id: ITracerInstanceFactory.java 1058 2009-02-18 17:33:54Z nwhitehead $
 */
public interface ITracerInstanceFactory {
	
	/**
	 * Returns the tracer factory tracing bridge
	 * @return the tracer factory tracing bridge
	 */
	public ITracingBridge getBridge();
	/**
	 * Returns a concrete instance of an TracerImpl.
	 * @return An ITracer instance.
	 * @throws TracerInstanceFactoryException
	 */
	public ITracer getTracer() throws TracerInstanceFactoryException;
	
	/**
	 * Returns a reference to the bridge which will be updated if the factory changes the bridge
	 * @return a reference to the bridge
	 */
	public AtomicReference<ITracingBridge> getBridgeRef();	
	
	/**
	 * Returns a virtual wrapper around a concrete instance of an TracerImpl.
	 * @param host The host the virtual tracer is tracing for
	 * @param agent The agent the virtual tracer is tracing for
	 * @return An IVirtualTracer instance.
	 * @throws TracerInstanceFactoryException
	 */
	public IVirtualTracer getVirtualTracer(String host, String agent) throws TracerInstanceFactoryException;
	
	/**
	 * Returns a temporal wrapper around a concrete instance of an TracerImpl.
	 * @return An ITemporalTracer instance.
	 * @throws TracerInstanceFactoryException
	 */
	public ITemporalTracer getTemporalTracer() throws TracerInstanceFactoryException;

	
	/**
	 * Returns an urgent wrapper around a concrete instance of an TracerImpl.
	 * @return An IUrgentTracer instance.
	 * @throws TracerInstanceFactoryException
	 */
	public IUrgentTracer getUrgentTracer() throws TracerInstanceFactoryException;
	
	/**
	 * Returns an interval wrapper around a concrete instance of an TracerImpl.
	 * @return An IIntervalTracer instance.
	 * @throws TracerInstanceFactoryException
	 */
	public IIntervalTracer getIntervalTracer() throws TracerInstanceFactoryException;
	
	/**
	 * Submits an array of interval traces to the tracing endpoint
	 * @param intervalTraces an array of interval traces to submit
	 */
	public void submitIntervalTraces(IIntervalTrace...intervalTraces);
	
	/**
	 * Submits a collection of interval traces to the tracing endpoint
	 * @param intervalTraces a collection of interval traces to submit
	 */
	public void submitIntervalTraces(Collection<IIntervalTrace> intervalTraces);

	/**
	 * Submits an array of traces to the tracing endpoint
	 * @param traces an array of traces to submit
	 */
	public void submitTraces(Trace...traces);
	
	/**
	 * Submits a collection of traces to the tracing endpoint
	 * @param traces a collection of traces to submit
	 */
	public void submitTraces(Collection<Trace> traces);
	
	/**
	 * Sets the thread pool used by this tracing bridge for reconnect polling and flushes.
	 * @param threadPool an executor	 */
	public void setExecutor(Executor threadPool);
	
	
	// Configuration Support - Configurable Singleton
	// Bridge: configure, start, shutdown, state, stats
	
	
	// Tracer:
	// =========
	// Raw: trace -->  bridge 
	// Interval: trace --> Accumulator --> flush --> tracer.intervalTrace --> bridge
	

		
	

}
