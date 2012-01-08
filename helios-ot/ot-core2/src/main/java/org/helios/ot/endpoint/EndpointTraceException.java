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

/**
 * <p>Title: EndpointTraceException</p>
 * <p>Description: Exception thrown by Endpoint concrete classes 
 *   indicating errors while submitting traces to the target endpoint.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class EndpointTraceException extends Exception {

	private static final long serialVersionUID = -6712038214916905694L;

	public EndpointTraceException() {
		super();
	}

	public EndpointTraceException(String message, Throwable cause) {
		super(message, cause);
	}

	public EndpointTraceException(String message) {
		super(message);
	}

	public EndpointTraceException(Throwable cause) {
		super(cause);
	}

}
