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
package org.helios.tracing.core.trace.cache;

import java.net.InetAddress;

import org.helios.sequence.SequenceManager;

/**
 * <p>Title: AbstractTraceModelCache</p>
 * <p>Description: Abstract base class for concrete TraceModelCache implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.cache.AbstractTraceModelCache</code></p>
 */

public abstract class AbstractTraceModelCache implements TraceModelCache {
	/** The sequence manager that will generate ID keys for new entities added to cache */
	protected SequenceManager sequenceManager = null;
	/** The persisted indicator for this cache */
	protected final PersistedIndicators persisted = new PersistedIndicators();
	
	
	
	/**
	 * Creates a new AbstractTraceModelCache
	 * @param sequenceManager The sequence manager that will generate ID keys for new entities added to cache 
	 */
	public AbstractTraceModelCache(SequenceManager sequenceManager) {
		super();
		this.sequenceManager = sequenceManager;
	}
	/**
	 * Attempts to resolve to the host name.
	 * @param hostName The hostname to resolve
	 * @return the resolved host name
	 */
	public String resolveHostName(String hostName) {
		if(hostName==null || hostName.length()<1) throw new RuntimeException("Host name was null or zero length", new Throwable());
		try {
			return InetAddress.getByName(hostName).getHostName();
		} catch (Exception e) {
			return hostName;
		}		
	}
	/**
	 * Attempts to resolve to the host address.
	 * @param hostName The hostname to resolve
	 * @return the resolved host address
	 */
	public String resolveHostAddress(String hostName) {
		if(hostName==null || hostName.length()<1) throw new RuntimeException("Host name was null or zero length", new Throwable());
		try {
			return InetAddress.getByName(hostName).getHostAddress();
		} catch (Exception e) {
			return hostName;
		}		
	}
	/**
	 * Attempts to resolve to the fully qualified host name.
	 * @param hostName The hostname to resolve
	 * @return the resolved fully qualified host name
	 */
	public String resolveHostFullName(String hostName) {
		if(hostName==null || hostName.length()<1) throw new RuntimeException("Host name was null or zero length", new Throwable());
		try {
			return InetAddress.getByName(hostName).getCanonicalHostName();
		} catch (Exception e) {
			return hostName;
		}		
	}
	/**
	 * Returns this cache's sequence manager
	 * @return the sequenceManager
	 */
	public SequenceManager getSequenceManager() {
		return sequenceManager;
	}
	
	/**
	 * Returns this cache's persisted indicators
	 * @return this cache's persisted indicators
	 */
	public PersistedIndicators getPersistedIndicators() {
		return persisted;
	}
	
	/**
	 * @param sequenceManager the sequenceManager to set
	 */
	public void setSequenceManager(SequenceManager sequenceManager) {
		this.sequenceManager = sequenceManager;
	}
	/**
	 * Creates a new hash
	 * @param name The name of the hash
	 * @param size The initial capacity
	 */
	public void addHash(String name, int size) {
		persisted.addHash(name, size);
	}
	/**
	 * Creates a new hash
	 * @param name The name of the hash
	 */
	public void addHash(String name) {
		persisted.addHash(name);
	}
	/**
	 * Adds a value to the named hash. If the named hash does not exist, it will be created.
	 * @param name The hash name
	 * @param value The value to add
	 */
	public void addValue(String name, long value) {
		persisted.addValue(name, value);
	}
	/**
	 * Determines if the named hash contains the passed value
	 * @param name The name of the hash
	 * @param value The value to check for 
	 * @return true if the named hash contains the passed value
	 */
	public boolean contains(String name, long value) {
		return persisted.contains(name, value);
	}
	/**
	 * Determines if the passed hash name exists in the hashSet
	 * @param name The hash name
	 * @return true if the named hash exists, false if it does not
	 */
	public boolean exists(String name) {
		return persisted.exists(name);
	}
	
	
	
}
