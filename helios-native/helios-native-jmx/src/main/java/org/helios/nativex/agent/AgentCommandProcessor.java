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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.helios.helpers.StringHelper;
import org.kohsuke.args4j.Option;

/**
 * <p>Title: AgentCommandProcessor</p>
 * <p>Description: Args4j annotated bean to parse and execute the agent arguments.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.agent.AgentCommandProcessor</code></p>
 * @TODO: Need to expose this as part of the <b>Installer</b> command.
 */

public class AgentCommandProcessor {
	/** jmx domains to register with */
	protected String[] jmxDomains = null;
	/** Indicates if the agent should run in verbose logging mode */
	@Option(name="-debug", usage="Enables verbose output in the install process")
	protected boolean debug = false;
	/** MBeanServer cross-registers */
	protected final Set<CrossRegister> crossRegisters = new HashSet<CrossRegister>();
	
	/**
	 * Adds a JMX CrossRegister for the target VM
	 * @param crossRegConfig A string in the format <b><code>&lt;localdomain&gt,&lt;targetDomain&gt,&lt;filter&gt</code></b>.
	 */
	@Option(name="-xreg", usage="Adds a JMX CrossRegister for the target VM.\nAccepts one string in the format\ncrossRegConfig A string in the format <localdomain>,<targetDomain>,<filter>.")
	public void setCrossRegister(String crossRegConfig) {
		try { crossRegisters.add(new CrossRegister(crossRegConfig)); } catch (Exception e) {}
	}
	
	
	/**
	 * <p>Title: CrossRegister</p>
	 * <p>Description: Defines the configuration for a cross-register request where MBeans are cross registered from one mbeanserver to another.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	public static class CrossRegister {
		private final String targetDomain;
		private final String localDomain;
		private final String filter;
		/**
		 * Creates a new CrossRegister
		 * @param localDomain The default domain name of the MBeanServer where the aliases will be registered
		 * @param targetDomain The default domain name of the MBeanServer where the MBeans to be cross registered are registered
		 * @param filter The string of an ObectName filter that specifies which MBeans in the target MBeanServer will be cross-registered. A null value will be treated as all MBeans (<b><code>*:*</code></b>).	 * 
		 */
		public CrossRegister(String localDomain, String targetDomain, String filter) {
			if(localDomain==null) throw new IllegalArgumentException("Passed localDomain was null", new Throwable());
			if(targetDomain==null) throw new IllegalArgumentException("Passed targetDomain was null", new Throwable());
			this.targetDomain = targetDomain;
			this.localDomain = localDomain;
			this.filter = filter==null ? "*:*" : filter;
		}
		
		/**
		 * Creates a new CrossRegister
		 * @param configString A string in the format <b><code>&lt;localdomain&gt,&lt;targetDomain&gt,&lt;filter&gt</code></b>.
		 */
		public CrossRegister(String configString) {
			if(configString==null) throw new IllegalArgumentException("Passed configString was null", new Throwable());
			String[] frags = StringHelper.split(configString, ",");
			if(frags.length <2 || frags.length > 3) throw new IllegalArgumentException("Invalid config string. Parsed values were " + Arrays.toString(frags), new Throwable());
			localDomain = frags[0];
			targetDomain = frags[1];
			if(frags.length==3) {
				filter = frags[2];
			} else {
				filter = "*:*";
			}
		}

		/**
		 * Returns the default domain of the MBeanServer where the target MBeans to be cross-registered are registered
		 * @return the targetDomain
		 */
		public String getTargetDomain() {
			return targetDomain;
		}

		/**
		 * Returns the default domain of the MBeanServer where the alias mbeans will be cross-registered
		 * @return the localDomain
		 */
		public String getLocalDomain() {
			return localDomain;
		}

		/**
		 * Returns the cross-register ObjectName filter
		 * @return the filter
		 */
		public String getFilter() {
			return filter;
		}

		/**
		 * Constructs a <code>String</code> with key attributes in name = value format.
		 * @return a <code>String</code> representation of this object.
		 */
		public String toString() {
		    final String TAB = "\n\t";
		    StringBuilder retValue = new StringBuilder("CrossRegister [")
		    	.append(TAB).append("localDomain = ").append(this.localDomain)
		        .append(TAB).append("targetDomain = ").append(this.targetDomain)		        
		        .append(TAB).append("filter = ").append(this.filter)
		        .append("\n]");    
		    return retValue.toString();
		}

		/**
		 * @return
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((filter == null) ? 0 : filter.hashCode());
			result = prime * result
					+ ((localDomain == null) ? 0 : localDomain.hashCode());
			result = prime * result
					+ ((targetDomain == null) ? 0 : targetDomain.hashCode());
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
			CrossRegister other = (CrossRegister) obj;
			if (filter == null) {
				if (other.filter != null)
					return false;
			} else if (!filter.equals(other.filter))
				return false;
			if (localDomain == null) {
				if (other.localDomain != null)
					return false;
			} else if (!localDomain.equals(other.localDomain))
				return false;
			if (targetDomain == null) {
				if (other.targetDomain != null)
					return false;
			} else if (!targetDomain.equals(other.targetDomain))
				return false;
			return true;
		}
	}
	
	
	
	/**
	 * Sets the option for jmx domains
	 * @param domains a comma separated list of jmx domains to register with
	 */
	@Option(name="-jmxdomains", usage="Accepts a comma separated list of JMX MBeanServer domains that Coronal MBeans will be registered with")
	public void setJMXDomains(String domains) {
		if(domains!=null && domains.length()>0) {
			Set<String> doms = new HashSet<String>();
			String[] frags = domains.split(",");
			if(frags==null || frags.length<1) return;
			for(String s: frags) {
				if(s==null) continue;
				s = s.trim();
				if(s.length()>0) {
					doms.add(s);
				}
			}
			jmxDomains = doms.toArray(new String[doms.size()]);
			
		}
	}



	/**
	 * @return the jmxDomains
	 */
	public String[] getJmxDomains() {
		return jmxDomains;
	}



	/**
	 * @return the debug
	 */
	public boolean isDebug() {
		return debug;
	}
}
