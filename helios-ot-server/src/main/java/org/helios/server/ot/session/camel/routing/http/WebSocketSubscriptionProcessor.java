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
package org.helios.server.ot.session.camel.routing.http;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.log4j.Logger;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.server.ot.session.OutputFormat;
import org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor;

/**
 * <p>Title: WebSocketSubscriptionProcessor</p>
 * <p>Description: Processor responsible for delivering a message to a client via a websocket call</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.http.WebSocketSubscriptionProcessor</code></p>
 */

public class WebSocketSubscriptionProcessor<T> extends WebSocketHandler implements SubscriptionOutputProcessor<T>, CamelContextAware {
	/** The subscriber session Id */
	protected final String sessionId;
	/** The subscriber output format */
	protected final OutputFormat outputFormat;
	/** The camel context */
	protected CamelContext camelContext = null;
	/** The drop counter */
	protected final AtomicLong dropCount = new AtomicLong(0L);
	/** The delivery counter */
	protected final AtomicLong deliveryCount = new AtomicLong(0L);
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The ID of the route this processor is running in */
	protected String routeId = null;
	
	/**
	 * Creates a new WebSocketSubscriptionProcessor
	 * @param sessionId The subscriber session Id
	 * @param outputFormat The subscriber output format 
	 */
	public WebSocketSubscriptionProcessor(String sessionId, OutputFormat outputFormat) {
		this.sessionId = sessionId;
		this.outputFormat = outputFormat;		 
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jetty.websocket.WebSocketFactory.Acceptor#doWebSocketConnect(javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * <p>Title: WebSocketProcessor</p>
	 * <p>Description: The websocket text message handler</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.camel.routing.http.WebSocketSubscriptionProcessor.WebSocketProcessor</code></p>
	 */
	private class WebSocketProcessor implements WebSocket.OnTextMessage {
		/** The websocket connection */
		private Connection connection;
		
		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jetty.websocket.WebSocket#onOpen(org.eclipse.jetty.websocket.WebSocket.Connection)
		 */
		public void onOpen(Connection connection) {
			this.connection = connection;
		}

		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jetty.websocket.WebSocket.OnTextMessage#onMessage(java.lang.String)
		 */
		public void onMessage(String data) {

		}

		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jetty.websocket.WebSocket#onClose(int, java.lang.String)
		 */
		public void onClose(int closeCode, String message) {

		}
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
	 */
	@Override
	public void process(Exchange exchange) throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor#getOutputFormat()
	 */
	@Override
	public OutputFormat getOutputFormat() {
		return outputFormat;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor#terminate()
	 */
	public void terminate() {
		try {
			camelContext.stopRoute(routeId);
			camelContext.removeRoute(routeId);
			log.info("Stopped Processor [" + routeId + "]");
		} catch (Exception e) {
			log.warn("Failed to stop route [" + routeId + "]");
		}
	}


	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.CamelContextAware#getCamelContext()
	 */
	@Override
	public CamelContext getCamelContext() {
		return camelContext;
	}

	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.CamelContextAware#setCamelContext(org.apache.camel.CamelContext)
	 */
	@Override
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;

	}

	/**
	 * The number of items delivered to the suscriber
	 * @return the deliveryCount
	 */
	@JMXAttribute(name="DeliveryCount", description="The number of items delivered to the suscriber", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getDeliveryCount() {
		return deliveryCount.get();
	}
	
	/**
	 * The number of items dropped because of a full delivery queue
	 * @return the drop Count
	 */
	@JMXAttribute(name="DropCount", description="The number of items dropped because of a full delivery queue", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getDropCount() {
		return dropCount.get();
	}
	
	/**
	 * {@inheritDoc}
	 * <p>Throws an {@link UnsupportedOperationException}
	 * @see org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor#poll(int)
	 */
	@Override
	public Set<T> poll(int atATime) {
		throw new UnsupportedOperationException("[" + getClass().getName() + "] is not a polling processor", new Throwable());
	}



	@Override
	public Set<T> poll(int atATime, long timeout) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void registerContinuation(Continuation continuation) {
		// TODO Auto-generated method stub
		
	}
	

}
