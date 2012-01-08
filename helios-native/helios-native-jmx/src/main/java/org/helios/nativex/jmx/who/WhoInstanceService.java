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

import java.util.Date;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.hyperic.sigar.Who;

/**
 * <p>Title: WhoInstanceService</p>
 * <p>Description: Native monitor service to track users logged into this system</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.who.WhoInstanceService</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class WhoInstanceService extends AbstractNativeCounter {
	/** The user logged in */
	protected final String user;
	/** The device context of the login */
	protected final String device;
	/** The host the user is logged into */
	protected final String host;
	/** The login time stamp */
	protected final long time;
	/** The login date */
	protected final Date date;
	/** The who instance key */
	protected final WhoKey whoKey;
	/**
	 * Creates a new WhoInstanceService
	 * @param who The who gatherer
	 */
	public WhoInstanceService(Who who, WhoKey key) {
		whoKey = key;
		user = who.getUser();
		device = who.getDevice();
		host = who.getHost();
		time = who.getTime();
		date = new Date(time);
		objectName = key.getObjectName();
		registerCounterMBean(key.getObjectName());
	}
	
	/**
	 * The user name logged in
	 * @return the user
	 */
	@JMXAttribute(name="User", description="The user name logged in", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getUser() {
		return user;
	}
	
	/**
	 * The device context of the login
	 * @return the device
	 */
	@JMXAttribute(name="Device", description="The device context of the login", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDevice() {
		return device;
	}
	
	/**
	 * The host the user is logged into
	 * @return the host
	 */
	@JMXAttribute(name="Host", description="The host the user is logged into", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getHost() {
		return host;
	}
	
	/**
	 * The timestamp of the user login
	 * @return the time
	 */
	@JMXAttribute(name="Time", description="The timestamp of the user login", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTime() {
		return time;
	}
	
	/**
	 * The date of the user login
	 * @return the date
	 */
	@JMXAttribute(name="Date", description="The date of the user login", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getDate() {
		return date;
	}
	
	/**
	 * Resets the interval scan flag.
	 */
	public void resetScan() {
		whoKey.setScanned(false);
	}
	
	/**
	 * Determines if this instance has been scanned this period
	 * @return true if this instance has been scanned this period
	 */
	public boolean isScanned() {
		return whoKey.isScanned();
	}
	
	/**
	 * Sets the scanned flag
	 * @param scanned true to mark as scanned.
	 */
	public void setScanned(boolean scanned) {
		whoKey.setScanned(scanned);
	}

}
