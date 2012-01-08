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

import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.helios.nativex.sigar.HeliosSigar;

/**
 * <p>Title: NativeJMXAgent</p>
 * <p>Description: Bootstrap for the Native Agent JMX Server </p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p>org.helios.nativex.jmx.NativeJMXAgent</p>
 */
public class NativeJMXAgent {
	/**  The default JMX Domains where the MBeans will be registered */
	public static final String[] DEFAULT_DOMAIN_REGISTRATION = {"DefaultDomain"};
	/**  The default native agent refresh rate */
	public static final int DEFAULT_REFRESH_RATE = 15;
	/** The native agent singleton */
	private static volatile NativeJMXAgent instance = null;
	/** The native agent singleton ctor lock */
	private static final Object lock = new Object();
	
	/** The helios-sigar instance */
	protected final HeliosSigar agent;
	
	public static NativeJMXAgent getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new NativeJMXAgent();
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * Private ctor.
	 * Creates a new NativeJMXAgent
	 */
	private NativeJMXAgent() {
		agent = HeliosSigar.getInstance();
	}
	
	public final static String JMX_URL = "service:jmx:rmi://%1$s:%2$d/jndi/rmi://%1$s:%3$d/jmxrmi" ;
	/**
	 * Launches the Native Agent JMX Server
	 * @param args <ol>
	 * 	<li>-domains <code>&lt;comma separated JMX Domains&gt;</code>. Default is Platform Agent.</li>
	 * 	<li>-refresh <code>&lt;native agent refresh rate (s)&gt;</code>. Default is 15s. </li>
	 * </ol>
	 */
	public static void main(String[] args) {
		//CLProcessor clp = new CLProcessor(args);
		JMXServiceURL url = null;
		try {
			String host = args[0];
			int servicePort = Integer.parseInt(args[1]);
			int registryPort = Integer.parseInt(args[2]);
			java.rmi.registry.LocateRegistry.createRegistry(registryPort);
			url = new JMXServiceURL(String.format( JMX_URL, host, servicePort, registryPort ));
			JMXConnectorServer connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, ManagementFactory.getPlatformMBeanServer());			
			connectorServer.start();
			ObjectName serverObjectName = new ObjectName("org.helios.jmx.remote:service=JMXConnectorServer");
			ManagementFactory.getPlatformMBeanServer().registerMBean(connectorServer, serverObjectName);
			System.out.println("Registered JMX Connector Server at [" + url + "]");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
        org.helios.nativex.jmx.ServiceBootStrap.boot(true, null);

		
		
//		if(args.length==1) {
//			if("list".equalsIgnoreCase(args[0])) {
//				StringBuilder b = new StringBuilder("\nDetected Java Virtual Machines\n=================================================\n");
//				for(ListedVirtualMachine lvm : NativeAgentAttacher.getVMList()) {
//					b.append(lvm.toString());
//					b.append("=====================================\n");
//				}
//				System.out.println(b);				
//			} else if("server".equalsIgnoreCase(args[0])) {
//				ServiceBootStrap.boot(true);
//			}
//		} else if(args.length>1) {
//			if("install".equalsIgnoreCase(args[0])) {
//				try {
//					String[] additionalArgs = new String[args.length-2];
//					System.arraycopy(args, 2, additionalArgs, 0, args.length-2);
//					NativeAgentAttacher.install(args[1], additionalArgs);
//				} catch (Exception e) {
//					e.printStackTrace(System.err);
//					System.exit(-1); 
//				}
//			}
//		}
	}

}


/*
Groovy Script:
==============

import org.hyperic.sigar.*;

def agent = Agent.getInstance();
println agent;
//long pid = 16892;
long pid = 860;
println "PID:${pid}";
println "Description:${ProcUtil.getDescription(agent.sigar,pid)}";
procCpu = agent.getProcCpu(pid);
println "CPU:${procCpu}";
println "Args:${agent.getProcArgs(pid)}";
//println "Cred:${agent.getProcCredName(pid)}";   // can be access denied
//println "Cred:${agent.getProcCred(pid)}";
println "Exe:${agent.getProcExe(pid)}";
println "FD:${agent.getProcFd(pid)}";
println "Mem:${agent.getProcMem(pid)}";
println "Stats:${agent.getProcState(pid)}";
println "System Stats:${agent.getProcStat()}";
println "Time:${agent.getProcTime(pid)}";
println "Modules:${agent.getProcModules(pid).size()} Modules";


agent.getWhoList().each() {
    println "\tWho:${it}";
}
println "Uptime:${agent.getUptime()}";
println "TCP-MIB:${agent.getTcp()}";
println "Swap:${agent.getSwap()}";
println "ResourceLimit:${agent.getResourceLimit()}";
println "Env:${agent.getProcEnv(pid).size()} Entries.";
println "NetStat:${agent.getNetStat().dump()}";
println "NetInfo:${agent.getNetInfo()}";
agent.getNetInterfaceList().each() {
    println "\tNIC[${it}]:${agent.getNetInterfaceConfig(it)}";
}
agent.getNetInterfaceList().each() {
    //println "\tNIC[${it}]:${agent.getNetInterfaceStat(it)}";
}
println "JavaUsage:${agent.getMultiProcCpu('State.Name.eq=java')}";
//println "LoadAvg:${agent.getLoadAverage()}";




import com.sun.tools.attach.VirtualMachine;
import java.lang.management.*;

System.getProperties().each() { k,v ->
    if(k.startsWith("com.sun.management.")) {
        println "${k}:${v}";
    }
}

pid = ManagementFactory.getRuntimeMXBean().getName().split('@')[0];
println "PID:${pid}";
String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

VirtualMachine vm = VirtualMachine.attach(pid);

// get the connector address
    String connectorAddress =
        vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
 
    // no connector address, so we start the JMX agent
    if (connectorAddress == null) {
       String agent = vm.getSystemProperties().getProperty("java.home") +
           File.separator + "lib" + File.separator + "management-agent.jar";
       vm.loadAgent(agent);
 
       // agent is started, get the connector address
           connectorAddress =
                  vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
            println "Connector Address: ${connectorAddress}";
        }




return null;
*/