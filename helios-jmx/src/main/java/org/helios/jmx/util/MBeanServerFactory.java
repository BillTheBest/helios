package org.helios.jmx.util;


import java.io.ObjectInputStream;
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
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.MBeanServerForwarder;

import org.helios.helpers.JMXHelperExtended;

/**
 * <p>Title: MBeanServerFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class MBeanServerFactory {
	public static MBeanServer createMBeanServer(String domain) {
		MBeanServer server = JMXHelperExtended.getLocalMBeanServer(domain, true);
		if(server==null) {
			server = javax.management.MBeanServerFactory.createMBeanServer(domain);			
		}
		return server;
	}
	
	public static MBeanServer createMBeanServerForwarder(final String domain, final MBeanServer server) {
		return new MBeanServerForwarder(){
			private MBeanServer innerServer = server;

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @param arg3
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#addNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
			 */
			public void addNotificationListener(ObjectName arg0,
					NotificationListener arg1, NotificationFilter arg2,
					Object arg3) throws InstanceNotFoundException {
				innerServer.addNotificationListener(arg0, arg1, arg2, arg3);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @param arg3
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#addNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
			 */
			public void addNotificationListener(ObjectName arg0,
					ObjectName arg1, NotificationFilter arg2, Object arg3)
					throws InstanceNotFoundException {
				innerServer.addNotificationListener(arg0, arg1, arg2, arg3);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @param arg3
			 * @return
			 * @throws ReflectionException
			 * @throws InstanceAlreadyExistsException
			 * @throws MBeanRegistrationException
			 * @throws MBeanException
			 * @throws NotCompliantMBeanException
			 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
			 */
			public ObjectInstance createMBean(String arg0, ObjectName arg1,
					Object[] arg2, String[] arg3) throws ReflectionException,
					InstanceAlreadyExistsException, MBeanRegistrationException,
					MBeanException, NotCompliantMBeanException {
				return innerServer.createMBean(arg0, arg1, arg2, arg3);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @param arg3
			 * @param arg4
			 * @return
			 * @throws ReflectionException
			 * @throws InstanceAlreadyExistsException
			 * @throws MBeanRegistrationException
			 * @throws MBeanException
			 * @throws NotCompliantMBeanException
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
			 */
			public ObjectInstance createMBean(String arg0, ObjectName arg1,
					ObjectName arg2, Object[] arg3, String[] arg4)
					throws ReflectionException, InstanceAlreadyExistsException,
					MBeanRegistrationException, MBeanException,
					NotCompliantMBeanException, InstanceNotFoundException {
				return innerServer.createMBean(arg0, arg1, arg2, arg3, arg4);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @return
			 * @throws ReflectionException
			 * @throws InstanceAlreadyExistsException
			 * @throws MBeanRegistrationException
			 * @throws MBeanException
			 * @throws NotCompliantMBeanException
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName)
			 */
			public ObjectInstance createMBean(String arg0, ObjectName arg1,
					ObjectName arg2) throws ReflectionException,
					InstanceAlreadyExistsException, MBeanRegistrationException,
					MBeanException, NotCompliantMBeanException,
					InstanceNotFoundException {
				return innerServer.createMBean(arg0, arg1, arg2);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @throws ReflectionException
			 * @throws InstanceAlreadyExistsException
			 * @throws MBeanRegistrationException
			 * @throws MBeanException
			 * @throws NotCompliantMBeanException
			 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName)
			 */
			public ObjectInstance createMBean(String arg0, ObjectName arg1)
					throws ReflectionException, InstanceAlreadyExistsException,
					MBeanRegistrationException, MBeanException,
					NotCompliantMBeanException {
				return innerServer.createMBean(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @throws InstanceNotFoundException
			 * @throws OperationsException
			 * @deprecated
			 * @see javax.management.MBeanServer#deserialize(javax.management.ObjectName, byte[])
			 */
			public ObjectInputStream deserialize(ObjectName arg0, byte[] arg1)
					throws InstanceNotFoundException, OperationsException {
				return innerServer.deserialize(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @throws OperationsException
			 * @throws ReflectionException
			 * @deprecated
			 * @see javax.management.MBeanServer#deserialize(java.lang.String, byte[])
			 */
			public ObjectInputStream deserialize(String arg0, byte[] arg1)
					throws OperationsException, ReflectionException {
				return innerServer.deserialize(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @return
			 * @throws InstanceNotFoundException
			 * @throws OperationsException
			 * @throws ReflectionException
			 * @deprecated
			 * @see javax.management.MBeanServer#deserialize(java.lang.String, javax.management.ObjectName, byte[])
			 */
			public ObjectInputStream deserialize(String arg0, ObjectName arg1,
					byte[] arg2) throws InstanceNotFoundException,
					OperationsException, ReflectionException {
				return innerServer.deserialize(arg0, arg1, arg2);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @throws MBeanException
			 * @throws AttributeNotFoundException
			 * @throws InstanceNotFoundException
			 * @throws ReflectionException
			 * @see javax.management.MBeanServer#getAttribute(javax.management.ObjectName, java.lang.String)
			 */
			public Object getAttribute(ObjectName arg0, String arg1)
					throws MBeanException, AttributeNotFoundException,
					InstanceNotFoundException, ReflectionException {
				return innerServer.getAttribute(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @throws InstanceNotFoundException
			 * @throws ReflectionException
			 * @see javax.management.MBeanServer#getAttributes(javax.management.ObjectName, java.lang.String[])
			 */
			public AttributeList getAttributes(ObjectName arg0, String[] arg1)
					throws InstanceNotFoundException, ReflectionException {
				return innerServer.getAttributes(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @return
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#getClassLoader(javax.management.ObjectName)
			 */
			public ClassLoader getClassLoader(ObjectName arg0)
					throws InstanceNotFoundException {
				return innerServer.getClassLoader(arg0);
			}

			/**
			 * @param arg0
			 * @return
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#getClassLoaderFor(javax.management.ObjectName)
			 */
			public ClassLoader getClassLoaderFor(ObjectName arg0)
					throws InstanceNotFoundException {
				return innerServer.getClassLoaderFor(arg0);
			}

			/**
			 * @return
			 * @see javax.management.MBeanServer#getClassLoaderRepository()
			 */
			public ClassLoaderRepository getClassLoaderRepository() {
				return innerServer.getClassLoaderRepository();
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
				return innerServer.getDomains();
			}

			/**
			 * @return
			 * @see javax.management.MBeanServer#getMBeanCount()
			 */
			public Integer getMBeanCount() {
				return innerServer.getMBeanCount();
			}

			/**
			 * @param arg0
			 * @return
			 * @throws InstanceNotFoundException
			 * @throws IntrospectionException
			 * @throws ReflectionException
			 * @see javax.management.MBeanServer#getMBeanInfo(javax.management.ObjectName)
			 */
			public MBeanInfo getMBeanInfo(ObjectName arg0)
					throws InstanceNotFoundException, IntrospectionException,
					ReflectionException {
				return innerServer.getMBeanInfo(arg0);
			}

			/**
			 * @param arg0
			 * @return
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#getObjectInstance(javax.management.ObjectName)
			 */
			public ObjectInstance getObjectInstance(ObjectName arg0)
					throws InstanceNotFoundException {
				return innerServer.getObjectInstance(arg0);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @return
			 * @throws ReflectionException
			 * @throws MBeanException
			 * @see javax.management.MBeanServer#instantiate(java.lang.String, java.lang.Object[], java.lang.String[])
			 */
			public Object instantiate(String arg0, Object[] arg1, String[] arg2)
					throws ReflectionException, MBeanException {
				return innerServer.instantiate(arg0, arg1, arg2);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @param arg3
			 * @return
			 * @throws ReflectionException
			 * @throws MBeanException
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#instantiate(java.lang.String, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
			 */
			public Object instantiate(String arg0, ObjectName arg1,
					Object[] arg2, String[] arg3) throws ReflectionException,
					MBeanException, InstanceNotFoundException {
				return innerServer.instantiate(arg0, arg1, arg2, arg3);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @throws ReflectionException
			 * @throws MBeanException
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#instantiate(java.lang.String, javax.management.ObjectName)
			 */
			public Object instantiate(String arg0, ObjectName arg1)
					throws ReflectionException, MBeanException,
					InstanceNotFoundException {
				return innerServer.instantiate(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @return
			 * @throws ReflectionException
			 * @throws MBeanException
			 * @see javax.management.MBeanServer#instantiate(java.lang.String)
			 */
			public Object instantiate(String arg0) throws ReflectionException,
					MBeanException {
				return innerServer.instantiate(arg0);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @param arg3
			 * @return
			 * @throws InstanceNotFoundException
			 * @throws MBeanException
			 * @throws ReflectionException
			 * @see javax.management.MBeanServer#invoke(javax.management.ObjectName, java.lang.String, java.lang.Object[], java.lang.String[])
			 */
			public Object invoke(ObjectName arg0, String arg1, Object[] arg2,
					String[] arg3) throws InstanceNotFoundException,
					MBeanException, ReflectionException {
				return innerServer.invoke(arg0, arg1, arg2, arg3);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @throws InstanceNotFoundException
			 * @see javax.management.MBeanServer#isInstanceOf(javax.management.ObjectName, java.lang.String)
			 */
			public boolean isInstanceOf(ObjectName arg0, String arg1)
					throws InstanceNotFoundException {
				return innerServer.isInstanceOf(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @return
			 * @see javax.management.MBeanServer#isRegistered(javax.management.ObjectName)
			 */
			public boolean isRegistered(ObjectName arg0) {
				return innerServer.isRegistered(arg0);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @see javax.management.MBeanServer#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)
			 */
			public Set queryMBeans(ObjectName arg0, QueryExp arg1) {
				return innerServer.queryMBeans(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @see javax.management.MBeanServer#queryNames(javax.management.ObjectName, javax.management.QueryExp)
			 */
			public Set queryNames(ObjectName arg0, QueryExp arg1) {
				return innerServer.queryNames(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @throws InstanceAlreadyExistsException
			 * @throws MBeanRegistrationException
			 * @throws NotCompliantMBeanException
			 * @see javax.management.MBeanServer#registerMBean(java.lang.Object, javax.management.ObjectName)
			 */
			public ObjectInstance registerMBean(Object arg0, ObjectName arg1)
					throws InstanceAlreadyExistsException,
					MBeanRegistrationException, NotCompliantMBeanException {
				return innerServer.registerMBean(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @param arg3
			 * @throws InstanceNotFoundException
			 * @throws ListenerNotFoundException
			 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
			 */
			public void removeNotificationListener(ObjectName arg0,
					NotificationListener arg1, NotificationFilter arg2,
					Object arg3) throws InstanceNotFoundException,
					ListenerNotFoundException {
				innerServer.removeNotificationListener(arg0, arg1, arg2, arg3);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @throws InstanceNotFoundException
			 * @throws ListenerNotFoundException
			 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener)
			 */
			public void removeNotificationListener(ObjectName arg0,
					NotificationListener arg1)
					throws InstanceNotFoundException, ListenerNotFoundException {
				innerServer.removeNotificationListener(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @param arg2
			 * @param arg3
			 * @throws InstanceNotFoundException
			 * @throws ListenerNotFoundException
			 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
			 */
			public void removeNotificationListener(ObjectName arg0,
					ObjectName arg1, NotificationFilter arg2, Object arg3)
					throws InstanceNotFoundException, ListenerNotFoundException {
				innerServer.removeNotificationListener(arg0, arg1, arg2, arg3);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @throws InstanceNotFoundException
			 * @throws ListenerNotFoundException
			 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName)
			 */
			public void removeNotificationListener(ObjectName arg0,
					ObjectName arg1) throws InstanceNotFoundException,
					ListenerNotFoundException {
				innerServer.removeNotificationListener(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @throws InstanceNotFoundException
			 * @throws AttributeNotFoundException
			 * @throws InvalidAttributeValueException
			 * @throws MBeanException
			 * @throws ReflectionException
			 * @see javax.management.MBeanServer#setAttribute(javax.management.ObjectName, javax.management.Attribute)
			 */
			public void setAttribute(ObjectName arg0, Attribute arg1)
					throws InstanceNotFoundException,
					AttributeNotFoundException, InvalidAttributeValueException,
					MBeanException, ReflectionException {
				innerServer.setAttribute(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @param arg1
			 * @return
			 * @throws InstanceNotFoundException
			 * @throws ReflectionException
			 * @see javax.management.MBeanServer#setAttributes(javax.management.ObjectName, javax.management.AttributeList)
			 */
			public AttributeList setAttributes(ObjectName arg0,
					AttributeList arg1) throws InstanceNotFoundException,
					ReflectionException {
				return innerServer.setAttributes(arg0, arg1);
			}

			/**
			 * @param arg0
			 * @throws InstanceNotFoundException
			 * @throws MBeanRegistrationException
			 * @see javax.management.MBeanServer#unregisterMBean(javax.management.ObjectName)
			 */
			public void unregisterMBean(ObjectName arg0)
					throws InstanceNotFoundException,
					MBeanRegistrationException {
				innerServer.unregisterMBean(arg0);
			}

			@Override
			public MBeanServer getMBeanServer() {
				return innerServer;
			}

			@Override
			public void setMBeanServer(MBeanServer mbs) {
				// NOOP				
			}
		};
	}
	
}
