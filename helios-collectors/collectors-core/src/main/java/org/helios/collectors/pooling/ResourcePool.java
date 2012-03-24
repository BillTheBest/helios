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
package org.helios.collectors.pooling;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.helios.helpers.Banner;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;

/**
 * <p>Title: ResourcePool</p>
 * <p>Description: A simple pooling service for stateful, expensive to create objects</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collectors.pooling.ResourcePool</code></p>
 */

public class ResourcePool<T> extends ManagedObjectDynamicMBean {
	/**  */
	private static final long serialVersionUID = 6638237182222928115L;
	/** The maximum size of the pool. Defaults to 10 */
	protected int maxResources = 10;
	/** The startup size of the pool. Defaults to 1 */
	protected int startingResources = 1;
	/** The default timeout in ms. when waiting for an available resource. Defaults to 500 ms. */
	protected long timeout = 500L;
	/** A map of pooled resources keyed by an identity serial number */
	protected final Map<Integer, T> resourceMap = new ConcurrentHashMap<Integer, T>();
	/** A map of pooled resource keys keyed by the system identity hash code of the resource */
	protected final Map<Integer, Integer> resourceKeys = new ConcurrentHashMap<Integer, Integer>();
	
	/** A set of serial numbers available */
	protected PriorityBlockingQueue<Integer> available;
	/** A set of serial numbers in use */
	protected final Set<Integer> inuse = new CopyOnWriteArraySet<Integer>();
	/** The resource factory */
	protected ResourceFactory<T> resourceFactory = null;
	/** Flag indicating pool is running */
	protected final AtomicBoolean running = new AtomicBoolean(false);
	/** Set of threads waiting for resources */
	protected final Set<Thread> waitingThreads = new CopyOnWriteArraySet<Thread>();
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	
	/**
	 * Starts the pool
	 * @throws Exception thrown on any exception configuring or starting the pool
	 */
	public void start() throws Exception {
		if(resourceFactory==null) throw new IllegalStateException("The resource factory has not been set", new Throwable());
		this.objectName = resourceFactory.getObjectName();
		Banner.banner("*", 2, 10, "Starting Resource pool [" + objectName + "]");
		quickValidate();
		available = new PriorityBlockingQueue<Integer>(maxResources, Collections.reverseOrder(new TreeSet<Integer>().comparator()));
		resourceMap.clear();
		resourceKeys.clear();
		available.clear();
		inuse.clear();		
		for(int i = 0; i < maxResources; i++) {
			getResource(i);
		}
		for(int i = 0; i < startingResources; i++) {
			resourceMap.put(i, resourceFactory.newResource());
		}		
		this.reflectObject(this);
		JMXHelper.getRuntimeHeliosMBeanServer().registerMBean(this, objectName);
		running.set(true);
		Banner.banner("*", 2, 10, 
				"Started Resource pool [" + objectName + "]",
				"Max Pool Size:" + maxResources,
				"Starting Pool Size:" + startingResources,
				"Default Timeout (ms):" + timeout
		);
	}
	
	/**
	 * Stops the pool and deallocates the pooled resources
	 */
	public void destroy() {		
		Banner.banner("*", 2, 10, "Stopping Resource pool [" + objectName + "]");
		running.set(false);
		for(T t: resourceMap.values()) {
			try { resourceFactory.close(t); } catch (Exception e) {}
		}
		resourceMap.clear();
		resourceKeys.clear();
		available.clear();
		inuse.clear();
		for(Thread t: waitingThreads) {
			log.warn("Interrupting thread waiting on closed pool [" + t.toString() + "]");
			t.interrupt();
		}
		Banner.banner("*", 2, 10, "Stopped Resource pool [" + objectName + "]");
	}
	
	/**
	 * Brief validation of configuration parameters
	 */
	protected void quickValidate() {
		if(maxResources<1) throw new RuntimeException("The max resources cannot be less than 1", new Throwable());
		if(startingResources<0) throw new RuntimeException("The starting resources cannot be less than 0", new Throwable());
		if(timeout<1) throw new RuntimeException("The timeout cannot be less than 1", new Throwable());
	}
	
	
	/**
	 * Acquires a resource from the pool
	 * @param timeout The length of time to wait in ms.
	 * @return A pooled resource instance
	 */
	public T getResource(long timeout) {
		waitingThreads.add(Thread.currentThread());		
		try {
			T t = null;
			Integer key = available.poll(timeout, TimeUnit.MILLISECONDS);
			if(key==null) {
				throw new RuntimeException("Thread timed out while waiting for a resource [" + timeout + "]", new Throwable());
			}
			inuse.add(key);
			t = resourceMap.get(key);			
			if(t==null) {
				t = resourceFactory.newResource();
				resourceMap.put(key, t);
			}
			return t;
		} catch (InterruptedException ie) {
			if(Thread.interrupted()) Thread.interrupted();
			throw new RuntimeException("Thread was interrupted while waiting for a resource", ie);
		} finally {
			waitingThreads.remove(Thread.currentThread());
		}
	}
	
