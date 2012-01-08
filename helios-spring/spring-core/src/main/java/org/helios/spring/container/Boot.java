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
package org.helios.spring.container;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import org.helios.io.file.RecursiveDirectorySearch;
import org.helios.io.file.filters.ConfigurableFileExtensionFilter;
import org.helios.spring.container.jmx.ApplicationContextService;

/**
 * <p>Title: Boot</p>
 * <p>Description: Bootstrap class for Helios Spring Container</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class Boot {
	/** The version for this component */
	public static final String VERSION = "HeliosSpringContainer v0.2a";
	/** The container's classpath loader */
	protected static ClassLoader containerClassLoader = null;
	
	
	public static final String CONF_ARG = "-conf";
	public static final String LIB_ARG = "-lib";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log(VERSION);
		Set<String> confDirs = new HashSet<String>();
		Set<String> libDirs = new HashSet<String>();
		Set<URL> classPath = new HashSet<URL>();		
		if(args.length <1) {
			confDirs.add(".");
		} else {
			if(args.length==1) {
				if("-help".equalsIgnoreCase(args[0])) {
					banner();
					return;
				} else {
					Collections.addAll(confDirs, args);
				}
			} else {
				for(int i = 0; i < args.length; i++) {
					if(CONF_ARG.equalsIgnoreCase(args[i]) && args.length >= (i+2)) {
						confDirs.add(args[i+1]);
						i++;
					} else if(LIB_ARG.equalsIgnoreCase(args[i]) && args.length >= (i+2)) {
						libDirs.add(args[i+1]);
						i++;						
					}
				}
				if(libDirs.size()>0) {
					for(String dir: libDirs) {
						String[] jarFiles = RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(".jar"), dir);
						for(String jar: jarFiles) {
							File f = new File(jar);
							if(f.exists()) {
								try {
									//classPath.add(f.getCanonicalFile().toURI().toURL());
									classPath.add(f.getCanonicalFile().toURL());
									//ClassPathHacker.addFile(f.getCanonicalFile());
								} catch (Exception e) {}
							}
						}
					}					
				}
				containerClassLoader = new URLClassLoader(classPath.toArray(new URL[classPath.size()]), ClassLoader.getSystemClassLoader().getParent());
				log("Added [" + classPath.size() + "] Jar files to the classpath");
				if("TRUE".equalsIgnoreCase(System.getProperty("org.helios.spring.debug"))) {
					StringBuilder b = new StringBuilder("Added following jar files to classpath:");
					for(URL url: classPath) {
						b.append("\n\t").append(url);
					}
					b.append("\n");
					log(b);
				}
			}
			
			//hsc.bootStrap(confDirs.toArray(new String[confDirs.size()]));
		}
		if(containerClassLoader==null) {
			containerClassLoader = Thread.currentThread().getContextClassLoader();
		}
		Thread.currentThread().setContextClassLoader(containerClassLoader);
		
		try {
			Class clazz = containerClassLoader.loadClass("org.helios.spring.container.HeliosContainerMain");
			//Class clazz = Class.forName("org.helios.spring.container.HeliosContainerMain", true, containerClassLoader);
			//Class clazz = Class.forName("org.helios.spring.container.HeliosContainerMain");
			Object container = clazz.newInstance();
			Method bootMethod = clazz.getDeclaredMethod("bootStrap", new String[]{}.getClass());
			log("Invoking Boot...");
			bootMethod.invoke(container, new Object[]{confDirs.toArray(new String[confDirs.size()])});
			Thread.currentThread().join();
		} catch (Throwable e) {
			log("Failed to Boot Helios Spring Container. Stack Trace Follows:");
			e.printStackTrace();
		}

	}
	protected static void log(Object message) {
		System.out.println("[HeliosSpringContainer]" + message);
	}
	
	protected static void banner() {
		StringBuilder b = new StringBuilder(VERSION);
		b.append("\n\tUsage: java org.helios.spring.container.HeliosContainerMain [-help]: Prints this banner.");
		b.append("\n\tUsage: java org.helios.spring.container.HeliosContainerMain \n\t\t[-conf <configuration directory>] \n\t\t[-lib <jar directory>]");
		b.append("\n\t-conf and -lib can be repeated more than once.");
		b.append("\n\t-lib will recursively search the passed directory and add any located jar files to the container's classpath.");
		b.append("\n\n");
		System.out.println(b);
	}	

}
