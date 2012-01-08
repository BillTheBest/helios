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
package org.helios.server.ot.web;

import javax.servlet.ServletContext;

import org.helios.helpers.JMXHelper;
import org.helios.spring.web.HeliosContextLoader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * <p>Title: WebContextLoader</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.web.WebContextLoader</code></p>
 */

public class WebContextLoader extends HeliosContextLoader {

	/**
	 * Creates a new WebContextLoader
	 */
	public WebContextLoader() {
	
	}
	
	/**
	 * to load or obtain an ApplicationContext instance which will be used as the parent context of the root WebApplicationContext.
	 * @param servletContext current servlet context 
	 * @return the parent application context
	 * @throws BeansException
	 * @see org.springframework.web.context.ContextLoader#loadParentContext(javax.servlet.ServletContext)
	 */
	protected ApplicationContext loadParentContext(ServletContext servletContext)   throws BeansException {
		ApplicationContext parent = (ApplicationContext)JMXHelper.getAttribute(JMXHelper.objectName("org.helios.spring:service=HeliosApplicationContext"), JMXHelper.getHeliosMBeanServer(), "AppContext");
		LOG.info("Returning Helios GenericWebApplicationContext as parent of WebAppContext");
		GenericWebApplicationContext wac = new GenericWebApplicationContext(servletContext);
		wac.setParent(parent);
		wac.refresh();
		servletContext.setAttribute("org.springframework.web.context.WebApplicationContext.ROOT", wac);
		return wac;
	}	

}
