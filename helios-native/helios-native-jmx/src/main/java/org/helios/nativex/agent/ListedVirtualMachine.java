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
package org.helios.nativex.agent;

import static org.helios.nativex.agent.NativeAgentAttacher.HELIOS_AGENT_INSTALLED;
import static org.helios.nativex.agent.NativeAgentAttacher.TARGET_JMX_CONNECTOR;
import static org.helios.nativex.agent.NativeAgentAttacher.VMD_CLASS;
import static org.helios.nativex.agent.NativeAgentAttacher.VM_CLASS;
import static org.helios.nativex.agent.NativeAgentAttacher.invoke;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * <p>Title: ListedVirtualMachine</p>
 * <p>Description: Container class to represent a listed JVM instance.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.agent.ListedVirtualMachine</code></p>
 */

public class ListedVirtualMachine {
	/** The JVM description  */
	protected final String description;
	/** The JVM process ID */
	protected final long pid;
	/** The JVM's system Properties */
	protected final Properties systemProperties;
	/** The JVM's agent Properties */
	protected final Properties agentProperties;
	/** Indicates if the helios agent is installed */
	protected final AtomicBoolean heliosInstalled = new AtomicBoolean(false);
	/** Indicates if the jmx agent is installed */
	protected final AtomicBoolean jmxAgentInstalled = new AtomicBoolean(false);
	/** The JMX Service URL of the JVM's JMX agent */
	protected String jmxServiceURL = null;
	
	/** Constant indicating the JVM does not have the JMX agent installed */
	public static final String JMX_NOT_INSTALLED = "<Agent Not Installed>";
	
	/**
	 * Creates a new ListedVirtualMachine
	 * @param vm An opaque object that the VirtualMachine details can be reflected from
	 * @param vmDescriptor An opaque object that the VirtualMachineDescriptor details can be reflected from
	 * @throws Exception thrown if a reflective operation fails.
	 */
	public ListedVirtualMachine(Object vm, Object vmDescriptor) throws Exception {
		description = (String)invoke(VMD_CLASS, "displayName", vmDescriptor);
		pid = Long.parseLong((String)invoke(VMD_CLASS, "id", vmDescriptor));
		systemProperties = (Properties)invoke(VM_CLASS, "getSystemProperties", vm);
		agentProperties = (Properties)invoke(VM_CLASS, "getAgentProperties", vm);
		jmxServiceURL = agentProperties.getProperty(TARGET_JMX_CONNECTOR, "<Agent Not Installed>");
		jmxAgentInstalled.set(!JMX_NOT_INSTALLED.equals(jmxServiceURL));
		heliosInstalled.set((systemProperties.get(HELIOS_AGENT_INSTALLED)!=null)); 
	}
	
	
	public boolean matches(Pattern p) {
		if(p==null) return true;
		return p.matcher(description).find();
	}

	/**
	 * The JMX Service URL of the JVM's JMX Agent
	 * @return the jmxServiceURL
	 */
	public String getJmxServiceURL() {
		return jmxServiceURL;
	}

	/**
	 * The JVM's description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * The JVM's process ID
	 * @return the pid
	 */
	public long getPid() {
		return pid;
	}

	/**
	 * The JVM's system properties
	 * @return the systemProperties
	 */
	public Properties getSystemProperties() {
		return systemProperties;
	}

	/**
	 * The JVM's agent properties
	 * @return the agentProperties
	 */
	public Properties getAgentProperties() {
		return agentProperties;
	}

	/**
	 * Indicates if the helios agent is installed
	 * @return the heliosInstalled
	 */
	public boolean getHeliosInstalled() {
		return heliosInstalled.get();
	}

	/**
	 * Indicates if the JMX agent is installed
	 * @return the jmxAgentInstalled
	 */
	public boolean getJmxAgentInstalled() {
		return jmxAgentInstalled.get();
	}

	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (pid ^ (pid >>> 32));
		return result;
	}

	/**
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ListedVirtualMachine other = (ListedVirtualMachine) obj;
		if (pid != other.pid)
			return false;
		return true;
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("VirtualMachine [")
	    	.append(description).append("/").append(pid).append("]")
	    	.append(TAB).append("pid = ").append(this.pid)
	    	.append(TAB).append("description = ").append(this.description)    
	        .append(TAB).append("systemProperties = ").append(systemProperties==null ? 0 : systemProperties.size())
	        .append(TAB).append("agentProperties = ").append(agentProperties==null ? 0 : agentProperties.size())
	        .append(TAB).append("heliosInstalled = ").append(this.heliosInstalled)
	        .append(TAB).append("jmxAgentInstalled = ").append(this.jmxAgentInstalled)
	        .append("\n");    
	    return retValue.toString();
	}
	
}
