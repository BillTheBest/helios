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

import static org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture.BLOCK;
import static org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture.CPU;
import static org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture.WAIT;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.instrumentation.TaskThreadInfoManager;

/**
 * <p>Title: AbstractDedicatedTargetThreadTask</p>
 * <p>Description: An abstract implementation of <code>DedicatedTargetThreadTask</code>.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(annotated=true, declared=false)
public abstract class AbstractDedicatedTargetThreadTask implements DedicatedTargetThreadTask {
	/** the pause state */
	protected AtomicBoolean paused = new AtomicBoolean(false);
	/** the run state */
	protected AtomicBoolean run = new AtomicBoolean(true);
	/** the mod# assignment for this task */
	protected int mod = 0;
	/** The thread running this task */
	protected Thread thread = null;
	/** the total pause time of this task */
	protected AtomicLong pauseTime = new AtomicLong(0);
	
	/** Object instance logger */
	protected Logger log = Logger.getLogger(getClass());
	
	
	/** The task name */
	protected String name = null;
	
	
	/**
	 * Creates a new AbstractDedicatedTargetThreadTask.
	 * @param thread The thread that has been assigned to run the task.
	 */
	public AbstractDedicatedTargetThreadTask(String name) {
		this.name = name;
	}
	
	/**
	 * Sets the worker thread that will run this task.
	 * @param thread
	 */
	public void setWorkerThread(Thread thread) {
		this.thread = thread;
		this.thread.setName(name);
	}
	
	/**
	 * Returns <code>this</code> and the <code>TaskThreadInfoManager</code> for instrumentation.
	 * @return
	 * @see org.helios.jmx.threadservices.dedicated.DedicatedTargetThreadTask#getInstrumentation()
	 */
	public Object[] getInstrumentation() {
		return new Object[]{this};
	}


	/**
	 * Pauses the rask thread. 
	 * @see org.helios.jmx.threadservices.dedicated.DedicatedTargetThreadTask#pause()
	 */
	public void pause() {
		if(!paused.get()) return;
		paused.set(true);
		thread.interrupt();
	}

	/**
	 * Resumes the task thread. 
	 * @see org.helios.jmx.threadservices.dedicated.DedicatedTargetThreadTask#resume()
	 */
	public void resume() {
		if(paused.get()) return;
		paused.set(false);
		thread.interrupt();		
	}

	/**
	 * Stops the task thread.
	 * @see org.helios.jmx.threadservices.dedicated.DedicatedTargetThreadTask#stop()
	 */
	public void stop() {
		run.set(false);
		paused.set(false);
		thread.interrupt();
	}

	/**
	 * Puts the thread into a run loop, executing the assigned task.
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(run.get()) {
			pauseTime.addAndGet(testForPause());			
			try {
				runTask();
			} catch (InterruptedException ie) {
				// can be called to pause the thread: it will continue the loop and pause in testForPause()
				// or to stop the thread: it will exit the loop 
				// either way, we just clear the interrupt state.
				Thread.interrupted();
			} catch (Exception e) {
				// unexpected exception.
				// log and contine
				log.warn("DedicatedTask Run Loop encountered exception. Continuing...", e);
			}
		}
		cleanup();
	}
	
	
	/**
	 * Tests to see if the pause flag has been set. 
	 * If it has, the thread goes into paused state.
	 * @return The total pause time.
	 */
	protected long testForPause() {
		if(paused.get()) {
			long startPause = System.currentTimeMillis();
			while(paused.get()) {
				try {
					Thread.currentThread().join();
				} catch (Exception e) {}  // thread might get interrupted while it is already paused.
			}
			return System.currentTimeMillis()-startPause;
		}	
		return 0;
	}
	
	/**
	 * Concrete extensions of this class should implement the repeating task here.
	 */
	public abstract void runTask() throws InterruptedException;

	/**
	 * Extenders of this class can override this method
	 * so that on task stop,  resources are cleaned up.
	 */
	protected void cleanup() {}

	/**
	 * @return the paused
	 */
	@JMXAttribute(name="{a:getName}Paused", description="The paused state of the thread", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getPaused() {
		return paused.get();
	}

	/**
	 * @return the pauseTime
	 */
	@JMXAttribute(name="{a:getName}PauseTime", description="The total pause time of the thread", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getPauseTime() {
		return pauseTime.get();
	}

}
