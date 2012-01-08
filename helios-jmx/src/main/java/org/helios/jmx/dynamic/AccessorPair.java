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
package org.helios.jmx.dynamic;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.dynamic.container.FieldContainer;

/**
 * <p>Title: AccessorPair</p>
 * <p>Description: A utility class for processing annotated attributes.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public class AccessorPair {
	protected String attributeName = null;
	protected Method setter = null;
	protected Method getter = null;
	protected AttributeMutabilityOption mutability = AttributeMutabilityOption.READ_WRITE;
	protected JMXAttribute getterAttribute = null;
	protected JMXAttribute setterAttribute = null;
	protected Object targetObject = null;
	protected Field targetField = null;
	protected String description = null;
	
	public static Class[] NO_SIG = new Class[]{};
	public static Object[] NO_ARGS = new Object[]{};
	
	/**
	 * @param attributeName
	 */
	public AccessorPair(String attributeName, Object targetObject) {
		this.attributeName = attributeName;
		this.targetObject = targetObject;
	}
	/**
	 * @param attributeName
	 * @param setter
	 * @param getter
	 */
	public AccessorPair(String attributeName, Method setter, Method getter, Object targetObject) {
		this.attributeName = attributeName;
		this.setter = setter;
		this.getter = getter;
		this.targetObject = targetObject;
	}
	
	/**
	 * @param attributeName
	 * @param setter
	 * @param getter
	 * @param targetObject
	 * @param targetField
	 */
	public AccessorPair(String attributeName, Method setter, Method getter, Object targetObject, Field targetField) {
		this.attributeName = attributeName;
		this.setter = setter;
		this.getter = getter;
		this.targetObject = targetObject;
		this.targetField = targetField;
	}
	
	public MBeanAttributeInfo getMBeanAttributeInfo()  throws IntrospectionException {
		MBeanAttributeInfo minfo = null;
		if(targetField==null) {
			if(targetObject instanceof FieldWrapper) {
				Field field = ((FieldWrapper)targetObject).getFieldContainer().getTargetField();
				minfo = new MBeanAttributeInfo(getAttributeName(), field.getType().getName(), getDescription(), getMutability().isReadable(), getMutability().isWritable(), (field.getType().equals(Boolean.TYPE)));
			} else {
				minfo =  new MBeanAttributeInfo(getAttributeName(), getDescription(), getGetter(), getSetter());
			}
			 
		} else {
			minfo = new MBeanAttributeInfo(getAttributeName(), getTargetField().getType().getName(), getDescription(), getMutability().isReadable(), getMutability().isWritable(), (getTargetField().getType().equals(Boolean.TYPE)));
		}
		return minfo;
	}
	
	/**
	 * @return the attributeName
	 */
	public String getAttributeName() {
		if(attributeName!=null) return attributeName;
		return getAttrName(getterAttribute, setterAttribute);
	}
	/**
	 * @param attributeName the attributeName to set
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	/**
	 * @return the setter
	 */
	public Method getSetter() {
		return setter;
	}
	/**
	 * @param setter the setter to set
	 */
	public void setSetter(Method setter) {
		this.setter = setter;
		this.setterAttribute = setter.getAnnotation(JMXAttribute.class);
	}
	/**
	 * @return the getter
	 */
	public Method getGetter() {
		return getter;
	}
	/**
	 * @param getter the getter to set
	 */
	public void setGetter(Method getter) {
		this.getter = getter;
		this.getterAttribute = getter.getAnnotation(JMXAttribute.class);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributeName == null) ? 0 : attributeName.hashCode());
		return result;
	}
	/* (non-Javadoc)
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
		final AccessorPair other = (AccessorPair) obj;
		if (attributeName == null) {
			if (other.attributeName != null)
				return false;
		} else if (!attributeName.equals(other.attributeName))
			return false;
		return true;
	}
	/**
	 * @return the mutability
	 */
	public AttributeMutabilityOption getMutability() {
		return mutability;
	}
	/**
	 * @param mutability the mutability to set
	 */
	public void setMutability(AttributeMutabilityOption mutability) {
		this.mutability = mutability;		
	}
	
	/**
	 * Logically examines the annotated descriptions for an attribute's getter and setter method and determines a single description for the attribute.
	 * @return The atribute description.
	 */
	public String getDescription() {
		if(description!=null) return description;
		String setterDescription = (setterAttribute==null ? null : getAttrDescription(setterAttribute));
		String getterDescription = (getterAttribute==null ? null : getAttrDescription(getterAttribute));
		if(setterDescription==null && getterDescription==null) return JMXAttribute.DEFAULT_DESCRIPTION;
		if(getterDescription.equals(setterDescription)) return getterDescription;
		if(getterDescription!=null && setterDescription==null) return getterDescription;
		if(setterDescription!=null && getterDescription==null) return setterDescription;		
		if(getterDescription.equals(JMXAttribute.DEFAULT_DESCRIPTION)) return setterDescription;
		return getterDescription;
	}
	
	/**
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Determines the description of the <code>JMXAttribute</code> driven attribute description.
	 * @param attr
	 * @return
	 */
	protected String getAttrDescription(JMXAttribute attr) {
		if(attr.introspectDescription()) {
			try {
				return targetObject.getClass().getMethod(attr.description(), NO_SIG).invoke(targetObject, NO_ARGS).toString();
			} catch (Exception e) {
				return JMXAttribute.DEFAULT_DESCRIPTION;
			} 
		} else {
			return attr.description();
		}
	}
	
	protected String getAttrName(JMXAttribute attr) {
		if(attr.introspectName()) {
			try {
				return targetObject.getClass().getMethod(attr.name(), NO_SIG).invoke(targetObject, NO_ARGS).toString();
			} catch (Exception e) {
				return attr.name();
			} 
		} else {
			return attr.name();
		}
	}
	
	
	/**
	 * Determines the name of the <code>JMXAttribute</code> driven attribute name. 
	 * @param getter
	 * @param setter
	 * @return
	 */
	protected String getAttrName(JMXAttribute getter, JMXAttribute setter) {
		if(setter == null && getter==null) return "Error. No Attr Name Determined";
		if(getter!=null) {
			return getAttrName(getter);
		} else {
			return getAttrName(setter);
		}
	}
	
	/**
	 * @return
	 */
	public JMXAttribute getGetterAttribute() {
		return getterAttribute;
	}
	/**
	 * @return
	 */
	public JMXAttribute getSetterAttribute() {
		return setterAttribute;
	}
	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	
	    StringBuilder retValue = new StringBuilder();
	    
	    retValue.append("AccessorPair ( ")
	        .append(super.toString()).append(TAB)
	        .append("attributeName = ").append(this.attributeName).append(TAB)
	        .append("setter = ").append(this.setter).append(TAB)
	        .append("getter = ").append(this.getter).append(TAB)
	        .append(" )");
	    
	    return retValue.toString();
	}
	/**
	 * @return the targetObject
	 */
	public Object getTargetObject() {
		return targetObject;
	}
	/**
	 * @param targetObject the targetObject to set
	 */
	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}
	/**
	 * @return the targetField
	 */
	public Field getTargetField() {
		return targetField;
	}
	/**
	 * @param targetField the targetField to set
	 */
	public void setTargetField(Field targetField) {
		this.targetField = targetField;
	}

}
