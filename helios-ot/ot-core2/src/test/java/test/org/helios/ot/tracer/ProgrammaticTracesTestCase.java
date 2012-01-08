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
package test.org.helios.ot.tracer;

import static org.helios.ot.trace.Trace.TRACE_STR_PATTERN;
import static test.org.helios.ot.trace.TraceValueDataGen.testValue;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.StringHelper;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerImpl;
import org.helios.ot.tracer.TracerManager3;
import org.helios.ot.type.MetricType;
import org.helios.time.SystemClock;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.org.helios.ot.util.CountDownPhaseTrigger;

/**
 * <p>Title: ProgrammaticTracesTestCase</p>
 * <p>Description: Test case for the programmatic API trace calls in ITracer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.tracer.ProgrammaticTracesTestCase</code></p>
 */

public class ProgrammaticTracesTestCase {
	
	private static final AtomicLong serial = new AtomicLong(0L);
	public static final Random random = new Random(System.nanoTime());
	public static final String AGENT = ManagementFactory.getRuntimeMXBean().getName();
	public static final String HOST = ManagementFactory.getRuntimeMXBean().getName().split("@")[1];
	public static void log(Object obj) { System.out.println(obj); }
	
	/** Instance logger */
	protected final Logger LOG = Logger.getLogger(getClass());

	
	/** The number of times to execute each inner test case */
	public static int testLoops = 10;
	
	/** A CountDownLatch reference which is counted down by a flush event and awaited on by the test thread when waiting for a flush */
	final AtomicReference<CountDownLatch> flushLatch = new AtomicReference<CountDownLatch>(null);
	/** A CountDownLatch reference which is counted down when a trace is applied to an interval and awaited on by the test thread when waiting for a submission to complete */
	final AtomicReference<CountDownLatch> applyLatch = new AtomicReference<CountDownLatch>(null);
	/** A phase trigger hit by a flush and that acquires the flushed trace for the test thread. */
	final CountDownPhaseTrigger flushTrigger = new CountDownPhaseTrigger(flushLatch, Phase.ATTACHED);

	/** PhaseTrigger tracer */
	final ITracer tracer = TracerManager3.getInstance().getTracer().getPhaseTriggerTracer(flushTrigger);
	/** Tracks the test name */
	@Rule
    public TestName testName= new TestName();

	static {
		BasicConfigurator.configure();
	}

	
	public static int METRIC_TYPE = 1;
	public static int METRIC_FQN = 2;
	public static int METRIC_VALUE = 3;
	public static int METRIC_TS = 4;
	public static int METRIC_NAMESPACE = 5;
	public static int METRIC_HOST = 6;
	public static int METRIC_AGENT = 7;
	
