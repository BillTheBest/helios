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
package org.helios.threads;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;


/**
 * <p>Title: ThreadFactoryBuilder</p>
 * <p>Description: Convenience class for creating customized thread factories.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.threads.ThreadFactoryBuilder</code></p>
 */

public class ThreadFactoryBuilder  {
	/** Used to create unique thread name prefixes when one is not defined in the builder */
	protected static final AtomicLong defaultThreadNameSerial = new AtomicLong(0);
	/** A map of created thread factories keyed by the thread group name / thread name prefix */
	protected static final Map<String, ThreadFactory> factories = new ConcurrentHashMap<String, ThreadFactory>();
	
	/**
	 * Creates a new Builder
	 * @return the created Builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}
	
	/**
	 * <p>Title: Builder</p>
	 * <p>Description: Builder impl. for building thread factory configurations.</p> 
	 * <p><code>org.helios.threads.ThreadFactoryBuilder.Builder</code></p>
	 */
	public static class Builder {
		/** Default name extension for config names that are not specified */
		private final long defaultSuffix = defaultThreadNameSerial.incrementAndGet();
		/** The default thread name prefix. Defaults to <code>org.helios.threads.ThreadFrom#--</code>*/
		private String threadNamePrefix = "HeliosThreadFrom#" + defaultSuffix;	
		/** The default thread group name prefix Defaults to <code>org.helios.threads.ThreadGroup#--</code> */
		private String threadGroupNamePrefix = "HeliosThreadGroup#" + defaultSuffix;		
		/** Indicates if created threads are daemons. Defaults to true */
		private boolean daemonThreads = true;
		/** The created thread priority. Defaults to <code>java.lang.Thread.NORM_PRIORITY</code> */
		private int priority = Thread.NORM_PRIORITY;
		/** The stack size for each created thread. Defaults to 0 */
		private long stackSize = 0L;
		/** The context class loader for created threads. defaults to null and created threads will have the default class loader */
		private ClassLoader contextClassLoader = null;
		/** The created threads' uncaught exception handler. Default is null and created threads will not have defined exception handlers. */
		private UncaughtExceptionHandler exceptionHandler = null;
		/** The parent thread group for this factory. Default is null and if threads are created in a thread group, that thread group will have no parent thread group. */
		private ThreadGroup parentThreadGroup = null;
		/** The JMX object name of the thread factory management interface. Default is null. If this value is null when the factory is built, no management interface will be registered */
		private ObjectName objectName = null;
		/** Indicates if a default ObjectName should be created based on the thread group and thread name prefix */
		private boolean defaultObjectName = false;
		/** Provides serial numbers for created threads */
		private final AtomicLong threadNameSerial = new AtomicLong(0L);
		
		/** The default domain for default management interface object names */
		public static final String OBJECT_NAME_DOMAIN = "org.helios.threading";
		
		/**
		 * Sets the thread name prefix for created threads. Ignored if null.
		 * @param threadNamePrefix the threadNamePrefix to set
		 * @return this builder
		 */
		public Builder setThreadNamePrefix(String threadNamePrefix) {
			if(threadNamePrefix!=null) {
				this.threadNamePrefix = threadNamePrefix;
			}
			return this;
		}

		/**
		 * Sets the thread group name prefix for created threads. If null, threads will not be created in a thread group.
		 * @param threadGroupNamePrefix the threadGroupNamePrefix to set
		 * @return this builder
		 */
		public Builder setThreadGroupNamePrefix(String threadGroupNamePrefix) {
			this.threadGroupNamePrefix = threadGroupNamePrefix;
			return this;
		}
		
