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
package test.org.helios.patterns.queues;



import gnu.trove.set.hash.TLongHashSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.helios.patterns.queues.Filterable;
import org.helios.patterns.queues.FilteredBlockingQueue;
import org.helios.patterns.queues.LongBitMaskFactory;
import org.helios.patterns.queues.LongDiscriminator;
import org.helios.patterns.queues.LongBitMaskFactory.LongBitMaskSequence;

/**
 * <p>Title: FilteredQueueTestCase</p>
 * <p>Description: Test case for filtered queues</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.patterns.queues.FilteredQueueTestCase</code></p>
 */

public class FilteredQueueTestCase  extends TestCase {
	protected static final LongBitMaskSequence OPTIONS = LongBitMaskFactory.newSequence();
	public static final long ONE = OPTIONS.next();
	public static final long TWO = OPTIONS.next();
	public static final long THREE = OPTIONS.next();
	public static final long FOUR = OPTIONS.next();
	public static final long FIVE = OPTIONS.next();
	public static final long SIX = OPTIONS.next();
	public static final long SEVEN = OPTIONS.next();
	public static final long EIGHT = OPTIONS.next();
	public static final long NINE = OPTIONS.next();
	public static final long TEN = OPTIONS.next();
	public static final long[] EVENS = {TWO, FOUR, SIX, EIGHT, TEN}; 
	public static final long[] ODDS = {ONE, THREE, FIVE, SEVEN, NINE};
	public static final long[] ALL = {ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN};
	protected LongBitMaskSequence sequence;
	private static boolean DEBUG = false;
	/**
	 * @throws java.lang.Exception
	 */
	protected void setUp() throws Exception {
		super.setUp();
		sequence = LongBitMaskFactory.newSequence();
		DEBUG = false;
	}

