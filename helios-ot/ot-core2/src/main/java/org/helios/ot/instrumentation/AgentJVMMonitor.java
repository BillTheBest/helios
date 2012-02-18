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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.JMXHelper;
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
	/** A map of timestamp/gc time to calc deltas */
	private final Map<String, long[]> gcDeltas = new HashMap<String, long[]>(gcMXBeans.size());
	/** A map of last gc atribute availability indicators for each GC Bean */
	private final Map<String, Boolean> gcLastGCAvailable = new HashMap<String, Boolean>(gcMXBeans.size());
	/** A map of last gc event ID for each gc bean keyed by the gc name */
	private final Map<String, Long> gcLastGCId = new HashMap<String, Long>(gcMXBeans.size());
	/** A set of NIO Buffer Pool MXBean ObjectNames */
	private final Set<ObjectName> bufferPoolObjectNames;
	/** A set of period executed runnables that perform the metrics collection */
	private final Set<Runnable> registeredCollectors = new CopyOnWriteArraySet<Runnable>();
	/** Simple timer for scheduling collections */
	private final Timer timer = new Timer(getClass().getSimpleName() + "-Thread", false);
	/** Timer task to execute the collection */
	private TimerTask timerTask = null;
	/** Instance logger */
	private final Logger log = Logger.getLogger(getClass());

	/** The platform MBeanServer */
	public static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
	
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
	/** Java 7 Indicator */
	public static final boolean java7 = isJava7();
	/** The JVM GC metrics name space  */
	public static final String GC_ROOT = ROOT + Trace.DELIM + "GarbageCollection";
	/** The NIO Buffer Pool Attributes */
	public static final String[] NIO_BUFFER_ATTRS = new String[]{"Count", "MemoryUsed", "TotalCapacity", "Name"};	
	/** The NIO namespace */
	public static final String NIO_ROOT = ROOT + Trace.DELIM + "NIO" + Trace.DELIM + "BufferPools";
	
	
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
	 * Determines if this JVM is version 7+
	 * @return true if this JVM is version 7+, false otherwise
	 */
	public static boolean isJava7() {
		try {
			Class.forName("java.lang.management.BufferPoolMXBean");
			return true;
		} catch (Exception e) {
			return false;
		}
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
		for(GarbageCollectorMXBean gcBean: gcMXBeans) {
			try {
				mbeanServer.getAttribute(gcBean.getObjectName(), "LastGcInfo");
				gcLastGCAvailable.put(gcBean.getName(), true);
			} catch(Exception e) {
				gcLastGCAvailable.put(gcBean.getName(), false);
			}
		}
		if(java7) {
			bufferPoolObjectNames = new HashSet<ObjectName>(mbeanServer.queryNames(JMXHelper.objectName("java.nio:type=BufferPool,name=*"), null));
		} else {
			bufferPoolObjectNames = Collections.emptySet();
		}
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
			collectGc();
			collectOS();
			if(java7) collectNio();
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
	 * Collects GC stats.
	 */
	protected void collectGc() {
		long now = System.currentTimeMillis();
		if(!gcDeltas.isEmpty()) {
			for(GarbageCollectorMXBean gcBean: gcMXBeans) {				
				String name = gcBean.getName();
				long[] lastPeriod = gcDeltas.get(name);
				if(lastPeriod!=null) {
					long gcTime = gcBean.getCollectionTime();
					long elapsed = now - lastPeriod[0];
					long gcElapsed = gcTime - lastPeriod[1];
					itracer.traceDelta(calcPercent(gcElapsed, elapsed), "Percent Time In GC", GC_ROOT, name);
				}				
			}
		}
		gcDeltas.clear();
		for(GarbageCollectorMXBean gcBean: gcMXBeans) {
			String name = gcBean.getName();
			long gcCount = gcBean.getCollectionCount();
			long gcTime = gcBean.getCollectionTime();
			gcDeltas.put(gcBean.getName(), new long[]{now, gcTime});
			itracer.traceDelta(gcTime, "GC Time", GC_ROOT, name);
			itracer.traceDelta(gcCount, "GC Count", GC_ROOT, name);
			if(gcLastGCAvailable.get(name)) {
				try {
					Long lastId = gcLastGCId.get(name);
					Long thisId = null;
					CompositeData gcInfo = (CompositeData)mbeanServer.getAttribute(gcBean.getObjectName(), "LastGcInfo");
					if(gcInfo!=null) {
						thisId = (Long)gcInfo.get("id");
						if(thisId.equals(lastId)) continue;
						gcLastGCId.put(name, thisId);
						try { itracer.trace((Long)gcInfo.get("duration"), "Last GC Duration", GC_ROOT, name, "Last GC"); } catch (Exception e) {}
						try { itracer.traceSticky((Integer)gcInfo.get("GcThreadCount"), "GC Threads", GC_ROOT, name); } catch (Exception e) {}
						TabularData memoryBeforeGc = (TabularData)gcInfo.get("memoryUsageBeforeGc");
						TabularData memoryAfterGc = (TabularData)gcInfo.get("memoryUsageAfterGc");
						String[] key = new String[1];
						for(String poolName: gcBean.getMemoryPoolNames()) {
							key[0] = poolName;
							MemoryUsage beforeGc = MemoryUsage.from((CompositeData)memoryBeforeGc.get(key).get("value"));
							MemoryUsage afterGc = MemoryUsage.from((CompositeData)memoryAfterGc.get(key).get("value"));
							long used = beforeGc.getUsed()-afterGc.getUsed();
							long committed = beforeGc.getCommitted()-afterGc.getCommitted();
							if(used<0) {
								itracer.trace(Math.abs(used), "Consumed", GC_ROOT, name, "Last GC", poolName);
							} else {
								itracer.trace(used, "Cleared", GC_ROOT, name, "Last GC", poolName);
							}
							if(committed<0) {
								itracer.trace(Math.abs(committed), "Allocated", GC_ROOT, name, "Last GC", poolName);
							} else {
								itracer.trace(committed, "Released", GC_ROOT, name, "Last GC", poolName);
							}							
						}
					}
				} catch (Exception e) {
						e.printStackTrace(System.err);
				}
			}
		}
	}
	
	/**
	 * Collects NIO stats 
	 */
	protected void collectNio() {
		for(ObjectName on: bufferPoolObjectNames) {
			try {
				AttributeList attrs = mbeanServer.getAttributes(on, NIO_BUFFER_ATTRS);
				Map<String, Object> attrMap = new HashMap<String, Object>(attrs.size());
				for(Attribute attr: attrs.asList()) {
					attrMap.put(attr.getName(), attr.getValue());
				}
				String name = (String)attrMap.get(NIO_BUFFER_ATTRS[3]);
				tracer.traceSticky((Long)attrMap.get(NIO_BUFFER_ATTRS[0]), "Count", NIO_ROOT, name);
				long used = (Long)attrMap.get(NIO_BUFFER_ATTRS[1]);
				long capacity = (Long)attrMap.get(NIO_BUFFER_ATTRS[2]);
				tracer.traceSticky(used, "MemoryUsed", NIO_ROOT, name);
				tracer.traceSticky(capacity, "TotalCapacity", NIO_ROOT, name);				
				tracer.traceSticky(calcPercent(used, capacity), "Percent Used", NIO_ROOT, name);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	
	/**
	 * Collects OS process stats
	 */
	protected void collectOS() {
		AttributeList attrs = null;
		try {
			attrs = mbeanServer.getAttributes(osMXBean.getObjectName(), windows ? WIN_OS_STATS : UNIX_OS_STATS);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return;
		}
		Map<String, Number> attrMap = new HashMap<String, Number>(attrs.size());
		for(Attribute attr: attrs.asList()) {
			attrMap.put(attr.getName(), (Number)attr.getValue());
		}		
		try {
			long totalSwap = attrMap.get(WIN_OS_STATS[1]).longValue();
			long freeSwap = attrMap.get(WIN_OS_STATS[2]).longValue();
			long usedSwap = totalSwap - freeSwap;
			tracer.traceSticky(totalSwap, WIN_OS_STATS[1], OS_ROOT, "Memory");
			tracer.traceSticky(freeSwap, WIN_OS_STATS[2], OS_ROOT, "Memory");
			tracer.traceSticky(calcPercent(usedSwap, totalSwap), "Percent Swap Used", OS_ROOT, "Memory");
		} catch (Exception e) {}
		try {
			long virtualMem = attrMap.get("CommittedVirtualMemorySize").longValue();
			long totalMem = attrMap.get("TotalPhysicalMemorySize").longValue();
			long freeMem = attrMap.get("FreePhysicalMemorySize").longValue();
			long usedMem = totalMem - freeMem;
			tracer.traceSticky(totalMem, "TotalPhysicalMemorySize", OS_ROOT, "Memory");
			tracer.traceSticky(freeMem, "FreePhysicalMemorySize", OS_ROOT, "Memory");
			tracer.traceSticky(usedMem, "UsedPhysicalMemorySize", OS_ROOT, "Memory");
			tracer.traceSticky(virtualMem, WIN_OS_STATS[0], OS_ROOT, "Memory");
			
			
			tracer.traceSticky(calcPercent(usedMem, totalMem), "Percent Memory Used", OS_ROOT, "Memory");
			tracer.traceSticky(calcPercent(freeMem, totalMem), "Percent Memory Free", OS_ROOT, "Memory");
			tracer.traceSticky(calcPercent(virtualMem, totalMem), "Percent Virtual Used", OS_ROOT, "Memory");
			tracer.traceSticky(calcPercent((totalMem-virtualMem), totalMem), "Percent Virtual Free", OS_ROOT, "Memory");
		} catch (Exception e) {}
		try {
			long now = System.nanoTime();
			long cpuElapsed = attrMap.get(WIN_OS_STATS[3]).longValue();
			if(processCpuTime!=null) {
				long elapsedClockTime = now - processCpuTime[0];
				long elapsedCpuTime = cpuElapsed - processCpuTime[1];
				long totalCpuTime = elapsedClockTime * processors;
				tracer.traceSticky(calcPercent(elapsedCpuTime, totalCpuTime), "JVM CPU Usage", OS_ROOT, "Processor");
			}
			processCpuTime = new long[]{now, cpuElapsed};
		} catch (Exception e) {}
		if(!windows) {
			try {
				long openFd = attrMap.get("OpenFileDescriptorCount").longValue();
				long maxFd = attrMap.get("MaxFileDescriptorCount").longValue();
				tracer.traceSticky(openFd, "Open File Descriptors", OS_ROOT, "Resources");
				tracer.traceSticky(calcPercent(openFd, maxFd), "Percentage Open File Descriptors", OS_ROOT, "Resources");
			} catch (Exception e) {}
		}
		
	}
	
	/** The root namespace for OS stats */
	public static final String OS_ROOT = ROOT + Trace.DELIM + ManagementFactory.getOperatingSystemMXBean().getName();
	
	/** OSMXBean Stats for Windows */
	public static final String[] WIN_OS_STATS = new String[]{"CommittedVirtualMemorySize", "TotalSwapSpaceSize", "FreeSwapSpaceSize", "ProcessCpuTime", "FreePhysicalMemorySize", "TotalPhysicalMemorySize"};
	/** OSMXBean Stats for Unix */
	public static final String[] UNIX_OS_STATS = new String[]{"CommittedVirtualMemorySize", "TotalSwapSpaceSize", "FreeSwapSpaceSize", "ProcessCpuTime", "FreePhysicalMemorySize", "TotalPhysicalMemorySize",
		"SystemCpuLoad", "ProcessCpuLoad", "SystemLoadAverage", "OpenFileDescriptorCount", "MaxFileDescriptorCount"
	};
	/** Indicates if this platform is Windows */
	public static final boolean windows = ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase().contains("windows");
	/** The number of processors */
	public static final int processors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** The timestamped process cpu time */
	protected long[] processCpuTime = null;
	
	
	/**
	 * Returns the passed byte count as Kb
	 * @param value the number of bytes
	 * @return the number of Kb
	 */
	protected static long toK(float value) {
		if(value==0) return 0L;
		float k = value/1024f;
		return (long)k;
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




/*
 * Unix OS
 * =======
   public native long getCommittedVirtualMemorySize();
    public native long getTotalSwapSpaceSize();
    public native long getFreeSwapSpaceSize();
    public native long getProcessCpuTime();
    public native long getFreePhysicalMemorySize();
    public native long getTotalPhysicalMemorySize();
    public native long getOpenFileDescriptorCount();
    public native long getMaxFileDescriptorCount();
    public native double getSystemCpuLoad();
    public native double getProcessCpuLoad();
    public double getSystemLoadAverage() 
    
    
    Windows OS
    ==========
    private native long getCommittedVirtualMemorySize0();
	public native long getTotalSwapSpaceSize();
	public native long getFreeSwapSpaceSize();
	public native long getProcessCpuTime();
  	public native long getFreePhysicalMemorySize();
  	public native long getTotalPhysicalMemorySize();
  	
  	Java 7 NIO
  	==========
  	java.nio:type=BufferPool,name=*
  	
  	Attrs:
  	Count
  	MemoryUsed
  	TotalCapacity
  	Name
  	
  	
 */
