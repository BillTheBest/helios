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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.log4j.Logger;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.server.ot.session.OutputFormat;
import org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor;

/**
 * <p>Title: PollingHttpSubscriptionProcessor</p>
 * <p>Description: Processor responsible for delivering a message to an output queue where it will be pulled by a REST call</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.http.PollingHttpSubscriptionProcessor</code></p>
 * @param <T> The type of the content that the processor delivers
 */
@JMXManagedObject(annotated=true, declared=true)
public class PollingHttpSubscriptionProcessor<T> implements SubscriptionOutputProcessor<T>, CamelContextAware {
	/** The subscriber session Id */
	protected final String sessionId;
	/** The subscriber output format */
	protected final OutputFormat outputFormat;
	/** The size of the delivery queue. Defaults to 100 */
	protected int queueSize = 100;
	/** The delivery queue */
	protected BlockingQueue<byte[]> deliveryQueue;
	/** The drop counter */
	protected final AtomicLong dropCount = new AtomicLong(0L);
	/** The delivery counter */
	protected final AtomicLong deliveryCount = new AtomicLong(0L);
	/** The camel context */
	protected CamelContext camelContext = null;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The ID of the route this processor is running in */
	protected String routeId = null;
	/** The current sessionContinuation */
	protected final AtomicReference<Continuation> sessionContinuation = new AtomicReference<Continuation>(null);
	
	
	/**
	 * Creates a new PollingHttpSubscriptionProcessor
	 * @param sessionId The subscriber session Id
	 * @param outputFormat The subscriber output format 
	 */
	public PollingHttpSubscriptionProcessor(String sessionId, OutputFormat outputFormat) {
		this.sessionId = sessionId;
		this.outputFormat = outputFormat;		 
	}
	
	/**
	 * Registers a continuation that the processor will resume when a new item is published
	 * @param continuation a jetty continuation
	 */
	public void registerContinuation(Continuation continuation) {
		sessionContinuation.set(continuation);
	}
	
	
	/**
	 * Retrieves subscription elements for delivery
	 * @param atATime The number of elements to retrieve
	 * @return A set of delivery elements
	 */
	public Set<T> poll(int atATime) {
		Set<byte[]> results = new HashSet<byte[]>();
		if(atATime<1) {
			deliveryQueue.drainTo(results);
		} else {
			deliveryQueue.drainTo(results, atATime);
		}
		deliveryCount.addAndGet(results.size());
		return (Set<T>) results;
	}

	/**
	 * Retrieves subscription elements for delivery
	 * @param atATime The maximum number of elements to retrieve
	 * @param timeout The period of time (ms.) to wait for results before the request times out and returns an empty result.
	 * @param request The http request
	 */
	public Set<T> poll(int atATime, long timeout) {
		Set<byte[]> results = new HashSet<byte[]>();
		if(!deliveryQueue.isEmpty()) {
			
			if(atATime<1) {
				deliveryQueue.drainTo(results);
			} else {
				deliveryQueue.drainTo(results, atATime);
			}
			deliveryCount.addAndGet(results.size());			
		}
		return (Set<T>) results;
	}



	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
	 */
	@Override
	public void process(Exchange exchange) throws Exception {
		byte[] items = exchange.getIn().getBody(byte[].class);
//		Continuation cont = sessionContinuation.get();
//		if(cont!=null && cont.isSuspended()) {
//			cont.setAttribute("items", items);
//			cont.resume();
//			writeResponse((HttpServletResponse)cont.getServletResponse(), items);
//			cont.complete();
//			log.info("Item processing found suspended continuation. Delivered 1 item");
//			sessionContinuation.set(null);
//			return;
//		}		
		if(!deliveryQueue.offer(items)) {
			dropCount.incrementAndGet();
		} 
		Continuation cont = sessionContinuation.get();
		if(cont!=null && cont.isSuspended()) {
			cont.resume();
//			log.info("Resumed Continuation");
			sessionContinuation.set(null);
		}
	}
	

