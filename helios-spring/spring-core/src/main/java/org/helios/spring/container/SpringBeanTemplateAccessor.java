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

import org.apache.log4j.Logger;
import org.helios.helpers.BeanHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Title: SpringBeanTemplateAccessor</p>
 * <p>Description: An accessor class to allow a freemarker template to call out to a named spring bean. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SpringBeanTemplateAccessor implements ITemplateAccessor {
	protected ApplicationContext appContext = null;
	protected Logger log = Logger.getLogger(getClass());
	/**
	 * @param appContext
	 */
	public SpringBeanTemplateAccessor(ApplicationContext appContext) {
		this.appContext = appContext;
	}
	
	/**
	 * @param beanName
	 * @param propertyName
	 * @return
	 */
	public Object get(String beanName, String propertyName) {
		Object bean = appContext.getBean(beanName);
		if(bean==null) {
			log.error("The bean name [" + beanName + "] was not found in the application context");
			return null;
		}
		Object value =  null;
		try {
			BeanHelper.getAttribute(propertyName, bean);
		} catch (Exception e) {
			log.error("The property [" + propertyName + "] was not found in the bean [" + beanName + "(" + bean.getClass().getName() + ")]");
		}
		return value;
	}
}
