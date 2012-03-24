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

import java.io.File;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.log4j.Logger;
import org.helios.helpers.CollectionHelper;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.URLHelper;
import org.helios.scripting.manager.ScriptThreading;



/**
 * <p>Title: ScriptInstance</p>
 * <p>Description: A script management container.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.scripting.manager.ScriptInstance</code></p>
 * <p>Execution Models:<ul>
 * 	<li>Parameters:<ul>
 * 		<li>Simple Arguments (<code>args[]</code>)</li>
 * 		<li>Invocation Bindings invocation scoped, name value pairs</li>
 * 	</ul></li>
 * 	<li>Output IO:<ul>
 * 		<li>Out</li>
 * 		<li>Err</li>
 * 	</ul></li>
 * 	<li>Return Value:<ul>
 * 		<li>print to output</li>
 * 		<li>wrapped response object</li>
 * 		<li>simple invocation return</li>
 * 	</ul></li>
 * </ul></p>
 * @ToDo:  Handle source update
 * @ToDo:  Propagate source change notifications
 */
public class ScriptInstance implements SourceChangeListener, ScriptEngine, Invocable {
	/** Indicates if the underlying engine supports script compilation */
	protected final boolean compilable;
	/** Indicates if the underlying engine supports script invocation */
	protected final boolean invocable;
	/** The thread safety of the underlying script engine */
	protected final ScriptThreading threading;
	/** The last time the source was compiled */
	protected final AtomicLong timeStamp = new AtomicLong(0L); 
	/** The compiled script if the engine supports compilable */
	protected final AtomicReference<CompiledScript> compiled = new AtomicReference<CompiledScript>(null);
	/** The script invocable if the engine supports invocable */
	protected final AtomicReference<Invocable> invoker = new AtomicReference<Invocable>(null);	
	/** The script engine for this script instance */
	protected final AtomicReference<ScriptEngine> scriptEngine = new AtomicReference<ScriptEngine>(null);
	/** The configured minimum check time for source updates */
	public final long minCheckTime;
	/** The local bindings injected into each invocation */
	protected final Map<String, Object> localBindings = new HashMap<String, Object>();
	
	/** The source code container */
	protected final Source source;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	
	/** The default minimum check time for source updates */
	public static final long DEFAULT_MIN_CHECK_TIME = 15000;
	/** The system prop name for overriding the min check time */
	public static final String DEFAULT_MIN_CHECK_TIME_PROP = ScriptInstance.class.getPackage().getName() + ".minchecktime";
	
	
	
	/**
	 * Creates a new ScriptInstance managing a script supplied by the passed File
	 * @param file The java io file of the script to manage.
	 */
	public ScriptInstance(File file) {
		this(URLHelper.toURL(file));
	}
	
	/**
	 * Creates a new ScriptInstance managing a script supplied by the passed source text
	 * @param source The source for the script
	 */
	public ScriptInstance(CharSequence source) {
		try {
			this.source = new Source(source);
			minCheckTime = ConfigurationHelper.getLongSystemThenEnvProperty(DEFAULT_MIN_CHECK_TIME_PROP, DEFAULT_MIN_CHECK_TIME);
			scriptEngine.set(new ScriptEngineManager().getEngineByExtension(this.source.getExtension()));
			compilable = scriptEngine instanceof Compilable;
			invocable = scriptEngine instanceof Invocable;
			threading = ScriptThreading.value((String)scriptEngine.get().getFactory().getParameter("THREADING"));
			if(compilable) {
				compiled.set(((Compilable)scriptEngine).compile(this.source.getSource()));
			}
		} catch (Exception e) {
			log.error("Failed to create script instance for raw source", e);
			throw new RuntimeException("Failed to create script instance for raw source", e);
		}
		
	}
	
	
	
