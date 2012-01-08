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
package org.helios.helpers;

/**
 * <p>Title: SystemEnvironmentHelper</p>
 * <p>Description: Class providing static helper methods for handling system and environment properties. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SystemEnvironmentHelper {
	/**
	 * Looks for a value for <code>name</code> in Environment Variables, then System Properties.
	 * Throws a RuntimeExcption if it is not found in either.
	 * @param name The name of the environmental variable or system property.
	 * @return The value of the environmental variable or system property.
	 */
	public static String getEnvThenSystemProperty(String name) {
		String s = System.getenv(name);
		if(s==null) s = System.getProperty(name);
		if(s!=null) return s;
		else throw new RuntimeException("Property [" + name + "] Not Found In Env or System Properties");
	}
	/**
	 * Looks for a value for <code>name</code> in System Properties, then Environmen Variables.
	 * Throws a RuntimeExcption if it is not found in either.
	 * @param name The name of the environmental variable or system property.
	 * @return The value of the environmental variable or system property.
	 */
	public static String getSystemPropertyThenEnv(String name) {
		String s = System.getProperty(name);
		if(s==null) s = System.getenv(name);
		if(s!=null) return s;
		else throw new RuntimeException("Property [" + name + "] Not Found In System Properties or Env");
	}
	
	/**
	 * Looks for a value for <code>name</code> in Environment Variables, then System Properties.
	 * Throws a RuntimeExcption if it is not found in either.
	 * @param name The name of the environmental variable or system property.
	 * @param defaultValue The default value to be returned if the name is not found.
	 * @return The value of the environmental variable or system property.
	 */
	public static String getEnvThenSystemProperty(String name, String defaultValue) {
		String s = System.getenv(name);
		if(s==null) s = System.getProperty(name);
		if(s!=null) return s;
		else return defaultValue;
	}
	
	/**
	 * Looks for a value for <code>name</code> in System Properties, then Environmen Variables.
	 * Throws a RuntimeExcption if it is not found in either.
	 * @param name The name of the environmental variable or system property.
	 * @param defaultValue The default value to be returned if the name is not found.
	 * @return The value of the environmental variable or system property.
	 */
	public static String getSystemPropertyThenEnv(String name, String defaultValue) {
		String s = System.getProperty(name);
		if(s==null) s = System.getenv(name);
		if(s!=null) return s;
		else return defaultValue;		
	}
}