	public ProgrammaticTracesTestCase() {
		SystemClock.setCurrentClock(SystemClock.TEST);
		log("Tracer Classpath:" + TracerImpl.class.getProtectionDomain().getCodeSource().getLocation());
		log("Trace Classpath:" + Trace.class.getProtectionDomain().getCodeSource().getLocation());
	}
	
	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(String, Object, MetricType)}
	 */
	@Test(timeout=3000)
	public void testTrace_String_Object_Metrictype() throws Exception  {
		Trace trace = null;
		for(MetricType type: MetricType.values()) {			
			if(type.isDelta() || type.isSticky()) continue;
			final long ts = SystemClock.tickTestTime();
			final Object testData = testValue(type);
			String point = "Metric#" + serial.incrementAndGet();
			// ==== Invoke trace op here ====
			flushLatch.set(new CountDownLatch(1));
			trace = tracer.trace(point, testData, type).format(tracer).build();
			tracer.traceTrace(trace);
			// ====
			if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
			Assert.assertEquals("The value of the trace", testData, trace.getValue());
			Map<Integer, String> traceMap = mapTrace(trace.toString());
			Assert.assertEquals("The type of the trace", type.name(), traceMap.get(METRIC_TYPE));
			Assert.assertEquals("The namespace of the trace", "", traceMap.get(METRIC_NAMESPACE));
			Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
			Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
			Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
			Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));
			flushLatch.get().await();		
			Trace fTrace = flushTrigger.getFlushedTrace();
			Assert.assertEquals("The value of the trace", testData, fTrace.getValue());
			traceMap = mapTrace(fTrace.toString());
			Assert.assertEquals("The type of the trace", type.name(), traceMap.get(METRIC_TYPE));
			Assert.assertEquals("The namespace of the trace", "", traceMap.get(METRIC_NAMESPACE));
			Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
			Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
			Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
			Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));
			
		}
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(String, Object, String)}
	 */
	@Test
	public void testTrace_String_Object_String()   {
		Trace trace = null;
		for(MetricType type: MetricType.values()) {			
			if(type.isDelta() || type.isSticky()) continue;
			final long ts = SystemClock.tickTestTime();
			final Object testData = testValue(type);
			String point = "Metric#" + serial.incrementAndGet();
			// ==== Invoke trace op here ====
			flushLatch.set(new CountDownLatch(1));
			trace = tracer.trace(point, testData, type.name()).format(tracer).build();
			tracer.traceTrace(trace);
			// ====
			if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
			Assert.assertEquals("The value of the trace", testData, trace.getValue());
			Map<Integer, String> traceMap = mapTrace(trace.toString());
			Assert.assertEquals("The type of the trace", type.name(), traceMap.get(METRIC_TYPE));
			Assert.assertEquals("The namespace of the trace", "", traceMap.get(METRIC_NAMESPACE));
			Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
			Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
			Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
			Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));
		}	
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(String, Object, int)}
	 */
	@Test
	public void testTrace_String_Object_Int()   {
		Trace trace = null;
		for(MetricType type: MetricType.values()) {			
			if(type.isDelta() || type.isSticky()) continue;
			final long ts = SystemClock.tickTestTime();
			final Object testData = testValue(type);
			String point = "Metric#" + serial.incrementAndGet();
			// ==== Invoke trace op here ====
			flushLatch.set(new CountDownLatch(1));
			trace = tracer.trace(point, testData, type.ordinal()).format(tracer).build();
			tracer.traceTrace(trace);
			// ====			
			if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
			Assert.assertEquals("The value of the trace", testData, trace.getValue());
			Map<Integer, String> traceMap = mapTrace(trace.toString());
			Assert.assertEquals("The type of the trace", type.name(), traceMap.get(METRIC_TYPE));
			Assert.assertEquals("The namespace of the trace", "", traceMap.get(METRIC_NAMESPACE));
			Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
			Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
			Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
			Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));
		}			
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(Date, String, String[])}
	 * @@param value The trace value.
	 * @@param metricName The metric name.
	 * @@param nameSpace The metric namespace suffix.
	 */
	@Test
	public void testTrace_Date_String_StringArr()   {
		Trace trace = null;
		final long ts = SystemClock.tickTestTime();
		final Date testData = new Date();
		String point = "Metric#" + serial.incrementAndGet();
		String[] nameSpace = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		// ==== Invoke trace op here ====
		flushLatch.set(new CountDownLatch(1));
		trace = tracer.trace(testData, point, nameSpace);
		// ====			
		if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
		Assert.assertEquals("The value of the trace", testData.getTime(), trace.getValue());
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.TIMESTAMP.name(), traceMap.get(METRIC_TYPE));
		Assert.assertEquals("The namespace of the trace", StringHelper.fastConcatAndDelim("/", nameSpace), traceMap.get(METRIC_NAMESPACE));
		Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
		Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
		Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(int, String, String[])}
	 */
	@Test
	public void testTrace_Int_String_StringArr()   {
		Trace trace = null;
		final long ts = SystemClock.tickTestTime();
		final int testData = random.nextInt();
		String point = "Metric#" + serial.incrementAndGet();
		String[] nameSpace = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		// ==== Invoke trace op here ====
		flushLatch.set(new CountDownLatch(1));
		trace = tracer.trace(testData, point, nameSpace);
		// ====			
		if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
		Assert.assertEquals("The value of the trace", testData, trace.getValue());
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.INT_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertEquals("The namespace of the trace", StringHelper.fastConcatAndDelim("/", nameSpace), traceMap.get(METRIC_NAMESPACE));
		Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
		Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
		Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(long, String, String[])}
	 */
	@Test
	public void testTrace_Long_String_StringArr()   {
		Trace trace = null;
		final long ts = SystemClock.tickTestTime();
		final long testData = random.nextLong();
		String point = "Metric#" + serial.incrementAndGet();
		String[] nameSpace = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		// ==== Invoke trace op here ====
		flushLatch.set(new CountDownLatch(1));
		trace = tracer.trace(testData, point, nameSpace);
		// ====			
		if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
		Assert.assertEquals("The value of the trace", testData, trace.getValue());
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.LONG_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertEquals("The namespace of the trace", StringHelper.fastConcatAndDelim("/", nameSpace), traceMap.get(METRIC_NAMESPACE));
		Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
		Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
		Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));	
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(String, String, String[])}
	 */
	@Test
	public void testTrace_String_String_StringArr()   {
		Trace trace = null;
		final long ts = SystemClock.tickTestTime();
		final String testData = testValue(MetricType.STRING).toString();
		String point = "Metric#" + serial.incrementAndGet();
		String[] nameSpace = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		// ==== Invoke trace op here ====
		flushLatch.set(new CountDownLatch(1));
		trace = tracer.trace(testData, point, nameSpace);
		// ====			
		
		if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
		Assert.assertEquals("The value of the trace", testData, trace.getValue());
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.STRING.name(), traceMap.get(METRIC_TYPE));
		Assert.assertEquals("The namespace of the trace", StringHelper.fastConcatAndDelim("/", nameSpace), traceMap.get(METRIC_NAMESPACE));
		Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
		Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
		Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));	
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(int, String, String[], String[])}
	 */
	@Test
	public void testTrace_Int_String_StringArr_StringArr()   {
		Trace trace = null;
		final long ts = SystemClock.tickTestTime();
		final int testData = random.nextInt();
		String point = "Metric#" + serial.incrementAndGet();
		String[] prefix = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		String[] nameSpace = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		String fullNameSpace = StringHelper.flattenArray("/", prefix, nameSpace);
		// ==== Invoke trace op here ====
		flushLatch.set(new CountDownLatch(1));
		trace = tracer.trace(testData, point, prefix, nameSpace);
		// ====					
		if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
		Assert.assertEquals("The value of the trace", testData, trace.getValue());
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.INT_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertEquals("The namespace of the trace", fullNameSpace, traceMap.get(METRIC_NAMESPACE));
		Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
		Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
		Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));	
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(long, String, String[], String[])}
	 */
	@Test
	public void testTrace_Long_String_StringArr_StringArr()   {
		Trace trace = null;
		final long ts = SystemClock.tickTestTime();
		final long testData = random.nextLong();
		String point = "Metric#" + serial.incrementAndGet();
		String[] prefix = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		String[] nameSpace = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		String fullNameSpace = StringHelper.flattenArray("/", prefix, nameSpace);
		// ==== Invoke trace op here ====
		flushLatch.set(new CountDownLatch(1));
		trace = tracer.trace(testData, point, prefix, nameSpace);
		// ====
		if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
		Assert.assertEquals("The value of the trace", testData, trace.getValue());
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.LONG_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertEquals("The namespace of the trace", fullNameSpace, traceMap.get(METRIC_NAMESPACE));
		Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
		Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
		Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));	
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(Date, String, String[], String[])}
	 */
	@Test
	public void testTrace_Date_String_StringArr_StringArr()   {
		Trace trace = null;
		final long ts = SystemClock.tickTestTime();
		final Date testData = new Date();
		String point = "Metric#" + serial.incrementAndGet();
		String[] prefix = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		String[] nameSpace = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
		String fullNameSpace = StringHelper.flattenArray("/", prefix, nameSpace);
		// ==== Invoke trace op here ====
		flushLatch.set(new CountDownLatch(1));
		trace = tracer.trace(testData, point, prefix, nameSpace);
		// ====
		if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
		Assert.assertEquals("The value of the trace", testData.getTime(), trace.getValue());
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.TIMESTAMP.name(), traceMap.get(METRIC_TYPE));
		Assert.assertEquals("The namespace of the trace", fullNameSpace, traceMap.get(METRIC_NAMESPACE));
		Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
		Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
		Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));
	}

	/**
	 * Test method for {@link org.helios.ot.tracer.ITracer#trace(String, String, String[], String[])}
	 */
	@Test
	public void testTrace_String_String_StringArr_StringArr() throws Exception  {
		try {
			Trace trace = null;
			final long ts = SystemClock.tickTestTime();
			final String testData = testValue(MetricType.STRING).toString();
			String point = "Metric#" + serial.incrementAndGet();
			String[] prefix = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
			String[] nameSpace = new String[]{"NS#" + serial.incrementAndGet(), "NS#" + serial.incrementAndGet()};
			String fullNameSpace = StringHelper.flattenArray("/", prefix, nameSpace);
			// ==== Invoke trace op here ====
			flushLatch.set(new CountDownLatch(1));
			trace = tracer.trace(testData, point, prefix, nameSpace);
			// ====
			if(LOG.isDebugEnabled()) LOG.debug("[" + testName.getMethodName() + "]:" + trace);
			Assert.assertEquals("The value of the trace", testData, trace.getValue());
			Map<Integer, String> traceMap = mapTrace(trace.toString());
			Assert.assertEquals("The type of the trace", MetricType.STRING.name(), traceMap.get(METRIC_TYPE));
			Assert.assertEquals("The namespace of the trace", fullNameSpace, traceMap.get(METRIC_NAMESPACE));
			Assert.assertEquals("The trace host", HOST, traceMap.get(METRIC_HOST));
			Assert.assertEquals("The trace agent", AGENT, traceMap.get(METRIC_AGENT));
			Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + point));			
			Assert.assertEquals("The timestamp of the trace", ts, Long.parseLong(traceMap.get(METRIC_TS)));
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw e;
		}			
	}
	
	

	
	/**
	 * Extracts the individual values of a trace by parsing the toString() of the trace.
	 * @param trace The trace to map
	 * @return A map of trace elements keyed by the METRIC_ constants.
	 */
	static Map<Integer, String> mapTrace(String trace) {
		if(trace==null) throw new IllegalArgumentException("Passed trace string was null", new Throwable());
		Matcher matcher = TRACE_STR_PATTERN.matcher(trace);
		Map<Integer, String> map = new HashMap<Integer, String>(matcher.groupCount());
		if(!matcher.matches()) {
			throw new RuntimeException("No match on trace string [" + trace + "]", new Throwable());
		}
		for(int i = 1; i <= matcher.groupCount(); i++) {
			map.put(i, matcher.group(i));
		}
		String fqn = map.get(METRIC_FQN);
		String[] frags = fqn.split("/");
		if(frags.length<3) throw new RuntimeException("Unexpected condition. FQN has < 3 fragments: [" + fqn + "]", new Throwable() );
		if(frags.length==3) {
			map.put(METRIC_NAMESPACE, "");
		} else {
			StringBuilder b = new StringBuilder();
			for(int i = 2; i < frags.length-1; i++) {
				b.append(frags[i]).append("/");
			}
			b.deleteCharAt(b.length()-1);
			map.put(METRIC_NAMESPACE, b.toString());
		}
		map.put(METRIC_HOST, frags[0]);
		map.put(METRIC_AGENT, frags[1]);
		return map;
	}
	


}
