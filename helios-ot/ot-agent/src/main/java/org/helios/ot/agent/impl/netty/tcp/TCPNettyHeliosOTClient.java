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
package org.helios.ot.agent.impl.netty.tcp;

import java.io.Serializable;
import java.util.concurrent.Executor;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.agent.impl.netty.AbstractNettyHeliosOTClient;
import org.helios.ot.helios.ClientProtocolOperation;
import org.helios.ot.helios.HeliosProtocolInvocation;
import org.helios.ot.trace.Trace;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.SocketChannel;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 * <p>Title: TCPNettyHeliosOTClient</p>
 * <p>Description: The TCP protocol HeliosOTClient implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.impl.netty.tcp.TCPNettyHeliosOTClient</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class TCPNettyHeliosOTClient extends AbstractNettyHeliosOTClient {
	/**  */
	private static final long serialVersionUID = 4505329393495223651L;
	/** The channel socket factory */
	protected NioClientSocketChannelFactory socketChannelFactory;
	/** The channel socket  */
	protected SocketChannel socketChannel;	
	/** The boss thread pool */
	protected Executor bossExecutor;

	/** The default listening port */
	public static final int DEFAULT_LISTENING_PORT = 9428;
	/**
	 * Creates a new TCPNettyHeliosOTClient
	 */
	public TCPNettyHeliosOTClient() {

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.AbstractNettyHeliosOTClient#getDefaultTargetPort()
	 */
	protected int getDefaultTargetPort() {
		return DEFAULT_LISTENING_PORT;
	}
	/**
	 * Returns the connection channel for this client instance
	 * @return the connection channel for this client instance
	 */
	public Channel getConnectionChannel() {
		return socketChannel;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#getProtocol()
	 */
	@Override
	@JMXAttribute(name="Protocol", description="The name of the remoting protocol implemented by this client", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getProtocol() {
		return "tcp";
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doSubmitTraces(org.helios.ot.trace.Trace[])
	 */
	@Override
	protected void doSubmitTraces(Trace[] traces) {
		socketChannel.write(HeliosProtocolInvocation.newInstance(ClientProtocolOperation.TRACE, traces)).addListener(sendListener);
	}


}
