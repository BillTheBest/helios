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
package org.helios.jmx.threadservices.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Stack;

/**
 * <p>Title: ThreadResourceMonitor</p>
 * <p>Description: Thread resource and contention monitoring utility class.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ThreadResourceMonitor {
	/** A handle to the threadMXBean */
	protected static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	protected static boolean threadCpuTimeSupported = threadMXBean.isThreadCpuTimeSupported();
	protected static boolean threadContentionSupported = threadMXBean.isThreadContentionMonitoringSupported();
	protected static boolean objectMonitorUsageSupported = false;
	protected static boolean synchronizerUsageSupported = false;
	protected static boolean is16 = false;
	protected static final Class<?>[] NULL_SIG = new Class[0];
	protected static final Object[] NULL_ARGS = new Object[0];
	protected static ThreadLocal<Stack<ThreadResourceSnapshot>> snapshots = new ThreadLocal<Stack<ThreadResourceSnapshot>>(); 
	
	
	static {
		if(threadCpuTimeSupported) threadMXBean.setThreadContentionMonitoringEnabled(true);
		if(threadContentionSupported) threadMXBean.setThreadContentionMonitoringEnabled(true);
		try {
			Class.forName("java.lang.management.LockInfo");
			is16 = true;
			objectMonitorUsageSupported = (Boolean)threadMXBean.getClass().getMethod("isObjectMonitorUsageSupported", NULL_SIG).invoke(null, NULL_ARGS);
			synchronizerUsageSupported = (Boolean)threadMXBean.getClass().getMethod("isSynchronizerUsageSupported", NULL_SIG).invoke(null, NULL_ARGS);
		} catch (Exception e) {
			is16 = false;
		}
	}
	
	/**
	 * Starts a new thread resource snapshot.
	 */
	public static void start() {
		Stack<ThreadResourceSnapshot> stack = snapshots.get();
		if(stack==null) {
			stack = new Stack<ThreadResourceSnapshot>();
			snapshots.set(stack);
		}
		stack.push(new ThreadResourceSnapshot());
		
	}
	
	/**
	 * Stops a thread resource snapshot, closes it and returns it.
	 * If, for some programmer or envionment error results in the in state snapshot not being found, a null will be returned.
	 * @return A closed ThreadResourceSnapshot or null.
	 */
	public static ThreadResourceSnapshot stop() {
		Stack<ThreadResourceSnapshot> stack = snapshots.get();
		if(stack==null) return null;
		ThreadResourceSnapshot trs = stack.pop();
		if(trs==null) return null;
		trs.close();
		return trs;
	}


	/**
	 * @return the threadCpuTimeSupported
	 */
	public static boolean isThreadCpuTimeSupported() {
		return threadCpuTimeSupported;
	}


	/**
	 * @return the threadContentionSupported
	 */
	public static boolean isThreadContentionSupported() {
		return threadContentionSupported;
	}
	
	
}
