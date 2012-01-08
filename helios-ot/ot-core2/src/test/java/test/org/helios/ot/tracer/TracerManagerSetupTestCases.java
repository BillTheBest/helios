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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.jmxenabled.service.ServiceState;
import org.helios.ot.endpoint.DefaultEndpoint;
import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import test.org.helios.ot.util.EventHandlerAppender;

/**
 * <p>Title: TracerManagerSetupTestCases</p>
 * <p>Description: Test case to validate the correct configuration, start, shutdown and restart of the TracerManager.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.tracer.TracerManagerSetupTestCases</code></p>
 */

public class TracerManagerSetupTestCases extends TracerManagerBaseTezt {
	/** The default event handler's logger */
	protected final Logger eventHandlerLogger = Logger.getLogger(DefaultEndpoint.class);
	/** The default event handler name */
	protected static final String DEF_APPENDER_NAME = "DefaultEventHandler";
	/** An event handler appender so we can capture handled trace collections */
	protected final EventHandlerAppender eventHandler = new EventHandlerAppender(DEF_APPENDER_NAME);

	/**
	 * Cleans up after each test case
	 * @throws java.lang.Exception thrown if cleanup fails.
	 */
	@After
	public void tearDown() throws Exception {
		try {
			tm.shutdown();
			Assert.assertEquals("The tm state", ServiceState.STOPPED, tm.getState());
		} catch (Exception e) {
			unrecoverable.set(true);
			throw e;
		}
	}


	/**
	 * Validate a simple tracer trace operation
	 * @throws Exception thrown on any runtime or assertion errors
	 */
	@SuppressWarnings({ "unchecked", "cast" })
	@Test(timeout=3000)
	public void testSingleEventTracer() throws Exception {
		startTm();
		eventHandlerLogger.addAppender(eventHandler);
		eventHandlerLogger.setLevel(Level.DEBUG);
		try {
			Date dt = new Date();
			Trace trace = tm.getTracer().trace(dt, "foo", "bar");			
			Object obj = eventHandler.take();			
			Assert.assertEquals("The polled object type", HashMap.class, obj.getClass());
			Map<String, Object> map = (Map<String, Object>)obj; 
			Collection<Trace> traces = (Collection<Trace>)map.get("Traces");
			Object[] tags = (Object[])map.get("Context");
			Assert.assertEquals("The number of traces", 1, traces.size());
			Trace newTrace = (Trace) traces.iterator().next();			
			Assert.assertEquals("The retrieved trace", true, newTrace==trace);
			Assert.assertEquals("The retrieved trace value", dt, new Date((Long)newTrace.getValue()));
		} finally {
			eventHandlerLogger.removeAppender(DEF_APPENDER_NAME);
		}
	}
	
	
	/**
	 * Validate a Validate a simple tracer trace operation before and after a restart
	 * @throws Exception thrown on any runtime or assertion errors
	 */
	@SuppressWarnings("unchecked")
	@Test(timeout=3000)
	public void testSingleEventTracerWithRestart() throws Exception {
		startTm();
		eventHandlerLogger.addAppender(eventHandler);
		eventHandlerLogger.setLevel(Level.DEBUG);
		try {
			Trace trace = tm.getTracer().trace(new Date(), "foo", "bar");
			Map<String, Object> context = (Map<String, Object>)eventHandler.take();
			Collection<Trace> traces = (Collection<Trace>)context.get("Traces");
			Assert.assertEquals("The number of traces", 1, traces.size());
			Trace newTrace = traces.iterator().next();
			Assert.assertEquals("The retrieved trace", true, newTrace==trace);			
			tm.shutdown();
			Assert.assertEquals("The tm state", ServiceState.STOPPED, tm.getState());
			startTm();
			Assert.assertEquals("The tm state", ServiceState.STARTED, tm.getState());
			trace = tm.getTracer().trace(new Date(), "foo", "bar");
			context = (Map<String, Object>)eventHandler.take();
			traces = (Collection<Trace>)context.get("Traces");
			newTrace = traces.iterator().next();
			Assert.assertEquals("The retrieved trace", true, newTrace==trace);
//			obj = eventHandler.take();			
//			Assert.assertEquals("The polled object type", true, obj.getClass().equals(TraceCollection.class));
//			tc = (TraceCollection)obj;
//			newTrace = (Trace) tc.getTraces().iterator().next();			
//			Assert.assertEquals("The retrieved trace", true, newTrace==trace);			
			
		} finally {
			eventHandlerLogger.removeAppender(DEF_APPENDER_NAME);
		}
	}
	

	
	
	
	/**
	 * Validate a large number of disruptor commits using the disruptor directly without a trace produced by multiple threads.
	 * @throws Exception thrown on any runtime or assertion errors
	 */
	@SuppressWarnings("unchecked")
	//@Test(timeout=300000)
	public void testSeveralEventDirectsMultiThreaded() throws Exception {
		startTm();
		eventHandlerLogger.addAppender(eventHandler);
		eventHandlerLogger.setLevel(Level.DEBUG);
		final int eventCount = 10000;
		int threadCount = 10;
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch completeLatch = new CountDownLatch(threadCount);
		for(int i = 0; i < threadCount; i++) {
			Thread t = new Thread() {
				public void run() {
					try {
						startLatch.await();
						for(int i = 0; i < eventCount; i++) {
							TraceCollection tc  = tm.getNextTraceCollectionSlot();
							tm.commit(tc);	
							if(i > 0 && i%1000==0) {
								LOG.info(getName() + " Completed 1000, Total:" + i);
							}
						}						
					} catch (Exception e) {
						LOG.error("testSeveralEventDirectsMultiThreaded Error", e);
					} finally {
						completeLatch.countDown();
					}
				}
			};
			t.setDaemon(true);
			t.start();
		}
		startLatch.countDown();
		LOG.info("testSeveralEventDirectsMultiThreaded Started");
		try {
			for(int i = 0; i < (eventCount * threadCount); i++) {
				Object obj = eventHandler.take();
				Assert.assertEquals("The polled object type", true, obj.getClass().equals(TraceCollection.class));
			}
		} finally {
			eventHandlerLogger.removeAppender(DEF_APPENDER_NAME);
		}
	}
	
	
	/**
	 * Validate several simple tracer trace operations
	 * @throws Exception thrown on any runtime or assertion errors
	 */
	@Test (timeout=3000)
	public void testSeveralEventTracers() throws Exception {
		if(testName.getMethodName().equals("testSeveralEventTracers")) {
			startTm();
		}
		eventHandlerLogger.addAppender(eventHandler);
		eventHandlerLogger.setLevel(Level.DEBUG);
		int eventCount = 100;
		try {
			for(int i = 0; i < eventCount; i++) {
				tm.getTracer().trace(new Date(), "foo", "bar");
			}
			int retrievedCount = 0;
			while(retrievedCount < eventCount) {
				Object obj = eventHandler.take();
				Assert.assertEquals("The polled object type", HashMap.class, obj.getClass());
				retrievedCount++;
			}
		} finally {
			eventHandlerLogger.removeAppender(DEF_APPENDER_NAME);
		}
	}
	
	
	/**
	 * Validate several simple tracer trace operations before and after a restart
	 * @throws Exception thrown on any runtime or assertion errors
	 */
	@Test(timeout=3000)
	public void testSeveralEventTracersWithRestart() throws Exception {
		startTm();
		testSeveralEventTracers();
		tm.shutdown();
		Assert.assertEquals("The tm state", ServiceState.STOPPED, tm.getState());
		startTm();
		Assert.assertEquals("The tm state", ServiceState.STARTED, tm.getState());
		testSeveralEventTracers();
	}
	
