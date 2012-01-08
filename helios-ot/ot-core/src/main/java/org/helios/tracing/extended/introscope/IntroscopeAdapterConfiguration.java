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
package org.helios.tracing.extended.introscope;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.misc.MainClassFinder;
import org.helios.tracing.trace.MetricId;

/**
 * <p>Title: IntroscopeAdapterConfiguration</p>
 * <p>Description: Configuration class for the Introscope Adapter.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.introscope.IntroscopeAdapterConfiguration</code></p>
 */

public class IntroscopeAdapterConfiguration implements Serializable {
	/**  */
	private static final long serialVersionUID = 5395911789301972550L;
	/** The location of the Introscope <code>Agent.jar</code> */
	protected String agentJarName = null;
	/** The introscope agent name */
	protected String agentName = null;
	/** The introscope process name */
	protected String processName = null;
	/** The introscope profile */
	protected String profile = null;
	
	
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(IntroscopeAdapterConfiguration.class);
	
	/** The Introscope defined system property for the agent name */
	public static final String AGENT_NAME = "introscope.agent.agentName";
	/** The Helios defined system property for the agent jat */
	public static final String AGENT_JAR = "introscope.agent.jar";
	
	/** The helios agent name system property */
	public static final String H_AGENT_NAME = MetricId.APPLICATION_ID;
	/** The helios process name system property */
	public static final String H_PROCESS_NAME = "org.helios.process.name";
	
	/** The agent's profile file */
	public static final String AGENT_PROFILE = "com.wily.introscope.agentProfile";
	
	/** The Introscope defined system property for the process name */
	public static final String PROCESS_NAME = "introscope.agent.defaultProcessName";
	/** The Introscope defined system property for the agent's profile */
	public static final String PROFILE_NAME = "introscope.agent.com.wily.introscope.agentProfile";
	/** The manifest entry key that identifies a jar as the introscope agent */
	public static final String WILY_MANIFEST_KEY = "com-wily-Name";
	/** The manifest entry value that identifies a jar as the introscope agent */
	public static final String WILY_MANIFEST_VALUE = "Introscope Agent";
	
	/** The Java Agent Signature Pattern */
	public static final Pattern JAVA_AGENT_PATTERN = Pattern.compile("-javaagent\\:(.*?\\.jar).*", Pattern.CASE_INSENSITIVE);
	
	/**
	 * Creates a new IntroscopeAdapterConfiguration
	 * @param agentJarName The location of the Introscope <code>Agent.jar</code>
	 * @param agentName The introscope agent name
	 * @param processName The introscope process name
	 * @param profile The introscope profile
	 */
	public IntroscopeAdapterConfiguration(String agentJarName, String agentName, String processName, String profile) {
		String javaAgentJar = getJavaAgentJar();
		this.agentJarName = javaAgentJar!=null ? javaAgentJar : agentJarName==null ? ConfigurationHelper.getSystemThenEnvProperty(AGENT_JAR, null) : agentJarName;
		if(this.agentJarName==null)  {  //throw new IllegalArgumentException("Introscope Agent Jar Not Found from -javaagent or passed [" + agentJarName + "]", new Throwable());
			LOG.warn("No Introscope Agent jar found. Tracer will not activate.");
		}
		this.agentName = conditionalSet(H_AGENT_NAME, AGENT_NAME, agentName, MainClassFinder.getMain(null), ManagementFactory.getRuntimeMXBean().getName()); 
		this.processName = conditionalSet(H_PROCESS_NAME, PROCESS_NAME, processName, System.getProperty("java.runtime.version"));
		this.profile = ConfigurationHelper.getSystemThenEnvProperty(AGENT_PROFILE, profile);
	}
	
	/**
	 * Creates a new IntroscopeAdapterConfiguration using all defaults.
	 */
	public IntroscopeAdapterConfiguration() {
		this(null, null, null, null);
	}
	
