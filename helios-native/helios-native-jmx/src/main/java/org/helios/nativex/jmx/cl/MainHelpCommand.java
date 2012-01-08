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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;

/**
 * <p>Title: MainHelpCommand</p>
 * <p>Description: Provides main help </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.cl.MainHelpCommand</code></p>
 */

public class MainHelpCommand implements AgentCommand {
	protected String commandHelp = null;
	
	
	/**
	 * Executes the main help command
	 * @param args The command line arguments 
	 */
	
	public void run(String...args) throws CmdLineException {
		if(commandHelp==null) {
			CLProcessor.log(CLProcessor.commandUsage);
		} else {
			if("help".equals(commandHelp)){
				CLProcessor.log("help for command " + commandHelp);
				CLProcessor.log(usage());
				return;
			}
			AgentCommand agentCommand = CLProcessor.commandMap.get(commandHelp);
			CLProcessor.log("help for command " + commandHelp);
			if(agentCommand==null) {
				CLProcessor.elog("Unrecognized command [" + commandHelp + "]. Valid commands are: " + CLProcessor.commands + "\n\n");
			} else {
				CLProcessor.log(agentCommand.usage());
			}
		}
	}

	/**
	 * @return
	 */
	@Override
	public String usage() {
		return CmdLineParserHelper.getUsage(this);
	}

	/**
	 * 
	 * @param commandHelp the commandHelp to set
	 */
	@Argument(index=0, usage="help [<command name>]", required=false)
	public void setCommandHelp(String commandHelp) {
		if(commandHelp!=null) {
			this.commandHelp = commandHelp.toLowerCase().trim();
		}
	}

}
