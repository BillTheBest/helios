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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;

import org.helios.helpers.JMXHelper;
import org.hyperic.sigar.NetRoute;

/**
 * <p>Title: NetRouteKey</p>
 * <p>Description: Defines a unique key for a Net Route</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.netroutes.NetRouteKey</code></p>
 */

public class NetRouteKey {
	/** The name of the network interface */
	protected final String netInterface;
	/** The destination network */
	protected final String destination;
	/** The network gateway */
	protected final String gateway;
	/** The network mask */
	protected final String mask;
	/** The derrived ObjectName */
	protected final ObjectName objectName;
	/** Scanning marker */
	protected final AtomicBoolean scanned = new AtomicBoolean(false);

	/**
	 * Creates a new NetRouteKey
	 * @param netRoute The Sigar gatherer for the Net Route this NetRouteKey represents.
	 */
	public NetRouteKey(NetRoute netRoute) {
		this.netInterface = netRoute.getIfname();
		this.destination = netRoute.getDestination();
		this.gateway = netRoute.getGateway();
		this.mask = netRoute.getMask();
		this.objectName = JMXHelper.objectName(getClass().getPackage().getName(), "service", "NetRoute", "ifname", netInterface, "destination", destination, "gateway", gateway, "mask", mask);
	}

	/**
	 * The name of the network interface
	 * @return the netInterface
	 */
	public String getNetInterface() {
		return netInterface;
	}

	/**
	 * The destination network
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * The network gateway
	 * @return the gateway
	 */
	public String getGateway() {
		return gateway;
	}

	/**
	 * The network net mask
	 * @return the mask
	 */
	public String getMask() {
		return mask;
	}

	/**
	 * Indicates if this net route has been scanned this period
	 * @return the scanned
	 */
	public boolean isScanned() {
		return scanned.get();
	}
	
	/**
	 * Sets the scan status for this net route for this period
	 * @param scanned true to set as scanned
	 */
	public void setScanned(boolean scanned) {
		this.scanned.set(scanned);
	}
	
	/**
	 * The ObjectName of the MBean representing this NetRoute
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}
	

	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((destination == null) ? 0 : destination.hashCode());
		result = prime * result
				+ ((gateway == null) ? 0 : gateway.hashCode());
		result = prime * result + ((mask == null) ? 0 : mask.hashCode());
		result = prime * result
				+ ((netInterface == null) ? 0 : netInterface.hashCode());
		return result;
	}

	/**
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NetRouteKey other = (NetRouteKey) obj;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (gateway == null) {
			if (other.gateway != null)
				return false;
		} else if (!gateway.equals(other.gateway))
			return false;
		if (mask == null) {
			if (other.mask != null)
				return false;
		} else if (!mask.equals(other.mask))
			return false;
		if (netInterface == null) {
			if (other.netInterface != null)
				return false;
		} else if (!netInterface.equals(other.netInterface))
			return false;
		return true;
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("NetRouteKey [")
	        .append(TAB).append("netInterface = ").append(this.netInterface)
	        .append(TAB).append("destination = ").append(this.destination)
	        .append(TAB).append("gateway = ").append(this.gateway)
	        .append(TAB).append("mask = ").append(this.mask)
	        .append(TAB).append("objectName = ").append(this.objectName)
	        .append(TAB).append("scanned = ").append(this.scanned)
	        .append("\n]");    
	    return retValue.toString();
	}


}
