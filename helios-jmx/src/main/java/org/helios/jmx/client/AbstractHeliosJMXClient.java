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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.Context;

import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.OperationNotFoundException;
import org.helios.jmx.dynamic.core.registry.AliasingProxy;

/**
 * <p>Title: AbstractHeliosJMXClient</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public abstract class AbstractHeliosJMXClient implements HeliosJMXClient {
	
	/**  The internal MBeanServer Connection */
	protected MBeanServerConnection mBeanServerConnection = null;
	/** The ObjectName of the bootstrap service */
	protected ObjectName bootStrapService = null;
	/** If the implementation supports async */
	protected boolean async = false;
	/** The async interface */
	protected AsyncHeliosJMXClient asyncClient = null;
	/** Aliased calls handler */
	protected AliasingProxy aliasedCallHandler = null;
	/** The client identifier */
	protected String clientPrincipal = null;
	/** The client credentials */
	protected String clientCredentials = null;
	/** The server URL */
	protected String serverURL = null;
	/** The default domain of the MBeanServer */
	protected String defaultDomain = null;
	/** The security level to use for naming authentication */
	protected Object securityLevel = null;
	/** The security protocol to use for naming authentication */
	protected Object securityProtocol = null;

	
	/**
	 * Parameterless constructor.
	 */
	public AbstractHeliosJMXClient() {
		
	}
	
	/**
	 * Initializes all standard environment properties.
	 * @param env
	 * @throws ConnectionException
	 * @throws ConstructionException
	 */
	public AbstractHeliosJMXClient(Properties env) throws ConnectionException, ConstructionException {
		defaultDomain = env.getProperty(JMX_DOMAIN, JMX_DOMAIN_DEFAULT);
		bootStrapService = JMXHelperExtended.objectName(env.getProperty(BOOTSTRAP_SERVICE, BOOTSTRAP_SERVICE_DEFAULT));
		clientPrincipal = env.getProperty(CLIENT_PRINCIPAL, CLIENT_PRINCIPAL_DEFAULT);
		clientCredentials = env.getProperty(CLIENT_CREDENTIALS, CLIENT_CREDENTIALS_DEFAULT);
		serverURL = env.getProperty(SERVER_URL, SERVER_URL_DEFAULT);
		securityLevel = env.getProperty(Context.SECURITY_AUTHENTICATION);
		securityProtocol = env.getProperty(Context.SECURITY_PROTOCOL);				
	}
	
	
	/**
	 * Returns the client's underlying JMX Agent connection.
	 * @return A connection to the target JMX Agent.
	 */
	public MBeanServerConnection getMBeanServerConnection() {
		return mBeanServerConnection;
	}
	
	/**
	 * Gets the value of a specific attribute of a named MBean. The MBean is identified by its object name.
	 * @param objectName The object name of the MBean from which the attribute is to be retrieved.
	 * @param attributeName A String specifying the name of the attribute to be retrieved.
	 * @return The value of the retrieved attribute.
	 * @throws AttributeNotFoundException The attribute specified is not accessible in the MBean.
	 * @throws InstanceNotFoundException The MBean specified is not registered in the MBean server.
	 */	
	public Object getAttribute(ObjectName objectName, String attributeName) 
		throws AttributeNotFoundException, InstanceNotFoundException {		
		try {
			return mBeanServerConnection.getAttribute(objectName, attributeName);
		} catch (MBeanException e) {
			throw processException("MBean Exception getting attribute", objectName, attributeName, e);
		} catch (ReflectionException e) {
			throw processException("Refelection Exception getting attribute", objectName, attributeName, e);
		} catch (IOException e) {
			throw processException("IO Exception getting attribute", objectName, attributeName, e);
		}
		
	}
	
	
	/**
	 * Retrieves the values of several attributes of a named MBean. The MBean is identified by its object name.
	 * @param objectName The object name of the MBean from which the attribute is to be retrieved.
	 * @param attributeName A list of the attributes to be retrieved.
	 * @return A Map of the retrieved object keyed by attribute name.
	 * @throws InstanceNotFoundException The MBean specified is not registered in the MBean server.
	 */
	public Map<String, Object> getAttributes(ObjectName objectName, String[] attributeNames) 
		throws InstanceNotFoundException {
		Map<String, Object> attributes = null;
		try {
			AttributeList attrs = mBeanServerConnection.getAttributes(objectName, attributeNames);
			attributes = new HashMap<String, Object>(attrs.size());
			Attribute a = null;
			for(int i = 0; i < attrs.size(); i++) {
				a = (Attribute)attrs.get(i);
				attributes.put(a.getName(), a.getValue());
			}
			return attributes;
		} catch (ReflectionException e) {			
			throw processException("Reflection Exception getting attributes", objectName, flatten(attributeNames), e);
		} catch (IOException e) {
			throw processException("IO Exception getting attributes", objectName, flatten(attributeNames), e);
		}
	}
	
	/**
	 * Gets the value of a specific attribute using an alias name which maps to a specific MBean/Attribute.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @return The retrieved value.
	 * @throws AliasFailureException The alias could not be resolved.
	 */
	public Object getAttribute(String aliasName) throws AliasFailureException {
		return aliasedCallHandler.getAttribute(aliasName);
	}

	/**
	 * Gets the values of specific attributes using alias names which map to specific MBeans/Attributes.
	 * @param aliasNames A list of alias names assumed to be mapped in the mnemonic registry.
	 * @return A Map of the retrieved object keyed by alias name.
	 */
	public Map<String, Object> getAttributes(String...aliasNames) {
		return aliasedCallHandler.getAttributes(aliasNames);
	}
	
	/**
	 * Sets the value of a specific attribute of a named MBean. The MBean is identified by its object name.
	 * @param objectName The name of the MBean within which the attribute is to be set.
	 * @param attributeName The attribute name for which the value will be set.
	 * @param value The value to set the attribute to.
	 * @throws AttributeNotFoundException The attribute specified is not accessible in the MBean.
	 * @throws InstanceNotFoundException The MBean specified is not registered in the MBean server.
	 */
	public void setAttribute(ObjectName objectName, String attributeName, Object value) throws AttributeNotFoundException, InstanceNotFoundException {
		try {
			mBeanServerConnection.setAttribute(objectName, new Attribute(attributeName, value));
		} catch (InvalidAttributeValueException e) {
			throw processException("Invalid Value Exception setting attribute", objectName, flatten(attributeName,value), e);
		} catch (MBeanException e) {
			throw processException("MBean Exception setting attribute", objectName, flatten(attributeName,value), e);
		} catch (ReflectionException e) {
			throw processException("Reflection Exception setting attribute", objectName, flatten(attributeName,value), e);
		} catch (IOException e) {
			throw processException("IO Exception setting attribute", objectName, flatten(attributeName,value), e);
		}
	}
	
	/**
	 * Sets the values of several attributes of a named MBean. The MBean is identified by its object name.
	 * @param objectName The object name of the MBean within which the attributes are to be set.
	 * @param values A name/value map of attribute name/value to be set in the MBean.
	 * @throws InstanceNotFoundException The MBean specified is not registered in the MBean server.
	 */
	public void setAttributes(ObjectName objectName, Map<String, ? extends Object> values) throws InstanceNotFoundException {
		if(values==null || values.size() < 1) return;
		AttributeList attrList = new AttributeList(values.size());
		for(Entry<String, ? extends Object> entry: values.entrySet()) {
			attrList.add(new Attribute(entry.getKey(), entry.getValue()));
		}		
	}

	
	/**
	 * Sets the value of a specific attribute using an alias name which maps to a specific MBean/Attribute.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param value The value the attribute will be set to.
	 * @throws AliasFailureException The alias could not be resolved.
	 */
	 public void setAttribute(String aliasName, Object value) throws AliasFailureException {
		 aliasedCallHandler.setAttribute(aliasName, value);
	 }
	
	
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
		OperationNotFoundException {
		Object retValue;
		try {
			retValue = mBeanServerConnection.invoke(objectName, action, args, signature);
			return retValue;
		} catch (MBeanException e) {
			throw processException("MBean Exception invoking op", objectName, action , e);
		} catch (ReflectionException e) {
			throw processException("Reflection Exception invoking op", objectName, action , e);
		} catch (IOException e) {
			throw processException("IO Exception invoking op", objectName, action , e);
		}		
	}
	
	/**
	 * Invokes an operation based on an aliased MBean operation.
	 * The mnemonic registry nullifies the need for a signature, the the parameters can be passed without it.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param args The parameters to the invocation.
	 * @return The object returned by the operation, which represents the result of invoking the operation represented by the alias.
	 * @throws AliasFailureException The alias could not be resolved.
	 */
	public Object invoke(String aliasName, Object...args) throws AliasFailureException {
		return aliasedCallHandler.invoke(aliasName, args);
	}
	
	
	/**
	 * Adds a listener to a registered MBean.
	 * @param objectName The name of the MBean on which the listener should be added.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted.
	 * @throws InstanceNotFoundException The MBean name provided does not match any of the registered MBeans.
	 */
	public void addNotificationListener(ObjectName objectName, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
		try {
			mBeanServerConnection.addNotificationListener(objectName, listener, filter, handback);
		} catch (IOException e) {
			throw processException("IO Exception adding notification listener", objectName, listener.toString(), e);
		}
	}
	
	/**
	 * Adds a listener to an attribute identified by the supplied alias.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted.
	 * @throws AliasFailureException The alias could not be resolved.
	 */	
	public void addNotificationListener(String aliasName, NotificationListener listener, NotificationFilter filter, Object handback) throws AliasFailureException {
		aliasedCallHandler.addNotificationListener(aliasName, listener, filter, handback);
	}
	

	/**
	 * Registers a notification listener against all MBeans registered in the bootstrap registry matching the supplied regular expression.
	 * @param regEx A string representing a regular expression which will be applied to all HeliosJMX registered MBeans.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted.
	 * @param perpetual If this parameter is true, any new MBeans registered in the bootstrap registry after the notification subscription will automatically be included.
	 * @return The nummber of matched object names.
	 */
	public int addWildcardNotificationListener(String regEx, NotificationListener listener, NotificationFilter filter, Object handback, boolean perpetual) {
		return aliasedCallHandler.addWildcardNotificationListener(regEx, listener, filter, handback, perpetual);
	}
	
	/**
	 * Registers a notification listener against all attributes with registered aliases matching the supplied regular expression.
	 * @param regEx A string representing a regular expression which will be applied to all registered aliases.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted.
	 * @param perpetual If this parameter is true, any new aliases registered in the mnemonic registry after the notification subscription will automatically be included.
	 * @return The nummber of matched aliases.
	 */
	public int addWildcardAttributeNotificationListener(String regEx, NotificationListener listener, NotificationFilter filter, Object handback, boolean perpetual) {
		return aliasedCallHandler.addWildcardAttributeNotificationListener(regEx, listener, filter, handback, perpetual);
	}
	
	/**
	 * Returns true if the client implements the <code>AsyncHeliosJMXClient</code>.
	 * @return true if client supports async operations.
	 */
	public boolean isAsync() {
		return async;
	}
	
	/**
	 * Returns the async interface for the current client.
	 * @return An instance of <code>AsyncHeliosJMXClient</code> for the current client.
	 * @throws AsyncClientInterfaceNotSupported Thrown if the current client does not support an async interface.
	 */
	public AsyncHeliosJMXClient getAsyncInterface() throws AsyncClientInterfaceNotSupported {
		if(!async) throw new AsyncClientInterfaceNotSupported();
		return asyncClient;
	}
	
	
	/**
	 * Helper to generate a runtime exception.
	 * @param cause Textual description.
	 * @param on The object name
	 * @param target The target attribute or method.
	 * @param exception The wrapped exception.
	 * @return A runtime exception.
	 */
	protected RuntimeException processException(String cause, ObjectName on, String target, Exception exception) {
		StringBuilder buff = new StringBuilder(cause);
		buff.append(" occured for object name [").append(on).append("] ");
		buff.append(" for target [").append(target).append("]");
		return new RuntimeException(buff.toString(), exception);
		
	}
	
	/**
	 * Flattens an array of objects into a comma separated string.
	 * @param <K>
	 * @param array An array of objects.
	 * @return A string.
	 */
	protected <K> String flatten(K[] array) {
		StringBuilder buff = new StringBuilder();
		for(K k: array) {
			buff.append(k.toString()).append(",");
		}
		buff.deleteCharAt(buff.length()-1);
		return buff.toString();
	}
	
	/**
	 * Flattens a name value pair.
	 * @param name The name
	 * @param value The value
	 * @return A string
	 */
	protected String flatten(String name, Object value) {
		StringBuilder buff = new StringBuilder(name);
		 buff.append(":").append(value);
		 return buff.toString();
	}


}
