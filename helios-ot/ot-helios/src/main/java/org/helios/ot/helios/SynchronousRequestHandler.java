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
package org.helios.ot.helios;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;


/**
 * <p>Title: SynchronousRequestHandler</p>
 * <p>Description: A netty channel handler for orchestrating synchronous requests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.SynchronousRequestHandler</code></p>
 */

public class SynchronousRequestHandler extends SimpleChannelHandler {
	/** The default synchronous op timeout in ms. */
	protected final long defaultSyncTimeout;
	
	/** A map of response waiting invocations keyed by the request id */
	protected final Map<Long, HeliosProtocolInvocation> synchOps = new ConcurrentHashMap<Long, HeliosProtocolInvocation>();
	
	/**
	 * Creates a new SynchronousRequestHandler
	 * @param defaultSyncTimeout The default synchronous op timeout in ms.
	 */
	public SynchronousRequestHandler(long defaultSyncTimeout) {
		this.defaultSyncTimeout = defaultSyncTimeout;
	}
	
	/**
	 * Creates a new SynchronousRequestHandler using the configured or default timeout
	 */
	public SynchronousRequestHandler() {
		this(HeliosEndpointConfiguration.getSynchOpTimeout());
	}
	
	
	/**
	 * The outbound synchronous request handler
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			HeliosProtocolInvocation hpi = (HeliosProtocolInvocation)((MessageEvent)e).getMessage();
			if(!hpi.isAsync()) {
				synchOps.put(hpi.getRequestId(), hpi.setSynchOps(synchOps));
			}
		}
		super.handleDownstream(ctx, e);
	}
	
	/**
	 * The inbound response handler
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			Object o = ((MessageEvent)e).getMessage();
			if(o instanceof HeliosProtocolResponse) {
				HeliosProtocolResponse hpr = (HeliosProtocolResponse)o;
				HeliosProtocolInvocation hpi = synchOps.get(hpr.getRequestSerial());
				if(hpi!=null) {
					hpi.setSynchResponse(hpr.getPayload());
				}				
			}
		} 
		super.handleUpstream(ctx, e);
	}
}
