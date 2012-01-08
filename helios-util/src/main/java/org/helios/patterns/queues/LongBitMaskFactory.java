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

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.containers.sets.ReadOnlyTLongHashSet;
/**
 * <p>Title: LongBitMaskFactory</p>
 * <p>Description: A factory for creating objects that provide binary incrementing longs for bit masks  </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.patterns.queues.LongBitMaskFactory</code></p>
 */

public class LongBitMaskFactory {
	
	
	
	/**
	 * Returns a maximum sized new LongBitMaskSequence starting at 1
	 * @return a new LongBitMaskSequence
	 */
	public static LongBitMaskSequence newSequence() {
		return newSequence(LongBitMaskSequence.MAX_KEYS);
	}
	
	/**
	 * Returns a sized new LongBitMaskSequence starting at 1
	 * @param int size The number of bit masks to serve from the sequence
	 * @return a new LongBitMaskSequence
	 */
	public static LongBitMaskSequence newSequence(int size) {
		return new LongBitMaskSequence(size);
	}
	
	/**
	 * Returns a sized new LongBitMaskSequence starting at 1 and filtering entries using the passed discriminator
	 * @param discriminator The discriminator 
	 * @return a new LongBitMaskSequence
	 */
	public static LongBitMaskSequence newSequence(LongDiscriminator discriminator) {
		return new LongBitMaskSequence(discriminator);
	}
	
	
	
	public static class LongBitMaskSequence { // implements Iterator<Long> {	
		/** The item counter seed  */
		private final AtomicInteger counter = new AtomicInteger(0);
		/** The max size for this sequence */
		private final int maxSize;
		/** The actual values of the sequence */
		private final long[] values;
		/** The maximum number of bit representations that can be issued. */
		public static final int MAX_KEYS = Long.SIZE;		
		/** The largest key value */
		public static final long MAX_VALUE = 4611686018427387904L;
		/** The smallest key value */
		public static final long MIN_VALUE = 1L;
		/** An array of the possible values */
		private static final long[] KVALUES = {1L,2L,4L,8L,16L,32L,64L,128L,256L,512L,1024L,2048L,4096L,8192L,16384L,32768L,65536L,131072L,262144L,524288L,1048576L,2097152L,4194304L,8388608L,16777216L,33554432L,67108864L,134217728L,268435456L,536870912L,1073741824L,2147483648L,4294967296L,8589934592L,17179869184L,34359738368L,68719476736L,137438953472L,274877906944L,549755813888L,1099511627776L,2199023255552L,4398046511104L,8796093022208L,17592186044416L,35184372088832L,70368744177664L,140737488355328L,281474976710656L,562949953421312L,1125899906842624L,2251799813685248L,4503599627370496L,9007199254740992L,18014398509481984L,36028797018963968L,72057594037927936L,144115188075855872L,288230376151711744L,576460752303423488L,1152921504606846976L,2305843009213693952L,4611686018427387904L,-9223372036854775808L};
		/** The possible key values */
		public static final ReadOnlyTLongHashSet VALUES = new ReadOnlyTLongHashSet(KVALUES); 


		
		/**
		 * Creates a new LongBitMaskSequence starting at 1 
		 */
		LongBitMaskSequence() {
			this(MAX_KEYS);
		}
		
		/**
		 * Creates a new LongBitMaskSequence starting at 1
		 * @param size The number of bit keys in the sequence. 
		 */
		LongBitMaskSequence(int size) {
			if(size < 1 || size > MAX_KEYS) throw new IllegalArgumentException("Invalid size:" + size + ". Size must by > 1 and <= " + MAX_KEYS);
			maxSize = size;
			values = new long[size];
			System.arraycopy(KVALUES, 0, values, 0, size);
			counter.set(0);
		}
		
		/**
		 * Creates a new LongBitMaskSequence where the members are determined by the passed discriminator
		 * @param discriminator the discriminator 
		 */
		LongBitMaskSequence(LongDiscriminator discriminator) {
			if(discriminator==null) {
				maxSize = MAX_KEYS;
				values = new long[maxSize];
				System.arraycopy(KVALUES, 0, values, 0, maxSize);								
			} else {
				Vector<Long> longs = new Vector<Long>();
				for(int i = 0; i < MAX_KEYS; i++) {
					long l = KVALUES[i];
					if(discriminator.include(l, i+1)) {
						longs.add(l);
					}
				}
				maxSize = longs.size();
				values = new long[maxSize];
				int cntr = 0;
				for(long l: longs) {
					values[cntr] = l;
					cntr++;
				}
			}
			counter.set(0);
		}
		
		
		/**
		 * Returns all the member values of the sequence
		 * @return an array of member bits
		 */
		public long[] getValues() {
			return values.clone();
		}
		
		
		
		/**
		 * Determines if this sequence can still serve bit masks
		 * @return true if the sequence is still open, false if it is closed.
		 */
		public boolean isOpen() {
			return counter.get() < maxSize;
		}

		/**
		 * Determines if this sequence is exhausted
		 * @return true if the sequence is exhausted, false if it is still open.
		 */
		public boolean isClosed() {
			return counter.get() >= maxSize;
		}
		
		/**
		 * Returns the number of bitMasks that have been produced from this sequence
		 * @return the number of bitMasks that have been produced from this sequence
		 */
		public int getServed() {
			return counter.get();
		}
		
		/**
		 * Returns the number of available bitMasks that can still be produced from this sequence
		 * @return the number of available bitMasks 
		 */
		public int getAvailable() {
			return maxSize-counter.get();
		}
		
		
		/**
		 * Returns the current seed value. 
		 * Technically, even if <code>next()</code> has not been called yet, this will return the first value rather than raising an exception.
		 * In other words, sequentially calling <code>getCurrentValue()</code> and then <code>next()</code> will return the same value.  
		 * @return the current seed value
		 */
		public long getCurrentValue() {
			return values[counter.get()];
		}
		
