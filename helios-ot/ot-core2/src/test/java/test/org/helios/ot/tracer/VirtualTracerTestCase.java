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


import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.ot.subtracer.RecursiveSubTracerException;
import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>Title: VirtualTracerTestCase</p>
 * <p>Description: Test case to validate that traces generated from a virtual tracer reflect the overriden host and agent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.tracer.VirtualTracerTestCase</code></p>
 */

public class VirtualTracerTestCase {
	static final String VHOST = "org.heliosdev.hdog";
	static final String VAGENT = "jbossAgent";
	static final Logger LOG = Logger.getLogger(VirtualTracerTestCase.class);
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
		//Thread.currentThread().join();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testVirtualHostAndAgent() {
		ITracer tracer = TracerManager3.getInstance().getVirtualTracer(VHOST, VAGENT);		
		Trace trace  = tracer.trace(1, "Point", "NS");
		Assert.assertEquals("The trace host", VHOST, trace.getHostName());
		Assert.assertEquals("The trace agent", VAGENT, trace.getAgentName());
		Assert.assertEquals("The trace urgency", false, trace.isUrgent());
		Assert.assertEquals("The trace temporal flag", false, trace.isTemporal());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUrgentVirtualHostAndAgent() {
		ITracer tracer = TracerManager3.getInstance().getUrgentTracer().getVirtualTracer(VHOST, VAGENT);
		LOG.info("Tracer is [" + tracer.getDefaultName() + "]");
		Trace trace  = tracer.trace(1, "Point", "NS");
		Assert.assertEquals("The trace host", VHOST, trace.getHostName());
		Assert.assertEquals("The trace agent", VAGENT, trace.getAgentName());
		Assert.assertEquals("The trace urgency", true, trace.isUrgent());
		Assert.assertEquals("The trace temporal flag", false, trace.isTemporal());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testTemporalVirtualHostAndAgent() {
		ITracer tracer = TracerManager3.getInstance().getTemporalTracer().getVirtualTracer(VHOST, VAGENT);
		LOG.info("Tracer is [" + tracer.getDefaultName() + "]");
		Trace trace  = tracer.trace(1, "Point", "NS");
		Assert.assertEquals("The trace host", VHOST, trace.getHostName());
		Assert.assertEquals("The trace agent", VAGENT, trace.getAgentName());
		Assert.assertEquals("The trace urgency", false, trace.isUrgent());
		Assert.assertEquals("The trace temporal flag", true, trace.isTemporal());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testTemporalUrgentVirtualHostAndAgent() {
		ITracer tracer = TracerManager3.getInstance().getTemporalTracer().getUrgentTracer().getVirtualTracer(VHOST, VAGENT);
		LOG.info("Tracer is [" + tracer.getDefaultName() + "]");
		Trace trace  = tracer.trace(1, "Point", "NS");
		Assert.assertEquals("The trace host", VHOST, trace.getHostName());
		Assert.assertEquals("The trace agent", VAGENT, trace.getAgentName());
		Assert.assertEquals("The trace urgency", true, trace.isUrgent());
		Assert.assertEquals("The trace temporal flag", true, trace.isTemporal());
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected=RecursiveSubTracerException.class)
	public void testRecursiveSubTracer() {
		ITracer tracer = TracerManager3.getInstance().getVirtualTracer(VHOST, VAGENT).getUrgentTracer().getVirtualTracer("FOO", "BAR");
		LOG.info("Tracer is [" + tracer.getDefaultName() + "]");
	}


	

}
