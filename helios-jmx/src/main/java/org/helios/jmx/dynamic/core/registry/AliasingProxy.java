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
package org.helios.jmx.dynamic.core.registry;

import java.util.Map;

import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import org.helios.jmx.client.AliasFailureException;

/**
 * <p>Title: AliasingProxy</p>
 * <p>Description: Interface defining the service that handles all requests using aliases.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public interface AliasingProxy {
	
	/**
	 * Gets the value of a specific attribute using an alias name which maps to a specific MBean/Attribute.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @return The retrieved value.
	 * @throws AliasFailureException The alias could not be resolved.
	 */
	public Object getAttribute(String aliasName) throws AliasFailureException;

	/**
	 * Gets the values of specific attributes using alias names which map to specific MBeans/Attributes.
	 * @param aliasNames A list of alias names assumed to be mapped in the mnemonic registry.
	 * @return A Map of the retrieved object keyed by alias name.
	 */
	public Map<String, Object> getAttributes(String...aliasNames);
	
	/**
	 * Sets the value of a specific attribute using an alias name which maps to a specific MBean/Attribute.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param value The value the attribute will be set to.
	 * @throws AliasFailureException The alias could not be resolved.
	 */
	public void setAttribute(String aliasName, Object value) throws AliasFailureException;
	
	/**
	 * Invokes an operation based on an aliased MBean operation.
	 * The mnemonic registry nullifies the need for a signature, the the parameters can be passed without it.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param args The parameters to the invocation.
	 * @return The object returned by the operation, which represents the result of invoking the operation represented by the alias.
	 * @throws AliasFailureException The alias could not be resolved.
	 */
	public Object invoke(String aliasName, Object...args) throws AliasFailureException;
	
	/**
	 * Adds a listener to an attribute identified by the supplied alias.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted.
	 * @throws AliasFailureException The alias could not be resolved.
	 */	
	public void addNotificationListener(String aliasName, NotificationListener listener, NotificationFilter filter, Object handback) throws AliasFailureException;
	

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
	
}
