package org.helios.cache.softref;

import java.lang.ref.ReferenceQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Title: SoftReferenceCache</p>
 * <p>Description:  A map based cache that stores values in a soft reference so they can be garbage collected. The cache is backed
 * by an active reference queue processor that will return and remove keys from the map where the referent has been enqueued.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.cache.softref.SoftReferenceCache</code></p>
 */
public class SoftReferenceCache<K, V> implements  Map<K, V> {
	/** The cache's backing map  */
	protected final Map<K, IdentifiedSoftReference<V>> innerMap;
	/** The refrence queue where soft references will be enqueued */
	protected final ReferenceQueue<V> refQueue;
	/** an array of enqueue event listeners */
	protected final ReferenceEnqueueListener[] listeners;
	/** A counter of evictions */
	protected final AtomicLong evictions = new AtomicLong(0);
	/** The timestamp of the last eviction */
	protected final AtomicReference<Date> lastEviction = new AtomicReference<Date>(null);
	

	/**
	 * Creates a new, empty SoftReferenceCache.
	 * @param refQueue The refrence queue where soft references will be enqueued
	 * @param rmap The service provided map to use the cache backing.
	 * @param listeners The listeners on the enqueue event.
	 */
	SoftReferenceCache(ReferenceQueue<V> refQueue, Map<K, IdentifiedSoftReference<V>> rmap, ReferenceEnqueueListener...listeners) {
		innerMap = rmap;
		this.refQueue = refQueue;
		this.listeners = listeners;
	}
	
	/**
	 * Creates a new SoftReferenceCache with the same mappings as the given map, except that the values of the passed map are stored in SoftReferences. 
	 * @param map the map
	 * @param refQueue The refrence queue where soft references will be enqueued
	 * @param rmap The service provided map to use the cache backing.
	 * @param listeners The listeners on the enqueue event. 
	 */
	SoftReferenceCache(Map<? extends K,? extends V> map, ReferenceQueue<V> refQueue, Map<K, IdentifiedSoftReference<V>> rmap, ReferenceEnqueueListener...listeners) {
		this(refQueue, rmap);		
		for(Map.Entry<? extends K,? extends V> entry : map.entrySet()) {
			K key = entry.getKey();
			innerMap.put(key, new IdentifiedSoftReference<V>(entry.getValue(), refQueue, key, listeners));
		}
	}

	
	/**
	 * Removes all items from the cache.
	 */
	public void clear() {
		innerMap.clear();
	}

	/**
	 * Tests if the specified object is a key in this cache.
	 * @param key possible key 
	 * @return true if and only if the specified object is a key in this cache, as determined by the equals method; false otherwise. 
	 */
	@SuppressWarnings("unchecked")
	public boolean containsKey(Object key) {
		return innerGet((K)key)!=null;
	}

	/**
	 * Returns true if this cache maps one or more keys to the specified value. 
	 * Note: This method requires a full internal traversal of the cache's values and referents, and so is much slower than method containsKey. 
	 * @param value value whose presence in this cache is to be tested 
	 * @return true if this cache maps one or more keys to the specified value 
	 */
	public boolean containsValue(Object value) {
		if(value==null) throw new NullPointerException("The passed value was null");
		for(Map.Entry<K,IdentifiedSoftReference<V>> entry : innerMap.entrySet()) {
			V val = innerGet(entry.getKey());
			if(val!=null && val.equals(value)) return true;			
		}
		return false;
	}

	/**
	 * Returns the value to which the specified key is mapped, or null if this cache contains no mapping for the key.
	 * @param key the key whose cached value is to be returned 
	 * @return the value to which the specified key is cached, or null if this cache contains no entry for the key 
	 */
	@SuppressWarnings("unchecked")
	public V get(Object key) {
		return innerGet((K)key);
	}

	/**
	 * Returns true if this cache contains no key-value mappings.
	 * @return true if this cache contains no key-value mappings
	 */
	public boolean isEmpty() {
		return innerMap.isEmpty();
	}

	/**
	 * Caches the specified value keyed by the specified key. Neither the key nor the value can be null. 
	 * @param key the cache key for the value
	 * @param value the value to cache
	 * @return the previous value cached under the passed key
	 */
	public V put(K key, V value) {
		if(value==null) throw new NullPointerException("The passed value was null");
		if(key==null) throw new NullPointerException("The passed key was null");
		V val = innerGet(key);
		innerMap.put(key , new IdentifiedSoftReference<V>(value, refQueue, key, listeners));
		return val;
	}

	/**
	 * Removes the key (and its corresponding value) from this cache. This method does nothing if the key is not in the cache. 
	 * @param key the key that needs to be removed 
	 * @return the previous value associated with key, or null if there was no cache entry for key
	 */
	@SuppressWarnings("unchecked")
	public V remove(Object key) {
		V value = innerGet((K)key);
		if(value!=null) {
			innerMap.remove(key);
			if(Thread.currentThread().getName().startsWith(SoftReferenceCacheService.CLEANER_THREAD_NAME)) {
				evictions.incrementAndGet();
				lastEviction.set(new Date());
			}
		}
		return value;
	}

