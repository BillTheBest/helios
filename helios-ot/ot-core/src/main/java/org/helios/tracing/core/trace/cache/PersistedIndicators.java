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
package org.helios.tracing.core.trace.cache;



import gnu.trove.set.hash.TLongHashSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.helpers.ClassHelper;

/**
 * <p>Title: PersistedIndicators</p>
 * <p>Description: A set of hashes that keeps the long pks of all cached objects in state, if the corresponding object is known to already be in the database.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.cache.PersistedIndicators</code></p>
 */

public class PersistedIndicators {
	/** A map of tracemodel pk hash sets keyed by the class name */
	protected final Map<String, TLongHashSet> hashSets = new ConcurrentHashMap<String, TLongHashSet>();
	
	/** The default hash size */
	public static int DEFAULT_HASH_SIZE = 100;

	/**
	 * Creates a new hash
	 * @param name The name of the hash
	 */
	public void addHash(String name) {
		addHash(name, DEFAULT_HASH_SIZE);
	}
	
	/**
	 * Resets all the hashes
	 */
	public void clear() {
		for(TLongHashSet th: hashSets.values()) {
			th.clear();
		}
	}
	
	
	/**
	 * Creates a new hash
	 * @param name The name of the hash
	 * @param size The initial capacity
	 */
	public void addHash(String name, int size) {
		TLongHashSet hashSet = hashSets.get(ClassHelper.nvl(name, "The passed name was null"));
		if(hashSet==null) {
			synchronized(hashSets) {
				hashSet= hashSets.get(name);
				if(hashSet==null) {
					hashSets.put(name, new TLongHashSet(size));
				}
			}
		}
	}
	
	/**
	 * Determines if the passed hash name exists in the hashSet
	 * @param name The hash name
	 * @return true if the named hash exists, false if it does not
	 */
	public boolean exists(String name) {
		return hashSets.containsKey(ClassHelper.nvl(name, "The passed name was null"));
	}
	
	/**
	 * Determines if the named hash contains the passed value
	 * @param name The name of the hash
	 * @param value The value to check for 
	 * @return true if the named hash contains the passed value
	 */
	public boolean contains(String name, long value) {
		if(hashSets.containsKey(ClassHelper.nvl(name, "The passed name was null"))) {
			return hashSets.get(name).contains(value);
		} else {
			addHash(name);
			return false;
		}
	}
	
	/**
	 * Adds a value to the named hash. If the named hash does not exist, it will be created.
	 * @param name The hash name
	 * @param value The value to add
	 */
	public void addValue(String name, long value) {
		if(!exists(name)) {
			addHash(name);
		}
		hashSets.get(name).add(value);
	}
}
