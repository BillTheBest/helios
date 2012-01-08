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
package org.helios.jmx.dynamic.core;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;

/**
 * <p>Title: BaseCoreExtensionManagedObject</p>
 * <p>Description: A concrete implementation of <code>CoreExtensionManagedObject</code> which implementations can extend.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
@JMXManagedObject(declared=true, annotated=true)
public class BaseCoreExtensionManagedObject implements CoreExtensionManagedObject {
	
	/**The instance of the ManagedObjectDynamicMBean that this object will issue core functions for.*/
	protected ManagedObjectDynamicMBean modb = null;
	/** A reference to the MBeanServer where the MODB is registered */
	protected MBeanServerConnection meanServerConnection = null;
	/** The object name of the Helios Bootstrap Service */
	protected ObjectName bootStrapService = null;
	
	/**
	 * Callback to set the ManagedObjectDynamicMBean reference.
	 * @param modb A reference to the container ManagedObjectDynamicMBean
	 */
	public void setManagedObjectDynamicMBean(ManagedObjectDynamicMBean modb) {
		this.modb = modb;
	}
	
	/**
	 * Callback to set the MBeanServer that the containing ManagedObjectDynamicMBean is registered in.
	 * @param meanServerConnection A reference to the MBeanServer.
	 */
	public void setMBeanServerConnection(MBeanServerConnection meanServerConnection) {
		this.meanServerConnection = meanServerConnection;
	}
	
	/**
	 * Callback to set the object name of the Helios bootstrap service.
	 * @param bootStrap The object name of the bootstrap service.
	 */
	public void setBootStrapService(ObjectName bootStrap) {
		this.bootStrapService = bootStrapService;
	}
	
	/**
	 * Callback from MODB to start sub-service.
	 * @see org.helios.jmx.dynamic.core.CoreExtensionManagedObject#start()
	 */
	public void start() throws Exception {
		
	}
	
	/**
	 * Callback from MODB to stop sub-service.
	 * @see org.helios.jmx.dynamic.core.CoreExtensionManagedObject#stop()
	 */
	public void stop() {
		
	}
	

}
