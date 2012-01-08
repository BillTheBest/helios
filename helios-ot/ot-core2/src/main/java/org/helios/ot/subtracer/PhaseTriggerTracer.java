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
package org.helios.ot.subtracer;

import javax.management.ObjectName;

import org.helios.helpers.ClassHelper;
import org.helios.ot.subtracer.pipeline.IPhaseTrigger;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.subtracer.pipeline.Phase.KeyedPhaseTrigger;
import org.helios.ot.trace.Trace.Builder;
import org.helios.ot.tracer.ITracer;

/**
 * <p>Title: PhaseTriggerTracer</p>
 * <p>Description: A tracer that provides a means of injecting runnables that will be invoked along the open trace operation pipe.
 * Implemented execution points are: <ol>
 * 	<li>When a Trace is applied to it's aggregate interval (meaning is was processed from the interval submission queue)</li>
 * </ol>
 * <p><b>THIS IS FOR TESTING ONLY. PERFORMANCE WILL DECREASE</b>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.subtracer.PhaseTriggerTracer</code></p>
 */

public class PhaseTriggerTracer extends DelegatingTracer {
	/** The phase triggers to fire for each trace */
	protected KeyedPhaseTrigger[] phaseTriggers = {};

	/**
	 * Returns the PhaseTriggerTracer appropriate to be a subtracer for the passed ITracer.
	 * @param innerTracer The inner tracer
	 * @param triggers triggers An array of IPhaseTriggers
	 * @return an PhaseTriggerTracer
	 */
	public static PhaseTriggerTracer getInstance(ITracer innerTracer, IPhaseTrigger...triggers) {		
		ClassHelper.nvl(innerTracer, "Passed innerTracer was null");
		ObjectName on = createObjectName(PhaseTriggerTracer.class, innerTracer);
		PhaseTriggerTracer tracer = (PhaseTriggerTracer) SUB_TRACERS.get(on);
		if(tracer==null) {
			tracer = (PhaseTriggerTracer) SUB_TRACERS.get(on);
			if(tracer==null) {
				tracer = new PhaseTriggerTracer(innerTracer, PhaseTriggerTracer.class.getSimpleName(), on, triggers);
				SUB_TRACERS.put(on, tracer);
			}
		} else {			
			tracer.setTriggers(triggers);
		}
		
		return tracer;
	}
	
	/**
	 * Clears the current triggers and replaces them with the passed triggers.
	 * @param triggers An array of IPhaseTriggers.
	 */
	protected void setTriggers(IPhaseTrigger...triggers) {
		clearTriggers();
		if(triggers!=null) {
			phaseTriggers = Phase.createPhaseTriggersFor(triggers);
		}
	}

	/**
	 * Clears all the phase triggers from this instance.
	 */
	public void clearTriggers() {
		phaseTriggers = new KeyedPhaseTrigger[0];  
	}

	
	/**
	 * Creates a new PhaseTriggerTracer
	 * @param vtracer The parent tracer
	 * @param tracerName The tracer name
	 * @param tracerObjectName The tracer ObjectName
	 * @param triggers An array of IPhaseTriggers
	 */
	public PhaseTriggerTracer(ITracer vtracer, String tracerName, ObjectName tracerObjectName, IPhaseTrigger...triggers) {
		super(vtracer, tracerName, tracerObjectName);		
		if(triggers!=null) {
			phaseTriggers = Phase.createPhaseTriggersFor(triggers);
		}

	}

	/**
	 * Adds the runnables to the trace stack
	 * @param builder The builder
	 * @return the Builder
	 */
	@Override
	public Builder subformat(Builder builder) {		
		sendCounter.incrementAndGet();
		builder = builder.addPhaseTriggers(phaseTriggers);
		return builder;
	}

}
