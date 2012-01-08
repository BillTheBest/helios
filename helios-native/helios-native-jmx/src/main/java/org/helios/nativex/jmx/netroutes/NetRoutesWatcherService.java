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
package org.helios.nativex.jmx.netroutes;

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
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.NetRoute;

/**
 * <p>Title: NetRoutesWatcherService</p>
 * <p>Description: Service to poll for new NetRoutes and publish NetRouteInstanceService instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.netroutes.NetRoutesWatcherService</code></p>
 */
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating a new NetRoute has been added", types={
                @JMXNotificationType(type=NetRoutesWatcherService.ROUTE_ADD)
        }),
        @JMXNotification(description="Notification indicating a new NetRoute has been deleted", types={
                @JMXNotificationType(type=NetRoutesWatcherService.ROUTE_DELETE)
        })
})
public class NetRoutesWatcherService extends AbstractNativeCounter {
	
	/**  */
	private static final long serialVersionUID = -4697071947084525498L;
	/** A map of the current NetRouteInstanceService keyed by the NetRouteKey */
	protected final Map<NetRouteKey, NetRouteInstanceService> netRoutes = new ConcurrentHashMap<NetRouteKey, NetRouteInstanceService>();
	
	/** Notification for Added Routes */
	public static final String ROUTE_ADD = "org.helios.nativex.jmx.netroutes.NetRoute.Added";
	/** Notification for Deleted Routes */
	public static final String ROUTE_DELETE = "org.helios.nativex.jmx.netroutes.NetRoute.Deleted";
	
	/**
	 * Creates a new NetRoutesWatcherService
	 */
	public NetRoutesWatcherService() {
		super();
		run();
		scheduleSampling();
		this.registerCounterMBean("service", "NetRouteWatchService");
	}
	
	/**
	 * Bootstraps this service
	 */
	public static void boot() {
		new NetRoutesWatcherService();
	}
	
	/**
	 * Invokes a net route refresh
	 */
	public void run() {
		long time = System.currentTimeMillis();
		for(NetRoute route: HeliosSigar.getInstance().getNetRouteList()) {
			NetRouteKey nrk = new NetRouteKey(route);
			NetRouteInstanceService nr = netRoutes.get(nrk);
			if(nr==null) {
				nr = new NetRouteInstanceService(route, nrk);
				netRoutes.put(nrk, nr);
				sendNotification(new Notification(ROUTE_ADD, nrk.getObjectName(), this.nextNotificationSequence(), time, "Added Route " + nrk.toString()));
			} else {
				nr.setNetRoute(route);
			}
			// refresh the gatherer 
			// and sets the scanned flag to true
			nr.run();
		}
		
		// find NetRoutes with false scanned and unregister
		// set all scanneds to false
		Set<NetRouteKey> removes = new HashSet<NetRouteKey>();
		for(Map.Entry<NetRouteKey, NetRouteInstanceService> route: netRoutes.entrySet()) {
			if(!route.getValue().isScanned()) {
				log.info("Removing Defunct NetRoute [" + route.getKey().getObjectName() + "]");				
				try { 
					JMXHelper.getHeliosMBeanServer().unregisterMBean(route.getKey().getObjectName()); 
					removes.add(route.getKey());
				} catch (Exception e) {}
			}
		}		
		for(NetRouteKey key: removes) {
			netRoutes.remove(key);
			sendNotification(new Notification(ROUTE_DELETE, key.getObjectName(), this.nextNotificationSequence(), time, "Deleted Route " + key.toString()));
		}
		for(NetRouteInstanceService route: netRoutes.values()) {
			route.resetScan();
		}		
	}
	
}
