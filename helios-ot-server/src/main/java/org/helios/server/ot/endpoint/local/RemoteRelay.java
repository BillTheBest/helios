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

import org.apache.camel.Exchange;
import org.helios.ot.endpoint.AbstractEndpoint;
import org.helios.ot.trace.Trace;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: RemoteRelay</p>
 * <p>Description: Relays traces from remote OT agents to endpoints in the ot server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.endpoint.local.RemoteRelay</code></p>
 */
public class RemoteRelay {
	/** THe endpoint to forward to */
	@SuppressWarnings("rawtypes")
	protected AbstractEndpoint endpoint = null;

	/**
	 * Sets the endpoint to forward to
	 * @param endpoint the endpoint to set
	 */
	public void setEndpoint(AbstractEndpoint endpoint) {
		if(endpoint==null) throw new IllegalArgumentException("The passed endpoint was null", new Throwable());
		this.endpoint = endpoint;
	}
	
	/**
	 * Extracts the traces from the passed exchange and relays them to the configured endpoint
	 * @param exchange The OT exchange
	 */
	public void process(Exchange exchange) {
		Object body = exchange.getIn().getBody();
		if(body!=null &&  body instanceof Trace[]) {
			Trace[] traces = (Trace[])body;
			if(traces.length>0) {
				TraceCollection tc = new TraceCollection();
				tc.load(traces);
				try {
					endpoint.processTraces(tc);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
