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
package org.helios.espercl.scripts;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.espercl.ConsoleOps;
import org.helios.espercl.commands.Command;
import org.helios.espercl.commands.CommandParameter;
import org.helios.espercl.options.Options;
import org.helios.scripting.manager.script.ScriptInstance;



/**
 * <p>Title: ScriptManager</p>
 * <p>Description: Loads and manages CL scripts.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.scripts.ScriptManager</code></p>
 */

public class ScriptManager {
	/** Singleton instance */
	private static volatile ScriptManager instance = null;
	/** Singleton instance lock */
	private static final Object lock = new Object();
	/** The currently loaded script instance */
	protected AtomicReference<ScriptInstance> script = new AtomicReference<ScriptInstance>(null);
	
	
	/**
	 * Creates a new ScriptManager 
	 */
	private ScriptManager() {
		
	}
	
	/**
	 * Acquires the ScriptManager singleton instance
	 * @return the ScriptManager singleton instance
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
	 * Loads a script from the user's script directory and prepares it for execution.
	 * @param scriptName The name of the script file relative to the user's script directory 
	 */
	@Command(name="scriptload", aliases={"sl"}, description="Loads the specified script name", params={
			@CommandParameter(optional=false, description="The name of the script file relative to the user's script directory")
	})
	public void load(String scriptName) {
		File scriptFile = new File(Options.getInstance().get(Options.SCRIPTDIR) + File.separator + scriptName);
		if(!scriptFile.canRead()) {
			ConsoleOps.err("Cannot read the requested script file [", scriptFile , "]");
		}
		try {
			ScriptInstance si = new ScriptInstance(scriptFile); 
			script.set(si);
			ConsoleOps.info("Loaded script [", si, "]");
		} catch (Exception e) {
			script.set(null);
			ConsoleOps.err("Failed to load script [", scriptFile, "]:", e.toString());
		}			
	}
	
	/**
	 * Unloads the loaded script.
	 */
	@Command(name="scriptunload", aliases={"su"}, description="Unloads the loaded script")
	public void unload() {
		ScriptInstance si = script.getAndSet(null);
		if(si!=null) {
			ConsoleOps.info("Unloaded script [", si, "]");
		} else {
			ConsoleOps.info("No script loaded");
		}		
	}
	
	/**
	 * Determines if a script is loaded
	 * @return true if a script is loaded, false if it is not 
	 */
	public boolean isScriptLoaded() {
		return script.get()!=null;
	}
	
	/**
	 * Prints info on the loaded script
	 */
	@Command(name="scriptinfo", aliases={"si"}, description="Prints info on the loaded script")
	public void info() {
		ScriptInstance si = script.get();
		if(si!=null) {
			ConsoleOps.info("Script is loaded [", si, "]");
		} else {
			ConsoleOps.info("No script loaded. Script Dir is [", Options.getInstance().get(Options.SCRIPTDIR), "]");
		}				
	}

	/**
	 * @return
	 * @see org.helios.scripting.manager.ScriptInstance#exec()
	 */
	public Object exec() {
		return script.get().exec();
	}

	/**
	 * @param args
	 * @return
	 * @see org.helios.scripting.manager.ScriptInstance#exec(java.lang.Object[])
	 */
	public Object exec(Object... args) {
		return script.get().exec(args);
	}
}
