/**
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
package org.helios.tracing.extended.graphite;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.EpochHelper;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.jmxenabled.queues.FlushQueueReceiver;
import org.helios.jmxenabled.queues.TimeSizeFlushQueue;



/**
 * <p>Title: GraphiteClient</p>
 * <p>Description: The low level graphite metric submitter and supporting asynch constructs</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.graphite.GraphiteClient</p></code>
 */
@JMXManagedObject(annotated=true, declared=true)
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating the graphite client connected", types={
                @JMXNotificationType(type=GraphiteClient.ConnectionEvents.CONNECT_EVENT)
        }),
        @JMXNotification(description="Notification indicating the graphite client disconnected", types={
                @JMXNotificationType(type=GraphiteClient.ConnectionEvents.DISCONNECT_EVENT)
        }),
        @JMXNotification(description="Notification indicating the graphite client failed to connect", types={
                @JMXNotificationType(type=GraphiteClient.ConnectionEvents.CONNECT_FAILED_EVENT)
        })
})
public class GraphiteClient extends ManagedObjectDynamicMBean implements Delayed, FlushQueueReceiver<byte[]> {
	/**  */
	private static final long serialVersionUID = 2005560762557471461L;
	
	/**
	 * <p>Title: ConnectionEvents</p>
	 * <p>Description: Defines constants for connection events.</p> 
	 * <p><code>org.helios.tracing.extended.graphite.GraphiteClient.ConnectionEvents</code></p>
	 */
	public static interface ConnectionEvents {
		/** Connection event notification type */
		public static final String CONNECT_EVENT = "org.helios.tracing.extended.graphite.client.connected";
		/** Disconnect event notification type */
		public static final String DISCONNECT_EVENT = "org.helios.tracing.extended.graphite.client.disconnected";
		/** Connection Failure event notification type */
		public static final String CONNECT_FAILED_EVENT = "org.helios.tracing.extended.graphite.client.connectfailure";
	}
	
	/** The Graphite Server host name or IP address */
	protected String graphiteHost;
	/** The Graphite Server listening port */
	protected int graphitePort;
	/** The flush queue size trigger */
	protected Integer sizeTrigger = DEFAULT_SIZE_TRIGGER;
	/** The flush queue time trigger in ms. */
	protected Long timeTrigger = DEFAULT_TIME_TRIGGER;
	
	/** The reconnect delay in ms. */
	protected long reconnectDelay = DEFAULT_RECONNECT_DELAY; 
	
	/** The connection socket */
	protected Socket socket = null;
	/** The connect timeout in ms. */
	protected int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
	/** The number of consecutive connection failures */
	protected AtomicInteger connFails = new AtomicInteger(0);
	/** The socket output stream */
	protected OutputStream os = null;
	/** The flush queue */
	protected final TimeSizeFlushQueue<byte[]> flushQueue;
	
	/** Indicates if the graphite tracer is connected */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** The queue submission option. Defaults to <code>WAIT</code> */
	protected QueueSubmission submitter = QueueSubmission.WAIT;
	
	/** The instance logger */
	protected final Logger log;
	/** the queue for client reconnect requests */
	protected static final DelayQueue<GraphiteClient> reconnects = new DelayQueue<GraphiteClient>();
	/** indicates if the full stack trace should be printed on submission errors */
	protected boolean reportError = false;
	
	/** the start time of the reconnect */
	protected final AtomicLong reconnectStartTime = new AtomicLong(0L);
	
	/** the number of messages traced to graphite */
	protected final AtomicLong messageSent = new AtomicLong(0L);

