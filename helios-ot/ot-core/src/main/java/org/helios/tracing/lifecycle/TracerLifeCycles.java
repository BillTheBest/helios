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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.tracing.TracerFactory;

/**
 * <p>Title: TracerLifeCycles</p>
 * <p>Description: A managed collection of TracerLifeCycle objects.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1647 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/lifecycle/TracerLifeCycles.java $
 * $Id: TracerLifeCycles.java 1647 2009-10-24 21:52:31Z nwhitehead $
 */
@JMXManagedObject (declared=false, annotated=true)
public class TracerLifeCycles {
	protected static final Map<String, TracerLifeCycle> lifecycles  = new ConcurrentHashMap<String, TracerLifeCycle>();

	
			
	/**
	 * Increments the constructors for the passed tracer name.
	 * @param tracerName The name of the tracer to increment.
	 */
	public static void incrementConstructors(String tracerName) {
		TracerLifeCycle tlc = getTLC(tracerName);
		tlc.inrementConstructs();
	}
	
	/**
	 * Increments the resets for the passed tracer name.
	 * @param tracerName The name of the tracer to increment.
	 */
	public static void incrementResets(String tracerName) {
		TracerLifeCycle tlc = getTLC(tracerName);
		tlc.incrementResets();
	}	
	
	/**
	 * Increments the finalizers for the passed tracer name.
	 * @param tracerName The name of the tracer to increment.
	 */
	public static void incrementFinalizers(String tracerName) {
		TracerLifeCycle tlc = getTLC(tracerName);
		tlc.incrementFinalizers();
	}	
	
	/**
	 * Acquires (creating if necessary) the named TracerLifeCycle.
	 * @param tracerName The tracer name.
	 * @return A TracerLifeCycle
	 */
	protected static TracerLifeCycle getTLC(String tracerName) {
		TracerLifeCycle tlc = lifecycles.get(tracerName);
		if(tlc == null) {
			tlc = new TracerLifeCycle(tracerName);	
			TracerFactory.getInstance().reflectObject(tlc);
			lifecycles.put(tracerName, tlc);
		}
		return tlc;
	}
	
	/**
	 * Returns the named TracerLifeCycle or null if it is not found.
	 * @param tracerName The tracer name.
	 * @return A TracerLifeCycle or null.
	 */
	public static TracerLifeCycle getTracerLifeCycle(String tracerName) {
		return lifecycles.get(tracerName);
	}
	
	@JMXOperation (name="reportLifeCycles", description="Reports tracer lifecycle stats.")
	public static String reportLifeCycles() {
		StringBuilder buff = new StringBuilder("Tracer Lifecycles");
		for(TracerLifeCycle tlc: lifecycles.values()) {
			buff.append("\n\t").append(tlc.toString());
		}
		buff.append("\n");
		return buff.toString();
	}
	
}
