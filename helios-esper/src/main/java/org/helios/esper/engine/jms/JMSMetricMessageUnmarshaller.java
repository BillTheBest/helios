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
package org.helios.esper.engine.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import com.espertech.esper.client.EPException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esperio.jms.JMSMessageUnmarshaller;

/**
 * <p>Title: JMSMetricMessageUnmarshaller</p>
 * <p>Description: ObjectMessage unmarshaller.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.esper.engine.jms.JMSMetricMessageUnmarshaller</code></p>
 */
public class JMSMetricMessageUnmarshaller implements JMSMessageUnmarshaller {

	/**
	 * {@inheritDoc}
	 * @see com.espertech.esperio.jms.JMSMessageUnmarshaller#unmarshal(com.espertech.esper.event.EventAdapterService, javax.jms.Message)
	 */
	@Override
	public Object unmarshal(EventAdapterService eventService, Message message) throws EPException {
		
		if(message instanceof ObjectMessage) {
			try {
				return ((ObjectMessage)message).getObject();
			} catch (JMSException jme) {
				throw new EPException("Failed to read object from message", jme);
			}
		}
		return null;
	}

}
