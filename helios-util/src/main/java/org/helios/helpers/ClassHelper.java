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

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.helios.enums.Primitive;
import org.helios.helpers.classext.Access;
import org.helios.helpers.classext.Accessor;
import org.helios.helpers.classext.ClassOrMember;
import org.helios.helpers.classext.IFilterBuilder;
import org.helios.helpers.classext.Modifiers;
/**
 * <p>Title: ClassHelper</p>
 * <p>Description: Static utility helper methods to assist with compiling complex meta-data on classes and their hierarchy.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.helpers.ClassHelper</code></p>
 */
public class ClassHelper {
	
	/**
	 * Examines the passed class looking for methods matching the passed name and having the passed number of arguments
	 * @param target The class to examine
	 * @param name The name of the method to search for
	 * @param argCount The number of arguments to match
	 * @return A set of matching methods
	 */
	public static Set<Method> getMatchingMethods(Class<?> target, String name, int argCount) {
		Set<Method> methods = new HashSet<Method>();
		Set<String> methodNames = new HashSet<String>();
		for(Method method: target.getDeclaredMethods()) {
			if(method.getName().equals(name) && method.getParameterTypes().length==argCount) {
				methods.add(method);
				method.setAccessible(true);
				methodNames.add(method.toGenericString());
			}
		}
		for(Method method: target.getMethods()) {
			if(method.getName().equals(name) && method.getParameterTypes().length==argCount) {
				if(!methodNames.contains(method.toGenericString())) {
					methods.add(method);
					method.setAccessible(true);
				}
			}
		}
		return methods;
	}
	
	/**
	 * Examines the passed class looking for methods matching the passed name and having parameters matching the passed argument types
	 * @param target The class to examine
	 * @param name The name of the method to search for
	 * @param args The arguments to match the type on
	 * @return The matching method or null if a method could not be matched
	 */
	public static Method getMatchingMethod(Class<?> target, String name, Object...args) {
		if(args==null) args=new Object[0];
		int argCount = args.length;
		Class<?>[][] argTypes = new Class[args.length][];
		for(int i = 0; i < args.length; i++) {
			if(Primitive.isDual(args[i].getClass())) {
				argTypes[i] = Primitive.getAutoBoxPair(args[i].getClass());
			} else {
				argTypes[i] = new Class[]{args[i].getClass()};
			}
		}
		for(Method method : getMatchingMethods(target, name, argCount)) {
			Class<?>[] methodSignature = method.getParameterTypes();
			Class<?>[][] methodTypes = new Class[methodSignature.length][];
			for(int x = 0; x < methodSignature.length; x++) {
				if(Primitive.isDual(methodSignature[x])) {
					methodTypes[x] = Primitive.getAutoBoxPair(methodSignature[x]);
				} else {
					methodTypes[x] = new Class[]{methodSignature[x]};
				}
			}			
			if(matchSignatures(methodTypes, argTypes)) {
				return method;
			}
		}
		return null;
	}
	
