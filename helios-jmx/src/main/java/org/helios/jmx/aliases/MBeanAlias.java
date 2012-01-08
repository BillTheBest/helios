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
package org.helios.jmx.aliases;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.threads.ThreadFactoryBuilder;

/**
 * <p>Title: MBeanAlias</p>
 * <p>Description: A wrapper for an MBean used to cross-register an MBean in an alternate MBeanServer.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.aliases.MBeanAlias</code></p>
 * @TODO: Refresh MBeanInfo
 * @TODO: Delegate ObjectName  ("//")
 */

public class MBeanAlias extends NotificationBroadcasterSupport implements DynamicMBean, NotificationListener {
	/** An MBeanServerConnection to the MBeanServer where the original MBean is registered */
	protected MBeanServerConnection originServer = null;
	/** The server where this alias is registered */
	protected MBeanServer server = null;
	/** The JMX ObjectName of the original MBean */
	protected ObjectName originObjectName = null;
	/** The ObjectName of this alias */
	protected ObjectName objectName = null;
	/** The source MBean's class name */
	protected String className = null;
	/** The source MBean's description */
	protected String description = null;
	
	/** static class logger */
	protected static final Logger LOG = Logger.getLogger(MBeanAlias.class);
	
	/** The alias attribute infos keyed by attribute name */
	protected final Map<String, MBeanAttributeInfo> attrInfos = new HashMap<String, MBeanAttributeInfo>();
	/** The alias operation info keyed by operation signature hash code */
	protected final Map<String, MBeanOperationInfo> opInfos = new HashMap<String, MBeanOperationInfo>();
	/** The alias notification infos */
	protected final Set<MBeanNotificationInfo> notificationInfos = new HashSet<MBeanNotificationInfo>();
	/** The alias ctor infos */
	protected final Set<MBeanConstructorInfo> ctorInfos = new HashSet<MBeanConstructorInfo>();
	/** The alias mbeaninfo */
	protected MBeanInfo mbeanInfo = null;
	/** A set of ObjectNames that the alias registered notification listeners for */
	protected final Set<ObjectName> notificationTargets = new HashSet<ObjectName>();
	
	/** Indicates if the alias has been registered */
	protected final AtomicBoolean registered = new AtomicBoolean(false);

	
	/** The default threadPool for instances not provided one */
	private static volatile ExecutorService defaultThreadPool;
	/** Creation lock for the default threadPool */
	private static final Object threadPoolLock = new Object();
	
	
	/**
	 * Acquires a reference to or creates a new MBeanAlias
	 * @param targetMBeanServer The MBeanServer where the source MBean is registered
	 * @param originObjectName The source MBean's ObjectName
	 * @param thisMBeanServer The MBeanServer where the alias will be registered
	 */
	public static MBeanAlias getInstance(MBeanServerConnection targetMBeanServer, ObjectName originObjectName, MBeanServer thisMBeanServer) {
		AliasMBeanRegistry registry = AliasMBeanRegistry.getInstance(targetMBeanServer, thisMBeanServer);
		MBeanAlias alias = registry.getAlias(originObjectName);
		if(alias==null) {
			if(LOG.isDebugEnabled()) LOG.debug("Creating MBeanAlias [" + originObjectName + "] using registry:" + registry.toString());
			alias = new MBeanAlias(targetMBeanServer, originObjectName, thisMBeanServer);
		}		
		return alias;
	}
	
	/**
	 * Creates a new MBeanAlias
	 * @param targetMBeanServer The MBeanServer where the source MBean is registered
	 * @param originObjectName The source MBean's ObjectName
	 * @param thisMBeanServer The MBeanServer where the alias will be registered
	 */
	private MBeanAlias(MBeanServerConnection targetMBeanServer, ObjectName originObjectName, MBeanServer thisMBeanServer) {
		super(getDefaultExecutor());
		originServer = targetMBeanServer;
		server = thisMBeanServer;
		this.originObjectName = originObjectName;
		this.objectName = originObjectName;
		try {
			initializeAlias();			
		} catch (Exception e) {
			throw new RuntimeException("Failed to create AliasMBean for [" + originObjectName + "]", e);
		}			
	}
	