	/**
	 * Creates a new ScriptInstance managing a script supplied by the passed URL.
	 * @param url The URL of the script to manage.
	 */
	public ScriptInstance(URL url) {		
		try {
			
			minCheckTime = ConfigurationHelper.getLongSystemThenEnvProperty(DEFAULT_MIN_CHECK_TIME_PROP, DEFAULT_MIN_CHECK_TIME);
			source = new Source(minCheckTime, url);
			scriptEngine.set(new ScriptEngineManager().getEngineByExtension(source.getExtension()));
			compilable = scriptEngine instanceof Compilable;
			invocable = scriptEngine instanceof Invocable;
			threading = ScriptThreading.value((String)scriptEngine.get().getFactory().getParameter("THREADING"));
			if(compilable) {
				compiled.set(((Compilable)scriptEngine).compile(source.getSource()));
			}
		} catch (Exception e) {
			log.error("Failed to create script instance for URL [" + url + "]", e);
			throw new RuntimeException("Failed to create script instance for URL [" + url + "]", e);
		}
	}
	
//	/**
//	 * Checks the timestamp of the source URL and updates the instance from the source
//	 * if it it is later than the source already loaded.
//	 */
//	protected void checkForUpdatedSource() {
//		long timestamp = URLHelper.getLastModified(scriptUrl);
//		if(timestamp > timeStamp.get()) {
//			source.set(URLHelper.getTextFromURL(scriptUrl));
//			timeStamp.set(timestamp);
//			if(compilable) {
//				try {
//					compiled.set(((Compilable)scriptEngine).compile(source.get()));
//				} catch (ScriptException e) {
//					throw new RuntimeException("Failed to compile script [" + scriptUrl + "]", e);
//				}
//			}					
//		}
//	}
	

	
	/**
	 * Executes the script with no parameters
	 * @return the return value of the script
	 */
	public Object exec() {
		//checkForUpdatedSource();
		try {
			if(compilable) {
				if(compiled.get()==null) {
					compiled.set(((Compilable)scriptEngine).compile(source.getSource()));
				}
				return compiled.get().eval(getLBindings());
			} else {
				return scriptEngine.get().eval(source.getSource(), getLBindings());
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute script [" + source.getName() + "]", e);
		}
	}
	
	
	protected Bindings getLBindings() {
		return new SimpleBindings(localBindings);
	}
	
	/**
	 * Callback from a source when the underlying source changes.
	 * @param source The Source object that changed
	 */
	public void onSourceChange(Source source) {
		
	}
	
	/**
	 * Executes the script with no parameters
	 * @param args An array of named objects to be bound. Expected as <b><code>name1, val1, name2, val2 etc.</code></b>.
	 * @return the return value of the script
	 */
	public Object exec(Object...args) {
		//checkForUpdatedSource();
		try {
			if(compilable) {
				if(compiled.get()==null) {
					compiled.set(((Compilable)scriptEngine).compile(source.getSource()));
				}
				return compiled.get().eval(new SimpleBindings(CollectionHelper.createNamedValueMap(args)));
			} else {
				return scriptEngine.get().eval(source.getSource(), new SimpleBindings(CollectionHelper.createNamedValueMap(args)));
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute script [" + source.getName() + "]", e);
		}
	}
	
	public void reload() {
		compiled.set(null);
		source.load();
	}

	/**
	 * Retrieves a value set in the state of this engine.
	 * @param key The key whose value is to be returned 
	 * @return the value for the given key 
	 * @see javax.script.ScriptEngine#get(java.lang.String)
	 */
	public Object get(String key) {
		if(ScriptEngine.FILENAME.equals(key)) {
			return source.getName();
		}
		return scriptEngine.get().getBindings(ScriptContext.ENGINE_SCOPE).get(key);
	}
	
	public void set(String key, Object value) {
		scriptEngine.get().getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
	}

	/**
	 * Returns the engine instance scoped bindings
	 * @return the engine instance scoped bindings
	 */
	public Bindings getBindings() {
		return scriptEngine.get().getBindings(ScriptContext.ENGINE_SCOPE);
	}

	/**
	 * Sets an engine instance scoped binding
	 * @param key the key of the binding 
	 * @param value the value of the binding
	 * @see javax.script.ScriptEngine#put(java.lang.String, java.lang.Object)
	 */
	public void put(String key, Object value) {
		scriptEngine.get().put(key, value);
	}

	/**
	 * Sets the engine level bindings
	 * @param bindings The bindings to set
	 */
	public void setBindings(Bindings bindings) {
		scriptEngine.get().setBindings(bindings, ScriptContext.ENGINE_SCOPE);
	}




	/**
	 * @return the timeStamp
	 */
	public long getTimestamp() {
		return timeStamp.get();
	}




	/**
	 * Returns a string representing this script instance
	 * @return a <code>String</code> representation  of this object.
	 */
	public String toString() {
	    return new StringBuilder().append("ScriptInstance [").append(source.getName()).append("\n]").toString();
	}
	
	
	
	//===============================================================================================================================
	//				Invocable and ScriptEngine Implementation
	//				These interfaces are implemented as a convenience
	//				
	//===============================================================================================================================

	/**
	 * Returns an implementation of an interface using member functions of a scripting object compiled in the interpreter.
	 * @param clazz The Class object of the interface to return. 
	 * @return An instance of requested interface - null if the requested interface is unavailable, 
	 * i. e. if compiled functions in the ScriptEngine cannot be found matching the ones in the requested interface. 
	 */
	@Override
	public <T> T getInterface(Class<T> clazz) {
		if(invocable) {
			return invoker.get().getInterface(clazz);
		} else {
			return null;
		}
	}

	/**
	 * Returns an implementation of an interface using member functions of a scripting object compiled in the interpreter.
	 * @param thiz The scripting object whose member functions are used to implement the methods of the interface.
	 * @param clazz The Class object of the interface to return. 
	 * @return An instance of requested interface - null if the requested interface is unavailable, 
	 * i. e. if compiled functions in the ScriptEngine cannot be found matching the ones in the requested interface. 
	 */
	@Override
	public <T> T getInterface(Object thiz, Class<T> clazz) {
		if(invocable) {
			return invoker.get().getInterface(thiz, clazz);
		} else {
			return null;
		}
	}

	/**
	 * Used to call top-level procedures and functions defined in scripts.
	 * @param name The name of the top level function to invoke
	 * @param args Arguments to pass to the procedure or function 
	 * @return the value returned by the invoked function
	 * @throws ScriptException
	 * @throws NoSuchMethodException
	 */
	@Override
	public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
		if(invocable) {
			return invoker.get().invokeFunction(name, args);
		} else {
			throw new UnsupportedOperationException("The script [" + source.getName() + "] does not support Invocable", new Throwable());
		}
	}

	/**
	 * Calls a method on a script object compiled during a previous script execution, which is retained in the state of the ScriptEngine. 
	 * @param thiz If the procedure is a member of a class defined in the script and thiz is an instance of that class returned by a previous execution or invocation, the named method is called through that instance.
	 * @param name The name of the method to invoke
	 * @param args Arguments to pass to the procedure
	 * @return The value returned by the procedure. 
	 * @throws ScriptException
	 * @throws NoSuchMethodException
	 */
	@Override
	public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
		if(invocable) {
			return invoker.get().invokeMethod(thiz, name, args);
		} else {
			throw new UnsupportedOperationException("The script [" + source.getName() + "] does not support Invocable", new Throwable());
		}
	}

