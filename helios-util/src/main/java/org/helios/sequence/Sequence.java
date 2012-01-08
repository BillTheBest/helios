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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.apache.log4j.Logger;
import org.helios.helpers.CollectionHelper;
import org.helios.helpers.OpenTypeHelper;

/**
 * <p>Title: Sequence</p>
 * <p>Description: A persisted and batched sequence generator.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.sequence.Sequence</code></p>
 */

public class Sequence implements Serializable, Cloneable, CompositeData {
	/**  */
	private static final long serialVersionUID = 7275928199870292338L;
	/** The sequence provider */
	protected final AtomicLong seq = new AtomicLong(0);
	/** The highwater mark */
	protected final AtomicLong hwm = new AtomicLong(0);
	/** The batch size counter, starts out at <b><code>batchSize</code></b> and is decremented every time <b><code>seq</code></b> is incremented. When <b><code>seqCounter</code></b> reaches <b><code>lowThreshold</code></b>, an asynch reload request is dispatched. 
	 * When <b><code>seqCounter</code></b> reaches zero, all requests will block until the <b><code>reloadComplete</code></b> condition is true, meaning the reload request is complete. 
	 */
	protected transient int seqCounter;
	/** reload counter */
	protected transient volatile long reloadCounter = 0;
	/** The sequence manager */
	protected transient SequenceManager sequenceManager;
	/** The batch size */
	protected int batchSize;	
	/** The low batch threshold */
	protected int lowThreshold;
	/** The sequence name */
	protected String name;
	/** The access lock */
	protected transient ReentrantLock lock = new ReentrantLock();  
    /** Condition for waiting on sequence reload */
	protected transient Condition reloadComplete = lock.newCondition();
	/** Static logger */
	protected transient static final Logger log = Logger.getLogger(Sequence.class);
	/** The CompositeData Keys */
	protected static final Map<Integer, String> KEYS = CollectionHelper.createIndexedValueMap(true, true, "Name", "CurrentSequence", "HighWaterMark", "SequenceCounter", "ReloadCounter", "BatchSize", "LowThreshold");
	/** The CompositeData Descriptions */
	protected static final Map<Integer, String> DESCS = CollectionHelper.createIndexedValueMap(true, true, "The name of the sequence", "The current sequence value", "The highWater mark", "The current sequence counter", "The number of reloads executed", "The configured batch size", "The low threshold");
	/** The Data Types */
	protected static final SimpleType[] TYPES = new SimpleType[]{SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.INTEGER, SimpleType.LONG, SimpleType.INTEGER, SimpleType.INTEGER};
	/** The Composite Type */
	protected static final CompositeType compositeType = OpenTypeHelper.createCompositeType("Sequence", "A persisted and batched sequence generator", KEYS.values().toArray(new String[KEYS.size()]), DESCS.values().toArray(new String[DESCS.size()]), TYPES); 
	
	
	/**
	 * Creates a new Sequence 
	 * @param seed The seed starting value for the sequence 
	 * @param batchSize The batchSize for the sequence
	 * @param lowThreshold The low threshold triggering a reload
	 * @param name The name of the sequence
	 * @param sequenceManager The sequence manager managing this sequence.
	 */
	Sequence(long seed, int batchSize, int lowThreshold, String name, SequenceManager sequenceManager) {
		seq.set(seed);
		hwm.set(seed + batchSize);
		this.batchSize = batchSize;
		seqCounter = this.batchSize;
		this.lowThreshold = lowThreshold;
		this.name = name;
		this.sequenceManager = sequenceManager;
	}
	
	
	
	/**
	 * Returns the next sequence entry
	 * @return the next sequence entry
	 */
	public long next() {
		final ReentrantLock lock = this.lock;        
		try {
			lock.lock();
            try {
                seqCounter--;
                if(seqCounter==lowThreshold) {
                	sequenceManager.reload(this);
                }			            	
                while (seq.get()==hwm.get()) {
                	log.info("SEQ==HWM:" + seq.get() + "/" + hwm.get() + ". Waiting for reload");
                	reloadComplete.await(200, TimeUnit.MILLISECONDS);
                	//reloadComplete.await();
                }
            } catch (InterruptedException ie) {
            	throw new Exception("Thread interrupted while waiting for sequence [" + name + "] to reload.", ie);
            }
			return seq.incrementAndGet();
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire next sequence", e);
		} finally {
			 lock.unlock();
		}
	}
	
	/**
	 * Callback when the loader has completed the reload
	 */
	void onReload() {
		seqCounter = batchSize;	
		reloadCounter++;
		log.info("onReload for [" + name + "]");
		try {
			if(lock.isLocked()) {
				if(lock.hasWaiters(reloadComplete)) {
					log.info("Sending signal for [" + name + "]");
					reloadComplete.signalAll();
				}
			}
		} catch (Exception e) {
			if(log.isDebugEnabled()) log.debug("Reload signal for [" + name + "] failed", e);
		}
	}
	
	/**
	 * Returns the current sequence entry
	 * @return the current sequence entry
	 */
	public long current() {
		return seq.get();
	}
	
	/**
	 * Returns the sequence highwater mark
	 * @return the sequence highwater mark
	 */
	public long highwater() {
		return hwm.get();
	}
	
	/**
	 * The sequence batch size
	 * @return the batchSize
	 */
	public int getBatchSize() {
		return batchSize;
	}

	/**
	 * The counter low throwshold that triggers a reload
	 * @return the lowThreshold
	 */
	public int getLowThreshold() {
		return lowThreshold;
	}

	/**
	 * The sequence name
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("Sequence [")
	    	.append(TAB).append("name = ").append(this.name)
	        .append(TAB).append("seq = ").append(this.seq.get())
	        .append(TAB).append("hwm = ").append(this.hwm.get())
	        .append(TAB).append("seqCounter = ").append(this.seqCounter)
	        .append(TAB).append("batchSize = ").append(this.batchSize)
	        .append(TAB).append("lowThreshold = ").append(this.lowThreshold)
	        .append(TAB).append("reload count = ").append(this.reloadCounter)
	        .append("\n]");    
	    return retValue.toString();
	}
	
	/**
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		seq.set(hwm.get());
		seqCounter = batchSize;
		reloadCounter = 0;
		lock = new ReentrantLock();  	    
		reloadComplete = lock.newCondition();		
	}

	/**
	 * Returns the cummulative load count
	 * @return the cummulative load count
	 */
	public long getReloadCounter() {
		return reloadCounter;
	}

	/**
	 * @param arg0
	 * @return
	 */
	@Override
	public boolean containsKey(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param arg0
	 * @return
	 */
	@Override
	public boolean containsValue(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param arg0
	 * @return
	 */
	@Override
	public Object get(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param arg0
	 * @return
	 */
	@Override
	public Object[] getAll(String[] arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public CompositeType getCompositeType() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public Collection<?> values() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
