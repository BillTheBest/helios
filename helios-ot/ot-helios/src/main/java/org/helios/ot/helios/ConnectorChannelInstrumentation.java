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
				
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;

/**
 * <p>Title: ConnectorChannelInstrumentation</p>
 * <p>Description: A channel handler that tracks the number of bytes transferred</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.ConnectorChannelInstrumentation</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class ConnectorChannelInstrumentation implements ChannelUpstreamHandler, ChannelDownstreamHandler  {
	/** Bytes written out counter */
	protected final AtomicLong bytesWritten = new AtomicLong(0);
	/** Bytes read in counter */
	protected final AtomicLong bytesRead = new AtomicLong(0);
	/** Channel events written out counter */
	protected final AtomicLong eventsWritten = new AtomicLong(0);
	/** Channel events read in counter */
	protected final AtomicLong eventsRead = new AtomicLong(0);
	
	/** Last reset time  */
	protected final AtomicLong lastReset = new AtomicLong(System.currentTimeMillis());
	
	/**
	 * Resets the metrics.
	 */
	@JMXOperation(name="reset", description="Resets the metrics")
	public void reset() {
		bytesWritten.set(0L);
		eventsWritten.set(0L);
		bytesRead.set(0L);
		eventsRead.set(0L);
		lastReset.set(System.currentTimeMillis());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelDownstreamHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		eventsWritten.incrementAndGet();
		measure(e, false);
		ctx.sendDownstream(e);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		eventsRead.incrementAndGet();
		measure(e, true);
		ctx.sendUpstream(e);
	}
	
	
	/**
	 * Reads the number of bytes in the transferring channel buffer
	 * @param ce The channel event
	 */
	public void measure(ChannelEvent ce, boolean up) {
		try {
			 if(ce!=null) {
				 if(ce instanceof MessageEvent) {
					 MessageEvent me = (MessageEvent) ce;
					 if (me.getMessage() instanceof ChannelBuffer) {
						 if(!up) bytesWritten.addAndGet(((ChannelBuffer)me.getMessage()).array().length);
						 else bytesRead.addAndGet(((ChannelBuffer)me.getMessage()).array().length);
					 }
				 }
			 }
		} catch (Exception e) {}
	 }
	
	/**
	 * Returns the number of bytes written out by the connector since the last reset
	 * @return the number of bytes written out by the connector
	 */
	@JMXAttribute(name="BytesWritten", description="The number of bytes written out by the connector since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getBytesWritten() {
		return bytesWritten.get();
	}
	/**
	 * Returns the number of bytes read in by the connector since the last reset
	 * @return the number of bytes read in by the connector
	 */
	@JMXAttribute(name="BytesRead", description="The number of bytes read in by the connector since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getBytesRead() {
		return bytesRead.get();
	}
	
	/**
	 * Returns the number of channel events written out by the connector since the last reset
	 * @return the number of channel events written out by the connector
	 */
	@JMXAttribute(name="ChannelEventsWritten", description="The number of channel events written out by the connector since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getChannelEventsWritten() {
		return eventsWritten.get();
	}

	/**
	 * Returns the number of channel events read in by the connector since the last reset
	 * @return the number of channel events read in by the connector
	 */
	@JMXAttribute(name="ChannelEventsRead", description="The number of channel events read in by the connector since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getChannelEventsRead() {
		return eventsRead.get();
	}
	
	
	/**
	 * Returns the timestamp of the last reset
	 * @return the timestamp of the last reset
	 */
	@JMXAttribute(name="LastReset", description="The timestamp of the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastReset() {
		return lastReset.get();
	}
	
	/**
	 * Returns the date of the last reset
	 * @return the date of the last reset
	 */
	@JMXAttribute(name="LastResetDate", description="The date of the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getLastResetDate() {
		return new Date(lastReset.get());
	}

	

}