	public static void main(String[] args) {
		log("MethodMatch Test");
		log("MethodMatch On Simple1:" + getMatchingMethod(MethodMatchTestClass.class, "simple", new Date()));
		log("MethodMatch On Simple2 With int:" + getMatchingMethod(MethodMatchTestClass.class, "simple", 1, new Date()));
		log("MethodMatch On Simple2 With Integer:" + getMatchingMethod(MethodMatchTestClass.class, "simple", new Integer(1), new Date()));
		log("MethodMatch On Simple2 With Integer:" + getMatchingMethod(MethodMatchTestClass.class, "simple", new Long(1), new Date()));
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	private static class MethodMatchTestClass {
		public void simple(Date date) {
			
		}
		public void simple(int i, Date date) {
			
		}
		
	}
	
	/**
	 * Tests the passed signatures for assignability
	 * @param sig1 A dual type signature
	 * @param sig2 A dual type signature
	 * @return true if there is an invocable match between the two signatures
	 */
	public static boolean matchSignatures(Class<?>[][] sig1, Class<?>[][] sig2) {
		return matchSignatures(sig1, sig2, false);
	}
	
	
	/**
	 * Tests the passed signatures for equality
	 * @param sig1 A dual type signature
	 * @param sig2 A dual type signature
	 * @param exact true if the types must be an exact match, false if an assignable subclass in sig2 should be considered a match
	 * @return true if there is an invocable match between the two signatures
	 */
	public static boolean matchSignatures(Class<?>[][] sig1, Class<?>[][] sig2, boolean exact) {
		if(sig1==null || sig2==null || sig1.length != sig2.length) return false;
		for(int i = 0; i < sig1.length; i++) {
			Class<?>[] sig1Entry = sig1[i];
			Class<?>[] sig2Entry = sig2[i];
			boolean match = false;
			for(int x = 0; x < sig1Entry.length; x++) {
				for(int y = 0; y < sig2Entry.length; y++) {
					if(exact) {
						if(sig1Entry[x].equals(sig2Entry[y])) {
							match = true;
							break;
						}
					} else {
						if(sig1Entry[x].isAssignableFrom(sig2Entry[y])) {
							match = true;
							break;
						}						
					}
				}
			}
			if(!match) return false;
		}
		return true;
	}
	
	/**
	 * Creates an array of classes comprising an array of the type or an array of the primitive and autoboxable
	 * @param types the types to create a signature from
	 * @return a 2D class array
	 */
	public static Class<?>[][] getDualSignature(Class<?>...types) {
		if(types==null || types.length<1) return new Class[0][0];
		Class[][] classSig = new Class[types.length][];
		int cntr = 0;
		for(Class<?> type: types) {
			if(Primitive.isDual(type)) {
				classSig[cntr] = Primitive.getAutoBoxPair(type);
			} else {
				classSig[cntr] = new Class[]{type};
			}
			cntr++;
		}
		return classSig;		
	}
	
	/**
	 * Creates an array of classes comprising an array of the type or an array of the primitive and autoboxable
	 * @param args the objects to get types for creating a signature from
	 * @return a 2D class array
	 */
	public static Class<?>[][] getDualSignature(Object...args) {
		if(args==null || args.length<1) return new Class[0][0];
		Class[][] classSig = new Class[args.length][];
		int cntr = 0;
		for(Object arg: args) {
			Class<?> argType = arg.getClass();
			if(Primitive.isDual(argType)) {
				classSig[cntr] = Primitive.getAutoBoxPair(argType);
			} else {
				classSig[cntr] = new Class[]{argType};
			}
			cntr++;
		}
		return classSig;
	}
	
	
	/**
	 * Examines the class of the passed object looking for methods matching the passed name and having the passed number of arguments
	 * @param target The class to examine
	 * @param name The name of the method to search for
	 * @param argCount The number of arguments to match
	 * @return A set of matching methods
	 */
	public static Set<Method> getMatchingMethods(Object target, String name, int argCount) {
		return getMatchingMethods(target.getClass(), name, argCount);
	}
	
	
	/**
	 * Returns a set of all the classes recursively found in the passed package
	 * @param packageName The name of the package to load from
	 * @param loaders An optional array of classloaders to use to find the correct package
	 * @return a set of classes
	 */
	public static Set<Class<?>> getClassesInPackage(String packageName, ClassLoader...loaders) {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader theRightOne = Package.getPackage(packageName) != null ? currentClassLoader : null;
		if(theRightOne==null) {
			if(loaders!=null) {
				for(ClassLoader cl: loaders) {
					if(cl==null) continue;
					try {
						Thread.currentThread().setContextClassLoader(currentClassLoader);
						if(Package.getPackage(packageName) != null) {
							theRightOne = cl;
							break;
						}
					} finally {
						Thread.currentThread().setContextClassLoader(currentClassLoader);
					}
				}
			}
		}
		if(theRightOne==null) {
			throw new RuntimeException("Failed to find a classloader that recognizes the package [" + packageName + "]", new Throwable());
		}
		
		
		return classes;
	}
	
	/**
	 * Null value checker. Tests the passed object. If it is null, throws an illegal argument exception. Otherwise returns the reference.
	 * @param ref The object reference to test
	 * @param errorMessage The error message to throw
	 * @return the not-null reference
	 * @throws IllegalArgumentException
	 */
	public static <T> T nvl(T ref, CharSequence errorMessage) {
		if(ref==null) throw new IllegalArgumentException(errorMessage.toString());
		return ref;
	}
	
	/**
	 * Null value checker. Tests the passed object. If it is null, throws an illegal argument exception. Otherwise returns the reference.
	 * @param ref The object reference to test
	 * @return the not-null reference
	 * @throws IllegalArgumentException
	 */
	public static <T> T nvl(T ref) {
		if(ref==null) throw new IllegalArgumentException("Reference was null");
		return ref;
	}
	
	/**
	 * Null value checker. Tests the passed object. If it is null, returns the default. Otherwise returns the reference.
	 * @param ref The object reference to test
	 * @param def The default to return if <code>ref</code> is null.
	 * @return the not-null reference or the default.
	 */
	public static <T> T nvl(T ref, T def) {
		if(ref==null) return def;
		return ref;
	}	
	
	
	/**
	 * Compares two arrays of classes for equality.
	 * @param arr1 An array of classes
	 * @param arr2 An array of classes
	 * @return true if they are equal.
	 */
	public static boolean classArraysEqual(Class<?>[] arr1, Class<?>[] arr2) {
		if(arr1==null && arr2==null) return true;
		if(arr1==null || arr2==null) return false;
		if(arr1.length!=arr2.length) return false;
		for(int i = 0; i < arr1.length; i++) {
			if(!arr1[i].equals(arr2[i])) return false;
		}
		return true;
	}
	
	/**
	 * Finds the named field in the passed class
	 * @param clazz The class to inspect
	 * @param fieldName The field name being looked for
	 * @param throwEx If true, and the field is not found, will throw a RuntimeException
	 * @return the named field or null if it was not found
	 */
	public static Field getField(Class<?> clazz, String fieldName, boolean throwEx) {
		Field f = null;
		if(fieldName==null) throw new IllegalArgumentException("Passed field name was null");
		Class<?> tclass = nvl(clazz, "Passed class was null");
		while(tclass!=null && !Object.class.equals(tclass)) {
			try { f = tclass.getDeclaredField(fieldName); } catch (Exception e) {}
			if(f!=null) break;
			try { f = tclass.getField(fieldName); } catch (Exception e) {}
			tclass = tclass.getSuperclass();
		}
		if(f==null && throwEx) {
			throw new RuntimeException("The field [" + fieldName + "] was not found in the class [" + clazz.getName() + "]", new Throwable());
		}
		return f;
	}
	
	/**
	 * Finds all annotated fields in a class 
	 * @param clazz The class to inspect
	 * @param annotation The annotation to match against
	 * @return an array of annotated fields
	 */
	public static Field[] getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
		Map<String, Field> fields = new HashMap<String, Field>();
		if(annotation==null) throw new IllegalArgumentException("Passed annotation was null");
		Class<?> tclass = nvl(clazz, "Passed class was null");
		LinkedList<Class<?>> hierarchy = new LinkedList<Class<?>>(); 
		while(tclass!=null && !Object.class.equals(tclass)) {
			hierarchy.addFirst(tclass);
			tclass = tclass.getSuperclass();
		}
		for(Class<?> c: hierarchy) {
			for(Field f: c.getFields()) {
				if(f.getAnnotation(annotation)!=null) fields.put(f.getName(), f);
			}
			for(Field f: c.getDeclaredFields()) {
				if(f.getAnnotation(annotation)!=null) fields.put(f.getName(), f);
			}			
		}
		return fields.values().toArray(new Field[fields.size()]);
	}

	/**
	 * Returns the annotation instance from the passed annotation array of the passed annotation type.
	 * @param target The type of the annotation to return
	 * @param src An array of annotations
	 * @return The matched annotation or null if no match is made
	 */
	public static Annotation getAnnotation(Class<? extends Annotation> target,  Annotation...src) {
		if(target==null || src==null || src.length<1) return null;
		for(Annotation ann: src) {
			if(ann==null) continue;
			if(target.equals(ann.annotationType())) return ann;
		}
		return null;
	}
	
