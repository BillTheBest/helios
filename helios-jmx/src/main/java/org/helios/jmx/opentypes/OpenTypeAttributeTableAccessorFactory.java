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
package org.helios.jmx.opentypes;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import org.apache.log4j.BasicConfigurator;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.opentypes.annotations.XCompositeAttribute;
import org.helios.jmx.opentypes.annotations.XCompositeType;
import org.helios.jmx.opentypes.property.ReadOnlyAttributeAccessor;
import org.helios.jmxenabled.counters.LongRollingCounter;


/**
 * <p>Title: OpenTypeAttributeTableAccessorFactory</p>
 * <p>Description: A factory and cache for OpenTypeAttributeTableAccessors.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.opentypes.OpenTypeAttributeTableAccessorFactory</code></p>
 */

public class OpenTypeAttributeTableAccessorFactory {
	/** A cache of created ReadOnlyAttributeAccessors class constructors keyed by the AccessibleObject they are accessors for. */
	protected static final Map<AccessibleObject, Constructor<ReadOnlyAttributeAccessor<?>>> accessors = new ConcurrentHashMap<AccessibleObject, Constructor<ReadOnlyAttributeAccessor<?>>>();
	/** A serial number factory for generated classes */
	protected static final AtomicLong serial = new AtomicLong(0L);
	/**
	 * Creates an accessor for the passed AccessibleObject (Field or Method)
	 * @param ao The accessible object
	 * @param ref An object instance
	 * @return a ReadOnlyAttributeAccessor for the passed AccessibleObject
	 */
	public static ReadOnlyAttributeAccessor<?> getAccessor(AccessibleObject ao, Object ref) {
		String key = validateAccessibleObject(ao);
		Constructor<ReadOnlyAttributeAccessor<?>> ctor = accessors.get(ao);
		if(ctor==null) {
			synchronized(accessors) {
				ctor = accessors.get(ao);
				if(ctor==null) {
					ctor = createAccessor(ao);
					accessors.put(ao, ctor);
				}
			}
		}
		try {
			return ctor.newInstance(ao, ref);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ReadOnlyAttributeAccessor for AccessibleObject [" + key + "]", e);
		}
	}
	
	public static Class<?> getDeclaringClass(AccessibleObject ao) {
		if(ao==null) throw new IllegalArgumentException("Passed ao was null", new Throwable());
		if(ao instanceof Method) {
			return ((Method)ao).getDeclaringClass();
		} else if(ao instanceof Field) {
			return ((Field)ao).getDeclaringClass();
		} else {
			return ((Constructor<?>)ao).getDeclaringClass();
		}
	}
	
	
	/**
	 * Validates the passed accessible object and returns a string key.
	 * @param ao The accessible object to validate.
	 * @return A string key that uniquely identifies the accessible object.
	 */
	protected static String validateAccessibleObject(AccessibleObject ao) {
		if(ao==null) throw new IllegalArgumentException("Passed accessible object was null", new Throwable());
		if(ao instanceof Constructor<?>) throw new IllegalArgumentException("AttributeAccessors cannot be created for constructors [" + ((Constructor<?>)ao).toGenericString() + "]", new Throwable());
		if(ao instanceof Field) {
			Field f = (Field)ao;
			return f.getDeclaringClass().getName() + "." + f.getName();
		} else if(ao instanceof Method) {
			Method m = (Method)ao;
			String key = m.getDeclaringClass().getName() + "." + m.toGenericString();
			if(void.class.equals(m.getReturnType())) {
				throw new IllegalArgumentException("AttributeAccessors cannot be created for non getter methods [" + key + "]", new Throwable());
			}
			if(m.getParameterTypes().length>0) {
				throw new IllegalArgumentException("AttributeAccessors cannot be created for non getter methods [" + key + "]", new Throwable());
			}
			return key;			
		} else {
			throw new RuntimeException("Unexpected AccessibleObject type [" + ao.getClass().getName() + "]", new Throwable());
		}		
	}
	
