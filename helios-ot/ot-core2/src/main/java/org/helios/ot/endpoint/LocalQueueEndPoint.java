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

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: LocalQueueEndPoint</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.endpoint.LocalQueueEndPoint</code></p>
 */

public class LocalQueueEndPoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T> {
	protected static AtomicReference<BlockingQueue<Trace>> queue = new AtomicReference<BlockingQueue<Trace>>(null); 
	/**
	 * Creates a new LocalQueueEndPoint
	 */
	private LocalQueueEndPoint(int size) {
		BlockingQueue<Trace> _queue = new ArrayBlockingQueue<Trace>(size);
		BlockingQueue<Trace> oldQueue = queue.getAndSet(_queue);
		if(oldQueue!=null) {
			_queue.addAll(Arrays.asList(oldQueue.toArray(new Trace[0])));
		}
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#connectImpl()
	 */
	@Override
	protected void connectImpl() throws EndpointConnectException {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#disconnectImpl()
	 */
	@Override
	protected void disconnectImpl() {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#newBuilder()
	 */
	@Override
	public LocalQueueEndPoint.Builder newBuilder() {
		return new LocalQueueEndPoint.Builder();
	}
	
	
	public static Trace nextTrace() {
		try {
			return queue.get().take();
		} catch (InterruptedException e) {
			throw new RuntimeException("nextTrace interrupted while waiting for a Trace", e);
		}
	}
	
	public static Trace nextTrace(long timeout, TimeUnit unit) {
		try {
			return queue.get().poll(timeout, unit);
		} catch (InterruptedException e) {
			throw new RuntimeException("nextTrace interrupted while waiting for a Trace", e);
		}
	}
	
	
	public static void clear() {
		queue.get().clear();
	}
	
	
	public static Builder getBuilder() {
		return new Builder();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#processTracesImpl(org.helios.ot.tracer.disruptor.TraceCollection)
	 */
	@Override
	protected boolean processTracesImpl(TraceCollection<T> traceCollection) throws EndpointConnectException, EndpointTraceException {
		for(Trace trace: traceCollection.getTraces()) {
			queue.get().offer(trace);
		}
		return true;
	}
	
	public static class Builder extends AbstractEndpoint.Builder {
		protected int size = 1024;
		
		/**
		 * Sets the Queue size
		 * @param size the size of the queue
		 */
		public void setSize(int size) {
			this.size= size;
		}
		
		/**
		 * Sets the Queue size
		 * @param size the size of the queue
		 * @return this builder
		 */
		public Builder size(int size) {
			this.size= size;
			return this;
		}
		

		
		/**
		 * {@inheritDoc}
		 * @see org.helios.ot.endpoint.AbstractEndpoint.Builder#build()
		 */
		@Override
		public LocalQueueEndPoint build() {			
			return new LocalQueueEndPoint(size);
		}
		
	}

}
