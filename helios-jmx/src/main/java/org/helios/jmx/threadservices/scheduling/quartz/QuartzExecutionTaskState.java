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
package org.helios.jmx.threadservices.scheduling.quartz;

/**
 * <p>Title: QuartzExecutionTaskState</p>
 * <p>Description: Enumerates a QuartzExecutionTask's possible states.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public enum QuartzExecutionTaskState {
	/** Task has never been fired */
	UNFIRED(0), 
	/** Task is being fired */
	FIRING(1), 
	/** Task has fired but end state is not known */
	FIRED(2), 
	/** Task has completed successfully and will not be fired again */
	COMPLETED(3),
	/** Task has completed with an exception and will not be fired again */
	EXCEPTION(4), 	
	/** Task has completed successfully and is scheduled to fire again */ 
	COMPLETED_CONTINUING(5),
	/** Task has completed with an exception and is scheduled to fire again */ 
	EXCEPTION_CONTINUING(6), 	
	/** Task has been cancelled */
	CANCELLED(7);

	/** Task has never been fired */
	public static final int UNFIRED_ID = 0;
	/** Task is being fired */
	public static final int FIRING_ID = 1;
	/** Task has fired but end state is not known */
	public static final int FIRED_ID = 2;
	/** Task has completed successfully and will not be fired again */
	public static final int COMPLETED_ID = 3;
	/** Task has completed with an exception and will not be fired again */
	public static final int EXCEPTION_ID = 4;	
	/** Task has completed successfully and is scheduled to fire again */
	public static final int COMPLETED_CONTINUING_ID = 5;
	/** Task has completed with an exception and is scheduled to fire again */
	public static final int EXCEPTION_CONTINUING_ID = 6;	
	/** Task has been cancelled */
	public static final int CANCELLED_ID = 7;

	
	private final int id;
	
	QuartzExecutionTaskState(int id) {
		this.id = id;
	}
	
	public static QuartzExecutionTaskState valueOf(int id) {
		switch (id) {
		case UNFIRED_ID:
			return QuartzExecutionTaskState.UNFIRED;
		case FIRING_ID:
			return QuartzExecutionTaskState.FIRING;
		case FIRED_ID:
			return QuartzExecutionTaskState.FIRED;
		case COMPLETED_ID:
			return QuartzExecutionTaskState.COMPLETED;
		case EXCEPTION_ID:
			return QuartzExecutionTaskState.EXCEPTION;
		case COMPLETED_CONTINUING_ID:
			return QuartzExecutionTaskState.COMPLETED_CONTINUING;
		case EXCEPTION_CONTINUING_ID:
			return QuartzExecutionTaskState.EXCEPTION_CONTINUING;
		case CANCELLED_ID:
			return QuartzExecutionTaskState.CANCELLED;
		default:
			throw new RuntimeException("Invalid id [" + id + "]");
		}
	}
	
	public int getId() {
		return id;
	}
	
	public boolean willFireAgain() {
		return !(id==COMPLETED_ID || id==CANCELLED_ID || id==EXCEPTION_ID);
	}
	
	public boolean isDone() {
		return !(id==UNFIRED_ID || id==FIRING_ID || id==FIRED_ID || id==CANCELLED_ID);
	}
	
}
