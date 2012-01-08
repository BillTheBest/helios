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


/**
 * <p>Title: IIntervalTracer</p>
 * <p>Description: Creates an interval wrapper for sumitting all traces to the AccumulatorManager in order to interval aggregate all creates traces.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.subtracer.IIntervalTracer</code></p>
 */

public interface IIntervalTracer extends ISubTracer {
	/**
	 * Submits an array of interval traces to the tracing endpoint
	 * @param intervalTraces an array of interval traces to submit
	 */
	//public void submitIntervalTraces(IIntervalTrace...intervalTraces);
	
	/**
	 * Returns the bit mask to flag metricIds with that will be used to filter interval flushes.
	 * @return the accumulatorBitMask
	 */
	public long getAccumulatorBitMask();	
	
}
