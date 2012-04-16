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
package org.helios.ot.agent.impl.netty.handler.listeners;

import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.helios.ot.agent.impl.netty.handler.FilteringInvocationRequestListener;
import org.helios.ot.agent.impl.netty.handler.FilteringInvocationResponseListener;
import org.helios.ot.agent.protocol.impl.ClientProtocolOperation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolResponse;
import org.helios.patterns.queues.TimeoutListener;
import org.helios.patterns.queues.TimeoutQueueMap;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

/**
 * <p>Title: SynchronousInvocationListener</p>
 * <p>Description: Intercepts and tracks syncrhonous invocations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.impl.netty.handler.listeners.SynchronousInvocationListener</code></p>
 */

public class SynchronousInvocationListener implements FilteringInvocationRequestListener, FilteringInvocationResponseListener, TimeoutListener<Long, HeliosProtocolInvocation> {
	/** The invocation timeout queue map */
	protected final TimeoutQueueMap<Long, HeliosProtocolInvocation> pendingInvocations; 
	
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/**
	 * Creates a new SynchronousInvocationListener
	 * @param timeout The default timeout in ms. for iintercepted invocations
	 */
	public SynchronousInvocationListener(long timeout) {
		pendingInvocations = new TimeoutQueueMap<Long, HeliosProtocolInvocation>(timeout);
		pendingInvocations.addListener(this);
		log.info("Created SynchronousInvocationListener with a default timeout of [" + timeout + "] ms.");
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.handler.InvocationRequestListener#onInvocationRequest(org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation, org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void onInvocationRequest(HeliosProtocolInvocation request, ChannelHandlerContext ctx, MessageEvent message) {
		request.setStartTimeNanos();
		long requestId = request.getRequestId();
		pendingInvocations.put(requestId, request);
	}
	
	/**
	 * Called when an in-flight synchronous invocation times out
	 * {@inheritDoc}
	 * @see org.helios.patterns.queues.TimeoutListener#onTimeout(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void onTimeout(Long key, HeliosProtocolInvocation timedOutInvocation) {
		log.warn("Invocation Timing Out:" + timedOutInvocation);
		timedOutInvocation.setSynchResponse(new TimeoutException());
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.handler.InvocationResponseListener#onInvocationResponse(org.helios.ot.agent.protocol.impl.HeliosProtocolResponse, org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void onInvocationResponse(HeliosProtocolResponse response, ChannelHandlerContext ctx, MessageEvent message) {
		HeliosProtocolInvocation hpi = pendingInvocations.remove(response.getRequestSerial());		
		if(hpi!=null) {
			hpi.setElapsedTimeNanos();
			hpi.setSynchResponse(response.getPayload());
			//log.info("Elapsed:" + hpi.getElapsedTimeNanos() + " ns.");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.handler.FilteringInvocationResponseListener#isResponseEnabled(org.helios.ot.agent.protocol.impl.HeliosProtocolResponse)
	 */
	@Override
	public boolean isResponseEnabled(HeliosProtocolResponse response) {
		return !ClientProtocolOperation.getOp(response.getOp()).isAsync() && pendingInvocations.containsKey(response.getRequestSerial());
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.handler.FilteringInvocationRequestListener#isRequestEnabled(org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation)
	 */
	@Override
	public boolean isRequestEnabled(HeliosProtocolInvocation request) {
		return !ClientProtocolOperation.getOp(request.getOp()).isAsync();
	}



}
