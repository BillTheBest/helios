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
package org.helios.ot.endpoint;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.ot.instrumentation.InstrumentationProfile;
import org.helios.ot.instrumentation.InvocationObserver;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: InstrumentedEndPointWrapper</p>
 * <p>Description: Wraps an endpoint's {@link IEndPoint#processTraces(TraceCollection)} method with an instrumenting invocation observer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.endpoint.InstrumentedEndPointWrapper</code></p>
 */
@JMXManagedObject (declared=true, annotated=true)
public class InstrumentedEndPointWrapper<T extends Trace<? extends ITraceValue>> extends ManagedObjectDynamicMBean implements LifecycleAwareIEndPoint<T> {
	/** The wrapped end point */
	protected final IEndPoint<T> wrappedEndPoint;
	/** The instrumentation profile */
	protected final InstrumentationProfile profile;
	/** the invocation observer */
	protected final InvocationObserver invocationObserver;
	/** instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	/**
	 * Creates a new InstrumentedEndPointWrapper
	 * @param wrappedEndPoint The wrapped end point
	 * @param profile The instrumentation profile
	 * @param rollingCounterSize The size of the rolling counters
	 */
	public InstrumentedEndPointWrapper(IEndPoint<T> wrappedEndPoint, InstrumentationProfile profile, int rollingCounterSize) {
		this.wrappedEndPoint = wrappedEndPoint;
		this.profile = profile;
		invocationObserver = profile.getInvocationObserver(wrappedEndPoint.getClass().getSimpleName(), rollingCounterSize);
		reflectObject(this);
		reflectObject(invocationObserver);
		if(wrappedEndPoint instanceof AbstractEndpoint) {
			objectName = JMXHelper.objectName(((AbstractEndpoint)wrappedEndPoint).getObjectName().toString() + ",instrumented=" + profile.name());
		} else {
			objectName = JMXHelper.objectName(getClass().getPackage().getName(), "service", "endpoint", "type", wrappedEndPoint.getClass().getSimpleName(), "instrumented", profile.name());
		}
		//JMXHelper.getRuntimeHeliosMBeanServer().registerMBean(this, objectName);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>Wraps the traceCollection call in the instrumentation profile's invocation observer</p>
	 * @see org.helios.ot.endpoint.IEndPoint#processTraces(org.helios.ot.tracer.disruptor.TraceCollection)
	 */
	public void processTraces(TraceCollection<T> traceCollection) throws Exception {
		invocationObserver.start();
		try {
			wrappedEndPoint.processTraces(traceCollection);
			invocationObserver.stop();
		} catch (Exception e) {
			invocationObserver.exception();
			throw e;
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.IEndPoint#connect()
	 */
	public boolean connect() {
		return wrappedEndPoint.connect();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.IEndPoint#disconnect()
	 */
	public void disconnect() {
		wrappedEndPoint.disconnect();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.IEndPoint#isConnected()
	 */
	public boolean isConnected() {
		return wrappedEndPoint.isConnected();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.IEndPoint#reconnect()
	 */
	public boolean reconnect() {
		return wrappedEndPoint.reconnect();
	}

	/**
	 * {@inheritDoc}
	 * <p>Unregisters the JMX MBean management interface</p>
	 * @see org.helios.ot.endpoint.LifecycleAwareIEndPoint#onTracerManagerShutdown()
	 */
	@Override
	public void onTracerManagerShutdown() {
		if(server!=null && objectName!=null) {
			try {
				if(server.isRegistered(objectName)) {
					server.unregisterMBean(objectName);
				}
			} catch (Exception e) {
				log.warn("Failed to unregister MBean [" + objectName + "]", e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>Registers the JMX MBean management interface</p>
	 * @see org.helios.ot.endpoint.LifecycleAwareIEndPoint#onTracerManagerStartup()
	 */
	@Override
	public void onTracerManagerStartup() {
		if(objectName!=null) {
			JMXHelper.getRuntimeHeliosMBeanServer().registerMBean(this, objectName);
		}		
	}
}
