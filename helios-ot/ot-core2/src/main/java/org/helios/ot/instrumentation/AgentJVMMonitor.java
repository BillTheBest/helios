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
package org.helios.ot.instrumentation;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.interval.IntervalAccumulator;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.helios.time.SystemClock;
import org.helios.time.SystemClock.ElapsedTime;

/**
 * <p>Title: AgentJVMMonitor</p>
 * <p>Description: Standard Java Agent built in monitor.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.instrumentation.AgentJVMMonitor</code></p>
 */

public class AgentJVMMonitor {
	/** Singleton instance */
	private static volatile AgentJVMMonitor instance = null;
	/** Singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Tracer instance */
	private ITracer tracer = null;
	/** Interval Tracer instance */
	private ITracer itracer = null;
	/** The ThreadMXBean */
	private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	/** The OperatingSystemMXBean */
	private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
	/** The ClassLoaderMXBean */
	private final ClassLoadingMXBean clMXBean = ManagementFactory.getClassLoadingMXBean();
	/** The CompilationMXBean */
	private final CompilationMXBean compMXBean = ManagementFactory.getCompilationMXBean();
	/** Flag indicates if compilation time is supported */
	private final boolean compileTimeSupported = compMXBean.isCompilationTimeMonitoringSupported();
	/** The RuntimeMXBean */
	private final RuntimeMXBean rtMXBean = ManagementFactory.getRuntimeMXBean();
	/** The MemoryMXBean */
	private final MemoryMXBean memMXBean = ManagementFactory.getMemoryMXBean();
	/** A set of heap memory pool mx beans */
	private final Set<MemoryPoolMXBean> heapMemPoolMXBeans;
	/** A set of non heap memory pool mx beans */
	private final Set<MemoryPoolMXBean> nonHeapMemPoolMXBeans;
	
	/** A set of gc mx beans */
	private final Set<GarbageCollectorMXBean> gcMXBeans = Collections.unmodifiableSet(new HashSet<GarbageCollectorMXBean>(ManagementFactory.getGarbageCollectorMXBeans()));
	/** A set of period executed runnables that perform the metrics collection */
	private final Set<Runnable> registeredCollectors = new CopyOnWriteArraySet<Runnable>();
	/** Simple timer for scheduling collections */
	private final Timer timer = new Timer(getClass().getSimpleName() + "-Thread", false);
	/** Timer task to execute the collection */
	private TimerTask timerTask = null;
	/** Instance logger */
	private final Logger log = Logger.getLogger(getClass());

	/** The root name space */
	public static final String ROOT = "JVM";
	/** The threading metrics name space  */
	public static final String THREAD_ROOT = ROOT + Trace.DELIM + "Threads";
	/** The classloading metrics name space  */
	public static final String CLASSLOAD_ROOT = ROOT + Trace.DELIM + "Classes";
	/** The JVM compiler metrics name space  */
	public static final String COMPILER_ROOT = ROOT + Trace.DELIM + "Compiler";
	/** The JVM memory metrics name space  */
	public static final String MEMORY_ROOT = ROOT + Trace.DELIM + "Memory";
	/** The JVM Heap memory metrics name space  */
	public static final String HEAP_MEMORY_ROOT = MEMORY_ROOT + Trace.DELIM + "Heap";
	/** The JVM Heap memory metrics name space  */
	public static final String NONHEAP_MEMORY_ROOT = MEMORY_ROOT + Trace.DELIM + "NonHeap";
	
