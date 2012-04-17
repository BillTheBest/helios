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
package org.helios.jmxenabled.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: LoggerControl</p>
 * <p>Description: A JMX enabled logger level editor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmxenabled.logging.LoggerControl</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class LoggerControl {
	/** The target logger to control */
	protected final Logger targetLogger;
	
	/** A map of the logging levels keyed by logger name */
	private static final Map<String, Level> levels = Collections.unmodifiableMap(getValidLevels());
	/** A string rendering all the valid levels */
	public static final String levelDescription = levels.keySet().toString();
	
	/**
	 * Retrieves the valid levels for log4j
	 * @return A map of logging levels keyed by name
	 */
	static Map<String, Level> getValidLevels() {
		final Map<String, Level> levs = new HashMap<String, Level>(8);
		try {
			for(Field f: Level.class.getDeclaredFields()) {
				if(Modifier.isStatic(f.getModifiers()) && Level.class.equals(f.getType())) {
					Level level = (Level)f.get(null);
					levs.put(level.toString(), level);
				}
			}
			return levs;
		} catch (Exception e) {
			Logger.getRootLogger().fatal("Failed to initialize Impl's cache of valid level names", e);
			throw new RuntimeException("Failed to initialize Impl's cache of valid level names", e);
		}
	}
	
	/**
	 * Creates a new LoggerControl
	 * @param targetLogger The target logger to control
	 */
	public LoggerControl(Logger targetLogger) {
		if(targetLogger==null)  throw new IllegalArgumentException("The target logger was null", new Throwable());
		this.targetLogger = targetLogger;
	}
	
	/**
	 * Returns the target logger's name
	 * @return the target logger's name
	 */
	@JMXAttribute(name="LoggerName", description="The target logger's name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getLoggerName() {
		return targetLogger.getName();
	}
	
	/**
	 * Returns the name of the actual level of the logger
	 * @return a logging level name
	 */
	@JMXAttribute(name="LoggerLevel", description="The current level of the logger which may be null. Valid level names are {f:levelDescription}", mutability=AttributeMutabilityOption.READ_WRITE)
	public String getLoggerLevel() {
		Level level = targetLogger.getLevel();
		if(level==null) return null;
		return level.toString();
	}
	
	/**
	 * Returns the name of the effective level of the logger
	 * @return a logging level name
	 */
	@JMXAttribute(name="LoggerEffectiveLevel", description="The effective level of the logger.", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getLoggerEffectiveLevel() {
		return targetLogger.getEffectiveLevel().toString();
	}
	
	/**
	 * Sets the additivity of the logger
	 * @param additive true to make the logger additive, false otherwise
	 */
	public void setLoggerAdditivity(boolean additive) {
		targetLogger.setAdditivity(additive);
	}
	
	/**
	 * Returns the logger additivity
	 * @return true if the logger is additive, false otherwise
	 */
	@JMXAttribute(name="LoggerAdditivity", description="Indicates if this logger is additive", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getLoggerAdditivity() {
		return targetLogger.getAdditivity();
	}
		
	
	/**
	 * Sets the level for this logger
	 * @param levelName The name of the level to set
	 */
	public void setLoggerLevel(String levelName) {
		if(levelName==null) throw new IllegalArgumentException("Passed level name was null", new Throwable());
		levelName = levelName.trim().toUpperCase();
		Level level = levels.get(levelName);
		if(level==null) throw new IllegalArgumentException("Passed level name was invalid [" + levelName + "]", new Throwable());
		targetLogger.setLevel(level);
	}	
	
}
