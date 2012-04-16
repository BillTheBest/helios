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
package org.helios.ot.agent.impl.netty.handler;

import org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

/**
 * <p>Title: InvocationRequestListener</p>
 * <p>Description: A listener that intercepts server invocation requests before they go out the door</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.impl.netty.handler.InvocationRequestListener</code></p>
 */

public interface InvocationRequestListener extends ProtocolListener {
	/**
	 * Callback from the helios protocol handler when an invocation is headed up to the server
	 * @param request The invocation request
	 * @param ctx The channel handler context (useful if we need to zip something back down the stack, perhaps a requested value from the server can be found in a local cache)
	 * @param message The original message event (useful if we need info from the event itself.
	 */
	public void onInvocationRequest(HeliosProtocolInvocation request, ChannelHandlerContext ctx, MessageEvent message);

}
