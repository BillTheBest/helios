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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerNotification;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;

/**
 * <p>Title: AliasMBeanRegistry</p>
 * <p>Description: A registry of alias MBeans</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.aliases.AliasMBeanRegistry</code></p>
 */

public class AliasMBeanRegistry extends NotificationBroadcasterSupport implements NotificationListener, AliasMBeanRegistryMBean, MBeanRegistration {
	/** The MBeanServer to get the alias registry for, that is, the remote MBeanServer that the aliases are being created for */
	private final MBeanServerConnection targetMBeanServer;
	/** The MBeanServer ID  of the target MBeanServer */
	private final String mbeanServerId;	
	/** The alias hosting MBeanServer where the aliases are being registered */
	private final MBeanServer thisMBeanServer;
	/** The MBeanServer ID  of the this MBeanServer */
	private final String thisMbeanServerId;	

	/** The Runtime name of the target MBeanServer */
	private final String targetRuntimeName;	
	/** The Runtime name of the loca MBeanServer */
	private final String localRuntimeName;
	
	/** The default domain of the target MBeanServer */
	private final String targetDomain;	
	/** The default domain of the loca MBeanServer */
	private final String localDomain;	
	
	/** The serial number for this registry */
	private final long registrySerial;
	
	
	
	/** Registered aliases for this MbeanServer */
	private final Map<ObjectName, MBeanAlias> aliases = new ConcurrentHashMap<ObjectName, MBeanAlias>();
	/** Registered ObjectName filters fore dynamic registrations for this MbeanServer */
	private final Set<ObjectName> filterMatches = new CopyOnWriteArraySet<ObjectName>();
	/** The threadPool instance for processing notifications asynchronously */
	private final ExecutorService defaultThreadPool;
	/** Instance logger */
	protected final Logger log;
	/** static class logger */
	protected static final Logger LOG = Logger.getLogger(AliasMBeanRegistry.class);
	
	

	/** A map of alias registry instances keyed by mbean server Ids */
	protected static final Map<String, AliasMBeanRegistry> registries = new ConcurrentHashMap<String, AliasMBeanRegistry>();	
	/** The MBeanServer delegate JMX ObjectName */
	public static final ObjectName MBEANSERVER_DELEGATE_OBJECTNAME = JMXHelper.objectName("JMImplementation:type=MBeanServerDelegate");
	/** The MBeanServer delegate server Id attribute name */
	public static final String MBEANSERVER_DELEGATE_SERVERID = "MBeanServerId";
	/** The registries JMX ObjectName */
	public static final ObjectName ALIAS_REGISTRY_OBJECTNAME = JMXHelper.objectName(AliasMBeanRegistry.class.getPackage().getName(), "service", AliasMBeanRegistry.class.getSimpleName());
	/** The RuntimeMXBean JMX ObjectName */
	public static final ObjectName RUNTIME_MXBEAN_OBJECTNAME = JMXHelper.objectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
	/** The RuntimeMXBean runtime attribute name */
	public static final String RUNTIME_MXBEAN_NAME = "Name";
	/** A factory for registry serial numbers */
	private static final AtomicLong serial = new AtomicLong(0);
	
	/**
	 * Procesurally cross-registers all MBeans from the target MBeanServer into the local MBeanServer.
	 * If the target MBean's ObjectName is already bound in the local MBeanServer, it will be ignored.
	 * @param localDomain The default domain name of the MBeanServer where the aliases will be registered
	 * @param targetDomain The default domain name of the MBeanServer where the MBeans to be cross registered are registered
	 * @param filter The string of an ObectName filter that specifies which MBeans in the target MBeanServer will be cross-registered. A null value will be treated as all MBeans (<b><code>*:*</code></b>).	 * 
	 */
	public static void crossRegister(String localDomain, String targetDomain, String filter) {
		String domain = null;
		MBeanServer localMBeanServer = null;
		MBeanServer targetMBeanServer = null;
		ObjectName on = JMXHelper.objectName(filter==null ? "*:*": filter);
		try {
			domain = localDomain;
			localMBeanServer = JMXHelper.getLocalMBeanServer(false, domain);
			domain = targetDomain;
			targetMBeanServer = JMXHelper.getLocalMBeanServer(false, domain);			
		} catch (Exception e) {
			throw new RuntimeException("Failed to resolve the domain [" + domain + "] to an MBeanServer", e);
		}
		crossRegister(localMBeanServer, targetMBeanServer, on);
	}
	
	
	/**
	 * Procesurally cross-registers all MBeans from the target MBeanServer into the local MBeanServer.
	 * If the target MBean's ObjectName is already bound in the local MBeanServer, it will be ignored.
	 * @param localMBeanServer The MBeanServer where the aliases will be registered
	 * @param targetMBeanServer The MBeanServer where the MBeans to be cross registered are registered 
	 */
	public static void crossRegister(MBeanServer localMBeanServer, MBeanServerConnection targetMBeanServer) {
		crossRegister(localMBeanServer, targetMBeanServer, null);
	}
	
