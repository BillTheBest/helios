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
package org.helios.jmx.threadservices.submission;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

/**
 * <p>Title: InvocationTask</p>
 * <p>Description: A callable implementation that is constructed with an encapsulated method invocation.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class InvocationTask<T> implements InvocationArgumentProvider, NamedTask, Callable<T> {
	protected Object targetObject = null;
	protected Method targetMethod = null;
	protected Object[] parameters = null;
	protected InvocationArgumentProvider argProvider = null;
	protected String name = null;
	protected boolean isStatic = false;
	

	/**
	 * Creates a new InvocationTask with static parameters.
	 * @param targetObject The object to invoke on.
	 * @param targetMethod The target method to invoke.
	 * @param parameters The parameters to pass to the invocation.
	 */
	public InvocationTask(Object targetObject, Method targetMethod,
			Object[] parameters) {
		this.targetObject = targetObject;
		this.targetMethod = targetMethod;
		this.parameters = parameters;
		argProvider = this;
		name = targetObject.getClass().getName() + "@" + targetObject.hashCode();
		isStatic = Modifier.isStatic(targetMethod.getModifiers());
		targetMethod.setAccessible(true);
	}
	
	/**
	 * Creates a new named InvocationTask with static parameters.
	 * @param targetObject The object to invoke on.
	 * @param targetMethod The target method to invoke.
	 * @param parameters The parameters to pass to the invocation.
	 * @param name The name of the task.
	 */
	public InvocationTask(Object targetObject, Method targetMethod,
			Object[] parameters, String name) {
		this.targetObject = targetObject;
		this.targetMethod = targetMethod;
		this.parameters = parameters;
		argProvider = this;
		this.name = name;
		isStatic = Modifier.isStatic(targetMethod.getModifiers());
		targetMethod.setAccessible(true);
		
	}
	
	/**
	 * Creates a new InvocationTask with dynamic parameters.
	 * @param targetObject The object to invoke on.
	 * @param targetMethod The target method to invoke.
	 * @param argProvider The parameter provider for the invocation.
	 */
	public InvocationTask(Object targetObject, Method targetMethod,
			InvocationArgumentProvider argProvider) {
		this.targetObject = targetObject;
		this.targetMethod = targetMethod;
		this.argProvider = argProvider;
		name = targetObject.getClass().getName() + "@" + targetObject.hashCode();
		isStatic = Modifier.isStatic(targetMethod.getModifiers());
		targetMethod.setAccessible(true);		
	}
	
	/**
	 * Creates a new named InvocationTask with dynamic parameters.
	 * @param targetObject The object to invoke on.
	 * @param targetMethod The target method to invoke.
	 * @param argProvider The parameter provider for the invocation.
	 * @param name The name of the task.
	 */
	public InvocationTask(Object targetObject, Method targetMethod,
			InvocationArgumentProvider argProvider, String name) {
		this.targetObject = targetObject;
		this.targetMethod = targetMethod;		
		this.argProvider = argProvider;
		this.name = name;
		isStatic = Modifier.isStatic(targetMethod.getModifiers());
		targetMethod.setAccessible(true);		
	}
	
	

	/**
	 * Returns the defined parameters.
	 * @return The invocation parameters.
	 * @see org.helios.jmx.threadservices.submission.InvocationArgumentProvider#getParameters()
	 */
	public Object[] getParameters() {
		return parameters;
	}

	/**
	 * Returns the task name.
	 * @return The task name.
	 * @see org.helios.jmx.threadservices.submission.NamedTask#getTaskName()
	 */
	public String getTaskName() {
		return name;
	}

	/**
	 * Invokes the configured invocation.
	 * @return
	 * @throws Exception
	 * @see java.util.concurrent.Callable#call()
	 */
	public T call() throws Exception {
		return (T)targetMethod.invoke((isStatic ? null : targetObject), argProvider.getParameters());
	}

}