	/**
	 * Bytecode compiles an attribute accessor for the passed accessible object
	 * @param ao The accessible object to create the accessor for
	 * @return the constructor for the accessor
	 */
	protected static Constructor<ReadOnlyAttributeAccessor<?>> createAccessor(AccessibleObject ao) {
		if(ao instanceof Field) {
			return createAccessor((Field)ao);
		} else {
			return createAccessor((Method)ao);			
		}	
	}
	
	@JMXManagedObject(annotated=true, declared=true)
	public static class TestClassMBean extends ManagedObjectDynamicMBean {
		protected final TestClass tc = new TestClass(new TestClass(null));
		protected final CompositeData cd = OpenTypeManager.getInstance().getCompositeDataInstance(tc);
		
		public TestClassMBean() {
			this.reflectObject(this);
			ctr2.put(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
		}
		/**
		 * @return the Test Class Composite Data
		 */
		@JMXAttribute(name="TestClassCompositeData", description="An example of a XCompositeType", mutability=AttributeMutabilityOption.READ_ONLY)
		public CompositeData getCompositeData() {
			return cd;
		}
		
		
		// === Fails
		@JMXAttribute(name="JMXAttrCtr", description="A Long Rolling Counter", mutability=AttributeMutabilityOption.READ_ONLY)
		@XCompositeAttribute
		public LongRollingCounter getJMXAttrCtr() {
			return ctr2;
		}
		LongRollingCounter ctr2 = new LongRollingCounter("TestClass2", 10);
		
		
		// === Works OK
		@JMXAttribute(name="JMXAttrComp", description="A Long Rolling Counter", mutability=AttributeMutabilityOption.READ_ONLY)		
		public CompositeData getJMXAttrComp() {
			return OpenTypeManager.getInstance().getCompositeDataInstance(ctr3);
		}
		LongRollingCounter ctr3 = new LongRollingCounter("TestClass3", 10);
		
	}
	
	
	@XCompositeType(description="TestClass")
	public static class TestClass {
		
