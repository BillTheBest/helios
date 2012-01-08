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
 * License with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.jmxenabled.counters;



import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.opentypes.annotations.XCompositeAttribute;
import org.helios.jmx.opentypes.annotations.XCompositeType;

/**
 * <p>Title: IntegerRollingCounter</p>
 * <p>Description: A rolling fixed length int value accumulator.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.counters.IntegerRollingCounter</code></p>
 * @TODO: Detect int value overflow.
 */
@XCompositeType(description="Rolling integer accumulator")
public class IntegerRollingCounter extends RollingCounter {
	/** The underlying int values counter */
	protected final TIntArrayList tInteger;
	
	/**
	 * Creates a new IntegerRollingCounter with the default read and write timeouts.
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 */
	public IntegerRollingCounter(String name, int capacity) {
		this(name, capacity, DEFAULT_READ_TIMEOUT, DEFAULT_WRITE_TIMEOUT, null);
	}
	
	/**
	 * Creates a new IntegerRollingCounter with the default read and write timeouts.
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 * @param registerGroup A register map to group counters
	 */
	public IntegerRollingCounter(String name, int capacity, final Map<String, RollingCounter> registerGroup) {
		this(name, capacity, DEFAULT_READ_TIMEOUT, DEFAULT_WRITE_TIMEOUT, registerGroup);
	}
	
	
	
	/**
	 * Creates a new IntegerRollingCounter
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 * @param readTimeout The read lock timeout in ms.
	 * @param writeTimeout The write lock timeout in ms.
	 */
	public IntegerRollingCounter(String name, int capacity, long readTimeout, long writeTimeout) {
		super(name, capacity, readTimeout, writeTimeout);
		tInteger = new TIntArrayList(capacity);
	}
	
	/**
	 * Creates a new IntegerRollingCounter
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 * @param readTimeout The read lock timeout in ms.
	 * @param writeTimeout The write lock timeout in ms.
	 * @param registerGroup A register map to group counters
	 */
	public IntegerRollingCounter(String name, int capacity, long readTimeout, long writeTimeout, final Map<String, RollingCounter> registerGroup) {
		super(name, capacity, readTimeout, writeTimeout, registerGroup);
		tInteger = new TIntArrayList(capacity);
	}
	
	/**
	 * Returns the type of the rolled items.
	 * @return the type of the rolled items.
	 */
	public Class<?> getCounterType() {
		return int.class;
	}
	