	/** The JVM GC metrics name space  */
	public static final String GC_ROOT = ROOT + Trace.DELIM + "GC";
	
	
	/**
	 * Acquires the AgentJVMMonitor singleton instance
	 * @return the AgentJVMMonitor singleton instance
	 */
	public static AgentJVMMonitor getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new AgentJVMMonitor();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new AgentJVMMonitor, initializes the tracing resources and schedules a callback for JMV metrics collection.
	 */
	private AgentJVMMonitor() {
		Set<MemoryPoolMXBean> tmpHeapSet = new HashSet<MemoryPoolMXBean>();
		Set<MemoryPoolMXBean> tmpNonHeapSet = new HashSet<MemoryPoolMXBean>();
		for(MemoryPoolMXBean pool: ManagementFactory.getMemoryPoolMXBeans()) {
			if("HEAP".equals(pool.getType().name())) tmpHeapSet.add(pool);
			else tmpNonHeapSet.add(pool);
		}
		heapMemPoolMXBeans = Collections.unmodifiableSet(tmpHeapSet);
		nonHeapMemPoolMXBeans = Collections.unmodifiableSet(tmpNonHeapSet);
		
		tracer = TracerManager3.getInstance().getTracer();
		itracer = TracerManager3.getInstance().getIntervalTracer();
		timerTask =  new TimerTask(){
			public void run() {
				collect();	
				collectClassLoading();
			}
		};
		long flushPeriod = ConfigurationHelper.getLongSystemThenEnvProperty(IntervalAccumulator.FLUSH_PERIOD_PROP, IntervalAccumulator.DEFAULT_FLUSH_PERIOD);
		timer.schedule(timerTask, flushPeriod, flushPeriod);
		log.info("Started AgentJVMMonitor");
	}
	
	
	/**
	 * Executes the JVM metric collection and tracing. 
	 */
	private void collect() {
		SystemClock.startTimer();
		try {
			collectThreads();
			collectClassLoading();
			if(compileTimeSupported) collectCompilation();
			collectMemory();
			collectMemoryPools();
			for(Runnable runnable: registeredCollectors) {
				runnable.run();
			}
			long elapsed = SystemClock.lapTimer().elapsedMs;
			itracer.trace(elapsed, "JVM Metrics Collect Time", ROOT);
		} catch (Exception e) {
			log.error("AgentJVMMonitor: Unexpected collection exception", e);
		} finally {
			ElapsedTime et = SystemClock.endTimer();
			if(log.isDebugEnabled()) log.debug("AgentJVMMonitor Collection Complete [" + et + "]");
		}
	}

	/**
	 * Registers a new collector runnable to be executed each period.
	 * @param runnable a collector runnable
	 */
	public void addCollector(Runnable runnable) {
		if(runnable==null) throw new IllegalArgumentException("The passed collector runnable was null", new Throwable());
		registeredCollectors.add(runnable);
	}
	
	
	/**
	 * Collects thread stats
	 */
	protected void collectThreads() {
		int threadCount = threadMXBean.getThreadCount();
		int daemonThreadCount = threadMXBean.getDaemonThreadCount();
		int nonDaemonThreadCount = threadCount-daemonThreadCount;
		long[] deadLocked = null;
		deadLocked = threadMXBean.findDeadlockedThreads();
		int deadlockedThreads  = deadLocked==null ? 0 : deadLocked.length;
		deadLocked = threadMXBean.findMonitorDeadlockedThreads();
		int monitorDeadlockedThreads = deadLocked==null ? 0 : deadLocked.length;
		int peakThreadCount = threadMXBean.getPeakThreadCount();
		threadMXBean.resetPeakThreadCount();
		itracer.trace(threadCount, "Thread Count", THREAD_ROOT);
		itracer.trace(daemonThreadCount, "Daemon Thread Count", THREAD_ROOT);
		itracer.trace(nonDaemonThreadCount, "NonDaemon Thread Count", THREAD_ROOT);
		itracer.trace(peakThreadCount, "Peak Thread Count", THREAD_ROOT);
		itracer.trace(deadlockedThreads, "Deadlocked Thread Count", THREAD_ROOT);
		itracer.trace(monitorDeadlockedThreads, "Monitor Deadlocked Thread Count", THREAD_ROOT);
	}
	
	/**
	 * Collects class loader stats
	 */
	protected void collectClassLoading() {
		int loadedClasses = clMXBean.getLoadedClassCount();
		long unLoadedClasses = clMXBean.getUnloadedClassCount();
		long totalLoadedClasses = clMXBean.getTotalLoadedClassCount();
		itracer.trace(loadedClasses, "Loaded Classes", CLASSLOAD_ROOT);
		itracer.traceDelta(unLoadedClasses, "Unload Class Rate", CLASSLOAD_ROOT);
		itracer.traceDelta(totalLoadedClasses, "Load Class Rate", CLASSLOAD_ROOT);
	}
	
