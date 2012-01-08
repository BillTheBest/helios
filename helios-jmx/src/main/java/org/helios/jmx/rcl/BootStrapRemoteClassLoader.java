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
 * <p>Title: BootStrapRemoteClassLoader</p>
 * <p>Description: The Phase 2 Remote ClassLoader for ReverseClassLoading</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.rcl.BootStrapRemoteClassLoader</p></code>
 */
public class BootStrapRemoteClassLoader extends ClassLoader implements BootStrapRemoteClassLoaderMBean {
	protected String hostName = null;
	protected int port = -1;
	/**
	 * @param hostName
	 * @param port
	 */
	public BootStrapRemoteClassLoader(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}
	
	public String getHostName() {
		return hostName;
	}
	public int getPort() {
		return port;
	}
	
	
	public Class<?> findClass(String name) throws ClassNotFoundException {
		System.out.println("Loading [" + name + "]");
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
			URL url = new URL("http://" +hostName + ":" + port + "/" + name);
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
