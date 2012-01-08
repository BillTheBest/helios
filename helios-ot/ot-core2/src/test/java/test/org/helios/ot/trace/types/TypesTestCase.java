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
package test.org.helios.ot.trace.types;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.helios.enums.Primitive;
import org.helios.helpers.ExternalizationHelper;
import org.helios.ot.trace.types.AbstractNumericTraceValue;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.trace.types.interval.IIntervalTraceValue;
import org.helios.ot.trace.types.interval.IMinMaxAvgIntervalTraceValue;
import org.helios.ot.type.MetricType;
import org.junit.Assert;
import org.junit.Test;



/**
 * <p>Title: TypesTestCase</p>
 * <p>Description: Test case for TraceValue classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.trace.types.TypesTestCase</code></p>
 */

public class TypesTestCase {
	public static final Random random = new Random(System.nanoTime());
	public static void log(Object obj) { System.out.println(obj); }
	
	/**
	 * Executes basic assertions on contrived instances of ITraceValues for each MetricType
	 */
	@Test
	public void testAllMetricTypeValues() {
		for(MetricType metricType: MetricType.values()) {
			String testValue = "" + random.nextInt();
			ITraceValue itValue = metricType.traceValue(testValue);
			Assert.assertEquals("[" + metricType + "] The type of itValue.TraceValueType was not the expected", metricType.getValueType(), itValue.getTraceValueType());
			Class<?> expectedValueClass = Primitive.up(
					metricType.getValueType().getBaseType()
			);
			Assert.assertEquals("[" + metricType + "] The type of itValue.getValue() was not the expected", expectedValueClass, Primitive.up(
					itValue.getValue().getClass())
			);
			if(MetricType.BYTES.equals(metricType)) {					
				Assert.assertArrayEquals("[" + metricType + "] The value of itValue.getValue() was not the expected", testValue.getBytes(), (byte[])itValue.getValue());
			} else {
				Assert.assertEquals("[" + metricType + "] The value of itValue.getValue() was not the expected", testValue, itValue.getValue().toString());
			}
			if(metricType.isNumber()) {
				Number n = random.nextInt();
				AbstractNumericTraceValue numericValue = (AbstractNumericTraceValue)metricType.traceValue(n);
				Assert.assertEquals("[" + metricType + "] The numeric value of itValue.getValue() was not the expected", n, (int)numericValue.getNativeValue());
			}
		}
	}
	
