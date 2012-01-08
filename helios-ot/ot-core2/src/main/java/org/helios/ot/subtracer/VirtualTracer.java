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
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.trace.MetricId;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.Trace.Builder;
import org.helios.ot.tracer.ITracer;

/**
 * <p>Title: VirtualTracer</p>
 * <p>Description: A decorated wrapper for a concrete top tracer that allows the host name and agent Id to be overriden.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject (declared=false, annotated=true)
public class VirtualTracer extends DelegatingTracer  {
	/**  */
	private static final long serialVersionUID = 7743099314180848943L;
	/** The host name for which this tracer is tracing */
	protected final String vhost;
	/** The agent name for which this tracer is tracing */
	protected final String vagent;
	
	
	
	/**
	 * Returns the VirtualTracer appropriate to be a subtracer for the passed ITracer.
	 * @param innerTracer The inner tracer
	 * @param vhost The host name for which this tracer is tracing
	 * @param vagent The agent name for which this tracer is tracing
	 * @return an VirtualTracer
	 * @ To Do: This should genericizable 
	 */
	public static VirtualTracer getInstance(ITracer innerTracer, String vhost, String vagent) {		
		ClassHelper.nvl(innerTracer, "Passed vtracer was null");
		ObjectName on = createObjectName(VirtualTracer.class, innerTracer, "vhost=" + vhost, "vagent=" + vagent);
		VirtualTracer tracer = (VirtualTracer) SUB_TRACERS.get(on);
		if(tracer==null) {
			tracer = (VirtualTracer) SUB_TRACERS.get(on);
			if(tracer==null) {
				tracer = new VirtualTracer(innerTracer, VirtualTracer.class.getSimpleName(), on, vhost, vagent);
				SUB_TRACERS.put(on, tracer);
			}
		}
		return tracer;
	}		
	
	
	/**
	 * Creates a new VirtualTracer
	 * @param vtracer The inner tracer
	 * @param tracerName The tracer name
	 * @param tracerObjectName The tracer ObjectName 
	 * @param vhost The host name for which this tracer is tracing
	 * @param vagent The agent name for which this tracer is tracing
	 */	
	private VirtualTracer(ITracer vtracer, String tracerName, ObjectName tracerObjectName, String vhost, String vagent) {
		super(vtracer, tracerName, tracerObjectName);
		this.vhost = vhost;
		this.vagent = vagent; 
	}
	
	
	
	
	
//	/**
//	 * Returns the tracer name.
//	 * @return the tracer name.
//	 */
//	@Override  // overriden so we can add the vhost and vagent to the tracer name
//	@JMXAttribute (name="DefaultName", description="The default name of this tracer", mutability=AttributeMutabilityOption.READ_ONLY)
//	public String getDefaultName() {
//		return new StringBuilder(vtracer.getDefaultName()).append("-->").append(getClass().getSimpleName()).append("(").append(vhost).append(",").append(vagent).append(")").toString();
//	}

	@Override
	public String getTracerName() {
		return new StringBuilder(getClass().getSimpleName()).append("(").append(vhost).append(",").append("vagent").append(")").toString();
	}


	/**
	 * Returns the agent name for which this tracer is tracing
	 * @return the agent name
	 * @see org.helios.tracing.subtracer.IVirtualTracer#getVirtualAgent()
	 */
	@JMXAttribute (name="VirtualAgent", description="The agent name for which this tracer is tracing", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getVirtualAgent() {
			return vagent;
	}

	/**
	 * Returns the host name for which this tracer is tracing
	 * @return the host name 
	 * @see org.helios.tracing.subtracer.IVirtualTracer#getVirtualHost()
	 */
	@JMXAttribute (name="VirtualHost", description="The host name for which this tracer is tracing", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getVirtualHost() {
		return vhost;
	}

	
	/**
	 * Overrides the builder defined host and agent to this virtual tracer's host and agent
	 * @param builder The builder to modify
	 * @return the modified builder
	 */
	@Override
	public Builder subformat(Builder builder) {
		return builder.virtualize(this);
	}


	/**
	 * Builds a metric name from the passed fragments
	 * @param point The metric point
	 * @param nameSpace The metric name space
	 * @return The fully qualified metric name
	 */
	@Override
	public String buildMetricName(CharSequence point, CharSequence...nameSpace) {
		return buildMetricName(point, null, nameSpace);
	}
	
	/**
	 * Builds a metric name from the passed fragments
	 * @param point The metric point
	 * @param prefix Prefixes for the namespace
	 * @param nameSpace The metric name space
	 * @return The fully qualified metric name
	 */
	@Override
	public String buildMetricName(CharSequence point, CharSequence[] prefix, CharSequence...nameSpace) {
		StringBuilder b = new StringBuilder();
		b.append(vhost).append(Trace.DELIM).append(vagent).append(Trace.DELIM);
		if(prefix!=null) {
			for(CharSequence ns: prefix) {
				if(ns!=null) {
					String s = ns.toString().trim();
					if(!"".equals(s)) {
						b.append(s).append(Trace.DELIM);
					}
				}
			}
		}		
		if(nameSpace!=null) {
			for(CharSequence ns: nameSpace) {
				if(ns!=null) {
					String s = ns.toString().trim();
					if(!"".equals(s)) {
						b.append(s).append(Trace.DELIM);
					}
				}
			}
		}
		if(point!=null) {
			String s = point.toString().toString();
			if(!"".equals(s)) {
				b.append(s).append(s).append(Trace.DELIM);
			}
		}
		return b.toString().intern();		
	}

	

}
