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
package org.helios.tracing.persistence.miniorm;

import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;

import org.apache.log4j.BasicConfigurator;
import org.helios.tracing.core.trace.Agent;
import org.helios.tracing.core.trace.AgentMetric;
import org.helios.tracing.core.trace.Host;
import org.helios.tracing.core.trace.Metric;
import org.helios.tracing.core.trace.TraceValue;
import org.helios.tracing.persistence.miniorm.TraceObjectIntrospector.TraceObjectMetaData;

/**
 * <p>Title: TraceObjectPersistorFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.persistence.miniorm.TraceObjectPersistorFactory</code></p>
 */

public class TraceObjectPersistorFactory {
	/**
	 * Creates a new persistor for the passed class
	 * @param clazz The class to create a persistor for
	 * @return an ITracePersistor for the passed class.
	 */
	public static ITracePersistor createPersistor(Class<?> clazz) {
		ClassPool classPool = new ClassPool();
		TraceObjectMetaData metaData = TraceObjectIntrospector.metaData(clazz);
		CtClass ctClass = null;
		try {
			classPool.appendClassPath(new ClassClassPath(clazz));
			ctClass = classPool.makeClass(clazz.getName() + ".javassist" + clazz.getSimpleName()  + "Persistor");			
			CtClass superClass = classPool.get(AbstractTracePersistor.class.getName());
			ctClass.setSuperclass(superClass);
			CtMethod ctMethod = new CtMethod(CtClass.voidType, "doBinds", new CtClass[]{classPool.get(Object.class.getName()), classPool.get(PreparedStatement.class.getName())}, ctClass);			
			ctMethod.setModifiers(ctMethod.getModifiers() & Modifier.PROTECTED);
			ctClass.addMethod(ctMethod);			
			CtConstructor ctor = new CtConstructor(new CtClass[]{classPool.get(Class.class.getName())}, ctClass);			
			ctClass.addConstructor(ctor);
			ctor.setBody("{ super($1); }");
			StringBuilder body = new StringBuilder("{");
			int cntr = 1;
			for(String columnName: metaData.getColumnNames()) {
				String accessor = metaData.getAccessor(columnName);
				if(metaData.getBindType(columnName)==Types.TIMESTAMP) {
					body.append("$2.setTimestamp(").append(cntr).append(", new java.sql.Timestamp(").append("((" + clazz.getName() + ")$1).").append(accessor).append(".getTime()));");
				} else {
					body.append("$2.setObject(").append(cntr).append(",").append("($w)((" + clazz.getName() + ")$1).").append(accessor).append(",").append(metaData.getBindType(columnName)).append(");");
				}
				body.append("\n");
				
				cntr++;
			}
			body.append("}");
			//log(body);
			ctMethod.setBody(body.toString());
			ctMethod.setModifiers(ctMethod.getModifiers() & ~Modifier.ABSTRACT);

			
			//ctClass.writeFile("/tmp");			
			Class<?> persistorClass = ctClass.toClass();
			Constructor<ITracePersistor> tpCtor = (Constructor<ITracePersistor>) persistorClass.getDeclaredConstructor(Class.class);
			//.newInstance(clazz);
			//log("JavaCtor:" + tpCtor.toGenericString());
			ITracePersistor persistor = tpCtor.newInstance(clazz);			
			return persistor;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to create trace persistor for class [" + clazz.getName() + "]", e);
		} finally {
			if(ctClass!=null) try { ctClass.detach(); } catch (Exception e) {}
		}
	}
	
	public static void main(String[] args) {
		log("Persistor Test");
		BasicConfigurator.configure();
		TraceObjectPersistorFactory.createPersistor(Host.class);
		TraceObjectPersistorFactory.createPersistor(Agent.class);
		TraceObjectPersistorFactory.createPersistor(Metric.class);
		TraceObjectPersistorFactory.createPersistor(AgentMetric.class);		
		TraceObjectPersistorFactory.createPersistor(TraceValue.class);
		try { Thread.currentThread().join(); } catch (Exception e) {}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
