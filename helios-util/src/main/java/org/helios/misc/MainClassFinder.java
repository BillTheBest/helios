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
package org.helios.misc;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Title: MainClassFinder</p>
 * <p>Description: Utility to make a best effort at finding a JVM instance's Main class.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.misc.MainClassFinder</code></p>
 */


public class MainClassFinder {
	/** The attach api main class */
	public static final String VIRTUAL_MACHINE = "com.sun.tools.attach.VirtualMachine";
	/** The attach api VirtualMachineDescriptor class */
	public static final String VIRTUAL_MACHINE_DESC = "com.sun.tools.attach.VirtualMachineDescriptor";
	
	/** The attach api VirtualMachineDescriptor method name to get the display name */
	public static final String DISPLAY = "displayName";
	/** The attach api VirtualMachine method name to get the VM's SystemProperties */
	public static final String SYSPROPS = "getSystemProperties"; 
	/** The attach api VirtualMachine  method name to get the VM's Agent Properties */
	public static final String AGENTPROPS = "getAgentProperties"; 
	/** The attach api VirtualMachine  method name to get the VM */
	public static final String ATTACH = "attach"; 
	/** The attach api VirtualMachine  method name to list VM Descriptors */
	public static final String LIST = "list"; 
	
	/** The agent property for the JVM command */
	public static final String CMD = "sun.java.command";
	/** The PID for this JVM */
	public static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	
	/** The virtual machine class */
	protected static volatile Class<?> VIRTUAL_MACHINE_CLASS = null;
	/** The virtual machine descriptor class */
	protected static volatile Class<?> VIRTUAL_MACHINE_DESCR_CLASS = null;
	/** The VM system properties accessor method */
	protected static volatile Method VM_SYSPROPS = null;
	/** The VM attach method */
	protected static volatile Method VM_ATTACH = null;
	
	/** The VM agent properties accessor method */
	protected static volatile Method VM_AGENTPROPS = null;
	/** The VM Descriptor display accessor method */
	protected static volatile Method VM_DISPLAY = null;
	/** The VM Descriptor list method */
	protected static volatile Method VM_LIST = null;
	/** Indicates if the attach api reflection variables are initialized */
	protected static final AtomicBoolean attachInitied = new AtomicBoolean(false);
	
	private static final ThreadLocal<String> currentPid = new ThreadLocal<String>();
	private static final ThreadLocal<Object> currentVm = new ThreadLocal<Object>();
	private static final ThreadLocal<Object> currentVmDescriptor = new ThreadLocal<Object>();
	
	
	// SysProp user.dir for working directory
	
	/*
	 * CMD in SysProps
	 * CMD in AgentProps
	 * DisplayName from VM
	 */
	
	
	public static void initAttach() {
		if(!attachInitied.get()) {
			try {
				if(VIRTUAL_MACHINE_CLASS==null) {
					VIRTUAL_MACHINE_CLASS = Class.forName(VIRTUAL_MACHINE);
				}
				if(VIRTUAL_MACHINE_DESCR_CLASS==null) {
					VIRTUAL_MACHINE_DESCR_CLASS = Class.forName(VIRTUAL_MACHINE_DESC);
				}			
				if(VM_SYSPROPS==null) {
					VM_SYSPROPS = VIRTUAL_MACHINE_CLASS.getMethod(SYSPROPS);
				}
				if(VM_AGENTPROPS==null) {
					VM_AGENTPROPS = VIRTUAL_MACHINE_CLASS.getMethod(AGENTPROPS);
				}
				if(VM_DISPLAY==null) {
					VM_DISPLAY = VIRTUAL_MACHINE_DESCR_CLASS.getMethod(DISPLAY);
				}
				if(VM_ATTACH==null) {
					VM_ATTACH = VIRTUAL_MACHINE_CLASS.getMethod(ATTACH, String.class);
				}
				if(VM_LIST==null) {
					VM_LIST = VIRTUAL_MACHINE_CLASS.getMethod(LIST);
				}
				attachInitied.set(true);
			} catch (Exception e) {
				throw new RuntimeException("Failed to initialize Attach API", e);
			}
		}
	}
	
	/**
	 * Returns the name of the main class for the JVM running with the passed PID.
	 * @param pid The pid of the target JVM
	 * @return the main class name
	 */
	public static String getMain(String pid) {
		if(pid==null) pid = PID;
		boolean local = pid.equals(PID);
		currentPid.set(pid);
		try {
			String command = System.getProperty(CMD);
			if(command==null) {				
				command = getVMAgentProperties(pid).getProperty(CMD);
			}
			if(command==null) {
				return null;
			}
			
			if(command.toLowerCase().contains(".lax") || command.toLowerCase().contains(".jar")) {
				return command.split("\\s+")[0];
			} else {
				return command.trim();
			}
		} catch (Exception e) {
			return null;
		} finally {
			currentPid.remove();
			currentVm.remove();
			currentVmDescriptor.remove();
		}
	}
	
	
	/**
	 * Returns the VM system properties for the VM identified by the passed PID
	 * @param pid The pid of the target VM
	 * @return the System Properties for the target VM
	 */
	public static Properties getVMSystemProperties(String pid) {
		if(pid.equals(PID)) {
			return System.getProperties();
		} else {
			try {
				return (Properties)VM_SYSPROPS.invoke(getVirtualMachine(pid));
			} catch (Exception e) {
				throw new RuntimeException("Failed to acquire system properties from VM [" + pid + "]", e);
			}
		}
	}
	
	/**
	 * Returns the VM agent properties for the VM identified by the passed PID
	 * @param pid The pid of the target VM
	 * @return the agent Properties for the target VM
	 */
	public static Properties getVMAgentProperties(String pid) {
		try {
			Object vm = getVirtualMachine(pid);
			
			Properties p =  (Properties)VM_AGENTPROPS.invoke(vm);
			
			return p;
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire agent properties from VM [" + pid + "]", e);
		}
	}
	
	
	public static String processJar(String userDir, String cmd) {
		return null;
	}
	
	public static String processLax(String userDir, String cmd) {
		return null;
	}
	
	public static Object getVirtualMachine(String pid) {
		try {
			Object vm = currentVm.get();
			if(vm==null) {
				initAttach();
				vm = VM_ATTACH.invoke(null, pid);
				currentVm.set(vm);
			}
			return vm;
		} catch (Exception e) {
			throw new RuntimeException("Failed to get VirtualMachine for PID [" + pid + "]", e);
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Testing Attach:" + System.getProperty("java.home"));
		String pid = "30801";
		log("VM for PID[" + pid + "]:" + getVirtualMachine(pid));
		log("SysPropCount:" + getVMSystemProperties(pid).size());
		log("AgentPropCount:" + getVMAgentProperties(pid).size());
		getMain(pid);
		log("UserDir:" + getVMSystemProperties(pid).getProperty("user.dir"));
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
