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

import java.util.concurrent.TimeUnit;

/**
 * <p>Title: CountDownLatchWatcherThread</p>
 * <p>Description: A thread spawned to watch a countdown latch and notify listeners when the latch counts down, times out or is interrupted.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1058 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/util/CountDownWatcherThread.java $
 * $Id: CountDownWatcherThread.java 1058 2009-02-18 17:33:54Z nwhitehead $
 */
public class CountDownWatcherThread extends Thread {
	protected CountDownLatchBroadcaster latch = null;
	protected long timeOut = -1L;
	protected TimeUnit unit = null;
	
	/**
	 * Creates a new CountDownWatcherThread with a timeout.
	 * @param latch The latch to await on.
	 * @param timeOut The timeout
	 * @param unit The unit of the timeout.
	 * @param serial The serial number of the latch.
	 */
	public CountDownWatcherThread(CountDownLatchBroadcaster latch, long timeOut,
			TimeUnit unit, long serial) {
		this(latch, serial);
		this.timeOut = timeOut;
		this.unit = unit;
	}
	
	/**
	 * Creates a new CountDownWatcherThread with no timeout.
	 * @param latch The latch to await on.
	 * @param serial The serial number of the latch.
	 */
	public CountDownWatcherThread(CountDownLatchBroadcaster latch, long serial) {		
		super();
		this.setDaemon(true);
		this.setName("CountDownWatcherThread#" + serial);		
		this.latch = latch;
	}
	
	/**
	 * Awaits on the latch and notifies listeners when it counts down, times out or is interrupted.
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		if(timeOut==-1L) {			
			try {
				latch.await();
				latch.complete();
			} catch (InterruptedException e) {
				latch.interrupted();
			}
		} else {
			try {
				if(latch.await(timeOut, unit)) {
					latch.complete();
				} else {
					latch.timeout();
				}				
			} catch (InterruptedException e) {
				latch.interrupted();
			}			
		}
	}
}
