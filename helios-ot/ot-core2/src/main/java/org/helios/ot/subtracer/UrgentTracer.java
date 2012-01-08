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
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.ot.trace.Trace.Builder;
import org.helios.ot.tracer.ITracer;

/**
 * <p>Title: UrgentTracer</p>
 * <p>Description: A Subtracer that marks all traces as urgent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.subtracer.UrgentTracer</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public class UrgentTracer extends DelegatingTracer {
	
	
	/**
	 * Returns the UrgentTracer appropriate to be a subtracer for the passed ITracer.
	 * @param innerTracer The inner tracer
	 * @return an UrgentTracer
	 * @ To Do: This should genericizable 
	 */
	public static UrgentTracer getInstance(ITracer innerTracer) {		
		ClassHelper.nvl(innerTracer, "Passed vtracer was null");
		ObjectName on = createObjectName(UrgentTracer.class, innerTracer);
		UrgentTracer tracer = (UrgentTracer) SUB_TRACERS.get(on);
		if(tracer==null) {
			tracer = (UrgentTracer) SUB_TRACERS.get(on);
			if(tracer==null) {
				tracer = new UrgentTracer(innerTracer, UrgentTracer.class.getSimpleName(), on);
				SUB_TRACERS.put(on, tracer);
			}
		}
		return tracer;
	}	

	/**
	 * Creates a new UrgentTracer
	 * @param vtracer The inner tracer
	 * @param tracerName The tracer name
	 * @param tracerObjectName The tracer ObjectName 
	 */
	private UrgentTracer(ITracer vtracer, String tracerName, ObjectName tracerObjectName) {
		super(vtracer, tracerName, tracerObjectName);
	}
	
	

	/**
	 * Sets the trace as urgent
	 * @param builder the trace builder
	 * @return the trace builder
	 */
	@Override
	public Builder subformat(Builder builder) {
		return builder.urgent();
	}


}
