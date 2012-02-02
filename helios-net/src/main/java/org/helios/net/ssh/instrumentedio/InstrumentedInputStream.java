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
import java.io.InputStream;

/**
 * <p>Title: InstrumentedInputStream</p>
 * <p>Description: A byte counting input stream wrapper</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.instrumentedio.InstrumentedInputStream</code></p>
 */

public class InstrumentedInputStream extends InputStream implements BytesInProvider {
	/** The delegate input stream */
	protected final InputStream inner;
	/** Stream instr support delegate */
	protected final StreamInstrumentationSupport instr = new StreamInstrumentationSupport(); 
	/**
	 * Creates a new InstrumentedInputStream
	 * @param inner The delegate input stream
	 */
	public InstrumentedInputStream(InputStream inner) {
		super();
		if(inner==null) throw new IllegalArgumentException("The delegate input stream was null", new Throwable());
		this.inner = inner;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.instrumentedio.BytesInProvider#getBytesInMetric()
	 */
	@Override
	public BytesInMetric getBytesInMetric() {
		return instr;
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		int b = inner.read();
		instr.read(1);
		return b;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException {
		int br = inner.read(b);
		instr.read(br);
		return br;
	}


	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int br =  inner.read(b, off, len);
		instr.read(br);
		return br;
	}


	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#skip(long)
	 */
	@Override
	public long skip(long n) throws IOException {
		long skipped = inner.skip(n);		
		instr.read(skipped);
		return skipped;
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
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return inner.equals(obj);
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#close()
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







	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#available()
	 */
	@Override
	public int available() throws IOException {
		return inner.available();
	}


	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#mark(int)
	 */
	@Override
	public void mark(int readlimit) {
		inner.mark(readlimit);
	}


	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#reset()
	 */
	@Override
	public void reset() throws IOException {
		inner.reset();
	}


	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#markSupported()
	 */
	@Override
	public boolean markSupported() {
		return inner.markSupported();
	}




}
