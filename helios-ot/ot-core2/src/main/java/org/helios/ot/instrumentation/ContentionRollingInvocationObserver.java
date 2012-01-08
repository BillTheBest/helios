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
package org.helios.ot.instrumentation;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.LongRollingCounter;

/**
 * <p>Title: ContentionRollingInvocationObserver</p>
 * <p>Description: Invocation observer that measures {@link InstrumentationProfile#PERF} plus invocation waits and blocks with rolling counters </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.instrumentation.ContentionRollingInvocationObserver</code></p>
 */

public class ContentionRollingInvocationObserver extends PerfRollingInvocationObserver {
	/** The wait count rolling counter */
	protected final LongRollingCounter waitCountCounter;
	/** The block count rolling counter */
	protected final LongRollingCounter blockCountCounter;
	/** The wait time rolling counter */
	protected final LongRollingCounter waitTimeCounter;
	/** The block time rolling counter */
	protected final LongRollingCounter blockTimeCounter;
	
	
	/** The invocation start waitTime */
	protected final ThreadLocal<long[]> startWaitTime = new ThreadLocal<long[]>();
	/** The invocation start blockTime */
	protected final ThreadLocal<long[]> startBlockTime = new ThreadLocal<long[]>();
	/** The invocation start wait count*/
	protected final ThreadLocal<long[]> startWaitCount = new ThreadLocal<long[]>();
	/** The invocation start block count */
	protected final ThreadLocal<long[]> startBlockCount = new ThreadLocal<long[]>();
	
	/** Indicates if JVM thread contention is supported and enabled.Set on the ctor and setThreadContention. */
	protected boolean threadContentionEnabled = false;
	
	/** A reference to the JVM's ThreadMXBean */
	public static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	

	/**
	 * Creates a new ContentionRollingInvocationObserver
	 * @param profile The instrumentation profile
	 * @param name the name of the observer
	 * @param size ignored
	 */
	public ContentionRollingInvocationObserver(InstrumentationProfile profile, String name, int size) {
		super(profile, name, size);
		threadContentionEnabled = threadMXBean.isThreadContentionMonitoringSupported() && threadMXBean.isThreadContentionMonitoringEnabled();
		waitCountCounter = new LongRollingCounter("WaitCount", size, registerGroup);		
		blockCountCounter = new LongRollingCounter("BlockCount", size, registerGroup);
		waitTimeCounter = new LongRollingCounter("WaitTime", size, registerGroup);		
		blockTimeCounter = new LongRollingCounter("BlockTime", size, registerGroup);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.PerfRollingInvocationObserver#start()
	 */
	@Override
	public void start() {
		ThreadInfo ti = threadMXBean.getThreadInfo(Thread.currentThread().getId());
		startWaitCount.set(new long[]{ti.getWaitedCount()});
		startBlockCount.set(new long[]{ti.getBlockedCount()});
		if(threadContentionEnabled) {
			startWaitTime.set(new long[]{ti.getWaitedTime()});
			startBlockTime.set(new long[]{ti.getBlockedTime()});			
		}
		super.start();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.PerfRollingInvocationObserver#stop()
	 */
	@Override
	public void stop() {		
		long[] wc = startWaitCount.get();
		long[] bc = startBlockCount.get();		
		ThreadInfo ti = threadMXBean.getThreadInfo(Thread.currentThread().getId());
		if(wc!=null && wc.length==1) {
			waitCountCounter.put(ti.getWaitedCount()-wc[0]);
		}
		if(bc!=null && bc.length==1) {
			blockCountCounter.put(ti.getBlockedCount()-bc[0]);
		}
		if(threadContentionEnabled) {
			long[] wt = startWaitTime.get();
			long[] bt = startBlockTime.get();		
			if(wt!=null && wt.length==1) {
				waitTimeCounter.put(ti.getWaitedTime()-wt[0]);
			}
			if(bt!=null && bt.length==1) {
				blockTimeCounter.put(ti.getBlockedTime()-bt[0]);
			}			
		}
		startWaitCount.remove();
		startBlockCount.remove();
		startWaitTime.remove();
		startBlockTime.remove();
		super.stop();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.PerfInvocationObserver#exception()
	 */
	@Override
	public void exception() {
		startWaitCount.remove();
		startBlockCount.remove();
		startWaitTime.remove();
		startBlockTime.remove();		
		super.exception();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.PerfRollingInvocationObserver#reset()
	 */
	@Override
	@JMXOperation(name="reset", description="Resets the observer's metrics")
	public void reset() {
		super.reset();
		waitCountCounter.reset();
		blockCountCounter.reset();
		waitTimeCounter.reset();
		blockTimeCounter.reset();
		
	}
	
	/**
	 * Returns the rolling wait count counter
	 * @return the rolling wait count counter
	 */
	@JMXAttribute(name="WaitCount", description="The rolling wait count counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public LongRollingCounter getWaitCountCounter() {
		return waitCountCounter;
	}
	
	/**
	 * Returns the rolling block count counter
	 * @return the rolling block count counter
	 */
	@JMXAttribute(name="BlockCount", description="The rolling block count counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public LongRollingCounter getBlockCountCounter() {
		return blockCountCounter;
	}
	
	/**
	 * Returns the rolling wait time counter
	 * @return the rolling wait time counter
	 */
	@JMXAttribute(name="WaitTime", description="The rolling wait time counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public LongRollingCounter getWaitTimeCounter() {
		return waitTimeCounter;
	}
	
	/**
	 * Returns the rolling block time counter
	 * @return the rolling block time counter
	 */
	@JMXAttribute(name="BlockTime", description="The rolling block time counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public LongRollingCounter getBlockTimeCounter() {
		return blockTimeCounter;
	}
	
	/**
	 * Returns the enabled state of the JVM's thread contention monitoring
	 * @return true if enabled, false if not
	 */
	@JMXAttribute(name="ThreadContentionMonitoring", description="The enabled state of the JVM's thread contention monitoring", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getThreadContentionMonitoringEnabled() {
		return threadContentionEnabled;
	}
	
	/**
	 * Sets the enabled state of the JVM's thread contention monitoring.
	 * Ignored if thread contention monitoring is not supported
	 * @param enabled true to enable, false to disable
	 */
	public void setThreadContentionMonitoringEnabled(boolean enabled) {
		if(threadMXBean.isThreadContentionMonitoringSupported()) {
			threadMXBean.setThreadContentionMonitoringEnabled(enabled);
			threadContentionEnabled = enabled;
		}
	}
	
	
	

}
