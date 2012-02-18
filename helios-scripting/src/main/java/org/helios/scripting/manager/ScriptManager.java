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

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.helios.classloading.OpenURLClassLoader;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.URLHelper;
import org.helios.io.file.RecursiveDirectorySearch;
import org.helios.io.file.filters.ConfigurableFileExtensionFilter;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: ScriptManager</p>
 * <p>Description: A service for managing, invoking and scheduling executable scripts.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.scripting.manager.ScriptManager</code></p>
 */
@JMXManagedObject(annotated=true, declared=true, objectName=ScriptManager.OBJECT_NAME_STR)
public class ScriptManager extends ManagedObjectDynamicMBean {
	/**  */
	private static final long serialVersionUID = -4801507671508779813L;
	/** The single script engine manager used for creating all script engine factories */
	protected ScriptEngineManager engineManager;
	/** The class loader used by the scriptEngineManager */
	protected final OpenURLClassLoader scriptEngineMgrClassLoader;
	/** A cache of ScriptEngineFactories keyed by engine name */
	protected final Map<String, ScriptEngineFactory> engineFactories = new ConcurrentHashMap<String, ScriptEngineFactory>();
	/** A cache of ScriptEngineFactories keyed by short engine name */
	protected final Map<String, ScriptEngineFactory> engineFactoriesShortName = new ConcurrentHashMap<String, ScriptEngineFactory>();
	/** A cache of ScriptEngineFactories keyed by extension */
	protected final Map<String, ScriptEngineFactory> engineFactoriesExtension = new ConcurrentHashMap<String, ScriptEngineFactory>();
	/** A cache of ScriptEngineFactories keyed by mime type */
	protected final Map<String, ScriptEngineFactory> engineFactoriesMimeType = new ConcurrentHashMap<String, ScriptEngineFactory>();
	/** A cache of ScriptEngineFactories keyed by language name */
	protected final Map<String, ScriptEngineFactory> engineFactoriesLanguage = new ConcurrentHashMap<String, ScriptEngineFactory>();
	
	
	
	/** The ScriptManager singleton instance */
	protected static volatile ScriptManager instance = null;
	/** The ScriptManager singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** The default ObjectName for this MODB */
	public static final String OBJECT_NAME_STR = "org.helios.scripting.manager:service=ScriptManager";
	
	/** The binding name for the Helios MBeanServer */
	public static final String HELIOS_MBEANSERVER_BN = "heliosMBeanServer";
	
	
	/**
	 * Creates a new ScriptManager. Private ctor. Access through singleton.
	 */
	private ScriptManager() {
		super();
		scriptEngineMgrClassLoader = new OpenURLClassLoader(this.getClass().getClassLoader());
		engineManager = new ScriptEngineManager(scriptEngineMgrClassLoader);
		seekNewEngines(false);
		//globalBindings.put(HELIOS_MBEANSERVER_BN, server==null ? JMXHelper.getHeliosMBeanServer() : server);
	}
	
	/**
	 * Acquires the ScriptManager singleton
	 * @return the ScriptManager singleton
	 */
	public static ScriptManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ScriptManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Returns the keys in the global bindings
	 * @return the keys in the global bindings
	 */
	@JMXAttribute(name="GlobalBindingKeys", description="The keys in the global bindings", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getGlobalBindingKeys() {
		return engineManager.getBindings().keySet().toArray(new String[0]);
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			getInstance();
			System.out.println("Test ScriptManager Instantiated");
			getInstance().addScriptEngineClassPath(true, new File("/home/nwhitehead/libs/javascript/rhino/rhino1_7R2/js.jar").toURI().toURL());
			for(String jar: RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(false, ".jar"), new File("/home/nwhitehead/hprojects/scripting/trunk/engines/").getAbsolutePath())) {
				//System.out.println("Adding ClassPath URL:" + jar );
				getInstance().addScriptEngineClassPath(false, false, new File(jar).toURI().toURL());
			}
			getInstance().seekNewEngines(false);
			
			
			
			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}

	/**
	 * Returns the platform ScriptEngineManager
	 * @return the engineManager
	 */
	public ScriptEngineManager getEngineManager() {
		return engineManager;
	}
	
	/**
	 * Executes a search for new and unregistered script engine factories. 
	 * For each found script engine factory, if the name is not registered, it is registered and indexed.
	 * Triggered at singleton initialization and after a new classpath is added to the instance's classloader
	 * @param pref Indicates if this is a seek based on a preferred classpath URL. If true, located factories in this seek will overwrite the search indexes of any existing engine factories 
	 */
	protected synchronized void seekNewEngines(boolean pref) {
		engineManager = new ScriptEngineManager(scriptEngineMgrClassLoader);
		for(ScriptEngineFactory sef: engineManager.getEngineFactories()) {
			String name = sef.getEngineName().toLowerCase().trim();
			System.out.println("Examining Engine Name [" + name + "]");
			if(!engineFactories.containsKey(name)) {
				synchronized(engineFactories) {
						if(!engineFactories.containsKey(name)) {							
							engineFactories.put(name, sef);
							registerEngine(pref, sef, engineFactoriesShortName, sef.getNames().toArray(new String[0]));
							registerEngine(pref, sef, engineFactoriesExtension, sef.getExtensions().toArray(new String[0]));
							registerEngine(pref, sef, engineFactoriesMimeType, sef.getMimeTypes().toArray(new String[0]));
							registerEngine(pref, sef, engineFactoriesLanguage, sef.getLanguageName());						
							try {
								ScriptEngine eng = sef.getScriptEngine();
								server.registerMBean(new ScriptEngineMetaService(sef, eng), JMXHelper.objectName(ScriptEngineMetaService.OBJECT_NAME_STR + sef.getEngineName()));
							} catch (Throwable e) {
								Throwable cause = e.getCause();
								if((e instanceof ClassNotFoundException) || (e instanceof NoClassDefFoundError)) {
									System.out.println("Failed to load eninge [" + name + "]. Class Dependency Error:" + e.getMessage());
								} else if(cause != null && ((cause instanceof ClassNotFoundException) || (cause instanceof NoClassDefFoundError))) {
									System.out.println("Failed to load eninge [" + name + "]. Class Dependency Error:" + cause.getMessage());
								} else {
									//throw new RuntimeException("Failed to load engine factory [" + name + "]", e);
									System.err.println("Failed to load engine factory [" + name + "]:" + e);
								}
							}							
						}
				}
			}
		}
	}
	
