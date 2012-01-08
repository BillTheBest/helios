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
package org.helios.jmx.server;

import java.io.ObjectInputStream;
import java.lang.management.ManagementFactory;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMX;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

import org.helios.jmx.aliases.AliasMBeanRegistry;

/**
 * <p>Title: WrappedMBeanServer</p>
 * <p>Description: An MBeanServer instance wrapper that allows for somea  customization.
 * Currently only provides a better domain name when the delegate's default domain is null. See todos.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.server.WrappedMBeanServer</code></p>
 */

public class WrappedMBeanServer implements MBeanServer {
	/** A cache of wrapped mbeanservers keyed by the system identity hash code of the delegate mbean server */
	private static final Map<Integer, SoftReference<MBeanServer>> instances = new ConcurrentHashMap<Integer, SoftReference<MBeanServer>>(); 
	/** A reference to the actual platform agent */
	private static final MBeanServer platformAgent = ManagementFactory.getPlatformMBeanServer();
	/** The mbeanserver the calls are delegated to */
	private final MBeanServer inner;
	/** The rewritten domain name */
	private final String domain;
	
	/**
	 * Creates a new WrappedMBeanServer 
	 * @param inner The MBeanServer calls are delegated to
	 */
	private WrappedMBeanServer(MBeanServer inner) {
		domain = getDomainName(inner);
		this.inner = inner;
	}
	