	public static void main(String[] args) {
		log("IntroscopeConfig Test");
		log("Helios Set");
		System.setProperty(IntroscopeAdapterConfiguration.AGENT_JAR, "/home/nwhitehead/introscope/9.0/agent/wily/Agent.jar");
		//System.setProperty(H_AGENT_NAME, "hagent");
		//System.setProperty(H_PROCESS_NAME, "hprocess");
		//System.setProperty(AGENT_NAME, "iagent");
		//System.setProperty(PROCESS_NAME, "iprocess");
		
		log(new IntroscopeAdapterConfiguration());
	}
	
	
	protected String conditionalSet(String testFirst, String testSecond, String...defaultValues) {
		String firstValue = ConfigurationHelper.getSystemThenEnvProperty(testFirst, null);
		String secondValue = ConfigurationHelper.getSystemThenEnvProperty(testSecond, null);
		if(firstValue != null) {
			return completeCondition(testFirst, testSecond, firstValue);
		} else if(secondValue != null) {
			return completeCondition(testFirst, testSecond, secondValue);
		} else {
			if(defaultValues!=null) {
				for(String s: defaultValues) {
					if(s!=null && s.length()>0) {
						return completeCondition(testFirst, testSecond, s);
					}
				}				
			}
			return completeCondition(testFirst, testSecond, "Unknown");			
		}
		
	}
	
	protected String completeCondition(String firstKey, String secondKey, String value) {
		//System.setProperty(firstKey, value);
		//System.setProperty(secondKey, value);
		return value;
	}


	/**
	 * Attempts to locate the Introscope Agent Jar from the JVM's input arguments.
	 * @return the jar file name or null if it was not found.
	 */
	public static String getJavaAgentJar() {
		for(String s: ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			s = s.trim();
			Matcher m = JAVA_AGENT_PATTERN.matcher(s);
			if(m.matches()) {
				String jar = m.group(1);
				if(isWilyJar(jar)) {
					return jar;
				}
			}
		}
		return null;
	}
	
	/**
	 * Determines if the passed string represents the Introscope Agent Jar
	 * @param jarName The file name
	 * @return true if the name is the agent jar
	 */
	public static boolean isWilyJar(String jarName) {
		JarFile jarFile = null;
		if(jarName==null) return false;
		try {
			jarFile = new JarFile(jarName);
			Manifest manifest = jarFile.getManifest();
			if(manifest==null) return false;
			Attributes attrs = manifest.getMainAttributes();
			if(attrs==null) return false;
			String value = attrs.getValue(WILY_MANIFEST_KEY);
			if(value==null) return false;
			return (value.trim().equals(WILY_MANIFEST_VALUE));
		} catch (Exception e) {
			return false;
		} finally {
			try { jarFile.close(); } catch (Exception e) {}
		}
	}
	
	
	
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
//	System.setProperty("introscope.agent.agentName", "TestTracerAdapter");
//	System.setProperty("introscope.agent.defaultProcessName", "JavaSE");

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("IntroscopeAdapterConfiguration [")
	        .append(TAB).append("agentJarName = ").append(this.agentJarName)
	        .append(TAB).append("agentName = ").append(this.agentName)
	        .append(TAB).append("processName = ").append(this.processName)
	        .append(TAB).append("profile = ").append(this.profile)
	        .append(TAB).append("SysProps:")
	        	.append(TAB).append("\t").append(AGENT_NAME).append(":").append(System.getProperty(AGENT_NAME, "Undefined"))
	        	.append(TAB).append("\t").append(H_AGENT_NAME).append(":").append(System.getProperty(H_AGENT_NAME, "Undefined"))
	        	.append(TAB).append("\t").append(PROCESS_NAME).append(":").append(System.getProperty(PROCESS_NAME, "Undefined"))
	        	.append(TAB).append("\t").append(H_PROCESS_NAME).append(":").append(System.getProperty(H_PROCESS_NAME, "Undefined"))
	        .append("\n]");    
	    return retValue.toString();
	}
	
	public void activate() {
		
	}

	/**
	 * Returns the location of the Introscope <code>Agent.jar</code>
	 * @return the agentJarName
	 */
	public String getAgentJarName() {
		return agentJarName;
	}

	/**
	 * Returns the Introscope agent name
	 * @return the agentName
	 */
	public String getAgentName() {
		return agentName;
	}

	/**
	 * Returns the Introscope process name
	 * @return the processName
	 */
	public String getProcessName() {
		return processName;
	}

	/**
	 * Returns the Introscope agent profile
	 * @return the profile
	 */
	public String getProfile() {
		return profile;
	}
	
//	-Dorg.helios.application.name=Collectors
//	-Dhelios.lib.version=1.0-SNAPSHOT
//	-Dintroscope.agent.agentName=JSAgent
//	-Dintroscope.agent.defaultProcessName=JavaSE
//	-Dcom.wily.introscope.agentProfile=/home/nwhitehead/introscope/9.0/agent/wily/nicholas.profile
//	-javaagent:/home/nwhitehead/introscope/9.0/agent/wily/Agent.jarwqefwqfewqef	
	
}
