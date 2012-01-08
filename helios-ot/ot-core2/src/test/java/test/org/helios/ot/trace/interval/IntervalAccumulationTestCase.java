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
package test.org.helios.ot.trace.interval;


import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.ot.deltas.DeltaManager;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.trace.IntervalTrace;
import org.helios.ot.trace.MetricId;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.interval.IMinMaxAvgIntervalTraceValue;
import org.helios.ot.tracer.ITracer;
import org.helios.time.SystemClock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import test.org.helios.ot.BaseOpenTraceTestCase;
import test.org.helios.ot.util.CountDownPhaseTrigger;

/**
 * <p>Title: IntervalAccumulationTestCase</p>
 * <p>Description: Test case to validate that interval traces have accumulated traces submitted during an interval as expected.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.trace.interval.IntervalAccumulationTestCase</code></p>
 * <p>TODO:<ol>
 * 	<li>Remaining data types</li>
 *  <li>Read test loops from system props</li>
 *  <li>Configure logging</li>
 *  <li>Implement code coverage</li>
 * </ol>
 */
@Ignore
public class IntervalAccumulationTestCase extends BaseOpenTraceTestCase {
	/** How many traces we generate for an interval test */
	public final int SingleIntervalTraceLoop = 10;
	/** The number of times to execute each inner test case */
	public static int testLoops = 10;
	
	/** The test timeouts */
	//public static final int timeout = Integer.MAX_VALUE;
	public static final int timeout = 3000;
	
	/** A CountDownLatch reference which is counted down by a flush event and awaited on by the test thread when waiting for a flush */
	final AtomicReference<CountDownLatch> flushLatch = new AtomicReference<CountDownLatch>(null);
	/** A CountDownLatch reference which is counted down when a trace is applied to an interval and awaited on by the test thread when waiting for a submission to complete */
	final AtomicReference<CountDownLatch> applyLatch = new AtomicReference<CountDownLatch>(null);
	/** A phase trigger hit by a flush and that acquires the flushed trace for the test thread. */
	final CountDownPhaseTrigger flushTrigger = new CountDownPhaseTrigger(flushLatch, Phase.FLUSHED);
	
