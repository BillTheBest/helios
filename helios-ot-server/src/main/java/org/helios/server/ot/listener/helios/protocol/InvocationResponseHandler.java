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
package org.helios.server.ot.listener.helios.protocol;

import org.helios.ot.helios.HeliosProtocolResponse;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

/**
 * <p>Title: InvocationResponseHandler</p>
 * <p>Description: Packages the response into a HeliosInvocationResponse</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.listener.helios.protocol.InvocationResponseHandler</code></p>
 */

public class InvocationResponseHandler extends ObjectEncoder {

	@Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		Long requestId = (Long)ctx.getAttachment();
		if(requestId==null) {
			return super.encode(ctx, channel, msg);
		} else {		
			return super.encode(ctx, channel, HeliosProtocolResponse.newInstance(requestId.longValue(), msg));
		}
	}

}
