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
package org.helios.tracing.queues;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.tracing.util.IntegerCircularCounter;

/**
 * <p>Title: AggregatingBlockingQueue</p>
 * <p>Description: An extension of an array blocking queue which delegates most of its methods to an array of underlying array blocking queues.
 * take(), poll(), poll(timeout, unit) and drainTo(...) round robin</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1647 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/queues/AggregatingBlockingQueue.java $
 * $Id: AggregatingBlockingQueue.java 1647 2009-10-24 21:52:31Z nwhitehead $
 */
public class AggregatingBlockingQueue<T> implements BlockingQueue<T> {
	
	private static final long serialVersionUID = 6039701019813469783L;
	protected BlockingQueue<T>[] queues = null;
	protected IntegerCircularCounter counter = null;
	protected AggregatingWorkerThread[] workerThreads = null;
	protected int workerThreadCount = 3;
	protected long zeroQueueSleepTime = 100L;
	protected AtomicInteger queuePullMaxSize = new AtomicInteger(50);
	protected AtomicBoolean started = new AtomicBoolean(false);
	
	
	/**
	 * Creates a new AggregatingBlockingQueue for the passed sub queues.
	 * @param queues The subqueues to drain.
	 */
	@SuppressWarnings("unchecked")
	public AggregatingBlockingQueue(BlockingQueue<T>...queues) {
		this.queues = new BlockingQueue[queues==null ? 0 : queues.length];
		if(queues!=null && queues.length>0) {
			System.arraycopy(queues, 0, queues.length, 0, queues.length);
		}
		counter = new IntegerCircularCounter(queues.length);		
	}

	
	/**
	 * Starts the aggregating worker threads which will drain to the passed executor. 
	 * @param executor The executor service to submit all drained tasks to.
	 */
	public void start(ExecutorService executor) {
		if(started.get()) return;
		workerThreads = new AggregatingWorkerThread[workerThreadCount];
		for(int i = 0; i < workerThreadCount; i++) {
			workerThreads[i] = new AggregatingWorkerThread(queues, executor, zeroQueueSleepTime, queuePullMaxSize);
		}
		for(int i = 0; i < workerThreadCount; i++) {
			workerThreads[i].start();
		}		
		started.set(true);
	}
	
	/**
	 * Stops the aggregating worker threads.
	 */
	public void stop() {
		if(!started.get()) return;
		for(int i = 0; i < workerThreadCount; i++) {
			workerThreads[0].stopMe(false);
		}				
		started.set(false);
	}
	
	/**
	 * Atomically removes all of the elements from this queue.
	 * @see java.util.concurrent.BlockingQueue#clear()
	 */
	public void clear() {
		for(BlockingQueue<T> q: queues) {
			q.clear();
		}
	}
	
	/**
	 * @param o
	 * @return
	 * @see java.util.concurrent.BlockingQueue#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		for(BlockingQueue<T> q: queues) {
			if(q.contains(o)) return true;
		}
		return false;
	}
	
	/**
	 * @param c
	 * @return
	 * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection)
	 */
	public int drainTo(Collection c) {
		return queues[counter.next()].drainTo(c);
	}
	
	/**
	 * @param c
	 * @param maxElements
	 * @return
	 * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection, int)
	 */
	public int drainTo(Collection c, int maxElements) {
		return queues[counter.next()].drainTo(c, maxElements);
	}
	
	/**
	 * @return
	 * @see java.util.concurrent.BlockingQueue#iterator()
	 */
	public Iterator<T> iterator() {
		Set<T> set = new HashSet<T>();
		for(BlockingQueue<T> q: queues) {
			set.addAll(q);
		}
		return set.iterator();
	}
	
	/**
	 * @param object
	 * @return
	 * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object)
	 */
	public boolean offer(T object) {
		throw new UnsupportedOperationException("[AggregatingBlockingQueue] This is a read only queue.");
	}

	/**
	 * @param object
	 * @param timeOut
	 * @param unit
	 * @return
	 * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	public boolean offer(T object, long timeOut, TimeUnit unit) {
		throw new UnsupportedOperationException("[AggregatingBlockingQueue] This is a read only queue.");
	}
	
	/**
	 * @param object
	 * @see java.util.concurrent.BlockingQueue#put(java.lang.Object)
	 */
	public void put(T object) {
		throw new UnsupportedOperationException("[AggregatingBlockingQueue] This is a read only queue.");
	}
	
	/**
	 * @return
	 * @see java.util.concurrent.BlockingQueue#peek()
	 */
	public T peek() {
		T value = null;		
		for(BlockingQueue<T> q: queues) {
			value = q.peek();
			if(value!=null) return value;
		}
		return null;
	}
	
	/**
	 * @return
	 * @see java.util.concurrent.BlockingQueue#poll()
	 */
	public T poll() {
		return queues[counter.next()].poll();
	}
	
	/**
	 * @param timeOut
	 * @param unit
	 * @return
	 * @see java.util.concurrent.BlockingQueue#poll(long, java.util.concurrent.TimeUnit)
	 */
	public T poll(long timeOut, TimeUnit unit) {
		return queues[counter.next()].poll();
	}
	
