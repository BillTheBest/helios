package org.helios.server.ot.cache;


/**
 * <p>Title: CacheEventManagerMBean</p>
 * <p>Description: Constancts interface for the CacheEventManager</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.cache.CacheEventManagerMBean</code></p>
 */
public interface CacheEventManagerMBean {
	
	/** The event cache type name prefix */
	public static final String EVENT_CACHE = "org.helios.cache.";
	/** The event cache entry type name prefix */
	public static final String EVENT_CACHE_ENTRY = "org.helios.cache.entry";
	/** The notification type indicating that a cache has been added to the cache manager */
	public static final String EVENT_CACHE_ADDED = EVENT_CACHE + "added";
	/** The notification type indicating that a cache has been purged */
	public static final String EVENT_CACHE_PURGED = EVENT_CACHE + "purged";
	/** The notification type indicating that a cache has been removed from the cache manager */
	public static final String EVENT_CACHE_REMOVED = EVENT_CACHE + "removed";
	/** The notification type indicating that a new cache entry has been put to a cache */
	public static final String EVENT_CACHE_ENTRY_PUT = EVENT_CACHE_ENTRY + "put";
	/** The notification type indicating that a new cache entry has been removed */
	public static final String EVENT_CACHE_ENTRY_REMOVED = EVENT_CACHE_ENTRY + "removed";
	/** The notification type indicating that a new cache entry has been evicted */
	public static final String EVENT_CACHE_ENTRY_EVICTED = EVENT_CACHE_ENTRY + "evicted";
	/** The notification type indicating that a new cache entry has expired */
	public static final String EVENT_CACHE_ENTRY_EXPIRED = EVENT_CACHE_ENTRY + "expired";
	/** The notification type indicating that a new cache entry has been modified */
	public static final String EVENT_CACHE_ENTRY_MODIFIED = EVENT_CACHE_ENTRY + "modified";

}