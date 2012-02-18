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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * <p>Title: ScriptHelper</p>
 * <p>Description: Static utility methods for script information extraction </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.scripting.manager.script.ScriptHelper</code></p>
 */

public class ScriptHelper {
	/** Regex pattern to extract constants from a script's body */
	public static final Pattern SCRIPT_CONST_PATTERN = Pattern.compile("##(.*?)::(.*?)##", Pattern.CASE_INSENSITIVE );
	
	/**
	 * Retrieves script constants from comment lines in the script.
	 * @param scriptCode The script source code
	 * @return A map of constant values keyed by their key
	 */
	public static Map<String, String> getScriptConstants(CharSequence scriptCode) {
		if(scriptCode==null) throw new IllegalArgumentException("Passed Script Code was null", new Throwable());
		Map<String, String> constants = new HashMap<String, String>();
		Matcher matcher = SCRIPT_CONST_PATTERN.matcher(scriptCode);
		while(matcher.find()) {
			try {
				String key = matcher.group(1);
				String value = matcher.group(2);
				constants.put(key, key);
			} catch (Exception e) {}
		}
		return constants;
	}
	
	/**
	 * Retrieves the return value of a named top level function from a script
	 * @param engine The script engine identified for the passed script. If null, an engine will be sniffed.
	 * @param scriptCode The script source code
	 * @param functionName The name of the function to invoke
	 * @param args Optional arguments to the function
	 * @return The return value or null if the method could not be invoked
	 */
	public static Object getTopFunctionResult(ScriptEngine engine, CharSequence scriptCode, String functionName, Object...args) {
		if(scriptCode==null) throw new IllegalArgumentException("Passed Script Code was null", new Throwable());
		if(engine==null) {
			engine = sniffEngine(scriptCode);
			if(engine==null) {
				throw new RuntimeException("Failed to determine engine for passed script", new Throwable());
			}
		}
		if(!(engine instanceof Invocable)) {
			return null;
		}
		try {
			engine.eval(scriptCode.toString());
			Invocable inv = (Invocable)engine;
			return inv.invokeFunction(functionName, args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to evaluate script", e);
		}
	}

	/**
	 * Retrieves the return value of a named top level function from a script
	 * @param scriptCode The script source code
	 * @param functionName The name of the function to invoke
	 * @param args Optional arguments to the function
	 * @return The return value or null if the method could not be invoked
	 */
	public static Object getTopFunctionResult(CharSequence scriptCode, String functionName, Object...args) {
		return getTopFunctionResult(null, scriptCode, functionName, args);
	}
	
	
	/**
	 * Retrieves the value of a named variable from a script
	 * @param engine The script engine identified for the passed script. If null, an engine will be sniffed.
	 * @param scriptCode The script source code
	 * @param variableName The name of the variable to retrieve the value of
	 * @return The variable value or null if it was not found.
	 */
	public static Object getScriptVariable(ScriptEngine engine, CharSequence scriptCode, String variableName) {
		if(scriptCode==null) throw new IllegalArgumentException("Passed Script Code was null", new Throwable());
		if(engine==null) {
			engine = sniffEngine(scriptCode);
			if(engine==null) {
				throw new RuntimeException("Failed to determine engine for passed script", new Throwable());
			}
		}
		try {
			engine.eval(scriptCode.toString());
			Object obj = null;
			try { obj = engine.getContext().getAttribute(variableName); } catch (Exception e) {}
			return obj;
		} catch (ScriptException e) {
			throw new RuntimeException("Failed to evaluate script", e);
		}
	}
	
	/**
	 * Retrieves the value of a named variable from a script
	 * @param scriptCode The script source code
	 * @param variableName The name of the variable to retrieve the value of
	 * @return The variable value or null if it was not found.
	 */
	public static Object getScriptVariable(CharSequence scriptCode, String variableName) {
		return getScriptVariable(null , scriptCode, variableName);
	}

	
	/**
	 * Attempts to determine what sort of script the passed code is by compiling the source against all known script engines using the default classloader
	 * @param scriptCode The code to sniff the engine for
	 * @return The first matchung ScriptEngine or null if one was not found.
	 */
	public static ScriptEngine sniffEngine(CharSequence scriptCode) {
		return sniffEngine(null, scriptCode);
	}
	/**
	 * Attempts to determine what sort of script the passed code is by compiling the source against all known script engines.
	 * @param cl The classloader to use for finding script engines
	 * @param scriptCode The code to sniff the engine for
	 * @return The first matchung ScriptEngine or null if one was not found.
	 */
	public static ScriptEngine sniffEngine(ClassLoader cl, CharSequence scriptCode) {
		if(scriptCode==null) throw new IllegalArgumentException("Passed Script Code was null", new Throwable());
		for(ScriptEngineFactory sef: cl==null ? new ScriptEngineManager().getEngineFactories() : new ScriptEngineManager(cl).getEngineFactories()) {
			try { 
				sef.getScriptEngine().eval(scriptCode.toString());
				return sef.getScriptEngine();
			} catch (Exception e) {}
		}
		return null;
	}
	
	
	public static void main(String[] args) throws ScriptException {
		for(ScriptEngineFactory sef: new ScriptEngineManager().getEngineFactories()) {
			log("Available Engine:" + sef.getEngineName());
		}
		
		log("Null Engine:" + sniffEngine("##!"));
		log("JS Engine:" + sniffEngine("var foo = 'Hello Venus'"));		
		
		log("JS Variable:" + getScriptVariable("var foo = 'Hello Venus'", "foo"));
		String grScript = "public String getGreeting() { return 'Hello Venus';  String foo = 'Hello Var Jupiter'; /*##Planet:Mercury##*/ }";
		log("GR Engine:" + sniffEngine(grScript));
		
		
		log("GR Function:" + getTopFunctionResult(grScript, "getGreeting"));
		ScriptEngine grEngine = sniffEngine(grScript);
		log("GR Function:" + getTopFunctionResult(grEngine, grScript, "getGreeting"));
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
