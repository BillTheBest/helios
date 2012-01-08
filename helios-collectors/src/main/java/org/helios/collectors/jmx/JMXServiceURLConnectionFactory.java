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
package org.helios.collectors.jmx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * <p>Title: JMXServiceURLConnectionFactory </p>
 * <p>Description: Gets an MBeanServerConnection using JMXServiceURL</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class JMXServiceURLConnectionFactory implements
		IMBeanServerConnectionFactory {
	
	protected String jmxServiceURL = null;
	protected Map properties = null;
	
	public MBeanServerConnection getMBeanServerConnection()
			throws MBeanServerConnectionFactoryException {
		MBeanServerConnection connection = null;
		try{
			if(jmxServiceURL == null)
				throw new MBeanServerConnectionFactoryException("An error occured connecting to the MBeanServer as JMXServiceURL is null.");
			JMXServiceURL serviceURL = new JMXServiceURL(jmxServiceURL);
			JMXConnector connector = JMXConnectorFactory.connect(serviceURL, properties);
			connection = connector.getMBeanServerConnection();
		}catch(MalformedURLException mfex){
			throw new MBeanServerConnectionFactoryException("An error occured connecting to the MBeanServer as invalid JMXServiceURL provided: "+jmxServiceURL, mfex);
		}catch(IOException ioex){
			throw new MBeanServerConnectionFactoryException("An error occured connecting to the MBeanServer located at this address: "+ jmxServiceURL, ioex);
		}
		return connection;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public void close() {}

	public void setJMXServiceURL(String jmxServiceURL) {
		this.jmxServiceURL = jmxServiceURL;		
	}

}
