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
package org.helios.tracing.extended.graphite;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Title: QueueSubmission</p>
 * <p>Description: Defines the options for submitting a new metric when the local queue is full.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.graphite.QueueSubmission</p></code>
 */
public enum QueueSubmission {
	/** Submission option that waits for queue space to become available. If the queue is full but the client is disconnected, message is dropped. */
	WAIT(new WaitSubmitter()),
	/** Submission option that drops the submission if no queue space is available */
	DROP(new DropSubmitter()),
	/** Submission option that drops the oldest queued item until space is available */
	DROP_OLDEST(new DropOldestSubmitter());
	
	/**
	 * Creates a new QueueSubmission
	 * @param submitter The instance's submitter 
	 */
	private QueueSubmission(QueueSubmitter submitter) {
		this.submitter = submitter;
	}
	
	/** The instance's submitter */
	private final QueueSubmitter submitter;
	
	/**
	 * Submits a message
	 * @param message The message to submit
	 * @param submissionQueue The submission queue
	 * @param connected Indicates if the client is connected.
	 * @return true if the message was submitted without a drop, true if a drop occurs.
	 */
	public boolean submit(byte[] message, final LinkedBlockingQueue<byte[]> submissionQueue, boolean connected) {
		return submitter.submit(message, submissionQueue, connected);
	}
	
	/**
	 * <p>Title: QueueSubmitter</p>
	 * <p>Description: Defines a queue submitter</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 */
	public static interface QueueSubmitter {
		/**
		 * Submits a message
		 * @param message The message to submit
		 * @param submissionQueue The submission queue
		 * @param connected Indicates if the client is connected.
		 * @return true if the message was submitted without a drop, true if a drop occurs.
		 */
		public boolean submit(byte[] message, final LinkedBlockingQueue<byte[]> submissionQueue, boolean connected);
	}
	
	/**
	 * <p>Title: WaitSubmitter</p>
	 * <p>Description: Submits a metric, waiting for space if necessary. If the queue is full but the client is disconnected, message is dropped.</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 */
	public static class WaitSubmitter implements QueueSubmitter {
		/**
		 * Submits a message
		 * @param message The message to submit
		 * @param submissionQueue The submission queue
		 * @param connected Indicates if the client is connected.
		 * @return true if the message was submitted without a drop, true if a drop occurs.
		 */
		public boolean submit(byte[] message,final LinkedBlockingQueue<byte[]> submissionQueue, boolean connected) {
			if(message==null || message.length < 1) return true;
			if(!connected) return false;
			try {
				submissionQueue.put(message);
				return true;
			} catch (InterruptedException e) {
				return false;
			} 
		}		
	}
	
	/**
	 * <p>Title: DropSubmitter</p>
	 * <p>Description: Submits a metric, dropping it immediately if no space is available.</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 */
	public static class DropSubmitter implements QueueSubmitter {
		/**
		 * Submits a message
		 * @param message The message to submit
		 * @param submissionQueue The submission queue
		 * @param connected Indicates if the client is connected.
		 * @return true if the message was submitted without a drop, true if a drop occurs.
		 */		
		public boolean submit(byte[] message,final LinkedBlockingQueue<byte[]> submissionQueue, boolean connected) {
			if(message==null || message.length < 1) return true;
			return submissionQueue.offer(message);  
		}		
	}
	
	/**
	 * <p>Title: DropOldestSubmitter</p>
	 * <p>Description: Submits a metric, dropping the oldest message if no space is available.</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 */
	public static class DropOldestSubmitter implements QueueSubmitter {
		/**
		 * Submits a message
		 * @param message The message to submit
		 * @param submissionQueue The submission queue
		 * @param connected Indicates if the client is connected.
		 * @return true if the message was submitted without a drop, true if a drop occurs.
		 */		
		public boolean submit(byte[] message,final LinkedBlockingQueue<byte[]> submissionQueue, boolean connected) {
			if(message==null || message.length < 1) return true;
			while(true) {
				if(!submissionQueue.offer(message)) {
					submissionQueue.poll();
				} else {
					break;
				}
			}
			return submissionQueue.offer(message);  
		}		
	}	
	
}
