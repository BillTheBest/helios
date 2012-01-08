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
package org.helios.collectors.jmxproxies.protocol.hsp;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;

/**
 * <p>Title: LHSPJMXConnector</p>
 * <p>Description: A JMXConnector facade that provides MBeanServerConnections to in-vm MBeanServers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.collectors.jmxproxies.protocol.hsp.LHSPJMXConnector</code></p>
 */

public class LHSPJMXConnector implements JMXConnector {
	/** The in-vm MBeanServer this facade is being created for */
	protected final MBeanServerConnection server;
	/** The MBeanServer's default domain */
	protected final String defaultDomain;
	/** The MBeanServer's ID */
	protected final String id;
	
	/** A serial number generator to create faux connection Ids */
	protected static final AtomicLong serial = new AtomicLong();
	
	/**
	 * Creates a new LHSPJMXConnector
	 * @param defaultDomain The default domain of the in-vm MBeanServer this facade is being created for
	 */
	public LHSPJMXConnector(String defaultDomain) {
		this.defaultDomain = defaultDomain;
		this.server = getMBeanServer(defaultDomain);
		try {			
			id = (String)server.getAttribute(MBeanServerDelegate.DELEGATE_NAME, "MBeanServerId");		
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire MBeanServerId", e);
		}		
	}
	
	/**
	 * Creates a new LHSPJMXConnector
	 * @param server The in-vm MBeanServer this facade is being created for
	 */
	public LHSPJMXConnector(MBeanServer server) {
		this.server = server;
		try {			
			id = (String)server.getAttribute(MBeanServerDelegate.DELEGATE_NAME, "MBeanServerId");		
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire MBeanServerId", e);
		}		
		try {			
			defaultDomain = this.server.getDefaultDomain();		
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire DefaultDomain", e);
		}		
		
	}
	
	/**
	 * Gets the in-vm MBeanServer for the passed DefaultDomain
	 * @param defaultDomain 
	 * @return
	 */
	protected static MBeanServer getMBeanServer(String defaultDomain) {
		for(MBeanServer mbs : MBeanServerFactory.findMBeanServer(null)) {
			if(mbs.getDefaultDomain().equals(defaultDomain)) {
				return  mbs;
			}
		}
		throw new RuntimeException("No MBeanServer found for DefaultDomain [" + defaultDomain + "]");
	}
	
	/**
	 * Adds a listener to be informed of changes in connection status, not supported yet
	 * @param listener
	 * @param filter
	 * @param handback
	 */
	@Override
	public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		throw new UnsupportedOperationException("Connection listeners not yet implemented in the HSP protocol");
	}

	/**
	 * If connection is not shared, returns the connection to the pool
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {			

	}
	

	/**
	 * No Op
	 * @throws IOException
	 */
	@Override
	public void connect() throws IOException {			
	}

	/**
	 * No Op
	 * @param environment
	 * @throws IOException
	 */
	@Override
	public void connect(Map<String, ?> environment) throws IOException {			
	}

	/**
	 * Returns a connection Id
	 * @return
	 * @throws IOException
	 */
	@Override
	public String getConnectionId() throws IOException {
		return new StringBuilder(id).append("/").append(defaultDomain).append("-#").append(serial.incrementAndGet()).toString();
	}
	

	/**
	 * Returns an MBeanServerConnection
	 * @return
	 * @throws IOException
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection() throws IOException {
		return server;
	}

	/**
	 * Returns an MBeanServerConnection, not supported yet
	 * @param subject
	 * @return
	 * @throws IOException
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection(Subject subject) throws IOException {
		throw new UnsupportedOperationException("Subject based connections not yet implemented in the HSP protocol");
	}

	/**
	 * Removes a registered connection listener, not supported yet.
	 * 
	 * @param listener
	 * @throws ListenerNotFoundException
	 */
	@Override
	public void removeConnectionNotificationListener( NotificationListener listener) throws ListenerNotFoundException {
		throw new UnsupportedOperationException("Connection listeners not yet implemented in the HSP protocol");			
	}

	/**
	 * Removes a registered connection listener, not supported yet.
	 * @param listener
	 * @param filter
	 * @param handback
	 * @throws ListenerNotFoundException
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		throw new UnsupportedOperationException("Connection listeners not yet implemented in the HSP protocol");
		
	}
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("LHSPJMXConnector [")
	        .append(TAB).append("defaultDomain = ").append(this.defaultDomain)
	        .append(TAB).append("id = ").append(this.id)
	        .append("\n]");    
	    return retValue.toString();
	}


}
