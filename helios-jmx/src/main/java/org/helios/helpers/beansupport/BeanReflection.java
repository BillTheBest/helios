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
package org.helios.helpers.beansupport;

import java.beans.BeanDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;


/**
 * <p>Title: BeanReflection</p>
 * <p>Description: Extended support for bean introspection.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */

public class BeanReflection {

	
	protected Class clazz = null;
	protected boolean managed = false;
	protected Method[] methods = null;
	protected JMXManagedObject jmxManagedObject = null;
	protected boolean managedDeclared = false;
	protected boolean managedAnnotated = false;
	protected BeanDescriptor beanDescriptor = null;
	protected BeanInfo beanInfo = null;
	/** All the propery descriptors keyed by bean attribute name */
	protected Map<String, PropertyDescriptor> propertyDescriptors = new HashMap<String, PropertyDescriptor>();	
	/** Maps each discovered JMXAttribute against the underlying method */
	protected Map<JMXAttribute, Method> jmxAttributes = new HashMap<JMXAttribute, Method>();
	/** Maps each annotated method against the associated attribute */
	protected Map<Method, JMXAttribute> jmxAttributesByMethod = new HashMap<Method, JMXAttribute>();
	/** Maps the JMXOperations to the underlying method */
	protected Map<JMXOperation, Method> jmxOperations = new HashMap<JMXOperation, Method>();
	/** Maps annotated methods to the associated JMXOperation */
	protected Map<Method, JMXOperation> jmxOperationsByMethod = new HashMap<Method, JMXOperation>();
	/** Map of JMXOperations to the contained JMXParameters */
	protected Map<JMXOperation, Set<JMXParameter>> jmxParameters = new HashMap<JMXOperation, Set<JMXParameter>>();
	/** Set of bean attribute names */
	protected Set<String> beanAttributeNames = new HashSet<String>();
	/** Map of bean attributes names to types */
	protected Map<String, String> beanAttributeTypes = new TreeMap<String, String>();
	/** Map of jmx attribute name to getter method */
	protected Map<JMXAttribute, Method> jmxAttributeGetters = new TreeMap<JMXAttribute, Method>();
	/** Map of jmx attribute name to setter method */
	protected Map<JMXAttribute, Method> jmxAttributeSetters = new TreeMap<JMXAttribute, Method>();
	
	//protected Map<String, PropertyDescriptor> propertyDescriptorsJMX = new HashMap<String, PropertyDescriptor>();
	//protected Map<String, JMXAttribute> attributeAnnotations = new HashMap<String, JMXAttribute>();
	
	protected static Logger log = Logger.getLogger(BeanReflection.class);
	
