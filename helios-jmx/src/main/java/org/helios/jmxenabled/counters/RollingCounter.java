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
package org.helios.jmxenabled.counters;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.opentypes.OpenTypeManager;
import org.helios.jmx.opentypes.annotations.DelegateCompositeData;
import org.helios.jmx.opentypes.annotations.XCompositeAttribute;
import org.helios.jmx.opentypes.annotations.XCompositeType;

/**
 * <p>Title: RollingCounter</p>
 * <p>Description: Base class for all rolling counters needing CompositeData support.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.counters.RollingCounter</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
//@XCompositeType(description="A rolling fixed length value accumulator", name="org.helios.jmxenabled.counters.RollingCounter")
@XCompositeType(description="Rolling fixed length value accumulator")
public abstract class RollingCounter implements DelegateCompositeData, Serializable {
	/**  */
	private static final long serialVersionUID = 8228564688213089514L;
	/** The name of the counter */
	protected final String name;
	/** The maximum size and capacity of the counter */
	protected final int size;
	/** The read/write lock */
	protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false); 
	/** The read lock */
	protected final ReadLock readLock = lock.readLock(); 
	/** The writelock */
	protected final WriteLock writeLock = lock.writeLock();
	/** The current size of the counter */
	protected final AtomicInteger currentSize = new AtomicInteger(0);
	/** The write timeout in ms. */
	protected final long writeTimeout;
	/** The read timeout in ms. */
	protected final long readTimeout;
	
	/** The default write timeout ms. */
	public static final long DEFAULT_WRITE_TIMEOUT = 200; 
	/** The default read timeout in ms. */
	public static final long DEFAULT_READ_TIMEOUT = 500;
	
	/** Static logger */
	protected static final Logger LOG = Logger.getLogger(RollingCounter.class);
	
	/** The delegated CompositeData instance */
	private final AtomicReference<CompositeData> compositeData = new AtomicReference<CompositeData>(null);
	/** Instance logger */
	protected Logger log;
	
	/**
	 * Creates a new RollingCounter with the default read and write timeouts.
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 */
	protected RollingCounter(String name, int capacity) {
		this(name, capacity, DEFAULT_READ_TIMEOUT, DEFAULT_WRITE_TIMEOUT);
	}
	
	/**
	 * Creates a new RollingCounter with the default read and write timeouts.
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 * @param registerGroup A register map to group counters
	 */
	protected RollingCounter(String name, int capacity, final Map<String, RollingCounter> registerGroup) {
		this(name, capacity, DEFAULT_READ_TIMEOUT, DEFAULT_WRITE_TIMEOUT, registerGroup);
	}

	
	/**
	 * Creates a new RollingCounter
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 * @param readTimeout The read lock timeout in ms.
	 * @param writeTimeout The write lock timeout in ms.
	 */
	protected RollingCounter(String name, int capacity, long readTimeout, long writeTimeout) {
		this(name, capacity, readTimeout, writeTimeout, null);
	}
	
	/**
	 * Creates a new RollingCounter
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 * @param readTimeout The read lock timeout in ms.
	 * @param writeTimeout The write lock timeout in ms.
	 * @param registerGroup A register map to group counters
	 */
	protected RollingCounter(String name, int capacity, long readTimeout, long writeTimeout, final Map<String, RollingCounter> registerGroup) {
		this.name = name;
		this.readTimeout = readTimeout;
		this.writeTimeout = writeTimeout;
		this.size = capacity;
		if(registerGroup!=null) {
			registerGroup.put(this.name, this);
		}
	}
	
	/**
	 * Returns the type of the rolled items.
	 * @return the type of the rolled items.
	 */
	public abstract Class<?> getCounterType();
	
	/**
	 * Returns the name of the counter.
	 * @return the name of the counter.
	 */
	@JMXAttribute(name="{f:name}Name", description="The name of the counter", mutability=AttributeMutabilityOption.READ_ONLY)
	@XCompositeAttribute(key=true)
	public String getName() {
		return name;
	}

	/**
	 * Returns the number of entries in the counter.
	 * @return the number of entries in the counter
	 */
	@JMXAttribute(name="{f:name}Size", description="Returns the number of entries in the counter", mutability=AttributeMutabilityOption.READ_ONLY)
	@XCompositeAttribute
	public int getSize() {
		return currentSize.get();
	}

	/**
	 * Returns the length of the rolling window
	 * @return the length of the rolling window
	 */
	@JMXAttribute(name="{f:name}Capacity", description="Returns the length of the rolling window", mutability=AttributeMutabilityOption.READ_ONLY)
	@XCompositeAttribute
	public int getCapacity() {
		return size;
	}
	

	/**
	 * Determines if the delegate CompositeType is primed.
	 * @return true if the delegate CompositeType is primed.
	 */
	public boolean isReady() {
		return compositeData.get()!=null;
	}
	
	private CompositeData getCd() {
		CompositeData cd = compositeData.get();
		if(cd==null) {
			synchronized(compositeData) {
				cd = compositeData.get();
				if(cd==null) {
					try {
						cd = OpenTypeManager.getInstance().getCompositeDataInstance(this);
					} catch (Exception e) {
						log.error("Failed to generate CompositeData", e);
						throw new RuntimeException(getClass().getName());
					}					
				}
			}
		}
		return cd;
	}
	
	
	/**
	 * @param key
	 * @return
	 * @see javax.management.openmbean.CompositeData#containsKey(java.lang.String)
	 */
	public boolean containsKey(String key) {
		return getCd().containsKey(key);
	}
	/**
	 * @param value
	 * @return
	 * @see javax.management.openmbean.CompositeData#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return getCd().containsValue(value);
	}
	/**
	 * @param key
	 * @return
	 * @see javax.management.openmbean.CompositeData#get(java.lang.String)
	 */
	public Object get(String key) {
		return getCd().get(key);
	}
	/**
	 * @param keys
	 * @return
	 * @see javax.management.openmbean.CompositeData#getAll(java.lang.String[])
	 */
	public Object[] getAll(String[] keys) {
		return getCd().getAll(keys);
	}
	/**
	 * @return
	 * @see javax.management.openmbean.CompositeData#getCompositeType()
	 */
	public CompositeType getCompositeType() {
		return getCd().getCompositeType();
	}
	/**
	 * @return
	 * @see javax.management.openmbean.CompositeData#toString()
	 */
	public String toString() {
		return getCd().toString();
	}
	/**
	 * @return
	 * @see javax.management.openmbean.CompositeData#values()
	 */
	public Collection<?> values() {
		return getCd().values();
	}
	
	/**
	 * Converts this map to a CompositeDataSupport instance on serialization
	 * @return a CompositeDataSupport instance
	 * @throws ObjectStreamException
	 */
	protected Object writeReplace() throws ObjectStreamException {
		try {
			return getCd();
		} catch (Exception e) {
			log.error("Failed to convert an instance of type [" + getClass().getName() + "] to a CompositeDataSupport on serialization", e);
			throw new RuntimeException("Failed to convert an instance of type [" + getClass().getName() + "] to a CompositeDataSupport on serialization", e);
		}
	}

	/**
	 * Returns the delegate composite data
	 * @return the compositeData
	 */
	public CompositeData getDelegate() {
		return getCd();
	}
	

}
