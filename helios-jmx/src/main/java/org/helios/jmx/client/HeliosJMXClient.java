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
package org.helios.jmx.client;

 import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.jmx.dynamic.OperationNotFoundException;

/**
 * <p>Title: HeliosJMXClient</p>
 * <p>Description: Base interface for HeliosJMX Client</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public interface HeliosJMXClient {
	
	//=======================================
	//	Standard client property constants.
	//=======================================
	/** Constant that holds the name of the environment property for specifying the object name of the helios bootstrap service in the target MBeanServer. */
	public static final String BOOTSTRAP_SERVICE = "org.helios.bootstrap.objectname";
	/** The default bootstrap service object name */
	public static final String BOOTSTRAP_SERVICE_DEFAULT = "org.helios:service=HeliosBootStrap";
	/** Constant that holds the name of the environment property for specifying the identity of the connecting client */
	public static final String CLIENT_PRINCIPAL = "org.helios.client.principal";
	/** The default identity of a connecting client */
	public static final String CLIENT_PRINCIPAL_DEFAULT = "HeliosClient";
	/** Constant that holds the name of the environment property for specifying the credentials of the connecting client */
	public static final String CLIENT_CREDENTIALS = "org.helios.client.credentials";
	/** The default credentials of a connecting client */
	public static final String CLIENT_CREDENTIALS_DEFAULT = "HeliosClientCredentials";
	/** Constant that holds the name of the environment property for specifying the class name of the client factory that will create an instance of a helios client. */
	public static final String CLIENT_FACTORY = "org.helios.client.factoryname";
	/** The class name of the default helios client factory. */
	public static final String CLIENT_FACTORY_DEFAULT = "org.helios.client.local.LocalMBeanServerClient";
	/** Constant that holds the name of the environment property for specifying the URL of the target MBeanServer connection provider. */
	public static final String SERVER_URL = "org.helios.server.url";
	/** The default server URL. */
	public static final String SERVER_URL_DEFAULT = null;
	/** Constant that holds the name of the environment property for specifying the default domain of the target MBeanServer. */
	public static final String JMX_DOMAIN = "org.helios.client.jmx.domain";
	/** The default JMX Domain */
	public static final String JMX_DOMAIN_DEFAULT = "DefaultDomain";
	//=======================================	
	
	
	/**
	 * Returns the client's underlying JMX Agent connection.
	 * @return A connection to the target JMX Agent.
	 */
	public MBeanServerConnection getMBeanServerConnection();
	
	/**
	 * Identifies if a client is connected to an in-vm mbeanserver.
	 * @return true if client and mbeanserver are in the same vm.
	 */
	public boolean isLocal();
	
	/**
	 * Gets the value of a specific attribute of a named MBean. The MBean is identified by its object name.
	 * @param objectName The object name of the MBean from which the attribute is to be retrieved.
	 * @param attributeName A String specifying the name of the attribute to be retrieved.
	 * @return The value of the retrieved attribute.
	 * @throws AttributeNotFoundException The attribute specified is not accessible in the MBean.
	 * @throws InstanceNotFoundException The MBean specified is not registered in the MBean server.
	 */	
	public Object getAttribute(ObjectName objectName, String attributeName) 
		throws AttributeNotFoundException, 
		InstanceNotFoundException;
	/**
	 * Retrieves the values of several attributes of a named MBean. The MBean is identified by its object name.
	 * @param objectName The object name of the MBean from which the attribute is to be retrieved.
	 * @param attributeName A list of the attributes to be retrieved.
	 * @return A Map of the retrieved object keyed by attribute name.
	 * @throws InstanceNotFoundException The MBean specified is not registered in the MBean server.
	 */
	public Map<String, Object> getAttributes(ObjectName objectName, String[] attributeNames) 
		throws InstanceNotFoundException;
	/**
	 * Gets the value of a specific attribute using an alias name which maps to a specific MBean/Attribute.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @return The retrieved value.
	 * @throws AliasFailureException The alias could not be resolved.
	 */
	public Object getAttribute(String aliasName) 
		throws AliasFailureException;
	/**
	 * Gets the values of specific attributes using alias names which map to specific MBeans/Attributes.
	 * @param aliasNames A list of alias names assumed to be mapped in the mnemonic registry.
	 * @return A Map of the retrieved object keyed by alias name.
	 */
	public Map<String, Object> getAttributes(String...aliasNames);
	
	/**
	 * Sets the value of a specific attribute of a named MBean. The MBean is identified by its object name.
	 * @param objectName The name of the MBean within which the attribute is to be set.
	 * @param attributeName The attribute name for which the value will be set.
	 * @param value The value to set the attribute to.
	 * @throws AttributeNotFoundException The attribute specified is not accessible in the MBean.
	 * @throws InstanceNotFoundException The MBean specified is not registered in the MBean server.
	 */
	public void setAttribute(ObjectName objectName, String attributeName, Object value)
		throws AttributeNotFoundException,
		InstanceNotFoundException;
	
	/**
	 * Sets the values of several attributes of a named MBean. The MBean is identified by its object name.
	 * @param objectName The object name of the MBean within which the attributes are to be set.
	 * @param values A name/value map of attribute name/value to be set in the MBean.
	 * @throws InstanceNotFoundException The MBean specified is not registered in the MBean server.
	 */
	public void setAttributes(ObjectName objectName, Map<String, ? extends Object> values)
		throws InstanceNotFoundException;

	
	/**
	 * Sets the value of a specific attribute using an alias name which maps to a specific MBean/Attribute.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param value The value the attribute will be set to.
	 * @throws AliasFailureException The alias could not be resolved.
	 */
	public void setAttribute(String aliasName, Object value)
		throws AliasFailureException;

	
	
	/**
	 * Invokes an operation on an MBean.
	 * @param objectName The object name of the MBean on which the method is to be invoked.
	 * @param action The name of the operation to be invoked.
	 * @param args An array containing the parameters to be set when the operation is invoked
	 * @param signature An array containing the signature of the operation. The class objects will be loaded using the same class loader as the one used for loading the MBean on which the operation was invoked.
	 * @return The object returned by the operation, which represents the result of invoking the operation on the MBean specified.
	 * @throws InstanceNotFoundException The MBean specified is not registered in the MBean server.
	 * @throws OperationNotFoundException The MBean did not expose or contain the requested operation.
	 */
	public Object invoke(ObjectName objectName, String action, Object[] args, String[] signature)
		throws InstanceNotFoundException,
		OperationNotFoundException;
	
	/**
	 * Invokes an operation based on an aliased MBean operation.
	 * The mnemonic registry nullifies the need for a signature, the the parameters can be passed without it.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param args The parameters to the invocation.
	 * @return The object returned by the operation, which represents the result of invoking the operation represented by the alias.
	 * @throws AliasFailureException The alias could not be resolved.
	 */
	public Object invoke(String aliasName, Object...args)
		throws AliasFailureException;
	
	
	/**
	 * Adds a listener to a registered MBean.
	 * @param name The name of the MBean on which the listener should be added.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted.
	 * @throws InstanceNotFoundException The MBean name provided does not match any of the registered MBeans.
	 */
	public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) 
		throws InstanceNotFoundException;
	
	/**
	 * Adds a listener to an attribute identified by the supplied alias.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted.
	 * @throws AliasFailureException The alias could not be resolved.
	 */	
	public void addNotificationListener(String aliasName, NotificationListener listener, NotificationFilter filter, Object handback) 
		throws AliasFailureException;
	

	/**
	 * Registers a notification listener against all MBeans registered in the bootstrap registry matching the supplied regular expression.
	 * @param regEx A string representing a regular expression which will be applied to all HeliosJMX registered MBeans.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted.
	 * @param perpetual If this parameter is true, any new MBeans registered in the bootstrap registry after the notification subscription will automatically be included.
	 * @return The nummber of matched object names.
	 */
	public int addWildcardNotificationListener(String regEx, NotificationListener listener, NotificationFilter filter, Object handback, boolean perpetual);
	
	/**
	 * Registers a notification listener against all attributes with registered aliases matching the supplied regular expression.
	 * @param regEx A string representing a regular expression which will be applied to all registered aliases.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted.
	 * @param perpetual If this parameter is true, any new aliases registered in the mnemonic registry after the notification subscription will automatically be included.
	 * @return The nummber of matched aliases.
	 */
	public int addWildcardAttributeNotificationListener(String regEx, NotificationListener listener, NotificationFilter filter, Object handback, boolean perpetual);
	
	/**
	 * Returns true if the client implements the <code>AsyncHeliosJMXClient</code>.
	 * @return true if client supports async operations.
	 */
	public boolean isAsync();
	
	/**
	 * Returns the async interface for the current client.
	 * @return An instance of <code>AsyncHeliosJMXClient</code> for the current client.
	 * @throws AsyncClientInterfaceNotSupported Thrown if the current client does not support an async interface.
	 */
	public AsyncHeliosJMXClient getAsyncInterface() throws AsyncClientInterfaceNotSupported;
	
}