	/**
	 * Scans a method's annotations and returns true if there is at least one match to the passed annotations.
	 * @param method The method to scan
	 * @param inherrited Indicates that super classes of the method's declaring class should be scanned if not found in the passed method 
	 * @param annotations An array of annotation types to match against
	 * @return true if one match is found, false if no matches are found.
	 */
	public static boolean isAnnotatedWithAny(Method method, boolean inherrited, Class<? extends Annotation>...annotations) {
		if(method==null || annotations==null) return false;
		for(Class<? extends Annotation> ann: annotations) {
			if(ann==null) continue;
			if(getAnnotationFromMethod(method, ann, inherrited)!=null) return true;
		}		
		return false;
	}
	
	/**
	 * Scans a method's annotations and returns true if there is at least one match to the passed annotations.
	 * @param method The method to scan
	 * @param inherrited Indicates that super classes of the method's declaring class should be scanned if not found in the passed method 
	 * @param annotations A set of annotation types to match against
	 * @return true if one match is found, false if no matches are found.
	 */
	@SuppressWarnings("unchecked")
	public static boolean isAnnotatedWithAny(Method method, boolean inherrited, Set<Class<? extends Annotation>> annotations) {
		if(annotations==null || annotations.size() < 1) return false;
		return isAnnotatedWithAny(method, inherrited, annotations.toArray(new Class[annotations.size()]));
	}
	
	/**
	 * Scans a method's annotations and returns true if there is a match to all of the passed annotations.
	 * @param method The method to scan
	 * @param inherrited Indicates that super classes of the method's declaring class should be scanned if not found in the passed method 
	 * @param annotations An array of annotation types to match against. Null members of the array are ignored.
	 * @return true if one match is found, false if no matches are found.
	 */
	public static boolean isAnnotatedWithAll(Method method, boolean inherrited, Class<? extends Annotation>...annotations) {
		if(method==null || annotations==null) return false;
		for(Class<? extends Annotation> ann: annotations) {
			if(ann==null) continue;
			if(getAnnotationFromMethod(method, ann, inherrited)==null) return false;
		}		
		return true;
	}
	
	/**
	 * Scans a method's annotations and returns true if there is a match to all of the passed annotations.
	 * @param method The method to scan
	 * @param inherrited Indicates that super classes of the method's declaring class should be scanned if not found in the passed method 
	 * @param annotations An set of annotation types to match against. Null members of the array are ignored.
	 * @return true if one match is found, false if no matches are found.
	 */
	@SuppressWarnings("unchecked")
	public static boolean isAnnotatedWithAll(Method method, boolean inherrited, Set<Class<? extends Annotation>> annotations) {
		if(annotations==null || annotations.size() < 1) return false;
		return isAnnotatedWithAll(method, inherrited, annotations.toArray(new Class[annotations.size()]));
	}
	
	
	/**
	 * Retrieves the specified annotation instance from the passed method 
	 * @param method The method to retrieve the annotation from
	 * @param annotationType The annotation type to retrieve
	 * @param inherrited Indicates that super classes of the method's declaring class should be scanned if not found in the passed method 
	 * @return A matching annotation or null if a match is not found.
	 */
	public static Annotation getAnnotationFromMethod(Method method, Class<? extends Annotation> annotationType, boolean inherrited) {
		Annotation matchedAnnotation = getAnnotation(annotationType, getAllAnnotations(method, !inherrited));
		if(matchedAnnotation==null && inherrited) {
			Class<?> pClass = method.getDeclaringClass();
			Method pMethod = null;
			while(pClass!=null && ( ! Object.class.equals((pClass=pClass.getSuperclass())))) {
				try {
					pMethod = pClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
					matchedAnnotation = getAnnotation(annotationType, getAllAnnotations(pMethod, !inherrited));
					if(matchedAnnotation!=null) break;
				} catch (Exception e) {}
			}
		}
		return matchedAnnotation;
	}
	
	/**
	 * Returns an array of all declared and optionally non-declared annotations associated to a method.
	 * @param method The method to retrieve annotations for
	 * @boolean declared 
	 * @return An array of located annotations
	 */
	public static Annotation[] getAllAnnotations(Method method, boolean declared) {
		if(method==null) return new Annotation[]{};
		Set<Annotation> annotations = new HashSet<Annotation>();		
		Collections.addAll(annotations, method.getDeclaredAnnotations());
		if(!declared) Collections.addAll(annotations, method.getAnnotations());
		return annotations.toArray(new Annotation[annotations.size()]);
	}
	
	/**
	 * Returns an array of annotation types for all declared and non-declared annotations associated to a method.
	 * @param method The method to retrieve annotation types for
	 * @return An array of located annotation types
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends Annotation>[] getAllAnnotationTypes(Method method, boolean inherrited) {
		if(method==null) return new Class[0];
		Set<Class<? extends Annotation>> types = new HashSet<Class<? extends Annotation>>();
		for(Annotation ann: getAllAnnotations(method, inherrited)) {
			types.add(ann.annotationType());
		}
		return types.toArray(new Class[types.size()]);
	}
	
	/**
	 * Determines if the passed modifier matches any of the passed modifier bits.
	 * @param modifier The modifier to test
	 * @param anyModifiers An array of modifier bits as defined in <code>Modifiers</code>.
	 * @return true if there is at least one match, or <code>anyModifiers</code> is null or zero length. False otherwise.
	 */
	public static boolean isModifierAnyOf(int modifier, int...anyModifiers) {
		if(anyModifiers==null || anyModifiers.length < 1) return true;
		return Modifiers.isAnyOf(modifier, anyModifiers);
	}
	
