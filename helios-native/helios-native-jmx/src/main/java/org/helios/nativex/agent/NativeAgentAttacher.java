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
package org.helios.nativex.agent;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javassist.Modifier;


/**
 * <p>Title: NativeAgentAttacher</p>
 * <p>Description: Utility class to attach the NativeAgent to a running JVM.
 * Since <b>tools.jar</b> is not always available in the classpath, this class attempts to locate
 * the jar in some typical locations based on this JVM's <b><code>java.home</code></p> location.
 * All invocations against the attach API are executed reflectively.
 * </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.agent.NativeAgentAttacher</code></p>
 */

public class NativeAgentAttacher {
	
	/** The Attach API VirtualMachine class name */
	public static final String VM_CLASS = "com.sun.tools.attach.VirtualMachine";
	/** The Attach API VirtualMachineDescriptor class name */
	public static final String VMD_CLASS = "com.sun.tools.attach.VirtualMachineDescriptor";
	/** Maps of Attach API Class methods keyed by name in a map keyed by class name */
	private static final Map<String, Map<String, Method>> VM_COMMANDS = new HashMap<String, Map<String, Method>>();
	/** Maps of Attach API Classes keyed by class name */
	private static final Map<String, Class<?>> VM_CLASSES = new HashMap<String, Class<?>>();
	
	/** The PID of this JVM */
	public static final String MY_PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	/** The Attach API class loader */
	private static volatile ClassLoader toolsClassLoader = null;
	/** Attach API class loader creation lock */
	private static final Object lock = new Object();
	/** The system property name that resolves to the location of the <b>tools.jar</b>. Useful for hinting where file is */
	public static final String NATIVE_AGENT_TOOLS_HINT = "org.helios.tools.jar";
	
	/** The agent property name of the JMX Connector Server ServiceURL */
	public static final String TARGET_JMX_CONNECTOR = "com.sun.management.jmxremote.localConnectorAddress";
	/** The system property name indicating this agent is installed in a JVM  (set to true) */
	public static final String HELIOS_AGENT_INSTALLED = NativeAgent.class.getName().toLowerCase();
	/** A null parameter class signature */
	public static final Class<?>[] NULL_SIGNATURE = {};
	/** The toString of a null parameter class signature */
	public static final String NULL_SIGNATURE_KEY_SUFFIX = Arrays.toString(NULL_SIGNATURE);
	
	/*
	 * Java Home: /usr/lib/jvm/java-6-sun-1.6.0.24/jre
	 * Tools: /usr/lib/jvm/java-6-sun-1.6.0.24/lib/tools.jar
	 */
	
	/**
	 * Attaches to and loads the NativeAgent into the JVM identified by the passed PID.
	 * @param pid The PID of the target JVM
	 * @param debug Enabled verbose logging in the install process
	 * @return true if the agent was installed, false if it is already installed.
	 * @throws Exception
	 */
	public static boolean install(String pid, boolean debug, Object...args) throws Exception {
		log("Installing Helios NativeJMXAgent to Java Process [" + pid + "]");
		String options = null;		
		if(args!=null && args.length > 0) {
			StringBuilder b = new StringBuilder();
			for(Object o: args) {
				if(o==null) continue;
				b.append(o.toString());
			}
			options = b.toString();
		}
		if(debug) {
			if(options==null) {
				options = " -debug";
			} else {
				options = options + " -debug";
			}
		}

		Class<?> vmClass = loadVMAttach(VM_CLASS);
		Object vm = invoke(VM_CLASS, "attach", new Class[]{String.class}, null, pid); 
		Properties targetSysProps = (Properties)invoke(VM_CLASS, "getSystemProperties", vm);
		targetSysProps.setProperty("foo", "bar");
		if(targetSysProps.containsKey(NativeAgent.BOOT_CLASS.toLowerCase())) {
			return false;
		}
		if(options==null) {
			vmClass.getDeclaredMethod("loadAgent", String.class).invoke(vm, NativeAgentAttacher.class.getProtectionDomain().getCodeSource().getLocation().getFile());
		} else {
			vmClass.getDeclaredMethod("loadAgent", String.class, String.class).invoke(vm, NativeAgentAttacher.class.getProtectionDomain().getCodeSource().getLocation().getFile(), options);
		}
		//vm.loadAgent(NativeAgentAttacher.class.getProtectionDomain().getCodeSource().getLocation().getFile());
		log("Successfully Attached Helios NativeJMXAgent to Java Process [" + pid + "]");
		return true;
	}
	
