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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.helios.jmx.threadservices.submission.InvocationArgumentProvider;

/**
 * <p>Title: ObjectExecutionTask</p>
 * <p>Description: An execution task based on a provided simple object, method and invocation arguments.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ObjectExecutionTask<T> implements ExecutionTask<T> {
	protected Object targetObject = null;
	protected Method method = null;
	protected Object[] arguments = null;
	protected InvocationArgumentProvider argProvider = null;
	protected IScheduledThreadPoolService threadPool = null;
	
	public static final String TARGET_OBJECT = "task.target.object";
	public static final String METHOD = "task.target.method";
	public static final String ARGUMENTS = "task.target.arguments";
	public static final String ARG_PROVIDER = "task.target.argprovider";
	public static final String THREAD_POOL = "task.target.thread.pool";
	public static final String TIMEOUT = "task.target.timeout";
	public static final long DEFAULT_TIMEOUT = 2000;
//	/**
//	 * Creates a new ObjectExecutionTask based on a Quartz JobDataMap.
//	 * @param jobDataMap a Quartz JobDataMap.
//	 */
//	public ObjectExecutionTask(JobDataMap jobDataMap) {
//		targetObject = jobDataMap.get(TARGET_OBJECT);
//		method = (Method) jobDataMap.get(METHOD);
//		arguments = (Object[]) jobDataMap.get(ARGUMENTS);
//		argProvider = (InvocationArgumentProvider) jobDataMap.get(ARG_PROVIDER);
//		threadPool = (IScheduledThreadPoolService) jobDataMap.get(THREAD_POOL);
//		Long timeOut = (Long) jobDataMap.get(TIMEOUT);
//		if(timeOut==null) {
//			timeOut = DEFAULT_TIMEOUT;
//		}
//		Future<T> future = threadPool.submit(this);
//		try {
//			future.get(timeOut, TimeUnit.MILLISECONDS);
//		} catch (InterruptedException e) {
//			
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			
//			e.printStackTrace();
//		} catch (TimeoutException e) {
//			
//			e.printStackTrace();
//		}
//	}
	
	/**
	 * Creates and validates a new ObjectExecutionTask.
	 * @param targetObject The target object to invoke against, or null if the method is static.
	 * @param method The method to invoke.
	 * @param arguments The arguments to pass to the invocation.
	 */
	public ObjectExecutionTask(Object targetObject, Method method, Object[] arguments) {
		this.targetObject = targetObject; 
		this.method = method;
		this.arguments = arguments;
		this.argProvider = null;
		this.method.setAccessible(true);		
		try {
			if(Modifier.isStatic(method.getModifiers())) {
				targetObject=null;
			} else {
				if(targetObject==null) {
					throw new IllegalArgumentException("Non-Static Method [" + method.toGenericString() + "] Requires Non Null Target Object");
				}
			}
			if(arguments==null) arguments = new Object[]{};
			if(method.getParameterTypes().length != arguments.length) {
				throw new IllegalArgumentException("Argument number mismatch between [" + method.toGenericString() + "] and provided arguments.");
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to validate ObjectExecutionTask", e);
		}
	}
	
	/**
	 * Creates and validates a new ObjectExecutionTask with an InvocationArgumentProvider.
	 * @param targetObject The target object to invoke against, or null if the method is static.
	 * @param method The method to invoke.
	 * @param argProvider An InvocationArgumentProvider to provide the arguments to pass to the invocation.
	 */
	public ObjectExecutionTask(Object targetObject, Method method, InvocationArgumentProvider argProvider) {
		this.targetObject = targetObject; 
		this.method = method;
		this.method.setAccessible(true);
		this.argProvider = argProvider;
		try {
			if(Modifier.isStatic(method.getModifiers())) {
				targetObject=null;
			} else {
				if(targetObject==null) {
					throw new IllegalArgumentException("Non-Static Method [" + method.toGenericString() + "] Requires Non Null Target Object");
				}
			}
			if(argProvider==null && method.getParameterTypes().length < 1) {
				throw new IllegalArgumentException("No argument provider for [" + method.toGenericString() + "] which requires arguments.");
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to validate ObjectExecutionTask", e);
		}
	}
	
	

	/**
	 * Executes the defined method on the defined object using the defined arguments.
	 * @return The return value of the defined method invocation.
	 * @throws Throwable
	 * @see org.helios.jmx.threadservices.scheduling.ExecutionTask#executeTask()
	 */
	public T executeTask() throws Throwable {
		Object[] args = null;
		if(argProvider != null) {
			args = argProvider.getParameters();
		} else {
			args = arguments;
		}
		T invoke = (T) method.invoke(targetObject, args);
		return invoke;
	}

	/**
	 * Callable wrapper method for executeTask.
	 * @return The return from the executeTask method.
	 * @throws Exception
	 * @see java.util.concurrent.Callable#call()
	 */
	public T call() throws Exception {
		try {
			return executeTask();
		} catch (Throwable t) {
			throw new Exception("Failed to invoke executeTask", t);
		}
	}



}
