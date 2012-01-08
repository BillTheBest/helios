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

/**
 * <p>Title: InstrumentationProfile</p>
 * <p>Description: Enumerates the standard instrumentation profiles and provides an observer factory for each.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.instrumentation.InstrumentationProfile</code></p>
 */

public enum InstrumentationProfile  {
	/** No instrumentation */
	OFF(new OffInvocationObserverFactory()),
	/** Measures invocation and exception counts */
	BASIC(new InvocationObserverFactory(){public InvocationObserver getInvocationObserver(String name, int size) { return new BasicInvocationObserver(BASIC, name, size); }}),
	/** Measures invocation and exception counts with rolling counters */
	ROLLING_BASIC(new InvocationObserverFactory(){public InvocationObserver getInvocationObserver(String name, int size) { return new BasicRollingInvocationObserver(ROLLING_BASIC, name, size); }}),
	/** Measures {@link InstrumentationProfile#BASIC} plus elapsed times and invocation concurrency */
	PERF(new InvocationObserverFactory(){public InvocationObserver getInvocationObserver(String name, int size) { return new PerfInvocationObserver(PERF, name, size); }}),
	/** Measures {@link InstrumentationProfile#BASIC} plus elapsed times and invocation concurrency with rolling counters */
	ROLLING_PERF(new InvocationObserverFactory(){public InvocationObserver getInvocationObserver(String name, int size) { return new PerfRollingInvocationObserver(ROLLING_PERF, name, size); }}),
	/** Measures {@link InstrumentationProfile#PERF} plus invocation waits and blocks */
	CONTENTION(new InvocationObserverFactory(){public InvocationObserver getInvocationObserver(String name, int size) { return new ContentionInvocationObserver(PERF, name, size); }}),
	/** Measures {@link InstrumentationProfile#PERF} plus invocation waits and blocks with rolling counters */
	ROLLING_CONTENTION(new InvocationObserverFactory(){public InvocationObserver getInvocationObserver(String name, int size) { return new ContentionRollingInvocationObserver(PERF, name, size); }});
//	/** Measures {@link InstrumentationProfile#PERF} plus invocation CPU time */
//	PERF_CPU,
//	/** Measures {@link InstrumentationProfile#PERF} plus invocation CPU time with rolling counters */
//	ROLLING_PERF_CPU,
//	/** Measures {@link InstrumentationProfile#PERF_CPU} plus {@link InstrumentationProfile#CONTENTION} */
//	FULL,
//	/** Measures {@link InstrumentationProfile#PERF_CPU} plus {@link InstrumentationProfile#CONTENTION} with rolling counters */
//	ROLLING_FULL;
	
	/**
	 * Creates a new InstrumentationProfile
	 * @param observerFactory the observer factory
	 */
	private InstrumentationProfile(InvocationObserverFactory observerFactory) {
		this.observerFactory = observerFactory;
	}
	
	/** The observer factory for this profile */
	protected final InvocationObserverFactory observerFactory;
	

	/**
	 * Creates a new invocation observer for this profile
	 * @param name The name of the observer
	 * @param size the size of the rolling counters, if applicable
	 * @return a new invocation observer 
	 */
	public InvocationObserver getInvocationObserver(String name, int size) {
		InvocationObserver io = observerFactory.getInvocationObserver(name, size);
		io.initPerfCounters();
		return io;
	}
	
	
	
	/**
	 * <p>Title: InvocationObserverFactory</p>
	 * <p>Description: Defines a factory class for an {@link InvocationObserver} </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.instrumentation.InstrumentationProfile.InstrumentationProfileFactory</code></p>
	 */
	private static interface InvocationObserverFactory {
		
		/**
		 * Creates a new {@link InvocationObserver}
		 * @param name The name of the profile. Arbitrary name.
		 * @param size The size of the rolling counters, if applicable. Ignored if not applicable.
		 * @return a new {@link InvocationObserver}
		 */
		InvocationObserver getInvocationObserver(String name, int size);
	}
	
	private static class OffInvocationObserverFactory implements InvocationObserverFactory {

		/**
		 * {@inheritDoc}
		 * @see org.helios.ot.instrumentation.InstrumentationProfile.InvocationObserverFactory#getInvocationObserver(java.lang.String, int)
		 */
		@Override
		public InvocationObserver getInvocationObserver(String name, int size) {
			return new InvocationObserver(){
				protected final long TS = System.currentTimeMillis();
				protected final Date DT = new Date(TS);
				protected final InstrumentationProfile profile = OFF;
				/**
				 * {@inheritDoc}
				 * <p>No Op</p>
				 * @see org.helios.ot.instrumentation.InvocationObserver#start()
				 */
				@Override
				public void start() {
				}
				
				/**
				 * No op
				 */
				public void initPerfCounters() {}	
				

				/**
				 * {@inheritDoc}
				 * <p>No Op</p>
				 * @see org.helios.ot.instrumentation.InvocationObserver#stop()
				 */
				@Override
				public void stop() {
				}
				
				/**
				 * {@inheritDoc}
				 * <p>No Op</p>
				 * @see org.helios.ot.instrumentation.InvocationObserver#exception()
				 */
				@Override
				public void exception() {
					
				}
				
				/**
				 * {@inheritDoc}
				 * <p>No Op</p>
				 * @see org.helios.ot.instrumentation.InvocationObserver#reset()
				 */
				public void reset() {
					
				}

				/**
				 * {@inheritDoc}
				 * @see org.helios.ot.instrumentation.InvocationObserver#getLastResetDate()
				 */
				@Override
				public Date getLastResetDate() {
					return DT;
				}

				/**
				 * {@inheritDoc}
				 * @see org.helios.ot.instrumentation.InvocationObserver#getLastResetTime()
				 */
				@Override
				public long getLastResetTime() {
					return TS;
				}

				/**
				 * {@inheritDoc}
				 * @see org.helios.ot.instrumentation.InvocationObserver#getInstrumentationProfile()
				 */
				@Override
				public String getInstrumentationProfile() {
					return profile.name();
				}
				
			};
		}
		
	}
	

}

















