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
package org.helios.tracing.nulltracer;

import java.util.concurrent.Executor;

import org.helios.tracing.AbstractTracerInstanceFactory;
import org.helios.tracing.ITracerInstanceFactory;
import org.helios.tracing.bridge.ITracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: NullTracerInstanceFactory</p>
 * <p>Description: Factory for NullTracer</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 * org.helios.tracing.nulltracer.NullTracerInstanceFactory
 */
public class NullTracerInstanceFactory extends AbstractTracerInstanceFactory implements  ITracingBridge, ITracerInstanceFactory{

	/**
	 * Creates a new NullTracerInstanceFactory.
	 */
	public NullTracerInstanceFactory() {
		super();
	}

	/**
	 * @return
	 */
	@Override
	public String getEndPointName() {
		return "Null";
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
		return false;
	}

	/**
	 * @return
	 */
	@Override
	public boolean isStateful() {
		return false;
	}

	/**
	 * No Op
	 * @param intervalTraces
	 */
	@Override
	public void submitIntervalTraces(IIntervalTrace... traceIntervals) {
	}

	/**
	 * No Op
	 * @param traces
	 */
	@Override
	public void submitTraces(Trace... traces) {
	}


}
