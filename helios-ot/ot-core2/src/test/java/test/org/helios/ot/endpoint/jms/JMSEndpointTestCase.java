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
package test.org.helios.ot.endpoint.jms;


import gnu.trove.list.array.TLongArrayList;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.Banner;
import org.helios.helpers.JMXHelper;
import org.helios.jmxenabled.threads.HeliosThreadFactory;
import org.helios.jmxenabled.threads.HeliosThreadGroup;
import org.helios.ot.endpoint.JMSEndpoint;
import org.helios.ot.subtracer.IntervalTracer;
import org.helios.ot.trace.IntervalTrace;
import org.helios.ot.trace.MetricId;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.interval.IntervalAccumulator;
import org.helios.ot.trace.types.interval.IIntervalTraceValue;
import org.helios.ot.trace.types.interval.IntIntervalTraceValue;
import org.helios.ot.trace.types.interval.LongIntervalTraceValue;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.helios.time.SystemClock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * <p>Title: JMSEndpointTestCase</p>
 * <p>Description: Unit tests for the JMSEndpoint</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.endpoint.jms.JMSEndpointTestCase</code></p>
 */
@Ignore
public class JMSEndpointTestCase {
	/** ActiveMQ Broker */
	protected static BrokerService brokerService = null;
	/** ActiveMQ Connection Factory */
	protected static ActiveMQConnectionFactory connFactory = null;
	/** Static test logger */
	protected static final Logger LOG = Logger.getLogger(JMSEndpointTestCase.class);
	/** Random instance */
	protected static final Random RANDOM = new Random(System.nanoTime());
	/** JMS Connection */
	protected static Connection connection = null;
	/** JMS Sender Sessions */
	protected static Session senderSession = null;
	/** JMS Listener Session */
	protected static Session listenerSession = null;
	/** JMS Listener Consumer */
	protected static MessageConsumer consumer = null;
	/** JMS Sender Producer Consumer */
	protected static MessageProducer producer = null;
	/** JMS Destination */
	protected static Destination destination = null;
	/** Test start time */
	protected long testStartTime = -1L;
	/** Background task thread factory */
	protected static final HeliosThreadFactory threadFactory = new HeliosThreadFactory(JMSEndpointTestCase.class.getSimpleName() + "Threads", -1, Thread.NORM_PRIORITY, null, true); 
	/** Background task thread factory */
	protected static final HeliosThreadGroup threadGroup = (HeliosThreadGroup)threadFactory.getThreadGroup(); 
	
	

	
	/** The test broker URI */
	public static final String BROKER_URI = "vm://helios";
	/** The connection factory URI */
	//public static final String CONN_FACTORY_URI = BROKER_URI + "?create=false&waitForStart=3000&marshal=true";
	//public static final String CONN_FACTORY_URI = "tcp://localhost:8185";
	public static final String CONN_FACTORY_URI = "nio://localhost:8186";
	
	
	/** The test JMS Queue Name */
	public static final String QUEUE_NAME = "HELIOS.OT.IN";
	
	//===============================================
	//  Instance fields
	//===============================================
	/** Tracks the test name */
	@Rule
    public TestName testNameRule = new TestName();
	/** The actual test name */
	protected String testName = null;
	

	
	
	public JMSEndpointTestCase() {
		LOG.info("JMSEndpointTestCase");
	}
	
	
	
