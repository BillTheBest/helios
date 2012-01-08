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
package org.helios.jmx.threadservices.concurrency;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: JMXManagedLock</p>
 * <p>Description: A JMX MBean that manages named reentrant locks to supply a guaranteed one-instance per JVM centralized lock.
 * The lock will always be registered in the platform agent using the statically defined ObjectName.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(annotated=true, declared=false)
public class JMXManagedLock implements Serializable {
	
	private static final long serialVersionUID = 8046614889402242368L;
	/** The static internal singleton instance of the lock. */
	private static volatile JMXManagedLock jmxManagedLock = null;
	/** The constant name of the lock ObjectName */
	public static final String LOCK_NAME = "org.helios.locking:service=JMXSingletonLock";
	/** The constant ObjectName where the lock is registered in the platform agent. */
	public static final ObjectName LOCK_OBJECT_NAME = JMXHelperExtended.objectName(LOCK_NAME);
	/** The description of the lock service */
	public static final String LOCK_SERVICE_DESCRIPTION = "JMX Managed Named Lock Service";
	/** The internal named reentrant locks that locking operations are delegated to. */
	private Map<String, ReentrantLock> innerLocks = new ConcurrentHashMap<String, ReentrantLock>();
	/** The platform MBeanServer */
	private static MBeanServer platformAgent = ManagementFactory.getPlatformMBeanServer();
	/** boot strap synchronization lock */
	private static Object lock = new Object();
	
