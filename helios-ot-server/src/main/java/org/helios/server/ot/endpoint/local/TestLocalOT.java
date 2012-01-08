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
package org.helios.server.ot.endpoint.local;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.helios.jmxenabled.threads.HeliosThreadGroup;
import org.helios.ot.subtracer.IntervalTracer;
import org.helios.ot.trace.interval.IntervalAccumulator;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;

/**
 * <p>Title: TestLocalOT</p>
 * <p>Description: A test local ot implementor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.endpoint.local.TestLocalOT</code></p>
 */

public class TestLocalOT implements Runnable {
	/** A reference to the OT TracerManager */
	protected TracerManager3 tm;
	/** Standard tracer */
	protected ITracer tracer;
	/** Interval tracer */
	protected IntervalTracer intervalTracer;
	/** Monitor thread */
	protected Thread monitorThread;
	/** Instance logger */
	private final Logger log = Logger.getLogger(getClass());
	/** Run flag */
	private final AtomicBoolean keepRunning = new AtomicBoolean(true);
	/** The local endpoint instance to append to the tracer manager */
	protected final LocalEndpoint localEndpoint;
	

	static {
		System.setProperty(IntervalAccumulator.FLUSH_PERIOD_PROP, "15000");
	}
	
	/**
	 * Creates a new TestLocalOT
	 * @param localEndpoint The local endpoint instance to append to the tracer manager
	 */
	public TestLocalOT(LocalEndpoint localEndpoint) {
		super();
		this.localEndpoint = localEndpoint;
	}
	
	
	public void start() throws Exception {
		tm = TracerManager3.getInstance(TracerManager3.Configuration.getDefaultConfiguration()
				.appendEndPoint(localEndpoint)
			);
		tracer = tm.getTracer();
		intervalTracer = tm.getIntervalTracer();
		monitorThread = new Thread(HeliosThreadGroup.getInstance(getClass().getSimpleName() + "ThreadGroup"), this, getClass().getSimpleName() + "Thread");
		monitorThread.setDaemon(true);
		monitorThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				log.error("Uncaught exception in [" + t + "]", e);
			}
		});
		monitorThread.start();		
	}
	
	public void stop() {
		log.info("Stopping Monitor Thread....");
		keepRunning.set(false);
		monitorThread.interrupt();
	}
	
	private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
	
	public void run() {
		long threadId = Thread.currentThread().getId();
		
		while(keepRunning.get()) {
			try {
				ThreadInfo ti = threadMXBean.getThreadInfo(threadId);
				intervalTracer.traceDelta(ti.getBlockedTime(), "BlockTime", "Threading", Thread.currentThread().getName(), "Blocking");
				intervalTracer.traceDelta(ti.getWaitedTime(), "WaitTime", "Threading", Thread.currentThread().getName(), "Waiting");
				intervalTracer.trace(threadMXBean.getDaemonThreadCount(), "Daemon Thread Count", "Threading");
				intervalTracer.trace(threadMXBean.getThreadCount(), "Total Thread Count", "Threading");
				intervalTracer.trace(threadMXBean.getThreadCount() - threadMXBean.getDaemonThreadCount(), "Non Daemon Thread Count", "Threading");
				
				MemoryUsage mem = memoryMXBean.getHeapMemoryUsage();
				intervalTracer.trace(mem.getCommitted(), "Committed", "MemoryUsage", "Heap");
				intervalTracer.trace(mem.getInit(), "Initial", "MemoryUsage", "Heap");
				intervalTracer.trace(mem.getMax(), "Max", "MemoryUsage", "Heap");
				intervalTracer.trace(mem.getUsed(), "Used", "MemoryUsage", "Heap");
				
				mem = memoryMXBean.getNonHeapMemoryUsage();
				intervalTracer.trace(mem.getCommitted(), "Committed", "MemoryUsage", "NonHeap");
				intervalTracer.trace(mem.getInit(), "Initial", "MemoryUsage", "NonHeap");
				intervalTracer.trace(mem.getMax(), "Max", "MemoryUsage", "NonHeap");
				intervalTracer.trace(mem.getUsed(), "Used", "MemoryUsage", "NonHeap");
				
				for(GarbageCollectorMXBean gcBean: gcBeans) {
					intervalTracer.traceDelta(gcBean.getCollectionCount(), "Collection Rate", "GC", gcBean.getName());
					intervalTracer.traceDelta(gcBean.getCollectionTime(), "Collection Elapsed", "GC", gcBean.getName());
				}
				
				
				Thread.currentThread().join(1000);
			} catch (InterruptedException ie) {				
			} catch (Exception e) {
				log.warn("Monitor Thread Exception", e);
			}
		}
		log.info("Monitor Thread Stopped");
	}


}
