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
package org.helios.classloading;

import static org.helios.helpers.ClassHelper.nvl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.helios.helpers.FileHelper;
import org.helios.helpers.JMXHelper;

/**
 * <p>Title: RemoteJMXClassLoader</p>
 * <p>Description: ClassLoader facade to class load from a remote HeliosJMX Class Server.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.classloading.RemoteJMXClassLoader</code></p>
 */

public class RemoteJMXClassLoader extends URLClassLoader implements NotificationListener, IRemoteClassLoader {

	/** The ObjectName used for registering this service */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.cp:service=ClassServer");
	/** The notification emitted when the classpath of the ClassServer changes */
	public static final String CP_CHANGED_TYPE = "org.helios.classserver.pathchanged";
	
	
	/** An MBeanServer connection to the MBeanServer where the ClassServer MBean is registered */
	protected final MBeanServerConnection jmxServer;
	/** An invocation stub for the remote class server */
	protected final IRemoteClassLoader classLoader;
	/** A set of class names known to be not found at the server */
	protected final Set<String> notFoundClasses = new CopyOnWriteArraySet<String>();
	/** A set of resource names known to be not found at the server */
	protected final Set<String> notFoundResources = new CopyOnWriteArraySet<String>();
	
	
	/**
	 * Creates a new RemoteJMXClassLoader with the System class loader as the parent
	 * @param jmxServiceUrl A JMXServiceURL to acquire a connection to the remote class server
	 * @throws IOException
	 */
	public RemoteJMXClassLoader(JMXServiceURL jmxServiceUrl) throws IOException {
		this(JMXConnectorFactory.connect(jmxServiceUrl).getMBeanServerConnection(), ClassLoader.getSystemClassLoader());
	}	
	/**
	 * Creates a new RemoteJMXClassLoader
	 * @param jmxServiceUrl A JMXServiceURL to acquire a connection to the remote class server
	 * @param parent the parent classloader to this classloader.
	 * @throws IOException
	 */
	public RemoteJMXClassLoader(JMXServiceURL jmxServiceUrl, ClassLoader parent) throws IOException {
		this(JMXConnectorFactory.connect(jmxServiceUrl).getMBeanServerConnection(), parent);
	}
	
	/**
	 * Creates a new RemoteJMXClassLoader
	 * @param jmxServer An MBeanServer connection to the MBeanServer where the ClassServer MBean is registered
	 * @param parent the parent classloader to this classloader.
	 * @throws IOException 
	 */
	public RemoteJMXClassLoader(MBeanServerConnection jmxServer, ClassLoader parent) throws IOException {
		super(new URL[]{}, nvl(parent, ClassLoader.getSystemClassLoader()));
		this.jmxServer = nvl(jmxServer, "The passed MBeanServerConnection was null");
		if(!jmxServer.isRegistered(OBJECT_NAME)) {
			throw new RuntimeException("The ClassServer [" + OBJECT_NAME + "] is not registered in the referenced MBeanServer");
		}
		classLoader = MBeanServerInvocationHandler.newProxyInstance(jmxServer, OBJECT_NAME, IRemoteClassLoader.class, false);
		try {
			jmxServer.addNotificationListener(OBJECT_NAME, this, null, null);
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception registering class server path change listener", e);
		}
	}
	
	/**
	 * Finds the class with the specified binary name. 
	 * @param className The binary name of the class 
	 * @return The resulting Class object 
	 * @throws ClassNotFoundException
	 */
	public Class<?> findClass(String className) throws ClassNotFoundException {
		Class<?> clazz = null;
		try {
			clazz = getParent().loadClass(nvl(className, "The passed class name was null"));
		} catch (ClassNotFoundException cne) {
		}
		if(clazz==null) {
			clazz = getRemoteClass(className);
		}
		if(clazz==null) {
			throw new ClassNotFoundException("Could not find class [" + className + "]");
		} else {
			return clazz;
		}
		
	}

	/**
	 * Loads the class with the specified binary name. 
	 * @param className The binary name of the class
	 * @param resolve If true then resolve the class  
	 * @return The resulting Class object 
	 * @throws ClassNotFoundException
	 */
	public Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
		Class<?> clazz = null;
		try {
			clazz = getParent().loadClass(nvl(className, "The passed class name was null"));
		} catch (ClassNotFoundException cne) {
		}
		if(clazz==null) {
			clazz = getRemoteClass(className);
		}
		if(clazz==null) {
			throw new ClassNotFoundException("Could not find class [" + className + "]");
		} else {
			return clazz;
		}		
	}
	
	/**
	 * Loads the class with the specified binary name. 
	 * @param className The binary name of the class
	 * @return The resulting Class object 
	 * @throws ClassNotFoundException
	 */
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		return loadClass(className, true);
	}
	
	/**
	 * Finds the resource with the given name.
	 * @param resourceName The resource name
	 * @return A URL object for reading the resource, or null if the resource could not be found
	 */
	public URL findResource(String resourceName) {
		URL url = getParent().getResource(resourceName);
		if(url==null) {
			try {
				byte[] resource = getRemoteResource(resourceName);
				if(resource==null) return null;
				File tmpFile = File.createTempFile("helios-resource", ".tmp");
				tmpFile.deleteOnExit();
				FileHelper.writeBytesToFile(tmpFile, false, resource);
				url = tmpFile.toURI().toURL();
			} catch (Exception e) {
				throw new RuntimeException("Internal error buffering resource [" + resourceName + "]", e);
			}
		}
		return url;
	}
	
	/**
	 * Invoked when a JMX notification occurs. 
	 * @param notif The notification.
	 * @param handback The notification handback object.
	 */
	@Override
	public void handleNotification(Notification notif, Object handback) {
		if(CP_CHANGED_TYPE.equals(notif.getType())) {
			notFoundClasses.clear();
			notFoundResources.clear();
		}
	}
	
	/**
	 * Returns the named classes bytes or null if it was not found.
	 * @param className The class name of the class to return.
	 * @return a classes bytes or null if it was not found.
	 */
	public byte[] getRemoteClassBytes(String className) {
		return classLoader.getRemoteClassBytes(className);
	}
	
	/**
	 * Returns the named class or null if it was not found.
	 * @param className The class name of the class to return.
	 * @return a class or null if it was not found.
	 */
	public Class<?> getRemoteClass(String className) {
		if(notFoundClasses.contains(className)) return null;
		byte[] classBytes = getRemoteClassBytes(className);
		if(classBytes==null) notFoundClasses.add(className);
		try {
			return makeClass(className, classBytes);
		} catch (Exception e) {
			throw new RuntimeException("Could not load class [" + className + "]", e);
		}
	}
	
	protected Class<?> makeClass(String className, byte[] classBytes) throws NotFoundException, CannotCompileException {
		ClassPool cp = ClassPool.getDefault();
		cp.appendClassPath(new LoaderClassPath(this));
		cp.appendClassPath(new ByteArrayClassPath(className, classBytes));
		CtClass ctClazz = cp.get(className);
		Class<?> clazz = cp.toClass(ctClazz, this, getClass().getProtectionDomain());
		ctClazz.detach();
		return clazz;
		
	}
	
	
	/**
	 * Returns the named resource as a byte array or null if it was not found.
	 * @param resourceName The resource name of the resource to return.
	 * @return a byte array or null if it was not found.
	 */	
	public byte[] getRemoteResource(String resourceName) {
		if(notFoundResources.contains(resourceName)) return null;
		byte[] resource = classLoader.getRemoteResource(resourceName);
		if(resource==null) notFoundResources.add(resourceName);
		return resource;
	}
	

}
