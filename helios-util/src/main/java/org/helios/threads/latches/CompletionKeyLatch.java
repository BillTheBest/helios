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
package org.helios.threads.latches;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Title: CompletionKeyLatch</p>
 * <p>Description: A simplified interface wrapper around a single count countdown latch. The thread that counts down the latch
 * provides a "completion key" which represents the identity or conclusion of how the latch was dropped. </p>
 * @param <T> The type of the completion key 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.threads.latches.CompletionKeyLatch</code></p>
 */

public class CompletionKeyLatch<T>  {
	/** The inner latch impl. */
	protected final CountDownLatch latch = new CountDownLatch(1);
	/** The completion key set when the latch is released */
	protected T completionKey = null;
	/** Indicates if this latch will accept a null completion key */
	protected final boolean allowNullCompletionKey;
	/** Indicates that the latch releaser supplied a null key */
	protected final AtomicBoolean nullCompletionKey = new AtomicBoolean(false);
	/** Indicates that the latch releaser supplied any key */
	protected final AtomicBoolean completionKeySet = new AtomicBoolean(false);
	/** Indicates, for each thread, if the await operation completed or timed out/interrupted. */
	protected final ThreadLocal<Boolean> threadCompleted = new ThreadLocal<Boolean>(){
		@Override
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}
	};

	
	/**
	 * Creates a new CompletionKeyLatch which accepts null completion keys
	 */
	public CompletionKeyLatch() {
		this(true);
	}
	
	
	
	/**
	 * Creates a new CompletionKeyLatch
	 * @param allowNullCompletionKey true if this latch will accept a null completion key
	 */
	public CompletionKeyLatch(boolean allowNullCompletionKey) {
		this.allowNullCompletionKey = allowNullCompletionKey;
	}
	
	/**
	 * Releases the latch, supplying the completion key
	 * @param completionKey The compoletion key for the latch release
	 * @see java.util.concurrent.CountDownLatch#countDown()
	 */
	public void release(T completionKey) {
		if(completionKey==null) {			
			nullCompletionKey.set(true);
			if(!allowNullCompletionKey) {
				throw new IllegalArgumentException("This CompletionKeyLatch does not accept null completion keys", new Throwable());
			}
		}
		this.completionKey = completionKey;
		latch.countDown();
	}

    /**
     * Causes the current thread to wait for the completion key to be set, unless the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>If the completion key has already been set, this method returns immediately.
     *
     * <p>If the completion key has not been set yet,  then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happen:
     * <ul>
     * <li>Another thread calls {@link #release} method with a legal completion key; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread in which case a null is returned.
     * </ul>
     *
     * <p>If the current thread has its interrupted status set on entry to this method
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * <p>When a null is returned to a calling thread, it is possible to determine if this occured because a null completion key was set
     * or because the operation did not complete by interrogating the latch with these methods:<ul>
     * 		<li><b>{@link #completed}</b></li>
     * 		<li><b>{@link #isCompletionKeySet}</b>:this method is succeptible to a race condition since the key may be set shortly after</li>
     * 		<li><b>{@link #isNullCompletionKey}</b>: returns true if the completion was set to a null value</li>
     * </ul>

     * 
     * @return the completion key
     */
	public T await() {
		boolean completed = false;
		try {
			latch.await();
			completed = true;
		} catch (InterruptedException e) {
			completed = false;
		}		
		threadCompleted.set(completionKey!=null || completed);
		return completionKey;
	}

    /**
     * Causes the current thread to wait for the completion key to be set, 
     * unless the thread is {@linkplain Thread#interrupt interrupted} 
     * or the specified waiting time elapses.
     *
     * <p>If the completion key has already been set, this method returns immediately.
     *
     * <p>If the completion key has not been set yet,  then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happen:
     * <ul>
     * <li>Another thread calls {@link #release} method with a legal completion key; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread in which case a null is returned.
     * <li>The specified waiting time elapses in which case a null is returned.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method 
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * <p>If the specified waiting time elapses then a null 
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     * 
     * <p>When a null is returned to a calling thread, it is possible to determine if this occured because a null completion key was set
     * or because the operation did not complete by interrogating the latch with these methods:<ul>
     * 		<li><b>{@link #completed}</b></li>
     * 		<li><b>{@link #isCompletionKeySet}</b>:this method is succeptible to a race condition since the key may be set shortly after</li>
     * 		<li><b>{@link #isNullCompletionKey}</b>: returns true if the completion was set to a null value</li>
     * </ul>
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return the completion key if key was set or null
     *         if the waiting time elapsed before the key was set.
     */
	public T await(long timeout, TimeUnit unit) {
		boolean completed = false;
		try {
			completed = latch.await(timeout, unit);
		} catch (InterruptedException e) {
			completed = false;
		}		
		threadCompleted.set(completionKey!=null || completed);
		return completionKey;
	}
	
	
	/**
	 * Indicates if the await method completed successfully or if timed out / was interrupted.
	 * Should only be called by the same thread that executed the await operation.
	 * @return true if the await completed, false if it timed out / was interrupted.
	 */
	public boolean completed() {
		return threadCompleted.get();
	}



	/**
     * Returns a string identifying the underlying latch, as well as its state.
     * The state, in brackets, includes the String {@code "Count ="}
     * followed by either <b>1</b> if the completion key has not been set or <b>0</b>
     * if the completion key was set. 
     *
     * @return a string identifying this latch, as well as its state
     */
	@Override
	public String toString() {
		return latch.toString();
	}



	/**
	 * Indicates if the completion key supplied a null completion key
	 * @return <ul>
	 * <li><b>true</b> if the completion key supplied a null completion key</li>
	 * <li><b>false</b> if the completion key supplied a non null completion key</li>
	 * <li><b>false</b> if the completion key has not been set</li>
	 * </ul>
	 */
	public boolean isNullCompletionKey() {
		return nullCompletionKey.get();
	}
	
	/**
	 * Indicates if the completion key has been set
	 * @return <ul>
	 * <li><b>true</b> if the completion key was set</li>
	 * <li><b>false</b> if the completion key has not been set</li>
	 * </ul>
	 */
	public boolean isCompletionKeySet() {
		return completionKeySet.get();
	}
	
	
	

}
