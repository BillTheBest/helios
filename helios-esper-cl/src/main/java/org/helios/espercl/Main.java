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
package org.helios.espercl;

import org.apache.log4j.BasicConfigurator;
import org.helios.classloading.RemoteJMXClassLoader;

/**
 * <p>Title: Main</p>
 * <p>Description: Command line entry point for the Esper Command Line util.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.Main</code></p>
 */

public class Main {
	protected static EsperCLStartOptions options = new EsperCLStartOptions();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		EsperCLRuntimeExecutor executor = null;
		options.doMain(args);
		try {
			RemoteJMXClassLoader cl = new RemoteJMXClassLoader(options.getJmxServiceUrl());
			Thread.currentThread().setContextClassLoader(cl);
			executor = new EsperCLRuntimeExecutor(options.getJmxServiceUrl());
		} catch (Exception e) {
			System.err.println("Failed to load HeliosEsperCL. Stack trace follows:");
			e.printStackTrace(System.err);
			System.exit(-1);
		}
		try { executor.getCommandReader().join(); } catch (Exception e) {}

	}

}
