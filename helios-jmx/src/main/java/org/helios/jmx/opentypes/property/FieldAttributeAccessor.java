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
package org.helios.jmx.opentypes.property;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.helios.helpers.ClassHelper;

/**
 * <p>Title: FieldAttributeAccessor</p>
 * <p>Description: A field wrapping attribute accessor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.opentypes.property.FieldAttributeAccessor</code></p>
 */

public class FieldAttributeAccessor<T> implements AttributeAccessor<T> {
	/** The wrapped field */
	protected final Field field;
	/** The object instance */
	protected final Object object;
	
	/**
	 * Creates a new FieldAttributeAccessor
	 * @param field The field wrapped by this accessor
	 * @param object The object instance
	 */
	public FieldAttributeAccessor(Field field, Object object) {
		this.field = ClassHelper.nvl(field, "Passed field was null");
		this.field.setAccessible(true);
		if(Modifier.isStatic(this.field.getModifiers())) {
			this.object = null;
		} else {
			if(object==null) throw new IllegalArgumentException("The field [" + field.getName() + "] is not static and passed object instance was null", new Throwable());
			this.object = object;
		}
	}
	
	/**
	 * Creates a new FieldAttributeAccessor
	 * @param fieldName The name of the field wrapped by this accessor
	 * @param object The object instance. If the object type is a class, it will be auto cast.
	 */
	public FieldAttributeAccessor(String fieldName, Object object) {
		this(ClassHelper.getField((object instanceof Class<?>) ? (Class<?>)object : object.getClass(), fieldName, true), object);
	}
	
	/**
	 * Indicates if the attribute is writable
	 * @return true if the attribute is writable, false if it is read only
	 */
	public boolean isWritable() {
		return true;
	}
	
	
	/**
	 * Returns the value of the field
	 * @return the value of the field
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T get() {
		try {
			return (T) field.get(object);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get value from field [" + field.getName() + "] in instance of [" + object.getClass().getName() + "]", e);
		}
	}

	/**
	 * Sets the value of the field
	 * @param t The value to set in the field
	 */
	@Override
	public void set(T t) {
		try {
			field.set(object, t);
		} catch (Exception e) {
			throw new RuntimeException("Failed to set value on field [" + field.getName() + "] in instance of [" + object.getClass().getName() + "]", e);
		}
	}

	@Override
	public String getName() {
		return field.getDeclaringClass().getName() + "." + field.getName();
	}

}
