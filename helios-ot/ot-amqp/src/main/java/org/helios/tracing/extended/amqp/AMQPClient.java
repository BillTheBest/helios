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

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;


/**
 * <p>Title: AMQPClient</p>
 * <p>Description: Generic AMQP client. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.amqp.AMQPClient</p></code>
 */
public class AMQPClient implements ShutdownListener {
	//===========================================================
	//		Mandatory
	//===========================================================
	/** The AMQP Host name or IP address */
	protected String amqpHost;
	/** The AMQP port */
	protected int amqpPort;
	//===========================================================
	/** The AMQP exchange */
	protected String exchange = DEFAULT_AMQP_EXCHANGE;
	/** The exchange type */
	protected String exchangeType  = DEFAULT_AMQP_EXCHANGE_TYPE;
	/** The AMQP user name */
	protected String amqpUser = null;
	/** The AMQP password */ 
	protected String amqpPassword = null;
	/** The AMQP designated queue name */
	protected String queueName = null;
	/** Indicates if the created exchange should be durable */
	protected boolean durableExchange = true;
	/** Indicates if the created exchange should be auto-delete */
	protected boolean autoDeleteExchange = false;
	/** The designated serializer for this client */
	protected volatile IDeliverySerializer serializer;
	/** Indicates if the client is connected */
	protected AtomicBoolean connected = new AtomicBoolean(false);
	/** A temporal connection factory  */
	protected ConnectionFactory cFact = null;
	/** The frequency based flush */
	protected long timeFlush = 5000;
	/** The pending size based flush */
	protected int itemFlush = 50;
	
	/** The AMQP connection */
	protected Connection conn = null;
	/** The AMQP channel */
	protected Channel channel = null;
	
	/** The instance logger */
	protected Logger log;
	
	/** The AMQP default host */
	public static final String DEFAULT_AMQP_HOST = "127.0.0.1";
	/** The AMQP default exchange */
	public static final String DEFAULT_AMQP_EXCHANGE = "graphite";
	/** The AMQP default exchange type */
	public static final String DEFAULT_AMQP_EXCHANGE_TYPE = "topic";
	/** The AMQP default port */
	public static final int DEFAULT_AMQP_PORT = 5672;
	/** The maximum port number */
	public static final int MAX_PORT = 65535;
	/** The AMQP default message serializer  */
	public static final IDeliverySerializer DEFAULT_AMQP_SERIALIZER = new SmartSerializer();


	/**
	 * Creates a new AMQPClient for the default  host and port.
	 */
	public AMQPClient() {
		this(DEFAULT_AMQP_HOST, DEFAULT_AMQP_PORT);
	}
	
	
	/**
	 * Creates a new AMQPClient for the passed host and client.
	 * @param amqpHost The AMQP host name or IP address
	 * @param amqpPort The AMQP listening port
	 */
	public AMQPClient(String amqpHost, int amqpPort) {
		if(amqpHost==null || amqpHost.length()<1) throw new RuntimeException("The passed AMQP host name was null or zero length", new Throwable());
		this.amqpHost = amqpHost;
		if(amqpPort<1 || amqpPort > MAX_PORT) throw new RuntimeException("The passed AMQP port was < 1 or > " + MAX_PORT, new Throwable());
		this.amqpPort = amqpPort;
		log = Logger.getLogger(getClass().getSimpleName() + "." + this.amqpHost + ":" + this.amqpPort);
		log.info("Created AMQPClient [" + this.amqpHost + ":" + this.amqpPort + "]");		
	}
	
	/**
	 * Stops the AMQP client and shuts down all the resources.
	 */
	public void stop() {
		log.info("\n\t===========================\n\tStopping AMQP Client\n\t===========================\n");
		try { channel.abort(); } catch (Exception e) {}
		try { conn.close(); } catch (Exception e) {}
		log.info("\n\t===========================\n\tStopped AMQP Client\n\t===========================\n");
	}
	
