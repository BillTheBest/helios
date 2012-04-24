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
package org.helios.collectors.scheduler.quartz;

/**
 * <p>Title: QuartzExecutionTaskType</p>
 * <p>Description: Enumerates the QuartzExecutionTask's payload options.</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public enum QuartzExecutionTaskType {


	CALLABLE(0), RUNNABLE(1), EXECUTION_TASK(2);

	public static final int CALLABLE_ID = 0;
	public static final int RUNNABLE_ID = 1;
	public static final int EXECUTION_TASK_ID = 2;


	private final int id;
	QuartzExecutionTaskType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
