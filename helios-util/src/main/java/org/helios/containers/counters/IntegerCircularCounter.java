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
package org.helios.containers.counters;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Title: IntegerCircularCounter</p>
 * <p>Description: An integer based circular counter.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class IntegerCircularCounter {
	protected AtomicInteger counter = null;
	protected int size = -1;
	protected int trigger = -1;
	protected int startAt = -1;
	
	/**
	 * Creates a new circular counter of the specified size and starting at the specified value.
	 * @param size The size of the counter which indicates how many values will be issued before recycling.
	 * @param startAt The value the counter should start at.
	 */
	public IntegerCircularCounter(int size, int startAt) {
		if(startAt >= (size-1) || startAt < -1) throw new RuntimeException("Start value overflow. Starting value of [" + startAt + "] is out of bounds for a counter of size [" + size + "]");
		counter = new AtomicInteger(startAt);
		this.size = size;
		this.trigger = size-1;
	}
	
	/**
	 * Creates a new circular counter of the specified size and starting at -1.
	 * @param size The size of the counter which indicates how many values will be issued before recycling.
	 */
	public IntegerCircularCounter(int size) {
		this(size, -1);
	}
	
	/**
	 * Increments the counter, resetting it if it has reached the maximum value, and returns the next tick.
	 * @return the next counter
	 */
	public int nextCounter() {
		int value = counter.incrementAndGet();
		if(value>trigger) {
			synchronized(this) {
				if(value>trigger) {
					counter.set(0);
					value = 0;
				}
			}
		}
		return value;
	}
	
	/**
	 * Returns the current counter value with no modification to the counter.
	 * @return the current counter
	 */
	public int currentCounter() {
		return counter.get();
	}
	
	/**
	 * Returns the prior counter value with no modification to the counter.
	 * @return the prior counter
	 */
	public int priorCounter() {
		int c = counter.get();
		c--;
		if(c<0) return trigger;
		else return c;
	}
	
}
