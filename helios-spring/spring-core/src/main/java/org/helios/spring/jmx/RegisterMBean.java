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
package org.helios.spring.jmx;

import javax.management.ObjectName;

import org.helios.helpers.BeanHelper;
import org.helios.helpers.JMXHelperExtended;

/**
 * <p>Title: RegisterMBean</p>
 * <p>Description: Simple utilitiy bean to register another Spring bean as an MBean in the Helios MBean Server</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class RegisterMBean {
	protected Object targetObject = null;
	protected ObjectName objectName = null;
	protected String attributeName = null;
	
	public void bind() throws Exception {
		ObjectName on = null;
		Object bindObject = null;
		if(attributeName!=null) {
			bindObject = BeanHelper.getAttribute(attributeName, targetObject);
			on = new ObjectName(objectName.toString() + ",name=" + attributeName);
		} else {
			bindObject = targetObject;
			on = objectName;
		}
		JMXHelperExtended.getHeliosMBeanServer().registerMBean(bindObject, on);
	}
	/**
	 * @param targetObject the targetObject to set
	 */
	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}
	/**
	 * @param objectName the objectName to set
	 */
	public void setObjectName(ObjectName objectName) {
		this.objectName = objectName;
	}
	/**
	 * @param attributeName the attributeName to set
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
}

