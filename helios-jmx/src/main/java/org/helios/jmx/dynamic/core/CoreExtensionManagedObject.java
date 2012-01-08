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

/**
 * <p>Title: CoreExtensionManagedObject</p>
 * <p>Description: Defines callbacks to a core managed object dynamic mbean extension class.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public interface CoreExtensionManagedObject {
	/**
	 * Callback to set the ManagedObjectDynamicMBean reference.
	 * @param modb A reference to the container ManagedObjectDynamicMBean
	 */
	public void setManagedObjectDynamicMBean(ManagedObjectDynamicMBean modb);
	
	/**
	 * Callback to set the MBeanServer that the containing ManagedObjectDynamicMBean is registered in.
	 * @param meanServerConnection A reference to the MBeanServer.
	 */
	public void setMBeanServerConnection(MBeanServerConnection meanServerConnection);
	
	/**
	 * Callback to set the object name of the Helios bootstrap service.
	 * @param bootStrap The object name of the bootstrap service.
	 */
	public void setBootStrapService(ObjectName bootStrap);
	
	/**
	 * Callback to start subservice.
	 * @throws Exception
	 */
	public void start() throws Exception;
	
	/**
	 * Callback to stop subservice.
	 */
	public void stop();
}
