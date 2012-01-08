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

import java.io.File;

import org.helios.helpers.StringHelper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * <p>Title: ServerCommand</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.cl.ServerCommand</code></p>
 */

public class ServerCommand implements AgentCommand {
	
	/** The local XML configuration file */
	private File configFile = null;
	
	
	/**
	 * Creates a new ServerCommand
	 */
	public ServerCommand() {
	}

	/**
	 * Invokes the initialized command
	 * @param args The command line argument
	 */
	public void run(String...args) throws CmdLineException {
		
	}
	
	/**
	 * Returns the usage for this launcher
	 * @return the usage
	 */
	public String usage() {
		return CmdLineParserHelper.getUsage(this);
	}

	/**
	 * the configuration from a local XML file
	 * @return the configFile
	 */
	public File getConfigFile() {
		return configFile;
	}

	/**
	 * Sets the configuration from a local XML file
	 * @param configFile A local XML file name
	 */
	@Option(name="-cf", usage="-cf <an XML file name> Sets the configuration from a local XML file")
	public void setConfigFile(File configFile) {
		this.configFile = configFile;
	}

}
