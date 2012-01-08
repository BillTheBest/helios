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

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.hyperic.sigar.NetRoute;

/**
 * <p>Title: NetRouteInstanceService</p>
 * <p>Description: MBean that represents one active Net Route</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.netroutes.NetRouteInstanceService</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class NetRouteInstanceService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = -8245306026948286894L;

	/** The NetRoute gatherer */
	protected NetRoute netRoute;
	/** The NetRoute Key */
	protected final NetRouteKey netRouteKey;
	
	/** NetRoute Mask */
	protected String mask = null;
	/** NetRoute Window */
	protected final LongRollingCounter windowValue = new LongRollingCounter("NetRouteWindowValue", 1);
	/** NetRoute Destination */
	protected String destination = null;
	/** NetRoute Flags */
	protected final LongRollingCounter flagsValue = new LongRollingCounter("NetRouteFlagsValue", 1);
	/** NetRoute Gateway */
	protected String gateway = null;
	/** NetRoute Refcnt */
	protected final LongRollingCounter refcntValue = new LongRollingCounter("NetRouteRefcntValue", 1);
	/** NetRoute Use */
	protected final LongRollingCounter useValue = new LongRollingCounter("NetRouteUseValue", 1);
	/** NetRoute Metric */
	protected final LongRollingCounter metricValue = new LongRollingCounter("NetRouteMetricValue", 1);
	/** NetRoute Mtu */
	protected final LongRollingCounter mtuValue = new LongRollingCounter("NetRouteMtuValue", 1);
	/** NetRoute Irtt */
	protected final LongRollingCounter irttValue = new LongRollingCounter("NetRouteIrttValue", 1);
	/** NetRoute Ifname */
	protected String ifname = null;
	
	/**
	 * Creates a new NetRouteInstanceService
	 * @param netRoute The NetRoute gatherer for this NetRoute
	 */
	public NetRouteInstanceService(NetRoute netRoute, final NetRouteKey netRouteKey) {
		super();
		this.netRoute = netRoute;
		this.netRouteKey = netRouteKey;
		mask = netRoute.getMask();
		destination = netRoute.getDestination();
		gateway = netRoute.getGateway();
		ifname = netRoute.getIfname();
		run();
		registerCounterMBean(netRouteKey.getObjectName());
	}
	
	/**
	 * Updates the counter values for this NetRoute
	 */
	public void run() {		
		//try { netRoute.gather(sigar); } catch (Throwable e) {}
		windowValue.put(netRoute.getWindow());		
		flagsValue.put(netRoute.getFlags());		
		refcntValue.put(netRoute.getRefcnt());
		useValue.put(netRoute.getUse());
		metricValue.put(netRoute.getMetric());
		mtuValue.put(netRoute.getMtu());
		irttValue.put(netRoute.getIrtt());				
		netRouteKey.setScanned(true);
	}
	
	/**
	 * Resets the interval scan flag.
	 */
	public void resetScan() {
		netRouteKey.setScanned(false);
	}
	
	/**
	 * Determines if this instance has been scanned this period
	 * @return true if this instance has been scanned this period
	 */
	public boolean isScanned() {
		return netRouteKey.isScanned();
	}
	
	/**
	 * Returns the NetRoute Mask 
	 * @return the NetRoute Mask
	 */
	@JMXAttribute(name="Mask", description="The NetRoute Mask", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getMask() {
		return mask;
	}

	/**
	 * Returns the NetRoute Window 
	 * @return the NetRoute Window
	 */
	@JMXAttribute(name="Window", description="The NetRoute Window", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getWindow() {
		return windowValue.getLastValue();
	}

	/**
	 * Returns the NetRoute Destination 
	 * @return the NetRoute Destination
	 */
	@JMXAttribute(name="Destination", description="The NetRoute Destination", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDestination() {
		return destination;
	}

	/**
	 * Returns the NetRoute Flags 
	 * @return the NetRoute Flags
	 */
	@JMXAttribute(name="Flags", description="The NetRoute Flags", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFlags() {
		return flagsValue.getLastValue();
	}

	/**
	 * Returns the NetRoute Gateway 
	 * @return the NetRoute Gateway
	 */
	@JMXAttribute(name="Gateway", description="The NetRoute Gateway", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getGateway() {
		return gateway;
	}

	/**
	 * Returns the NetRoute Refcnt 
	 * @return the NetRoute Refcnt
	 */
	@JMXAttribute(name="Refcnt", description="The NetRoute Refcnt", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRefcnt() {
		return refcntValue.getLastValue();
	}

	/**
	 * Returns the NetRoute Use 
	 * @return the NetRoute Use
	 */
	@JMXAttribute(name="Use", description="The NetRoute Use", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getUse() {
		return useValue.getLastValue();
	}

	/**
	 * Returns the NetRoute Metric 
	 * @return the NetRoute Metric
	 */
	@JMXAttribute(name="Metric", description="The NetRoute Metric", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMetric() {
		return metricValue.getLastValue();
	}

	/**
	 * Returns the NetRoute Mtu 
	 * @return the NetRoute Mtu
	 */
	@JMXAttribute(name="Mtu", description="The NetRoute Mtu", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMtu() {
		return mtuValue.getLastValue();
	}

	/**
	 * Returns the NetRoute Irtt 
	 * @return the NetRoute Irtt
	 */
	@JMXAttribute(name="Irtt", description="The NetRoute Irtt", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getIrtt() {
		return irttValue.getLastValue();
	}

	/**
	 * Returns the NetRoute Ifname 
	 * @return the NetRoute Ifname
	 */
	@JMXAttribute(name="Ifname", description="The NetRoute Ifname", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getIfname() {
		return ifname;
	}

	/**
	 * @param netRoute the netRoute to set
	 */
	public void setNetRoute(NetRoute netRoute) {
		this.netRoute = netRoute;
	}

	
}
