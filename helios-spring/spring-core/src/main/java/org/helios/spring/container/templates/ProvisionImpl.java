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

import org.helios.spring.container.templates.merging.ArgumentMerger;

/**
 * <p>Title: ProvisionImpl</p>
 * <p>Description: A basic value object implementation of the TemplateProvider Provision interface.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ProvisionImpl implements Provision {
	protected String beanName = null;
	protected String contextName = null;
	protected Map<Integer, Object> constructorArguments = null;
	protected Map<String, Object> properties = null;
	protected Map<Class, ArgumentMerger> argumentMergers = new HashMap<Class, ArgumentMerger>();
	
	/**
	 * 
	 */
	public ProvisionImpl() {
		
	}
	
	
	/**
	 * @param beanName
	 * @param constructorArguments
	 * @param properties
	 */
	public ProvisionImpl(String beanName,
			Map<Integer, Object> constructorArguments,
			Map<String, Object> properties) {
		super();
		this.beanName = beanName;
		this.constructorArguments = constructorArguments;
		this.properties = properties;
	}
	/**
	 * @return the beanName
	 */
	public String getBeanName() {
		return beanName;
	}
	/**
	 * @param beanName the beanName to set
	 */
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
	/**
	 * @return the constructorArguments
	 */
	public Map<Integer, Object> getConstructorArguments() {
		return constructorArguments;
	}
	/**
	 * @param constructorArguments the constructorArguments to set
	 */
	public void setConstructorArguments(Map<Integer, Object> constructorArguments) {
		this.constructorArguments = constructorArguments;
	}
	/**
	 * @return the properties
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}
	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}


	/**
	 * @return the contextName
	 */
	public String getContextName() {
		return contextName;
	}


	/**
	 * @param contextName the contextName to set
	 */
	public void setContextName(String contextName) {
		this.contextName = contextName;
	}


	/**
	 * @return the argumentMergers
	 */
	public Map<Class, ArgumentMerger> getArgumentMergers() {
		return argumentMergers;
	}


	/**
	 * @param argumentMergers the argumentMergers to set
	 */
	public void setArgumentMergers(Map<Class, ArgumentMerger> argumentMergers) {
		this.argumentMergers = argumentMergers;
	}	


}
