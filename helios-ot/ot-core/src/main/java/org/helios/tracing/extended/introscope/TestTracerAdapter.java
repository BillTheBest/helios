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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.helios.aop.DynaClassFactory;

/**
 * <p>Title: TestTracerAdapter</p>
 * <p>Description: Test class for testing generation of the Introscope tracer DynaClass</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.introscope.TestTracerAdapter</code></p>
 */

public class TestTracerAdapter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Creating Introscope tracer DynaClass");
		System.setProperty("introscope.agent.agentName", "TestTracerAdapter");
		System.setProperty("introscope.agent.processName", "JavaSE");
		BasicConfigurator.configure();
		ClassLoader cl = DynaClassFactory.getClassLoaderForFile("/home/nwhitehead/introscope/9.0/agent/wily/Agent.jar");
		Thread.currentThread().setContextClassLoader(cl);
		IntroscopeAdapter adapter = (IntroscopeAdapter)DynaClassFactory.generateClassInstance(IntroscopeTracerAdapter.class.getPackage().getName() + ".TracerInstance", IntroscopeTracerAdapter.class, DynaClassFactory.clArr(cl));
		log("Created [" + adapter.getClass().getName() + "]");
		try {
			adapter.addConnectionListener(new IntroscopeAgentConnectionListener() {
				public void connectionUp() {
					log("\n\t========\n\tOuter Connection is UP !\n\t========\n");
				}
				public void connectionDown() {
					log("\n\t========\n\tOuter Connection is DOWN !\n\t========\n");
				}
			});
			adapter.addDataPoint("Foo", 1);
			log("Traced point");
			Field f = adapter.getClass().getDeclaredField("agent");
			f.setAccessible(true);
			log("Abstract:" + java.lang.reflect.Modifier.isAbstract(adapter.getClass().getMethod("getAgent").getModifiers()));
			Object agent = adapter.getAgent(); //adapter.getClass().getMethod("getAgent").invoke(adapter);
			log("Agent:" + agent);
			log("IsConnected:" + adapter.isAgentConnected());
			
			Thread.currentThread().join(5000);
			
			adapter.disconnect();
			
			Thread.currentThread().join(5000);
			
			log("Waiting for Connect");
			long start = System.currentTimeMillis();
			if(adapter.connectWithWait(15, TimeUnit.SECONDS)) {
				long elapsed = System.currentTimeMillis()-start;
				log("Connected in [" + elapsed + "] ms.");
				log("Triplet:" + Arrays.toString(adapter.getHostProcessAgent()));
			} else {
				log("ConnectWithWait Timed Out");
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
