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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: DedicatedTargetThreadPool</p>
 * <p>Description: A pool manager for a group of DedicatedTargetThreads.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(annotated=true, declared=true)
public class DedicatedTargetThreadPool extends ManagedObjectDynamicMBean implements ThreadFactory {
	private static final long serialVersionUID = -4438814114776769954L;
	/** An array of dedicated target worker thread tasks */
	protected Set<DedicatedTargetThreadTask> tasks = new CopyOnWriteArraySet<DedicatedTargetThreadTask>();
	/** The nme of the pool */
	protected String name = null;
	/** The MBeanServer where the pool management interface will be registered */
	protected MBeanServer server = null;
	/** The JMX ObjectName of the thread pool management interface */
	protected ObjectName objectName = null;
	/** Indicates if the pool is paused */
	protected boolean paused = false;
	/** The thread factory that will create the worker threads for this pool */
	protected ThreadFactory threadFactory = null;
	/** Object instance logger */
	protected Logger log = Logger.getLogger(getClass());
	
	/**
	 * Creates a new dedicated task thread pool.
	 * @param tasks An array of tasks. Ignored if null.
	 * @param name The name of the pool.
	 * @param server The MBeanServer where the pool will be registered.
	 * @param objectName The JMX ObjectName of the pool.
	 * @param threadFactory The thread factory to use to generate new threads. If null, will use a default.
	 */
	public DedicatedTargetThreadPool(DedicatedTargetThreadTask[] tasks,
			String name, MBeanServer server, ObjectName objectName, ThreadFactory threadFactory) {
		super();
		if(tasks != null) {
			for(DedicatedTargetThreadTask task: tasks) { this.tasks.add(task); }
		}
		this.name = name;
		this.server = server;
		this.objectName = objectName;
		if(threadFactory==null) {
			this.threadFactory = this;			
		} else {
			this.threadFactory = threadFactory;
		}
		reflectObject(this);
		for(DedicatedTargetThreadTask task: tasks) {
			addDedicatedTask(task);
		}		
		try {
			server.registerMBean(this, objectName);
		} catch (Exception e) {
			log.warn("Failed to JMX register DedicatedTargetThreadPool[" + objectName + "]", e);
		}
	}

	/**
	 * Initializes a dedicated task.
	 * @param task The task to initialize
	 */
	protected void addDedicatedTask(DedicatedTargetThreadTask task) {
		Thread t = this.threadFactory.newThread(task);
		task.setWorkerThread(t);
		for(Object o: task.getInstrumentation()) {
			if(o!=null) {
				try {
					if(log.isDebugEnabled()) log.info("Registering Instrumentation for [" + o + "]");
					reflectObject(o);
				} catch (Exception e) {
					log.warn("Failed to add dynamic instrumentation to [" + objectName + "]", e);
				}
			}
		}
	}
	
	/**
	 * Adds a new task to the pool
	 * @param task The task to add.
	 */
	public void addDedicatedTargetThreadTask(DedicatedTargetThreadTask task) {
		tasks.add(task);
		addDedicatedTask(task);
	}
	
	/**
	 * Starts the underlying worker threads.
	 */
	public void start() {
		for(DedicatedTargetThreadTask task: tasks) {
			Thread t = threadFactory.newThread(task);
			task.setWorkerThread(t);
			t.start();
			if(log.isDebugEnabled()) log.info("Started DedicatedTask Thread [" + task.getName() + "]");
		}
	}
	
	/**
	 * Stops the underlying worker threads.
	 */
	public void stop() {
		for(DedicatedTargetThreadTask task: tasks) {
			if(log.isDebugEnabled()) log.info("Stopping DedicatedTask Thread [" + task.getName() + "]");
			task.stop();
			if(log.isDebugEnabled()) log.info("Stopped DedicatedTask Thread [" + task.getName() + "]");
		}
		try {
			server.unregisterMBean(objectName);
		} catch (Exception e) {			
		}
	}
	
	/**
	 * Pauses the underlying worker threads.
	 */
	public void pause() {
		if(paused) return;
		for(DedicatedTargetThreadTask task: tasks) {
			if(log.isDebugEnabled()) log.info("Pausing DedicatedTask Thread [" + task.getName() + "]");
			task.pause();
			if(log.isDebugEnabled()) log.info("Paused DedicatedTask Thread [" + task.getName() + "]");
		}
		paused=true;
	}
	
	/**
	 * Resumes the underlying worker threads.
	 */
	public void resume() {
		if(!paused) return;
		for(DedicatedTargetThreadTask task: tasks) {
			if(log.isDebugEnabled()) log.info("Resuming DedicatedTask Thread [" + task.getName() + "]");
			task.resume();
			if(log.isDebugEnabled()) log.info("Resumed DedicatedTask Thread [" + task.getName() + "]");
		}
		paused=false;
	}

	/**
	 * The default thread creation routine for this thread pool.
	 * @param r The runnable assigned to this thread.
	 * @return A new thread.
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, name);
		t.setDaemon(true);
		t.setPriority(Thread.NORM_PRIORITY +1);
		return t;
	}

	/**
	 * @return the paused
	 */
	@JMXAttribute(name="Paused", description="The paused state of the pool", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getPaused() {
		return paused;
	}
}
