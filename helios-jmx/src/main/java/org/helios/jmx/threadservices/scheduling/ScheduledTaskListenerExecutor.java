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
package org.helios.jmx.threadservices.scheduling;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * <p>Title: ScheduledTaskListenerExecutor</p>
 * <p>Description: Dynamic Proxy that handles event firing from the QuartzDeferredTaskResult and dispatches them to a thread pool</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ScheduledTaskListenerExecutor implements InvocationHandler, Callable {
	/** The invocation target listener */
	protected ScheduledTaskListener listener = null;
	/** The thread pool to dispatch executions to */
	protected ExecutorService threadPool = null;
	/** The method to execute */
	protected Method method = null;
	/** The arguments to the method */
	protected Object[] methodArgs = null;
	
	/**
	 * Creates a new ScheduledTaskListenerExecutor
	 * @param listener The invocation target listener
	 * @param threadPool The thread pool to dispatch executions to
	 */
	private ScheduledTaskListenerExecutor(ScheduledTaskListener listener, ExecutorService threadPool) {
		super();
		this.listener = listener;
		this.threadPool = threadPool;
	}
	
	/**
	 * Returns a ScheduledTaskListener which is a proxy that dispatches invocations to a thread pool.
	 * @param listener The invocation target listener
	 * @param threadPool The thread pool to dispatch executions to
	 * @return A ScheduledTaskListener.
	 */
	public static ScheduledTaskListener getListenerExecutorProxy(ScheduledTaskListener listener, ExecutorService threadPool) {
		ScheduledTaskListenerExecutor executor = new ScheduledTaskListenerExecutor(listener, threadPool);
		ScheduledTaskListener proxy = (ScheduledTaskListener) Proxy.newProxyInstance(ScheduledTaskListener.class.getClassLoader(),
                new Class[] { ScheduledTaskListener.class },
                executor);
		return proxy;
	}

	/**
	 * Processes a method invocation on a proxy instance and returns the result.
	 * @param proxy
	 * @param method
	 * @param args
	 * @return
	 * @throws Throwable
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		this.method = method;
		this.methodArgs = args;
		threadPool.submit(this);
		return null;
	}

	/**
	 * Invokes the proxied call against the target listener.
	 * @return Null.
	 * @throws Exception Never throws an exception.
	 * @see java.util.concurrent.Callable#call()
	 */
	public Object call() throws Exception {
		try {
			method.invoke(listener, methodArgs);
		} catch (Throwable t) {}
		return null;
	}

}
