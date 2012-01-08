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
package org.helios.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * <p>Title: CollectionHelper</p>
 * <p>Description: Generic utility methods for working with collections and maps.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.helpers.CollectionHelper</code></p>
 */

public class CollectionHelper {

	/**
	 * Creates a map from an array of objects.
	 * @param args An even numbered array of objects
	 * @return a map.
	 */
	public static <K,V> Map<K,V> createMap(Object...args) {
		if(args==null) return Collections.emptyMap();
		if(args.length%2!=0) {
			throw new IllegalArgumentException("Odd number of arguments:" + args.length);
		}
		int size = args.length/2;
		Map<K,V> map = new HashMap<K,V>(size);
		for(int i = 0; i < args.length;) {
			K key = (K) args[i];
			i++;
			V value = (V) args[i];
			i++;
			map.put(key, value);
		}
		return map;
	}
	
	
	
	/**
	 * Creates a map of named values from an array of objects.
	 * @param args An even numbered array of objects
	 * @return a map.
	 */
	public static <V> Map<String,V> createNamedValueMap(Object...args) {
		if(args==null) return Collections.emptyMap();
		if(args.length%2!=0) {
			throw new IllegalArgumentException("Odd number of arguments:" + args.length);
		}
		int size = args.length/2;
		Map<String,V> map = new HashMap<String,V>(size);
		for(int i = 0; i < args.length;) {
			String key = args[i].toString();
			i++;
			V value = (V) args[i];
			i++;
			map.put(key, value);
		}
		return map;
	}
	
	/**
	 * Creates a map of keyed values from an array of objects.
	 * @param args An even numbered array of objects
	 * @return a map.
	 */
	public static <K,V> Map<K,V> createKeyedValueMap(Class<? extends K> keyType, Class<? extends V> valueType, Object...args) {
		if(args==null) return Collections.emptyMap();
		if(args.length%2!=0) {
			throw new IllegalArgumentException("Odd number of arguments:" + args.length);
		}
		int size = args.length/2;
		Map<K,V> map = new HashMap<K,V>(size);
		for(int i = 0; i < args.length;) {
			K key = (K) args[i];
			i++;
			V value = (V) args[i];
			i++;
			map.put(key, value);
		}
		return map;
	}
	
	
	/**
	 * Creates a map of numbered values from an array of objects.
	 * @param sorted If true, map will be a sorted map
	 * @param unmodifiable If true, map will be unmodifiable
	 * @param args An array of objects to key by a simple sequence number
	 * @return a map.
	 * TODO: Fix all these params with a map builder
	 */
	public static <V> Map<Integer,V> createIndexedValueMap(boolean sorted, boolean unmodifiable, V...args) {
		if(args==null) return Collections.emptyMap();
		Map<Integer,V> map = sorted ? new TreeMap<Integer,V>() : new HashMap<Integer,V>(args.length);
		for(int i = 0; i < args.length; i++) {
			map.put(i, args[i]);
		}
		return unmodifiable ? (sorted ? Collections.unmodifiableSortedMap((SortedMap<Integer,V>) map) : Collections.unmodifiableMap(map)) : map;
	}
	
	/**
	 * Creates a concurrent map from the passed map
	 * @param sorted If true, map will be a sorted map
	 * @param map A map to create the concurrent map from
	 * @return a map.
	 * TODO: Fix all these params with a map builder
	 */
	public static <K,V> Map<K,V> createConcurrentMap(boolean sorted, Map<K,V> map) {
		Map<K,V> newMap = sorted ? new ConcurrentSkipListMap<K, V>() : new ConcurrentHashMap<K,V>(map.size());
		newMap.putAll(map);
		return newMap;
	}
	
	
	
}
