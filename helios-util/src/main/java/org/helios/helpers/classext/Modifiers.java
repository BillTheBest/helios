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
package org.helios.helpers.classext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>Title: Modifiers</p>
 * <p>Description: Static helpers for filtering members and classes by modifier </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class Modifiers {
	private static final Map<Integer, Method> CODE2METHOD;
	public static final int BRIDGE    = 0x00000040;
	public static final int VARARGS   = 0x00000080;
	public static final int SYNTHETIC = 0x00001000;
	public static final int ANNOTATION= 0x00002000;
	public static final int ENUM      = 0x00004000;
	public static final int DEFAULT = 3;
    
	static {
		Map<Integer, Method> code2Method = new HashMap<Integer, Method>(13);
		Map<String, Method> ucaseMethods = new HashMap<String, Method>(12);
		try {
			for(Method m: Modifier.class.getDeclaredMethods()) {
				if(Modifier.isPublic(m.getModifiers()) && m.getName().startsWith("is")) {
					ucaseMethods.put(m.getName().toUpperCase(), m);
				}				
			}
			for(Method m: Modifiers.class.getDeclaredMethods()) {
				if(Modifier.isPublic(m.getModifiers()) && m.getName().startsWith("is") && m.getParameterTypes().length==1 ) {
					ucaseMethods.put(m.getName().toUpperCase(), m);
				}				
			}
			
			for(Field f: Modifier.class.getDeclaredFields()) {
				if(Modifier.isPublic(f.getModifiers())) {
					Method target = ucaseMethods.get("IS" + f.getName());
					if(target!=null) code2Method.put(f.getInt(null), target);
				}
			}
			for(Field f: Modifiers.class.getDeclaredFields()) {
				if(Modifier.isPublic(f.getModifiers())) {
					Method target = ucaseMethods.get("IS" + f.getName());
					if(target!=null) code2Method.put(f.getInt(null), target);
				}
			}
			
			//code2Method.put(Access.DEFAULT.modifierCode(), Modifiers.class.getDeclaredMethod("isDefault", Integer.TYPE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		CODE2METHOD = Collections.unmodifiableMap(code2Method);
	}
	
	/**
	 * Determines if the the passed member or class modifier matches any of the passed attributes which map to <code>java.lang.reflect.Modifier</code> constants.
	 * @param modifier The member or class modifier 
	 * @param attributes an array of <code>java.lang.reflect.Modifier</code> defined attributes.
	 * @return true if any attributes match. false if no matches are found or the attribute count is zero.
	 */
	public static boolean isAnyOf(int modifier, int...attributes) {
		if(attributes==null || attributes.length < 1) return false;
		for(int attr: attributes) {
			if(execInspection(modifier, attr)) return true;
		}
		return false;
	}
	
	public static String[] getModifierNames(int modifiers) {
		Set<String> names = new HashSet<String>();

		if(Modifier.isInterface(modifiers)) {
			names.add("INTERFACE");
		}
		if(Modifier.isAbstract(modifiers)) {
			names.add("ABSTRACT");
		}
		if(Modifier.isProtected(modifiers)) {
			names.add("PROTECTED");
		}
		if(Modifier.isFinal(modifiers)) {
			names.add("FINAL");
		}
		if(Modifier.isStatic(modifiers)) {
			names.add("STATIC");
		}
		if(Modifier.isPublic(modifiers)) {
			names.add("PUBLIC");
		}
		if(Modifier.isPrivate(modifiers)) {
			names.add("PRIVATE");
		}
		if(Modifier.isNative(modifiers)) {
			names.add("NATIVE");
		}
		if(Modifier.isStrict(modifiers)) {
			names.add("STRICT");
		}
		if(Modifier.isSynchronized(modifiers)) {
			names.add("SYNCHRONIZED");
		}
		if(Modifier.isTransient(modifiers)) {
			names.add("TRANSIENT");
		}
		if(Modifier.isVolatile(modifiers)) {
			names.add("VOLATILE");
		}		
		return names.toArray(new String[names.size()]);
	}
	
	/**
	 * Determines if the the passed member or class modifier matches all of the passed attributes which map to <code>java.lang.reflect.Modifier</code> constants.
	 * @param modifier The member or class modifier 
	 * @param attributes an array of <code>java.lang.reflect.Modifier</code> defined attributes.
	 * @return true if all of the attributes match or the attribute count is zero.
	 */
	public static boolean isAllOf(int modifier, int...attributes) {
		if(attributes==null || attributes.length < 1) return true;
		for(int attr: attributes) {
			if(!execInspection(modifier, attr)) return false;
		}
		return true;
	}
	
	
	/**
	 * Invokes the static method in <code>java.lang.reflect.Modifier</code> with the passed modifier picking the method that relates to the constant attribute passed
	 * @param modifier The modifier to test
	 * @param attribute The attribute Id to test for
	 * @return true if the method returns true, false if it returns false or a matching method cannot be found.
	 */
	protected static boolean execInspection(int modifier, int attribute) {
		if(!CODE2METHOD.containsKey(attribute)) return false;
		try {
			//return (Boolean)CODE2METHOD.get(attribute).invoke(null, new Object[]{modifier});
			return (Boolean)CODE2METHOD.get(attribute).invoke(null, modifier);
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Determines if the passed modifier represents the package protected (default) access modifier.
	 * @param mod The modifier to test
	 * @return true if the modfier is not private, protected or public.
	 */
	public static boolean isDefault(int mod) {
		return (!Modifier.isPrivate(mod) && !Modifier.isProtected(mod) && !Modifier.isPublic(mod));
	}
	
	public static boolean isVarArgs(int mod) {
		return (mod & VARARGS) != 0;
	}
	
	public static boolean isSynthetic(int mod) {
		return (mod & SYNTHETIC) != 0;
	}

	public static boolean isEnum(int mod) {
		return (mod & ENUM) != 0;
	}
	
	public static boolean isBridge(int mod) {
		return (mod & BRIDGE) != 0;
	}
	
	
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("CODE2METHODS:");
		for(Map.Entry<Integer, Method> entry: CODE2METHOD.entrySet()) {
			//log("[" + entry.getKey() + "] --> " + entry.getValue()==null ? "Null" : entry.getValue().getName());
			log(entry.getKey() + ":" + entry.getValue().getName());
		}

	}
	
	public static void log(Object message) {
		System.out.println(message);
	}

}
