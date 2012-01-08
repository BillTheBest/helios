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
package org.helios.esper.tracing;


import java.util.concurrent.Executor;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.tracing.AbstractTracerInstanceFactory;
import org.helios.tracing.bridge.ITracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventSender;

/**
 * <p>Title: EsperTracerInstanceFactory</p>
 * <p>Description: </p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.tracing.EsperTracerInstanceFactory</code></p>
 */

public class EsperTracerInstanceFactory extends AbstractTracerInstanceFactory implements ITracingBridge {
	/** The singleton ctor lock */
	protected static final Object lock = new Object();
	/** the esper service provider */
	protected EPServiceProvider esperProvider = null;
	
	/** the esper event sender */
	protected EventSender eventSender = null;
	
//	EsperTracer(EPServiceProvider serviceProvider) {
//		super();		
//		esperProvider = serviceProvider;
//		try { esperProvider.getEPAdministrator().getConfiguration().addEventTypeAutoName(Trace.class.getPackage().getName()); } catch (Exception e) {}
//		try { esperProvider.getEPAdministrator().getConfiguration().addEventType(Trace.class); } catch (Exception e) {}
//		try { esperProvider.getEPAdministrator().getConfiguration().addEventType(Trace.class.getName(), Trace.class.getName()); } catch (Exception e) {}		
//		try { esperProvider.getEPAdministrator().getConfiguration().addEventType("Metric", Trace.class.getName()); } catch (Exception e) {}
//		eventSender = esperProvider.getEPRuntime().getEventSender(Trace.class.getName());
//		TracerFactory.getInstance().registerTracer(getTracerName(), this);
//		jmxSingletonRegister(this);
//	}

	
//	/**
//	 * Traces a metric to Esper.
//	 * @param trace the trace
//	 * @return always null
//	 * @see org.helios.tracing.AbstractTracer#traceTrace(org.helios.tracing.Trace)
//	 */
//	@Override
//	@JMXOperation(name="traceTrace", description="Traces a metric to Esper")
//	public Trace traceTrace(@JMXParameter(name="trace", description="The trace to send to Esper") Trace trace) {
//		eventSender.sendEvent(trace);
//		sendCounter.incrementAndGet();
//		return null;
//	}
	
	
	/**
	 * Constructs a new EsperTracerInstanceFactory
	 * @param esperProvider the esper service provider
	 */
	public EsperTracerInstanceFactory(EPServiceProvider esperProvider) {
		super();
		this.esperProvider = esperProvider;
		
	}

	/**
	 * Constructs a new EsperTracerInstanceFactory
	 */
	public EsperTracerInstanceFactory() {
		super();
	}
	




	/**
	 * Returns the esper service provider
	 * @return the esper service provider
	 */
	@JMXAttribute (name="EsperProvider", description="The Esper Service Provider", mutability=AttributeMutabilityOption.READ_WRITE)
	public EPServiceProvider getEsperProvider() {
		return esperProvider;
	}

	/**
	 * Sets the esper service provider
	 * @param esperProvider the esper service provider
	 */
	public void setEsperProvider(EPServiceProvider esperProvider) {
		this.esperProvider = esperProvider;
	}

	/* (non-Javadoc)
	 * @see org.helios.tracing.bridge.ITracingBridge#getEndPointName()
	 */
	@Override
	public String getEndPointName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.helios.tracing.bridge.ITracingBridge#getExecutor()
	 */
	@Override
	public Executor getExecutor() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.helios.tracing.bridge.ITracingBridge#isIntervalCapable()
	 */
	@Override
	public boolean isIntervalCapable() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.helios.tracing.bridge.ITracingBridge#isStateful()
	 */
	@Override
	public boolean isStateful() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.helios.tracing.bridge.ITracingBridge#submitIntervalTraces(org.helios.tracing.interval.IIntervalTrace[])
	 */
	@Override
	public void submitIntervalTraces(IIntervalTrace... traceIntervals) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.helios.tracing.bridge.ITracingBridge#submitTraces(org.helios.tracing.trace.Trace[])
	 */
	@Override
	public void submitTraces(Trace... traces) {
		// TODO Auto-generated method stub
		
	}

}
