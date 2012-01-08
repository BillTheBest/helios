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
package org.helios.tracing;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.helios.tracing.bridge.ITracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.interval.accumulator.AccumulatorManager;
import org.helios.tracing.subtracer.IIntervalTracer;
import org.helios.tracing.subtracer.ISubTracer;
import org.helios.tracing.subtracer.ITemporalTracer;
import org.helios.tracing.subtracer.IUrgentTracer;
import org.helios.tracing.subtracer.IVirtualTracer;
import org.helios.tracing.subtracer.IntervalTracer;
import org.helios.tracing.subtracer.SubTracerKey;
import org.helios.tracing.subtracer.TemporalTracer;
import org.helios.tracing.subtracer.UrgentTracer;
import org.helios.tracing.subtracer.VirtualTracer;
import org.helios.tracing.trace.Trace;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * <p>Title: AbstractTracerInstanceFactory</p>
 * <p>Description: </p>
 * <p>Extending requires the implementation of <ol>
 * 	<li><b><code>getTracer()</code></b></li>
 * 	<li><b><code>submitIntervalTraces(arr)</code></b></li>
 * 	<li><b><code>submitTraces(arr)</code></b></li>
 * 	<li><b><code></code></b></li>
 * 	<li><b><code></code></b></li>
 * </ol></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.AbstractTracerInstanceFactory</code></p>
 */

public abstract class AbstractTracerInstanceFactory implements ITracerInstanceFactory {
	/** A reference to the current instance of the tracer. */
	protected final ITracer tracerInstance;
	/** A reference to the current instance of the bridge. */
	private final AtomicReference<ITracingBridge> bridgeRef = new AtomicReference<ITracingBridge>(null);
	/** A the current instance of the bridge. */
	protected volatile ITracingBridge bridge = null;	
	
	/** A map of subtracers created for this tracing endpoint */
	protected final Map<SubTracerKey, ISubTracer> subTracerCache = new ConcurrentHashMap<SubTracerKey, ISubTracer>();
	/** The tracer factory thread pool */
	protected Executor executor = null;
	/** Factory Id */
	protected final String factoryId = getClass().getSimpleName() + "TracerFactory";
	/** Instance Logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/** The trace send counter */
	protected final AtomicLong traceSendCounter = new AtomicLong(0L);
	/** The interval trace send counter */
	protected final AtomicLong intervalTraceSendCounter = new AtomicLong(0L);
	/** The trace drop counter */
	protected final AtomicLong traceDropCounter = new AtomicLong(0L);
	/** The interval trace drop counter */
	protected final AtomicLong intervalTraceDropCounter = new AtomicLong(0L);	
	/** The last counter reset timestamp */
	protected final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
	
	/** Singleton Cache */
	protected static final Map<String, ITracerInstanceFactory> instances = new ConcurrentHashMap<String, ITracerInstanceFactory>();
	
	/**
	 * Acquires the current singleton instance of <b>T</b> or creates a new one with the passed configuration and caches it. 
	 * @param className The class name of the tracer factory to acquire an instance of 
	 * @param configuration Args4j style configuration for the tracer factory
	 * @return a configured instance of the tracer factory 
	 */
	public static <T extends ITracerInstanceFactory> T _getInstance(CharSequence className, String...configuration) {
		try {
			return _getInstance((Class<T>) Class.forName(className.toString()), configuration);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create instance of [" + className + "]", e);
		}
	}
	
	/**
	 * Acquires a newly configured tracer factory with the passed configuration and caches it, replacing any instance that was present. 
	 * @param className The class name of the tracer factory to acquire an instance of 
	 * @param configuration Args4j style configuration for the tracer factory
	 * @return a configured instance of the tracer factory 
	 */
	public static <T extends ITracerInstanceFactory> T _getForcedInstance(CharSequence className, String...configuration) {
		try {
			return _getForcedInstance((Class<T>) Class.forName(className.toString()), configuration);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create instance of [" + className + "]", e);
		}
	}
	
	
	
