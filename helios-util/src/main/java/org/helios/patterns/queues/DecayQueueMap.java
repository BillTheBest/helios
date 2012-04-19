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

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * <p>Title: DecayQueueMap</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.patterns.queues.DecayQueueMap</code></p>
 */

public class DecayQueueMap<K, V> extends TimeoutQueueMap<K, V> {

	/**
	 * {@inheritDoc}
	 * @see org.helios.patterns.queues.TimeoutQueueMap#getKeyFor(java.lang.Object, java.lang.Object, long)
	 */
	protected DecayQueueMapKey<K, V> getKeyFor(K key, V value, long delayTime) {
		return new DecayQueueMapKey<K, V>(key, value, delayTime);
	}
	
	/**
	 * @param key
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public V get(Object key) {
		V v = referenceMap.get(key);
		if(v!=null) {
			/*  Need to touch key in queue
			 * but there's no way to retrieve a reference
			 * to the mapkey in the queue, so we have to iterate
			 * all the keys in the queue.
			 * Needs to be optimized. 
			 */ 
			boolean foundKey = false;
			for(TimeoutQueueMapKey<K, V> k: this.timeOutQueue) {
				if(key.equals(k.getKey())) {
					((DecayQueueMapKey)k).touch();
					foundKey = true;
					break;
				}
			}			
			if(!foundKey) {
				throw new RuntimeException("Value for key [" + key + "] was found in reference map but not in decay queue. Programmer Error", new Throwable());
			}
		}
		return v;
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
	
	/**
	 * <p>Title: DecayQueueMapKey</p>
	 * <p>Description: A timestamped {@link Delayed} wrapper around a referenced object</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.patterns.queues.DecayQueueMap.DecayQueueMapKey</code></p>
	 */
	protected class DecayQueueMapKey<K, V> extends TimeoutQueueMapKey<K, V> {
		/** The updateable delay timestamp */
		protected final AtomicLong decayTimestamp = new AtomicLong(-1L);
		/** The original delay time offset */
		protected final long delayTime;
		
		/**
		 * Creates a new DecayQueueMapKey
		 * @param key
		 * @param delayed
		 * @param delayTime
		 */
		public DecayQueueMapKey(K key, V delayed, long delayTime) {
			super(key, delayed, delayTime);
			this.delayTime = delayTime;
			touch();
		}
		
		/**
		 * Returns the time remaining on the delay for this object.
		 * {@inheritDoc}
		 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
		 */
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert((decayTimestamp.get()-System.currentTimeMillis()), TimeUnit.MILLISECONDS);
		}
		
		/**
		 * Touches the updateable delay timestamp
		 */
		public void touch() {
			decayTimestamp.set(System.currentTimeMillis() + delayTime);
		}
		
	}	

}
