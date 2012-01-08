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
package org.helios.classloading;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Title: OpenURLClassLoader</p>
 * <p>Description: An extension of URLClassLoader to add some additional methods and expose addURL.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.classloading.OpenURLClassLoader</code></p>
 */

public class OpenURLClassLoader extends URLClassLoader {

	/**
	 * Creates a new OpenURLClassLoader
	 * @param urls The URLs from which to load classes and resources 
	 */
	public OpenURLClassLoader(URL...urls) {
		super(clean(urls));
	}

	/**
	 * Creates a new OpenURLClassLoader
	 * @param parent The parent classloader
	 * @param urls The URLs from which to load classes and resources 
	 */
	public OpenURLClassLoader(ClassLoader parent, URL...urls) {
		super(clean(urls), parent);
	}

	/**
	 * Creates a new OpenURLClassLoader
	 * @param parent The parent classloader
	 * @param factory the URLStreamHandlerFactory to use when creating URLs
	 * @param urls The URLs from which to load classes and resources 
	 */
	public OpenURLClassLoader(ClassLoader parent, URLStreamHandlerFactory factory, URL...urls) {
		super(clean(urls), parent, factory);
	}
	
	/**
	 * Adds URLs to this classloader
	 * @param urls An array of URLs
	 */
	public void addURLs(URL...urls) {
		if(urls!=null) {
			for(URL url: urls) {
				if(url!=null) {
					super.addURL(url);
				}
			}
		}
	}
	
	/**
	 * Cleans a URL varg
	 * @param urls The URL varg
	 * @return a cleaned array of URLS with no nulls.
	 */
	public static URL[] clean(URL...urls) {
		Set<URL> set = new HashSet<URL>();
		if(urls!=null) {
			for(URL url: urls) {
				if(url!=null) {
					set.add(url);
				}
			}			
		}
		return set.toArray(new URL[set.size()]);
	}

}
