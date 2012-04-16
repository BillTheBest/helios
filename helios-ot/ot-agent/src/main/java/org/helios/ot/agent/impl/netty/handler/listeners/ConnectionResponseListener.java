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
import org.helios.helpers.Banner;
import org.helios.ot.agent.AbstractHeliosOTClientImpl;
import org.helios.ot.agent.impl.netty.handler.FilteringInvocationResponseListener;
import org.helios.ot.agent.protocol.impl.ClientProtocolOperation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolResponse;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

/**
 * <p>Title: ConnectionResponseListener</p>
 * <p>Description: The post connection handshake response listener</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.impl.netty.handler.listeners.ConnectionResponseListener</code></p>
 */

public class ConnectionResponseListener implements FilteringInvocationResponseListener {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The helios client thatis being listened for  */
	protected final AbstractHeliosOTClientImpl theClient;
	
	/**
	 * Creates a new ConnectionResponseListener
	 * @param theClient The helios client thatis being listened for
	 */
	public ConnectionResponseListener(AbstractHeliosOTClientImpl theClient) {
		this.theClient = theClient;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.handler.InvocationResponseListener#onInvocationResponse(org.helios.ot.agent.protocol.impl.HeliosProtocolResponse, org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void onInvocationResponse(HeliosProtocolResponse response, ChannelHandlerContext ctx, MessageEvent message) {
		String sessionId = (String)response.getPayload();		
		log.info(Banner.banner("*", 2, 6, "Connection Handshake Repsonse", "Session ID:" + sessionId));
		theClient.postConnectHandshake(sessionId);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.handler.FilteringInvocationResponseListener#isResponseEnabled(org.helios.ot.agent.protocol.impl.HeliosProtocolResponse)
	 */
	@Override
	public boolean isResponseEnabled(HeliosProtocolResponse response) {
		return response.getOp()==ClientProtocolOperation.CONNECT.getOperationCode();
	}

}