		/**
		 * Sets whether created threads will be daemon threads. 
		 * @param daemonThreads if true, created threads will be daemon threads, otherwise they will not.
		 * @return this builder
		 */
		public Builder setDaemonThreads(boolean daemonThreads) {
			this.daemonThreads = daemonThreads;
			return this;
		}
		/**
		 * Sets the priority of created threads
		 * @param priority the priority to set
		 * @return this builder
		 */
		public Builder setPriority(int priority) {
			this.priority = priority;
			return this;
		}
		/**
		 * Sets the stack size of created threads.
		 * @param stackSize the stackSize to set
		 * @return this builder
		 */
		public Builder setStackSize(long stackSize) {
			this.stackSize = stackSize;
			return this;
		}
		/**
		 * Sets the context classloader for created threads. Ignored if null.
		 * @param contextClassLoader the contextClassLoader to set
		 * @return this builder
		 */
		public Builder setContextClassLoader(ClassLoader contextClassLoader) {
			this.contextClassLoader = contextClassLoader;
			return this;
		}
		/**
		 * Sets the exception handler for created threads. Ignored if null.
		 * @param exceptionHandler the exceptionHandler to set
		 * @return this builder
		 */
		public Builder setExceptionHandler(UncaughtExceptionHandler exceptionHandler) {
			this.exceptionHandler = exceptionHandler;
			return this;
		}
		/**
		 * Sets the parent thread group parent for the created threads thread group. If null, the created threads thread group will not have a parent.
		 * @param parentThreadGroup the parentThreadGroup to set
		 * @return this builder
		 */
		public Builder setParentThreadGroup(ThreadGroup parentThreadGroup) {
			this.parentThreadGroup = parentThreadGroup;
			return this;
		}
		
		/**
		 * Sets the JMX ObjectName for the thread factory's management interface. 
		 * @param objectName the objectName to set
		 * @return this builder
		 */
		public Builder setObjectName(ObjectName objectName) {
			this.objectName = objectName;
			return this;
		}
		
		/**
		 * Directs the builder to configure a default ObjectName based on the thread group name and thread prefix name.
		 * @param set If true, a default ObjectName will be created.
		 * @return this builder
		 */
		public Builder setDefaultObjectName(boolean set) {
			defaultObjectName =set;
			return this;
		}
		
		
		/**
		 * Builds the configured ThreadFactory.
		 * @return a ThreadFactory
		 */
		public ThreadFactory build() {
			final Builder finalBuilder = this;
			ThreadFactory tf = new HeliosThreadFactory() {

				/**
				 * Creates a new Thread
				 * @param runnable The runnable the thread will execute
				 * @return the created thread
				 */
				@Override
				public Thread newThread(Runnable runnable) {
					ThreadGroup tg = null;
					if(finalBuilder.threadGroupNamePrefix !=null) {
						if(finalBuilder.parentThreadGroup!=null) {
							tg = new ThreadGroup(finalBuilder.parentThreadGroup, finalBuilder.threadGroupNamePrefix);
						} else {
							tg = new ThreadGroup(finalBuilder.threadGroupNamePrefix);
						}
					}
					Thread t = null;
					if(tg!=null) {
						t = new HeliosThread(tg, runnable, finalBuilder.threadNamePrefix + "-" + threadNameSerial.incrementAndGet(), finalBuilder.stackSize);
					} else {
						t = new HeliosThread(runnable, finalBuilder.threadNamePrefix + "-" + threadNameSerial.incrementAndGet());
					}
					t.setDaemon(finalBuilder.daemonThreads);
					t.setPriority(finalBuilder.priority);
					if(finalBuilder.contextClassLoader != null) {
						t.setContextClassLoader(finalBuilder.contextClassLoader);
					}
					if(finalBuilder.exceptionHandler!=null) {
						t.setUncaughtExceptionHandler(finalBuilder.exceptionHandler);
					}
					return t;
				}
				
			};
			ObjectName on = null;
			if(finalBuilder.objectName!=null) {
				on = finalBuilder.objectName;
			} else {
				if(finalBuilder.defaultObjectName) {
					try {
						on = new ObjectName(new StringBuilder()
							.append("service=HeliosThreadFactory")
							.append(finalBuilder.threadGroupNamePrefix==null ? "" : ",group=" + finalBuilder.threadGroupNamePrefix)
							.append(finalBuilder.threadNamePrefix==null ? "" : ",name=" + finalBuilder.threadNamePrefix)
							.toString());
					} catch (Exception e) {
						throw new RuntimeException("Failed to create default ObjectName for HeliosThreadFactory [" + tf + "].", e);
					}
				}
			}
			return tf;
		}

		
	}
	
	public static interface HeliosThreadFactoryMBean extends ThreadFactory {
		
	}
	
	public static abstract class HeliosThreadFactory implements HeliosThreadFactoryMBean {
	}

}
