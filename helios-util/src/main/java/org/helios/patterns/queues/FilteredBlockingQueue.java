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
package org.helios.patterns.queues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Title: FilteredBlockingQueue</p>
 * <p>Description: BlockingQueue decorator that filters enqueued items. Generics are:<ul>
 * 	<li><b>E</b>: The type of objects queued</li>
 * 	<li><b>F</b>: The type of the queue's configured filter key</li>
 * </ul></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.patterns.queues.FilteredBlockingQueue</code></p>
 */

public class FilteredBlockingQueue<E extends Filterable<F>, F> implements BlockingQueue<E> {
	/** The underlying blocking queue instance. */
	protected final BlockingQueue<E> innerQueue;
	/** The filtering queue's filter key */
	protected final AtomicReference<F> filterKey = new AtomicReference<F>(null);
	protected F foo = null;
	/**
	 * Creates a new FilteredBlockingQueue
	 * @param innerQueue The underlying blocking queue instance.
	 * @param filterKey The filtering queue's filter key. If this is null, no filtering will take place.
	 */
	public FilteredBlockingQueue(BlockingQueue<E> innerQueue, F filterKey) {
		if(innerQueue==null) throw new IllegalArgumentException("The passed blocking queue was null", new Throwable());
		this.innerQueue = innerQueue;
		this.filterKey.set(filterKey);
	}
	
	/**
	 * Sets the queue's filter key
	 * @param filterKey a filter key
	 */
	public void setFilterKey(F filterKey) {
		this.filterKey.set(filterKey);
	}
	
	/**
	 * Returns the queue's filter key
	 * @return the filterKey
	 */
	public F getFilterKey() {
		return filterKey.get();
	}
	
	
	/**
	 * Detemrines if the passed filterable should be dropped
	 * @param e The item to test
	 * @return true if the item should be dropped, false if it should be enqueued.
	 */
	protected boolean drop(E e) {
		F f = filterKey.get();
		if(f==null) return false;
		return e.drop(f);
	}
	
	/**
	 * Applies the filter to each filterable element in the passed collection 
	 * @param eColl A collection of filterable elements.
	 * @return the collection with all filtered items removed
	 */
	protected Collection<? extends E> filter(Collection<? extends E> eColl) {
		if(eColl==null || eColl.isEmpty()) return Collections.emptyList();
		F f = filterKey.get();
		if(f==null) return eColl;
		Collection<? extends E> copy = new ArrayList<E>(eColl);
		for(Iterator<? extends E> eIter = copy.iterator(); eIter.hasNext();) {
			if(eIter.next().drop(f)) {
				eIter.remove();
			}
		}
		return copy;
	}

	/**
	 * Inserts the specified element into this queue if it is not filtered and it is possible to do so immediately without violating capacity restrictions, returning true upon success and throwing an IllegalStateException if no space is currently available.
	 * @param e The item to insert
	 * @return true if the item was added, false if it was not
	 * @see java.util.concurrent.BlockingQueue#add(java.lang.Object)
	 */
	public boolean add(E e) {
		return drop(e) ? false : innerQueue.add(e);
	}

	/**
	 * Adds all of the unfiltered elements in the specified collection to this queue
	 * @param eColl A collection of filterables to enqueue
	 * @return true if one or more elements were enqueued, false otherwise
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends E> eColl) {
		if(eColl==null) return false;
		F f = filterKey.get();
		if(f==null) return innerQueue.addAll(eColl);
		boolean anyAdded = false;
		for(E e: eColl) {
			if(!e.drop(f)) {
				anyAdded = true;
				innerQueue.add(e);
			}
		}
		return anyAdded;
		
		//return innerQueue.addAll(filter(eColl));
	}

	/**
	 * Clears the queue
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		innerQueue.clear();
	}

	/**
	 * Determines if the queue contains the passed element
	 * @param e The element to check for
	 * @return true if the passed element is enqueued in this queue
	 * @see java.util.concurrent.BlockingQueue#contains(java.lang.Object)
	 */
	public boolean contains(Object e) {
		return innerQueue.contains(e);
	}

	/**
	 * Determines if the queue contains all the elements in the passed collection of elements
	 * @param eColl A collection of elements to check for
	 * @return true if all the elements are present
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> eColl) {
		return innerQueue.containsAll(eColl);
	}

	/**
	 * Removes at most the given number of available elements from this queue and adds them to the given collection. 
	 * @param eColl The collection to drain the elements to 
	 * @param max the maximum number of elements to transfer 
	 * @return the number of elements transferred 
	 * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection, int)
	 */
	public int drainTo(Collection<? super E> eColl, int max) {
		return innerQueue.drainTo(eColl, max);
	}

	/**
	 * Removes all available elements from this queue and adds them to the given collection. 
	 * @param eColl The collection to drain the elements to 
	 * @return the number of elements transferred
	 * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection)
	 */
	public int drainTo(Collection<? super E> eColl) {
		return innerQueue.drainTo(eColl);
	}

