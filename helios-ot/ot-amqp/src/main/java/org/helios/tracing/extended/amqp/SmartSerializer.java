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
package org.helios.tracing.extended.amqp;

import java.io.IOException;

/**
 * <p>Title: SmartSerializer</p>
 * <p>Description: Object serializer that inspects the passed object and delegates to the appropriate serializer to perform the serialization.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.amqp.SmartSerializer</p></code>
 */
public class SmartSerializer implements IDeliverySerializer {
	/** A Java serializer  */
	public static final JavaSerializer jSerializer = new JavaSerializer();
	/** A String serializer */
	public static final StringSerializer sSerializer = new StringSerializer();
	/** A thread local to hold the mime type of the last serialization */
	private static final ThreadLocal<String> lastMimeType = new ThreadLocal<String>();	
	/** An empty byte array */
	public static final byte[] NULL = new byte[0];

	/**
	 * Serializes a java object to a byte array. If the passed object is null, returns a zero length byte array.
	 * @param obj The object to serialize.
	 * @return a byte array.
	 * @throws IOException
	 */
	public byte[] serialize(Object obj) throws IOException {
		if(obj==null) {
			lastMimeType.set("null");
			return NULL; 
		} else if(obj instanceof CharSequence) {
			lastMimeType.set(sSerializer.getMimeType());
			return sSerializer.serialize(obj);
		}  else {
			lastMimeType.set(jSerializer.getMimeType());
			return jSerializer.serialize(obj);			
		}
	}
	
	/**
	 * Returns the MIME type of the most recent serialized byte arrays produced by this serializer for the current thread.
	 * @return the MIME type of the most recent serialized byte arrays produced by this serializer for the current thread.
	 */
	public String getMimeType() {
		return lastMimeType.get();
	}
}
