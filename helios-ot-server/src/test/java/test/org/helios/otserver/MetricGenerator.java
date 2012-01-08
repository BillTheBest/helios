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
package test.org.helios.otserver;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.ot.endpoint.EndpointConfigException;
import org.helios.ot.endpoint.JMSEndpoint;
import org.helios.ot.trace.interval.IntervalAccumulator;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;

/**
 * <p>Title: MetricGenerator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.otserver.MetricGenerator</code></p>
 */

public class MetricGenerator {
	/** Static Class Logger */
	protected static final Logger LOG = Logger.getLogger(MetricGenerator.class);
	/** ActiveMQ Connection Factory */
	protected static ActiveMQConnectionFactory connFactory = null;
	/** The metric input destination */
	protected static ActiveMQQueue destination = null;

	/** The input JMS Dest Name. */
	public static final String INPUT_NAME = "HELIOS.OT.IN.";
	/** The broker URI */
	public static final String CONN_FACTORY_URI = "nio://localhost:8186";
	/**
	 * Starts the metric generator
	 * @param args none
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		//System.setProperty(IntervalAccumulator.FLUSH_PERIOD_PROP, "" + 5000);
		Logger.getLogger("org.apache.activemq.transport.InactivityMonitor").setLevel(Level.INFO);
		final Random random = new Random(System.nanoTime());
		LOG.info("Generating Metric Load");
		destination = new ActiveMQQueue(INPUT_NAME + ManagementFactory.getRuntimeMXBean().getName().replace('@', '.'));		
		connFactory = new ActiveMQConnectionFactory(CONN_FACTORY_URI);
		final TracerManager3 tm;
		try {
			tm = TracerManager3.getInstance(TracerManager3.Configuration.getDefaultConfiguration()
					.appendEndPoint(
							JMSEndpoint.getBuilder()
							.connectionFactory(connFactory)
							.timeToLive(2000)
							.p2p(true)
							.batchSize(50)
							.processorThreadCount(3)						
							
							.endpointArg("JMSConnection", "setAlwaysSessionAsync", true)
							.build()
					)	
				);
		} catch (EndpointConfigException e1) {
			throw new RuntimeException("Endpoint Configuration Exception", e1);
		}
		ITracer tracer = tm.getTracer();
		
		Set<ITracer> tracers = new HashSet<ITracer>();
		tracers.add(tracer);
		final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		threadMXBean.setThreadCpuTimeEnabled(true);
		threadMXBean.setThreadContentionMonitoringEnabled(true);
		for(int i = 1; i < 10; i++) {			
			tracers.add(tracer.getVirtualTracer("host" + i, "agent" + i));
		}
		for(ITracer t: tracers) {
			final ITracer tr = t;
			Thread tracerThread = new Thread() {
				
				public void run() {
					
					LOG.info("Starting Metric Loader [" + tr.toString() + "]");
					while(true) {
						try {
							ITracer intervalTracer = tm.getIntervalTracer();
							intervalTracer.traceDelta(threadMXBean.getCurrentThreadCpuTime(), "SystemTime", "Threading", Thread.currentThread().getName(), "CPU");
							intervalTracer.traceDelta(threadMXBean.getCurrentThreadUserTime(), "UserTime", "Threading", Thread.currentThread().getName(), "CPU");
							ThreadInfo ti = threadMXBean.getThreadInfo(Thread.currentThread().getId());
							intervalTracer.traceDelta(ti.getBlockedTime(), "BlockTime", "Threading", Thread.currentThread().getName(), "Blocking");
							intervalTracer.traceDelta(ti.getWaitedTime(), "WaitTime", "Threading", Thread.currentThread().getName(), "Waiting");
							intervalTracer.trace(threadMXBean.getDaemonThreadCount(), "Daemon Thread Count", "Threading");
							intervalTracer.trace(threadMXBean.getThreadCount(), "Total Thread Count", "Threading");
							intervalTracer.trace(threadMXBean.getThreadCount() - threadMXBean.getDaemonThreadCount(), "Non Daemon Thread Count", "Threading");
							Thread.currentThread().join(5000);
							final CountDownLatch cdl = new CountDownLatch(1);
							new Thread() {
								public void run() {
									try { Thread.currentThread().join(Math.abs(random.nextInt(20))); } catch (Exception e) {}
									cdl.countDown();
								}
							}.start();
							cdl.await();
						} catch (Exception e) {
							e.printStackTrace(System.err);
						}
					}
				}
			};
			tracerThread.setDaemon(true);
			tracerThread.start();
		}
		
	}

}
