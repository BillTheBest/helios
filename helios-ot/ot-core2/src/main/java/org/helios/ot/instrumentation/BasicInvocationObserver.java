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

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: BasicInvocationObserver</p>
 * <p>Description: Measures invocation and exception counts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.instrumentation.BasicInvocationObserver</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class BasicInvocationObserver implements InvocationObserver {
	/** Invocation count counter */
	protected final AtomicLong invocationCount = new AtomicLong(0L);
	/** Exception count counter */
	protected final AtomicLong exceptionCount = new AtomicLong(0L);
	/** Last reset time */
	protected final AtomicLong resetTime = new AtomicLong(System.currentTimeMillis());
	/** The Instrumentation Profile */
	protected final InstrumentationProfile profile;
	
	/** The name of this observer */
	protected final String name;
	/** The size of the rolling counters */
	protected final int size;

	
	/**
	 * Creates a new BasicInvocationObserver
	 * @param profile The instrumentation profile
	 * @param name the name of the observer
	 * @param size ignored.
	 */
	public BasicInvocationObserver(InstrumentationProfile profile, String name, int size) {
		this.name = name;
		this.size = size;
		this.profile = profile;
	}
	
	/**
	 * No op
	 */
	public void initPerfCounters() {}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#start()
	 */
	@Override
	public void start() {
		invocationCount.incrementAndGet();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#stop()
	 */
	@Override
	public void stop() {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#exception()
	 */
	@Override
	public void exception() {
		exceptionCount.incrementAndGet();
	}
	
	/**
	 * Returns the invocation count
	 * @return the invocation count
	 */
	@JMXAttribute(name="InvocationCount", description="The invocation count", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getInvocationCount() {
		return invocationCount.get();
	}
	
	/**
	 * Returns the exception count
	 * @return the exception count
	 */
	@JMXAttribute(name="ExceptionCount", description="The exception count", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getExceptionCount() {
		return exceptionCount.get();
	}
	
	/**
	 * Returns the UTC timestamp of the last reset
	 * @return a UTC timestamp
	 */
	@JMXAttribute(name="LastResetTime", description="The UTC timestamp of the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastResetTime() {
		return resetTime.get();
	}
	
	/**
	 * Returns the date of the last reset
	 * @return a date
	 */
	@JMXAttribute(name="LastResetDate", description="The date of the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getLastResetDate() {
		return new Date(resetTime.get());
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#reset()
	 */
	@Override
	@JMXOperation(name="reset", description="Resets the observer's metrics")
	public void reset() {
		exceptionCount.set(0L);
		invocationCount.set(0L);
		resetTime.set(System.currentTimeMillis());
	}
	
	/**
	 * Returns the name of the instrumentation profile
	 * @return the name of the instrumentation profile
	 */
	@JMXAttribute(name="Profile", description="The instrumentation profile name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getInstrumentationProfile() {
		return profile.name();
	}


	

}