	/**
	 * Executes basic assertions on contrived instances of IIntervalTraceValues for each MetricType
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testAllMetricTypeIntervalValues() {
		Set<String> testedTypes = new HashSet<String>();
		Set<String> notTestedTypes = new HashSet<String>();
		int sampleCount = 10;
		final String MSG_PREFIX = "Test Message-";
		TIntArrayListWAvg testInts = new TIntArrayListWAvg(sampleCount);
		List<String> testValues = new ArrayList<String>(sampleCount);
		for(int i = 0; i< sampleCount; i++) {
			int val = Math.abs(random.nextInt());
			testValues.add(MSG_PREFIX + val);
			if(i==(sampleCount-1)) log("Last Value [" + MSG_PREFIX + val + "]");
			testInts.add(val);
		}
		
		for(MetricType metricType: MetricType.values()) {
			notTestedTypes.add(metricType.name());
			IIntervalTraceValue t = metricType.intervalTraceValue();
			Assert.assertEquals("[" + metricType + "] The type of itValue.TraceValueType was not the expected", metricType.getIntervalTraceValueType(), t.getTraceValueType());
			
			if(metricType.isNumber() && !metricType.isTimestamp()) {
				for(int val: testInts.toArray()) {
					t.apply(metricType.traceValue(val));
				}
				Assert.assertEquals("[" + metricType + "] Invalid count on interval metric", sampleCount, t.getCount());
				//log("Count Test Passed for [" + metricType + "]");
				Assert.assertEquals("[" + metricType + "] The return type of itValue.getValue was not the expected", metricType.getIntervalTraceValueType().getBaseType(), Primitive.primitive(t.getValue().getClass()));
				//log("Count Test Passed for [" + metricType + "] on return type [" + metricType.getIntervalTraceValueType().getBaseType().getName() + "] / [" + t.getValue().getClass().getName() + "]");
				
				if(metricType.isMinMaxAvg()) {
					testedTypes.add(metricType.name());
					notTestedTypes.remove(metricType.name());					
					Assert.assertEquals("[" + metricType + "] min interval value", testInts.min() , ((IMinMaxAvgIntervalTraceValue)t).getMinimum().intValue());
					Assert.assertEquals("[" + metricType + "] max interval value", testInts.max(), ((IMinMaxAvgIntervalTraceValue)t).getMaximum().intValue());
					Assert.assertEquals("[" + metricType + "] avg interval value", testInts.avg(), ((IMinMaxAvgIntervalTraceValue)t).getAverage().intValue());
				} else if(metricType.isIncident()) {
					testedTypes.add(metricType.name());
					notTestedTypes.remove(metricType.name());										
					log("Incident Total:" + t.getValue());
					Assert.assertEquals("[" + metricType + "] total interval value", testInts.total(), t.getValue());
				}
			} else if(metricType.isString()) {
				for(int val: testInts.toArray()) {
					t.apply(metricType.traceValue(MSG_PREFIX + val));
				}				
				Assert.assertEquals("[" + metricType + "] Invalid count on interval metric", sampleCount, t.getCount());
				Assert.assertEquals("[" + metricType + "] The return type of itValue.getValue was not the expected", metricType.getIntervalTraceValueType().getBaseType(), t.getValue().getClass());
				if(metricType.equals(MetricType.STRING)) {
					testedTypes.add(metricType.name());
					notTestedTypes.remove(metricType.name());
					String lastMsg = testValues.get(sampleCount-1);
					Assert.assertEquals("[" + metricType + "] The return value of itValue.getValue was not the expected value", lastMsg, t.getValue());
				} else if(metricType.equals(MetricType.STRINGS)) {
					testedTypes.add(metricType.name());
					notTestedTypes.remove(metricType.name());
					String[] expected = testValues.toArray(new String[sampleCount]);
					Arrays.sort(expected);
					String[] actual = (String[])t.getValue();
					Arrays.sort(actual);
					Assert.assertArrayEquals("[" + metricType + "] The return value of itValue.getValue was not the expected value", expected, actual);					
				}
			} else if(metricType.equals(MetricType.TIMESTAMP)) {
				testedTypes.add(metricType.name());
				notTestedTypes.remove(metricType.name());
			} else if(metricType.equals(MetricType.BYTES)) {
				testedTypes.add(metricType.name());
				notTestedTypes.remove(metricType.name());
			} else {
//				notTestedTypes.add(metricType.name());
			}
		}
		Assert.assertEquals("Some MetricTypes were not tested " + notTestedTypes.toString(), 0, notTestedTypes.size());
		log("Completed Basic Interval TraceValue Tests for Numerics " + testedTypes.toString());
		log("Completed Basic Interval TraceValue Tests for Non Numerics " + notTestedTypes.toString());
	}
	

	/**
	 * Tests that contrived ITraceValue instances for each MetricType serialize and then deserialize with same values.  
	 */
	@Test
	public void testSerializationAllMetricTypeValues() {
		for(MetricType metricType: MetricType.values()) {
			String testValue = "" + random.nextInt();
			ITraceValue t = metricType.traceValue(testValue);
			//OptimizedObjectOutputStream.addClass(t.getClass());
			byte[] ser = ExternalizationHelper.serialize(t, false);
			log("Serialized [" + t.getClass().getName() + "] Size [" + ser.length + "]");
			ITraceValue t2 = ExternalizationHelper.deserialize(ser);
			log("Deserialized  [" + t.getClass().getName() + "] Successfully");
			if(MetricType.BYTES.equals(metricType)) {
				Assert.assertArrayEquals("[" + metricType + "] The deserialized value of itValue.getValue() was not the expected", (byte[])t.getValue(), (byte[])t2.getValue());
			} else {
				Assert.assertEquals("[" + metricType + "] The deserialized value of itValue.getValue() was not the expected", t.getValue(), t2.getValue());
			}
		}
	}
	
	public static class TIntArrayListWAvg extends TIntArrayList {
		public TIntArrayListWAvg(int capacity) {
			super(capacity);
		}
		
		public long total() {
			long total = 0;
			for(int i: this._data) {
				total += i;
			}
			return total;
		}

		public int avg() {
			double total = 0;
			double cnt = 0;
			for(int i: this._data) {
				total += i;
				cnt++;
			}
			return (int)(total/cnt);			
		}
		
	}
	
