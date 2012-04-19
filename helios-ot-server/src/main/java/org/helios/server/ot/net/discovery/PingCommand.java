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
package org.helios.server.ot.net.discovery;

import org.springframework.context.ApplicationContext;

/**
 * <p>Title: PingCommand</p>
 * <p>Description: Implements a simple multicast ping and returns the non-address payload of the ping request</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.net.discovery.PingCommand</code></p>
 */

public class PingCommand implements IDiscoveryCommand {

	/**
	 * Command name is <b><code>PING</code></b>
	 * {@inheritDoc}
	 * @see org.helios.server.ot.net.discovery.IDiscoveryCommand#getCommandName()
	 */
	@Override
	public String getCommandName() {
		return "PING";
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.net.discovery.IDiscoveryCommand#execute(java.lang.String[], org.springframework.context.ApplicationContext)
	 */
	@Override
	public String execute(String[] fullCommandString, ApplicationContext ctx) {
		if(fullCommandString.length>2) {
			StringBuilder b = new StringBuilder();
			for(int i = 2; i < fullCommandString.length; i++) {				
				if(i>3) b.append("|");
				b.append(fullCommandString[i]);
			}
			return b.toString();
		}
		return "";
	}

}
