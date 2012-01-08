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
package org.helios.tracing.extended.graphite;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.EpochHelper;
import org.helios.helpers.StringHelper;
import org.helios.jmx.threadservices.instrumentation.ThreadInfoCapture;
import org.helios.tracing.AbstractTracerInstanceFactory;
import org.helios.tracing.ITracer;
import org.helios.tracing.ITracerInstanceFactory;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.MetricType;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: GraphiteTracerInstanceFactory</p>
 * <p>Description: Tracer instance factory for Graphite Tracer. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.graphite.GraphiteTracerInstanceFactory</p></code>
 */
public class GraphiteTracerInstanceFactory extends AbstractTracerInstanceFactory implements ITracerInstanceFactory {
	/** The Graphite Server host name or IP address */
	protected String graphiteHost;
	/** The Graphite Server listening port */
	protected int graphitePort;
	/** The reconnect delay in ms. for the built GraphiteClient */
	protected long reconnectDelay = GraphiteClient.DEFAULT_RECONNECT_DELAY;
	/** The queue submission option. Defaults to <code>WAIT</code> */
	protected QueueSubmission submitter = QueueSubmission.WAIT;			
	/** auto connect option */
	protected boolean autoConnect = true;
	/** The queue depth */
	protected int queueDepth = GraphiteClient.DEFAULT_SIZE_TRIGGER;
	
	protected GraphiteClient client = null;
	
	/**
	 * @param client the client to set
	 */
	public void setClient(GraphiteClient client) {
		this.client = client;
	}

	/** the singleton synchronization lock */
	protected static final Object lock = new Object();	

	/**
	 * Creates a configured tracer using all the default settings.
	 */
	public GraphiteTracerInstanceFactory() {
		this(GraphiteClient.DEFAULT_GRAPHITE_HOST, GraphiteClient.DEFAULT_GRAPHITE_PORT);
	}
	
	/**
	 * Creates a configured tracer for the passed host name and port, using the default settings for all other configuration items.
	 * @param graphiteHost The Graphite Server host name or IP address
	 * @param graphitePort he Graphite Server listening port
	 */
	public GraphiteTracerInstanceFactory(String graphiteHost, int graphitePort) {		
		this.graphiteHost = graphiteHost;
		this.graphitePort = graphitePort;		
	}
	
//	/**
//	 * Creates a fully configured GraphiteTracerInstanceFactory
//	 * @param graphiteHost The Graphite Server host name or IP address
//	 * @param graphitePort he Graphite Server listening port
//	 * @param reconnectDelay The reconnect delay in ms. for the built GraphiteClient
//	 * @param submitter The queue submission option name
//	 * @param queueDepth the submission queue depth
//	 */
//	public GraphiteTracerInstanceFactory(String graphiteHost, int graphitePort, long reconnectDelay, String submitter, int queueDepth) {
//		this.graphiteHost = graphiteHost;
//		this.graphitePort = graphitePort;
//		this.reconnectDelay = reconnectDelay;
//		this.submitter = QueueSubmission.valueOf(submitter);
//		this.queueDepth = queueDepth;
//	}
//	
	/**
	 * @return the reconnectDelay
	 */
	public long getReconnectDelay() {
		return reconnectDelay;
	}

	/**
	 * @param reconnectDelay the reconnectDelay to set
	 */
	public void setReconnectDelay(long reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
	}

	/**
	 * @return the submitter
	 */
	public QueueSubmission getSubmitter() {
		return submitter;
	}

	/**
	 * @param submitter the submitter to set
	 */
	public void setSubmitter(QueueSubmission submitter) {
		this.submitter = submitter;
	}

	/**
	 * @return the autoConnect
	 */
	public boolean isAutoConnect() {
		return autoConnect;
	}

	/**
	 * @param autoConnect the autoConnect to set
	 */
	public void setAutoConnect(boolean autoConnect) {
		this.autoConnect = autoConnect;
	}

	/**
	 * @return the queueDepth
	 */
	public int getQueueDepth() {
		return queueDepth;
	}

	/**
	 * @param queueDepth the queueDepth to set
	 */
	public void setQueueDepth(int queueDepth) {
		this.queueDepth = queueDepth;
	}

	/**
	 * @return the graphiteHost
	 */
	public String getGraphiteHost() {
		return graphiteHost;
	}

	/**
	 * @return the graphitePort
	 */
	public int getGraphitePort() {
		return graphitePort;
	}

	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger log = Logger.getLogger(GraphiteTracerInstanceFactory.class);
		log.info("GraphiteTracerInstanceFactory Test");
		GraphiteTracerInstanceFactory factory = new GraphiteTracerInstanceFactory("192.168.1.14", 2003);
		ITracer tracer = factory.getTracer();
		log.info("Created tracer [" + tracer.getTracerName() + "]");
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
	 * @param graphiteHost the graphiteHost to set
	 */
	public void setGraphiteHost(String graphiteHost) {
		this.graphiteHost = graphiteHost;
	}

	/**
	 * @param graphitePort the graphitePort to set
	 */
	public void setGraphitePort(int graphitePort) {
		this.graphitePort = graphitePort;
	}

	/**
	 * @param intervalTraces
	 */
	@Override
	public void submitIntervalTraces(IIntervalTrace... intervalTraces) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @param traces
	 */
	@Override
	public void submitTraces(Trace... traces) {
		if(client!=null) {
			if(traces!=null ) {
				//StringBuilder b = StringHelper.getStringBuilder(flushedItems.size()*20);			
				for(Trace trace: traces) {
					if(trace.getMetricId().getType().isNumber()) {				
						client.submit(StringHelper.fastConcatAndDelim(" ", trace.getFQN().replace(" ", "").replace('(', '_').replace(")", "").replace('@', '_'), trace.getValue().toString(), EpochHelper.getUnixTimeStr(), "\n" ).replace('/', '.'));
						this.intervalTraceSendCounter.incrementAndGet();
						//graphiteClient.submit(StringHelper.fastConcatAndDelim(" ", trace.getFQN(), trace.getValue().toString(), EpochHelper.getUnixTimeStr(), "\n" ).replace('/', '.'));
					}
				}
			}		
		
		}
		
	}
	
	
}
