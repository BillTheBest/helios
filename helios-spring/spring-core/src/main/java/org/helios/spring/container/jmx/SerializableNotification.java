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
package org.helios.spring.container.jmx;

import java.io.ObjectStreamException;

import javax.management.Notification;

import org.helios.reflection.PrivateAccessor;

/**
 * <p>Title: SerializableNotification</p>
 * <p>Description: An extension of a standard JMX notification that rewrites the user data if the notiifcation is serialized.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.spring.container.jmx.SerializableNotification</code></p>
 */

public class SerializableNotification extends Notification {

	/**
	 * Rewrites the SerializableNotification as a standard JMX Notification
	 * @return a Notification
	 * @throws ObjectStreamException Thrown when the SerializableNotification fails to be replaced and serialized
	 */
	protected Object writeReplace() throws ObjectStreamException {
		Object source = null;
		Object newSource = getSource(); 
		if(newSource!=null)
		try {
			source = PrivateAccessor.invoke(newSource, "getObjectName");
		} catch (Exception e) {
			source = newSource.toString();
		}
		Notification notif = new Notification(this.getType(), source, this.getSequenceNumber(), this.getTimeStamp(), this.getMessage());
		if(getUserData()!=null) {
			notif.setUserData(getUserData().toString());
		}		
		return notif;
	}

	
	/**
	 * Creates a new SerializableNotification
	 * @param type The JMX notification type
	 * @param source The notification source
	 * @param sequenceNumber The notification sequence number
	 */
	public SerializableNotification(String type, Object source, long sequenceNumber) {
		super(type, source, sequenceNumber);
	}

	/**
	 * Creates a new SerializableNotification
	 * @param type The JMX notification type
	 * @param source The notification source
	 * @param sequenceNumber The notification sequence number
	 * @param message The message
	 */
	public SerializableNotification(String type, Object source, long sequenceNumber, String message) {
		super(type, source, sequenceNumber, message);
	}

	/**
	 * Creates a new SerializableNotification
	 * @param type The JMX notification type
	 * @param source The notification source
	 * @param sequenceNumber The notification sequence number
	 * @param timeStamp The timestamp of the notification
	 */
	public SerializableNotification(String type, Object source, long sequenceNumber, long timeStamp) {
		super(type, source, sequenceNumber, timeStamp);
	}

	/**
	 * Creates a new SerializableNotification
	 * @param type The JMX notification type
	 * @param source The notification source
	 * @param sequenceNumber The notification sequence number
	 * @param timeStamp The timestamp of the notification
	 * @param message The message
	 */
	public SerializableNotification(String type, Object source, long sequenceNumber, long timeStamp, String message) {
		super(type, source, sequenceNumber, timeStamp, message);
	}

}
