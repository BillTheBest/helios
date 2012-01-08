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
package org.helios.spring.container.templates.provider;

import java.util.Map;
import java.util.Set;

/**
 * <p>Title: ITemplateProvision</p>
 * <p>Description: Defines a class that provides bind variable token values for unique values in a template generated dynamic deployment.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface ITemplateProvision {
	
	public String getId();
	
	public int getProvisionId();
	
	/**
	 * Adds a key value pair to the provision store.
	 * Values implementing the <code>java.util.Collection</code> interface will be automatically converted to arrays.
	 * @param key The key
	 * @param value The value
	 */
	public void addValue(String key, Object value);
	
	/**
	 * Clears all the values from the provision store.
	 */
	public void clear();
	
	/**
	 * Removes all the matching values for the passed keys.
	 * @param key An array of keys
	 * @return An map containing the key/value of each item removed.
	 */
	public Map<String, Object> remove(String...key);
	
	
	/**
	 * Gets the target value from a provision by name.
	 * @param name The key value
	 * @return The provision value for the provided name. Null if name is not bound.
	 */
	public Object get(String name);
	
	/**
	 * Gets the target value from a provision by name and array/collection index.
	 * @param key The key value
	 * @param index The provision value for the provided name and index. Null if name is not bound, index is invalid or value is not indexed.
	 * @return
	 */
	public Object get(String key, int index);
	
	/**
	 * Gets the number of entries in the store.
	 * @return the number of entries in the store.
	 */
	public int getSize();
	
	/**
	 * Gets the size of an indexed bound value.
	 * @param name The key value.
	 * @return The size of the indexed value or -1 if the name is not bound or the value not indexed.
	 */
	public int getSize(String name);	
	
	/**
	 * Gets an array of all the items in an indexed value.
	 * @param name The key value.
	 * @return An array of objects or null if the name if the name is not bound or the value not indexed.
	 */
	public Object[] getValues(String name);
	
	/**
	 * Gets an array of all the values bound in the provision.
	 * @return An object array which may be zero sized.
	 */
	public Object[] getValues();
	
	/**
	 * Gets an array of all the keys in the provision.
	 * @return A string array which may be zero sized.
	 */
	public String[] getKeys();
	
	/**
	 * Gets an entry-set of the key/value pairs in the provision.
	 * @return A set of <code>Map.Entry</code>s that may be zero sized.
	 */
	public Set<Map.Entry<String, Object>> getEntrySet();
	
	
}
