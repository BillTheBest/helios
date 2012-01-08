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
package org.helios.jmxenabled.threads;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.helios.helpers.ClassHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

import sun.management.ThreadInfoCompositeData;

/**
 * <p>Title: HeliosThreadGroup</p>
 * <p>Description: ThreadGroup extension to add in additional thread control and notifications.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.threads.HeliosThreadGroup</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class HeliosThreadGroup extends ThreadGroup {
	/** Cleaner thread */
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory(){
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "HeliosThreadGroupCacheCleaner");
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY+2);
			return t;
		}
		
	});
	/** A cache of thread groups keyed by name so requests for the same named thread group will return the same instance */
	private static final Map<String, HeliosThreadGroup> threadGroups = new ConcurrentHashMap<String, HeliosThreadGroup>();
	
	/** The tabular type for thread infos */
	public static final TabularType threadInfoTabularType = getThreadInfoTabularType();
	
	/** The cleaner thread period in ms. Defaults to 15000 ms. */
	protected final long cleanerPeriod;
	
	/** A set this group's threads */
	protected final ConcurrentSkipListSet<SoftThreadReference<HeliosThread, Thread>> threads = new ConcurrentSkipListSet<SoftThreadReference<HeliosThread, Thread>>();
	
	protected static final ThreadMXBean tmxBean = ManagementFactory.getThreadMXBean();
	
	/**
	 * Creates a new HeliosThreadGroup
	 * @param name The name of the thread group
	 * @param cleanerPeriod The cleaner thread period in ms.
	 */
	private HeliosThreadGroup(String name, long cleanerPeriod) {
		super(name);
		this.cleanerPeriod = cleanerPeriod;
		scheduler.scheduleWithFixedDelay(new Runnable(){public void run(){clean();}}, this.cleanerPeriod, this.cleanerPeriod, TimeUnit.MILLISECONDS);
	}
	
	
	/**
	 * Creates a new HeliosThreadGroup using the default cleaner period of 15000 ms.
	 * @param name The name of the thread group
	 */
	private HeliosThreadGroup(String name) {
		this(name, 15000L);
	}
	
	/**
	 * Acquires the named ThreadGroup with a default cleaner period.
	 * @param name The name of the ThreadGroup
	 * @return the named ThreadGroup
	 */
	public static HeliosThreadGroup getInstance(String name) {
		return getInstance(name, 15000L);
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(name="ThreadInfos", description="The ThreadInfos for all the threads in this thread group", mutability=AttributeMutabilityOption.READ_ONLY)
	public TabularData getThreadInfos() {
		TabularDataSupport tds = new TabularDataSupport(threadInfoTabularType, threads.size(), 0.75f);
		for(SoftThreadReference<HeliosThread, Thread> threadRef : threads) {
			Thread thread = threadRef.get();
			if(thread!=null) {				
				ThreadInfo ti = tmxBean.getThreadInfo(thread.getId());
				if(ti!=null) {
					tds.put(ThreadInfoCompositeData.toCompositeData(ti));
				}
			}
		}
		return tds;
		 
	}
	
	
	/**
	 * Acquires the named ThreadGroup
	 * @param name The name of the ThreadGroup
	 * @param cleanerPeriod The cleaner thread period in ms. Ignored if the thread group already exists.
	 * @return the named ThreadGroup
	 */
	public static HeliosThreadGroup getInstance(String name, long cleanerPeriod) {
		if(name==null) throw new IllegalArgumentException("Passed thread group name was null", new Throwable());
		HeliosThreadGroup tg = threadGroups.get(name);
		if(tg==null) {
			synchronized(threadGroups) {
				tg = threadGroups.get(name);
				if(tg==null) {
					tg = new HeliosThreadGroup(name, cleanerPeriod);
					threadGroups.put(name, tg);
				}
			}
		}
		return tg;
	}
	
	/**
	 * Adds a new trackable thread to to the thread group.
	 * Ignored if thread is null or in a terminated state
	 * @param t The thread to add.
	 */
	void addThread(HeliosThread t) {
		if(t!=null && !t.getState().equals(Thread.State.TERMINATED)) {
			threads.add(new SoftThreadReference<HeliosThread, Thread>(t));
		}
	}
	
	/**
	 * Removes a trackable thread from the thread group
	 * @param t The thread to remove
	 */
	void removeThread(HeliosThread t) {
		if(t!=null) {
			threads.remove(t);
		}
	}
	
	/**
	 * Periodic task to clean the thread cache of garbage collected or terminated threads.
	 */
	void clean() {
		if(!threads.isEmpty()) {
			while(true) {
				SoftThreadReference<HeliosThread, Thread> softRef = threads.last();
				HeliosThread ht = softRef.get();
				if(ht==null || ht.getState().equals(Thread.State.TERMINATED)) {
					threads.remove(softRef);
				} else {
					break;
				}
			}
		}
	}
	
	/**
	 * Returns a collection of the threads in the thread group.
	 * @return a collection of the threads in the thread group.
	 */
	public Collection<HeliosThread> getThreads() {
		Set<HeliosThread> ts = new HashSet<HeliosThread>(threads.size());		
		for(Iterator<SoftThreadReference<HeliosThread, Thread>> iter = threads.iterator(); iter.hasNext();) {
			SoftThreadReference<HeliosThread, Thread> softRef = iter.next();
			HeliosThread ht = softRef.get();
			if(ht==null) {
				iter.remove();
				continue;
			}
			ts.add(ht);
		}
		return Collections.unmodifiableCollection(ts);
	}
	
	/**
	 * Generates a tabular type for ThreadInfos
	 * @return a tabular type
	 */
	private static TabularType getThreadInfoTabularType() {
		String typeName = ThreadInfo.class.getName() + "TabularType";
		try {
			return new TabularType(
					typeName, 
					"A table of all the thread infos for the threads in this group", 
					ThreadInfoCompositeData.toCompositeData(
							ManagementFactory.getThreadMXBean().getThreadInfo(
									Thread.currentThread().getId()
									)
							).getCompositeType(), 
					new String[]{"threadId"}
				);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ThreadInfoTabularType", e);
		}
	}

	/**
	 * <p>Title: SoftThreadReference</p>
	 * <p>Description: SoftReference construct to cache threads in the thread group but allow the cache to drop the threads if they are garbage collected.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	class SoftThreadReference<T extends Comparable<C>, C> extends SoftReference<T> implements Comparable<C> {
		private final int hashCode;
		/**
		 * Creates a new SoftThreadReference
		 * @param referent The thread to create a soft reference for
		 */
		public SoftThreadReference(T referent) {			
			super(ClassHelper.nvl(referent, "Cannot create SoftReference for null referent"));
			hashCode = System.identityHashCode(referent);
		}


		/**
		 * Delegates the comparator operation to the referent. If the referent is null, sorts high.
		 * @param t A referent of the same type as this referent to compare to
		 * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object. 
		 */
		@Override
		public int compareTo(C c) {
			T t = get();
			if(t==null) {
				threads.remove(this);
				return 1;
			}
			return new Long(((Thread)t).getId()).compareTo(
					new Long(
							((SoftThreadReference<HeliosThread, Thread>)c).get().getId()
					)
			);
		}


		/**
		 * @return
		 */
		@Override
		public int hashCode() {
			return hashCode;
		}


		/**
		 * @param obj
		 * @return
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SoftThreadReference other = (SoftThreadReference) obj;
			if (hashCode != other.hashCode)
				return false;
			return true;
		}		
		
	}
}
