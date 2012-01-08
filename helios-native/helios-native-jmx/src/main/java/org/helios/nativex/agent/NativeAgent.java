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
import java.net.URL;

/**
 * <p>Title: NativeAgent</p>
 * <p>Description: The JavaAgent to bootstrap the agent or inject using attach. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.agent.NativeAgent</code></p>
 */

public class NativeAgent {
	
	/** The agent bootstrap class name */
	public static final String BOOT_CLASS = "org.helios.nativex.agent.NativeAgentBoot";
	/** The agent bootstrap debug sysprop name */
	public static final String BOOT_DEBUG_PROP = "org.helios.nativex.agent.debug";	
	/** The agent bootstrap debug that specifies the verbosity of simple system loggers */
	static boolean BOOT_DEBUG = System.getProperty(BOOT_DEBUG_PROP, "no").trim().equalsIgnoreCase("true");
	

	/**
	 * Acquires the singleton instance of the Native Agent
	 * @param agentArgs The agent arguments
	 * @param inst The JVM's Instrumentation instance
	 * @return the NativeAgent singleton instance
	 */
	private static void getInstance(String agentArgs, Instrumentation inst) {
		ClassLoader defaultCl = Thread.currentThread().getContextClassLoader();
		try {
			if(agentArgs!=null && agentArgs.contains("-debug")) {
				BOOT_DEBUG = true;
				System.out.println("Helios Coronal Native JMX Agent:  DEBUG enabled");
			}
			URL agentLibURL = NativeAgent.class.getProtectionDomain().getCodeSource().getLocation();
			ClassLoader ucl = new IsolatedArchiveLoader(new URL[]{agentLibURL}); //new URLClassLoader(new URL[]{NativeAgent.class.getProtectionDomain().getCodeSource().getLocation()}, new IsolatedArchiveLoader());
			Thread.currentThread().setContextClassLoader(ucl);
			Class.forName("org.helios.jmx.platform.PlatformMBeanServerNullDomainPatch", true, ucl);
			Class.forName(BOOT_CLASS, true, ucl).getDeclaredConstructor(String.class, Instrumentation.class).newInstance(agentArgs, inst);
			System.setProperty(BOOT_CLASS.toLowerCase(), agentLibURL.toString());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			Thread.currentThread().setContextClassLoader(defaultCl);
		}
	}
	

	/**
	 * The pre-main entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		getInstance(agentArgs, inst);
	}
	
	/**
	 * The pre-main entry point for JVMs not supporting a <b><code>java.lang.instrument.Instrumentation</code></b> implementation.
	 * @param agentArgs The agent bootstrap arguments
	 */	
	public static void premain(String agentArgs) {
		getInstance(agentArgs, null);
	}
	
	/**
	 * The agent attach entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		getInstance(agentArgs, inst);
	}
	
	/**
	 * The agent attach entry point for JVMs not supporting a <b><code>java.lang.instrument.Instrumentation</code></b> implementation.
	 * @param agentArgs The agent bootstrap arguments
	 */
	public static void agentmain(String agentArgs) {
		getInstance(agentArgs, null);
	}
	
}
