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
package org.helios.collectors.pooling;

import java.util.Map;

import javax.management.ObjectName;

/**
 * <p>Title: ResourceFactory</p>
 * <p>Description: Defines a factory that creates resources for pooling</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collectors.pooling.ResourceFactory</code></p>
 */

public interface ResourceFactory<T> {

	/**
	 * Sets a map of resource acquisition configuration
	 * @param configuration a map of resource acquisition configuration
	 */
	public void configure(Map<String, Object> configuration);
	
	/**
	 * Creates a new instance of the resource to be pooled
	 * @return a new resource instance
	 */
	public T newResource();
	
	/**
	 * Deallocates the passed resource instance
	 * @param resource the resource instance to close
	 */
	public void close(T resource);
	
	/**
	 * Validates that the passed resource instance is in a usable state
	 * @param resource THe resource instance to test
	 * @return true if the instance is in a usable state
	 */
	public boolean testResource(T resource);
	
	/**
	 * Returns a JMX ObjectName that suitably and uniquely identifies the resource group created by this factory
	 * @return a JMX ObjectName 
	 */
	public ObjectName getObjectName();

}