	/**
	 * Assigns a slightly more useful domain name for delegates that have a null default domain
	 * @return the default domain that this mbean server will provide
	 */
	protected static String getDomainName(MBeanServer target) {
		String innerDomain = target.getDefaultDomain();
		if(innerDomain==null) {			
			if(System.identityHashCode(target)==System.identityHashCode(platformAgent)) {
				innerDomain = "DefaultDomain";
			} else {
				innerDomain = "Unknown-" + target.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(target));
			}
		}
		return innerDomain;
	}
	
	/**
	 * Acquires a wrapped MBean server for the passed actual delegate
	 * @param delegate The MBeanServer to be wrapped
	 * @return a wrapped MBean server 
	 */
	public static MBeanServer getInstance(MBeanServer delegate) {
		if(delegate==null) throw new IllegalArgumentException("Passed MBeanServer was null", new Throwable());
		return getFromCache(delegate);
	}
	
	
	/**
	 * Retrieves the MBeanServer proxy instance from cache, creating it if it is not in cache
	 * @param server The delegate MBeanServer
	 * @return a WrappedMBeanServer for the passed delegate MBeanServer  
	 */
	protected static MBeanServer getFromCache(MBeanServer server) {
		if(server==null) throw new IllegalArgumentException("Passed MBeanServer was null", new Throwable());
		Integer key = System.identityHashCode(server);
		MBeanServer wms = null;
		SoftReference<MBeanServer> ref = instances.get(key);
		if(ref!=null) {
			wms = ref.get();
		}
		if(wms==null) {
			String domain = getDomainName(server);						
			boolean syntheticDomainName = !(domain.equals(server.getDefaultDomain()));
			if(syntheticDomainName) {
				wms = new MBeanServerBuilder().newMBeanServer(domain, server, JMX.newMBeanProxy(server, AliasMBeanRegistry.MBEANSERVER_DELEGATE_OBJECTNAME, MBeanServerDelegate.class));
			} else {
				wms = new WrappedMBeanServer(server);
			}

			
			instances.put(key, new SoftReference<MBeanServer>(wms));
		}
		return wms;
	}
	
	//=======================================
	//	MBeanServer impl.
	//=======================================

	/**
	 * @param name
	 * @param listener
	 * @param filter
	 * @param handback
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#addNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException {
		inner.addNotificationListener(name, listener, filter, handback);
	}

	/**
	 * @param name
	 * @param listener
	 * @param filter
	 * @param handback
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#addNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(ObjectName name, ObjectName listener,
			NotificationFilter filter, Object handback)
			throws InstanceNotFoundException {
		inner.addNotificationListener(name, listener, filter, handback);
	}

	/**
	 * @param className
	 * @param name
	 * @param params
	 * @param signature
	 * @return
	 * @throws ReflectionException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MBeanException
	 * @throws NotCompliantMBeanException
	 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	public ObjectInstance createMBean(String className, ObjectName name,
			Object[] params, String[] signature) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException {
		return inner.createMBean(className, name, params, signature);
	}

	/**
	 * @param className
	 * @param name
	 * @param loaderName
	 * @param params
	 * @param signature
	 * @return
	 * @throws ReflectionException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MBeanException
	 * @throws NotCompliantMBeanException
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException {
		return inner
				.createMBean(className, name, loaderName, params, signature);
	}

	/**
	 * @param className
	 * @param name
	 * @param loaderName
	 * @return
	 * @throws ReflectionException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MBeanException
	 * @throws NotCompliantMBeanException
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName)
	 */
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException,
			InstanceNotFoundException {
		return inner.createMBean(className, name, loaderName);
	}

	/**
	 * @param className
	 * @param name
	 * @return
	 * @throws ReflectionException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MBeanException
	 * @throws NotCompliantMBeanException
	 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName)
	 */
	public ObjectInstance createMBean(String className, ObjectName name)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException {
		return inner.createMBean(className, name);
	}

	/**
	 * @param name
	 * @param data
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws OperationsException
	 * @deprecated
	 * @see javax.management.MBeanServer#deserialize(javax.management.ObjectName, byte[])
	 */
	public ObjectInputStream deserialize(ObjectName name, byte[] data)
			throws InstanceNotFoundException, OperationsException {
		return inner.deserialize(name, data);
	}

	/**
	 * @param className
	 * @param data
	 * @return
	 * @throws OperationsException
	 * @throws ReflectionException
	 * @deprecated
	 * @see javax.management.MBeanServer#deserialize(java.lang.String, byte[])
	 */
	public ObjectInputStream deserialize(String className, byte[] data)
			throws OperationsException, ReflectionException {
		return inner.deserialize(className, data);
	}

	/**
	 * @param className
	 * @param loaderName
	 * @param data
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws OperationsException
	 * @throws ReflectionException
	 * @deprecated
	 * @see javax.management.MBeanServer#deserialize(java.lang.String, javax.management.ObjectName, byte[])
	 */
	public ObjectInputStream deserialize(String className,
			ObjectName loaderName, byte[] data)
			throws InstanceNotFoundException, OperationsException,
			ReflectionException {
		return inner.deserialize(className, loaderName, data);
	}

	/**
	 * @param name
	 * @param attribute
	 * @return
	 * @throws MBeanException
	 * @throws AttributeNotFoundException
	 * @throws InstanceNotFoundException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#getAttribute(javax.management.ObjectName, java.lang.String)
	 */
	public Object getAttribute(ObjectName name, String attribute)
			throws MBeanException, AttributeNotFoundException,
			InstanceNotFoundException, ReflectionException {
		return inner.getAttribute(name, attribute);
	}

	/**
	 * @param name
	 * @param attributes
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#getAttributes(javax.management.ObjectName, java.lang.String[])
	 */
	public AttributeList getAttributes(ObjectName name, String[] attributes)
			throws InstanceNotFoundException, ReflectionException {
		return inner.getAttributes(name, attributes);
	}

	/**
	 * @param loaderName
	 * @return
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#getClassLoader(javax.management.ObjectName)
	 */
	public ClassLoader getClassLoader(ObjectName loaderName)
			throws InstanceNotFoundException {
		return inner.getClassLoader(loaderName);
	}

	/**
	 * @param mbeanName
	 * @return
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#getClassLoaderFor(javax.management.ObjectName)
	 */
	public ClassLoader getClassLoaderFor(ObjectName mbeanName)
			throws InstanceNotFoundException {
		return inner.getClassLoaderFor(mbeanName);
	}

	/**
	 * @return
	 * @see javax.management.MBeanServer#getClassLoaderRepository()
	 */
	public ClassLoaderRepository getClassLoaderRepository() {
		return inner.getClassLoaderRepository();
	}

	/**
	 * @return
	 * @see javax.management.MBeanServer#getDefaultDomain()
	 */
	public String getDefaultDomain() {
		return domain;
	}

	/**
	 * @return
	 * @see javax.management.MBeanServer#getDomains()
	 */
	public String[] getDomains() {
		return inner.getDomains();
	}

	/**
	 * @return
	 * @see javax.management.MBeanServer#getMBeanCount()
	 */
	public Integer getMBeanCount() {
		return inner.getMBeanCount();
	}

	/**
	 * @param name
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws IntrospectionException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#getMBeanInfo(javax.management.ObjectName)
	 */
	public MBeanInfo getMBeanInfo(ObjectName name)
			throws InstanceNotFoundException, IntrospectionException,
			ReflectionException {
		return inner.getMBeanInfo(name);
	}

	/**
	 * @param name
	 * @return
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#getObjectInstance(javax.management.ObjectName)
	 */
	public ObjectInstance getObjectInstance(ObjectName name)
			throws InstanceNotFoundException {
		return inner.getObjectInstance(name);
	}

	/**
	 * @param className
	 * @param params
	 * @param signature
	 * @return
	 * @throws ReflectionException
	 * @throws MBeanException
	 * @see javax.management.MBeanServer#instantiate(java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	public Object instantiate(String className, Object[] params,
			String[] signature) throws ReflectionException, MBeanException {
		return inner.instantiate(className, params, signature);
	}

	/**
	 * @param className
	 * @param loaderName
	 * @param params
	 * @param signature
	 * @return
	 * @throws ReflectionException
	 * @throws MBeanException
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#instantiate(java.lang.String, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	public Object instantiate(String className, ObjectName loaderName,
			Object[] params, String[] signature) throws ReflectionException,
			MBeanException, InstanceNotFoundException {
		return inner.instantiate(className, loaderName, params, signature);
	}

	/**
	 * @param className
	 * @param loaderName
	 * @return
	 * @throws ReflectionException
	 * @throws MBeanException
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#instantiate(java.lang.String, javax.management.ObjectName)
	 */
	public Object instantiate(String className, ObjectName loaderName)
			throws ReflectionException, MBeanException,
			InstanceNotFoundException {
		return inner.instantiate(className, loaderName);
	}

	/**
	 * @param className
	 * @return
	 * @throws ReflectionException
	 * @throws MBeanException
	 * @see javax.management.MBeanServer#instantiate(java.lang.String)
	 */
	public Object instantiate(String className) throws ReflectionException,
			MBeanException {
		return inner.instantiate(className);
	}

	/**
	 * @param name
	 * @param operationName
	 * @param params
	 * @param signature
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws MBeanException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#invoke(javax.management.ObjectName, java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	public Object invoke(ObjectName name, String operationName,
			Object[] params, String[] signature)
			throws InstanceNotFoundException, MBeanException,
			ReflectionException {
		return inner.invoke(name, operationName, params, signature);
	}

	/**
	 * @param name
	 * @param className
	 * @return
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#isInstanceOf(javax.management.ObjectName, java.lang.String)
	 */
	public boolean isInstanceOf(ObjectName name, String className)
			throws InstanceNotFoundException {
		return inner.isInstanceOf(name, className);
	}

	/**
	 * @param name
	 * @return
	 * @see javax.management.MBeanServer#isRegistered(javax.management.ObjectName)
	 */
	public boolean isRegistered(ObjectName name) {
		return inner.isRegistered(name);
	}

	/**
	 * @param name
	 * @param query
	 * @return
	 * @see javax.management.MBeanServer#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)
	 */
	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
		return inner.queryMBeans(name, query);
	}

	/**
	 * @param name
	 * @param query
	 * @return
	 * @see javax.management.MBeanServer#queryNames(javax.management.ObjectName, javax.management.QueryExp)
	 */
	public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
		return inner.queryNames(name, query);
	}

	/**
	 * @param object
	 * @param name
	 * @return
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws NotCompliantMBeanException
	 * @see javax.management.MBeanServer#registerMBean(java.lang.Object, javax.management.ObjectName)
	 */
	public ObjectInstance registerMBean(Object object, ObjectName name)
			throws InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException {
		return inner.registerMBean(object, name);
	}

	/**
	 * @param name
	 * @param listener
	 * @param filter
	 * @param handback
	 * @throws InstanceNotFoundException
	 * @throws ListenerNotFoundException
	 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException,
			ListenerNotFoundException {
		inner.removeNotificationListener(name, listener, filter, handback);
	}

	/**
	 * @param name
	 * @param listener
	 * @throws InstanceNotFoundException
	 * @throws ListenerNotFoundException
	 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener)
	 */
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener) throws InstanceNotFoundException,
			ListenerNotFoundException {
		inner.removeNotificationListener(name, listener);
	}

	/**
	 * @param name
	 * @param listener
	 * @param filter
	 * @param handback
	 * @throws InstanceNotFoundException
	 * @throws ListenerNotFoundException
	 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(ObjectName name,
			ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException {
		inner.removeNotificationListener(name, listener, filter, handback);
	}

	/**
	 * @param name
	 * @param listener
	 * @throws InstanceNotFoundException
	 * @throws ListenerNotFoundException
	 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName)
	 */
	public void removeNotificationListener(ObjectName name, ObjectName listener)
			throws InstanceNotFoundException, ListenerNotFoundException {
		inner.removeNotificationListener(name, listener);
	}

	/**
	 * @param name
	 * @param attribute
	 * @throws InstanceNotFoundException
	 * @throws AttributeNotFoundException
	 * @throws InvalidAttributeValueException
	 * @throws MBeanException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#setAttribute(javax.management.ObjectName, javax.management.Attribute)
	 */
	public void setAttribute(ObjectName name, Attribute attribute)
			throws InstanceNotFoundException, AttributeNotFoundException,
			InvalidAttributeValueException, MBeanException, ReflectionException {
		inner.setAttribute(name, attribute);
	}

	/**
	 * @param name
	 * @param attributes
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#setAttributes(javax.management.ObjectName, javax.management.AttributeList)
	 */
	public AttributeList setAttributes(ObjectName name, AttributeList attributes)
			throws InstanceNotFoundException, ReflectionException {
		return inner.setAttributes(name, attributes);
	}

	/**
	 * @param name
	 * @throws InstanceNotFoundException
	 * @throws MBeanRegistrationException
	 * @see javax.management.MBeanServer#unregisterMBean(javax.management.ObjectName)
	 */
	public void unregisterMBean(ObjectName name)
			throws InstanceNotFoundException, MBeanRegistrationException {
		inner.unregisterMBean(name);
	}

	
}
