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
package org.helios.ot.agent.protocol.tcp;

import java.util.Properties;

import org.helios.ot.agent.HeliosOTClient;
import org.helios.ot.agent.protocol.AbstractHeliosOTClientProvider;

/**
 * <p>Title: TCPHeliosOTClientProvider</p>
 * <p>Description: A HeliosOTClientFactory implementation that uses Netty and TCP to communicate with a Helios OpenTrace Server.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.protocol.tcp.TCPHeliosOTClientProvider</code></p>
 */
public class TCPHeliosOTClientProvider extends AbstractHeliosOTClientProvider {
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClientProvider#newInstance(java.util.Properties)
	 */
	@Override
	public HeliosOTClient newInstance(Properties clientProperties) {
		// TODO Auto-generated method stub
		return null;
	}
	


}
