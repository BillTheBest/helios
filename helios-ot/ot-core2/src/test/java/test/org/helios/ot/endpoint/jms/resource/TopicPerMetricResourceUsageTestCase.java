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
package test.org.helios.ot.endpoint.jms.resource;


import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.Banner;
import org.helios.helpers.JMXHelper;
import org.helios.jmxenabled.threads.HeliosThreadFactory;
import org.helios.jmxenabled.threads.HeliosThreadGroup;
import org.helios.ot.endpoint.JMSEndpoint;
import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.helios.time.SystemClock;
import org.helios.time.SystemClock.ElapsedTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;



/**
 * <p>Title: TopicPerMetricResourceUsageTestCase</p>
 * <p>Description: A resource usage test case to measure the resource utilization of creating a topic per metric.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.endpoint.jms.resource.TopicPerMetricResourceUsageTestCase</code></p>
 */
@Ignore
public class TopicPerMetricResourceUsageTestCase {
	/** ActiveMQ Broker */
	protected static BrokerService brokerService = null;
	/** ActiveMQ Connection Factory */
	protected static ActiveMQConnectionFactory connFactory = null;
	/** Static test logger */
	protected static final Logger LOG = Logger.getLogger(TopicPerMetricResourceUsageTestCase.class);
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
	protected static final HeliosThreadFactory threadFactory = new HeliosThreadFactory(TopicPerMetricResourceUsageTestCase.class.getSimpleName() + "Threads", -1, Thread.NORM_PRIORITY, null, true); 
	/** Background task thread factory */
	protected static final HeliosThreadGroup threadGroup = (HeliosThreadGroup)threadFactory.getThreadGroup(); 
	/** The number of republisher threads */
	protected static final int republisherThreads = 3; 

	/** The test broker URI */
	public static final String BROKER_URI = "vm://helios";
	/** The connection factory URI */
	public static final String CONN_FACTORY_URI = BROKER_URI + "?create=false&waitForStart=3000&marshal=true";
	//public static final String CONN_FACTORY_URI = "tcp://localhost:8185";
	//public static final String CONN_FACTORY_URI = "nio://0.0.0.0:8186";
	
	/** The input JMS Topic Name. */
	public static final String INPUT_TOPIC_NAME = "HELIOS.OT.IN.";
	
	//===============================================
	//  Instance fields
	//===============================================
	/** Tracks the test name */
	@Rule
    public TestName testNameRule = new TestName();
	/** The actual test name */
	protected String testName = null;
	

	
	
