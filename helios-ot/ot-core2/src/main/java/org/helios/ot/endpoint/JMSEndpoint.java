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
package org.helios.ot.endpoint;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.management.openmbean.TabularData;

import org.apache.log4j.Logger;
import org.helios.helpers.Banner;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.threads.HeliosThreadFactory;
import org.helios.jmxenabled.threads.HeliosThreadGroup;
import org.helios.ot.trace.MetricId;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: JMSEndpoint</p>
 * <p>Description: Endpoint implementation that dispatches traces on a JMS destination</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.endpoint.JMSEndpoint</code></p>
 * @param <T> The type of objects delivered in the TraceCollection
 */
@JMXManagedObject (declared=false, annotated=true)
//@JMXNotifications(notifications={
//        @JMXNotification(description="Notification indicating change in endpoint state", types={
//                @JMXNotificationType(type="org.helios.ot.endpoint.state")
//        })
//})
public class JMSEndpoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T>  implements ExceptionListener, UncaughtExceptionHandler {
	/** The JMS connection factory */
	protected ConnectionFactory connectionFactory;
	/** The JMS connection */
	protected volatile Connection connection;
	
	/** The JMS destination */
	protected volatile Destination destination;
	/** The submission queue */
	protected final BlockingQueue<Set<Trace>> submissionQueue;
	/** The configured builder */
	protected Builder builder = null;
	/** The thread contexts to run this endpoint */
	protected final JMSThreadContext[] processors;
	/** The thread factory for creating processor threads */
	protected final HeliosThreadFactory threadFactory;
	/** The thread group created processor threads run in */
	protected final HeliosThreadGroup threadGroup;
	/** A shared flag to indicate threads should shutdown */
	protected final AtomicBoolean keepRunning = new AtomicBoolean(true);
	
	/** The template for building a destination name */
	public static final String AGENT_DEST_PREFIX = "helios.agent.in.%s.%s";
	
	/**
	 * <p>Title: JMSThreadContext</p>
	 * <p>Description: A runnable that processes trace collection submissions off the endpoint's submission queue </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.endpoint.JMSThreadContext</code></p>
	 */
	private static class JMSThreadContext  implements Runnable {
		/** The JMS session */
		protected final Session session;
		/** The JMS message producer */
		protected final MessageProducer producer;	
		/** The configured builder */
		private final Builder builder;
		/** This context's key */
		private final int key;		
		/** A shared flag to indicate threads should shutdown */
		protected final AtomicBoolean keepRunning;
		/** The submission queue to take from */
		protected final BlockingQueue<Set<Trace>> submissionQueue;
		/** loop counter */
		protected int loopCount = 0;
		/** Static class logger */
		protected static final Logger LOG = Logger.getLogger(JMSThreadContext.class);
		/**
		 * Creates a new JMSThreadContext
		 * @param submissionQueue The submission queue to take from
		 * @param keepRunning The run flag
		 * @param connection The JMS Connection 
		 * @param destination The JMS Destination to send to 
		 * @param builder The configuration builder
		 * @param key the sequential key that uniquely identifies this JMSThreadContext within the endpoint instance 
		 * @throws EndpointConnectException thrown if this JMSThreadContext fails to connect
		 */
		protected JMSThreadContext(final BlockingQueue<Set<Trace>> submissionQueue, final AtomicBoolean keepRunning, Connection connection, Destination destination, Builder builder, int key) throws EndpointConnectException {
			this.key = key;
			this.keepRunning = keepRunning;
			this.submissionQueue = submissionQueue;
			try {
				this.builder = builder;
				session = connection.createSession(builder.isTransacted(), Session.AUTO_ACKNOWLEDGE);				
				producer = session.createProducer(destination);
				producer.setDeliveryMode(builder.getDeliveryMode());
				producer.setDisableMessageID(builder.isDisableMessageIds());
				producer.setDisableMessageTimestamp(builder.isDisableMessageTimestamps());
				producer.setPriority(builder.getPriority());
				producer.setTimeToLive(builder.getTimeToLive());
			} catch (Exception e) {
				throw new EndpointConnectException("Failed to connect JMSThreadContext#" + key, e);
			}
		}
		
