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
package org.helios.jmx.dynamic.annotations.introspection;

/**
 * <p>Title: IntrospectionExpressionException</p>
 * <p>Description: Generic Introspection Expression evaluation exception. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * org.helios.jmx.dynamic.annotations.introspection.IntrospectionExpressionException
 */
public class IntrospectionExpressionException extends Exception {

	/**  */
	private static final long serialVersionUID = -3041282108824918108L;

	/**
	 * 
	 */
	public IntrospectionExpressionException() {
		super();
	}

	/**
	 * @param message
	 */
	public IntrospectionExpressionException(String message) {
		super(message);
	}

	/**
	 * @param t
	 */
	public IntrospectionExpressionException(Throwable t) {
		super(t);
	}

	/**
	 * @param message
	 * @param t
	 */
	public IntrospectionExpressionException(String message, Throwable t) {
		super(message, t);
	}

}
