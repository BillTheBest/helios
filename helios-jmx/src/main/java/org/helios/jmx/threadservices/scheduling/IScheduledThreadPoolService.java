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

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.helios.jmx.threadservices.IThreadPoolService;

/**
 * <p>Title: IScheduledThreadPoolService</p>
 * <p>Description: Defines a task execution interface for deferred or scheduled repeating tasks to be submitted to a scheduler.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface IScheduledThreadPoolService // IThreadPoolService 
{
	
	//=================================
	//  One time deferred execution
	//=================================	
	
	public <T> ScheduledTaskHandle<T> scheduleDeferred(Callable<T> callable, long delay, TimeUnit timeUnit);
	
	public <T> ScheduledTaskHandle<T> scheduleDeferred(Runnable runnable, long delay, TimeUnit timeUnit);
	
	public <T> ScheduledTaskHandle<T> scheduleDeferred(ExecutionTask<T> executionTask, long delay, TimeUnit timeUnit);
	
	//=================================
	//  Repeating frequency based executions
	//=================================
	
	public <T> ScheduledTaskHandle<T> scheduleAtFrequency(boolean fixedRate, Callable<T> callable, long delay, long period, TimeUnit timeUnit);
	
	public <T> ScheduledTaskHandle<T> scheduleAtFrequency(boolean fixedRate, Runnable runnable, long delay, long period, TimeUnit timeUnit);
	
	public <T> ScheduledTaskHandle<T> scheduleAtFrequency(boolean fixedRate, ExecutionTask<T> executionTask, long initalDelay, long period, TimeUnit timeUnit);
	
	//=================================
	//  Repeating schedule based executions
	//=================================
	
	public <T> ScheduledTaskHandle<T> scheduleWithCron(Callable<T> callable, String cron);
	
	public <T> ScheduledTaskHandle<T> scheduleWithCron(Runnable runnable, String cron);
	
	public <T> ScheduledTaskHandle<T> scheduleWithCron(ExecutionTask<T> executionTask, String cron);
	
	
}