	/**
	 * Procesurally cross-registers MBeans from the target MBeanServer into the local MBeanServer.
	 * If the target MBean's ObjectName is already bound in the local MBeanServer, it will be ignored.
	 * @param localMBeanServer The MBeanServer where the aliases will be registered
	 * @param targetMBeanServer The MBeanServer where the MBeans to be cross registered are registered
	 * @param filter An ObectName filter that specifies which MBeans in the target MBeanServer will be cross-registered. A null value will be treated as all MBeans (<b><code>*:*</code></b>).	 * 
	 */
	public static void crossRegister(MBeanServer localMBeanServer, MBeanServerConnection targetMBeanServer, ObjectName filter) {
		if(localMBeanServer==null) throw new IllegalArgumentException("Passed localMBeanServer was null", new Throwable());
		if(targetMBeanServer==null) throw new IllegalArgumentException("Passed targetMBeanServer was null", new Throwable());
		if(filter==null) {
			filter = JMXHelper.objectName("*:*");
		}
		long cnt = 0;
		try {
			for(ObjectName target: targetMBeanServer.queryNames(filter, null)) {
				if(localMBeanServer.isRegistered(target)) continue;
				try {
					MBeanAlias.getInstance(targetMBeanServer, target, localMBeanServer);
					cnt++;
					if(LOG.isDebugEnabled()) LOG.debug("CrossRegistered [" + target + "]");
				} catch (Exception e) {
					LOG.warn("Failed to cross-register [" + target + "]", e);
				}
			}
			LOG.info("Cross Registered [" + cnt + "] MBeans");
		} catch (Exception e) {
			throw new RuntimeException("Failed to crossRegister [" + filter + "]", e);
		}
	}
	
	
	/**
	 * Creates a new  AliasMBeanRegistry and registers it.
	 * @param thisMBeanServer The alias hosting MBeanServer where the aliases are being registered
	 * @param targetMBeanServer The MBeanServer to get the alias registry for, that is, the remote MBeanServer that the aliases are being created for
	 * @param mbeanServerId The MBeanServer ID  of the target MBeanServer
	 * @throws InstanceNotFoundException
	 * @throws NotCompliantMBeanException 
	 * @throws InstanceAlreadyExistsException 
	 * @throws IOException 
	 * @throws ReflectionException 
	 * @throws MBeanException 
	 * @throws AttributeNotFoundException 
	 */
	private AliasMBeanRegistry(MBeanServer thisMBeanServer, MBeanServerConnection targetMBeanServer, String mbeanServerId) throws InstanceNotFoundException, InstanceAlreadyExistsException, NotCompliantMBeanException, IOException, AttributeNotFoundException, MBeanException, ReflectionException {
		super();
		log = Logger.getLogger(getClass().getName() + "." + targetMBeanServer.getDefaultDomain());
		this.registrySerial = serial.incrementAndGet();
		this.mbeanServerId = mbeanServerId;
		this.targetMBeanServer = targetMBeanServer;
		this.thisMBeanServer = thisMBeanServer;
		thisMbeanServerId = JMXHelper.getAttribute(this.thisMBeanServer, MBEANSERVER_DELEGATE_OBJECTNAME, MBEANSERVER_DELEGATE_SERVERID, this.thisMBeanServer.toString());
		targetRuntimeName = JMXHelper.getAttribute(this.targetMBeanServer, RUNTIME_MXBEAN_OBJECTNAME, RUNTIME_MXBEAN_NAME, "Target Unknown");
		localRuntimeName = ManagementFactory.getRuntimeMXBean().getName();
		targetDomain = getDomainName(targetMBeanServer); 
		localDomain = getDomainName(thisMBeanServer);
		defaultThreadPool = MBeanAlias.getDefaultExecutor();
		thisMBeanServer.registerMBean(this, ALIAS_REGISTRY_OBJECTNAME);
		registerForMBeanRegistrationEvents();
	}
	
