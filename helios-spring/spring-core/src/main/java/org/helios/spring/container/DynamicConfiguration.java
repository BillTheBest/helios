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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * <p>Title: DynamicConfiguration</p>
 * <p>Description: A container and manager for a dynamic configuration deployment </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class DynamicConfiguration implements ApplicationListener, Comparable<Object> {
	protected final File configurationFile;
	protected long timestamp = 0L;
	protected final Set<DynamicConfiguration> dynamicTemplatized = new CopyOnWriteArraySet<DynamicConfiguration>();
	protected final Map<String, Object> managedBeans = new ConcurrentHashMap<String, Object>();
	protected final AtomicBoolean started = new AtomicBoolean(false);
	protected final HeliosApplicationContext parentAppContext;
	protected HeliosApplicationSubContext myAppContext;
	protected long lastStartTime = 0L;
	protected long lastStopTime = 0L;
	protected static final AtomicLong serial = new AtomicLong(0);
	protected final AtomicLong versionSerial = new AtomicLong(0);
	protected Logger log = Logger.getLogger(getClass());
	

	
	/**
	 * @param appEvent
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	public void onApplicationEvent(ApplicationEvent appEvent) {
		
		if(appEvent instanceof ApplicationContextEvent) {
			if(appEvent instanceof ContextClosedEvent) {
				
			} else if(appEvent instanceof ContextRefreshedEvent) {
				
			} else if(appEvent instanceof ContextStartedEvent) {
				
			} else if(appEvent instanceof ContextStoppedEvent) {
				if(appEvent.getSource().equals(myAppContext)) retractFromParent();
			} else {
				
			}
		}
	}	
	
	/**
	 * @param configFile
	 * @throws IOException 
	 */
	public DynamicConfiguration(File configFile, HeliosApplicationContext parentAppContext) throws IOException {
		this.parentAppContext = parentAppContext;
		this.configurationFile = configFile;
		this.timestamp = this.configurationFile.lastModified();	
		this.myAppContext = new HeliosApplicationSubContext(configFile.toURI().toURL(), configFile.getName(), "DynamicConf#" + serial.incrementAndGet() + ",version=" + versionSerial.incrementAndGet(), parentAppContext); 
		this.myAppContext.addApplicationListener(this);
		this.myAppContext.refresh();
		propagateToParent();
	}
	
	/**
	 *
	 */
	protected void propagateToParent() {
		StringBuilder b = new StringBuilder("\n\t+++++++++++++++++++++++++++++++\n\tDynamic Deployment Complete\n\t+++++++++++++++++++++++++++++++");
		b.append("\n\t\tFile Name:").append(configurationFile);
		b.append("\n\t\tDeployed Beans:");
		for(String beanName: myAppContext.getBeanDefinitionNames()) {
			BeanDefinition beanDef = myAppContext.getBeanFactory().getBeanDefinition(beanName);
			b.append("\n\t\t\t").append(beanName).append(" [").append(beanDef.getBeanClassName()).append("]");
			((DefaultListableBeanFactory)parentAppContext.getBeanFactory()).registerBeanDefinition(beanName, beanDef);			
		}
		b.append("\n\t+++++++++++++++++++++++++++++++\n");		              
		log.info(b);
	}
	
	protected void retractFromParent() {
		StringBuilder b = new StringBuilder("\n\t-------------------------------\n\tDynamic Components Undeployed\n\t-------------------------------");
		b.append("\n\t\tFile Name:").append(configurationFile);
		b.append("\n\t\tUndeployed Beans:");
		for(String beanName: myAppContext.getBeanDefinitionNames()) {
			BeanDefinition beanDef = myAppContext.getBeanFactory().getBeanDefinition(beanName);
			b.append("\n\t\t\t").append(beanName).append(" [").append(beanDef.getBeanClassName()).append("]");
			((DefaultListableBeanFactory)parentAppContext.getBeanFactory()).removeBeanDefinition(beanName);			
		}
		b.append("\n\t-------------------------------\n");
		log.info(b);
	}	
	
	/**
	 * Stops the DynamicConfiguration
	 * @throws Exception
	 */
	public void stop() throws Exception {
		if(!isStarted()) {
			throw new Exception("Cannot stop inactive DynamicConfiguration [" + this + "]");
		}
		try { myAppContext.setParent(null); } catch (Exception e) {}
		myAppContext.stop();
		this.started.set(false);
	}
	
	/**
	 * Starts the DynamicConfiguration
	 * @throws Exception
	 */
	public void start() throws Exception {
		if(isStarted()) {
			throw new Exception("Cannot start active DynamicConfiguration [" + this + "]");
		}
		//myAppContext.refresh();
		myAppContext.start();		
		updateTimestamp();
		this.started.set(true);
		for(String beanName: myAppContext.getBeanDefinitionNames()) {
			managedBeans.put(beanName, myAppContext.getBean(beanName));
		}
	}
	
	/**
	 * @return
	 */
	public Set<Map.Entry<String, Object>> getManagedBeans() {
		return managedBeans.entrySet();
	}
	
	/**
	 * @throws Exception
	 */
	public void refresh() throws Exception {
		if(isStarted()) {
			//throw new Exception("Cannot start active DynamicConfiguration [" + this + "]");
			myAppContext.stop();			
		}
		myAppContext = new HeliosApplicationSubContext(configurationFile.toURI().toURL(), configurationFile.getName(), "DynamicConf#" + serial.incrementAndGet() + ",version=" + versionSerial.incrementAndGet(), parentAppContext);
		this.myAppContext.addApplicationListener(this);
		this.myAppContext.refresh();
		propagateToParent();
		updateTimestamp();
	}
	
	/**
	 * 
	 */
	protected void updateTimestamp() {
		timestamp = configurationFile.lastModified();
	}
	
	
	
	/**
	 * Returns true if the file has been modified since the last watch.
	 * @return
	 */
	public boolean hasChanged() {
		return configurationFile.lastModified() > timestamp;
	}
	
	/**
	 * Returns true if the configuration has been started 
	 * @return
	 */
	public boolean isStarted() {
		return started.get();
	}
	
	/**
	 * Returns true if the underlying application context is running.
	 * @return
	 */
	public boolean isRunning() {
		return myAppContext.isRunning();
	}
	
	/**
	 * Returns true if the underlying application context is active. 
	 * @return
	 */
	public boolean isActive() {
		return myAppContext.isActive();
	}
	
	
	

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString()  {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("DynamicConfiguration [");    
	    retValue.append(TAB).append("configurationFile=").append(this.configurationFile);   
	    retValue.append(TAB).append("started=").append(this.started.get());    
	    retValue.append(TAB).append("active=").append(isActive());
	    retValue.append(TAB).append("running=").append(isRunning());
	    retValue.append(TAB).append("lastStartTime=").append(lastStartTime==0 ? "Never" : new Date(lastStartTime));
	    retValue.append(TAB).append("lastStopTime=").append(lastStopTime==0 ? "Never" : new Date(lastStopTime));
	    retValue.append("\n]");
	    return retValue.toString();
	}

	/**
	 * @param listener
	 * @see org.springframework.context.support.AbstractApplicationContext#addApplicationListener(org.springframework.context.ApplicationListener)
	 */
	public void addApplicationListener(ApplicationListener listener) {
		myAppContext.addApplicationListener(listener);
	}

	/**
	 * @param beanFactoryPostProcessor
	 * @see org.springframework.context.support.AbstractApplicationContext#addBeanFactoryPostProcessor(org.springframework.beans.factory.config.BeanFactoryPostProcessor)
	 */
	public void addBeanFactoryPostProcessor(
			BeanFactoryPostProcessor beanFactoryPostProcessor) {
		myAppContext.addBeanFactoryPostProcessor(beanFactoryPostProcessor);
	}

	/**
	 * @param name
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#containsBean(java.lang.String)
	 */
	public boolean containsBean(String name) {
		return myAppContext.containsBean(name);
	}

	/**
	 * @param name
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#containsBeanDefinition(java.lang.String)
	 */
	public boolean containsBeanDefinition(String name) {
		return myAppContext.containsBeanDefinition(name);
	}

	/**
	 * @param name
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#containsLocalBean(java.lang.String)
	 */
	public boolean containsLocalBean(String name) {
		return myAppContext.containsLocalBean(name);
	}

	/**
	 * @param name
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getAliases(java.lang.String)
	 */
	public String[] getAliases(String name) {
		return myAppContext.getAliases(name);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getApplicationListeners()
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return myAppContext.getApplicationListeners();
	}

	/**
	 * @param name
	 * @param requiredType
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String, java.lang.Class)
	 */
	public Object getBean(String name, Class requiredType)
			throws BeansException {
		return myAppContext.getBean(name, requiredType);
	}

	/**
	 * @param name
	 * @param args
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String, java.lang.Object[])
	 */
	public Object getBean(String name, Object[] args) throws BeansException {
		return myAppContext.getBean(name, args);
	}

	/**
	 * @param name
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String)
	 */
	public Object getBean(String name) throws BeansException {
		return myAppContext.getBean(name);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanDefinitionCount()
	 */
	public int getBeanDefinitionCount() {
		return myAppContext.getBeanDefinitionCount();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanDefinitionNames()
	 */
	public String[] getBeanDefinitionNames() {
		return myAppContext.getBeanDefinitionNames();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractRefreshableApplicationContext#getBeanFactory()
	 */
	public final ConfigurableListableBeanFactory getBeanFactory() {
		return myAppContext.getBeanFactory();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanFactoryPostProcessors()
	 */
	public List getBeanFactoryPostProcessors() {
		return myAppContext.getBeanFactoryPostProcessors();
	}

	/**
	 * @param type
	 * @param includePrototypes
	 * @param allowEagerInit
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanNamesForType(java.lang.Class, boolean, boolean)
	 */
	public String[] getBeanNamesForType(Class type, boolean includePrototypes,
			boolean allowEagerInit) {
		return myAppContext.getBeanNamesForType(type, includePrototypes,
				allowEagerInit);
	}

	/**
	 * @param type
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanNamesForType(java.lang.Class)
	 */
	public String[] getBeanNamesForType(Class type) {
		return myAppContext.getBeanNamesForType(type);
	}

	/**
	 * @param type
	 * @param includePrototypes
	 * @param allowEagerInit
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeansOfType(java.lang.Class, boolean, boolean)
	 */
	public Map getBeansOfType(Class type, boolean includePrototypes,
			boolean allowEagerInit) throws BeansException {
		return myAppContext.getBeansOfType(type, includePrototypes,
				allowEagerInit);
	}

	/**
	 * @param type
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeansOfType(java.lang.Class)
	 */
	public Map getBeansOfType(Class type) throws BeansException {
		return myAppContext.getBeansOfType(type);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getDisplayName()
	 */
	public String getDisplayName() {
		return myAppContext.getDisplayName();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getId()
	 */
	public String getId() {
		return myAppContext.getId();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getStartupDate()
	 */
	public long getStartupDate() {
		return myAppContext.getStartupDate();
	}

	/**
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.AbstractApplicationContext#getType(java.lang.String)
	 */
	public Class getType(String name) throws NoSuchBeanDefinitionException {
		return myAppContext.getType(name);
	}

	/**
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.AbstractApplicationContext#isPrototype(java.lang.String)
	 */
	public boolean isPrototype(String name)
			throws NoSuchBeanDefinitionException {
		return myAppContext.isPrototype(name);
	}

	/**
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.AbstractApplicationContext#isSingleton(java.lang.String)
	 */
	public boolean isSingleton(String name)
			throws NoSuchBeanDefinitionException {
		return myAppContext.isSingleton(name);
	}

	/**
	 * @param name
	 * @param targetType
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.AbstractApplicationContext#isTypeMatch(java.lang.String, java.lang.Class)
	 */
	public boolean isTypeMatch(String name, Class targetType)
			throws NoSuchBeanDefinitionException {
		return myAppContext.isTypeMatch(name, targetType);
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((configurationFile == null) ? 0 : configurationFile
						.hashCode());
		return result;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DynamicConfiguration other = (DynamicConfiguration) obj;
		if (configurationFile == null) {
			if (other.configurationFile != null)
				return false;
		} else if (!configurationFile.equals(other.configurationFile))
			return false;
		return true;
	}

	public int compareTo(Object o) {
		if(o instanceof DynamicConfiguration) {
			DynamicConfiguration other = (DynamicConfiguration) o;
			return this.getId().compareTo(other.getId());
		}
		return 0;
	}
	
	





	
}