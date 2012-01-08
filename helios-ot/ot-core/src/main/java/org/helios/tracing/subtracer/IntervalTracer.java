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
package org.helios.tracing.subtracer;

import org.helios.tracing.ITracer;
import org.helios.tracing.interval.accumulator.AccumulatorManager;
import org.helios.tracing.trace.Trace.Builder;

/**
 * <p>Title: IntervalTracer</p>
 * <p>Description: SubTracer that supresses trace propagation and instead submits all traces to the AccumulatorManager.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.subtracer.IntervalTracer</code></p>
 */

public class IntervalTracer extends DelegatingTracer implements IIntervalTracer {
	/** The bit mask to flag metricIds with that will be used to filter interval flushes. */
	protected final long accumulatorBitMask;
	
	/**
	 * Creates a new IntervalTracer 
	 * @param vtracer The parent tracer
	 * @param accumulatorBitMask The bit mask to flag metricIds with that will be used to filter interval flushes.
	 */
	public IntervalTracer(ITracer vtracer, long accumulatorBitMask) {
		super(vtracer, "IntervalTracer->" + vtracer.getTracerName());	
		this.accumulatorBitMask = accumulatorBitMask;
	}

	/**
	 * Builds the trace, sends it to the AccumulatorManager and returns null.
	 * @param builder The trace builder
	 * @return null.
	 */
	@Override
	public Builder format(Builder builder) {
		AccumulatorManager.getInstance().get().processTrace(builder.build());
		return null;
	}

	/**
	 * Returns the bit mask to flag metricIds with that will be used to filter interval flushes.
	 * @return the accumulatorBitMask
	 */
	public long getAccumulatorBitMask() {
		return accumulatorBitMask;
	}

}
