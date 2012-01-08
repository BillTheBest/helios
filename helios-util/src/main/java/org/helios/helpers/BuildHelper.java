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
import java.io.*;
import java.util.*;
/**
 * <p>Title: BuildHelper</p>
 * <p>Description: Static build process helper methods.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 222 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-utils/trunk/src/org/helios/helpers/BuildHelper.java $
 * $Id: BuildHelper.java 222 2008-07-08 21:05:12Z nwhitehead $
 */
public class BuildHelper {
	/**
	 * Updates the version in a properties file.
	 * For example, and entry <code>HeliosUtils_version=0.1</code> would become <code>HeliosUtils_version=0.2</code>.
	 * The update pattern is simple and the right most integer is incremented.
	 * @param fileName The properties file name.
	 * @param key The key in the properties file to increment.
	 */
	public static void updateVersion(String fileName, String key) {
		File file = new File(fileName);
		if(!file.canRead() || !file.canWrite()) {
			System.err.println("Cannot Read or Write File:" + fileName);
			throw new RuntimeException("Cannot Read or Write File:" + fileName);
		}
		FileInputStream fis = null;
		FileOutputStream fos = null;
		Properties properties = new Properties();
		BufferedInputStream bis = null;
		try {
			fis = new FileInputStream(file);	
			bis = new BufferedInputStream(fis);
			properties.load(bis);
			System.out.println("Loaded " + properties.size() + " Properties");
			fis.close();
			fos = new FileOutputStream(file);
			if(!properties.containsKey(key)) {
				throw new Exception("Key [" + key + "] Not Found");
			}
			String value = properties.getProperty(key);
			String[] fragments = value.split("\\.");
			String incrVal = fragments[fragments.length-1];
			int iVal = Integer.parseInt(incrVal);
			iVal++;
			StringBuilder buff = new StringBuilder();
			for(int i = 0; i < fragments.length-1; i++) {
				buff.append(fragments[i]).append(".");
			}
			buff.append(iVal);			
			properties.setProperty(key, buff.toString());
			//properties.store(fos, "Updated Key [" + key + "] on " + new Date());
			properties.store(fos, null);
			fos.flush();
			fos.close();
			System.out.println("Incremented " + file.getCanonicalPath() + "/" + key + " to " + buff);
		} catch (Exception e) {
			System.err.println("Failed to process File:" + fileName + ":" + e);
			throw new RuntimeException("Failed to process File:" + fileName, e);			
		} finally {
			try { bis.close(); } catch (Exception e) {}
			try { fis.close(); } catch (Exception e) {}
			try { fos.flush(); } catch (Exception e) {}
			try { fos.close(); } catch (Exception e) {}
		}
	}
	
	public static void main(String[] args) {
		if(args.length < 1) return;
		if(args[0].equalsIgnoreCase("updateVersion")) {
			System.out.println("\tCalling BuildHelper.updateVersion(" + args[1] + "," + args[2] + ")");
			updateVersion(args[1], args[2]);
			
		}
	}
}
