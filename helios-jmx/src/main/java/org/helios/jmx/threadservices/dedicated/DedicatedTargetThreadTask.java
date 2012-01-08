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

/**
 * <p>Title: DedicatedTargetThreadTask</p>
 * <p>Description: Defines a task to be assigned exclusively to a <code>DedicatedTargetThread</code>.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface DedicatedTargetThreadTask extends Runnable {
	/** Retrieves the backlog items for this task */
	public int getBackLog();
	/** pause the task */
	public void pause();
	/** resume the task */
	public void resume();
	/** stop the thread and clear any allocated resources */
	public void stop();
	/** allows the task to return an instrumentation object that will be injected into the MODB */
	public Object[] getInstrumentation();
	/** Assigns the actual thread that will do the work */
	public void setWorkerThread(Thread thread);
	/** Returns the name of the task */
	public String getName();
	
	
}
