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
package org.helios.tracing.bridge;

import java.util.Collection;
import java.util.concurrent.Executor;

import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: ITracingBridge</p>
 * <p>Description: Defines a class that provides a bridge to a tracing end point.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.ITracingBridge</code></p>
 */

public interface ITracingBridge {
	/**
	 * Returns true if the bridge is stateful and can be up or down.
	 * @return true if the bridge is stateful and can be up or down, false if it is stateless.
	 */
	public boolean isStateful();
	
	/**
	 * Returns the name of the tracing end point
	 * @return the name of the tracing end point
	 */
	public String getEndPointName();
	
	
	/**
	 * Sets the thread pool used by this tracing bridge for reconnect polling and flushes.
	 * @param threadPool an executor	 */
	public void setExecutor(Executor threadPool);
	
	/**
	 * Returns the configured executor
	 * @return the configured executor
	 */
	public Executor getExecutor();
	
	/**
	 * Submits traces to the tracing endpoint
	 * @param traces the traces to submit
	 */
	public void submitTraces(Collection<Trace> traces);
	
	/**
	 * Submits trace intervals to the tracing endpoint
	 * @param intervalTraces the trace intervals to submit
	 */
	public void submitIntervalTraces(Collection<IIntervalTrace> intervalTraces);
	
	/**
	 * Submits traces to the tracing endpoint
	 * @param traces the traces to submit
	 */
	public void submitTraces(Trace... traces);
	
	
	/**
	 * Submits trace intervals to the tracing endpoint
	 * @param intervalTraces the trace intervals to submit
	 */
	public void submitIntervalTraces(IIntervalTrace... traceIntervals);
	
	
	/**
	 * Indicates if the underlying bridge endoint can accept interval metrics.
	 * @return if the underlying bridge endoint can accept interval metrics, otherwise, intervals are filtered down to individual traces.
	 */
	public boolean isIntervalCapable();
	
	// Add bridge configuration class. JMX Enabled.  Optionally bypass.
	// Meta Annotations:  @RequiresRestart
	
}
