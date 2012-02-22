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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * <p>Title: HeliosApplicationContext</p>
 * <p>Description: Customized app context that keeps a registry of it's child contexts and makes them accessible either directly or using a prefix.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class HeliosApplicationContext extends GenericXmlApplicationContext implements ApplicationListener {
	/** the main Spring app context */
	protected Map<String, ApplicationContext> childContexts = new ConcurrentHashMap<String, ApplicationContext>();
	/** a map of all beans by name */
	protected final Map<String, Object> allBeans = new ConcurrentHashMap<String, Object>();
//	/** An XML Bean Definition Reader */
//	protected final XmlBeanDefinitionReader beanDefReader = new XmlBeanDefinitionReader(this);
	/** All registered configuration files */
	protected final Set<String> configurations = new HashSet<String>();
	/** Indicated if configurations have been loaded */
	protected boolean configurationLoaded = false;
	/** The number of bean definitions loaded */
	protected int beanDefsLoaded = 0;
	
	
	
	/** instance logger */
	protected Logger log = Logger.getLogger(getClass());
	
	/**
	 * @param name
	 * @return
	 */
	public ApplicationContext getNamedApplicationContext(String name) {
		return childContexts.get(name);
	}
	
	
	

	/**
	 * @param configLocation
	 * @throws BeansException
	 */
	public HeliosApplicationContext(String configLocation) throws BeansException {
		super();
		configurations.add(configLocation);
	}




	/**
	 * @param configLocations
	 * @param parent
	 * @throws BeansException
	 * @throws MalformedURLException 
	 */
	public HeliosApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException, MalformedURLException {
		super();
		setParent(parent);
		Collections.addAll(configurations, configLocations);
		initBeans();
	}




	/**
	 * @param configLocations
	 * @param refresh
	 * @param parent
	 * @throws BeansException
	 * @throws MalformedURLException 
	 */
	public HeliosApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent) throws BeansException, MalformedURLException {
		this(configLocations, parent);
		if(refresh) {
			try {
				beanDefsLoaded = initBeans();
			} catch (MalformedURLException mue) {
				throw new FatalBeanException("Failed to load bean definitions", mue);
			}			
		}
	}




	/**
	 * @param configLocations
	 * @param refresh
	 * @throws BeansException
	 * @throws MalformedURLException 
	 */
	public HeliosApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations);
		if(refresh) {
			try {
				beanDefsLoaded = initBeans();
			} catch (MalformedURLException mue) {
				throw new FatalBeanException("Failed to load bean definitions", mue);
			}			
		}

	}




	/**
	 * @param configLocations
	 * @throws BeansException
	 * @throws MalformedURLException 
	 */
	public HeliosApplicationContext(String[] configLocations) throws BeansException {
		super();
		Collections.addAll(configurations, configLocations);
		try {
			beanDefsLoaded = initBeans();
		} catch (MalformedURLException mue) {
			throw new FatalBeanException("Failed to load bean definitions", mue);
		}			
	}
	
	/**
	 * Adds the passed configuration locations to the pre-refresh configs.
	 * @param configLocations An array of config locations.
	 */
	public void addConfigurations(String...configLocations) {
		Collections.addAll(configurations, configLocations);
	}
	
	public void load(String...configFiles) {
		super.load(configFiles);
		beanDefsLoaded = getBeanDefinitionCount();
	}

	/**
	 * Loads the bean definitions from all configurations.
	 * @return The number of bean definitions found
	 * @throws MalformedURLException
	 */
	protected int initBeans() throws MalformedURLException {
		int beansFound = 0;
		List<Resource> resources = new ArrayList<Resource>();
		for(String config: configurations) {			
			Resource rez = new UrlResource(config);
			//beansFound += beanDefReader.loadBeanDefinitions(rez);
		}
		load(resources.toArray(new Resource[resources.size()]));
		beansFound = getBeanDefinitionCount();
		configurationLoaded = true;
		return beansFound;
	}
	
	/**
	 * Load or refresh the persistent representation of the configuration, which might an XML file, properties file, or relational database schema. 
	 * As this is a startup method, it should destroy already created singletons if it fails, to avoid dangling resources. In other words, after invocation of that method, either all or no singletons at all should be instantiated. 
	 * @see org.springframework.context.support.AbstractApplicationContext#refresh()
	 */
	public void refresh() {
		// load files
		if(!configurationLoaded) {
			try {
				beanDefsLoaded = initBeans();
			} catch (MalformedURLException mue) {
				throw new FatalBeanException("Failed to load bean definitions", mue);
			}
		}
		this.registerShutdownHook();
		try {
			super.refresh();
		} catch (Exception e) {
			log.error("Faied to refresh HeliosApplicationContext", e);
		}
	}


	/**
	 * Override of the GenericApplicationContext method.
	 * Returns this context's bean names as well as the all the child bean names prefixed with the child context display name.
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanDefinitionNames()
	 */	
	public String[] getQualifiedBeanDefinitionNames() {
		List<String> beanNames = new ArrayList<String>();
		for(String s: super.getBeanDefinitionNames()) {
			beanNames.add(s);
		}
		for(Entry<String, ApplicationContext> entry: childContexts.entrySet()) {
			for(String s: entry.getValue().getBeanDefinitionNames()) {
				beanNames.add(entry.getKey() + "/" + s);
			}			
		}
		return beanNames.toArray(new String[beanNames.size()]);
	}
	
