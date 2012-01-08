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
 * <p>Title: HeliosThreadPoolExecutorImpl</p>
 * <p>Description: ThreadPoolExecutor extension to add task monitoring and terminate events.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.HeliosThreadPoolExecutorImpl</code></p>
 */

public class HeliosThreadPoolExecutorImpl extends ThreadPoolExecutor {
	/** The amount of time (in ms.) to wait for the pool to shutdown */
	protected long terminationWaitTime = -1L;
	
	/**
	 * Sets the termination time
	 * @param terminationWaitTime the amount of time (in ms.) to wait for the pool to shutdown
	 */
	public void setTerminationTime(long terminationWaitTime) {
		if(terminationWaitTime<1L) throw new IllegalArgumentException("Invalid value for termination time [" + terminationWaitTime + "]", new Throwable());
		this.terminationWaitTime = terminationWaitTime;
	}

	/**
	 * Returns the termination time (in ms.)
	 * @return The amount of time (in ms.) to wait for the pool to shutdown
	 */
	public long getTerminationTime() {
		return terminationWaitTime;
	}

	/**
	 * Creates a new HeliosThreadPoolExecutorImpl
	 * @param corePoolSize The thread pool's core pool size
	 * @param maximumPoolSize The thread pool's maximum pool size
	 * @param keepAliveTime The keep alive time for idle threads above the core number
	 * @param unit The unit of time for the keep alive time
	 * @param workQueue The work queue for submitting tasks to be run
	 * @param threadFactory The thread factory the pool uses to create worker threads
	 * @param handler The reject task handler used when the workQueue is full
	 * @param terminationWaitTime The period of time the pool is allowed to shutdown cleanly (ms)
	 * @param metricsEnabled If true, the pool will instrument executed tasks
	 */
	public HeliosThreadPoolExecutorImpl(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
			RejectedExecutionHandler handler, long terminationWaitTime, boolean metricsEnabled) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory, handler);
		this.terminationWaitTime = terminationWaitTime;
	}
	
	/**
	 * Issues a lazy shutdown. If the termination time has been set, the op will
	 * wait that amount of time, and if that time elapses with the pool still running,
	 * the pool wil be forcibly shutdown.
	 */
	@Override
	public void shutdown() {
		super.shutdown();
		if(terminationWaitTime>0) {
			try {
				if(!super.awaitTermination(terminationWaitTime, TimeUnit.MILLISECONDS)) {
					shutdownNow();
				}
			} catch (InterruptedException e) {
				if(!isShutdown() && !isTerminating()) {
					shutdownNow();
				}
			}
		}
	}
	
	public HeliosThreadGroup getThreadGroup() {
		return (HeliosThreadGroup) ((HeliosThreadFactory)this.getThreadFactory()).getThreadGroup();
	}

}
