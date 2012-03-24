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
package test.org.helios.ot.trace;

import java.lang.reflect.Method;

/**
 * <p>Title: PerIncidentTrace</p>
 * <p>Description: Test for per interval incident tracing to Introscope</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.ot.trace.PerIncidentTrace</code></p>
 */

public class PerIncidentTrace {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("PerInterval Test");
		try {
			Class<?> drfClazz = Class.forName("com.wily.introscope.agent.api.DataRecorderFactory");
			log("Class:" + drfClazz.getName());
			Method m = drfClazz.getDeclaredMethod("createPerIntervalCounterDataRecorder", String.class);
			Object recorder = m.invoke(null, "Incidents");
			m = recorder.getClass().getDeclaredMethod("recordMultipleIncidents", int.class);
			m.setAccessible(true);
			log("Acquired Method [" + m.toGenericString() + "]");
			for(int i = 1; i < 20; i++) {
				m.invoke(recorder, i);
				log("Recorded [" + i + "]");
				Thread.sleep(5000);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
