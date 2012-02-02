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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: InstrumentedOutputStream</p>
 * <p>Description: A byte counting output stream wrapper</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.instrumentedio.InstrumentedOutputStream</code></p>
 */

public class InstrumentedOutputStream extends OutputStream implements BytesOutProvider {
	/** The delegate output stream */
	protected final OutputStream inner;
	/** Stream instr support delegate */
	protected final StreamInstrumentationSupport instr = new StreamInstrumentationSupport(); 
	
	/**
	 * Creates a new InstrumentedOutputStream
	 * @param inner The delegate output stream
	 */
	public InstrumentedOutputStream(OutputStream inner) {
		super();
		if(inner==null) throw new IllegalArgumentException("The delegate output stream was null", new Throwable());
		this.inner = inner;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.instrumentedio.BytesOutProvider#getBytesOutMetric()
	 */
	@Override
	public BytesOutMetric getBytesOutMetric() {
		return instr;
	}	

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {		
		inner.write(b);
		instr.write(1);

	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return inner.hashCode();
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] b) throws IOException {
		inner.write(b);
		instr.write(b.length);
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		inner.write(b, off, len);
		instr.write(len);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return inner.equals(obj);
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
		inner.flush();
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		inner.close();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return inner.toString();
	}


}
