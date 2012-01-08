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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.ClassHelper;


/**
 * <p>Title: SequenceManager</p>
 * <p>Description: Manages a group of sequences.</p> 
 * <p>The persisted sequence counter (seq) always contains the highwater mark
 * to which the sequence is allowed to increment to without a reload. THe lowThreshold is
 * the descremented value of seqCounter that triggers a reload. Seed setting lifecycle:<ul>
 * <li>On a new Sequence creation, the saved highwatermark is the initial seed + batch size.</li>
 * <li>On a Sequence save, the saved highwatermark is the initial seed + batch size.</li>
 * </ul>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.sequence.SequenceManager</code></p>
 */

public class SequenceManager {
	/** The sequence cache */
	protected final SequenceCache sequenceCache;
	/** The sequence manager loader */
	protected final SequenceCacheLoader loader;
	/** Static logger */
	protected static final Logger log = Logger.getLogger(SequenceManager.class);
	/** Shared thread pool for asynch sequence reloads */
	protected static final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory(){
		private final AtomicLong serial = new AtomicLong(0L);
		private final ThreadGroup threadGroup = new ThreadGroup("SequenceManagerThreadGroup");
		public Thread newThread(Runnable r) {
			Thread t = new Thread(threadGroup, r, "SequenceManagerThread#" + serial.incrementAndGet());
			t.setDaemon(true);			
			return t;
		}
	});
	
	/** The default batch size */
	public static final int DEFAULT_BATCH_SIZE = 100;
	/** The default low threshold */
	public static final int DEFAULT_LOW_THRESHOLD = 10;
	
	
	/**
	 * Creates a new SequenceManager 
	 * @param loader The loader the sequence manager will use
	 */
	public SequenceManager(SequenceCacheLoader loader) {		
		this.loader = ClassHelper.nvl(loader, new FileSequenceCacheLoader());
		sequenceCache = this.loader.loadSequenceCache();
		for(Sequence s: sequenceCache.values()) {
			s.sequenceManager = this;
		}
		if(log.isDebugEnabled()) log.debug("SequenceCache loaded with [" + sequenceCache.size() + "] entries.");
	}
	
	/**
	 * Adds a new sequence to the cache persistent store
	 * @param name The name of the sequence
	 * @param batchSize The batch size of the sequence
	 * @param lowThreshold The low threshold of the sequence
	 * @param initialSeed The initial seed value for the sequence
	 * @param seqManager The owning sequence manager
	 * @return The created sequence
	 */
	public Sequence addSequence(String name, int batchSize, int lowThreshold, long initialSeed, SequenceManager seqManager) {
		if(sequenceCache.containsKey(ClassHelper.nvl(name, "Sequence name was null"))) {
			throw new RuntimeException("A sequence named [" + name + "] already exists");
		}
		Sequence sequence = new Sequence(initialSeed, batchSize, lowThreshold, name, seqManager);
		sequenceCache.put(name, sequence);
		loader.saveSequence(sequence, sequenceCache);
		return sequence;
		
	}
	
	/**
	 * Adds a new sequence to the cache persistent store with an initial seed of 0.
	 * @param name The name of the sequence
	 * @param batchSize The batch size of the sequence
	 * @param lowThreshold The low threshold of the sequence
	 * @param seqManager The owning sequence manager
	 * @return The created sequence
	 */
	public Sequence addSequence(String name, int batchSize, int lowThreshold, SequenceManager seqManager ) {
		return addSequence(name, batchSize, lowThreshold, 0, seqManager);
	}
	
	/**
	 * Purges the sequence cache underlying data store, deleting all sequence cache entries.
	 */
	public void purge() {
		sequenceCache.clear();
		loader.purge();		
	}
	
	/**
	 * Returns an array of all the sequence names in cache
	 * @return an array of all the sequence names in cache
	 */
	public String[] getSequenceNames() {
		return sequenceCache.keySet().toArray(new String[sequenceCache.size()]);
	}
	
	
	/**
	 * Indicates if the named sequence is created
	 * @param name The name of the sequence
	 * @return true if the sequence is created, false if not
	 */
	public boolean isSequenceCreated(String name) {
		return sequenceCache.containsKey(ClassHelper.nvl(name, "Sequence name was null"));
	}
	
	/**
	 * Returns the named sequence
	 * @param name the name of the sequence to retrieve
	 * @return the named sequence
	 */
	public Sequence getSequence(String name) {
		Sequence seq = sequenceCache.get(ClassHelper.nvl(name, "Sequence name was null"));
		if(seq == null) {
			synchronized(sequenceCache) {
				seq = sequenceCache.get(name);
				if(seq == null) {
					addSequence(name, DEFAULT_BATCH_SIZE, DEFAULT_LOW_THRESHOLD, this);
				}
			}
		}
		return seq;
	}
	

	/**
	 * Creates a new SequenceManager using the default loader 
	 */
	public SequenceManager() {
		this(null);
	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		log.info("SequenceManager Test");
		SequenceManager sm = new SequenceManager();
		log.info(sm);
		
		Sequence seq = sm.addSequence("" + System.currentTimeMillis(), 10, 2, sm);
		log.info("Created sequence:" + seq);
		log.info(sm);
		for(int i = 0; i < 20; i++) {
			seq.next();
			log.info(seq);
		}	
		log.info("Done");
	}
	
	
	
	


	/**
	 * Call from a sequence requesting a reload on a low threshold
	 * @param sequence The sequence requesting the reload
	 */
	void reload(final Sequence sequence) {
		log.info("Dispatching reload for sequence [" + sequence.getName() + "]");
		executor.execute(new Runnable(){
			public void run() {				
				sequence.hwm.set(sequence.hwm.get() + sequence.batchSize);
				loader.saveSequence(sequence, sequenceCache);
				sequence.onReload();
				log.info("Completed reload for sequence [" + sequence.getName() + "]\n\t" + sequence);
			}
		});
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("SequenceManager [")
	    	.append(TAB).append("loader = ").append(this.loader)
	        .append(TAB).append("SequenceCache Entries:");
	        for(Sequence seq: sequenceCache.values()) {
	        	retValue.append("\n\t\t[").append(seq.getName()).append("] Current:").append(seq.current())
	        	.append(" Batch Size:").append(seq.getBatchSize())
	        	.append(" Low Threshold:").append(seq.getLowThreshold());
	        }
	        retValue.append("\n]");    
	    return retValue.toString();
	}
}
