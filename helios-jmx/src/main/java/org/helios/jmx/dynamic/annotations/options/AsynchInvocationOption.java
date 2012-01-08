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
package org.helios.jmx.dynamic.annotations.options;

/**
 * <p>Title: JMXAsynchInvocationOption</p>
 * <p>Description: An enum listing the options for dispatching an asynch operation.
 * These are the current options:<ul>
 * <li>SCHEDULE: Invocation is scheduled to execute through a ScheduledThreadPoolExecutor.
 * <li>THREAD_POOL: Invocation is passed off to a ThreadPoolExecutor.
 * <li>TIMER: Invocation is scheduled through the host MBean's <code>TimerUtil</code>.
 * <li>SPAWN: Invocation is passed to a new spawned thread.
 * </ul></p>   
 * <p>The combination of the <code>JMXAsynchInvocationOption</code> and the <code>ScheduleOption</code>
 * will determine the return type of the original MBean asynch invocation.
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */

public enum AsynchInvocationOption {
	/**Invocation is scheduled to execute through a ScheduledThreadPoolExecutor. */
	SCHEDULE(0),
	/**Invocation is passed off to a ThreadPoolExecutor. <b>DEFAULT</b>*/	
	THREAD_POOL(1),
	/**Invocation is scheduled through the host MBean's <code>TimerUtil</code>. */
	TIMER(2),
	/**Invocation is passed to a new spawned thread. */
	SPAWN(3);
	
    /** the index of this unit */
    private final int index;

    /** Internal constructor */
    AsynchInvocationOption(int index) { 
        this.index = index; 
    }
    
    /**
     * The default invocation dispatch.  
     * @return
     */
    public AsynchInvocationOption DEFAULT() {
    	return THREAD_POOL;
    }
    
    /**
     * Returns true if the current enum indicates a scheduled invocation.
     * @return
     */
    public boolean isScheduled() {
    	return (index==SCHEDULE.index ||  index==TIMER.index);
    }
}
