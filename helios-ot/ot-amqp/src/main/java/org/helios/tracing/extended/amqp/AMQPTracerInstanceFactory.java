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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executor;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture;
import org.helios.tracing.AbstractTracerInstanceFactory;
import org.helios.tracing.ITracer;
import org.helios.tracing.ITracerInstanceFactory;
import org.helios.tracing.TracerInstanceFactoryException;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.MetricType;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: AMQPTracerInstanceFactory</p>
 * <p>Description: The tracer factory for the AMQPTracer.</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.amqp.AMQPTracerInstanceFactory</code></p>
 */

public class AMQPTracerInstanceFactory extends AbstractTracerInstanceFactory implements  ITracerInstanceFactory {
	/** The AMQP Host name or IP address */
	protected String amqpHost = AMQPClient.DEFAULT_AMQP_HOST;
	/** The AMQP port */
	protected int amqpPort = AMQPClient.DEFAULT_AMQP_PORT;
	//===========================================================
	/** The AMQP exchange */
	protected String exchange = AMQPClient.DEFAULT_AMQP_EXCHANGE;
	/** The exchange type */
	protected String exchangeType  = AMQPClient.DEFAULT_AMQP_EXCHANGE_TYPE;
	/** The AMQP user name */
	protected String amqpUser = null;
	/** The AMQP password */ 
	protected String amqpPassword = null;
	/** Indicates if the created exchange should be durable */
	protected boolean durableExchange = false;
	/** Indicates if the created exchange should be auto-delete */
	protected boolean autoDeleteExchange = false;
	/** The designated serializer for this client */
	protected volatile IDeliverySerializer serializer = new SmartSerializer();
	
	

	/**
	 * 
	 */
	public AMQPTracerInstanceFactory() {
		super(new AMQPTracingBridge());
	}


	/**
	 * @param args
	 * @throws TracerInstanceFactoryException 
	 */
	public static void main(String[] args) throws TracerInstanceFactoryException {
		BasicConfigurator.configure();
		Logger LOG = Logger.getLogger(AMQPTracerInstanceFactory.class);
		AMQPTracerInstanceFactory fact = new AMQPTracerInstanceFactory();
		fact.amqpHost = "192.168.1.14";
		fact.amqpPort = 5672;
		fact.exchange = "graphite";
		ITracer tracer  = fact.getTracer();
		LOG.info("Created tracer [" + tracer.getTracerName() + "]");
		ThreadMXBean mxThread = ManagementFactory.getThreadMXBean();
		mxThread.setThreadContentionMonitoringEnabled(true);
		mxThread.setThreadCpuTimeEnabled(true);
		while(true) {
			tracer.startThreadInfoCapture(ThreadInfoCapture.CPU+ThreadInfoCapture.WAIT, true);
			tracer.smartTrace(MetricType.STICKY_DELTA_LONG_AVG, "" + System.nanoTime(), "Elapsed Time", "TracingTest", "A", "B");			
			tracer.smartTrace(MetricType.STICKY_LONG_AVG, "" + Runtime.getRuntime().freeMemory(), "Free Memory (bytes)", "JVM", "Memory");
			tracer.smartTrace(MetricType.STICKY_LONG_AVG, "" + Runtime.getRuntime().totalMemory(), "Total Memory (bytes)", "JVM", "Memory");			
			tracer.endThreadInfoCapture("Collector Thread Stats");
			try { Thread.currentThread().join(5); } catch (Exception e) {}
			//log.info("Trace Complete");
		}

	}

	/**
	 * @return the amqpHost
	 */
	public String getAmqpHost() {
		return amqpHost;
	}

	/**
	 * @param amqpHost the amqpHost to set
	 */
	public void setAmqpHost(String amqpHost) {
		this.amqpHost = amqpHost;
	}

	/**
	 * @return the amqpPort
	 */
	public int getAmqpPort() {
		return amqpPort;
	}

	/**
	 * @param amqpPort the amqpPort to set
	 */
	public void setAmqpPort(int amqpPort) {
		this.amqpPort = amqpPort;
	}

	/**
	 * @return the exchange
	 */
	public String getExchange() {
		return exchange;
	}

	/**
	 * @param exchange the exchange to set
	 */
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	/**
	 * @return the exchangeType
	 */
	public String getExchangeType() {
		return exchangeType;
	}

	/**
	 * @param exchangeType the exchangeType to set
	 */
	public void setExchangeType(String exchangeType) {
		this.exchangeType = exchangeType;
	}

	/**
	 * @return the amqpUser
	 */
	public String getAmqpUser() {
		return amqpUser;
	}

	/**
	 * @param amqpUser the amqpUser to set
	 */
	public void setAmqpUser(String amqpUser) {
		this.amqpUser = amqpUser;
	}

	/**
	 * @return the amqpPassword
	 */
	public String getAmqpPassword() {
		return amqpPassword;
	}

	/**
	 * @param amqpPassword the amqpPassword to set
	 */
	public void setAmqpPassword(String amqpPassword) {
		this.amqpPassword = amqpPassword;
	}

	/**
	 * @return the durableExchange
	 */
	public boolean isDurableExchange() {
		return durableExchange;
	}

	/**
	 * @param durableExchange the durableExchange to set
	 */
	public void setDurableExchange(boolean durableExchange) {
		this.durableExchange = durableExchange;
	}

	/**
	 * @return the autoDeleteExchange
	 */
	public boolean isAutoDeleteExchange() {
		return autoDeleteExchange;
	}

	/**
	 * @param autoDeleteExchange the autoDeleteExchange to set
	 */
	public void setAutoDeleteExchange(boolean autoDeleteExchange) {
		this.autoDeleteExchange = autoDeleteExchange;
	}

	/**
	 * @return the serializer
	 */
	public IDeliverySerializer getSerializer() {
		return serializer;
	}

	/**
	 * @param serializer the serializer to set
	 */
	public void setSerializer(IDeliverySerializer serializer) {
		this.serializer = serializer;
	}


	/**
	 * Submits intervalTraces through the tracing bridge
	 * @param An array of intervalTraces
	 */
	@Override
	public void submitIntervalTraces(IIntervalTrace... traceIntervals) {
		this.getBridge().submitIntervalTraces(traceIntervals);
		
	}

	/**
	 * Submits traces through the tracing bridge
	 * @param traces an array of traces
	 */
	@Override
	public void submitTraces(Trace... traces) {
		this.getBridge().submitTraces(traces);
		
	}

}
