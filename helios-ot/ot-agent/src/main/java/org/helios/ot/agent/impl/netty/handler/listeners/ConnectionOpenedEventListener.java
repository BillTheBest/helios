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

import org.apache.log4j.Logger;
import org.helios.ot.agent.impl.netty.handler.FilteringChannelStateChangeListener;
import org.helios.ot.agent.protocol.impl.ClientProtocolOperation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation;
import org.helios.ot.trace.MetricId;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;

/**
 * <p>Title: ConnectionOpenedEventListener</p>
 * <p>Description: Listener to act on a channel connected event</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.impl.netty.handler.listeners.ConnectionOpenedEventListener</code></p>
 */

public class ConnectionOpenedEventListener implements FilteringChannelStateChangeListener {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.handler.ChannelStateChangeListener#onChannelStateChange(boolean, org.jboss.netty.channel.ChannelState, org.jboss.netty.channel.Channel, org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void onChannelStateChange(boolean upstream, ChannelState channelState, final Channel channel, final ChannelHandlerContext ctx, final ChannelEvent e) {
		e.getFuture().addListener(new ChannelFutureListener(){
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isDone() && future.isSuccess()) {
					ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), Channels.future(channel), HeliosProtocolInvocation.newInstance(ClientProtocolOperation.CONNECT, new String[]{MetricId.getHostname(), MetricId.getApplicationId()}), channel.getRemoteAddress()));
				} 
			}
		});
//		new Thread() {
//			public void run() {
//				e.getFuture().addListener(new ChannelFutureListener(){
//					public void operationComplete(ChannelFuture future) throws Exception {
//						log.info("Connect Event Complete:" + future.isDone());
//						log.info("Connect Event Success:" + future.isSuccess());
//						log.info("Channel Connected. Sending Downstream Handshake Request");
//						ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), Channels.future(channel), HeliosProtocolInvocation.newInstance(ClientProtocolOperation.CONNECT, new String[]{MetricId.getHostname(), MetricId.getApplicationId()}), channel.getRemoteAddress()));
//						log.info("Downstream Handshake Request Sent");
//						
//					}
//				});
//			}
//		}.start();
		
		//channel.write(HeliosProtocolInvocation.newInstance(ClientProtocolOperation.CONNECT, new String[]{MetricId.getHostname(), MetricId.getApplicationId()}));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.handler.FilteringChannelStateChangeListener#isStateChangeEnabled(boolean, org.jboss.netty.channel.ChannelState)
	 */
	@Override
	public boolean isStateChangeEnabled(boolean upstream, ChannelState state) {		
		return !upstream && state==ChannelState.CONNECTED;
		
	}

}
