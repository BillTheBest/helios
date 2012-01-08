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

import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;


/**
 * <p>Title: JNDIMBeanServerConnectionFactory </p>
 * <p>Description: Gets an MBeanServerConnection through JNDI</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class JNDIMBeanServerConnectionFactory implements
		IMBeanServerConnectionFactory {

	/**	The properties to make the JNDI Connection */
	protected Properties jndiProperties = new Properties();
	/**	The JNDI name of the MBeanServerConnection Object to retrieve */
	protected String jndiName = null;
	/**	The context acquired */
	protected Context context = null;
	
	/** Default key to acquire jndiName passed through Helios configuration.
	 *  If this key is missing from configuration, DEFAULT_JNDI_NAME will be used as 
	 *  a fallback.
	 */ 
	public static final String JNDI_NAME = "jndi.name";
	
	/** Default JNDI name to acquire MBeanServerConnection */
	protected static final String DEFAULT_JNDI_NAME = "jmx/invoker/RMIAdaptor";
	
	protected static final Logger log = Logger.getLogger(JNDIMBeanServerConnectionFactory.class);
	
	/**
	 * Returns an MBeanServerConnection acquired through JNDI lookup and close the context
	 * 
	 * @throws MBeanServerConnectionFactoryException when fail to acquire either context 
	 * or the lookup based on jndiName 
	 */
	public MBeanServerConnection getMBeanServerConnection()
			throws MBeanServerConnectionFactoryException {
		try {
			context = new InitialContext(jndiProperties);
			return (MBeanServerConnection)context.lookup(jndiName);
		} catch (Exception e) {
			if(context == null) {
				throw new MBeanServerConnectionFactoryException("Failed to get JNDI Connection", e);
			} else {
				throw new MBeanServerConnectionFactoryException("Failed JNDI Lookup of [" + jndiName + "]", e);
			}
		}finally{
			close();
		}
	}

	/**
	 * Reset properties to acquire the Context
	 */
	public void setProperties(Properties properties) {
		jndiProperties.clear();
		jndiName = properties.getProperty(JNDI_NAME, DEFAULT_JNDI_NAME);
		jndiProperties.putAll(properties);
	}

	/**
	 * Closes an open context
	 */
	public void close() {
		try { 
			if(context!=null){
				context.close();
			}
		} catch (Exception e)
		{
			log.error("Error while closing context", e);
		}
	}

	public void setJMXServiceURL(String jmxServiceURL) {}	
	
}
