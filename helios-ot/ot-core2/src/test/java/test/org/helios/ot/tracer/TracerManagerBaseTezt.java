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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.jmxenabled.service.ServiceState;
import org.helios.jmxenabled.service.ServiceState.ServiceStateController;
import org.helios.ot.tracer.TracerManager3;
import org.helios.ot.tracer.TracerManager3.Configuration;
import org.helios.reflection.PrivateAccessor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * <p>Title: TracerManagerBaseTezt</p>
 * <p>Description: Base test class for TracerManager tests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.tracer.TracerManagerBaseTezt</code></p>
 */

public class TracerManagerBaseTezt {
	/** The TracerManager configuration under test */
	protected final AtomicReference<Configuration> testConfiguration = new AtomicReference<Configuration>(null);
	/** The TracerManager markup configuration to use */
	protected final AtomicReference<String> testMarkup = new AtomicReference<String>(null); 
	
	
	/** The one and only TM instance */
	protected static TracerManager3 tm = null;
	/** The tm instance state controller */
	protected static ServiceStateController state = null;
	
	/** Indicates if a prior test left the TM in an untestable state */
	protected static final AtomicBoolean unrecoverable = new AtomicBoolean(false);
	
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(TracerManager3.class);
	
	/** Tracks the test name */
	@Rule
    public TestName testName = new TestName();
	
	
	/**
	 * Bootstraps the test class
	 * @throws java.lang.Exception thrown if any of these sketchy reflective ops fail.
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		tm = (TracerManager3)PrivateAccessor.getStaticFieldValue(TracerManager3.class, "instance");		
		state = (ServiceStateController)PrivateAccessor.getFieldValue(tm, "state");
	}

	/**
	 * Cleans up after this test class is done with
	 * @throws java.lang.Exception thrown on any error doing nothing
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}


	
	
	/**
	 * Starts the TM
	 */
	protected void startTm() {
		Assert.assertFalse("TM is in an unrecoverable state", unrecoverable.get());
		// Validate that the TM is currently shut down
		Assert.assertEquals("TM state", ServiceState.STOPPED, state.getState());
		// Start whatever configuration is in state		
		Configuration conf = testConfiguration.get();
		if(conf!=null) {
			TracerManager3.getInstance(conf);
		} else {
			TracerManager3.getInstance();
		}
		Assert.assertEquals("TM state", ServiceState.STARTED, state.getState());
	}
	
	/**
	 * Tests.... nothing.
	 * Actually a simple first test to validate start/stop
	 */
	@Test
	public void testNothing() {
		LOG.info("Testing nothing...");
	}

}
