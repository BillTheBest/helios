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
package org.helios.server.ot.session.camel.routing;

import java.util.Collections;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.jetty.continuation.Continuation;
import org.helios.server.ot.session.OutputFormat;

/**
 * <p>Title: SubscriptionOutputProcessor</p>
 * <p>Description: Defines a Camel processor responsible for delivering content to a subscriber</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.routing.SubscriptionOutputProcessor</code></p>
 * @param <T> The type of the content that the processor delivers
 */

public interface SubscriptionOutputProcessor<T> extends Processor {
	
	/** Place holder SubscriptionOutputProcessor for prototypes in spring */
	public static final SubscriptionOutputProcessor PROTOTYPE = new SubscriptionOutputProcessor(){

		@Override
		public OutputFormat getOutputFormat() {
			return OutputFormat.TEXT;
		}

		@Override
		public Set poll(int atATime) {
			return Collections.EMPTY_SET;
		}

		@Override
		public Set poll(int atATime, long timeout) {
			return Collections.EMPTY_SET;
		}

		@Override
		public void terminate() {
		}

		@Override
		public void process(Exchange exchange) throws Exception {
		}
		
		/**
		 * Registers a continuation that the processor will resume when a new item is published
		 * @param continuation a jetty continuation
		 */
		public void registerContinuation(Continuation continuation) {}
		
		
	};
	
	/**
	 * Returns the subscribers OutputFormat for this processor
	 * @return an OutputFormat
	 */
	public OutputFormat getOutputFormat();
	
	/**
	 * Registers a continuation that the processor will resume when a new item is published
	 * @param continuation a jetty continuation
	 */
	public void registerContinuation(Continuation continuation);
	
	/**
	 * Terminates the processor
	 */
	public void terminate();
	
	/**
	 * Polls for delivery
	 * @param atATime The number of items to retrieve in this call
	 * @return A (possibly empty) set of subscription items
	 */
	public Set<T> poll(int atATime);
	
	/**
	 * Retrieves subscription elements for delivery
	 * @param atATime The maximum number of elements to retrieve
	 * @param timeout The period of time (ms.) to wait for results before the request times out and returns an empty result.
	 * @return A [possibly] empty set of response items.
	 */
	public Set<T> poll(int atATime, long timeout);
	
	
}
