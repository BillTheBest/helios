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
package org.helios.containers;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: TimeoutQueue</p>
 * <p>Description: A queue implementation where queued items timeout and are removed from the queue internally if not retrieved within the defined timeout.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class TimeoutQueue<E> implements Queue<E> {
	/** The timeout period */
	protected long timeout = 0L;
	/** The timeout unit */
	protected TimeUnit timeUnit = TimeUnit.MILLISECONDS;
	/** The backing queue */
	protected ArrayBlockingQueue<E> backingQueue = null;
	/** Registered listeners */
	protected Set<TimedOutItemListener<E>> listeners = new CopyOnWriteArraySet<TimedOutItemListener<E>>();
	/** The scheduler to fire off purges of timed out items */	
	protected static ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1, new TQThreadFactory());
	/** A map of scheduledFutures keyed by queued items */
	protected Map<E, ScheduledFuture> futures = null;
	/** The defaul capacity of the queue */
	public static final int DEFAULT_CAPACITY = 128;
	/** The default fairness of the queue */
	public static final boolean DEFAULT_FAIRNESS = false;
	
	
	/**
	 * @param capacity
	 * @param fair
	 * @param timeout
	 * @param timeUnit
	 */
	public TimeoutQueue(int capacity, boolean fair, long timeout, TimeUnit timeUnit) {
		backingQueue = new ArrayBlockingQueue<E>(capacity, fair);
		this.timeout = timeout;
		this.timeUnit = timeUnit;
		futures = Collections.synchronizedMap(new IdentityHashMap<E, ScheduledFuture>(capacity));
	}
	
	/**
	 * @param timeout
	 * @param timeUnit
	 */
	public TimeoutQueue(long timeout, TimeUnit timeUnit) {
		this(DEFAULT_CAPACITY, DEFAULT_FAIRNESS, timeout, timeUnit);
	}
	
	/**
	 * Registers a new listener to be notified when items are timed out of the queue.
	 * @param tl The listener to register.
	 */
	public void registerTimedOutEventListener(TimedOutItemListener<E> tl) {
		if(tl!=null) {
			listeners.add(tl);
		}
	}
	
	/**
	 * Removes a registered timedout item listener.
	 * @param tl The listener to unregister.
	 */
	public void removeTimedOutEventListener(TimedOutItemListener<E> tl) {
		if(tl!=null) {
			listeners.remove(tl);
		}
	}


	/**
	 * Schedules a task to remove a queued item after the timeout period.
	 * The scheduledFuture is placed into the futures map keyed by the queuedItem.
	 * @param queuedItem The item to remove from the timeout queue.
	 */
	protected void scheduleTimeOut(final E queuedItem) {
		if(queuedItem==null) return;
		final TimeoutQueue<E> tq = this;
		futures.put(queuedItem, 
			scheduler.schedule(new Runnable() {
			    public void run() {
			    	try {
			    		backingQueue.remove(queuedItem);
			    		futures.remove(queuedItem);
			    		for(TimedOutItemListener<E> tl: listeners) {
			    			tl.itemTimedOut(queuedItem, tq);
			    		}
			    	} catch (Exception e) {}
			    }}, timeout, timeUnit)
			 );
	}
	
	/**
	 * Cancels the scheduled task for the passed item.
	 * @param queuedItem The item that was removed from the queue before the timeout period.
	 */
	protected void cancel(E queuedItem) {
		if(queuedItem==null) return;
		ScheduledFuture sf = futures.remove(queuedItem);
		if(sf!=null) {
			sf.cancel(true);
		}
	}
	
	

	/**
	 * @param e
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#add(java.lang.Object)
	 */
	public boolean add(E e) {
		scheduleTimeOut(e);
		return backingQueue.add(e);
	}


	/**
	 * @param c
	 * @return
	 * @see java.util.AbstractQueue#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends E> c) {
		for(E e: c) { scheduleTimeOut(e); }
		return backingQueue.addAll(c);
	}


	/**
	 * 
	 * @see java.util.concurrent.ArrayBlockingQueue#clear()
	 */
	public void clear() {
		for(E e: backingQueue) { cancel(e); }
		backingQueue.clear();
	}


	/**
	 * @param o
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		return backingQueue.contains(o);
	}


	/**
	 * @param c
	 * @return
	 * @see java.util.AbstractCollection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		return backingQueue.containsAll(c);
	}


	/**
	 * @param c
	 * @param maxElements
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#drainTo(java.util.Collection, int)
	 */
	public int drainTo(Collection<? super E> c, int maxElements) {
		for(E e: backingQueue) { cancel(e); }  // Need to reimplement. The drain may be slow or be stalled.
		return backingQueue.drainTo(c, maxElements);
	}


	/**
	 * @param c
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#drainTo(java.util.Collection)
	 */
	public int drainTo(Collection<? super E> c) {
		for(E e: backingQueue) { cancel(e); }  // Need to reimplement. The drain may be slow or be stalled.
		return backingQueue.drainTo(c);
	}


	/**
	 * @return
	 * @see java.util.AbstractQueue#element()
	 */
	public E element() {
		return backingQueue.element();
	}


	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return backingQueue.equals(obj);
	}


	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return backingQueue.hashCode();
	}


	/**
	 * @return
	 * @see java.util.AbstractCollection#isEmpty()
	 */
	public boolean isEmpty() {
		return backingQueue.isEmpty();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#iterator()
	 */
	public Iterator<E> iterator() {
		return backingQueue.iterator();
	}


	/**
	 * @param e
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.ArrayBlockingQueue#offer(java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	public boolean offer(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		if(backingQueue.offer(e, timeout, unit)) {
			this.scheduleTimeOut(e);
			return true;
		} else {
			return false;
		}
	}


	/**
	 * @param e
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#offer(java.lang.Object)
	 */
	public boolean offer(E e) {
		if(backingQueue.offer(e)) {
			this.scheduleTimeOut(e);
			return true;
		} else {
			return false;
		}			
	}


	/**
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#peek()
	 */
	public E peek() {
		return backingQueue.peek();
	}


	/**
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#poll()
	 */
	public E poll() {
		E e = backingQueue.poll();
		if(e!=null) this.cancel(e);
		return e;
	}


	/**
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.ArrayBlockingQueue#poll(long, java.util.concurrent.TimeUnit)
	 */
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		E e = backingQueue.poll(timeout, unit);
		if(e!=null) this.cancel(e);
		return e;		
	}


	/**
	 * @param e
	 * @throws InterruptedException
	 * @see java.util.concurrent.ArrayBlockingQueue#put(java.lang.Object)
	 */
	public void put(E e) throws InterruptedException {
		scheduleTimeOut(e);
		backingQueue.put(e);
	}


	/**
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#remainingCapacity()
	 */
	public int remainingCapacity() {
		return backingQueue.remainingCapacity();
	}


	/**
	 * @return
	 * @see java.util.AbstractQueue#remove()
	 */
	public E remove() {
		E e = backingQueue.remove();
		cancel(e);
		return e;
	}


	/**
	 * @param o
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		if(backingQueue.remove(o)) {
			try {
				cancel((E) o);
			} catch (Exception e) {}
			return true;
		} else {
			return false;
		}
	}


	/**
	 * @param c
	 * @return
	 * @see java.util.AbstractCollection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
		boolean affected = false;
		if(c!=null) {
			for(Object o: c) {
				affected = remove(o);
			}
		}
		return affected;
	}


	/**
	 * @param c
	 * @return
	 * @see java.util.AbstractCollection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
		boolean affected = false;
		if(c!=null) {
			for(E e: backingQueue) {
				if(!c.contains(e)) {
					affected = remove(e);
				}				
			}
		}
		return affected;
	}


	/**
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#size()
	 */
	public int size() {
		return backingQueue.size();
	}


	/**
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.ArrayBlockingQueue#take()
	 */
	public E take() throws InterruptedException {
		E e = backingQueue.take();
		cancel(e);
		return e;
	}


	/**
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#toArray()
	 */
	public Object[] toArray() {
		return backingQueue.toArray();
	}


	/**
	 * @param <T>
	 * @param a
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#toArray(T[])
	 */
	public <T> T[] toArray(T[] a) {
		return backingQueue.toArray(a);
	}


	/**
	 * @return
	 * @see java.util.concurrent.ArrayBlockingQueue#toString()
	 */
	public String toString() {
		return backingQueue.toString();
	}
	
	/**
	 * <p>Title: TQThreadFactory</p>
	 * <p>Description: Thread creator for the TimeoutQueue.</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 * @version $LastChangedRevision$
	 * $HeadURL$
	 * $Id$
	 */
	public static class TQThreadFactory implements ThreadFactory {

		/* 
		 * @param r
		 * @return
		 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
		 */		
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "HeliosTimeoutQueue-PurgeThread");
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY+1);
			return t;
		}

	}
	
	public static void main(String args[]) {
		log("TimeoutQueueTest");
		long timeout = 20L;
		TimeoutQueue<String> tq = new TimeoutQueue<String>(timeout, TimeUnit.MILLISECONDS);
		tq.registerTimedOutEventListener(new TimedOutItemListener<String>() {
				public void itemTimedOut(String e, TimeoutQueue<String> tq) {
					log("Item [" + e + "] Timed Out of Queue. New Queue Size:" + tq.size());
				}		
			}		
		);
		tq.add("First String");
		assert tq.size()==1;
		try {Thread.sleep(10);} catch (Exception e) {}
		tq.add("Second String");
		assert tq.size()==2;
		try {Thread.sleep(10);} catch (Exception e) {}
		assert tq.size()==1;
		try {Thread.sleep(10);} catch (Exception e) {}
		assert tq.size()==0;
		log("Test Complete");
	}
	
	public static void log(Object message) {
		System.out.println("[" + new Date() + "]" + message);
	}

	
}

