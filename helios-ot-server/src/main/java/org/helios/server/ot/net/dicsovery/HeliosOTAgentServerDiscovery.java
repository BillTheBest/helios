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
package org.helios.server.ot.net.dicsovery;

import java.net.URI;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.netty.NettyEndpoint;
import org.helios.helpers.InetAddressHelper;
import org.springframework.context.ApplicationContext;

/**
 * <p>Title: HeliosOTAgentServerDiscovery</p>
 * <p>Description: Discovery service to send a broadcasting agent the endpoints for connection.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.net.dicsovery.HeliosOTAgentServerDiscovery</code></p>
 */
public class HeliosOTAgentServerDiscovery implements IDiscoveryCommand {

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.net.dicsovery.IDiscoveryCommand#getCommandName()
	 */
	@Override
	public String getCommandName() {
		return "DISCOVER";
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.net.dicsovery.IDiscoveryCommand#execute(java.lang.String[], org.springframework.context.ApplicationContext)
	 */
	@Override
	public String execute(String[] fullCommandString, ApplicationContext ctx) {
		String preferred = null;
		if(fullCommandString.length>2) {
			preferred = fullCommandString[2].trim().toUpperCase();
			if(preferred.isEmpty()) {
				preferred = null;
			}
		}
		
		Endpoint selectedEndpoint = null;
		for(Endpoint ep :ctx.getBean("HeliosContext", CamelContext.class).getEndpoints()) {
			if(!(ep instanceof NettyEndpoint)) continue;
			if(selectedEndpoint==null) {
				selectedEndpoint = ep;
				if(preferred==null) break;
			}
			try {
				URI uri = new URI(ep.getEndpointUri());
				if(uri.getScheme().toUpperCase().trim().equals(preferred)) {
					selectedEndpoint = ep;
					break;
				}					
			} catch (Exception e) {}
		}
		if(selectedEndpoint==null) {
			return "none";
		} else {
			try {
				String finalUri = selectedEndpoint.getEndpointUri();
				URI uri = new URI(finalUri);
				if("0.0.0.0".equalsIgnoreCase(uri.getHost())) {
					return finalUri.replace("0.0.0.0", InetAddressHelper.hostName());					
				} else {
					return finalUri;
				}
			} catch (Exception e) {
				// this should NEVER happen
				throw new RuntimeException("Failed to render selected endpoint [" + selectedEndpoint.getEndpointUri() + "]", e);
			}
		}
	}

}
