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
package org.helios.jmx.opentypes.property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * <p>Title: PropertyTable</p>
 * <p>Description: A map implementation that dispatches gets and sets to @OpenTypeAttribute annotated fields and methods.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.opentypes.property.PropertyTable</code></p>
 * @param <T>
 */

public class PropertyTable<K, V> implements Map<String,V>, SortedMap<String,V> {
	/** The unsorted property map */
	protected final Map<String,AttributeAccessor<V>> map;
	/** The sorted property map */
	protected final SortedMap<String,AttributeAccessor<V>> sortedMap;
	/** Indicates if the map is sorted or unsorted */
	protected final boolean sorted;
	
	/** The default size of unsorted maps */
	public static final int DEFAULT_SIZE = 64;
	
	public static void main(String[] args) {
		//Map<String, AttributeAccessor<Object>> pt = new PropertyTable<String, AttributeAccessor<Object>>(10, true);
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Creates a new PropertyTable
	 * @param size The initial size of the map
	 * @param sorted true if the map is sorted
	 */
	public PropertyTable(int size, boolean sorted) {
		super();
		this.sorted = sorted;
		if(sorted) {
			sortedMap = new ConcurrentSkipListMap<String,AttributeAccessor<V>>();
			map = null;
		} else {
			sortedMap = null;
			map = new ConcurrentHashMap<String,AttributeAccessor<V>>(size);
		}
	}

	/**
	 * 
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		if(sorted) sortedMap.clear();
		else map.clear();
	}

	/**
	 * @return
	 * @see java.util.SortedMap#comparator()
	 */
	public Comparator<? super String> comparator() {
		if(sorted) return sortedMap.comparator();
		else throw new RuntimeException("This property table is not sorted");
		
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		if(sorted) return sortedMap.containsKey(key);
		else return map.containsKey(key);
	}

	/**
	 * @param value
	 * @return
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		if(sorted) return sortedMap.containsValue(value);
		else return map.containsValue(value);
	}

	/**
	 * @return
	 * @see java.util.SortedMap#entrySet()
	 */
	public Set<Map.Entry<String, V>> entrySet() {
		return getEntrySet(sorted ? sortedMap : map);
	}
	


	/**
	 * Creates an entry set for this property table
	 * @param map The inner map
	 * @return an entry set
	 */
	protected Set<Map.Entry<String, V>> getEntrySet(Map<String, AttributeAccessor<V>> map) {
		Set<Map.Entry<String, V>> set = new HashSet<Map.Entry<String, V>>(map.size());
		for(Map.Entry<String, AttributeAccessor<V>> entry: map.entrySet()) {
			set.add(new PropertyTableEntry(entry.getKey(), entry.getValue()));
		}
		return set;
	}
	
	

	/**
	 * @param o
	 * @return
	 * @see java.util.Map#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return sorted ? sortedMap.equals(o) : map.equals(o); 
	}

	/**
	 * @return
	 * @see java.util.SortedMap#firstKey()
	 */
	public String firstKey() {
		if(sorted) return sortedMap.firstKey();
		else throw new RuntimeException("This property table is not sorted");
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public V get(Object key) {
		if(sorted) return sortedMap.get(key).get();
		else return map.get(key).get();
	}

	/**
	 * @return
	 * @see java.util.Map#hashCode()
	 */
	public int hashCode() {
		if(sorted) return sortedMap.hashCode();
		else return map.hashCode();
	}

	/**
	 * @param toKey
	 * @return
	 * @see java.util.SortedMap#headMap(java.lang.Object)
	 */
	public SortedMap<String, V> headMap(String toKey) {
		if(!sorted) throw new RuntimeException("This property table is not sorted");
		SortedMap<String, AttributeAccessor<V>> innerHeadMap = sortedMap.headMap(toKey);
		SortedMap<String, V> headMap = new ConcurrentSkipListMap<String, V>();
		for(Map.Entry<String, AttributeAccessor<V>> entry: innerHeadMap.entrySet()) {
			headMap.put(entry.getKey(), entry.getValue().get());
		}
		return headMap;
	}

	/**
	 * @return
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		if(sorted) return sortedMap.isEmpty();
		else return map.isEmpty();
	}

	/**
	 * @return
	 * @see java.util.SortedMap#keySet()
	 */
	public Set<String> keySet() {
		if(sorted) return sortedMap.keySet();
		else return map.keySet();
	}

	/**
	 * @return
	 * @see java.util.SortedMap#lastKey()
	 */
	public String lastKey() {
		if(!sorted) throw new RuntimeException("This property table is not sorted");
		return sortedMap.lastKey();
	}

	/**
	 * @param key
	 * @param value
	 * @return
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public AttributeAccessor<V> put(String key, AttributeAccessor<V> value) {
		if(sorted) return sortedMap.put(key, value);
		else return map.put(key, value);
	}
	
	/**
	 * @param key
	 * @param value
	 * @return
	 */
	public V put(String key, V value) {
		AttributeAccessor<V>  accessor = sorted ? sortedMap.get(key) : map.get(key);
		if(accessor==null) throw new RuntimeException("No attribute accessor for key [" + key + "]");
		accessor.set(value);
		return value;
	}

	
	
	/**
	 * @param map
	 */
	public void putAll(Map<? extends String, ? extends V> map) {
		for(Map.Entry<? extends String, ? extends V> entry: map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public V remove(Object key) {
		AttributeAccessor<V>  accessor = sorted ? sortedMap.remove(key) : map.remove(key);
		if(accessor!=null) {
			return accessor.get();
		} else {
			return null;
		}
	}

	/**
	 * @return
	 * @see java.util.Map#size()
	 */
	public int size() {
		if(sorted) return sortedMap.size();
		else return map.size();
	}

	/**
	 * @param fromKey
	 * @param toKey
	 * @return
	 * @see java.util.SortedMap#subMap(java.lang.Object, java.lang.Object)
	 */
	public SortedMap<String, V> subMap(String fromKey, String toKey) {
		if(!sorted) throw new RuntimeException("This property table is not sorted");
		SortedMap<String, AttributeAccessor<V>> innerSubMap = sortedMap.subMap(fromKey, toKey);
		SortedMap<String, V> map = new ConcurrentSkipListMap<String, V>();
		for(Map.Entry<String, AttributeAccessor<V>> entry: innerSubMap.entrySet()) {
			map.put(entry.getKey(), entry.getValue().get());
		}		
		return map;
	}

	/**
	 * @param fromKey
	 * @return
	 * @see java.util.SortedMap#tailMap(java.lang.Object)
	 */
	public SortedMap<String, V> tailMap(String fromKey) {
		if(!sorted) throw new RuntimeException("This property table is not sorted");
		SortedMap<String, AttributeAccessor<V>> innerTailMap = sortedMap.tailMap(fromKey);
		SortedMap<String, V> map = new ConcurrentSkipListMap<String, V>();
		for(Map.Entry<String, AttributeAccessor<V>> entry: innerTailMap.entrySet()) {
			map.put(entry.getKey(), entry.getValue().get());
		}		
		return map;
	}

	/**
	 * @return
	 * @see java.util.SortedMap#values()
	 */
	public Collection<V> values() {
		List<V> collection = new ArrayList<V>(size());
		for(AttributeAccessor<V> accessor: sorted ? sortedMap.values() : map.values()) {
			collection.add(accessor.get());
		}
		return collection;
	}

	
	
}

class PropertyTableEntry<V> implements Map.Entry<String, V> {
	/** The entry key */
	protected final String key;
	/** the accessor for the entry value */
	protected final AttributeAccessor<V> accessor;
	
	
	
	/**
	 * Creates a new PropertyTableEntry
	 * @param key The entry key
	 * @param accessor the accessor for the entry value
	 */
	public PropertyTableEntry(String key, AttributeAccessor<V> accessor) {
		this.key = key;
		this.accessor = accessor;
	}




	public String getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return accessor.get();
	}

	@Override
	public V setValue(V value) {
		accessor.set(value);
		return value;
	}
	
}