	/**
	 * Determines if the passed modifier matches all of the passed modifier bits.
	 * @param modifier The modifier to test
	 * @param allModifiers An array of modifier bits as defined in <code>Modifiers</code>.
	 * @return true if all modifiers match or <code>anyModifiers</code> is null or zero length. False otherwise.
	 */
	public static boolean isModifierAllOf(int modifier, int...allModifiers) {
		if(allModifiers==null || allModifiers.length < 1) return true;
		return Modifiers.isAllOf(modifier, allModifiers);
	}
	
	
	/**
	 * <p>Title: FilterBuilder</p>
	 * <p>Description: Base abstract filter builder class</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 * @version $LastChangedRevision$
	 * $HeadURL$
	 * $Id$
	 * @param <K>
	 */
	public abstract static class FilterBuilder<K> implements IFilterBuilder<K> {
		//=====================================
		//	Generic Matchers
		//=====================================		
		boolean inherrited = false; // 1
		Pattern namePattern = null; // 1
		Set<Class<? extends Annotation>> allAnnotations = new HashSet<Class<? extends Annotation>>(); //1
		Set<Class<? extends Annotation>> anyAnnotations = new HashSet<Class<? extends Annotation>>(); //1
		int[] allModifiers = null; //1
		int[] anyModifiers = null; //1
		Access asRestrictive = null; //1
		Access asOpen = null; //1
		Access asAccess = null; //1
		//=====================================
		//	Ctor or Method Matchers
		//=====================================
		Class<?>[] parameterTypes = null; //1
		Class<?>[] exceptionTypes = null; //1
		int argumentCount = -1; //1
		//=====================================
		//	Method Matchers
		//=====================================
		Class<?> returnType = null; //1
		Accessor accessor = null; //1

		//=====================================
		//	Ctor Matchers
		//=====================================

		//=====================================
		//	Field Matchers
		//=====================================
		Class<?> fieldType = null; // 1
		
		
		//==========================================================
		//	Generic Conditionals
		//==========================================================
		
		
		/**
		 * Specifies a group of modifiers of which the class or member must match at least one.
		 * @param modifiers An array of modifiers
		 * @return the modified filter
		 */
		public IFilterBuilder<K> setModifiersAny(int...modifiers) {
			if(modifiers!=null && modifiers.length > 0) {
				anyModifiers=modifiers;
			}
			return this;
		}
		
		/**
		 * Clears the specified <code>any</code> modifiers.
		 * @return the modified filter
		 */
		public IFilterBuilder<K> clearModifiersAny() {
			anyModifiers=null;
			return this;
		}
		
		/**
		 * Specifies a group of modifiers of which the class or member must match all.
		 * @param modifiers An array of modifiers
		 * @return the modified filter
		 */
		public IFilterBuilder<K> setModifiersAll(int...modifiers) {
			if(modifiers!=null && modifiers.length > 0) {
				allModifiers=modifiers;
			}
			return this;
		}
		
		/**
		 * Clears the specified <code>all</code> modifiers.
		 * @return the modified filter
		 */
		public IFilterBuilder<K> clearModifiersAll() {
			allModifiers=null;
			return this;
		}
		
		
		
		/**
		 * Specifies that matching methods must be at least as open as the passed access and clears any other set access filters.
		 * @param access The access level that the matching method should be equal to or greater than.
		 * @return the modified filter
		 */
		public IFilterBuilder<K> setAccessAtLeast(Access access) {
			asRestrictive = access;
			asOpen = null;
			asAccess = null;
			return this;
		}
		
		/**
		 * Clears the finders minimum access specifier
		 * @return the modified filter
		 */
		public IFilterBuilder<K> clearAccessAtLeast() {
			asRestrictive = null;
			return this;
		}
		
		/**
		 * Specifies that matching methods must be at least as closed as the passed access  and clears any other set access filters.
		 * @param access The access level that the matching method should be equal to or less than.
		 * @return the modified filter
		 */		
		public IFilterBuilder<K> setAccessAtMost(Access access) {
			asOpen = access;
			asRestrictive = null;
			asAccess = null;
			return this;			
		}

		/**
		 * Clears the finders maximum access specifier
		 * @return the modified filter
		 */		
		public IFilterBuilder<K> clearAccessAtMost() {
			asOpen = null;
			return this;
		}
		
		/**
		 * Specifies that matching methods must be of the specified access  and clears any other set access filters.
		 * @param access The access level that the matching method should be.
		 * @return the modified filter
		 */				
		public IFilterBuilder<K> setAccessIs(Access access) {
			asAccess = access;
			asOpen = null;
			asRestrictive = null;
			return this;
		}
		
		/**
		 * Clears the finders matching access specifier 
		 * @return the modified filter
		 */
		public IFilterBuilder<K> clearAccessIs() {
			asAccess = null;
			return this;
		}
		
		//========================================================
		//	Ctor or Method Conditionals
		//========================================================

		/**
		 * Specifies that the filter should only keep members that have the passed parameter types.
		 * @param params The parameter types to match.
		 * @return the modified filter.
		 */
		public IFilterBuilder<K> setParameterTypes(Class<?>[] params) {
			if(params!=null && params.length > 0) {
				parameterTypes = params;
			}
			return this;
		}
		
		/**
		 * Clears the parameter type filter.
		 * @return the modified filter.
		 */
		public IFilterBuilder<K> clearParameterTypes() {
			parameterTypes = null;
			return this;
		}
		
		/**
		 * Specifies that the filter should only keep members that throw the passed exception types.
		 * @param params The exception types to match.
		 * @return the modified filter.
		 */
		public IFilterBuilder<K> setExceptionTypes(Class<?>[] params) {
			if(params!=null && params.length > 0) {
				exceptionTypes = params;
			}
			return this;
		}
		
		/**
		 * Clears the exception type filter.
		 * @return the modified filter.
		 */
		public IFilterBuilder<K> clearExceptionTypes() {
			exceptionTypes = null;
			return this;
		}
		
		/**
		 * Directs the filter to match methods by the number of arguments.
		 * @param argCount The number of arguments the method should have.
		 * @return The modified filter.
		 */
		public IFilterBuilder<K> setArgumentCount(int argCount) {
			if(argCount >= 0) {
				argumentCount = argCount;
			}
			return this;
		}						
		
		/**
		 * Clears the argument count specification
		 * @return the modified filter
		 */
		public IFilterBuilder<K> clearArgumentCount() {
			argumentCount = -1;
			return this;
		}
		//=============================================================
		//  Method Conditionals
		//=============================================================
		
		/**
		 * Specifies the accessor method type to match
		 * @param accessor The accessor type to match on
		 * @return the modified filter
		 */
		public IFilterBuilder<K> setAccessorType(Accessor accessor) {
			this.accessor = accessor;
			return this;
		}
		