//	/**
//	 * Return the names of all beans defined in this factory.
//	 * @return the names of all beans defined in this factory, or an empty array if none defined
//	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanDefinitionNames()
//	 */
//	public String[] getBeanDefinitionNames() {
//		List<String> beanNames = new ArrayList<String>();
//		for(String s: super.getBeanDefinitionNames()) {
//			beanNames.add(s);
//		}
//		
//		for(Entry<String, ApplicationContext> entry: childContexts.entrySet()) {
//			for(String s: entry.getValue().getBeanDefinitionNames()) {
//				beanNames.add(s);
//			}			
//		}
//		return beanNames.toArray(new String[beanNames.size()]);		
//	}
	
	
//	/**
//	 * Return an instance, which may be shared or independent, of the specified bean. 
//	 * @param beanName the name of the bean to retrieve 
//	 * @return an instance of the bean 
//	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String)
//	 */
//	public Object getBean(String beanName) {
//		Object o = allBeans.get(beanName); 
//		if(o!=null) return o;
//		return super.getBean(beanName);
//		
//	}
	
//	/**
//	 * @return
//	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanDefinitionCount()
//	 */
//	public int getBeanDefinitionCount() {
//		return getBeanDefinitionNames().length;
//	}
//	
//	/**
//	 * Return an instance, which may be shared or independent, of the specified bean. 
//	 * @param beanName the name of the bean to retrieve
//	 * @param clazz type the bean must match. Can be an interface or superclass of the actual class, or null for any match. For example, if the value is Object.class, this method will succeed whatever the class of the returned instance. 
//	 * @return an instance of the bean 
//	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String, java.lang.Class)
//	 */
//	@SuppressWarnings("unchecked")
//	public Object getBean(String beanName, Class clazz) {
//		Object o = allBeans.get(beanName); 
//		if(o==null) throw new NoSuchBeanDefinitionException(beanName);
//		if(!o.getClass().equals(clazz)) throw new BeanNotOfRequiredTypeException(beanName, clazz, o.getClass()); 
//		return o;
//	}
//	
//	/**
//	 * Return an instance, which may be shared or independent, of the specified bean. 
//	 * @param beanName the name of the bean to retrieve
//	 * @param args arguments to use if creating a prototype using explicit arguments to a static factory method. It is invalid to use a non-null args value in any other case. 
//	 * @return an instance of the bean 
//	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String, java.lang.Object[])
//	 */
//	public Object getBean(String beanName, Object[] args) {
//		Object o = allBeans.get(beanName); 
//		if(o==null) throw new NoSuchBeanDefinitionException(beanName);
//		if(childContexts.containsKey(beanName)) {
//			childContexts.get(beanName).getBean(beanName, args);
//		} else {
//			super.getBean(beanName, args);
//		}
//		return o;
//	}
	
	/**
//	 * Does this bean factory contain a bean with the given name? More specifically, is getBean(java.lang.String) able to obtain a bean instance for the given name? 
//	 * @param beanName the name of the bean to query 
//	 * @return whether a bean with the given name is defined
//	 * @see org.springframework.context.support.AbstractApplicationContext#containsBean(java.lang.String)
//	 */
//	public boolean containsBean(String beanName) {
//		return allBeans.containsKey(beanName);
//	}
//	
	/**
	 * Fired when this container is closed.
	 * @see org.springframework.context.support.AbstractApplicationContext#onClose()
	 */
	public void onClose() {
		for(ApplicationContext context: childContexts.values()) {
			if(context instanceof AbstractApplicationContext) {
				((AbstractApplicationContext)context).close();
			}
		}
		super.onClose();
	}
	
	/**
	 * 
	 */
	public HeliosApplicationContext() {
		addApplicationListener(this);
	}



	/**
	 * @param parent
	 */
	public HeliosApplicationContext(ApplicationContext parent) {
		super();
		setParent(parent);
		addApplicationListener(this);
	}


	
	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#toString()
	 */
	public String toString() {
		StringBuilder b = new StringBuilder(super.toString());
		b.append("\nSubContexts:");
		if(childContexts!=null) {
			for(ApplicationContext gac: childContexts.values()) {
				b.append("\n\t").append(gac.toString());
			}
		}
		return b.toString();
		
	}
	

	
	