		public TestClass(TestClass tc) {
			testClass = tc;
			testClassName = testClass==null ? "TestClass - Non Recursive" : "TestClass - Recursive" ;			
			System.out.println("\n\tTestClass Package:" + getClass().getPackage().getName() + "\n");
			ctr.put(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
		}		
		private TestClass testClass;				
		@XCompositeAttribute
		private String testClassName;		
		@XCompositeAttribute
		private String privateField = "PrivateField";
		@XCompositeAttribute
		private static String privateStaticField = "PrivateStaticField";
		@XCompositeAttribute
		protected String protectedField = "protectedField";
		@XCompositeAttribute
		protected static String protectedStaticField = "protectedStaticField";
		@XCompositeAttribute
		String packageProtectedField = "packageProtectedField";
		@XCompositeAttribute
		static String packageProtectedStaticField = "packageProtectedStaticField";
		@XCompositeAttribute
		public String publicField = "publicField";
		@XCompositeAttribute
		public static String publicStaticField = "publicStaticField";		
		@XCompositeAttribute
		public String getMyPrivateField() {
			return privateField;
		}
		@XCompositeAttribute(name="AnnFieldCtr", description="Annotated Field Counter")
		LongRollingCounter ctr = new LongRollingCounter("TestClass", 10);

	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		log("CompositeDataTest\nPID:" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		TestClass tc = new TestClass(null);
		CompositeData cd = OpenTypeManager.getInstance().getCompositeDataInstance(tc);
		CompositeType ct = cd.getCompositeType();
		StringBuilder  b = new StringBuilder("Composite Type:");
		b.append(ct.getDescription()).append("[");
		for(String key: ct.keySet()) {
			b.append("\n\tKey:").append(key);
			b.append("\n\t\tType:(").append(ct.getType(key).getClass().getName()).append(")-->").append(ct.getType(key));
			b.append("\n\t\tDescription:").append(ct.getDescription(key));
			
		}
		log(b);
		log(cd);
		LongRollingCounter lrc = new LongRollingCounter("foo", 10);
		lrc.put(System.currentTimeMillis());
		cd = OpenTypeManager.getInstance().getCompositeDataInstance(lrc);
		log("LRC CT:\n" + cd.getCompositeType());
		for(String s: cd.getCompositeType().keySet()) {
			log("[" + s + "]:" + cd.get(s));
		}
		TestClassMBean tcm = new TestClassMBean();
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(tcm, JMXHelper.objectName("org.helios.jmx:service=CompositeDataExample"));
			
			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static void log(Object message) {
		System.out.println(message);
	}
	
	/**
	 * Determines if the passed field is package protected
	 * @param f The field to test
	 * @return true if the field is package protected
	 */
	protected static boolean isPackageProtected(Field f) {
		int mod = f.getModifiers();
		return (!Modifier.isPrivate(mod) && !Modifier.isPublic(mod) && !Modifier.isProtected(mod));
	}
	
	/**
	 * Determines if the passed method is package protected
	 * @param m The method to test
	 * @return true if the method is package protected
	 */
	protected static boolean isPackageProtected(Method m) {
		int mod = m.getModifiers();
		return (!Modifier.isPrivate(mod) && !Modifier.isPublic(mod) && !Modifier.isProtected(mod));
	}
	
	
	/**
	 * Creates an attribute accessor for the passed field.
	 * @param f The field to generate an accessor for
	 * @return the constructor for the accessor
	 */
	protected static Constructor<ReadOnlyAttributeAccessor<?>> createAccessor(Field f) {		
		try {			
			Class<?> clazz = f.getDeclaringClass();			
			ClassPool classPool = ClassPool.getDefault();
			classPool.appendClassPath(new ClassClassPath(clazz));
			classPool.appendClassPath(new ClassClassPath(ReadOnlyAttributeAccessor.class));
			String className = null;
			if(isPackageProtected(f)) {
				className = (clazz.getPackage().getName() + "." + clazz.getSimpleName() +  "FieldAttrAccessor" + f.getName());
			} else {
				className = clazz.getName() + "FieldAttrAccessor" + f.getName();
			}
			CtClass ctClass = classPool.makeClass(className);
			CtClass aoClass = classPool.get(AccessibleObject.class.getName());
			CtClass iface = classPool.get(ReadOnlyAttributeAccessor.class.getName());
			int modifier = f.getModifiers();
			boolean packageProtected = isPackageProtected(f);
			if(!Modifier.isStatic(modifier)) {
				ctClass.addField(new CtField(classPool.get(f.getDeclaringClass().getName()), "ref", ctClass));
				if(!Modifier.isPublic(modifier) && !packageProtected) {					
					ctClass.addField(new CtField(classPool.get(Field.class.getName()), "field", ctClass));
				}
			} else {
				if(!Modifier.isPublic(modifier) && !packageProtected) {
					ctClass.addField(new CtField(classPool.get(Field.class.getName()), "field", ctClass));
				}				
			}
			
			CtConstructor ctCtor = new CtConstructor(new CtClass[]{aoClass, classPool.get(Object.class.getName())}, ctClass);
			ctClass.addConstructor(ctCtor);
			CtMethod method = new CtMethod(classPool.get(Object.class.getName()), "get", new CtClass[]{}, ctClass);
			CtMethod nameMethod = new CtMethod(classPool.get(String.class.getName()), "getName", new CtClass[]{}, ctClass);
			
			ctClass.addMethod(method);
			ctClass.addMethod(nameMethod);
			nameMethod.setBody(new StringBuilder("{ return \"FieldAccessor:")
				.append(Modifier.isStatic(modifier) ? "[Static]" : "")
				.append(f.getDeclaringClass().getName())
				.append(".")
				.append(f.getName())
				.append("\";}")
			.toString());
			StringBuilder b = new StringBuilder("{return ");
			if(!Modifier.isStatic(modifier)) {				
				// Non Static Field
				if(!Modifier.isPublic(modifier) && !packageProtected) {
					// Not accessible
					ctCtor.setBody("{this.field = (java.lang.reflect.Field)$1; this.ref = (" + f.getDeclaringClass().getName() + ")$2; this.field.setAccessible(true);}");
					b.append("field.get(ref)");					
				} else {
					// Accessible
					ctCtor.setBody("{this.ref = (" + f.getDeclaringClass().getName() + ")$2;}");
					b.append("ref").append(".").append(f.getName());
				}
			} else {
				// Static field
				if(!Modifier.isPublic(modifier) && !packageProtected) {
					// Not accessible
					ctCtor.setBody("{this.field = (java.lang.reflect.Field)$1;this.field.setAccessible(true);}");
					b.append("field.get(null)");					
				} else {
					// Accessible
					ctCtor.setBody("{}");
					b.append(f.getDeclaringClass().getName()).append(".").append(f.getName());
				}
			}
			b.append(";}");
			method.setBody(b.toString());
			ctClass.addInterface(iface);
			ctClass.setModifiers(ctClass.getModifiers() & ~Modifier.ABSTRACT);
			method.setModifiers(method.getModifiers() & ~Modifier.ABSTRACT);
			nameMethod.setModifiers(method.getModifiers() & ~Modifier.ABSTRACT);
			File file = new File(System.getProperty("java.io.tmpdir") + File.separatorChar + ctClass.getName().replace('.', File.separatorChar) + ".class");
			file.delete();
			ctClass.writeFile(System.getProperty("java.io.tmpdir"));
			
			Class<ReadOnlyAttributeAccessor<?>> newClass = ctClass.toClass();
			ctClass.detach();
			return newClass.getDeclaredConstructor(AccessibleObject.class, Object.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create field accessor", e);
		}
	}
	
	/**
	 * Creates an attribute accessor for the passed method
	 * @param m The method to generate an accessor for
	 * @return the constructor for the accessor
	 */
	protected static Constructor<ReadOnlyAttributeAccessor<?>> createAccessor(Method method) {
		try {
			StringBuilder debug = new StringBuilder();
			Class<?> clazz = method.getDeclaringClass();
			debug.append("Method Attribute Accessor");
			debug.append("Declaring Class:").append(clazz.getName());
			ClassPool classPool = ClassPool.getDefault();
			classPool.appendClassPath(new ClassClassPath(clazz));
			classPool.appendClassPath(new ClassClassPath(ReadOnlyAttributeAccessor.class));
			String className = null;
			if(isPackageProtected(method)) {
				className = (clazz.getPackage().getName() + "." + clazz.getSimpleName() +  "MethodAttrAccessor" + method.getName()).replace("$", ".");
			} else {
				className = clazz.getName() + "MethodAttrAccessor" + method.getName();
			}
			debug.append("\n\tGenerated Class:").append(className);
			CtClass ctClass = classPool.makeClass(className);
			CtClass returnClass = classPool.get(method.getReturnType().getName());
			debug.append("\n\tReturn Type:").append(returnClass.getName());
			CtClass aoClass = classPool.get(AccessibleObject.class.getName());
			CtClass iface = classPool.get(ReadOnlyAttributeAccessor.class.getName());
			int modifier = method.getModifiers();
			boolean packageProtected = isPackageProtected(method);
			if(!Modifier.isStatic(modifier)) {
				ctClass.addField(new CtField(classPool.get(method.getDeclaringClass().getName()), "ref", ctClass));
				debug.append("\n\tAdded Field ref:").append(method.getDeclaringClass().getName());
				if(!Modifier.isPublic(modifier) && !packageProtected) {					
					ctClass.addField(new CtField(classPool.get(Method.class.getName()), "method", ctClass));
					debug.append("\n\tAdded Field method:").append(method.getClass().getName());
				}
			} else {
				if(!Modifier.isPublic(modifier) && !packageProtected) {
					ctClass.addField(new CtField(classPool.get(Method.class.getName()), "method", ctClass));
					debug.append("\n\tAdded Field method:").append(method.getClass().getName());
				}				
			}
			
			CtConstructor ctCtor = new CtConstructor(new CtClass[]{aoClass, classPool.get(Object.class.getName())}, ctClass);
			ctClass.addConstructor(ctCtor);
			debug.append("\n\tAdded Ctor:").append(Arrays.toString(new CtClass[]{aoClass, classPool.get(Object.class.getName())}));
			CtMethod amethod = new CtMethod(classPool.get(Object.class.getName()), "get", new CtClass[]{}, ctClass);
			CtMethod nameMethod = new CtMethod(classPool.get(String.class.getName()), "getName", new CtClass[]{}, ctClass);
			
			ctClass.addMethod(amethod);
			debug.append("\n\tAdded Method:").append(amethod.getName()).append("/").append(amethod.getSignature());
			ctClass.addMethod(nameMethod);
			nameMethod.setBody(new StringBuilder("{ return \"MethodAccessor:")
				.append(Modifier.isStatic(modifier) ? "[Static]" : "")
				.append(method.getDeclaringClass().getName())
				.append(".")
				.append(method.getName())
				.append("\";}")
			.toString());
			StringBuilder b = new StringBuilder("{return ($w)");
			if(!Modifier.isStatic(modifier)) {				
				// Non Static Field
				if(!Modifier.isPublic(modifier) && !packageProtected) {
					// Not accessible
					ctCtor.setBody("{this.method = (java.lang.reflect.Method)$1; this.ref = (" + method.getDeclaringClass().getName() + ")$2;}");
					debug.append("\n\tSet Ctor Body:").append("{this.method = (java.lang.reflect.Method)$1; this.method.setAccessible(true); this.ref = (" + method.getDeclaringClass().getName() + ")$2;}");					
					b.append("method.invoke(ref)");					
				} else {
					// Accessible
					ctCtor.setBody("{this.ref = (" + method.getDeclaringClass().getName() + ")$2;}");
					debug.append("\n\tSet Ctor Body:").append("{this.ref = (" + method.getDeclaringClass().getName() + ")$2;}");
					b.append("ref").append(".").append(method.getName()).append("()");
				}
			} else {
				// Static field
				if(!Modifier.isPublic(modifier) && !packageProtected) {
					// Not accessible
					ctCtor.setBody("{this.method = (java.lang.reflect.Method)$1; this.method.setAccessible(true);}");
					debug.append("\n\tSet Ctor Body:").append("{this.method = (java.lang.reflect.Method)$1;}");
					b.append("method.invoke(null)");					
				} else {
					// Accessible
					ctCtor.setBody("{}");
					debug.append("\n\tSet Ctor Body:").append("{}");
					b.append(method.getDeclaringClass().getName()).append(".").append(method.getName()).append("()");
				}
			}
			b.append(";}");
			debug.append("\n\tSet get() Method Body:").append(b.toString());
			//log(debug);
			amethod.setBody(b.toString());
			
			
			ctClass.addInterface(iface);
			debug.append("\n\tAdded Interface:").append(iface.getName());
			ctClass.setModifiers(ctClass.getModifiers() & ~Modifier.ABSTRACT);
			amethod.setModifiers(method.getModifiers() & ~Modifier.ABSTRACT);
			nameMethod.setModifiers(method.getModifiers() & ~Modifier.ABSTRACT);
			File file = new File(System.getProperty("java.io.tmpdir") + File.separatorChar + ctClass.getName().replace('.', File.separatorChar) + ".class");
			file.delete();
			ctClass.writeFile(System.getProperty("java.io.tmpdir"));
			//log(debug);
			Class<ReadOnlyAttributeAccessor<?>> newClass = ctClass.toClass();
			ctClass.detach();
			return newClass.getDeclaredConstructor(AccessibleObject.class, Object.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create field accessor", e);
		}
	}
	
	
	
}
