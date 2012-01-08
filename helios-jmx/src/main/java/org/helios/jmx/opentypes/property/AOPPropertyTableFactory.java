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

import java.beans.Introspector;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Inherited;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

import org.helios.helpers.ClassHelper;
import org.helios.jmx.opentypes.annotations.OpenTypeAttribute;

/**
 * <p>Title: AOPPropertyTableFactory</p>
 * <p>Description: Generates a dynamic byte code supported PropertyTable.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.opentypes.property.AOPPropertyTableFactory</code></p>
 */

public class AOPPropertyTableFactory {
	/** Generated class cache keyed by wrapped class */
	private static final Map<Class<?>, Class<?>> classCache = new ConcurrentHashMap<Class<?>, Class<?>>();
	
	protected static Class<?> generateAccessors(Object instance) throws NotFoundException, CannotCompileException, IOException {
		Class<?> clazz = instance.getClass();
		Set<Class<?>> accessors = new HashSet<Class<?>>();
		Field[] propertyFields = ClassHelper.getAnnotatedFields(clazz, OpenTypeAttribute.class);
		Set<Method> propertyMethods = ClassHelper.getAnnotatedMethods(clazz, OpenTypeAttribute.class);
		
		ClassPool classPool = ClassPool.getDefault();
		CtClass ctClass = classPool.makeClass(clazz.getName() + "PTAccessor");
		//CtClass ctClass = classPool.makeClass(clazz.getName() + "_" + System.nanoTime() + "_PTAccessor");
		CtClass objectClass = classPool.get(clazz.getName());
		CtConstructor ctor = new CtConstructor(new CtClass[]{objectClass}, ctClass);
		CtField objectField = new CtField(objectClass, "ptWrapped", ctClass);
		ctClass.addField(objectField);
		ctClass.addConstructor(ctor);
		ctor.setBody("ptWrapped = $1;");
		
		for(Field f: propertyFields) {
			OpenTypeAttribute ota = f.getAnnotation(OpenTypeAttribute.class);
			String id = ota.id();
			if("".equals(id)) {
				id = f.getName();
			}
			f.setAccessible(false);
			CtClass fType = classPool.get(f.getType().getName());
			CtMethod getter = new CtMethod(fType, "get" + Introspector.decapitalize(f.getName()), new CtClass[]{}, ctClass);
			ctClass.addMethod(getter);
			getter.setBody("return ptWrapped." + f.getName() + ";");
			CtMethod setter = new CtMethod(CtClass.voidType, "set" + Introspector.decapitalize(f.getName()), new CtClass[]{fType}, ctClass);
			ctClass.addMethod(setter);
			setter.setBody("ptWrapped." + f.getName() + "=$1;");
			
		}
		Set<Method> covered = new HashSet<Method>(propertyMethods.size());
		for(Method m: propertyMethods) {
			if(covered.contains(m)) continue;
			OpenTypeAttribute ota = m.getAnnotation(OpenTypeAttribute.class);
			Method reader = null, writer = null;
			String id = ota.id();
			if("".equals(id)) {
				id = m.getName().substring(3);
			}
			if(m.getName().startsWith("get")) {
				reader = m;
				reader.setAccessible(true);
				writer = ota.writable() ? ClassHelper.getOpposer(m) : null;
			} else {
				writer = ota.writable() ? m : null;
				reader = ClassHelper.getOpposer(m);
			}
			if(writer!=null) {
				writer.setAccessible(true);
			}
			if(writer!=null) {
				covered.add(writer);
				CtClass fType = classPool.get(writer.getParameterTypes()[0].getName());
				CtMethod setter = new CtMethod(CtClass.voidType, writer.getName(), new CtClass[]{fType}, ctClass);
				ctClass.addMethod(setter);
				setter.setBody("ptWrapped." + writer.getName()  + "($1);");
			}
			if(reader!=null) {
				covered.add(reader);
				CtClass fType = classPool.get(reader.getReturnType().getName());
				CtMethod getter = new CtMethod(fType, reader.getName(), new CtClass[]{}, ctClass);
				ctClass.addMethod(getter);
				getter.setBody("return ptWrapped." + reader.getName() + "();");				
			}
			
		}
		//ctClass.writeFile(System.getProperty("java.io.tmpdir"));
		DataInputStream dis = null;
		FileInputStream fis = null;
		File file = new File(System.getProperty("java.io.tmpdir") + File.separatorChar + ctClass.getName().replace('.', File.separatorChar) + ".class");
		file.delete();
		//log("Writing annotation to [" + file + "]");
		classPool.importPackage(Inherited.class.getPackage().getName());
		try {
			//fis = new FileInputStream(file);
			//dis = new DataInputStream(fis);
			ClassFile cf = ctClass.getClassFile();
			ConstPool cp = cf.getConstPool();
			AnnotationsAttribute attr = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);			
			Annotation a = new Annotation(Inherited.class.getName(), cp);
			attr.setAnnotation(a);
			cf.addAttribute(attr);
			cf.setVersionToJava5();		
			
		} finally {
			try { fis.close(); } catch (Exception e) {}
			try { dis.close(); } catch (Exception e) {}
		}

		ctClass.setModifiers(ctClass.getModifiers() & ~Modifier.ABSTRACT);
		ctClass.writeFile(System.getProperty("java.io.tmpdir"));
		Class<?> accessorClazz = ctClass.toClass();
		return accessorClazz;
	}
	
	/**
	 * Generates a new PropertyTable instance based on a dynamically generated AttributeAccessor set.
	 * @param object The wrapped object instance
	 * @param sorted true if the property table will be sorted
	 * @return a PropertyTable instance
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	public static PropertyTable<String, Object> createPropertyTable(Object object, boolean sorted) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Class<?> clazz = ClassHelper.nvl(object, "Passed object was null").getClass();
		Class<?> accessor = classCache.get(clazz);
		if(accessor==null) {
			synchronized(classCache) {
				accessor = classCache.get(clazz);
				if(accessor==null) {
					try {
						accessor = generateAccessors(object);
						classCache.put(clazz, accessor);
					} catch (Exception e) {
						throw new RuntimeException("Failed to generate PT for [" + clazz.getName() + "]", e);
					}
				}
			}
		}
		Object o = accessor.getDeclaredConstructor(clazz).newInstance(object);
		log("Created instance of [" + o.getClass().getName() + "]");
		Inherited inh = o.getClass().getAnnotation(Inherited.class);
		log("Inh:" + inh);
		for(java.lang.annotation.Annotation ann: o.getClass().getDeclaredAnnotations()) {
			log("Found Annotation instance of [" + ann.annotationType().getName() + "]");	
		}
		for(java.lang.annotation.Annotation ann: o.getClass().getAnnotations()) {
			log("Found Annotation instance of [" + ann.annotationType().getName() + "]");	
		}
		
		return null;
		
		
	}
	
	public static void main(String[] args)  {
		log("AOPPropertyTable Test");
		Sample s = new Sample();
		PropertyTable<String, Object> pt = null;
		try {
			pt = AOPPropertyTableFactory.createPropertyTable(s, true);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return;
		}
		if(pt==null) return;
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
