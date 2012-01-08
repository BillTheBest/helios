package org.helios.cache.softref;

import java.lang.management.ManagementFactory;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


import javax.management.MBeanServer;

import org.apache.log4j.Logger;



@SuppressWarnings({ "unchecked", "hiding"})
public class SoftReferenceCacheService<K, V>  implements SoftReferenceCacheServiceMBean, Runnable {
	/** The reference queue to which cached sift references are appended by the garbage collector after the appropriate reachability changes are detected.  */
	protected ReferenceQueue refQueue = new ReferenceQueue();
	/** A map of soft references to objects that will receive a call back when a child item has been garbage collected */
	protected Map<Long, IdentifiedSoftReference> caches;
	/** A serial number generator to assign each new cache a unique ID. */
	protected static final AtomicLong serial = new AtomicLong(0);
	/** The cache service singleton instance */
	protected static volatile SoftReferenceCacheService service = null;
	/** The singleton creation time lock */
	protected static final Object lock = new Object();
	/** Static logger instance  */
	protected static final Logger LOG = Logger.getLogger(SoftReferenceCacheService.class);
	/** The thread name of the reference queue reader thread */
	protected static final String CLEANER_THREAD_NAME = "SoftReferenceCacheServiceCleanerThread";
	/** the reference queue reader thread */
	protected final Thread cleanerThread;
	/** A flag indicating if the cleaner thread has been started */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The number of evictions completed as a result of a dequeued reference */
	protected final AtomicLong evictions= new AtomicLong(0);
	/** The number of failed evictions completed as a result of a dequeued reference */
	protected final AtomicLong failedEvictions= new AtomicLong(0);
	
	/** The number of references that have been dequeued from the reference queue */
	protected final AtomicLong referenceDequeues = new AtomicLong(0);
	
	/** The MBeanServer where the MBean is registered */
	protected MBeanServer server = null;
	
	protected static final String[] NULL_SIG = {};
	protected static final Object[] NULL_PARAM = {};

	/**
	 * Creates the new singleton service and starts the cleaner thread.
	 */
	private SoftReferenceCacheService() {
		caches = new ConcurrentHashMap<Long, IdentifiedSoftReference>();
		IdentifiedSoftReference internalRef = new IdentifiedSoftReference(caches, refQueue, -1L);
		caches.put(-1L, internalRef);
		cleanerThread = new Thread(this, CLEANER_THREAD_NAME);
		cleanerThread.setDaemon(false);
		cleanerThread.setPriority(Thread.NORM_PRIORITY-1);
		server = ManagementFactory.getPlatformMBeanServer();
		if(server.isRegistered(serviceObjectName)) {
			try { 
				server.invoke(serviceObjectName, "stopService", NULL_PARAM, NULL_SIG);
			} catch (Exception e) {}
		}
		try { 
			server.unregisterMBean(serviceObjectName);
		} catch (Exception e) {
			LOG.warn("Old SoftReferenceCacheService MBean was found registered, but failed to unregister:" + e);				
		}
		try { 
			server.registerMBean(this, serviceObjectName);
		} catch (Exception e) {
			LOG.warn("Failed to register SoftReferenceCacheService MBean. Continuing...." + e);				
		}
		try { 
			server.invoke(serviceObjectName, "startService", NULL_PARAM, NULL_SIG);
		} catch (Exception e) {
			LOG.fatal("Failed to start SoftReferenceCacheService MBean. Cannot continue." + e);
			throw new RuntimeException("Failed to start SoftReferenceCacheService MBean. Cannot continue." + e);
		}						
	}
	
	/**
	 * Starts the cleaner thread and marks the service started.
	 * @throws Exception
	 * @see org.jboss.system.ServiceMBeanSupport#startService()
	 */
	public void startService() {
		cleanerThread.start();
		started.set(true);
		LOG.info("\n\t===================================================\n\tStarted SoftReferenceCacheService\n\t===================================================\n");
	}
	
	/**
	 * Stops the cleaner service and marks the service stopped.
	 * @see org.jboss.system.ServiceMBeanSupport#stopService()
	 */
	public  void stopService(){		
		started.set(false);
		cleanerThread.interrupt();
		LOG.info("\n\t===================================================\n\tStopped SoftReferenceCacheService\n\t===================================================\n");
	}
	
	
	/**
	 * Returns the SoftReferenceCacheService singleton 
	 * @return the SoftReferenceCacheService
	 */	
	public static SoftReferenceCacheService getInstance() {		
		if(service==null) {
			synchronized(lock) {
				if(service==null) {
					service = new SoftReferenceCacheService();
					//service.startService();
				}
			}
		}
		return service;
	}
	
