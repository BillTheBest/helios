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
package org.helios.jmxenabled.counters;



import gnu.trove.list.array.TLongArrayList;

import java.util.concurrent.atomic.AtomicLong;


/**
 * <p>Title: LongRollingCASCounter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.counters.LongRollingCASCounter</code></p>
 */

public class LongRollingCASCounter implements IRollingCounter {
	/** The name of the counter */
	protected final String name;
	/** The maximum size and capacity of the counter */
	protected final int capacity;
	/** The underlying long values counter */
	protected final TLongArrayList tLong;
	/** The update ticket counter */
	protected final AtomicLong ticketCounter = new AtomicLong(0L);
	/** The action counter */
	protected final AtomicLong actionCounter = new AtomicLong(1L);

	/**
	 * Creates a new LongRollingCASCounter 
	 * @param name The name of the counter
	 * @param capacity The maximum size of the rolling counter
	 */
	public LongRollingCASCounter(String name, int capacity) {
		this.name = name;
		this.capacity = capacity;
		tLong = new TLongArrayList(capacity);
	}

	public void put(long value) {
		long ticket = ticketCounter.incrementAndGet();
		//if("CAS".equals(name)) log("[Thread-" + Thread.currentThread().getId() + "  Ticket:" + ticket + "  Action:" + actionCounter.get());
		//Thread.currentThread().setName("[Thread-" + Thread.currentThread().getId() + "  Ticket:" + ticket + "  Action:" + actionCounter.get());
		while(actionCounter.get()!=ticket) {
			Thread.yield();
		}
		_put(value);
//		if(actionCounter.compareAndSet(ticket, nextTicket)) {
//			System.err.println("CAS Barrier Failed [" + ticket + "/" + nextTicket + "]");
//		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	
	private void _put(long value) {
		tLong.insert(0, value);
		int newSize = tLong.size(); 
		if(newSize>capacity) {
			tLong.remove(newSize-1);
		}
		actionCounter.incrementAndGet();
	}

}
