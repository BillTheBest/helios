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
package org.helios.ot.generic.updateable;

import org.helios.ot.generic.IGenericMetric;


/**
 * <p>Title: IUpdateableGenericMetric</p>
 * <p>Description: Defines a generic metric that allows the update of some fields.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.generic.updateable.IUpdateableGenericMetric</code></p>
 */
public interface IUpdateableGenericMetric extends IGenericMetric {
	/**
	 * Sets the metric interval start time
	 * @param intervalStart the intervalStart to set
	 */
	public void setStartTime(long intervalStart);

	/**
	 * Sets the metric interval end time
	 * @param intervalEnd the intervalEnd to set
	 */
	public void setEndTime(long intervalEnd);

	/**
	 * Sets the average value
	 * @param avg the avg to set
	 */
	public void setAvg(long avg);

	/**
	 * Sets the maximum value
	 * @param max the max to set
	 */
	public void setMax(long max);

	/**
	 * Sets the minimum value
	 * @param min the min to set
	 */
	public void setMin(long min);

	/**
	 * Sets the metric sample count
	 * @param count the count to set
	 */
	public void setCount(long count);

}
