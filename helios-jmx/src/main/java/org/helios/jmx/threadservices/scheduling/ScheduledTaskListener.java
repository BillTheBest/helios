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

/**
 * <p>Title: ScheduledTaskListener</p>
 * <p>Description: Defines a listener that receives callbacks on events emitted from a scheduled task.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface ScheduledTaskListener<T> {
	/**
	 * Callback occurs when a scheduled task starts execution.
	 * @param taskName The name of the task that emmitted the event.
	 * @param fireCount the number of times the task has fired (including this one). 
	 */
	public void onTaskFired(String taskName, int fireCount);
	/**
	 * Callback occurs when a scheduled task execution throws an exception or a trigger misfire occurs.
	 * @param taskName The name of the task that emmitted the event.
	 * @param failCount the number of times the task execution has failed (including this one). 
	 */
	public void onTaskFailed(String taskName, int failCount);
	/**
	 * Callback occurs when a scheduled task execution completes.
	 * @param taskName The name of the task that emmitted the event.
	 * @param result The result of the execution. 
	 * @param completeCount The number of times the task has completed (including this one).
	 */
	public void onTaskComplete(String taskName, T result, int completeCount);
	
	/**
	 * Callback occurs when a scheduled task's execution is vetoed.
	 * @param taskName The name of the task that emmitted the event.
	 * @param vetoCount The number of times the task execution has been vetoed (including this one).
	 */
	public void onTaskVetoed(String taskName, int vetoCount);
		
	/**
	 * Callback occurs when a scheduled task is paused or resumed.
	 * @param taskName The name of the task that emmitted the event.
	 * @param paused true if the task was paused, false if it was resumed.
	 */
	public void onTaskPaused(String taskName, boolean paused);
	
	/**
	 * Callback occurs when a scheduled task is cancelled.
	 * @param taskName The name of the task that emmitted the event.
	 */
	public void onTaskCancelled(String taskName);
	
}