	/**
	 * Collects JIT compiler stats
	 */
	protected void collectCompilation() {
		itracer.traceDelta(compMXBean.getTotalCompilationTime(), "Compilation Rate", COMPILER_ROOT);
	}
	
	/**
	 * Collects heap and non heap memory stats.
	 */
	protected void collectMemory() {
		itracer.trace(memMXBean.getObjectPendingFinalizationCount(), "Objects Pending Finalization", MEMORY_ROOT);
		MemoryUsage heap = memMXBean.getHeapMemoryUsage();
		MemoryUsage nonHeap = memMXBean.getNonHeapMemoryUsage();

		itracer.trace(heap.getCommitted(), "Committed", HEAP_MEMORY_ROOT);
		itracer.trace(heap.getMax(), "Max", HEAP_MEMORY_ROOT);
		itracer.trace(heap.getInit(), "Init", HEAP_MEMORY_ROOT);
		itracer.trace(heap.getUsed(), "Used", HEAP_MEMORY_ROOT);
		itracer.trace(calcPercent(heap.getUsed(), heap.getCommitted()), "% Usage", HEAP_MEMORY_ROOT);
		itracer.trace(calcPercent(heap.getUsed(), heap.getMax()), "% Capacity", HEAP_MEMORY_ROOT);
		
		itracer.trace(nonHeap.getCommitted(), "Committed", NONHEAP_MEMORY_ROOT);
		itracer.trace(nonHeap.getMax(), "Max", NONHEAP_MEMORY_ROOT);
		itracer.trace(nonHeap.getInit(), "Init", NONHEAP_MEMORY_ROOT);
		itracer.trace(nonHeap.getUsed(), "Used", NONHEAP_MEMORY_ROOT);
		itracer.trace(calcPercent(nonHeap.getUsed(), nonHeap.getCommitted()), "% Usage", NONHEAP_MEMORY_ROOT);
		itracer.trace(calcPercent(nonHeap.getUsed(), nonHeap.getMax()), "% Capacity", NONHEAP_MEMORY_ROOT);
		
	}
	
	/**
	 * Collects memory pool stats.
	 */
	protected void collectMemoryPools() {
		for(MemoryPoolMXBean pool: heapMemPoolMXBeans) {
			MemoryUsage usage = pool.getUsage();
			itracer.trace(usage.getCommitted(), "Committed", HEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(usage.getMax(), "Max", HEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(usage.getInit(), "Init", HEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(usage.getUsed(), "Used", HEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(calcPercent(usage.getUsed(), usage.getCommitted()), "% Usage", HEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(calcPercent(usage.getUsed(), usage.getMax()), "% Capacity", HEAP_MEMORY_ROOT, pool.getName());
		}
		for(MemoryPoolMXBean pool: nonHeapMemPoolMXBeans) {
			MemoryUsage usage = pool.getUsage();
			itracer.trace(usage.getCommitted(), "Committed", NONHEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(usage.getMax(), "Max", NONHEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(usage.getInit(), "Init", NONHEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(usage.getUsed(), "Used", NONHEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(calcPercent(usage.getUsed(), usage.getCommitted()), "% Usage", NONHEAP_MEMORY_ROOT, pool.getName());
			itracer.trace(calcPercent(usage.getUsed(), usage.getMax()), "% Capacity", NONHEAP_MEMORY_ROOT, pool.getName());
		}
		
	}
	
	
	/**
	 * Calcs an integer percentage
	 * @param part The part
	 * @param whole The whole
	 * @return The percentage that the part is of the whole
	 */
	protected static int calcPercent(float part, float whole) {
		if(part==0 || whole==0) return 0;
		float perc = part/whole*100;
		return (int)perc;
	}
	
}
