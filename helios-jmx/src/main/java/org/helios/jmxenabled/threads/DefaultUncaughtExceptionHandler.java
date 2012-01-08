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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.helpers.StringHelper;

/**
 * <p>Title: DefaultUncaughtExceptionHandler</p>
 * <p>Description: The default {@ java.lang.Thread.UncaughtExceptionHandler} for the Helios {@ ExecutorFactory}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.DefaultUncaughtExceptionHandler</code></p>
 */

public class DefaultUncaughtExceptionHandler implements UncaughtExceptionHandler {
	/** Instance logger */
	protected final Logger log;
	/** The name for the logger */
	protected final CharSequence handlerName;
	/** The notification sender used to send a notification on an uncaught exception */
	protected volatile NotificationSender sender; 
	/** The notification sequence factory */
	protected final AtomicLong sequence = new AtomicLong(0L);
	
	/**
	 * Creates a new DefaultUncaughtExceptionHandler
	 * @param handlerName The name for the logger that will log the exception.
	 * @param sender An optional sender used to send JMX notifications.
	 */
	public DefaultUncaughtExceptionHandler(CharSequence handlerName, NotificationSender sender) {				
		if(handlerName==null) {
			this.handlerName = getClass().getSimpleName();
		} else {
			this.handlerName = handlerName;			
		}
		this.log = Logger.getLogger(this.handlerName.toString());
		this.sender = sender;
	}

	/**
	 * Method invoked when the given thread terminates due to the given uncaught exception.
	 * @param t The thread
	 * @param e The exception
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		if(log.isEnabledFor(Level.WARN)) {
			StringBuilder b = new StringBuilder("Thread uncaught exception:");
			b.append("\nThread:").append(t.toString());
			log.warn(b.toString(), e);
		}
		if(sender!=null) {
			Notification n = new Notification(ExecutorFactory.NOTIFICATION_UNHANDLED_EXCEPTION, t.toString(), sequence.incrementAndGet(), System.currentTimeMillis(), "Unhandled exception in thread [" + t.toString() + "]:[" + e + "]");
			n.setUserData(StringHelper.formatStackTrace(e));
			sender.sendNotification(n);
		}
	}

	/**
	 * Returns this exception handler's JMX notification sender
	 * @return this exception handler's JMX notification sender
	 */
	public NotificationSender getSender() {
		return sender;
	}

	/**
	 * Sets this exception handler's JMX notification sender
	 * @param sender a JMX notification sender
	 */
	public void setSender(NotificationSender sender) {
		this.sender = sender;
	}

}