	/**
	 * The maximum number of items in the delivery queue
	 * @return the queueLimit
	 */
	@JMXAttribute(name="QueueLimit", description="The maximum number of items in the delivery queue", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getQueueLimit() {
		return queueSize;
	}
	
	/**
	 * Returns the number of items in the delivery queue
	 * @return the number of items in the delivery queue
	 */
	@JMXAttribute(name="QueueSize", description="The number of items in the delivery queue", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getQueueSize() {
		return deliveryQueue.size();
	}
	
	/**
	 * Returns the remaining capacity of the delivery queue
	 * @return the number of items in the delivery queue
	 */
	@JMXAttribute(name="QueueSize", description="The remaining capacity of the delivery queue", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getQueueCapacity() {
		return deliveryQueue.remainingCapacity();
	}
	
	/**
	 * Determines if the request continuation is expired
	 * @return true if the continuation is expired, false if it is not expired and null if no continuation is registered.
	 */
	@JMXAttribute(name="RequestExpired", description="Indicates if the request continuation is expired", mutability=AttributeMutabilityOption.READ_ONLY)
	public Boolean isRequestExpired() {
		Continuation c = sessionContinuation.get();
		if(c==null) return null;
		return c.isExpired();
	}
	
	/**
	 * Determines if the request continuation is suspended
	 * @return true if the continuation is suspended, false if it is not suspended and null if no continuation is registered.
	 */
	@JMXAttribute(name="RequestSuspended", description="Indicates if the request continuation is suspended", mutability=AttributeMutabilityOption.READ_ONLY)
	public Boolean isRequestSuspended() {
		Continuation c = sessionContinuation.get();
		if(c==null) return null;
		return c.isSuspended();
	}
	
	/**
	 * Determines if the request continuation is resumed
	 * @return true if the continuation is resumed, false if it is not resumed and null if no continuation is registered.
	 */
	@JMXAttribute(name="RequestResumed", description="Indicates if the request continuation is resumed", mutability=AttributeMutabilityOption.READ_ONLY)
	public Boolean isRequestResumed() {
		Continuation c = sessionContinuation.get();
		if(c==null) return null;
		return c.isResumed();
	}
	
	/**
	 * Determines if the request continuation is initial
	 * @return true if the continuation is initial, false if it is not initial and null if no continuation is registered.
	 */
	@JMXAttribute(name="RequestInitial", description="Indicates if the request continuation is initial", mutability=AttributeMutabilityOption.READ_ONLY)
	public Boolean isRequestInitial() {
		Continuation c = sessionContinuation.get();
		if(c==null) return null;
		return c.isInitial();
	}
	
	
	/**
	 * Clears the delivery queue
	 */
	@JMXOperation(name="clearDeliveryQueue", description="Clears the delivery queue")
	public void clearDeliveryQueue() {
		deliveryQueue.clear();
	}
	

	/**
	 * Sets the queue size and builds the delivery queue
	 * @param queueSize the queueSize to set
	 */
	public void setQueueLimit(int queueSize) {
		if(deliveryQueue!=null) throw new IllegalStateException("The delivery queue has already been created", new Throwable());
		this.queueSize = queueSize;
		deliveryQueue = new ArrayBlockingQueue<byte[]>(this.queueSize);
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @return the outputFormat
	 */
	public OutputFormat getOutputFormat() {
		return outputFormat;
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
	 * Returns the Camel Context
	 * @return the camelContext
	 */
	public CamelContext getCamelContext() {
		return camelContext;
	}

	/**
	 * Sets the Camel Context
	 * @param camelContext the camelContext to set
	 */
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
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
	 * Returns the ID of the route this processor is running in 
	 * @return the routeId
	 */
	public String getRouteId() {
		return routeId;
	}

	/**
	 * Sets the ID of the route this processor is running in 
	 * @param routeId the routeId to set
	 */
	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

}
