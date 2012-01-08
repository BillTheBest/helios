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


import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.helios.jmx.threadservices.submission.InvocationArgumentProvider;

/**
 * <p>Title: MBeanOperationExecutionTask</p>
 * <p>Description: ExecutionTask for a JMX operation against a defined MBean through a defined MBeanServerConnection.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class MBeanOperationExecutionTask<T> implements ExecutionTask<T> {
	protected Object[] arguments = null;
	protected InvocationArgumentProvider argProvider = null;
	protected MBeanServerConnection mbeanServer = null;
	protected ObjectName objectName = null;
	protected String operationName = null;
	protected String[] operationSignature = null;
	
	
	
	

	/**
	 * Creates a new raw argument MBeanOperationExecutionTask.
	 * @param mbeanServer
	 * @param objectName
	 * @param operationName
	 * @param operationSignature
	 * @param arguments
	 */
	public MBeanOperationExecutionTask(MBeanServerConnection mbeanServer, ObjectName objectName,
			String operationName, String[] operationSignature, Object[] arguments) {
		this.arguments = arguments;
		this.mbeanServer = mbeanServer;
		this.objectName = objectName;
		this.operationName = operationName;
		this.operationSignature = operationSignature;
		this.argProvider = null;
	}
	
	/**
	 * Creates a new InvocationArgumentProvider argument provided MBeanOperationExecutionTask.
	 * @param mbeanServer
	 * @param objectName
	 * @param operationName
	 * @param operationSignature
	 * @param argProvider
	 */
	public MBeanOperationExecutionTask(MBeanServerConnection mbeanServer, ObjectName objectName,
			String operationName, String[] operationSignature, InvocationArgumentProvider argProvider) {
		this.mbeanServer = mbeanServer;
		this.objectName = objectName;
		this.operationName = operationName;
		this.operationSignature = operationSignature;
		this.argProvider = argProvider;
	}	

	/**
	 * @return
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
		T invoke = (T) mbeanServer.invoke(objectName, operationName, args, operationSignature);
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
