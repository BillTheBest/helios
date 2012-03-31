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
package org.helios.ot.helios;

import org.helios.helpers.ConfigurationHelper;

/**
 * <p>Title: HeliosEndpointConstants</p>
 * <p>Description: Provides environment or system property overridable constant values for the {@link HeliosEndpoint}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.HeliosEndpointConstants</code></p>
 */

public class HeliosEndpointConstants {
	/** The property name for the helios ot server host name or ip address */
	public static final String HOST = "org.helios.ot.host";
	/** The property name for the helios ot server listening port */
	public static final String PORT = "org.helios.ot.port";
	/** The property name for the protocol the agent should use to comm with the helios-ot server (UDP or TCP) */
	public static final String PROTOCOL = "org.helios.ot.protocol";
	/** The default helios ot server host name or ip address */
	public static final String DEFAULT_HOST = "localhost";
	/** The default helios ot server listening port */
	public static final int DEFAULT_PORT = 9428;
	/** The default protocol the agent should use to comm with the helios-ot server (UDP or TCP) */
	public static final String DEFAULT_PROTOCOL = "TCP";
	
	/**
	 * Returns the configured host
	 * @return the configured host
	 */
	public static String getHost() {
		return ConfigurationHelper.getSystemThenEnvProperty(HOST, DEFAULT_HOST);
	}

	/**
	 * Returns the configured port
	 * @return the configured port
	 */
	public static int getPort() {
		return ConfigurationHelper.getIntSystemThenEnvProperty(PORT, DEFAULT_PORT);
	}
	
	/**
	 * Returns the configured protocol
	 * @return the configured protocol
	 */
	public static Protocol getProtocol() {
		return Protocol.forValue(ConfigurationHelper.getSystemThenEnvProperty(PROTOCOL, DEFAULT_PROTOCOL));
	}
	
	
}
