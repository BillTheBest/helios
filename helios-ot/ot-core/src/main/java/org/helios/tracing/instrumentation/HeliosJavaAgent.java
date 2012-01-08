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
package org.helios.tracing.instrumentation;

import java.lang.instrument.Instrumentation;
import org.helios.tracing.TracerFactory;

/**
 * <p>Title: HeliosJavaAgent</p>
 * <p>Description: JavaAgent that exposes the core JVM instrumentation instance to the TracerFactory.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version @helios.java.agent.version@
 * $Id: HeliosJavaAgent.java 1058 2009-02-18 17:33:54Z nwhitehead $
 */
public class HeliosJavaAgent {

		/**
		 * The java agent's pre-main method called before the first main in the JVM startup.
		 * Passes a reference to the JVM's instrumentation instance and agent arguments to the TracerFactory.
		 * @param agentArgs The agent initialization string.
		 * @param instrumentation The JVM instance provided instrumentation interface.
		 */
		public static void premain(String agentArgs, Instrumentation instrumentation) {
			TracerFactory.getInstance().setJavaAgentArguments(agentArgs);
			TracerFactory.getInstance().setInstrumentation(instrumentation);
		}
		
		/**
		 * Prints the version of the JavaAgent.
		 * @param args
		 */
		public static void main(String[] args) {
			System.out.println(getVersion());
		}
		
		/**
		 * The release version banner.
		 * @return release.
		 */
		public static String getVersion() {
			return "Helios JavaAgent v. @helios.java.agent.version@";
		}				




}
