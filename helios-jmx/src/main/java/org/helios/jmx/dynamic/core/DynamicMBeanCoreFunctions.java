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



import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;


/**
 * <p>Title: DynamicMBeanCoreFunctions</p>
 * <p>Description: A set of core functions for the ManagedObjectDynamicMBean.
 * Implemented as a stand alone object to keep the simple invocation structure.</p>  
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
@JMXManagedObject(declared=true, annotated=true)
public class DynamicMBeanCoreFunctions extends BaseCoreExtensionManagedObject {

	/**
	 * Adds a new managed object to the managed object mbean.
	 * @param managedObject An object to be managed.
	 */
	@JMXOperation(description="Adds a new object to be managed.", name="addManagedObject")
	public void addManagedObject(
			@JMXParameter(name="NewManagedObject", description="A new object to be managed by the MBean.") Object managedObject) {
		modb.reflectObject(managedObject);
		modb.updateMBeanInfo();
	}

}
