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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.atomic.AtomicLong;
/**
 * <p>Title: TaskRejectionPolicy</p>
 * <p>Description: Enumerification of the ThreadPoolExecutor's rejection policy built-ins.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.TaskRejectionPolicy</code></p>
 */

public enum TaskRejectionPolicy implements RejectedExecutionHandler {
	ABORT(new AbortPolicy()),
	CALLERRUNS(new CallerRunsPolicy()),
	DISCARDOLDEST(new DiscardOldestPolicy()),
	DISCARD(new DiscardPolicy());
	
	/**
	 * Creates a new TaskRejectionPolicy 
	 * @param policy The policy rejection handler
	 */
	private TaskRejectionPolicy(RejectedExecutionHandler policy) {
		this.policy = policy;
	}
	
	/** The policy rejection handler */
	private final RejectedExecutionHandler policy;
	/** The individual policy rejection count */
	private final AtomicLong count = new AtomicLong(0L);
	/** The global policy rejection count */
	private static final AtomicLong globalCount = new AtomicLong(0L);
	
	/**
	 * Returns the TaskRejectionPolicy for the passed name
	 * @param name The name to get the TaskRejectionPolicy for
	 * @return the matching TaskRejectionPolicy or null if there was no match
	 */
	public TaskRejectionPolicy getPolicy(CharSequence name) {
		if(name==null) return null;
		try {
			return TaskRejectionPolicy.valueOf(name.toString().toUpperCase().trim());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Executes the policy rejection handler on the passed runnable
	 * @param r the runnable task requested to be executed
	 * @param executor the executor attempting to execute this task
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {	
		count.incrementAndGet();
		globalCount.incrementAndGet();
		policy.rejectedExecution(r, executor);
	}

	/**
	 * The total number of rejected tasks handled by this policy
	 * @return the total number of rejected tasks handled by this policy
	 */
	public AtomicLong getCount() {
		return count;
	}

	/**
	 * The total number of rejected tasks handled by all policies
	 * @return the total number of rejected tasks handled by all policies
	 */
	public static AtomicLong getGlobalcount() {
		return globalCount;
	}
	
	/**
	 * Returns a map of all the policy rejection counts keyed by the policy name
	 * @return a map of all the policy rejection counts keyed by the policy name
	 */
	public static Map<String, Long> getRejectionCounts() {
		Map<String, Long> map = new HashMap<String, Long>(TaskRejectionPolicy.values().length);
		for(TaskRejectionPolicy t: TaskRejectionPolicy.values()) {
			map.put(t.name(), t.count.get());
		}		
		return map;
	}

	/**
	 * The rejection policy instance
	 * @return the policy
	 */
	public RejectedExecutionHandler getPolicy() {
		return policy;
	}
	
	
}