	/** A reference to the actual platform agent */
	private static final MBeanServer platformAgent = ManagementFactory.getPlatformMBeanServer();
	
	
	public static String getDomainName(MBeanServerConnection conn) {
		if(conn==null) throw new IllegalArgumentException("Passed MBeanServerConnection was null", new Throwable());
		try {
			return conn.getDefaultDomain();
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to get domain name for [" + conn + "]", e);
		}
	}
	
//	/**
//	 * Validates an MBeanServerConnection before it is used in a registry:<ul>
//	 * 	<li>Assigns a slightly more useful domain name for delegates that have a null default domain<li>
//	 * 	<li>If the domain name is modified, the passed target will be unregistered and replaced with a wrapped instance</li>
//	 * </ul>
//	 * @param target The actual MBeanServerConnection to validate
//	 * @return the MBeanServerConnection to use in the registry, which may be the same instance as the one passed in, or it may be wrapped.
//	 */
//	public static MBeanServerConnection validateMBeanServerConnection(MBeanServerConnection target) {
//		try {
//			MBeanServerConnection swap = target;
//			String innerDomain = target.getDefaultDomain();
//			if(innerDomain==null) {			
//				if(System.identityHashCode(target)==System.identityHashCode(platformAgent)) {
//					innerDomain = "DefaultDomain";
//				} else {
//					innerDomain = "Unknown-" + target.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(target));
//				}
//				if(target instanceof MBeanServer) {
//					try {
//						swap = MBeanServerBuilder.newMBeanServer(innerDomain);
//						//ArrayList<MBeanServer> arrList = (ArrayList<MBeanServer>)PrivateAccessor.getStaticFieldValue(MBeanServerFactory.class, "mBeanServerList");
//						// which server do we put in ?
//					} catch (Exception e) {
//						// should log warning, but not critical
//					}
//				}
//			}
//			return swap;
//		} catch (Exception e) {
//			throw new RuntimeException("Failed to validate mbean server connection", e);
//		}
//	}
	
	
	
	
	/**
	 * Acquires the AliasMBeanRegistry for the passed MBeanServer
	 * @param targetMBeanServer The MBeanServer to get the alias registry for, that is, the remote MBeanServer that the aliases are being created for
	 * @param thisMBeanServer The alias hosting MBeanServer where the aliases are being registered
	 * @return The existing or newly created AliasMBeanRegistry for the passed MBeanServer
	 */
	public static AliasMBeanRegistry getInstance(MBeanServerConnection targetMBeanServer, MBeanServer thisMBeanServer) {
		if(targetMBeanServer==null) throw new IllegalArgumentException("Passed MBeanServerConnection was null", new Throwable());
		String serverId = null;
		try {
			serverId = targetMBeanServer.getAttribute(MBEANSERVER_DELEGATE_OBJECTNAME, MBEANSERVER_DELEGATE_SERVERID).toString();
			AliasMBeanRegistry registry = registries.get(serverId);
			if(registry==null) {
				synchronized(registries) {
					registry = registries.get(serverId);
					if(registry==null) {
						registry = new AliasMBeanRegistry(thisMBeanServer, targetMBeanServer, serverId);
						registries.put(serverId, registry);
					}
				}
			}
			return registry;					
		} catch (Exception e) {
			throw new RuntimeException("Failed to register AliasMBeanRegistry with MBeanServer [" + serverId + "]", e);
		}		
	}
	
	
	
