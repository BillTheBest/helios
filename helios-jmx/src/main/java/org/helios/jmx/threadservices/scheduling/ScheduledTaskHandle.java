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
package org.helios.jmx.threadservices.scheduling;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Title: ScheduledTaskHandle</p>
 * <p>Description: A handle to a scheduled task that exposes <code>ScheduledFuture</code> operations, extended operations to interact with the scehduler and a number of instrumentation attributes.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface ScheduledTaskHandle<V> extends ScheduledFuture<V> {
	/**
	 * Causes the current thead to wait on the next execution of the trigger.
	 * @throws InterruptedException 
	 */
	public void waitOnNextExecution() throws InterruptedException;
	
	/**
	 * Causes the current thead to wait on the next execution of the trigger for the specified period.
	 * @param wait The timeout period.
	 * @param unit The unit of the period.
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public void waitOnNextExecution(long wait, TimeUnit unit) throws InterruptedException, TimeoutException;
	
	/**
	 * The number of times the task has been executed.
	 * @return the executionCount
	 */
	public int getExecuteCount();
	
	/**
	 * The number of times the task has executed and completed.
	 * @return the completeCount
	 */
	public int getCompleteCount();

	/**
	 * The number of times the task execution has thrown an exception.
	 * @return the failCount
	 */
	public int getFailCount();

	/**
	 * The number of times the task has been vetoed.
	 * @return the vetoCount
	 */
	public int getVetoCount();
	
	/**
	 * Determines if the task trigger is being vetoed.
	 * If true, the trigger will be vetoed when fired.
	 * @return the veto Enabled state
	 */
	public boolean getPaused();

	/**
	 * Enables and disables the veto.
	 * If true, the trigger will be vetoed when fired.
	 * @param vetoEnabled the state to set veto Enabled to
	 */
	public void setPaused(boolean vetoEnabled);
	
	/**
	 * Registers a new listener on this scheduled task.
	 * @param scheduledTaskListener the listener to register.
	 */
	public void registerScheduledTaskListener(ScheduledTaskListener<V> scheduledTaskListener);
	
	/**
	 * Unregisters a new listener on this scheduled task.
	 * @param scheduledTaskListener the listener to unregister.
	 */
	public void unRegisterScheduledTaskListener(ScheduledTaskListener<V> scheduledTaskListener);
	
	
	
}