	/**
	 * Executes the reference queue polling.
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while(started.get()) {
			try {
				IdentifiedSoftReference ref = (IdentifiedSoftReference)refQueue.remove();
				if(ref!=null) {
					referenceDequeues.incrementAndGet();
					ReferenceEnqueueListener[] listeners = ref.getListeners();
					if(LOG.isDebugEnabled()) LOG.debug("\n\t***********\nCleaning up refs for " + ref + "\n\t***********\n");
					if(listeners!=null && listeners.length>0) {
						for(ReferenceEnqueueListener listener : listeners) {
							if(listener!=null) {
								listener.onEnqueuedReference(ref);
								ref.removeListener(listener);
							}
							
						}
					}					
				}
			} catch (InterruptedException iex) {
				if(!started.get()) {
					LOG.info("Stopping Cleaner Thread");
					break;
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * Stores created caches so they can be reference collected.
	 * @param map The backing map of a newly created cache.
	 * @return the identifier of the backing map.
	 */
	protected long storeCacheBacking(Map map) {
		long ser = serial.incrementAndGet();
		if(ser==-1) ser = serial.incrementAndGet();
		caches.put(ser, new IdentifiedSoftReference(map, refQueue, ser));
		return ser;
	}
	
	/**
	 * Creates a new ReferenceEnqueueListener that will remove the item from the object cache-keyed by the passed id.
	 * Object types that are recognized are:<ul>
	 * <li><code>java.util.Map</code> implementations.</li>
	 * <li><code>java.util.Collection</code> implementations.</li>
	 * <li><code>java.util.concurrent.atomic.AtomicReference</code> instances that contain one of the following:<ul>
	 * 		<li><code>java.util.Map</code></li>
	 * 		<li><code>java.util.Collection</code></li> 
	 * 		<li><code>Array</code></li>
	 * </li>
	 * </ul>
	 * @param target The object to clear the reference from.
	 * @return a new ReferenceEnqueueListener
	 */
	protected ReferenceEnqueueListener newReferenceEnqueueListener(final long target) {
		return new ReferenceEnqueueListener() {
			private long mapId = target;
			public void onEnqueuedReference(IdentifiedSoftReference enqueuedRef) {
				boolean evictionSuccessful = false;
				if(enqueuedRef!=null) {
					Object id = enqueuedRef.getReferentIdentity();
					if(id!=null) {
						IdentifiedSoftReference sr = caches.get(mapId);
						if(sr!=null) {
							Object parent = sr.get();
							if(parent!=null) {
								if(removeFromParent(parent, id)) {
									evictions.incrementAndGet();
									evictionSuccessful=true;
								}
							}
						} else {
							try { 
								caches.remove(id); 
							} catch (Exception e) {}
						}
					}
				}
				if(!evictionSuccessful) failedEvictions.incrementAndGet();
			}			
		};
	}
	
	/**
	 * Reflects the passed parent to match against a collection, map or array
	 * and then removes the child from it.
	 * @param parent The parent to remove the child from
	 * @param child The child to remove
	 * @return true if the child is successfully removed, false if it is not.
	 */
	protected boolean removeFromParent(Object parent, Object child) {
		if(parent==null || child==null) return false;
		if(Map.class.isInstance(parent)) {
			return ((Map)parent).remove(child)!=null;
		} else if(Collection.class.isInstance(parent)) {
			return ((Collection)parent).remove(child);			
		} else if(AtomicReference.class.isInstance(parent)) {
			parent = ((AtomicReference)parent).get();
			if(parent!=null) {
				Class clazz = parent.getClass();
				if(clazz.isArray()) {
					int arrLength = Array.getLength(parent);
					Set<?> set = new HashSet(arrLength);
					if(set.remove(child)) {						
						((AtomicReference)parent).set(set.toArray((K[]) Array.newInstance(clazz, arrLength-1)));
						return true;
					} else {
						return false;
					}
				} else {
					return removeFromParent(parent, child);
				}
			}			
		}
		return false;
	}
	
