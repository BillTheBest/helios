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
package org.helios.jmx.dynamic.container;


import java.io.Serializable;
import java.lang.reflect.Method;

import javax.management.AttributeNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.ReflectionException;

import org.helios.jmx.dynamic.AccessorPair;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: AttributeContainer</p>
 * <p>Description: A managed container for a dynamic MBean's attributes.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public class AttributeContainer extends MBeanContainer implements Comparable<AttributeContainer>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6272664974883231003L;
	/**	A generated attribute info for the managed object's exposed attribute. */
	protected MBeanAttributeInfo attributeInfo = null;
	/**	The method for setting the attribute in the managed object. */
	protected Method targetSetterMethod = null;
	/**	 The method for getting the attribute in the managed object. */
	protected Method targetGetterMethod = null;
	/** Indicates if the attribute should be read only after the first access */
	protected boolean writeOnce = false;
	/** The attributes mutability */
	protected AttributeMutabilityOption mutability = null;
	/** Indicates if the first write has taken effect */
	protected boolean writen = false;
	/** The attribute name */
	protected String attributeName = null;
	/**	Empty object array */
	protected final static  Object[] NO_ARGS = new Object[]{};
	
	protected AccessorPair pair = null;
	
	
	/**
	 * Simple constructor.
	 */
	public AttributeContainer() {
		
	}
	
	/**
	 * Constructs a new AttributeContainer based on an AccessorPair.
	 * @param ap
	 * @throws IntrospectionException
	 */
	public AttributeContainer(AccessorPair ap) throws IntrospectionException {
		this(ap.getTargetObject(), ap.getMBeanAttributeInfo(), ap.getGetter(), ap.getSetter());
		//this(ap.getTargetObject(), new MBeanAttributeInfo(ap.getAttributeName(), ap.getDescription(), ap.getGetter(), ap.getSetter()), ap.getGetter(), ap.getSetter());
		mutability = ap.getMutability(); 
		pair = ap;
		writeOnce = (mutability.compareTo(AttributeMutabilityOption.WRITE_ONCE)==0);
	}
	
	

	/**
	 * Creates a new attribute container for the specified managed object's attribute. 
	 * @param targetObject The managed object.
	 * @param attributeInfo The JMX Attribute information.
	 * @param targetGetterMethod The method for setting the attribute in the managed object.
	 * @param targetSetterMethod The method for getting the attribute in the managed object.
	 */
	public AttributeContainer(Object targetObject, MBeanAttributeInfo attributeInfo, Method targetGetterMethod, Method targetSetterMethod) {
		super();
		this.targetObject = targetObject;
		this.attributeInfo = attributeInfo;
		this.targetSetterMethod = targetSetterMethod;
		this.targetGetterMethod = targetGetterMethod;
		attributeName = attributeInfo.getName();
	}
	
	/**
	 * Sets the value of the attribute.
	 * @param newValue The new value to set.
	 * @throws AttributeNotFoundException Thrown if the attribute does not have a setter method.
	 * @throws ReflectionException Thrown if any error occurs invoking the setter method.
	 */
	public void setAttributeValue(Object newValue) throws AttributeNotFoundException, ReflectionException {
		if(targetSetterMethod != null) {
			try {
				if(writeOnce && writen) {
					throw new AttributeNotFoundException("The attribute " + attributeName + " is WRITE_ONCE and has already been writen.");
				}
				targetSetterMethod.invoke(targetObject, newValue);
				if(writeOnce) {
					tripWriteOnce();
				}
			} catch (Exception e) {
				throw new ReflectionException(e, "Failed to call setter [" + targetSetterMethod.getName() + "] for attribute " + attributeName);
			}
		} else {
			throw new AttributeNotFoundException("The attribute " + attributeName + " does not have a setter.");
		}
	}
	
	/**
	 * Returns the value of the attribute.
	 * @return The value of the attribute.
	 * @throws AttributeNotFoundException Thrown if the attribute does not have a getter method.
	 * @throws ReflectionException Thrown if any error occurs invoking the getter method.
	 */
	public Object getAttributeValue()  throws AttributeNotFoundException, ReflectionException {
		if(targetGetterMethod != null) {
			try {
				return targetGetterMethod.invoke(targetObject, NO_ARGS);
			} catch (Exception e) {
				throw new ReflectionException(e, "Failed to call getter [" + targetGetterMethod.getName() + "] for attribute " + attributeName);
			}			
		} else {
			throw new AttributeNotFoundException("The attribute " + attributeName + " does not have a getter.");
		}
	}

	/**
	 * @return the attributeInfo
	 * @throws IntrospectionException 
	 */
	public MBeanAttributeInfo getAttributeInfo() throws IntrospectionException {
		if(pair!=null) return pair.getMBeanAttributeInfo();
		return attributeInfo;
	}

	/**
	 * @param attributeInfo the attributeInfo to set
	 */
	public void setAttributeInfo(MBeanAttributeInfo attributeInfo) {
		this.attributeInfo = attributeInfo;
		attributeName = attributeInfo.getName();
	}

	/**
	 * @return the targetField
	 */
	public Method getTargetGetterMethod() {
		return targetGetterMethod;
	}

	/**
	 * @param targetGetterMethod the targetGetterMethod to set
	 */
	public void setTargetGetterMethod(Method targetGetterMethod) {
		this.targetGetterMethod = targetGetterMethod;
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
	 * @return the targetSetterMethod
	 */
	public Method getTargetSetterMethod() {
		return targetSetterMethod;
	}

	/**
	 * @param targetSetterMethod the targetSetterMethod to set
	 */
	public void setTargetSetterMethod(Method targetSetterMethod) {
		this.targetSetterMethod = targetSetterMethod;
	}

	/**
	 * @return the writeOnce
	 */
	public boolean isWriteOnce() {
		return writeOnce;
	}

	/**
	 * @param writeOnce the writeOnce to set
	 */
	public void setWriteOnce(boolean writeOnce) {
		this.writeOnce = writeOnce;
	}

	/**
	 * @return the writen
	 */
	public boolean isWriten() {
		return writen;
	}

	/**
	 * @param writen the writen to set
	 */
	public void setWriten(boolean writen) {
		this.writen = writen;
	}
	
	/**
	 * Converts a READ_WRITE attribute to a READ_ONLY.
	 */
	protected void tripWriteOnce() {
		writen = true;
		try {
			attributeInfo = new MBeanAttributeInfo(attributeInfo.getName(), attributeInfo.getDescription(), targetGetterMethod, null);
		} catch (IntrospectionException e) {
			throw new RuntimeException("Unexpected failure converting attribute to ReadOnly", e);
		}
	}
	
	/**
	 * Resets a tripped WRITE_ONCE attribute. 
	 */
	public void resetWriteOnce() {
		if(writeOnce) {
			writen = false;
			try {
				attributeInfo = new MBeanAttributeInfo(attributeInfo.getName(), attributeInfo.getDescription(), targetGetterMethod, targetSetterMethod);
			} catch (IntrospectionException e) {
				throw new RuntimeException("Unexpected failure resetting attribute to WriteOnce", e);
			}
		}
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
		final AttributeContainer other = (AttributeContainer) obj;
		if (attributeName == null) {
			if (other.attributeName != null)
				return false;
		} else if (!attributeName.equals(other.attributeName))
			return false;
		return true;
	}

	public AttributeMutabilityOption getMutability() {
		return mutability;
	}


	public void setMutability(AttributeMutabilityOption mutability) {
		this.mutability = mutability;
	}


	public String getAttributeName() {
		return attributeName;
	}

	public int compareTo(AttributeContainer ac) {
		return attributeName.compareTo(ac.getAttributeName());
	}

}