	/**
	 * Static singleton accessor to the one instance of the singleton in the VM.
	 * @return
	 */
	public static JMXManagedLock getInstance() {
		try {
			if(jmxManagedLock!=null) {
				return jmxManagedLock;
			} else {
				synchronized(lock) {
					if(jmxManagedLock!=null) {
						return jmxManagedLock;
					} else {
						if(platformAgent.isRegistered(LOCK_OBJECT_NAME)) {
							jmxManagedLock = getRegisteredInstance();
							return jmxManagedLock;
						} else {
							jmxManagedLock = new JMXManagedLock();
							try {
								platformAgent.registerMBean(new ManagedObjectDynamicMBean(LOCK_SERVICE_DESCRIPTION, jmxManagedLock), LOCK_OBJECT_NAME);
								return jmxManagedLock;
							} catch (InstanceAlreadyExistsException e) {
								jmxManagedLock = getRegisteredInstance();
								return jmxManagedLock;
							} 
						}					
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire the VM Singleton JMXManagedLock in unexpected error.", e);
		}
	}
	
	
	/**
	 * Acquires the already registered instance of the JMXManagedLock.
	 * @return The JMXManagedLock singleton.
	 * @throws AttributeNotFoundException
	 * @throws InstanceNotFoundException
	 * @throws MBeanException
	 * @throws ReflectionException
	 */
	private static JMXManagedLock getRegisteredInstance() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
		return (JMXManagedLock)platformAgent.getAttribute(LOCK_OBJECT_NAME, "LockManager");
	}
	
	
	/**
	 * 
	 */
	private JMXManagedLock() {

	}
	
	/**
	 * Returns the named innerLock.
	 * @param lockName The name of the lock.
	 * @return An existing or newly created lock.
	 * TODO: Should we expose this ?
	 * TODO: Since the method is synchronized, do we need a concurrent hash map ?
	 */
	protected synchronized ReentrantLock getLock(String lockName) {
		ReentrantLock innerLock = innerLocks.get(lockName);
		if(innerLock==null) {
			innerLock = new ReentrantLock();
			innerLocks.put(lockName, innerLock);
		}
		return innerLock;
	}
	
	/**
	 * Accessor to the instance of the JMXManagedLock.
	 * @return The VM singleton JMXManagedLock instance.
	 */
	@JMXAttribute(name="LockManager", description="The VM singleton JMXManagedLock instance.", mutability=AttributeMutabilityOption.READ_ONLY)
	public JMXManagedLock getLockManager() {
		return getInstance();
	}

	/**
	 * Acquires the lock. 
	 * If the lock does not exist, it will be created and acquired.
	 * If the lock exists but is not available then the current thread becomes disabled for thread scheduling purposes and lies dormant until the lock has been acquired.
	 * @param lockName The logical name of the requested lock.  
	 * @see java.util.concurrent.locks.ReentrantLock#lock()
	 */
	@JMXOperation(name="lock", description="Acquires the lock on the named lock.")
	public void lock(@JMXParameter(name="lockName", description="The logical name of the requested lock.") String lockName) {		
		getLock(lockName).lock();
	}


	/**
	 * Acquires the named lock unless the current thread is interrupted. 
	 * Acquires the named lock if it is available and returns immediately. 
	 * If the lock is not available then the current thread becomes disabled for 
	 * thread scheduling purposes and lies dormant until one of two things happens:<ul>
	 * <li>The lock is acquired by the current thread; or </li>
	 * <li>Some other thread interrupts the current thread, and interruption of lock acquisition is supported.</li>
	 * </ul>
	 * @param lockName The logical name of the requested lock.  
	 * @throws InterruptedException
	 * @see java.util.concurrent.locks.ReentrantLock#lockInterruptibly()
	 */
	@JMXOperation(name="lockInterruptibly", description="Acquires the lock on the named lock unless the current thread is interrupted. The wait can be interrupted by another thread.")
	public void lockInterruptibly(@JMXParameter(name="lockName", description="The logical name of the requested lock.") String lockName) throws InterruptedException {
		getLock(lockName).lockInterruptibly();
	}


	/**
	 * Returns a new Condition instance that is bound to the named Lock instance. 
	 * Before waiting on the condition the lock must be held by the current thread. 
	 * A call to Condition.await() will atomically release the lock before waiting and re-acquire the lock before the wait returns.
	 * @param lockName The logical name of the requested lock.   
	 * @return A new Condition instance for this Lock instance. 
	 * @see java.util.concurrent.locks.ReentrantLock#newCondition()
	 */
	@JMXOperation(name="newCondition", description="Returns a new Condition instance that is bound to the named Lock instance.")
	public Condition newCondition(@JMXParameter(name="lockName", description="The logical name of the requested lock.") String lockName) {
		return getLock(lockName).newCondition();
	}


	/**
	 * Acquires the lock only if it is free at the time of invocation. 
	 * Acquires the lock if it is available and returns immediately with the value true. If the lock is not available then this method will return immediately with the value false.
	 * @param lockName The logical name of the requested lock.   
	 * @return true if the lock was acquired and false  otherwise.
	 * @see java.util.concurrent.locks.ReentrantLock#tryLock()
	 */
	@JMXOperation(name="tryLock", description="Acquires the lock only if it is free at the time of invocation.")
	public boolean tryLock(@JMXParameter(name="lockName", description="The logical name of the requested lock.") String lockName) {
		return getLock(lockName).tryLock();
	}


	/**
	 * Acquires the named lock if it is free within the given waiting time and the current thread has not been interrupted. 
	 * If the lock is available this method returns immediately with the value true. 
	 * If the lock is not available then the current thread becomes disabled for thread scheduling purposes 
	 * and lies dormant until one of three things happens:<ul>
	 * <li>The lock is acquired by the current thread; or </li>
	 * <li>Some other thread interrupts the current thread, and interruption of lock acquisition is supported; or </li>
	 * <li>The specified waiting time elapses </li>
	 * </ul>
	 * If the lock is acquired then the value true is returned. 
	 * @param lockName The logical name of the requested lock.   
	 * @param timeout the maximum time to wait for the lock
	 * @param unit the time unit of the time argument. 
	 * @return true if the lock was acquired and false  if the waiting time elapsed before the lock was acquired. 
	 * @throws InterruptedException
	 * @see java.util.concurrent.locks.ReentrantLock#tryLock(long, java.util.concurrent.TimeUnit)
	 */
	@JMXOperation(name="tryLock", description="Acquires the lock if it is free within the given waiting time and the current thread has not been interrupted.")
	public boolean tryLock(
			@JMXParameter(name="lockName", description="The logical name of the requested lock.") String lockName,
			@JMXParameter(name="timeout", description="the maximum time to wait for the lock") long timeout, 
			@JMXParameter(name="unit", description="the time unit of the time argument.") TimeUnit unit)
			throws InterruptedException {
		return getLock(lockName).tryLock(timeout, unit);
	}


	/**
	 * Releases the named lock. 
	 * @param lockName The logical name of the requested lock.   
	 * @see java.util.concurrent.locks.ReentrantLock#unlock()
	 */
	@JMXOperation(name="unlock", description="Releases the lock.")
	public void unlock(@JMXParameter(name="lockName", description="The logical name of the requested lock.") String lockName) {
		getLock(lockName).unlock();
	}
	
	/**
	 * Returns an estimate of the number of threads waiting to acquire this named lock. 
	 * The value is only an estimate because the number of threads may change dynamically while this method traverses internal data structures. 
	 * This method is designed for use in monitoring of the system state, not for synchronization control.
	 * @param lockName The logical name of the requested lock.   
	 * @return the estimated number of threads waiting for this lock
	 */
	@JMXOperation(name="getQueueLength", description="An estimate of the number of threads waiting to acquire this lock.")
	public int getQueueLength(@JMXParameter(name="lockName", description="The logical name of the requested lock.") String lockName) {
		return getLock(lockName).getQueueLength();
	}
	
	/**
	 * Returns an estimate of the number of threads waiting to acquire all named locks. 
	 * The value is only an estimate because the number of threads may change dynamically while this method traverses internal data structures. 
	 * This method is designed for use in monitoring of the system state, not for synchronization control.  
	 * @return the estimated number of threads waiting for all named locks.
	 */
	@JMXAttribute(name="TotalQueueLength", description="An estimate of the number of threads waiting to acquire named locks.", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized int getTotalQueueLength() {
		int total = 0;
		for(ReentrantLock lock: innerLocks.values()) {
			total += lock.getQueueLength();
		}
		return total;
	}
	
	/**
	 * Returns the total number of named locks.
	 * @return The number of named locks.
	 */
	@JMXAttribute(name="LockCount", description="The number of named locks.", mutability=AttributeMutabilityOption.READ_ONLY)
	public synchronized int getLockCount() {
		return innerLocks.size();
	}
	
	/**
	 * Queries if the named lock is held by any thread. 
	 * @param lockName The logical name of the requested lock.   
	 * @return true if any thread holds the named lock and false otherwise.
	 */
	@JMXOperation(name="isLocked", description="Queries if the named lock is held by any thread.")
	public boolean isLocked(@JMXParameter(name="lockName", description="The logical name of the requested lock.") String lockName) {
		return getLock(lockName).isLocked();
	}
	
	
	

}