	private List<String> getAppenderNames(Enumeration<Appender> enumer) {
		List<String> set = new ArrayList<String>();
		while(enumer.hasMoreElements()) {
			Appender appender = enumer.nextElement(); 
			set.add(appender.getName() + "/" + appender.getClass().getName());
		}
		return set;
	}
	
//	/**
//	 * Validate simple tracer trace operations against an event handler pipeline configured disruptor.
//	 * @throws Exception thrown on any runtime or assertion errors
//	 */
//	@Test (timeout=3000)
//	public void testEventTracersWithPipeline() throws Exception {
//		String markup = "A->[B->[C->]]";
//		testConfiguration.set(Configuration.getDefaultConfiguration()
//				.configurationMarkup(markup)
//				.eventHandler("A", new ParameterizedNameEventHandler("A"))
//				.eventHandler("B", new ParameterizedNameEventHandler("B"))
//				.eventHandler("C", new ParameterizedNameEventHandler("C"))
//		);
//		Logger.getLogger(ParameterizedNameEventHandler.class).addAppender(eventHandler);
//		Logger.getLogger(ParameterizedNameEventHandler.class).setLevel(Level.DEBUG);
//		try {
//			if(testName.getMethodName().equals("testEventTracersWithPipeline")) {
//				startTm();
//			}			
//			LOG.info("PNEH Appenders:" + getAppenderNames(Logger.getLogger(ParameterizedNameEventHandler.class).getAllAppenders()));
//			//Trace trace = tm.getTracer().trace(new Date(), "foo", "bar");
//			TraceCollection tc  = tm.getNextTraceCollectionSlot();
//			tm.commit(tc);							
//			
//			int retrievedCount = 0;
//			Object[] tcContext = null;  
//			while(retrievedCount < 3) {
//				Object obj = eventHandler.take();
//				tcContext = tc.getContextTags();
//				Assert.assertEquals("The polled object type", Object[].class, obj.getClass());
//				retrievedCount++;
//			}
//			Assert.assertNotNull("Last TC Context", tcContext);
//			Assert.assertArrayEquals("TC Context entries", new Object[]{"A", "B", "C"}, tcContext);
//			LOG.info("Final TC Context:" + Arrays.toString(tcContext));			
//		} finally {
//			Logger.getLogger(ParameterizedNameEventHandler.class).removeAppender(DEF_APPENDER_NAME);
//		}
//		
//		
//		
//	}
	
	

}