	/**
	 * Initiates a connect on a configured client.
	 * @throws Exception
	 */
	public void connect() throws Exception {
		if(!connected.get()) {
			if(serializer==null) {
				serializer = DEFAULT_AMQP_SERIALIZER;
			}
			try {
				if(log.isDebugEnabled()) log.debug("Connecting " + this);
				cFact = new ConnectionFactory();
				cFact.setHost(amqpHost);
				cFact.setPort(amqpPort);
				if(amqpUser!=null) {
					cFact.setUsername(amqpUser);					
				}
				if(amqpPassword!=null) {
					cFact.setPassword(amqpPassword);
				}
				conn = cFact.newConnection();
				if(log.isDebugEnabled()) log.debug("Acquired connection [" + conn + "]");
				channel = conn.createChannel();
				if(log.isDebugEnabled()) log.debug("Acquired channel [" + channel + "]");
				channel.exchangeDeclare(exchange, exchangeType, durableExchange, autoDeleteExchange, new HashMap<String, Object>());
				if(log.isDebugEnabled()) log.debug("Declared Exchange [" + exchange + "]");
				queueName = channel.queueDeclare().getQueue();
				if(log.isDebugEnabled()) log.debug("Queue [" + queueName + "]");
				channel.queueBind(queueName, exchange, "#");
				if(log.isDebugEnabled()) log.debug("Bound Queue [" + queueName + "] to exchange [" + exchange + "]");
				connected.set(true);
			} catch (Exception e) {
				log.error("Failed to connect " + this, e);
				throw new Exception("Failed to connect " + this, e);
			}
		}
	}
	
	/**
	 * Creates a queueing consumer that allows the client to snoop on messages that have been sent to the exchange.
	 * @return a queueing consumer.
	 */
	public void createSnoop(Consumer consumer) {
		if(!connected.get()) throw new RuntimeException("Client is not connected", new Throwable());
		try {
			channel.basicConsume(queueName, true, consumer);			
		} catch (IOException e) {
			throw new RuntimeException("Failed to create consumer for " + this, e);
		}
	}
	