	/**
	 * Acquires the current singleton instance of <b>T</b> or creates a new one with the passed configuration and caches it. 
	 * @param clazz The class of the tracer factory to acquire an instance of 
	 * @param configuration Args4j style configuration for the tracer factory
	 * @return a configured instance of the tracer factory 
	 */
	public static <T extends ITracerInstanceFactory> T _getInstance(Class<T> clazz, String...configuration) {
		String className = clazz.getName();
		ITracerInstanceFactory factory = instances.get(className);
		Logger LOG = Logger.getLogger(clazz);
		if(factory == null) {
			synchronized(instances) {
				factory = instances.get(className);
				if(factory == null) {
					CmdLineParser parser = null;
					try {
						factory = (ITracerInstanceFactory)clazz.newInstance();
						if(configuration!=null && configuration.length > 0) {
							if(LOG.isDebugEnabled()) {
								LOG.debug("Creating " + className + " with configuration " + Arrays.toString(configuration));
							}
							parser = new CmdLineParser(factory);
							parser.parseArgument(configuration);							
						}
						instances.put(className, factory);
					} catch (CmdLineException cle) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						parser.printUsage(baos);
						LOG.error("\n\tTracerInstanceFactory Failure.\n\t" + cle.getMessage() + "\n\t" + baos.toString());
				          
						
					} catch (Exception e) {
						throw new RuntimeException("Failed to acquire TracerInstanceFactory[" + className + "]", e);
					}
				}
			}
		}
		return (T) factory;
	}
	
	/**
	 * Acquires a newly configured tracer factory with the passed configuration and caches it, replacing any instance that was present. 
	 * @param clazz The class of the tracer factory to acquire an instance of 
	 * @param configuration Args4j style configuration for the tracer factory
	 * @return a configured instance of the tracer factory 
	 */
	public static <T extends ITracerInstanceFactory> T _getForcedInstance(Class<T> clazz, String...configuration) {		
		synchronized(instances) {
			instances.remove(clazz.getName());
			return _getInstance(clazz, configuration);
		}
	}
	
	/**
	 * Sets a specific instance of a tracer factory to the singleton provided version
	 * @param instance the tracer factory version
	 */
	public static void setInstance(ITracerInstanceFactory instance) {
		synchronized(instances) {
			instances.put(instance.getClass().getName(), instance);
		}
		
	}
	
	
	
	
	/**
	 * Creates a new AbstractTracerInstanceFactory with the passed bridge.
	 * @param bridge a created bridge for this tracer factory
	 */
	protected AbstractTracerInstanceFactory(ITracingBridge bridge) {
		setBridge(bridge);
		tracerInstance = new TracerImpl(this);
	}
	
	/**
	 * Creates a new AbstractTracerInstanceFactory
	 */
	protected AbstractTracerInstanceFactory() {
		tracerInstance = new TracerImpl(this);
	}
	
	/**
	 * Returns this factory's tracing bridge
	 * @return a tracing bridge
	 */
	@Override
	public ITracingBridge getBridge() {
		return bridge;
	}
	
	/**
	 * Returns a reference to the bridge which will be updated if the factory changes the bridge
	 * @return a reference to the bridge
	 */
	public AtomicReference<ITracingBridge> getBridgeRef() {
		return bridgeRef;
	}
	
	
	/**
	 * Updates the bridge and bridge reference
	 * @param bridge the new bridge
	 */
	protected void setBridge(ITracingBridge bridge) {
		bridgeRef.set(bridge);
		this.bridge = bridge;
	}

	/**
	 * Returns an interval tracer for this factory
	 * @return an interval tracer
	 * @throws TracerInstanceFactoryException
	 */
	@Override
	public IIntervalTracer getIntervalTracer() throws TracerInstanceFactoryException {
		SubTracerKey key = this.getSubTracerKey(IntervalTracer.class);
		ISubTracer itracer = getSubTracer(key);
		if(itracer==null) {
			synchronized(subTracerCache) {
				itracer = getSubTracer(key);
				if(itracer==null) {
					long bitMask = AccumulatorManager.getInstance().get().registerTracerFactory(this);
					if(bitMask==-1) {
						// Already registered. Why are we here ?
					}
					itracer = new IntervalTracer(getTracer(), bitMask);
					subTracerCache.put(key, itracer);
				}
			}
		}
		return (IIntervalTracer)itracer;
	}
	
	/**
	 * The unique identifier for this factory
	 * @return the factoryId
	 */
	public String getFactoryId() {
		return factoryId;
	}
	
	
	/**
	 * Looks up the specified subTracer
	 * @param subTracerType The subtracer class name
	 * @param subKeys The subtracer subKeys
	 * @return the cached subtracer or null if one was not found.
	 */
	public ISubTracer getSubTracer(Class<? extends ISubTracer> subTracerType, String...subKeys) {
		return subTracerCache.get(getSubTracerKey(subTracerType, subKeys));
	}
	
	/**
	 * Looks up the subTracer identified by the passed subtracer key
	 * @param key The subtracer cache key
	 * @return the cached subtracer or null if one was not found.
	 */
	public ISubTracer getSubTracer(SubTracerKey key) {
		return subTracerCache.get(key);
	}
	
	
	/**
	 * Creates a subtracer key
	 * @param subTracerType The subtracer type
	 * @param subKeys the subtracer subkeys
	 * @return a subtracer key
	 */
	public SubTracerKey getSubTracerKey(Class<? extends ISubTracer> subTracerType, String...subKeys) {
		return SubTracerKey.makeKey(subTracerType, subKeys);
	}
	

	/**
	 * Acquires a temporal tracer from this factory
	 * @return a temporal tracer
	 * @throws TracerInstanceFactoryException
	 */
	@Override
	public ITemporalTracer getTemporalTracer() throws TracerInstanceFactoryException {
		SubTracerKey key = this.getSubTracerKey(TemporalTracer.class);
		ISubTracer itracer = getSubTracer(key);
		if(itracer==null) {
			synchronized(subTracerCache) {
				itracer = getSubTracer(key);
				if(itracer==null) {
					itracer = new TemporalTracer(getTracer());
					subTracerCache.put(key, itracer);
				}
			}
		}
		return (ITemporalTracer)itracer;
	}

	/**
	 * @return
	 * @throws TracerInstanceFactoryException
	 */
	@Override
	public ITracer getTracer() throws TracerInstanceFactoryException {
		return tracerInstance;
	}
	
	

	/**
	 * Returns an urgent tracer for this factory
	 * @return an urgent tracer
	 * @throws TracerInstanceFactoryException
	 */
	@Override
	public IUrgentTracer getUrgentTracer() throws TracerInstanceFactoryException {
		SubTracerKey key = this.getSubTracerKey(UrgentTracer.class);
		ISubTracer itracer = getSubTracer(key);
		if(itracer==null) {
			synchronized(subTracerCache) {
				itracer = getSubTracer(key);
				if(itracer==null) {
					itracer = new UrgentTracer(getTracer());
					subTracerCache.put(key, itracer);
				}
			}
		}
		return (IUrgentTracer)itracer;
	}

	/**
	 * Returns a virtual tracer for the passed host and agent
	 * @param host The virtual host
	 * @param agent The virtual agent name
	 * @return a virtual tracer
	 * @throws TracerInstanceFactoryException
	 */
	@Override
	public IVirtualTracer getVirtualTracer(String host, String agent) throws TracerInstanceFactoryException {
		SubTracerKey key = this.getSubTracerKey(VirtualTracer.class);
		ISubTracer itracer = getSubTracer(key);
		if(itracer==null) {
			synchronized(subTracerCache) {
				itracer = getSubTracer(key);
				if(itracer==null) {
					itracer = new VirtualTracer(host, agent, getTracer());
					subTracerCache.put(key, itracer);
				}
			}
		}
		return (IVirtualTracer)itracer;
	}

	/**
	 * Sets the thread pool for this factory
	 * @param threadPool
	 */
	@Override
	public void setExecutor(Executor threadPool) {
		this.executor = threadPool;

	}


	/**
	 * @param intervalTraces
	 */
	@Override
	public void submitIntervalTraces(Collection<IIntervalTrace> intervalTraces) {
		if(intervalTraces!=null && !intervalTraces.isEmpty()) {
			submitIntervalTraces(intervalTraces.toArray(new IIntervalTrace[intervalTraces.size()]));
		}
	}


	/**
	 * @param traces
	 */
	@Override
	public void submitTraces(Collection<Trace> traces) {
		if(traces!=null && !traces.isEmpty()) {
			submitTraces(traces.toArray(new Trace[traces.size()]));
		}
	}

	/**
	 * The number of traces sent since the last reset
	 * @return the traceSendCounter
	 */
	public long getTraceSendCounter() {
		return traceSendCounter.get();
	}

	/**
	 * The number of interval traces sent since the last reset
	 * @return the intervalTraceSendCounter
	 */
	public long getIntervalTraceSendCounter() {
		return intervalTraceSendCounter.get();
	}

	/**
	 * The number of traces dropped since the last reset
	 * @return the traceDropCounter
	 */
	public long getTraceDropCounter() {
		return traceDropCounter.get();
	}

	/**
	 * The number of interval traces dropped since the last reset
	 * @return the intervalTraceDropCounter
	 */
	public long getIntervalTraceDropCounter() {
		return intervalTraceDropCounter.get();
	}

	/**
	 * @return the lastResetTime
	 */
	public Date getLastResetTime() {
		return new Date(lastResetTime.get());
	}
	
	/**
	 * Resets the send and drop counters
	 */
	public void resetCounters() {
		traceSendCounter.set(0L);
		traceDropCounter.set(0L);
		intervalTraceSendCounter.set(0L);
		intervalTraceDropCounter.set(0L);
		lastResetTime.set(System.currentTimeMillis());
	}

}
