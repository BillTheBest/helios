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

import java.io.Serializable;
import java.net.URI;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;

import org.apache.log4j.Logger;
import org.helios.helpers.StringHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.agent.jmx.NettyLoggerController;
import org.helios.ot.agent.protocol.impl.ClientProtocolOperation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation;
import org.helios.ot.trace.MetricId;
import org.helios.ot.trace.Trace;

/**
 * <p>Title: AbstractHeliosOTClientImpl</p>
 * <p>Description: Base abstract OT client.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.AbstractHeliosOTClientImpl</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
@JMXNotifications(notifications={
        @JMXNotification(description="Client connected notification", types={
                @JMXNotificationType(type=HeliosOTClient.NOTIFICATION_CONNECT)
        }),
        @JMXNotification(description="Client disconnected notification", types={
                @JMXNotificationType(type=HeliosOTClient.NOTIFICATION_DISCONNECT)
        })
})
public abstract class AbstractHeliosOTClientImpl extends ManagedObjectDynamicMBean implements HeliosOTClient, HeliosOTClientEventListener {
	/**  */
	private static final long serialVersionUID = 1739421438427185681L;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** Logger controller */
	protected final NettyLoggerController logController = new NettyLoggerController(log);
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
	/** A random generator to use with ping ops */
	protected Random random = new Random(System.nanoTime());
	/** The failed remote op counter */
	protected final AtomicLong failedOpCounter = new AtomicLong(0);
	/** The succeeded remote op counter */
	protected final AtomicLong opCounter = new AtomicLong(0);
	/** This client's session ID */
	protected String sessionId = null;
	/** The connect timeout latch */
	protected CountDownLatch connectLatch = null;
	/** The parsed URI specified configuration parameters */
	protected final Properties uriParameters = new Properties();
	/** Flag to indicate if a disconnect was deliberate or not. Deliberate disconnects set the flag to true. */
	protected final AtomicBoolean deliberateDisconnect = new AtomicBoolean(false);

	/** The configuration name for the connect timeout */
	public static final String CONFIG_CONNECT_TIMEOUT = "connectTimeoutMillis";
	/** The configuration name for the operation timeout */
	public static final String CONFIG_OP_TIMEOUT = "operationTimeoutMillis";

	/**
	 * Creates a new AbstractHeliosOTClientImpl
	 */
	protected AbstractHeliosOTClientImpl() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#connect()
	 */
	@Override
	@JMXOperation(name="connect", description="Connects the client to the configured server")
	public void connect() {
		deliberateDisconnect.set(false);
		doConnect();

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#connect(boolean, org.helios.ot.agent.HeliosOTClientEventListener[])
	 */
	public void connect(boolean asynch, final HeliosOTClientEventListener...listeners) {
		deliberateDisconnect.set(false);
		if(!asynch) {
			// Since we're not connect asynch, we have to wait for the connect latch to drop
			connectLatch = new CountDownLatch(1);
			doConnect(listeners);
			try {
				if(!connectLatch.await(connectTimeout, TimeUnit.MILLISECONDS)) {
					throw new Exception("Timeout elapsed without a connection");
				}
			} catch (Exception e) {
				throw new RuntimeException("Connection for client [" + this + "] Timed out waiting for connection after [" + connectTimeout + "] ms.", e);
			}
		} else {
			// Asynch connect, so we don't care. If it fails, an asynch handler will take care of it.
			doConnect(listeners);
		}
	}
	
	/**
	 * Event callback when the client connects
	 * @param client The client that connected
	 */
	public void onConnect(HeliosOTClient client) {
		
	}
	/**
	 * Event callback when the client fails to connect
	 * @param client The client that connected
	 * @param cause The associated cause of the connection failure
	 */
	public void onConnectFailure(HeliosOTClient client, Throwable cause) {
		
	}
	
	/**
	 * Event callback when the client disconnects
	 * @param client The client that disconnected
	 * @param cause The cause of an unintended disconnect. Null if disconnect was requested.
	 */
	public void onDisconnect(HeliosOTClient client, Throwable cause) {
		
	}
	
	/**
	 * @param op
	 * @param payload
	 * @return
	 */
	protected HeliosProtocolInvocation invoke(ClientProtocolOperation op, Object payload) {			
		return doInvoke(op, payload);
	}	
	
	/**
	 * @param op
	 * @param payload
	 * @return
	 */
	protected abstract HeliosProtocolInvocation doInvoke(ClientProtocolOperation op, Object payload);

	
	/**
	 * Post connect handshake delivers the server assigned session ID and completes the connect cycle
	 * @param sessionId the server assigned session ID
	 */
	public void postConnectHandshake(String sessionId) {
		try {
			this.sessionId = sessionId;
			log.info("Connected to [" + uri + "] with session [" + sessionId + "]");
			if(connectLatch!=null) connectLatch.countDown();			
		} catch (Exception e) {
			//System.err.println("Failed to complete post connect handshake:" + e);
			doDisconnect();
			throw new RuntimeException("Failed to complete post connect handshake" , e);
			
		}
	}
	
	/**
	 * Concrete impl. connect.
	 * When called, if no exception is thrown, the client is assumed to be connected	 * 
	 * @param listeners The connection event only listeners
	 */
	protected abstract void doConnect(final HeliosOTClientEventListener...listeners);

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#disconnect()
	 */
	@Override
	@JMXOperation(name="disconnect", description="Disconnects the client to the configured server")
	public void disconnect() {
		// ====================
		//  Need to clean up connected resources
		// ====================
		connected.set(false);
		deliberateDisconnect.set(true);
		sessionId = null;
		doDisconnect();
	}
	
	/**
	 * Concrete impl. disconnect.
	 */
	protected abstract void doDisconnect();
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#purge()
	 */
	public void purge() {
		doPurge();
	}
	
	/**
	 * Disconnects and deallocates all resources associated with this client.
	 */
	protected abstract void doPurge();
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#ping()
	 */
	@Override
	public boolean ping() {
		return doPing();
	}
	
	/**
	 * The concrete client ping implementation
	 */
	protected abstract boolean doPing();

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#echo(java.io.Serializable)
	 */
	@Override
	public <T extends Serializable> T echo(T payload) {	
		return doEcho(payload);
	}
	
	/**
	 * The concrete client echo implementation
	 * @param payload The echo payload
	 * @return the echoed value from the server
	 */
	protected abstract <T extends Serializable> T doEcho(T payload);
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#submitTraces(org.helios.ot.trace.Trace[])
	 */
	@Override
	public void submitTraces(@SuppressWarnings("rawtypes") Trace[] traces) {
		if(traces!=null && traces.length>0) {
			doSubmitTraces(traces);
		}
		
	}
	
	/**
	 * The concrete client submit trace implementation
	 * @param traces The array of traces to submit
	 */
	protected abstract void doSubmitTraces(@SuppressWarnings("rawtypes") Trace[] traces);
	
	


	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#isConnected()
	 */
	@Override
	@JMXAttribute(name="Connected", description="Indicates if the client is connected", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean isConnected() {		
		return connected.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#getConnectionURI()
	 */
	@Override
	@JMXAttribute(name="ConnectionURI", description="The connection URI", mutability=AttributeMutabilityOption.READ_ONLY)
	public URI getConnectionURI() {
		return uri;
	}
	
	
	/**
	 * Sets the URI to use to establish a connection to the Helios OT Server
	 * @param uri the URI to use to establish a connection to the Helios OT Server
	 */
	public void configureClient(URI uri) {
		if(uri==null) throw new IllegalArgumentException("The passed uri was null", new Throwable());
		this.uri = uri;
		String paramString = this.uri.getQuery();
		if(paramString!=null && !paramString.isEmpty()) {
			for(String paramPair: paramString.split("&")) {
				String[] splitPair = paramPair.split("=");
				uriParameters.setProperty(splitPair[0], splitPair[1]);
			}
		}
		connectTimeout = Configuration.getLongConfigurationOption(CONFIG_CONNECT_TIMEOUT, Configuration.CONNECT_TIMEOUT,  Configuration.DEFAULT_CONNECT_TIMEOUT, uriParameters);
		operationTimeout = Configuration.getLongConfigurationOption(CONFIG_OP_TIMEOUT, Configuration.SYNCH_OP_TIMEOUT,  Configuration.DEFAULT_SYNCH_OP_TIMEOUT, uriParameters);
		doConfigureClient(this.uri);		
	}
	
	/**
	 * Executes client specific processing of the URI
	 * @param uri the URI to use to establish a connection to the Helios OT Server
	 */
	protected abstract void doConfigureClient(URI uri);

	/**
	 * Returns this client's session ID
	 * @return this client's session ID
	 */
	@JMXAttribute(name="SessionId", description="The client session ID", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getSessionId() {
		return sessionId;
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
	@JMXAttribute(name="ConnectTimeout", description="The connection timeout in ms.", mutability=AttributeMutabilityOption.READ_WRITE)
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
	@JMXAttribute(name="OperationTimeout", description="The operation timeout in ms.", mutability=AttributeMutabilityOption.READ_WRITE)	
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
		sendNotification(new Notification(HeliosOTClient.NOTIFICATION_CONNECT, getObjectName(), nextNotificationSequence(), System.currentTimeMillis(), "Client Connected [" + this.toString() + "]"));
	}
	
	/**
	 * Emits a client connected event to all registered listeners
	 */
	protected void fireOnConnectFailure(Throwable cause) {
		for(HeliosOTClientEventListener listener: listeners) {
			listener.onConnectFailure(this, cause);
		}	 
		Notification notif = new Notification(HeliosOTClient.NOTIFICATION_CONNECT, getObjectName(), nextNotificationSequence(), System.currentTimeMillis(), "Client Connection Failure [" + this.toString() + "]");
		notif.setUserData(StringHelper.formatStackTrace(cause));
		sendNotification(notif);
	}
	
	
	/**
	 * Emits a client disconnected event to all registered listeners
	 * @param cause The cause of the disconnect, or null if a disconnect was requested
	 */
	protected void fireOnDisconnect(Throwable cause) {
		for(HeliosOTClientEventListener listener: listeners) {
			listener.onDisconnect(this, cause);
		} 
		sendNotification(new Notification(HeliosOTClient.NOTIFICATION_DISCONNECT, getObjectName(), nextNotificationSequence(), System.currentTimeMillis(), "Client Disconnected [" + this.toString() + "]"));		
	}

	

}
