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
package org.helios.spring.container;

import java.util.concurrent.CountDownLatch;

/**
 * <p>Title: ContainerNonDaemonThread</p>
 * <p>Description: A non daemon thread intended to keep the JVM resident in the absence of any other non-daemon threads.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ContainerNonDaemonThread extends Thread {
	protected CountDownLatch latch = new CountDownLatch(1);
	
	
	public void halt() {
		latch.countDown();
	}
	
	public ContainerNonDaemonThread() {
		super();
		setName("HeliosContainerNonDaemonThread");
		setDaemon(false);		
	}
	
	public void run() {
		int interrupts = 0;
		while(true) {
			try {
				latch.await();
				break;
			} catch (InterruptedException e) {
				interrupts++;
				System.err.println("\n\t!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n\tContainerNonDaemonThread was interrupted.[" + interrupts + "]\n\t!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
				interrupted();
				latch = new CountDownLatch(1);
			}
		}
	}
}
