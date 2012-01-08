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
package org.helios.io.file.change;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

/**
 * <p>Title: FileChangeService</p>
 * <p>Description: A simple file change detection service that uses a background polling service to poll configured directories.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.io.file.change.FileChangeService</code></p>
 */

public class FileChangeService implements Runnable {
	/** The singleton instance */
	private static volatile FileChangeService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** The polling period in ms. */
	public static final long POLL_PERIOD = 5000;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** Uncaught poller thread exception count */
	protected final AtomicLong pollerExceptionCount = new AtomicLong(0L);
	/** Uncaught notification thread exception count */
	protected final AtomicLong notificationExceptionCount = new AtomicLong(0L);
	
	/** Work queue dropped task count */
	protected final AtomicLong fullWorkQueueDropCount = new AtomicLong(0L);
	
	/** The file change polling scheduler */
	protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory(){
		private final AtomicInteger serial = new AtomicInteger(0);
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "FileChangeServicePollingThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			t.setUncaughtExceptionHandler(new UncaughtExceptionHandler(){
				public void uncaughtException(Thread thread, Throwable throwable) {
					pollerExceptionCount.incrementAndGet();
					log.warn("Uncaught FileChangeService Poller Exception[" + thread.toString() + "]", throwable);
				}
			});
			return t;
		}
	});
	/** The change notification task execution thread pool */
	protected final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 5, 3*POLL_PERIOD, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100, true), new ThreadFactory(){
		private final AtomicInteger serial = new AtomicInteger(0);
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "FileChangeNotificationThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			t.setUncaughtExceptionHandler(new UncaughtExceptionHandler(){
				public void uncaughtException(Thread thread, Throwable throwable) {
					notificationExceptionCount.incrementAndGet();
					log.warn("Uncaught FileChangeNotification Exception[" + thread.toString() + "]", throwable);
				}
			});
			return t;
		}
			}, new RejectedExecutionHandler(){
				public void rejectedExecution(Runnable r, ThreadPoolExecutor threadPool) {
					fullWorkQueueDropCount.incrementAndGet();
				}		
	});
	
	/**
	 * Static singleton accessor
	 * @return the singleton instance of the FileChangeService
	 */
	public static FileChangeService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new FileChangeService();
				}
			}
		} 
		return instance;
	}
	
	/**
	 * Creates a new FileChangeService and initializes the service resources.
	 */
	private FileChangeService() {
		log.info("\n\t==================================\n\tStarted FileChangeService\n\t==================================\n");
	}

	/**
	 * Callback from the poller schedule to fire off a change poll.
	 */
	@Override
	public void run() {
		if(log.isDebugEnabled()) log.debug("Starting File Change Poll");
	}
}
