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
package org.helios.scripting.manager.script;

import java.util.Collections;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.scripting.manager.ConcurrentBindings;

/**
 * <p>Title: DefaultBindingsRegistry</p>
 * <p>Description: A registry for tracking and providing the default bindings</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.scripting.manager.script.DefaultBindingsRegistry</code></p>
 */
@JMXManagedObject(annotated=true, declared=true, objectName="org.helios.scripting.manager.script:service=DefaultBindingsRegistry")
public class DefaultBindingsRegistry extends ManagedObjectDynamicMBean {
	/** The singleton instance */
	private static volatile DefaultBindingsRegistry instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** default bindings for the {@link javax.script.Bindings.ENGINE_SCOPE} scoped bindings */
	private final ConcurrentBindings engineBindings = new ConcurrentBindings();
	/** default bindings for the {@link javax.script.Bindings.GLOBAL_SCOPE} scoped bindings */
	private final ConcurrentBindings globalBindings = new ConcurrentBindings();
	
	/**
	 * Creates a new DefaultBindingsRegistry 
	 */
	private DefaultBindingsRegistry() {
		this.reflectObject(this);
	}
	
	/**
	 * Returns the DefaultBindingsRegistry singleton instance
	 * @return a DefaultBindingsRegistry
	 */
	public static DefaultBindingsRegistry getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new DefaultBindingsRegistry();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Returns an unmodifiable instance of the global bindings
	 * @return the global bindings
	 */
	public Bindings getGlobalBindings() {
		return (Bindings) Collections.unmodifiableMap(globalBindings);
	}
	
	/**
	 * Returns a copy of the engine bindings
	 * @return the engine bindings
	 */
	public Bindings getEngineBindings() {
		Bindings b = new SimpleBindings();
		b.putAll(engineBindings);
		return b;
	}
	
	/**
	 * Adds a global binding
	 * @param name The name of the binding
	 * @param value The value of the binding
	 */
	public void addGlobalBinding(String name, Object value) {
		if(name==null) throw new IllegalArgumentException("Passed binding name was null", new Throwable());
		if(value==null) throw new IllegalArgumentException("Passed binding value was null", new Throwable());
		globalBindings.put(name, value);
	}
	
	/**
	 * Adds an engine binding
	 * @param name The name of the binding
	 * @param value The value of the binding
	 */
	public void addEngineBinding(String name, Object value) {
		if(name==null) throw new IllegalArgumentException("Passed binding name was null", new Throwable());
		if(value==null) throw new IllegalArgumentException("Passed binding value was null", new Throwable());
		engineBindings.put(name, value);
	}
	
	
	
	/**
	 * Loads the default bindings from the passed provider 
	 * @param provider the bindings provider
	 */
	public void load(DefaultBindingsProvider provider) {
		if(provider==null) throw new IllegalArgumentException("Passed provider was null", new Throwable());
		Bindings bindings = provider.getEngineBindings();
		if(bindings!=null) {
			engineBindings.putAll(bindings);
		}
		bindings = provider.getGlobalBindings();
		if(bindings!=null) {
			globalBindings.putAll(bindings);
		}
	}
	
	/**
	 * Creates an instance of the passed DefaultBindingsProvider class name and then loads from it
	 * @param providerClassName The class name of a DefaultBindingsProvider. Must have a public parameterless Ctor.
	 * @param cl The class loader to use to load the class.
	 */
	public void load(String providerClassName, ClassLoader cl)  {
		try {
			ClassLoader loader = cl==null ? Thread.currentThread().getContextClassLoader() : cl;
			DefaultBindingsProvider provider = (DefaultBindingsProvider)loader.loadClass(providerClassName).newInstance();
			load(provider);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load DefaultBindingsProvider Class [" + providerClassName + "], e");
		}
	}
	
	/**
	 * Creates an instance of the passed DefaultBindingsProvider class name 
	 * using the thread's context classloader and then loads from it
	 * @param providerClassName The class name of a DefaultBindingsProvider. Must have a public parameterless Ctor.
	 */
	public void load(String providerClassName)  {
		load(providerClassName, null);
	}
	
	
}
