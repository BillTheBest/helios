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
package org.helios.ot.agent;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;

/**
 * <p>Title: AbstractHeliosOTClientImpl</p>
 * <p>Description: Base abstract OT client.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.AbstractHeliosOTClientImpl</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating exception during collect callback for any collector", types={
                @JMXNotificationType(type="org.helios.collectors.exception.notification")
        }),
        @JMXNotification(description="Notification indicating change in CollectorState", types={
                @JMXNotificationType(type="org.helios.collectors.AbstractCollector.CollectorState")
        })       
})

public abstract class AbstractHeliosOTClientImpl implements HeliosOTClient {
	/** Connectivity flag */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** Registered client listeners */
	protected final Set<HeliosOTClientEventListener> listeners = new CopyOnWriteArraySet<HeliosOTClientEventListener>();
	/** The connection endpoint URI */
	protected URI uri = null;
	/** The connection timeout */
	protected long connectTimeout = -1L;
	/** The operation timeout */
	protected long operationTimeout = -1L;

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#connect()
	 */
	@Override
	public void connect() {
		

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#connect(boolean, org.helios.ot.agent.HeliosOTClientEventListener[])
	 */
	public void connect(boolean asynch, HeliosOTClientEventListener...listeners) {
		if(listeners!=null && listeners.length>0) {
			for(HeliosOTClientEventListener listener: listeners) {
				addListener(listener);
			}
		}
		if(!asynch) {
			connect();
		}
	}
	
	/**
	 * Concrete impl. connect.
	 * When called, if no exception is thrown, the client is assumed to be connected
	 * @throws Exception
	 */
	protected abstract void doConnect() throws Exception;

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#disconnect()
	 */
	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#isConnected()
	 */
	@Override
	public boolean isConnected() {		
		return connected.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#getConnectionURI()
	 */
	@Override
	public URI getConnectionURI() {
		return uri;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#addListener(org.helios.ot.agent.HeliosOTClientEventListener)
	 */
	@Override
	public void addListener(HeliosOTClientEventListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#removeListener(org.helios.ot.agent.HeliosOTClientEventListener)
	 */
	@Override
	public void removeListener(HeliosOTClientEventListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#getConnectTimeout()
	 */
	public long getConnectTimeout() {
		return connectTimeout;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#setConnectTimeout(long)
	 */
	public void setConnectTimeout(long timeout) {
		connectTimeout = timeout;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#getOperationTimeout()
	 */
	public long getOperationTimeout() {
		return operationTimeout;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#setOperationTimeout(long)
	 */
	public void setOperationTimeout(long timeout) {
		this.operationTimeout = timeout;
	}
		
	
	/**
	 * Emits a client connected event to all registered listeners
	 */
	protected void fireOnConnect() {
		for(HeliosOTClientEventListener listener: listeners) {
			listener.onConnect(this);
		}
	}
	
	/**
	 * Emits a client disconnected event to all registered listeners
	 * @param cause The cause of the disconnect, or null if a disconnect was requested
	 */
	protected void fireOnDisconnect(Throwable cause) {
		for(HeliosOTClientEventListener listener: listeners) {
			listener.onDisconnect(this, cause);
		}
	}
	

}
