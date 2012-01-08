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
package org.helios.nativex.jmx.process;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Title: ProcessObjectName</p>
 * <p>Description: Enumerates the supported keys in a Process MBean </p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p>org.helios.nativex.jmx.process.ProcessObjectName</p>
 */
public enum ProcessObjectNameProperty {
	/** The simple process name  */
	NAME("name"),
	/**  The process PID */
	PID("pid"),
	/**  The process owner */
	OWNER("owner"),	
	/** The PID of the parent process  */
	PPID("ppid"),
	/** The full image name of the process  */
	IMAGE("image"),
	/** The working directory of the process  */
	WORKING_DIR("workingdir");
	
	/**
	 * Returns the default ProcessObjectNamePropertys
	 * @return an array of ProcessObjectNamePropertys
	 */
	public static ProcessObjectNameProperty[] getDefaultProperties() {
		return new ProcessObjectNameProperty[]{NAME, PID};
	}
	
	/**
	 * Returns all the keys as a set
	 * @return all the keys as a set
	 */
	public static Set<String> getKeys() {
		Set<String> keys = new HashSet<String>(ProcessObjectNameProperty.values().length);
		for(ProcessObjectNameProperty ponp: ProcessObjectNameProperty.values()) {
			keys.add(ponp.name());
		}
		return keys;
	}
	
	/** The keys  */
	public static final Set<String> KEYS = Collections.unmodifiableSet(getKeys());
	
	/**
	 * Creates a new ProcessObjectName
	 * @param key The ObjectName property key
	 */
	private ProcessObjectNameProperty(String key) {
		this.key = key;
	}
	
	/**  The ObjectName property key */
	private final String key;

	/**
	 * Returns the ObjectName property key
	 * @return the ObjectName property key
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Returns an array of ProcessObjectNamePropertys for the matching comma separated names passed in
	 * @param names The comma separated names to decode into ProcessObjectNamePropertys.
	 * @return A possibly empty array of ProcessObjectNamePropertys.
	 */	
	public static ProcessObjectNameProperty[] extract(String names) {
		if(names==null || names.length()<1) return new ProcessObjectNameProperty[0];
		return extract(names.split(","));
	}
	
	/**
	 * Returns an array of ProcessObjectNamePropertys for the matching names passed in
	 * @param names The names to decode into ProcessObjectNamePropertys.
	 * @return A possibly empty array of ProcessObjectNamePropertys.
	 */
	public static ProcessObjectNameProperty[] extract(String...names) {
		if(names==null || names.length<1) return new ProcessObjectNameProperty[0];
		Set<ProcessObjectNameProperty> set = new HashSet<ProcessObjectNameProperty>();
		for(String s: names) {
			if(s==null) continue;
			if(KEYS.contains(s.toUpperCase().trim())) {
				set.add(ProcessObjectNameProperty.valueOf(s.toUpperCase().trim()));
			}
		}
		return set.toArray(new ProcessObjectNameProperty[set.size()]);
	}
}
