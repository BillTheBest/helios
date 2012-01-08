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
package org.helios.collectors.os.sigar.remote;

import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.helios.collectors.os.ISystem;


/**
 * <p>Title: SigarMBeanProxyFactory</p>
 * <p>Description: Factory class that will expose ISystem interface to an OSCollector for a remote SIGAR target host.
 * It creates an MBean proxy to SigarImplMBean deployed on remote host's MBean server.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class SigarMBeanProxyFactory {
	public static Logger log = Logger.getLogger(SigarMBeanProxyFactory.class);
	
	public static ISystem newSigarMBeanProxy(Properties properties, String sigarMBeanObjectName){
		ObjectName oName = null;
		Context ctx = null;
		MBeanServerConnection mbeanConn = null;
		try{
			ctx = new InitialContext(properties);
			mbeanConn = (MBeanServerConnection)ctx.lookup("jmx/rmi/RMIAdaptor");
		}catch(NamingException nex){
			log.error("An exception occured while getting the initial context.", nex);
		}
		try {
			oName = ObjectName.getInstance(sigarMBeanObjectName);
		}catch(MalformedObjectNameException monex){
			log.error("An exception occured creating an ObjectName.", monex);
		} catch (NullPointerException npex) {
			log.error("An exception occured creating an ObjectName.", npex);
		}
		ISystem proxy = (ISystem)
	    MBeanServerInvocationHandler.newProxyInstance(  mbeanConn,
	    												oName,
	    												org.helios.collectors.os.ISystem.class,
	                                                    false);
		return proxy;

	}
	
}
