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
package org.helios.sequence;

/**
 * <p>Title: SequenceCacheLoader</p>
 * <p>Description: Defines a sequence cache loader responsible managing the persistence of a sequence cache.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.sequence.SequenceCacheLoader</code></p>
 */

public interface SequenceCacheLoader {
	
	/**
	 * Loads the sequence cache from the persistent store
	 * @return a sequence cache
	 */
	public SequenceCache loadSequenceCache();
	
	/**
	 * Saves the passed sequence cache, overwriting all values
	 * @param cache The cache to save
	 */
	public void saveSequenceCache(SequenceCache cache);
	
	/**
	 * Loads the next batch window for the passed sequence and persists persists the cache to store.
	 * @param sequence The sequence to reload
	 * @return the next seed for the passed sequence.
	 */
	public long reloadSequence(Sequence sequence);
	
	/**
	 * Saves a new sequence into the persistent cache store
	 * @param sequence The new sequence
	 * @param cache The sequence cache the sequence has already been writen into
	 */
	public void saveSequence(Sequence sequence, SequenceCache cache);
	
	/**
	 * Purges the underlying data store, deleting all sequence cache entries.
	 */
	public void purge();
	
	
}
