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
package org.helios.jmx.client;

/**
 * <p>Title: AsyncClientInterfaceNotSupported</p>
 * <p>Description: Exception thrown when an async interface is requested from a client that does not support it.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public class AsyncClientInterfaceNotSupported extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5568183981441439787L;

	/**
	 * 
	 */
	public AsyncClientInterfaceNotSupported() {

	}

	/**
	 * @param message
	 */
	public AsyncClientInterfaceNotSupported(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public AsyncClientInterfaceNotSupported(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public AsyncClientInterfaceNotSupported(String message, Throwable cause) {
		super(message, cause);
	}

}