	/**
	 * Creates a new TopicPerMetricResourceUsageTestCase
	 */
	public TopicPerMetricResourceUsageTestCase() {
		LOG.info("TopicPerMetricResourceUsageTestCase");
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
		brokerService.setUseJmx(true);
		brokerService.getManagementContext().setCreateConnector(false);
		PolicyMap policyMap = new PolicyMap();
		PolicyEntry policy = new PolicyEntry();
		policy.setEnableAudit(false);
		policy.setMemoryLimit(1024);
		policy.setProducerFlowControl(false);
		policy.setTopicPrefetch(1);
		policy.setReduceMemoryFootprint(true);
		policy.setStrictOrderDispatch(false);
		policy.setUseCache(false);
		policyMap.setDefaultEntry(new PolicyEntry());
		brokerService.setDestinationPolicy(policyMap);
		brokerService.start(true);
		connFactory = new ActiveMQConnectionFactory(CONN_FACTORY_URI);
		destination = new ActiveMQQueue(INPUT_TOPIC_NAME);		
		connection = connFactory.createConnection();
		((ActiveMQConnection)connection).setAlwaysSyncSend(true);
		senderSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
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
	 * Executes a tracing loop with gradually increasing numbers of metrics that are published to 
	 * a topic per metric. At each increase in the number of metrics, System.gc() is called
	 * and then the heap usage is measured.
	 * @throws Exception If any errors occur
	 */
	@Test
	@Ignore
	public void testTopicMemoryUsage() throws Exception {
		new File("/tmp/stats.csv").delete();
		startRepublishers();
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
			
			int loopsPerStep = 10;
			int stepSize = 100;
			// Topic ObjectName: org.apache.activemq:BrokerName=helios,Type=Topic,Destination=helios.metrictree.njw810.1168@njw810.JMS Test Long Trace 11
			TLongArrayList tarr = new TLongArrayList(loopsPerStep); 
			TLongArrayList elapsed = new TLongArrayList(loopsPerStep);
			int metricCount = 100;			
			while(true) {
				
				tarr.clear(loopsPerStep);
				elapsed.clear(loopsPerStep);
				for(int x = 0; x < loopsPerStep; x++) {
					SystemClock.startTimer();
					for(int i = 0; i < metricCount; i++) {
						tracer.trace(RANDOM.nextLong(), "JMS Test Long Trace " + i);
					}
					int topicCount = getTopicCount();					
					ManagementFactory.getMemoryMXBean().gc();
					long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
					
					if(topicCount==metricCount) {
						tarr.add(heapUsed);
					}
					ElapsedTime el = SystemClock.endTimer();
					elapsed.add(el.elapsedMs);
					LOG.info("Tracing Elapsed:" + el + "  TopicCount:" + topicCount + "  Heap:" + heapUsed);
					Thread.currentThread().join(1000);
				}
				// end of step size
				long avgHeap = avg(tarr.toArray()).longValue();
				long avgTime = avg(elapsed.toArray()).longValue();
				writeStats(metricCount, avgTime, avgHeap, (avgHeap/100000));
				LOG.info("Step Complete for StepSize[" + metricCount + "]   Average Heap Used:" + avgHeap + "  Average Elapsed:" + avgTime + " ms.");
				metricCount += stepSize;				
			}
			
			//tm.shutdown();		
	}
	
	protected int getTopicCount() {
		return JMXHelper.getRuntimeHeliosMBeanServer().queryNames(JMXHelper.objectName("org.apache.activemq:BrokerName=helios,Type=Topic,Destination=helios.metrictree.*"), null).size();
	}
	
	protected void writeStats(int metricCount, long avgTime, long avgHeap, long adjustedHeap) throws IOException {
		File f = new File("/tmp/stats.csv");
		boolean exists = f.exists();
		if(!exists) {
			f.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(f, true);
		if(!exists) {
			fos.write("MetricCount, Elapsed, HeapUsed, AdjustedHeapUsed\n".getBytes());
		}
		fos.write(new StringBuilder("" + metricCount).append(",").append(avgTime).append(",").append(avgHeap).append(",").append(adjustedHeap).append("\n").toString().getBytes());
		
	}
	
	/**
	 * Starts a threadpool that listens on the input topic, receives the metric messages
	 * and republishes each metric on its own topic as a header only message
	 * @throws Exception thrown on any error
	 */
	protected void startRepublishers() throws Exception {
		final Connection fconnection = connection; 
		for(int i = 0; i < republisherThreads; i++) {
			final int threadId = i;
			threadFactory.newThread(new Runnable(){
				Session listenerSession = null;
				MessageConsumer consumer = null;
				public void run() {
					LOG.info(">>>> Starting Republisher Thread #" + threadId);
					try {
						listenerSession = fconnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
						consumer = listenerSession.createConsumer(destination);
						consumer.setMessageListener(new Republisher(fconnection));
						LOG.info("<<<< Started Republisher Thread #" + threadId);
					} catch (Exception e) {
						LOG.error("Failed to start republisher #" + threadId, e);
						throw new RuntimeException("Failed to start republisher #" + threadId, e);
					}
				}
			}).start();
		}
	}
	
	
	/**
	 * <p>Title: Republisher</p>
	 * <p>Description: The republisher message listener.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	protected static class Republisher implements MessageListener {
		/** The JMS connection to use */
		protected final Connection connection;
		/** The JMS session */
		protected final Session session;
		/** The message producer */
		protected final MessageProducer producer;
		
		/** A cache of JMS destinations keyed by metric name */
		protected static final Map<String, Destination> DEST_CACHE = new ConcurrentHashMap<String, Destination>(2000);
		
		protected Destination getDestination(String name) {
			Destination d = DEST_CACHE.get(name);
			if(d==null) {
				synchronized(DEST_CACHE) {
					d = DEST_CACHE.get(name);
					if(d==null) {
						d = new ActiveMQTopic(name);
						DEST_CACHE.put(name, d);
					}
				}
			}
			return d;
		}
		
		/**
		 * Creates a new Republisher
		 * @param connection The JMS connection to use
		 */
		public Republisher(Connection connection) throws Exception {
			this.connection = connection;
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			producer = session.createProducer(null);
		}


		public void onMessage(Message message) {
			try {
				Trace[] traces = (Trace[])((ObjectMessage)message).getObject();
				if(traces!=null && traces.length>0) {
					for(Trace trace: traces) {
						
						// =============================================
						// Topic Republisher
						// =============================================
						Message msg = session.createMessage();
						Map<String, Object> headers = trace.getTraceMap();
						
						for(Map.Entry<String, Object> header: headers.entrySet()) {
							msg.setObjectProperty(header.getKey(), header.getValue());
						}
						String destName = "helios.metrictree." + trace.getFQN().replace('/', '.');
						msg.setStringProperty("Dest", destName);			
						producer.send(getDestination(destName), msg);
					}
				}
			} catch (Exception e) {
				LOG.error("Message processing error", e);
			}
		}
	}
	
	static Number avg(long[] values) {
		double total = 0;
		for(long l: values) {
			total += l;
		}
		double count = values.length;
		if(total==0 || count==0) return 0;
		double avg = total/count;
		return avg;
	}


}
