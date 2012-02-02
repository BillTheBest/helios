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
package org.helios.net.ssh.instrumentedio;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: StreamInstrumentationSupport</p>
 * <p>Description: Support class for implemening instrumented byte traffic providers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.instrumentedio.StreamInstrumentationSupport</code></p>
 */

public class StreamInstrumentationSupport implements BytesOutMetric, BytesInMetric {
	/** The number of bytes read */
	protected final AtomicLong bytesRead = new AtomicLong(0L);
	/** The number of bytes written */
	protected final AtomicLong bytesWritten= new AtomicLong(0L);	
	/** The timestamp of the last byte count reset */
	protected final AtomicLong resetTime = new AtomicLong(System.currentTimeMillis());
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.instrumentedio.ResetableMetrics#resetMetrics()
	 */
	@Override
	public void resetMetrics() {
		bytesRead.set(0L);
		bytesWritten.set(0L);
		resetTime.set(System.currentTimeMillis());

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.instrumentedio.ResetableMetrics#getLastResetTimestamp()
	 */
	@Override
	public long getLastResetTimestamp() {
		return resetTime.get();		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.instrumentedio.ResetableMetrics#getLastResetDate()
	 */
	@Override
	public Date getLastResetDate() {
		return new Date(resetTime.get());
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.instrumentedio.BytesInMetric#getBytesIn()
	 */
	@Override
	public long getBytesIn() {
		return bytesRead.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.instrumentedio.BytesOutMetric#getBytesOut()
	 */
	@Override
	public long getBytesOut() {
		return bytesWritten.get();
	}
	
	/**
	 * Increments the read bytes
	 * @param bytes the number of bytes read
	 */
	public void read(long bytes) {
		if(bytes>0) {
			bytesRead.addAndGet(bytes);
		}
	}
	
	/**
	 * Increments the written bytes
	 * @param bytes the number of bytes written
	 */
	public void write(long bytes) {
		if(bytes>0) {
			bytesWritten.addAndGet(bytes);
		}
	}
	

}
