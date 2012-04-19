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
package org.helios.patterns.queues;

import org.helios.patterns.queues.TimeoutQueueMap.TimeoutQueueMapKey;


/**
 * <p>Title: DecayQueueMap</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.patterns.queues.DecayQueueMap</code></p>
 */

public class DecayQueueMap<K, V> extends TimeoutQueueMap<K, V> {

	protected TimeoutQueueMapKey<K, V> getKeyFor(K key, V value, long delayTime) {
		return new TimeoutQueueMapKey<K, V>(key, value, delayTime);
	}

	
	
	/**
	 * Creates a new DecayQueueMap
	 * @param defaultDelayTime
	 */
	public DecayQueueMap(long defaultDelayTime) {
		super(defaultDelayTime);
	}

	/**
	 * Creates a new DecayQueueMap
	 * @param defaultDelayTime
	 * @param initialCapacity
	 * @param loadFactor
	 * @param concurrencyLevel
	 */
	public DecayQueueMap(long defaultDelayTime, int initialCapacity,
			float loadFactor, int concurrencyLevel) {
		super(defaultDelayTime, initialCapacity, loadFactor, concurrencyLevel);
	}
		

	/**
	 * Creates a new DecayQueueMap
	 * @param defaultDelayTime
	 * @param initialCapacity
	 * @param loadFactor
	 */
	public DecayQueueMap(long defaultDelayTime, int initialCapacity,
			float loadFactor) {
		super(defaultDelayTime, initialCapacity, loadFactor);
	}

	/**
	 * Creates a new DecayQueueMap
	 * @param defaultDelayTime
	 * @param initialCapacity
	 */
	public DecayQueueMap(long defaultDelayTime, int initialCapacity) {
		super(defaultDelayTime, initialCapacity);
	}
	
	

}
