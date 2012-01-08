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
package org.helios.jmx.threadservices.concurrency;

import java.util.Date;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Title: NamedCyclicBarrier</p>
 * <p>Description: An extension of CyclicBarrier with a defined name and an immediate countdown.
 * The immediate count down calls a reset() but the BrokenBarrierException is supressed. The resultAccessBarrier does not trip until reset.
 * If a thread is the n th thread to await where n is the number of parties, and exception is thrown. 
 * This class still has some concurrency issues but is intended to be a multi purpose resultAccessBarrier to stop all waiting threads until a specific event occurs with each 
 * waiting thread needing to acquire an exclusive lock.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class NamedCyclicBarrier extends CyclicBarrier  {
	/** The default number of parties */
	public static final int DEFAULT_PARTIES = 1024;
	/** the name of the lock */
	protected String lockName = null;
	/** A unique serial number generator for default names. */ 
	protected static AtomicLong serialNumber = new AtomicLong(0);
	/** The prefix for the default name */
	public static final String PREFIX = "NamedCyclicBarrier#";
	/** The number of threads awaiting */
	protected AtomicInteger awaiters = new AtomicInteger(0);
	/** Lock to protect the awaiters increment */
	protected ReentrantLock awaiterLock = new ReentrantLock(true);
	
	public static String testVal = null;


	/**
	 * Creates a new CyclicBarrier with a default name that will cause the waiting threads to wait until the resultAccessBarrier is reset.
	 * Supports the default number of waiting threads (1024).
	 */
	public NamedCyclicBarrier() {
		super(DEFAULT_PARTIES+1);
		lockName = PREFIX + serialNumber.incrementAndGet();
	}
	
	/**
	 * Creates a new CyclicBarrier with the provided name that will cause the waiting threads to wait until the resultAccessBarrier is reset.
	 * Supports the default number of waiting threads (1024).
	 * @param name The name of the resultAccessBarrier.
	 */
	public NamedCyclicBarrier(String name) {
		super(DEFAULT_PARTIES+1);
		lockName = name;
	}
	
	/**
	 * Creates a new CyclicBarrier with a default name that will cause the waiting threads to wait until the resultAccessBarrier is reset.
	 * @param parties The number of waiting threads supported.
	 * @param name The name of the resultAccessBarrier.
	 */
	public NamedCyclicBarrier(int parties, String name) {
		super(parties+1);
		lockName = name;
	}
	
	
	/**
	 * Creates a new CyclicBarrier with a default name that will cause the waiting threads to wait until the resultAccessBarrier is reset. 
	 * @param parties The number of waiting threads supported.
	 */
	public NamedCyclicBarrier(int parties) {
		super(parties+1);
		lockName = PREFIX + serialNumber.incrementAndGet();
	}

	/**
	 * Creates a new CyclicBarrier with a default name that will cause the waiting threads to wait until the resultAccessBarrier is reset.
	 * @param parties The number of waiting threads supported.
	 * @param barrierAction Ignored.
	 */
	public NamedCyclicBarrier(int parties, Runnable barrierAction) {
		this(parties+1);
	}
	
	/**
	 * The name assigned to this resultAccessBarrier.
	 * @return The name of the resultAccessBarrier.
	 */
	public String getName() {
		return lockName;
	}
	
	/**
	 * Returns a string identifying this resultAccessBarrier 
	 * @return a string identifying this resultAccessBarrier
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return lockName + "/" + super.toString();
	}	
	
	/**
	 * Causes the thread to wait until the resultAccessBarrier is reset.
	 * A runtime exception will be thrown if the maximum number of threads are already waiting.
	 * @return the arrival index of the current thread, where index getParties() - 1 indicates the first to arrive and zero indicates the last to arrive. A -1 is returned when the resultAccessBarrier is reset. 
	 * @throws InterruptedException if the current thread was interrupted while waiting 
	 * @throws BrokenBarrierException Not thrown.
	 * @see java.util.concurrent.CyclicBarrier#await()
	 */
	public int await() throws InterruptedException, BrokenBarrierException {
		awaiterLock.lock();
		try {
			if(awaiters.get()+1==this.getParties()) {
				throw new RuntimeException("Barrier Has Maxium Number of Waiting Threads [" + (this.getParties() -1) + "]");
			} else {
				awaiters.incrementAndGet();
			}
		} finally {
			awaiterLock.unlock();
		}
		try {
			return super.await();
		} catch (BrokenBarrierException bbe) {
			return -1;
		} finally {
			awaiters.decrementAndGet();
		}
	}
	
	
	/**
	 * Causes the thread to wait until the resultAccessBarrier is reset or the provided timeout elapsed and an InterruptedException is thrown. 
	 * A runtime exception will be thrown if the maximum number of threads are already waiting.
	 * @param timeOut the time to wait for the resultAccessBarrier
	 * @param unit the time unit of the timeout parameter 
	 * @return the arrival index of the current thread, where index getParties() - 1 indicates the first to arrive and zero indicates the last to arrive. A -1 is returned when the resultAccessBarrier is reset.
	 * @throws InterruptedException if the specified timeout elapses. 
	 * @throws TimeoutException if the specified timeout elapses. 
	 * @throws BrokenBarrierException not thrown.
	 * @see java.util.concurrent.CyclicBarrier#await(long, java.util.concurrent.TimeUnit)
	 */
	public int await(long timeOut, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException  {
		awaiterLock.lock();
		try {
			if(awaiters.get()+1==this.getParties()) {
				throw new RuntimeException("Barrier Has Maxium Number of Waiting Threads [" + (this.getParties() -1) + "]");
			} else {
				awaiters.incrementAndGet();
			}
		} finally {
			awaiterLock.unlock();
		}
		try {
			return super.await(timeOut, unit);
		} catch (BrokenBarrierException bbe) {
			return -1;
		} finally {
			awaiters.decrementAndGet();
		}
	}

	/**
	 * The maximum number of waiting threads.
	 * @return The maximum number of waiting threads.
	 * @see java.util.concurrent.CyclicBarrier#getParties()
	 */
	public int getParties() {
		return super.getParties();
	}
	

	/**
	 * Releases the resultAccessBarrier allowing all waiters to continue, and resets the awaiter count.
	 * @see java.util.concurrent.CyclicBarrier#reset()
	 */
	public void reset() {
		awaiterLock.lock();
		try {
			super.reset();			
		} finally {
			awaiterLock.unlock();
		}		
	}
	
	public static void log(Object message) {
		System.out.println("[" + new Date() + "]:" + message);
	}

	public static void main(String[] args) {
		log("Test NamedCyclicBarrier");
		
		testVal = "ABC";
		final NamedCyclicBarrier barrier = new NamedCyclicBarrier(9, "Test NamedCyclicBarrier");
		Runtime.getRuntime().addShutdownHook(new Thread() {public void run() {log("Waiters On Shutdown:" + barrier.getNumberWaiting() + "/" + barrier.getAwaiters() + "/" + barrier.getAwaiterLockWaiterCount());}});
		int i = 0;
		log("Waiters:" + barrier.getNumberWaiting() + "/" + barrier.getAwaiters());
		for(i = 0; i < 10; i++) {
			Worker worker = new Worker(i, barrier);
			worker.start();
		}
		try {
			Thread.sleep(150000);
		} catch (InterruptedException e) {}
		log("Reseting Barrier");
		log("Waiters Before Reset:" + barrier.getNumberWaiting() + "/" + barrier.getAwaiters() + "/" + barrier.getAwaiterLockWaiterCount());		
		barrier.reset();
		log("Waiters After Reset:" + barrier.getNumberWaiting() + "/" + barrier.getAwaiters() + "/" + barrier.getAwaiterLockWaiterCount());
		for(i = 10; i < 20; i++) {
			Worker worker = new Worker(i, barrier);
			worker.start();
		}
		testVal = "XYZ";
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {}
		log("Reseting Barrier");
		log("Waiters Before Reset:" + barrier.getNumberWaiting() + "/" + barrier.getAwaiters() + "/" + barrier.getAwaiterLockWaiterCount());		
		barrier.reset();
		log("Waiters After Reset:" + barrier.getNumberWaiting() + "/" + barrier.getAwaiters() + "/" + barrier.getAwaiterLockWaiterCount());
		
	}

	/**
	 * @return the awaiters
	 */
	public int getAwaiters() {
		return awaiters.get();
	}

	/**
	 * @return the awaiterLock
	 */
	public int getAwaiterLockWaiterCount() {
		return awaiterLock.getQueueLength();
	}

}

class Worker extends Thread {
	protected int id = 0;
	protected NamedCyclicBarrier barrier = null;
	/**
	 * @param id
	 */
	public Worker(int id, NamedCyclicBarrier barrier) {
		this.barrier = barrier;
		this.id = id;
		this.setName("WorkerThread-" + id);
		this.setDaemon(false);
	}
	
	public void run() {
		log("Thread [" + getName() + "] Waiting on [" + barrier.toString() + "]");
		try {
			barrier.await();
			log("Thread [" + getName() + "] Completed -->" + barrier.testVal);
		} catch (Exception e) {
			log("Thread [" + getName() + "] Failed with exception:" + e);
		}
		
	}
	
	public static void log(Object message) {
		System.out.println("[" + new Date() + "]:" + message);
	}
}
