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

import java.util.concurrent.atomic.AtomicLong;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.LongRollingCounter;

/**
 * <p>Title: PerfRollingInvocationObserver</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.instrumentation.PerfRollingInvocationObserver</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class PerfRollingInvocationObserver extends BasicRollingInvocationObserver {
	/** The concurrency rolling counter */
	protected final LongRollingCounter concurrencyCounter;
	/** The elapsed time rolling counter */
	protected final LongRollingCounter elapsedTimeCounter;
	/** Concurrency counter */
	protected final AtomicLong concurrency = new AtomicLong(0L);	
	/** The invocation start time */
	protected final ThreadLocal<long[]> startTime = new ThreadLocal<long[]>();
	

	/**
	 * Creates a new PerfRollingInvocationObserver
	 * @param profile The instrumentation profile
	 * @param name the name of the observer
	 * @param size the size of the rolling counters
	 */
	public PerfRollingInvocationObserver(InstrumentationProfile profile, String name, int size) {
		super(profile, name, size);
		concurrencyCounter = new LongRollingCounter("Concurrency", size, registerGroup);
		elapsedTimeCounter = new LongRollingCounter("ElapsedTime", size, registerGroup);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.BasicRollingInvocationObserver#start()
	 */
	@Override
	public void start() {
		startTime.set(new long[]{System.nanoTime()});
		concurrencyCounter.put(concurrency.incrementAndGet());
		super.start();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.BasicInvocationObserver#stop()
	 */
	@Override
	public void stop() {		
		long[] start = startTime.get();
		if(start!=null && start.length==1) {
			elapsedTimeCounter.put(System.nanoTime()-start[0]);			
		}
		startTime.remove();
		concurrency.decrementAndGet();
		super.stop();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.BasicRollingInvocationObserver#exception()
	 */
	@Override
	public void exception() {
		concurrency.decrementAndGet();
		startTime.remove();
		super.exception();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.BasicRollingInvocationObserver#reset()
	 */
	@Override
	@JMXOperation(name="reset", description="Resets the observer's metrics")
	public void reset() {
		concurrencyCounter.reset();
		elapsedTimeCounter.reset();
		super.reset();
	}
	
	/**
	 * Returns the rolling concurrency counter
	 * @return the rolling concurrenct counter
	 */
	@JMXAttribute(name="ConcurrencyCounter", description="The rolling concurrency counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public LongRollingCounter getConcurrencyCounter() {
		return concurrencyCounter;
	}
	
	/**
	 * Returns the rolling elapsed time counter
	 * @return the rolling elapsed time
	 */
	@JMXAttribute(name="ElapsedTimeCounter", description="The rolling elapsed time counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public LongRollingCounter getElapsedTimeCounter() {
		return elapsedTimeCounter;
	}	
	

}
