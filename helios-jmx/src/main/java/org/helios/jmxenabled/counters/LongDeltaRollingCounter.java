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

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.opentypes.annotations.XCompositeAttribute;
import org.helios.jmx.opentypes.annotations.XCompositeType;

/**
 * <p>Title: LongDeltaRollingCounter</p>
 * <p>Description: A rolling fixed length long delta value accumulator.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.counters.LongDeltaRollingCounter</code></p>
 * @TODO: Detect long value overflow.
 */
@XCompositeType(description="Rolling long rate accumulator")
public class LongDeltaRollingCounter extends LongRollingCounter {
	
	/** State for the first submitted value to provide a baseline. */
	protected AtomicReference<Long> deltaState = new AtomicReference<Long>(null); 

	/**
	 * Creates a new LongDeltaRollingCounter 
	 * @param name The name of the counter
	 * @param capacity The length of the rolling window
	 */
	public LongDeltaRollingCounter(String name, int capacity) {
		super(name, capacity);
	}
	
	/**
	 * Creates a new LongDeltaRollingCounter 
	 * @param name The name of the counter
	 * @param capacity The length of the rolling window
	 * @param registerGroup A register map to group counters
	 */
	public LongDeltaRollingCounter(String name, int capacity, final Map<String, RollingCounter> registerGroup) {
		super(name, capacity, registerGroup);
	}
	

	/**
	 * Creates a new LongDeltaRollingCounter with a read and write timeout
	 * @param name The name of the counter
	 * @param capacity The length of the rolling window
	 * @param readTimeout The counter read timeout in ms.
	 * @param writeTimeout The counter write timeout in ms.
	 */
	public LongDeltaRollingCounter(String name, int capacity, long readTimeout, long writeTimeout) {
		super(name, capacity, readTimeout, writeTimeout);
	}
	
	/**
	 * Creates a new LongDeltaRollingCounter with a read and write timeout
	 * @param name The name of the counter
	 * @param capacity The length of the rolling window
	 * @param readTimeout The counter read timeout in ms.
	 * @param writeTimeout The counter write timeout in ms.
	 * @param registerGroup A register map to group counters
	 */
	public LongDeltaRollingCounter(String name, int capacity, long readTimeout, long writeTimeout, final Map<String, RollingCounter> registerGroup) {
		super(name, capacity, readTimeout, writeTimeout, registerGroup);
	}
	
	
	/**
	 * Returns the value of the delta state
	 * @return the value of the delta state
	 */
	@JMXAttribute(name="{f:name}DeltaState", description="Returns the value of the delta state", mutability=AttributeMutabilityOption.READ_ONLY)
	@XCompositeAttribute
	public Long getDeltaState() {
		return deltaState.get();
	}
	
	/**
	 * Adds a new "most recent" long value to the counter where the value is a delta of the prior value.
	 * If no prior value exists, the current value is held in state until the next value is submitted.
	 * @param value the long value to add
	 */
	@Override
	@JMXOperation(name="put", description="Puts a new value at the head of the counter")
	public void put(@JMXParameter(name="value", description="The long value to put into the counter")long value) {		
		boolean acquiredLock = false;
		try {
			acquiredLock = writeLock.tryLock(writeTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {							
				Long baseline = deltaState.getAndSet(value);
				if(currentSize.get()<1) {					
					if(baseline==null) {						
						return;
					}
					value = value-baseline.longValue();					
				} else {
					value = value-baseline.longValue();
				}				
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
	 * Resets the rolling counter  
	 */
	@Override
	@JMXOperation(name="reset", description="Resets the rolling counter")
	public void reset() {
		boolean acquiredLock = false;
		try {
			acquiredLock = writeLock.tryLock(writeTimeout, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				super.reset();
				deltaState.set(null);
			} else {
				throw new TimeoutException("Failed to acquire writeLock within [" + writeTimeout + "] ms.");
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to reset the counter", e);
		} finally {
			if(acquiredLock) try { writeLock.unlock(); } catch (Exception e) {}
		}			

		
	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		LOG.setLevel(Level.INFO);
		LOG.info("LongDeltaRollingCounter Test");
		LongRollingCounter ctr = new LongDeltaRollingCounter("SnafuDelta", 10);
		ObjectName on = JMXHelper.objectName("org.helios.test:type=DeltaRollingCounter");
		ManagedObjectDynamicMBean modb = new ManagedObjectDynamicMBean("Delta Rolling Counter", ctr); 
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), on, modb);
		LOG.info("Empty Contents:[" + Arrays.toString(ctr.getContents()) + "]");
		AtomicLong seed = new AtomicLong(1);
		Random random = new Random(System.nanoTime());
		//LOG.info("Size:" + ctr.getSize() + " Last Value:" + ctr.getLastValue());
		for(int i = 0; i < 2000; i++) {
			long seedValue = seed.get();
			ctr.put(Math.abs(seedValue));
			LOG.info("Size:" + ctr.getSize() + " Last Value:" + ctr.getLastValue() + ", Average:" + ctr.getAverage());
			seed.set(seedValue+random.nextInt(10));
			try { Thread.sleep(1000); } catch (Exception e) {}
		}
		LOG.info("==============================");
		LOG.info("Last 5 Values:" + Arrays.toString(ctr.getContents(5)));
	}

}
