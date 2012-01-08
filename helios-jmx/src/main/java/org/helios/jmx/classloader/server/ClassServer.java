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
package org.helios.jmx.classloader.server;

import static org.helios.helpers.ClassHelper.nvl;

import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.URLHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: ClassServer</p>
 * <p>Description: Provides remote class loading services for remote clients.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.classloader.server.ClassServer</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
@JMXNotifications(notifications={
		@JMXNotification(description="HeliosJMX ClassServer JMX Notification", types={
				@JMXNotificationType(type="org.helios.classserver.pathchanged")
		})		
})

public class ClassServer extends ManagedObjectDynamicMBean {
	/**  */
	private static final long serialVersionUID = 6698578825965748863L;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** Externally specified supplementary URLs to class load from */
	protected final Set<URL> libUrls = new CopyOnWriteArraySet<URL>();
	/** The classloader used to find classes to serve */
	protected ClassServerLoader classLoader = null;
	/** The number of classes served */
	protected final AtomicLong servedClassCount = new AtomicLong(0L);
	/** The number of resources served */
	protected final AtomicLong servedResourceCount = new AtomicLong(0L);
	/** The number of class request misses */
	protected final AtomicLong classRequestMissCount = new AtomicLong(0L);
	/** The number of resource request misses */
	protected final AtomicLong resourceRequestMissCount = new AtomicLong(0L);
	/** The javassist class pool */
	protected final ClassPool classPool  = new ClassPool();
	

	/** The ObjectName used for registering this service */
	public static final ObjectName OBJECT_NAME = org.helios.classloading.RemoteJMXClassLoader.OBJECT_NAME;
	
	/**
	 * Starts the class server
	 */
	public void start() throws Exception {
		log.info("\n\t======================================\n\tStarting ClassServer\n\t======================================");
		classLoader = new ClassServerLoader(libUrls, getClass().getClassLoader());
		log.info("ClassLoader:" + classLoader.toString());
		classPool.appendClassPath(new LoaderClassPath(classLoader));
		this.reflectObject(this);
		JMXHelper.getHeliosMBeanServer().registerMBean(this, OBJECT_NAME);
		log.info("\n\t======================================\n\tStarted ClassServer\n\t======================================");
	}
	
	
	/**
	 * Returns an unmodifiable copy of the service's supplementary URLs
	 * @return an unmodifiable copy of the service's supplementary URLs
	 */
	@JMXAttribute(name="SupplementaryURLs", description="The service's supplementary URLs", mutability=AttributeMutabilityOption.READ_WRITE)
	public Set<URL> getSupplementaryURLs() {
		return Collections.unmodifiableSet(libUrls);
	}
	
	/**
	 * Adds a set of supplementary URLs
	 * @param urls a set of supplementary URLs to add to the service
	 */
	public void setSupplementaryURLs(Set<URL> urls) {
		if(urls!=null && urls.size()>0) {
			libUrls.addAll(urls);
			classLoader.addURLs(urls.toArray(new URL[0]));
		}
	}
	
	/**
	 * Returns the named class bytes or null if it was not found.
	 * @param className The class name of the class to return.
	 * @return a classes bytes or null if it was not found.
	 */
	@JMXOperation(name="getRemoteClassBytes", description="Returns the named class or null if it was not found")
	public byte[] getRemoteClassBytes(@JMXParameter(name="className", description="The class name of the class to return") String className)  {
		try {
			Class<?>  clazz = classLoader.loadClass(nvl(className, "Passed class name was null"));
			if(clazz!=null) {
				CtClass ctClazz = classPool.get(className);
				 byte[] byteCode = ctClazz.toBytecode();
				 ctClazz.detach();
				 servedClassCount.incrementAndGet();
				return byteCode;
			} else {
				classRequestMissCount.incrementAndGet();
				return null;				
			}
		} catch (Exception e) {
			if(log.isDebugEnabled()) log.debug("Request for class [" + className + "] could not find class");
			classRequestMissCount.incrementAndGet();
			return null;
		}
	}

	/**
	 * Returns the named resource as a byte array or null if it was not found.
	 * @param resourceName The resource name of the resource to return.
	 * @return a byte array or null if it was not found.
	 */
	@JMXOperation(name="getRemoteResource", description="Returns the named resource or null if it was not found")	
	public byte[] getRemoteResource(@JMXParameter(name="resourceName", description="The resource name of the resource to return") String resourceName) {
		try {
			URL resourceURL = classLoader.findResource(nvl(resourceName, "Passed resource name was null"));
			if(resourceURL!=null) {
				resourceURL = classLoader.getResource(resourceName);
			}
			if(resourceURL!=null) {
				servedResourceCount.incrementAndGet();
				return URLHelper.getBytesFromURL(resourceURL);
			} else {
				if(log.isDebugEnabled()) log.debug("Request for resource [" + resourceName + "] could not find resource");
				resourceRequestMissCount.incrementAndGet();
				return null;
			}			
		} catch (Exception e) {
			if(log.isDebugEnabled()) log.debug("Request for resource [" + resourceName + "] could not find resource");
			resourceRequestMissCount.incrementAndGet();
			return null;
		}
		
	}
	
	/**
	 * Resets the class server's counters
	 */
	@JMXOperation(name="resetCounts", description="Resets the class server's counters")
	public void resetCounts() {
		servedClassCount.set(0L);
		servedResourceCount.set(0L);
		classRequestMissCount.set(0L);
		resourceRequestMissCount.set(0L);
	}


	/**
	 * Returns the number of classes served
	 * @return the servedClassCount
	 */
	@JMXAttribute(name="ServedClassCount", description="Returns the number of classes served", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getServedClassCount() {
		return servedClassCount.get();
	}


	/**
	 * Returns the number of resources served
	 * @return the servedResourceCount
	 */
	@JMXAttribute(name="ServedResourceCount", description="Returns the number of resources served", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getServedResourceCount() {
		return servedResourceCount.get();
	}


	/**
	 * Returns the number of class request misses
	 * @return the classRequestMissCount
	 */
	@JMXAttribute(name="ClassRequestMissCount", description="Returns the number of class request misses", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getClassRequestMissCount() {
		return classRequestMissCount.get();
	}


	/**
	 * Returns the number of resource request misses
	 * @return the resourceRequestMissCount
	 */
	@JMXAttribute(name="ResourceRequestMissCount", description="Returns the number of resource request misses", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getResourceRequestMissCount() {
		return resourceRequestMissCount.get();
	}

}
