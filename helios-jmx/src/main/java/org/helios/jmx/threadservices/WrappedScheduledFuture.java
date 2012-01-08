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
package org.helios.jmx.threadservices;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Title: WrappedScheduledFuture</p>
 * <p>Description: Wrapper for scheduled future where a callable is wrapped in a runnable interface.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 * @param <T>
 */
public class WrappedScheduledFuture<T> implements ScheduledFuture<T> {
	protected ScheduledFuture<T> innerScheduledFuture = null;
	protected CallableRunnable<T> callableRunnable = null;

	/**
	 * @param innerScheduledFuture
	 * @param callableRunnable
	 */
	public WrappedScheduledFuture(ScheduledFuture<T> innerScheduledFuture,
			CallableRunnable<T> callableRunnable) {
		super();
		this.innerScheduledFuture = innerScheduledFuture;
		this.callableRunnable = callableRunnable;
	}

	/**
	 * @param mayInterruptIfRunning
	 * @return
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	public boolean cancel(boolean mayInterruptIfRunning) {
		return innerScheduledFuture.cancel(mayInterruptIfRunning);
	}

	/**
	 * @param o
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Delayed o) {
		return innerScheduledFuture.compareTo(o);
	}

	/**
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @see java.util.concurrent.Future#get()
	 */
	public T get() throws InterruptedException, ExecutionException {
		callableRunnable.waitForCompletion(); 
		if(callableRunnable.isCompletedOk()) {
			return callableRunnable.getResult();
		} else {
			throw new ExecutionException("CallableRunnable found completed with exception", callableRunnable.getException());
		}
	}

	/**
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	public T get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		callableRunnable.waitForCompletion(timeout, unit); 
		if(callableRunnable.isCompletedOk()) {
			return callableRunnable.getResult();
		} else {
			if(callableRunnable.isComplete()) {
				throw new TimeoutException("Thread time out waiting CallableRunnable completion");
			} else {
				throw new ExecutionException("CallableRunnable found completed with exception", callableRunnable.getException());
			}
		}
	}

	/**
	 * @param unit
	 * @return
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	public long getDelay(TimeUnit unit) {
		return innerScheduledFuture.getDelay(unit);
	}

	/**
	 * @return
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	public boolean isCancelled() {
		return innerScheduledFuture.isCancelled();
	}

	/**
	 * @return
	 * @see java.util.concurrent.Future#isDone()
	 */
	public boolean isDone() {
		return innerScheduledFuture.isDone();
	}

}
