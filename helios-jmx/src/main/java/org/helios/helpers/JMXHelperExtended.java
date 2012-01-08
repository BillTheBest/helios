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
package org.helios.helpers;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.BeanHelper;

/**
 * <p>Title: JMXHelperExtended</p>
 * <p>Description: Some extended JMX helper utilities not available in HeliosUtils. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public class JMXHelperExtended {
	
	/** The property name where the jmx default domain is referenced */
	public static final String JMX_DOMAIN_PROPERTY = "helios.opentrace.config.jmx.domain";
	/** The default jmx default domain is referenced */
	public static final String JMX_DOMAIN_DEFAULT = "DefaultDomain";
	
	/**
	 * Acquires the configured or default Helios target MBeanServer.
	 * @return An MBeanServer.
	 */
	public static MBeanServer getHeliosMBeanServer() {
		MBeanServer server = null;
		String jmxDomain = ConfigurationHelper.getEnvThenSystemProperty(JMX_DOMAIN_PROPERTY, JMX_DOMAIN_DEFAULT);
		server = getLocalMBeanServer(jmxDomain, true);
		if(server==null) {
			return ManagementFactory.getPlatformMBeanServer();
		}
		
		return server;
	}
	
	/**
	 * Returns an MBeanConnection for an in-vm MBeanServer that has the specified default domain.
	 * @param domain The default domain of the requested MBeanServer.
	 * @return The located MBeanServerConnection or null if one cannot be located. 
	 */
	public static MBeanServer getLocalMBeanServer(String domain) {
		return getLocalMBeanServer(domain, true);
	}
	
	/**
	 * Searches for a matching MBeanServer in the passed list of domains and returns the first located.
	 * If one cannot be located a null will be returned. 
	 * @param domains The default domain of the requested MBeanServer.
	 * @return The located MBeanServerConnection or null if one cannot be found.
	 */
	public static MBeanServer getLocalMBeanServer(String...domains) {
		return getLocalMBeanServer(true, domains);
	}
	
	/**
	 * Searches for a matching MBeanServer in the passed list of domains and returns the first located.
	 * If one cannot be located, returnNullIfNotFound will either cause a null to be returned, or a RuntimeException. 
	 * @param returnNullIfNotFound If true, returns a null if a matching MBeanServer cannot be found. Otherwise, throws a RuntimeException.
	 * @param domains The default domain of the requested MBeanServer.
	 * @return The located MBeanServerConnection or null if one cannot be found and returnNullIfNotFound is true.
	 */
	public static MBeanServer getLocalMBeanServer(boolean returnNullIfNotFound, String...domains) {
		MBeanServer server = null;
		StringBuilder buff = new StringBuilder();
		for(String domain: domains) {
			server = getLocalMBeanServer(domain);
			buff.append(domain).append(",");
			if(server!=null) return server;
		}
		if(returnNullIfNotFound) {
			return null;
		} else {
			throw new RuntimeException("No MBeanServer located for domains [" + buff.toString() + "]"); 
		}
	}
	
	
	/**
	 * Returns an MBeanConnection for an in-vm MBeanServer that has the specified default domain.
	 * @param domain The default domain of the requested MBeanServer.
	 * @param returnNullIfNotFound If true, returns a null if a matching MBeanServer cannot be found. Otherwise, throws a RuntimeException. 
	 * @return The located MBeanServerConnection or null if one cannot be found and returnNullIfNotFound is true. 
	 */
	@SuppressWarnings("unchecked")
	public static MBeanServer getLocalMBeanServer(String domain, boolean returnNullIfNotFound) {
		if(domain==null || domain.equals("") || domain.equalsIgnoreCase("DefaultDomain") || domain.equalsIgnoreCase("Default")) {
			return ManagementFactory.getPlatformMBeanServer();
		}
		List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		for(MBeanServer server: servers) {
			if(server.getDefaultDomain().equals(domain)) return server;
		}
		throw new RuntimeException("No MBeanServer located for domain [" + domain + "]");
	}
	
	/**
	 * Creates a new JMX object name.
	 * @param on
	 * @return an ObjectName
	 */
	public static ObjectName objectName(String on) {
		try {
			return new ObjectName(on);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create Object Name", e);
		}
	}
	
	/**
	 * Retrieves MBeanInfo on the specified object name.
	 * @param server
	 * @param on
	 * @return an MBeanInfo
	 */
	public static MBeanInfo mbeanInfo(MBeanServerConnection server, ObjectName on) {
		try {
			return server.getMBeanInfo(on);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get MBeanInfo", e);
		}		
	}
	
	/**
	 * Sets an MBean attribute.
	 * @param on
	 * @param server
	 * @param name
	 * @param value
	 */
	public static void setAttribute(ObjectName on, MBeanServerConnection server, String name, Object value) {
		try {
			server.setAttribute(on, new Attribute(name, value));
		} catch (Exception e) {
			throw new RuntimeException("Failed to set Attribute", e);
		}				
	}
	
	
	/**
	 * Sets a list of MBean attributes. Throws no exceptions. Returns a list of successfully set values.
	 * @param on
	 * @param server
	 * @param attributes
	 * @return
	 */
	public static Map<String, Object> setAttributesWithRet(ObjectName on, MBeanServerConnection server, Object...attributes) {
		Map<String, Object> returnValues = new HashMap<String, Object>(attributes.length);		
		Collection<NVP> list = NVP.generate(attributes);
		for(NVP nvp: list) {
			try {
				setAttribute(on, server, nvp.getName(), nvp.getValue());
				returnValues.put(nvp.getName(), nvp.getValue());
			} catch (Exception e) {}
		}
		return returnValues;
	}
	
	/**
	 * Returns a String->Object Map of the named attributes from the Mbean.
	 * @param on The object name of the MBean.
	 * @param server The MBeanServerConnection the MBean is registered in.
	 * @param attributes An array of attribute names to retrieve.
	 * @return A name value map of the requested attributes.
	 */
	public static Map<String, Object> getAttributes(ObjectName on, MBeanServerConnection server, String...attributes) {
		try {
			Map<String, Object> attrs = new HashMap<String, Object>(attributes.length);
			AttributeList attributeList = server.getAttributes(on, attributes);
			
			
			for(int i = 0; i < attributeList.size(); i++) {
				Attribute at = (Attribute)attributeList.get(i);
				if(isIn(at.getName(), attributes)) {
					attrs.put(at.getName(), at.getValue());
				}
			}
			return attrs;
		} catch (Exception e) {
			throw new RuntimeException("Failed to getAttributes on [" + on + "]", e);
		}
	}
	
	/**
	 * Inspects the array to see if it contains the passed string.
	 * @param name
	 * @param array
	 * @return true if the array contains the passed string.
	 */
	public static boolean isIn(String name, String[] array) {
		if(array==null || name==null) return false;
		for(String s: array) {
			if(s.equals(name)) return true;
		}
		return false;
		
	}
	
	
	/**
	 * Sets a list of MBean attributes. Throws an exception on any failure. Returns a list of successfully set values.
	 * @param on
	 * @param server
	 * @param attributes
	 * @return
	 */
	public static Map<String, Object> setAttributes(ObjectName on, MBeanServerConnection server, Object...attributes) {
		Map<String, Object> returnValues = new HashMap<String, Object>(attributes.length);		
		Collection<NVP> list = NVP.generate(attributes);
		for(NVP nvp: list) {
			setAttribute(on, server, nvp.getName(), nvp.getValue());
			returnValues.put(nvp.getName(), nvp.getValue());
		}
		return returnValues;
	}
	
	/**
	 * Gets an attribute value from an mbean.
	 * @param on
	 * @param server
	 * @param name
	 * @return
	 */
	public static Object getAttribute(ObjectName on, MBeanServerConnection server, String name) {
		try {
			return server.getAttribute(on,name);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get attribute", e);
		}
	}
	
	/**
	 * Invokes an operation on the mbean.
	 * @param on
	 * @param server
	 * @param action
	 * @param args
	 * @param signature
	 * @return
	 */
	public static Object invoke(ObjectName on, MBeanServerConnection server, String action, Object[] args, String[] signature) {
		try {
			return server.invoke(on, action, args, signature);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke operation", e);
		}
	}
	
	/**
	 * Inspects the attributes of the pojo and returns a map of booleans indicating if the attribute getter is an <code>is</code> method.
	 * The map key is the attribute name.
	 * The map value is a Boolean. It is true if the getter is an <code>is</code> and false if there is not getter or it is not an <code>is</code> method. 
	 * @param pojo
	 * @return An <code>is</code> map for the pojo attributes.
	 */
	public static Map<String, Boolean> getAttributeIs(Object pojo) {
		Map<String, Boolean> isms = new HashMap<String, Boolean>();
		for(PropertyDescriptor pd: getPropertyDescriptors(pojo)) {
			if(pd.getReadMethod()!=null) {
				if(pd.getReadMethod().getName().startsWith("is")) {
					isms.put(getAttributeName(pd), Boolean.TRUE);
				} else {
					isms.put(getAttributeName(pd), Boolean.FALSE);
				}
			} else {
				isms.put(getAttributeName(pd), Boolean.FALSE);
			}
		}
		return isms;
	}
	
	/**
	 * Returns the name of the attribute for the passed property descriptor.
	 * Both the getter and setter methods are checked for a <code>JMXAttribute</code> annotation in that order.
	 * If one is found, that name is returned. Otherwise the property descriptor name is returned.
	 * @param pd The property descriptor
	 * @return The attribute name.
	 */
	public static String getAttributeName(PropertyDescriptor pd) {
		Method getMethod = pd.getReadMethod();
		Method setMethod = pd.getWriteMethod();
		if(getMethod!=null && getMethod.isAnnotationPresent(JMXAttribute.class)) {
			return getMethod.getAnnotation(JMXAttribute.class).name();
		} else if(setMethod!=null && setMethod.isAnnotationPresent(JMXAttribute.class)) {
			return setMethod.getAnnotation(JMXAttribute.class).name();
		} else {
			return pd.getName();
		}
	}
	
	/**
	 * Inspects the attributes of the pojo and returns a map of booleans indicating if the attribute is read/write.
	 * The map key is the attribute name.
	 * The map value is a boolean[2] --> [0] is read, [1] is write.
	 * @param pojo
	 * @return A mutability map for the pojo attributes.
	 */
	public static Map<String, boolean[]> getAttributeMutability(Object pojo) {
		Map<String, boolean[]> mutabilities = new HashMap<String, boolean[]>();
		boolean[] mutability = null;
		String name = null;
		for(PropertyDescriptor pd: getPropertyDescriptors(pojo)) {
			mutability = new boolean[2];
			name = pd.getName();
			mutability[0] = (pd.getReadMethod()!=null);
			mutability[1] = (pd.getWriteMethod()!=null);
			mutabilities.put(getAttributeName(pd), mutability);
		}
		return mutabilities;
	}
	
	
	/**
	 * Gets a complete list of all a pojo's attribute names.
	 * Attributes with a <code>@JMXAttribute</code> annotation will be named after the annotation's specifications.
	 * @param pojo
	 * @return
	 */
	public static List<String> getPojoAttributes(Object pojo) {
		BeanInfo bi = getBeanInfo(pojo);
		List<String> list = new ArrayList<String>();		
		for(PropertyDescriptor pd : bi.getPropertyDescriptors()) {
			Method getMethod = null;
			Method setMethod = null;
			JMXAttribute getAttribute = null;
			JMXAttribute setAttribute = null;
			String getName = null;
			String setName = null;			
			getMethod = pd.getReadMethod();
			setMethod = pd.getWriteMethod();
			if(getMethod!=null) getAttribute = getMethod.getAnnotation(JMXAttribute.class);
			if(setMethod!=null) setAttribute = setMethod.getAnnotation(JMXAttribute.class);
			if(getAttribute!=null) getName=getAttribute.name();
			if(setAttribute!=null) setName=setAttribute.name();
			if(getName==null && setName==null) {
				list.add(pd.getName());
			} else {
				if(getName!=null && !list.contains(getName)) {
					list.add(getName);
				}
				if(setName!=null && !list.contains(setName)) {
					list.add(setName);
				}				
			}			
		}
		return list;
	}
	
	
	/**
	 * Generates and returns an alpha sorted String array of the pojo's attribute names.
	 * @param pojo
	 * @return
	 */
	public static String[] getSortedAttributeNames(Object pojo) {
		String[] attrs = null;		
		List<String> list = getPojoAttributes(pojo);
		List<String> objList = getPojoAttributes(new Object());
		list.removeAll(objList);
		Collections.sort(list);
		attrs = list.toArray(new String[list.size()]);
		return attrs;
	}
	
	/**
	 * Generates and returns an alpha sorted String array of the pojo's attribute descriptions.
	 * @param pojo
	 * @return
	 */
	public static String[] getSortedAttributeDescriptions(Object pojo) {
		String[] attrDescs = null;
		List<String> list = BeanHelper.getPojoBeanAttributes(pojo);		
		attrDescs = new String[list.size()];
		PropertyDescriptor pd = null;
		Class clazz = pojo.getClass();
		for(int i = 0; i < attrDescs.length; i++) {
			pd = BeanHelper.getPropertyDescriptor(clazz, list.get(i));
			attrDescs[i] = getAttributeDescription(pd);
		} 
		return (String[])sortArray(String.class, attrDescs);
	}
	
	/**
	 * Returns the attribute description for the passed property descriptor.
	 * @param pd The property descriptor.
	 * @return The description of the attribute.
	 */
	public static String getAttributeDescription(PropertyDescriptor pd) {
		Method getMethod = pd.getReadMethod();
		Method setMethod = pd.getWriteMethod();
		JMXAttribute jmxAttribute = null;
		if(getMethod!=null) {
			if(getMethod.isAnnotationPresent(JMXAttribute.class)) {
				jmxAttribute = getMethod.getAnnotation(JMXAttribute.class);
				return jmxAttribute.description();
			}			
		} 
		if(setMethod!=null) {
			if(setMethod.isAnnotationPresent(JMXAttribute.class)) {
				jmxAttribute = setMethod.getAnnotation(JMXAttribute.class);
				return jmxAttribute.description();
			}			
		} 
		return JMXAttribute.DEFAULT_DESCRIPTION;
	}
	
	
	/**
	 * Sorts an array of objects that implement the comparable interface.
	 * @param args An array of objects.
	 * @return An array of objects sorted by their natural order.
	 */
	@SuppressWarnings("unchecked")
	public static Object[] sortArray(Object...args) {
		List<Comparable> list = new ArrayList<Comparable>(args.length);
		for(Object o: args) {list.add((Comparable)o);}
		Collections.sort(list);
		return list.toArray(new Object[list.size()]);
	}
	
	
	
	
	/**
	 * Returns a list of a pojo's <code>PropertyDescriptor</code>s minus the same for <code>java.lang.Object</code>.
	 * @param pojo
	 * @return A list of <code>PropertyDescriptor</code>s.
	 */
	public static List<PropertyDescriptor> getPropertyDescriptors(Object pojo) {
		List<PropertyDescriptor> list = getAllPropertyDescriptors(pojo);
		List<PropertyDescriptor> objList = getAllPropertyDescriptors(new Object());
		list.removeAll(objList);
		return list;
	}
	
	/**
	 * Returns a list of all of a pojo's <code>PropertyDescriptor</code>s
	 * @param pojo
	 * @return A list of <code>PropertyDescriptor</code>s.
	 */
	public static List<PropertyDescriptor> getAllPropertyDescriptors(Object pojo) {
		List<PropertyDescriptor> list = new ArrayList<PropertyDescriptor>();
		BeanInfo bi = getBeanInfo(pojo);
		for(PropertyDescriptor pd : bi.getPropertyDescriptors()) {
			list.add(pd);
		}
		return list;
	}
	
	
	/**
	 * Wrapped call to <code>java.beans.Introspector</code>.
	 * Impl. may be swapped out.
	 * @param pojo The object to get the bean info for.
	 * @return A BeanInfo instance.
	 */
	public static BeanInfo getBeanInfo(Object pojo) {
		try {
			return Introspector.getBeanInfo(pojo.getClass());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create bean info", e);
		}
	}
	
	/**
	 * Reregisters mbeans from one MBeanServer to another.
	 * @param query An ObjectName mask.
	 * @param source The source MBeanServer
	 * @param target The target MBeanServer
	 * @return The number of MBeans susccessfully re-registered.
	 */
	public static int remapMBeans(ObjectName query, MBeanServer source, MBeanServer target) {
		int remaps = 0;
		Set<ObjectName> mbeans = target.queryNames(query, null);
		for(ObjectName on: mbeans) {
			try {
				Object proxy = MBeanServerInvocationHandler.newProxyInstance(source, on, DynamicMBean.class, true);
				target.registerMBean(proxy, on);
				remaps++;
			} catch (Exception e) {}
		}
		return remaps;
	}
	
	
}


	


