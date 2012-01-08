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
package org.helios.tracing.persistence.jdbc;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.tracing.core.trace.Agent;
import org.helios.tracing.core.trace.AgentMetric;
import org.helios.tracing.core.trace.Host;
import org.helios.tracing.core.trace.Metric;
import org.helios.tracing.core.trace.TraceInstance;
import org.helios.tracing.core.trace.TraceValue;
import org.helios.tracing.core.trace.cache.TraceModelCache;
import org.helios.tracing.persistence.miniorm.ITracePersistor;
import org.helios.tracing.persistence.miniorm.TraceObjectPersistorFactory;

/**
 * <p>Title: TraceInstanceJDBCPersistor</p>
 * <p>Description: Entry point for persisting TraceInstances to a JDBC store.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.persistence.jdbc.TraceInstanceJDBCPersistor</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class TraceInstanceJDBCPersistor extends ManagedObjectDynamicMBean implements Runnable {
	/**  */
	private static final long serialVersionUID = 7648729917838900693L;
	/** Static Logger */
	protected static final Logger LOG = Logger.getLogger(TraceInstanceJDBCPersistor.class);
	/** The trace model cache */
	protected TraceModelCache traceModelCache = null;
	/** The JDBC DataSource providing connections to the target trace persistence DB */
	protected DataSource dataSource = null;
	/** The last flush time */
	protected final AtomicLong lastFlush = new AtomicLong(0L);
	/** The time flush period */
	protected final AtomicLong timeTrigger = new AtomicLong(15000L);
	/** The elapsed time of the last flush */
	protected final AtomicLong lastFlushTime = new AtomicLong(0L);
	/** The number of items saved in the last flush */
	protected final AtomicInteger lastFlushSize = new AtomicInteger(0);	
	/** The size flush trigger */
	protected final AtomicInteger sizeTrigger = new AtomicInteger(100);
	/** the number of times flush has been called */
	protected final AtomicLong flushCount = new AtomicLong(0L);
	/** the total number of processed traces */
	protected final AtomicLong traceCount = new AtomicLong(0L);	
	
	/** Indicates if inserts should be batched */
	protected AtomicBoolean batchedInserts = new AtomicBoolean(false);
	/** The persistence worker thread */
	protected Thread persistorThread = null;
	/** The queue of pending items to persist */
	protected PersistenceQueue workQueue; 
	/** The incoming persistence item queue */
	protected ArrayBlockingQueue<TraceInstance> inputQueue = null;
	/** Indicates if the worker thread should keep running */
	protected final AtomicBoolean running = new AtomicBoolean(false);
	/** The configured item persistors */
	protected final Map<Class<?>, ITracePersistor> persistors = new HashMap<Class<?>, ITracePersistor>(5);
	/** An array of classes that will have persistors created for */
	protected static final Class<?>[] PERSISTENT_CLASSES = new Class[]{Host.class, Agent.class, Metric.class, AgentMetric.class, TraceValue.class};
	/** Worker thread serial number generator */
	protected static final AtomicInteger serial = new AtomicInteger();
	/** The worker thread's thread group */
	protected static final ThreadGroup THREAD_GROUP = new ThreadGroup("TraceInstanceJDBCPersistorThreadGroup");
	/** A thread factory for worker threads */
	protected static final ThreadFactory THREAD_FACTORY = new ThreadFactory(){
		public Thread newThread(Runnable runnable) {
			Thread t = new Thread(THREAD_GROUP, runnable, "TraceInstanceJDBCPersistorThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}		
	};
	
	
	/**
	 * Configures and starts the persistor thread
	 * @throws Exception
	 */
	public void start() throws Exception {
		LOG.info("\n\t==========================================\n\tStarting TraceInstanceJDBCPersistor\n\t==========================================\n");
		for(Class<?> pc: PERSISTENT_CLASSES) {
			persistors.put(pc, TraceObjectPersistorFactory.createPersistor(pc));
		}
		LOG.info("Created Persistors");
		persistorThread = THREAD_FACTORY.newThread(this);
		LOG.info("Created Worker Thread [" + persistorThread + "]");
		workQueue = new PersistenceQueue(traceModelCache);
		inputQueue = new ArrayBlockingQueue<TraceInstance>((sizeTrigger.get()*2));
		try {
			ObjectName on = JMXHelper.objectName("org.helios.tracing.persistence:service=PersistorService,type=" + getClass().getSimpleName());
			MBeanServer server = JMXHelper.getHeliosMBeanServer();
			if(server.isRegistered(on)) {
				try { server.unregisterMBean(on); } catch (Exception e) {}				
			}
			this.reflectObject(this);
			server.registerMBean(this, on);
		} catch (Exception e) {
			LOG.warn("Failed to create JMX interface for [" + getClass().getName() + "]. Continuing without.", e);
		}
		LOG.info("Loading TraceModelCache from DB....");
		long[] stats = primeCache();
		LOG.info("Loaded TraceModelCache from DB with [" + stats[0] + "] items in [" + stats[1] + "] ms.");
		persistorThread.start();
		running.set(true);
		LOG.info("Started persistor thread");
		LOG.info("\n\t==========================================\n\tStarted TraceInstanceJDBCPersistor\n\t==========================================\n");
	}
	
	/**
	 * Stops the persistor thread
	 */
	public void stop() {
		LOG.info("\n\t==========================================\n\tStopping TraceInstanceJDBCPersistor\n\t==========================================\n");
		running.set(false);
		persistorThread.interrupt();
		LOG.info("\n\t==========================================\n\tStopped TraceInstanceJDBCPersistor\n\t==========================================\n");
	}
	
	/**
	 * Creates a new TraceInstanceJDBCPersistor 
	 */
	public TraceInstanceJDBCPersistor() {
	}
	
	/**
	 * Clears and reloads the trace model cache from the DB
	 * @throws Exception
	 */
	protected long[] primeCache() throws Exception {
		traceModelCache.clear();
		long start = System.currentTimeMillis();
		long count = 0;
		for(Class<?> clazz: PERSISTENT_CLASSES) {
			ITracePersistor itp = persistors.get(clazz);
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rset = null;
			try {
				conn = dataSource.getConnection();
				long size = itp.getCount(conn);
				int fetchSize = size > Short.MAX_VALUE ? Short.MAX_VALUE : (int)size; 
				ps = itp.getLoadStatement(conn);
				ps.setFetchSize(fetchSize);
				rset = ps.executeQuery();
				Constructor<?> ctor = null;
				try { ctor = clazz.getDeclaredConstructor(ResultSet.class, TraceModelCache.class); } catch (Exception e) {}
				if(ctor==null) {
					LOG.info("Skipping Cache Prime for [" + clazz.getSimpleName() + "] (No correct ctor)");
					continue;
				}
				while(rset.next()) {
					ctor.newInstance(rset, traceModelCache);
					count++;
				}
			} catch (Exception e) {
				LOG.error("Failed to prime cache for [" + clazz.getSimpleName() + "]", e);
			} finally {
				try { rset.close(); } catch (Exception e) {}
				try { ps.close(); } catch (Exception e) {}
				try { conn.close(); } catch (Exception e) {}
			}
		}
		return new long[]{count, System.currentTimeMillis()-start};
	}
	
	/**
	 * The runnable definition to start the process loop
	 */
	public void run() {
		loop();
	}

	
	
	/**
	 * Runs the persistence loop
	 */
	protected void loop() {
		lastFlush.set(System.currentTimeMillis());
		while(running.get()) {
			try {
				long pollTime = timeTrigger.get() - (System.currentTimeMillis()-lastFlush.get());
				if(LOG.isTraceEnabled()) LOG.trace("Polling for [" + pollTime + "] ms.");
				TraceInstance ti = inputQueue.poll(pollTime, TimeUnit.MILLISECONDS);
				if(ti!=null) {
					if(LOG.isTraceEnabled()) LOG.trace("Queueing trace instance");
					workQueue.process(ti);
					traceCount.incrementAndGet();
				}
				if(workQueue.getQueueSize() >= sizeTrigger.get() || System.currentTimeMillis()-timeTrigger.get() >= lastFlush.get()) {
					if(LOG.isTraceEnabled()) LOG.trace("Calling flush....");
					processWorkQueue();					
				}				
			} catch (InterruptedException ie) {
				if(running.get()) {
					Thread.interrupted();
					Thread.interrupted();
				} else {
					break;
				}
			} catch (Exception e) {
				LOG.warn("Unexpected exception in persistence loop", e);
			}
		}
	}
	
	/**
	 * Persists all the items in the work queue
	 * @TODO: This can be optimized.
	 * @TODO: Implement batching option
	 * @TODO: Persistor instances can keep their PS instance. 
	 */
	@JMXOperation(name="processWorkQueue", description="Persists all the items in the work queue")
	public synchronized void processWorkQueue() {
		flushCount.incrementAndGet();
		lastFlush.set(System.currentTimeMillis());
		Connection conn = null;
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>(5);
		try {
			long start = System.currentTimeMillis();
			conn = dataSource.getConnection();	
			conn.setAutoCommit(false);
			int itemsSaved = 0;
			itemsSaved += processCollection(Host.class, conn, statements, workQueue.flushHosts());
			itemsSaved += processCollection(Agent.class, conn, statements, workQueue.flushAgents());
			itemsSaved += processCollection(Metric.class, conn, statements, workQueue.flushMetrics());
			itemsSaved += processCollection(AgentMetric.class, conn, statements, workQueue.flushAgentMetrics());
			itemsSaved += processCollection(TraceValue.class, conn, statements, workQueue.flushTraceValues());
			if(batchedInserts.get()) {
				for(PreparedStatement ps: statements) {
					ps.executeBatch();
				}
			}
			conn.commit();
			long elapsed = System.currentTimeMillis()-start;
			lastFlushTime.set(elapsed);
			lastFlushSize.set(itemsSaved);			
			if(itemsSaved>0) {
				LOG.info("Persistor saved [" + itemsSaved + "] items in [" + elapsed + "] ms.");
			}
		} catch (Exception e) {
			LOG.error("Unexpected exception in processWorkQueue", e);
		} finally {
			for(PreparedStatement ps: statements) {
				try { ps.close(); } catch (Exception e) {}
			}
			if(conn!=null) try { conn.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Executes the persistence process on each of the passed items
	 * @param type The class of the items to be persisted
	 * @param conn The JDBC connection
	 * @param statements A list of statements created if the insert is batched
	 * @param items The items to be persisted
	 * @return The number of items inserted
	 * @throws SQLException
	 */
	protected int processCollection(Class<?> type, Connection conn, List<PreparedStatement> statements, Collection<?> items) throws SQLException {
		int cnt = 0;
		if(!items.isEmpty()) {			
			ITracePersistor itp = persistors.get(type);
			if(batchedInserts.get()) {
				PreparedStatement ps = conn.prepareStatement(itp.getInsertSql());
				statements.add(ps);	
				for(Object item: items) {
					try {
						itp.doInsert(item, ps);
						traceModelCache.flagAsSaved(item);
						cnt++;
					} catch (Exception e) {}
				}
			} else {
				for(Object item: items) {
					try {
						itp.doInsert(item, conn);
						traceModelCache.flagAsSaved(item);
						cnt++;
					} catch (Exception e) {}
				}				
			}			
		}
		return cnt;
	}
	
	/**
	 * Processes a TraceInstance exchange
	 * @param exchange
	 * @throws InterruptedException
	 */
	public void process(TraceInstance trace) throws InterruptedException {		
		//workQueue.process(trace);
		inputQueue.put(trace);
	}

	/**
	 * @param traceModelCache the traceModelCache to set
	 */
	public void setTraceModelCache(TraceModelCache traceModelCache) {
		this.traceModelCache = traceModelCache;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Sets the maximum elapsed time before the work queue is flushed (ms)
	 * @param timeTrigger the timeTrigger to set
	 */
	public void setTimeTrigger(long timeTrigger) {
		this.timeTrigger.set(timeTrigger);
	}
	
	/**
	 * Returns the maximum elapsed time before the work queue is flushed
	 * @return the maximum elapsed time before the work queue is flushed
	 */
	@JMXAttribute(name="TimeTrigger", description="The maximum size of the items queue before it is flushed (ms)", mutability=AttributeMutabilityOption.READ_WRITE)
	public long getTimeTrigger() {
		return timeTrigger.get();
	}
	
	/**
	 * Returns the number of items pending in the work queue
	 * @return the number of items pending in the work queue
	 */
	@JMXAttribute(name="WorkQueueSize", description="The number of items pending in the work queue", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getWorkQueueSize() {
		return workQueue.getQueueSize();
	}

	/**
	 * Sets the maximum size of the items queue before it is flushed 
	 * @param sizeTrigger the sizeTrigger to set
	 */
	public void setSizeTrigger(int sizeTrigger) {
		this.sizeTrigger.set(sizeTrigger);
	}
	
	/**
	 * Returns the maximum size of the items queue before it is flushed
	 * @return the maximum size of the items queue before it is flushed
	 */
	@JMXAttribute(name="SizeTrigger", description="the maximum size of the items queue before it is flushed", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getSizeTrigger() {
		return sizeTrigger.get();
	}

	/**
	 * Indicates if batched inserts are being used
	 * @return true if using batched inserts, false otherwise
	 */
	@JMXAttribute(name="BatchedInserts", description="Indicates if batched inserts are being used", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getBatchedInserts() {
		return batchedInserts.get();
	}

	/**
	 * Sets the batchedInserts option
	 * @param batchedInserts true to use batched inserts, false otherwise
	 */
	public void setBatchedInserts(boolean batchedInserts) {
		this.batchedInserts.set(batchedInserts);
	}

	/**
	 * Returns the elapsed time in ms. of the last flush
	 * @return the elapsed time in ms. of the last flush
	 */
	@JMXAttribute(name="LastFlushTime", description="The elapsed time in ms. of the last flush", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastFlushTime() {
		return lastFlushTime.get();
	}
	
	/**
	 * Returns the time of the last flush
	 * @return the time of the last flush
	 */
	@JMXAttribute(name="LastFlushTimestamp", description="The time of the last flush", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastFlushTimestamp() {
		return lastFlush.get();
	}
	
	/**
	 * Returns the time until the next time triggered flush
	 * @return the time until the next time triggered flush
	 */
	@JMXAttribute(name="TimeToNextFlush", description="The time until the next time triggered flush", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTimeToNextFlush() {
		return (lastFlush.get() + timeTrigger.get())-System.currentTimeMillis();
	}

	/**
	 * Returns the number of items saved in the last flush
	 * @return the number of items saved in the last flush
	 */
	@JMXAttribute(name="LastFlushSize", description="The number of items saved in the last flush", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getLastFlushSize() {
		return lastFlushSize.get();
	}
	
	/**
	 * Returns the number of times flush has been called
	 * @return the number of times flush has been called
	 */
	@JMXAttribute(name="FlushCount", description="The number of times flush has been called", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFlushCount() {
		return flushCount.get();
	}
	
	/**
	 * Returns the total number of traces processed
	 * @return the total number of traces processed
	 */
	@JMXAttribute(name="TraceCount", description="The total number of traces processed", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTraceCount() {
		return traceCount.get();
	}
	
}