	/**
	 * Creates and starts an ActiveMQ JMS instance
	 * @throws java.lang.Exception thrown on any errors setting up the broker
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Logger.getRootLogger().removeAllAppenders();
		BasicConfigurator.configure();
		brokerService = new BrokerService();
		brokerService.addConnector(BROKER_URI);
		brokerService.setDeleteAllMessagesOnStartup(true);
		brokerService.setPersistent(false);
		brokerService.setBrokerName("helios");
		brokerService.setBrokerId("helios");
		brokerService.setUseJmx(false);
		brokerService.getManagementContext().setCreateConnector(false);
		brokerService.start(true);
		connFactory = new ActiveMQConnectionFactory(CONN_FACTORY_URI);
		destination = new ActiveMQQueue(QUEUE_NAME);		
		connection = connFactory.createConnection();
		((ActiveMQConnection)connection).setAlwaysSyncSend(true);
		senderSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		listenerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		consumer = listenerSession.createConsumer(destination);
		producer = senderSession.createProducer(destination);
		connection.start();
	}

	/**
	 * Stops the create broker
	 * @throws java.lang.Exception thrown on any errors stopping the broker
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if(producer!=null) try { producer.close(); } catch (Exception e) {}
		if(consumer!=null) try { consumer.close(); } catch (Exception e) {}
		if(listenerSession!=null) try { listenerSession.close(); } catch (Exception e) {}
		if(senderSession!=null) try { senderSession.close(); } catch (Exception e) {}
		if(connection!=null) try { connection.close(); } catch (Exception e) {}
		if(brokerService!=null) brokerService.stop();
	}

	/**
	 * Executed before each test
	 * @throws java.lang.Exception If any errors occur
	 */
	@Before
	public void setUp() throws Exception {
		testName = testNameRule.getMethodName();
		LOG.info(Banner.banner("*", 1, 10, "Test Case [" + testName + "]"));
		testStartTime = System.currentTimeMillis();
	}

	/**
	 * Cleans up any resources after each test
	 * @throws java.lang.Exception If any errors occur
	 */
	@After
	public void tearDown() throws Exception {
		threadGroup.stop();
		long elapsed = System.currentTimeMillis() - testStartTime;
		LOG.info(Banner.banner("*", 1, 10, "Test Case [" + testName + "]", "Elapsed:" + elapsed + " ms."));
		
	}
	
	/**
	 * Tests that JMS is setup correctly
	 * @throws java.lang.Exception If any errors occur
	 */
	@Test(timeout=1000)
	public void testJMSSetup() throws Exception {
		final String testMessage = testName + RANDOM.nextLong();
		producer.send(senderSession.createTextMessage(testMessage));
		Message msg = consumer.receive();
		Assert.assertTrue("The message is a TextMessage", msg instanceof TextMessage);
		TextMessage tm = (TextMessage)msg;
		LOG.info("Message:\n" + msg);
		Assert.assertEquals("The test message", testMessage, tm.getText());
	}
	
	
	/**
	 * Validates a simple round trip of one trace using the JMSEndpoint
	 * @throws Exception If any errors occur
	 */
	@Test(timeout=5000)
	public void testBasicTypeTraces() throws Exception {
		TracerManager3.getInstance();
		TracerManager3 tm = TracerManager3.getInstance(TracerManager3.Configuration.getDefaultConfiguration()
			.appendEndPoint(
					JMSEndpoint.getBuilder().destination(destination)
					.connectionFactory(connFactory)					
					.build()
			)	
		);
		ITracer tracer = tm.getTracer();
		LOG.info("Calling [JMS Test Date Trace]");
		validateJMSTrace(tracer.trace(new Date(), "JMS Test Date Trace"));
		LOG.info("Calling [JMS Test Int Trace]");
		validateJMSTrace(tracer.trace(RANDOM.nextInt(), "JMS Test Int Trace"));
		LOG.info("Calling [JMS Test Long Trace]");
		validateJMSTrace(tracer.trace(RANDOM.nextLong(), "JMS Test Long Trace"));
		LOG.info("Calling [JMS Test String Trace]");
		validateJMSTrace(tracer.trace("Foo", "JMS Test String Trace"));
		tm.shutdown();
	}
	
	/**
	 * Validates a simple round trip of many traces using the JMSEndpoint
	 * @throws Exception If any errors occur
	 */
	@Test //(timeout=5000)
	@Ignore
	public void testBasicVolume() throws Exception {
		TracerManager3 tm = TracerManager3.getInstance(TracerManager3.Configuration.getDefaultConfiguration()
				.appendEndPoint(
						JMSEndpoint.getBuilder().destination(destination)
						.connectionFactory(connFactory)
						.batchSize(50)
						.processorThreadCount(3)
						.build()
				)	
			);
			ITracer tracer = tm.getTracer();
			final int messageCount = 10000;
			SystemClock.startTimer();
			final MessageConsumer mc = consumer;
			threadFactory.newThread(new Runnable(){
				public void run() {
					SystemClock.startTimer();
					for(int i = 0; i < messageCount; i++) {
						try {
							mc.receive();
						} catch (Exception e) {}
					}
					LOG.info("Receives Elapsed:" + SystemClock.endTimer());
				}				
			}).start();
			for(int i = 0; i < messageCount; i++) {
				tracer.trace(RANDOM.nextLong(), "JMS Test Long Trace");
			}
			LOG.info("Tracing Elapsed:" + SystemClock.endTimer());
			tm.shutdown();		
	}
	
