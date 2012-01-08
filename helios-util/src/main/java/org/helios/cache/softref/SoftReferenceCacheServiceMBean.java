
package org.helios.cache.softref;

import javax.management.ObjectName;

import org.helios.helpers.JMXHelper;


/**
 * <p>Title: SoftReferenceCacheServiceMBean</p>
 * <p>Description: JMX Management Interface for the SoftReferenceCacheService </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.cache.softref.SoftReferenceCacheServiceMBean</code></p>
 */
public interface SoftReferenceCacheServiceMBean  {
	/** The JMX ObjectName under which this service is registered  */
	public static final ObjectName serviceObjectName = JMXHelper.objectName("org.helios.cache:service=SoftReferenceCacheService");
	
	/**
	 * Returns the total number of managed caches.
	 * @return the total number of managed caches.
	 * @see com.onexchange.jboss.cache.softref.SoftReferenceCacheServiceMBean#getCacheCount()
	 */
	public int getCacheCount();
	/**
	 * Returns the total number of evicitons.
	 * @return the total number of evicitons.
	 * @see com.onexchange.jboss.cache.softref.SoftReferenceCacheServiceMBean#getEvictionCount()
	 */
	public long getEvictionCount();
	
	/**
	 * Returns the total number of ReferenceQueue dequeues.
	 * @return the total number of ReferenceQueue dequeues.
	 */
	public long getReferenceDequeues();
	
	/**
	 * Returns the total number of failed evicitons.
	 * @return the total number of failed evicitons.
	 */	
	public long getFailedEvictionCount();	
	
	public void startService();
	
	/**
	 * Stops the cleaner service and marks the service stopped.
	 * @see org.jboss.system.ServiceMBeanSupport#stopService()
	 */
	public  void stopService();		
	
	
//	/**
//	 * Returns the ReferenceQueue event depth
//	 * @return the ReferenceQueue event depth
//	 */
//	public long getReferenceQueueDepth();
	

}
