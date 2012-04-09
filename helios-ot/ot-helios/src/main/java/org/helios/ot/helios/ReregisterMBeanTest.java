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
package org.helios.ot.helios;

import java.io.ObjectInputStream;
import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.StandardMBean;
import javax.management.loading.ClassLoaderRepository;

import sun.management.HotspotInternal;
import sun.management.HotspotInternalMBean;

/**
 * <p>Title: ReregisterMBeanTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.ReregisterMBeanTest</code></p>
 */
public class ReregisterMBeanTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			AltObjectNameMBeanServer rr = new AltObjectNameMBeanServer(server, new ObjectName("*:*"));			
			Class clazz = Class.forName("sun.management.HotspotInternal");
			HotspotInternalMBean obj = (HotspotInternal)clazz.newInstance();
			ObjectInstance oi = rr.registerMBean(new StandardMBean(obj, HotspotInternalMBean.class), new javax.management.ObjectName("sun.management:type=HotspotInternal"));
			System.out.println("ObjectName:" + oi.getObjectName());
			//Thread.currentThread().join();  // To give us time to look at jconsole
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}
	
	private static class AltObjectNameMBeanServer implements MBeanServer {
		protected final MBeanServer innerServer;
		protected final ObjectName filter;
		
		public AltObjectNameMBeanServer(MBeanServer innerServer, ObjectName filter) {
			this.innerServer = innerServer;
			this.filter = filter;
		}
		
		public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException,
				MBeanRegistrationException, NotCompliantMBeanException {
			if(filter.apply(name)) {
				name = reformat(name);			
			}
			return innerServer.registerMBean(object, name);
		}

		/**
		 * @param name
		 * @throws InstanceNotFoundException
		 * @throws MBeanRegistrationException
		 * @see javax.management.MBeanServer#unregisterMBean(javax.management.ObjectName)
		 */
		public void unregisterMBean(ObjectName name)
				throws InstanceNotFoundException, MBeanRegistrationException {
			if(innerServer.isRegistered(name)) {
				innerServer.unregisterMBean(name);
			} else {
				innerServer.unregisterMBean(reformat(name));
			}
		}

		
		public static ObjectName reformat(ObjectName on) {
			try {
				int id = on.toString().hashCode();
				return new ObjectName(new StringBuilder(on.toString()).append(",serial=").append(id).toString());
				
			} catch (Exception e) {
				throw new RuntimeException("Failed to reformat [" + on + "]", e);
			}
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
			return innerServer.createMBean(className, name);
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
			return innerServer.createMBean(className, name, loaderName);
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
				Object[] params, String[] signature)
				throws ReflectionException, InstanceAlreadyExistsException,
				MBeanRegistrationException, MBeanException,
				NotCompliantMBeanException {
			return innerServer.createMBean(className, name, params, signature);
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
			return innerServer.createMBean(className, name, loaderName, params,
					signature);
		}


		/**
		 * @param name
		 * @return
		 * @throws InstanceNotFoundException
		 * @see javax.management.MBeanServer#getObjectInstance(javax.management.ObjectName)
		 */
		public ObjectInstance getObjectInstance(ObjectName name)
				throws InstanceNotFoundException {
			return innerServer.getObjectInstance(name);
		}

		/**
		 * @param name
		 * @param query
		 * @return
		 * @see javax.management.MBeanServer#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)
		 */
		public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
			return innerServer.queryMBeans(name, query);
		}

		/**
		 * @param name
		 * @param query
		 * @return
		 * @see javax.management.MBeanServer#queryNames(javax.management.ObjectName, javax.management.QueryExp)
		 */
		public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
			return innerServer.queryNames(name, query);
		}

		/**
		 * @param name
		 * @return
		 * @see javax.management.MBeanServer#isRegistered(javax.management.ObjectName)
		 */
		public boolean isRegistered(ObjectName name) {
			return innerServer.isRegistered(name);
		}

		/**
		 * @return
		 * @see javax.management.MBeanServer#getMBeanCount()
		 */
		public Integer getMBeanCount() {
			return innerServer.getMBeanCount();
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
			return innerServer.getAttribute(name, attribute);
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
			return innerServer.getAttributes(name, attributes);
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
				InvalidAttributeValueException, MBeanException,
				ReflectionException {
			innerServer.setAttribute(name, attribute);
		}

		/**
		 * @param name
		 * @param attributes
		 * @return
		 * @throws InstanceNotFoundException
		 * @throws ReflectionException
		 * @see javax.management.MBeanServer#setAttributes(javax.management.ObjectName, javax.management.AttributeList)
		 */
		public AttributeList setAttributes(ObjectName name,
				AttributeList attributes) throws InstanceNotFoundException,
				ReflectionException {
			return innerServer.setAttributes(name, attributes);
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
			return innerServer.invoke(name, operationName, params, signature);
		}

		/**
		 * @return
		 * @see javax.management.MBeanServer#getDefaultDomain()
		 */
		public String getDefaultDomain() {
			return innerServer.getDefaultDomain();
		}

		/**
		 * @return
		 * @see javax.management.MBeanServer#getDomains()
		 */
		public String[] getDomains() {
			return innerServer.getDomains();
		}

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
			innerServer.addNotificationListener(name, listener, filter,
					handback);
		}

		/**
		 * @param name
		 * @param listener
		 * @param filter
		 * @param handback
		 * @throws InstanceNotFoundException
		 * @see javax.management.MBeanServer#addNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
		 */
		public void addNotificationListener(ObjectName name,
				ObjectName listener, NotificationFilter filter, Object handback)
				throws InstanceNotFoundException {
			innerServer.addNotificationListener(name, listener, filter,
					handback);
		}

		/**
		 * @param name
		 * @param listener
		 * @throws InstanceNotFoundException
		 * @throws ListenerNotFoundException
		 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName)
		 */
		public void removeNotificationListener(ObjectName name,
				ObjectName listener) throws InstanceNotFoundException,
				ListenerNotFoundException {
			innerServer.removeNotificationListener(name, listener);
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
			innerServer.removeNotificationListener(name, listener, filter,
					handback);
		}

		/**
		 * @param name
		 * @param listener
		 * @throws InstanceNotFoundException
		 * @throws ListenerNotFoundException
		 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener)
		 */
		public void removeNotificationListener(ObjectName name,
				NotificationListener listener)
				throws InstanceNotFoundException, ListenerNotFoundException {
			innerServer.removeNotificationListener(name, listener);
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
			innerServer.removeNotificationListener(name, listener, filter,
					handback);
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
			return innerServer.getMBeanInfo(name);
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
			return innerServer.isInstanceOf(name, className);
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
			return innerServer.instantiate(className);
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
			return innerServer.instantiate(className, loaderName);
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
			return innerServer.instantiate(className, params, signature);
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
				Object[] params, String[] signature)
				throws ReflectionException, MBeanException,
				InstanceNotFoundException {
			return innerServer.instantiate(className, loaderName, params,
					signature);
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
			return innerServer.deserialize(name, data);
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
			return innerServer.deserialize(className, data);
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
			return innerServer.deserialize(className, loaderName, data);
		}

		/**
		 * @param mbeanName
		 * @return
		 * @throws InstanceNotFoundException
		 * @see javax.management.MBeanServer#getClassLoaderFor(javax.management.ObjectName)
		 */
		public ClassLoader getClassLoaderFor(ObjectName mbeanName)
				throws InstanceNotFoundException {
			return innerServer.getClassLoaderFor(mbeanName);
		}

		/**
		 * @param loaderName
		 * @return
		 * @throws InstanceNotFoundException
		 * @see javax.management.MBeanServer#getClassLoader(javax.management.ObjectName)
		 */
		public ClassLoader getClassLoader(ObjectName loaderName)
				throws InstanceNotFoundException {
			return innerServer.getClassLoader(loaderName);
		}

		/**
		 * @return
		 * @see javax.management.MBeanServer#getClassLoaderRepository()
		 */
		public ClassLoaderRepository getClassLoaderRepository() {
			return innerServer.getClassLoaderRepository();
		}


	}
	

}
