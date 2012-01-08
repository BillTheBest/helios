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
package org.helios.ot.endpoint;

import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.ot.jmx.JMXMetric;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.trace.IntervalTrace;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: IEndPoint</p>
 * <p>Description: Defines an endpoint that closed traces and interval traces will be delivered to for processing. </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.endpoint.IEndPoint</code></p>
 */
public interface IEndPoint<T extends Trace<? extends ITraceValue>>  {

	/**
	 * This method will be implemented by AbstractEndpoint class only.  It 
	 * calls processTracesImpl implemented by concrete endpoint classes..
	 * @param traceCollection The trace collection to process.
	 * @throws Exception on any processing exception
	 */
	public void processTraces(TraceCollection<T> traceCollection) throws Exception;

	/**
	 * This method will be implemented by AbstractEndpoint class only.  It calls
	 * connectImpl implemented by concrete endpoint classes.
	 * 
	 * @return boolean flag: true if reconnected successfully - false otherwise
	 */
	public boolean connect();


	/**
	 * This method will be implemented by AbstractEndpoint class only.  It calls
	 * disconnectImpl implemented by concrete endpoint classes.
	 */
	public void disconnect();


	/**
	 * This method will be implemented by AbstractEndpoint only.
	 * @return boolean flag: true if reconnected successfully - false otherwise
	 */
	public boolean reconnect();


	/**
	 * This method returns the status of this endpoint
	 * @return boolean flag: true if this Endpoint is active - false otherwise
	 */
	public boolean isConnected();


}
