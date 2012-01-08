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
package org.helios.tracing.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: LongCircularCounter</p>
 * <p>Description: Circular counter that iterates over the same range of numbers to requesters.
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1058 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/util/LongCircularCounter.java $
 * $Id: LongCircularCounter.java 1058 2009-02-18 17:33:54Z nwhitehead $
 */
public class LongCircularCounter {
	/** the number generator */
	protected AtomicLong counter = new AtomicLong();
	/** the size of the range of numbers that is returned */
	protected long size = 1L;
	/** the number the counter initializes and resets to */
	protected long startAt = 0L;
	/** the high end number at which the counter resets. */
	protected long range = 0L;
	
	/**
	 * Creates a new LongCircularCounter
	 * @param size The size of the range.
	 * @param startAt The number to start at. This will be the first number returned.
	 */
	public LongCircularCounter(long size, long startAt) {
		this.size = size;
		this.startAt = startAt-1;
		this.range = startAt + size-1;
		counter = new AtomicLong(startAt);
	}
	
	/**
	 * Creates a new LongCircularCounter. Defaults to startAt of 1.
	 * @param size The size of the range.
	 */
	public LongCircularCounter(long size) {
		this(size, 0);
	}
	
	
	/**
	 * Retrieves the next number in the circular range.
	 * @return The next number.
	 */
	public long next() {
		long l = counter.incrementAndGet();
		if(l==range) {
			counter.set(startAt);
		}
		return l;
	}
	
	public static void main(String args[]) {
		LongCircularCounter lcc = new LongCircularCounter(2);
		for(int i = 0; i < 10; i++) {
			System.out.println("Value:" + lcc.next());
		}
		
	}
}
