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
package test.org.helios.tracing.opentrace.accumulator;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.tracing.nulltracer.NullTracerInstanceFactory;
import org.helios.tracing.subtracer.IIntervalTracer;


/**
 * <p>Title: AccumulatorRunner</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.tracing.opentrace.accumulator.AccumulatorRunner</code></p>
 */

public class AccumulatorRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger log = Logger.getLogger(AccumulatorRunner.class);
		log.info("AccumulatorRunner");
//		AccumulatorManager accMgr = new AccumulatorManager(); 
//		accMgr.start();
		final IIntervalTracer tracer = new NullTracerInstanceFactory().getIntervalTracer();
		for(int i = 0; i < 20; i++) {
			new Thread() {
				public void run() {
					Random random = new Random(System.nanoTime());
					ThreadMXBean txBean = ManagementFactory.getThreadMXBean();
					txBean.setThreadCpuTimeEnabled(true);
					txBean.setThreadContentionMonitoringEnabled(true);
					while(true) {
						tracer.startThreadInfoCapture();
						tracer.trace(random.nextInt(100), "Foo (%}", "Int Metrics");
						tracer.trace(random.nextLong(), "Bar", "Long Metrics");
						tracer.trace(Runtime.getRuntime().freeMemory(), "Memory", "Free");
						tracer.trace(Runtime.getRuntime().maxMemory(), "Memory", "Max");
						tracer.trace(Runtime.getRuntime().totalMemory(), "Memory", "Total");
						tracer.traceDelta(txBean.getCurrentThreadCpuTime(), "Thread" , "CPU Time");
						tracer.traceDelta(txBean.getCurrentThreadUserTime(), "Thread" , "User Time");
						//tracer.endThreadInfoCapture("Thread", "Stats");
						try {
							Thread.currentThread().join(50);
						} catch (Exception e) {
							e.printStackTrace();
						}
						tracer.endThreadInfoCapture("Main", "TraceLoop", "ThreadStats");
					}					
				}
			}.start();
		}
	}

}
