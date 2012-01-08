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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * <p>Title: LongCounterPerf</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.counters.LongCounterPerf</code></p>
 */

public class LongCounterPerf {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("LongCounterPerf Test");
		warmup();
		long loops = 10000000; // 100,000,000
		int size = 20;
		int threadCount = 5;
		Set<Thread> counters = new HashSet<Thread>(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(threadCount);
		
		
		IRollingCounter counter = new LongRollingCounter("CAS", size); 
		
		for(int i = 0; i < threadCount; i++) {
			Thread thread = new CounterPutter(loops, startLatch, endLatch, counter);
			counters.add(thread);
			thread.start();
		}
		
		log("Starting [" + counter.getClass().getSimpleName() + "] Test");
		long start = System.currentTimeMillis();
		startLatch.countDown();
		try { endLatch.await(); } catch (Exception e) { e.printStackTrace(System.err); }
		long elapsed = System.currentTimeMillis()-start;
		log(counter.getClass().getSimpleName() + " Elapsed Time:" + elapsed + " ms.");

		
		
		counters.clear();
		startLatch = new CountDownLatch(1);
		endLatch = new CountDownLatch(threadCount);
		counter = new LongRollingCASCounter("Lock", size); 
		for(int i = 0; i < threadCount; i++) {
			Thread thread = new CounterPutter(loops, startLatch, endLatch, counter);
			counters.add(thread);
			thread.start();
		}
		log("Starting [" + counter.getClass().getSimpleName() + "] Test");
		start = System.currentTimeMillis();
		startLatch.countDown();
		try { endLatch.await(); } catch (Exception e) { e.printStackTrace(System.err); }
		elapsed = System.currentTimeMillis()-start;
		log(counter.getClass().getSimpleName() + " Elapsed Time:" + elapsed + " ms.");
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public static void warmup() {
		long loops = 100000; // 100,000,000
		LongRollingCASCounter counter = new LongRollingCASCounter("20", 20); 
		LongRollingCounter counter2 = new LongRollingCounter("20", 20);
		for(long i = 0; i < loops; i++) {
			counter.put(i);
		}
		for(long i = 0; i < loops; i++) {
			counter2.put(i);
		}
	}

}

class CounterPutter extends Thread {
	final long loops;
	final CountDownLatch startLatch;
	final CountDownLatch endLatch;
	final IRollingCounter counter;
	/**
	 * Creates a new 
	 * @param loops
	 * @param startLatch
	 * @param endLatch
	 * @param counter
	 */
	protected CounterPutter(long loops, CountDownLatch startLatch, CountDownLatch endLatch, IRollingCounter counter) {
		super();
		this.loops = loops;
		this.startLatch = startLatch;
		this.endLatch = endLatch;
		this.counter = counter;
	}
	
	public void run() {
		try {
			startLatch.await();
			for(long i = 0; i < loops; i++) {
				counter.put(i);
				//if(i%10==0) {
					//sleep(10);
				//}
			}			
			endLatch.countDown();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		
	}
}