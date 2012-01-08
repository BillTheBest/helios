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
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: PerfInvocationObserver</p>
 * <p>Description: Measures {@link InstrumentationProfile#BASIC} plus elapsed times and invocation concurrency</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.instrumentation.PerfInvocationObserver</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class PerfInvocationObserver extends BasicInvocationObserver {
	/** Invocation time counter */
	protected final AtomicLong invocationTime = new AtomicLong(-1L);
	/** Concurrency counter */
	protected final AtomicLong concurrency = new AtomicLong(0L);
	/** The invocation start time */
	protected final ThreadLocal<long[]> startTime = new ThreadLocal<long[]>();
	
	/**
	 * Creates a new PerfInvocationObserver
	 * @param profile The instrumentation profile
	 * @param name the name of the observer
	 * @param size ignored
	 */
	public PerfInvocationObserver(InstrumentationProfile profile, String name, int size) {
		super(profile, name, size);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#start()
	 */
	@Override
	public void start() {
		startTime.set(new long[]{System.nanoTime()});
		concurrency.incrementAndGet();
		super.start();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#stop()
	 */
	@Override
	public void stop() {		
		long[] start = startTime.get();
		if(start!=null && start.length==1) {
			invocationTime.addAndGet(System.nanoTime()-start[0]);
		}
		startTime.remove();
		concurrency.decrementAndGet();
		super.stop();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#exception()
	 */
	@Override
	public void exception() {
		concurrency.decrementAndGet();
		startTime.remove();
		super.exception();
	}
	
	
	
	/**
	 * Returns the last elapsed time (ns)
	 * @return the last elapsed time (ns)
	 */
	@JMXAttribute(name="LastElapsedTime", description="The last elapsed time (ns)", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastElapsedTime() {
		return invocationTime.get();
	}
	
	/**
	 * Returns the current concurrency
	 * @return the current concurrency
	 */
	@JMXAttribute(name="Concurrency", description="The current concurrency", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getConcurrency() {
		return concurrency.get();
	}
	
	

}
