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
package org.helios.jmx.threadservices;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Title: ExtendedSheduledFuture</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ExtendedSheduledFuture implements ScheduledFuture {

	/**
	 * 
	 */
	public ExtendedSheduledFuture() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	public long getDelay(TimeUnit arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Delayed arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	public boolean cancel(boolean arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @see java.util.concurrent.Future#get()
	 */
	public Object get() throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	public Object get(long arg0, TimeUnit arg1) throws InterruptedException,
			ExecutionException, TimeoutException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @return
	 * @see java.util.concurrent.Future#isDone()
	 */
	public boolean isDone() {
		// TODO Auto-generated method stub
		return false;
	}

}