	/**
	 * Creates a new, empty SoftReferenceCache with a default initial capacity (16), load factor (0.75) and concurrencyLevel (16).
	 */	
	public <K,V> SoftReferenceCache<K, V> createCache() {
		Map<K, IdentifiedSoftReference<V>> backingMap = new ConcurrentHashMap<K, IdentifiedSoftReference<V>>();
		return new SoftReferenceCache<K,V>(refQueue, backingMap, newReferenceEnqueueListener(storeCacheBacking(backingMap)));		
	}
	
	
	/**
	 * Creates a new, empty SoftReferenceCache with the specified initial capacity, and with default load factor (0.75) and concurrencyLevel (16).
	 * @param initialCapacity The implementation performs internal sizing to accommodate this many elements.
	 */
	public <K,V> SoftReferenceCache<K,V> createCache(int initialCapacity) {
		Map<K, IdentifiedSoftReference<V>> backingMap = new ConcurrentHashMap<K, IdentifiedSoftReference<V>>(initialCapacity);
		return new SoftReferenceCache<K,V>(refQueue, backingMap, newReferenceEnqueueListener(storeCacheBacking(backingMap)));		

	}
	
	/**
	 * Creates a new, empty SoftReferenceCache with the specified initial capacity and load factor and with the default concurrencyLevel (16).
	 * @param initialCapacity The implementation performs internal sizing to accommodate this many elements.
	 * @param loadFactor the load factor threshold, used to control resizing. Resizing may be performed when the average number of elements per bin exceeds this threshold.
	 */
	public <K,V> SoftReferenceCache<K,V> createCache(int initialCapacity, float loadFactor) {
		Map<K, IdentifiedSoftReference<V>> backingMap = new ConcurrentHashMap<K, IdentifiedSoftReference<V>>(initialCapacity, loadFactor, 16);
		return new SoftReferenceCache<K,V>(refQueue, backingMap, newReferenceEnqueueListener(storeCacheBacking(backingMap)));		
	}

	/**
	 * Creates a new, empty map with the specified initial capacity, load factor and concurrency level. 
	 * @param initialCapacity The implementation performs internal sizing to accommodate this many elements.
	 * @param loadFactor the load factor threshold, used to control resizing. Resizing may be performed when the average number of elements per bin exceeds this threshold. 
	 * @param concurrencyLevel the estimated number of concurrently updating threads. The implementation performs internal sizing to try to accommodate this many threads.
	 */
	public <K,V> SoftReferenceCache<K,V> createCache(int initialCapacity, float loadFactor, int concurrencyLevel) {		
		Map<K, IdentifiedSoftReference<V>> backingMap = new ConcurrentHashMap<K, IdentifiedSoftReference<V>>(initialCapacity, loadFactor, concurrencyLevel);
		return new SoftReferenceCache<K,V>(refQueue, backingMap, 
				newReferenceEnqueueListener(
						storeCacheBacking(backingMap)));		
	}
	
	/**
	 * Creates a new SoftReferenceCache with the same mappings as the given map, except that the values of the passed map are stored in SoftReferences. 
	 * The SoftReferenceCache is created with a capacity of 1.5 times the number of mappings in the given map or 16 (whichever is greater), and a default load factor (0.75) and concurrencyLevel (16). 
	 * @param map the map
	 */
	public <K,V> SoftReferenceCache<K,V> createCache(Map<? extends K,? extends V> map) {
		Map<K, IdentifiedSoftReference<V>> backingMap = new ConcurrentHashMap<K, IdentifiedSoftReference<V>>();
		return new SoftReferenceCache<K,V>(map, refQueue, backingMap, newReferenceEnqueueListener(storeCacheBacking(backingMap)));		
	}

	/**
	 * Returns the total number of managed caches.
	 * @return the total number of managed caches.
	 * @see com.onexchange.jboss.cache.softref.SoftReferenceCacheServiceMBean#getCacheCount()
	 */
	public int getCacheCount() {
		return caches.size();
	}

	/**
	 * Returns the total number of evicitons.
	 * @return the total number of evicitons.
	 * @see com.onexchange.jboss.cache.softref.SoftReferenceCacheServiceMBean#getEvictionCount()
	 */
	public long getEvictionCount() {
		return evictions.get();
	}
	
	/**
	 * Returns the total number of failed evicitons.
	 * @return the total number of failed evicitons.
	 * @see com.onexchange.jboss.cache.softref.SoftReferenceCacheServiceMBean#getFailedEvictionCount()
	 */	
	public long getFailedEvictionCount() {
		return failedEvictions.get();
	}
	
	/**
	 * Returns the total number of ReferenceQueue dequeues.
	 * @return the total number of ReferenceQueue dequeues.
	 */
	public long getReferenceDequeues() {
		return referenceDequeues.get();
	}
	
	

}
