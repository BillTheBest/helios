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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * <p>Title: JavaSerializer</p>
 * <p>Description: Object serializer that uses simple Java serialization to produce a byte array.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.amqp.JavaSerializer</p></code>
 */
public class JavaSerializer implements IDeliverySerializer {
	/** The content type of the byte array returned by this serializer  */
	public static final String MIME_TYPE = "application/octet-stream";
	/**
	 * Serializes a java object to a byte array. If the passed object is null, returns a zero length byte array.
	 * @param obj The object to serialize.
	 * @return a byte array.
	 * @throws IOException
	 */
	public byte[] serialize(Object obj) throws IOException {
		if(obj==null) return new byte[0];
		if(obj instanceof byte[]) return (byte[]) obj;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);
		oos.flush();
		baos.flush();
		return baos.toByteArray();		
	}
	
	/**
	 * Returns the MIME type of the serialized byte arrays produced by this serializer.
	 * @return the MIME type of the serialized byte arrays produced by this serializer.
	 */
	public String getMimeType() {
		return MIME_TYPE;
	}
}