	/**
	 * Retrieves, but does not remove, the head of this queue. 
	 * @return The head of this queue
	 * @see java.util.Queue#element()
	 */
	public E element() {
		return innerQueue.element();
	}


	/**
	 * Determines if this queue is empty
	 * @return true if this queue is empty
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		return innerQueue.isEmpty();
	}

	/**
	 * Returns an iterator over the elements in this collection.
	 * @return an iterator
	 * @see java.util.Collection#iterator()
	 */
	public Iterator<E> iterator() {
		return innerQueue.iterator();
	}

	/**
	 * Inserts the specified element into this queue if it is not filtered, waiting up to the specified wait time if necessary for space to become available. 
	 * @param e The element to offer to the queue
	 * @param timeout The offer timeout
	 * @param unit The unit of the timeout
	 * @return true if successful, or false if the element was filtered or the specified waiting time elapses before space is available 
	 * @throws InterruptedException
	 * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		return drop(e) ? false : innerQueue.offer(e, timeout, unit);
	}

	/**
	 * Inserts the specified element into this queue if it is not filtered and it is possible to do so immediately without violating capacity restrictions, returning true upon success and false if no space is currently available. 
	 * @param e The element to offer
	 * @return     true if the element was added to this queue, else false 
	 * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object)
	 */
	public boolean offer(E e) {
		return drop(e) ? false : innerQueue.offer(e);
	}

	/**
	 * Retrieves and removes the head of this queue, or returns null if this queue is empty. 
	 * @return the head of this queue, or null if this queue is empty
	 * @see java.util.Queue#peek()
	 */
	public E peek() {
		return innerQueue.peek();
	}

	/**
	 * Retrieves and removes the head of this queue, or returns null if this queue is empty. 
	 * @return the head of this queue, or null if this queue is empty
	 * @see java.util.Queue#poll()
	 */
	public E poll() {
		return innerQueue.poll();
	}

	/**
	 * Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary for an element to become available.
	 * @param timeout how long to wait before giving up, in units of unit
	 * @param unit the unit of the timeout
	 * @return the head of this queue, or null if the specified waiting time elapses before an element is available 
	 * @throws InterruptedException
	 * @see java.util.concurrent.BlockingQueue#poll(long, java.util.concurrent.TimeUnit)
	 */
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		return innerQueue.poll(timeout, unit);
	}

	/**
	 * Inserts the specified element into this queue if it is not filtered, waiting if necessary for space to become available.
	 * @param e The element to add
	 * @throws InterruptedException
	 * @see java.util.concurrent.BlockingQueue#put(java.lang.Object)
	 */
	public void put(E e) throws InterruptedException {
		if(!drop(e)) innerQueue.put(e);
	}

	/**
	 * Returns the number of additional elements that this queue can ideally (in the absence of memory or resource constraints) accept without blocking, or Integer.MAX_VALUE if there is no intrinsic limit.
	 * @return the remaining capacity
	 * @see java.util.concurrent.BlockingQueue#remainingCapacity()
	 */
	public int remainingCapacity() {
		return innerQueue.remainingCapacity();
	}

	/**
	 * Retrieves and removes the head of this queue. This method differs from poll only in that it throws an exception if this queue is empty. 
	 * @return the head of the queue
	 * @see java.util.Queue#remove()
	 */
	public E remove() {
		return innerQueue.remove();
	}

	/**
	 * Removes a single instance of the specified element from this queue, if it is present.
	 * @param e element to be removed from this queue, if present 
	 * @return true if the queue changed on account of this call
	 * @see java.util.concurrent.BlockingQueue#remove(java.lang.Object)
	 */
	public boolean remove(Object e) {
		return innerQueue.remove(e);
	}

	/**
	 * Removes all of this collection's elements that are also contained in the specified collection (optional operation). After this call returns, this collection will contain no elements in common with the specified collection. 
	 * @param eColl collection containing elements to be removed from this collection 
	 * @return true if this collection changed as a result of the call 
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> eColl) {
		return innerQueue.removeAll(eColl);
	}

	/**
	 * Retains only the elements in this collection that are contained in the specified collection. 
	 * In other words, removes from this collection all of its elements that are not contained in the specified collection. 
	 * @param eColl containing elements to be retained in this collection 
	 * @return true if this collection changed as a result of the call 
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> eColl) {
		return innerQueue.retainAll(eColl);
	}

	/**
	 * Returns the number of elements in the queue
	 * @return the queue size
	 * @see java.util.Collection#size()
	 */
	public int size() {
		return innerQueue.size();
	}

	/**
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.BlockingQueue#take()
	 */
	public E take() throws InterruptedException {
		return innerQueue.take();
	}

	/**
	 * @return
	 * @see java.util.Collection#toArray()
	 */
	public Object[] toArray() {
		return innerQueue.toArray();
	}

	/**
	 * @param <T>
	 * @param arg0
	 * @return
	 * @see java.util.Collection#toArray(T[])
	 */
	public <T> T[] toArray(T[] arg0) {
		return innerQueue.toArray(arg0);
	}


	

	
	
}
