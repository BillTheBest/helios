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
package org.helios.jmx.aliases;

import java.util.Set;

import javax.management.ObjectName;

/**
 * <p>Title: AliasMBeanRegistryMBean</p>
 * <p>Description: JMX MBean interface for AliasMBeanRegistry.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.aliases.AliasMBeanRegistryMBean</code></p>
 */

public interface AliasMBeanRegistryMBean {
	/**
	 * Registers a new alias
	 * @param alias
	 */
	void registerAlias(MBeanAlias alias);
	
	/**
	 * Registers a new dynamic registration filter
	 * @param filter An ObjectName filter
	 */
	void registerDynamicRegistrationFilter(ObjectName filter);
	
	/**
	 * Unregisters an alias
	 * @param alias
	 */
	void unregisterAlias(MBeanAlias alias);
	
	/**
	 * Returns a set of the JMX ObjectNames of the registered aliases
	 * @return a set of ObjectNames
	 */
	public Set<ObjectName> getAliases();
	
	/**
	 * Returns a set of the JMX ObjectName filters of the registered dynamic registration MBean filters
	 * @return a set of ObjectNames
	 */
	public Set<ObjectName> getDynamicRegistrationFilters();
	
	/**
	 * Determines if the passed ObjectName represents a registered MBeanAlias
	 * @param aliasObjectName An ObjectName to test for
	 * @return true if an MBeanAlias with the passed ObjectName has been registered
	 */
	public boolean isRegistered(ObjectName aliasObjectName);

	/**
	 * Determines if the passed alias is registered
	 * @param alias The alias to test for
	 * @return true if the MBeanAlias is registered
	 */	
	public boolean isRegistered(MBeanAlias alias);
	
	/**
	 * Returns the target MBeanServer ID
	 * @return the mbeanServerId
	 */
	public String getTargetMBeanServerId();

	/**
	 * Returns the local MBeanServer ID
	 * @return the thisMbeanServerId
	 */
	public String getLocalMBeanServerId();


	/**
	 * Returns the JVM runtime name of the target MBeanServer
	 * @return the targetRuntimeName
	 */
	public String getTargetRuntimeName();


	/**
	 * Returns the JVM runtime name of the local MBeanServer
	 * @return the localRuntimeName
	 */
	public String getLocalRuntimeName();	
	
	/**
	 * Returns the default domain of the target MBeanServer
	 * @return the targetDomain
	 */
	public String getTargetDomain();

	/**
	 * Returns the default domain of the local MBeanServer
	 * @return the localDomain
	 */
	public String getLocalDomain();
	
	/**
	 * This registry's unique serial number
	 * @return the registrySerial
	 */
	public long getRegistrySerial();	
}