	/**
	 * Parameterized test support for IMinMaxInterval type metric accumulation
	 * @param intType true if the type is an int, false if it is a long
	 * @param point The metric name point
	 * @param namespace The metric namespace
	 * @throws Exception Thrown if any error occurs
	 */
	@SuppressWarnings("unchecked")
	protected void parameterizedMinMaxAvgTest(boolean intType, String point, String...namespace) throws Exception {
		long[] iaConfig = getTestIASettings();
		try {
			Assert.assertEquals("The IntervalAccumulator Flush Period", iaConfig[0], ia.getFlushPeriod());
			Assert.assertEquals("The OpenTrace Mod", (int)iaConfig[1], MetricId.getMod());			
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw e;
		}
		String metricName = tracer.buildMetricName(point, namespace);
		flushLatch.set(new CountDownLatch(1));
		applyLatch.set(new CountDownLatch(SingleIntervalTraceLoop));
		//tracer.getPhaseTriggerTracer().clearTriggers();
		ITracer rst = tracer.getPhaseTriggerTracer(
				new CountDownPhaseTrigger(applyLatch, Phase.APPLIED), flushTrigger
				
		).getIntervalTracer();
		LOG.info("Tracer:" + rst.getDefaultName());
		
		long longIndex = 0L, longAvg = 0L, longTotal = 0L, longMax = Long.MIN_VALUE, longMin = Long.MAX_VALUE;		
		int intIndex = 0, intAvg = 0, intTotal = 0, intMax = Integer.MIN_VALUE, intMin = Integer.MAX_VALUE;
		Random random = new Random(System.nanoTime());
		if(intType) {
			Trace trace = null;
			for(;intIndex < SingleIntervalTraceLoop; intIndex++) {
				int value =  Math.abs(random.nextInt(1000));
				if(value>intMax) intMax = value;
				if(value<intMin) intMin = value;				
				trace = rst.trace(value, point, namespace);
				intTotal +=  value;				
				Assert.assertTrue("The trace is an int type", trace.getType().isInt());
			}
		} else {
			Trace trace = null;
			for(;longIndex < SingleIntervalTraceLoop; longIndex++) {
				long value =  Math.abs(random.nextInt());
				if(value>longMax) longMax = value;
				if(value<longMin) longMin = value;								
				trace = rst.trace(value, point, namespace);
				longTotal += value;
				Assert.assertTrue("The trace is a long type", trace.getType().isLong());
			}			
		}		
		if(intType) {
			intAvg = avg(intTotal, SingleIntervalTraceLoop).intValue();
		} else {
			longAvg = avg(longTotal, SingleIntervalTraceLoop).longValue();
		}		
		if(LOG.isDebugEnabled()) LOG.debug("Average Value:" + (intType ? intAvg : longAvg));
		SystemClock.startTimer();
		applyLatch.get().await();
		LOG.info("Apply Latch dropped:" + SystemClock.endTimer().toString());
		ia.flush(); 
		SystemClock.startTimer();
		if(LOG.isDebugEnabled()) LOG.debug("Waiting on Flush Latch [" + System.identityHashCode(flushLatch.get()) + "]" + 
				"\n\tTrigger Instance [" + System.identityHashCode(flushTrigger) + "]"
		);
		flushLatch.get().await();
		IntervalTrace it = (IntervalTrace)flushTrigger.getFlushedTrace();
		Assert.assertNotNull("The interval trace is null", it);
		if(LOG.isDebugEnabled()) LOG.debug("Flush Latch droptracer.geetPhaseTriggerTracer().clearTriggers();ped:" + SystemClock.endTimer().toString());
		if(LOG.isDebugEnabled()) LOG.debug("Received Interval Trace:" + it + "(" + System.identityHashCode(it) + ")");
		
		
		Assert.assertEquals("The interval trace name", metricName, it.getFQN());
		Assert.assertEquals("The interval trace count", SingleIntervalTraceLoop, it.getIntervalTraceValue().getCount());
		if(intType) {
			Assert.assertTrue("The trace is an int type", it.getType().isInt());
			Assert.assertEquals("The interval int trace max", intMax, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().intValue());
			Assert.assertEquals("The interval int trace min", intMin, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().intValue());
			Assert.assertEquals("The interval int trace average", intAvg, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().intValue());			
		} else {
			Assert.assertTrue("The trace is a long type", it.getType().isLong());
			Assert.assertEquals("The interval long trace max", longMax, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().longValue());
			Assert.assertEquals("The interval long trace min", longMin, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().longValue());
			Assert.assertEquals("The interval long trace average", longAvg, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().longValue());						
		}
		
		
		// =======
		// Deferring this. Seperate issue.
		// Currently, there is a race condition between this thread and the disruptor task
		// that publishes the IntervalTrace as an OpenMetricType.
		// We need a phase trigger to drop a latch when this occurs to ensure this thread
		// does not request the OMT before it is published.
		// =======
//		OpenMetricType omt = (OpenMetricType)JMXHelper.getHeliosMBeanServer().getAttribute(JMXMetric.getCategoryObjectNameForMetric(it), it.getMetricName());
//		Assert.assertEquals("OMT metric type name", it.getMetricId().getType().name(), omt.getMetricType());
//		Assert.assertEquals("OMT max", SingleIntervalTraceLoop-1, omt.getMaximum().longValue());
		// =======
		
		
		// =========================================================================
		// We're now emulating a zero trace interval.
		// The average should be 0, since we're not using a sticky type
		// =========================================================================
		flushLatch.set(new CountDownLatch(1));
		
		ia.flush(); 
		SystemClock.startTimer();
		flushLatch.get().await();   
		it = (IntervalTrace)flushTrigger.getFlushedTrace();
		if(LOG.isDebugEnabled()) LOG.debug("2nd Flush Latch dropped:" + SystemClock.endTimer().toString());
		if(LOG.isDebugEnabled()) LOG.debug("Received 2nd Interval Trace:" + it + "(" + System.identityHashCode(it) + ")");		
		Assert.assertEquals("The interval trace name", metricName, it.getFQN());
		Assert.assertEquals("The interval trace count", 0, it.getIntervalTraceValue().getCount());
		if(intType) {
			Assert.assertTrue("The trace is an int type", it.getType().isInt());
			Assert.assertEquals("The interval int trace max", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().intValue());
			Assert.assertEquals("The interval int trace min", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().intValue());
			Assert.assertEquals("The interval int trace average", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().intValue());			
		} else {
			Assert.assertTrue("The trace is a long type", it.getType().isLong());
			Assert.assertEquals("The interval long trace max", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().longValue());
			Assert.assertEquals("The interval long trace min", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().longValue());
			Assert.assertEquals("The interval long trace average", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().longValue());						
		}

		// =======
		// Deferred. See above.
		// =======
//		omt = (OpenMetricType)JMXHelper.getHeliosMBeanServer().getAttribute(JMXMetric.getCategoryObjectNameForMetric(it), it.getMetricName());
//		Assert.assertEquals("OMT metric type name", it.getMetricId().getType().name(), omt.getMetricType());
//		Assert.assertEquals("OMT max", 0, omt.getMaximum().longValue());
		// =======
		
		
		
	}
	
	
	
	
	/**
	 * Parameterized test support for Sticky IMinMaxInterval type metric accumulation
	 * @param intType true if the type is an int, false if it is a long
	 * @param point The metric name point
	 * @param namespace The metric namespace
	 * @throws Exception Thrown if any error occurs
	 */
	@SuppressWarnings("unchecked")
	protected void parameterizedStickyMinMaxAvgTest(boolean intType, String point, String...namespace) throws Exception {
		long[] iaConfig = getTestIASettings();
		try {
			Assert.assertEquals("The IntervalAccumulator Flush Period", iaConfig[0], ia.getFlushPeriod());
			Assert.assertEquals("The OpenTrace Mod", (int)iaConfig[1], MetricId.getMod());			
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw e;
		}
		String metricName = tracer.buildMetricName(point, namespace);

		flushLatch.set(new CountDownLatch(1));
		
		applyLatch.set(new CountDownLatch(SingleIntervalTraceLoop));
		
		tracer.getPhaseTriggerTracer().clearTriggers();
		ITracer rst = tracer.getPhaseTriggerTracer(
				new CountDownPhaseTrigger(applyLatch, Phase.APPLIED), flushTrigger
				
		).getIntervalTracer();
		LOG.info("Tracer:" + rst.getDefaultName());
		
		long longIndex = 0L, longAvg = 0L, longTotal = 0L, longMax = Long.MIN_VALUE, longMin = Long.MAX_VALUE;		
		int intIndex = 0, intAvg = 0, intTotal = 0, intMax = Integer.MIN_VALUE, intMin = Integer.MAX_VALUE;
		Random random = new Random(System.nanoTime());
		if(intType) {
			Trace trace = null;
			for(;intIndex < SingleIntervalTraceLoop; intIndex++) {
				int value =  Math.abs(random.nextInt(1000));
				if(value>intMax) intMax = value;
				if(value<intMin) intMin = value;				
				trace = rst.traceSticky(value, point, namespace);
				intTotal +=  value;				
				Assert.assertTrue("The trace is an int type", trace.getType().isInt());
				Assert.assertTrue("The trace is a sticky type", trace.getType().isSticky());
			}
		} else {
			Trace trace = null;
			for(;longIndex < SingleIntervalTraceLoop; longIndex++) {
				long value =  Math.abs(random.nextInt());
				if(value>longMax) longMax = value;
				if(value<longMin) longMin = value;								
				trace = rst.traceSticky(value, point, namespace);
				longTotal += value;
				Assert.assertTrue("The trace is a long type", trace.getType().isLong());
				Assert.assertTrue("The trace is a sticky type", trace.getType().isSticky());
			}			
		}		
		if(intType) {
			intAvg = avg(intTotal, SingleIntervalTraceLoop).intValue();
		} else {
			longAvg = avg(longTotal, SingleIntervalTraceLoop).longValue();
		}		
		LOG.info("Average Value:" + (intType ? intAvg : longAvg));
		SystemClock.startTimer();
		applyLatch.get().await();
		if(LOG.isDebugEnabled()) LOG.debug("Apply Latch dropped:" + SystemClock.endTimer().toString());
		ia.flush(); 
		SystemClock.startTimer();
		flushLatch.get().await();
		IntervalTrace it = (IntervalTrace)flushTrigger.getFlushedTrace();
		if(LOG.isDebugEnabled()) LOG.debug("Flush Latch dropped:" + SystemClock.endTimer().toString());
		if(LOG.isDebugEnabled()) LOG.debug("Received Interval Trace:" + it + "(" + System.identityHashCode(it) + ")");
		
		
		Assert.assertEquals("The interval trace name", metricName, it.getFQN());
		Assert.assertEquals("The interval trace count", SingleIntervalTraceLoop, it.getIntervalTraceValue().getCount());
		if(intType) {
			Assert.assertTrue("The trace is an int type", it.getType().isInt());
			Assert.assertEquals("The interval int trace max", intMax, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().intValue());
			Assert.assertEquals("The interval int trace min", intMin, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().intValue());
			Assert.assertEquals("The interval int trace average", intAvg, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().intValue());			
		} else {
			Assert.assertTrue("The trace is a long type", it.getType().isLong());
			Assert.assertEquals("The interval long trace max", longMax, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().longValue());
			Assert.assertEquals("The interval long trace min", longMin, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().longValue());
			Assert.assertEquals("The interval long trace average", longAvg, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().longValue());						
		}
		// =========================================================================
		// We're now emulating a zero trace interval.
		// The average should be the same as the prior interval, since we're using a sticky type
		// =========================================================================
		flushLatch.set(new CountDownLatch(1));
		ia.flush(); 
		SystemClock.startTimer();
		flushLatch.get().await();   
		it = (IntervalTrace)flushTrigger.getFlushedTrace();
		if(LOG.isDebugEnabled()) LOG.debug("2nd Flush Latch dropped:" + SystemClock.endTimer().toString());
		if(LOG.isDebugEnabled()) LOG.debug("Received 2nd Interval Trace:" + it + "(" + System.identityHashCode(it) + ")");		
		Assert.assertEquals("The interval trace name", metricName, it.getFQN());
		Assert.assertEquals("The interval trace count", 0, it.getIntervalTraceValue().getCount());
		if(intType) {
			Assert.assertTrue("The trace is an int type", it.getType().isInt());
			Assert.assertEquals("The interval int trace max", intMax, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().intValue());
			Assert.assertEquals("The interval int trace min", intMin, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().intValue());
			Assert.assertEquals("The interval int trace average", intAvg, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().intValue());			
		} else {
			Assert.assertTrue("The trace is a long type", it.getType().isLong());
			Assert.assertEquals("The interval long trace max", longMax, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().longValue());
			Assert.assertEquals("The interval long trace min", longMin, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().longValue());
			Assert.assertEquals("The interval long trace average", longAvg, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().longValue());						
		}
	}

	
	
	
	/**
	 * Tests a sequence of 10 int traces in one interval, listens for the flushed interval metric, and validates it.
	 * Initializes the OT MOD to 1 and the Flush Period to 0 (manual flush).
	 * @throws Exception if any exceptions are thrown
	 */
	@Test(timeout=timeout)
	public void testIntAverageAccumulator_flushPeriod_0_OtMod_1_() throws Exception {
		try {
			currentThreadName = Thread.currentThread().getName();
			Thread.currentThread().setName("TestingThread-" + testName.getMethodName());					
			for(int i = 0; i < 0; i++) {
				parameterizedMinMaxAvgTest(true, "MyIntMetric", "helios", "ot", "ia");
			}
		} finally {
			Thread.currentThread().setName(currentThreadName);
		}
	}
	
