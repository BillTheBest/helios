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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;

import org.helios.helpers.JMXHelper;
import org.hyperic.sigar.Who;

/**
 * <p>Title: WhoKey</p>
 * <p>Description: A logical key for a who</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.who.WhoKey</code></p>
 */

public class WhoKey {
	/** The user name */
	protected final String user;
	/** The context device of the login  */
	protected final String device;
	/** The JMX ObjectName for this who */
	protected final ObjectName objectName;	
	/** The scan flag */
	protected final AtomicBoolean scanned = new AtomicBoolean(false);
	
	public static final String PACKAGE = WhoKey.class.getPackage().getName();
	
	/**
	 * Creates a new WhoKey
	 * @param who The who representing this user login
	 */
	public WhoKey(Who who) {
		user = who.getUser();
		device = who.getDevice();
		objectName = JMXHelper.objectName(PACKAGE, "user", user, "device", device);
	}
	
	/**
	 * The user name
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	
	/**
	 * The context device for the login
	 * @return the device
	 */
	public String getDevice() {
		return device;
	}
	
	
	/**
	 * Indicates if this who has been scanned this period
	 * @return the scanned
	 */
	public boolean isScanned() {
		return scanned.get();
	}
	
	/**
	 * Sets the scan status for this who for this period
	 * @param scanned true to set as scanned
	 */
	public void setScanned(boolean scanned) {
		this.scanned.set(scanned);
	}
	
	/**
	 * The ObjectName of the MBean representing this who
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
		result = prime * result + ((device == null) ? 0 : device.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		WhoKey other = (WhoKey) obj;
		if (device == null) {
			if (other.device != null)
				return false;
		} else if (!device.equals(other.device))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("WhoKey [")
	        .append(TAB).append("user = ").append(this.user)
	        .append(TAB).append("device = ").append(this.device)
	        .append(TAB).append("objectName = ").append(this.objectName)
	        .append(TAB).append("scanned = ").append(this.scanned)
	        .append("\n]");    
	    return retValue.toString();
	}
	
	
}
