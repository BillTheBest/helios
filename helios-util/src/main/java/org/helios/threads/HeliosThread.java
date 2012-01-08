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

/**
 * <p>Title: HeliosThread</p>
 * <p>Description: Extended utility thread class.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.threads.HeliosThread</code></p>
 */

public class HeliosThread extends Thread {

	/**
	 * Creates a new HeliosThread
	 */
	public HeliosThread() {
	}

	/**
	 * Creates a new HeliosThread 
	 * @param runnable The runnable the thread will run
	 */
	public HeliosThread(Runnable runnable) {
		super(runnable);
	}

	/**
	 * Creates a new HeliosThread
	 * @param threadName The name of the thread
	 */
	public HeliosThread(String threadName) {
		super(threadName);
	}

	/**
	 * Creates a new HeliosThread 
	 * @param threadGroup The thread group this thread will belong to 
	 * @param runnable The runnable the thread will run
	 */
	public HeliosThread(ThreadGroup threadGroup, Runnable runnable) {
		super(threadGroup, runnable);
	}

	/**
	 * Creates a new HeliosThread
	 * @param threadGroup The thread group this thread will belong to 
	 * @param threadName The name of the thread
	 */
	public HeliosThread(ThreadGroup threadGroup, String threadName) {
		super(threadGroup, threadName);
	}

	/**
	 * Creates a new HeliosThread
	 * @param runnable The runnable the thread will run
	 * @param threadName The name of the thread
	 */
	public HeliosThread(Runnable runnable, String threadName) {
		super(runnable, threadName);
	}

	/**
	 * Creates a new HeliosThread
	 * @param threadGroup The thread group this thread will belong to
	 * @param runnable The runnable the thread will run
	 * @param threadName The name of the thread
	 */
	public HeliosThread(ThreadGroup threadGroup, Runnable runnable, String threadName) {
		super(threadGroup, runnable, threadName);
	}

	/**
	 * Creates a new HeliosThread
	 * @param threadGroup The thread group this thread will belong to
	 * @param runnable The runnable the thread will run
	 * @param threadName The name of the thread
	 * @param stackSize The thread's stack size
	 */
	public HeliosThread(ThreadGroup threadGroup, Runnable runnable, String threadName, long stackSize) {
		super(threadGroup, runnable, threadName, stackSize);
	}

}
