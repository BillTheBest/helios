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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: HeliosThreadFactory</p>
 * <p>Description: A specialized thread factory to support HeliosThreadPoolExecutors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.HeliosThreadFactory</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class HeliosThreadFactory implements ThreadFactory {
	/** The thread group name */
	protected String groupName;
	/** The thread stack size */
	protected long stackSize = 0;
	/** The thread priorities */
	protected int priority;
	/** The uncaught exception handler */
	protected UncaughtExceptionHandler eHandler;
	/** true if created threads will be daemons */
	protected boolean daemon;
	/** The thread group that threads will be created in */
	protected final HeliosThreadGroup threadGroup;
	/** Counter of new threads */
	protected final AtomicLong newThreadCount = new AtomicLong(0L);
	/** Counter of terminated threads */
	protected final AtomicLong terminatedThreadCount = new AtomicLong(0L);
	
	
	
	/**
	 * Creates a new HeliosThreadFactory
	 * @param groupName The thread group name
	 * @param stackSize The thread stack size
	 * @param priority The thread priorities
	 * @param eHandler The uncaught exception handler
	 * @param daemon true if created threads will be daemons
	 */	
	public HeliosThreadFactory(String groupName, long stackSize, int priority, UncaughtExceptionHandler eHandler, boolean daemon) {
		this.groupName = groupName;
		this.stackSize = stackSize;
		this.priority = priority;
		this.eHandler = eHandler;
		this.daemon = daemon;
		this.threadGroup = HeliosThreadGroup.getInstance(this.groupName);
	}




	/**
	 * Creates a new HeliosThreadFactory
	 * @param r The runnable the thread will run
	 * @return a new thread
	 */
	@Override
	public Thread newThread(Runnable r) {
		HeliosThread t = new HeliosThread(threadGroup, r, groupName + "Thread#" + newThreadCount.incrementAndGet(), 0, terminatedThreadCount);
		t.setDaemon(daemon);
		t.setPriority(priority);
		t.setUncaughtExceptionHandler(eHandler);
		threadGroup.addThread(t);
		return t;
	}




	/**
	 * Returns the number of created threads
	 * @return the newThreadCount
	 */
	@JMXAttribute(name="NewThreadCount", description="The cummulative number of threads created", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getNewThreadCount() {
		return newThreadCount.get();
	}




	/**
	 * Returns the number of terminated threads
	 * @return the terminatedThreadCount
	 */
	@JMXAttribute(name="TerminatedThreadCount", description="The cummulative number of threads terminated", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTerminatedThreadCount() {
		return terminatedThreadCount.get();
	}




	/**
	 * @return the groupName
	 */
	public String getGroupName() {
		return groupName;
	}




	/**
	 * @param groupName the groupName to set
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}




	/**
	 * @return the stackSize
	 */
	public long getStackSize() {
		return stackSize;
	}




	/**
	 * @param stackSize the stackSize to set
	 */
	public void setStackSize(long stackSize) {
		this.stackSize = stackSize;
	}




	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}




	/**
	 * @param priority the priority to set
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}




	/**
	 * @return the eHandler
	 */
	public UncaughtExceptionHandler getUncaughtExceptionHandler() {
		return eHandler;
	}




	/**
	 * @param eHandler the eHandler to set
	 */
	public void setUncaughtExceptionHandler(UncaughtExceptionHandler eHandler) {
		this.eHandler = eHandler;
	}




	/**
	 * @return the daemon
	 */
	public boolean isDaemon() {
		return daemon;
	}




	/**
	 * @param daemon the daemon to set
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}




	/**
	 * @return the threadGroup
	 */
	public ThreadGroup getThreadGroup() {
		return threadGroup;
	}

}
