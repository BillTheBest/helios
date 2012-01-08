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
package test.org.helios.ot.closed;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.ot.deltas.DeltaManager;
import org.helios.ot.endpoint.LocalQueueEndPoint;
import org.helios.ot.subtracer.IntervalTracer;
import org.helios.ot.trace.ClosedMinMaxAvgTrace;
import org.helios.ot.trace.ClosedTrace;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.interval.IntervalAccumulator;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.helios.time.SystemClock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * <p>Title: ClosedTracesTestCase</p>
 * <p>Description: Tests for ClosedTrace serialization and toStrings.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.closed.ClosedTracesTestCase</code></p>
 */
@SuppressWarnings("unchecked")
public class ClosedTracesTestCase {
	protected static TracerManager3 tm;
	protected static ITracer tracer;
	protected static IntervalTracer intervalTracer;
	protected static final String agentName = ManagementFactory.getRuntimeMXBean().getName();
	protected static final Random random = new Random(System.nanoTime());
	protected static final String hostName = agentName.split("@")[1];
	protected static final Logger LOG = Logger.getLogger(ClosedTracesTestCase.class);
	/** Tracks the test name */
	@Rule
    public TestName testName= new TestName();

	@Test
	public void testIntTrace() throws Exception {
		int value = Integer.MAX_VALUE;
		String name = "MyIntValue";
		Trace trace = tracer.trace(value, name, "Ns1", "Ns2");
		ClosedTrace ct = ClosedTrace.newClosedTrace(trace);
		Assert.assertEquals("The trace value", value, ct.getValue());
		Assert.assertEquals("The trace metric name", name, ct.getMetricName());
		Assert.assertArrayEquals("The trace metric namespace", new String[]{"Ns1", "Ns2"}, ct.getNamespace());
		Assert.assertEquals("The trace agent name", agentName, ct.getAgentName());
		Assert.assertEquals("The trace host name", hostName, ct.getHostName());
		
		ClosedTrace ct2 = thereAndBackAgain(ct, ClosedTrace.class);
		Assert.assertEquals("The deserialized trace", ct, ct2);
		Assert.assertEquals("The trace value", ct.getValue(), ct2.getValue());
		
//		LOG.info(ct);
//		LOG.info("========================================");
//		LOG.info(ct2);
	}
	
	@Test
	public void testLongTrace() throws Exception {
		long value = Long.MAX_VALUE;
		String name = "MyLongValue";
		Trace trace = tracer.trace(value, name, "Ns1", "Ns2");
		ClosedTrace ct = ClosedTrace.newClosedTrace(trace);
		Assert.assertEquals("The trace value", value, ct.getValue());
		Assert.assertEquals("The trace metric name", name, ct.getMetricName());
		Assert.assertArrayEquals("The trace metric namespace", new String[]{"Ns1", "Ns2"}, ct.getNamespace());
		Assert.assertEquals("The trace agent name", agentName, ct.getAgentName());
		Assert.assertEquals("The trace host name", hostName, ct.getHostName());
		ClosedTrace ct2 = thereAndBackAgain(ct, ClosedTrace.class);
		Assert.assertEquals("The deserialized trace", ct, ct2);
		Assert.assertEquals("The trace value", ct.getValue(), ct2.getValue());
	}
	
	@Test
	public void testDateTrace() throws Exception {
		long value = System.currentTimeMillis();
		Date myDate = new Date();
		String name = "MyDateValue";
		Trace trace = tracer.trace(myDate, name, "Ns1", "Ns2");
		ClosedTrace ct = ClosedTrace.newClosedTrace(trace);
		Assert.assertEquals("The trace value", value, ct.getValue());
		Assert.assertEquals("The trace metric name", name, ct.getMetricName());
		Assert.assertArrayEquals("The trace metric namespace", new String[]{"Ns1", "Ns2"}, ct.getNamespace());
		Assert.assertEquals("The trace agent name", agentName, ct.getAgentName());
		Assert.assertEquals("The trace host name", hostName, ct.getHostName());
		ClosedTrace ct2 = thereAndBackAgain(ct, ClosedTrace.class);
		Assert.assertEquals("The deserialized trace", ct, ct2);
		Assert.assertEquals("The trace value", ct.getValue(), ct2.getValue());
	}
	
