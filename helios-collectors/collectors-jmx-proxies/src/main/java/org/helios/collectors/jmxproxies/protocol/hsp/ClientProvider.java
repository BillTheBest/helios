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
package org.helios.collectors.jmxproxies.protocol.hsp;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;

import org.helios.helpers.JMXHelper;

/**
 * <p>Title: ClientProvider</p>
 * <p>Description: A custom client provider that creates a JMXConnector to an internal Helios MBeanServerConnection MBean proxy</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.collectors.jmxproxies.protocol.hsp.ClientProvider</code></p>
 */

public class ClientProvider implements JMXConnectorProvider {
	/** Option Name constant for shared connections (true/false) */
	public static final String OPTION_SHARED = "shared";
	
	/** A template for Proxy ObjectNames */
	public static final String PROXY_OBJECT_NAME_TEMPLATE = "org.helios.jmx.mbeanservers:host={0},vm={1},domain={2}";
	/**
	 * Creates a new connector client that is ready to connect to the connector server at the given address. Each successful call to this method produces a different JMXConnector object.
	 * @param serviceURL The address of the connector server to connect to.
	 * @param environment a read-only Map containing named attributes to determine how the connection is made. Keys in this map must be Strings. The appropriate type of each associated value depends on the attribute. 
	 * @return a JMXConnector
	 * @throws IOException
	 */
	@Override
	public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
		int port = serviceURL.getPort();
		if(port!=0) {
			throw new IllegalArgumentException("Found port [" + port + "]. Remote relays not supported yet");
		}
		if("lhsp".equals(serviceURL.getProtocol())) {
			return new LHSPJMXConnector(serviceURL.toString().replace("service:jmx:lhsp://", ""));
		}
		String host = serviceURL.getHost();
		String[] parts = serviceURL.getURLPath().split("\\?");
		boolean shared = true;
		Map<String, Object> environmentExt = new HashMap<String, Object>(environment);
		if(parts.length>1) {
			String[] options = parts[1].split("&");
			if(options!=null && options.length>0) {
				for(String option: options) {
					String[] nvp = option.split("=");
					if(nvp!=null && nvp.length>1) {
						String name = nvp[0];
						String value = nvp[1];
						if(name!=null && name.length()>0 && value!=null && value.length() >0) {
							environmentExt.put(name.trim(), value.trim());
						}
					}						
				}
			}
			Object sharedOption = environmentExt.get(OPTION_SHARED);
			if(sharedOption!=null) {
				shared = "true".equals(sharedOption.toString());
			}
		}
		String[] vmDomain = parts[0].replaceFirst("/", "").split("/");
		String vmId = vmDomain[0];
		String domain = vmDomain[1];
		ObjectName proxyObjectName = JMXHelper.objectName(MessageFormat.format(PROXY_OBJECT_NAME_TEMPLATE, host, vmId, domain));
		return new HSPJMXConnector(JMXHelper.getHeliosMBeanServer(), proxyObjectName, environmentExt, shared);
	}
	

}
