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

import org.helios.ot.subtracer.pipeline.IPhaseTrigger;
import org.helios.ot.tracer.ITracer;

/**
 * <p>Title: ISubTracerProvider</p>
 * <p>Description: Defines a class that can provide subtracers. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.subtracer.ISubTracerProvider</code></p>
 */

public interface ISubTracerProvider {
	/**
	 * Returns a virtual tracer 
	 * @param host The virtual tracer's host
	 * @param agent The virtual tracer's agent name
	 * @return a virtual tracer
	 */
	public VirtualTracer getVirtualTracer(String host, String agent);
	
	/**
	 * Returns a temporal tracer that sets all traces generated to be temporal.
	 * @return a temporal tracer.
	 */
	public TemporalTracer getTemporalTracer();
	
	/**
	 * Creates an interval tracer 
	 * @return a temporal tracer.
	 */
	public IntervalTracer getIntervalTracer();
	
	/**
	 * Creates an urgent tracer 
	 * @return a temporal tracer.
	 */
	public UrgentTracer getUrgentTracer();
	
	/**
	 * Creates a phase trigger tracer
	 * @param triggers An array of phase triggers 
	 * @return a PhaseTriggerTracer 
	 */
	public PhaseTriggerTracer getPhaseTriggerTracer(IPhaseTrigger...triggers);
	

}
