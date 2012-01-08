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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.helios.spring.container.HeliosApplicationContext;
import org.helios.spring.container.templates.merging.ArgumentMerger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Title: DynamicBeanFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class DynamicBeanFactory implements  BeanNameAware 
	, ApplicationContextAware
{
	protected GenericApplicationContext appContext = null;
	protected String beanName = null;
	protected String contextName = null;
	protected String templateName = null;
	protected Logger log = Logger.getLogger(getClass());
	protected Map<Integer, Object> constructorArguments = new HashMap<Integer, Object>();	 
	protected Map<String, Object> properties = new HashMap<String, Object>();	 
	protected TemplateProvider templateProvider = null;
	protected Map<Class, ArgumentMerger> argumentMergers = new HashMap<Class, ArgumentMerger>();
	

	public void foo() {
		
	}
	
	@SuppressWarnings("unchecked")
	public void start() {
		
		
		log.info("Starting DynamicBeanFactory[" + beanName + "]");
		log.info("Template Name:" + templateName + ". Valid:" + appContext.containsBeanDefinition(templateName));
		GenericApplicationContext gac = new GenericApplicationContext(appContext);
		if(contextName != null) {
			gac.setDisplayName(contextName);
		}
		
		if(templateProvider!=null && templateProvider.getProvisions() != null && templateProvider.getProvisions().length > 0) {
			for(Provision provision: templateProvider.getProvisions()) {
				try {
					if(contextName == null) {
						contextName = provision.getContextName();
						gac.setDisplayName(contextName);
					}
					GenericBeanDefinition templateDef = (GenericBeanDefinition)appContext.getBeanDefinition(templateName);
					BeanDefinitionBuilder bdb = BeanDefinitionBuilder.genericBeanDefinition(templateDef.getBeanClassName());
					Class clazz = Class.forName(templateDef.getBeanClassName());
					Map<Integer, Object> innerConstructorArguments = provision.getConstructorArguments();
					Map<String, Object> innerProperties = provision.getProperties();
					Map<String, String> propertyByRefs = new HashMap<String, String>();
							
					constructorArguments.putAll(innerConstructorArguments);

					properties = mergeProperties(clazz, properties, innerProperties);
					for(Entry<String, Object> beanProp: properties.entrySet()) {
						String value = beanProp.getValue().toString(); 
						if(value.toLowerCase().startsWith("ref:")) {
							propertyByRefs.put(beanProp.getKey(), value.substring(4));
						}
					}
					for(String remKey: propertyByRefs.keySet()) {
						properties.remove(remKey);
					}

					
					if(log.isDebugEnabled()) {
						log.debug("Constructor Args:[" + constructorArguments + "]");
						log.debug("Properties:[" + properties + "]");
						log.debug("Property References:[" + propertyByRefs + "]");
					}
					
					
					TreeSet<Integer> set = new TreeSet<Integer>(constructorArguments.keySet());
					for(Integer ctorArgId: set) {
						Object value = constructorArguments.get(ctorArgId);
						String valueStr = value.toString();
						if(valueStr.toLowerCase().startsWith("ref:")) {
							bdb.addConstructorArgReference(valueStr.substring(4));
							if(log.isDebugEnabled()) log.debug("\tAdding Ctor Reference [" + ctorArgId + "]:" + valueStr.substring(4));
						} else {
							if(log.isDebugEnabled()) log.debug("\tAdding Ctor Value [" + ctorArgId + "]:" + value);
							bdb.addConstructorArgValue(value);										
						}
					}
					for(Entry<String, Object> prop: properties.entrySet()) {
						if(log.isDebugEnabled()) log.debug("\tAdding Property Value [" + prop.getKey() + "]:" + prop.getValue());
						bdb.addPropertyValue(prop.getKey(), prop.getValue());
					}					
					for(Entry<String, String> refProp: propertyByRefs.entrySet()) {
						if(log.isDebugEnabled()) log.debug("\tAdding Property Reference [" + refProp.getKey() + "]:" + refProp.getValue());
						bdb.addPropertyReference(refProp.getKey(), refProp.getValue());
					}
					
					
					bdb.setLazyInit(templateDef.isLazyInit());
					bdb.setScope(templateDef.getScope());
					bdb.setAutowireMode(templateDef.getAutowireMode());
					bdb.setDependencyCheck(templateDef.getDependencyCheck());
					if(templateDef.getDestroyMethodName()!=null) {
						bdb.setDestroyMethodName(templateDef.getDestroyMethodName());
					}
					if(templateDef.getInitMethodName()!=null) {
						bdb.setInitMethodName(templateDef.getInitMethodName());
					}
					
					
					BeanDefinition beanDefinition = bdb.getBeanDefinition();
					log.info("Registering new bean [" + provision.getBeanName() + "]");
			
					gac.registerBeanDefinition(provision.getBeanName(), beanDefinition);
					gac.registerShutdownHook();
					log.info("Registered [" + provision.getBeanName() + "]");					
				} catch (Exception e) {
					log.error("Failed to generate dynamic bean [" + provision.getBeanName() + "]", e);
				}				
			}  // end of provisions loop
			try {
				gac.refresh();
			} catch (Exception e) {
				log.error("Failed to refresh dynamic bean application context [" + contextName + "]", e);
			}
		}	
	}
	
	protected Map<String, Object> mergeProperties(Class targetClass, Map<String, Object> templateProperties, Map<String, Object> provisionProperties) {
		if(templateProperties==null || templateProperties.size()<1) return provisionProperties;
		if(provisionProperties==null || provisionProperties.size()<1) return templateProperties;
		Map<String, Object> merged = new HashMap<String, Object>();
		Set<String> intersects = new HashSet<String>();
		intersects.addAll(templateProperties.keySet());
		intersects.addAll(provisionProperties.keySet());
		for(String key: intersects) {
			merged.put(key, mergeArguments(key, targetClass, templateProperties.get(key), provisionProperties.get(key)));
			templateProperties.remove(key);
			provisionProperties.remove(key);
		}
		merged.putAll(templateProperties);
		merged.putAll(provisionProperties);
		
		return merged;
	}
	
	protected Object mergeArguments(String key, Class targetClass, Object templateArg, Object provisionArg) {
		if(provisionArg==null) return templateArg;
		if(templateArg==null) return provisionArg;
		Class targetType = getPropertyType(key, targetClass);
		if(targetType==null) return provisionArg;
		ArgumentMerger am = argumentMergers.get(targetType);
		if(am==null) return provisionArg;
		return am.mergeArguments((String)templateArg, (String)provisionArg);
	}
	
	protected Class getPropertyType(String name, Class targetClass) {
		Method[] methods = targetClass.getMethods();
		for(Method method: methods) {
			if(method.getName().equalsIgnoreCase("set" + name) && method.getParameterTypes().length==1) {
				return method.getParameterTypes()[0];
			}
		}
		return null;
	}
	
	

	protected Constructor introspectConstructor(Map<Integer, Object> templateArgs, Map<Integer, Object> provisionArgs, Class targetClass) {
			Constructor ctor = null;
			Constructor[] candidates = targetClass.getConstructors();			
			Set<Integer> argIndexes = new HashSet<Integer>(templateArgs.keySet());
			argIndexes.addAll(provisionArgs.keySet());
			int argCount = argIndexes.size();
			candidates = getConstructorWithArgCount(candidates, argCount);
			if(candidates.length==0) throw new RuntimeException("No constructors in class [" + targetClass.getName() + "] have the required number of parameters [" + argCount + "]");
			if(candidates.length>1) throw new RuntimeException("Ambiguous constructors in class [" + targetClass.getName() + "] have the required number of parameters [" + argCount + "]");
			if(candidates.length==1) ctor = candidates[0];
			return ctor;
	}
	
	/**
	 * Returns an array of constructors from the passed constructors that have the number of parameters passed.
	 * @param ctorList The list of candidate constructors.
	 * @param argCount The number of parameters
	 * @return An array of matching constructors.
	 */
	protected Constructor[] getConstructorWithArgCount(Constructor[] ctorList, int argCount) {
		Set<Constructor> matches = new HashSet<Constructor>();
		for(Constructor ctor: ctorList) {
			if(ctor.getParameterTypes().length==argCount) {
				matches.add(ctor);
			}
		}
		return matches.toArray(new Constructor[matches.size()]);
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
	public void setConstructorArguments(Map<Integer, Object> constructorArguments) {
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
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}
	
	
	
	/**
	 * @param appContext
	 * @throws BeansException
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext appContext) throws BeansException {
		this.appContext = (GenericApplicationContext)appContext;
	}

	/**
	 * @param beanName
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String beanName) {
		this.beanName = beanName;
		log = Logger.getLogger(getClass().getName() + "." + beanName);
	}

	/**
	 * @return the templateName
	 */
	public String getTemplateName() {
		return templateName;
	}

	/**
	 * @param templateName the templateName to set
	 */
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
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
	 * @param templateProvider the templateProvider to set
	 */
	public void setTemplateProvider(TemplateProvider templateProvider) {
		this.templateProvider = templateProvider;
	}

	/**
	 * @param argumentMergers the argumentMergers to set
	 */
	public void setArgumentMergers(Map<Class, ArgumentMerger> argumentMergers) {
		if(argumentMergers!=null) {
			argumentMergers.putAll(this.argumentMergers);
		}
		this.argumentMergers = argumentMergers; 		
	}


}
