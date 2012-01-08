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

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.InvalidKeyException;
import javax.management.openmbean.OpenDataException;

/**
 * <p>Title: ReadOnlyCompositeAttributeMap</p>
 * <p>Description: A read only TreeMap implementation that outwardly appears to be a map of values, but internally is a map of ReadOnlyAttributeAccessors keyed by composite type keys.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.opentypes.property.ReadOnlyCompositeAttributeMap</code></p>
 */

public class ReadOnlyCompositeAttributeMap implements NavigableMap<String, Object>, CompositeData, Serializable {

	/**  */
	private static final long serialVersionUID = -6433936966170430095L;

	private final TreeMap<String, ReadOnlyAttributeAccessor<?>> map;
	private final int size;
	private final CompositeType compositeType;
	private final Set<String> keySet;	

	/**
	 * Creates a new ReadOnlyCompositeAttributeMap 
	 * @param m A map if ReadOnlyAttributeAccessors keyed by the accessor key
	 * @param compositeType The composite type exposed by this map;
	 */
	public ReadOnlyCompositeAttributeMap(Map<? extends String, ? extends ReadOnlyAttributeAccessor<?>> m, CompositeType compositeType) {
		if(m==null) throw new IllegalArgumentException("Passed map was null", new Throwable());
		map = new TreeMap<String, ReadOnlyAttributeAccessor<?>>(m);
		size = map.size();
		this.compositeType = compositeType;
		keySet = map.keySet();		
	}
	
	/**
	 * @return
	 * @see java.util.SortedMap#values()
	 */
	public Collection<Object> values() {
		Set<Object> set = new TreeSet<Object>();
		for(ReadOnlyAttributeAccessor<?> r: map.values()) {
			set.add(r.get());
		}
		return Collections.unmodifiableCollection(set);
	}
	
	/**
	 * Returns a snapshot map of the attribute map
	 * @return a snapshot map of the attribute map
	 */
	public TreeMap<String, Object> snapshot() {
		TreeMap<String, Object> m = new TreeMap<String, Object>();
		for(Map.Entry<String, ReadOnlyAttributeAccessor<?>> entry: map.entrySet()) {
			m.put(entry.getKey(), entry.getValue().get());
		}
		return m;
	}
	
