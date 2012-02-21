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
package org.helios.server.ot.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.Notification;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheManagerEventListener;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;


/**
 * <p>Title: CacheEventManager</p>
 * <p>Description: Registers cache event listeners and manages multicasting of events to subscribers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.cache.CacheEventManager</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating that a cache has been added to the cache manager", types=
            @JMXNotificationType(type=CacheEventManager.EVENT_CACHE_ADDED)
        ),
        @JMXNotification(description="Notification indicating that a cache has been removed from the cache manager", types=
            @JMXNotificationType(type=CacheEventManager.EVENT_CACHE_REMOVED)
        ),
        @JMXNotification(description="Notification indicating that a cache entry has been evicted", types=
            @JMXNotificationType(type=CacheEventManager.EVENT_CACHE_ENTRY_EVICTED)
        ),
        @JMXNotification(description="Notification indicating that a cache entry has expired", types=
            @JMXNotificationType(type=CacheEventManager.EVENT_CACHE_ENTRY_EXPIRED)
        ),
        @JMXNotification(description="Notification indicating that a cache entry has been modified", types=
            @JMXNotificationType(type=CacheEventManager.EVENT_CACHE_ENTRY_MODIFIED)
        ),
        @JMXNotification(description="Notification indicating that a new cache entry has been put to a cache", types=
            @JMXNotificationType(type=CacheEventManager.EVENT_CACHE_ENTRY_PUT)
        ),
        @JMXNotification(description="Notification indicating that a cache entry has been removed", types=
            @JMXNotificationType(type=CacheEventManager.EVENT_CACHE_ENTRY_REMOVED)
        )
})
public class CacheEventManager extends ManagedObjectDynamicMBean implements CacheEventListener, CacheManagerEventListener, CacheEventManagerMBean {
	/** The injected cache manager */
	protected final CacheManager cacheManager;
	/** Cache names registered */
	protected final Map<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The listener status */
	protected final AtomicReference<Status> status = new AtomicReference<Status>(Status.STATUS_UNINITIALISED); 
	/** The count of cache add events */
	protected final AtomicLong cacheAddEvents = new AtomicLong(0L); 
	/** The count of cache remove events */
	protected final AtomicLong cacheRemoveEvents = new AtomicLong(0L);
	/** The count of cache purge events */
	protected final AtomicLong cachePurgeEvents = new AtomicLong(0L); 
	/** The count of cache element put events */
	protected final AtomicLong cacheElementPutEvents = new AtomicLong(0L); 
	/** The count of cache element remove events */
	protected final AtomicLong cacheElementRemoveEvents = new AtomicLong(0L);
	/** The count of cache element evict events */
	protected final AtomicLong cacheElementEvictEvents = new AtomicLong(0L); 
	/** The count of cache element expire events */
	protected final AtomicLong cacheElementExpireEvents = new AtomicLong(0L);
	/** The count of cache element modified events */
	protected final AtomicLong cacheElementModifyEvents = new AtomicLong(0L); 
	/** The timestamp of the last counter reset */
	protected final AtomicLong lastCounterResetTime = new AtomicLong(System.currentTimeMillis()); 
	/** A set of ca che names to supress notifications for */
	protected final Set<String> supressedCacheNames = new CopyOnWriteArraySet<String>();
	


	/**
	 * Creates a new CacheEventManager
	 * @param ehCacheManager The injected cache manager
	 */
	public CacheEventManager(CacheManager ehCacheManager) {			
		this.cacheManager = ehCacheManager;
		this.objectName = JMXHelper.objectName(getClass().getPackage().getName() + ":service=" + getClass().getSimpleName());		
		log.info("Initializing [" + objectName + "]");
		this.reflectObject(this);
		cacheManager.getCacheManagerEventListenerRegistry().registerListener(this);
		StringBuilder cList = new StringBuilder("\n\tAdded CacheEventListener to:");
		for(String cacheName: cacheManager.getCacheNames()) {
			if(!caches.containsKey(cacheName)) {
				synchronized(caches) {
					if(!caches.containsKey(cacheName)) {
						Cache cache = cacheManager.getCache(cacheName);
						cache.getCacheEventNotificationService().registerListener(this);
						caches.put(cacheName, cache);
					}
				}
			}			
			cList.append("\n\t\t").append(cacheName);
		}
		status.set(Status.STATUS_ALIVE);
	}
	
	
	/**
	 * Returns an array of the names of caches that have this listener registered.
	 * @return an array of cache names
	 */
	@JMXAttribute(name="CacheNames", description="The names of caches that have this listener registered", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getCacheNames() {
		return caches.keySet().toArray(new String[caches.size()]);
	}

	/**
	 * Returns the cache manager name
	 * @return the cache manager name
	 */
	@JMXAttribute(name="CacheManagerName", description="The cache manager name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getCacheManagerName() {
		return cacheManager.getName();
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.event.CacheEventListener#dispose()
	 */
	@Override
	public void dispose() {
		log.info("Disposing...");		
		cacheManager.getCacheManagerEventListenerRegistry().unregisterListener(this);
		for(Cache cache: caches.values()) {
			cache.getCacheEventNotificationService().unregisterListener(this);
		}
		caches.clear();
		try { JMXHelper.getRuntimeHeliosMBeanServer().unregisterMBean(objectName); } catch (Exception e) {}
		status.set(Status.STATUS_SHUTDOWN);
		log.info("Disposed");
	}
	
	/**
	 * Sends a JMX notification representing a cache event
	 * @param type The notification type
	 * @param message The notification message
	 * @param userData User data items serialized into strings.
	 */
	protected void fireNotification(String type, String message, Object...userData) {
		try {
			Notification notif = new Notification(type, objectName, this.nextNotificationSequence(), System.currentTimeMillis(), message);
			if(userData==null) userData = new Object[]{};
			List<String> userDataItems = new ArrayList<String>();
			for(Object obj: userData) {
				if(obj!=null) {
					userDataItems.add(obj.toString());
				}
			}
			notif.setUserData(userDataItems);
			this.sendNotification(notif);
		} catch (Exception e) {
			log.warn("Failed to send notification", e);
		}
	}


	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.event.CacheEventListener#notifyElementEvicted(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
	 */
	@Override
	public void notifyElementEvicted(Ehcache cache, Element element) {
		cacheElementEvictEvents.incrementAndGet();
		String msg = "Element evicted [" + cache.getName() + "/" + element.getObjectKey() + "]";
		if(!supressedCacheNames.contains(cache.getName())) {
			fireNotification(EVENT_CACHE_ENTRY_EVICTED + "." + cache.getName(), msg, cache.getName(), element.getKey());
		}
		
		if(log.isDebugEnabled()) log.debug(msg);
	}


	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.event.CacheEventListener#notifyElementExpired(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
	 */
	@Override
	public void notifyElementExpired(Ehcache cache, Element element) {
		cacheElementExpireEvents.incrementAndGet();
		String msg = "Element expired [" + cache.getName() + "/" + element.getObjectKey() + "]";
		if(!supressedCacheNames.contains(cache.getName())) {
			fireNotification(EVENT_CACHE_ENTRY_EXPIRED + "." + cache.getName(), msg, cache.getName(), element.getKey());
		}
		if(log.isDebugEnabled()) log.debug(msg);
	}


	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.event.CacheEventListener#notifyElementPut(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
	 */
	@Override
	public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
		cacheElementPutEvents.incrementAndGet();
		String msg = "Element put [" + cache.getName() + "/" + element.getObjectKey() + "]";
		if(!supressedCacheNames.contains(cache.getName())) {
			fireNotification(EVENT_CACHE_ENTRY_PUT + "." + cache.getName(), msg, cache.getName(), element.getKey());
		}
		if(log.isDebugEnabled()) log.debug(msg);
		
	}


	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.event.CacheEventListener#notifyElementRemoved(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
	 */
	@Override
	public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
		cacheElementRemoveEvents.incrementAndGet();
		String msg = "Element removed [" + cache.getName() + "/" + element.getObjectKey() + "]";
		if(!supressedCacheNames.contains(cache.getName())) {
			fireNotification(EVENT_CACHE_ENTRY_REMOVED + "." + cache.getName(), msg, cache.getName(), element.getKey());
		}
		if(log.isDebugEnabled()) log.debug(msg);				
	}


	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.event.CacheEventListener#notifyElementUpdated(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
	 */
	@Override
	public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
		cacheElementModifyEvents.incrementAndGet();
		String msg = "Element updated [" + cache.getName() + "/" + element.getObjectKey() + "]";
		if(!supressedCacheNames.contains(cache.getName())) {
			fireNotification(EVENT_CACHE_ENTRY_MODIFIED + "." + cache.getName(), msg, cache.getName(), element.getKey());
		}
		if(log.isDebugEnabled()) log.debug(msg);		
	}


	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.event.CacheEventListener#notifyRemoveAll(net.sf.ehcache.Ehcache)
	 */
	@Override
	public void notifyRemoveAll(Ehcache cache) {
		cachePurgeEvents.incrementAndGet();
		String msg = "Cache purged [" + cache.getName() + "]";
		fireNotification(EVENT_CACHE_PURGED + "." + cache.getName(), msg, cache.getName());
		if(log.isDebugEnabled()) log.debug(msg);				
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	@Override
	public CacheEventManagerMBean clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("This listener cannot be cloned");
	}


	@Override
	public Status getStatus() {
		return status.get();
	}


	@Override
	public void init() throws CacheException {
		log.info("Initializing CacheManagerEventListener");
		
	}


	@Override
	public void notifyCacheAdded(String cacheName) {
		cacheAddEvents.incrementAndGet();
		if(!caches.containsKey(cacheName)) {
			synchronized(caches) {
				if(!caches.containsKey(cacheName)) {
					Cache cache = cacheManager.getCache(cacheName);
					cache.getCacheEventNotificationService().registerListener(this);
					caches.put(cacheName, cache);
				}
			}
		}
		String msg = "Cache added [" + cacheName + "]";
		if(!supressedCacheNames.contains(cacheName)) {
			fireNotification(EVENT_CACHE_ADDED + "." + cacheName, msg, cacheName);
		}
		log.info(msg);						
	}


	@Override
	public void notifyCacheRemoved(String cacheName) {
		cacheRemoveEvents.incrementAndGet();
		caches.remove(cacheName);
		String msg = "Cache removed [" + cacheName + "]";
		if(!supressedCacheNames.contains(cacheName)) {
			fireNotification(EVENT_CACHE_REMOVED + "." + cacheName, msg, cacheName);
		}				
		if(log.isDebugEnabled()) log.debug(msg);
	}


	/**
	 * Resets the event counters
	 */
	@JMXOperation(name="resetCounters", description="Resets the event counters")
	public void resetCounters() {
		cacheAddEvents.set(0L); 
		cacheRemoveEvents.set(0L);
		cachePurgeEvents.set(0L); 
		cacheElementPutEvents.set(0L); 
		cacheElementRemoveEvents.set(0L);
		cacheElementEvictEvents.set(0L); 
		cacheElementExpireEvents.set(0L);
		cacheElementModifyEvents.set(0L); 
		lastCounterResetTime.set(System.currentTimeMillis()); 		
	}
	
	/**
	 * Returns the number of cache add events since the last reset
	 * @return the cache Add Event count
	 */
	@JMXAttribute(name="CacheAddEvents", description="The number of cache add events since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCacheAddEvents() {
		return cacheAddEvents.get();
	}

	/**
	 * Returns the number of cache remove events since the last reset
	 * @return the cache remove Event count
	 */
	@JMXAttribute(name="CacheRemoveEvents", description="The number of cache remove events since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCacheRemoveEvents() {
		return cacheRemoveEvents.get();
	}
	
	
	/**
	 * Returns the number of cache purge events since the last reset
	 * @return the cache purge Event count
	 */
	@JMXAttribute(name="CachePurgeEvents", description="The number of cache purge events since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCachePurgeEvents() {
		return cachePurgeEvents.get();
	}
	
	/**
	 * Returns the number of cache element put events since the last reset
	 * @return the cache element put Event count
	 */
	@JMXAttribute(name="CacheElementPutEvents", description="The number of cache element put events since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCacheElementPutEvents() {
		return cacheElementPutEvents.get();
	}
	
	/**
	 * Returns the number of cache element remove events since the last reset
	 * @return the cache element remove Event count
	 */
	@JMXAttribute(name="CacheElementRemoveEvents", description="The number of cache element remove events since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCacheElementRemoveEvents() {
		return cacheElementRemoveEvents.get();
	}
	
	/**
	 * Returns the number of cache element eviction events since the last reset
	 * @return the cache element evict Event count
	 */
	@JMXAttribute(name="CacheElementEvictEvents", description="The number of cache element eviction events since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCacheElementEvictEvents() {
		return cacheElementEvictEvents.get();
	}
	
	/**
	 * Returns the number of cache element expire events since the last reset
	 * @return the cache element expire Event count
	 */
	@JMXAttribute(name="CacheElementExpireEvents", description="The number of cache element expire events since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCacheElementExpireEvents() {
		return cacheElementExpireEvents.get();
	}
	
	/**
	 * Returns the number of cache element modify events since the last reset
	 * @return the cache element modify Event count
	 */
	@JMXAttribute(name="CacheElementModifyEvents", description="The number of cache element modify events since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCacheElementModifyEvents() {
		return cacheElementModifyEvents.get();
	}


	/**
	 * Returns the timestamp of the last counter reset 
	 * @return the timestamp of the last counter reset 
	 */
	@JMXAttribute(name="LastCounterResetTime", description="The timestamp of the last counter reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastCounterResetTime() {
		return lastCounterResetTime.get();
	}
	
	/**
	 * Returns the date of the last counter reset 
	 * @return the date of the last counter reset 
	 */
	@JMXAttribute(name="LastCounterResetDate", description="The date of the last counter reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getLastCounterResetDate() {
		return new Date(lastCounterResetTime.get());
	}


	/**
	 * Returns the names of caches for which notifications are supressed.
	 * @return the names of caches for which notifications are supressed.
	 */
	@JMXAttribute(name="SupressedCacheNames", description="The names of caches for which notifications are supressed", mutability=AttributeMutabilityOption.READ_WRITE)
	public Set<String> getSupressedCacheNames() {
		return Collections.unmodifiableSet(supressedCacheNames);
	}
	
	/**
	 * Adds the passed collection of cache names to the supressed cache name list
	 * @param names A collection of cache names to supress notifications for
	 */
	public void setSupressedCacheNames(Set<String> names) {
		if(names!=null) {
			for(String s: names) {
				if(s==null || s.trim().length()<1) continue;
				supressedCacheNames.add(s.trim());
			}
		}		
	}
	
	/**
	 * Removes the passed cache names from the supressed list
	 * @param names The names of the caches to remove from the supressed list
	 */
	@JMXOperation(name="removeSupressedCacheNames", description="Removes the passed cache names from the supressed list")
	public void removeSupressedCacheNames(@JMXParameter(name="names", description="The names of the caches to remove from the supressed list") String...names) {
		if(names!=null) {
			for(String s: names) {
				if(s==null || s.trim().length()<1) continue;
				supressedCacheNames.remove(s.trim());
			}
		}		
	}
	
	

}
