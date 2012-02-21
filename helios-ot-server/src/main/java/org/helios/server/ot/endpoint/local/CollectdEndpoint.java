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

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.endpoint.AbstractEndpoint;
import org.helios.ot.endpoint.EndpointConnectException;
import org.helios.ot.endpoint.EndpointTraceException;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.trace.types.IntTraceValue;
import org.helios.ot.trace.types.LongTraceValue;
import org.helios.ot.trace.types.interval.IntIntervalTraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: CollectdEndpoint</p>
 * <p>Description: A helios OT end point for sending metrics to a collectd daemon</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.endpoint.local.CollectdEndpoint</code></p>
 */

public class CollectdEndpoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T> {
	/**  */
	private static final long serialVersionUID = 6886015557719724803L;


	/** The server name or ip address that the collectd server is running on */
	protected String server = "localhost";
	/** The port that the collectd server is listening on */
	protected int port = 8125;
	
	/**
	 * Creates a new CollectdEndpoint
	 */
	public CollectdEndpoint() {
		super();		
	}
	

	/**
	 * Creates a new CollectdEndpoint
	 * @param server the server name or ip address that the collectd server is running on
	 * @param port the port that the collectd server is listening on 
	 */
	public CollectdEndpoint(String server, int port) {
		super();
		this.server = server;
		this.port = port;
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
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#connectImpl()
	 */
	@Override
	protected void connectImpl() throws EndpointConnectException {
//		try {
//			client= new StatsdClient(server, port);
//		} catch (Exception e) {
//			client=null;
//			throw new EndpointConnectException("The statsd client failed to connect to [" + server + ":" + port + "]", e);
//		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#processTracesImpl(org.helios.ot.tracer.disruptor.TraceCollection)
	 */
	@Override
	protected boolean processTracesImpl(TraceCollection<T> traceCollection) throws EndpointConnectException, EndpointTraceException {
		if(traceCollection==null) return false;
//		if(client==null) return false;
//		Set<T> set = (Set<T>)traceCollection.getTraces();
//		if(set.isEmpty()) return false;
//		try {
//			for(T t: set) {
//				if(t.getMetricType().isInt()) {
//					boolean success = false;
//					if(t.isInterval()) {
//						success = client.count(t.getFQN().replace("/", ".").replace(" ", "_"), ((IntIntervalTraceValue)t.getTraceValue()).getAvg());
//					} else {
//						success = client.count(t.getFQN().replace("/", ".").replace(" ", "_"), ((IntTraceValue)t.getTraceValue()).getIntValue());
//					}	
//					log.info("Success:" + success);
//				} else if(t.getMetricType().isLong()) {
////					if(t.isInterval()) {
////						long value = ((LongTraceValue)t.getTraceValue()).getLongValue();
////						if(value<=Integer.MAX_VALUE) {
////						
////					}
////					long value = ((LongTraceValue)t.getTraceValue()).getLongValue();
////					if(value<=Integer.MAX_VALUE) {
////						client.count(t.getFQN().replace("/", "."), (int)value);
////					}
//				}
//			}
//			log.info("Forwarded [" + set.size() + "] Traces");
//		} catch (Exception e) {
//			throw new EndpointTraceException("Failed to send exchange", e);
//		}		
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#disconnectImpl()
	 */
	@Override
	protected void disconnectImpl() {
//		try { 
//			if(client!=null) {
//				client.shutdown();
//			}
//		} catch (Exception e) {			
//		} finally {
//			client=null;
//		}
	}
	
	/**
	 * Returns the server name or ip address that the collectd server is running on
	 * @return the server name or ip address that the collectd server is running on
	 */
	@JMXAttribute(name="Server", description="The server name or ip address that the collectd server is running on", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getServer() {
		return server;
	}

	/**
	 * Returns the port that the collectd server is listening on
	 * @return the port that the collectd server is listening on
	 */
	@JMXAttribute(name="Port", description="The port that the collectd server is listening on", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getPort() {
		return port;
	}

	

}