	/**
	 * Receives a message from the queue and compares the payload to the passed trace
	 * @param trace The trace to compare to 
	 * @throws Exception If any errors occur
	 */
	@SuppressWarnings("unchecked")
	protected void validateJMSTrace(Trace trace) throws Exception {
		Message msg = null;
		try {
			msg = consumer.receive();
			Assert.assertTrue("The message is an ObjectMessage", msg instanceof ObjectMessage);
			ObjectMessage om = (ObjectMessage)msg;		
			Assert.assertEquals("The object message's object type", Trace[].class, om.getObject().getClass());
			Trace[] payload = (Trace[])om.getObject();	
			LOG.info("Received Message for [" + payload[0].getMetricName() + "]");
			Assert.assertEquals("The Object Array Length for [" + trace.getMetricName() + "]", 1, payload.length);
			Assert.assertTrue("The Object Array Item [0] is the Trace for [" + trace.getMetricName() + "]", trace.equals(payload[0]));
		} finally {
			if(msg!=null) try { msg.acknowledge(); } catch (Exception e) {}
		}		
	}
	
	/**
	 * Tests a loop of interval long average traces
	 * @throws Exception thrown on any exception
	 */
	@Test (timeout=100000)
	@Ignore  // FAILS on Cloudbees
	public void intervalLongAveragedTest() throws Exception {
		final int metricNameCount = 10;
		final int metricCount = 1000;
		intervalTest(new LoopTracer(metricCount) {
			public void trace() {
				long nano = RANDOM.nextInt(Integer.MAX_VALUE);
				String metricName = "JMS Test Long Interval Trace " + (nano%metricNameCount);
				tracer.trace(nano, metricName);
				addInstance(metricName, nano);
			}
			public void test(String metricName, Trace trace, int iteration) {
				IntervalTrace it = (IntervalTrace)trace;
				LongIntervalTraceValue litv =  (LongIntervalTraceValue)it.getIntervalTraceValue();
				TLongArrayList arr = metricValues.get(metricName);
				Assert.assertNotNull("The accumulated metric value array for [" + metricName + "] iteration [" + iteration + "]", arr);
				Assert.assertEquals("The number of accumulated metric values for [" + metricName + "] iteration [" + iteration + "]", metricCount, this.addedInstanceCount);				
				Assert.assertEquals("The provided metric name vs. the trace metric name", metricName, trace.getMetricName());
				long[] minMaxAvg = minMaxAvg(arr.toArray());
				Assert.assertEquals("The MIN value for [" + metricName + "] iteration [" + iteration + "]", minMaxAvg[0], litv.getMin());
				Assert.assertEquals("The MAX value for [" + metricName + "] iteration [" + iteration + "]", minMaxAvg[1], litv.getMax());
				Assert.assertEquals("The AVG value for [" + metricName + "] iteration [" + iteration + "]", minMaxAvg[2], litv.getAvg());
				
			}
		});
	}
	
