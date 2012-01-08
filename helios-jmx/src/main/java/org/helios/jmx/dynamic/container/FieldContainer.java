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

import java.lang.reflect.Field;

import javax.management.AttributeNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.ReflectionException;

import org.helios.jmx.dynamic.AccessorPair;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: FieldContainer</p>
 * <p>Description: A managed container for a dynamic MBean's attributes that are managing an object fields.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class FieldContainer extends AttributeContainer  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5683422381079379348L;
	/**	The managed field */
	protected Field targetField = null;

	
	/**
	 * Constructs a new FieldContainer  based on an AccessorPair.
	 * @param ap
	 * @throws IntrospectionException
	 */
	public FieldContainer(AccessorPair ap) throws IntrospectionException {
		
		super(ap.getTargetObject(), ap.getMBeanAttributeInfo(), ap.getGetter(), ap.getSetter());
		mutability = ap.getMutability(); 
		writeOnce = (mutability.compareTo(AttributeMutabilityOption.WRITE_ONCE)==0);		
		targetField = ap.getTargetField();
	}
	
	public String getDescription() {
		return attributeInfo.getDescription();
	}
	
	/**
	 * Sets the value of the field.
	 * @param newValue The new value to set.
	 * @throws AttributeNotFoundException Thrown if the attribute does not have a setter method.
	 * @throws ReflectionException Thrown if any error occurs invoking the setter method.
	 */
	public void setAttributeValue(Object newValue) throws AttributeNotFoundException, ReflectionException {
		if(targetSetterMethod != null) {
			try {
				if(writeOnce && writen) {
					throw new AttributeNotFoundException("The field " + attributeName + " is WRITE_ONCE and has already been writen.");
				}
				targetSetterMethod.invoke(targetField, targetObject, newValue);
				if(writeOnce) {
					tripWriteOnce();
				}
			} catch (Exception e) {
				throw new ReflectionException(e, "Failed to call setter [" + targetSetterMethod.getName() + "] for field " + attributeName);
			}
		} else {
			throw new AttributeNotFoundException("The field " + attributeName + " does not have a setter. Is it READ_ONLY ?");
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
				return targetGetterMethod.invoke(targetField, targetObject);
			} catch (Exception e) {
				throw new ReflectionException(e, "Failed to call getter [" + targetGetterMethod.getName() + "] for field " + attributeName);
			}			
		} else {
			throw new AttributeNotFoundException("The field " + attributeName + " does not have a getter. Is it WRITE_ONLY ?");
		}
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
