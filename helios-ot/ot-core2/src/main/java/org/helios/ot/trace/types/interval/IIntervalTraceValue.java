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
package org.helios.ot.trace.types.interval;

import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: IIntervalTraceValue</p>
 * <p>Description: An abstraction of the value of the aggregate of all traces for one metric during one interval.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.interval.IIntervalTraceValue</code></p>
 */

public interface IIntervalTraceValue<T extends ITraceValue> extends ITraceValue  {
	/**
	 * Aggregates the passed ITraceValue into this interval trace value
	 * @param value The ITraceValue to apply
	 */
	public void apply(T value);

	/**
	 * Determines if any traces have een applied to this interval
	 * @return true if any traces have een applied to this interval, false otherwise
	 */
	public boolean isTouched();
	/**
	 * Clones the state of this interval trace value and then resets it's state for the next interval
	 * @param metricType The metric type of the owning trace passed so that the interval value 
	 * can execute the reset with the correct semantics.
	 * @return A clone of this object prior to reset.
	 */
	public IIntervalTraceValue<T> cloneReset(MetricType metricType);
	
	/**
	 * Returns the number of traces aggregated during the interval
	 * @return the number of traces aggregated during the interval
	 */
	public int getCount();	
	

}