	@Test
	public void testStringTrace() throws Exception {
		String value = ManagementFactory.getRuntimeMXBean().getClassPath();
		String name = "MyStringValue";
		Trace trace = tracer.trace(value, name, "Ns1", "Ns2");
		ClosedTrace ct = ClosedTrace.newClosedTrace(trace);
		Assert.assertEquals("The trace value", value, ct.getValue());
		Assert.assertEquals("The trace metric name", name, ct.getMetricName());
		Assert.assertArrayEquals("The trace metric namespace", new String[]{"Ns1", "Ns2"}, ct.getNamespace());
		Assert.assertEquals("The trace agent name", agentName, ct.getAgentName());
		Assert.assertEquals("The trace host name", hostName, ct.getHostName());
		ClosedTrace ct2 = thereAndBackAgain(ct, ClosedTrace.class);
		Assert.assertEquals("The deserialized trace", ct, ct2);
		Assert.assertEquals("The trace value", ct.getValue(), ct2.getValue());
	}
	
	
	@Test (timeout=10000)
	public void testIntIntervalTrace() throws Exception {
		int[] values = new int[]{Math.abs(random.nextInt()), Math.abs(random.nextInt()), Math.abs(random.nextInt())};
		int[] minMaxAvg = minMaxAvg(values);
		String name = "MyIntValue";
		intervalTracer.resetMapChannel();
		for(int i = 0; i < values.length; i++) {
			intervalTracer.trace(values[i], name, "Ns1", "Ns2");
		}
		intervalTracer.flush();
		Trace trace = LocalQueueEndPoint.nextTrace();
		ClosedTrace ct = ClosedTrace.newClosedTrace(trace);
		Assert.assertEquals("The trace type", ClosedMinMaxAvgTrace.class, ct.getClass());
		Assert.assertEquals("The trace value", minMaxAvg[2], ct.getValue());
		ClosedMinMaxAvgTrace minMaxAvgTrace = (ClosedMinMaxAvgTrace)ct;
		Assert.assertEquals("The trace count", 3, minMaxAvgTrace.getCount());
		Assert.assertEquals("The trace Min", minMaxAvg[0], minMaxAvgTrace.getMin());
		Assert.assertEquals("The trace Max", minMaxAvg[1], minMaxAvgTrace.getMax());
		Assert.assertEquals("The trace Avg", minMaxAvg[2], minMaxAvgTrace.getAvg());
		Assert.assertEquals("The trace metric name", name, ct.getMetricName());
		Assert.assertArrayEquals("The trace metric namespace", new String[]{"Ns1", "Ns2"}, ct.getNamespace());
		Assert.assertEquals("The trace agent name", agentName, ct.getAgentName());
		Assert.assertEquals("The trace host name", hostName, ct.getHostName());
		
		ClosedTrace ct2 = thereAndBackAgain(ct, ClosedTrace.class);
		Assert.assertEquals("The deserialized trace", ct, ct2);
		//Assert.assertEquals("The trace value", ct.getValue(), ct2.getValue());
		
		LOG.info(ct);
		LOG.info("========================================");
		LOG.info(ct2);
	}
	
