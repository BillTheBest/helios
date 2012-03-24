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
package org.helios.esper.engine;

import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;

/**
 * <p>Title: AgentNameCatalog</p>
 * <p>Description: A real time catalog of connected agents. Agents not touching in the time out period will be aged out.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.AgentNameCatalog</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating that an agent has connected", types=
                @JMXNotificationType(type=AgentNameCatalog.AGENT_CONNECTED_NOTIFICATION_TYPE)
        ),
        @JMXNotification(description="Notification indicating that an agent has disconnected", types=
                @JMXNotificationType(type=AgentNameCatalog.AGENT_DISCONNECTED_NOTIFICATION_TYPE)
        )               
})

public class AgentNameCatalog extends ManagedObjectDynamicMBean implements ListenerRegistration {

	/**  */
	private static final long serialVersionUID = 6011538777850841622L;
	
	public static final String AGENT_CONNECTED_NOTIFICATION_TYPE = "org.helios.server.engine.agent.connect";
	public static final String AGENT_DISCONNECTED_NOTIFICATION_TYPE = "org.helios.server.engine.agent.disconnect";

	/**
	 * @return
	 * @see org.helios.esper.engine.ListenerRegistration#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 * @see org.helios.esper.engine.ListenerRegistration#getTargetStatements()
	 */
	@Override
	public String[] getTargetStatements() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void update(EventBean[] arg0, EventBean[] arg1, EPStatement arg2,
			EPServiceProvider arg3) {
		// TODO Auto-generated method stub
		
	}

}