		/**
		 * Clears the accessor type specification.
		 * @return the modified filter
		 */
		public IFilterBuilder<K> clearAccessorType() {
			this.accessor = null;
			return this;			
		}
		
		/**
		 * Directs the filter to match methods by the return type
		 * @param returnType The return type to match against.
		 * @return The modified filter.
		 */
		public IFilterBuilder<K> setReturnType(Class<?> returnType) {
			if(returnType!=null) {
				this.returnType = returnType;
			}
			return this;
		}
		
		/**
		 * Directs the filter to match methods by the return type
		 * @param returnType The return type name to match against.
		 * @return The modified filter.
		 */
		public IFilterBuilder<K> returnType(String returnType) {
			if(returnType!=null) {
				try {
					this.returnType = Class.forName(returnType);
				} catch (Exception e) {
					throw new RuntimeException("Could not classload the type [" + returnType + "]");
					
				}
			}
			return this;
		}
		
		
		/**
		 * Clears the return type specification
		 * @return The modified filter.
		 */
		public IFilterBuilder<K> clearReturnType() {
			this.returnType = null;
			return this;
		}
		
		//=============================================================
		//  Ctor Conditionals
		//=============================================================
		
		
		//=============================================================
		//  Field Conditionals
		//=============================================================
		/** 
		 * Specifies the type of the field to match.
		 * @param fieldType The type of the field.
		 * @return the modified builder.
		 */
		public IFilterBuilder<K> setFieldType(Class<?> fieldType) {
			this.fieldType = fieldType;
			return this;
		}

		/**
		 * Clears the field type specification.
		 * @returnthe modified builder.
		 */
		public IFilterBuilder<K> clearFieldType() {
			this.fieldType = null;
			return this;
		}
		
		//=============================================================
		//  Ops
		//=============================================================

		
		/**
		 * Determines if the passed modifier is a match for any defined access modifier filter.
		 * @param modifier The modifier for the member 
		 * @return true if a match is found or no filters are defined.
		 */
		protected boolean isMatchedAccess(int modifier) {
			Access access = Access.fromModifiers(modifier);
			if(asOpen!=null) {
				return asOpen.asAccessible(access);
			} else if(asRestrictive!=null) {
				return asRestrictive.asAccessible(access);
			} else if(asAccess!=null) {
				return asAccess.equals(access);
			} else {
				return true;
			}
		}
		
		/**
		 * Filter to be implemented by concrete filter implementations.
		 * @param inspectedObject the object being inspected by the filter.
		 * @return true if the inspected object should be included, false if not.
		 */
		protected boolean doFilter(ClassOrMember inspectedObject) {
			return false;
		}
		
		/**
		 * Generates a working list of the objects that will be filtered.
		 * @param inspectedClass The class the objects will be generated from.
		 * @param inherrited If false, only declared members will be included, otherwise all members will be included.
		 * @return a set of objects to be inspected.
		 */
		protected abstract Set<K> filterSet(ClassOrMember inspectedClass, boolean inherrited);
		
		/**
		 * Creates a unique string key from an object's name and attributes.
		 * @param inspectedObject The object to generate a key for,
		 * @return A string key
		 */
		protected String encodeObject(ClassOrMember inspectedObject) {
			return inspectedObject.encode();
		}

		
		/**
		 * Executes the build method filter.
		 * @param clazz The class to execute the method filter on.
		 * @return An array of matching methods.
		 */
		//===========================================================
//	     PUSH DOWN TO CLASS, METHOD, FIELD and CTOR IMPLS					
	//===========================================================					
		
//		public Set<K> filter(Class<?> clazz) {
//			Set<K> filterSet = new HashSet<K>();
//			if(clazz==null) return filterSet;
//			try {				
//				Map<String, K> allMethods = new HashMap<String, K>();
//				
//				for(K inspectedObject: filterSet(clazz)) {
//					if(!doFilter(inspectedObject)) continue;
//					if(anyAnnotations.size() > 0) if( ! isAnnotatedWithAny(method, inherrited,  anyAnnotations)) continue;
//					if(allAnnotations.size() > 0) if( ! isAnnotatedWithAll(method, inherrited,  allAnnotations)) continue;
//					int mod = method.getModifiers();
//					if(!isMatchedAccess(mod)) continue;
//					if(!isModifierAnyOf(mod)) continue;
//					if(!isModifierAllOf(mod)) continue;
//					allMethods.put(encodeObject(method), method);
//				}				
//				if(inherrited) {
//					for(Method method: clazz.getMethods()) {
//						Class<?>[] paramTypes = method.getParameterTypes();
//						if(anyAnnotations.size() > 0) if( ! isAnnotatedWithAny(method, inherrited,  anyAnnotations)) continue;
//						if(allAnnotations.size() > 0) if( ! isAnnotatedWithAll(method, inherrited,  allAnnotations)) continue;
//						int mod = method.getModifiers();
//						if(!isMatchedAccess(mod)) continue;
//						if(!isModifierAnyOf(mod)) continue;
//						if(!isModifierAllOf(mod)) continue;						
//						String key = encodeObject(method);
//						if(!allMethods.containsKey(key)) {
//							allMethods.put(key, method);
//						}						
//					}
//				}
////				methods = allMethods.values().toArray(new Method[allMethods.size()]);
////				return methods;
//			} catch (Exception e) {
//				throw new RuntimeException("Unexpected exception in filter search", e);
//			}
//		}
		
		
		
		
		private FilterBuilder() {			
		}
		
		/**
		 * Creates a new builder.
		 * @return a new builder
		 */
		public static IFilterBuilder newFilter(Object targetObject) {
			if(targetObject==null) throw new RuntimeException("Target Object Was Null");
			if(Method.class.equals(targetObject.getClass())) {
				return new MethodFilterBuilder();
			}
			return null;
		}
		
		/**
		 * Directs the filter to match methods by all the applied annotations
		 * @param annotations The annotations that should be applied to matched methods
		 * @return the modified filter.
		 */
		public FilterBuilder<K> setAnnotatedWithAll(Class<? extends Annotation>...annotations) {
			if(annotations !=null && annotations.length > 0) {
				for(Class<? extends Annotation> an: annotations) {
					if(an!=null) {
						allAnnotations.add(an);
					}
				}
			}
			return this;
		}
		
