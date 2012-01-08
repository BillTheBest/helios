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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.helios.helpers.Banner;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: DefaultEndpoint</p>
 * <p>Description: The default endpoint used by the TracerManager when no other endpoints are configured.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.endpoint.DefaultEndpoint</code></p>
 */

public class DefaultEndpoint<T extends Trace<? extends ITraceValue>> implements IEndPoint<T> {
	/** Static class logger */
	protected static final Logger handlerLogger = Logger.getLogger(DefaultEndpoint.class);
	/** the current sequence number */
	protected AtomicLong sequence = new AtomicLong(-1L);
	/**
	 * Called when a publisher has published an AbstractEvent to the RingBuffer
	 * @param traceCollection The trace collection published to the ring buffer
	 * @throws Exception if the EventHandler would like the exception handled further up the chain.
	 */
	@Override
	public void processTraces(TraceCollection<T> traceCollection) throws Exception {
		long id = sequence.incrementAndGet();
		if(handlerLogger.isDebugEnabled()) Banner.bannerOut("*", 2, 5, "DEH Processing event [" + id + "] in TC [" + traceCollection.getSequence() + "]");
		if(handlerLogger.isDebugEnabled()) {
			Map<String, Object> map = new HashMap<String, Object>(2);
			map.put("Traces", traceCollection.getTraces());
			map.put("Context", traceCollection.getContextTags());			
			MDC.put("TraceCollectionContext", map);
			handlerLogger.debug("DefaultEventHandler Event[TC:" + traceCollection.getSequence() + "]");				
		}
		//Thread.currentThread().join(0, 10000);
	}

	/**
	 * @return
	 */
	@Override
	public boolean connect() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 
	 */
	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	/**
	 * @return
	 */
	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}


	/**
	 * @return
	 */
	@Override
	public boolean reconnect() {
		// TODO Auto-generated method stub
		return false;
	}


}
