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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXServiceURL;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.netty.NettyEndpoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.helios.helpers.Banner;
import org.helios.helpers.InetAddressHelper;
import org.springframework.context.ApplicationContext;

/**
 * <p>Title: InfoDumpDiscoveryCommand</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.net.dicsovery.InfoDumpDiscoveryCommand</code></p>
 */
public class InfoDumpDiscoveryCommand implements IDiscoveryCommand {

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.net.dicsovery.IDiscoveryCommand#getCommandName()
	 */
	@Override
	public String getCommandName() {
		return "INFO";
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.net.dicsovery.IDiscoveryCommand#execute(java.lang.String, org.springframework.context.ApplicationContext)
	 */
	@Override
	public String execute(String[] fullCommandString, ApplicationContext ctx) {
		String version = ctx.getClass().getPackage().getImplementationVersion();
		Map<String, String> data = new LinkedHashMap<String, String>();
		data.put("Version", version==null ? "Crazy Unstable Dev Build 0.00001" : version);
		data.put("Spring Version", ApplicationContext.class.getPackage().getImplementationVersion());
		data.put("Deployed Bean Count", "" + ctx.getBeanDefinitionCount());		
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		data.put("JVM", new StringBuilder(runtimeMXBean.getVmVendor()).append("").append(runtimeMXBean.getVmName()).append(" ").append(runtimeMXBean.getVmVersion()).toString());
		data.put("PID", runtimeMXBean.getName().split("@")[0]);
		data.put("Host", runtimeMXBean.getName().split("@")[1]);
		data.put("Start Time", new Date(runtimeMXBean.getStartTime()).toString());
		data.put("Up Time", "" + TimeUnit.MINUTES.convert(runtimeMXBean.getUptime(), TimeUnit.MILLISECONDS) + " minutes");
		data.put("Helios OT Remote Endpoints", "");
		for(Endpoint ep :ctx.getBean("HeliosContext", CamelContext.class).getEndpoints()) {
			if(ep instanceof NettyEndpoint) {
				try {
					URI uri = new URI(ep.getEndpointUri());
					data.put(uri.getScheme(), uri.toString());
				} catch (Exception e) {
					data.put(ep.getEndpointKey(), ep.getEndpointUri());
				}
			}
		}
		data.put("ENDOF|Helios OT Remote Endpoints", "");
		data.put("WebApp Endpoints", "");
		for(WebAppContext webApp: ctx.getBeansOfType(WebAppContext.class).values()) {
			for(Connector ct: webApp.getServer().getConnectors()) {
				StringBuilder webapp = new StringBuilder();								
				webapp.append(ct.getConfidentialPort()==0 ? "http://" : "https://");
				webapp.append(ct.getHost().equalsIgnoreCase("0.0.0.0") ? InetAddressHelper.hostName() : ct.getHost());
				webapp.append(":").append(ct.getPort()).append(webApp.getContextPath());
				data.put(webApp.getDisplayName(), webapp.toString());
			}			
		}
		data.put("ENDOF|WebApp Endpoints", "");
		data.put("JMX Connector URLs", "");
		for(JMXServiceURL svc: ctx.getBeansOfType(JMXServiceURL.class).values()) {
			data.put(svc.getProtocol(), svc.toString());
		}
		data.put("ENDOF|JMX Connector URLs", "");
		if(fullCommandString.length>2) {
			return InfoFormat.getInstance(fullCommandString[2]).format(data);
		} else {
			return InfoFormat.TEXT.format(data);
		}
	}

}