		/**
		 * Directs the filter to match methods by the any of the applied annotations
		 * @param annotations The annotations that should be applied to matched methods where any match is acceptable
		 * @return the modified filter.
		 */
		public FilterBuilder<K> setAnnotatedWithAny(Class<? extends Annotation>...annotations) {
			if(annotations !=null && annotations.length > 0) {
				for(Class<? extends Annotation> an: annotations) {
					if(an!=null) {
						anyAnnotations.add(an);
					}
				}
			}
			return this;
		}
		
		/**
		 * Clears the match any annotation specification.
		 * @return the modified filter
		 */
		public FilterBuilder<K> clearAnnotatedWithAny() {
			anyAnnotations.clear();
			return this;
		}
		
		/**
		 * Clears the match all annotation specification.
		 * @return the modified filter
		 */
		public FilterBuilder<K> clearAnnotatedWithAll() {
			allAnnotations.clear();
			return this;
		}


		
		/**
		 * Directs the filter to match names according to the passed regular expression.
		 * @param regex The pattern to match the member or class name to.
		 * @param caseSensitive If true, the match will be case sensitive.
		 * @return the modified filter
		 */
		public FilterBuilder<K> setName(String regex, boolean caseSensitive) {
			if(regex!=null) {
				if(caseSensitive) {
					namePattern = Pattern.compile(regex);
				} else {
					namePattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
				}
			}
			return this;
		}
		
		/**
		 * Directs the filter to match object names according to the passed case sensitive regular expression.
		 * @param regex The pattern to match the object name to.
		 * @return the modified filter
		 */
		public FilterBuilder<K> setName(String regex) {
			return setName(regex, true);
		}
		
		/**
		 * Clears the name matcher
		 * @return the modified filter
		 */
		public FilterBuilder<K> clearName() {
			namePattern = null;
			return this;
		}
		
		/**
		 * Defines if the class hierarchy should be climbed looking for matching methods or not.
		 * @param b if false, only methods in the current child class will be returned. If true, the class hierarchy will be climbed.
		 * @return The modified builder.
		 */
		public FilterBuilder<K> inherrited(boolean b) {
			inherrited = b;
			return this;
		}
		
	}
	
	/**
	 * <p>Title: ParameterizedMemberFilterBuilder</p>
	 * <p>Description: Base filter builder for methods, constructors and generic classes</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 * @version $LastChangedRevision$
	 * $HeadURL$
	 * $Id$
	 */
	public abstract static class ParameterizedMemberFilterBuilder<K> extends FilterBuilder<K> {
		

		
		
		
		/**
		 * Filter to be implemented by concrete filter implementations.
		 * @param inspectedObject the object being inspected by the filter.
		 * @return true if the inspected object should be included, false if not.
		 */
//		@Override
//		protected boolean doFilter(K inspectedObject) {						
//			ClassOrMember com = new ClassOrMember(inspectedObject);
//			Class<?>[] pTypes = com.getParameterTypes();
//			Class<?>[] eTypes = com.getExceptionTypes();
//			if(argumentCount != -1 && pTypes.length!=argumentCount) return false;
//			if(parameterTypes != null && classArraysEqual(parameterTypes, pTypes)) return false;
//			if(exceptionTypes != null && classArraysEqual(parameterTypes, eTypes)) return false;
//			return super.doFilter(inspectedObject);			
//		}
	}
	
	/**
	 * <p>Title: CtorFilterBuilder</p>
	 * <p>Description: A builder and executor to search for constructors on a class by various search options. </p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 * @version $LastChangedRevision$
	 * $HeadURL$
	 * $Id$
	 */
	public static class CtorFilterBuilder<K> extends ParameterizedMemberFilterBuilder<K> {
		/**
		 * Generates a set of possible match candidate constructors for the passed class.
		 * @param inspectedClass The class the constructors will be reflected from.
		 * @param inherrited If false, only declared members will be included, otherwise all members will be included.
		 * @return a set of constructors
		 */
		protected Set<K> filterSet(ClassOrMember inspectedClass, boolean inherrited) {
			Set<K> set = new HashSet<K>();
			for(Constructor ctor: inherrited ? inspectedClass.getAllCtors() : inspectedClass.getDeclaredConstructors()) {
				set.add((K)ctor);
			}
			return set;
		}

	}
	
	/**
	 * <p>Title: MethodFilterBuilder</p>
	 * <p>Description: A builder and executor to search for methods on a class by various search options. </p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 * @version $LastChangedRevision$
	 * $HeadURL$
	 * $Id$
	 */
	public static class MethodFilterBuilder<K> extends ParameterizedMemberFilterBuilder<K> {
		/**
		 * Generates a set of possible match candidate methods for the passed class.
		 * @param inspectedClass The class the methods will be reflected from.
		 * @param inherrited If false, only declared members will be included, otherwise all members will be included.
		 * @return a set of methods
		 */
		protected Set<K> filterSet(ClassOrMember inspectedClass, boolean inherrited) {
			Set<K> set = new HashSet<K>();
			for(Method m: inherrited ? inspectedClass.getAllMethods() : inspectedClass.getDeclaredMethods()) {
				set.add((K)m);
			}
			return set;
		}

		
		
		/**
		 * Filter to be implemented by concrete filter implementations.
		 * @param inspectedObject the object being inspected by the filter.
		 * @return true if the inspected object should be included, false if not.
		 */
//		@Override
//		protected boolean doFilter(Object inspectedObject) {
//			Method method = null;
//			if(inspectedObject instanceof Method) {
//				method = (Method)inspectedObject;
//			} else {
//				return false;
//			}
//			Class<?> paramTypes[] = method.getParameterTypes();			
//			
//			if(returnType != null && !method.getReturnType().equals(returnType)) return false;
//			if(namePattern != null && !namePattern.matcher(method.getName()).matches()) return false;
//			return true;			
//		}
	}
	