	public void snoop() {
		Consumer consumer = new DeliveryListener(exchange);
		createSnoop(consumer);		
	}
	
	
	/**
	 * Sends a message to the connected exchange.
	 * @param routingKey The header routing key
	 * @param payload The message payload
	 * @return true if message was sent successfully.
	 * TODO: add option for routing key vararg
	 */
	public boolean send(String routingKey, Object payload) {
		if(connected.get()) {
			try {
				if(routingKey==null || routingKey.length()<1) routingKey = "#";
				byte[] body = serializer.serialize(payload);
				String mimeType = serializer.getMimeType();
				AMQP.BasicProperties props = new AMQP.BasicProperties();
				props.setContentType(mimeType);
				channel.basicPublish(exchange, routingKey, props, body);				
				return true;
			} catch (Exception e) {
				log.error("Failed to send message [" + routingKey + "]", e);
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Cancels the subscription for the passed consumer.
	 * @param consumer the consumer subscription to cancel
	 */
	public void closeConsumer(DefaultConsumer consumer) {
		try {
			channel.basicCancel(consumer.getConsumerTag());
		} catch (Exception e) {}
	}
	
	/**
	 * Creates a subscriber for the connected queue and exchange.
	 * @return a consumer.
	 */
	public QueueingConsumer createSubscriber() {
		QueueingConsumer consumer = new QueueingConsumer(channel);
		if(!connected.get()) throw new RuntimeException("Client is not connected", new Throwable());
		try {
			channel.basicConsume(queueName, true, this.toString(), consumer);
			log.info("Created consumer on [" + exchange + "/" + queueName + "] for consumer [" + consumer.getConsumerTag() + "]");
			return consumer;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create subscriber for [" + exchange + "]", e);
		}
		
		
	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger log = Logger.getLogger(AMQPClient.class);
		AMQPClient client = null;
		try {
			log.info("AMQPClient test");
			client = new AMQPClient();
			client.connect();
			log.info("\n\t===============================\n\tSuccessful Init\n\t===============================\n");
			client.snoop();
			for(int i = 0; i < 10000; i++) {
				client.send("hello.world", "Hello World#" + i);
				log.info("Sent Message #" + i);
				Thread.sleep(2000);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		} finally {
			try { client.processDisconnect(null); } catch (Exception e) {};
		}
	}
	
	/**
	 * Executes a clean connect with no thrown exceptions.
	 * @return true if the connect succeeded, false if it failed.
	 */
	public boolean cleanConnect() {
		try {
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Indicates if the client is connected
	 * @return true if the client is connected
	 */
	public boolean isConnected() {
		return connected.get();
	}
	
	
	/**
	 * Processes a disconnect.
	 * @param exc The cause of the disconnect. Null if clean shutdown.
	 */
	protected void processDisconnect(Throwable exc) {
		connected.set(false);
		try { channel.close(); } catch (Exception e) {}
		try { conn.close(); } catch (Exception e) {}
		queueName = null;
	}
	
	
	
	
	/**
	 * Callback from any shutdown notifier as a signal to disconnect
	 * @param signal The shutdown signal
	 * @see com.rabbitmq.client.ShutdownListener#shutdownCompleted(com.rabbitmq.client.ShutdownSignalException)
	 */
	@Override
	public void shutdownCompleted(ShutdownSignalException signal) {
		if(connected.get()) {
			processDisconnect(signal.isInitiatedByApplication() ? null : signal.getCause());
		}
	}


	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString()  {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("AMQPClient [");    
	    retValue.append(TAB).append("amqpHost=").append(this.amqpHost);    
	    retValue.append(TAB).append("amqpPort=").append(this.amqpPort);
	    retValue.append(TAB).append("connected=").append(this.connected);
	    if(exchange!=null) retValue.append(TAB).append("exchange=").append(this.exchange);    
	    retValue.append(TAB).append("exchangeType=").append(this.exchangeType);    
	    if(amqpUser!=null) retValue.append(TAB).append("amqpUser=").append(this.amqpUser);      
	    if(queueName!=null) retValue.append(TAB).append("queueName=").append(this.queueName);    
	    retValue.append("\n]");
	    return retValue.toString();
	}


	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((amqpHost == null) ? 0 : amqpHost.hashCode());
		result = prime * result + amqpPort;
		return result;
	}


	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AMQPClient))
			return false;
		AMQPClient other = (AMQPClient) obj;
		if (amqpHost == null) {
			if (other.amqpHost != null)
				return false;
		} else if (!amqpHost.equals(other.amqpHost))
			return false;
		if (amqpPort != other.amqpPort)
			return false;
		return true;
	}


	/**
	 * @return the amqpHost
	 */
	public String getAmqpHost() {
		return amqpHost;
	}


	/**
	 * @return the amqpPort
	 */
	public int getAmqpPort() {
		return amqpPort;
	}


	/**
	 * @return the exchange
	 */
	public String getExchange() {
		return exchange;
	}


	/**
	 * @return the exchangeType
	 */
	public String getExchangeType() {
		return exchangeType;
	}


	/**
	 * @return the amqpUser
	 */
	public String getAmqpUser() {
		return amqpUser;
	}


	/**
	 * @return the queueName
	 */
	public String getQueueName() {
		return queueName;
	}


	/**
	 * @return the durableExchange
	 */
	public boolean isDurableExchange() {
		return durableExchange;
	}


	/**
	 * @return the autoDeleteExchange
	 */
	public boolean isAutoDeleteExchange() {
		return autoDeleteExchange;
	}


	/**
	 * @return the serializer
	 */
	public IDeliverySerializer getSerializer() {
		return serializer;
	}


	/**
	 * @return the timeFlush
	 */
	public long getTimeFlush() {
		return timeFlush;
	}


	/**
	 * @return the itemFlush
	 */
	public int getItemFlush() {
		return itemFlush;
	}
	
	
}