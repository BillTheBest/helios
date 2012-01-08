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
 * <p>Title: InvocationObserver</p>
 * <p>Description: Defines an invocation wrapper that will collect different levels of instrumentation </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.instrumentation.InvocationObserver</code></p>
 */

public interface InvocationObserver {
	/**
	 * Starts an invocation observation.
	 */
	public void start();
	/**
	 * Stops an invocation observation and tabulates the results.
	 */
	public void stop();
	
	/**
	 * Initializes rolling counter performance counters 
	 */
	public void initPerfCounters();
	
	/**
	 * Called if the invocation throws.
	 */
	public void exception();
	
	/**
	 * Resets the invocation observer's counters
	 */
	public void reset();
	
	/**
	 * Returns the UTC timestamp of the last reset
	 * @return a UTC timestamp
	 */
	public long getLastResetTime();
	
	/**
	 * Returns the date of the last reset
	 * @return a date
	 */
	public Date getLastResetDate();
	
	/**
	 * Returns the name of the instrumentation profile
	 * @return the name of the instrumentation profile
	 */
	public String getInstrumentationProfile();
	
}
