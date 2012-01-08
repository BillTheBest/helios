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
package test.org.helios.ot;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.ot.deltas.DeltaManager;
import org.helios.ot.trace.MetricId;
import org.helios.ot.trace.interval.IntervalAccumulator;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * <p>Title: BaseOpenTraceTestCase</p>
 * <p>Description: Abstract unit test case to serve as base class for OpenTrace unit tests. Provides default OpenTrace lifecycle control for test case classes and methods.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.BaseOpenTraceTestCase</code></p>
 */
@Ignore
public class BaseOpenTraceTestCase {
	/** Instance logger */
	protected final Logger LOG = Logger.getLogger(getClass());
	/** Interval accumulator service handle */
	protected IntervalAccumulator ia;
	/** The root tracer */
	protected ITracer tracer = null;
	
	protected String currentThreadName = null;
	
	/** Tracks the test name */
	@Rule
    public TestName testName= new TestName();
	/** Pattern matcher to reset the flush period of the IntervalAccumulator for the specific test */
	public static final Pattern FLUSH_PERIOD = Pattern.compile("_flushPeriod_(\\d+).*?");
	/** Pattern matcher to reset the mod of the metricId for the specific test */
	public static final Pattern OT_MOD = Pattern.compile("_OtMod_(\\d+).*?");
	
	
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		String methodName = testName.getMethodName();
//		currentThreadName = Thread.currentThread().getName();
//		Thread.currentThread().setName("TestingThread-" + methodName);		
		LOG.debug("\n\t******\n\t Setup [" + getClass().getSimpleName() + "." + methodName + "]\n\t ******");
		MetricId.reset();
		//ia.getInstance().stop();		
		testForIntInMethodName(methodName, OT_MOD, MetricId.MOD_PROP);
		testForIntInMethodName(methodName, FLUSH_PERIOD, IntervalAccumulator.FLUSH_PERIOD_PROP);		
		tracer = TracerManager3.getInstance().getTracer();
		ia = IntervalAccumulator.getInstance();
		DeltaManager.getInstance().reset();
		LOG.debug("\n\t******\n\t Setup [" + getClass().getSimpleName() + "." + methodName + "] Complete \n\t******");
	}
	


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		String methodName = testName.getMethodName();
		Thread.currentThread().setName(currentThreadName);
		LOG.debug("\n\t******\n\t Teardown [" + getClass().getSimpleName() + "." + methodName + "]\n\t ******");
		
		MetricId.reset();
		TracerManager3.getInstance().shutdown();
		LOG.debug("\n\t******\n\t Teardown [" + getClass().getSimpleName() + "." + methodName + "] Complete\n\t ******");		
	}

	/**
	 * Pattern matches the upcoming test method name. If the pattern matches, the system property will be set to the extracted value
	 * @param methodName The method name coming up
	 * @param pattern The pattern to match against
	 * @param propertyName The property name to set if the pattern matching succeeds.
	 */
	public void testForIntInMethodName(String methodName, Pattern pattern, String propertyName) {
		if(methodName==null) return;
		Matcher m = pattern.matcher(methodName);
		if(m.find()) {
			try { 
				Integer.parseInt(m.group(1));
				System.setProperty(propertyName, m.group(1));
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Returns the FlushPeriod and OtMod determined from the test name
	 * @return a long array with the flush period in 0 and the mod in 1.
	 */
	public long[] getTestIASettings() {
		long[] res = new long[2];
		Matcher m = FLUSH_PERIOD.matcher(testName.getMethodName());
		if(m.find()) {
			try { 
				res[0] = Integer.parseInt(m.group(1));
			} catch (Exception e) {
				res[0] = -1;
			}
		} else {
			res[0] = IntervalAccumulator.DEFAULT_FLUSH_PERIOD;
		}
		m = OT_MOD.matcher(testName.getMethodName());
		if(m.find()) {
			try { 
				res[1] = Integer.parseInt(m.group(1));
			} catch (Exception e) {
				res[1] = -1;
			}
		} else {
			res[1] = MetricId.DEFAULT_MOD;
		}		
		return res;
	}
	
	

}