		/**
		 * {@inheritDoc}
		 * <p>Pulls trace collections off the endpoint's submission queue and submits them </p>
		 * @see java.lang.Runnable#run()
		 */
		@SuppressWarnings("unchecked")
		public void run() {
			int batchSize = builder.getBatchSize();
			Set<Trace> grabbedCollections = new HashSet<Trace>(batchSize);
			Set<Trace> traces = null;
			while(keepRunning.get()) {
				try {
					traces = submissionQueue.take();
					if(traces.isEmpty()) continue;
					grabbedCollections.addAll(traces);
					for(int i = 1; i < batchSize; i++) {
						traces = submissionQueue.poll();
						if(traces==null) break;
						if(traces.isEmpty()) {
							continue;
						}
						grabbedCollections.addAll(traces);
					}
					try {						
						producer.send(session.createObjectMessage(grabbedCollections.toArray(new Trace[grabbedCollections.size()])));
						if(LOG.isTraceEnabled()) LOG.trace("\n\tSent [" + grabbedCollections.size() + "] Traces");
						//LOG.info("\n\tSent [" + grabbedCollections.size() + "] Traces");
					} finally {
						grabbedCollections.clear();
					}
					loopCount++;
				} catch (InterruptedException ie) {
					if(keepRunning.get()) {
						Thread.interrupted();
					} else {
						break;
					}					
				} catch (JMSException je) {
					throw new RuntimeException(je);
				}		
			}
		}
		
