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
package org.helios.jmx.platform;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;

import org.helios.reflection.PrivateAccessor;

/**
 * <p>Title: PlatformMBeanServerNullDomainPatch</p>
 * <p>Description: In certain cases, if the JVM's default MBeanServerBuilder has been overriden by a 3rd party builder,
 * the platform MBeanServer is created with a null domain. This makes it inelligible to be referenced through the MBeanServerFactory.find utility.
 * This utility attempts to fix that.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.platform.PlatformMBeanServerNullDomainPatch</code></p>
 */

public class PlatformMBeanServerNullDomainPatch extends MBeanServerBuilder {
	/** The delegate builder */
	protected static volatile MBeanServerBuilder innerBuilder;
	/** delegate builder lock */
	protected static final Object lock = new Object();
	
	/** The sys prop name that defines the class name of the MBeanServerBuilder to use. */
	public static final String BUILDER_PROP = "javax.management.builder.initial";
	
	static {
		String currentBuilder = System.getProperty(BUILDER_PROP, MBeanServerBuilder.class.getName());
		try {
			try { System.clearProperty(BUILDER_PROP); } catch (Exception e) {}
			MBeanServer platformMBS = ManagementFactory.getPlatformMBeanServer(); 
			if(platformMBS.getDefaultDomain()==null) {
				PrivateAccessor.setStaticFieldValue(ManagementFactory.class, "platformMBeanServer", null);
				try { System.clearProperty(BUILDER_PROP); } catch (Exception e) {}
				String platformDomain = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();
				MBeanServerFactory.releaseMBeanServer(platformMBS);
				if(platformDomain==null) {
					System.err.println("Failed to install PlatformMBeanServerNullDomainPatch");
				} else {
					System.out.println("Successfully patched platform MBeanServer [" + platformDomain + "]");
				}
			} else {
				// If it's not null now, then the platform MBeanServer is immutably set.
			}
		} finally {
			System.setProperty(BUILDER_PROP, currentBuilder);
		}
	}
	
	/**
	 * Creates a new PlatformMBeanServerNullDomainPatch
	 */
	public PlatformMBeanServerNullDomainPatch() {
		super();
	}

	/**
	 * This method creates a new MBeanServer implementation object.
	 * @param defaultDomain Default domain of the new MBeanServer. 
	 * @param outer A pointer to the MBeanServer object that must be passed to the MBeans when invoking their MBeanRegistration interface.
	 * @param delegate A pointer to the MBeanServerDelegate associated with the new MBeanServer. The new MBeanServer must register this MBean in its MBean repository. 
	 * @return A new private implementation of an MBeanServer.
	 */
	@Override
	public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer, MBeanServerDelegate delegate) {
		return innerBuilder.newMBeanServer(defaultDomain==null ? "DefaultDomain" : defaultDomain, outer, delegate);
	}
	
	/**
	 * Determines if the builder override system property is set
	 * @return true if it is set
	 */
	public static boolean isBuilderDefined() {
		return System.getProperty(BUILDER_PROP, null)!=null;
	}
	
	/**
	 * Retrurns the system configured MBeanServerBuilder.
	 * @return the system configured MBeanServerBuilder. or the default if the get fails.
	 */
	public static MBeanServerBuilder getSystemDefinedBuilder() {
		String prop = System.getProperty(BUILDER_PROP, MBeanServerBuilder.class.getName());
		try {
			Class<MBeanServerBuilder> clazz = (Class<MBeanServerBuilder>) Class.forName(prop, true, ClassLoader.getSystemClassLoader());
			return clazz.newInstance();
		} catch (Exception e) {
			System.err.println("Failed to create the configured MBeanServerBuilder [" + prop + "]:" + e);
			return new MBeanServerBuilder();
		}
	}
}
