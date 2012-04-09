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
package org.helios.ot.helios;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;

import com.sun.jmx.mbeanserver.JmxMBeanServer;

/**
 * <p>Title: InterceptingMBeanServerBuilder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.InterceptingMBeanServerBuilder</code></p>
 */
public class InterceptingMBeanServerBuilder extends MBeanServerBuilder {

	/**
	 * Creates a new InterceptingMBeanServerBuilder
	 */
	public InterceptingMBeanServerBuilder() {
		// -- -Djavax.management.builder.initial=org.helios.ot.helios.InterceptingMBeanServerBuilder
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerBuilder#newMBeanServer(java.lang.String, javax.management.MBeanServer, javax.management.MBeanServerDelegate)
	 */
	@Override
	public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer, MBeanServerDelegate delegate) {
		return JmxMBeanServer.newMBeanServer(defaultDomain, outer, delegate, true);
	}
	
	

}
