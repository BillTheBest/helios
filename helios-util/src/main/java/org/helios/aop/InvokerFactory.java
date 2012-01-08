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

import java.lang.annotation.Annotation;
import java.security.SecureClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.MethodInfo;

/**
 * <p>Title: InvokerFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class InvokerFactory {

	/**
	 * Bytecode generates an invoker class.
	 * @param <I>
	 * @param iface
	 * @param targetClass
	 * @return
	 */
	public static <I> I generateInvoker(Object targetObject, Class<I> iface, Class<?> targetClass, Class<? extends Annotation>[] annotations) throws Exception {
		I invoker = null;
		String targetClassName = targetClass.getName();
		String packageName = targetClass.getPackage().getName();
		String simpleClassName = "_heliosAOPInvoker" + targetClass.getSimpleName();
		ClassPool cp = ClassPool.getDefault();
		CtClass ctClass = cp.get(targetClassName);
		CtClass ctInterface = cp.get(iface.getName());
		CtClass invokerClass = cp.makeClass(packageName + "." + simpleClassName);
		invokerClass.addInterface(ctInterface );
		invokerClass.addField(new CtField(ctClass, "invocationTarget", invokerClass));
		CtClass[] annotationClasses = new CtClass[annotations.length];
		for(int i = 0; i < annotations.length; i++) {
			annotationClasses[i] = cp.get(annotations[i].getName());
		}
		CtConstructor ctor=new CtConstructor(new CtClass[]{ctClass}, invokerClass);
		ctor.setBody("invocationTarget = $1;");
		invokerClass.addConstructor(new CtConstructor(new CtClass[]{ctClass}, invokerClass));
		Set<CtMethod> ifaceMethods = new HashSet<CtMethod>();
		Collections.addAll(ifaceMethods, ctInterface.getMethods());
		Collections.addAll(ifaceMethods, ctInterface.getDeclaredMethods());
		for(CtMethod imethod: ifaceMethods) {
			CtMethod method = CtNewMethod.copy(imethod,invokerClass,null);
			if(isAnnotationIn(method, annotationClasses)) {				
				StringBuilder body = new StringBuilder("invocationTarget.");
				body.append(method.getName()).append("(");
				CtClass[] paramTypes=method.getParameterTypes();
				for(int i = 0; i < paramTypes.length; i++) {
					body.append("$").append(i).append(",");
				}
				if(paramTypes.length > 0) {
					body.deleteCharAt(body.length()-1);
				}
				body.append(");");
				method.setBody(body.toString());
			} else {
				method.setBody(";");
			}
			invokerClass.addMethod(method);
		}
		byte[] byteCode = invokerClass.toBytecode();
		Class<I> iclass = new DirectLoader().load(packageName + "." + simpleClassName, byteCode);
		invoker = iclass.getDeclaredConstructor(targetClass).newInstance(targetObject);
		return invoker;
	}
	
	
	
	/**
	 * @param target
	 * @param annotations
	 * @return
	 */
	protected static boolean isAnnotationIn(CtMethod targetMethod, CtClass[] annotations) {
		if(targetMethod==null || annotations==null || annotations.length < 1) return false;
		MethodInfo minfo = targetMethod.getMethodInfo();
		AnnotationsAttribute attr = (AnnotationsAttribute) minfo.getAttribute(AnnotationsAttribute.visibleTag);
		if(attr==null) return false;
		for(javassist.bytecode.annotation.Annotation methodAnn: attr.getAnnotations()) {
			for(CtClass annClass: annotations) {
				if(methodAnn.getTypeName().equals(annClass.getName())) return true;
			}
		}
		return false;
	}
	
	protected static class DirectLoader extends SecureClassLoader
	{
	    protected DirectLoader() {
	        super(InvokerFactory.class.getClassLoader());
	    }
	    
	    protected Class load(String name, byte[] data) {
	        return super.defineClass(name, data, 0, data.length);
	    }
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}
	
	public static void log(Object message) {
		System.out.println(message);
	}

}