	/**
	 * Registers a new alias
	 * @param alias The alias to register
	 */
	public void registerAlias(MBeanAlias alias) {
		if(alias==null) throw new IllegalArgumentException("Passed MBeanAlias was null", new Throwable());
		if(!aliases.containsKey(alias.objectName)) {
			synchronized(aliases) {
				if(!aliases.containsKey(alias.objectName)) {
					aliases.put(alias.objectName, alias);
				}
			}
		}		
	}
	
	/**
	 * Returns the MBeanAlias registered with the passed ObjectName
	 * @param aliasObjectName The ObjectName of the target AliasMBean to acquire
	 * @return an MBeanAlias or null if it was not registered
	 */
	public MBeanAlias getAlias(ObjectName aliasObjectName) {
		if(aliasObjectName==null) throw new IllegalArgumentException("Passed MBeanAlias ObjectName was null", new Throwable());
		return aliases.get(aliasObjectName);
	}
	
	/**
	 * Determines if the passed ObjectName represents a registered MBeanAlias
	 * @param aliasObjectName An ObjectName to test for
	 * @return true if an MBeanAlias with the passed ObjectName has been registered
	 */
	public boolean isRegistered(ObjectName aliasObjectName) {
		if(aliasObjectName==null) throw new IllegalArgumentException("Passed MBeanAlias ObjectName was null", new Throwable());
		return aliases.containsKey(aliasObjectName);
	}

	/**
	 * Determines if the passed alias is registered
	 * @param alias The alias to test for
	 * @return true if the MBeanAlias is registered
	 */	
	public boolean isRegistered(MBeanAlias alias) {
		if(alias==null) throw new IllegalArgumentException("Passed MBeanAlias was null", new Throwable());
		return aliases.containsKey(alias.objectName);
	}
	
	
	/**
	 * Registers a new dynamic registration filter
	 * @param filter An ObjectName filter
	 */
	public void registerDynamicRegistrationFilter(ObjectName filter) {
		if(filter==null) throw new IllegalArgumentException("Passed ObjectName filter was null", new Throwable());
		this.filterMatches.add(filter);
	}
	
	
	/**
	 * Unregisters an alias
	 * @param alias
	 */
	public void unregisterAlias(MBeanAlias alias) {
		if(alias==null) throw new IllegalArgumentException("Passed MBeanAlias was null", new Throwable());
		aliases.remove(alias.objectName);
	}
	
	
	/**
	 * Registers to be notified of new MBean registration or MBean unregistration events from the source MBeanServer
	 * @throws InstanceNotFoundException
	 * @throws IOException 
	 */
	private void registerForMBeanRegistrationEvents() throws InstanceNotFoundException, IOException {
		targetMBeanServer.addNotificationListener(MBEANSERVER_DELEGATE_OBJECTNAME, this, null, mbeanServerId);
	}
	
	/**
	 * Unregisters for notifications from the source MBeanServer
	 * @throws InstanceNotFoundException
	 * @throws IOException 
	 */
	private void unregisterForMBeanRegistrationEvents() throws InstanceNotFoundException, IOException {
		try {
			targetMBeanServer.removeNotificationListener(MBEANSERVER_DELEGATE_OBJECTNAME, this);
		} catch (ListenerNotFoundException e) {
		}
	}
	
	/**
	 * Returns a set of the JMX ObjectNames of the registered aliases
	 * @return a set of ObjectNames
	 */
	public Set<ObjectName> getAliases() {
		return Collections.unmodifiableSet(aliases.keySet());
	}
	
	/**
	 * Returns a set of the JMX ObjectName filters of the registered dynamic registration MBean filters
	 * @return a set of ObjectNames
	 */
	public Set<ObjectName> getDynamicRegistrationFilters() {
		return Collections.unmodifiableSet(filterMatches);
	}
	
	
	
