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
package org.helios.nativex.jmx.swap;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.IntegerRollingCounter;
import org.helios.jmxenabled.counters.LongDeltaRollingCounter;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.Swap;

/**
 * <p>Title: SwapSpaceService</p>
 * <p>Description: Native monitor for system swap space. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.swap.SwapSpaceService</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class SwapSpaceService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = -5524111977376467291L;
	/** The swap stats gatherer */
	protected final Swap swap;
	/** Free Swap KBytes Counter */
	protected final LongRollingCounter freeCounter = new LongRollingCounter("FreeSwapKBytes", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Total Swap KBytes Counter */
	protected final LongRollingCounter totalCounter = new LongRollingCounter("TotalSwapKBytes", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Used Swap KBytes Counter */
	protected final LongRollingCounter usedCounter = new LongRollingCounter("UsedSwapKBytes", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Percent Used Swap KBytes Counter */
	protected final IntegerRollingCounter percentUsedCounter = new IntegerRollingCounter("UsedSwapKBytes", DEFAULT_ROLLING_SIZE, registerGroup);	
	/** Percent Free Swap KBytes Counter */
	protected final IntegerRollingCounter percentFreeCounter = new IntegerRollingCounter("UsedSwapKBytes", DEFAULT_ROLLING_SIZE, registerGroup);	
	
	/** Pages In Counter */
	protected final LongDeltaRollingCounter pagesInCounter = new LongDeltaRollingCounter("SwapPagesIn", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Pages Out Counter */
	protected final LongDeltaRollingCounter pagesOutCounter = new LongDeltaRollingCounter("SwapPagesOut", DEFAULT_ROLLING_SIZE, registerGroup);
	

	/**
	 * Creates a new SwapSpaceService
	 */
	public SwapSpaceService() {
		super();
		swap = HeliosSigar.getInstance().getSwap();		
		this.scheduleSampling();
		registerCounterMBean("service", "SwapService");
		initPerfCounters();
	}
	
	
	/**
	 * Gathers and increments swap stats
	 */
	@Override
	public void run() {
		try {
			swap.gather(sigar);
			long total = swap.getTotal();
			long free = swap.getFree();
			long used = swap.getUsed();
			
			freeCounter.put(free);			
			totalCounter.put(total);
			usedCounter.put(used);
			pagesInCounter.put(swap.getPageIn());
			pagesOutCounter.put(swap.getPageOut());
			percentUsedCounter.put(percent(total, used));
			percentFreeCounter.put(percent(total, free));
		} catch (Exception e) {
			log.error("Failed to gather on service [" + objectName + "]", e);
			throw new RuntimeException("Failed to gather on service [" + objectName + "]", e);
		}
	}

	/**
	 * Bootstraps this service 
	 */
	public static void boot() {
		new SwapSpaceService();
	}

	/**
	 * Returns the swap free KBytes
	 * @return the freeCounter
	 */
	@JMXAttribute(name="SwapFree", description="The free swap space (K)", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSwapFree() {
		return freeCounter.getLastValue();
	}


	/**
	 * Returns the swap total KBytes
	 * @return the totalCounter
	 */
	@JMXAttribute(name="SwapTotal", description="The total swap space (K)", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSwapTotal() {
		return totalCounter.getLastValue();
	}


	/**
	 * Returns the swap used KBytes
	 * @return the usedCounter
	 */
	@JMXAttribute(name="SwapUsed", description="The used swap space (K)", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSwapUsed() {
		return usedCounter.getLastValue();
	}


	/**
	 * Returns the swap pages In rate
	 * @return the pagesInCounter
	 */
	@JMXAttribute(name="SwapPageIn", description="The swap pages in rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getPagesInCounter() {
		return pagesInCounter.getLastValue();
	}


	/**
	 * Returns the swap pages Out rate
	 * @return the pagesOutCounter
	 */
	@JMXAttribute(name="SwapPageOut", description="The swap pages out rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getPagesOutCounter() {
		return pagesOutCounter.getLastValue();
	}

}
