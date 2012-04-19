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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.jboss.netty.channel.Channel;

/**
 * <p>Title: ChannelOutputStream</p>
 * <p>Description: An output stream implementation that writes all the writes it receives to the supplied Netty Channel</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.listener.helios.protocol.ChannelOutputStream</code></p>
 */
public class ChannelOutputStream extends OutputStream {
	/** The channel that the output bytes will be written to */
	protected final Channel channel;
	
	/**
	 * Creates a new ChannelOutputStream
	 * @param channel The channel that the output bytes will be written to
	 */
	public ChannelOutputStream(Channel channel) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null", new Throwable());
		this.channel = channel;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] b) throws IOException {		
		channel.write(b);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {		
		channel.write(Arrays.copyOfRange(b, off, off+len) );
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		channel.write(b);
	}

}
