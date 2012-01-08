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
package org.helios.helpers.counters;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: LongCircularCounter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class LongCircularCounter implements CircularCounter, Externalizable {
	/** The core counter */
	protected AtomicLong counter = new AtomicLong(0);
	/** The counter size */
	protected long size = 0L;
	
	/**
	 * Creates a new LongCircularCounter of the specified size.
	 * The size defines the number of <code>increment</code> 
	 * operations required to reset the counter back to zero.
	 * @param size the size of the circular counter.
	 */
	public LongCircularCounter(long size) {
		this.size = size-1;
	}
	
	/**
	 * Creates a new LongCircularCounter of the specified size.
	 * The size defines the number of <code>increment</code> 
	 * operations required to reset the counter back to zero.
	 * @param size the size of the circular counter.
	 */
	public LongCircularCounter(String size) {
		this.size = Long.parseLong(size)-1;
	}
		
	
	/**
	 * @return
	 * @see org.helios.helpers.counters.CircularCounter#getCurrentValue()
	 */
	public Long getCurrentValue() {
		return counter.get();
	}
	
	/**
	 * @return
	 * @see org.helios.helpers.counters.CircularCounter#getSize()
	 */
	public Number getSize() {
		return size+1;
	}


	/**
	 * @return
	 * @see org.helios.helpers.counters.CircularCounter#increment()
	 */
	public boolean increment() {
		long l = counter.incrementAndGet();
		if(l==size) {
			counter.set(0);
			return true;
		}
		return false;
	}	
	
	
	/**
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		counter = new AtomicLong(in.readLong());
	}

	/**
	 * @param out
	 * @throws IOException
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(counter.get());
	}


 

}
