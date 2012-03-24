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

import java.util.Date;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.helios.esper.engine.Engine;
import org.helios.ot.generic.IGenericMetric;
import org.helios.time.SystemClock;
import org.helios.time.SystemClock.ElapsedTime;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EventSender;

/**
 * <p>Title: JMSGenericMetricProcessor</p>
 * <p>Description: Service to receive JMS messages with generic metric data and send to Esper.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.esper.engine.jms.JMSGenericMetricProcessor</code></p>
 */
@ManagedResource(description="Service to receive JMS messages with generic metric data and send to Esper.", objectName="org.helios.esper:service=JMSGenericMetricProcessor")
public class JMSGenericMetricProcessor implements MessageListener {
	/** the JMS Connection Factory */
	protected ConnectionFactory connectionFactory = null;
	/** the JMS Connection  */
	protected Connection connection = null;
	/** the JMS Session */
	protected Session session = null;
	/** The JMS destination to listen on */
	protected Destination destination = null;
	/** the JMS Message Consumder */
	protected MessageConsumer consumer = null;
	
	/** The last elapsed time in ms. to process a message */
	protected long elapsedMs = -1;
	/** The last elapsed time in ns. to process a message */
	protected long elapsedNs = -1;
	/** The last number of metrics received */
	protected int metricCount = 0;
	/** The total number of messages processed */
	protected long totalMessageCount = 0;
	
	/** The last timestamp of the metrics reset */
	protected long resetTime = System.currentTimeMillis(); 
	
	
	/** The helios eszper engine */
	protected Engine engine = null;
	/** The esper runtime */
	protected EPRuntime runtime = null;
	/** The esper event sender */
	protected EventSender sender = null;
	
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	
	/**
	 * Starts the service
	 * @throws Exception
	 */
	@ManagedOperation(description="Starts the service")
	public void start() throws Exception {
		log.info("\n\t=============================================\n\tStarting JMSMetricProcessor\n\t=============================================\n");
		initJms();
		runtime = engine.getEsperRuntime();
		sender = runtime.getEventSender("Metric");		
		log.info("\n\t=============================================\n\tStarted JMSMetricProcessor\n\t=============================================\n");
	}
	
	/**
	 * Stops the service
	 */
	@ManagedOperation(description="Stops the service")
	public void stop() {
		log.info("\n\t=============================================\n\tStopping JMSMetricProcessor\n\t=============================================\n");
		closeJms();	
		sender = null;
		log.info("\n\t=============================================\n\tStopped JMSMetricProcessor\n\t=============================================\n");		
	}
	
	/**
	 * Returns the number of metrics submitted
	 * @return the number of metrics submitted
	 */
	@ManagedAttribute(description="The number of metrics submitted")
	public long getNumEventsEvaluated()  {
		return runtime.getNumEventsEvaluated();
	}
	
	/**
	 * Initializes the JMS subscription
	 */
	protected void initJms() {
		try {
			log.info("Initializing JMS...");
			connection = connectionFactory.createConnection();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			consumer = session.createConsumer(destination);
			consumer.setMessageListener(this);
			connection.start();			
			log.info("JMS Initialized JMS...");
		} catch (JMSException jme) {			
			throw new RuntimeException("Failed to initialize JMS", jme);
		}		
	}
	
	/**
	 * Closes out the JMS subscription
	 */
	protected void closeJms() {
		log.info("Closing out JMS...");
		try { consumer.setMessageListener(null); } catch (Exception e) {}
		try { consumer.close(); } catch (Exception e) {}
		try { session.close(); } catch (Exception e) {}
		try { connection.stop(); } catch (Exception e) {}
		try { connection.close(); } catch (Exception e) {}		
		log.info("JMS Closed Out");
	}
	

	/**
	 * {@inheritDoc}
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	@SuppressWarnings("static-access")
	@Override
	public void onMessage(Message message) {
		SystemClock.currentClock().startTimer();
		try {			
			IGenericMetric[] metrics = (IGenericMetric[])((ObjectMessage)message).getObject();
			totalMessageCount++;
			metricCount = metrics.length;
			for(IGenericMetric igm: metrics) {
				sender.sendEvent(igm);
			}
		} catch (Exception e) {
			log.error("Failed to process message", e);
		} finally {
			ElapsedTime et = SystemClock.currentClock().endTimer();
			elapsedMs = et.elapsedMs;
			elapsedNs = et.elapsedNs;
		}
	}

	/**
	 * Sets the JMS Connection Factory
	 * @param connectionFactory the connectionFactory to set
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * @param destination the destination to set
	 */
	public void setDestination(Destination destination) {
		this.destination = destination;
	}



	/**
	 * @param engine the engine to set
	 */
	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	/**
	 * Returns the last elapsed time for message processing in ms.
	 * @return the elapsedMs
	 */
	@ManagedAttribute(description="The last elapsed time for message processing in ms.")
	public long getElapsedMs() {
		return elapsedMs;
	}

	/**
	 * Returns the last elapsed time for message processing in ns.
	 * @return the elapsedNs
	 */
	@ManagedAttribute(description="The last elapsed time for message processing in ns.")
	public long getElapsedNs() {
		return elapsedNs;
	}

	/**
	 * Returns the last read number of metrics
	 * @return the metricCount
	 */
	@ManagedAttribute(description="The number of metrics in the last message")
	public int getMetricCount() {
		return metricCount;
	}

	/**
	 * Returns the total number of messages processed
	 * @return the totalMessageCount
	 */
	@ManagedAttribute(description="The total number of messages processed")
	public long getTotalMessageCount() {
		return totalMessageCount;
	}
	
	/**
	 * Resets the metrics
	 */
	@ManagedOperation(description="Resets the metrics")
	public void reset() {
		runtime.resetStats();
		totalMessageCount = 0;
		resetTime = System.currentTimeMillis();
	}

	/**
	 * The UTC Long timestamp of the last metric reset
	 * @return the resetTime
	 */
	@ManagedAttribute(description="The UTC Long timestamp of the last metric reset")
	public long getLastResetTime() {
		return resetTime;
	}
	
	/**
	 * The java date of the last metric reset
	 * @return the resetDate
	 */
	@ManagedAttribute(description="The date of the last metric reset")
	public Date getLastResetDate() {
		return new Date(resetTime);
	}
	
}
