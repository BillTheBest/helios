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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.helpers.StringHelper;

/**
 * <p>Title: HeliosThread</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.HeliosThread</code></p>
 */

public class HeliosThread extends Thread implements Comparable<Thread> {
	/** The actual task runnable being run by this thread, or null if it is idle */
	protected Runnable target;
	/** A counter this thread will increment when it terminates */
	protected final AtomicLong termCounter;
	/** Instance logger */
	protected final Logger log;
	

	/**
	 * Creates a new HeliosThread
	 * @param group The thread group the thread belongs to
	 * @param target The runnable target the thread will run
	 * @param name The thread name
	 * @param stackSize The thread's stack size
	 * @param termCounter The shared thread termination counter
	 */
	public HeliosThread(ThreadGroup group, Runnable target, String name, long stackSize, final AtomicLong termCounter) {
		super(group, target, name, stackSize);
		this.termCounter = termCounter;
		log = Logger.getLogger(getClass().getName() + "." + name);
		if(log.isDebugEnabled()) log.debug("Created HeliosThread[" + name + "] with Runnable of type[" + target.getClass().getName() + "]");
	}
	
	/**
	 * Calls the super interrupt.
	 */
	@Override
	public void interrupt() {
//		String stackTrace = StringHelper.formatStackTrace(Thread.currentThread());
//		log.info("Helios Thread [" + getName() + "] interrupted by Thread [" + Thread.currentThread().toString() + "] with stack:\n" + stackTrace);
		super.interrupt();
	}
	
	/**
	 * Runs the thread's runnable and increments the termCounter when it exits.
	 */
	@Override
	public void run() {
		try {
			super.run();
		} finally {
			termCounter.incrementAndGet();
		}
	}
	
	/**
	 * The actual task runnable being run by this thread, or null if it is idle
	 * @return the runnable being run by this thread, or null if it is idle
	 */
	public Runnable getActualRunnable() {
		return target;
	}
	
	/**
	 * Called by the thread pool executor's <code>beforeExecute</code> to specify what the actual task is.
	 * @param target The actual task being run by this thread.
	 */
	void setActualRunnable(Runnable target) {
		this.target = target;
	}

	/**
	 * Reverse integer Comparator to sort null and then {@link java.lang.Thread.State#TERMINATED} threads high.
	 * Compares two threads for order according to the ordinal of the Thread.State. Returns a positive integer, zero, or a negative integer as this thread is less than, equal to, or greater than the passed thread.
	 * @param t The thread to compare this thread to
	 * @return an integer which is negative, zero or positive
	 */
	@Override
	public int compareTo(Thread t) {
		if(t==null) return 1;
		return this.getState().ordinal() - t.getState().ordinal();
	}

}