		public void disconnect() {
			if(producer!=null) {
				try { producer.close(); } catch (Exception e) {}
			}
			if(session!=null) {
				try { session.close(); } catch (Exception e) {}
			}

		}
		
	}
	
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error("Exception thrown from worker thread [" + t + "]", e.getCause());
		
	}


	/**
	 * Creates a new JMSEndpoint
	 * @param builder The configured builder
	 */
	public JMSEndpoint(Builder builder) {
		super(builder);
		this.builder = builder;
		connectionFactory = builder.getConnectionFactory();
		destination = builder.getDestination();
		submissionQueue = new ArrayBlockingQueue<Set<Trace>>(builder.getQueueSize(), false);
		processors = new JMSThreadContext[builder.getProcessorThreadCount()];
		threadFactory = new HeliosThreadFactory(builder.getProcessorThreadPrefix(), -1, Thread.NORM_PRIORITY, this, true);
		reflectObject(threadFactory);
		threadGroup = (HeliosThreadGroup) threadFactory.getThreadGroup();
		reflectObject(threadGroup);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#connectImpl()
	 */
	@Override
	protected void connectImpl() throws EndpointConnectException {
		keepRunning.set(true);
		try {
			applyEndpointArguments("JMSConnectionFactory", connectionFactory);
			connection = connectionFactory.createConnection();
			applyEndpointArguments("JMSConnection", connection);
			connection.setClientID(builder.getClientId());
			connection.setExceptionListener(this);
			if(destination==null) {
				Session session = null;
				try {
					String destinationName = builder.getDestinationName()!=null ? builder.getDestinationName() : String.format(AGENT_DEST_PREFIX, MetricId.getHostname(), MetricId.getApplicationId());
					session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
					destination = builder.isP2p() ? session.createQueue(destinationName) : session.createTopic(destinationName);
					applyEndpointArguments("JMSDestination", destination);
				} catch (Exception e) {
					throw new EndpointConnectException("Failed to create destination for name [" + builder.getDestinationName() + "]", e);
				} finally {
					try { session.close(); } catch (Exception e) {}
				}
			}
			for(int i = 0; i < builder.getProcessorThreadCount(); i++) {
				JMSThreadContext jmst = new JMSThreadContext(submissionQueue, keepRunning, connection, destination, builder, i); 
				processors[i] = jmst;
				threadFactory.newThread(jmst).start();
			}
		} catch (Exception e) {
			throw new EndpointConnectException(getClass().getName() + " instance failed to connect", e);
		}
	}
	
	/**
	 * Cleans up all JMS resources left in an unknown state
	 */
	@Override
	public void cleanup() {	
		submissionQueue.clear();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#newBuilder()
	 */
	@Override
	public Builder newBuilder() {
		return new Builder();
	}
	
	/**
	 * Returns a new {@link JMSEndpoint.Builder}
	 * @return a JMSEndpoint Builder
	 */
	public static Builder getBuilder() {
		return new Builder();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#disconnectImpl()
	 */
	@Override
	protected void disconnectImpl() {
		keepRunning.set(false);
		threadGroup.interrupt();
		for(int i = 0; i < builder.getProcessorThreadCount(); i++) {
			processors[i].disconnect();
			processors[i] = null;
		}
		if(connection!=null) {
			try { connection.close(); } catch (Exception e) {}
			connection=null;
		}		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#processTracesImpl(org.helios.ot.tracer.disruptor.TraceCollection)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected boolean processTracesImpl(TraceCollection traceCollection) throws EndpointConnectException, EndpointTraceException {
		return submissionQueue.offer(traceCollection.getTraces());
	}
	
	
	

	
	/**
	 * {@inheritDoc}
	 * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
	 */
	@Override
	public void onException(JMSException exception) {
		Banner.bannerErr("!", 3, 4, "Exception:" + exception);		
		reconnect();
	}
	
	



	/**
	 * <p>Title: Builder</p>
	 * <p>Description: FLuent builder for a JMSEndpoint</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.endpoint.JMSEndpoint.Builder</code></p>
	 */
	public static class Builder extends AbstractEndpoint.Builder {
		//==============================================================
		//  Connection and destination resources
		//==============================================================
		/** The JMS connection factory */
		private ConnectionFactory connectionFactory;
		/** The JMS destination */
		private Destination destination;
		/** The JMS destination name */
		private String destinationName;	
		/** Indicates if the endpoint is using P2P or PubSub. Defaults to p2p */
		private boolean p2p = true;
		//==============================================================
		//  Session and message producer configuration
		//==============================================================
		/** Transacted sender. Defaults to false */
		private boolean transacted = false;
		/** The connection clientId. Defaults to a derrived name based on the OT Agent name */
		private String clientId = null;
		/** The delivery mode. Defaults to {@link javax.jms.DeliveryMode#NON_PERSISTENT} */
		private int deliveryMode = DeliveryMode.NON_PERSISTENT;
		/** The message priority for non-urgent messages. Defaults to {@link javax.jms.Message#DEFAULT_PRIORITY} */
		private int priority = Message.DEFAULT_PRIORITY;
		/** The message time-to-live for messages. Defaults to {@link javax.jms.Message#DEFAULT_TIME_TO_LIVE} */
		private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;
		/** Indicates if message IDs generation should be disabled. Default is false */
		private boolean disableMessageIds = false;
		/** Indicates if message timestamp generation should be disabled. Default is false */
		private boolean disableMessageTimestamps = false;
		
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.ot.endpoint.AbstractEndpoint.Builder#build()
		 */
		public JMSEndpoint build() {
			JMSEndpoint jmsEp = new JMSEndpoint(this);		
			if(clientId==null) {
				clientId = MetricId.getHostname() + "/" + MetricId.getApplicationId();
			}
			return jmsEp;
		}
		
		/**
		 * The configured connection factory
		 * @return the connectionFactory
		 */
		public ConnectionFactory getConnectionFactory() {
			return connectionFactory;
		}
		/**
		 * Sets the configured connection factory
		 * @param connectionFactory the connectionFactory to set
		 */
		public void setConnectionFactory(ConnectionFactory connectionFactory) {
			this.connectionFactory = connectionFactory;
		}
		
		/**
		 * Sets the configured connection factory
		 * @param connectionFactory the connection factory
		 * @return this builder
		 */
		public Builder connectionFactory(ConnectionFactory connectionFactory) {
			this.connectionFactory = connectionFactory;
			return this;
		}
		/**
		 * The destination that messages will be sent to
		 * @return the destination
		 */
		public Destination getDestination() {
			return destination;
		}
		/**
		 * Sets the destination that messages will be sent to
		 * @param destination the destination to set
		 */
		public void setDestination(Destination destination) {
			this.destination = destination;
		}
		
		/**
		 * Sets the destination that messages will be sent to
		 * @param destination the destination to set
		 * @return this builder
		 */
		public Builder destination(Destination destination) {
			this.destination = destination;
			return this;
		}
		
		/**
		 * The name of the destination that messages will be sent to 
		 * @return the destinationName
		 */
		public String getDestinationName() {
			return destinationName;
		}
		/**
		 * Sets the name of the destination that messages will be sent to
		 * @param destinationName the destinationName to set
		 */
		public void setDestinationName(String destinationName) {
			this.destinationName = destinationName;
		}
		
		/**
		 * Sets the name of the destination that messages will be sent to
		 * @param destinationName the destinationName to set
		 * @return this builder
		 */
		public Builder destinationName(String destinationName) {
			this.destinationName = destinationName;
			return this;
		}
		
		/**
		 * Indicates if the messages are sent to a topic (false) or a queue (true).
		 * @return the p2p
		 */
		public boolean isP2p() {
			return p2p;
		}
		/**
		 * Sets if the messages are sent to a topic (false) or a queue (true).
		 * This is only required if the builder is only supplied with a destination name
		 * and will have to create the destination 
		 * @param p2p true for a queue, false for a topic
		 */
		public void setP2p(boolean p2p) {
			this.p2p = p2p;
		}
		/**
		 * Sets if the messages are sent to a topic (false) or a queue (true).
		 * This is only required if the builder is only supplied with a destination name
		 * and will have to create the destination 
		 * @param p2p true for a queue, false for a topic
		 * @return this builder
		 */
		public Builder p2p(boolean p2p) {
			this.p2p = p2p;
			return this;
		}
		
		/**
		 * Indicates if the JMS session should be transacted 
		 * @return the transacted
		 */
		public boolean isTransacted() {
			return transacted;
		}
		/**
		 * Sets the JMS session to be transacted
		 * @param transacted true to set to transacted, false otherwise
		 */
		public void setTransacted(boolean transacted) {
			this.transacted = transacted;
		}
		/**
		 * Returns the JMS Connection ClientID
		 * @return the clientId
		 */
		public String getClientId() {
			return clientId;
		}
		/**
		 * Sets the JMS Connection ClientID
		 * @param clientId the clientId to set
		 */
		public void setClientId(String clientId) {
			this.clientId = clientId;
		}
		/**
		 * Returns the JMS Message Delivery Mode
		 * @return the deliveryMode
		 */
		public int getDeliveryMode() {
			return deliveryMode;
		}
		/**
		 * Sets the JMS Message Delivery Mode
		 * @param deliveryMode the deliveryMode to set
		 */
		public void setDeliveryMode(int deliveryMode) {
			this.deliveryMode = deliveryMode;
		}
		/**
		 * Returns the JMS Message priority 
		 * @return the priority
		 */
		public int getPriority() {
			return priority;
		}
		/**
		 * Sets the JMS Message priority
		 * @param priority the priority to set
		 */
		public void setPriority(int priority) {
			this.priority = priority;
		}
		/**
		 * Returns the JMS Message Time To Live in ms.
		 * @return the timeToLive
		 */
		public long getTimeToLive() {
			return timeToLive;
		}
		/**
		 * Set the JMS Message Time To Live in ms.
		 * @param timeToLive the timeToLive to set
		 */
		public void setTimeToLive(long timeToLive) {
			this.timeToLive = timeToLive;
		}
		
		/**
		 * Set the JMS Message Time To Live in ms.
		 * @param timeToLive the timeToLive to set
		 * @return this builder
		 */
		public Builder timeToLive(long timeToLive) {
			this.timeToLive = timeToLive;
			return this;
		}
		
		
		/**
		 * Indicates if JMS Message MessageID generation is disabled
		 * @return the disableMessageIds
		 */
		public boolean isDisableMessageIds() {
			return disableMessageIds;
		}
		/**
		 * Sets if JMS Message MessageID generation should be disabled
		 * @param disableMessageIds the disableMessageIds to set
		 */
		public void setDisableMessageIds(boolean disableMessageIds) {
			this.disableMessageIds = disableMessageIds;
		}
		/**
		 * Indicates if JMS Message timestamp generation is disabled
		 * @return the disableMessageTimestamps
		 */
		public boolean isDisableMessageTimestamps() {
			return disableMessageTimestamps;
		}
		/**
		 * Sets if JMS Message timestamp generation is disabled
		 * @param disableMessageTimestamps the disableMessageTimestamps to set
		 */
		public void setDisableMessageTimestamps(boolean disableMessageTimestamps) {
			this.disableMessageTimestamps = disableMessageTimestamps;
		}
		
		
		
	}

	/**
	 * Returns the connection's ClientID
	 * @return the connection's ClientID
	 */
	@JMXAttribute(name="ConnectionClientID", description="The JMS connection's ClientID", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getConnectionClientID() {
		if(connection==null) {
			return null;
		}
		try {
			return connection.getClientID();
		} catch (JMSException je) {
			return "Error Retrieving ClientID:" + je.getMessage();
		}
	}
	
	/**
	 * Returns the connection's class name
	 * @return the connection's class name
	 */
	@JMXAttribute(name="ConnectionType", description="The JMS connection's Class Name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getConnectionType() {
		if(connection==null) {
			return null;
		}
		return connection.getClass().getName();
	}
	
	/**
	 * Returns the JMS destination that messages are sent to 
	 * @return the JMS destination that messages are sent to 
	 */
	@JMXAttribute(name="Destination", description="The JMS destination that messages are sent to", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDestination() {
		if(destination==null) {
			return null;
		}
		return destination.toString();
	}


	/**
	 * Returns the number of pending submissions in the submission queue 
	 * @return the submission Queue size
	 */
	@JMXAttribute(name="SubmissionQueueSize", description="The number of pending submissions in the submission queue", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getSubmissionQueueSize() {		
		return submissionQueue.size();
	}
	
	/**
	 * Returns the remaining capacity of the submission queue 
	 * @return the remaining capacity of the submission queue
	 */
	@JMXAttribute(name="SubmissionQueueCapacity", description="The remaining capacity of the submission queue", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getSubmissionQueueCapacity() {		
		return submissionQueue.remainingCapacity();
	}
	


	/**
	 * Returns the number of JMSEndpoint processors
	 * @return the number of JMSEndpoint processors
	 */
	@JMXAttribute(name="ProcessorCount", description="The number of JMSEndpoint processors", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getProcessorCount() {
		return processors==null ? 0 : processors.length;
	}

	/**
	 * Indicates if the processors are flagged to be running
	 * @return true if the processors are running, false otherwise
	 */
	@JMXAttribute(name="ProcessorRunning", description="Indicates if the processors are flagged to be running", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getProcessorRunning() {
		return keepRunning.get();
	}

}
