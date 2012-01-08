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
package org.helios.version;

import java.io.InputStream;
import java.util.jar.Manifest;


public class VersionHelper {
	/** This is what we show if no version can be found */
	public static final String UNKNOWN_VERSION = "Unknown Version";
	/** The manifest key for the helios version */
	public static final String HELIOS_VERSION = "helios-version";
	
	/**
	 * Returns the Helios Java API version from the manifest of the jar located by the passed class.
	 * @param clazz the class to use to locate the version
	 * @return the determined version
	 */
	public static String getHeliosVersion(Class<?> clazz) {
		if(clazz==null) return UNKNOWN_VERSION;
		InputStream jis = null;
		String version = null;
		try {
			jis = clazz.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
			Manifest mf = new Manifest(jis);
			version = mf.getMainAttributes().getValue(HELIOS_VERSION);
			if(version==null || version.length()<1) {
				version = UNKNOWN_VERSION;
			}
		} catch (Exception e) {
			version = UNKNOWN_VERSION;
		} finally {
			try { jis.close(); } catch (Exception e) {}			
		}
		return version;
	}
	
	/**
	 * Returns the Helios Java API version from the manifest of the jar located by the passed object's class.
	 * @param obj the object to get the class to use to locate the version
	 * @return the determined version
	 */
	public static String getHeliosVersion(Object obj) {
		if(obj==null) return UNKNOWN_VERSION;
		return getHeliosVersion(obj.getClass());
	}

}
