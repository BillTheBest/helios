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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

/**
 * <p>Title: ContinuationBarrier</p>
 * <p>Description: A resultAccessBarrier that prevents threads from progressing until the resultAccessBarrier is dropped.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ContinuationBarrier {
	/** instance logger */
	protected Logger log = Logger.getLogger(getClass());
	/** The name of the resultAccessBarrier */
	protected String lockName = null;
	/** A unique serial number generator for default names. */ 
	protected static AtomicLong serialNumber = new AtomicLong(0);
	/** The prefix for the default name */
	public static final String PREFIX = "ContinuationBarrier#";
	/** Indicates if resultAccessBarrier is up (true) or down (false) */
	protected AtomicBoolean barrierUp = new AtomicBoolean(true);
	/** thread un park counter */
	protected Map<Thread, Integer> unparkCounter = new ConcurrentHashMap<Thread, Integer>();
	
	/** A map of waiting threads */
	protected Map<Long, Thread> waiters = new ConcurrentHashMap<Long, Thread>();
	/** Waiting thread tracking concurrency lock */
	protected ReentrantLock waiterOpLock = new ReentrantLock(true);
	/** Countdown latch barrier */
	protected CountDownLatch barrierLatch = null;
	
	
	/**
	 * Creates a new ContinuationBarrier using a default name and the resultAccessBarrier up.
	 */
	public ContinuationBarrier() {
		this(PREFIX + serialNumber.incrementAndGet(), true);
	}
	
	/**
	 * Creates a new ContinuationBarrier using the provided name and the resultAccessBarrier up.
	 * @param name The name of the resultAccessBarrier.
	 */
	public ContinuationBarrier(String name) {
		this(name, true);
	}
	
	
	/**
	 * Creates a new ContinuationBarrier using a default name and the resultAccessBarrier state provided.
	 * @param barrierUp true if resultAccessBarrier should be up, false if not.
	 */
	public ContinuationBarrier(boolean barrierUp) {
		this(PREFIX + serialNumber.incrementAndGet(), barrierUp);
	}
	
	/**
	 * Creates a new ContinuationBarrier using the provided name and the resultAccessBarrier state provided.
	 * @param name the resultAccessBarrier name.
	 * @param barrierUp true if resultAccessBarrier should be up, false if not.
	 */
	public ContinuationBarrier(String name, boolean barrierUp) {
		lockName = name;
		this.barrierUp.set(barrierUp);
		barrierLatch = new CountDownLatch(Integer.MAX_VALUE);
		log = Logger.getLogger(getClass().getName() + "." + lockName);
	}
	
	protected void park() {
		if(log.isDebugEnabled())log.debug("Calling park");
		waiters.put(Thread.currentThread().getId(), Thread.currentThread());
		try {
			Thread.currentThread().join();
		} catch (InterruptedException ignore) {
			Thread.interrupted();
		}
		if(log.isDebugEnabled())log.debug("Park was signalled");
	}
	
	protected void park(long timeOut, TimeUnit unit) throws InterruptedException {
		if(log.isDebugEnabled())log.debug("Calling park");
		waiters.put(Thread.currentThread().getId(), Thread.currentThread());
		try {
			Thread.currentThread().join(TimeUnit.MILLISECONDS.convert(timeOut,unit));
		} catch (InterruptedException ignore) {
			Thread.interrupted();
		}
		if(log.isDebugEnabled())log.debug("Park was signalled");
	}
	
	
	protected void unpark() {
		if(log.isDebugEnabled())log.debug("Calling unpark on " + waiters.size());
		Iterator<Thread> waitingThreads = waiters.values().iterator();
		while(waitingThreads.hasNext()) {
			waitingThreads.next().interrupt();
			waitingThreads.remove();
		}
		if(log.isDebugEnabled())log.debug("Completed unpark");
	}
	
	
	
//	protected synchronized void park() {
//		if(log.isDebugEnabled())log.debug("Calling park");
//		if(unparkCounter.containsKey(Thread.currentThread())) {
//			int unparks = unparkCounter.get(Thread.currentThread());
//			if(log.isDebugEnabled())log.debug("Unpark Counter:" + unparks);
//			for(int i = 0; i < unparks; i++) {
//				java.util.concurrent.locks.LockSupport.park();
//			}
//			if(log.isDebugEnabled())log.debug("Unparks Counted Down");
//		}
//		if(log.isDebugEnabled())log.debug("Calling LockSupport.park");
//		java.util.concurrent.locks.LockSupport.park();
//	}
	
