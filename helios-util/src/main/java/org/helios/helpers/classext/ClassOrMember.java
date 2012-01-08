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

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>Title: ClassOrMember</p>
 * <p>Description: MetaData oriented wrapper for classes and class members.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ClassOrMember {
	private static volatile MessageDigest digest = null;
	
	static {
		try {
			digest =  MessageDigest.getInstance("SHA-1");
		} catch (Exception e) {}
	}
	
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }
	
	
	public static String hash(String s) {
		if(s==null) return null;
		if(digest!=null) {
			byte[] sha1hash = new byte[40];
		    digest.update(s.getBytes());
		    sha1hash = digest.digest();
		    return convertToHex(sha1hash);
		} else {
			return "" + s.hashCode();
		}
	}
	
	enum ComType {
		CLASS(Class.class),
		METHOD(Method.class),
		FIELD(Field.class),
		CTOR(Constructor.class);
		
		private ComType(Class<?> clazz) {
			this.type = clazz;
		}
		
		private Class<?> type = null;
		
		public Class<?> type() {
			return type;
		}
		
		public String toString() {
			return type.getName();
		}
		
		
	}
	
	protected ComType type = null;
	protected Class<?> targetClass = null;
	protected Method targetMethod = null;
	protected Field targetField = null;
	protected Constructor<?> targetCtor = null;
	
	
	
	public ClassOrMember(Object targetObject) {
		if(targetObject==null) throw new RuntimeException("The target object was null");
		if(targetObject instanceof Class) {
			type = ComType.CLASS;
			targetClass = (Class<?>)targetObject;
		} else if(targetObject instanceof Method) {
			type = ComType.METHOD;
			targetMethod = (Method)targetObject;
		} else if(targetObject instanceof Field) {
			type = ComType.FIELD;
			targetField = (Field)targetObject;
		} else if(targetObject instanceof Constructor) {
			type = ComType.CTOR;
			targetCtor = (Constructor<?>)targetObject;
		} else {
			throw new RuntimeException("Invalid type [" + targetObject.getClass().getName() + "] Valid types are " + Arrays.toString(ComType.values()) );
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ClassOrMember com = new ClassOrMember("".getClass());
		} catch (Exception e) {
			System.err.println(e);			
		}

	}
	
	protected void ctorCheck() {
		if(!ComType.CTOR.equals(type)) {
			throw new RuntimeException("Requested Constructor Operation when type is [" + type + "]");
		}
	}
	
	protected void classCheck() {
		if(!ComType.CLASS.equals(type)) {
			throw new RuntimeException("Requested Class Operation when type is [" + type + "]");
		}
	}
	
	protected void methodCheck() {
		if(!ComType.METHOD.equals(type)) {
			throw new RuntimeException("Requested Method Operation when type is [" + type + "]");
		}
	}
	
	protected void fieldCheck() {
		if(!ComType.FIELD.equals(type)) {
			throw new RuntimeException("Requested Field Operation when type is [" + type + "]");
		}
	}
	


	/**
	 * @return the type
	 */
	public ComType getComType() {
		return type;
	}

	/**
	 * @return the targetClass
	 */
	public Class<?> getTargetClass() {
		classCheck();
		return targetClass;
	}

	/**
	 * @return the targetMethod
	 */
	public Method getTargetMethod() {
		methodCheck();
		return targetMethod;
	}

	/**
	 * @return the targetField
	 */
	public Field getTargetField() {
		fieldCheck();
		return targetField;
	}

	/**
	 * @return the targetCtor
	 */
	public Constructor<?> getTargetCtor() {
		ctorCheck();
		return targetCtor;
	}
	
	//===========================
	//				OVERLAP
	//				METHODS
	//===========================
	
	public Annotation[] getAllAnnotations()  {
		Set<Annotation> anns = new HashSet<Annotation>();
		if(ComType.CLASS.equals(type)) {
			Collections.addAll(anns, targetClass.getDeclaredAnnotations());
			Collections.addAll(anns, targetClass.getAnnotations());
		} else if(ComType.METHOD.equals(type)) {
			Collections.addAll(anns, targetMethod.getDeclaredAnnotations());
			Collections.addAll(anns, targetMethod.getAnnotations());
		} else if(ComType.CTOR.equals(type)) {
			Collections.addAll(anns, targetCtor.getDeclaredAnnotations());
			Collections.addAll(anns, targetCtor.getAnnotations());
		}  else {
			Collections.addAll(anns, targetField.getDeclaredAnnotations());
			Collections.addAll(anns, targetField.getAnnotations());			
		}		
		return anns.toArray(new Annotation[anns.size()]);		
	}
	
	
	
	/**
	 * Returns all methods for the passed class 
	 * @param includeParents If true, instances of the same method in a child and it's parent will be included.
	 * @param sameAccess If true, <code>includeParents</code> will only be honored if the accessibility is the same as the child.
	 * @param includeObject If true, methods for <code>java.lang.Object</code> will be included in the return.
	 * @return
	 */
	public Method[] getAllMethods(boolean includeParents, boolean sameAccess, boolean includeObject)  {
		Set<Method> methods = new HashSet<Method>();
		if(ComType.CLASS.equals(type)) {
			
		}		
		return methods.toArray(new Method[methods.size()]);		
	}
	
	/**
	 * Returns all methods for the passed class defaulting as follows:<ul>
	 * <li><b><code>includeParents</code></b>:false</li>
	 * <li><b><code>sameAccess</code></b>:true</li>
	 * <li><b><code>includeObject</code></b>:false</li>
	 * </ul>
	 * @return An array of matching methods
	 */
	public Method[] getAllMethods() {
		return getAllMethods(false, true, false);
	}

	public Field[] getAllFields()  {
		Set<Field> fields = new HashSet<Field>();
		if(ComType.CLASS.equals(type)) {
			Collections.addAll(fields, targetClass.getDeclaredFields());
			Collections.addAll(fields, targetClass.getFields());
		}		
		return fields.toArray(new Field[fields.size()]);		
	}
	
	public Constructor<?>[] getAllCtors()  {
		Set<Constructor> ctors = new HashSet<Constructor>();
		if(ComType.CLASS.equals(type)) {
			Collections.addAll(ctors, targetClass.getDeclaredConstructors());
			Collections.addAll(ctors, targetClass.getConstructors());
		}		
		return ctors.toArray(new Constructor[ctors.size()]);		
	}
	

	
	public String encode() {
		StringBuilder b = new StringBuilder();
		if(ComType.CLASS.equals(type)) {
			b.append("CLASS:").append(targetClass.getName());			
		} else if(ComType.METHOD.equals(type)) {
			b.append("METHOD:").append(targetMethod.getModifiers()).append(targetMethod.getReturnType().getName());
			b.append(targetMethod.getName());
			for(Class<?> clazz: targetMethod.getParameterTypes()) {
				b.append(clazz.getName());
			}
		} else if(ComType.CTOR.equals(type)) {
			b.append("CTOR:").append(targetCtor.getModifiers());
			b.append(targetCtor.getName());
			for(Class<?> clazz: targetCtor.getParameterTypes()) {
				b.append(clazz.getName());
			}			
		}  else {
			b.append("FIELD:").append(targetField.getModifiers());
			b.append(targetField.getName());
			b.append(targetField.getType());
			b.append(targetField.getGenericType());
		}	
		return hash(b.toString());
	}
	
	public static Collection<ClassOrMember> reduce(Set<ClassOrMember> rset) {
		if(rset!=null) {
			Map<String, ClassOrMember> map = new HashMap<String, ClassOrMember>(rset.size());
			for(ClassOrMember com: rset) {
				map.put(com.encode(), com);
			}
			return new HashSet<ClassOrMember>(map.values());
		} else {
			return new HashSet<ClassOrMember>();
		}
	}
	
	
	public boolean equals(Object object)  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.equals(object);
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.equals(object);
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.equals(object);
		}  else {
			return targetField.equals(object);
		}		
	}
	
	
	public int getModifiers()  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.getModifiers();
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.getModifiers();
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.getModifiers();
		}  else {
			return targetField.getModifiers();
		}
	}
	
	public String toString()  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.toString();
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.toString();
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.toString();
		}  else {
			return targetField.toString();
		}		
	}

	public String getName()  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.getName();
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.getName();
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.getName();
		}  else {
			return targetField.toString();
		}						
	}

	public <T extends Annotation> Annotation getAnnotation(Class<? extends Annotation> clazz)  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.getAnnotation(clazz);
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.getAnnotation(clazz);
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.getAnnotation(clazz);
		}  else {
			return targetField.getAnnotation(clazz);
		}						

	}

	public Annotation[] getDeclaredAnnotations()  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.getDeclaredAnnotations();
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.getDeclaredAnnotations();
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.getDeclaredAnnotations();
		}  else {
			return targetField.getDeclaredAnnotations();
		}						
	}
	
	public Annotation[] getAnnotations()  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.getAnnotations();
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.getAnnotations();
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.getAnnotations();
		}  else {
			return targetField.getAnnotations();
		}						
	}
	

	public Class<?> getDeclaringClass()  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.getDeclaringClass();
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.getDeclaringClass();
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.getDeclaringClass();
		}  else {
			return targetField.getDeclaringClass();
		}								
	}

	public boolean isSynthetic()  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.isSynthetic();
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.isSynthetic();
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.isSynthetic();
		}  else {
			return targetField.isSynthetic();
		}										
	}
	
	public int hashCode()  {
		if(ComType.CLASS.equals(type)) {
			return targetClass.hashCode();
		} else if(ComType.METHOD.equals(type)) {
			return targetMethod.hashCode();
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.hashCode();
		}  else {
			return targetField.hashCode();
		}												
	}
	
	public String toGenericString()  {
		if(ComType.METHOD.equals(type)) {
			return targetMethod.toGenericString();
		} else if(ComType.CTOR.equals(type)) {
			return targetCtor.toGenericString();
		}  else {
			return targetField.toGenericString();
		}
	}
		
		public Class<?>[] getParameterTypes()  {
			if(ComType.METHOD.equals(type)) {
				return targetMethod.getParameterTypes();
			} else {
				return targetCtor.getParameterTypes();
			} 
		}
		
		public Class<?>[] getExceptionTypes()  {
			if(ComType.METHOD.equals(type)) {
				return targetMethod.getExceptionTypes();
			} else {
				return targetCtor.getExceptionTypes();
			} 			
		}		
		
		public TypeVariable[] getTypeParameters()  {
			if(ComType.METHOD.equals(type)) {
				return targetMethod.getTypeParameters();
			} else {
				return targetCtor.getTypeParameters();
			} 			
		}
		
		public Type[] getGenericExceptionTypes()  {
			if(ComType.METHOD.equals(type)) {
				return targetMethod.getGenericExceptionTypes();
			} else {
				return targetCtor.getGenericExceptionTypes();
			} 														
		}

		public Type[] getGenericParameterTypes()  {
			if(ComType.METHOD.equals(type)) {
				return targetMethod.getGenericParameterTypes();
			} else {
				return targetCtor.getGenericParameterTypes();
			} 														
		}

		public Annotation[][] getParameterAnnotations()  {
			ctorCheck();
			return targetCtor.getParameterAnnotations();
		}

		public boolean isVarArgs()  {
			ctorCheck();
			return targetCtor.isVarArgs();
		}	
		
		
		
	
	//================================================
	//		CTOR
	//		DELEGATES
	//================================================









	public Object newInstance(Object[] objects) throws InstantiationException , IllegalAccessException , IllegalArgumentException , InvocationTargetException  {
		ctorCheck();
		return targetCtor.newInstance(objects);
	}




	
	//================================================
	//		METHOD
	//		DELEGATES
	//================================================
	
	public Object invoke(Object object, Object[] objects) throws IllegalAccessException , IllegalArgumentException , InvocationTargetException  {
		methodCheck();
		return targetMethod.invoke(object, objects);
	}



	public Class getReturnType()  {
		methodCheck();
		return targetMethod.getReturnType();
	}


	public Object getDefaultValue()  {
		methodCheck();
		return targetMethod.getDefaultValue();
	}





	public Type getGenericReturnType()  {
		methodCheck();
		return targetMethod.getGenericReturnType();
	}


	public boolean isBridge()  {
		methodCheck();
		return targetMethod.isBridge();
	}


	
	
	//================================================
	//		CLASS
	//		DELEGATES
	//================================================
	
	
	public Class forName(String string, boolean bool, ClassLoader clazzloader) throws ClassNotFoundException  {
		classCheck();
		return targetClass.forName(string, bool, clazzloader);
	}

	public Class forName(String string) throws ClassNotFoundException  {
		classCheck();
		return targetClass.forName(string);
	}

	public boolean isAssignableFrom(Class<?> clazz)  {
		classCheck();
		return targetClass.isAssignableFrom(clazz);
	}

	public boolean isInstance(Object object)  {
		classCheck();
		return targetClass.isInstance(object);
	}


	public boolean isInterface()  {
		classCheck();
		return targetClass.isInterface();
	}

	public boolean isArray()  {
		classCheck();
		return targetClass.isArray();
	}

	public boolean isPrimitive()  {
		classCheck();
		return targetClass.isPrimitive();
	}

	public Class getSuperclass()  {
		classCheck();
		return targetClass.getSuperclass();
	}

	public Class getComponentType()  {
		classCheck();
		return targetClass.getComponentType();
	}





	public Class asSubclass(Class<?> clazz)  {
		classCheck();
		return targetClass.asSubclass(clazz);
	}

	public Object cast(Object object)  {
		classCheck();
		return targetClass.cast(object);
	}

	public boolean desiredAssertionStatus()  {
		classCheck();
		return targetClass.desiredAssertionStatus();
	}



	public String getCanonicalName()  {
		classCheck();
		return targetClass.getCanonicalName();
	}

	public ClassLoader getClassLoader()  {
		classCheck();
		return targetClass.getClassLoader();
	}

	public Class[] getClasses()  {
		classCheck();
		return targetClass.getClasses();
	}

	public Constructor getConstructor(Class[] clazz) throws NoSuchMethodException , SecurityException  {
		classCheck();
		return targetClass.getConstructor(clazz);
	}

	public Constructor[] getConstructors() throws SecurityException  {
		classCheck();
		return targetClass.getConstructors();
	}



	public Class[] getDeclaredClasses() throws SecurityException  {
		classCheck();
		return targetClass.getDeclaredClasses();
	}

	public Constructor getDeclaredConstructor(Class[] clazz) throws NoSuchMethodException , SecurityException  {
		classCheck();
		return targetClass.getDeclaredConstructor(clazz);
	}

	public Constructor[] getDeclaredConstructors() throws SecurityException  {
		classCheck();
		return targetClass.getDeclaredConstructors();
	}

	public Field getDeclaredField(String string) throws NoSuchFieldException , SecurityException  {
		classCheck();
		return targetClass.getDeclaredField(string);
	}

	public Field[] getDeclaredFields() throws SecurityException  {
		classCheck();
		return targetClass.getDeclaredFields();
	}

	public Method getDeclaredMethod(String string, Class[] clazz) throws NoSuchMethodException , SecurityException  {
		classCheck();
		return targetClass.getDeclaredMethod(string, clazz);
	}

	public Method[] getDeclaredMethods() throws SecurityException  {
		classCheck();
		return targetClass.getDeclaredMethods();
	}


	public Class getEnclosingClass()  {
		classCheck();
		return targetClass.getEnclosingClass();
	}

	public Constructor getEnclosingConstructor()  {
		classCheck();
		return targetClass.getEnclosingConstructor();
	}

	public Method getEnclosingMethod()  {
		classCheck();
		return targetClass.getEnclosingMethod();
	}

	public Object[] getEnumConstants()  {
		classCheck();
		return targetClass.getEnumConstants();
	}

	public Field getField(String string) throws NoSuchFieldException , SecurityException  {
		classCheck();
		return targetClass.getField(string);
	}

	public Field[] getFields() throws SecurityException  {
		classCheck();
		return targetClass.getFields();
	}

	public Type[] getGenericInterfaces()  {
		classCheck();
		return targetClass.getGenericInterfaces();
	}

	public Type getGenericSuperclass()  {
		classCheck();
		return targetClass.getGenericSuperclass();
	}

	public Class[] getInterfaces()  {
		classCheck();
		return targetClass.getInterfaces();
	}

	public Method getMethod(String string, Class[] clazz) throws NoSuchMethodException , SecurityException  {
		classCheck();
		return targetClass.getMethod(string, clazz);
	}

	public Method[] getMethods() throws SecurityException  {
		classCheck();
		return targetClass.getMethods();
	}

	public Package getPackage()  {
		classCheck();
		return targetClass.getPackage();
	}

	public ProtectionDomain getProtectionDomain()  {
		classCheck();
		return targetClass.getProtectionDomain();
	}

	public URL getResource(String string)  {
		classCheck();
		return targetClass.getResource(string);
	}

	public InputStream getResourceAsStream(String string)  {
		classCheck();
		return targetClass.getResourceAsStream(string);
	}

	public Object[] getSigners()  {
		classCheck();
		return targetClass.getSigners();
	}

	public String getSimpleName()  {
		classCheck();
		return targetClass.getSimpleName();
	}

	public boolean isAnnotation()  {
		classCheck();
		return targetClass.isAnnotation();
	}

	public boolean isAnnotationPresent(Class<? extends Annotation> clazz)  {
		classCheck();
		return targetClass.isAnnotationPresent(clazz);
	}

	public boolean isAnonymousClass()  {
		classCheck();
		return targetClass.isAnonymousClass();
	}

	public boolean isEnum()  {
		classCheck();
		return targetClass.isEnum();
	}

	public boolean isLocalClass()  {
		classCheck();
		return targetClass.isLocalClass();
	}

	public boolean isMemberClass()  {
		classCheck();
		return targetClass.isMemberClass();
	}


	public Object newInstance() throws InstantiationException , IllegalAccessException  {
		classCheck();
		return targetClass.newInstance();
	}	
	
	//================================================
	//		FIELD
	//		DELEGATES
	//================================================
	
	public Class getType()  {
		fieldCheck();
		return targetField.getType();
	}
	
	
	public Object get(Object object) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		return targetField.get(object);
	}

	public boolean getBoolean(Object object) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		return targetField.getBoolean(object);
	}

	public byte getByte(Object object) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		return targetField.getByte(object);
	}

	public short getShort(Object object) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		return targetField.getShort(object);
	}

	public char getChar(Object object) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		return targetField.getChar(object);
	}

	public int getInt(Object object) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		return targetField.getInt(object);
	}

	public long getLong(Object object) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		return targetField.getLong(object);
	}

	public float getFloat(Object object) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		return targetField.getFloat(object);
	}

	public double getDouble(Object object) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		return targetField.getDouble(object);
	}




	public Type getGenericType()  {
		fieldCheck();
		return targetField.getGenericType();
	}


	public boolean isEnumConstant()  {
		fieldCheck();
		return targetField.isEnumConstant();
	}

	public void set(Object object, Object object2) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		targetField.set(object, object2);
	}

	public void setBoolean(Object object, boolean bln) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		targetField.setBoolean(object, bln);
	}

	public void setByte(Object object, byte byt) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		targetField.setByte(object, byt);
	}

	public void setChar(Object object, char chr) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		targetField.setChar(object, chr);
	}

	public void setDouble(Object object, double dbl) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		targetField.setDouble(object, dbl);
	}

	public void setFloat(Object object, float flt) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		targetField.setFloat(object, flt);
	}

	public void setInt(Object object, int nt) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		targetField.setInt(object, nt);
	}

	public void setLong(Object object, long lng) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		targetField.setLong(object, lng);
	}

	public void setShort(Object object, short shrt) throws IllegalArgumentException , IllegalAccessException  {
		fieldCheck();
		targetField.setShort(object, shrt);
	}


}