	/**
	 * Returns an array of the PIDs of all instances of instrumentation capable JVMs running on this host, except this one.
	 * @return an array of pids
	 */
	public static Long[] getPlatformJVMPids() {
		Set<Long> pids = new HashSet<Long>();
		try {
			Class<?> vmClass = loadVMAttach(VM_CLASS);
			List<?> vms = (List<?>)vmClass.getDeclaredMethod("list").invoke(null);			
			for(Object o: vms) {
				String pid = (String)invoke(VMD_CLASS, "id", o);
				if(MY_PID.equals(pid)) continue;
				pids.add(Long.parseLong(pid.trim()));
			}
		} catch (Exception e) {			
		}				
		return pids.toArray(new Long[pids.size()]);
	}
	
	/**
	 * Prints the details of all instances of instrumentation capable JVMs running on this host, except this one.
	 */
	public static void printVMList()  {
		try {			
			List<?> vms = (List<?>)invoke(VM_CLASS, "list", null);
			StringBuilder b = new StringBuilder();
			for(Object o: vms) {
				log("VMD Type [" + o.getClass().getName() + "]");
				String pid = (String)invoke(VMD_CLASS, "id", o);
				if(MY_PID.equals(pid)) continue;
				b.append("\nVirtual Machine\n===============");
				b.append("\n\tDisplay[").append(invoke(VMD_CLASS, "displayName", o)).append("]");
				b.append("\n\tID[").append(pid).append("]");
			}
			log(b);
		} catch (Exception e) {
			throw new RuntimeException("Failed to list running Virtual Machines", e);
		}		
	}
	
