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
package org.helios.spring.container.templates;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: Template</p>
 * <p>Description: Template class definition</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class Template {
	protected String className = null;
	protected Map<Integer, ?> constructorArguments = new HashMap<Integer, Object>();
	protected Map<String, ?> properties = new HashMap<String, Object>();
	
	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}
	/**
	 * @param className the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}
	/**
	 * @return the constructorArguments
	 */
	public Map<Integer, ?> getConstructorArguments() {
		return constructorArguments;
	}
	/**
	 * @param constructorArguments the constructorArguments to set
	 */
	public void setConstructorArguments(Map<Integer, ?> constructorArguments) {
		this.constructorArguments = constructorArguments;
	}
	/**
	 * @return the properties
	 */
	public Map<String, ?> getProperties() {
		return properties;
	}
	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Map<String, ?> properties) {
		this.properties = properties;
	}
	
	
}
