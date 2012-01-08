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

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * <p>Title: CommandImpl</p>
 * <p>Description: Impl for <code>@Command</code> annotation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.commands.CommandImpl</code></p>
 */

public class CommandImpl {
	/** The command's underlying method */
	protected final Method commandMethod;
	/** The command's underlying annotation */
	protected final Command command;
	/** Indicates if the method is static */	
	protected final boolean isStatic;
	/** The command name */
	protected final String commandName;
	/** Command name aliases */
	protected final String[] aliases;
	/** The command's parameters */
	protected final CommandParameterImpl[] parameters;
	/** The command description */
	protected final String description;
	/** The full command help text */
	protected final String fullHelpText;
	/** The total number of parameters */
	protected final int paramCount;
	/** The number of mandatory parameters */
	protected final int mandatoryParamCount;
	/** The number of optional parameters */
	protected final int optionalParamCount;
	
	
	
	
	/**
	 * Creates a new CommandImpl for the passed method.
	 * @param commandMethod the method to generate a command for.
	 */
	public CommandImpl(Method commandMethod) {
		this.commandMethod = commandMethod;		
		command = this.commandMethod.getAnnotation(Command.class);
		if(command==null) throw new RuntimeException("The method [" + this.commandMethod.toGenericString() + "] is not annotated with @Command", new Throwable());
		isStatic = Modifier.isStatic(this.commandMethod.getModifiers());
		commandName = command.name();
		aliases = command.aliases();
		description = command.description();
		StringBuilder b = new StringBuilder("Command:");
		b.append("[").append(commandName).append("] ").append(description);
		parameters = new CommandParameterImpl[commandMethod.getParameterTypes().length];
		paramCount = commandMethod.getParameterTypes().length;
		int tmpMandatory = 0, tmpOptional = 0;
		boolean optionalSwitch = false;
		for(int i = 0; i < commandMethod.getParameterTypes().length; i++) {
			parameters[i] = new CommandParameterImpl(commandMethod, i);
			b.append("\n\t").append(parameters[i].getDescription());
			if(parameters[i].isOptional()) {
				tmpOptional++;
				optionalSwitch = true;
			} else {
				tmpMandatory++;
				if(optionalSwitch) {
					throw new RuntimeException("The method [" + this.commandMethod.toGenericString() + "] has a mandatory parameter (" + i + ") after an optional parameter", new Throwable());
				}
			}
		}
		mandatoryParamCount = tmpMandatory;
		optionalParamCount = tmpOptional;
		fullHelpText = b.toString();
	}
	
	/**
	 * Executes this command against the passed target object.
	 * @param target The target object to invoke on. Can be null if the command method is static.
	 * @param args The string arguments
	 * @return the return value from the invocation
	 */
	public Object invoke(Object target, String[] args) {
		if((args==null || args.length<1) && this.mandatoryParamCount>0) {
			elog("Invalid parameter count for command [", commandName, "]. Usage:\n", fullHelpText);
		}
		try {
			Object[] arguments = new Object[paramCount];
			Arrays.fill(arguments, null);
			if(args != null && args.length>0) {
				for(int i = 0; i < args.length; i++) {
					arguments[i] = this.parameters[i].getValue(args[i]);
				}
			}
			if(isStatic) {
				return commandMethod.invoke(null, arguments);
			} else {
				if(target==null) throw new RuntimeException("Passed target object was null");
				if(!commandMethod.getDeclaringClass().isAssignableFrom(target.getClass())) {
					throw new RuntimeException("The passed target of type [" + target.getClass().getName() + "] is not compatibel with the command method [" + commandMethod.getName() + "] declared by [" + commandMethod.getDeclaringClass().getName() + "]");
				}
				if(!commandMethod.isAccessible()) {
					commandMethod.setAccessible(true);
				}
				return commandMethod.invoke(target, arguments);					
			}
		} catch (Exception e) {
			elog("Unexpected exception executing [", commandName, "]. Stack trace follows.");
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	/**
	 * Outputs a message to StdOut
	 * @param frags the message fragments to concatenate
	 */
	public static void log(Object...frags) {
		log(System.out, frags);
	}
	
	/**
	 * Outputs a message to StdErr
	 * @param frags the message fragments to concatenate
	 */
	public static void elog(Object...frags) {
		log(System.err, frags);
	}
	
	
	/**
	 * Conctenates the message fragments and prints out to the passed print stream 
	 * @param ps the print stream to print out to 
	 * @param frags the message fragments to concatenate
	 */
	public static void log(PrintStream ps, Object...frags) {
		if(frags==null || frags.length<1 || ps==null) return;
		StringBuilder b = new StringBuilder();
		for(Object o: frags) {
			if(o!=null) {
				b.append(o.toString());
			}
		}
		ps.println(b.toString());
	}
	
	
	/**
	 * Returns the name of the command that maps the method, type or constructor.
	 * @return the name of the command that maps the method, type or constructor.
	 */
	public String getName() {
		return null;
	}
	/**
	 * Returns the parameter descriptors.
	 * @return the parameter descriptors.
	 */
	public CommandParameterImpl[] getParams() {
		return null;
	}


	/**
	 * Returns true if the target method is static
	 * @return true if the target method is static
	 */
	public boolean isStatic() {
		return isStatic;
	}


	/**
	 * Returns the command name
	 * @return the commandName
	 */
	public String getCommandName() {
		return commandName;
	}


	/**
	 * Returns the command description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * Returns the full command description with parameter descriptions
	 * @return the fullHelpText
	 */
	public String getFullHelpText() {
		return fullHelpText;
	}


	/**
	 * Returns the parameter count
	 * @return the paramCount
	 */
	public int getParamCount() {
		return paramCount;
	}


	/**
	 * Returns the mandatoryt parameter count
	 * @return the mandatoryParamCount
	 */
	public int getMandatoryParamCount() {
		return mandatoryParamCount;
	}


	/**
	 * Returns the optional parameter count
	 * @return the optionalParamCount
	 */
	public int getOptionalParamCount() {
		return optionalParamCount;
	}

	/**
	 * Returns the defined command aliases
	 * @return the aliases
	 */
	public String[] getAliases() {
		return aliases;
	}

}