	/**
	 * Adds a new "most recent" int value to the counter
	 * @param value the int value to add
	 */
	@JMXOperation(name="put", description="Puts a new value at the head of the counter")
	public void put(@JMXParameter(name="value", description="The int value to put into the counter") int value) {
		boolean acquiredLock = false;
		try {
			acquiredLock = writeLock.tryLock(writeTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				tInteger.insert(0, value);
				int newSize = currentSize.incrementAndGet();
				if(newSize>size) {
					tInteger.remove(newSize-1);
					currentSize.decrementAndGet();
				}
			}
		} catch (Exception e) {
			LOG.error("int value put error [" + value + "]", e);
		} finally {
			if(acquiredLock) try { writeLock.unlock(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Renders the contents of the counter.
	 * @return A string representation of a int array
	 */
	public String toString() {
		return Arrays.toString(getContents());		
	}
	
	/**
	 * Returns a copy of the internal contents of the counter.
	 * @return the counter contents 
	 */
	@JMXAttribute(name="{f:name}Contents", description="Returns a copy of the internal contents of the counter", mutability=AttributeMutabilityOption.READ_ONLY)
	@XCompositeAttribute
	public int[] getContents() {
		boolean acquiredLock = false;
		try {
			acquiredLock = readLock.tryLock(readTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				return tInteger.toArray();
			} else {
				throw new TimeoutException("Failed to acquire readLock within [" + readTimeout + "] ms.");
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to render the counter", e);
		} finally {
			if(acquiredLock) try { readLock.unlock(); } catch (Exception e) {}
		}			
	}
	
	/**
	 * Returns a subrange copy of the internal contents of the counter.
	 * @param lastN the number of items to return, starting with the most recent
	 * @return the counter contents subrange
	 */
	@JMXOperation(name="getContents", description="Returns a subrange copy of the internal contents of the counter")
	public int[] getContents(@JMXParameter(name="lastN", description="the number of items to return, starting with the most recent") int lastN) {		
		if(currentSize.get()<1) {
			return new int[]{};
		}		
		int[] values = getContents();
		int size = values.length;
		int rangeSize = lastN>size ? size : lastN;
		int[] subrange = new int[rangeSize];
		System.arraycopy(values, 0, subrange, 0, rangeSize);
		return subrange;		
	}
	
	
	/**
	 * Resets the rolling counter  
	 */
	@JMXOperation(name="reset", description="Resets the rolling counter")
	public void reset() {
		boolean acquiredLock = false;
		try {
			acquiredLock = writeLock.tryLock(writeTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				tInteger.reset();
				currentSize.set(0);
			} else {
				throw new TimeoutException("Failed to acquire writeLock within [" + writeTimeout + "] ms.");
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to reset the counter", e);
		} finally {
			if(acquiredLock) try { writeLock.unlock(); } catch (Exception e) {}
		}			
	}
	
	

	
	/**
	 * Returns the maximum value in the history counter
	 * @return the maximum value in the history counter
	 */
	@JMXAttribute(name="{f:name}RangeMaximum", description="The maximum value in the history counter", mutability=AttributeMutabilityOption.READ_ONLY)
	@XCompositeAttribute
	public int getRangeMax() {
		boolean acquiredLock = false;
		try {
			acquiredLock = readLock.tryLock(readTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				return tInteger.max();
			} else {
				throw new TimeoutException("Failed to acquire readLock within [" + readTimeout + "] ms.");
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire the counter range max", e);
		} finally {
			if(acquiredLock) try { readLock.unlock(); } catch (Exception e) {}
		}			
		
	}
	
	/**
	 * Returns the minimum value in the history counter
	 * @return the minimum value in the history counter
	 */
	@JMXAttribute(name="{f:name}RangeMinimum", description="The minimum value in the history counter", mutability=AttributeMutabilityOption.READ_ONLY)
	@XCompositeAttribute
	public int getRangeMin() {
		boolean acquiredLock = false;
		try {
			acquiredLock = readLock.tryLock(readTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				return tInteger.min();
			} else {
				throw new TimeoutException("Failed to acquire readLock within [" + readTimeout + "] ms.");
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire the counter range min", e);
		} finally {
			if(acquiredLock) try { readLock.unlock(); } catch (Exception e) {}
		}			
		
	}
	
	
	
	/**
	 * Returns the last value inserted into the counter
	 * @return the last value inserted into the counter
	 */
	@JMXAttribute(name="{f:name}LastValue", description="Returns the last value inserted into the counter", mutability=AttributeMutabilityOption.READ_ONLY)
	@XCompositeAttribute
	public int getLastValue() {
		if(currentSize.get()<1) {
			return -1;
		} else {
			int[] values = getContents();
			return values.length<1 ? -1 : values[0];
		}
	}
	
	/**
	 * Returns the total of all the values in the history counter
	 * @return the total 
	 */
	@XCompositeAttribute
	public int getTotal() {
		if(currentSize.get()<1) {
			return 0;
		} else {
			int[] values = getContents();
			int total = 0;
			for(int value: values) {
				total = total + value;
			}
			return total;
		}		
	}

	
	/**
	 * Returns the total of the most recent <code>lastN</code> values
	 * @param lastN the number of items to return, starting with the most recent
	 * @return the total of the most recent <code>lastN</code> values
	 */
	public int getTotal(int lastN) {
		if(currentSize.get()<1) {
			return 0;
		} else {
			int[] values = getContents(lastN);
			int total = 0;
			for(int value: values) {
				total = total + value;
			}
			return total;
		}				
	}
	
	
	/**
	 * Returns the average of all the values in the history counter
	 * @return the average 
	 */
	@JMXAttribute(name="{f:name}Average", description="Returns the average of all the values in the history counter", mutability=AttributeMutabilityOption.READ_ONLY)
	@XCompositeAttribute
	public int getAverage() {
		if(currentSize.get()<1) {
			return 0;
		} else {
			int[] values = getContents();
			int total = 0;
			for(int value: values) {
				total = total + value;
			}
			return avg(values.length, total);
		}				
	}
	
	/**
	 * Returns the average of the most rcent <code>lastN</code> values in the history counter
	 * @param lastN The number of items to calculate an average for, starting with the most recent
	 * @return the average 
	 */
	@JMXOperation(name="getAverage", description="Returns the average of the most rcent <code>lastN</code> values in the history counter")
	public int getAverage(@JMXParameter(name="lastN", description="The number of items to calculate an average for, starting with the most recent") int lastN) {
		if(currentSize.get()<1) {
			return 0;
		} else {
			int[] values = getContents(lastN);
			int total = 0;
			for(int value: values) {
				total = total + value;
			}
			return avg(values.length, total);
		}				
	}
	
	
	/**
	 * Calculates an average
	 * @param count the count of items
	 * @param total the sum of the items
	 * @return the calculated average
	 */
	public static int avg(int count, int total) {
		if(count<1 || total < 1) return 0;
		int avg = total/count;
		return (int)avg;
	}
	
	/*
	 * Average for Last n
	 */
	

	public static void main(String[] args) {
		BasicConfigurator.configure();
		LOG.setLevel(Level.INFO);
		LOG.info("IntegerRollingCounter Test");
		IntegerRollingCounter ctr = new IntegerRollingCounter("Snafu", 10);
		LOG.info("Empty Contents:[" + Arrays.toString(ctr.getContents()) + "]");
		Random r = new Random(System.nanoTime());
		LOG.info("Size:" + ctr.getSize() + " Last Value:" + ctr.getLastValue());
		for(int i = 0; i < 20; i++) {
			ctr.put(Math.abs(r.nextInt()));
			LOG.info("Size:" + ctr.getSize() + " Last Value:" + ctr.getLastValue() + ", Average:" + ctr.getAverage());			
		}
		LOG.info("==============================");
		LOG.info("Last 5 Values:" + Arrays.toString(ctr.getContents(5)));
	}
}
