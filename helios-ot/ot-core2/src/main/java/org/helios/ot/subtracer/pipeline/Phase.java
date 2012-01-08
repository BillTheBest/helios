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
package org.helios.ot.subtracer.pipeline;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.JMXHelper;
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.helios.jmxenabled.threads.HeliosThreadPoolExecutorImpl;
import org.helios.jmxenabled.threads.TaskRejectionPolicy;
import org.helios.ot.trace.Trace;

/**
 * <p>Title: Phase</p>
 * <p>Description: A point in the OpenTrace pipeline where the trace processor will inspect a {@link org.helios.ot.trace.types.ITraceValue}  and fire any phase matching {@link org.helios.ot.subtracer.pipeline.IPhaseTrigger}s </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.subtracer.pipeline.Phase</code></p>
 */

public enum Phase {
	/** Enqueued to submission queue */
	SUBQ,
	/** Attached to disruptor TraceCollection */
	ATTACHED,
	/** Applied to an IntervalTrace */
	APPLIED,
	/** Flushed from an Interval Trace */
	FLUSHED,
	/** Processed by an end point */
	ENDPOINT;

	/** The asynch trigger processing thread pool */
	private static volatile HeliosThreadPoolExecutorImpl executor;

	
	/**
	 * Creates an array of KeyPhasedTriggers for the passed trigger as a convenience to pass to an ITrace or Builder.
	 * @param triggers The triggers to reflect/wrap
	 * @return an array of KeyedPhaseTriggers
	 */
	public static KeyedPhaseTrigger[] createPhaseTriggersFor(IPhaseTrigger...triggers) {
		if(triggers==null || triggers.length<1) return new KeyedPhaseTrigger[0];
		Set<KeyedPhaseTrigger> set = new HashSet<KeyedPhaseTrigger>();
		for(IPhaseTrigger trigger: triggers) {
			try {
				Phase[] phases = null;
				boolean asynch = false;
				PipeLineTrigger plt = null;
				if(trigger instanceof IPhaseAwarePhaseTrigger) {
					phases = ((IPhaseAwarePhaseTrigger)trigger).phases();
					asynch = ((IPhaseAwarePhaseTrigger)trigger).isAsynch();
				} else {
					plt = trigger.getClass().getAnnotation(PipeLineTrigger.class);
					if(plt==null) {
						try {
							//phaseTrigger(String phaseName, Trace trace);
							Method method = trigger.getClass().getDeclaredMethod("phaseTrigger", String.class, Trace.class);
							plt = method.getAnnotation(PipeLineTrigger.class);
							if(plt!=null) {
								phases = plt.phase();
								asynch = plt.asynch();
							}
						} catch (Exception e) {
							plt = null;
						}
					} else {
						phases = plt.phase();
						asynch = plt.asynch();
					}
				}
				if(phases!=null) {
					for(Phase phase: phases) {
						if(asynch) {
							set.add(new KeyedPhaseTrigger(phase, new AsynchPhaseTrigger(trigger), plt==null ? trigger.getClass().getSimpleName() : plt.name()));
						} else {
							set.add(new KeyedPhaseTrigger(phase, trigger, plt==null ? trigger.getClass().getSimpleName() : plt.name()));
						}
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to create KeyPhasedTriggers for instance of [" + trigger.getClass().getName() + "]", e);
			}
		}
		return set.toArray(new KeyedPhaseTrigger[set.size()]);
	}
	
	/**
	 * <p>Title: AsynchPhaseTrigger</p>
	 * <p>Description: A wrapper class for an IPhaseTrigger which is annotated to be asynchronous</p> 
	 * <p><code>org.helios.ot.subtracer.pipeline.Phase.AsynchPhaseTrigger</code></p>
	 */
	public static class AsynchPhaseTrigger implements IPhaseTrigger {
		/** The trigger to wrap */
		private final IPhaseTrigger wrappedTrigger;
		
		/**
		 * Creates a new AsynchPhaseTrigger
		 * @param trigger The trigger to wrap
		 */
		AsynchPhaseTrigger(IPhaseTrigger trigger) {
			if(trigger==null) throw new IllegalArgumentException("Passed trigger to be wrapped was null", new Throwable());
			if(executor==null) {
				bootAsynchThreadPool();
			}
			wrappedTrigger = trigger;
		}
		
		@Override
		public String toString() {
			return wrappedTrigger.toString();
		}
		
		/**
		 * Returns the asynch trigger's wrapped trigger
		 * @return the wrapped trigger
		 */
		public IPhaseTrigger getWrappedTrigger() {
			return wrappedTrigger;
		}
		
		/**
		 * The delegating call to fire the wrapped phase trigger asynchronously
		 * @param phaseName The phase name
		 * @param trace The trace
		 */
		public void phaseTrigger(final String phaseName, final Trace trace) {
			executor.execute(
			new Runnable() {
				public void run() {
					wrappedTrigger.phaseTrigger(phaseName, trace);
				}
			});
		}		
	}
	
	/**
	 * <p>Title: KeyedPhaseTrigger</p>
	 * <p>Description: A convenience packaging of a phase and a trigger that fires within it.</p> 
	 * <p><code>org.helios.ot.subtracer.pipeline.Phase.KeyedPhaseTrigger</code></p>
	 */
	public static class KeyedPhaseTrigger {
		/** The phase to trigger on */
		final Phase phase;
		/** The trigger to fire */
		final IPhaseTrigger trigger;
		/** The name of this trigger */
		final String name;
		/**
		 * Creates a new KeyedPhaseTrigger
		 * @param phase The phase to trigger on
		 * @param trigger The trigger to fire
		 * @param name The name of this trigger
		 */
		public KeyedPhaseTrigger(Phase phase, IPhaseTrigger trigger, String name) {
			this.phase = phase;
			this.trigger = trigger;
			this.name = checkName(name, trigger);
		}
		
		/**
		 * Checks the passed name and substitutes it with a synthetic name if it is blank.
		 * @param name The annotated trigger name
		 * @param trigger The trigger instance
		 * @return the name to implement
		 */
		private static String checkName(String name, IPhaseTrigger trigger) {
			if(!"".equals(name)) return name;
			Class<?> clazz = null;
			IPhaseTrigger actualTrigger = null;
			if(trigger instanceof AsynchPhaseTrigger) {
				actualTrigger = ((AsynchPhaseTrigger)trigger).getWrappedTrigger();
				clazz = actualTrigger.getClass();
			} else {
				actualTrigger = trigger;
				clazz = trigger.getClass();
			}
			return new StringBuilder(clazz.getSimpleName()).append("@").append(System.identityHashCode(actualTrigger)).toString();			
		}

		/**
		 * Returns the phase in which this trigger will be fired
		 * @return the phase
		 */
		public Phase getPhase() {
			return phase;
		}
		
		/**
		 * Returns the designated trigger name
		 * @return the trigger name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the trigger to fire
		 * @return the trigger
		 */
		public IPhaseTrigger getTrigger() {
			return trigger;
		}
		
	}
	
	/** The System/Env property name to define the Phase asynch executor's core pool size */
	public static final String ASYNCH_POOL_CORE = Phase.class.getPackage().getName() + ".threadpool.core";
	/** The default Phase asynch executor's core pool size which is 2 X the number of available processors */
	public static final int DEFAULT_ASYNCH_POOL_CORE = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()*2;
	/** The System/Env property name to define the Phase asynch executor's max pool size */
	public static final String ASYNCH_POOL_MAX = Phase.class.getPackage().getName() + ".threadpool.max";
	/** The default Phase asynch executor's max pool size which is 5 X the number of available processors */
	public static final int DEFAULT_ASYNCH_POOL_MAX = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()*5;
	/** The System/Env property name to define the Phase asynch executor's task queue size */
	public static final String ASYNCH_POOL_QSIZE = Phase.class.getPackage().getName() + ".threadpool.qsize";
	/** The default Phase asynch executor's task submission queue size */
	public static final int DEFAULT_ASYNCH_POOL_QSIZE = 100;

	
	/**
	 * Bootstraps the asynch trigger processing thread pool.
	 */
	private static void bootAsynchThreadPool() {
		if(executor==null) {
			ObjectName on = JMXHelper.objectName(Phase.class.getPackage().getName(), "service", "ThreadPool", "type", "AsynchPhaseTriggers");
			executor = (HeliosThreadPoolExecutorImpl) ExecutorBuilder.newBuilder()
			.setCoreThreads(ConfigurationHelper.getIntSystemThenEnvProperty(ASYNCH_POOL_CORE, DEFAULT_ASYNCH_POOL_CORE))
			.setMaxThreads(ConfigurationHelper.getIntSystemThenEnvProperty(ASYNCH_POOL_MAX, DEFAULT_ASYNCH_POOL_MAX))
			.setDaemonThreads(true)
			.setExecutorType(true)
			.setFairSubmissionQueue(false)
			.setPolicy(TaskRejectionPolicy.CALLERRUNS)
			.setPrestartThreads(1)
			.setTaskQueueSize(ConfigurationHelper.getIntSystemThenEnvProperty(ASYNCH_POOL_QSIZE, DEFAULT_ASYNCH_POOL_QSIZE))
			.setThreadGroupName("OpenTrace.PhaseTriggerProcessor")
			.setPoolObjectName(on)
			.setJmxDomains("DefaultDomain")
			.build();
		}
	}
}
