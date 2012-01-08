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
package org.helios.jmx.classloader.server;

import static org.helios.helpers.ClassHelper.nvl;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Title: ClassServerLoader</p>
 * <p>Description: A simple extension of <code>URLClassLoader</code> to support dynamic additions of new URL classpath elements.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.classloader.server.ClassServerLoader</code></p>
 */

public class ClassServerLoader extends URLClassLoader {

	/**
	 * Creates a new ClassServerLoader
	 * @param a set of URLs to add
	 */
	public ClassServerLoader(Set<URL> urls) {
		super(nvl(urls, new HashSet<URL>(0)).toArray(new URL[0]), ClassLoader.getSystemClassLoader());
	}
	
	/**
	 * Creates a new ClassServerLoader
	 * @param a set of URLs to add
	 * @param the classloader to use this classloader's parent.
	 */
	public ClassServerLoader(Set<URL> urls, ClassLoader parent) {
		super(nvl(urls, new HashSet<URL>(0)).toArray(new URL[0]), parent);
	}
	

	/**
	 * Adds new URLs to the class-loader's path.
	 * @param urls An array of URLs to add.
	 */
	public synchronized void addURLs(URL...urls) {
		if(urls!=null && urls.length>0) {
			Set<URL> added = new HashSet<URL>(Arrays.asList(getURLs()));
			for(URL url: urls) {
				if(url!=null) {
					if(!added.contains(url)) {
						addURL(url);
						added.add(url);
					}
				}
			}
		}
	}
	
	/**
	 * Renders the classloader as an informative string.
	 * @return a string describing this classloader
	 */
	public String toString() {
		StringBuilder b = new StringBuilder("ClassServerLoader@");
		b.append(System.identityHashCode(this));
		b.append("\nParent:").append(this.getParent().toString());
		for(URL url: getURLs()) {
			b.append("\n\t").append(url);
		}
		b.append("\n");
		return b.toString();
	}

}
