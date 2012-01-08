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
package org.helios.nativex.jmx.cl;

import org.helios.nativex.agent.NativeAgent;
import org.helios.nativex.agent.NativeAgentAttacher;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

/**
 * <p>Title: InstallCommand</p>
 * <p>Description: Command Line command for installing the helios or jmx agent into a running JVM.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.cl.InstallCommand</code></p>
 */

public class InstallCommand implements AgentCommand {
	@Option(name="-pid", usage="Specifies the Process ID of the JVM to install to.")
	protected Long targetPid;
	@Option(name="-type", usage="Specifies which agent to install.\n\thelios: The Helios Native JMX Agent\n\tjmx: The standard com.sun JMX agent\n\tDefaults to helios.")
	protected String agentType = "helios";   // jmx or helios
	@Option(name="-debug", usage="Enables verbose output in the install process")
	protected boolean debug = false;
	@Option(name="-agentArgs", usage="Specifies the arguments to be passed to the installed agent.\ne.g.: -agentArgs \"-jmxdomains jboss\"")
	protected String agentArgs = null;
	
	/**
	 * Executes the install command
	 * @param args The command line arguments
	 * @throws CmdLineException
	 */
	@Override
	public void run(String...args) throws CmdLineException {
		if(targetPid==null) {
			CLProcessor.elog("No PID provided. Usage:");
			CLProcessor.elog(usage());
			return;
		}
		if("helios".equals(agentType.toLowerCase())) {
			if(debug) {
				CLProcessor.log("Enabling Verbose Logging In Install");
				System.setProperty(NativeAgent.BOOT_DEBUG_PROP, "true");
			}
			installHelios(debug, agentArgs);
		} else if("jmx".equals(agentType.toLowerCase())) {
			// install jmx agent.
			throw new UnsupportedOperationException("jmx agent install not implemented yet");
		} else {
			CLProcessor.elog("Unrecognized agent type [" + agentType + "]. Usage:");
			CLProcessor.elog(usage());
		}
		
//		try {
//		} catch (Exception e) {
//			CLProcessor.elog("Failed to install Helios Coronal Native JMX Agent into JVM with PID [" + targetPid + "]:" + e);
//			e.printStackTrace(System.err);
//		}
	}
	
	/**
	 * Installs the Helios Coronal Native JMX Agent
	 * @param debug Enabled verbose logging in the install
	 */
	protected void installHelios(boolean debug, String args) {
		try {
			CLProcessor.log("Installing Helios Coronal Native JMX Agent into JVM with PID [" + targetPid + "].....");
			if(NativeAgentAttacher.install("" + targetPid, debug, args)) {
				CLProcessor.log("Successfully installed Helios Coronal Native JMX Agent into JVM with PID [" + targetPid + "].....");
			} else {
				CLProcessor.log("The helios agent is already installed in the JVM with PID [" + targetPid + "]");
			}
		} catch (Exception e) {
			CLProcessor.elog("Failed to install Helios Coronal Native JMX Agent into JVM with PID [" + targetPid + "]:" + e);
			e.printStackTrace(System.err);
		}			
	}

	/**
	 * Returns the usage for this command
	 * @return
	 */
	@Override
	public String usage() {
		return CmdLineParserHelper.getUsage(this);
	}

}
