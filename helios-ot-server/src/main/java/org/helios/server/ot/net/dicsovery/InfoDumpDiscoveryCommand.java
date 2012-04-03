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
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.netty.NettyEndpoint;
import org.eclipse.jetty.webapp.WebAppContext;
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
	public String execute(String fullCommandString, ApplicationContext ctx) {
		StringBuilder b = new StringBuilder();
		b.append("Version:").append(ctx.getClass().getPackage().getImplementationVersion()).append("\n");
		b.append("Spring Version:").append(ApplicationContext.class.getPackage().getImplementationVersion()).append("\n");
		b.append("Deployed Beans:").append(ctx.getBeanDefinitionCount()).append("\n");
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		b.append("JVM:").append(runtimeMXBean.getVmVendor()).append("").append(runtimeMXBean.getVmName()).append(" ").append(runtimeMXBean.getVmVersion()).append("\n");
		b.append("Process ID:").append(runtimeMXBean.getName().split("@")[0]).append("\n");
		b.append("Host:").append(runtimeMXBean.getName().split("@")[1]).append("\n");
		b.append("Start Time:").append(new Date(runtimeMXBean.getStartTime())).append("\n");
		b.append("Up Time:").append(TimeUnit.MINUTES.convert(runtimeMXBean.getStartTime(), TimeUnit.MILLISECONDS)).append("");
		b.append("\nHelios OT Remote Endpoints:");
		
		for(Endpoint ep :ctx.getBean("HeliosContext", CamelContext.class).getEndpoints()) {
			if(ep instanceof NettyEndpoint) {
				b.append("\n\t").append(ep.getEndpointUri());
			}
		}
		b.append("\nWebApp Endpoints:");
		for(WebAppContext webApp: ctx.getBeansOfType(WebAppContext.class).values()) {
			b.append("\n\t").append(webApp.getDisplayName()).append(":").append(webApp.getContextPath());
		}
		b.append("\n");
		return b.toString();
	}

}
