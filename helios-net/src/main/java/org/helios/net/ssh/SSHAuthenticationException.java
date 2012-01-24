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
package org.helios.net.ssh;

import java.util.Map;

/**
 * <p>Title: SSHAuthenticationException</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.SSHAuthenticationException</code></p>
 */
public class SSHAuthenticationException extends Exception {

	/**  */
	private static final long serialVersionUID = -1195278329904064741L;

	/**
	 * Creates a new SSHAuthenticationException
	 */
	public SSHAuthenticationException() {
	}
	
	/**
	 * Creates a new SSHAuthenticationException
	 * @param message The authentication failure message
	 * @param authResults A map of authentication exceptions keyed by the authentication type
	 */
	public SSHAuthenticationException(String message, Map<String, Exception> authResults) {
		this(buildMessage(message, authResults));
	}
	
	protected static String buildMessage(String message, Map<String, Exception> authResults) {
		StringBuilder b = new StringBuilder(message);
		for (Map.Entry<String, Exception> entry: authResults.entrySet()) {
			b.append("\n\t[").append(entry.getKey()).append("]:").append(entry.getValue());
		}
		return b.toString();
	}
	

	/**
	 * Creates a new SSHAuthenticationException
	 * @param message The authentication failure message
	 */
	public SSHAuthenticationException(String message) {
		super(message);
	}

	/**
	 * Creates a new SSHAuthenticationException
	 * @param cause The authentication failure cause
	 */
	public SSHAuthenticationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new SSHAuthenticationException
	 * @param message The authentication failure message
	 * @param cause The authentication failure cause
	 */
	public SSHAuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

}
