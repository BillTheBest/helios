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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javassist.Modifier;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * <p>Title: OpenTypeHelper</p>
 * <p>Description: Utility class for creating OpenTypes </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.helpers.OpenTypeHelper</code></p>
 */

public class OpenTypeHelper {
	/**
	 * Creates a new CompositeType
	 * @param typeName The name given to the composite type this instance represents; cannot be a null or empty string. 
	 * @param description The human readable description of the composite type this instance represents; cannot be a null or empty string. 
	 * @param itemNames The names of the items contained in the composite data values described by this CompositeType instance; cannot be null and should contain at least one element; no element can be a null or empty string. Note that the order in which the item names are given is not important to differentiate a CompositeType instance from another; the item names are internally stored sorted in ascending alphanumeric order. 
	 * @param itemDescriptions The descriptions, in the same order as itemNames, of the items contained in the composite data values described by this CompositeType instance; should be of the same size as itemNames; no element can be a null or empty string
	 * @param itemTypes The open type instances, in the same order as itemNames, describing the items contained in the composite data values described by this CompositeType instance; should be of the same size as itemNames; no element can be null. 
	 * @return a new CompositeType
	 */
	public static CompositeType createCompositeType(String typeName, String description, String[] itemNames, String[] itemDescriptions, OpenType<?>[] itemTypes) {
		try {
			return new CompositeType(typeName, description, itemNames, itemDescriptions, itemTypes);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create composite type named [" + typeName + "]", e);
		}
	}
	
	private static final Map<String, OpenType> SIMPLE_TYPES = new HashMap<String, OpenType>(28);
	
	static {
		try {
			for(Field f: SimpleType.class.getDeclaredFields()) {
				if(Modifier.isPublic(f.getModifiers())) {
					if(f.getType().equals(SimpleType.class)) {
						SimpleType st = (SimpleType)f.get(null);
						SIMPLE_TYPES.put(st.getTypeName(), st);
						SIMPLE_TYPES.put(Class.forName(st.getTypeName()).getSimpleName(), st);
						SIMPLE_TYPES.put(Class.forName(st.getTypeName()).getSimpleName() + "[]", new ArrayType(2, st));
						
						
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	
	
	public static void main(String[] args) throws Exception {
		log("OpenTypeHelper Test [" + SIMPLE_TYPES.size() + "]");
		log("String Arr:" + new String[]{}.getClass().getName());
		for(Map.Entry<String, OpenType> e: SIMPLE_TYPES.entrySet()) {
			
			log(e.getKey() + "  :  " + e.getValue().getClass().getName());
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
