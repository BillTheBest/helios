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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.helpers.ClassHelper;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.RestOfArgumentsHandler;
import org.kohsuke.args4j.spi.StopOptionHandler;

/**
 * <p>Title: CLTest</p>
 * <p>Description: Small test case to iron out args4j usage</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.CLTest</code></p>
 */

public class CLTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Args4jTest");
		CommandFinder cf = new CommandFinder();
		try {
			CmdLineParser clp = new CmdLineParser(cf);
			clp.parseArgument(args);
			cf.run();
		} catch (CmdLineException e) {
			e.printStackTrace(System.err);
		}

	}
	
	public static interface AgentCommand {
		public void run();
		public String usage();
	}
	
	public static abstract class AbstractAgentCommand implements AgentCommand {
		protected String[] args = new String[]{};
		
		protected CmdLineParser clp;
		
//		public AbstractAgentCommand (String...args) throws CmdLineException {
//			this.args = args;
//			log("Created [", this.getClass().getSimpleName(), "] with options ", Arrays.toString(args));
//			clp = new CmdLineParser(this);
//			clp.parseArgument(args);
//		}
		
		public String usage() {
			return getUsage(this);
		}
	}
	
	public static class ListCommand extends AbstractAgentCommand {

		@Option(name="-e", usage="Specify an expression to filter Virtual Machines By")
		protected String expression = null;
		
		public ListCommand(String[] args) throws CmdLineException {
			this.args = args;
			log("Created [", this.getClass().getSimpleName(), "] with options ", Arrays.toString(args));
			clp = new CmdLineParser(this);
			clp.parseArgument(args);
		}

		public void run() {
			if(expression!=null) {
				log("Invoking List with Expression Filter [", expression, "]");
			} else {
				log("Invoking List with no expression");
			}
		}

		
	}
	
	public static class CommandFinder {
		public static final Map<String, Class<? extends AgentCommand>> commands = new HashMap<String, Class<? extends AgentCommand>>();
		
		@Argument(index=0, usage="Specify the name of a command")
		protected String command = null;
		
		@Argument(index=1) //, handler=RestOfArgumentsHandler.class, multiValued=true, usage="The options to pass to the specified command")
		@Option(name="-opts", handler=StopOptionHandler.class)
		protected List<String> args = new ArrayList<String>();
		
		static {
			commands.put("list", ListCommand.class);
		}
		
		public void run() {
			try {
				Class<? extends AgentCommand> commandClass = commands.get(command==null ? "help" : command.toLowerCase().trim());
				if(commandClass!=null) {
					Constructor<? extends AgentCommand> ctor = commandClass.getDeclaredConstructor(String[].class);
					AgentCommand agentCommand = ctor.newInstance(new Object[]{args.toArray(new String[args.size()])});
					agentCommand.run();
				} else {
					log("Print Banner and Help");
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	public static void log(Object...msgs) {
		StringBuilder b = new StringBuilder("[CLTest]");
		if(msgs!=null) {
			for(Object msg: msgs) {
				if(msg!=null) {
					b.append(msg.toString());
				}
			}
		}
		System.out.println(b.toString());
	}
	
	
	/** Usage string cache keyed by parsed class */
	private static final Map<String, String> usageCache = new ConcurrentHashMap<String, String>();
	
	/**
	 * Returns the usage string for the passed bean
	 * @param bean The bean to get the usage for
	 * @return The usage string for the passed bean
	 */
	public static String getUsage(Object bean) {
		ClassHelper.nvl(bean, "Passed Bean Was Null");
		String className = bean.getClass().getName();
		String usage = usageCache.get(className);
		if(usage==null) {
			synchronized(usageCache) {
				usage = usageCache.get(className);
				if(usage==null) {
					CmdLineParser clp = new CmdLineParser(bean);
					ByteArrayOutputStream baos = null;
					try {
						baos = new ByteArrayOutputStream();
						clp.printUsage(baos);			
						usage = baos.toString();
					} finally {
						try { baos.close(); } catch (Exception e) {}
					}				
				}
			}
		}
		return usage;
	}
	
}

