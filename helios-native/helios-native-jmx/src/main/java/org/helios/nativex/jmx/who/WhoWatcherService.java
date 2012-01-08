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
package org.helios.nativex.jmx.who;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Notification;

import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.jmx.netroutes.NetRouteInstanceService;
import org.helios.nativex.jmx.netroutes.NetRouteKey;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.Who;

/**
 * <p>Title: WhoWatcherService</p>
 * <p>Description: Monitors logged in users through who and generates/unregisters MBean for each when they login/logout</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.who.WhoWatcherService</code></p>
 */
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating a new login", types={
                @JMXNotificationType(type=WhoWatcherService.WHO_ADD)
        }),
        @JMXNotification(description="Notification indicating a logout", types={
                @JMXNotificationType(type=WhoWatcherService.WHO_DELETE)
        })
})
public class WhoWatcherService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = -2240753516200541793L;

	/** A map of the current whos keyed by the whoKey */
	protected final Map<WhoKey, WhoInstanceService> whos = new ConcurrentHashMap<WhoKey, WhoInstanceService>();
	
	/** Notification for new logins */
	public static final String WHO_ADD = "org.helios.nativex.jmx.who.Login";
	/** Notification for recognized logouts */
	public static final String WHO_DELETE = "org.helios.nativex.jmx.who.Logout";
	
	/**
	 * Creates a new WhoWatcherService
	 */
	public WhoWatcherService() {
		super();
		run();
		scheduleSampling();
		this.registerCounterMBean("service", "WhoWatchService");
	}
	
	/**
	 * Bootstraps this service
	 */
	public static void boot() {
		new WhoWatcherService();
	}
	
	/**
	 * Invokes a who refresh
	 */
	public void run() {
		long time = System.currentTimeMillis();
		for(Who who: HeliosSigar.getInstance().getWhoList()) {
			WhoKey whk = new WhoKey(who);
			WhoInstanceService wis = whos.get(whk);
			if(wis==null) {
				wis = new WhoInstanceService(who, whk);
				whos.put(whk, wis);
				sendNotification(new Notification(WHO_ADD, whk.getObjectName(), this.nextNotificationSequence(), time, "Login Event " + whk.toString()));
			} 
			wis.setScanned(true);
		}
		
		// find Whos with false scanned and unregister
		// set all scanneds to false
		Set<WhoKey> removes = new HashSet<WhoKey>();
		for(Map.Entry<WhoKey, WhoInstanceService> who: whos.entrySet()) {
			if(!who.getValue().isScanned()) {
				log.info("Removing Logged Out Who [" + who.getKey().getObjectName() + "]");				
				try { 
					JMXHelper.getHeliosMBeanServer().unregisterMBean(who.getKey().getObjectName()); 
					removes.add(who.getKey());
				} catch (Exception e) {}
			}
		}		
		for(WhoKey key: removes) {
			whos.remove(key);
			sendNotification(new Notification(WHO_DELETE, key.getObjectName(), this.nextNotificationSequence(), time, "Logout Event " + key.toString()));
		}
		for(WhoInstanceService who: whos.values()) {
			who.resetScan();
		}		
	}
	
	
	
		
		

}
