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
package org.helios.nativex.jmx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;

import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.opentypes.tabular.ConcurrentTabularData;
import org.helios.jmxenabled.counters.RollingCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.Sigar;

/**
 * <p>Title: AbstractNativeCounter</p>
 * <p>Description: Base class for native counter implementations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.AbstractNativeCounter</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public abstract class AbstractNativeCounter extends ManagedObjectDynamicMBean implements Runnable  {
	/**  */
	private static final long serialVersionUID = 6707084458912905159L;
	/** The shared sigar instance */
	protected static final Sigar sigar = HeliosSigar.getInstance().getSigar();
	/** The sampling thread pool thread group */
	protected static final ThreadGroup schedulerThreadGroup = new ThreadGroup("NativeSamplingGroup");
	/** The sampling thread pool thread name serial factory */
	protected static final AtomicInteger serial = new AtomicInteger(0);	
	/** A Composite/Tabular exposing all the rolling counters in one type */
	protected final Map<String, ConcurrentTabularData> counters = new  TreeMap<String, ConcurrentTabularData>();
	/** The sampling thread scheduler */
	protected static final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(
			3, new ThreadFactory() {
				public Thread newThread(Runnable r) {
					Thread t = new Thread(schedulerThreadGroup, r, "NativeSamplingThread#" + serial.incrementAndGet());
					t.setDaemon(true);
					return t;
				} 
			}
	); 
	/** instance logger */
	protected Logger log = Logger.getLogger(getClass());	
	/** The last sampling time for this counter */
	protected final AtomicLong samplingTime = new AtomicLong(0L);
	/** The last sampling CPU time for this counter */
	protected final AtomicLong samplingCPUTime = new AtomicLong(0L);
	/** The last sampling timestamp for this counter */
	protected final AtomicLong samplingTimeStamp = new AtomicLong(0L);
	
	/** The registered performance counters for this monitor */
	protected final Map<String, RollingCounter> registerGroup = new HashMap<String, RollingCounter>();
		
	
	
	
	/**
	 * Registers the counter's JMX management interface
	 * @param pairs a string array containing the name value pairs used for this counter's ObjectName
	 */
	protected void registerCounterMBean(String...pairs) {		
		StringBuilder b = new StringBuilder(getClass().getPackage().getName());
		b.append(":");		
		if(pairs==null) throw new RuntimeException("NativeCounter implementation of type [" + getClass().getName() + "] returned null ObjectNameProperties", new Throwable());
		if(pairs.length%2!=0) throw new RuntimeException("NativeCounter implementation of type [" + getClass().getName() + "] returned non-even number of ObjectNameProperties " + Arrays.toString(pairs), new Throwable());
		for(int i = 0; i < pairs.length; i++) {
			b.append(cleanAttr(pairs[i]));
			b.append("=");
			i++;
			b.append(cleanAttr(pairs[i]));
			b.append(",");
		}
		b.deleteCharAt(b.length()-1);
		objectName = JMXHelper.objectName(b);
		registerCounterMBean(objectName);
	}
	
	/**
	 * Cleans an attribute for use in an ObjectName.
	 * @TODO: We will need to add to this
	 * @param attr The attribute to clean
	 * @return a clean string
	 */
	protected static String cleanAttr(CharSequence attr) {
		if(attr==null) return "";
		return attr.toString().replace(":", ";");
	}
	
	/**
	 * Registers the counter's JMX management interface
	 * @param objectName The ObjectName for this MBean
	 */
	protected void registerCounterMBean(ObjectName objectName) {		
		this.objectName = objectName;
		this.reflectObject(this);
		try {
			if(JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
				JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName);
			}
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (Exception e) {
			log.error("Failed to register management interface for counter [" + objectName + "]", e);
		}
		// prime the gathering
		try { run(); } catch (Exception e) {}
	}
	
	
	/** The default rolling counter size */
	public static final int DEFAULT_ROLLING_SIZE = ConfigurationHelper.getIntSystemThenEnvProperty("org.helios.nativex.rollingcounter.size", 20);
	/** The default counter sampling period  in ms. */
	public static final int DEFAULT_SAMPLING_PERIOD = ConfigurationHelper.getIntSystemThenEnvProperty("org.helios.nativex.samplingperiod", 15000);
	/** The scheduling task handle */
	protected final Set<ScheduledFuture<?>> scheduleHandles = new HashSet<ScheduledFuture<?>>();
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Schedules the native sampling task using the default sampling schedule
	 * @param time The period of the sampling callback
	 * @param unit The unit of the period
	 * @param callback An optional callback object passed as an argument
	 */
	protected void scheduleSampling() {
		scheduleSampling(DEFAULT_SAMPLING_PERIOD, TimeUnit.MILLISECONDS, null);
	}
	
	
	/**
	 * Schedules the native sampling task
	 * @param time The period of the sampling callback
	 * @param unit The unit of the period
	 * @param callback An optional callback object passed as an argument
	 */
	protected void scheduleSampling(long time, TimeUnit unit, final Object callback) {	
		final AbstractNativeCounter thisCounter = this;
		Runnable r = new Runnable() {
			public void run() {
				if(callback!=null) {
					thisCounter.run(callback);
				} else {
					long startTime = System.currentTimeMillis();
					long cpuTime = HeliosSigar.getInstance().getThreadCpu().getTotal();
					thisCounter.run();
					samplingTime.set(System.currentTimeMillis()-startTime);
					samplingCPUTime.set(HeliosSigar.getInstance().getThreadCpu().getTotal()-cpuTime);
					samplingTimeStamp.set(System.currentTimeMillis());
				}
			}
		};
		scheduleHandles.add(scheduler.scheduleWithFixedDelay(r, 0, time, unit));
	}
	
	
	/**
	 * Default run impl.
	 */
	public void run() {
		
	}
	
	/**
	 * Default run with callback Object impl.
	 * @param The callback object
	 */
	public void run(Object callback) {
		
	}
	
	
	/**
	 * Cancels the scheduled sampling task when the MBean is unregistered
	 */
	@Override
	public void postDeregister() {
		super.postDeregister();
		if(!scheduleHandles.isEmpty()) {
			for(ScheduledFuture<?> sf: scheduleHandles) {
				sf.cancel(false);
			}
			scheduleHandles.clear();
		}
	}
	
	
	/**
	 * Rounds a double fraction to an int percent 
	 * @param d The double to convert
	 * @return an int
	 */
	public static int doubleToIntPercent(double d) {		
		return (int)(d*100);
	}
	
	/**
	 * Calculates a percentage
	 * @param total
	 * @param portion
	 * @return
	 */
	public static int percent(double total, double portion) {
		if(total==0 || portion==0) return 0;
		double perc = (total/portion)*100;
		return (int)perc;
	}
	
	
	/**
	 * The last sampling time for this counter in ms. 
	 * @return last sampling time
	 */
	@JMXAttribute(name="LastSamplingTime", description="The last sampling time for this counter in ms.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastSamplingTime() {
		return samplingTime.get();
	}
	
	/**
	 * The last sampling CPU time for this counter in ns. 
	 * @return last sampling CPU time
	 */
	@JMXAttribute(name="LastSamplingCPUTime", description="The last sampling CPU time for this counter in ns.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastSamplingCPUTime() {
		return samplingCPUTime.get();
	}
	
	/**
	 * The last sampling timestamp for this counter 
	 * @return last sampling timestamp
	 */
	@JMXAttribute(name="LastSamplingTimeStamp", description="The last sampling timestamp for this counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastSamplingTimeStamp() {
		return samplingTimeStamp.get();
	}
	
	/**
	 * The last sampling date for this counter 
	 * @return last sampling date
	 */
	@JMXAttribute(name="LastSamplingDate", description="The last sampling date for this counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getLastSamplingDate() {
		return new Date(samplingTimeStamp.get());
	}

	/**
	 * Returns a composite of all this monitor's rolling counters
	 * @return a composite of all this monitor's rolling counters
	 */
	@JMXAttribute(name="PerformanceCounters", description="A composite of all this monitor's rolling counters", mutability=AttributeMutabilityOption.READ_ONLY)
	public CompositeData  getPerformanceCounters() {
		return performanceCountersData;
	}


	/**
	 * Registers the performance counters into the aggregated composite.
	 */
	protected void initPerfCounters() {
		log.info("Initializing Rolling Performance Counters for [" + this.objectName + "]");
		for(Map.Entry<String, RollingCounter> reg: this.registerGroup.entrySet()) {
			Class<?> rcType = reg.getValue().getClass();
			RollingCounter rc = reg.getValue();
			String groupName = rc.getClass().getSimpleName() + "s";
			ConcurrentTabularData ctd = counters.get(groupName);			
			if(ctd==null) {
				ctd = new ConcurrentTabularData(rcType);
				if(log.isDebugEnabled()) log.debug("Created PerformanceCounters for [" + rcType.getSimpleName() + "]");
				counters.put(groupName, ctd);				
			}			
			ctd.put(rc);
		}
		if(counters.size()>0) {
			try {
				List<OpenType<?>> types = new ArrayList<OpenType<?>>();
				for(TabularData td: counters.values()) {
					types.add(td.getTabularType());
				}
				
				performanceCountersType = new CompositeType(getClass().getName() + "PerformanceCounters", "Rolling PerformanceCounters for " + getClass().getName(), 
						counters.keySet().toArray(new String[counters.size()]), counters.keySet().toArray(new String[counters.size()]),
						types.toArray(new OpenType[counters.size()]) 
						
				);
				performanceCountersData = new CompositeDataSupport(performanceCountersType, counters);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		
	}
	
	protected volatile CompositeType performanceCountersType = null;
	protected volatile CompositeData performanceCountersData = null;
	
	
	///    protected final Map<Class<?>, Map<String, ConcurrentTabularData>> counters = new  HashMap<Class<?>, Map<String, ConcurrentTabularData>>();
	
	
}
