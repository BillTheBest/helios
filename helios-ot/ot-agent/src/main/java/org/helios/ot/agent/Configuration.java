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
package org.helios.ot.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.InetAddressHelper;


/**
 * <p>Title: Configuration</p>
 * <p>Description: Configuration management class for the Helios OT Agent</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.Configuration</code></p>
 */
public class Configuration {
	/** The property name prefix for all connection properties */
	public static final String CONNECTION_PREFIX = "org.helios.connection";	
	
	
	/** The property name for the helios ot server host name or ip address */
	public static final String HOST = CONNECTION_PREFIX + ".host";
	/** The property name for the helios ot server listening port */
	public static final String PORT = CONNECTION_PREFIX + ".port";
	/** The property name for the protocol the agent should use to comm with the helios-ot server */
	public static final String PROTOCOL = CONNECTION_PREFIX + ".protocol";
	/** The property name for the maximum size of the trace buffer before it is flushed */
	public static final String FLUSH_SIZE_MAX = CONNECTION_PREFIX + ".maxsize";
	/** The property name for the maximum amount of time in ms. before a non empty trace buffer is flushed */
	public static final String FLUSH_TIME_MAX = CONNECTION_PREFIX + ".maxtime";
	/** The property name for the connect timeout in ms. */
	public static final String CONNECT_TIMEOUT = CONNECTION_PREFIX + ".connectTimeoutMillis";
	/** The property name for synchronous operation timeouts in ms. */
	public static final String SYNCH_OP_TIMEOUT = CONNECTION_PREFIX + "operationTimeoutMillis";
	
	
	

	/** System props and environment config name for tcp no delay option */
	public static final String CONFIG_NODELAY = CONNECTION_PREFIX + "tcpNoDelay";
	/** System props and environment config name for tcp keep alive option */
	public static final String CONFIG_KEEPALIVE = CONNECTION_PREFIX + "keepAlive";
	/** System props and environment config name for socket reuse option option */
	public static final String CONFIG_REUSEADDRESS = CONNECTION_PREFIX + "reuseAddress";
	/** System props and environment config name for socket linger time option */
	public static final String CONFIG_SOLINGER = CONNECTION_PREFIX + "soLinger";
	/** System props and environment config name for traffic class option */
	public static final String CONFIG_TRAFFIC_CLASS = CONNECTION_PREFIX + "trafficClass";
	/** System props and environment config name for socket receive buffer size */
	public static final String CONFIG_RECEIVE_BUFFER = CONNECTION_PREFIX + "receiveBufferSize";
	/** System props and environment config name for socket send buffer size */
	public static final String CONFIG_SEND_BUFFER = CONNECTION_PREFIX + "sendBufferSize";
	

	
	/** The default helios ot server host name or ip address */
	public static final String DEFAULT_HOST = "localhost";
	/** The default protocol the agent should use to comm with the helios-ot server */
	public static final String DEFAULT_PROTOCOL = "TCP";
	/** The default maximum size of the trace buffer before it is flushed */
	public static final int DEFAULT_FLUSH_SIZE_MAX = 200;
	/** The default maximum amount of time in ms. before a non empty trace buffer is flushed */
	public static final long DEFAULT_FLUSH_TIME_MAX = 5000;
	/** The default connect timeout in ms. */
	public static final long DEFAULT_CONNECT_TIMEOUT = 3000;
	/** The default synchronous operation timeouts in ms. */
	public static final long DEFAULT_SYNCH_OP_TIMEOUT = 3000;
	
	//=============================================
	//   Discovery 
	//=============================================
	/** The property name prefix for discovery properties */
	public static final String DISCOVERY_PREFIX = CONNECTION_PREFIX + ".discovery";	
	/** The property name for the helios ot server discovery multicast network */
	public static final String DISCOVERY_NETWORK = DISCOVERY_PREFIX + ".network";
	/** The property name for the helios ot discovery enablement */
	public static final String DISCOVERY_ENABLED = DISCOVERY_PREFIX + ".enabled";
	/** The property name for the maximum number of discovery attempts in one connect round */
	public static final String DISCOVERY_MAX_ATTEMPTS = DISCOVERY_PREFIX + ".maxattempts";
	
