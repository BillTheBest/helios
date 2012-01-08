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
package org.helios.spring.container.jmx;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

/**
 * <p>Title: ApplicationContextService</p>
 * <p>Description: Exposes the passed ApplicatonContext as an MBean.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(annotated=true, declared=false)
@JMXNotifications(notifications={
		@JMXNotification(description="Notification indicating that an application context has started", types=
			@JMXNotificationType(type=ApplicationContextService.EVENT_CONTEXT_STARTED)
		),
		@JMXNotification(description="Notification indicating that an application context stopped", types=
			@JMXNotificationType(type=ApplicationContextService.EVENT_CONTEXT_STOPPED)
		),
		@JMXNotification(description="Notification indicating that an application context refreshed", types=
			@JMXNotificationType(type=ApplicationContextService.EVENT_CONTEXT_REFRESHED)
		),
		@JMXNotification(description="Notification indicating that an application context closed", types=
			@JMXNotificationType(type=ApplicationContextService.EVENT_CONTEXT_CLOSED)
		),
		@JMXNotification(description="Notification indicating that the main Helios application context refreshed", types=
			@JMXNotificationType(type=ApplicationContextService.EVENT_HELIOS_CONTEXT_STARTED)
		),
		@JMXNotification(description="Notification forwarding a generic application context event", types=
			@JMXNotificationType(type=ApplicationContextService.EVENT_CONTEXT_GENERIC)
		)
		
		
})
public class ApplicationContextService extends ManagedObjectDynamicMBean implements ApplicationListener, ApplicationContext, BeanNameAware {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3364542152401761437L;
	protected String beanName = null;
	protected ApplicationContext appContext = null;
	protected boolean isGenericAppContext = false;
	protected boolean isAbstractAppContext = false;
	protected boolean isRootContext = false;
	
	/** The HeliosApplicationContext JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.spring:service=HeliosApplicationContext");
	
	/**
	 * Serial number factory for assigning unique names to anonymous application contexts.
	 */
	protected static final AtomicLong serial = new AtomicLong(0);
	
	protected static final AtomicBoolean rootAssigned = new AtomicBoolean(false);
	
	/**
	 * The default app context display name
	 */
	public static final String DEFAULT_DISPLAY_NAME = "HeliosRootSpringContext";
	
	/** The prefix for JMX notifications emmited from this service */
	public static final String EVENT_PREFIX = "org.helios.spring.event.";
	/** The context closed JMX notification type */
	public static final String EVENT_CONTEXT_CLOSED = EVENT_PREFIX + "context.closed";
	/** The context refreshed JMX notification type */
	public static final String EVENT_CONTEXT_REFRESHED = EVENT_PREFIX + "context.refreshed";
	/** The context started JMX notification type */
	public static final String EVENT_CONTEXT_STARTED = EVENT_PREFIX + "context.started";
	/** The context stopped JMX notification type */
	public static final String EVENT_CONTEXT_STOPPED = EVENT_PREFIX + "context.stopped";
	/** The helios main application context started JMX notification type */
	public static final String EVENT_HELIOS_CONTEXT_STARTED = EVENT_PREFIX + ".helios.context.started";
	/** Generic Application Event JMX notification type */
	public static final String EVENT_CONTEXT_GENERIC = EVENT_PREFIX + "generic";
	
	/**
	 * Republishes application events as JMX notifications.
	 * @param event The Spring application event
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if(event==null) return;
		if(event instanceof ContextClosedEvent) {
			forwardEventNotification(event, EVENT_CONTEXT_CLOSED);
		} else if(event instanceof ContextStoppedEvent) {
			forwardEventNotification(event, EVENT_CONTEXT_STOPPED);
		} else if(event instanceof ContextStartedEvent) {
			forwardEventNotification(event, EVENT_CONTEXT_STARTED);
		} else if(event instanceof ContextRefreshedEvent) {
			forwardEventNotification(event, EVENT_CONTEXT_REFRESHED);
			if(((ContextRefreshedEvent)event).getApplicationContext().equals(appContext)) {
				forwardEventNotification(event, EVENT_HELIOS_CONTEXT_STARTED);
			}
		} else {
			forwardEventNotification(event, EVENT_CONTEXT_GENERIC);
		}		
	}	
	
	/**
	 * Executed after this mbean is registered in the mbean server; Registers the service as a Spring application listener.
	 * @param registrationDone Indicates whether or not the MBean has been successfully registered in the MBean server.
	 */
	public void postRegister(Boolean registrationDone) {
		super.postRegister(registrationDone);
		if(registrationDone) {
			((GenericApplicationContext)appContext).addApplicationListener(this);
		}
	}
	
	/**
	 * Forwards an application event as a JMX notification
	 * @param event The application event
	 * @param type The JMX notification type
	 */
	protected void forwardEventNotification(ApplicationEvent event, String type) {
		Notification notif = new SerializableNotification(type, this, this.nextNotificationSequence(), System.currentTimeMillis());
		notif.setUserData(event);
		this.sendNotification(notif);
	}
	
	/**
	 * Constructs a new ApplicationContextService
	 * @param appContext The application context to wrap
	 */
	public ApplicationContextService(ApplicationContext appContext) {
		this(appContext, null);

	}
	
	/**
	 * Stops the JVM
	 */
	@JMXOperation(name="jvmShutdown", description="Shuts down the JVM")
	public void jvmShutdown() {
		System.out.println("Processing Shutdown Signal");
		StringBuilder b = new StringBuilder("\n\tNon Daemon Threads Running At Shutdown Signal:");
		for(Thread t: Thread.getAllStackTraces().keySet()) {
			if(!t.isDaemon()) {
				b.append("\n\t\t").append("NonDaemon Thread:").append(t.getName()).append("/").append(t.getId());
			}
		}
		System.out.println(b);		
		System.exit(-1);
	}
	
	/**
	 * Constructs a new ApplicationContextService
	 * @param appContext The application context to wrap
	 * @param name The display name for the app context
	 */
	public ApplicationContextService(ApplicationContext appContext, String name) {
		this.appContext = appContext;
		isGenericAppContext = (this.appContext instanceof GenericApplicationContext);
		isAbstractAppContext = (this.appContext instanceof AbstractApplicationContext);
		isRootContext = rootAssigned.compareAndSet(false, true);
		String appContextName = appContext.getDisplayName();
		
		if(isRootContext) {
			beanName = DEFAULT_DISPLAY_NAME;
			if((appContextName==null || appContextName.equals("")) && isAbstractAppContext) {
				((AbstractApplicationContext)this.appContext).setDisplayName(beanName);
			}
		} else {
			// appContextName
			// name
			// generated
		}
		
		
		
		if(isAbstractAppContext) {
			//((AbstractApplicationContext)this.appContext).setDisplayName(name);
		}
		this.reflectObject(this);				
	}
	
	/**
	 * @return the beanName
	 */
	@JMXAttribute(name="BeanName", description="The bean name of the application context service", mutability=AttributeMutabilityOption.READ_ONLY)
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
	 * @return the appContext
	 */
	@JMXAttribute(name="AppContext", description="The underlying application context", mutability=AttributeMutabilityOption.READ_ONLY)
	public ApplicationContext getAppContext() {
		return appContext;
	}
	/**
	 * @param appContext the appContext to set
	 */
	public void setAppContext(ApplicationContext appContext) {
		this.appContext = appContext;
	}

	/**
	 * @param beanName
	 * @return
	 * @see org.springframework.beans.factory.BeanFactory#containsBean(java.lang.String)
	 */
	@JMXOperation(name="containsBean", description="Determines if the named bean is registered")
	public boolean containsBean(@JMXParameter(name="beanName")String beanName) {
		return appContext.containsBean(beanName);
	}

	/**
	 * @param beanName
	 * @return
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition(java.lang.String)
	 */
	@JMXOperation(name="containsBeanDefinition", description="Determines if the named bean definition is registered")
	public boolean containsBeanDefinition(@JMXParameter(name="beanName")String beanName) {
		return appContext.containsBeanDefinition(beanName);
	}
	
	@JMXOperation(name="ClassPath", description="A formatted display of the system class path")
	public String getClassPath() {
		StringBuilder b = new StringBuilder();
		String[] cps = System.getProperty("java.class.path").split(File.pathSeparator);
		for(String cp: cps) {
			if(cp!=null && cp.length() > 2) {
				b.append(cp).append("\n");
			}
		}
		return b.toString();
	}

	/**
	 * @param beanName
	 * @return
	 * @see org.springframework.beans.factory.HierarchicalBeanFactory#containsLocalBean(java.lang.String)
	 */
	@JMXOperation(name="containsBeanDefinition", description="Determines if the named bean definition is registered")
	public boolean containsLocalBean(@JMXParameter(name="beanName")String beanName) {
		return appContext.containsLocalBean(beanName);
	}

	/**
	 * @param beanName
	 * @return
	 * @see org.springframework.beans.factory.BeanFactory#getAliases(java.lang.String)
	 */
	@JMXOperation(name="getAliases", description="Return the aliases for the given bean name, if any.")
	public String[] getAliases(@JMXParameter(name="beanName")String beanName) {
		return appContext.getAliases(beanName);
	}

	/**
	 * @return
	 * @throws IllegalStateException
	 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
	 */
	@JMXAttribute(name="AutowireCapableBeanFactory", description="Expose AutowireCapableBeanFactory functionality for this context", mutability=AttributeMutabilityOption.READ_ONLY)
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory()
			throws IllegalStateException {
		return appContext.getAutowireCapableBeanFactory();
	}



	/**
	 * @param beanName
	 * @param args
	 * @return
	 * @throws BeansException
	 * @see org.springframework.beans.factory.BeanFactory#getBean(java.lang.String, java.lang.Object[])
	 */
	@JMXOperation(name="getBean", description="Return an instance, which may be shared or independent, of the specified bean")
	public Object getBean(
			@JMXParameter(name="beanName")String beanName, 
			@JMXParameter(name="args")Object...args) throws BeansException {
		return appContext.getBean(beanName, args);
	}

	/**
	 * @param beanName
	 * @return
	 * @throws BeansException
	 * @see org.springframework.beans.factory.BeanFactory#getBean(java.lang.String)
	 */
	@JMXOperation(name="getBean", description="Return an instance, which may be shared or independent, of the specified bean")
	public Object getBean(@JMXParameter(name="beanName")String beanName) throws BeansException {
		return appContext.getBean(beanName);
	}

	/**
	 * @return
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionCount()
	 */
	@JMXAttribute(name="BeanDefinitionCount", description="Return the number of beans defined in the factory", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getBeanDefinitionCount() {
		return appContext.getBeanDefinitionCount();
	}

	/**
	 * @return
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionNames()
	 */
	@JMXAttribute(name="BeanDefinitionNames", description="Return the names of all beans defined in this factory", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getBeanDefinitionNames() {
		return appContext.getBeanDefinitionNames();
	}

	/**
	 * @param clazz the class or interface to match, or null for all bean names
	 * @param includeNonSingletons whether to include prototype or scoped beans too or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize lazy-init singletons and objects created by FactoryBeans (or by factory methods with a "factory-bean" reference) for the type check
	 * @return
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType(java.lang.Class, boolean, boolean)
	 */
	@JMXOperation(name="getBeanNamesForType", description="Return the names of beans matching the given type (including subclasses), judging from either bean definitions or the value of getObjectType  in the case of FactoryBeans")
	public String[] getBeanNamesForType(
			@JMXParameter(name="clazz", description="the class or interface to match, or null for all bean names")Class clazz, 
			@JMXParameter(name="includeNonSingletons", description="whether to include prototype or scoped beans too or just singletons (also applies to FactoryBeans)")boolean includeNonSingletons, 
			@JMXParameter(name="allowEagerInit ", description="whether to initialize lazy-init singletons and objects created by FactoryBeans (or by factory methods with a factory-bean reference) for the type check")boolean allowEagerInit) {
		return appContext.getBeanNamesForType(clazz, includeNonSingletons, allowEagerInit);
	}

	/**
	 * @param clazz the class or interface to match, or null for all bean names
	 * @return
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanNamesForType(java.lang.Class)
	 */
	@JMXOperation(name="getBeanNamesForType", description="Return the names of beans matching the given type (including subclasses), judging from either bean definitions or the value of getObjectType  in the case of FactoryBeans")
	public String[] getBeanNamesForType(@JMXParameter(name="clazz", description="the class or interface to match, or null for all bean names")Class clazz) {
		return appContext.getBeanNamesForType(clazz);
	}

	/**
	 * Return the bean instances that match the given object type (including subclasses), judging from either bean definitions or the value of getObjectType in the case of FactoryBeans. 
	 * @param clazz the class or interface to match, or null for all bean names
	 * @param includeNonSingletons whether to include prototype or scoped beans too or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize lazy-init singletons and objects created by FactoryBeans (or by factory methods with a "factory-bean" reference) for the type check
	 * @return
	 * @throws BeansException
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeansOfType(java.lang.Class, boolean, boolean)
	 */
	@JMXOperation(name="getBeansOfType", description="Return the names of beans matching the given type (including subclasses), judging from either bean definitions or the value of getObjectType  in the case of FactoryBeans")
	public <T> Map<String, T> getBeansOfType(@JMXParameter(name="clazz", description="the class or interface to match, or null for all bean names")Class<T> clazz, 
			@JMXParameter(name="includeNonSingletons", description="whether to include prototype or scoped beans too or just singletons (also applies to FactoryBeans)")boolean includeNonSingletons, 
			@JMXParameter(name="allowEagerInit ", description="whether to initialize lazy-init singletons and objects created by FactoryBeans (or by factory methods with a factory-bean reference) for the type check")boolean allowEagerInit) 
			throws BeansException {
		return appContext.getBeansOfType(clazz, includeNonSingletons, allowEagerInit);
	}

	/**
	 * Return the bean instances that match the given object type (including subclasses), judging from either bean definitions or the value of getObjectType in the case of FactoryBeans. 
	 * @param clazz the class or interface to match, or null for all bean names
	 * @return
	 * @throws BeansException
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeansOfType(java.lang.Class)
	 */
	@JMXOperation(name="getBeansOfType", description="Return the names of beans matching the given type (including subclasses), judging from either bean definitions or the value of getObjectType  in the case of FactoryBeans")
	public <T> Map<String, T> getBeansOfType(@JMXParameter(name="clazz", description="the class or interface to match, or null for all bean names")Class<T> clazz) throws BeansException {
		return appContext.getBeansOfType(clazz);
	}

	/**
	 * @return
	 * @see org.springframework.core.io.ResourceLoader#getClassLoader()
	 */
	@JMXAttribute(name="ClassLoader", description="Expose the ClassLoader used by this ResourceLoader", mutability=AttributeMutabilityOption.READ_ONLY)
	public ClassLoader getClassLoader() {
		return appContext.getClassLoader();
	}

	/**
	 * @return
	 * @see org.springframework.context.ApplicationContext#getDisplayName()
	 */
	@JMXAttribute(name="DisplayName", description="Return a friendly name for this context", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDisplayName() {
		return beanName;
	}

	/**
	 * @return
	 * @see org.springframework.context.ApplicationContext#getId()
	 */	
	@JMXAttribute(name="Id", description="Return the unique id of this application context", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getId() {
		return appContext.getId();
	}

	/**
	 * @param resolvable value object storing attributes required to properly resolve a message
	 * @param locale the Locale in which to do the lookup 
	 * @return
	 * @throws NoSuchMessageException
	 * @see org.springframework.context.MessageSource#getMessage(org.springframework.context.MessageSourceResolvable, java.util.Locale)
	 */
	@JMXOperation(name="getMessage", description="Try to resolve the message using all the attributes contained within the MessageSourceResolvable argument that was passed in")
	public String getMessage(
			@JMXParameter(name="resolvable", description="value object storing attributes required to properly resolve a message")MessageSourceResolvable resolvable, 
			@JMXParameter(name="locale", description="locale")Locale locale)
			throws NoSuchMessageException {
		return appContext.getMessage(resolvable, locale);
	}
	

	/**
	 * @param code the code to lookup up, such as 'calculator.noRateSet'
	 * @param args Array of arguments that will be filled in for params within the message (params look like "{0}", "{1,date}", "{2,time}" within a message), or null if none.
	 * @param locale the Locale in which to do the lookup
	 * @return
	 * @throws NoSuchMessageException
	 * @see org.springframework.context.MessageSource#getMessage(java.lang.String, java.lang.Object[], java.util.Locale)
	 */
	@JMXOperation(name="getMessage", description="Try to resolve the message. Treat as an error if the message can't be found")
	public String getMessage(
			@JMXParameter(name="code", description="the code to lookup up, such as 'calculator.noRateSet'")String code, 
			@JMXParameter(name="args", description="Array of arguments that will be filled in for params within the message")Object[] args, 
			@JMXParameter(name="locale", description="the Locale in which to do the lookup")Locale locale)
			throws NoSuchMessageException {
		return appContext.getMessage(code, args, locale);
	}

	/**
	 * @param code the code to lookup up, such as 'calculator.noRateSet'
	 * @param args Array of arguments that will be filled in for params within the message (params look like "{0}", "{1,date}", "{2,time}" within a message), or null if none.
	 * @param defaultMessage String to return if the lookup fails
	 * @param locale the Locale in which to do the lookup
	 * @return
	 * @see org.springframework.context.MessageSource#getMessage(java.lang.String, java.lang.Object[], java.lang.String, java.util.Locale)
	 */
	//@JMXOperation(name="getMessage", description="Try to resolve the message. Return default message if no message was found")
	public String getMessage(
			@JMXParameter(name="code", description="the code to lookup up, such as 'calculator.noRateSet'")String code, 
			@JMXParameter(name="args", description="Array of arguments that will be filled in for params within the message")Object[] args,
			@JMXParameter(name="defaultMessage", description="String to return if the lookup fails")String defaultMessage,
			@JMXParameter(name="locale", description="the Locale in which to do the lookup")Locale locale) {
		return appContext.getMessage(code, args, defaultMessage, locale);
	}

	/**
	 * @return
	 * @see org.springframework.context.ApplicationContext#getParent()
	 */
	@JMXAttribute(name="Parent", description="Return the parent context, or null if there is no parent and this is the root of the context hierarchy", mutability=AttributeMutabilityOption.READ_ONLY)
	public ApplicationContext getParent() {
		return appContext.getParent();
	}

	/**
	 * @return
	 * @see org.springframework.beans.factory.HierarchicalBeanFactory#getParentBeanFactory()
	 */
	@JMXAttribute(name="ParentBeanFactory", description="Return the parent bean factory, or null if there is none", mutability=AttributeMutabilityOption.READ_ONLY)
	public BeanFactory getParentBeanFactory() {
		return appContext.getParentBeanFactory();
	}

	/**
	 * @param location the resource location 
	 * @return
	 * @see org.springframework.core.io.ResourceLoader#getResource(java.lang.String)
	 */
	@JMXOperation(name="getResource", description="Return a Resource handle for the specified resource")
	public Resource getResource(
			@JMXParameter(name="location", description="the resource location")String location) {
		return appContext.getResource(location);
	}

	/**
	 * @param locationPattern the location pattern to resolve 
	 * @return
	 * @throws IOException
	 * @see org.springframework.core.io.support.ResourcePatternResolver#getResources(java.lang.String)
	 */	
	@JMXOperation(name="getResources", description="Resolve the given location pattern into Resource objects")
	public Resource[] getResources(@JMXParameter(name="locationPattern", description="the location pattern to resolve ")String locationPattern) throws IOException {
		return appContext.getResources(locationPattern);
	}

	/**
	 * @return
	 * @see org.springframework.context.ApplicationContext#getStartupDate()
	 */
	@JMXAttribute(name="StartupDate", description="Return the timestamp when this context was first loaded", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getStartupDate() {
		return appContext.getStartupDate();
	}
	
	@JMXAttribute(name="StartupTime", description="Return the date when this context was first loaded", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getStartupTime() {
		return new Date(appContext.getStartupDate());
	}	

	/**
	 * Determine the type of the bean with the given name
	 * @param name the name of the bean to query 
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.beans.factory.BeanFactory#getType(java.lang.String)
	 */
	@JMXOperation(name="getType", description="Resolve the given location pattern into Resource objects")
	public Class getType(@JMXParameter(name="name", description="the name of the bean to query")String name) throws NoSuchBeanDefinitionException {
		return appContext.getType(name);
	}

	/**
	 * Is this bean a prototype? 
	 * @param name the name of the bean to query 
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.beans.factory.BeanFactory#isPrototype(java.lang.String)
	 */
	@JMXOperation(name="isPrototype", description="Is this bean a prototype?")
	public boolean isPrototype(@JMXParameter(name="name", description="the name of the bean to query")String name)
			throws NoSuchBeanDefinitionException {
		return appContext.isPrototype(name);
	}

	/**
	 * @param name the name of the bean to query
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.beans.factory.BeanFactory#isSingleton(java.lang.String)
	 */
	@JMXOperation(name="isSingleton", description="Is this bean a singleton?")
	public boolean isSingleton(@JMXParameter(name="name", description="the name of the bean to query")String name)
			throws NoSuchBeanDefinitionException {
		return appContext.isSingleton(name);
	}

	/**
	 * @param name the name of the bean to query 
	 * @param targetType the type to match against 
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.beans.factory.BeanFactory#isTypeMatch(java.lang.String, java.lang.Class)
	 */
	@JMXOperation(name="isTypeMatch", description="Check whether the bean with the given name matches the specified type")
	public boolean isTypeMatch(
			@JMXParameter(name="name", description="the name of the bean to query")String name, 
			@JMXParameter(name="targetType", description="the type to match against")Class targetType)
			throws NoSuchBeanDefinitionException {
		return appContext.isTypeMatch(name, targetType);
	}

	/**
	 * @param appEvent the event to publish
	 * @see org.springframework.context.ApplicationEventPublisher#publishEvent(org.springframework.context.ApplicationEvent)
	 */
	@JMXOperation(name="publishEvent", description="Notify all listeners registered with this application of an application event")
	public void publishEvent(@JMXParameter(name="appEvent", description="the event to publish")ApplicationEvent appEvent) {
		appContext.publishEvent(appEvent);
	}
	
	/**
	 * HeliosApplicationContext static accessor
	 * @return the root helios spring application server
	 */
	public static ApplicationContext get() {
		MBeanServer mbs = JMXHelper.getRuntimeHeliosMBeanServer();
		if(mbs.isRegistered(OBJECT_NAME)) {
			return JMXHelper.getRuntimeHeliosMBeanServer().getAttribute(OBJECT_NAME, "ApplicationContext", ApplicationContext.class);
		}
		throw new IllegalStateException("The HeliosApplicationContext MBean is not registered", new Throwable());
	}
	
	/**
	 * Stops this application context
	 */
	@JMXOperation(name="stopContext", description="Stops this application context")
	public void stopContext() {
		try { ((GenericApplicationContext)appContext).stop();} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * Closes this application context
	 */
	@JMXOperation(name="closeContext", description="Closes this application context")
	public void closeContext() {
		try { ((GenericApplicationContext)appContext).close();} catch (Exception e) {}
	}
	
	/**
	 * Starts this application context
	 */
	@JMXOperation(name="startContext", description="Starts this application context")
	public void startContext() {
		try { ((GenericApplicationContext)appContext).start();} catch (Exception e) {}
	}
	
	/**
	 * Refreshes this application context
	 */
	@JMXOperation(name="refreshContext", description="Refreshes this application context")
	public void refreshContext() {
		try { ((GenericApplicationContext)appContext).refresh(); } catch (Exception e) {}
	}
	
	/**
	 * Returns the delegate application context
	 * @return an application context
	 */
	@JMXAttribute(name="ApplicationContext", description="The main helios application context", mutability=AttributeMutabilityOption.READ_ONLY)
	public ApplicationContext getApplicationContext() {
		return appContext;
	}
	
	public static void main(String args[]) {
		BasicConfigurator.configure();
		StaticApplicationContext sac = new StaticApplicationContext();
		ApplicationContextService acs = new ApplicationContextService(sac);
		ObjectName on = JMXHelper.objectName("org.helios.spring:service=AppContextService,name=Boo");
		sac.refresh();
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(acs, on);
			System.out.println("Registered:" + ManagementFactory.getRuntimeMXBean().getName());
			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Indicates if the underlying application context is an instance of GenericApplicationContext
	 * @return the isGenericAppContext
	 */
	@JMXAttribute(name="GenericAppContext", description="Indicates if the underlying application context is an instance of GenericApplicationContext", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getGenericAppContext() {
		return isGenericAppContext;
	}

	/**
	 * Indicates if the underlying application context is an instance of AbstractApplicationContext
	 * @return the isAbstractAppContext
	 */
	@JMXAttribute(name="AbstractAppContext", description="Indicates if the underlying application context is an instance of AbstractApplicationContext", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean isAbstractAppContext() {
		return isAbstractAppContext;
	}

	/**
	 * Indicates if the underlying application context is the HeliosSpring root context
	 * @return the isRootContext
	 */
	@JMXAttribute(name="RootContext", description="Indicates if the underlying application context is the HeliosSpring root context", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean isRootContext() {
		return isRootContext;
	}

	/**
	 * Find a Annotation of annotationType on the specified bean, traversing its interfaces and super classes if no annotation can be found on the given class itself. 
	 * @param name the name of the bean
	 * @param annotation the annotation to look for
	 * @return the annotation of the given type found, or null
	 * @see org.springframework.beans.factory.ListableBeanFactory#findAnnotationOnBean(java.lang.String, java.lang.Class)
	 */
	@Override
	public <A extends Annotation> A findAnnotationOnBean(String name, Class<A> annotation) {
		return appContext.findAnnotationOnBean(name, annotation);
	}


	/**
	 * Finds beans annotated with the passed annotation
	 * @param annotation the annotation to look for
	 * @return a map of beans keyed by name
	 * @throws BeansException
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeansWithAnnotation(java.lang.Class)
	 */
	@Override
	public Map<String, Object> getBeansWithAnnotation( Class<? extends Annotation> annotation) throws BeansException {
		return appContext.getBeansWithAnnotation(annotation);
	}

	/**
	 * Return the bean instance that uniquely matches the given object type, if any. 
	 * @param clazz the type the bean must match; can be an interface or superclass. null is disallowed. 
	 * This method goes into ListableBeanFactory by-type lookup territory but may also be translated into 
	 * a conventional by-name lookup based on the name of the given type. 
	 * For more extensive retrieval operations across sets of beans, use ListableBeanFactory and/or BeanFactoryUtils.  
	 * @return an instance of the single bean matching the required type 
	 * @throws BeansException
	 * @see org.springframework.beans.factory.BeanFactory#getBean(java.lang.Class)
	 */
	@Override
	public <T> T getBean(Class<T> clazz) throws BeansException { 
		return appContext.getBean(clazz);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.Behaves the same as getBean(String), 
	 * but provides a measure of type safety by throwing a BeanNotOfRequiredTypeException if the bean is not of the required type. 
	 * This means that ClassCastException can't be thrown on casting the result correctly, as can happen with getBean(String).
	 * Translates aliases back to the corresponding canonical bean name. Will ask the parent factory if the bean cannot be found in this factory instance. 
	 * @param name the bean name
	 * @param clazz the type the bean must match
	 * @return an instance of the bean
	 * @throws BeansException
	 * @see org.springframework.beans.factory.BeanFactory#getBean(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T> T getBean(String name, Class<T> clazz) throws BeansException {
		return appContext.getBean(name, clazz);
	}

	/**
	 * Returns the application context's environment
	 * @return the application context's environment
	 */
	@Override
	public Environment getEnvironment() {
		return appContext.getEnvironment();
	}
	
	/**
	 * Returns the set of profiles explicitly made active for this environment.
	 * @return the set of profiles explicitly made active for this environment.
	 */
	@JMXAttribute(name="ActiveProfiles", description="The set of profiles explicitly made active for this environment.", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getActiveProfiles() {
		return appContext.getEnvironment().getActiveProfiles();
	}
	
	/**
	 * Returns the set of profiles to be active by default when no active profiles have been set explicitly.
	 * @return the set of profiles to be active by default when no active profiles have been set explicitly.
	 */
	@JMXAttribute(name="DefaultProfiles", description="The set of profiles to be active by default when no active profiles have been set explicitly.", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getDefaultProfiles() {
		return appContext.getEnvironment().getDefaultProfiles();
	}
	

}
