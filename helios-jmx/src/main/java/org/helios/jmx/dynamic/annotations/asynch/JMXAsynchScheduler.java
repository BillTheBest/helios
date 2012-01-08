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

import org.helios.jmx.dynamic.annotations.options.AsynchScheduleOption;

/**
 * <p>Title: JMXAsynchScheduler</p>
 * <p>Description: Describes the threading mechanism used to schedule the asynch operation.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision: 24 $
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JMXAsynchScheduler {
	/**
	 * Sets the scheduling option for a scheduled asynch invocation.
	 * The current options are:<ul>
	 * <li>SCHEDULE: Schedules the invocation for a one time execution at some time in the future.
	 * <li>FIXED_RATE: Schedules the invocation for recurring execution at a fixed rate. (Ignores how long the invocation takes)
	 * <li>FIXED_DELAY: Schedules the invocation for recurring execution at a fixed delay. (Will not execute until the prior execution is complete)
	 * </ul></p> 
	 * @return A ScheduleOption
	 */
	AsynchScheduleOption asynchScheduleOption() default AsynchScheduleOption.SCHEDULE;
	
	// # of times to execute
	// time to execute until
	// time to execute at
	// inital Delay
	// Rate/Period or Delay/delay
	
//	 Need: initialDelay, period, TimeUnit, recurring|number of invocations|until
	
	
}