	/** The property name for the helios ot server discovery multicast port */
	public static final String DISCOVERY_PORT = DISCOVERY_PREFIX + ".port";
	/** The property name for the helios ot server discovery multicast timeout in ms. */
	public static final String DISCOVERY_TIMEOUT = DISCOVERY_PREFIX + ".timeout";
	/** The property name for the helios ot server discovery preferred protocol */
	public static final String DISCOVERY_PREF_PROTOCOL = DISCOVERY_PREFIX + ".prefprotocol";
	/** The property name for the helios ot server discovery listening interface */
	public static final String DISCOVERY_LISTEN_IFACE = DISCOVERY_PREFIX + ".interface";
	/** The property name for the helios ot server discovery request transmission nic */
	public static final String DISCOVERY_TRANSMIT_NIC = DISCOVERY_PREFIX + ".nic";

	
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
	/** The default helios ot server discovery enablement */
	public static final boolean DEFAULT_DISCOVERY_ENABLED = true;
	/** The default maximum number of discovery attempts per connect round */
	public static final int DEFAULT_DISCOVERY_MAX_ATTEMPTS = 3;
	/** The  default pattern for matching against NIC names that will be used to transmit the discovery request */
	public static final String DEFAULT_DISCOVERY_TRANSMIT_NIC = ".*";
	//=============================================
	
