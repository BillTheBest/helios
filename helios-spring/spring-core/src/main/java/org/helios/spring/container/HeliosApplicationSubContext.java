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

import java.net.URL;
import java.util.Map;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * <p>Title: HeliosApplicationSubContext</p>
 * <p>Description: The Spring application context implementation for hot deployable HeliosSpring subcontexts.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(annotated=true, declared=true)
public class HeliosApplicationSubContext extends GenericApplicationContext implements ApplicationListener {
	/** An XML Bean Definition Reader */
	protected final XmlBeanDefinitionReader beanDefReader = new XmlBeanDefinitionReader(this);
	/** instance logger */
	protected final Logger log;
	/** The subcontext's JMX ObjectName */
	protected final ObjectName objectName;
	/** A MODB for this context */
	protected final ManagedObjectDynamicMBean modb;
	/** The base JMX domain name of the subcontext */
	public static final String SUBCONTEXT_DOMAIN = "org.helios.spring.subcontext";
	/** The configuration URL */
	protected URL configurationUrl;
	
	
	public HeliosApplicationSubContext(String name, String id, ApplicationContext parent) {
		super(parent);
		this.setDisplayName(name);
		this.setId(id);
		objectName = JMXHelper.objectName(SUBCONTEXT_DOMAIN + ":name=" + name + ",id=" + id);
		log = Logger.getLogger(getClass().getName() + ".[" + objectName + "]");
		modb = new ManagedObjectDynamicMBean(name, this);
		try {			
			JMXHelperExtended.getHeliosMBeanServer().registerMBean(modb, objectName);
		} catch (Exception e) {
			log.warn("Failed to register JMX MBean interface for Spring Subcontext [" + objectName + "]", e);
		}
		
		
	}
	
	
	/**
	 * Creates a new HeliosApplicationSubContext that will deploy the passed bean with the passedbean name
	 * @param name The name of the subcontext
	 * @param id The id of the subcontext
	 * @param parent The parent application context
	 * @param beanClassName The bean class name
	 * @param attributes The bean attributes
	 * @param initMethodName The init method name
	 * @param destroyMethodName The stop method name
	 * @param beanName The deployed bean name
	 */
	public HeliosApplicationSubContext(boolean start, String name, String id, ApplicationContext parent, String beanClassName, Map<String, Object> attributes, String initMethodName, String destroyMethodName, String beanName) {
		this(name, id, parent);
		GenericBeanDefinition beanDef = new GenericBeanDefinition();
		beanDef.setBeanClassName(beanClassName);
		beanDef.setPropertyValues(new MutablePropertyValues(attributes));		
		if(initMethodName!=null) beanDef.setInitMethodName(initMethodName);
		if(destroyMethodName!=null)  beanDef.setDestroyMethodName(destroyMethodName);
		beanDef.setNonPublicAccessAllowed(true);
		this.registerBeanDefinition(beanName, beanDef);
		this.addApplicationListener(this);
		if(start) this.refresh();
	}
	
	/**
	 * Creates a new HeliosApplicationSubContext that will deploy the passed bean definition
	 * @param start
	 * @param name
	 * @param id
	 * @param parent
	 * @param beanDef
	 * @param beanName
	 */
	public HeliosApplicationSubContext(boolean start, String name, String id, ApplicationContext parent, GenericBeanDefinition beanDef, String beanName) {
		this(name, id, parent);
		this.registerBeanDefinition(beanName, beanDef);
		if(start) this.refresh();		
	}
	
	/**
	 * Creates a new HeliosApplicationSubContext that will deploy beans based on an XML definition
	 * @param configurationUrl The URL of the bean xml configuration file
	 * @param name The name of the subcontext
	 * @param id The id of the subcontext
	 * @param parent Te parent application context
	 */
	public HeliosApplicationSubContext(URL configurationUrl, String name, String id, ApplicationContext parent) {
		this(name, id, parent);
		this.configurationUrl = configurationUrl;
		Resource rez = new UrlResource(configurationUrl);
		int beansFound = beanDefReader.loadBeanDefinitions(rez);
		StringBuilder b = new StringBuilder("\nCreated Subcontext [");
		b.append(objectName).append("] with [").append(beansFound).append("]");
		for(String beanName: this.getBeanDefinitionNames()) {
			b.append("\n\t").append(beanName);
		}
		b.append("\n===================================\n");
		log.info(b.toString());
	}
	
	public void stop() {
		super.doClose();
		super.stop();
		try {
			if(JMXHelperExtended.getHeliosMBeanServer().isRegistered(objectName)) {
				JMXHelperExtended.getHeliosMBeanServer().unregisterMBean(objectName);
				log.info("Unregistered JMX MBean for subcontext [" + objectName + "]");
			}
		} catch (Exception e) {
			log.warn("Failed to unregister subcontext [" + objectName + "]");
		}		
	}

	/**
	 * @param event
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	public void onApplicationEvent(ApplicationEvent event) {
		Object source = event.getSource();
		if(event instanceof ContextClosedEvent && source.equals(this)) {
			try {
				if(JMXHelperExtended.getHeliosMBeanServer().isRegistered(objectName)) {
					JMXHelperExtended.getHeliosMBeanServer().unregisterMBean(objectName);
					log.info("Unregistered JMX MBean for subcontext [" + objectName + "]");
				}
			} catch (Exception e) {
				log.warn("Failed to unregister subcontext [" + objectName + "]");
			}
		}

	}
	
	@JMXAttribute(name="Running", description="Indicates if the component is running", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getRunning() {
		return this.isRunning();
	}

	@JMXAttribute(name="Active", description=" Determine whether this application context is active", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getActive() {
		return this.isActive();
	}

	/**
	 * The configuration URL for this subcontext
	 * @return the configurationUrl
	 */
	@JMXAttribute(name="ConfigurationUrl", description="The configuration URL for this subcontext", mutability=AttributeMutabilityOption.READ_ONLY)
	public URL getConfigurationUrl() {
		return configurationUrl;
	}
	
	/**
	 * Returns the names of the beans deployed in this subcontext
	 * @return an array of bean names
	 */
	@JMXAttribute(name="LocalBeanNames", description="The names of the beans deployed in this subcontext", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getLocalBeanNames() {
		return this.getBeanDefinitionNames();
	}
	
	/**
	 * Returns the names of the beans visible to this subcontext
	 * @return an array of bean names
	 */
	@JMXAttribute(name="VisibleBeanNames", description="The names of the beans visible to this subcontext", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getVisibleBeanNames() {
		return this.getParent().getBeanDefinitionNames();
	}
	
	
	/**
	 * Returns the name of the parent context
	 * @return the name of the parent context
	 */
	@JMXAttribute(name="ParentName", description="The name of the parent context", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getParentName() {
		return this.getParent().getDisplayName();
	}
	
	/**
	 * Returns the Idof the parent context
	 * @return the Id of the parent context
	 */
	@JMXAttribute(name="ParentId", description="The Id of the parent context", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getParentId() {
		return this.getParent().getId();
	}
	

}
