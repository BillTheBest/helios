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
package org.helios.espercl.commands;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.espercl.ConsoleOps;
import org.helios.helpers.ClassHelper;

/**
 * <p>Title: CommandMapper</p>
 * <p>Description: Extracts and indexes the @Command and @CommandParameter annotations in a class
 * and implements the execution of a command stream against an indexed instance.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.commands.CommandMapper</code></p>
 */

public class CommandMapper {
	/** The catalog of primary command names (without aliases) */
	protected final Set<String> primaryCommands = new CopyOnWriteArraySet<String>();
	/** The catalog of command implementations keyed by the command name and alias */
	protected final Map<String, CommandImpl> commands = new ConcurrentHashMap<String, CommandImpl>();
	/** The catalog of command implementations targets keyed by the command name and alias */
	protected final Map<String, Object> targets = new ConcurrentHashMap<String, Object>();
	
	/**
	 * Creates a new CommandMapper and registers itself as the help provider. 
	 */
	public CommandMapper() {
		this.mapCommands(this);
	}
	
	
	/**
	 * Inspects a class and returns a map of introspected <code>CommandImpl</code>s found in the class.
	 * @param clazz the class to inspect
	 * @return a map of introspected <code>CommandImpl</code>s found in the class
	 */
	public void mapCommands(Class<?> clazz) {
		if(clazz==null) throw new RuntimeException("mapCommands was passed a null class");
		for(Method m: ClassHelper.getAnnotatedMethods(clazz, Command.class, true)) {
			CommandImpl cmd = new CommandImpl(m);
			if(!cmd.isStatic()) {
				CommandImpl.elog("Skipping non-static command [", cmd.getCommandName(), "] from static deployment of [", clazz.getName(), "]");
				continue;
			}
			Set<String> keys = new HashSet<String>(Arrays.asList(cmd.getAliases()));
			keys.add(cmd.getCommandName());
			primaryCommands.add(cmd.getCommandName());
			for(String key: keys) {			
				if(commands.containsKey(key)) {
					throw new RuntimeException(dupCmdErr(cmd));
				} else {
					synchronized(commands) {
						if(commands.containsKey(key)) {
							throw new RuntimeException(dupCmdErr(cmd));
						} else {
							commands.put(key, cmd);
						}
					}				
				}
			}
		}
	}
	/**
	 * With a null parameter, prints a list of command.
	 * If one parameter is passed, it is assumed to be a command name, in which case
	 * the details of that command are printed.
	 * @param command An optional command name
	 */
	@Command(name="help", aliases={"h"}, description="Prints help", params={
			@CommandParameter(optional=true, description="The name of a command to print help for")
	})
	public void help(String command) {
		if(command==null || command.length()<1) {
			StringBuilder b = new StringBuilder("Command List:");
			for(String primaryName: primaryCommands) {
				CommandImpl ci = commands.get(primaryName);
				b.append("\n\t").append(ci.getCommandName());
				if(ci.getAliases().length>0) {
					b.append("(");
					for(String alias: ci.getAliases()) {
						b.append(alias).append(",");
					}
					b.deleteCharAt(b.length()-1);
					b.append(")");
				}
				b.append(" : ").append(ci.getDescription());
			}
			CommandImpl.log(b);
		} else {
			StringBuilder b = new StringBuilder("Command [");
			b.append(command).append("]");
			CommandImpl ci = commands.get(command);
			if(ci==null) {
				b.append("\n\tCommand [").append(command).append("] not recognized.\n\tType 'help' to a full list of commands");
			} else {
				b.append(ci.getFullHelpText());
			}			
			CommandImpl.log(b);			
		}
	}
	
	/**
	 * Inspects a class and returns a map of introspected <code>CommandImpl</code>s found in the class.
	 * @param clazz the class to inspect
	 * @return a map of introspected <code>CommandImpl</code>s found in the class
	 */
	public void mapCommands(Object target) {
		if(target==null) throw new RuntimeException("mapCommands was passed a null object");
		Class<?> clazz = target.getClass();
		if(clazz==null) return;
		for(Method m: ClassHelper.getAnnotatedMethods(clazz, Command.class, true)) {
			CommandImpl cmd = new CommandImpl(m);	
			Set<String> keys = new HashSet<String>(Arrays.asList(cmd.getAliases()));
			keys.add(cmd.getCommandName());
			primaryCommands.add(cmd.getCommandName());
			for(String key: keys) {
				if(commands.containsKey(key)) {
					throw new RuntimeException(dupCmdErr(cmd));
				} else {
					synchronized(commands) {
						if(commands.containsKey(key)) {
							throw new RuntimeException(dupCmdErr(cmd));
						} else {
							commands.put(key, cmd);
							targets.put(key, target);
						}
					}				
				}
			}
		}
	}
	
	
	/**
	 * Invokes the named command
	 * @param commandName the command name
	 * @param args the command arguments
	 * @return the result of the invocation
	 */
	public Object invoke(String commandName, String...args) {
		try {
			CommandImpl cmd = commands.get(commandName);		
			if(cmd==null) {
				ConsoleOps.err("Unrecognized command= [", commandName, "]. Type 'help' for a list of commands");
				return null;
			} else {
				Object target = targets.get(commandName);
				if(target==null && !cmd.isStatic()) {
					ConsoleOps.err("No registered target object for [", commandName, "]. Configuration error.");
					return null;
				}
				return cmd.invoke(target, args);
			}
		} catch (Exception e) {
			ConsoleOps.err("Failed to execute command [", commandName, "].", e);
			return null;
		}
	}
	
	/**
	 * Prepares a duplicate command name error message
	 * @param dupCmd the detected duplicate command 
	 * @return an error message
	 */
	public String dupCmdErr(CommandImpl dupCmd) {
		StringBuilder b = new StringBuilder("Duplicate command name [" + dupCmd + "]");
		b.append("\nThe command method [").append(dupCmd.commandMethod.getDeclaringClass().getName()).append("/").append(dupCmd.commandMethod.getName()).append("] has the same command name as \n[");
		CommandImpl cmd = commands.get(dupCmd.getCommandName());
		b.append(cmd.commandMethod.getDeclaringClass().getName()).append("/").append(cmd.commandMethod.getName()).append("]");		
		return b.toString();
	}
	
	
}