	/**
	 * Returns an uninitialized Bindings.
	 * @return an uninitialized Bindings
	 */
	@Override
	public Bindings createBindings() {
		return scriptEngine.get().createBindings();
	}

	/**
	 * Executes the script using the Bindings argument as the ENGINE_SCOPE Bindings of the ScriptEngine during the script execution.
	 * @param reader The source of the script
	 * @param n the bindings of attributes
	 * @return The value returned by the script
	 * @throws ScriptException
	 */
	@Override
	public Object eval(Reader reader, Bindings n) throws ScriptException {
		return scriptEngine.get().eval(reader, n);
	}

	/**
	 * Causes the immediate execution of the script whose source is the String passed as the first argument.
	 * @param reader The source of the script
	 * @param context A ScriptContext exposing sets of attributes in different scopes. The meanings of the scopes ScriptContext.GLOBAL_SCOPE, and ScriptContext.ENGINE_SCOPE are defined in the specification. 
	 * @return The value returned from the execution of the script. 
	 * @throws ScriptException
	 */
	@Override
	public Object eval(Reader reader, ScriptContext context) throws ScriptException {
		return scriptEngine.get().eval(reader, context);
	}

	/**
	 * Executes the specified script. The default ScriptContext for the ScriptEngine is used. 
	 * @param reader The source of the script
	 * @return The return value of the script
	 * @throws ScriptException
	 */
	@Override
	public Object eval(Reader reader) throws ScriptException {
		return scriptEngine.get().eval(reader);
	}

