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



import gnu.trove.list.array.TLongArrayList;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.opentypes.OpenTypeManager;
import org.helios.jmx.opentypes.annotations.XCompositeAttribute;
import org.helios.jmx.opentypes.annotations.XCompositeType;
import org.helios.jmx.opentypes.tabular.ConcurrentTabularData;

/**
 * <p>Title: LongRollingCounter</p>
 * <p>Description: A rolling fixed length long value accumulator.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.counters.LongRollingCounter</code></p>
 * @TODO: Detect long value overflow.
 */
@XCompositeType(description="Rolling long accumulator")
public class LongRollingCounter extends RollingCounter implements IRollingCounter {
	/**  */
	private static final long serialVersionUID = -7250206330548277469L;
	/** The underlying long values counter */
	protected final TLongArrayList tLong;
	
	/**
	 * Creates a new LongRollingCounter with the default read and write timeouts.
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 */
	public LongRollingCounter(String name, int capacity) {
		this(name, capacity, DEFAULT_READ_TIMEOUT, DEFAULT_WRITE_TIMEOUT);
	}
	
	/**
	 * Creates a new LongRollingCounter with the default read and write timeouts.
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 * @param registerGroup A register map to group counters
	 */
	public LongRollingCounter(String name, int capacity, final Map<String, RollingCounter> registerGroup) {
		this(name, capacity, DEFAULT_READ_TIMEOUT, DEFAULT_WRITE_TIMEOUT, registerGroup);
	}
	
	
	
	/**
	 * Creates a new LongRollingCounter
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 * @param readTimeout The read lock timeout in ms.
	 * @param writeTimeout The write lock timeout in ms.
	 */
	public LongRollingCounter(String name, int capacity, long readTimeout, long writeTimeout) {
		super(name, capacity, readTimeout, writeTimeout);
		tLong = new TLongArrayList(capacity);
	}
	
	/**
	 * Creates a new LongRollingCounter
	 * @param name The name of the counter
	 * @param capacity the capacity of the accumulator and maximum size.
	 * @param readTimeout The read lock timeout in ms.
	 * @param writeTimeout The write lock timeout in ms.
	 * @param registerGroup A register map to group counters
	 */
	public LongRollingCounter(String name, int capacity, long readTimeout, long writeTimeout, final Map<String, RollingCounter> registerGroup) {
		super(name, capacity, readTimeout, writeTimeout, registerGroup);
		tLong = new TLongArrayList(capacity);
	}
	
	/**
	 * Returns the type of the rolled items.
	 * @return the type of the rolled items.
	 */
	public Class<?> getCounterType() {
		return long.class;
	}

	
	
