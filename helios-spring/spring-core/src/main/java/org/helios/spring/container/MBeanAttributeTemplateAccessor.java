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
package org.helios.spring.container;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;

/**
 * <p>Title: MBeanAttributeTemplateAccessor</p>
 * <p>Description: An accessor class to allow a freemarker template to call out to named JMX MBean attributes </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class MBeanAttributeTemplateAccessor  implements ITemplateAccessor {
	protected MBeanServer server = null;
	
	/**
	 * @param server
	 */
	public MBeanAttributeTemplateAccessor(MBeanServer server) {
		this.server = server;
	}
	
	protected Logger log = Logger.getLogger(getClass());

	/**
	 * @param objectName
	 * @param attrName
	 * @return
	 * @see org.helios.spring.container.ITemplateAccessor#get(java.lang.String, java.lang.String)
	 */
	public Object get(String objectName, String attrName) {
		try {
			ObjectName on = JMXHelper.objectName(objectName);
			return server.getAttribute(on, attrName);
		} catch (Exception e) {
			log.error("Failed to acquire attribute value for ObjectName [" + objectName + "] attribute name [" + attrName + "]:" + e);
			return null;
		}		
	}

}
