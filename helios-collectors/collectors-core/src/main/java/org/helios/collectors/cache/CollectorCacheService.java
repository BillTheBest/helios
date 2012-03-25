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
package org.helios.collectors.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.helios.collectors.ICollector;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;


/**
 * <p>Title: CollectorCacheService</p>
 * <p>Description: A cache service for collectors to cache arbitrary values in caches reserved for each collector namespace, and be notified of changes in the cache state.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collectors.cache.CollectorCacheService</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class CollectorCacheService extends ManagedObjectDynamicMBean {
	/**  */
	private static final long serialVersionUID = 3879344440137844276L;

	/** The cache manager */	
	protected CacheManager cacheManager = null;
	

	/** The collector caches */
	protected final Map<String, Ehcache> collectorCaches = new ConcurrentHashMap<String, Ehcache>();
	
	/** The CollectorCacheService JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.collectors.cache:service=CollectorCacheService");
	
	/** The singleton instance */
	protected static volatile CollectorCacheService instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();

	/**
	 * Creates a new CollectorCacheService and registers its management interface
	 */
	private CollectorCacheService() {
		this.reflectObject(this);
		this.objectName = OBJECT_NAME;
		//JMXHelper.getRuntimeHeliosMBeanServer().registerMBean(this, OBJECT_NAME);
	}
	
	/**
	 * Returns the CollectorCacheService singleton instance
	 * @return the CollectorCacheService singleton instance
	 */
	public static CollectorCacheService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new CollectorCacheService();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Returns the collector cache for the passed collector
	 * @param collector The collector to get the cache for
	 * @return a collector cache
	 */
	public Ehcache getCacheForCollector(ICollector collector) {
		if(collector==null) throw new IllegalArgumentException("The passed collector was null", new Throwable());
		String cacheName = collector.getClass().getSimpleName();
//		/*
//		 * This next paragraph should not be needed but there is a current dependency issue
//		 * where collectors come asking for their cache before the cache service has been initialized.
//		 * We need a way of implicitly making collectors depend on a set of core services like this one
//		 * before they start collecting.
//		 */
//		if(cacheManager==null) {
//			synchronized(this) {
//				if(cacheManager==null) {
//					cacheManager = CacheManager.getInstance();
//				}
//			}
//		}
		Ehcache cache = cacheManager.addCacheIfAbsent(cacheName);
		if(!collectorCaches.containsKey(cacheName)) {
			collectorCaches.put(cacheName, cache);			
		}
		return cache;
	}
	
	
	/**
	 * Returns the number of collector cache instances
	 * @return the number of collector cache instances
	 */
	@JMXAttribute(name="CollectorCacheCount", description="The number of collector cache instances", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getCollectorCacheCount() {
		return collectorCaches.size();
	}
	
	/**
	 * Returns the collector cache names
	 * @return the collector cache names
	 */
	@JMXAttribute(name="CollectorCacheNames", description="The collector cache names", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getCollectorCacheNames() {
		return collectorCaches.keySet().toArray(new String[collectorCaches.size()]);
	}
	
	/**
	 * Returns the collector cache manager
	 * @return the collector cache manager
	 */
	public CacheManager getCacheManager() {
		return cacheManager;
	}

	/**
	 * Sets the collector cache manager
	 * @param cacheManager the collector cache manager
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}
	
}
