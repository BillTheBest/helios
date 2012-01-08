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
package org.helios.nativex.agent;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.helios.helpers.JMXHelper;
import org.helios.jmx.aliases.AliasMBeanRegistry;
import org.helios.nativex.agent.AgentCommandProcessor.CrossRegister;
import org.kohsuke.args4j.CmdLineParser;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * <p>Title: NativeAgentBoot</p>
 * <p>Description: The actual agent impl. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.agent.NativeAgentBoot</code></p>
 */

public class NativeAgentBoot {
	/** The singleton instance */
	private static volatile NativeAgent agent = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Flag indicating that the agent has been initialized through a formal agent entry point */
	private static final AtomicBoolean agentInit = new AtomicBoolean(false);
	/** The agent arguments */
	protected final String agentArgs;
	/** The JVM's Instrumentation instance */
	protected final Instrumentation inst;
	
	/** The service bootstrap class name */
	public static final String BOOT_CLASS = "org.helios.nativex.jmx.ServiceBootStrap";
	/** The class name for the log4j DOMConfigurator */
	public static final String DOM_CONFIGURATOR = "org.apache.log4j.xml.DOMConfigurator";
	/** The class name for the log4j LogManager */
	public static final String LOG_MANAGER = "org.apache.log4j.LogManager";
	
	/**
	 * Creates a new NativeAgentBoot
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public NativeAgentBoot(String agentArgs, Instrumentation inst) {
		this.agentArgs = agentArgs;
		this.inst = inst;
		AgentCommandProcessor acp = new AgentCommandProcessor();
		CmdLineParser parser = new CmdLineParser(acp);
		try {
			parser.parseArgument(agentArgs.split("\\s+"));
		} catch (Exception e) {
			System.err.println("Invalid Agent Arguments. Valid Usage:\n");
			parser.printUsage(System.err);
		}
		NativeAgent.BOOT_DEBUG = acp.isDebug();
		
		try {									
			configureLogging(this.getClass().getClassLoader());
			System.out.println(String.format("\n\t[%s]Loading [%s]: \n\tClassLoader:[%s]\n\tAgent Args:[%s]\n", Thread.currentThread().toString(), NativeAgent.class.getName(), this.getClass().getClassLoader().toString(), agentArgs));
			crossDomainRegistration(acp);
			Class.forName(BOOT_CLASS).getDeclaredMethod("boot", boolean.class, AgentCommandProcessor.class).invoke(null, false, acp);				
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}		
	}
	
	
	/**
	 * Preps the AliasMBean registry for cross JMX domain registration
	 * @param acp The agent command processor that contains the names of the domains to cross-register with
	 */
	protected void crossDomainRegistration(AgentCommandProcessor acp) {
		String[] jmxDomains = acp.getJmxDomains();
		MBeanServer thisDomainServer = JMXHelper.getHeliosMBeanServer();
		// First register the future filters with the derived AliasMBean registries
		// which will cause new MBeans registered in the target MBeanServers to be
		// cross registered in the local MBeanServer
		if(jmxDomains!=null && jmxDomains.length > 0) {
			for(String domain: jmxDomains) {
				if(domain==null) continue;
				if("*".equals(domain.trim())) {
					for(MBeanServer server: MBeanServerFactory.findMBeanServer(null)) {
						try {
							if(!server.getDefaultDomain().equals(thisDomainServer.getDefaultDomain())) {
								AliasMBeanRegistry.getInstance(server, thisDomainServer).registerDynamicRegistrationFilter(JMXHelper.ALL_MBEANS_FILTER);
							}
						} catch (Exception e) {}
					}
					break;
				}
				MBeanServer server = JMXHelper.getLocalMBeanServer(true, domain);
				if(server!=null) {
					AliasMBeanRegistry.getInstance(thisDomainServer, server).registerDynamicRegistrationFilter(JMXHelper.ALL_MBEANS_FILTER);
				}
			}			
		}
		// Second, process specific/procedural cross-registers of MBeans that are already registered 
		for(CrossRegister xreg: acp.crossRegisters) {
			MBeanServer local = JMXHelper.getLocalMBeanServer(true, xreg.getLocalDomain());
			MBeanServer target = JMXHelper.getLocalMBeanServer(true, xreg.getTargetDomain());
			if(local==null) {
				System.err.println("Failed to process JMX CrossRegister due to irresolvable local domain [" + xreg.getLocalDomain() + "]");
			}
			if(target==null) {
				System.err.println("Failed to process JMX CrossRegister due to irresolvable target domain [" + xreg.getTargetDomain() + "]");
			}
			try {
				AliasMBeanRegistry.getInstance(target, local).crossRegister(local, target, JMXHelper.objectName(xreg.getFilter()));
			} catch (Exception e) {
				System.err.println("Failed to process JMX CrossRegister:" + xreg + "\nStack Trace Follows");
				e.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * Acquires the top most classloader that can be resolved
	 * @return the top classloader
	 */
	protected ClassLoader getTopClassLoader() {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		while(true) {
			if(cl.getParent()==null) return cl;
			cl = cl.getParent();
		}
	}
	
//	/**
//	 * Acquires the singleton instance of the Native Agent
//	 * @param agentArgs The agent arguments
//	 * @param inst The JVM's Instrumentation instance
//	 * @return the NativeAgent singleton instance
//	 */
//	private static NativeAgent getInstance(String agentArgs, Instrumentation inst) {
//		if(agent==null) {
//			synchronized (lock) {
//				if(agent==null) {					
//					ClassLoader defaultCl = Thread.currentThread().getContextClassLoader();					
//					try {
//						URL rtUrl = new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar").toURI().toURL();
//						URLClassLoader ucl = new URLClassLoader(new URL[]{NativeAgent.class.getProtectionDomain().getCodeSource().getLocation(), rtUrl}, null);
//						Thread.currentThread().setContextClassLoader(ucl);
//						//agent = new NativeAgent(agentArgs, inst);
//						agentInit.set(true);
//					} catch (Exception e) {
//						e.printStackTrace(System.err);
//					} finally {
//						Thread.currentThread().setContextClassLoader(defaultCl);
//					}
//				}
//			}
//		}		
//		return agent;
//	}
	
	
	
	

	/**
	 * Configures the default logging for the agent.
	 * @param cl The agent's classloader
	 */
	protected void configureLogging(ClassLoader cl) {
		try {
//			Object rootLogger = cl.loadClass(LOG_MANAGER).getDeclaredMethod("getRootLogger").invoke(null);			
//			System.out.println("Root Logger Loaded By:" + rootLogger.getClass().getClassLoader());
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Element loggingElement = documentBuilder.parse(new InputSource(this.getClass().getClassLoader().getResourceAsStream("logging/helios-log4j.xml"))).getDocumentElement();
			Class.forName(DOM_CONFIGURATOR, true, cl).getDeclaredMethod("configure", Element.class).invoke(null, loggingElement);			
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	

	

}
