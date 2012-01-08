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
package org.helios.threads;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: DroppedThreadPoolTaskCounter</p>
 * <p>Description: A simple RejectedExecutionHandler that keeps a count of dropped tasks.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.util.DroppedThreadPoolTaskCounter</code></p>
 */

public class DroppedThreadPoolTaskCounter implements RejectedExecutionHandler {
	/** The count of dropped tasks */
	protected final AtomicLong drops;

	/**
	 * Creates a new DroppedThreadPoolTaskCounter
	 * @param drops The  drop counter to use
	 */
	public DroppedThreadPoolTaskCounter(final AtomicLong drops) {		
		this.drops = drops;
	}
	
	/**
	 * Creates a new DroppedThreadPoolTaskCounter
	 */
	public DroppedThreadPoolTaskCounter() {		
		this.drops = new AtomicLong(0L);
	}
	

	/**
	 * Method that may be invoked by a ThreadPoolExecutor when execute cannot accept a task.
	 * @param runnable the runnable task requested to be executed
	 * @param executor the executor attempting to execute this task 
	 */
	@Override
	public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
		if(this.drops!=null) this.drops.incrementAndGet();

	}

	/**
	 * Returns the count of dropped tasks
	 * @return the count of dropped tasks
	 */
	public long getDrops() {
		return drops.get();
	}
	
	/**
	 * Returns the dropped task counter
	 * @return the dropped task counter
	 */
	public AtomicLong getDropsCounter() {
		return drops;
	}
	
	/**
	 * Resets the dropped task counter.
	 */
	public void resetCounter() {
		drops.set(0L);
	}
	

}
