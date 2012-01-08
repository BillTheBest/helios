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
package org.helios.scripting.console;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import groovy.ui.Console;
/**
 * <p>Title: GroovyService</p>
 * <p>Description: Bootstrap to launch a groovy console inside the Helios JVM.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.scripting.console.GroovyService</code></p>
 */
@JMXManagedObject(annotated=true, declared=true, objectName=GroovyService.OBJECT_NAME_STR )
public class GroovyService extends ManagedObjectDynamicMBean{ 
	/**  */
	private static final long serialVersionUID = -2758890352866603234L;
	public static final String OBJECT_NAME_STR = "org.helios.scripting:service=GroovyService";
	/** The JMX ObjectName for the GroovyService service */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName(OBJECT_NAME_STR);
	/** Static logger */
	protected Logger LOG = Logger.getLogger(GroovyService.class);
	
	
	/**
	 * Creates a new GroovyService and registers the MBean interface 
	 */
	public GroovyService() {
		LOG.info("\n\t==============================\n\tStarting GroovyService\n\t==============================\n");
		try {
//			this.reflectObject(this);
//			JMXHelper.getHeliosMBeanServer().registerMBean(this, OBJECT_NAME);
			LOG.info("\n\t==============================\n\tStarted GroovyService\n\t==============================\n");
		} catch (Exception e) {
			LOG.error("Failed to start GroovyService", e);
		}
		
	}
	
	/**
	 * Launches an interactive GroovyService UI
	 */
	@JMXOperation(name="launchConsole", description="Launches an interactive GroovyService UI")
	public void launchConsole() {
		Console.main(new String[]{});
	}
}
