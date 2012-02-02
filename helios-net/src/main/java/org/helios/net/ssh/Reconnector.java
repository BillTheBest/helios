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
package org.helios.net.ssh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.jmxenabled.threads.ExecutorBuilder;



/**
 * <p>Title: Reconnector</p>
 * <p>Description:Service to periodically attempt a reconnect of connection lost SSHServices.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.Reconnector</code></p>
 */

public class Reconnector {
	/** The reconnector singleton instance */
	private volatile static Reconnector instance = null;
	/** The reconnector singleton instance ctor lock */
	private final static Object lock = new Object();
	/** The scheduler to schedule reconnect attempts */
	protected final ScheduledThreadPoolExecutor reconnectThreadPool;
	/** A map of tracked SSHServices */
	protected final Map<SSHService, ScheduledFuture<?>> reconnectees = new ConcurrentHashMap<SSHService, ScheduledFuture<?>>();
	/** Static class logger */
	protected static final Logger log = Logger.getLogger(Reconnector.class);
	
	/** The configuration variable for the reconnect period in ms. */
	public static final String RECONNECT_PERIOD_PROP = "org.helios.net.ssh.reconnect.period";
	/** The default reconnect period */	
	public static final long DEFAULT_RECONNECT_PERIOD = 10000; 
	
	/**
	 * Acquires the Reconnector singleton
	 * @return the Reconnector singleton
	 */
	public static Reconnector getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Reconnector();
					log.info("Reconnector Service Started");
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * Creates a new Reconnector
	 */
	private Reconnector() {
		reconnectThreadPool = (ScheduledThreadPoolExecutor)ExecutorBuilder.newBuilder()
				.setExecutorType(false)
				.setCoreThreads(5)
				.setDaemonThreads(true)
				.setPoolObjectName(getClass().getPackage().getName(), "service", "Scheduler", "name", getClass().getSimpleName())
				.setThreadGroupName(getClass().getSimpleName() + "ThreadGroup")			
				.build();
	}

	
	/**
	 * Schedules a reconnect task for the passed service
	 * @param service The service to reconnect
	 */
	public void scheduleReconnect(final SSHService service) {
		long period = ConfigurationHelper.getLongSystemThenEnvProperty(RECONNECT_PERIOD_PROP, DEFAULT_RECONNECT_PERIOD);
		scheduleReconnect(service, period, TimeUnit.MILLISECONDS);
	}

	
	/**
	 * Schedules a reconnect task for the passed service
	 * @param service The service to reconnect
	 * @param reconnectPeriod The reconnect attempt period
	 * @param unit The period unit
	 */
	public void scheduleReconnect(final SSHService service, long reconnectPeriod, TimeUnit unit) {
		ScheduledFuture<?> sf = reconnectThreadPool.scheduleWithFixedDelay(new Runnable(){
			public void run() {
				log.info("Attempting to reconnect [" + service + "]");
				if(service.reconnect()) {
					log.info("Reconnected [" + service + "]");
					ScheduledFuture<?> cancel = reconnectees.get(service);
					if(cancel!=null) {
						cancel.cancel(false);
					}
				}
			}
		}, reconnectPeriod, reconnectPeriod, unit);
		ScheduledFuture<?> oldsf = reconnectees.put(service, sf);
		if(oldsf!=null) {
			oldsf.cancel(true);
		}		
	}
	
	
}
