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
package test.org.helios.ot.trace;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;

/**
 * <p>Title: ConcurrentMapUpdateTestCase</p>
 * <p>Description: POC for thread safe updates of a ConcurrentHashMap</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.trace.ConcurrentMapUpdateTestCase</code></p>
 */
@Ignore
public class ConcurrentMapUpdateTestCase {
	//public static final int THREAD_COUNT = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()*2;
	public static final int THREAD_COUNT = 20;
	public static final long LOOP_COUNT = 1000000;
	public static final long TOTAL = THREAD_COUNT * LOOP_COUNT;
	public static final Integer KEY = new Integer(0);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("ConcurrentMapUpdateTestCase");
		ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(true);
		runTest(false, false);
		runTest(true, false);
		runTest(false, true);
		runTest(true, true);
		
	}

	/**
	 * 
	 */
	private static void runTest(final boolean enqueue, final boolean printSummary) {
		final CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT); 
		final CountDownLatch startLatch = new CountDownLatch(1);
		final Map<Integer, ThreadAwareCounter> map = new ConcurrentHashMap<Integer, ThreadAwareCounter>(1);
		final AtomicLong waits = new AtomicLong(0);
		final AtomicLong waitTime = new AtomicLong(0);
		final AtomicLong blocks = new AtomicLong(0);
		final AtomicLong blockTime = new AtomicLong(0);
		final ThreadAwareCounter twc = new ThreadAwareCounter(enqueue);
		map.put(KEY, twc);
		for(int i = 0; i < THREAD_COUNT; i++) {
			new Thread("CounterThread#" + i){
				public void run() {
					try {
						startLatch.await();
						ThreadInfo ti = ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId());						
						waits.addAndGet(ti.getWaitedCount()*-1);
						waitTime.addAndGet(ti.getWaitedTime()*-1);
						blocks.addAndGet(ti.getBlockedCount()*-1);
						blockTime.addAndGet(ti.getBlockedTime()*-1);
						
						for(int x = 0; x < LOOP_COUNT; x++) {
							if(enqueue) map.get(KEY).enqueue(); 
							else map.get(KEY).increment();							
						}
						ti = ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId());
						waits.addAndGet(ti.getWaitedCount());
						waitTime.addAndGet(ti.getWaitedTime());
						blocks.addAndGet(ti.getBlockedCount());
						blockTime.addAndGet(ti.getBlockedTime());
						
//						log("[" + Thread.currentThread().getName() + "]  Blocks:" +
//								ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId()).getBlockedCount() + 
//								"  Block Time: " + ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId()).getBlockedTime() + 
//								"  Waits: " + ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId()).getWaitedCount() +
//								"  Wait Time: " + ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId()).getWaitedTime()); 
						endLatch.countDown();						
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}.start();
		}
		long start = System.nanoTime();
		startLatch.countDown();
		try { endLatch.await(); } catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
		
		while(twc.getValue()!=TOTAL) {
			Thread.yield();
		}
		long elapsed = System.nanoTime()-start;
		if(printSummary) {
			if(enqueue) {
				log("\t===================\n\tEnqueue Test\n\t===================");
			} else {
				log("\t===================\n\tSynchronized Test\n\t===================");
			}
			log("Test Completed In [" + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS) + "] ms.");
			log("Avg Counter Update Time [" + elapsed(elapsed, TOTAL) + "] ns.  (" + elapsedd(elapsed, TOTAL) + " )");
			log("Counter:" + map.get(KEY).getValue() + ", Expected:" + (LOOP_COUNT * THREAD_COUNT));
			log("Waits:" + waits + "  Wait Time:" + (waitTime.get()/THREAD_COUNT));
			log("Blocks:" + blocks + " Block Time:" + (blockTime.get()/THREAD_COUNT));
		} else {
			if(enqueue) {
				log("Enqueue Warmup:" + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS) + " ms.");
			} else {
				log("Synchronized Warmup:" + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS) + " ms.");
			}			
		}
	}
	
	static long elapsed(double elapsed, double total) {
		double speed = elapsed/total;
		return (long) speed;
	}

	static double elapsedd(double elapsed, double total) {
		return elapsed/total;
	}

	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}

class ThreadAwareCounter {
	long counter = 0;
	final AtomicInteger threads = new AtomicInteger(0);
	final AtomicLong atomicCounter= new AtomicLong(0);
	final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>((int)(
			ConcurrentMapUpdateTestCase.LOOP_COUNT*ConcurrentMapUpdateTestCase.THREAD_COUNT), false);
	final boolean q;
	public ThreadAwareCounter(boolean q) {
		this.q=q;
		if(q) {
			int tCount = ConcurrentMapUpdateTestCase.THREAD_COUNT/2;
			if(tCount==0) tCount = 1;
			for(int i = 0; i < ConcurrentMapUpdateTestCase.THREAD_COUNT/2; i++) {
				Thread t = new Thread() {
					public void run() {
						while(true) {
							try {
								queue.take();
								atomicCounter.incrementAndGet();
							} catch (Exception e) {
								e.printStackTrace(System.err);
							}
						}
					}
				};
				t.setDaemon(true);
				t.setPriority(Thread.MAX_PRIORITY);
				t.start();
			}
		}
	}
	
	public long getValue() {
		if(q) return atomicCounter.get();
		else return counter;
	}
	public void enqueue() {
		if(!queue.add(ConcurrentMapUpdateTestCase.KEY)) {
			log("!!!! Failed to put !!!");
		}
	}
	
	public synchronized void increment() {
		int tCount = threads.incrementAndGet();
		if(tCount>1) {
			//log("!!!! Thread Count Is [" + tCount + "] !!!");
		}
		counter++;
		threads.decrementAndGet();
	}
	
	public static void log(Object msg) {
		System.out.println("[" + Thread.currentThread().toString() + "]" + msg);
	}	
}


