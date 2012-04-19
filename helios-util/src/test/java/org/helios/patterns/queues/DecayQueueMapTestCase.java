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
package org.helios.patterns.queues;

import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * <p>Title: DecayQueueMapTestCase</p>
 * <p>Description: Test cases for [@link DecayQueueMap}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.patterns.queues.DecayQueueMapTestCase</code></p>
 */

public class DecayQueueMapTestCase {
	/** The test map instance */
	protected DecayQueueMap<Long, String>  decayMap = null;
	
	/** Tracks the test name */
	@Rule
    public TestName testName= new TestName();
	
	/**
	 * Shuts down the decayMap instance after each test
	 */
	@After
	public void shutdownDecayMap() {
		if(decayMap!=null) {
			decayMap.shutdown();
			decayMap = null;
		}
	}
	
	@Test
	public void testItemRetrieved() {
		decayMap = new DecayQueueMap<Long, String>(1000);
		Long key = System.nanoTime();
		String value = testName.getMethodName();
		decayMap.put(key, value);
		String value2 = decayMap.get(key);
		Assert.assertNotNull("Value returned from map was null", value2);
		Assert.assertEquals("Value returned from map was not the expected value", value, value2);
		Assert.assertEquals("DecayMap size was not 1", 1, decayMap.size());
		Assert.assertEquals("DecayMap timeout count was not 0", 0, decayMap.getTimeOutCount());
		
	}
	
	@Test
	public void testItemRetrievedThenTimedout() throws Exception {
		decayMap = new DecayQueueMap<Long, String>(1000);
		Long key = System.nanoTime();
		String value = testName.getMethodName();
		decayMap.put(key, value);
		String value2 = decayMap.get(key);
		Assert.assertNotNull("Value returned from map was null", value2);
		Assert.assertEquals("Value returned from map was not the expected value", value, value2);
		Thread.currentThread().join(1010);
		value2 = decayMap.get(key);
		Assert.assertNull("Value returned from map was not null", value2);
		Assert.assertEquals("DecayMap size was not 0", 0, decayMap.size());
		Assert.assertEquals("DecayMap timeout count was not 1", 1, decayMap.getTimeOutCount());
		
	}
	
	@Test
	public void testEntrySetRetrieved() throws Exception {
		decayMap = new DecayQueueMap<Long, String>(1000);
		Long key = System.nanoTime();
		String value = testName.getMethodName();
		decayMap.put(key, value);
		Set<Entry<Long, String>> entrySet = decayMap.entrySet();
		Assert.assertNotNull("EntrySet returned from map was null", entrySet);
		Assert.assertEquals("EntrySet size was not 1", 1, entrySet.size());
		Entry<Long, String> entry = entrySet.iterator().next();
		Assert.assertEquals("Entry Key was not the expected key", key, entry.getKey());
		Assert.assertEquals("Entry Value was not the expected value", value, entry.getValue());
	}

	@Test(expected=UnsupportedOperationException.class)
	public void testEntrySetIsReadOnly() throws Exception {
		decayMap = new DecayQueueMap<Long, String>(1000);
		Long key = System.nanoTime();
		String value = testName.getMethodName();
		decayMap.put(key, value);
		Set<Entry<Long, String>> entrySet = decayMap.entrySet();
		Assert.assertNotNull("EntrySet returned from map was null", entrySet);
		Assert.assertEquals("EntrySet size was not 1", 1, entrySet.size());
		Entry<Long, String> entry = entrySet.iterator().next();
		Assert.assertEquals("Entry Key was not the expected key", key, entry.getKey());
		Assert.assertEquals("Entry Value was not the expected value", value, entry.getValue());
		entry.setValue("Foo");
	}
	
	
	@Test
	public void testItemRetrievedThenRefreshed() throws Exception {
		decayMap = new DecayQueueMap<Long, String>(1000);
		Long key = System.nanoTime();
		String value = testName.getMethodName();
		decayMap.put(key, value);		
		for(int i = 0; i < 10; i++) {
			String value2 = decayMap.get(key);
			Assert.assertNotNull("Value returned from map was null on loop " + i, value2);
			Assert.assertEquals("Value returned from map was not the expected value on loop " + i, value, value2);
			Thread.currentThread().join(500);			
		}
		Thread.currentThread().join(1010);
		String value2 = decayMap.get(key);
		Assert.assertNull("Value returned from map was not null", value2);
		Assert.assertEquals("DecayMap size was not 0", 0, decayMap.size());
		Assert.assertEquals("DecayMap timeout count was not 1", 1, decayMap.getTimeOutCount());
	}
	
	@Test
	public void testItemRetrievedThenRemoved() throws Exception {
		decayMap = new DecayQueueMap<Long, String>(2000);
		Long key = System.nanoTime();
		String value = testName.getMethodName();
		decayMap.put(key, value);		
		String value2 = decayMap.get(key);
		Assert.assertNotNull("Value returned from map was null", value2);
		Assert.assertEquals("Value returned from map was not the expected value", value, value2);
		String value3 = decayMap.remove(key);
		Assert.assertNotNull("Value removed from map was null", value3);
		Assert.assertEquals("Value removed from map was not the expected value", value, value3);
		Assert.assertEquals("DecayMap size was not 0", 0, decayMap.size());
		Assert.assertEquals("DecayMap timeout count was not 0", 0, decayMap.getTimeOutCount());		
	}
	
	@Test
	public void testRemoveListener() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicLong tKey = new AtomicLong(0);
		final AtomicReference<String> tValue = new AtomicReference<String>();
		decayMap = new DecayQueueMap<Long, String>(2000);
		Long key = System.nanoTime();
		String value = testName.getMethodName();
		decayMap.put(key, value);		
		String value2 = decayMap.get(key);
		Assert.assertNotNull("Value returned from map was null", value2);
		Assert.assertEquals("Value returned from map was not the expected value", value, value2);
		decayMap.addListener(new TimeoutListener<Long, String>(){
			public void onTimeout(Long key, String value) {
				tKey.set(key);
				tValue.set(value);
				latch.countDown();
			}
		});
		Assert.assertTrue("Latch did not drop within the timeout period", latch.await(2020, TimeUnit.MILLISECONDS));
		Assert.assertEquals("Listener provided key was not the expected key", key, new Long(tKey.get()));
		Assert.assertEquals("Listener provided value was not the expected value", value, tValue.get());
		Assert.assertEquals("DecayMap size was not 0", 0, decayMap.size());
		Assert.assertEquals("DecayMap timeout count was not 1", 1, decayMap.getTimeOutCount());		
		
	}
	// Test variable timeouts on inserted values
	

}