	/**
	 * <p>Title: FieldFilterBuilder</p>
	 * <p>Description: A builder and executor to search for fields on a class by various search options. </p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 * @version $LastChangedRevision$
	 * $HeadURL$
	 * $Id$
	 */
	public static class FieldFilterBuilder<K> extends FilterBuilder<K> {
		/**
		 * Generates a set of possible match candidate fields for the passed class.
		 * @param inspectedClass The class the fields will be reflected from.
		 * @param inherrited If false, only declared members will be included, otherwise all members will be included.
		 * @return a set of fields
		 */
		protected Set<K> filterSet(ClassOrMember inspectedClass, boolean inherrited) {
			Set<K> set = new HashSet<K>();
			for(Field f: inherrited ? inspectedClass.getAllFields() : inspectedClass.getDeclaredFields()) {
				set.add((K)f);
			}
			return set;
		}

	}	
	
	/**
	 * Returns the instance of the specified annotation from the indexed parameter in the passed Constructor or Method.
	 * @param methodOrCtor A constructor or method. If a Field is passed, will return null.
	 * @param index The index of the parameter.
	 * @param annotation The annotation type to look for.
	 * @return The requested annotation or null if it was not found.
	 */
	public static <T extends Annotation> T getParameterAnnotation(AccessibleObject methodOrCtor, int index, Class<T> annotation) {
		try {
			if(methodOrCtor==null || annotation==null || methodOrCtor instanceof Field) {
				return null;
			}
			for(Annotation ann : (methodOrCtor instanceof Constructor<?>) ? ((Constructor<?>)methodOrCtor).getParameterAnnotations()[index] : ((Method)methodOrCtor).getParameterAnnotations()[index]) {
				if(ann.annotationType().equals(annotation)) return (T) ann;
			}
		} catch (Exception e) {}
		return null;		
	}
	
