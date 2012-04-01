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
package org.helios.ot.trace.interval;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.time.SystemClock;

/**
 * <p>Title: FlushProcessor</p>
 * <p>Description: The flush processor triggered by the accumulator's scheduler every flush period (end of the current interval)</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.interval.FlushProcessor</code></p>
 */
@JMXManagedObject(declared=true, annotated=false)
public class FlushProcessor implements Runnable {
	/** The accumulator map manager */
	protected final AccumulatorSwitch accSwitch;	
	/** The interval accumulator */
	protected final IntervalAccumulator ia;
	/** The interval serial number factory */
	protected final AtomicLong flushSerial = new AtomicLong(0);
	/** Instance logger */
	private final Logger log = Logger.getLogger(getClass());
	

	/**
	 * Creates a new FlushProcessor
	 * @param accSwitch The accumulator map manager
	 */
	public FlushProcessor(AccumulatorSwitch accSwitch, IntervalAccumulator ia) {
		super();
		this.accSwitch = accSwitch;
		this.ia = ia;
	}

	/**
	 * Executes the interval flush process 
	 */
	@Override
	public void run() {
		log.info("\n\t**************\n\tFlush Signal\n\tSerial#:" + System.identityHashCode(this) + "\n\t**************");
		long serial = flushSerial.incrementAndGet();
		ia.fireFlushSignalStart(serial);
		long currentTime = SystemClock.time();
		long start = System.nanoTime();
		accSwitch.switchChannel(currentTime);
		long elapsed = System.nanoTime() - start;		
		if(log.isDebugEnabled()) log.debug("AccSwitch Waited [" + elapsed + "] ns. [" + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS) + "] ms. for processing flush");
		ia.fireFlushSignalEnd(serial);
	}

}
 