	/**
	 * @return
	 * @see java.util.concurrent.BlockingQueue#remainingCapacity()
	 */
	public int remainingCapacity() {
		int i = 0;
		for(BlockingQueue<T> q: queues) {
			i += q.remainingCapacity();
		}
		return i;
	}
	
	/**
	 * @param object
	 * @return
	 * @see java.util.concurrent.BlockingQueue#remove(java.lang.Object)
	 */
	public boolean remove(Object object) {
		for(BlockingQueue<T> q: queues) {
			if(q.remove(object)) return true;
		}
		return false;	
	}
	
	/**
	 * @return
	 * @see java.util.concurrent.BlockingQueue#size()
	 */
	public int size() {
		int i = 0;
		for(BlockingQueue<T> q: queues) {
			i += q.size();
		}
		return i;		
	}
	
	/**
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.BlockingQueue#take()
	 */
	public T take() throws InterruptedException {
		return queues[counter.next()].take();
	}
	
	/**
	 * @return
	 * @see java.util.concurrent.BlockingQueue#toArray()
	 */
	public T[] toArray() {
		Set<T> set = new HashSet<T>();
		for(BlockingQueue<T> q: queues) {
			set.addAll(q);
		}		
		return (T[]) set.toArray();
	}
	
	/**
	 * @param tarr
	 * @return
	 * @see java.util.concurrent.BlockingQueue#toArray(T[])
	 */
	public T[] toArray(Object[] tarr) {
		Set<T> set = new HashSet<T>();
		for(BlockingQueue<T> q: queues) {
			set.addAll(q);
		}		
		return (T[]) set.toArray();
	}
	
	/**
	 * @return
	 * @see java.util.concurrent.BlockingQueue#toString()
	 */
	public String toString() {
		StringBuilder b = new StringBuilder("[");
		for(BlockingQueue<T> q: queues) {
			b.append(Arrays.toString(q.toArray())).append(",");
		}
		b.deleteCharAt(b.length()-1);
		b.append("]");
		return b.toString();
	}

	/**
	 * @param e
	 * @return
	 * @see java.util.concurrent.BlockingQueue#add(java.lang.Object)
	 */
	public boolean add(T e) {
		throw new UnsupportedOperationException("[AggregatingBlockingQueue] This is a read only queue.");
	}


	/**
	 * @return
	 * @see java.util.Queue#element()
	 */
	public T element() {
		T object = peek();
		if(object==null) {
			throw new NoSuchElementException();
		} else {
			return object;
		}
	}

	/**
	 * @return
	 * @see java.util.Queue#remove()
	 */
	public T remove() {
		T object = poll();
		if(object==null) {
			throw new NoSuchElementException();
		} else {
			return object;
		}
	}

	/**
	 * @param c
	 * @return
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException("[AggregatingBlockingQueue] This is a read only queue.");
	}

	/**
	 * @param c
	 * @return
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		for(Object o: c) {
			for(BlockingQueue<T> q: queues) {
				if(q.contains(o)) continue;
				else return false;
			}			
		}
		return true;
	}

	/**
	 * @return
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		for(BlockingQueue<T> q: queues) {
			if(!q.isEmpty()) return false;
		}
		return true;
	}

	/**
	 * @param c
	 * @return
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for(Object o: c) {
			for(BlockingQueue<T> q: queues) {
				if(q.remove(o)) {
					changed = true;
					continue;
				}			
			}
		}
		return changed;
	}

	/**
	 * @param c
	 * @return
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
		boolean changed = false;
		for(BlockingQueue<T> q: queues) {
			if(q.retainAll(c)) {
				changed = true;
				continue;
			}			
		}
		return changed;
	}



	/**
	 * @return the workerThreadCount
	 */
	public int getWorkerThreadCount() {
		return workerThreadCount;
	}



	/**
	 * @param workerThreadCount the workerThreadCount to set
	 */
	public void setWorkerThreadCount(int workerThreadCount) {
		this.workerThreadCount = workerThreadCount;
	}



	/**
	 * @return the zeroQueueSleepTime
	 */
	public long getZeroQueueSleepTime() {
		return zeroQueueSleepTime;
	}



	/**
	 * @param zeroQueueSleepTime the zeroQueueSleepTime to set
	 */
	public void setZeroQueueSleepTime(long zeroQueueSleepTime) {
		this.zeroQueueSleepTime = zeroQueueSleepTime;
	}



	/**
	 * @return the queuePullMaxSize
	 */
	public int getQueuePullMaxSize() {
		return queuePullMaxSize.get();
	}



	/**
	 * @param queuePullMaxSize the queuePullMaxSize to set
	 */
	public void setQueuePullMaxSize(int queuePullMaxSize) {
		this.queuePullMaxSize.set(queuePullMaxSize);
	}


	/**
	 * @return the started
	 */
	public boolean isStarted() {
		return started.get();
	}

}