	/**
	 * Returns all the located methods in the passed class that are annotated with the passed annotation.
	 * @param clazz the class to inspect
	 * @param annotation the annotation to search for
	 * @param climb If true, the class hierarchy will be climbed to find methods in parent classes.
	 * @return a set of methods which may be empty if no annotated methods are found or the passed class or annotation is null.
	 */	
	public static Set<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotation, boolean climb) {
		if(!climb) {
			return getAnnotatedMethods(clazz, annotation);
		} else {
			Set<Method> methods = new HashSet<Method>();
			Map<String, Method> methodMap = new HashMap<String, Method>();
			
			Class<?> current = clazz;
			while(!current.equals(Object.class) && current!=null) {
				for(Method m: getAnnotatedMethods(clazz, annotation)) {
					if(!methodMap.containsKey(m.toGenericString())) {
						methodMap.put(m.toGenericString(), m);
					}
				}
				current = current.getSuperclass();
			}
			methods.addAll(methodMap.values());
			return methods;			
		}		
	}
	
	/**
	 * Returns all the located methods in the passed class that are annotated with any of the passed annotations.
	 * @param clazz the class to inspect
	 * @param climb If true, the class hierarchy will be climbed to find methods in parent classes.
	 * @param annotations the annotations to search for
	 * @return a set of methods which may be empty if no annotated methods are found or the passed class or annotation is null.
	 */	
	public static Set<Method> getMethodsAnnotatedWithAny(Class<?> clazz, boolean climb, Class<? extends Annotation>...annotations) {
		if(annotations==null || annotations.length<1) return Collections.emptySet();
		Set<Method> methods = new HashSet<Method>();
		Map<String, Method> methodMap = new HashMap<String, Method>();
		
		for(Class<? extends Annotation> annotation: annotations) {
			if(!climb) {
				for(Method m: getAnnotatedMethods(clazz, annotation)) {
					if(!methodMap.containsKey(m.toGenericString())) {
						methodMap.put(m.toGenericString(), m);
					}					
				}
			} else {				
				Class<?> current = clazz;
				while(!current.equals(Object.class) && current!=null) {
					for(Method m: getAnnotatedMethods(clazz, annotation)) {
						if(!methodMap.containsKey(m.toGenericString())) {
							methodMap.put(m.toGenericString(), m);
						}
					}
					current = current.getSuperclass();
				}
			}
		}
		methods.addAll(methodMap.values());
		return methods;					
	}
	
	
	/**
	 * Determines the dimensions of the passed array class.
	 * eg. <b><code>String[][][].class</code></b> would return 3.
	 * If the class is not an array, will return 0.
	 * @param clazz The array class to test
	 * @return The dimensions of the array class. 
	 */
	public static int getArrayTypeDimension(Class<?> clazz) {
		if(!nvl(clazz, "Passed class was null").isArray()) {
			return 0;
		}
		Class<?> cl = clazz.getComponentType();
		int i = 1;
		while(cl.isArray()) {
			cl = cl.getComponentType();
			i++;
		}
		return i;
	}
	
	/**
	 * Determines if the passed class is an array of a primitive type (of any dimension)
	 * @param clazz The type to test
	 * @return true if the passed class is an array of a primitive type (of any dimension), false otherwise
	 */
	public static boolean isPrimitiveArray(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null", new Throwable());
		if(!clazz.isArray()) return false;
		while(true) {
			clazz = clazz.getComponentType();
			if(!clazz.isArray()) {
				return clazz.isPrimitive();
			}
		}
	}

	/**
	 * Determines the root type of an array type.
	 * @param clazz The type to test
	 * @return The root type of an array type or the passed type if it is not an array type.
	 */
	public static Class<?> getCoreType(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null", new Throwable());
		if(!clazz.isArray()) return clazz;
		while(true) {
			clazz = clazz.getComponentType();
			if(!clazz.isArray()) {
				return clazz;
			}
		}
		
	}
	
	/**
	 * Returns all the located constructors in the passed class that are annotated with the passed annotation.
	 * @param clazz the class to inspect
	 * @param annotation the annotation to search for
	 * @param climb If true, the class hierarchy will be climbed to find constructors in parent classes.
	 * @return a set of constructors which may be empty if no annotated constructors are found or the passed class or annotation is null.
	 */	
	public static Set<Constructor<?>> getAnnotatedCtors(Class<?> clazz, Class<? extends Annotation> annotation, boolean climb) {
		if(!climb) {
			return getAnnotatedCtors(clazz, annotation);
		} else {
			Set<Constructor<?>> ctors = new HashSet<Constructor<?>>();
			Map<String, Constructor<?>> ctorMap = new HashMap<String, Constructor<?>>();			
			Class<?> current = clazz;
			while(!current.equals(Object.class) && current!=null) {
				for(Constructor<?> ctor: getAnnotatedCtors(clazz, annotation)) {
					if(!ctorMap.containsKey(ctor.toGenericString())) {
						ctorMap.put(ctor.toGenericString(), ctor);
					}
				}
				current = current.getSuperclass();
			}
			ctors.addAll(ctorMap.values());
			return ctors;			
		}		
	}
	
	
	/**
	 * Returns all the located methods in the passed class that are annotated with the passed annotation.
	 * @param clazz the class to inspect
	 * @param annotation the annotation to search for
	 * @return a set of methods which may be empty if no annotated methods are found or the passed class or annotation is null.
	 */
	public static Set<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotation) {
		if(clazz==null || annotation==null) return Collections.emptySet();
		Set<Method> methods = new HashSet<Method>();
		Map<String, Method> methodMap = new HashMap<String, Method>();
		for(Method m: clazz.getMethods()) {
			Annotation ann = m.getAnnotation(annotation);
			if(ann!=null) {
				methodMap.put(m.toGenericString(), m);
			}
		}
		for(Method m: clazz.getDeclaredMethods()) {
			Annotation ann = m.getAnnotation(annotation);
			if(ann!=null) {
				methodMap.put(m.toGenericString(), m);
			}
		}
		methods.addAll(methodMap.values());
		return methods;
	}
	
	/**
	 * Returns all the located constructors in the passed class that are annotated with the passed annotation.
	 * @param clazz the class to inspect
	 * @param annotation the annotation to search for
	 * @return a set of constructors which may be empty if no annotated constructors are found or the passed class or annotation is null.
	 */
	public static Set<Constructor<?>> getAnnotatedCtors(Class<?> clazz, Class<? extends Annotation> annotation) {
		if(clazz==null || annotation==null) return Collections.emptySet();
		Set<Constructor<?>> ctors = new HashSet<Constructor<?>>();
		Map<String, Constructor<?>> ctorMap = new HashMap<String, Constructor<?>>();
		for(Constructor<?> ctor: clazz.getConstructors()) {
			Annotation ann = ctor.getAnnotation(annotation);
			if(ann!=null) {
				ctorMap.put(ctor.toGenericString(), ctor);
			}
		}
		for(Constructor<?> ctor: clazz.getDeclaredConstructors()) {
			Annotation ann = ctor.getAnnotation(annotation);
			if(ann!=null) {
				ctorMap.put(ctor.toGenericString(), ctor);
			}
		}
		ctors.addAll(ctorMap.values());
		return ctors;
	}
	
	
	/**
	 * Returns the getter/setter pair for a class attribute name
	 * @param clazz The class to inspect
	 * @param attributeName The attribute name
	 * @return an array of methods containing the [0] getter and [1] setter. Either or both may be null. 
	 */
	public static Method[] getAttributeMethodPair(Class<?> clazz, String attributeName) {
		return new Method[]{
				getAttributeMethod(true, clazz, attributeName),
				getAttributeMethod(false, clazz, attributeName)
		};
	}
		
	/**
	 * Finds an attribute accessor method for the passed attribute name
	 * @param getter if true, looks for the getter, otherwise gets the setter
	 * @param clazz The class to inspect
	 * @param attributeName The attribute name
	 * @return the accessor method or null if one cannot be found.
	 */
	public static Method getAttributeMethod(boolean getter, Class<?> clazz, String attributeName) {
		String methodName = getter ? "get" : "set" + nvl(attributeName, "Attribute name was null");
		Class<?> tclass = nvl(clazz, "Passed class was null");
		while(tclass!=null && !Object.class.equals(tclass)) {
			for(Method m: tclass.getDeclaredMethods()) {
				if(m.getName().equals(methodName)) {
					if(getter) {
						if(m.getParameterTypes().length==0 && !void.class.equals(m.getReturnType())) {
							return m;
						}
					} else {
						if(m.getParameterTypes().length==1 && void.class.equals(m.getReturnType())) {
							return m;
						}						
					}
				}
			}
			for(Method m: tclass.getMethods()) {
				if(m.getName().equals(methodName)) {
					if(getter) {
						if(m.getParameterTypes().length==0 && !void.class.equals(m.getReturnType())) {
							return m;
						}
					} else {
						if(m.getParameterTypes().length==1 && void.class.equals(m.getReturnType())) {
							return m;
						}						
					}
				}
			}			
			tclass = tclass.getSuperclass();
		}
		return null;
	}
	
	
	/**
	 * Finds the opposing attribute accessor for the passed method.  
	 * @param accessor The method to get the opposer for
	 * @return the getter if a setter was passed or the setter if the getter was passed. Will be null if an opposer was not found.
	 */
	public static Method getOpposer(Method accessor) {
		String methodName = nvl(accessor, "The passed method was null").getName(); 
		if(!methodName.startsWith("get") && !methodName.startsWith("set")) {
			throw new IllegalArgumentException("The method [" + accessor.getDeclaringClass().getName() + "." + methodName + "] is not a getter or a setter");
		}
		boolean getter = nvl(accessor, "The passed method was null").getName().startsWith("get");
		if(getter) {
			if(accessor.getParameterTypes().length!=0 && !void.class.equals(accessor.getReturnType())) {
				throw new IllegalArgumentException("The method [" + accessor.getDeclaringClass().getName() + "." + methodName + "] is not a compliant getter");
			}
		} else {
			if(accessor.getParameterTypes().length==1 && void.class.equals(accessor.getReturnType())) {
				throw new IllegalArgumentException("The method [" + accessor.getDeclaringClass().getName() + "." + methodName + "] is not a compliant setter");
			}
		}
		String attrName = nvl(accessor, "The passed method was null").getName().substring(3);
		return getAttributeMethod(!getter, accessor.getDeclaringClass(), attrName);
	}
	
	
	public static Annotation getAnnotation(Class<?> type, Class<? extends Annotation> ann) {
		return type.getAnnotation(ann);
	}
	
	public static Annotation getAnnotation(Class<?> type, AnnotatedElement element, Class<? extends Annotation> ann) {
		return type.getAnnotation(ann);
	}

	
}



































