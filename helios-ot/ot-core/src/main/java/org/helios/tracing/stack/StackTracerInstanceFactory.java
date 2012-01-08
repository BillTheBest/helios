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
package org.helios.tracing.stack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.tracing.AbstractTracerInstanceFactory;
import org.helios.tracing.ITracer;
import org.helios.tracing.ITracerInstanceFactory;
import org.helios.tracing.bridge.ITracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;
/**
 * <p>Title: StackTracerInstanceFactory</p>
 * <p>Description: ITracerInstanceFactory for the StackTracer. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.stack.StackTracerInstanceFactory</code></p>
 */
public class StackTracerInstanceFactory extends AbstractTracerInstanceFactory implements ITracingBridge, ITracerInstanceFactory {

	/** The tracer stack */
	protected Set<ITracerInstanceFactory> tracerStack = new CopyOnWriteArraySet<ITracerInstanceFactory>();
	
	/**
	 * Creates a configured stack tracer.
	 * @param tracerStack The collection of tracers that traces will be delegated to.	
	 */
	public StackTracerInstanceFactory(ArrayList<ITracerInstanceFactory> tracerStack) {
		super();
		tracerStack.remove(this);
		this.tracerStack.addAll(tracerStack);
	}
	
	/**
	 * Adds tracers to the stack
	 * @param tracers the ITracers to add
	 */
	public void addTracers(ITracerInstanceFactory...tracers) {
		if(tracers!=null) {
			for(ITracerInstanceFactory tracer: tracers) {
				
				if(tracer!=null && !tracerStack.contains(tracer) && !this.equals(tracer)) {
					tracerStack.add(tracer);
				}
			}
		}
	}
	
	/**
	 * Removes tracers from the stack
	 * @param tracers the ITracers to remove
	 */
	public void removeTracers(ITracerInstanceFactory...tracers) {
		if(tracers!=null) {
			for(ITracerInstanceFactory tracer: tracers) {
				if(tracer!=null) {
					tracerStack.remove(tracer);
				}
			}
		}
	}
	
	
	/**
	 * Adds tracers to the stack
	 * @param tracers a collection of ITracers to add
	 */
	public void addTracers(Collection<ITracerInstanceFactory> tracers) {
		if(tracers!=null) {
			for(ITracerInstanceFactory tracer: tracers) {
				if(tracer!=null && !tracerStack.contains(tracer) && !this.equals(tracer)) {
					tracerStack.add(tracer);
				}
			}
		}
	}
	
	/**
	 * Removes tracers from the stack
	 * @param tracers the ITracers to remove
	 */
	public void removeTracers(Collection<ITracerInstanceFactory> tracers) {
		if(tracers!=null) {
			tracerStack.removeAll(tracers);
		}
	}
	
	/**
	 * Returns the names of the tracers in the stack
	 * @return the names of the tracers in the stack
	 */
	@JMXAttribute (name="StackedTracers", description="The names of the tracers in the stack", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getStackedTracers() {
		Set<String> names = new HashSet<String>(tracerStack.size());
		for(ITracerInstanceFactory tracer: tracerStack) {
			names.add(tracer.getClass().getSimpleName());
		}
		return names.toArray(new String[names.size()]);
	}

	/**
	 * @return
	 */
	@Override
	public String getEndPointName() {
		return "StackTracer:" + Arrays.toString(getStackedTracers());
	}

	/**
	 * @return
	 */
	@Override
	public Executor getExecutor() {
		return this.executor;
	}

	/**
	 * @return
	 */
	@Override
	public boolean isIntervalCapable() {
		return true;
	}

	/**
	 * @return
	 */
	@Override
	public boolean isStateful() {
		return false;
	}

	/**
	 * @param traceIntervals
	 */
	@Override
	public void submitIntervalTraces(IIntervalTrace... traceIntervals) {
		for(ITracerInstanceFactory tracer: tracerStack) {			
			tracer.submitIntervalTraces(traceIntervals);
		}		
	}

	/**
	 * @param traces
	 */
	@Override
	public void submitTraces(Trace... traces) {
		for(ITracerInstanceFactory tracer: tracerStack) {			
			tracer.submitTraces(traces);
		}				
	}
	
	
	

	

}

