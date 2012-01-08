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


/**
 * <p>Title: IRemoteClassLoader</p>
 * <p>Description: Defines the main interface of the HeliosJMX Class Server API </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.classloading.IRemoteClassLoader</code></p>
 */

public interface IRemoteClassLoader {
	
	/**
	 * Returns the named classes bytes or null if it was not found.
	 * @param className The class name of the class to return.
	 * @return a classes bytes or null if it was not found.
	 */
	public byte[] getRemoteClassBytes(String className);

	/**
	 * Returns the named resource as a byte array or null if it was not found.
	 * @param resourceName The resource name of the resource to return.
	 * @return a byte array or null if it was not found.
	 */	
	public byte[] getRemoteResource(String resourceName);
	
}
