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
package test.org.helios.ot.util;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;
import org.helios.helpers.Banner;

/**
 * <p>Title: EventHandlerAppender</p>
 * <p>Description: A log4j appender we use to attach to the TracerManager logger which will callback with the passed TraceCollections passed to handlers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.util.EventHandlerAppender</code></p>
 */

public class EventHandlerAppender extends AppenderSkeleton {
	/** A queue where all the appended logging event message objects are queued to */
	protected final BlockingQueue<Object> eventMessageQueue = new LinkedBlockingQueue<Object>();
	/**
	 * Creates a new EventHandlerAppender
	 * @param name The appender's name
	 */
	public EventHandlerAppender(String name) {
		super(true);
		setName(name);
	}


	/**
	 * Appends a logging event
	 * @param event The event to capture
	 */
	@Override
	protected void append(LoggingEvent event) {
		//Banner.bannerOut("*", 2, 5, "EventHandlerAppender", event.getMessage().toString());
		Object context = event.getMDC("TraceCollectionContext");
		//Banner.bannerOut("#", 2, 5, "EventHandlerAppender.append(" + System.identityHashCode(context) + "):", context.toString());
		if(context!=null) {
			eventMessageQueue.add(context);
			MDC.remove("TraceCollectionContext");
		}
		
	}

	/**
	 * No Op.
	 */
	@Override
	public void close() {
	}

	/**
	 * Returns false.
	 * @return false
	 */
	@Override
	public boolean requiresLayout() {
		return false;
	}


	/**
	 * Retrieves and removes the head of this queue, or returns null if this queue is empty. 
	 * @return the head of this queue, or null if this queue is empty
	 * @see java.util.Queue#poll()
	 */
	public Object poll() {
		return eventMessageQueue.poll();
	}


	/**
	 * Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary for an element to become available. 
	 * @param timeout how long to wait before giving up, in units of <code>unit</code>
	 * @param unit a TimeUnit determining how to interpret the <code>timeout</code> parameter 
	 * @return the head of this queue, or null if the specified waiting time elapses before an element is available 
	 * @throws InterruptedException if interrupted while waiting
	 * @see java.util.concurrent.BlockingQueue#poll(long, java.util.concurrent.TimeUnit)
	 */
	public Object poll(long timeout, TimeUnit unit) throws InterruptedException {
		return eventMessageQueue.poll(timeout, unit);
	}





	/**
	 * Returns the number of items in the queue
	 * @return the number of items in the queue
	 * @see java.util.Collection#size()
	 */
	public int size() {
		return eventMessageQueue.size();
	}


	/**
	 * Retrieves and removes the head of this queue, waiting if necessary until an element becomes available.
	 * @return the head of this queue 
	 * @throws InterruptedException if interrupted while waiting
	 * @see java.util.concurrent.BlockingQueue#take()
	 */
	public Object take() throws InterruptedException {
		Object obj =  eventMessageQueue.take();
		//Banner.bannerOut("#", 2, 5, "EventHandlerAppender.take(" + System.identityHashCode(obj) + "):", obj.toString());
		return obj;
	}


	/**
	 * Drains the queue into the passed collection
	 * @param drain The collection to drain to
	 * @return the number of items drained
	 * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection)
	 */
	public int drainTo(Collection<? super Object> drain) {
		return eventMessageQueue.drainTo(drain);
	}

}
