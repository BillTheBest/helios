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
package org.helios.server.ot.endpoint.local;

import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.StartupListener;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.endpoint.AbstractEndpoint;
import org.helios.ot.endpoint.EndpointConnectException;
import org.helios.ot.endpoint.EndpointTraceException;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: LocalEndpoint</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.endpoint.local.LocalEndpoint</code></p>
 */

public class LocalEndpoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T> implements CamelContextAware, StartupListener {
	/** The sender template */
	protected ProducerTemplate producerTemplate;
	/** The OT Input Camel URI */
	protected final String uri;
	/** The Camel Endpoint where this OT endpoint will send traces to */
	protected Endpoint endpoint = null;
	/** The Camel context */
	protected CamelContext camelContext = null;
	/** Indicates if this endpoint is ready to accept traces */
	protected boolean ready = false;
	/** The number of times processTraces has been called when the endpoint is not ready */
	protected long notReadyRejections = 0;
	

	/**
	 * Creates a new Local endpoint for OT instances running in the OT server
	 * @param uri The OT Input Camel URI 
	 */
	public LocalEndpoint(String uri) {		
		this.uri = uri;
	}
	
	
	/**
	 * {@inheritDoc}
	 * <p>Configures the producer template</p>
	 * @see org.helios.ot.endpoint.AbstractEndpoint#connectImpl()
	 */
	@Override
	protected void connectImpl() throws EndpointConnectException {
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#disconnectImpl()
	 */
	@Override
	protected void disconnectImpl() {
	}

	/**
	 * <p>Throws a UnsupportedOperationException</p>
	 * @see org.helios.ot.endpoint.AbstractEndpoint#newBuilder()
	 */
	@Override
	public org.helios.ot.endpoint.AbstractEndpoint.Builder newBuilder() {
		throw new UnsupportedOperationException("LocalEndpoint is a Spring/Camel specific class and should be instantiated through the public constructor", new Throwable());
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#processTracesImpl(org.helios.ot.tracer.disruptor.TraceCollection)
	 */
	@Override
	protected boolean processTracesImpl(TraceCollection<T> traceCollection) throws EndpointConnectException, EndpointTraceException {
		if(!ready) {
			notReadyRejections++;
			if(notReadyRejections%10==0) {
				try { onCamelContextStarted(camelContext, true); } catch (Exception e) {}
			}
		}
		if(traceCollection==null) return false;
		
		Set<T> set = (Set<T>)traceCollection.getTraces();
		if(set.isEmpty()) return false;
		try {
			producerTemplate.sendBody(uri, ExchangePattern.OutOnly, set.toArray(new Trace[set.size()]));
			log.info("Forwarded [" + set.size() + "] Traces");
		} catch (Exception e) {
			throw new EndpointTraceException("Failed to send exchange", e);
		}		
		return true;
	}
	
	/**
	 * Returns the number of unready process trace calls
	 * @return the number of unready process trace calls
	 */
	@JMXAttribute(name="UnreadyCalls", description="The number of unready process trace calls", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getUnreadyCalls() {
		return notReadyRejections;
	}
	
	/**
	 * Indicates if the endpoint is ready
	 * @return true if the endpoint is ready
	 */
	@JMXAttribute(name="Ready", description="Indicates if the endpoint is ready", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getReady() {
		return ready;
	}
	

	/**
	 * Returns the configured Camel Endpoint URI
	 * @return the configured Camel Endpoint URI
	 */
	@JMXAttribute(name="Uri", description="The configured Camel Endpoint URI", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getUri() {
		return uri;
	}



	/**
	 * @return the camelContext
	 */
	public CamelContext getCamelContext() {
		return camelContext;
	}



	/**
	 * @param camelContext the camelContext to set
	 */
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
		try {
			this.camelContext.addStartupListener(this);
		} catch (Exception e) {
			log.error("Failed to add startup listener to Camel Context.", e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.StartupListener#onCamelContextStarted(org.apache.camel.CamelContext, boolean)
	 */
	@Override
	public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) throws Exception {
		producerTemplate = camelContext.createProducerTemplate();
		endpoint = camelContext.getEndpoint(uri);
		ready = true;
		log.info("OT LocalEndpoint Configured with endpoint [" + endpoint.getEndpointKey() + "/" + endpoint.getEndpointUri() + "]");
		
	}




}