	/**
	 * Returns the number of entries in the cache
	 * @return the number of entries in the cache
	 */
	public int size() {
		return innerMap.size();
	}

	/**
	 * Returns a disconnected Collection of the values contained in this cache. Changes to the returned collection
	 * have no effect on the cache. 
	 * @return a disconnected collection of the values contained in this map
	 */
	public Collection<V> values() {
		Collection<IdentifiedSoftReference<V>> refs = innerMap.values();
		Iterator<IdentifiedSoftReference<V>> iter = refs.iterator();
		Set<V> set = new HashSet<V>(size());
		while(iter.hasNext()) {
			IdentifiedSoftReference<V> ref = iter.next();
			if(ref.isEnqueued()) {
				iter.remove();
				continue;
			}
			V value = ref.get();
			if(value==null) {
				iter.remove();
				continue;
			}
			set.add(value);
		}		
		return set;
	}
	
	/**
	 * Custom get that extracts the value from IdentifiedSoftReference and clears the map entry if the IdentifiedSoftReference has been enqueued.
	 * @param key
	 * @return
	 */
	protected V innerGet(K key) {
		IdentifiedSoftReference<V> ref = innerMap.get(key);
		if(ref==null) return null;
		V value = ref.get();
		if(value!=null && !ref.isEnqueued()) return value;
		synchronized(innerMap) {
			ref = innerMap.get(key);
			value = ref.get();
			if(value==null) innerMap.remove(key);			
		}
		return null;
	}
	
	/**
	 * Returns the number of registered eviction listeners.
	 * @return the number of registered eviction listeners.
	 */
	public int getListenerCount() {
		return listeners==null ? 0 : listeners.length;
	}

	/**
	 * Returns the total number of evictions.
	 * @return the total number of evictions.
	 */
	public long getEvictions() {
		return evictions.get();
	}

	/**
	 * Returns the timestamp of the most recent eviction.
	 * @return the timestamp of the most recent eviction or null if one has not occured.
	 */
	public Date getLastEviction() {
		return lastEviction.get();
	}

	/**
	 * Returns a disconnected and unmodifiable entry set of the contents of the cache.
	 * @return A set containing the entries in the cache.
	 * @see java.util.Map#entrySet()
	 */
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Map<K, V> map = new HashMap<K,V>(innerMap.size());
		for(Map.Entry<K,IdentifiedSoftReference<V>> entry : innerMap.entrySet()) {
			V val = innerGet(entry.getKey());
			map.put(entry.getKey(), val);		
		}		
		return Collections.unmodifiableMap(map).entrySet();
	}

	/**
	 * Returns a disconnected and unmodifiable set of the keys in the cache.
	 * @return a set of the cache keys.
	 * @see java.util.Map#keySet()
	 */
	public Set<K> keySet() {
		Set<K> set = new HashSet<K>(innerMap.keySet());
		return Collections.unmodifiableSet(set);
	}

	/**
	 * Adds the contents of the passed map to the cache.
	 * @param map A map of keys and values to be added to the cache.
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends K, ? extends V> map) {
		for(Map.Entry<? extends K,? extends V> entry : map.entrySet()) {
			K key = entry.getKey();
			innerMap.put(key, new IdentifiedSoftReference<V>(entry.getValue(), refQueue, key, listeners));
		}
	}
	
	/**
	 * Registers the specified enqueue listener with each of the soft references associated to the passed keys.
	 * @param listener The listener to register
	 * @param keys The keys to identify the soft references to register listeners with.
	 * @return an array of booleans the same size as the array of keys passed in, 
	 * each entry being true where the listener was successfully registered, and false otherwise.
	 */
	public boolean[] registerListener(ReferenceEnqueueListener listener, K...keys) {
		if(keys==null || keys.length < 1) return new boolean[]{};
		boolean[] successful = new boolean[keys.length];
		int cntr = 0;
		for(K key: keys) {
			IdentifiedSoftReference<V> softRef = innerMap.get(key);
			if(softRef!= null && !softRef.isEnqueued() && softRef.get() != null) {
				softRef.registerListener(listener);
				successful[cntr] = true;				
			} else {
				successful[cntr] = false;
			}
			cntr++;
		}
		return successful;		
	}
	
	/**
	 * Removes the specified enqueue listener from each of the soft references associated to the passed keys.
	 * @param listener The listener to remove
	 * @param keys The keys to identify the soft references to remove listeners from.
	 * @return an array of booleans the same size as the array of keys passed in, 
	 * each entry being true where the listener was successfully removed, and false otherwise.
	 */
	public boolean[] removeListener(ReferenceEnqueueListener listener, K...keys) {
		if(keys==null || keys.length < 1) return new boolean[]{};
		boolean[] successful = new boolean[keys.length];
		int cntr = 0;
		for(K key: keys) {
			IdentifiedSoftReference<V> softRef = innerMap.get(key);
			if(softRef!= null && !softRef.isEnqueued() && softRef.get() != null) {
				softRef.removeListener(listener);
				successful[cntr] = true;				
			} else {
				successful[cntr] = false;
			}
			cntr++;
		}
		return successful;		
	}
	


}
