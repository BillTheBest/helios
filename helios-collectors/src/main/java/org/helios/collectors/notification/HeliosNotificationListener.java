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
package org.helios.collectors.notification;

import java.util.concurrent.ExecutorService;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.apache.log4j.Logger;

/**
 * <p>Title: HeliosNotificationListener</p>
 * <p>Description: Custom singleton listener for all Helios related notification and then channeling them 
 * to Helios agents through configured Helios adapters and Marshellers.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class HeliosNotificationListener implements NotificationListener{
	protected Logger log = Logger.getLogger(HeliosNotificationListener.class);
	protected ExecutorService executor = null;
	private static HeliosNotificationListener listenerInstance = null;
	
	private HeliosNotificationListener(java.util.concurrent.ExecutorService executor){
		this.executor = executor;
	}
	
	public static HeliosNotificationListener getInstance(java.util.concurrent.ExecutorService executor){
		if(listenerInstance==null){
			listenerInstance=new HeliosNotificationListener(executor);
		}
		return listenerInstance;
	}
	
	public void handleNotification(Notification notification, Object handback) {
		// 
		// UNCOMMENT WHEN NotificationRouter is FIXED
		//
//		NotificationRouter router = new NotificationRouter(notification,handback);
//		executor.execute(router);
	}

}
