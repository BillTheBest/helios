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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.opentypes.tabular.ConcurrentTabularData;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.jmxenabled.counters.RollingCounter;

/**
 * <p>Title: BasicRollingInvocationObserver</p>
 * <p>Description: Measures {@link InstrumentationProfile#BASIC} plus elapsed times and invocation concurrency </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.instrumentation.BasicRollingInvocationObserver</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class BasicRollingInvocationObserver extends BasicInvocationObserver {
	/** The rolling counter registration group */
	protected final Map<String, RollingCounter> registerGroup = new HashMap<String, RollingCounter>();
	/** The invocation count rolling counter */
	protected final LongRollingCounter invocationCounter;
	/** The exception count rolling counter */
	protected final LongRollingCounter exceptionCounter;
	
	
	/**
	 * Creates a new BasicRollingInvocationObserver
	 * @param profile The instrumentation profile
	 * @param name the name of the observer
	 * @param size ignored.
	 */
	public BasicRollingInvocationObserver(InstrumentationProfile profile, String name, int size) {
		super(profile, name, size);		
		invocationCounter = new LongRollingCounter("Invocations", size, registerGroup);
		exceptionCounter = new LongRollingCounter("Exceptions", size, registerGroup);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#exception()
	 */
	@Override
	public void exception() {
		super.exception();
		exceptionCounter.put(1L);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#start()
	 */
	@Override
	public void start() {
		super.start();
		invocationCounter.put(1L);
	}


	
	/**
	 * Returns the rolling invocation counter
	 * @return the rolling invocation counter
	 */
	@JMXAttribute(name="InvocationCounter", description="The rolling invocation counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public LongRollingCounter getInvocationCounter() {
		return invocationCounter;
	}
	
	/**
	 * Returns the rolling exception counter
	 * @return the rolling exception counter
	 */
	@JMXAttribute(name="ExceptionCounter", description="The rolling exception counter", mutability=AttributeMutabilityOption.READ_ONLY)
	public LongRollingCounter getExceptionCounter() {
		return exceptionCounter;
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.instrumentation.InvocationObserver#reset()
	 */
	@Override
	@JMXOperation(name="reset", description="Resets the observer's metrics")
	public void reset() {
		exceptionCounter.reset();
		invocationCounter.reset();
		super.reset();
	}
	
	/** A Composite/Tabular exposing all the rolling counters in one type */
	protected final Map<String, ConcurrentTabularData> counters = new  TreeMap<String, ConcurrentTabularData>();
	protected volatile CompositeType performanceCountersType = null;
	protected volatile CompositeData performanceCountersData = null;

	@JMXAttribute(name="PerformanceCounters", description="The performance counters", mutability=AttributeMutabilityOption.READ_ONLY)
	public CompositeData getPerformanceCounters() {
		return performanceCountersData;
	}
	
	/**
	 * Registers the performance counters into the aggregated composite.
	 */
	public void initPerfCounters() {
		
		for(Map.Entry<String, RollingCounter> reg: this.registerGroup.entrySet()) {
			Class<?> rcType = reg.getValue().getClass();
			RollingCounter rc = reg.getValue();
			String groupName = rc.getClass().getSimpleName() + "s";
			ConcurrentTabularData ctd = counters.get(groupName);			
			if(ctd==null) {
				ctd = new ConcurrentTabularData(rcType);				
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
	

}
