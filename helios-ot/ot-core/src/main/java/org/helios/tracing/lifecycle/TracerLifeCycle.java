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
package org.helios.tracing.lifecycle;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: TracerLifeCycle</p>
 * <p>Description: A container class for tracking lifecycle events of tracers.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1058 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/lifecycle/TracerLifeCycle.java $
 * $Id: TracerLifeCycle.java 1058 2009-02-18 17:33:54Z nwhitehead $
 */
@JMXManagedObject (declared=false, annotated=true)
public class TracerLifeCycle {
	protected String tracerName = null;
	protected long constructs = 0;
	protected long resets = 0;
	protected long finalizers = 0;
	
	/**
	 * Creates a new TracerLifeCycle
	 * @param tracerName
	 */
	public TracerLifeCycle(String tracerName) {
		super();
		this.tracerName = tracerName;
	}

	/**
	 * Returns the tracer name.
	 * @return the tracerName
	 */
	public String getTracerName() {
		return tracerName;
	}
	
	public String getCTracerName() {
		return tracerName + "Constructors";
	}
	public String getFTracerName() {
		return tracerName + "Finalizers";
	}
	public String getRTracerName() {
		return tracerName + "Resets";
	}
	

	/**
	 * Returns the number of constructor calls.
	 * @return the constructs
	 */
	@JMXAttribute (name="getCTracerName", introspectName=true,  description="Returns the number of constructor calls.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getConstructs() {
		return constructs;
	}

	/**
	 * Increments the number of constructor calls.
	 */
	public void inrementConstructs() {
		constructs++;
	}

	/**
	 * Returns the number of times the tracer singleton has been reset.
	 * @return the resets
	 */
	@JMXAttribute (name="getRTracerName", introspectName=true, description="Returns the number of reset calls.", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getResets() {
		return resets;
	}

	/**
	 * Increments the number of resets.
	 */
	public void incrementResets() {
		resets++;
	}
	/**
	 * Returns the number of tracer finalizers.
	 * @return the finalizers
	 */
	@JMXAttribute (name="getFTracerName", introspectName=true, description="Returns the number of finalizer calls.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getFinalizers() {
		return finalizers;
	}
	
	/**
	 * Increments the number of finalizers.
	 */
	public void incrementFinalizers() {
		finalizers++;
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((tracerName == null) ? 0 : tracerName.hashCode());
		return result;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TracerLifeCycle other = (TracerLifeCycle) obj;
		if (tracerName == null) {
			if (other.tracerName != null)
				return false;
		} else if (!tracerName.equals(other.tracerName))
			return false;
		return true;
	}

	/**
	 * Constructs a <code>String</code> displaying the tracer name and constructs/resets/finalizers.
	 * @return a <code>String</code> representation 
	 */
	public String toString() {
	    StringBuilder retValue = new StringBuilder("Lifecycle[");
	    retValue.append(tracerName);
	    retValue.append("]");
	    retValue.append(constructs).append("/").append(resets).append("/").append(finalizers);
	    return retValue.toString();
	}
	
	
}
