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
package org.helios.scripting.manager;

import java.net.URL;
import java.util.List;

import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: ScriptEngineMetaService</p>
 * <p>Description: Service to expose the meta data of registered script engines through JMX</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.scripting.manager.ScriptEngineMetaService</code></p>
 */
@JMXManagedObject(declared=true, annotated=true)
public class ScriptEngineMetaService extends ManagedObjectDynamicMBean {
	/**  */
	private static final long serialVersionUID = -6146618838743883341L;

	/** The ScriptEngineFactory for which the meta-data is being publsihed */
	protected final ScriptEngineFactory engineFactory;
	/** An instance of the engine for meta-data */
	protected final ScriptEngine engine;
	/** Indicates if the engine is Invocable */
	protected final boolean invocable;
	/** Indicates if the engine is Compilable */
	protected final boolean compilable;
	/** The thread safety of the engine */
	protected final ScriptThreading threading;
	/** The name of the engine */
	protected final String engineClassName;
	/** The classpath location of the engine class */
	protected final URL url;
	/** The ObjectName template for this MODB */
	public static final String OBJECT_NAME_STR = "org.helios.scripting.manager.engines:name=";


	/**
	 * Creates a new ScriptEngineMetaService
	 * @param engineFactory
	 */
	protected ScriptEngineMetaService(ScriptEngineFactory engineFactory, ScriptEngine engine) {
		super();
		this.engineFactory = engineFactory;
		this.engine = engine;
		threading = ScriptThreading.value((String)engineFactory.getParameter("THREADING"));
		engineClassName = engine.getClass().getName();
		url = getEngineClassLocation(engine);
		invocable = (engine instanceof Invocable);
		compilable = (engine instanceof Compilable);
		this.reflectObject(this);
	}

	private static URL getEngineClassLocation(ScriptEngine engine) {
		try {
			return engine.getClass().getProtectionDomain().getCodeSource().getLocation();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getEngineName()
	 */
	@JMXAttribute(name="EngineName", description="The full name of the Script Engine", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getEngineName() {
		return engineFactory.getEngineName();
	}


	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getEngineVersion()
	 */
	@JMXAttribute(name="EngineVersion", description="The version of the Script Engine", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getEngineVersion() {
		return engineFactory.getEngineVersion();
	}


	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getExtensions()
	 */
	@JMXAttribute(name="Extensions", description="An arrays of filename extensions, which generally identify scripts written in the language supported by this ScriptEngine", mutability=AttributeMutabilityOption.READ_ONLY)
	public List<String> getExtensions() {
		return engineFactory.getExtensions();
	}


	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getLanguageName()
	 */
	@JMXAttribute(name="LanguageName", description="The scripting language supported by this engine", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getLanguageName() {
		return engineFactory.getLanguageName();
	}


	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getLanguageVersion()
	 */
	@JMXAttribute(name="LanguageVersion", description="The version of the scripting language supported by this engine", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getLanguageVersion() {
		return engineFactory.getLanguageVersion();
	}


	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getMimeTypes()
	 */
	@JMXAttribute(name="MimeTypes", description="The mimetypes associated with scripts that can be executed by the engine", mutability=AttributeMutabilityOption.READ_ONLY)
	public List<String> getMimeTypes() {
		return engineFactory.getMimeTypes();
	}


	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getNames()
	 */
	@JMXAttribute(name="ShortNames", description="The short names for the ScriptEngine, which may be used to identify the ScriptEngine by the ScriptEngineManager", mutability=AttributeMutabilityOption.READ_ONLY)
	public List<String> getShortNames() {
		return engineFactory.getNames();
	}


	/**
	 * Indicates if the engine is Invocable
	 * @return true if the engine is invocable
	 */
	@JMXAttribute(name="Invocable", description="Indicates if the engine is Invocable", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getInvocable() {
		return invocable;
	}


	/**
	 * Indicates if the engine is Compilable
	 * @return true if the engine is Compilable
	 */
	@JMXAttribute(name="Compilable", description="Indicates if the engine is Compilable", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getCompilable() {
		return compilable;
	}


	/**
	 * Returns the class name of the engine
	 * @return the class name of the engine
	 */
	@JMXAttribute(name="EngineClassName", description="The class name of the engine", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getEngineClassName() {
		return engineClassName;
	}


	/**
	 * Returns the classpath location of the engine class
	 * @return the classpath location of the engine class
	 */
	@JMXAttribute(name="ClassLocation", description="The classpath location of the engine class", mutability=AttributeMutabilityOption.READ_ONLY)
	public URL getClassLocation() {
		return url;
	}

	/**
	 * @return the threading
	 */
	@JMXAttribute(name="ThreadSafety", description="The threading model for this engine", mutability=AttributeMutabilityOption.READ_ONLY)	
	public String getThreadSafety() {
		return threading.name();
	}
}