	/**
	 * Adds a new "most recent" long value to the counter
	 * @param value the long value to add
	 */
	@JMXOperation(name="put", description="Puts a new value at the head of the counter")
	public void put(@JMXParameter(name="value", description="The long value to put into the counter") long value) {
		boolean acquiredLock = false;
		try {
			acquiredLock = writeLock.tryLock(writeTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				tLong.insert(0, value);
				int newSize = currentSize.incrementAndGet();
				if(newSize>size) {
					tLong.remove(newSize-1);
					currentSize.decrementAndGet();
				}
			}
		} catch (Exception e) {
			LOG.error("long value put error [" + value + "]", e);
		} finally {
			if(acquiredLock) try { writeLock.unlock(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Renders the contents of the counter.
	 * @return A string representation of a long array
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
	public long[] getContents() {
		boolean acquiredLock = false;
		try {
			acquiredLock = readLock.tryLock(readTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				return tLong.toArray();
			}
			throw new TimeoutException("Failed to acquire readLock within [" + readTimeout + "] ms.");
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
	public long[] getContents(@JMXParameter(name="lastN", description="the number of items to return, starting with the most recent") int lastN) {		
		if(currentSize.get()<1) {
			return new long[]{};
		}		
		long[] values = getContents();
		int size = values.length;
		int rangeSize = lastN>size ? size : lastN;
		long[] subrange = new long[rangeSize];
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
				tLong.reset();
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
	public long getRangeMax() {
		boolean acquiredLock = false;
		try {
			acquiredLock = readLock.tryLock(readTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				if(tLong.isEmpty()) return 0;
				return tLong.max();
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
	public long getRangeMin() {
		boolean acquiredLock = false;
		try {
			acquiredLock = readLock.tryLock(readTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				if(tLong.isEmpty()) return 0;
				return tLong.min();
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
	public long getLastValue() {
		if(currentSize.get()<1) {
			return -1;
		} else {
			long[] values = getContents();
			return values.length<1 ? -1 : values[0];
		}
	}
	
	/**
	 * Returns the total of all the values in the history counter
	 * @return the total 
	 */
	public double getTotal() {
		if(currentSize.get()<1) {
			return 0;
		} else {
			long[] values = getContents();
			double total = 0L;
			for(long value: values) {
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
	public double getTotal(int lastN) {
		if(currentSize.get()<1) {
			return 0;
		} else {
			long[] values = getContents(lastN);
			double total = 0L;
			for(long value: values) {
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
	public long getAverage() {
		if(currentSize.get()<1) {
			return 0;
		} else {
			long[] values = getContents();
			double total = 0L;
			for(long value: values) {
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
	public long getAverage(@JMXParameter(name="lastN", description="The number of items to calculate an average for, starting with the most recent") int lastN) {
		if(currentSize.get()<1) {
			return 0;
		} else {
			long[] values = getContents(lastN);
			double total = 0L;
			for(long value: values) {
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
	public static long avg(double count, double total) {
		if(count<1 || total < 1) return 0L;
		double avg = total/count;
		return (long)avg;
	}
	
	
	@JMXManagedObject(annotated=true, declared=true)
	public static class LRC extends ManagedObjectDynamicMBean {
		protected final LongRollingCounter usedHeap = new LongRollingCounter("Used Heap Memory", 10);
		protected final LongRollingCounter usedComm = new LongRollingCounter("Committed Heap Memory", 10);
		protected final LongDeltaRollingCounter threadCpu = new LongDeltaRollingCounter("This thread's CPU utilization rate", 10);
		protected final IntegerRollingCounter threadCount = new IntegerRollingCounter("The number of threads", 10);
		
		public LRC() {
			this.reflectObject(this);
			ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
			usedHeap.put(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
			usedComm.put(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted());
			threadCpu.put(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime());
			threadCpu.put(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime());
			threadCount.put(ManagementFactory.getThreadMXBean().getAllThreadIds().length);
		}
		
		/**
		 * @return the usedHeap
		 */
		@JMXAttribute(name="ThreadCount", description="The number of threads", mutability=AttributeMutabilityOption.READ_ONLY)
		public IntegerRollingCounter getThreadCount() {
			return threadCount;
		}
		

		/**
		 * @return the usedHeap
		 */
		@JMXAttribute(name="UsedHeap", description="A rolling counter of used heap space", mutability=AttributeMutabilityOption.READ_ONLY)
		public LongRollingCounter getUsedHeap() {
			return usedHeap;
		}

		/**
		 * @return the usedComm
		 */
		@JMXAttribute(name="CommittedHeap", description="A rolling counter of committed heap space", mutability=AttributeMutabilityOption.READ_ONLY)
		public LongRollingCounter getCommittedHeap() {
			return usedComm;
		}

		/**
		 * @return the threadCpu
		 */
		@JMXAttribute(name="ThreadCpu", description="This thread's CPU utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
		public LongDeltaRollingCounter getThreadCpu() {
			return threadCpu;
		}
		
	}
	
	@JMXManagedObject(annotated=true, declared=true)
	public static class LRCMap extends ManagedObjectDynamicMBean {
		protected final LongRollingCounter usedHeap = new LongRollingCounter("Used Heap Memory", 10);
		protected final LongRollingCounter usedComm = new LongRollingCounter("Committed Heap Memory", 10);
		protected final LongDeltaRollingCounter threadCpu = new LongDeltaRollingCounter("This thread's CPU utilization rate", 10);
		protected final ConcurrentTabularData counters = new ConcurrentTabularData(RollingCounter.class);
		public LRCMap() {
			this.reflectObject(this);
			ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
			usedHeap.put(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
			usedComm.put(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted());
			threadCpu.put(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime());
			threadCpu.put(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime());
			counters.put("usedHeap", usedHeap);
			counters.put("usedComm", usedComm);
			counters.put("threadCpu", threadCpu);
			log("LRCMap TabType:" + OpenTypeManager.getInstance().getTabularType(RollingCounter.class));
		}



		/**
		 * @return the counters
		 */
		@JMXAttribute(name="PerformanceCounters", description="A map of performance counters", mutability=AttributeMutabilityOption.READ_ONLY)
		public ConcurrentTabularData getCounters() {
			return counters;
		}

//		public Map<String, CompositeData> getCounters() {
//			return Collections.unmodifiableMap(counters);
//		}
		
	}
	

	public static void main(String[] args) {
		BasicConfigurator.configure();
		LOG.setLevel(Level.INFO);
		LOG.info("LongRollingCounter Test");
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(new LRC(), new ObjectName("org.helios.jmx:service=LRC"));
			
			ManagementFactory.getPlatformMBeanServer().registerMBean(new LRCMap(), new ObjectName("org.helios.jmx:service=LRCMap"));
			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
