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

import java.util.HashSet;
import java.util.Set;

import javax.management.MXBean;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;

/**
 * <p>Title: ChannelGroupJMXWrapper</p>
 * <p>Description: JMX wrapper for a channel group</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.listener.helios.protocol.jmx.ChannelGroupJMXWrapper</code></p>
 */
@MXBean
public class ChannelGroupJMXWrapper implements ChannelGroupMXBean {
	/** The wrapped channel group */
	private final ChannelGroup channelGroup;
	
	/**
	 * Creates a new ChannelGroupJMXWrapper
	 * @param channelGroup The channel group to wrap
	 */
	public ChannelGroupJMXWrapper(ChannelGroup channelGroup) {
		if(channelGroup==null) throw new IllegalArgumentException("The passed channel group was null", new Throwable());
		this.channelGroup = channelGroup;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelGroupMXBean#getName()
	 */
	@Override
	public String getName() {
		return channelGroup.getName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelGroupMXBean#getSize()
	 */
	@Override
	public int getSize() {		
		return channelGroup.size();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelGroupMXBean#unbind()
	 */
	public void unbind() {
		channelGroup.unbind();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.listener.helios.protocol.jmx.ChannelGroupMXBean#close()
	 */
	public void close() {
		channelGroup.close();
	}
	
	public ChannelMXBean[] getChannels() {
		Set<ChannelMXBean> channels = new HashSet<ChannelMXBean>(getSize());
		for(Channel channel: channelGroup) {
			channels.add(new ChannelJMXWrapper(channel));
		}
		return channels.toArray(new ChannelMXBean[channels.size()]);
	}

}