	/**
	 * Returns a set of successfully listed virtual machines 
	 * @return a set of virtual machines
	 */
	public static Set<ListedVirtualMachine> getVMList() {
		Set<ListedVirtualMachine> jvms = new HashSet<ListedVirtualMachine>();
		List<?> vmds = (List<?>)invoke(VM_CLASS, "list", null);
		try { loadVMAttach(VMD_CLASS); } catch (Exception e1) {}
		for(Object vmd: vmds) {
			try {			
				Object vm = invoke(VM_CLASS, "attach", new Class[]{VM_CLASSES.get(VMD_CLASS)}, null, vmd);
				ListedVirtualMachine lvm = new ListedVirtualMachine(vm, vmd);
				if(MY_PID.equals("" + lvm.getPid())) continue;
				jvms.add(lvm);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		return jvms;
	}
	
	
	/**
	 * Reflectively invokes a method. Does not support overloaded methods
	 * @param className The class name of the target class
	 * @param methodName The method name of the target method 
	 * @param target The target object to invoke on, or null if the method is static. 
	 * @param args The method arguments
	 * @return the return value of the method invocation
	 */
	public static Object invoke(String className, String methodName, Object target, Object...args) {
		try {
			Map<String, Method> methods = VM_COMMANDS.get(className);
			if(methods==null) {
				loadVMAttach(className);
				methods = VM_COMMANDS.get(className);
			}
			Method m = methods.get(methodName + NULL_SIGNATURE_KEY_SUFFIX);
			return m.invoke(Modifier.isStatic(m.getModifiers()) ? null : target, args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke [" + className + "." + methodName + "]", e);
		}
	}
	
	/**
	 * Reflectively invokes a method. 
	 * @param className The class name of the target class
	 * @param methodName The method name of the target method
	 * @param signature The class signature of the target method 
	 * @param target The target object to invoke on, or null if the method is static. 
	 * @param args The method arguments
	 * @return the return value of the method invocation
	 */
	public static Object invoke(String className, String methodName, Class<?>[] signature, Object target, Object...args) {
		try {
			Map<String, Method> methods = VM_COMMANDS.get(className);
			if(methods==null) {
				loadVMAttach(className);
				methods = VM_COMMANDS.get(className);
			}
			Method m = getMethod(className, methodName, signature);
			return m.invoke(Modifier.isStatic(m.getModifiers()) ? null : target, args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke [" + className + "." + methodName + "]", e);
		}
	}
	
	
	/**
	 * Loads the named class, creating a classloader for <b>tools.jar</b> if necessary.
	 * @param className The class name to load
	 * @return The named class
	 * @throws Exception Thrown if the class is not in the current classpath and the <b>tools.jar</b> could not be found.
	 */
	public static Class<?> loadVMAttach(String className) throws Exception {
		if(toolsClassLoader==null) {
			synchronized(lock) {
				if(toolsClassLoader==null) {
					// Might be in the classpath already
					Class<?> attachClass = null;
					try { attachClass = Class.forName(VM_CLASS); } catch (Exception e) {}
					if(attachClass!=null) {
						toolsClassLoader = attachClass.getClassLoader();
					} else {
						File tools = findTools();
						if(tools==null) {
							throw new Exception("Unable to find JVM's tools.jar when attempting to load the class [" + className + "]", new Throwable());
						}					
						toolsClassLoader = new URLClassLoader(new URL[]{tools.toURI().toURL()}, ClassLoader.getSystemClassLoader());
					}
				}
			}
		}
		Class<?> vmClass = Class.forName(className, true, toolsClassLoader);
		Map<String, Method> mmap = new HashMap<String, Method>();
		for(Method m: vmClass.getDeclaredMethods()) {
			mmap.put(m.getName() + Arrays.toString(m.getParameterTypes()), m);
		}
		VM_COMMANDS.put(className, mmap);
		VM_CLASSES.put(className, vmClass);
		return vmClass;
	}
	
	/**
	 * Retrieves the named class from the class cache
	 * @param className The class name
	 * @return the class
	 */
	public static Class<?> getClass(String className) {
		Class<?> clazz = VM_CLASSES.get(className);
		if(clazz==null) {
			try {
				clazz = Class.forName(className, true, toolsClassLoader);
			} catch (Exception e) {
				throw new RuntimeException("Failed to find class [" + className + "]", e);
			}
			VM_CLASSES.put(className, clazz);
		}
		return clazz;
	}
	
	/**
	 * Retrieves a class method from the cache
	 * @param className The class name
	 * @param methodName The method name
	 * @param signature The method signature
	 * @return The located method or null if one was not found.
	 */
	public static Method getMethod(String className, String methodName, Class<?>...signature) {
		String key = methodName + (signature==null ? NULL_SIGNATURE_KEY_SUFFIX : Arrays.toString(signature));
		Map<String, Method> mmap = VM_COMMANDS.get(className);		
		return mmap==null ? null : mmap.get(key);
	}
	
	/**
	 * Attempts to find this JVM's <b>tools.jar</b>. 
	 * @return the <b>tools.jar</b> or null if it could not be found.
	 */
	public static File findTools() {
		File tools = null;
		String hint = System.getProperty(NATIVE_AGENT_TOOLS_HINT);
		if(hint!=null) {
			tools = new File(hint);
			if(tools.exists() && tools.canRead()) {
				log("Loading tools.jar from hinted location [" + hint + "]");
				return tools;
			} else {
				log("Hinted tools.jar location [" + hint + "] did not exist or could not be read.");
				tools = null;
			}
		}
		File javaHome = new File(System.getProperty("java.home"));
		if(javaHome.getName().contains("jre")) {
			tools = new File(javaHome.getParent() + File.separator + "lib" + File.separator + "tools.jar");
			if(tools.exists() && tools.canRead()) {
				log("Loading tools.jar from discovered location [" + tools + "]");
			} else {
				tools = null;
			}
		}
		return tools;
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