//	public boolean containsBeanDefinition(String beanName) {
//		if(templateFacto)
//		return templateFactory.containsBeanDefinition(beanName);
//	}
	
	

	/**
	 * Listens on application events and registers any comtext refreshed events from child app contexts.
	 * @param event
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	public void onApplicationEvent(ApplicationEvent event) {	
		if(log.isDebugEnabled()) log.debug("\n\tAPP EVENT:" + event);
		Object source = event.getSource();
		if(event instanceof ContextRefreshedEvent) {
			if(log.isDebugEnabled()) log.debug("ContextRefreshedEvent");			
			if(source instanceof AbstractApplicationContext) {
				if(source.equals(this)) {
					//========================================================
					//  Needed for dynamics ?
					//========================================================
					String[] beanNames = super.getBeanDefinitionNames();
					for(String s: beanNames) {
						if(allBeans.containsKey(s)) {
							if(log.isDebugEnabled()) log.warn("Global Bean Name Cache Parent Entry Overwrote One Named Reference to bean name [" + s + "]");
						}
						allBeans.put(s, super.getBean(s));
						if(log.isDebugEnabled()) log.debug("Added Parent Bean [" + s + "] to global context.");						
					}
					//========================================================
				} else {
					if(log.isDebugEnabled()) log.debug("Creating Child Context");
					AbstractApplicationContext child = (AbstractApplicationContext)source;
					child.addApplicationListener(this);
					if(child.getParent().equals(this)) {
						log.info("Adding Child App Context Instance[" + child.getDisplayName() + "]");
						childContexts.put(child.getDisplayName(), child);
						String[] beanNames = child.getBeanDefinitionNames();
						for(String s: beanNames) {						
							if(allBeans.containsKey(s)) {
								if(log.isDebugEnabled()) log.warn("Global Bean Name Cache Child Entry Overwrote One Named Reference to bean name [" + s + "]");
							}
							allBeans.put(s, child.getBean(s));
							if(log.isDebugEnabled()) log.debug("Added Child Bean [" + s + "] to global context.");
						}
						log.info("Added [" + beanNames.length + "] Beans from Child App Context Instance[" + child.getDisplayName() + "]");
					}
				}
			}
		} else if(event instanceof ContextClosedEvent) {
			if(source instanceof AbstractApplicationContext) {
				AbstractApplicationContext appContext = (AbstractApplicationContext)source;
				childContexts.remove(appContext.getDisplayName());
				String[] beanNames = appContext.getBeanDefinitionNames();
				for(String s: beanNames) {
					allBeans.remove(s);
				}
				
			}
		}  else if(event instanceof ContextStartedEvent) {
			if(source instanceof AbstractApplicationContext) {
				if(!source.equals(this)) {
					AbstractApplicationContext appContext = (AbstractApplicationContext)source;
					childContexts.put(appContext.getDisplayName(), appContext);
					String[] beanNames = appContext.getBeanDefinitionNames();
					for(String s: beanNames) {
						//allBeans.put(s, appContext.getBean(s));
						//((DefaultListableBeanFactory)this.getBeanFactory()).registerBeanDefinition(s, appContext.getBeanFactory().getBeanDefinition(s));
					}

				}
			}			
		}
	}






//	/**
//	 * @param name
//	 * @return
//	 * @see org.springframework.context.support.AbstractApplicationContext#containsBeanDefinition(java.lang.String)
//	 */
//	public boolean containsBeanDefinition(String name) {
//		return allBeans.containsKey(name);
//	}
//
//	/**
//	 * @param name
//	 * @return
//	 * @see org.springframework.context.support.AbstractApplicationContext#containsLocalBean(java.lang.String)
//	 */
//	public boolean containsLocalBean(String name) {
//		return super.containsLocalBean(name);
//	}
//
//
//	/**
//	 * @param type
//	 * @param includePrototypes
//	 * @param allowEagerInit
//	 * @return
//	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanNamesForType(java.lang.Class, boolean, boolean)
//	 */
//	public String[] getBeanNamesForType(Class type, boolean includePrototypes,
//			boolean allowEagerInit) {
//		Set<String> names = new HashSet<String>();
//		for(Map.Entry<String, Object> bean: allBeans.entrySet()) {
//			if(type.isAssignableFrom(bean.getValue().getClass())) {
//				//==== BAD BAD BAD ====
//				names.add(bean.getKey());
//			}
//		}
//		return names.toArray(new String[names.size()]);
//	}
//
//	/**
//	 * @param type
//	 * @return
//	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanNamesForType(java.lang.Class)
//	 */
//	public String[] getBeanNamesForType(Class type) {
//		Set<String> names = new HashSet<String>();
//		for(Map.Entry<String, Object> bean: allBeans.entrySet()) {
//			if(type.isAssignableFrom(bean.getValue().getClass())) {
//				names.add(bean.getKey());
//			}
//		}
//		return names.toArray(new String[names.size()]);
//	}
//
//	/**
//	 * @param type
//	 * @param includePrototypes
//	 * @param allowEagerInit
//	 * @return
//	 * @throws BeansException
//	 * @see org.springframework.context.support.AbstractApplicationContext#getBeansOfType(java.lang.Class, boolean, boolean)
//	 */
//	@SuppressWarnings("unchecked")
//	public Map getBeansOfType(Class type, boolean includePrototypes,
//			boolean allowEagerInit) throws BeansException {
//		if(allBeans==null) return Collections.emptyMap();
//		Map matches = new HashMap();				
//		for(Map.Entry<String, Object> bean: allBeans.entrySet()) {
//			if(type.isAssignableFrom(bean.getValue().getClass())) {
//				//==== BAD BAD BAD ====
//				matches.put(bean.getKey(), bean.getValue());
//			}
//		}
//		
//		return matches;
//	}
//
//	/**
//	 * @param type
//	 * @return
//	 * @throws BeansException
//	 * @see org.springframework.context.support.AbstractApplicationContext#getBeansOfType(java.lang.Class)
//	 */
//	@SuppressWarnings("unchecked")
//	public Map getBeansOfType(Class type) throws BeansException {
//		Map matches = new HashMap();
//		if(allBeans==null) return Collections.emptyMap();
//		for(Map.Entry<String, Object> bean: allBeans.entrySet()) {
//			if(type.isAssignableFrom(bean.getValue().getClass())) {
//				//==== BAD BAD BAD ====
//				matches.put(bean.getKey(), bean.getValue());
//			}
//		}
//		
//		return matches;
//	}

}
