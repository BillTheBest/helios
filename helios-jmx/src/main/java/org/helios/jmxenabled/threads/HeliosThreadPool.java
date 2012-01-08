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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: HeliosThreadPool</p>
 * <p>Description: A ThreadPoolExecutor extension with some minor tweaks to enable better capturing of events and metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.HeliosThreadPool</code></p>
 */

public class HeliosThreadPool extends HeliosThreadPoolBase {

	/**
	 * Creates a new HeliosThreadPool
	 * @param threadPool The underlying ThreadPoolExecutor
	 */
	protected HeliosThreadPool(ThreadPoolExecutor threadPool) {
		this(threadPool, null);
	}

	/**
	 * Creates a new HeliosThreadPool 
	 * @param threadPool The underlying ThreadPoolExecutor
	 * @param description An arbitrary pool description
	 */
	public HeliosThreadPool(ThreadPoolExecutor threadPool, String description) {
		super(threadPool, description);
	}



}
