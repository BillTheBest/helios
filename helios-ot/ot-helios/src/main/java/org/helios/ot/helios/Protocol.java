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

/**
 * <p>Title: Protocol</p>
 * <p>Description: Enumerates the available protocols the helios endpoint can use to comm with the OT server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.Protocol</code></p>
 */

public enum Protocol {
	/** TCP/IP */
	TCP(9428, new ConnectorFactory(){public AbstractEndpointConnector createConnector(@SuppressWarnings("rawtypes") HeliosEndpoint endpoint){return new HeliosEndpointTCPConnector(endpoint);}}),
	/** UDP */
	UDP(9427, new ConnectorFactory(){public AbstractEndpointConnector createConnector(@SuppressWarnings("rawtypes") HeliosEndpoint endpoint){return new HeliosEndpointUDPConnector(endpoint);}});
	
	private Protocol(int defaultPort, ConnectorFactory factory) {
		this.factory = factory;
		this.defaultPort = defaultPort;
	}
	
	private final ConnectorFactory factory;
	private final int defaultPort;
	
	/**
	 * Creates a connector of this protocol's type for the passed endpoint
	 * @param The endpoint to create the connector for
	 * @return a new connector
	 */
	public AbstractEndpointConnector createConnector(@SuppressWarnings("rawtypes") HeliosEndpoint endpoint) {
		return factory.createConnector(endpoint);
	}
	
	/**
	 * Returns the default listening port for this protocol
	 * @return the default listening port for this protocol
	 */
	public int getDefaultPort() {
		return defaultPort;
	}
	
	
	/**
	 * Decodes the passed string into a protocol using trim and uppercase
	 * @param value the value to decode
	 * @return the decoded protocol
	 */
	public static Protocol forValue(CharSequence value) {
		if(value==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
		try {
			return Protocol.valueOf(value.toString().toUpperCase().trim());
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed value was an invalid Protocol name [" + value + "]", new Throwable());
		}
	}
	
	/**
	 * <p>Title: ConnectorFactory</p>
	 * <p>Description: Defines the connector factories that create connectors associated with each supported protocol</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.ot.helios.Protocol.ConnectorFactory</code></p>
	 */
	public static interface ConnectorFactory {
		/**
		 * Creates a new connector
		 * @param endpoint The endpoint to create the connector for
		 * @return a new connector
		 */
		@SuppressWarnings("rawtypes")
		public AbstractEndpointConnector createConnector(HeliosEndpoint endpoint);
	}
	
	
	
}
