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
package org.helios.nativex.jmx;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.nativex.agent.AgentCommandProcessor;
import org.helios.nativex.jmx.cpu.CPUUtilizationService;
import org.helios.nativex.jmx.cpu.SystemCPUUtilizationService;
import org.helios.nativex.jmx.disk.FileSystemService;
import org.helios.nativex.jmx.memory.SystemMemoryService;
import org.helios.nativex.jmx.net.NetworkInterfaceService;
import org.helios.nativex.jmx.net.tcp.TCPStateService;
import org.helios.nativex.jmx.netroutes.NetRoutesWatcherService;
import org.helios.nativex.jmx.process.ProcessService;
import org.helios.nativex.jmx.resources.ResourceLimitService;
import org.helios.nativex.jmx.swap.SwapSpaceService;
import org.helios.nativex.jmx.who.WhoWatcherService;
import org.helios.nativex.sigar.HeliosSigar;

/**
 * <p>Title: ServiceBootStrap</p>
 * <p>Description: Utility class to bootstrap all native monitor services.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.ServiceBootStrap</code></p>
 */

public class ServiceBootStrap {
	/** Static class logger */	
	protected static final Logger LOG = Logger.getLogger(ServiceBootStrap.class);
	
	/**
	 * Starts all native monitor services. 
	 * @param args If the first argument is case-ins <b><code>join</code></b>, the main thread will join indefinitely, keeping the services running.
	 */
	public static void main(String[] args) {
		boolean join = true;
		if(args.length>0) {
			join = ("join".equalsIgnoreCase(args[0].trim()));
		}
		boot(join, null);
		
	}
	
	/**
	 * Bootstraps all the native monitor services.
	 * @param join If true, the main thread will join indefinitely, keeping the services running.
	 * @param commandProcessor The agent args command processor. Ignored if null
	 */
	public static void boot(boolean join, AgentCommandProcessor commandProcessor) {
		LOG.info("Starting default Helios Coronal ServiceBootStrap\n\tJoin:" + join);
		if(commandProcessor!=null) {
			String[] jmxDomains = commandProcessor.getJmxDomains();
			if(jmxDomains!=null) {
				LOG.info("Will attempt to register Helios Coronal MBeans in these JMX Domains:" + Arrays.toString(jmxDomains));
			}
		} else {
			LOG.info("Helios Coronal Native MBeans will be registered in JMX Domain [" + JMXHelper.getHeliosMBeanServer().getDefaultDomain() + "]");
		}
		
		ProcessService.boot();
		SwapSpaceService.boot();
		FileSystemService.boot();
		SystemMemoryService.boot();
		CPUUtilizationService.boot();
		SystemCPUUtilizationService.boot();
		NetworkInterfaceService.boot();
		// Temp patch for Solaris where TCPState is not implemented.
		if(!ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase().contains("solaris")) {
			TCPStateService.boot();
		}		
		ResourceLimitService.boot();
		NetRoutesWatcherService.boot();
		WhoWatcherService.boot();
		LOG.info("Helios Coronal Native JMX Agent Launched. PID:" + HeliosSigar.getInstance().getPid());
		if(join) {
			try { Thread.currentThread().join(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Stops the collection scheduler and unregisters all the MBeans.
	 */
	public static void shutdown() {
		AbstractNativeCounter.scheduler.shutdownNow();
		for(ObjectName on: JMXHelper.getHeliosMBeanServer().queryNames(JMXHelper.objectName("org.helios.nativex.jmx*:*"), null)) {
			try {
				JMXHelper.getHeliosMBeanServer().unregisterMBean(on);
				LOG.info("Unregistered [" + on + "]");
			} catch (Exception e) {}
		}
		try {
			JMXHelper.getHeliosMBeanServer().unregisterMBean(ManagedObjectDynamicMBean.DEFAULT_NOTIFICATION_BROADCASTER_TP_ON);
		} catch (Exception e ) {}
	}

}
