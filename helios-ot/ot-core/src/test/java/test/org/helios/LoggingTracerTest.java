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
package test.org.helios;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.tracing.ITracer;
import org.helios.tracing.extended.logging.Log4JTracerInstanceFactory;
import org.junit.Ignore;

/**
 * <p>Title: LoggingTracerTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.LoggingTracerTest</code></p>
 */
@Ignore
public class LoggingTracerTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger LOG = Logger.getLogger(LoggingTracerTest.class);
		LOG.info("LoggingTracerTest");
		Log4JTracerInstanceFactory factory = null;
		try {
			factory = Log4JTracerInstanceFactory.getInstance("-BAD Option", "DEBUG");
		} catch (Exception e) {}
		
		factory = Log4JTracerInstanceFactory.getInstance("-level", "DEBUG");
		//factory.setLevel("DEBUG");
		ITracer tracer = factory.getTracer();
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		
		String level = "DEBUG";
		while(true) {
			MemoryUsage heap = mem.getHeapMemoryUsage();
			MemoryUsage nonheap = mem.getHeapMemoryUsage();
			tracer.trace(heap.getCommitted(), "Committed", "Heap");
			tracer.trace(heap.getUsed(), "Used", "Heap");
			tracer.trace(nonheap.getCommitted(), "Committed", "NonHeap");
			tracer.trace(nonheap.getUsed(), "Used", "NonHeap");			
			try { Thread.sleep(2000); } catch (Exception e) {}
			if("DEBUG".equals(level)) {
				level = "INFO";				
			}  else {
				level = "DEBUG";
			}
			tracer = Log4JTracerInstanceFactory.getForcedInstance("-level", level).getTracer();
			
		}

	}

}
