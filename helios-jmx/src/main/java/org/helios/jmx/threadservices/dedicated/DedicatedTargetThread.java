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
package org.helios.jmx.threadservices.dedicated;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServer;

import org.helios.jmx.dynamic.annotations.JMXManagedObject;

/**
 * <p>Title: DedicatedTargetThread</p>
 * <p>Description: A thread extension that is allocated one specific to perform but is managed by a dedicated target thread pool.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(annotated=true, declared=true)
public class DedicatedTargetThread extends Thread {
	/** The thread sequence number in the pool also used to specify target resources. */
	protected int threadNumber = 0;
	/** The MBeanServer where the management interface will be registered */
	protected MBeanServer server = null;
	/** The dedicated task for this thread to execute */
	protected DedicatedTargetThreadTask task = null;
	/** the paused state of this thread */
	protected AtomicBoolean paused = new AtomicBoolean(true);
	
	//======================================
	//		Instrumentation
	//======================================	
	/** the number of tasks executed */
	protected long tasksExecuted = 0;
	/** the total time elapsed executing tasks */
	protected long totalExecutionTime = 0;
	/** the total CPU time accumulated by this thread */
	protected long totalCpuTime = 0;
	/** the total Block time accumulated by this thread */
	protected long totalBlockTime = 0;
	/** the total wait time accumulated by this thread */
	protected long totalWaitTime = 0;
	/** the total stopped time (block + wait) accumulated by this thread */
	protected long totalStoppedTime = 0;
	
	
	
	//======================================
	//		Baselines
	//======================================
	protected long blockTime = 0;
	protected long waitTime = 0;
	protected long startTime = 0;
	protected long cpuTime = 0;
	
}
