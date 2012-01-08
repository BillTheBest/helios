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

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.helios.jmx.threadservices.submission.InvocationArgumentProvider;

/**
 * <p>Title: IThreadPoolService</p>
 * <p>Description: Defines an extended thread pool executor.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface IThreadPoolService {
	
	/**
	 * Submits a Runnable task for execution and returns a Future representing that task. 
	 * @param runnable the task to submit
	 * @return a Future representing pending completion of the task, and whose get() method will return null  upon completion. 
	 */
	public Future<?> submit(Runnable runnable);
	
	/**
	 * Submits a Runnable task for execution and returns a Future representing that task that will upon completion return the given result 
	 * @param runnable the task to submit
	 * @param result the result to return 
	 * @return a Future representing pending completion of the task, and whose get() method will return the given result upon completion.
	 */
	public <T> Future<T> submit(Runnable runnable, T result);
	
	/**
	 * Submits a value-returning task for execution and returns a Future representing the pending results of the task. 
	 * @param task the task to submit 
	 * @return a Future representing pending completion of the task
	 */
	public <T> Future<T> submit(Callable<T> task);
	
	/**
	 * Submits a defined target object, a method to invoke on the object and the arguments to pass for execution and returns a Future representing the pending results of the invocation.
	 * @param target The target object to invoke against.
	 * @param method The method to invoke on the target object.
	 * @param arguments The arguments to pass to the method invocation.
	 * @return a Future representing pending completion of the task
	 */
	public <T> Future<T> submit(Object target, Method method, Object[] arguments);
	
	/**
	 * Submits a defined target object, a method to invoke on the object and the arguments to pass for execution and returns a Future representing the pending results of the invocation.
	 * @param target The target object to invoke against.
	 * @param method The method to invoke on the target object.
	 * @param argProvider The argument provider that provides the parameters to pass to the method invocation.
	 * @return a Future representing pending completion of the task
	 */
	public <T> Future<T> submit(Object target, Method method, InvocationArgumentProvider argProvider);
	
	
	
	
	
	/**
	 * The pausable state of the thread pool.
	 * @return true if the pool is paused, false if it is not.
	 */
	public boolean getPaused();
	
	/**
	 * @param paused
	 */
	public void setPaused(boolean paused);
	
	
}