	/**
	 * Invoked when a JMX notification occurs. The implementation of this method should return as soon as possible, to avoid blocking its notification broadcaster. 
	 * @param notification The notification modified to designate the alias as the source
	 * @param handback The handback
	 */
	@Override
	public void handleNotification(final Notification notification, final Object handback) {
		defaultThreadPool.execute(new Runnable(){
			public void run() {
				if(mbeanServerId.equals(handback)) {
					if(notification instanceof MBeanServerNotification) {
						MBeanServerNotification msn = (MBeanServerNotification)notification;
						if(MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(msn.getType())) {
							ObjectName on = msn.getMBeanName();
							for(ObjectName match: filterMatches) {
								if(match.apply(on) && !thisMBeanServer.isRegistered(on)) {
									if(log.isDebugEnabled()) log.debug("Creating AliasMBean for [" + on + "]");
									//log.info("Creating AliasMBean for [" + on + "]" );
									MBeanAlias.getInstance(targetMBeanServer, on, thisMBeanServer);
								}
							}
						} else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(msn.getType())) {
							MBeanAlias alias = aliases.remove(msn.getMBeanName());
							if(alias!=null) {
								//if(log.isDebugEnabled()) log.debug("Unregistering AliasMBean for [" + alias.objectName + "]");
								log.info("Unregistering AliasMBean for [" + alias.objectName + "]");
								alias.unregister();
							}
						}
					}
				}				
			}
		});
	}

	/**
	 * Allows the MBean to perform any operations needed after having been unregistered in the MBean server.
	 */
	@Override
	public void postDeregister() {
		try {
			this.unregisterForMBeanRegistrationEvents();
		} catch (Exception e) {
		}		
	}

	/**
	 * Allows the MBean to perform any operations needed after having been registered in the MBean server or after the registration has failed.
	 * @param registrationDone true if registration was successful
	 */
	@Override
	public void postRegister(Boolean registrationDone) {
	}

	/**
	 * Allows the MBean to perform any operations it needs before being unregistered by the MBean server.
	 * @throws Exception
	 */
	@Override
	public void preDeregister() throws Exception {
	}

	/**
	 * Allows the MBean to perform any operations it needs before being registered in the MBean server.
	 * @param server The MBean server in which the MBean will be registered.
	 * @param name The object name of the MBean.
	 * @return The name under which the MBean is to be registered. 
	 * @throws Exception
	 */
	@Override
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		return name;
	}

	/**
	 * Returns the target MBeanServer ID
	 * @return the mbeanServerId
	 */
	public String getTargetMBeanServerId() {
		return mbeanServerId;
	}

	/**
	 * Returns the local MBeanServer ID
	 * @return the thisMbeanServerId
	 */
	public String getLocalMBeanServerId() {
		return thisMbeanServerId;
	}


	/**
	 * Returns the JVM runtime name of the target MBeanServer
	 * @return the targetRuntimeName
	 */
	public String getTargetRuntimeName() {
		return targetRuntimeName;
	}


	/**
	 * Returns the JVM runtime name of the local MBeanServer
	 * @return the localRuntimeName
	 */
	public String getLocalRuntimeName() {
		return localRuntimeName;
	}




	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("AliasMBeanRegistry [")
	    	.append(TAB).append("registrySerial# = ").append(this.registrySerial)
	        .append(TAB).append("targetmbeanServerId = ").append(this.mbeanServerId)
	        .append(TAB).append("localMbeanServerId = ").append(this.thisMbeanServerId)
	        .append(TAB).append("targetRuntimeName = ").append(this.targetRuntimeName)
	        .append(TAB).append("localRuntimeName = ").append(this.localRuntimeName)
	        .append(TAB).append("targetDomain = ").append(this.targetDomain)
	        .append(TAB).append("localDomain = ").append(this.localDomain)	        
	        .append(TAB).append("aliases = ").append(this.aliases.size())
	        .append("\n]");    
	    return retValue.toString();
	}


	/**
	 * Returns the default domain of the target MBeanServer
	 * @return the targetDomain
	 */
	public String getTargetDomain() {
		return targetDomain;
	}

	/**
	 * Returns the default domain of the local MBeanServer
	 * @return the localDomain
	 */
	public String getLocalDomain() {
		return localDomain;
	}




	/**
	 * This registry's unique serial number
	 * @return the registrySerial
	 */
	public long getRegistrySerial() {
		return registrySerial;
	}

}