	/**
	 * @throws java.lang.Exception
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	
	public void testEnableEvens() throws Exception {
		DEBUG = true;
		log("[testEnableEvens]");
		long value = 0L;
		// Enable Evens
		value = LongBitMaskSequence.setEnabledFor(value, EVENS);
//
//		log("Evens:" + Arrays.toString(EVENS));
//		log("Odds:" + Arrays.toString(ODDS));
//		log("BitMask[" + Long.toBinaryString(value) + "]");
		for(int i = 0; i < ALL.length; i++) {
//			log("Testing [" + i + "]  Mask Compare:\n\t[" + Long.toBinaryString(value) + "]\n\t[" + Long.toBinaryString(i) + "]\nEnabled:" + LongBitMaskSequence.isEnabledFor(value, i));
			if(isEven((i+1))) {
				Assert.assertEquals(
						"[" + i + "](" + ALL[i] + ") is EVEN so Enabled is TRUE", 
						true, LongBitMaskSequence.isEnabledFor(value, ALL[i]));
			} else {
				Assert.assertEquals("[" + i + "] is ODD so Enabled is FALSE", false, LongBitMaskSequence.isEnabledFor(ALL[i], value));
			}
		}
		
	}
	
	public static boolean isEven(long l) {
		if(l==1) return false;
		return l%2==0;
	}
	
	public void testEnableOdds() throws Exception {
		//DEBUG = true;
		log("[testEnableOdds]");
		long value = 0L;
		// Enable Odds
		value = LongBitMaskSequence.setEnabledFor(value, ODDS);
		log("BitMask[" + Long.toBinaryString(value) + "]");
		for(int i = 0; i < ALL.length; i++) {
//			log("Testing [" + i + "]  Mod:[" + (i%2) + "] Mask Compare:\n\t[" + Long.toBinaryString(ALL[i-1]) + "]\n\t[" + Long.toBinaryString(value) + "]\nEnabled:" + LongBitMaskSequence.isEnabledFor(ALL[i-1], value));
			if(isEven(i+1)) {
				Assert.assertEquals("[" + ALL[i] + "] is EVEN so Enabled is FALSE", false, LongBitMaskSequence.isEnabledFor(ALL[i], value));
			} else {
				Assert.assertEquals("[" + ALL[i] + "] is ODD so Enabled is TRUE", true, LongBitMaskSequence.isEnabledFor(ALL[i], value));
			}
		}
		
	}
	
	public void testDisableEvens() throws Exception {
		log("[testDisableEvens]");
		long value = 0L;
		// Enable All
		value = LongBitMaskSequence.setEnabledFor(value, ALL);
		// Disable Evens
		value = LongBitMaskSequence.setDisabledFor(value, EVENS);		
		log("BitMask[" + Long.toBinaryString(value) + "]");
		for(int i = 0; i < ALL.length; i++) {
//			log("Testing [" + i + "]  Mod:[" + (i%2) + "] Mask Compare:\n\t[" + Long.toBinaryString(ALL[i-1]) + "]\n\t[" + Long.toBinaryString(value) + "]\nEnabled:" + LongBitMaskSequence.isEnabledFor(ALL[i-1], value));
			if(isEven(i+1)) {
				Assert.assertEquals("[" + ALL[i] + "] is EVEN so Enabled is FALSE", false, LongBitMaskSequence.isEnabledFor(ALL[i], value));
			} else {
				Assert.assertEquals("[" + ALL[i] + "] is ODD so Enabled is TRUE", true, LongBitMaskSequence.isEnabledFor(ALL[i], value));
			}
		}
		
	}
	
	public void testDisableOdds() throws Exception {
		log("[testDisableOdds]");
		long value = 0L;
		// Enable All
		value = LongBitMaskSequence.setEnabledFor(value, ALL);		
		// Disable Odds
		value = LongBitMaskSequence.setDisabledFor(value, ODDS);
		
//		log("BitMask[" + Long.toBinaryString(value) + "]");
		for(int i = 0; i < ALL.length; i++) {
//			log("Testing [" + i + "]  Mod:[" + (i%2) + "] Mask Compare:\n\t[" + Long.toBinaryString(ALL[i-1]) + "]\n\t[" + Long.toBinaryString(value) + "]\nEnabled:" + LongBitMaskSequence.isEnabledFor(ALL[i-1], value));
			if(isEven(i+1)) {
				Assert.assertEquals("[" + ALL[i] + "] is EVEN so Enabled is TRUE", true, LongBitMaskSequence.isEnabledFor(ALL[i], value));
			} else {
				Assert.assertEquals("[" + ALL[i] + "] is ODD so Enabled is FALSE", false, LongBitMaskSequence.isEnabledFor(ALL[i], value));
			}
		}		
	}
	
	

	
	public void testEvenOddQueues() throws Exception {
		//DEBUG = true;
		log("[testEvenOddQueues]");
		long evenKey = LongBitMaskSequence.newMask(EVENS);
		long oddKey = LongBitMaskSequence.newMask(ODDS);
		
		TLongHashSet evenSet = new TLongHashSet(EVENS);
		TLongHashSet oddSet = new TLongHashSet(ODDS);
		FilteredBlockingQueue<LongBitMaskFilterable, Long> evenQueue = new FilteredBlockingQueue<LongBitMaskFilterable, Long>(new ArrayBlockingQueue<LongBitMaskFilterable>(EVENS.length), evenKey); 
		FilteredBlockingQueue<LongBitMaskFilterable, Long> oddsQueue = new FilteredBlockingQueue<LongBitMaskFilterable, Long>(new ArrayBlockingQueue<LongBitMaskFilterable>(ODDS.length), oddKey);
		
		for(int value = 0; value < ALL.length; value++) {
			LongBitMaskFilterable lbmf = new LongBitMaskFilterable(ALL[value]);
			evenQueue.add(lbmf);
			oddsQueue.add(lbmf);
		}
		log("EvenQueue:" + Arrays.toString(evenQueue.toArray()));
		log("OddQueue:" + Arrays.toString(oddsQueue.toArray()));

		Assert.assertEquals("Even Queue Should Have [" + EVENS.length + "] items", EVENS.length, evenQueue.size());
		Assert.assertEquals("Odd Queue Should Have [" + ODDS.length + "] items", ODDS.length, oddsQueue.size());

		for(LongBitMaskFilterable even: evenQueue.toArray(new LongBitMaskFilterable[EVENS.length])) {
			Assert.assertEquals("The value [" + even + "] should be EVEN", true, evenSet.contains(even.getMask()));
			Assert.assertEquals("The value [" + even + "] should not be ODD", false, oddSet.contains(even.getMask()));
		}
		for(LongBitMaskFilterable odd: oddsQueue.toArray(new LongBitMaskFilterable[ODDS.length])) {
			Assert.assertEquals("The value [" + odd + "] should be ODD", true, oddSet.contains(odd.getMask()));
			Assert.assertEquals("The value [" + odd + "] should not be EVEN", false, evenSet.contains(odd.getMask()));
		}
		
	}
	
	
	public void testFilterTime() throws Exception {
		DEBUG = true;
		log("[testFilterTime]");
		
		int queueSize = 1000000;
		sequence = LongBitMaskFactory.newSequence(new LongDiscriminator(){
			public boolean include(long value, int order) {
				return order%3==0;
			}			
		});
		log("Mask Members:" + sequence.getValues().length + Arrays.toString(sequence.getValues()));
		long filterMask = sequence.getMask();
		log("Mask[" + Long.toBinaryString(filterMask) + "]");
		Set<LongBitMaskFilterable> filterables = new HashSet<LongBitMaskFilterable>(queueSize);
		sequence = LongBitMaskFactory.newSequence();
		
		for(int i = 0; i < queueSize; i++) {
			long key = sequence.next();
			filterables.add(new LongBitMaskFilterable(key));
			//log("Added LongBitMaskFilterable [" + Long.toBinaryString(key) + "]");
			if(!sequence.hasNext()) sequence.reset();
		}
		log("Filterable Set Is Loaded");		
		BlockingQueue<LongBitMaskFilterable> queue = new ArrayBlockingQueue<LongBitMaskFilterable>(queueSize);
		FilteredBlockingQueue<LongBitMaskFilterable, Long> fQueue = new FilteredBlockingQueue<LongBitMaskFilterable, Long>(queue, filterMask);
		
		log("Starting Warmup");
		int warmups = 20;
		//DEBUG = false;
		for(int i = 0; i < warmups; i++) {
			queue.addAll(filterables);
			queue.clear();
		}
		log("Non Filtered Warmup Complete");
		for(int i = 0; i < warmups; i++) {
			fQueue.addAll(filterables);
			queue.clear();
		}
		log("Filtered Warmup Complete");
		fQueue.setFilterKey(null);		
		for(int i = 0; i < warmups; i++) {
			fQueue.addAll(filterables);
			queue.clear();
		}
		log("Null Filter Warmup Complete");
		DEBUG = true;
		log("Warmup Complete");
		// ============================================================
		// Filtering Queue: 30% Filter In
		// ============================================================
		queue.clear();
		fQueue.setFilterKey(filterMask);
		long start = System.currentTimeMillis();
		fQueue.addAll(filterables);
		long elapsed = System.currentTimeMillis()-start;
		log("Filtered Queue (30% In) Put[" + fQueue.size() + "]:" + elapsed + " ms.");
		int partialCount = fQueue.size();
		// ============================================================
		// Filtering Queue: 100% Filter In
		// ============================================================
		queue.clear();
		fQueue.setFilterKey(LongBitMaskSequence.newMask(LongBitMaskSequence.VALUES.toArray()));
		start = System.currentTimeMillis();
		fQueue.addAll(filterables);
		elapsed = System.currentTimeMillis()-start;
		log("Filtered Queue (100% In) Put[" + fQueue.size() + "]:" + elapsed + " ms.");
		// ============================================================
		// Null Filter Filtering Queue: ( 100% In )
		// ============================================================		
		queue.clear();
		fQueue.setFilterKey(null);
		start = System.currentTimeMillis();
		fQueue.addAll(filterables);
		elapsed = System.currentTimeMillis()-start;
		log("Null Filter Queue (100% In) Put[" + fQueue.size() + "]:" + elapsed + " ms.");
		// ============================================================
		// Non Filtering Queue: ( 100% In )
		// ============================================================		
		queue.clear();
		start = System.currentTimeMillis();
		queue.addAll(filterables);
		elapsed = System.currentTimeMillis()-start;
		log("Non Filtering Queue (100% In) Put[" + queue.size() + "]:" + elapsed + " ms.");
		// ============================================================
		// Non Filtering Queue: ( 30% In )
		// ============================================================		
		queue.clear();
		Iterator<LongBitMaskFilterable> iter = filterables.iterator();
		while(iter.hasNext()) {
			iter.next(); iter.remove();
			if(filterables.size()==partialCount) break;
		}
		start = System.currentTimeMillis();
		queue.addAll(filterables);
		elapsed = System.currentTimeMillis()-start;
		log("Non Filtering Queue (30% In) Put[" + queue.size() + "]:" + elapsed + " ms.");
		// ============================================================
		// Null Filter Filtering Queue: ( 100% In )
		// ============================================================		
		queue.clear();
		fQueue.setFilterKey(null);
		start = System.currentTimeMillis();
		fQueue.addAll(filterables);
		elapsed = System.currentTimeMillis()-start;
		log("Null Filter Queue (30% In) Put[" + fQueue.size() + "]:" + elapsed + " ms.");
		
		
	}
	
	
//	for(Field f: FilteredQueueTestCase.class.getDeclaredFields()) {
//		if(long.class.equals(f.getType())) {
//			long l = f.getLong(null);
//			log("OPTION:" + f.getName() + "[" + Long.toBinaryString(l) + "]" );
//		}
//	}
	
	
	public static void log(Object msg) {
		if(DEBUG) {
			System.out.println(msg);
		}
	}

	
	public static class LongBitMaskFilterable implements Filterable<Long> {
		protected final long mask;
		/**
		 * Creates a new LongBitMaskFilterable 
		 * @param mask
		 */
		public LongBitMaskFilterable(long mask) {
			this.mask = mask;
		}

		/**
		 * @param filterKey
		 * @return
		 */
		@Override
		public boolean drop(Long filterKey) {
			boolean drop = LongBitMaskSequence.isEnabledFor(mask, filterKey);
			//log("Testing [" + Long.toBinaryString(filterKey) + "] against Mask [" + Long.toBinaryString(mask) + "] Match:" + drop);
			return !drop;
		}

		/**
		 * @return the mask
		 */
		public long getMask() {
			return mask;
		}

		/**
		 * Constructs a <code>String</code> with key attributes in name = value format.
		 * @return a <code>String</code> representation of this object.
		 */
		public String toString() {		     
		    return "" + mask + " [" + Long.toBinaryString(mask) + "]";
		}
		
	}
	
	
	
}
