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
package org.helios.server.ot.jmx.nhub;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;


/**
 * <p>Title: NotificationHubService</p>
 * <p>Description: MBean service that will acept subscriptions to notifications based on an ObjectName wildcard.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.jmx.nhub.NotificationHubService</code></p>
 */
public class NotificationHubService extends ManagedObjectDynamicMBean  {	
	/**  */
	private static final long serialVersionUID = 729125531669754133L;
	
	protected final Map<NotificationListener, Set<ObjectName>> subscribers = new ConcurrentHashMap<NotificationListener, Set<ObjectName>>();
	/**
	 * Creates a new NotificationHubService
	 * @param executor The executor used to dispatch notifications
	 */
	public NotificationHubService(Executor executor) {
		super(executor);
	}
	/**
	 * Creates a new NotificationHubService
	 */
	public NotificationHubService() {
		this(getDefaultExecutor());
	}
	
}