//	protected void park(long timeOut, TimeUnit unit) {
//		if(unparkCounter.containsKey(Thread.currentThread())) {
//			int parks = unparkCounter.get(Thread.currentThread());			
//			for(int i = 0; i < parks; i++) {
//				java.util.concurrent.locks.LockSupport.park();
//			}
//		}
//		java.util.concurrent.locks.LockSupport.park(TimeUnit.NANOSECONDS.convert(timeOut, unit));
//	}
	
	
//	protected void unpark(Thread thread) {
//		Integer i = unparkCounter.get(thread);
//		if(i==null) {
//			i = new Integer(0);
//		}
//		i++;
//		unparkCounter.put(thread, i);
//		if(log.isDebugEnabled())log.debug("Thread(" + thread.getName() + " Unpark Count:" + i);
//		java.util.concurrent.locks.LockSupport.unpark(thread);
//	}
	
	protected int getUnparkCount(Thread thread) {
		Integer i = unparkCounter.get(thread);
		if(i==null) return 0;
		else return i;
	}
	
	/**
	 * Renders a string containing the resultAccessBarrier name, state and number of waiting threads.
	 * @return the name and state of the resultAccessBarrier.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder(lockName);
		buff.append("/State:").append(barrierUp.get() ? "Up" : "Down");
		buff.append("/Waiters:").append(getWaiterCount());
		buff.append("/Unpark Counter:").append(getUnparkCount(Thread.currentThread()));
		return buff.toString();
	}
	
	/**
	 * Causes the current thread to wait until the resultAccessBarrier drops unless the thread is interrupted. 
	 * @throws InterruptedException if the current thread is interrupted while waiting.
	 */
	public void await() throws InterruptedException {
		if(!barrierUp.get()) return;
		else {
			if (Thread.interrupted()) throw new InterruptedException();
			try {
				park();
				if (Thread.interrupted()) throw new InterruptedException();
				return;  // clean return on account of resultAccessBarrier being dropped.
			} finally {
			}
		}
	}
	
	/**
	 * Causes the current thread to wait until the resultAccessBarrier drops unless the thread is interrupted or the specified timeout elapses.
	 * @param timeOut the maximum time to wait
	 * @param unit the time unit of the timeout argument. 
	 * @throws InterruptedException if the current thread is interrupted while waiting.
	 * @throws TimeoutException if the timeout period elapses.
	 */
	public void await(long timeOut, TimeUnit unit) throws InterruptedException, TimeoutException {
		if(!barrierUp.get()) return;
		else {
			if (Thread.interrupted()) throw new InterruptedException();
			try {
				waiterOpLock.lock();
				park(timeOut, unit);
				if(barrierUp.get()) throw new TimeoutException();				
				return;  // clean return on account of resultAccessBarrier being dropped.
			} finally {
				waiterOpLock.unlock();
			}
		}
	}
	
	/**
	 * Indicates if the resultAccessBarrier is currently up.
	 * @return true if the resultAccessBarrier is up, false if it is down.
	 */
	public boolean isBarrierUp() {
		return barrierUp.get();
	}
	
	/**
	 * Raises the resultAccessBarrier unless it is already up.
	 * @return true if the resultAccessBarrier was raised, false if it was already up.
	 */
	public boolean raiseBarrier() {
		if(!isBarrierUp()) {
//			for(Thread t: waiters.values()) {  // hopefully not necessary, but release and clear the waiters before raising the resultAccessBarrier.
//				unpark(t);
//				Thread.yield();
//				waiters.remove(t.getId());
//			}
			barrierLatch = new CountDownLatch(Integer.MAX_VALUE);
			unpark();
			barrierUp.set(true);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Drops the resultAccessBarrier and releases all the waiting threads unless the resultAccessBarrier is already dropped.
	 * @return true if the resultAccessBarrier was dropped, false if it was already down.
	 */
	public boolean dropBarrier() {
		if(isBarrierUp()) {
			barrierUp.set(false);
//			for(Thread t: waiters.values()) {  
//				unpark(t);
//				Thread.yield();
//				waiters.remove(t.getId()); // redundant ?
//			}
			unpark();
			return true;			
		} else {
			return false;
		}
	}
	
	/**
	 * Returns the number of threads waiting on the resultAccessBarrier.
	 * @return the number of resultAccessBarrier waiters.
	 */
	public int getWaiterCount() {
		try {
			waiterOpLock.lock();
			return waiters.size();
		} finally {
			waiterOpLock.unlock();
		}
		
	}
	
	public static void log(Object message) {
		System.out.println("[" + new Date() + "]:" + message);
	}

	public static void main(String[] args) {
		log("Test ContinuationBarrier");
		
		
		final ContinuationBarrier barrier = new ContinuationBarrier("Test ContinuationBarrier", true);
		Runtime.getRuntime().addShutdownHook(new Thread() {public void run() {log("On Exit State:" + barrier.toString());}});
		int i = 0;
		log("State:" + barrier.toString());
		for(i = 0; i < 10; i++) {
			CWorker worker = new CWorker(i, barrier);
			worker.start();
		}
		Thread.yield();
		log("Reseting Barrier");
		log("State Before Reset:" + barrier.toString());		
		barrier.dropBarrier();
		Thread.yield();
		log("State After Reset:" + barrier.toString());
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}
		barrier.raiseBarrier();
		for(i = 10; i < 20; i++) {
			CWorker worker = new CWorker(i, barrier);
			worker.start();
		}
		Thread.yield();
		log("Reseting Barrier");
		log("State Before Reset:" + barrier.toString());		
		barrier.dropBarrier();
		Thread.yield();
		log("State After Reset:" + barrier.toString());
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}
		barrier.raiseBarrier();
		
	}
	
	
}

class CWorker extends Thread {
	protected int id = 0;
	protected ContinuationBarrier barrier = null;
	/**
	 * @param id
	 */
	public CWorker(int id, ContinuationBarrier barrier) {
		this.barrier = barrier;
		this.id = id;
		this.setName("WorkerThread-" + id);
		this.setDaemon(false);
	}
	
	public void run() {
		log("Thread [" + getName() + "] Waiting on [" + barrier.toString() + "]");
		try {
			barrier.await();
			log("Thread [" + getName() + "] Completed --->" + barrier.toString());
		} catch (Exception e) {
			log("Thread [" + getName() + "] Failed with exception:" + e);
		}
		
	}
	
	public static void log(Object message) {
		System.out.println("[" + new Date() + "]:" + message);
	}
}