	/**
	 * Acquires a resource from the pool waiting the defailt timeout period
	 * @return A pooled resource instance
	 */
	public T getResource() {
		return getResource(this.timeout);
	}
	
	/**
	 * Returns a resource to the pool
	 * @param resource The resource to return
	 */
	public void returnResource(T resource) {
		if(resource==null) throw new IllegalArgumentException("The passed resource was null", new Throwable());
		int hash = System.identityHashCode(resource);
		Integer serial = resourceKeys.get(hash);
		if(serial==null) {
			log.warn("Unrecognized resource returned to pool [" + resource + "]. Closing it.");
			resourceFactory.close(resource);
		} else {
			if(!inuse.remove(serial)) {
				if(available.contains(serial)) {
					log.warn("Unreserved resource returned to pool [" + resource + "]. Ignoring it.");
				} else {
					log.warn("Untracked resource returned to pool [" + resource + "]. Closing it.");
					resourceFactory.close(resource);
				}				
			} else {
				if(!available.contains(serial)) {
					available.add(serial);
				} else {
					log.warn("Tracked resource returned to pool but marked as available [" + resource + "]. Ignoring it.");
				}
			}
		}
	}
	
	/**
	 * Internal only.
	 * @param serial The serial number of the resource to get
	 * @return the indicated resource instance
	 */
	protected T getResource(int serial) {
		T t = resourceMap.get(serial);
		if(t==null) {
			synchronized(resourceMap) {
				t = resourceMap.get(serial);
				if(t==null) {
					t = resourceFactory.newResource();
					resourceMap.put(serial, t);
					resourceKeys.put(System.identityHashCode(t), serial);
				}
			}
		}
		try {
			if(!resourceFactory.testResource(t)) {
				throw new Exception();
			}
		} catch (Exception e) {
			resourceMap.remove(serial);
			resourceKeys.remove(System.identityHashCode(t));
			try { resourceFactory.close(t); } catch (Exception ex) {}
			t = resourceFactory.newResource();
			try {
				if(!resourceFactory.testResource(t)) {
					throw new Exception("Failed to test resource [" + t + "]", new Throwable());
				}
				resourceMap.put(serial, t);
				resourceKeys.put(System.identityHashCode(t), serial);				
			} catch (Exception ex) {
				throw new RuntimeException("Failed to validate newly created resource", ex);
			}
		}
		return t;
	}
	
	
	
	
	
	/**
	 * Returns the maximum number of resource instances that can be in use at any time 
	 * @return the maximum number of resource instances that can be in use at any time 
	 */
	public int getMaxResources() {
		return maxResources;
	}
	/**
	 * Sets the maximum number of resource instances that can be in use at any time 
	 * @param maxResources the maximum number of resource instances that can be in use at any time 
	 */
	public void setMaxResources(int maxResources) {
		this.maxResources = maxResources;
	}
	/**
	 * Returns the number of resource instances to create on pool start
	 * @return the number of resource instances to create on pool start
	 */
	public int getStartingResources() {
		return startingResources;
	}
	/**
	 * Sets the number of resource instances to create on pool start
	 * @param startingResources the number of resource instances to create on pool start
	 */
	public void setStartingResources(int startingResources) {
		this.startingResources = startingResources;
	}
	/**
	 * Returns the resource factory
	 * @return the resourceFactory
	 */
	public ResourceFactory<T> getResourceFactory() {
		return resourceFactory;
	}
	/**
	 * Sets the resource factory
	 * @param resourceFactory the resourceFactory to set
	 */
	public void setResourceFactory(ResourceFactory<T> resourceFactory) {
		this.resourceFactory = resourceFactory;
	}
	/**
	 * Returns the number of resources currently available
	 * @return the number of resources currently available
	 */
	public int getAvailable() {
		return available.size();
	}
	/**
	 * Returns the number of resources currently in use
	 * @return the number of resources currently in use
	 */
	public int getInuse() {
		return inuse.size();
	}
	/**
	 * Returns the default timeout in ms. when waiting for an available resource
	 * @return the timeout in ms. when waiting for an available resource
	 */
	public long getTimeout() {
		return timeout;
	}
	/**
	 * Sets the default timeout in ms. when waiting for an available resource
	 * @param timeout the timeout in ms. when waiting for an available resource
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * @return the running
	 */
	public AtomicBoolean getRunning() {
		return running;
	}

	
	
}