	/**
	 * Tests a loop of interval int average traces
	 * @throws Exception thrown on any exception
	 */
	@Test (timeout=10000)
	@Ignore  // FAILS on Cloudbees
	public void intervalIntAveragedTest() throws Exception {
		final int metricNameCount = 10;
		final int metricCount = 1000;
		intervalTest(new LoopTracer(metricCount) {
			public void trace() {
				int nano = RANDOM.nextInt(Integer.MAX_VALUE);
				String metricName = "JMS Test Int Interval Trace " + (nano%metricNameCount);
				tracer.trace(nano, metricName);
				addInstance(metricName, nano);
			}
			public void test(String metricName, Trace trace, int iteration) {
				IntervalTrace it = (IntervalTrace)trace;
				IntIntervalTraceValue litv =  (IntIntervalTraceValue)it.getIntervalTraceValue();
				TLongArrayList arr = metricValues.get(metricName);
				Assert.assertNotNull("The accumulated metric value array for [" + metricName + "] iteration [" + iteration + "]", arr);
				Assert.assertEquals("The number of accumulated metric values for [" + metricName + "] iteration [" + iteration + "]", metricCount, this.addedInstanceCount);				
				Assert.assertEquals("The provided metric name vs. the trace metric name", metricName, trace.getMetricName());
				long[] minMaxAvg = minMaxAvg(arr.toArray());
				Assert.assertEquals("The MIN value for [" + metricName + "] iteration [" + iteration + "]", minMaxAvg[0], litv.getMin());
				Assert.assertEquals("The MAX value for [" + metricName + "] iteration [" + iteration + "]", minMaxAvg[1], litv.getMax());
				Assert.assertEquals("The AVG value for [" + metricName + "] iteration [" + iteration + "]", minMaxAvg[2], litv.getAvg());
				
			}
		});
	}
	
	
	public static abstract class LoopTracer {
		ITracer tracer;		
		final int metricCount;
		final Map<String, TLongArrayList> metricValues; 				
		int addedInstanceCount = 0;
		
		public LoopTracer(int metricCount) {			
			this.metricCount = metricCount;
			metricValues = new HashMap<String, TLongArrayList>();
		}
		
		int getMetricNameCount() {
			return metricValues.size();
		}
		
		public void addInstance(String metricName, long value) {
			TLongArrayList values = metricValues.get(metricName);
			if(values==null) {
				values = new TLongArrayList(metricCount);
				metricValues.put(metricName, values);
			}
			values.add(value);
			addedInstanceCount++;
		}
		
		public void reset() {
			for(TLongArrayList arr: metricValues.values()) {
				arr.clear(metricCount);
			}
			addedInstanceCount = 0;
		}
		
		
		public void setTracer(ITracer tracer) {
			this.tracer = tracer;
		}
		
		public abstract void trace();
		
		public abstract void test(String metricName, Trace trace, int iteration);
	}
	