	/**
	 * Returns the ScriptEngineFactory that supports the passed language
	 * @param language The language name
	 * @return A ScriptEngineFactory or null if one was not found.
	 */
	@JMXOperation(name="getFactoryByLanguage", description="Returns the ScriptEngineFactory that supports the passed language")
	public ScriptEngineFactory getFactoryByLanguage(@JMXParameter(name="language", description="The language name") String language) {
		if(language==null) return null;
		return engineFactoriesLanguage.get(language.toLowerCase().trim());
	}
	
	
	/**
	 * Returns the ScriptEngineFactory that is identified by the passed mime type
	 * @param mimeType The mime type
	 * @return A ScriptEngineFactory or null if one was not found.
	 */
	@JMXOperation(name="getFactoryByExtension", description="Returns the ScriptEngineFactory that is identified by the passed mime type")
	public ScriptEngineFactory getFactoryByMimeType(@JMXParameter(name="mimeType", description="The mime type") String mimeType) {
		if(mimeType==null) return null;
		return engineFactoriesMimeType.get(mimeType.toLowerCase().trim());
	}
	
	
	/**
	 * Returns the ScriptEngineFactory that is identified by the passed file name extension
	 * @param extension The file name extension
	 * @return A ScriptEngineFactory or null if one was not found.
	 */
	@JMXOperation(name="getFactoryByExtension", description="Returns the ScriptEngineFactory that is identified by the passed file name extension")
	public ScriptEngineFactory getFactoryByExtension(@JMXParameter(name="extension", description="The file name extension") String extension) {
		if(extension==null) return null;
		return engineFactoriesExtension.get(extension.toLowerCase().trim());
	}

	/**
	 * Returns the ScriptEngineFactory with the passed name
	 * @param name The name of the ScriptEngineFactory
	 * @return The named ScriptEngineFactory or null if one was not found.
	 */
	@JMXOperation(name="getFactoryByName", description="Returns the ScriptEngineFactory with the passed name")
	public ScriptEngineFactory getFactoryByName(@JMXParameter(name="engineName", description="The name of the ScriptEngineFactory") String name) {
		if(name==null) return null;
		ScriptEngineFactory se = engineFactories.get(name.toLowerCase().trim());
		if(se==null) {
			se = engineFactoriesShortName.get(name.toLowerCase().trim());
		}
		return se;
	}
	
	
	
	/**
	 * Stores a ScriptEngineFactory into cache keyed by the passed keys. The keys are forced to lower case. 
	 * @param pref Indicates if this is a preferred engine factory. If it is, it will overwrite any existing entries.
	 * @param se The ScriptEngineFactory to cache
	 * @param cache The ScriptEngineFactory cache
	 * @param keys The keys to cache the ScriptEngineFactory by
	 */
	protected void registerEngine(boolean pref, ScriptEngineFactory se, Map<String, ScriptEngineFactory> cache, String...keys) {
		if(keys!=null) {
			for(String key: keys) {
				if(key==null) continue;
				key = key.toLowerCase().trim();
				if("".equals(key)) continue;
				if(!cache.containsKey(key)) {
					synchronized(cache) {
						if(!cache.containsKey(key)) {
							cache.put(key, se);
						}
					}
				}				
			}
		}
	}
	
	/**
	 * Adds the passed URL to the script manager's classpath
	 * @param url A string form of a URL pointing to a class direcotry or jar
	 * @param pref Indicates if the seek executed for this classpath add should overwrite any existing engine factories already indexed
	 */
	public void addScriptEngineClassPath(boolean pref, String url) {
		addScriptEngineClassPath(pref, URLHelper.toURL(url));
	}
	
	/**
	 * Adds the passed URL to the script manager's classpath
	 * @param triggerSeek If true, triggers a seek.
	 * @param url A URL pointing to a class direcotry or jar
	 * @param pref Indicates if the seek executed for this classpath add should overwrite any existing engine factories already indexed
	 */
	public void addScriptEngineClassPath(boolean triggerSeek, boolean pref, URL url) {
		if(url==null) throw new IllegalArgumentException("Passed URL was null", new Throwable());
		this.scriptEngineMgrClassLoader.addURLs(url);
		if(triggerSeek) {
			seekNewEngines(pref);
		}
	}
	
	/**
	 * Adds the passed URL to the script manager's classpath
	 * @param url A URL pointing to a class direcotry or jar
	 * @param pref Indicates if the seek executed for this classpath add should overwrite any existing engine factories already indexed
	 */
	public void addScriptEngineClassPath(boolean pref, URL url) {
		addScriptEngineClassPath(true, pref, url);
	}
	

}
