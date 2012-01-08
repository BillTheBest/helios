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

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * <p>Title: BootstrapServiceMBean</p>
 * <p>Description: MBean interface for the Helios BootstrapService.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public interface BootstrapServiceMBean {
	public void unregister() throws InstanceNotFoundException, MBeanRegistrationException;
	public void registerMBean(Object mbean, ObjectName objectName) throws InstanceAlreadyExistsException, NotCompliantMBeanException, InstanceNotFoundException, MBeanException, ReflectionException, IOException;
	public void registerModb(String description, ObjectName objectName, Object...mbean) throws InstanceAlreadyExistsException, NotCompliantMBeanException, InstanceNotFoundException, MBeanException, ReflectionException, IOException;
}
