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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import org.apache.log4j.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * <p>Title: DeliveryListener</p>
 * <p>Description: A queue traffic logger.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.amqp.DeliveryListener</p></code>
 */
public class  DeliveryListener implements Consumer {
	protected final String key;
	protected final Logger log;
	/**
	 * @param key
	 */
	public DeliveryListener(String key) {
		this.key = key;
		this.log = Logger.getLogger(getClass().getName() + "." + key);
	}

	@Override
	public void handleCancelOk(String consumerTag) {
		log.info("handleCancelOk(" + consumerTag + ")");		
	}

	@Override
	public void handleConsumeOk(String consumerTag) {
		log.info("handleConsumeOk(" + consumerTag + ")");		
	}

	@Override
	public void handleDelivery(java.lang.String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
		String mimeType = properties.getContentType();
		Object obj = null;
		if(JavaSerializer.MIME_TYPE.equals(mimeType)) {
			obj = deserialize(body);
		} else if(StringSerializer.MIME_TYPE.equals(mimeType)) {
			obj = new String(body);			
		} else {
			obj = "Unknown ContentType [" + mimeType + "]";
		}
			
			//deserialize(body);
		log.info("Delivery [" + consumerTag + "]:" + obj);
	}

	@Override
	public void handleRecoverOk() {
		log.info("handleRecoverOk()");
		
	}

	@Override
	public void handleShutdownSignal(String arg0, ShutdownSignalException arg1) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Serializes an object to a byte array.
	 * @param obj The object to serialize
	 * @return a byet array
	 */
	public Object deserialize(byte[] bytes)   {
		try {
			if(bytes==null || bytes.length<1) return null;
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (Exception e) {
			log.error("Failed to deserialize object", e);
			throw new RuntimeException("Failed to deserialize object", e);
		}
	}
		
	
}