	/**
	 * Tests a sequence of 10 long traces in one interval, listens for the flushed interval metric, and validates it.
	 * Initializes the OT MOD to 1 and the Flush Period to 0 (manual flush).
	 * @throws Exception if any exceptions are thrown
	 */
	@Test(timeout=timeout)
	public void testLongAverageAccumulator_flushPeriod_0_OtMod_1_() throws Exception {
		try {
			currentThreadName = Thread.currentThread().getName();
			Thread.currentThread().setName("TestingThread-" + testName.getMethodName());					
			for(int i = 0; i < testLoops; i++) {
				parameterizedMinMaxAvgTest(false, "MyLongMetric", "helios", "ot", "ia");
			}
		} finally {
			Thread.currentThread().setName(currentThreadName);
		}		
	}
	

	
	/**
	 * Tests a sequence of 10 sticky int traces in one interval, listens for the flushed interval metric, and validates it.
	 * Initializes the OT MOD to 1 and the Flush Period to 0 (manual flush).
	 * @throws Exception if any exceptions are thrown
	 */
	@Test(timeout=timeout)
	public void testStickyIntAverageAccumulator_flushPeriod_0_OtMod_1_() throws Exception {
		try {
			currentThreadName = Thread.currentThread().getName();
			Thread.currentThread().setName("TestingThread-" + testName.getMethodName());					
			for(int i = 0; i < testLoops; i++) {
				parameterizedStickyMinMaxAvgTest(true, "MyStickyIntMetric", "helios", "ot", "ia");
			}
		} finally {
			Thread.currentThread().setName(currentThreadName);
		}				
	}
	