	/** A rolling counter for the number of bytes sent */
	protected final LongRollingCounter bytesSentCounter = new LongRollingCounter("BytesSent", 10); 
	/** A rolling counter for the elapsed time of metric flushed */
	protected final LongRollingCounter sendElapsedTimeCounter = new LongRollingCounter("SendTime", 10); 
	
	
	/** A static map of created clients */
	protected static final Map<String, GraphiteClient> clients = new ConcurrentHashMap<String, GraphiteClient>();
	/** A map of registered listeners */
	protected static final Set<GraphiteClientConnectionListener> listeners = new CopyOnWriteArraySet<GraphiteClientConnectionListener>();
	protected static Executor executor = Executors.newCachedThreadPool(new ThreadFactory(){
		/** Thread factory serial factory */
		protected AtomicLong serial = new AtomicLong(0L);
		/** Thread pool thread group */
		protected final ThreadGroup executorThreadPoolGroup = new ThreadGroup("GraphiteClientThreadGroup");
		
		/**
		 * Creates a new thread
		 * @param r
		 * @return
		 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
		 */
		public Thread newThread(Runnable r) {
			Thread t = new Thread(executorThreadPoolGroup, r, "GraphiteClientPooledThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY-2);
			return t;
		}	
	});
	
	/** The Graphite Server default IP address */
	public static final String DEFAULT_GRAPHITE_HOST = "127.0.0.1";
	/** The Graphite Server default listening port */
	public static int DEFAULT_GRAPHITE_PORT = 2003;
	/** The default submission queue size trigger */
	public static final int DEFAULT_SIZE_TRIGGER = 500;
	/** The default submission queue time trigger */
	public static final long DEFAULT_TIME_TRIGGER = 15000;
	/** The default connect timeout in ms. */
	public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
	/** The default reconnect delay */
	public static final long DEFAULT_RECONNECT_DELAY = 15000;
	/** The configuration prefix */
	public static final String CONFIG_PREFIX = GraphiteClient.class.getPackage().getName() + ".client.";
	/** The JMX notification sequence provider */
	private static final AtomicLong notificationSequence = new AtomicLong(0);
	
	
	static {
		new ReconnectorThread(reconnects).start();
	}
	
	/**
	 * Registers a GraphiteClient connection listener
	 * @param listener the listener to register
	 */
	public static void registerListener(GraphiteClientConnectionListener listener) {
		if(listener!=null && !listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a GraphiteClient connection listener
	 * @param listener the listener to unregister
	 */
	public static void unregisterListener(GraphiteClientConnectionListener listener) {
		if(listener!=null && listeners.contains(listener)) {
			listeners.remove(listener);
		}
	}
	
	/**
	 * Returns a descriptive tag describing the graphite server location.
	 * @return a descriptive tag describing the graphite server location.
	 */
	public String getServerDesc() {
		StringBuilder b = new StringBuilder("Graphite[");
		b.append(this.graphiteHost).append(":").append(this.graphitePort).append("].Connected:").append(isConnected());
		return b.toString();
	}

	/**
	 * Acquires a GraphiteClient for the environment defined or default host name and IP address.
	 */	
	public static GraphiteClient getClient() {
		String _host = ConfigurationHelper.getSystemThenEnvProperty(CONFIG_PREFIX + "host", DEFAULT_GRAPHITE_HOST);
		int _port = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "port", DEFAULT_GRAPHITE_PORT);
		return getClient(_host, _port);
	}
	
	
	/**
	 * Acquires a GraphiteClient for the passed host name and IP address.
	 * @param graphiteHost The Graphite Server host name or IP address
	 * @param graphitePort The Graphite Server listening port
	 */	
	public static GraphiteClient getClient(String graphiteHost, int graphitePort) {
		if(graphiteHost==null || graphiteHost.length()<1) {
			throw new RuntimeException("The GraphiteHost passed for the GraphiteTracerInstanceFactory ctor was null or zero length", new Throwable());
		}
		String key = graphiteHost + ":" + graphitePort;
		GraphiteClient client = clients.get(key);
		if(client==null) {
			synchronized(clients) {
				client = clients.get(key);
				if(client==null) {
					client = new GraphiteClient(graphiteHost, graphitePort);
					clients.put(key, client);
				}
			}
		}
		return client;
	}
	
	
	
	/**
	 * <p>Title: ReconnectorThread</p>
	 * <p>Description: The GraphiteClient reconnector.</p> 
	 * <p>Company: ICE Futures US</p>
	 * @author Whitehead (nicholas.whitehead@theice.com)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.tracing.extended.graphite.ReconnectorThread</code></p>
	 */
	private static class ReconnectorThread extends Thread {
		/** A set of client keys that have requested reconnects */
		protected final DelayQueue<GraphiteClient> reconnects;
		/** Instance logger */
		protected final Logger log = Logger.getLogger(getClass());
		/**
		 * Creates the reconnect thread
		 * @param reconnects The reconnect queue
		 */
		public ReconnectorThread(final DelayQueue<GraphiteClient> reconnects) {
			this.reconnects = reconnects;
			this.setName("GraphiteClientReconnectThread");
			this.setDaemon(true);
		}
		
		public void run() {
			log.info("Starting GraphiteClient reconnect thread");
			while(true) {
				GraphiteClient client = null;
				try {
					client = reconnects.take();
					if(client!=null && !client.connected.get()) {
						log.info("Reconnecting GraphiteClient [" + client + "].....");
						client.connect();
						if(client.connected.get()) {
							log.info("Reconnected GraphiteClient [" + client + "]");
						} else {
							throw new Exception("Client not connected");
						}
					}
				} catch (Exception e) {
					log.warn("Failed to reconnect GraphiteClient [" + client + "/" + client.connFails.get() + "]. Submitting for reconnect.");
					client.submitForReconnect();
				}
			}
		}
	}
	
	/**
	 * Submits the client for reconnect
	 */
	protected void submitForReconnect() {
		if(!reconnects.contains(this)) {
			reconnects.add(this);
			reconnectStartTime.set(System.currentTimeMillis()+reconnectDelay);
			log.info("GraphiteClient [" + this + "] submitted for reconnect.");
		}
	}
	
	/**
	 * Creates a new GraphiteClient
	 * @param graphiteHost The Graphite Server host name or IP address
	 * @param graphitePort The Graphite Server listening port
	 */
	private GraphiteClient(String graphiteHost, int graphitePort) {
		if(graphiteHost==null || graphiteHost.length()<1) {
			throw new RuntimeException("The GraphiteHost passed for the GraphiteTracerInstanceFactory ctor was null or zero length", new Throwable());
		}
		log = Logger.getLogger(getClass().getSimpleName() + "." + graphiteHost.replace('.','_') + ":" + graphitePort);
		this.graphiteHost = graphiteHost;
		this.graphitePort = graphitePort;
		if(sizeTrigger==null) sizeTrigger = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "trigger.size", DEFAULT_SIZE_TRIGGER);
		if(timeTrigger==null) timeTrigger = ConfigurationHelper.getLongSystemThenEnvProperty(CONFIG_PREFIX + "trigger.time", DEFAULT_TIME_TRIGGER);
		flushQueue = new TimeSizeFlushQueue<byte[]>("FlushQueue", sizeTrigger, timeTrigger, this);
		log.info("Created GraphiteClient for [" + graphiteHost + ":" + graphitePort + "]");
		try {
			objectName = JMXHelper.objectName(getClass().getPackage().getName(), "service", "GraphiteClient", "host", this.graphiteHost, "port", "" + this.graphitePort );
			this.reflectObject(this);
			this.reflectObject(flushQueue);
			this.reflectObject(bytesSentCounter);
			this.reflectObject(sendElapsedTimeCounter);
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (Exception e) {
			log.warn("Failed to register GraphiteClient management interface", e);
		}
	}
	
	
	/**
	 * Broadcasts a disconnect notification
	 * @param client The client that disconnected
	 * @param ex The exception that casued the disconnect. Null if disconnect was intended.
	 */
	protected static void sendDisconnectNotification(GraphiteClient client, Exception ex) {
		client.sendNotification(new Notification(ConnectionEvents.DISCONNECT_EVENT, client.objectName, notificationSequence.incrementAndGet(), (ex==null ? "Clean Disconnect" : "Connection Failure") + client.toString()));
		for(GraphiteClientConnectionListener listener: listeners) {
			listener.onDisconnect(client.getGraphiteHost(), client.getGraphitePort(), ex);
		}
	}
	
	/**
	 * Broadcasts a connect notification
	 * @param client The client that connected
	 */
	protected static void sendConnectNotification(GraphiteClient client) {
		client.sendNotification(new Notification(ConnectionEvents.CONNECT_EVENT, client.objectName, notificationSequence.incrementAndGet(), client.toString()));
		for(GraphiteClientConnectionListener listener: listeners) {
			listener.onConnect(client.getGraphiteHost(), client.getGraphitePort());
		}
	}
	
	
	/**
	 * Broadcasts a connect failure notification
	 * @param client The client that failed to connect
	 * @param failCount The consecutive failure count
	 * @param ex The failure causing exception
	 */
	protected static void sendConnectFailureNotification(GraphiteClient client, int failCount, Exception ex) {
		client.sendNotification(new Notification(ConnectionEvents.CONNECT_FAILED_EVENT, client.objectName, notificationSequence.incrementAndGet(), client.toString()));
		for(GraphiteClientConnectionListener listener: listeners) {
			listener.onConnectFailure(client.getGraphiteHost(), client.getGraphitePort(), failCount, ex);
		}
	}
	
	/** The message format for the submission  */
	public static final String MESSAGE_FORMAT = "{0} {1} {2} \n";
	
	/**
	 * Submits a metric into the submission queue
	 * @param name The metric name
	 * @param value The metric value
	 */
	public void submit(String name, Number value) {
		submit(MessageFormat.format(MESSAGE_FORMAT, name, "" + value, EpochHelper.getUnixTimeStr() ).getBytes());
	}
	
	/**
	 * Submits a raw byte array
	 * @param msg a raw byte array
	 */
	public void submit(byte[] msg) {
		//if(log.isDebugEnabled()) log.debug("Enqueueing Graphite Message [" + new String(msg) + "]");
		flushQueue.add(msg);				
	}
	
	/**
	 * Submits a string message
	 * @param chars a string message
	 */
	public void submit(CharSequence chars) {
		log.info("Submitting [" + chars + "]");
		submit(chars.toString().getBytes());
	}
	
	
	
	/**
	 * Processes a detected disconnect.
	 * @param exc The cause of the disconnect
	 */
	protected void processDisconnect(Exception exc) {
		if(!connected.get()) return;
		connected.set(false);
		os = null;
		try { socket.close(); } catch (Exception e) {}
		socket = null;
		sendDisconnectNotification(this, exc);
		submitForReconnect();
	}
	
	/**
	 * Disconnects the client
	 */
	@JMXOperation(name="disconnect", description="Disconnects the client")
	public void disconnect() {
		processDisconnect(null);
	}
	
	/**
	 * Connects the client if it is disconnected
	 */
	@JMXOperation(name="connect", description="Connects the client if it is disconnected")
	public void connect() {
		if(!connected.get()) {
			synchronized(connected) {
				if(!connected.get()) {
					InetSocketAddress address = new InetSocketAddress(graphiteHost, graphitePort);
					try {
						socket = new Socket();
						socket.setReuseAddress(false);
						socket.connect(address, connectTimeout);
						connected.set(true);
						if(!graphiteHost.equals(objectName.getKeyProperty("host")) || 
							!("" + graphitePort).equalsIgnoreCase(objectName.getKeyProperty("port"))) {
							// republish mbean
							try {
								try {
									JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName);
								} catch (Exception e) {}
								objectName = JMXHelper.objectName(getClass().getPackage().getName(), "host", this.graphiteHost, "port", "" + this.graphitePort );
//								this.reflectObject(this);
//								this.reflectObject(flushQueue);
								JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
							} catch (Exception e) {
								log.warn("Failed to register GraphiteClient management interface", e);
							}
							
						}
						
						sendConnectNotification(this);
						connFails.set(0);						
						os = socket.getOutputStream();
					} catch (Exception e) {
						connected.set(false);
						int fails = connFails.incrementAndGet();
						submitForReconnect();
						sendConnectFailureNotification(this, fails, e);
					}
				}
			}
		}
	}


	
	
	/**
	 * Returns a new builder for the passed host and port
	 * @param graphiteHost
	 * @param graphitePort
	 * @return a GraphiteClient builder
	 */
	public static Builder newBuilder(String graphiteHost, int graphitePort) {
		return new Builder(graphiteHost, graphitePort);
	}
	
	/**
	 * Returns a new builder for the default host and port
	 * @return a GraphiteClient builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}
	

	
	/**
	 * <p>Title: Builder</p>
	 * <p>Description: A GraphiteClient builder.</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 */
	static class Builder {
		/** The Graphite Server host name or IP address */
		protected final String graphiteHost;
		/** The Graphite Server listening port */
		protected final int graphitePort;
		/** The reconnect delay in ms. for the built GraphiteClient */
		protected long reconnectDelay = DEFAULT_RECONNECT_DELAY;
		/** indicates if the full stack trace should be printed on submission errors */
		protected boolean reportError = false;
		/** the submission option */
		protected QueueSubmission submission = QueueSubmission.WAIT;
		
		/** auto connect option */
		protected boolean autoConnect = true;
		/** The flush queue size trigger */
		protected int sizeTrigger = DEFAULT_SIZE_TRIGGER;
		/** The flush queue time trigger */
		protected long timeTrigger = DEFAULT_TIME_TRIGGER;
		
		/** Connection listeners to add */
		protected Set<GraphiteClientConnectionListener> listeners = new HashSet<GraphiteClientConnectionListener>();
		
		
		/**
		 * Creates a new builder
		 * @param graphiteHost
		 * @param graphitePort
		 */
		public Builder(String graphiteHost, int graphitePort) {
			if(graphiteHost==null || graphiteHost.length()<1) {
				throw new RuntimeException("The GraphiteHost passed for the GraphiteTracerInstanceFactory ctor was null or zero length", new Throwable());
			}			
			if(graphitePort < 1) {
				throw new RuntimeException("The GraphitePort passed for the GraphiteTracerInstanceFactory ctor was < 1", new Throwable());
			}
			this.graphiteHost = graphiteHost;
			this.graphitePort = graphitePort;
		}
		
		/**
		 * Creates a new builder
		 */
		public Builder() {
			this(DEFAULT_GRAPHITE_HOST, DEFAULT_GRAPHITE_PORT);
		}
		
		/**
		 * Sets the verbosity of the submisson disconnect error reporting
		 * @param reportError
		 * @return this builder
		 */
		public Builder setReportError(boolean reportError) {
			this.reportError = reportError;
			return this;
		}
		
		/**
		 * Sets the submission option
		 * @param submission
		 * @return this builder
		 */
		public Builder setSubmitter(QueueSubmission submission) {
			this.submission = submission;
			return this;
		}
		
		/**
		 * Sets the configured client's reconnect delay in ms.
		 * @param reconnectDelay
		 * @return this builder
		 */
		public Builder setReconnectDelay(long reconnectDelay) {
			this.reconnectDelay = reconnectDelay;
			return this;
		}
		
		/**
		 * Sets the autoConnect option
		 * @param autoConnect
		 * @return this builder
		 */
		public Builder setAutoConnect(boolean autoConnect) {
			this.autoConnect = autoConnect;
			return this;
		}
		
		/**
		 * Sets the flush queue size trigger
		 * @param size trigger
		 * @return this builder
		 */
		public Builder setSizeTrigger(int sizeTrigger) {
			this.sizeTrigger = sizeTrigger;
			return this;
		}

		/**
		 * Sets the flush queue time trigger
		 * @param time trigger
		 * @return this builder
		 */
		public Builder setTimeTrigger(long timeTrigger) {
			this.timeTrigger = timeTrigger;
			return this;
		}
		
		
		/**
		 * Adds connection listeners
		 * @param listeners
		 * @return this builder
		 */
		public Builder addConnectionListeners(GraphiteClientConnectionListener...listeners) {
			if(listeners!=null) {
				for(GraphiteClientConnectionListener listener: listeners) {
					if(listener!=null && !this.listeners.contains(listener)) {
						this.listeners.add(listener);
					}
				}
			}
			return this;
		}
		
		/**
		 * Builds and returns the configured GraphiteClient
		 * @return the configured GraphiteClient
		 */
		public GraphiteClient build() {
			String key = graphiteHost + ":" + graphitePort;
			GraphiteClient client = clients.get(key);
			if(client==null) {
				synchronized(clients) {
					client = clients.get(key);
					if(client==null) {
						client = new GraphiteClient(graphiteHost, graphitePort);
						client.setReconnectDelay(reconnectDelay);
						client.reportError = reportError;
						for(GraphiteClientConnectionListener listener: listeners) {
							GraphiteClient.registerListener(listener);
						}
						clients.put(key, client);
					}
				}
			}
			if(!client.connected.get() && autoConnect) {
				client.connect();
			}
			return client;
		}
		

	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		final Logger log = Logger.getLogger(GraphiteClient.class);
		log.info("GraphiteClient Test");
		//GraphiteClient client = GraphiteClient.newBuilder("localhost", 2003).setAutoConnect(true).addConnectionListeners(new GraphiteClientConnectionListener(){
		GraphiteClient client = GraphiteClient.newBuilder("192.168.56.101", 2003).setAutoConnect(true).addConnectionListeners(new GraphiteClientConnectionListener(){
			public void onConnect(String graphiteHost, int graphitePort) {
				log.info("Connected [" + graphiteHost + ":" + graphitePort);
			}
			public void onConnectFailure(String graphiteHost, int graphitePort, int failureCount, Exception ex) {
				log.error("Connect Failure [" + graphiteHost + ":" + graphitePort + "]", ex);
				//System.exit(-1);
			}
			public void onDisconnect(String graphiteHost, int graphitePort, Exception ex) {
				log.info("Disconnect [" + graphiteHost + ":" + graphitePort, ex);
			}
			
		}).build();
		Random random = new Random(System.nanoTime());
		while(true) {
			int a = random.nextInt(100);
			int c = random.nextInt(10);
			client.submit("A.B.C.D", a);
			for(int i = 0; i < c; i++) {
				client.submit("A.B.C.D" + i, random.nextInt(100));
			}
			log.info("---> [" + c+1 + "]");
			
			try { Thread.sleep(15000); } catch (Exception e) {}
		}
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString()  {
	    StringBuilder retValue = new StringBuilder("GraphiteClient [");    
	    retValue.append("graphiteHost=").append(this.graphiteHost);    
	    retValue.append(" graphitePort=").append(this.graphitePort);    
	    retValue.append("]");
	    return retValue.toString();
	}
	
	/**
	 * Sets this client's reconnect delay
	 * @param reconnectDelay
	 */
	public void setReconnectDelay(long reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
	}

	/**
	 * Returns the reconnect delay for this client
	 * @param unit The unit to render the delay in
	 * @return the client's reconnect delay in the passed unit
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(reconnectStartTime.get()-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Returns the time until the next reconnect attempt in ms.
	 * @return the time until the next reconnect attempt in ms.
	 */
	@JMXAttribute(name="Delay", description="The time until the next reconnect attempt in ms.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getDelay() {
		long time = reconnectStartTime.get()-System.currentTimeMillis();
		return time>0 ? time : -1L;
	}
	

	/**
	 * Compares the delay on this client to another delayed instance.
	 * @param delayed
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed delayed) {
		if(delayed.getDelay(TimeUnit.MILLISECONDS)==this.reconnectDelay) return -1;
		return this.reconnectDelay < delayed.getDelay(TimeUnit.MILLISECONDS) ? -1 : 1;
	}
	
	/**
	 * Returns the state of the connection
	 * @return true if the client is connected, false if it is not.
	 */
	public boolean isConnected() {
		return connected.get();
	}
	
	/**
	 * Returns the state of the connection
	 * @return true if the client is connected, false if it is not.
	 */
	@JMXAttribute(name="Connected", description="The state of the connection", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getConnected() {
		return connected.get();
	}

	/**
	 * @param flushedItems
	 */
	@Override
	public void flushTo(Collection<byte[]> flushedItems) {
		int itemCount = 0;
		try {
			if(flushedItems==null) return;
			itemCount = flushedItems.size();
			if(itemCount<1) return;
			//log.info("Starting Submission");
			if(connected.get()) {
				long startTime = System.currentTimeMillis();
				long bytesFlushed = 0L;
				//ByteArrayOutputStream baos = new ByteArrayOutputStream(flushedItems.length*100);
				for(byte[] ba: flushedItems) {
					//baos.write(ba);
					try {
						os.write(ba);
						bytesFlushed += ba.length;
					} catch (SocketException se) {
						if(reportError) {
							log.error("Socket exception on GraphiteClient [" + this + "]. Closing and submitting for reconnect.", se);
						} else {
							log.warn("Socket exception on GraphiteClient [" + this + "]. Closing and submitting for reconnect." + se);
						}
						processDisconnect(se);
						break;
					}					
				}
				os.flush();
				InputStream is = null;
//				try {
//					is = socket.getInputStream();
//					log.info("-------------->Bytes In:" + is.available());
//				} catch (Exception e) {
//					
//				} finally {
//					try { is.close(); } catch (Exception e) {}
//				}
				messageSent.addAndGet(itemCount);
				bytesSentCounter.put(bytesFlushed);
				flushedItems.clear();
				long elapsed = System.currentTimeMillis()-startTime;
				sendElapsedTimeCounter.put(elapsed);
				log.info("Subed [" + itemCount + "] items in [" + elapsed + "] ms.");										
			}
		} catch (Exception e) {
			log.error("Failed to flush submissions", e);
		}		
	}

	/**
	 * Returns the number of messages sent to graphite since the last reset
	 * @return the messageSent
	 */
	@JMXAttribute(name="MessageSent", description="The number of messages sent to graphite since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMessageSent() {
		return messageSent.get();
	}
	
	/**
	 * Returns a representation of the graphite server
	 * @return a representation of the graphite server
	 */
	@JMXAttribute(name="Name", description="The representation of the graphite server", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getName() {
		return graphiteHost + ":" + graphitePort;
	}

	/**
	 * Returns the host or ip address of the Graphite Server
	 * @return the Graphite Server
	 */
	@JMXAttribute(name="GraphiteHost", description="The host or ip address of the Graphite Server", mutability=AttributeMutabilityOption.READ_WRITE)
	public String getGraphiteHost() {
		return graphiteHost;
	}
	
	/**
	 * Sets the graphite server host name or IP address
	 * @param host the host
	 */
	public void setGraphiteHost(String host) {
		this.graphiteHost = host;
	}

	/**
	 * Returns the port that the Graphite Server is listening on
	 * @return the Graphite Server port
	 */
	@JMXAttribute(name="GraphitePort", description="The Graphite Server port", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getGraphitePort() {
		return graphitePort;
	}
	
	/**
	 * Sets the graphite server port
	 * @param port the port
	 */
	public void setGraphitePort(int port) {
		this.graphitePort = port;
	}
	
	/**
	 * Determines if this client is reconnecting
	 * @return true if this client is reconnecting
	 */
	@JMXAttribute(name="Reconnecting", description="True if this client is reconnecting", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getReconnecting() {
		return GraphiteClient.reconnects.contains(this);
	}
}
