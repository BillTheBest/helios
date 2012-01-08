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
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.helios.helpers.ClassHelper;
import org.helios.jmx.opentypes.annotations.OpenTypeAttribute;

/**
 * <p>Title: PropertyTableFactory</p>
 * <p>Description: Factory and caching utility class for creating property table instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.opentypes.property.PropertyTableFactory</code></p>
 */

public class PropertyTableFactory {
	public static PropertyTable<String, Object> createPropertyTable(Object object, boolean sorted) {
		Class<?> clazz = ClassHelper.nvl(object, "Passed object was null").getClass();
		Field[] propertyFields = ClassHelper.getAnnotatedFields(clazz, OpenTypeAttribute.class);
		Set<Method> propertyMethods = ClassHelper.getAnnotatedMethods(clazz, OpenTypeAttribute.class);
		
		PropertyTable<String, Object> pt = new PropertyTable<String, Object> (propertyFields.length, true);
		for(Field f: propertyFields) {
			OpenTypeAttribute ota = f.getAnnotation(OpenTypeAttribute.class);
			String id = ota.id();
			if("".equals(id)) {
				id = f.getName();
			}
			pt.put(id, new FieldAttributeAccessor(f, object));
		}
		for(Method m: propertyMethods) {
			OpenTypeAttribute ota = m.getAnnotation(OpenTypeAttribute.class);
			String id = ota.id();
			if("".equals(id)) {
				id = m.getName().substring(3);
			}
			pt.put(id, new MethodAttributeAccessor(m, object));
		}		
		
		return pt;
	}
	
	public static void main(String[] args) {
		log("PropertyTable Test");
		Sample s = new Sample();
		PropertyTable<String, Object> pt = PropertyTableFactory.createPropertyTable(s, true);
		log(pt.values());
		StringBuilder b = new StringBuilder("PT Map Contents:");
		for(Map.Entry<String, Object> e: pt.entrySet()) {
			b.append("\n\t").append(e.getKey()).append(":").append(e.getValue());
		}
		log(b);
		pt.put("FOO", "BAR");
		pt.put("bar", 10);
		pt.put("MyLong", 2398572);
		b = new StringBuilder("PT Map Contents:");
		for(Map.Entry<String, Object> e: pt.entrySet()) {
			b.append("\n\t").append(e.getKey()).append(":").append(e.getValue());
		}
		log(b);		
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}


class Sample {
	@OpenTypeAttribute(id="FOO")
	String foo = "Foo";
	@OpenTypeAttribute
	int bar = 5;
	@OpenTypeAttribute
	static Date today = new Date();
	
	
	protected long myLong = System.nanoTime();

	/**
	 * @return the myLong
	 */
	@OpenTypeAttribute(writable=true)
	public long getMyLong() {
		return myLong;
	}

	/**
	 * @param myLong the myLong to set
	 */
	public void setMyLong(long myLong) {
		this.myLong = myLong;
	}
	
}