	public static void main(String[] args) {
		try {
			for(Field f: Configuration.class.getDeclaredFields()) {
				if(!Modifier.isStatic(f.getModifiers())) continue;
				System.out.println(f.getName() + ":" + f.get(null));
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * Retrieves the string value of a named configuration item or returns the default value if the item is not configured.
	 * @param localName The local name is typically used internally or embedded in URIs
	 * @param systemName The system name is the configuration item name used in system properties and environmental variables
	 * @param defaultValue The default value to return if the configuration value is not found in any of the expected locations
	 * @param confSource The local or internal configuration source
	 * @return the configuration item's value
	 */
	public static String getConfigurationOption(String localName, String systemName, String defaultValue, Properties confSource) {
		String value = confSource.getProperty(localName);
		if(value==null) {
			value = ConfigurationHelper.getSystemThenEnvProperty(systemName, defaultValue, confSource);
		}
		return value;
	}
	
	/**
	 * Retrieves the int value of a named configuration item or returns the default value if the item is not configured.
	 * @param localName The local name is typically used internally or embedded in URIs
	 * @param systemName The system name is the configuration item name used in system properties and environmental variables
	 * @param defaultValue The default value to return if the configuration value is not found in any of the expected locations
	 * @param confSource The local or internal configuration source
	 * @return the configuration item's value
	 */
	public static int getIntConfigurationOption(String localName, String systemName, int defaultValue, Properties confSource) {
		String value = getConfigurationOption(localName, systemName, "" + defaultValue, confSource);
		return Integer.parseInt(value);
	}
	
	/**
	 * Retrieves the long value of a named configuration item or returns the default value if the item is not configured.
	 * @param localName The local name is typically used internally or embedded in URIs
	 * @param systemName The system name is the configuration item name used in system properties and environmental variables
	 * @param defaultValue The default value to return if the configuration value is not found in any of the expected locations
	 * @param confSource The local or internal configuration source
	 * @return the configuration item's value
	 */
	public static long getLongConfigurationOption(String localName, String systemName, long defaultValue, Properties confSource) {
		String value = getConfigurationOption(localName, systemName, "" + defaultValue, confSource);
		return Long.parseLong(value);
	}
	
	/**
	 * Retrieves the boolean value of a named configuration item or returns the default value if the item is not configured.
	 * @param localName The local name is typically used internally or embedded in URIs
	 * @param systemName The system name is the configuration item name used in system properties and environmental variables
	 * @param defaultValue The default value to return if the configuration value is not found in any of the expected locations
	 * @param confSource The local or internal configuration source
	 * @return the configuration item's value
	 */
	public static boolean getBooleanConfigurationOption(String localName, String systemName, boolean defaultValue, Properties confSource) {
		String value = getConfigurationOption(localName, systemName, "" + defaultValue, confSource);
		return Boolean.parseBoolean(value);
	}
	
	
	
	
	/**
	 * Captures a snapshot of all OT connection properties which means environment variables or system properties
	 * that start with {@link CONNECTION_PREFIX}. System properties will take presedence over environmental variables.
	 * @return a properties instance with all the OT connection properties that were set
	 */
	public static Properties getAllConnectionProperties() {
		Properties p = new Properties();
		for(Map.Entry<String, String> entry: System.getenv().entrySet()) {
			if(entry.getKey().startsWith(CONNECTION_PREFIX)) {
				p.setProperty(entry.getKey(), entry.getValue());
			}
		}
		for(Map.Entry<Object, Object> entry: System.getProperties().entrySet()) {
			String key = entry.getKey().toString();
			if(key.startsWith(CONNECTION_PREFIX)) {
				p.setProperty(key, entry.getValue().toString());
			}
		}
		return p;		
	}
	
	/**
	 * Returns the maximum amount of time in ms. before a non empty trace buffer is flushed
	 * @return the maximum amount of time in ms. before a non empty trace buffer is flushed
	 */
	public static long getMaxFlushTime() {
		return ConfigurationHelper.getLongSystemThenEnvProperty(FLUSH_TIME_MAX, DEFAULT_FLUSH_TIME_MAX);
	}
	
	/**
	 * Returns the connection timeout in ms.
	 * @return the connection timeout in ms.
	 */
	public static long getConnectTimeout() {
		return ConfigurationHelper.getLongSystemThenEnvProperty(CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
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
		return ConfigurationHelper.getIntSystemThenEnvProperty(PORT, -1);
	}
	
//	/**
//	 * Returns the configured protocol
//	 * @return the configured protocol
//	 */
//	public static Protocol getProtocol() {
//		return Protocol.forValue(ConfigurationHelper.getSystemThenEnvProperty(PROTOCOL, DEFAULT_PROTOCOL));
//	}

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
	 * Returns the maximum number of discovery attempts per connect round
	 * @return the maximum number of discovery attempts per connect round
	 */
	public static int getDiscoveryMaxAttempts() {
		return ConfigurationHelper.getIntSystemThenEnvProperty(DISCOVERY_MAX_ATTEMPTS, DEFAULT_DISCOVERY_MAX_ATTEMPTS);
	}	
	
	/**
	 * Returns the configured discovery timeout in ms.
	 * @return the configured discovery timeout in ms.
	 */
	public static int getDiscoveryTimeout() {
		return ConfigurationHelper.getIntSystemThenEnvProperty(DISCOVERY_TIMEOUT, DEFAULT_DISCOVERY_TIMEOUT);
	}
	
	/**
	 * Returns the pattern that will match the NIC names to use to transmit the discovery request
	 * @return a pattern match against NIC names
	 */
	public static Pattern getDiscoveryTransmitNic() {
		String pattern = ConfigurationHelper.getSystemThenEnvProperty(DISCOVERY_TRANSMIT_NIC, DEFAULT_DISCOVERY_TRANSMIT_NIC);
		try {
			return Pattern.compile(pattern);
		} catch (Exception e) {
			return Pattern.compile(DEFAULT_DISCOVERY_TRANSMIT_NIC);
		}		
	}
	
	
	/**
	 * Returns the configured synchronous operation timeout in ms.
	 * @return the configured synchronous operation timeout in ms.
	 */
	public static long getSynchOpTimeout() {
		return ConfigurationHelper.getLongSystemThenEnvProperty(SYNCH_OP_TIMEOUT, DEFAULT_SYNCH_OP_TIMEOUT);
	}
	
	
	
	
	/**
	 * Returns the configured discovery enablement
	 * @return true if discovery is enabled, false otherwise
	 */
	public static boolean isDiscoveryEnabled() {
		return ConfigurationHelper.getBooleanSystemThenEnvProperty(DISCOVERY_ENABLED, DEFAULT_DISCOVERY_ENABLED);
	}
	

}
