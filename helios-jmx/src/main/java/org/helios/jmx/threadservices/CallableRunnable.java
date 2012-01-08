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

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * <p>Title: CallableRunnable</p>
 * <p>Description: Runnable wrapper for a callable</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 * @param <T>
 */
public class CallableRunnable<T> implements Runnable {
	protected Callable<T> callable = null;
	protected T result = null;
	protected Throwable exception = null;
	protected AtomicBoolean complete = new AtomicBoolean(false);
	protected AtomicBoolean completedOk = new AtomicBoolean(false);
	protected Queue<Thread> waiters = new ConcurrentLinkedQueue<Thread>();

	

	/**
	 * @param callable
	 */
	public CallableRunnable(Callable<T> callable) {
		super();
		this.callable = callable;
	}
	/**
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		complete.set(false);
		try {
			result = callable.call();
			completedOk.set(true);
		} catch (Exception e) {
			exception = e;
			completedOk.set(false);
		} finally {
			complete.set(true);
			Thread waiter = null;
			while((waiter = waiters.poll()) != null) {
				LockSupport.unpark(waiter);
			}
			
		}
	}
	/**
	 * @return the callable
	 */
	public Callable<T> getCallable() {
		return callable;
	}
	/**
	 * @return the result
	 */
	public T getResult() {		
		if(!complete.get()) {
			waiters.add(Thread.currentThread());
			LockSupport.park();
		}
		if(Thread.currentThread().isInterrupted()) {
			return null;
		}	
		return result;
	}
	
	/**
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws TimeoutException
	 */
	public T getResult(long timeout, TimeUnit unit) throws TimeoutException  {
		if(!complete.get()) {
			waiters.add(Thread.currentThread());
			LockSupport.parkNanos(unit.toNanos(timeout));
		}		
		waiters.remove(Thread.currentThread());
		if(Thread.currentThread().isInterrupted()) {
			return null;
		}		
		if(!complete.get()) {
			throw new TimeoutException("Timed Out While Waiting on CallableRunnable Completion for [" + timeout + "/" + unit.name() + "]");
		}
		return result;
	}
	
	/**
	 * @return the exception
	 */
	public Throwable getException() {
		if(!complete.get()) {
			waiters.add(Thread.currentThread());
			LockSupport.park();
		}		
		waiters.remove(Thread.currentThread());
		if(Thread.currentThread().isInterrupted()) {
			return null;
		}		
		return exception;
	}
	
	/**
	 * @return the exception
	 */
	public Throwable getException(long timeout, TimeUnit unit)  throws TimeoutException  {
		if(!complete.get()) {
			waiters.add(Thread.currentThread());
			LockSupport.parkNanos(unit.toNanos(timeout));			
		}		
		waiters.remove(Thread.currentThread());
		if(Thread.currentThread().isInterrupted()) {
			return null;
		}
		if(!complete.get()) {
			throw new TimeoutException("Timed Out While Waiting on CallableRunnable Completion for [" + timeout + "/" + unit.name() + "]");
		}		
		return exception;
	}
	
	/**
	 * @return
	 */
	public boolean waitForCompletion() {
		if(!complete.get()) {
			waiters.add(Thread.currentThread());
			LockSupport.park();
		}		
		waiters.remove(Thread.currentThread());
		if(Thread.currentThread().isInterrupted()) {
			return false;
		}		
		return complete.get();
	}
	
	/**
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws TimeoutException
	 */
	public boolean waitForCompletion(long timeout, TimeUnit unit)  throws TimeoutException {
		if(!complete.get()) {
			waiters.add(Thread.currentThread());
			LockSupport.parkNanos(unit.toNanos(timeout));
		}	
		waiters.remove(Thread.currentThread());
		if(Thread.currentThread().isInterrupted()) {
			return false;
		}		
		if(!complete.get()) {
			throw new TimeoutException("Timed Out While Waiting on CallableRunnable Completion for [" + timeout + "/" + unit.name() + "]");
		}		
		return complete.get();
	}
	
	
	/**
	 * @return the complete
	 */
	public boolean isComplete() {
		return complete.get();
	}
	/**
	 * @return the completedOk
	 */
	public boolean isCompletedOk() {
		return completedOk.get();
	}

}
