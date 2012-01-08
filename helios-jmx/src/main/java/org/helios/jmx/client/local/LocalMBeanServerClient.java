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
package org.helios.jmx.client.local;

import java.util.Properties;

import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.client.AbstractHeliosJMXClient;
import org.helios.jmx.client.ConnectionException;
import org.helios.jmx.client.ConstructionException;

/**
 * <p>Title: LocalMBeanServerClientFactory</p>
 * <p>Description: HeliosJMXClient implementation for an in-vm MBeanServer.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public class LocalMBeanServerClient extends AbstractHeliosJMXClient {
	
	/**
	 * Creates a total default client.
	 * This connects to the <code>DefaultDomain</code> local platform agent.  
	 */
	public LocalMBeanServerClient() {
		defaultDomain = JMX_DOMAIN_DEFAULT;
		mBeanServerConnection = JMXHelperExtended.getLocalMBeanServer(JMX_DOMAIN_DEFAULT);
	}
	
	public LocalMBeanServerClient(Properties env)  throws ConnectionException, ConstructionException {		
		super(env);
	}
	
	/**
	 * Identifies if a client is connected to an in-vm mbeanserver.
	 * @return true if client and mbeanserver are in the same vm.
	 */	
	public boolean isLocal() {
		return true;
	}
	
	public boolean isAsync() {
		return false;
	}
	
	


}
