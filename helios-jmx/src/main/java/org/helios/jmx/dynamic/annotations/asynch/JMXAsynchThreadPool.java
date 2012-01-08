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

/**
 * <p>Title: JMXAsynchThreadPool</p>
 * <p>Description: Describes the threading mechanism to be implemented to dispatch the asynchronous operation.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JMXAsynchThreadPool {
	
	/**	The default JMX MBean ObjectName for the asynch operation dispatch ThreadPool */
	public static final String DEFAULT_THREAD_POOL_MBEAN_NAME = "";
	/**	The default JMX MBean Class Name for the asynch operation dispatch ThreadPool */
	public static final String DEFAULT_THREAD_POOL_CLASS_NAME = "";
	
	/**
	 * The name of the MBean that exposes the <code>java.util.concurrent.Executor</code> interface.
	 * This value must be assigned if the <code>@asynchInvocationType</code> is <code>JMXAsynchInvocationOption.SCHEDULE</code> or <code>JMXAsynchInvocationOption.THREAD_POOL</code>.
	 * If the <code>@asynchInvocationType</code> is <code>JMXAsynchInvocationOption.SCHEDULE</code>, the MBean should expose <code>java.util.concurrent.ScheduledExecutorService</code>.
	 * @return An MBean name for an MBean that exposes a <code>java.util.concurrent.Executor</code> or <code>java.util.concurrent.ScheduledExecutorService</code>.
	 */
	String asynchThreadPoolMBeanName() default DEFAULT_THREAD_POOL_MBEAN_NAME;
	
	/**
	 * When a <code>@asynchThreadPoolMBeanName</code> is specified, it is assumed that the MBean already exists.
	 * If it does not, the MBean can be created and registered automatically.
	 * This annotation specifies the class name of the <code>java.util.concurrent.Executor</code> providing class.
	 * @return A class name for the ThreadPool MBean to be created.
	 */
	String asynchThreadPoolMBeanClassName() default DEFAULT_THREAD_POOL_CLASS_NAME;
	
	

}