	/**
	 * Tests a sequence of 10 sticky long traces in one interval, listens for the flushed interval metric, and validates it.
	 * Initializes the OT MOD to 1 and the Flush Period to 0 (manual flush).
	 * @throws Exception if any exceptions are thrown
	 */
	@Test(timeout=timeout)
	public void testStickyLongAverageAccumulator_flushPeriod_0_OtMod_1_() throws Exception {
		try {
			currentThreadName = Thread.currentThread().getName();
			Thread.currentThread().setName("TestingThread-" + testName.getMethodName());					
			for(int i = 0; i < testLoops; i++) {
				parameterizedStickyMinMaxAvgTest(false, "MyStickyLongMetric", "helios", "ot", "ia");
			}
		} finally {
			Thread.currentThread().setName(currentThreadName);
		}						
	}
	
	
	
	
	
	/**
	 * Tests a sequence of 10 delta int traces in one interval, listens for the flushed interval metric, and validates it.
	 * Initializes the OT MOD to 1 and the Flush Period to 0 (manual flush).
	 * @throws Exception if any exceptions are thrown
	 */
	@Test(timeout=timeout)
	public void testDeltaIntAverageAccumulator_flushPeriod_0_OtMod_1_() throws Exception {
		try {
			currentThreadName = Thread.currentThread().getName();
			Thread.currentThread().setName("TestingThread-" + testName.getMethodName());					
			for(int i = 0; i < testLoops; i++) {
				parameterizedDeltaMinMaxAvgTest(true, "MyDeltaIntMetric", "helios", "ot", "ia");
				DeltaManager.getInstance().reset();
			}
		} finally {
			Thread.currentThread().setName(currentThreadName);
		}
	}
	
	
	/**
	 * Tests a sequence of 10 long traces in one interval, listens for the flushed interval metric, and validates it.
	 * Initializes the OT MOD to 1 and the Flush Period to 0 (manual flush).
	 * @throws Exception if any exceptions are thrown
	 */
	@Test(timeout=timeout)
	public void testDeltaLongAverageAccumulator_flushPeriod_0_OtMod_1_() throws Exception {
		try {
			currentThreadName = Thread.currentThread().getName();
			Thread.currentThread().setName("TestingThread-" + testName.getMethodName());					
			for(int i = 0; i < testLoops; i++) {
				parameterizedDeltaMinMaxAvgTest(false, "MyDeltaLongMetric", "helios", "ot", "ia");
				DeltaManager.getInstance().reset();
			}
		} finally {
			Thread.currentThread().setName(currentThreadName);
		}
	}
	



