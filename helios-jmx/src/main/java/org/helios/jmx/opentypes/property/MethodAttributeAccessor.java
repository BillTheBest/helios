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

import static org.helios.helpers.ClassHelper.nvl;

import java.lang.reflect.Method;

import org.helios.helpers.ClassHelper;
import org.helios.jmx.opentypes.annotations.OpenTypeAttribute;

/**
 * <p>Title: MethodAttributeAccessor</p>
 * <p>Description: A method pair wrapping attribute accessor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.opentypes.property.MethodAttributeAccessor</code></p>
 */

public class MethodAttributeAccessor<T> implements AttributeAccessor<T> {
	/** The wrapped reader */
	protected final Method reader;
	/** The wrapped writer */
	protected final Method writer;	
	/** The object instance */
	protected final Object object;
	
	/**
	 * Creates a new MethodAttributeAccessor
	 * @param object The object instance
	 * @param accessor A getter or setter
	 * 
	 */
	public MethodAttributeAccessor(Method accessor, Object object) {
		this.object = nvl(object, "Passed object was null"); 
		OpenTypeAttribute ota = nvl(nvl(accessor, "Accessor method was null").getAnnotation(OpenTypeAttribute.class), "The method was not annotated with @OpenTypeAttribute");
		if(accessor.getName().startsWith("get")) {
			reader = accessor;
			reader.setAccessible(true);
			writer = ota.writable() ? ClassHelper.getOpposer(accessor) : null;
		} else {
			writer = ota.writable() ? accessor : null;
			reader = ClassHelper.getOpposer(accessor);
		}
		if(writer!=null) {
			writer.setAccessible(true);
		}
	}

	/**
	 * Sets the wrapped attribute value
	 * @param t the value to set to 
	 */
	public void set(T t) {
		if(writer!=null) {
			try {
				writer.invoke(object, t);
			} catch (Exception e) {
				throw new RuntimeException("Failed to invoke method [" + writer.getDeclaringClass().getName() + "." + writer.getName() + "] with value [" + t + "]", e);
			}
		}
	}
	/**
	 * Retrieves the attribute value
	 * @return the value of the wrapped attribute
	 */
	public T get() {
		try {			
			return (T) reader.invoke(object);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke method [" + reader.getDeclaringClass().getName() + "." + reader.getName() + "]", e);
		}
		
	}
	
	/**
	 * Indicates if the attribute is writable
	 * @return true if the attribute is writable, false if it is read only
	 */
	public boolean isWritable() {
		return writer!=null;
	}

	@Override
	public String getName() {
		return reader.getDeclaringClass().getName() + "." + reader.getName();
	}
	

}
