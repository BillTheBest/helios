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
package org.helios.jmx;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

import sun.management.ThreadInfoCompositeData;

/**
 * <p>Title: JVMDiagnosticService</p>
 * <p>Description: Provides some simple JVM diagnostics and operations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.JVMDiagnosticService</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class JVMDiagnosticService extends ManagedObjectDynamicMBean {
	/**  */
	private static final long serialVersionUID = 6597348013740676045L;
	/** The default diagnostic service JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.jvm:service=JVMDiagnostics");
	/** Indicates if the bean is registered */
	private static final AtomicBoolean registered = new AtomicBoolean(false);
	/** the instance */
	private static volatile JVMDiagnosticService instance = null;
	/** The threadmx bean */
	public static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	/** Indicates if the bean is registered */
	private final AtomicInteger stackDepth = new AtomicInteger(Integer.MAX_VALUE);
	
	/**
	 * Registers the diagnostic service.
	 */
	public static JVMDiagnosticService getInstance() {
		if(!registered.get()) {
			synchronized(registered) {
				if(!registered.get()) {
					instance = new JVMDiagnosticService();
					instance.reflectObject(instance);
					try {
						JMXHelper.getHeliosMBeanServer().registerMBean(instance, OBJECT_NAME);
					} catch (Exception e) {
						throw new RuntimeException("Failed to register JVMDiagnosticService", e);
					}
				}
			}
		}
		return instance;
	}
	
	/**
	 * Calls <code>System.exit(0)</code>.
	 */
	@JMXOperation(name="jvmExit", description="Calls system exit with a 0 return code")
	public void jvmExit() {
		System.exit(0);
	}
	
	/**
	 * Calls <code>System.exit</code> with the passed code.
	 * @param exitCode The exit code to return.
	 */
	@JMXOperation(name="jvmExitWithCode", description="Calls system exit with a passed return code")
	public void jvmExit(@JMXParameter(name="exitCode", description="The exit code to return") int exitCode) {
		System.exit(exitCode);
	}
	
	/**
	 * Returns the <code>ThreadInfo</code>s for the VM's non daemon threads.
	 * @return an array of <code>ThreadInfo</code>s
	 */
	@JMXAttribute(name="NonDaemonThreads", description="ThreadInfos for all non daemon threads", mutability=AttributeMutabilityOption.READ_ONLY)
	public CompositeData[] getNonDaemonThreads() {
		return getThreads(false);
	}
	
	/**
	 * Returns the <code>ThreadInfo</code>s for the VM's daemon threads.
	 * @return an array of <code>ThreadInfo</code>s
	 */
	@JMXAttribute(name="DaemonThreads", description="ThreadInfos for all daemon threads", mutability=AttributeMutabilityOption.READ_ONLY)
	public CompositeData[] getDaemonThreads() {
		return getThreads(true);
	}
	
	
	/**
	 * Returns the <code>ThreadInfo</code>s for the VM's specified thread type.
	 * @param daemon If true, returns <code>ThreadInfo</code>s for daemon threads, otherwise returns for non-daemon threads. 
	 * @return an array of <code>ThreadInfo</code>s
	 */
	public CompositeData[] getThreads(boolean daemon) {
		Set<CompositeData> tis = new HashSet<CompositeData>();
		for(Thread t: Thread.getAllStackTraces().keySet()) {
			if((daemon && t.isDaemon()) || (!daemon && !t.isDaemon())) {
				try {tis.add(ThreadInfoCompositeData.toCompositeData(threadMXBean.getThreadInfo(t.getId(), stackDepth.get()))); } catch (Exception e) {}
			}
		}		
		return tis.toArray(new CompositeData[tis.size()]);
	}

	/**
	 * Returns the stack depth of the thread infos to be retrieved 
	 * @return the stackDepth
	 */
	@JMXAttribute(name="StackDepth", description="The stack depth of the thread infos to be retrieved", mutability=AttributeMutabilityOption.READ_WRITE)	
	public int getStackDepth() {
		return stackDepth.get();
	}
	
	/**
	 * Sets the stack depth of the thread infos to be retrieved 
	 * @param depth the stackDepth
	 */
	public void setStackDepth(int depth) {
		stackDepth.set(depth);
	}
	
	
}
