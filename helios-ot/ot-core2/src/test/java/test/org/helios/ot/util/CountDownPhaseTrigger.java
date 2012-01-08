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
package test.org.helios.ot.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.ot.subtracer.pipeline.IPhaseAwarePhaseTrigger;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.trace.Trace;

/**
 * <p>Title: CountDownPhaseTrigger</p>
 * <p>Description: A phase trigger to acquiring traces asynchronosuly dispatched downstream. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.util.CountDownPhaseTrigger</code></p>
 */

public class CountDownPhaseTrigger implements IPhaseAwarePhaseTrigger {
	final AtomicReference<CountDownLatch> latch;
	Phase[] phases;
	Trace[] it = new Trace[1];
	
	/**
	 * Creates a new CountDownPhaseTrigger
	 * @param latch the latch to countdown
	 * @param phases The phases this trigger should fire on
	 */
	public CountDownPhaseTrigger(AtomicReference<CountDownLatch> latch, Phase...phases) {			
		this.latch = latch;
		this.phases = phases==null ? new Phase[0] : phases;
	}


	/**
	 * @return the latch
	 */
	public CountDownLatch getLatch() {
		return latch.get();
	}

	/**
	 * @param latch the latch to set
	 */
	public void setLatch(CountDownLatch latch) {
		this.latch.set(latch);
	}

	@Override
	public void phaseTrigger(String phaseName, Trace trace) {
//		long cnt = latch.get().getCount();
		latch.get().countDown();
//		System.out.println("Latch Count[" + trace.getFQN() + "]: Before:" + cnt + "  After:" + latch.get().getCount() + 
//					"\n\tLatch ID[" + System.identityHashCode(latch.get()) + "]" + 
//					"\n\tTrigger ID[" + System.identityHashCode(this) + "]" + 
//					"\n\tTrace:[" + trace.getClass().getSimpleName() + "]--[" + trace.getFQN() + "]" 
//					
//		);

		it[0] = trace;
	}
	
	public Trace getFlushedTrace() {
		Trace  trace = it[0];
		it[0] = null;
		return trace;
	}


	@Override
	public boolean isAsynch() {
		return false;
	}


	@Override
	public Phase[] phases() {
		return phases;
	}
}
