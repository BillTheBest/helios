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
package org.helios.ot.helios;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.agent.HeliosOTClient;
import org.helios.ot.agent.HeliosOTClientFactory;
import org.helios.ot.endpoint.AbstractEndpoint;
import org.helios.ot.endpoint.EndpointConnectException;
import org.helios.ot.endpoint.EndpointTraceException;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.helios.time.SystemClock;
import org.helios.version.VersionHelper;

/**
 * <p>Title: HeliosEndpoint</p>
 * <p>Description: OpenTrace endpoint optimized for the Helios OT Server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.HeliosEndpoint</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public class HeliosEndpoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T> implements UncaughtExceptionHandler {
	/** The helios OT agent */
	protected HeliosOTClient otAgent = null;
	
	/** The count of exceptions */
	protected final AtomicLong exceptionCount = new AtomicLong(0);
	
	
	/**  */
	private static final long serialVersionUID = -433677190518825263L;
	/** The last elapsed message */
	protected String lastElapsed = null;
	
	
	/**
	 * Creates a new HeliosEndpoint from system properties and an optional external XML file
	 */
	public HeliosEndpoint() {
		// Read the basic config
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#connectImpl()
	 */
	@Override
	protected void connectImpl() throws EndpointConnectException {
		try {
			otAgent = HeliosOTClientFactory.newInstance();
			otAgent.connect(false);
		} catch (Exception e) {
			throw new EndpointConnectException("HeliosEndpoint failed to connect:" + e);			
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#disconnectImpl()
	 */
	@Override
	protected void disconnectImpl() {
		otAgent.disconnect();
		otAgent = null;
	}
	
	
		
	
	protected static String banner() {
		return "Helios OpenTrace Agent [" + VersionHelper.getHeliosVersion(HeliosEndpoint.class) + "]";
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error("Uncaught exception on thread [" + t + "]", e);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#newBuilder()
	 */
	@Override
	public org.helios.ot.endpoint.AbstractEndpoint.Builder newBuilder() {
		return null;
	}

	/**
	 * Returns the system clock measurement of the last submission elapsed time
	 * @return the system clock measurement of the last submission elapsed time
	 */
	@JMXAttribute(name="LastElapsed", description="The system clock measurement of the last submission elapsed time", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getLastElapsed() {
		return lastElapsed;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#processTracesImpl(org.helios.ot.tracer.disruptor.TraceCollection)
	 */
	@Override
	protected boolean processTracesImpl(TraceCollection<T> traceCollection) throws EndpointConnectException, EndpointTraceException {
		SystemClock.startTimer();
		if(isConnected()) {
			otAgent.submitTraces(traceCollection.getTraces().toArray(new Trace[0]));
			lastElapsed = SystemClock.endTimer().toString();
			return true;
		} 
		return false;
	}
	

	
	

	/**
	 * Returns the helios OT server host name or ip address
	 * @return the host
	 */
	@JMXAttribute(name="Host", description="The helios OT server host name or ip address", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getHost() {
		return "";
	}

	/**
	 * Returns the helios OT server listening port
	 * @return the port
	 */
	@JMXAttribute(name="Port", description="The helios OT server listening port", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getPort() {
		return -1;
	}

	/**
	 * Returns the helios OT server comm protocol
	 * @return the protocol
	 */
	@JMXAttribute(name="Protocol", description="The helios OT server comm protocol", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getProtocol() {
		return "";
	}

	/**
	 * Returns the cumulative exception count
	 * @return the exceptionCount
	 */
	@JMXAttribute(name="ExceptionCount", description="The cumulative exception count", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getExceptionCount() {
		return exceptionCount.get();
	}
	
	



}
