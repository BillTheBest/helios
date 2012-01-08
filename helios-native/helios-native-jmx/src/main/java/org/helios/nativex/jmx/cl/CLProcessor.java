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

import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


/**
 * <p>Title: CLProcessor</p>
 * <p>Description: The NativeJMXAgent command line processor. Targets:<ul>
 * <li><b>server</b>: Starts a standalone monitor server</li>
 * <li><b>list</b>: Prints a list of instrumentation enabled JVMs on this host</li>
 * <li><b>attach</b>: Installs the native jmx agent into a target JVM</li>
 * <li><b></b>:</li>
 * <li><b></b>:</li>
 * <li><b>default (no args)</b>: Product banner and a brief native summary</li>
 * </ul>
 * </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.cl.CLProcessor</code></p>
 */

public class CLProcessor {
	/** The primary commands that can be executed at the command line */
	public static final String commands = "server, list, install, help";
	/** The primary command line usage */
	public static final String commandUsage = "<command name> <command options> where the command name is one of " + commands;
	/** The actual command implementations keyed by command name */
	static final Map<String, AgentCommand> commandMap = new HashMap<String, AgentCommand>(commands.split(",").length+1);
	/** the banner */
	public static final String BANNER = "Helios Coronal Native JMX Agent, v." + getVersion();
	
	
	/**
	 * Returns the package version.
	 * @return the package version or <b>InDev</b> if not running from a built package.
	 */
	public static String getVersion() {
		String v = CLProcessor.class.getPackage().getImplementationVersion();
		if(v==null || v.length()<1) {
			v = "InDev";
		}
		return v;
	}
	
	static {		
		commandMap.put("help", new MainHelpCommand());
		commandMap.put("list", new ListCommand());
		commandMap.put("server", new ServerCommand());
		commandMap.put("install", new InstallCommand());
	}
	
	/** The validated command */
	protected AgentCommand command = null;
	
	/**
	 * Creates a new CLProcessor
	 * @param args The command line supplied parameters
	 */
	public CLProcessor(String...args) {
		AgentCommand agentCommand = null;
		log("\n\n" + BANNER);
		if(args==null || args.length <1) {
			// No command supplied. Print banner and usage.
			elog("No command supplied. Usage:");
			log("\t" + commandUsage);
			log("\n\n");
		} else {
			agentCommand = commandMap.get(args[0].toLowerCase().trim());
			if(agentCommand==null) {
				// Unrecognized command supplied. Print error, banner and usage
				elog("Invalid command [" + args[0].toLowerCase().trim() + "]. Usage:");
				log("\t" + commandUsage);
				log("\n\n");
			} else {
				// Recognized command. Execute it.
				log("Running Command [" + args[0].toLowerCase().trim() + "]");
				int remainderLength = args.length-1;
				String[] remainder = new String[remainderLength];
				System.arraycopy(args, 1, remainder, 0, remainderLength);
				try {
					CmdLineParser clp = new CmdLineParser(agentCommand);
					clp.parseArgument(remainder);
					agentCommand.run();
				} catch (CmdLineException cle) {
					elog("Command Failure For [" + args[0].toLowerCase().trim() + "]");
					elog(cle.getMessage());
					//log("Usage for [" + args[0].toLowerCase().trim() + "]\n" + agentCommand.usage());
				}
			}
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public static void elog(Object msg) {
		System.err.println(msg);
	}
	
	
	/**
	 * Runs the validated command
	 */
	public void run(String...args) throws CmdLineException {
		if(command==null) {
			command = commandMap.get("help");			
		}
		new CmdLineParser(command).parseArgument(args);
		command.run(args);		
	}
	
	/**
	 * The main entry point
	 * @param args The command line arguments.
	 */
	public static void main(String [] args) {
		CLProcessor clp = new CLProcessor(args);
	}
	
	
	
}
