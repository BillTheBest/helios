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
package org.helios.jmx.rcl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * <p>Title: TestCL</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.rcl.TestCL</p></code>
 */
public class TestCL {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("TestCL");
		try {
			ClassLoader cl = new NLoader(Thread.currentThread().getContextClassLoader());
			//Thread.currentThread().setContextClassLoader(cl);
			String className = "org.helios.jmx.rcl.Foo";
			log("Loading Class");
			//Class<?> clazz = cl.loadClass(className);
			Class<?> clazz = Class.forName("org.helios.jmx.rcl.Foo", true, cl);
			log("Loaded Class:" + clazz.getName());
			
		} catch (Throwable t) {
			log("Error");
			t.printStackTrace(System.err);
		}
	}

	public static void log(Object message) {
		System.out.println(message);
	}
	
	private static class NLoader extends ClassLoader {
		protected ClassLoader parent = null;
		public NLoader(ClassLoader parent) {
			super(parent);
			this.parent = parent;
		}
		public Class<?> findClass(String name) throws ClassNotFoundException {
			log("Loading [" + name + "]");
			try {
				byte[] b = loadRemoteClass(name);
				return this.defineClass(name, b, 0, b.length);
			} catch (Throwable t) {
				throw new ClassNotFoundException("Class could not be loaded [" + name + "]", t);
			}
		}
		
		protected byte[] loadRemoteClass(String name) throws Exception {
			ByteArrayOutputStream baos = null;
			InputStream is = null;
			try {
				baos = new ByteArrayOutputStream();
				URL url = new URL("http://localhost:9090/" + name);
				is = url.openStream();
				byte[] buffer = new byte[2048];
				int bytesRead = 0;
				while((bytesRead=is.read(buffer)) != -1) {
					baos.write(buffer, 0, bytesRead);
				}
				return baos.toByteArray();
			} catch (Exception e) {
				throw new Exception("Failed to load remote class [" + name + "]", e);
			} finally {
				try { is.close(); } catch (Exception e) {}
			}
		}
	}
	
}