	/**
	 * Parameterized test support for IMinMaxInterval delta type metric accumulation
	 * @param intType true if the type is an int, false if it is a long
	 * @param point The metric name point
	 * @param namespace The metric namespace
	 * @throws Exception Thrown if any error occurs
	 */
	@SuppressWarnings("unchecked")
	protected void parameterizedDeltaMinMaxAvgTest(boolean intType, String point, String...namespace) throws Exception {
		long[] iaConfig = getTestIASettings();
		try {
			Assert.assertEquals("The IntervalAccumulator Flush Period", iaConfig[0], ia.getFlushPeriod());
			Assert.assertEquals("The OpenTrace Mod", (int)iaConfig[1], MetricId.getMod());			
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw e;
		}
		String metricName = tracer.buildMetricName(point, namespace);
		
		flushLatch.set(new CountDownLatch(1));
		
		applyLatch.set(new CountDownLatch(SingleIntervalTraceLoop-1));
		
		tracer.getPhaseTriggerTracer().clearTriggers();
		ITracer rst = tracer.getPhaseTriggerTracer(
				new CountDownPhaseTrigger(applyLatch, Phase.APPLIED), flushTrigger
				
		).getIntervalTracer();
		LOG.info("Tracer:" + rst.getDefaultName());
		
		long longIndex = 0L, longAvg = 0L, longTotal = 0L, longMax = Long.MIN_VALUE, longMin = Long.MAX_VALUE, longPrev = -1;		
		int intIndex = 0, intAvg = 0, intTotal = 0, intMax = Integer.MIN_VALUE, intMin = Integer.MAX_VALUE, intPrev = -1;
		Random random = new Random(System.nanoTime());
		if(intType) {
			Trace trace = null;
			int value =  Math.abs(random.nextInt(1000));
			intPrev = value;
			for(;intIndex < SingleIntervalTraceLoop; intIndex++) {
				value += Math.abs(random.nextInt(1000));				
				int delta = value - intPrev;
				intPrev = value;
				trace = rst.traceDelta(value, point, namespace);
				if(trace!=null) {
					if(delta>intMax) intMax = delta;
					if(delta<intMin) intMin = delta;									
					intTotal +=  delta;
					Assert.assertEquals("The int delta value", delta, trace.getValue());					
					Assert.assertTrue("The trace is an int type", trace.getType().isInt());
					Assert.assertTrue("The trace is a delta type", trace.getType().isDelta());
				}
			}
		} else {
			Trace trace = null;
			long value =  Math.abs(random.nextInt());
			longPrev = value;
			for(;longIndex < SingleIntervalTraceLoop; longIndex++) {
				value +=  Math.abs(random.nextInt());
				long delta = value - longPrev;
				longPrev = value;				
				trace = rst.traceDelta(value, point, namespace);
				if(trace!=null) {
					if(delta>longMax) longMax = delta;
					if(delta<longMin) longMin = delta;													
					longTotal += delta;
					if(LOG.isDebugEnabled()) LOG.debug("\n\tDelta Trace:\n\t\tValue:" + value + "\n\t\tTrace:" + trace.getTraceValue());
					Assert.assertEquals("The long delta value", delta, trace.getValue());
					Assert.assertTrue("The trace is a long type", trace.getType().isLong());
					Assert.assertTrue("The trace is a delta type", trace.getType().isDelta());
				}
			}			
		}		
		if(intType) {
			intAvg = avg(intTotal, SingleIntervalTraceLoop-1).intValue();
		} else {
			longAvg = avg(longTotal, SingleIntervalTraceLoop-1).longValue();
		}		
		if(LOG.isDebugEnabled()) LOG.debug("Average Value:" + (intType ? intAvg : longAvg));
		SystemClock.startTimer();
		applyLatch.get().await();
		LOG.info("Apply Latch dropped:" + SystemClock.endTimer().toString());
		ia.flush(); 
		SystemClock.startTimer();
		flushLatch.get().await();
		IntervalTrace it = (IntervalTrace)flushTrigger.getFlushedTrace();
		Assert.assertNotNull("The interval trace is null", it);
		if(LOG.isDebugEnabled()) LOG.debug("Flush Latch dropped:" + SystemClock.endTimer().toString());
		if(LOG.isDebugEnabled()) LOG.debug("Received Interval Trace:" + it + "(" + System.identityHashCode(it) + ")");
		
		
		Assert.assertEquals("The interval trace name", metricName, it.getFQN());
		Assert.assertEquals("The interval trace count", SingleIntervalTraceLoop-1, it.getIntervalTraceValue().getCount());
		if(intType) {
			Assert.assertTrue("The trace is an int type", it.getType().isInt());
			Assert.assertEquals("The interval int trace max", intMax, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().intValue());
			Assert.assertEquals("The interval int trace min", intMin, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().intValue());
			Assert.assertEquals("The interval int trace average", intAvg, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().intValue());			
		} else {
			Assert.assertTrue("The trace is a long type", it.getType().isLong());
			Assert.assertEquals("The interval long trace max", longMax, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().longValue());
			Assert.assertEquals("The interval long trace min", longMin, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().longValue());
			Assert.assertEquals("The interval long trace average", longAvg, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().longValue());						
		}
		// =========================================================================
		// We're now emulating a zero trace interval.
		// The average should be 0, since we're not using a sticky type
		// =========================================================================
		flushLatch.set(new CountDownLatch(1));
		ia.flush(); 
		SystemClock.startTimer();
		flushLatch.get().await();   
		it = (IntervalTrace)flushTrigger.getFlushedTrace();
		if(LOG.isDebugEnabled()) LOG.debug("2nd Flush Latch dropped:" + SystemClock.endTimer().toString());
		if(LOG.isDebugEnabled()) LOG.debug("Received 2nd Interval Trace:" + it + "(" + System.identityHashCode(it) + ")");		
		Assert.assertEquals("The interval trace name", metricName, it.getFQN());
		Assert.assertEquals("The interval trace count", 0, it.getIntervalTraceValue().getCount());
		if(intType) {
			Assert.assertTrue("The trace is an int type", it.getType().isInt());
			Assert.assertEquals("The interval int trace max", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().intValue());
			Assert.assertEquals("The interval int trace min", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().intValue());
			Assert.assertEquals("The interval int trace average", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().intValue());			
		} else {
			Assert.assertTrue("The trace is a long type", it.getType().isLong());
			Assert.assertEquals("The interval long trace max", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMaximum().longValue());
			Assert.assertEquals("The interval long trace min", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getMinimum().longValue());
			Assert.assertEquals("The interval long trace average", 0, ((IMinMaxAvgIntervalTraceValue)it.getIntervalTraceValue()).getAverage().longValue());						
		}
	}
	
	
	protected static Number avg(double total, double count) {
		if(total==0 || count==0) return 0;
		else return total/count;
	}
	
	
	

}