	/**
	 * Executes the passed runnable within an interval test loop
	 * @param runnable the runnable to execute traces
	 * @throws Exception thrown on any error
	 */
	@SuppressWarnings("unchecked")
	public void intervalTest(LoopTracer loopTracer) throws Exception {
		//TracerManager3.getInstance().shutdown();
		JMXHelper.getRuntimeHeliosMBeanServer().invoke(JMXHelper.objectName("org.apache.activemq:BrokerName=helios,Type=Queue,Destination=HELIOS.OT.IN"), "purge", new Object[]{}, new String[]{});
		System.setProperty(IntervalAccumulator.FLUSH_PERIOD_PROP, "0");
		System.setProperty(MetricId.MOD_PROP, "1");
		TracerManager3 tm = TracerManager3.getInstance(TracerManager3.Configuration.getDefaultConfiguration()
				.appendEndPoint(
						JMSEndpoint.getBuilder().destination(destination)						
						.connectionFactory(connFactory)
						.build()
				)	
			);
		final int messageCount = loopTracer.metricCount;
		final int loopCount = 2;
		IntervalTracer tracer = tm.getIntervalTracer();
		loopTracer.setTracer(tracer);
		for(int x = 0; x < loopCount; x++) {
			for(int i = 0; i < messageCount; i++) {
				loopTracer.trace();
			}
			int metricNameCount = loopTracer.getMetricNameCount();
			Long queueDepth = (Long)JMXHelper.getRuntimeHeliosMBeanServer().getAttribute(JMXHelper.objectName("org.apache.activemq:BrokerName=helios,Type=Queue,Destination=HELIOS.OT.IN"), "QueueSize");
			Assert.assertEquals("The number of messages in the queue before flush call", 0, queueDepth.longValue());
			tracer.flush();
//			long[] minMaxAvg = minMaxAvg(loopTracer.values.toArray());
//			int metricNameCount = loopTracer.metricNames.size();
//			loopTracer.metricNames.clear();
//			loopTracer.values.clear(messageCount);
//			Long queueDepth = (Long)JMXHelper.getRuntimeHeliosMBeanServer().getAttribute(JMXHelper.objectName("org.apache.activemq:BrokerName=helios,Type=Queue,Destination=HELIOS.OT.IN"), "QueueSize");
			Map<String, Trace> retrievedTraces = new HashMap<String, Trace>(metricNameCount);
			int cntr = 0;
			while(retrievedTraces.size()<metricNameCount) {
				ObjectMessage msg = (ObjectMessage)consumer.receive();				
				queueDepth = (Long)JMXHelper.getRuntimeHeliosMBeanServer().getAttribute(JMXHelper.objectName("org.apache.activemq:BrokerName=helios,Type=Queue,Destination=HELIOS.OT.IN"), "QueueSize");
				
				Trace[] traces = (Trace[])msg.getObject();
				for(Trace trace: traces) {
					retrievedTraces.put(trace.getMetricName(), trace);
					cntr++;
				}
				LOG.info("------>Dequeued [" + cntr + "] of [" + metricNameCount + "] expected, msg in queue [" + queueDepth + "]");
			}
			Assert.assertEquals("The number of traces", metricNameCount, retrievedTraces.size());
			for(Map.Entry<String, Trace> entry: retrievedTraces.entrySet()) {
				loopTracer.test(entry.getKey(), entry.getValue(), x);
			}
			loopTracer.reset();
			retrievedTraces.clear();
			queueDepth = (Long)JMXHelper.getRuntimeHeliosMBeanServer().getAttribute(JMXHelper.objectName("org.apache.activemq:BrokerName=helios,Type=Queue,Destination=HELIOS.OT.IN"), "QueueSize");
			//Assert.assertEquals("The number of messages in the queue after flush test", 0, queueDepth.longValue());
			//LOG.info("Received Flushed Message #" + x);
			//Assert.assertEquals("The number of traces", 3, traces.length);
			//Assert.assertNotNull("The Trace Array Instance 0", traces[0]);			
		}		
		// Give the IA a chance to finish flushing.
		Thread.currentThread().join(2000);
	}
	
	static long[] minMaxAvg(long[] values) {
		long[] results = new long[3];
		int size = values.length;
		Arrays.sort(values);
		results[0] = values[0];
		results[1] = values[size-1];
		long total = 0L;
		for(long l: values) {
			total += l;
		}
		results[2] = avg(total, size).longValue(); 
		return results;
	}
	
	static Number avg(double total, double count) {
		if(total==0 || count==0) return 0;
		double avg = total/count;
		return avg;
	}
	

	
	
	@Test
	//@Ignore
	public void stallTest() throws Exception {
		TracerManager3 tm = TracerManager3.getInstance(TracerManager3.Configuration.getDefaultConfiguration()
				.appendEndPoint(
						JMSEndpoint.getBuilder()
						.connectionFactory(connFactory)
						.p2p(true)
						.batchSize(50)
						.processorThreadCount(3)						
						.endpointArg("JMSConnection", "setAlwaysSessionAsync", true)
						.build()
				)	
			);
		final int messageCount = Integer.MAX_VALUE;
		final MessageConsumer mc = consumer;
		new Thread() {
			public void run() {				
				for(int i = 0; i < messageCount; i++) {
					try {
						mc.receive();
					} catch (Exception e) {}
				}				
			}
		}.start();
		final String[] NAMES = new String[]{
				"JMX/Counter1",
				"JMX/Counter2",
				"JMX/Counter3",
				"OS/Metric1",
				"OS/Metric2",
				"OS/Metric3",
				"Process/Stat1",
				"Process/Stat2",
				"Process/Stat3",
				"Health/A"
		};
		new Thread() {
			IntervalTracer tracer = TracerManager3.getInstance().getIntervalTracer();
			public void run() {				
				for(int i = 0; i < messageCount; i++) {		
					long value = Math.abs(RANDOM.nextLong());
					tracer.trace(value, "Reading", NAMES[i%10]);
					try { Thread.currentThread().join(10); } catch (Exception e) {}
				}				
			}
		}.start();
		
		Thread.currentThread().join();
	}

}
