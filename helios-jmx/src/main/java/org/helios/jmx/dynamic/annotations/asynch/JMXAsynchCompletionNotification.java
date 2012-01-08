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
package org.helios.jmx.dynamic.annotations.asynch;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.helios.jmx.dynamic.annotations.options.AsynchCompletionNotifyOption;

/**
 * <p>Title: JMXAsynchCompletionNotification</p>
 * <p>Description: Annotation to define how the completion of an asynch operation should be broadcast as a JMX notification.
 * If this option is enabled, the notification will be built as follows:<ul>
 * <li>Type: This will be <code>NOTIFICATION_TYPE</code> or the overriden value in <code>@asynchCompletionNotifyType</code>.
 * <li>Object: The source object which is the underlying managed pojo.
 * <li>Sequence Number: The incrementing sequence number indicating the number of times the operation has been invoked.
 * <li>Timestamp: The completion time of the operation. (Also approximately the emission time of the notification.)
 * <li>Message: A synthesized string comprised of the following dot separated fields: <ul>
 * <li>The mbean name.
 * <li>The managed object's registration name.
 * <li>The method name invoked asynchronously.
 * </ul>
 * <li>UserData: The return value of the operation.
 * </ul>
 * </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision: 26 $
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JMXAsynchCompletionNotification {
	
	public static final String NOTIFICATION_TYPE = "Helios.JMX.Asynch.Operation.Complete";

	/**
	 * Defines how the asynch operation will handle the completion.
	 * The default is <code>RETURN</code> and this default also applied if this annotation is not applied.
	 * The options are:<ul>
	 * <li>NOTIFY: Emit a notification only.
	 * <li>RETURN: Return the applicable completion object only.
	 * <li>BOTH: Return and notify.
	 * <li>EXCEPTION_ONLY_NOTIFY: Only notify if the operation throws an exception.
	 * <li>EXCEPTION_ONLY_RETURN: Only return if the operation throws an exception.
	 * <li>EXCEPTION_ONLY_BOTH: Return and notify if the operation throws an exception.
	 * </ul> 
	 * @return The completion handling option.
	 */
	AsynchCompletionNotifyOption asynchCompletionNotify() default AsynchCompletionNotifyOption.RETURN;
	
	/**
	 * This value defines the name of a method in the underlying managed pojo that when invoked
	 * will return a list of <code>javax.management.NotificationListener</code>s that the notification will be sent to.
	 * @return A method name in the underlying pojo.
	 */
	String asynchCompletionNotificationListeners() default "";
	
	
	/**
	 * The value of the <code>type</code> of the <code>Notification</code> that will be emitted when the operation completes.
	 * @return The completion notification type.
	 */
	String asynchCompletionNotificationType() default NOTIFICATION_TYPE;
	
	
	
}
