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
package org.helios.aop;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;

import org.helios.helpers.ClassHelper;
import org.helios.helpers.XMLHelper;
import org.w3c.dom.Node;
/**
 * <p>Title: DynaClassFactory</p>
 * <p>Description: A factory class for creating dynamic classes and instances thereof.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.aop.DynaClassFactory</code></p>
 */
//Set<Constructor<?>> ctors = ClassHelper.getAnnotatedCtors(base, ConstructorDefinition.class, true);
public class DynaClassFactory {
	/** A cache of created classes keyed by class name */
	protected static final Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>(128);
	/** A cache of created class constructors that are id annotated keyed by class */
	protected static final Map<Class<?>, Map<String, Constructor<?>>> classCtors = new ConcurrentHashMap<Class<?>, Map<String, Constructor<?>>>(128);

	/**
	 * Creates a dynamically generated class instance
	 * @param className The class name of the class to be generated
	 * @param base The class template which can be an annotated interface or abstract class
	 * @param ctorId The id of the target constructor
	 * @param loaders An optional array of class loaders
	 * @return the generated class
	 */
	public static Object generateClassInstance(String className, Class<?> base, String ctorId, ClassLoader[] loaders, Object...args)  {
		if(ctorId==null) throw new IllegalArgumentException("[generateClassInstance] Passed ctorId was null", new Throwable());
		Class<?> clazz = generateClass(className, base, loaders);
		Map<String, Constructor<?>> ctorMap = classCtors.get(clazz);
		if(ctorMap==null) throw new IllegalArgumentException("No Ctor with id [" + ctorId + "] found on class [" + clazz.getName() + "]");
		Constructor<?> ctor = ctorMap.get(ctorId);
		if(ctor==null) throw new IllegalArgumentException("No Ctor with id [" + ctorId + "] found on class [" + clazz.getName() + "]");
		if(!ctor.isAccessible()) ctor.setAccessible(true);
		try {
			return ctor.newInstance(args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke ctor on class [" + clazz.getName() + "]", e);
		}
	}
	
	/**
	 * Creates a dynamically generated class instance using the default constructor
	 * @param className The class name of the class to be generated
	 * @param base The class template which can be an annotated interface or abstract class
	 * @param loaders An optional array of class loaders
	 * @return the generated class
	 */
	public static Object generateClassInstance(String className, Class<?> base, ClassLoader[] loaders, Object...args)  {
		return generateClassInstance(className, base, "Default", loaders, args);
	}	
	
	/**
	 * Creates a dynamically generated class instance
	 * @param className The class name of the class to be generated
	 * @param base The class template which can be an annotated interface or abstract class
	 * @param loaders An optional array of class loaders
	 * @return the generated class
	 */
	public static Object generateClassInstance(String className, Class<?> base, String ctorId, Object...args)  {
		return generateClassInstance(className, base, ctorId, new ClassLoader[]{}, args); 
	}
	

	
	/**
	 * Creates a dynamically generated class
	 * @param className The class name of the class to be generated
	 * @param base The class template which can be an annotated interface or abstract class
	 * @param loaders An optional array of class loaders
	 * @return the generated class
	 */
	public static Class<?> generateClass(String className, Class<?> base, ClassLoader...loaders)  {
		if(base==null) throw new IllegalArgumentException("[generateClass] Passed class was null", new Throwable());
		if(className==null) throw new IllegalArgumentException("[generateClass] Passed classname was null", new Throwable());
		try {
			Class<?> clazz = classes.get(className);
			if(clazz==null) {
				synchronized(classes) {
					if(clazz==null) {
						long start = System.currentTimeMillis();
						clazz = _generateClass(className, base, loaders);
						long elapsed = System.currentTimeMillis()-start;
						log("Created [" + className + "] in " + elapsed + " ms.");
						classes.put(className, clazz);
					}
				}
			}
			return clazz;
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate class [" + className + "] based off [" + base.getName() + "]", e);
		}
	}
	
	
	protected static Class<?> _generateClass(String className, Class<?> base, ClassLoader...loaders) throws ClassNotFoundException, RuntimeException, NotFoundException, CannotCompileException, NoSuchMethodException {
		StringBuilder b = new StringBuilder("\nBuilding Dynamic Class [\n\tClassName:");
		b.append(className);
		b.append("\n\tBase Class:").append(base==null ? "none" : base.getName());
		b.append("\n\tClassLoaders:");
		if(loaders!=null) {
			for(ClassLoader cl: loaders) {
				b.append("\n\t\t").append(cl);
			}
		}
		b.append("\n]");
		log(b);
		Class<?> dynaClass = null;
		if(base==null) throw new IllegalArgumentException("[generateClass] Passed class was null", new Throwable());
		ClassDefinition classDef = base.getAnnotation(ClassDefinition.class);
		Set<Method> methods = ClassHelper.getAnnotatedMethods(base, MethodDefinition.class, true);
		
		if(classDef==null && methods.isEmpty()) {
			throw new RuntimeException("[generateClass] Passed class [" + base.getName() + "] was not annotated with any DynaClass annotations", new Throwable());
		}
		boolean urlPropDefined = classDef==null ? false : !classDef.urlprop().equals("");
		ClassPool classPool = ClassPool.getDefault();
		classPool.appendClassPath(new ClassClassPath(base));		
		Map<Method, MethodDefinition> annotatedMethodMap = new HashMap<Method, MethodDefinition>();
		Map<String, CtField> classFields = new HashMap<String, CtField>();
		prepareClassPool(classDef, methods, classPool, annotatedMethodMap, loaders);
		CtClass ctClass = null;
		//  Superclass could be one of: 
			// 1. None.  (base is interface, no superclass in ClassDef)
			// 2. the passed base class.  (base is not interface, no superclass in ClassDef)
			// 3. the superclass defined in the ClassDef annot.  (base is interface, superclass in ClassDef) 
		
		boolean isBaseInterface = base.isInterface();
		boolean isSuperclassDefined = classDef==null ? false : !"".equals(classDef.superClass());
		if(isBaseInterface) {
			if(isSuperclassDefined) {
				// #3  --> Parent is ClassDef.superclass
				ctClass = classPool.makeClass(className, classPool.get(classDef.superClass()));
				log("SuperClass will be [" + classDef.superClass() + "]");
			} else {
				// #1 --> New class,  no parent
				log("No SuperClass.");
				ctClass = classPool.makeClass(className);
			}
			ctClass.addInterface(classPool.get(base.getName()));
		} else {
			if(isSuperclassDefined) {
				throw new RuntimeException("Ambiguous SuperClass Definition \n\t" + base.getName() + "\n\tvs.\n\t" + classDef.superClass() + "\nCannot resolve.");
			} else {
				// #2 --> Parent is baseClass
				log("SuperClass will be [" + base.getName() + "]");
				CtClass baseClass = classPool.get(base.getName());
				
				ctClass = classPool.makeClass(className, baseClass);
				for(Constructor<?> ctor: base.getDeclaredConstructors()) {
					CtConstructor ctc  = new CtConstructor(getCtClassArray(ctor.getParameterTypes(), classPool), ctClass);					
					ctClass.addConstructor(ctc);
					ctc.setBody("super($$);");
					log("Added Ctor [" + ctc.getLongName() + "]");
				}
			}			
		}		
		if(classDef!=null) {
			for(String iface: classDef.interfaces()) {
				ctClass.addInterface(classPool.get(iface));
				log("Added Interface [" + iface + "]");
			}
			processFields(classDef, classPool, classFields, ctClass, loaders);
		}
		Node sourceNode = classDef==null ? null : urlPropDefined ? getClassDefNode(classDef.urlprop())  : null;
		
		processMethods(urlPropDefined, classPool, annotatedMethodMap, ctClass,
				isBaseInterface, sourceNode, classFields);
		
		
		
		ctClass.setModifiers(ctClass.getModifiers() & ~Modifier.ABSTRACT);
		dynaClass = ctClass.toClass();
		log("Examining Constructors");
		Map<String, Constructor<?>> ctorMap = new HashMap<String, Constructor<?>>(); 
		classCtors.put(dynaClass, ctorMap);				
		for(Constructor<?> ctor: base.getDeclaredConstructors()) {
			ConstructorDefinition cd = ctor.getAnnotation(ConstructorDefinition.class);
			if(cd!=null) {
				Constructor<?> dCtor = dynaClass.getDeclaredConstructor(ctor.getParameterTypes());
				ctorMap.put(cd.id(), dCtor);
				log("Added Ctor [" + ctor.getName() + "] with ID [" + ctor.getAnnotation(ConstructorDefinition.class).id() + "]");
			}
			
			
		}

		ctClass.debugWriteFile(System.getProperty("java.io.tmpdir"));
		
		ctClass.detach();
		classPool.clearImportedPackages();
		
		return dynaClass;
	}

	/**
	 * @param classDef
	 * @param classPool
	 * @param classFields
	 * @param ctClass
	 * @param loaders
	 * @throws NotFoundException
	 * @throws ClassNotFoundException
	 * @throws CannotCompileException
	 */
	protected static void processFields(ClassDefinition classDef,
			ClassPool classPool, final Map<String, CtField> classFields,
			CtClass ctClass, ClassLoader... loaders) throws NotFoundException,
			ClassNotFoundException, CannotCompileException {
		for(FieldDefinition fd: classDef.fields()) {
			CtClass fieldType = classPool.get((fd.type().equals(FieldDefinition.VoidClass.class) ? getFromLoaders(fd.typeName(), loaders) : fd.type()).getName());
			String initer = fd.initializer();
			CtField ctField = new CtField(fieldType, fd.name(), ctClass);
			classFields.put(fd.name(), ctField);
			
			if(!"".equals(initer)) {
				ctClass.addField(ctField, CtField.Initializer.byExpr(initer));
			} else {				
				ctClass.addField(new CtField(fieldType, fd.name(), ctClass));
			}
			ctField.setModifiers(ctField.getModifiers() | HModifier.getModifier(fd.modifiers()));
							
			log("Added Class Member Field [" + fd.name() + "], modifiers " + HModifier.getModifierNames(ctField.getModifiers()));
		}
	}

	/**
	 * @param urlPropDefined
	 * @param classPool
	 * @param annotatedMethodMap
	 * @param ctClass
	 * @param isBaseInterface
	 * @param sourceNode
	 * @throws NotFoundException
	 * @throws CannotCompileException
	 */
	protected static void processMethods(boolean urlPropDefined,
			ClassPool classPool,
			Map<Method, MethodDefinition> annotatedMethodMap, CtClass ctClass,
			boolean isBaseInterface, Node sourceNode, final Map<String, CtField> classFields) throws NotFoundException,
			CannotCompileException {
		for(Map.Entry<Method, MethodDefinition> mdPair: annotatedMethodMap.entrySet()) {
			Method method = mdPair.getKey();			
			MethodDefinition md = mdPair.getValue();
			if(!"".equals(md.fieldGetter()) && !"".equals(md.fieldSetter())) throw new RuntimeException("The method [" + method.toGenericString() + "] cannot be defined as a fieldGetter and a fieldSetter", new Throwable()); 
			CtMethod ctMethod = null;
			CtField fld = classFields.get(md.fieldGetter());
			if(!"".equals(md.fieldGetter())) {
				if(fld==null) throw new RuntimeException("Failed to create getter for field [" + md.fieldGetter() + "] as field was not found.", new Throwable());
				ctMethod = CtNewMethod.getter(method.getName(), fld);
			} else 	if(!"".equals(md.fieldSetter())) {
				if(fld==null) throw new RuntimeException("Failed to create setter for field [" + md.fieldGetter() + "] as field was not found.", new Throwable());
				ctMethod = CtNewMethod.setter(method.getName(), fld);
			} else {
				ctMethod = new CtMethod(getReturnType(method, md.retType(), classPool), method.getName(), 
						getParameterSignature(method, md.params(), classPool), ctClass);				
			}

			
			//if(isBaseInterface) {}
			ctClass.addMethod(ctMethod);
			
			
			String beforeCode = urlPropDefined ? getSourceFromNode(md.bref(), sourceNode) : md.bsrc();
			if(md.callSuper() && !isBaseInterface) {
				beforeCode += "super." + method.getName() + "($$);";
			}
			if(!"".equals(beforeCode)) {
				try {
					if(md.replace() || java.lang.reflect.Modifier.isAbstract(method.getModifiers())) {					
						ctMethod.setBody(beforeCode);					
						//ctMethod.insertBefore(beforeCode);
					} else {
						ctMethod.insertBefore(beforeCode);
					}
				} catch (Exception e) {
					throw new RuntimeException("Failed to compile method beforeCode [" + beforeCode + "] for method [" + ctMethod.getLongName() + "]",e);
				}
			}
			for(FieldDefinition fd: md.fields()) {
				ctMethod.addLocalVariable(fd.name(), getCtClass(fd.typeName(), fd.type(), classPool));
			}
			
			String afterCode = urlPropDefined ? getSourceFromNode(md.aref(), sourceNode) : md.asrc();
			if(!"".equals(afterCode)) {
				ctMethod.insertAfter(afterCode);
			}
			
			ctMethod.setModifiers(ctMethod.getModifiers() | HModifier.getModifier(md.modifiers()));
			ctMethod.setModifiers(ctMethod.getModifiers() & ~Modifier.ABSTRACT);
			log("Added method [" + ctMethod.getLongName() + "], modifiers " + HModifier.getModifierNames(ctMethod.getModifiers()));

		}
	}
	
	
	public static CtClass getCtClass(String typeName, Class<?> type, ClassPool classPool) throws NotFoundException {
		return classPool.get("".equals(typeName) ? type.getName() : typeName);
	}
	/**
	 * Returns the CtClass for the return type
	 * @param method
	 * @param rtd
	 * @param classPool
	 * @return
	 * @throws NotFoundException
	 */
	public static CtClass getReturnType(Method method, ReturnTypeDefinition rtd, ClassPool classPool) throws NotFoundException {
		if(rtd.typeName().equals("") && rtd.type().equals(ReturnTypeDefinition.VoidClass.class)) {
			return classPool.get(method.getReturnType().getName());
		} else if(rtd.typeName().equals("")) {
			return classPool.get(rtd.type().getName());
		} else {
			return classPool.get(rtd.typeName());
		}
	}
	
	/**
	 * @param method
	 * @param pds
	 * @param classPool
	 * @return
	 * @throws NotFoundException
	 */
	public static CtClass[] getParameterSignature(Method method, ParameterDefinition[] pds, ClassPool classPool) throws NotFoundException {
		int i = method.getParameterTypes().length + pds.length;
		int cntr = 0;
		CtClass[] signature = new CtClass[i];
		for(; cntr < method.getParameterTypes().length; cntr++) {
			signature[cntr] = classPool.get(method.getParameterTypes()[cntr].getName());
		}
		for(; cntr < i; cntr++) {
			signature[cntr] = pds[i].type().equals(ParameterDefinition.VoidClass.class) ?
					classPool.get(pds[i].typeName()) :
					classPool.get(pds[i].type().getName());
		}
		return signature;
	}
	
	/**
	 * Converts an array of java classes to an array of javassist ctclasses
	 * @param javaArr An array of java classes
	 * @param classPool the javassist classpool
	 * @return an array of javassist ctclasses
	 * @throws NotFoundException
	 */
	public static CtClass[] getCtClassArray(Class<?>[] javaArr, ClassPool classPool) throws NotFoundException {
		if(javaArr==null || javaArr.length<1) return new CtClass[]{};
		CtClass[] clazzes = new CtClass[javaArr.length];
		for(int i = 0; i < javaArr.length; i++) {
			clazzes[i] = classPool.get(javaArr[i].getName());
		}
		return clazzes;
	}
	
	/**
	 * Creates a class loader for the passed file name using the current thread's context classloader as the parent.
	 * @param fileName The file name
	 * @return a classloader referencing the passed file name
	 */
	public static ClassLoader getClassLoaderForFile(CharSequence fileName) {
		return getClassLoaderForFile(Thread.currentThread().getContextClassLoader(), fileName);
	}
	
	
	/**
	 * Creates a class loader for the passed file name
	 * @param parent The parent class loader
	 * @param fileName The file name
	 * @return a classloader referencing the passed file name
	 */
	public static ClassLoader getClassLoaderForFile(ClassLoader parent, CharSequence fileName) {
		if(fileName==null) throw new IllegalArgumentException("Passed file name was null", new Throwable());
		File file = new File(fileName.toString());
		if(!file.exists()) throw new IllegalArgumentException("Passed file name [" + file + "] does not exist", new Throwable());
		try {
			return new URLClassLoader(new URL[]{file.toURI().toURL()}, parent);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ClassLoader for file [" + fileName + "]", e);
		}
	}
	
	
	/**
	 * Convenience utility to create an array of classloaders
	 * @param classLoaders
	 * @return
	 */
	public static ClassLoader[] clArr(ClassLoader...classLoaders) {
		if(classLoaders==null) return new ClassLoader[]{};
		else return classLoaders;
	}
	
	/**
	 * Returns the parsed ClassDef URL prop document
	 * @param urlProp The name fo the system property defining the URL where the XML can be read from
	 * @return an XML node
	 */
	protected static Node getClassDefNode(String urlProp) {
		return XMLHelper.parseXML(System.getProperty(urlProp)).getDocumentElement();
	}
	
	/**
	 * Retrieves the code defined in the source node for the passed expression
	 * @param nodeName The expression defining which source fragment to retrieve
	 * @param sourceNode The source node to retrieve the code from
	 * @return A source fragment, or null if one was not found.
	 */
	protected static String getSourceFromNode(String nodeName, Node sourceNode) {
		try {
			return XMLHelper.getNodeTextValue(XMLHelper.xGetNode(sourceNode, nodeName));
		} catch (Exception e) {
			return null;
		}
	}


	/**
	 * @param classDef
	 * @param methods
	 * @param classPool
	 * @param annotatedMethodMap
	 * @param loaders
	 * @throws ClassNotFoundException
	 * @throws RuntimeException
	 */
	protected static void prepareClassPool(final ClassDefinition classDef, final Set<Method> methods, final ClassPool classPool, final Map<Method, MethodDefinition> annotatedMethodMap, final ClassLoader... loaders) throws ClassNotFoundException, RuntimeException {
		Set<Class<?>> appendClassPath = new HashSet<Class<?>>();
		if(loaders!=null) {
			for(ClassLoader cl: loaders) {
				classPool.appendClassPath(new LoaderClassPath(cl));
			}
		}		
		if(classDef!=null) {
			log("Processing classDef: Preparing ClassPool");
			for(String imp: classDef.imports()) {
				classPool.importPackage(imp);
			}
			for(String ifaceName: classDef.interfaces()) {
				//classPool.appendClassPath(new ClassClassPath(getFromLoaders(ifaceName, loaders)));
				appendClassPath.add(getFromLoaders(ifaceName, loaders));
			}
			if(!"".equals(classDef.superClass())) {
				//classPool.appendClassPath(new ClassClassPath(getFromLoaders(classDef.superClass(), loaders)));
				appendClassPath.add(getFromLoaders(classDef.superClass(), loaders));
			}
			for(FieldDefinition fd: classDef.fields()) {
				if(fd.type().equals(FieldDefinition.VoidClass.class) && "".equals(fd.typeName())) {
					throw new RuntimeException("Invalid Field Definition has type of VoidClass and blank type name [" + fd + "]");
				}
				//classPool.appendClassPath(new ClassClassPath(fd.type().equals(FieldDefinition.VoidClass.class) ? getFromLoaders(fd.typeName(), loaders) : fd.type()));
				appendClassPath.add(fd.type().equals(FieldDefinition.VoidClass.class) ? getFromLoaders(fd.typeName(), loaders) : fd.type());
			}
		}
		if(!methods.isEmpty()) {
			for(Method method: methods) {
				MethodDefinition md = method.getAnnotation(MethodDefinition.class);
				annotatedMethodMap.put(method, md);
				for(ParameterDefinition pd: md.params()) {
					if(pd.type().equals(ParameterDefinition.VoidClass.class) && "".equals(pd.typeName())) {
						throw new RuntimeException("Invalid ParameterDefinition has type of VoidClass and blank type name [" + pd + "]");
					}
					//classPool.appendClassPath(new ClassClassPath(pd.type().equals(ParameterDefinition.VoidClass.class) ? getFromLoaders(pd.typeName(), loaders) : pd.type()));
					appendClassPath.add(pd.type().equals(ParameterDefinition.VoidClass.class) ? getFromLoaders(pd.typeName(), loaders) : pd.type());
				}
				ReturnTypeDefinition rtd = method.getAnnotation(ReturnTypeDefinition.class);
				if(rtd!=null) {
					if("".equals(rtd.typeName())) {
						classPool.appendClassPath(new ClassClassPath(rtd.type()));
						appendClassPath.add(rtd.type());
					} else {
						//classPool.appendClassPath(new ClassClassPath(getFromLoaders(rtd.typeName(), loaders)));
						appendClassPath.add(getFromLoaders(rtd.typeName(), loaders));
					}
				}
				for(FieldDefinition fd: md.fields()) {
					if(fd.type().equals(FieldDefinition.VoidClass.class) && "".equals(fd.typeName())) {
						throw new RuntimeException("Invalid Method Local Field Definition on method [" + method.toGenericString() + "] has type of VoidClass and blank type name [" + fd + "]");
					}
					//classPool.appendClassPath(new ClassClassPath(fd.type().equals(FieldDefinition.VoidClass.class) ? getFromLoaders(fd.typeName(), loaders) : fd.type()));
					appendClassPath.add(fd.type().equals(FieldDefinition.VoidClass.class) ? getFromLoaders(fd.typeName(), loaders) : fd.type());
				}				
			}			
		}
		if(!appendClassPath.isEmpty()) {
			Set<String> packageNames = new HashSet<String>();
			log("Adding [" + appendClassPath.size() + "] Classes to ClassPool");
			for(Class<?> clazz: appendClassPath) {
				classPool.appendClassPath(new ClassClassPath(clazz));
				//classPool.importPackage(clazz.getPackage().getName());
				if(clazz.getPackage()!=null) {
					packageNames.add(clazz.getPackage().getName());
				}
			}
			for(String packageName: packageNames) {
				classPool.importPackage(packageName);
			}
		}
		log("\n\t=================\n\tClassPool Ready\n\t=================\n");
	}
	
	
	/**
	 * Attempts to load the named class from the default class loader or any of the passed class loaders
	 * @param className The class name
	 * @param loaders Additional class loaders to be used in the order provided
	 * @return the loaded class
	 * @throws ClassNotFoundException
	 */
	public static Class<?> getFromLoaders(String className, ClassLoader...loaders) throws ClassNotFoundException {
		if(className==null || className.length()<1) throw new IllegalArgumentException("[getFromLoaders] Passed class name was null", new Throwable());
		Class<?> clazz = null;
		try { clazz = Class.forName(className); } catch (Exception e) {}
		if(clazz!=null) return clazz;
		if(loaders!=null) {
			for(ClassLoader cl: loaders) {
				try { clazz = Class.forName(className, true, cl); } catch (Exception e) {}
				if(clazz!=null) return clazz;
			}
		} 
		throw new ClassNotFoundException("Failed to load the class [" + className + "] from any of the provided loaders");
		
	}
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