	/**
	 * Creates a new BeanRelectionDB entry for the passed pojo. 
	 * @param pojo The pojo to reflect.
	 */
	public   BeanReflection(Object pojo)  {
		int a = 0;
		int o = 0;
		int p = 0;
		clazz = pojo.getClass(); 
		managed = clazz.isAnnotationPresent(JMXManagedObject.class);
		if(managed) {
			jmxManagedObject = (JMXManagedObject)clazz.getAnnotation(JMXManagedObject.class);
			managedDeclared = jmxManagedObject.declared();
			managedAnnotated = jmxManagedObject.annotated();	
			if(managedDeclared) {
				methods = clazz.getDeclaredMethods();
			} else {
				methods = clazz.getMethods();
			}
			for(Method m: methods) {
				if(m.isAnnotationPresent(JMXAttribute.class)) {
					JMXAttribute ja = m.getAnnotation(JMXAttribute.class);
					if(ja.expose()) {
						jmxAttributes.put(ja, m);
						jmxAttributesByMethod.put(m, ja);
						a++;
					}
					if(m.getParameterTypes().length==0) {
						jmxAttributeGetters.put(ja, m);
					} else {
						jmxAttributeSetters.put(ja, m);
					}
				}
				if(m.isAnnotationPresent(JMXOperation.class)) {
					JMXOperation jo = m.getAnnotation(JMXOperation.class);
					if(jo.expose()) {
						jmxOperations.put(jo, m);
						jmxOperationsByMethod.put(m, jo);
						o++;
						Map<Integer, JMXParameter> params = getJMXParameters(m);
						if(params.size()>0) {						
							jmxParameters.put(jo, new HashSet<JMXParameter>(params.values()));
							p = p + params.size();
						}						
					}					
				}
				
			}
		}
		log("Added Attributes/Operations/Parameters:" + a + "/" + o + "/" + p);
		beanDescriptor = new BeanDescriptor(clazz);

		
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
			for(PropertyDescriptor pd: beanInfo.getPropertyDescriptors()) {
				if(!this.isDeclared(pd)) continue;
				beanAttributeNames.add(pd.getName());
				propertyDescriptors.put(pd.getName(), pd);
				beanAttributeTypes.put(pd.getName(), pd.getPropertyType().getName());
				Method getMethod = pd.getReadMethod();
				Method setMethod = pd.getWriteMethod();
				JMXAttribute attributeAnnotation = null;
				//log("PropertyDescriptor Type:" + pd.getPropertyType().getName());
				if(getMethod!=null && getMethod.isAnnotationPresent(JMXAttribute.class)) {
					attributeAnnotation = getMethod.getAnnotation(JMXAttribute.class);
					propertyDescriptors.put(attributeAnnotation.name(), pd);
				}
				if(setMethod!=null && setMethod.isAnnotationPresent(JMXAttribute.class)) {
					attributeAnnotation = setMethod.getAnnotation(JMXAttribute.class);
					if(!propertyDescriptors.containsKey(attributeAnnotation.name())) {
						propertyDescriptors.put(attributeAnnotation.name(), pd);
					}
				}
				
			}
		} catch (IntrospectionException e) {
			throw new RuntimeException("Failed to introspect bean", e);
		}
	}
	
	/**
	 * Determines if a property descriptor represents methods declared in this class.
	 * <b><i>Note:</i><b>If the declaring class for the method is <code>java.lang.Object</code>, 
	 * this will always be false.
	 * @param pd
	 * @return
	 */
	protected boolean isDeclared(PropertyDescriptor pd) {
		Method readMethod = pd.getReadMethod();
		Method writeMethod = pd.getWriteMethod();
		boolean dec = false;
		if(
				(readMethod!=null && (readMethod.getDeclaringClass().equals(clazz)) && !readMethod.getDeclaringClass().equals(Object.class))
				||
				(writeMethod!=null && (writeMethod.getDeclaringClass().equals(clazz) && !writeMethod.getDeclaringClass().equals(Object.class)))
		) dec = true;
		else dec = false;
//		StringBuilder buff = new StringBuilder(clazz.getName());
//		buff.append(".isDeclared for[");
//		if(readMethod!=null) buff.append(readMethod.getName());
//		buff.append("]/[");
//		if(writeMethod!=null) buff.append(writeMethod.getName());
//		buff.append("]:");
//		buff.append(dec);
//		log(buff.toString());
		return dec;
	}
	
	
	
	/**
	 * Returns a hashmap of JMXParameter annotations for the passed method indexed by parameter sequence.
	 * @param method
	 * @return
	 */
	protected Map<Integer, JMXParameter> getJMXParameters(Method method) {
		Map<Integer, JMXParameter> params = new HashMap<Integer, JMXParameter>();
		Annotation[][] pAnns = method.getParameterAnnotations();
		int index = 0;
		for(Annotation[] anns: pAnns) {
			for(Annotation ann: anns) {
				if(ann instanceof JMXParameter) {
					params.put(index, (JMXParameter)ann);
				}
			}
			index++;
		}
		
		return params;
	}
	
	
	

	
	/**
	 * The class type of the pojo.
	 * @return the clazz
	 */
	public Class getType() {
		return clazz;
	}


	/**
	 * Is the managed object delcared only.
	 * @return the managedDeclared
	 */
	public boolean isManagedDeclared() {
		return managedDeclared;
	}


	/**
	 * Is the managed object annotated only.
	 * @return the managedAnnotated
	 */
	public boolean isManagedAnnotated() {
		return managedAnnotated;
	}


	/**
	 * @return the beanDescriptor
	 */
	public BeanDescriptor getBeanDescriptor() {
		return beanDescriptor;
	}


	/**
	 * @return the beanInfo
	 */
	public BeanInfo getBeanInfo() {
		return beanInfo;
	}


	/**
	 * Returns a map of propertyDescriptors keyed by bean attribute names only.
	 * @return the propertyDescriptors
	 */
	public Map<String, PropertyDescriptor> getPropertyDescriptors() {
		return propertyDescriptors;
	}


	/**
	 * Returns a map of propertyDescriptors keyed by JMX attribute name, or bean attribute name if not annotated.
	 * @return the propertyDescriptorsJMX
	 */
	public Map<String, PropertyDescriptor> getJMXPropertyDescriptors(Object pojo) {
		validatePojoType(pojo);
		Map<String, PropertyDescriptor> pDesc = new HashMap<String, PropertyDescriptor>();
		// jmxAttributesByMethod <Method, JMXAttribute>
		for(PropertyDescriptor pd: propertyDescriptors.values()) {
			if(pd.getWriteMethod()!=null && jmxAttributesByMethod.containsKey(pd.getWriteMethod())) {
				pDesc.put(jmxAttributesByMethod.get(pd.getWriteMethod()).name(), pd);
			}			
			if(pd.getReadMethod()!=null && jmxAttributesByMethod.containsKey(pd.getReadMethod())) {
				pDesc.put(jmxAttributesByMethod.get(pd.getReadMethod()).name(), pd);
			}
		}		
		return pDesc;
	}
	
	/**
	 * Returns a list of exposed <code>JMXAttribute</code> names as configured in the passed pojo.
	 * @param pojo The object to extract names from. 
	 * @return A list of JMX Attribute Names.
	 */
	public Set<String> getJMXAttributeNames(Object pojo) {
		validatePojoType(pojo);		Set<String> jmxAttributeNames = new HashSet<String>();		
		for(Method m: jmxAttributes.values()) {
			jmxAttributeNames.add(getJMXAttributeName(m, pojo));
		}
		return new TreeSet<String>(jmxAttributeNames);
	}
	
	/**
	 * Returns the attribute names of the pojo, interlacing bean and jmx names.
	 * @param pojo The pojo to get names from.
	 * @return Alpha sorted set of attribute names.
	 */
	public Set<String> getAttributeNames(Object pojo) {
		validatePojoType(pojo);
		Set<String> attributeNames = new TreeSet<String>();
		Map<String, String> jmxAttrs = getBeanToJMXAttributeNames(pojo);
		Set<String> beanAttrs = getBeanAttributeNames();
		for(String s: beanAttrs) {
			if(jmxAttrs.containsKey(s)) {
				attributeNames.add(jmxAttrs.get(s));
			} else {
				attributeNames.add(s);
			}
		}
		return attributeNames;
	}
	
	
	/**
	 * Returns the attribute name of the passed method for the passed pojo.
	 * @param method The method to get the name from.
	 * @param pojo The object to the the name from.
	 * @return The JMX Attribute Name.
	 */
	public String getJMXAttributeName(Method method, Object pojo) {
		try {
			JMXAttribute attr = method.getAnnotation(JMXAttribute.class);
			if(attr.introspectName()) {
				return (String)clazz.getMethod(attr.name(), new Class[]{}).invoke(pojo, new Object[]{});
			} else {
				return attr.name();
			}			
		} catch (Exception e) {
			throw new RuntimeException("Method [" + method.getName() + "] Is Not Annotated With JMXAttribute.");
		}
	}
	
	/**
	 * Returns the attribute description of the passed method for the passed pojo.
	 * @param method The method to get the description from.
	 * @param pojo The object to the the description from.
	 * @return The JMX Attribute Description.
	 */
	public String getJMXAttributeDescription(Method method, Object pojo) {
		validatePojoType(pojo);
		try {
			JMXAttribute attr = method.getAnnotation(JMXAttribute.class);
			if(attr.introspectDescription()) {
				return (String)clazz.getMethod(attr.description(), new Class[]{}).invoke(pojo, new Object[]{});
			} else {
				return attr.description();
			}			
		} catch (Exception e) {
			throw new RuntimeException("Method [" + method.getName() + "] Is Not Annotated With JMXAttribute.");
		}
	}
	
	/**
	 * Returns a map of the JMX attribute descriptions, keyed by attribute name.
	 * @param pojo The object to retrieve descriptions from.
	 * @return An alpha sorted map of descriptions keyed by name.
	 */
	public Map<String, String> getJMXAttributeDescriptions(Object pojo) {		
		validatePojoType(pojo);
		Map<String, String> jmxAttrDescs = new TreeMap<String, String>();
		for(Method method: jmxAttributesByMethod.keySet()) {
			jmxAttrDescs.put(getJMXAttributeName(method, pojo), getJMXAttributeDescription(method, pojo));			
		}
		return jmxAttrDescs;		
	}
	
	
	/**
	 * Returns attribute descriptions, interlacing JMX and Bean descriptions
	 * @param pojo The object to acquire descriptions from.
	 * @return An alpha sorted set of the object's descriptions.
	 */
	public Map<String, String> getAttributeDescriptions(Object pojo) {
		Map<String, String> attrDescs = new TreeMap<String, String>(getJMXAttributeDescriptions(pojo));
		Collection<PropertyDescriptor> jmxPds = getJMXPropertyDescriptors(pojo).values();		
		for(PropertyDescriptor pd: propertyDescriptors.values()) {
			if(!jmxPds.contains(pd)) {
				attrDescs.put(pd.getName(), JMXAttribute.DEFAULT_DESCRIPTION);
			}
		}		
		return attrDescs;
	}
	
	
	
	
	/**
	 * Returns a map linking bean attribute names to jmx attribute names.
	 * @param pojo The object to report on.
	 * @return A map of attribute links.
	 */
	public Map<String, String> getBeanToJMXAttributeNames(Object pojo) {
		Map<String, String> attrs = new HashMap<String, String>();	
		Collection<Method> annotatedMethods = jmxAttributes.values();
		for(Method annotatedMethod: annotatedMethods) {			
			try {
				String jmxName = getJMXAttributeName(annotatedMethod, pojo);
				PropertyDescriptor pd = getPropertyDescriptorForMethod(annotatedMethod);
				attrs.put(pd.getName(), jmxName);
			} catch (Exception e) {
				log.info("Failed to get PD for method", e);
			}
		}
		return attrs;
	}
	
	/**
	 * Returns a map linking jmx attribute names to bean attribute names.
	 * @param pojo The object to report on.
	 * @return A map of attribute links.
	 */
	public Map<String, String> getJMXAttributeToBeanNames(Object pojo) {
		return swapKeyValues(getBeanToJMXAttributeNames(pojo));
	}	
	
	/**
	 * Returns the passed map with the values and keys swapped.
	 * @param <V>
	 * @param <K>
	 * @param map
	 * @return A swapped map.
	 */
	protected <V,K> Map<V,K> swapKeyValues(Map<K,V> map) {
		Map<V,K> newMap = new HashMap<V,K>(map.size());
		for(Entry<K,V> entry: map.entrySet()) {
			newMap.put(entry.getValue(), entry.getKey());
		}
		return newMap;
	}
	/**
	 * Generates a formatted string representing the "print" of a map.
	 * @param map
	 * @return A string.
	 */
	protected String printMap(Map<? extends Object, ? extends Object> map) {
		StringBuilder buff = new StringBuilder();
		for(Entry<? extends Object, ? extends Object> entry: map.entrySet()) {
			buff.append("\t").append(entry.getKey().toString()).append(":").append(entry.getValue().toString()).append("\n");
		}
		return buff.toString();
	}
	
	/**
	 * Searches the property descriptors to find one that has the passed method as a getter or setter.
	 * @param method The method find the matching property descriptor for.
	 * @return The located property descriptor.
	 * TODO: Can be optimized by caching this at startup.
	 */
	public PropertyDescriptor getPropertyDescriptorForMethod(Method method) {
		for(PropertyDescriptor pd: propertyDescriptors.values()) {
			if(pd.getReadMethod() != null && pd.getReadMethod().equals(method)) {
				return pd;
			} else if(pd.getWriteMethod() != null && pd.getWriteMethod().equals(method)) {
				return pd;
			}
		}
		throw new RuntimeException("No PropertyDescriptor Found For Method [" + method.getName() + "]");
	}
	
	/**
	 * Validates that any pojo passed in is the same type as the the type that the BeanReflection was initialized with.
	 * @param pojo
	 */
	protected void validatePojoType(Object pojo) {
		if(pojo==null) {
			throw new RuntimeException("Parameter null");
		}
		if(!pojo.getClass().equals(clazz)) {
			StringBuilder buff = new StringBuilder("Parameter of Invalid Type. Expected [");
			buff.append(clazz.getName()).append("] but received [");
			buff.append(pojo.getClass().getName()).append("]");
			throw new RuntimeException(buff.toString());
		}
	}
	
	
	public static void log(Object message) {
		System.out.println(message);
	}
	
	
	/*
	public static <T> Collection<T> extract(Collection<?> src) {
	    HashSet<T> dest = new HashSet<T>();
	    for (Object o : src)
	      if (o instanceof T )     // error
	         dest.add( (T) o);      // unchecked warning
	    return dest;
	 }
	 */ 	
	
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		
		
	}

	/**
	 * Returns a set of alpha sorted bean attribute names.
	 * @return the beanAttributeNames
	 */
	
	public Set<String> getBeanAttributeNames() {
		return Collections.unmodifiableSortedSet(new TreeSet<String>(beanAttributeNames));		
	}

	/**
	 * Returns a map of alpha sorted bean attribute names to bean attribute types.
	 * @return the beanAttributeTypes
	 */
	public Map<String, String> getBeanAttributeTypes() {
		return Collections.unmodifiableSortedMap((SortedMap<String, String>)beanAttributeTypes);
	}
	
	/**
	 * Returns a map of alpha sorted jmx attribute names to jmx attribute types.
	 * @return the jmx attribute types
	 */
	public Map<String, String> getJMXAttributeTypes(Object pojo) {
		validatePojoType(pojo);
		Map<String, String> jmxTypes = new TreeMap<String, String>();
		String name = null;
		String type = null;
		
		
				
		for(Method m: jmxAttributeGetters.values()) {
			if(m.getParameterTypes().length!=1) {
				throw new RuntimeException("Getter and Setter Types Managing Different Types");
			}
			jmxTypes.put(getJMXAttributeName(m, pojo), m.getReturnType().getName());			
		}
		
		for(Method m: jmxAttributeSetters.values()) {
			name = getJMXAttributeName(m, pojo);
			type = m.getReturnType().getName();
			if(jmxTypes.containsKey(name) && !jmxTypes.get(name).equals(type)) {
				throw new RuntimeException("Getter and Setter Types Managing Different Types");
			}			
		}
		

		

		return Collections.unmodifiableSortedMap((SortedMap<String, String>)jmxTypes);
	}
}