	/**
	 * Returns a descending snapshot map of the attribute map
	 * @return a descending  snapshot map of the attribute map
	 */
	public TreeMap<String, Object> descendingSnapshot() {
		TreeMap<String, Object> m = new TreeMap<String, Object>(Collections.reverseOrder(map.comparator()));
		for(Map.Entry<String, ReadOnlyAttributeAccessor<?>> entry: map.descendingMap().entrySet()) {
			m.put(entry.getKey(), entry.getValue().get());
		}
		return m;
	}
	

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#ceilingEntry(java.lang.Object)
	 */
	public Entry<String, Object> ceilingEntry(String key) {
		Entry<String, ReadOnlyAttributeAccessor<?>> entry = map.ceilingEntry(key);
		if(entry==null) return null;
		return new AbstractMap.SimpleImmutableEntry<String, Object>(entry.getKey(), entry.getValue().get());
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#ceilingKey(java.lang.Object)
	 */
	public String ceilingKey(String key) {
		return map.ceilingKey(key);
	}

	/**
	 * 
	 * @see java.util.TreeMap#clear()
	 */
	public void clear() {
		throw new UnsupportedOperationException("This map is read only", new Throwable());
	}

	/**
	 * @return
	 * @see java.util.TreeMap#clone()
	 */
	public Object clone() {
		return map.clone();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#comparator()
	 */
	public Comparator<? super String> comparator() {
		return map.comparator();
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	/**
	 * @param value
	 * @return
	 * @see java.util.TreeMap#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return snapshot().containsValue(value);
	}

	/**
	 * @return
	 * @see java.util.TreeMap#descendingKeySet()
	 */
	public NavigableSet<String> descendingKeySet() {
		return map.descendingKeySet();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#descendingMap()
	 */
	public NavigableMap<String, Object> descendingMap() {
		return descendingSnapshot();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#entrySet()
	 */
	public Set<Entry<String, Object>> entrySet() {
		return snapshot().entrySet();
	}

	/**
	 * @param o
	 * @return
	 * @see java.util.AbstractMap#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return map.equals(o);
	}

	/**
	 * @return
	 * @see java.util.TreeMap#firstEntry()
	 */
	public Entry<String, Object> firstEntry() {
		Entry<String, ReadOnlyAttributeAccessor<?>> en = map.firstEntry();
		return new AbstractMap.SimpleImmutableEntry<String, Object>(en.getKey(), en.getValue().get());
	}

	/**
	 * @return
	 * @see java.util.TreeMap#firstKey()
	 */
	public String firstKey() {
		return map.firstKey();
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#floorEntry(java.lang.Object)
	 */
	public Entry<String, Object> floorEntry(String key) {
		Entry<String, ReadOnlyAttributeAccessor<?>> en = map.floorEntry(key);
		return new AbstractMap.SimpleImmutableEntry<String, Object>(en.getKey(), en.getValue().get());
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#floorKey(java.lang.Object)
	 */
	public String floorKey(String key) {
		return map.floorKey(key);
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#get(java.lang.Object)
	 */
	public Object get(Object key) {
		ReadOnlyAttributeAccessor<?> r = map.get(key);		
		return r==null ? null : r.get();
	}

	/**
	 * @return
	 * @see java.util.AbstractMap#hashCode()
	 */
	public int hashCode() {
		return map.hashCode();
	}

	/**
	 * @param toKey
	 * @param inclusive
	 * @return
	 * @see java.util.TreeMap#headMap(java.lang.Object, boolean)
	 */
	public NavigableMap<String, Object> headMap(String toKey, boolean inclusive) {
		return snapshot().headMap(toKey, inclusive);
	}

	/**
	 * @param toKey
	 * @return
	 * @see java.util.TreeMap#headMap(java.lang.Object)
	 */
	public SortedMap<String, Object> headMap(String toKey) {
		return snapshot().headMap(toKey);
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#higherEntry(java.lang.Object)
	 */
	public Entry<String, Object> higherEntry(String key) {
		Entry<String, ReadOnlyAttributeAccessor<?>> en = map.higherEntry(key);
		return new AbstractMap.SimpleImmutableEntry<String, Object>(en.getKey(), en.getValue().get());
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#higherKey(java.lang.Object)
	 */
	public String higherKey(String key) {
		return map.higherKey(key);
	}

	/**
	 * @return
	 * @see java.util.AbstractMap#isEmpty()
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#keySet()
	 */
	public Set<String> keySet() {
		return map.keySet();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#lastEntry()
	 */
	public Entry<String, Object> lastEntry() {
		Entry<String, ReadOnlyAttributeAccessor<?>> en = map.lastEntry();
		return new AbstractMap.SimpleImmutableEntry<String, Object>(en.getKey(), en.getValue().get());
	}

	/**
	 * @return
	 * @see java.util.TreeMap#lastKey()
	 */
	public String lastKey() {
		return map.lastKey();
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#lowerEntry(java.lang.Object)
	 */
	public Entry<String, Object> lowerEntry(String key) {
		Entry<String, ReadOnlyAttributeAccessor<?>> en = map.lowerEntry(key);
		return new AbstractMap.SimpleImmutableEntry<String, Object>(en.getKey(), en.getValue().get());
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#lowerKey(java.lang.Object)
	 */
	public String lowerKey(String key) {
		return map.lowerKey(key);
	}

	/**
	 * @return
	 * @see java.util.TreeMap#navigableKeySet()
	 */
	public NavigableSet<String> navigableKeySet() {
		return map.navigableKeySet();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#pollFirstEntry()
	 */
	public Entry<String, Object> pollFirstEntry() {
		Entry<String, ReadOnlyAttributeAccessor<?>> en = map.pollFirstEntry();
		return new AbstractMap.SimpleImmutableEntry<String, Object>(en.getKey(), en.getValue().get());
	}

	/**
	 * @return
	 * @see java.util.TreeMap#pollLastEntry()
	 */
	public Entry<String, Object> pollLastEntry() {
		Entry<String, ReadOnlyAttributeAccessor<?>> en = map.pollLastEntry();
		return new AbstractMap.SimpleImmutableEntry<String, Object>(en.getKey(), en.getValue().get());
	}

	/**
	 * @param key
	 * @param value
	 * @return
	 * @see java.util.TreeMap#put(java.lang.Object, java.lang.Object)
	 */
	public ReadOnlyAttributeAccessor<?> put(String key, Object value) {
		throw new UnsupportedOperationException("This map is read only", new Throwable());
	}

	/**
	 * @param map
	 * @see java.util.TreeMap#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends String, ? extends Object> map) {
		throw new UnsupportedOperationException("This map is read only", new Throwable());
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#remove(java.lang.Object)
	 */
	public ReadOnlyAttributeAccessor<?> remove(Object key) {
		return map.remove(key);
	}

	/**
	 * @return
	 * @see java.util.TreeMap#size()
	 */
	public int size() {
		return size;
	}

	/**
	 * @param fromKey
	 * @param fromInclusive
	 * @param toKey
	 * @param toInclusive
	 * @return
	 * @see java.util.TreeMap#subMap(java.lang.Object, boolean, java.lang.Object, boolean)
	 */
	public NavigableMap<String, Object> subMap(String fromKey, boolean fromInclusive, String toKey, boolean toInclusive) {
		return snapshot().subMap(fromKey, fromInclusive, toKey, toInclusive);
	}

	/**
	 * @param fromKey
	 * @param toKey
	 * @return
	 * @see java.util.TreeMap#subMap(java.lang.Object, java.lang.Object)
	 */
	public SortedMap<String, Object> subMap(String fromKey, String toKey) {
		return snapshot().subMap(fromKey, toKey);
	}

	/**
	 * @param fromKey
	 * @param inclusive
	 * @return
	 * @see java.util.TreeMap#tailMap(java.lang.Object, boolean)
	 */
	public NavigableMap<String, Object> tailMap(String fromKey, boolean inclusive) {
		return snapshot().tailMap(fromKey, inclusive);
	}

	/**
	 * @param fromKey
	 * @return
	 * @see java.util.TreeMap#tailMap(java.lang.Object)
	 */
	public SortedMap<String, Object> tailMap(String fromKey) {
		return snapshot().tailMap(fromKey);
	}

//	/**
//	 * @return
//	 * @see java.util.AbstractMap#toString()
//	 */
//	public String toString() {
//		StringBuilder b = new StringBuilder(size*20);
//		b.append(compositeType.getTypeName()).append(" [");		
//		for(Map.Entry<String, Object> entry: snapshot().entrySet()) {
//			if(b.length()>1) b.append(", ");
//			b.append(entry.getKey()).append(":").append(entry.getValue().toString());
//		}
//		b.append("]");
//		return b.toString();
//	}

	/**
	 * @param key
	 * @return
	 */
	@Override
	public boolean containsKey(String key) {		
		return map.containsKey(key);
	}

	/**
	 * @param key
	 * @return
	 */
	@Override
	public Object get(String key) {
		ReadOnlyAttributeAccessor<?> r = map.get(key);
		return r==null ? null : r.get();
	}

	/**
	 * @param keys
	 * @return
	 */
	@Override
	public Object[] getAll(String[] keys) {		
		List<Object> list = new ArrayList<Object>();
		if(keys!=null) {
			for(String key: keys) {
				if(key==null || "".equals(key)) {
					throw new IllegalArgumentException("The supplied keys contained a null or a zero length string", new Throwable());
				}
				if(!keySet.contains(key)) {
					throw new InvalidKeyException("The key [" + key + "] is not a key for this type");
				}
				ReadOnlyAttributeAccessor<?> r = map.get(key);
				if(r!=null) {
					list.add(r.get());
				}
			}
		} else {
			throw new IllegalArgumentException("The supplied keys were null", new Throwable());
		}

		return list.toArray(new Object[list.size()]);
	}

	/**
	 * @return
	 */
	@Override
	public CompositeType getCompositeType() {
		return compositeType;
	}
	
	/**
	 * Converts this map to a CompositeDataSupport instance on serialization
	 * @return a CompositeDataSupport instance
	 * @throws ObjectStreamException
	 */
	protected Object writeReplace() throws ObjectStreamException {
		try {
			CompositeDataSupport cds = new CompositeDataSupport(compositeType, snapshot());
			return cds;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to convert an instance of type [" + compositeType.getTypeName() + "] to a CompositeDataSupport on serialization", e);
		}
	}
	
//	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		out.writeObject(writeReplace());
//	}
	

}
