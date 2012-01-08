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

import java.util.regex.Pattern;

import org.helios.nativex.agent.ListedVirtualMachine;
import org.helios.nativex.agent.NativeAgentAttacher;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

/**
 * <p>Title: ListCommand</p>
 * <p>Description: Lists running instrumentation capable JVMs on this hiost, excepting this one.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.cl.ListCommand</code></p>
 */

public class ListCommand implements AgentCommand {
	
	
	/** An optional regular expression that is used to filter displayed VirtualMachines by their descriptions. */
	protected String expression = null;
	
	
	@Option(name="-e", usage="Filters the listed JVMs by the passed case insensitive regular expression")
	public void setExpression(String expr)  {
		if(expr!=null) {
			this.expression = expr.trim();
		}
	}
	
	/**
	 * @param args The command line arguments
	 */
	@Override
	public void run(String...args) {
		Pattern p = null;
		try {
			if(expression!=null) {
				p = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
			}				
		} catch (Exception e) {
			System.err.println("Bad regular expression [" + expression + "]. Ignoring.");
		}
		StringBuilder b = new StringBuilder("\nDetected Java Virtual Machines\n=================================================\n");
		for(ListedVirtualMachine lvm : NativeAgentAttacher.getVMList()) {
			if(lvm.matches(p)) {
				b.append(lvm.toString());
				b.append("=====================================\n");
			}
		}
		System.out.println(b);
	}

	/**
	 * @return
	 */
	@Override
	public String usage() {
		return CmdLineParserHelper.getUsage(this);
	}

}
