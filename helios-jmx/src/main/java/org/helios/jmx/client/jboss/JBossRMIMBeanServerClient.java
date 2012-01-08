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
package org.helios.jmx.client.jboss;

import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;


import org.helios.jmx.client.AbstractHeliosJMXClient;
import org.helios.jmx.client.ConnectionException;
import org.helios.jmx.client.ConstructionException;


/**
 * <p>Title: JBossRMIMBeanServerClient</p>
 * <p>Description: HeliosJMXClient implementation for JBoss RMIAdaptor.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public class JBossRMIMBeanServerClient extends AbstractHeliosJMXClient {
	
	/** JNDI Naming context for remote JBoss Server */ 
	protected Context ctx = null;
	/** RMIAdaptor for remote JBoss Server */
	protected MBeanServerConnection rmiAdaptor = null;
	/** The JNDI name of the RMIAdaptor */
	protected String rmiAdaptorName = null;
	/** indicates if basic mbeanserver ops are supported */
	protected boolean basicOpsSupported = false;
	/** indicates if remote notification ops are supported */
	protected boolean notificationOpsSupported = false;
	
	
	public static final String JNDI_FACTORY = "org.jnp.interfaces.NamingContextFactory";
	public static final String JNDI_URL_PKG = "org.jboss.naming:org.jnp.interfaces";
	public static final String RMI_ADAPTOR_NAME = "org.jboss.rmiadaptor";
	public static final String RMI_ADAPTOR_NAME_DEFAULT = "jmx/invoker/RMIAdator";
	
	
	

	/**
	 * Unimplemented default constructor.
	 */
	public JBossRMIMBeanServerClient() {
		super();
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Creates a new JBossRMIMBeanServerClient.
	 * @param env The environment properties to initialize the JBossRMIMBeanServerClient.
	 * @throws ConnectionException
	 * @throws ConstructionException
	 */
	public JBossRMIMBeanServerClient(Properties env) throws ConnectionException, ConstructionException {
		super(env);
		rmiAdaptorName = env.getProperty(RMI_ADAPTOR_NAME, RMI_ADAPTOR_NAME_DEFAULT);
		if(clientPrincipal.equals(CLIENT_PRINCIPAL_DEFAULT)) clientPrincipal = null;
		if(clientCredentials.equals(CLIENT_CREDENTIALS_DEFAULT)) clientCredentials = null;
		initJNDI();
		initAdaptor();
	}
	
	/**
	 * Initializes the JNDI naming context in accordance with the constructor's environment properties.
	 * @throws ConnectionException Thrown if context cannot be acquired.
	 */
	protected void initJNDI() throws ConnectionException {
		try {
			Properties p = new Properties();
			p.put(Context.URL_PKG_PREFIXES, JNDI_URL_PKG);
			p.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
			p.put(Context.PROVIDER_URL, serverURL);
			if(clientPrincipal!=null) {
				p.put(Context.SECURITY_PRINCIPAL, clientPrincipal);
			}
			if(clientCredentials!=null) {
				p.put(Context.SECURITY_CREDENTIALS, clientCredentials);
			}
			if(securityLevel!=null) {
				p.put(Context.SECURITY_AUTHENTICATION, securityLevel);
			}
			if(securityProtocol!=null) {
				p.put(Context.SECURITY_PROTOCOL, securityProtocol);
			}
			
			ctx = new InitialContext(p);
		} catch (Exception e) {
			throw new ConnectionException("Failed to acquire JNDI Context to JBoss Server at [" + serverURL + "]", e); 
		}
	}
	
	/**
	 * Initializes the MBeanServerConnection.
	 * If the discovered adaptor is an RMIAdaptor, remote notifications will be enabled.
	 * @throws ConnectionException
	 */
	protected void initAdaptor() throws ConnectionException {
		try {
			mBeanServerConnection = (MBeanServerConnection)ctx.lookup(rmiAdaptorName);
			basicOpsSupported = true;
			try {
				rmiAdaptor = mBeanServerConnection;
				notificationOpsSupported=true;
			} catch (Exception e) {
				notificationOpsSupported=false;
			}			
		} catch (Exception e) {
			throw new ConnectionException("Failed to acquire JNDI Context to JBoss Server at [" + serverURL + "]", e);
		}
	}
	

	/**
	 * @see org.helios.jmx.client.HeliosJMXClient#isLocal()
	 */
	public boolean isLocal() {
		return false;
	}

}
