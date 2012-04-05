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
import org.helios.helpers.InetAddressHelper;

/**
 * <p>Title: HeliosEndpointConfiguration</p>
 * <p>Description: Provides environment or system property overridable constant values for the {@link HeliosEndpoint}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.HeliosEndpointConfiguration</code></p>
 */

public class HeliosEndpointConfiguration {
	/** The property name for the helios ot server host name or ip address */
	public static final String HOST = "org.helios.ot.host";
	/** The property name for the helios ot server listening port */
	public static final String PORT = "org.helios.ot.port";
	/** The property name for the protocol the agent should use to comm with the helios-ot server */
	public static final String PROTOCOL = "org.helios.ot.protocol";
	/** The property name for the maximum size of the trace buffer before it is flushed */
	public static final String FLUSH_SIZE_MAX = "org.helios.ot.flush.maxsize";
	/** The property name for the maximum amount of time in ms. before a non empty trace buffer is flushed */
	public static final String FLUSH_TIME_MAX = "org.helios.ot.flush.maxtime";

	
	/** The default helios ot server host name or ip address */
	public static final String DEFAULT_HOST = "localhost";
	/** The default protocol the agent should use to comm with the helios-ot server (UDP or TCP) */
	public static final String DEFAULT_PROTOCOL = "TCP";
	/** The default maximum size of the trace buffer before it is flushed */
	public static final int DEFAULT_FLUSH_SIZE_MAX = 200;
	/** The default maximum amount of time in ms. before a non empty trace buffer is flushed */
	public static final long DEFAULT_FLUSH_TIME_MAX = 5000;
	
	//=============================================
	//   Discovery 
	//=============================================
	/** The property name for the helios ot server discovery multicast network */
	public static final String DISCOVERY_NETWORK = "org.helios.ot.discovery.network";
	/** The property name for the helios ot server discovery multicast port */
	public static final String DISCOVERY_PORT = "org.helios.ot.discovery.port";
	/** The property name for the helios ot server discovery multicast timeout in ms. */
	public static final String DISCOVERY_TIMEOUT = "org.helios.ot.discovery.timeout";
	/** The property name for the helios ot server discovery preferred protocol */
	public static final String DISCOVERY_PREF_PROTOCOL = "org.helios.ot.discovery.prefprotocol";
	/** The property name for the helios ot server discovery listening interface */
	public static final String DISCOVERY_LISTEN_IFACE = "org.helios.ot.discovery.interface";

	
	/** The default ot server discovery multicast network */
	public static final String DEFAULT_DISCOVERY_NETWORK = "224.9.3.7";
	/** The default ot server discovery multicast port */
	public static final int DEFAULT_DISCOVERY_PORT = 1836;
	/** The default helios ot server discovery multicast timeout in ms. */
	public static final int DEFAULT_DISCOVERY_TIMEOUT = 3000;
	/** The default helios ot server discovery preferred protocol */
	public static final String DEFAULT_DISCOVERY_PREF_PROTOCOL = "TCP";
	/** The default helios ot server discovery listening interface */
	public static final String DEFAULT_DISCOVERY_LISTEN_IFACE = InetAddressHelper.hostName();
	
	//=============================================
	
	
	/**
	 * Returns the maximum amount of time in ms. before a non empty trace buffer is flushed
	 * @return the maximum amount of time in ms. before a non empty trace buffer is flushed
	 */
	public static long getMaxFlushTime() {
		return ConfigurationHelper.getLongSystemThenEnvProperty(FLUSH_TIME_MAX, DEFAULT_FLUSH_TIME_MAX);
	}
	
	/**
	 * Returns the maximum size of the trace buffer before it is flushed
	 * @return the maximum size of the trace buffer before it is flushed
	 */
	public static int getMaxFlushSize() {
		return ConfigurationHelper.getIntSystemThenEnvProperty(FLUSH_SIZE_MAX, DEFAULT_FLUSH_SIZE_MAX);
	}
	
	
	
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
		return ConfigurationHelper.getIntSystemThenEnvProperty(PORT, getProtocol().getDefaultPort());
	}
	
	/**
	 * Returns the configured protocol
	 * @return the configured protocol
	 */
	public static Protocol getProtocol() {
		return Protocol.forValue(ConfigurationHelper.getSystemThenEnvProperty(PROTOCOL, DEFAULT_PROTOCOL));
	}

	/**
	 * Returns the configured discovery response listening interface address
	 * @return the configured discovery response listening interface address
	 */
	public static String getDiscoveryListenAddress() {
		return ConfigurationHelper.getSystemThenEnvProperty(DISCOVERY_LISTEN_IFACE, DEFAULT_DISCOVERY_LISTEN_IFACE);
	}
	
	
	/**
	 * Returns the configured discovery preferred protocol
	 * @return the configured discovery preferred protocol
	 */
	public static String getDiscoveryPreferredProtocol() {
		return ConfigurationHelper.getSystemThenEnvProperty(DISCOVERY_PREF_PROTOCOL, DEFAULT_DISCOVERY_PREF_PROTOCOL);
	}
	
	
	/**
	 * Returns the configured discovery network
	 * @return the configured discovery network
	 */
	public static String getDiscoveryNetwork() {
		return ConfigurationHelper.getSystemThenEnvProperty(DISCOVERY_NETWORK, DEFAULT_DISCOVERY_NETWORK);
	}
	
	/**
	 * Returns the configured discovery port
	 * @return the configured discovery port
	 */
	public static int getDiscoveryPort() {
		return ConfigurationHelper.getIntSystemThenEnvProperty(DISCOVERY_PORT, DEFAULT_DISCOVERY_PORT);
	}
	
	/**
	 * Returns the configured discovery timeout in ms.
	 * @return the configured discovery timeout in ms.
	 */
	public static int getDiscoveryTimeout() {
		return ConfigurationHelper.getIntSystemThenEnvProperty(DISCOVERY_TIMEOUT, DEFAULT_DISCOVERY_TIMEOUT);
	}
	
	
	
	
}