	/**
	 * Executes the script using the Bindings argument as the ENGINE_SCOPE Bindings of the ScriptEngine during the script execution. The Reader, Writer and non-ENGINE_SCOPE Bindings of the default ScriptContext are used. The ENGINE_SCOPE Bindings of the ScriptEngine is not changed, and its mappings are unaltered by the script execution. 
	 * @param script The script source
	 * @param n The Bindings of attributes to be used for script execution. 
	 * @return The value returned by the script;
	 * @throws ScriptException
	 */
	@Override
	public Object eval(String script, Bindings n) throws ScriptException {
		return scriptEngine.get().eval(script, n);
	}

	/**
	 * Causes the immediate execution of the script whose source is the String passed as the first argument. T
	 * @param script the script source
	 * @param context A ScriptContext exposing sets of attributes in different scopes. The meanings of the scopes ScriptContext.GLOBAL_SCOPE, and ScriptContext.ENGINE_SCOPE are defined in the specification. 
	 * @return the value returned by the script
	 * @throws ScriptException
	 */
	@Override
	public Object eval(String script, ScriptContext context) throws ScriptException {
		return scriptEngine.get().eval(script, context);
	}

	/**
	 * Executes the specified script. The default ScriptContext for the ScriptEngine is used. 
	 * @param script The source of the script
	 * @return The value returned by the script
	 * @throws ScriptException
	 */
	@Override
	public Object eval(String script) throws ScriptException {
		return scriptEngine.get().eval(script);
	}

	/**
	 * Returns a scope of named values.
	 * @param scope Either ScriptContext.ENGINE_SCOPE or ScriptContext.GLOBAL_SCOPE which specifies the Bindings to return. Implementations of ScriptContext may define additional scopes. If the default ScriptContext of the ScriptEngine defines additional scopes, any of them can be passed to get the corresponding Bindings. 
	 * @return The Bindings with the specified scope. 
	 */
	@Override
	public Bindings getBindings(int scope) {
		return scriptEngine.get().getBindings(scope);
	}

	/**
	 * Returns the default ScriptContext of the ScriptEngine whose Bindings, Reader and Writers are used for script executions when no ScriptContext is specified. 
	 * @return The default ScriptContext of the ScriptEngine.
	 */
	@Override
	public ScriptContext getContext() {
		return scriptEngine.get().getContext();
	}

	/**
	 * Returns a ScriptEngineFactory for the class to which this ScriptEngine belongs. 
	 * @return the ScriptEngineFactory
	 */
	@Override
	public ScriptEngineFactory getFactory() {
		return scriptEngine.get().getFactory();
	}

	/**
	 * Sets a scope of named values to be used by scripts. 
	 * @param bindings The bindings for the specified scope
	 * @param scope The specified scope
	 */
	@Override
	public void setBindings(Bindings bindings, int scope) {
		scriptEngine.get().setBindings(bindings, scope);
		
	}

	/**
	 * Sets the default ScriptContext of the ScriptEngine whose Bindings, Reader and Writers are used for script executions when no ScriptContext is specified. 
	 * @param context A ScriptContext that will replace the default ScriptContext in the ScriptEngine. 
	 */
	@Override
	public void setContext(ScriptContext context) {
		scriptEngine.get().setContext(context);
		
	}

	/**
	 * Returns the local bindings
	 * @return the localBindings
	 */
	public Map<String, Object> getLocalBindings() {
		return localBindings;
	}

	/**
	 * Sets the local bindings
	 * @param localBindings the localBindings to set
	 */
	public void setLocalBindings(Map<String, Object> localBindings) {
		if(localBindings != null) {
			this.localBindings.putAll(localBindings);
		}
	}

	

}
