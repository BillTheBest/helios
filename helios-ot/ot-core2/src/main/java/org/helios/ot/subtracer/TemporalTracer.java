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
 * <p>Title: TemporalTracer</p>
 * <p>Description: A subtracer that marks all built traces as temporal. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.subtracer.TemporalTracer</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public class TemporalTracer extends DelegatingTracer {
	
	/**
	 * Returns the TemporalTracer appropriate to be a subtracer for the passed ITracer.
	 * @param innerTracer The inner tracer
	 * @return an TemporalTracer
	 * @ To Do: This should genericizable 
	 */
	public static TemporalTracer getInstance(ITracer innerTracer) {		
		ClassHelper.nvl(innerTracer, "Passed vtracer was null");
		ObjectName on = createObjectName(TemporalTracer.class, innerTracer);
		TemporalTracer tracer = (TemporalTracer) SUB_TRACERS.get(on);
		if(tracer==null) {
			tracer = (TemporalTracer) SUB_TRACERS.get(on);
			if(tracer==null) {
				tracer = new TemporalTracer(innerTracer, TemporalTracer.class.getSimpleName(), on);
				SUB_TRACERS.put(on, tracer);
			}
		}
		return tracer;
	}
	

	/**
	 * Creates a new TemporalTracer
	 * @param vtracer The parent tracer
	 * @param tracerName The tracer name
	 * @param tracerObjectName The tracer ObjectName 
	 */
	private TemporalTracer(ITracer vtracer, String tracerName, ObjectName tracerObjectName) {
		super(vtracer, tracerName, tracerObjectName);
	}
	
	
	/**
	 * Marks the trace to be built as temporal
	 * @param builder The builder to mark
	 * @return the modified builder
	 */
	@Override
	public Builder subformat(Builder builder) {
		return builder.temporal();
	}


}
