package test.org.helios.ot.trace;

import static org.helios.ot.trace.Trace.TRACE_STR_PATTERN;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;

import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.helios.ot.type.MetricType;
import org.junit.Assert;
import org.junit.Test;


/**
 * <p>Title: TraceGenerationTestCase</p>
 * <p>Description: Test case to validate that tracer operations generate traces with the expected values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.trace.TraceGenerationTestCase</code></p>
 */

public class TraceGenerationTestCase {
	public static final Random random = new Random(System.nanoTime());
	public static void log(Object obj) { System.out.println(obj); }
	
	public static int METRIC_TYPE = 1;
	public static int METRIC_FQN = 2;
	public static int METRIC_VALUE = 3;
	public static int METRIC_TS = 4;
	
	
	public static int SOAK_COUNT = 4000000;
	
	/**
	 * Tests an INT_AVG trace operation 
	 */
	@Test
	public void testIntAvgTrace() throws Exception {
		try {
			final int value = random.nextInt();
			String metricPoint = "MyMetric" + value;
			ITracer tracer = TracerManager3.getInstance().getTracer();
			Trace trace = tracer.smartTrace(MetricType.INT_AVG.ordinal(), "" + value, metricPoint);
			log(trace);
			Assert.assertEquals("The value of the trace", value, trace.getValue());
			log("[" + value + "] = [" + trace.getValue() + "]");
			Map<Integer, String> traceMap = mapTrace(trace.toString());
			Assert.assertEquals("The type of the trace", MetricType.INT_AVG.name(), traceMap.get(METRIC_TYPE));
			Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + metricPoint));
			// TODO: Synth clock to check timestamp
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw e;
		}
	}
	
	/**
	 * Tests an DELTA_INT_AVG trace operation 
	 */
	@Test
	public void testDeltaIntAvgTrace() {
		final int value = random.nextInt();
		String metricPoint = "MyMetric" + value;
		ITracer tracer = TracerManager3.getInstance().getTracer();
		Trace trace = tracer.smartTrace(MetricType.DELTA_INT_AVG.ordinal(), "" + value, metricPoint);
		log(trace);
		Assert.assertEquals("The value of the trace", value, trace.getValue());
		log("[" + value + "] = [" + trace.getValue() + "]");
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.DELTA_INT_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + metricPoint));
		// TODO: Synth clock to check timestamp
	}
	
	/**
	 * Tests an STICKY_INT_AVG trace operation 
	 */
	@Test
	public void testStickyIntAvgTrace() {
		final int value = random.nextInt();
		String metricPoint = "MyMetric" + value;
		ITracer tracer = TracerManager3.getInstance().getTracer();
		Trace trace = tracer.smartTrace(MetricType.STICKY_INT_AVG.ordinal(), "" + value, metricPoint);
		log(trace);
		Assert.assertEquals("The value of the trace", value, trace.getValue());
		log("[" + value + "] = [" + trace.getValue() + "]");
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.STICKY_INT_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + metricPoint));
		// TODO: Synth clock to check timestamp
	}
	
	/**
	 * Tests an STICKY_DELTA_INT_AVG trace operation 
	 */
	@Test
	public void testStickyDeltaIntAvgTrace() {
		final int value = random.nextInt();
		String metricPoint = "MyMetric" + value;
		ITracer tracer = TracerManager3.getInstance().getTracer();
		Trace trace = tracer.smartTrace(MetricType.STICKY_DELTA_INT_AVG.ordinal(), "" + value, metricPoint);
		log(trace);
		Assert.assertEquals("The value of the trace", value, trace.getValue());
		log("[" + value + "] = [" + trace.getValue() + "]");
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.STICKY_DELTA_INT_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertTrue("The point of the trace", traceMap.get(METRIC_FQN).endsWith("/" + metricPoint));
		// TODO: Synth clock to check timestamp
	}
	
	/**
	 * Tests an LONG_AVG trace operation 
	 */
	@Test
	public void testLongAvgTrace() {
		final Long value = Long.MAX_VALUE;
		String metricPoLong = "MyMetric" + value;
		ITracer tracer = TracerManager3.getInstance().getTracer();
		Trace trace = tracer.smartTrace(MetricType.LONG_AVG.ordinal(), "" + value, metricPoLong);
		log(trace);
		log("[" + value + "] = [" + trace.getValue() + "]");
		Assert.assertEquals("The value of the trace", value, trace.getValue());
		
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.LONG_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertTrue("The poLong of the trace", traceMap.get(METRIC_FQN).endsWith("/" + metricPoLong));
		// TODO: Synth clock to check timestamp
	}
	
	/**
	 * Tests an DELTA_LONG_AVG trace operation 
	 */
	@Test
	public void testDeltaLongAvgTrace() {
		final Long value = random.nextLong();
		String metricPoLong = "MyMetric" + value;
		ITracer tracer = TracerManager3.getInstance().getTracer();
		Trace trace = tracer.smartTrace(MetricType.DELTA_LONG_AVG.ordinal(), "" + value, metricPoLong);
		log(trace);
		Assert.assertEquals("The value of the trace", value, trace.getValue());
		log("[" + value + "] = [" + trace.getValue() + "]");
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.DELTA_LONG_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertTrue("The poLong of the trace", traceMap.get(METRIC_FQN).endsWith("/" + metricPoLong));
		// TODO: Synth clock to check timestamp
	}
	
	/**
	 * Tests an STICKY_LONG_AVG trace operation 
	 */
	@Test
	public void testStickyLongAvgTrace() {
		final Long value = random.nextLong();
		String metricPoLong = "MyMetric" + value;
		ITracer tracer = TracerManager3.getInstance().getTracer();
		Trace trace = tracer.smartTrace(MetricType.STICKY_LONG_AVG.ordinal(), "" + value, metricPoLong);
		log(trace);
		Assert.assertEquals("The value of the trace", value, trace.getValue());
		log("[" + value + "] = [" + trace.getValue() + "]");
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.STICKY_LONG_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertTrue("The poLong of the trace", traceMap.get(METRIC_FQN).endsWith("/" + metricPoLong));
		// TODO: Synth clock to check timestamp
	}
	
	/**
	 * Tests an STICKY_DELTA_LONG_AVG trace operation 
	 */
	@Test
	public void testStickyDeltaLongAvgTrace() {
		final Long value = random.nextLong();
		String metricPoLong = "MyMetric" + value;
		ITracer tracer = TracerManager3.getInstance().getTracer();
		Trace trace = tracer.smartTrace(MetricType.STICKY_DELTA_LONG_AVG.ordinal(), "" + value, metricPoLong);
		log(trace);
		Assert.assertEquals("The value of the trace", value, trace.getValue());
		log("[" + value + "] = [" + trace.getValue() + "]");
		Map<Integer, String> traceMap = mapTrace(trace.toString());
		Assert.assertEquals("The type of the trace", MetricType.STICKY_DELTA_LONG_AVG.name(), traceMap.get(METRIC_TYPE));
		Assert.assertTrue("The poLong of the trace", traceMap.get(METRIC_FQN).endsWith("/" + metricPoLong));
		// TODO: Synth clock to check timestamp
	}
	
	
	
	
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
		return map;
	}
	
	static long per(double total, double time) {
		double rate = time/total;
		return (long)rate;
		
	}

}


//for(int i = 0; i < 10000; i++) {
//	tracer.smartTrace(MetricType.INT_AVG.ordinal(), "" + value, metricPoint);
//}
//log("Starting Soak Test");
//long start = System.nanoTime();
//for(int i = 0; i < SOAK_COUNT; i++) {
//	tracer.smartTrace(MetricType.INT_AVG.ordinal(), "" + value, metricPoint);
//}
//long elapsed = System.nanoTime()-start;
//log("Elapsed time for [" + SOAK_COUNT + "] is [" + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS) + "] ms. Per:" + TimeUnit.MICROSECONDS.convert(per(SOAK_COUNT, elapsed), TimeUnit.NANOSECONDS) + " us.");
