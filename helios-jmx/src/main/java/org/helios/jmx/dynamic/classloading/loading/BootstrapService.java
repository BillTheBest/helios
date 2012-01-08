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
package org.helios.jmx.dynamic.classloading.loading;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;

/**
 * <p>Title: BootstrapService</p>
 * <p>Description: A BootStrap MBean that identifies its own Classloader ObjectName and exposes it as an attribute.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public class BootstrapService implements BootstrapServiceMBean, MBeanRegistration {
	
	protected MBeanServer server = null; 
	protected ObjectName objectName = null;

	
	public void postDeregister() {}

	public void postRegister(Boolean registrationDone) {}

	public void preDeregister() throws Exception {}

	public ObjectName preRegister(MBeanServer server, ObjectName name)
			throws Exception {
		this.server = server;
		objectName = name;
		return name;
	}
	
	
	public void unregister() throws InstanceNotFoundException, MBeanRegistrationException {
		server.unregisterMBean(objectName);
	}

	public void registerMBean(Object mbean, ObjectName objectName) throws InstanceAlreadyExistsException, NotCompliantMBeanException, InstanceNotFoundException, MBeanException, ReflectionException, IOException {
		server.registerMBean(mbean, objectName);		
	}
	
	public void registerModb(String description, ObjectName objectName, Object...mbean) throws InstanceAlreadyExistsException, NotCompliantMBeanException, InstanceNotFoundException, MBeanException, ReflectionException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			ManagedObjectDynamicMBean modb = new ManagedObjectDynamicMBean(description, mbean);
			modb.reflectObject(mbean);
			server.registerMBean(modb, objectName);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
				
	}


}
