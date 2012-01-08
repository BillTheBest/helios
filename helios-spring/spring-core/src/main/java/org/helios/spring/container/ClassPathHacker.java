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

import java.io.IOException;
import java.io.File;
import java.net.URLClassLoader;
import java.net.URL;
import java.lang.reflect.Method;

/**
 * <p>Title: ClassPathHacker</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ClassPathHacker {
	   private static final Class[] parameters = new Class[]{URL.class};

	   public static void addFile(String s) throws IOException {
	      File f = new File(s);
	      addFile(f);
	   }//end method

	   public static void addFile(File f) throws IOException {
	      addURL(f.toURL());
	   }//end method


	   public static void addURL(URL u) throws IOException {

	      URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
	      Class sysclass = URLClassLoader.class;

	      try {
	         Method method = sysclass.getDeclaredMethod("addURL", parameters);
	         method.setAccessible(true);
	         method.invoke(sysloader, new Object[]{u});
	      } catch (Throwable t) {
	         t.printStackTrace();
	         throw new IOException("Error, could not add URL to system classloader");
	      }//end try catch

	   }//end method
}
