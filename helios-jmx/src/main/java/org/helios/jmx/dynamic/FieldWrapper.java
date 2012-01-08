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

import javax.management.AttributeNotFoundException;
import javax.management.ReflectionException;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.dynamic.container.FieldContainer;

/**
 * <p>Title: FieldWrapper</p>
 * <p>Description: Serves as a field wrapper for cases where an annotated field is to be registered as an attribute in a MODB that is not the field's declaring object.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject
public class FieldWrapper<T> {
	protected FieldContainer fieldContainer = null;
	protected AttributeMutabilityOption mutability = AttributeMutabilityOption.READ_ONLY; 
	
	/**
	 * @param fieldContainer
	 */
	public FieldWrapper(FieldContainer fieldContainer) {
		super();
		this.fieldContainer = fieldContainer;
		fieldContainer.getMutability();
	}

	public String getName() {
		return fieldContainer.getAttributeName();
	}
	
	public String getDescription() {
		return fieldContainer.getDescription();
	}
	
	
	@JMXAttribute (name="getName", introspectName=true, description="getDescription", introspectDescription=true, mutabilityName="getMutability", introspectMutability=true)
	public T getValue() throws AttributeNotFoundException, ReflectionException {
		return (T)fieldContainer.getAttributeValue();
	}
	
	public void setValue(T value) throws AttributeNotFoundException, ReflectionException {
		fieldContainer.setAttributeValue(value);
	}

	/**
	 * @return the fieldContainer
	 */
	public FieldContainer getFieldContainer() {
		return fieldContainer;
	}

	/**
	 * @param fieldContainer the fieldContainer to set
	 */
	public void setFieldContainer(FieldContainer fieldContainer) {
		this.fieldContainer = fieldContainer;
	}

	/**
	 * @return the mutability
	 */
	public AttributeMutabilityOption getMutability() {
		return mutability;
	}
}
