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
package org.helios.jmx.dynamic.container;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.management.MBeanOperationInfo;

/**
 * <p>Title: OperationContainer</p>
 * <p>Description: A managed container for a dynamic MBean's operations.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public class OperationContainer extends MBeanContainer implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4915660094118175218L;
	/**	The JMX information on the operation. */
	protected MBeanOperationInfo operInfo = null;
	/**	The actual method that backs the JMX operation. */
	protected Method targetMethod = null;
	/**	Indicates if the method will be handled by the asynch request thread pool. */
	protected boolean asynch = false;
	
	/**
	 * Creates a new OperationContainer for the passed managed object and specified method.
	 * @param targetObject The managed object where the attribute resides.
	 * @param operInfo The JMX information on the operation.
	 * @param targetMethod The actual method that backs the JMX operation.
	 * @param async Indicates if the method will be handled by the asynch request thread pool.
	 */
	public OperationContainer(Object targetObject, MBeanOperationInfo operInfo, Method targetMethod, boolean async) {
		super();
		this.targetObject = targetObject;
		this.operInfo = operInfo;
		this.targetMethod = targetMethod;
		this.asynch = async;
	}
	
	/**
	 * Creates a new OperationContainer for the passed managed object and specified method.
	 * Defaults to non-async.
	 * @param targetObject
	 * @param description
	 * @param targetMethod
	 */
	public OperationContainer(Object targetObject, String description, Method targetMethod) {		
		this(targetObject, new MBeanOperationInfo(description, targetMethod), targetMethod, false);
	}
	
	/**
	 * Invokes the managed operation.
	 * @param args An array of arguments to the operation.
	 * @return The return value of the operation call.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Object invokeOperation(Object...args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return targetMethod.invoke(targetObject, args);
	}

	/**
	 * @return the asynch
	 */
	public boolean isAsynch() {
		return asynch;
	}

	/**
	 * @param asynch the asynch to set
	 */
	public void setAsynch(boolean asynch) {
		this.asynch = asynch;
	}

	/**
	 * @return the operInfo
	 */
	public MBeanOperationInfo getOperInfo() {
		return operInfo;
	}

	/**
	 * @param operInfo the operInfo to set
	 */
	public void setOperInfo(MBeanOperationInfo operInfo) {
		this.operInfo = operInfo;
	}

	/**
	 * @return the targetMethod
	 */
	public Method getTargetMethod() {
		return targetMethod;
	}

	/**
	 * @param targetMethod the targetMethod to set
	 */
	public void setTargetMethod(Method targetMethod) {
		this.targetMethod = targetMethod;
	}

	/**
	 * @return the targetObject
	 */
	public Object getTargetObject() {
		return targetObject;
	}

	/**
	 * @param targetObject the targetObject to set
	 */
	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}
	
	
	/**
	 * Generates a unique hash code for an operation.
	 * @param actionName
	 * @param signature
	 * @return
	 */
	public static String hashOperationName(String actionName, String[] signature) {
		if(signature==null || signature.length<1) return "" + actionName.hashCode();
		StringBuilder buff = new StringBuilder(actionName);
		for(String s: signature) {
			buff.append(s);
		}
		return "" + buff.toString().hashCode();
	}
	
	/**
	 * Generates a unique hash code for an operation.
	 * @param actionName
	 * @param signature
	 * @return
	 */
	public static String hashOperationName(String actionName, Class[] signature) {
		if(signature==null || signature.length<1) return "" + actionName.hashCode();
		String[] classes = new String[signature.length];
		for(int i = 0; i < signature.length; i++) {			
			classes[i] = signature[i].getName();			
		}
		return hashOperationName(actionName, classes);		
	}
	


}