		/**
		 * Generates a bit mask with all the member values of this sequence turned on
		 * @return a bit mask
		 */
		public long getMask() {
			return newMask(values);
		}
		
		/**
		 * Determines if the passed bitMask has the passed bit turned on.  
		 * @param bitMask The bit mask to test
		 * @param value The bit to test for.  
		 * @return true if the passed bitMask value is enabled in the passed bitMask
		 */
		public static boolean isEnabledFor(final long bitMask, final long value) {
			return (value & bitMask)!=0;
		}
		
		/**
		 * Determines if the passed bitMask is enabled for all the passed bit values
		 * @param bitMask The bit mask to test
		 * @param values The bits to test for
		 * @return true if all the passed bits are enabled in the bit mask, false otherwise
		 */
		public static boolean isEnabledForAll(final long bitMask, final long...values) {
			if(values==null || values.length < 1) return false;
			for(long l: values) {
				if(!isEnabledFor(bitMask, l)) return false;
			}
			return true;
		}
		
		/**
		 * Determines if the passed bitMask is enabled for any of the passed bit values
		 * @param bitMask The bit mask to test
		 * @param values The bits to test for
		 * @return true if at least one the passed bits are enabled in the bit mask, false otherwise
		 */
		public static boolean isEnabledForAny(final long bitMask, final long...values) {
			if(values==null || values.length < 1) return false;
			for(long l: values) {
				if(isEnabledFor(bitMask, l)) return true;
			}
			return false;
		}
		
		
		
		/**
		 * Turns on the bits in the passed bit mask represented by the passed values 
		 * @param bitMask The starting bitmask
		 * @param values The bits to apply. Values that are not valid single bit entries are ignored.
		 * @return the new bitmask
		 */
		public static long setEnabledFor(final long bitMask, final long...values) {
			long bitM = bitMask;
			if(values!=null) {
				for(long l: values) {
					if(isMaskBit(l)) {
						bitM = bitM | l;
					}
				}
			}
			return bitM;
		}
		
		/**
		 * Creates a new bitmask enabled for the passed bits
		 * @param values the bit settings to turn on
		 * @return a new bit mask
		 */
		public static long newMask(long...values) {
			long bitMask = 0;
			bitMask = setEnabledFor(bitMask, values);
			return bitMask;
		}
		
		/**
		 * Turns off the bits in the passed bit mask represented by the passed value 
		 * @param bitMask
		 * @param value
		 * @return the new bitmask
		 */
		public static long setDisabledFor(long bitMask, long...values) {
			if(values==null || values.length <1) return bitMask;
			long bitM = bitMask;
			for(long l: values) {
				bitM = l ^ bitM;
			}
			return bitM;
		}
		
		/**
		 * Determines if the passed long is a valid mask bit
		 * @param value the value to test
		 * @return true if the value is a valid mask bit
		 */
		public static boolean isMaskBit(long value) {
			return VALUES.contains(value);
		}
		
		/**
		 * Determines if the passed longs are all valid mask bits
		 * @param values the values to test
		 * @return true if all the values are valid mask bits
		 */
		public static boolean isMaskBit(long...values) {
			return VALUES.containsAll(values);
		}
		
		/**
		 * Determines if the passed number is even or odd
		 * @param l the value to test
		 * @return true if it is even, false if it is odd
		 */
		public static boolean isEven(long l) {
			if(l==1) return false;
			return l%2==0;
		}
		
		/**
		 * Resets the sequence.
		 */
		public void reset() {
			counter.set(0);
		}
		
		/**
		 * Returns the next seed in the sequence 
		 * @return the next seed in the sequence
		 */
		public long next() {
			int ctr = counter.get();
			if(ctr>=maxSize) {
				throw new RuntimeException("LongBitMaskSequence is exhausted as it has served [" + maxSize + "] bit masks.", new Throwable());
			}
			counter.incrementAndGet();
			return values[ctr];
		}

		/**
		 * Determines if the sequence can serve another value
		 * @return true if the sequence is open
		 */
		public boolean hasNext() {
			return isOpen();
		}

	}
	
	
	
	public static void main(String[] args) {
		log("Dumping values from a LongBitMaskSequence");
		LongBitMaskSequence seq = LongBitMaskFactory.newSequence(LongDiscriminator.EVENS);
		
		int i = 1;
		long l = 0;
		while(seq.hasNext()) {
			l = seq.next();
			log("[" + i + "]  Value:[" + l + "] Mask:[" + Long.toBinaryString(l) + "]  Mod:" + (l%2) + "Mod(i):" + (i%2));
			i++;
		}
		log("Final: [" + Long.toBinaryString(l).length() + "]");
		log("Max:[" + Long.toBinaryString(Long.MAX_VALUE).length() + "]");
		log("Max Long:[" + Long.MAX_VALUE + "]");
		StringBuilder b = new StringBuilder("1");
		for(i = 0; i < 62; i++) {
			b.append("0");
		}
		
		log("Parsed Long:[" + Long.parseLong(b.toString(), 2) + "]");
		log("Long Size:" + Long.SIZE);
		log("\n\n\n=======================");
		b = new StringBuilder("private static final long[] VALUES = {");
		seq.reset();
		while(seq.hasNext()) {
			b.append(seq.next()).append("L,");
		}
		b.deleteCharAt(b.length()-1);
		b.append("};");
		log(b.toString());
		
		
		
		
	}

	public static void log(Object msg) {
		System.out.println(msg);
	}
}