	/**
	 * Executes serialization assertions on contrived instances of IIntervalTraceValues for each MetricType
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSerializationAllMetricTypeIntervalValues() {
		Set<String> testedTypes = new HashSet<String>();
		Set<String> notTestedTypes = new HashSet<String>();
		int sampleCount = 10;
		final String MSG_PREFIX = "Test Message-";
		TIntArrayListWAvg testInts = new TIntArrayListWAvg(sampleCount);
		List<String> testValues = new ArrayList<String>(sampleCount);
		for(int i = 0; i< sampleCount; i++) {
			int val = Math.abs(random.nextInt());
			testValues.add(MSG_PREFIX + val);
			if(i==(sampleCount-1)) log("Last Value [" + MSG_PREFIX + val + "]");
			testInts.add(val);
		}
		
		for(MetricType metricType: MetricType.values()) {
			testedTypes.add(metricType.name());
			notTestedTypes.remove(metricType.name());
			IIntervalTraceValue t = metricType.intervalTraceValue();
			for(int i : testInts.toArray()) {				
				if(metricType.isNumber()) { 
					t.apply(metricType.traceValue(i));
				} else {
					t.apply(metricType.traceValue(MSG_PREFIX + i));
				}				
			}
			byte[] ser = ExternalizationHelper.serialize(t);
			IIntervalTraceValue t2 = ExternalizationHelper.deserialize(ser);
			if(t.getValue().getClass().isArray()) {
				
//				Object tValue = Array.newInstance(t.getValue().getClass(), sampleCount); 
//				Object t2Value = Array.newInstance(t2.getValue().getClass(), sampleCount);
//				System.arraycopy(t.getValue(), 0, tValue, 0, sampleCount);
//				System.arraycopy(t2.getValue(), 0, t2Value, 0, sampleCount);
//				Arrays.sort(tValue);
//				Arrays.sort(t2Value);
				//Assert.assertArrayEquals("[" + metricType + "] The deserialized value of itValue.getValue() was not the expected", sortedArray(t.getValue()), sortedArray(t2.getValue()));
				Assert.assertArrayEquals("[" + metricType + "] The deserialized value of itValue.getValue() was not the expected", (Object[])t.getValue(), (Object[])t2.getValue());
			} else {
				Assert.assertEquals("[" + metricType + "] The deserialized value of itValue.getValue() was not the expected for [" + t.getClass().getName() + "]", t.getValue(), t2.getValue());
			}
		}
		Assert.assertEquals("Some MetricTypes were not tested " + notTestedTypes.toString(), 0, notTestedTypes.size());
		log("Completed Basic Interval TraceValue Tests for Numerics " + testedTypes.toString());
		log("Completed Basic Interval TraceValue Tests for Non Numerics " + notTestedTypes.toString());
	}
	
	
	private Object[] sortedArray(Object obj) {
		if(obj==null) throw new IllegalArgumentException("The passed object was null", new Throwable());
		if(!obj.getClass().isArray()) throw new IllegalArgumentException("The type [" + obj.getClass().getName() + "] is not an array", new Throwable());
		Object[] arr = (Object[])obj;
		Arrays.sort(arr);
		return arr;
		
	}
	
//
//	
//
//	/**
//	 * Validates that an IntTraceValue ser/dsers correctly.
//	 */
//	@Test
//	public void testSerializeIntTraceValue() {
//		int v = random.nextInt();
//		ITraceValue t = new IntTraceValue(v);
//		byte[] ser = ExternalizationHelper.serialize(t);
//		log("Serialized IntTraceValue Size [" + ser.length + "]");
//		ITraceValue t2 = ExternalizationHelper.deserialize(ser);		
//		Assert.assertEquals("Trace value did not ser/deser", t.getValue(), t2.getValue());
//		int v2 = ((IntTraceValue)t2).getIntValue();
//		Assert.assertEquals("Trace value did not ser/deser", v, v2);
//		v2 = (int)((IntTraceValue)t).getIntValue();
//		Assert.assertEquals("Trace value did not return correct Dvalue", v, v2);		
//	}

}



///**
//* @throws java.lang.Exception
//*/
//@BeforeClass
//public static void setUpBeforeClass() throws Exception {
//}
//
///**
//* @throws java.lang.Exception
//*/
//@AfterClass
//public static void tearDownAfterClass() throws Exception {
//}
//
///**
//* @throws java.lang.Exception
//*/
//@Before
//public void setUp() throws Exception {
//}
//
///**
//* @throws java.lang.Exception
//*/
//@After
//public void tearDown() throws Exception {
//}
