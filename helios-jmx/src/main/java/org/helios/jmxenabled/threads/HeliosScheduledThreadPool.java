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
package org.helios.jmxenabled.threads;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>Title: HeliosScheduledThreadPool</p>
 * <p>Description: A ScheduledThreadPoolExecutor extension with some minor tweaks to enable better capturing of events and metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.HeliosScheduledThreadPool</code></p>
 */

public class HeliosScheduledThreadPool extends HeliosThreadPool {

	/**
	 * Creates a new 
	 * @param threadPool
	 * @param description
	 */
	public HeliosScheduledThreadPool(ThreadPoolExecutor threadPool,
			String description) {
		super(threadPool, description);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new 
	 * @param threadPool
	 */
	public HeliosScheduledThreadPool(ThreadPoolExecutor threadPool) {
		super(threadPool);
		// TODO Auto-generated constructor stub
	}






}
