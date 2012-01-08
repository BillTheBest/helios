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
import javax.management.ObjectName;

import org.helios.jmx.dynamic.OperationNotFoundException;

/**
 * <p>Title: AsyncHeliosJMXClient</p>
 * <p>Description: Extended client interface for asynchronous operations. Implementations provide fire and forget versions of all write operations.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public interface AsyncHeliosJMXClient extends HeliosJMXClient {

	/**
	 * Async call to set the value of a specific attribute of a named MBean. The MBean is identified by its object name.
	 * @param objectName The name of the MBean within which the attribute is to be set.
	 * @param attributeName The attribute name for which the value will be set.
	 * @param value The value to set the attribute to.
	 */
	public void setAttributeAsync(ObjectName objectName, String attributeName, Object value);

	/**
	 * Async call to set the values of several attributes of a named MBean. The MBean is identified by its object name.
	 * @param objectName The object name of the MBean within which the attributes are to be set.
	 * @param values A name/value map of attribute name/value to be set in the MBean.
	 */
	public void setAttributesAsync(ObjectName objectName, Map<String, ? extends Object> values);

	
	/**
	 * Async call to set the value of a specific attribute using an alias name which maps to a specific MBean/Attribute.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param value The value the attribute will be set to.
	 */
	public void setAttributeAsync(String aliasName, Object value);
	
	
	/**
	 * Async call to invoke an operation on an MBean.
	 * @param objectName The object name of the MBean on which the method is to be invoked.
	 * @param action The name of the operation to be invoked.
	 * @param args An array containing the parameters to be set when the operation is invoked
	 * @param signature An array containing the signature of the operation. The class objects will be loaded using the same class loader as the one used for loading the MBean on which the operation was invoked.
	 */
	public void invokeAsync(ObjectName objectName, String action, Object[] args, String[] signature);
	
	/**
	 * Async call to invoke an operation based on an aliased MBean operation.
	 * The mnemonic registry nullifies the need for a signature, the the parameters can be passed without it.
	 * @param aliasName The alias name which is assumed to be mapped in the mnemonic registry.
	 * @param args The parameters to the invocation.
	 */
	public Object invokeAsync(String aliasName, Object...args);
	
}
