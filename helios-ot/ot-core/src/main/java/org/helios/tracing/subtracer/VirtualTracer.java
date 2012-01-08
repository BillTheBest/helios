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
package org.helios.tracing.subtracer;

import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.tracing.ITracer;
import org.helios.tracing.trace.Trace.Builder;

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
public class VirtualTracer extends DelegatingTracer implements IVirtualTracer  {
	/**  */
	private static final long serialVersionUID = 7743099314180848943L;
	/** The host name for which this tracer is tracing */
	protected final String vhost;
	/** The agent name for which this tracer is tracing */
	protected final String vagent;
	
	
	
	/**
	 * Creates a new VirtualTracer
	 * @param vhost The host name for which this tracer is tracing
	 * @param vagent The agent name for which this tracer is tracing
	 * @param vtracer The inner wrapped concrete tracer
	 */	
	public VirtualTracer(String vhost, String vagent, ITracer vtracer) {
		super(vtracer, "VirtualTracer->" + vhost + ":" + vagent + "->" + vtracer.getTracerName());
		this.vhost = vhost;
		this.vagent = vagent; 
		this.tracerObjectName = JMXHelper.objectName(getClass().getPackage().getName(), "type", getClass().getSimpleName(), "host", vhost, "agent", vagent);
		try {
			this.reflectObject(this);
			JMXHelper.getHeliosMBeanServer().registerMBean(this, this.tracerObjectName);
		} catch (Exception e) {
			log.warn("Failed to register VirtualTracer Managemenht Interface for [" + this.tracerObjectName + "]", e);
		}
	}

	/**
	 * Returns the agent name for which this tracer is tracing
	 * @return the agent name
	 * @see org.helios.tracing.subtracer.IVirtualTracer#getVirtualAgent()
	 */
	@JMXAttribute(name="VirtualAgent", description="The agent name for which this tracer is tracing", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getVirtualAgent() {
			return vagent;
	}

	/**
	 * Returns the host name for which this tracer is tracing
	 * @return the host name 
	 * @see org.helios.tracing.subtracer.IVirtualTracer#getVirtualHost()
	 */
	@JMXAttribute(name="VirtualAgent", description="The host name for which this tracer is tracing", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getVirtualHost() {
		return vhost;
	}


	/**
	 * Builder interceptor to virtualize the trace being built by the builder.
	 * @param builder The instance of the builder building a trace to be virtualized.
	 * @return The builder instance.
	 * @see org.helios.tracing.TracerImpl#format(org.helios.tracing.trace.Trace.Builder)
	 */
	@Override
	public Builder format(Builder builder) {		
		return vtracer.format(builder).virtual(this);
	}

	

}
