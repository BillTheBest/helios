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
package org.helios.server.ot.listener.helios.protocol.jmx;

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;

/**
 * <p>Title: ChannelJMXWrapper</p>
 * <p>Description: JMX wrapper for a Netty channel</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.listener.helios.protocol.jmx.ChannelJMXWrapper</code></p>
 */

public class ChannelJMXWrapper implements ChannelMXBean {
	/** The wrapped channel */
	private final Channel channel;
	/**
	 * Creates a new ChannelJMXWrapper
	 * @param channel The channel to wrap
	 */
	public ChannelJMXWrapper(Channel channel) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null", new Throwable());
		this.channel = channel;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#getId()
	 */
	@Override
	public int getId() {
		return channel.getId();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#getLocalAddress()
	 */
	@Override
	public String getLocalAddress() {
		return channel.getLocalAddress().toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#getRemoteAddress()
	 */
	@Override
	public String getRemoteAddress() {
		return channel.getRemoteAddress().toString();		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#isBound()
	 */
	@Override
	public boolean isBound() {
		return channel.isBound();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return channel.isConnected();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return channel.isOpen();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#isReadable()
	 */
	@Override
	public boolean isReadable() {
		return channel.isReadable();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return channel.isWritable();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#unbind()
	 */
	public void unbind() {
		channel.unbind();

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#close()
	 */
	public void close() {
		channel.close();

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean#disconnect()
	 */
	public void disconnect() {
		channel.disconnect();

	}

}
