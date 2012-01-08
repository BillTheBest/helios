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

import org.helios.ot.subtracer.ISubTracerProvider;

import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: ITracerManager</p>
 * <p>Description:Defines a TracerManager </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.tracer.ITracerManager</code></p>
 */

public interface ITracerManager extends ISubTracerProvider {
	
	
	/**
	 * Returns a standard tracer
	 * @return an ITracer
	 */
	public TracerImpl getTracer();
	
	
	/**
	 * Stops the tracer manager, the disruptor and deallocates resources.
	 */
	public void shutdown();


	/**
	 * Acquires the next trace invocation slot. If the underlying trace processor is unable to provide one
	 * at the time of this call, or times out, a null will be returned.
	 * @return a TraceCollection or null if the acquire timed out.
	 */
	public TraceCollection getNextTraceCollectionSlot();
	
	/**
	 * Commits a trace collection for processing by endpoints.
	 * @param traceCollection the trace collection to commit.
	 */
	public void commit(TraceCollection traceCollection);

	
}
