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
package org.helios.javaagent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * <p>Title: AttachAgent</p>
 * <p>Description: Utility class to create and access a <code>java.lang.instrument.Instrumentation</code> instance when the JVM
 * was not started with a JavaAgent or the ones used are not accessible.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.javaagent.AttachAgent</code></p>
 */

public class AttachAgent {
	/** The instrumentation reference */
	public static Instrumentation INSTRUMENTATION;
	
	/**
	 * The agent main entry point
	 * @param agentArgs
	 * @param inst
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		INSTRUMENTATION = inst;
	}
	public static final String VM_CLASS = "com.sun.tools.attach.VirtualMachine";
	/**
	 * Returns the native JavaAgent that the ICE InstrumentationAgent will delegate to
	 * @return an Instrumentation instance.
	 */
	public Instrumentation getAgent() {
		try {
			String thisVmId = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
			
			String agentLib = createTempAgentJar();
			Class<?> vmClass = Class.forName(VM_CLASS);
			Object vm = vmClass.getDeclaredMethod("attach", String.class).invoke(null, thisVmId);
			vmClass.getDeclaredMethod("loadAgent", String.class).invoke(vm, agentLib);
			return AttachAgent.INSTRUMENTATION;
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return null;
	}
	
	protected String createTempAgentJar() throws Exception {
		File tmpFile = File.createTempFile(AttachAgent.class.getName(), ".jar");
		tmpFile.deleteOnExit();		
		ByteArrayInputStream bais = new ByteArrayInputStream(("Manifest-Version: 1.0\nAgent-Class: " + AttachAgent.class.getName() + "\n").getBytes());
		Manifest mf = new Manifest(bais);
		FileOutputStream fos = new FileOutputStream(tmpFile, false);
		JarOutputStream jos = new JarOutputStream(fos, mf);
		jos.flush();
		jos.close();
		fos.flush();
		fos.close();
		return tmpFile.getAbsolutePath();
	}

}
