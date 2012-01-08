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
package org.helios.tracing.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: CountDownLatchBroadcaster</p>
 * <p>Description: An extension of java.util.concurrent.CountDownLatch that broadcasts when the countdown is complete.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1647 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/util/CountDownLatchBroadcaster.java $
 * $Id: CountDownLatchBroadcaster.java 1647 2009-10-24 21:52:31Z nwhitehead $
 */
public final class CountDownLatchBroadcaster extends CountDownLatch {
	protected Set<CountDownLatchListener> listeners = new HashSet<CountDownLatchListener>();
	protected long timeOut = 0L;
	protected TimeUnit unit = null;
	protected long serial = 0L;	
	

	
	/**
	 * Creates a new CountDownLatchBroadcaster for the specified countdown with no timeout.
	 * @param count The countdown.
	 * @param serial The interval serial number
	 * @param listeners An array of listeners to register.
	 */
	public CountDownLatchBroadcaster(int count, long serial, CountDownLatchListener...listeners) {
		super(count);
		this.serial = serial;
		if(listeners!=null) {
			for(CountDownLatchListener listener: listeners) {
				this.listeners.add(listener);
			}
		}
		CountDownWatcherThread watcher = new CountDownWatcherThread(this, serial);
		watcher.start();
	}
	
	/**
	 * Creates a new CountDownLatchBroadcaster for the specified countdown.
	 * @param count The countdown.
	 * @param serial The interval serial number
	 * @param timeOut The timeout of the countdown.
	 * @param unit The unit of the timeout.
	 * @param listeners An array of listeners to register.
	 */
	public CountDownLatchBroadcaster(int count, long serial, long timeOut, TimeUnit unit, CountDownLatchListener...listeners) {
		this(count, serial, listeners);
		this.timeOut = timeOut;
		this.unit = unit;
	}
	
	
	
	/**
	 * Callback from CountDownWatcherThread when countdown completes.
	 */
	public void complete() {
		for(CountDownLatchListener listener: listeners) {
			listener.countDownComplete(serial);
		}
	}
	
	/**
	 * Callback from CountDownWatcherThread when countdown timesout.
	 */
	public void timeout() {
		for(CountDownLatchListener listener: listeners) {
			listener.countDownTimedOut(serial);
		}
	}
	
	/**
	 * Callback from CountDownWatcherThread when countdown is interrupted.
	 */
	public void interrupted() {
		for(CountDownLatchListener listener: listeners) {
			listener.countDownInterrupted(serial);
		}
	}	
	

}

	