	@Test(timeout=5000)
	public void testLongIntervalTrace() throws Exception {
		long[] values = new long[]{Math.abs(random.nextLong())/3, Math.abs(random.nextLong())/3, Math.abs(random.nextLong()/3)};
		long[] minMaxAvg = minMaxAvg(values);
		String name = "MyLongValue";
		intervalTracer.resetMapChannel();
		for(int i = 0; i < values.length; i++) {
			intervalTracer.trace(values[i], name, "Ns1", "Ns2");
		}
		Thread.currentThread().join(500);
		intervalTracer.flush();
		Thread.currentThread().join(500);
		Trace trace = LocalQueueEndPoint.nextTrace();
		ClosedTrace ct = ClosedTrace.newClosedTrace(trace);
		LOG.info(ct);
		Assert.assertEquals("The trace type", ClosedMinMaxAvgTrace.class, ct.getClass());
		Assert.assertEquals("The trace value", minMaxAvg[2], ct.getValue());
		ClosedMinMaxAvgTrace minMaxAvgTrace = (ClosedMinMaxAvgTrace)ct;
		Assert.assertEquals("The trace count", 3, minMaxAvgTrace.getCount());
		Assert.assertEquals("The trace Min", minMaxAvg[0], minMaxAvgTrace.getMin());
		Assert.assertEquals("The trace Max", minMaxAvg[1], minMaxAvgTrace.getMax());
		Assert.assertEquals("The trace Avg", minMaxAvg[2], minMaxAvgTrace.getAvg());
		Assert.assertEquals("The trace metric name", name, ct.getMetricName());
		Assert.assertArrayEquals("The trace metric namespace", new String[]{"Ns1", "Ns2"}, ct.getNamespace());
		Assert.assertEquals("The trace agent name", agentName, ct.getAgentName());
		Assert.assertEquals("The trace host name", hostName, ct.getHostName());
		
		ClosedTrace ct2 = thereAndBackAgain(ct, ClosedTrace.class);
		Assert.assertEquals("The deserialized trace", ct, ct2);
		//Assert.assertEquals("The trace value", ct.getValue(), ct2.getValue());
		
//		LOG.info(ct);
//		LOG.info("========================================");
//		LOG.info(ct2);
	}
	
	
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		System.setProperty(IntervalAccumulator.FLUSH_PERIOD_PROP, "0");
		
		tm = TracerManager3.getInstance(TracerManager3.Configuration.getDefaultConfiguration()
				.appendEndPoint(
						LocalQueueEndPoint.getBuilder().size(1024).build()
				)	
			);
		tracer = tm.getTracer();
		intervalTracer = tm.getIntervalTracer();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		tm.shutdown();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		LocalQueueEndPoint.clear();
		DeltaManager.getInstance().reset();
		
		LOG.info("\n\t================================\n\t Test:" +  testName.getMethodName() + "\n\t================================");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}
	
	
	/**
	 * Serializes an object then deserializes and returns it.
	 * @param obj The object
	 * @param type The type of the object
	 * @return The deserialized object
	 * @throws Exception Thrown on any exception
	 * @param <T> The type of the object 
	 */
	protected static <T> T thereAndBackAgain(T obj, Class<T> type)  throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		SystemClock.startTimer();
		oos.writeObject(obj);
		oos.flush();
		baos.flush();
		byte[] bytes = baos.toByteArray();
		int size = bytes.length;
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object obj2 = ois.readObject();
		LOG.info("ThereAndBackAgain: Size[" + size + "]  Elapsed Time[" + SystemClock.endTimer() + "]");
		return type.cast(obj2);		
	}
	
	static long[] minMaxAvg(long[] values) {
		long[] results = new long[3];
		int size = values.length;
		Arrays.sort(values);
		results[0] = values[0];
		results[1] = values[size-1];
//		BigDecimal total = new BigDecimal(0);
//		for(long l: values) {
//			total = total.add(new BigDecimal(l));
//		}
		double total = 0;
		for(long l: values) {
			total += l;
		}
		results[2] = avg(total, size).longValue(); 
		return results;
	}
	
	static int[] minMaxAvg(int[] values) {
		int[] results = new int[3];
		int size = values.length;
		Arrays.sort(values);
		results[0] = values[0];
		results[1] = values[size-1];
		long total = 0L;
		for(long l: values) {
			total += l;
		}
		results[2] = avg(total, size).intValue(); 
		return results;
	}
	
	
	static Number avg(double total, double count) {
		if(total==0 || count==0) return 0;
		double avg = total/count;
		return avg;
	}
	
	static Number avg(BigDecimal total, double count) {
		if(total.intValue()<1 || count==0) return 0;
		return total.divide(new BigDecimal(count), 1, RoundingMode.HALF_EVEN);
	}
	

}
