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
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;

import org.apache.log4j.Logger;

/**
 * <p>Title: HSPJMXConnector</p>
 * <p>Description: An internal JMXConnector to a Helios MBeanServerConnection MBean Proxy</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.collectors.jmxproxies.protocol.hsp.ClientProvider.HSPJMXConnector</code></p>
 */
public class HSPJMXConnector implements JMXConnector {
	/** The MBeanServer where the connection proxy are registered */
	protected final MBeanServerConnection proxyConnection;
	/** The ObjectName of the connection proxy MBean */
	protected final ObjectName proxyObjectName;
	/** The effective proxied MBeanServerConnection */
	protected MBeanServerConnection effectiveConnection;
	/** Environment options (future) */
	protected final Map<String, Object> environment;
	/** Indicates if this connector is allocated a shared or dedicated pooled connection */
	protected final boolean sharedConnection;
	/** An assigned connection Id */
	protected final String connectionId;
	/** Instance Logger */
	protected final Logger log;
	/** Null Args */
	public static final Object[] NULL_ARGS = {};
	/** Null Signature */
	public static final String[] NULL_SIGNATURE = {};
	/** MBeanServerConnection Args */
	public static final Object[] CONN_ARGS = new MBeanServerConnection[1];		
	/** MBeanServerConnection Signature */
	public static final String[] CONN_SIGNATURE = {MBeanServerConnection.class.getName()};
	/** Serial factory for shared connection Ids */
	protected static final AtomicLong serial = new AtomicLong(0);
	
	/**
	 * Creates a new HSPJMXConnector
	 * @param proxyConnection
	 * @param proxyObjectName
	 * @param environment
	 * @param sharedConnection
	 */
	public HSPJMXConnector(MBeanServerConnection proxyConnection, ObjectName proxyObjectName, Map<String, Object> environment, boolean sharedConnection) {
		log = Logger.getLogger(getClass());
		this.proxyConnection = proxyConnection;
		this.proxyObjectName = proxyObjectName;
		this.sharedConnection = sharedConnection;
		this.environment = environment;
		if(this.sharedConnection) {
			effectiveConnection = (MBeanServerConnection) MBeanServerInvocationHandler.newProxyInstance(proxyConnection, proxyObjectName, MBeanServerConnection.class, true);
			connectionId = "Shared ProxiedConnection[" + proxyObjectName + "]#" + System.identityHashCode(effectiveConnection); 
		} else {
			try {
				effectiveConnection = (MBeanServerConnection) proxyConnection.invoke(proxyObjectName, "getPooledConnection", NULL_ARGS, NULL_SIGNATURE);
				connectionId = "Dedicated ProxiedConnection[" + proxyObjectName + "]#" + serial.incrementAndGet();
			} catch (Exception e) {
				throw new RuntimeException("Failed to acquire non-shared connection from [" + proxyObjectName + "]", e);
			}
		}
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
		if(!sharedConnection) {
			try {
				CONN_ARGS[0] = effectiveConnection;
				proxyConnection.invoke(proxyObjectName, "returnPooledConnection", CONN_ARGS, CONN_SIGNATURE);
			} catch (Exception e) {
				log.warn("Failed to return pooled connection from [" + proxyObjectName + "]", e);
			} finally {
				effectiveConnection = null;
			}
			
		}
	}
	
	/**
	 * Closes an unclosed and unshared connection on finalization
	 * @throws Throwable
	 */
	public void finalize() throws Throwable {
		if(effectiveConnection != null && !sharedConnection) {
			try { close(); } catch (Throwable e) {}
		}
		try { super.finalize(); } catch (Throwable e) {}
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
		return connectionId;
	}
	
	/**
	 * Returns the connection's connectionId
	 * @return the connection's connectionId
	 */
	public String toString() {
		return connectionId;
	}

	/**
	 * Returns an MBeanServerConnection
	 * @return
	 * @throws IOException
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection() throws IOException {
		return effectiveConnection;
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
	
}