	/**
	 * Initializes the alias.
	 * Currently extracts all mbean exposed features into their own collections which is not strictly necessary
	 * for the simplified alias, but will be required for target MBeans with dynamically updating meta-data.
	 * @return true if the alias was registered successfully 
	 * @throws Exception
	 */
	protected boolean initializeAlias() throws Exception {
		try {
			MBeanInfo remoteInfo = originServer.getMBeanInfo(originObjectName); 			
			this.className = remoteInfo.getClassName();
			StringBuilder targetDisplay = new StringBuilder(" MBean Alias to [");
			targetDisplay.append(originObjectName).append("] at ");
			targetDisplay.append(originServer.getDefaultDomain()).append(" in ");
			try {
				targetDisplay.append(originServer.getAttribute(JMXHelper.objectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "Name"));
			} catch (Exception e) {
				targetDisplay.append(originServer.getAttribute(AliasMBeanRegistry.MBEANSERVER_DELEGATE_OBJECTNAME, AliasMBeanRegistry.MBEANSERVER_DELEGATE_SERVERID));
			}
			for(MBeanAttributeInfo ainfo: remoteInfo.getAttributes()) {
				attrInfos.put(ainfo.getName(), ainfo);
			}
			for(MBeanOperationInfo oinfo: remoteInfo.getOperations()) {
				MBeanParameterInfo[] pinfos = oinfo.getSignature();
				String[] signature = new String[pinfos.length];
				for(int i = 0; i < pinfos.length; i++) {
					signature[i] = pinfos[i].getType();
				}
				opInfos.put(ManagedObjectDynamicMBean.hashOperationName(oinfo.getName(), signature), oinfo);
			}
			Collections.addAll(notificationInfos, remoteInfo.getNotifications());
			Collections.addAll(ctorInfos, remoteInfo.getConstructors());		
			this.description = remoteInfo.getDescription() + targetDisplay.toString();	
			mbeanInfo = new MBeanInfo(this.className, this.description, remoteInfo.getAttributes(), remoteInfo.getConstructors(), remoteInfo.getOperations(), remoteInfo.getNotifications());
			if(notificationInfos.size()>0) {
				originServer.addNotificationListener(originObjectName, this, null, null);
				notificationTargets.add(originObjectName);
			}
			server.registerMBean(this, originObjectName);
			AliasMBeanRegistry.getInstance(originServer, server).registerAlias(this);
			return true;
		} catch (Exception e) {
			LOG.error("Failed to initialize mbean alias for [" + originObjectName + "]", e);
			return false;
		}
	}
	
	
	/**
	 * Creates the default thread pool
	 * @return an executor
	 */
	static ExecutorService getDefaultExecutor() {
		if(defaultThreadPool==null) {
			synchronized(threadPoolLock) {
				if(defaultThreadPool==null) {
					defaultThreadPool = new ThreadPoolExecutor(1, 5, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1200, false), 
						ThreadFactoryBuilder.newBuilder()
							.setThreadGroupNamePrefix("MBeanAlias Notification Forwarder DefaultExecutor ThreadGroup")
							.setThreadNamePrefix("MBeanAlias Notification Forwarder DefaultExecutor Thread")
							.build(), 
							new RejectedExecutionHandler() {
								public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
									System.err.println("MBeanAlias Notification Forwarder DefaultExecutor rejected submited runnable [" + r.toString() + "]");
								}
						
							}
					);
						
				}
			}
		}
		return defaultThreadPool;
	}
	
	
	
	/**
	 * Obtain the value of a specific attribute of the Dynamic MBean.
	 * @param attribute The name of the attribute
	 * @return The value of the attribute
	 * @throws AttributeNotFoundException
	 * @throws MBeanException
	 * @throws ReflectionException
	 */
	@Override
	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		try {
			return originServer.getAttribute(originObjectName, attribute);
		} catch (InstanceNotFoundException e) {
			unregister();
			throw new MBeanException(e, "The source MBean [" + originObjectName + "] is no longer registered");
		} catch (IOException ioe) {
			throw new MBeanException(ioe, "Failure acquiring attribute from the source MBean [" + originObjectName + "]");
		}
	}
	
	
	/**
	 * Unregisters this alias and removes all notification listeners registered with the origin MBeanServer
	 */
	protected void unregister() {
		registered.set(false);
		AliasMBeanRegistry.getInstance(originServer, server).unregisterAlias(this);
		try { server.unregisterMBean(objectName); } catch (Exception e) {}
		for(ObjectName ntarget: notificationTargets) {
			try { originServer.removeNotificationListener(ntarget, this); } catch (Exception e) {}
		}
	}
	

	/**
	 * Get the values of several attributes of the Dynamic MBean.
	 * @param attributes The attribute names of the attributes to fetch
	 * @return The list of attributes retrieved.
	 */
	@Override
	public AttributeList getAttributes(String[] attributes) {
		try {
			return originServer.getAttributes(originObjectName, attributes);
		} catch (InstanceNotFoundException e) {
			unregister();		
			return new AttributeList(0);
		} catch (Exception e) {			
			return new AttributeList(0);
		}
	}

	/**
	 * Provides the exposed attributes and actions of the Dynamic MBean using an MBeanInfo object.
	 * @return An instance of MBeanInfo allowing all attributes and actions exposed by this Dynamic MBean to be retrieved.
	 */
	@Override
	public MBeanInfo getMBeanInfo() {
		return mbeanInfo;
	}

	/**
	 * Allows an action to be invoked on the Dynamic MBean. 
	 * @param actionName The name of the action to be invoked.
	 * @param params An array containing the parameters to be set when the action is invoked.
	 * @param signature An array containing the signature of the action. The class objects will be loaded through the same class loader as the one used for loading the MBean on which the action is invoked.
	 * @return The object returned by the action, which represents the result of invoking the action on the MBean specified.
	 * @throws MBeanException
	 * @throws ReflectionException
	 */
	@Override
	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
		try {
			return originServer.invoke(originObjectName, actionName, params, signature);
		} catch (InstanceNotFoundException e) {
			unregister();		
			throw new MBeanException(e, "The source MBean [" + originObjectName + "] is no longer registered");
		} catch (Exception e) {			
			throw new MBeanException(e, "Failed to invoke operation [" + actionName + "] against the source MBean [" + originObjectName + "]");
		}
	}
	
	

	/**
	 * Set the value of a specific attribute of the Dynamic MBean.
	 * @param attribute The attribute to set
	 * @throws AttributeNotFoundException
	 * @throws InvalidAttributeValueException
	 * @throws MBeanException
	 * @throws ReflectionException
	 */
	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		try {
			originServer.setAttribute(originObjectName, attribute);
		} catch (InstanceNotFoundException e) {
			unregister();		
			throw new MBeanException(e, "The source MBean [" + originObjectName + "] is no longer registered");
		} catch (Exception e) {			
			throw new MBeanException(e, "Failed to set attribute [" + attribute + "] on the source MBean [" + originObjectName + "]");
		}
	}

	/**
	 * Sets the values of several attributes of the Dynamic MBean.
	 * @param attributes The attribute list to set
	 * @return the attribute list of attributes that were successfully set
	 */
	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		try {
			return originServer.setAttributes(originObjectName, attributes);
		} catch (InstanceNotFoundException e) {
			unregister();		
			return new AttributeList(0);
		} catch (Exception e) {			
			return new AttributeList(0);
		}
	}


	/**
	 * Invoked when a JMX notification occurs. The implementation of this method should return as soon as possible, to avoid blocking its notification broadcaster. 
	 * @param notification The notification modified to designate the alias as the source
	 * @param handback The handback
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		notification.setSource(objectName);		
		sendNotification(notification);
	}


	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((objectName == null) ? 0 : objectName.hashCode());
		return result;
	}


	/**
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MBeanAlias other = (MBeanAlias) obj;
		if (objectName == null) {
			if (other.objectName != null)
				return false;
		} else if (!objectName.equals(other.objectName))
			return false;
		return true;
	}

}
