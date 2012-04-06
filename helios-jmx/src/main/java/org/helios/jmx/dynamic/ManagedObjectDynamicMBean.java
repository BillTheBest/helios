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
package org.helios.jmx.dynamic;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.JMXHelperExtended;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXField;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.dynamic.container.AttributeContainer;
import org.helios.jmx.dynamic.container.FieldContainer;
import org.helios.jmx.dynamic.container.OperationContainer;
import org.helios.jmx.dynamic.core.CoreExtensionManagedObject;
import org.helios.jmx.dynamic.core.DynamicMBeanCoreFunctions;
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.helios.jmxenabled.threads.ExecutorMBeanPublisher;
import org.helios.jmxenabled.threads.NotificationSender;
import org.helios.jmxenabled.threads.TaskRejectionPolicy;
/**
 * <p>Title: ManagedObjectDynamicMBean</p>
 * <p>Description: DynamicMBean implementation that dynamically exposes operations and attributes of passed objects.
 * The bean exposes managed objects through JMX via the <code>reflectObject</code> methods. There are several options supported for types of classes that can be exposed.<ul>
 * <li>HeliosJMX Annotated Classes. This allows for the most customizable JMX exposure.</li>
 * <li>Un-annotated classes. These are exposed using a simple defaulted java bean model.</li>
 * <li>Hybrid. The hybrid model exposes a simple un-annotated object but can use directives supplied by a secondary annotated interface.</li>
 * </ul></p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */

public class ManagedObjectDynamicMBean extends NotificationBroadcasterSupport implements DynamicMBean, MBeanRegistration, Serializable, NotificationSender {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1077748676265011430L;

	public static final String DEFAULT_DESCRIPTION = "ManagedObjectDynamicMBean";
	
	/**	A map of the current MBean's Attributes and target managed objects. */
	protected Map<String, AttributeContainer> attributes = new ConcurrentHashMap<String, AttributeContainer>();
	/**	A map of the current MBean's Operations and target managed objects. */
	protected Map<String, OperationContainer> operations = new ConcurrentHashMap<String, OperationContainer>();	
	/**	A map of the current MBean's Notifications and target managed objects. */
	protected List<MBeanNotificationInfo> notifications = Collections.synchronizedList(new ArrayList<MBeanNotificationInfo>());
	/**	A map of the current MBean's Constructors. */
	protected static MBeanConstructorInfo[] constructors = new MBeanConstructorInfo[0];		
	
	/**	A reference to the MBeanServer that this MBean is registered in */
	protected transient MBeanServer server = null;
	/**	The MBean's object name */
	protected ObjectName objectName = null;
	/**	The MBeanServer's agent Id */
	protected String agentId = "";
	/** An optional MBean description */
	protected String mbeanDescription = DEFAULT_DESCRIPTION;
	/**	The dynamic MBean MBeanInfo */
	protected MBeanInfo mbeanInfo = null;
	/**	Empty object array */
	protected final static  Object[] NO_ARGS = {};
	/**	Empty class array */
	protected final static  Class<?>[] NO_SIG = {};
	/**	Empty string array */
	protected final static  String[] NO_STR_SIG = {};
//	/** Static class Logger */
//	protected static final Logger LOG = Logger.getLogger("DefaultNotificationThreadPool");
	/** An index of the hash codes of already reflected objects */
	protected Set<Integer> registeredObjectIndex = new HashSet<Integer>();
	/** A flag indicating the MBeanServer registration status of this MODB */
	protected boolean registered = false;
	/** A notification sequence number factory */
	protected final AtomicLong notificationSequence = new AtomicLong(0L);
	/** A defined attribute exposure directive map of booleans keyed by the attribute name */
	protected final Map<String, Boolean> exposures = new ConcurrentHashMap<String, Boolean>();
	
	/** Indicates if reflectObject has been called at least once */
	protected final AtomicBoolean reflected = new AtomicBoolean(false);
	
	/** pattern to extract <code>{f:*}</code> or <code>{a:*}</code> patterns from an annotation value */ 
	protected static Pattern dynamicTokenPattern = Pattern.compile("(\\{[F|A]:\\w+\\})", Pattern.CASE_INSENSITIVE );
	/** patern to extract the type and value of a dynamic token */
	protected static Pattern dynamicTokenValues = Pattern.compile("\\{([f,a]):(.*)\\}", Pattern.CASE_INSENSITIVE);
	/** The default threadPool for instances not provided one */
	protected static volatile ExecutorService defaultThreadPool;
	/** Creation lock for the default threadPool */
	private static final Object threadPoolLock = new Object();
	
	/** The ObjectName of the default notification broadcaster thread pool */
	public static final ObjectName DEFAULT_NOTIFICATION_BROADCASTER_TP_ON = JMXHelper.objectName("org.helios.jmx:service=ThreadPool,type=DefaultNotificationBroadcaster");

	
	static {
		Constructor<ManagedObjectDynamicMBean>[] consts = (Constructor<ManagedObjectDynamicMBean>[])ManagedObjectDynamicMBean.class.getConstructors();
		constructors = new MBeanConstructorInfo[consts.length];		
		for(int i = 0; i < consts.length; i++) {
			constructors[i] = new MBeanConstructorInfo("Constructor #" + i+1, consts[i]);
		}
	}
	
	/**
	 * Creates the default thread pool
	 * @return an executor
	 */
	@SuppressWarnings("cast")
	private static ExecutorService getDefaultExecutor() {
		if(defaultThreadPool==null) {
			synchronized(threadPoolLock) {
				if(defaultThreadPool==null) {
					defaultThreadPool = (ThreadPoolExecutor) ExecutorBuilder.newBuilder()
					.setCoreThreads(1)
					.setMaxThreads(5)
					.setKeepAliveTime(60000)
					.setDaemonThreads(true)
					.setExecutorType(true)
					.setFairSubmissionQueue(false)
					.setPolicy(TaskRejectionPolicy.CALLERRUNS)
					.setPrestartThreads(2)
					.setTaskQueueSize(200)
					.setThreadGroupName("DefaultNotificationBroadcaster")
//					.setPoolObjectName("org.helios.jmx:service=ThreadPool,type=DefaultNotificationBroadcaster")
//					.setJmxDomains("DefaultDomain")
					.build();
					ExecutorMBeanPublisher.register(defaultThreadPool, DEFAULT_NOTIFICATION_BROADCASTER_TP_ON);
				}
			}
		}
		return defaultThreadPool;
	}
	
	
	public ManagedObjectDynamicMBean() {
		super(getDefaultExecutor());
		addCoreFunctions();
		autoRegister();
		if(!reflected.get()) {
			this.reflectObject(this);
		}
	}
	
	/**
	 * Attempts an auto-register of this MODB
	 */
	protected void autoRegister() {
		try {
			JMXManagedObject mo = this.getClass().getAnnotation(JMXManagedObject.class);
			if(mo!=null) {
				String oName = mo.objectName();
				String[] domains = mo.domains();
				if(!oName.equals(JMXManagedObject.NULL_OBJECT_NAME) && domains.length>0) {
					ObjectName on = JMXHelper.objectName(oName);
					if(on.isPattern()) return;
					for(String domain: domains) {
						if(domain!=null && domain.length()>0) {
							MBeanServer server = JMXHelper.getLocalMBeanServer(true, domain);
							if(server!=null) {
								try { 
									if(!server.isRegistered(on)) {
										server.registerMBean(this, on);
									}
								} catch (Exception e) {
									elog("Warning: Failed to auto register [" + objectName + "] in domain [" + domain + "]:" + e);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			elog("Warning: Failed to auto register [" + objectName + "]:" + e);
		}
	}
	
	/**
	 * Returns the next notification sequence Id.
	 * @return the next notification sequence Id.
	 */
	public long nextNotificationSequence() {
		return notificationSequence.incrementAndGet();
	}
	
	
	/**
	 * @param args
	 */
	public ManagedObjectDynamicMBean(Object...args) {
		this();
		for(Object o: args) {
			reflectObject(o);
		}
		updateMBeanInfo();
	}
	
	/**
	 * @param description
	 * @param args
	 */
	public ManagedObjectDynamicMBean(String description, Object...args) {
		this(args);
		mbeanDescription = description;
	}
	
	/**
	 * @param description
	 */
	public ManagedObjectDynamicMBean(String description) {
		this();
		mbeanDescription = description;
	}
	
	/**
	 * @param name
	 * @param description
	 * @param notifTypes
	 */
	public void registerNotificationSupport(String name, String description, String...notifTypes) {
		MBeanNotificationInfo notificationInfo = new MBeanNotificationInfo(notifTypes, name, description);
		notifications.add(notificationInfo);
		updateMBeanInfo();
	}

	
	/**
	 * Creates a new MBeanInfo from the configured collections of attributes, operations and notifications.
	 */
	public void updateMBeanInfo() {
		mbeanInfo = new MBeanInfo(
				this.getClass().getName(),
				mbeanDescription,
				getAttrInfos(),
				constructors,
				getOperInfos(),
				getNotificationInfos()
		);		
	}
	
	/**
	 * Extracts an array of <code>MBeanNotificationInfo</code>s from the notifications collection.
	 * @return An array of <code>MBeanNotificationInfo</code>s representing the notifications broadcasted by this mbean.
	 */
	protected MBeanNotificationInfo[] getNotificationInfos() {
		return notifications.toArray(new MBeanNotificationInfo[notifications.size()]);
	}
	
	/**
	 * Extracts an array of <code>MBeanAttributeInfo</code>s from the attributes collection.
	 * @return An array of <code>MBeanAttributeInfo</code>s representing the attributes exposed by this mbean.
	 */
	protected MBeanAttributeInfo[] getAttrInfos() {		
		try {
			MBeanAttributeInfo[] arr = new MBeanAttributeInfo[attributes.size()];
			List<AttributeContainer> acs = new ArrayList<AttributeContainer>(attributes.values());
			Collections.sort(acs);
			int index = 0; 
			for(AttributeContainer ac: acs) {
				arr[index] = ac.getAttributeInfo();
				index++;
			}
			return arr;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to sort attributes", e);
		}
	}
	
	/**
	 * Generates an of <code>MBeanOperationInfo</code>s for core ManagedObjectDynamicMBean operations.
	 * @return An array of <code>MBeanOperationInfo</code>s.
	 */
	protected MBeanOperationInfo[] getCoreOperations() {
		List<MBeanOperationInfo> list = new ArrayList<MBeanOperationInfo>();
//		MBeanOperationInfo ro = new MBeanOperationInfo("reflectObject", "Adds an object to be managed", 
//				new MBeanParameterInfo[]{new MBeanParameterInfo("Object", java.lang.Object.class.getName(), "The object to be managed")}, 
//				Void.TYPE.getName(), MBeanOperationInfo.ACTION); 
//		list.add(ro);
		return list.toArray(new MBeanOperationInfo[list.size()]);
	}
	
	/**
	 * Adds core functions to this bean.
	 */
	public void addCoreFunctions() {
		DynamicMBeanCoreFunctions dcf = new DynamicMBeanCoreFunctions();
		reflectObject(dcf);
	}
	
	/**
	 * Extracts an array of <code>MBeanOperationInfo</code>s from the operations collection.
	 * @return An array of <code>MBeanOperationInfo</code>s representing the operations exposed by this mbean.
	 */
	protected MBeanOperationInfo[] getOperInfos() {
		MBeanOperationInfo[] coreOps = getCoreOperations();
		MBeanOperationInfo[] arr = new MBeanOperationInfo[operations.size() + coreOps.length];
		int index = 0; 
		for(OperationContainer oc: operations.values()) {
			arr[index] = oc.getOperInfo();
			index++;
		}
		for(MBeanOperationInfo oper: coreOps) {
			arr[index] = oper;
			index++;
		}
		return arr;
	}
	
	/**
	 * Reflects a passed managed objects and generates the appropriate MBean attributes and operations.
	 * The reflection looks for recognized JMX annotations and applies where applicable.
	 * Minimum reflection will create javabean style attributes and method named operations.
	 * @param object The object to add as a managed object by the Modb
	 * @param exposures An optional map of <b><code>&lt;Attribute Name&gt;,&lt;Expose (true/false)&gt;</code></b> pairs.
	 */
	
	public void reflectObject(Object object, Map<String, Boolean> exposures) {
		if(exposures!=null) {
			this.exposures.putAll(exposures);
		}
		reflectObject(object);
	}
	
	
	/**
	 * Reflects a passed managed objects and generates the appropriate MBean attributes and operations.
	 * The reflection looks for recognized JMX annotations and applies where applicable.
	 * Minimum reflection will create javabean style attributes and method named operations.
	 * @param object The object to add as a managed object by the Modb
	 * @param exposures An optional string array of <b><code>&lt;Attribute Name&gt;,&lt;Expose (true/false)&gt;</code></b> pairs.
	 */
	
	public void reflectObject(Object object, String...exposures) {
		if(exposures!=null) {
			if(exposures.length%2!=0) {
				throw new RuntimeException("Invalid number of exposures. Must be even. " + Arrays.toString(exposures) );
			}
			for(int i = 0; i < exposures.length; i++) {
				String key = exposures[i];
				i++;
				boolean exposed = "true".equalsIgnoreCase(exposures[i]);
				this.exposures.put(key, exposed);
			}
		}
		reflectObject(object);
	}
	
	
	/**
	 * Reflects a passed managed objects and generates the appropriate MBean attributes and operations.
	 * The reflection looks for recognized JMX annotations and applies where applicable.
	 * Minimum reflection will create javabean style attributes and method named operations.
	 * @param object
	 */
	@SuppressWarnings("unchecked")
	public void reflectObject(Object object) {		
		long start = System.currentTimeMillis();
		if(object==null) return;		
		int targetObjectHashCode = object.hashCode();
		if(registeredObjectIndex.contains(targetObjectHashCode)) {
			return;
		} else {
			registeredObjectIndex.add(targetObjectHashCode);
		}		
		if(object instanceof CoreExtensionManagedObject) {
			((CoreExtensionManagedObject)object).setManagedObjectDynamicMBean(this);
		}
		BeanInfo beanInfo = null;
		Class clazz = object.getClass();
		try {
			if(isAnnotatedWith(clazz,JMXManagedObject.class)) {			
				reflectAnnotatedObjectAttributes(object);
				reflectAnnotatedOperations(object);	
				reflectAnnotatedFields(object);
			} else {				
				beanInfo = java.beans.Introspector.getBeanInfo(clazz);
				reflectUnAnnotatedAttributeMethods(object, beanInfo, arrayToSet(clazz.getMethods()), false);
				reflectUnAnnotatedOperations(object, beanInfo, false);
			}
		} catch (java.beans.IntrospectionException e) {
			throw new RuntimeException("Failed to introspect object of class [" + clazz.getName() + "]", e);
		}
		try {
//			if(clazz.isAnnotationPresent(JMXNotifications.class)) {	
//				reflectNotifications(object, (JMXNotifications)clazz.getAnnotation(JMXNotifications.class));
//			}
			if(isAnnotatedWith(clazz, JMXNotifications.class)) {
				reflectNotifications(object, (JMXNotifications)getAnnotationFrom(clazz,JMXNotifications.class));				
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to introspect JMXNotifications on object of class [" + clazz.getName() + "]", e);
		}				
		
		updateMBeanInfo();
		reflected.set(true);
		long elapsed = System.currentTimeMillis() - start;
		//log("Reflect Object on [" + object.getClass().getName() + "/" + object.toString() + "] Elapsed Time:" + elapsed + " ms.");
	}
	
	/**
	 * Indicates if the passed class or its super classes have the passed annotation.
	 * @param clazz The class to inspect
	 * @param annotationClass The annotation class to look for
	 * @return true if the annotation is found, false if it is not.
	 */
	public static boolean isAnnotatedWith(Class clazz, Class<? extends Annotation> annotationClass) {
		if(clazz==null || annotationClass==null) return false;
		if(clazz.isAnnotationPresent(annotationClass)) return true;
		Annotation[] annotations = clazz.getAnnotations();
		for(Annotation annotation: annotations) {
			if(annotation.equals(annotationClass)) return true;
		}
		annotations = clazz.getDeclaredAnnotations();
		for(Annotation annotation: annotations) {
			if(annotation.equals(annotationClass)) return true;
		}
		if(clazz.getSuperclass()!=null) {
			return isAnnotatedWith(clazz.getSuperclass(), annotationClass);
		}		
		return false;
	}
	
	/**
	 * Returns the annotation from the passed class or its superclasses.
	 * @param clazz The class to inspect.
	 * @param annotationClass The annotation class to look for.
	 * @return The located annotation or null if it is not found.
	 */
	public static Annotation getAnnotationFrom(Class clazz, Class<? extends Annotation> annotationClass) {
		if(clazz==null || annotationClass==null) return null;
		Annotation target = null;
		Annotation[] annotations = null;
		target = clazz.getAnnotation(annotationClass);
		if(target!=null) return target;
		annotations = clazz.getAnnotations();
		target = getAnnotation(annotations, annotationClass);
		if(target!=null) return target;
		annotations = clazz.getDeclaredAnnotations();
		target = getAnnotation(annotations, annotationClass);
		if(target!=null) return target;
		if(clazz.getSuperclass()!=null) {
			return getAnnotationFrom(clazz.getSuperclass(), annotationClass);
		}		
		return null;
	}
	
	/**
	 * Serarches an array of annotations for match against the passed annotation class.
	 * @param annotations The array of annotations to search.
	 * @param annotationClass The annotation class to look for.
	 * @return The located annotation or null if it is not found.
	 */
	public static Annotation getAnnotation(Annotation[] annotations, Class<? extends Annotation> annotationClass) {
		for(Annotation ann: annotations) {
			if(ann.getClass().equals(annotationClass)) return ann;
		}
		return null;
	}
	
	
	/**
	 * Very simple (and non introspection supporting) notification annotation processor.
	 * @param obj
	 * @param jmxNotifs
	 */
	protected void reflectNotifications(Object obj, JMXNotifications jmxNotifs) {
		if(obj==null || jmxNotifs==null) return;
		for(JMXNotification notif: jmxNotifs.notifications()) {
			String description = format(notif.description(), obj);
			String name = format(notif.name(), obj);
			String[] types = new String[notif.types().length];
			for(int i = 0; i < types.length; i++) {
				types[i] = format(notif.types()[i].type(), obj);
			}
			notifications.add(new MBeanNotificationInfo(types, name, description));			
		}
	}
	
	/**
	 * Exposes a HeliosJMX annotated object's attributes.
	 * In order to support non java-bean attribute methods (not getX or setX), annotated methods can serve as attribute getters and setters.
	 * The requirement is that the getter method match <code>Object MyMethod()</code> and the setter method match <code>* MyMethod(Object o)</code>.
	 * In some instances, it is possible that the getter and setter may both be annotated and could have different attribute mutability options.
	 * This is supported but in some cases, if the two options conflict, this will result in an exception and the attribute will not be exposed.
	 * In other cases, provided the intent of either of the mutability options are not violated, a "compromise" option will be elected.
	 * @param object
	 */
	@SuppressWarnings("unchecked")
	protected void reflectAnnotatedObjectAttributes(Object object) {
		if(object==null) return;
		JMXAttribute jmxAttr = null;
		boolean declared = false;
		boolean annotated = false;
		Set<Method> methods = null;
		Set<Method> remove = new HashSet<Method>();
		Class clazz = object.getClass();
		BeanInfo beanInfo = null;

		// Acquire the managed object attributes.
		JMXManagedObject jmo = object.getClass().getAnnotation(JMXManagedObject.class);
		declared = jmo.declared();
		annotated = jmo.annotated();
		
		// Process attributes/methods
		if(declared) {
			methods = arrayToSet(clazz.getDeclaredMethods());
		} else {
			methods = arrayToSet(clazz.getMethods());
		}
		// If un-annotated methods should be exposed,
		// acquire a BeanDescriptor.
		if(!annotated) {
			try {
				beanInfo = java.beans.Introspector.getBeanInfo(clazz);
				reflectUnAnnotatedAttributeMethods(object, beanInfo, methods, true);
			} catch (java.beans.IntrospectionException e) {
				throw new RuntimeException("Failed to introspect object of class [" + clazz.getName() + "]", e);
			}
		}
		// Create a map of getter and setter methods, keyed by the attribute name.
		Map<String, AccessorPair> methodMap = new HashMap<String, AccessorPair>();
		AccessorPair ap = null;
		String attrName = null;
		boolean introspectName = false;
		for(Method method: methods) {
			if(method.isAnnotationPresent(JMXAttribute.class)) {
				jmxAttr = method.getAnnotation(JMXAttribute.class);
				boolean expose = jmxAttr.expose();
				try {
					expose = extractExposure(object, clazz, jmxAttr.introspectExpose());
				} catch (Exception e) {}
				if(!expose) {
					remove.add(method);
					continue;
				}				
				method.setAccessible(true);
				introspectName = jmxAttr.introspectName();
				attrName = jmxAttr.name();
				try {
					attrName = extractAttributeName(object, clazz, attrName, introspectName);
				} catch (Exception e) {
					attrName = jmxAttr.name();
				}				 
				ap = methodMap.get(attrName);
				if(ap==null) {
					ap = new AccessorPair(attrName, null, method, object);
					methodMap.put(attrName, ap);
				}
				if(method.getParameterTypes().length==0) {
					ap.setGetter(method);
				} else {
					if(jmxAttr.mutability().isWritable()) {
						ap.setSetter(method);
					}
				}
			} else {
				if(!annotated) {
					remove.add(method);
				}
			}
		}
		methods.removeAll(remove);
		// Validate the mutability option of the AccessorPair.
		AttributeMutabilityOption getterMutability = null;
		AttributeMutabilityOption setterMutability = null;
		
		for(AccessorPair pair : methodMap.values()) {
			try {
				if(pair.getGetter()!=null) getterMutability = getMutability(object, pair.getGetter());
				if(pair.getSetter()!=null) setterMutability = getMutability(object, pair.getSetter());
				if(getterMutability == null) { pair.setMutability(setterMutability); }
				if(setterMutability == null) { pair.setMutability(getterMutability); }
			
				boolean fi = testForFillIn(methods, pair);				
				AttributeMutabilityOption tmp = pair.getMutability();
				pair.setMutability(AttributeMutabilityOption.selectOption(getterMutability, setterMutability));				
				if(pair.getMutability()==null) {
					if(fi) {
						pair.setMutability(tmp);
						attributes.put(pair.getAttributeName(), new AttributeContainer(pair));
					}
				} else {
					try {
						attributes.put(pair.getAttributeName(), new AttributeContainer(pair));
					} catch (Exception e) {
						elog("AttributeMutability Error on [" + pair.getAttributeName() + "]:" + e);
						e.printStackTrace();
					}
				}
				Boolean expose = exposures.get(pair.getAttributeName());
				if(expose!=null && !expose) {
					attributes.remove(pair);
				}
				
			} catch (Exception e) {
				elog("Failed to process annotated attribute:" + e);
				e.printStackTrace();
			}			
		}
		updateMBeanInfo();
	}


	/**
	 * Interprets the passed attribute name and returns the determined attribute name based on the instrospect flag and any dynamic tokens.
	 * The <code>instrospected</code> flag has been deprecated as it was obsoleted by dynamic tokens, but it behaves in a backward compatible way.
	 * Accordingly, if the <code>instrospected</code> flag is true, the raw attribute name must represent an attribute name and any dynamic tokens will cause an error.
	 * In order to use dynamic tokens, the <code>instrospected</code> flag should be false. 
	 * @param object The object to read dynamic or introscpected values from.
	 * @param clazz The class of the object.
	 * @param attrName The raw attribute name
	 * @param instrospected The introspected flag.
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private String extractAttributeName(Object object, Class clazz,
			String attrName, boolean instrospected) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		if(attrName.contains("{") || attrName.contains("}") || attrName.contains(":")) {
			String[] tokens = extractDynamicTokens(attrName);
			if(tokens.length==0) {
				throw new IllegalArgumentException("The attribute name [" + attrName + "] has token characters but had no valid tokens.");
			}
			for(String token: tokens) {
				attrName = attrName.replace(token, extractDynamicTokenValue(token, object).toString());
			}			
			return attrName;
		}
		if(instrospected) { 
			return (String)clazz.getMethod(attrName, NO_SIG).invoke(object, NO_ARGS);
		}
		return attrName;
	}
	
	/**
	 * Examines and processes the passed attribute name to determine the introspected exposure of an attribute
	 * @param object The object instance
	 * @param clazz The class
	 * @param attrName The attribute value
	 * @return true if the attribute evaluates to a positive, false otherwise.
	 */
	private boolean extractExposure(Object object, Class<?> clazz, String attrName)  {
		try {
			if("".equals(attrName)) return true;
			Object value =  null;
			if(attrName.startsWith("{") && attrName.endsWith("}") && attrName.contains(":")) {
				String[] tokens = extractDynamicTokens(attrName);
				if(tokens.length==0) {
					throw new IllegalArgumentException("The attribute name [" + attrName + "] has token characters but had no valid tokens.");
				}
				value = extractDynamicTokenValue(tokens[0], object);
				
			} else {
				value = clazz.getMethod(attrName, NO_SIG).invoke(object, NO_ARGS);
			}
			if(value==null) return false;
			if(value instanceof Boolean) return ((Boolean)value).booleanValue();
			if(value instanceof Number) {
				int intValue = ((Number)value).intValue();
				if(intValue==1) return true;
				return false;
			}
			String sval = value.toString().toLowerCase().trim();
			return sval.equals("true") || sval.equals("yes") || sval.equals("1");
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Executes simple dynamic token replacement
	 * @param value
	 * @param obj
	 * @return
	 */
	public static String format(String value, Object obj) {
		if(value==null) return null;
		if(value.contains("{") || value.contains("}") || value.contains(":")) {
			String[] tokens = extractDynamicTokens(value);
			if(tokens.length==0) {
				return value;
			}
			for(String token: tokens) {
				value = value.replace(token, extractDynamicTokenValue(token, obj).toString());
			}
		}
		return value;
	}
	
	
	/**
	 * Locates any instances of dynamic tokens and returns an array of the extracted tokens.
	 * @param value The string to search for tokens in
	 * @return An array of located tokens
	 */
	protected static String[] extractDynamicTokens(String value) {
		List<String> tokens = new ArrayList<String>();
		Matcher m = dynamicTokenPattern.matcher(value);
		while(m.find()) {
		    if(m.groupCount() > 0) {
		        for(int i = 1; i <= m.groupCount(); i++) {
		            tokens.add(m.group(i));
		        }
		    }			
		}
		return tokens.toArray(new String[tokens.size()]);
	}
	
	/**
	 * Parses a dynamic token and reflects the passed object to acquire the String referenced by the token.
	 * @param token The token
	 * @param object The object to extract the value from.
	 * @return The dereferenced string.
	 * @throws Exception An exception will be thrown on failure to parse the token or execute the reflective operations.
	 */
	protected static Object extractDynamicTokenValue(String token, Object object) throws IllegalArgumentException {
		try {
			Matcher m = dynamicTokenValues.matcher(token);
			if(m.matches()) {
				String type = m.group(1);
				String name = m.group(2);
				Object retValue = null;
				if("A".equalsIgnoreCase(type)) {
					Method meth = getMethod(object.getClass(), name);			
					if(meth==null) return ""; //throw new Exception("Could Not Located Method [" + name + "].");
					meth.setAccessible(true);
					retValue = meth.invoke(object, NO_ARGS);			
					return retValue==null ? meth.getName() : retValue;
				} else if("F".equalsIgnoreCase(type)) {
					Field f = getField(object.getClass(), name);
					if(f==null) return "";//throw new Exception("Could Not Located Field [" + name + "].");
					f.setAccessible(true);
					retValue = f.get(object); 
					return retValue==null ? f.getName() : retValue;
				} else {
					throw new Exception("The token type [" + type + "] is invalid"); 
				}
			}
			throw new Exception("The value [" + token + "] is not a valid token");
		} catch (Exception e) {
			throw new IllegalArgumentException("Failure to process token dereference on [" + token + "]", e);
		}
	}
	
	/**
	 * Returns the named parameterless method in the passed class or its superclasses.
	 * @param clazz The class to search
	 * @param name The name of the method.
	 * @return A method or null if it could not be found.
	 */
	public static Method getMethod(Class clazz, String name) {
		Method m = null;
		try  {
			m = clazz.getMethod(name);
		} catch (Exception e) {}
		if(m!=null) return m;
		try  {
			m = clazz.getDeclaredMethod(name);
		} catch (Exception e) {}
		if(m!=null) return m;
		if(clazz.getSuperclass() != null) {
			return getMethod(clazz.getSuperclass(), name);
		}
		return null;
	}
	
	/**
	 * Returns the named field in the passed class or its superclasses.
	 * @param clazz The class to search
	 * @param name The name of the field.
	 * @return A field or null if it could not be found.
	 */
	public static Field getField(Class clazz, String name) {
		Field f = null;
		try  {
			f = clazz.getField(name);
		} catch (Exception e) {}
		if(f!=null) return f;
		try  {
			f = clazz.getDeclaredField(name);
		} catch (Exception e) {}
		if(f!=null) return f;
		if(clazz.getSuperclass() != null) {
			return getField(clazz.getSuperclass(), name);
		}
		return null;
	}
	
	
	/**
	 * Acquires the mutability option defined by the JMXAttribute of the passed method in the passed object.
	 * @param targetObject
	 * @param targetMethod
	 * @return A mutability option or null if either of the parameters are null or if the targetMethod is not annotated with JMXAttribute.
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	protected AttributeMutabilityOption getMutability(Object targetObject, Method targetMethod) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if(targetObject==null || targetMethod==null) return null;
		JMXAttribute jmxAttribute = targetMethod.getAnnotation(JMXAttribute.class);
		if(jmxAttribute==null) return null;
		if(!jmxAttribute.introspectMutability()) {
			return jmxAttribute.mutability();
		} else {
			return (AttributeMutabilityOption) targetObject.getClass().getMethod(jmxAttribute.mutabilityName(), NO_SIG).invoke(targetObject, NO_ARGS);
		}
	}
	
	protected void reflectAnnotatedFields(Object object) {
		boolean localAttribsAdded = false;
		Class targetClass = object.getClass();
		Field[] fields = targetClass.getDeclaredFields();
		for(Field field: fields) {
			try {
				JMXField jmxField = field.getAnnotation(JMXField.class);
				if(jmxField == null || !jmxField.expose()) continue;
				field.setAccessible(true);
				//=====================
				// jmxField attributes
				//=====================
				String description = jmxField.description();
				String name = jmxField.name();
				String jmxDomain = jmxField.jmxDomain();
				String objectNameStr = jmxField.objectName();
				boolean introspectName = jmxField.introspectName();
				boolean introspectDescription = jmxField.introspectDescription();
				boolean introspectObjectName = jmxField.introscpectObjectName();
				boolean introspectJmxDomain = jmxField.introscpectJmxDomain();
				AttributeMutabilityOption mutability = jmxField.mutability();
				if(introspectName) {
					name = targetClass.getMethod(name, NO_SIG).invoke(object, NO_ARGS).toString();
				}
				if(introspectDescription) {
					description = targetClass.getMethod(description, NO_SIG).invoke(object, NO_ARGS).toString();
				}
				if(introspectObjectName) {
					objectNameStr = targetClass.getMethod(objectNameStr, NO_SIG).invoke(object, NO_ARGS).toString();
				}
				if(introspectJmxDomain) {
					jmxDomain = targetClass.getMethod(jmxDomain, NO_SIG).invoke(object, NO_ARGS).toString();
				}
				
				// define setter and getter
				Method getter = null, setter = null;
				if(mutability.isReadable()) {
					getter = Field.class.getMethod("get", new Class[]{Object.class});
				}
				if(mutability.isWritable()) {
					setter = Field.class.getMethod("set", new Class[]{Object.class, Object.class});
				}
				AccessorPair ap = new AccessorPair(name, setter, getter, object, field);
				ap.setDescription(description);

				if("".equalsIgnoreCase(objectNameStr) || "this".equalsIgnoreCase(objectNameStr) || JMXHelperExtended.objectName(objectNameStr).equals(this.objectName)) {
					// register in this MODB
					attributes.put(ap.getAttributeName(), new AttributeContainer(ap));
					log("Adding Attribute [" + ap.getAttributeName() + "] to MODB [" + this.getClass().getName() + "]");
					localAttribsAdded = true;
				} else {
					FieldWrapper fw = new FieldWrapper(new FieldContainer(ap));
					ObjectName targetModb = new ObjectName(objectNameStr);
					MBeanServer targetAgent = null;
					if("".equalsIgnoreCase(jmxDomain) || "this".equalsIgnoreCase(jmxDomain)) {
						targetAgent = this.server;
					} else {
						targetAgent = JMXHelperExtended.getLocalMBeanServer(jmxDomain);
					}
					
					if(targetAgent.isRegistered(targetModb)) {
						log("[" + this.getClass().getName() + "]Adding ManagedObject [" + fw.getClass().getName() + "] to MBean[" + targetModb + "]");
						targetAgent.invoke(targetModb, "addManagedObject", new Object[]{fw}, new String[]{Object.class.getName()});
					} else {
						log("[" + this.getClass().getName() + "]Registering new MODB for object [" + fw.getClass().getName() + "] to MBean[" + targetModb + "]");
						targetAgent.registerMBean(new ManagedObjectDynamicMBean(fw), targetModb);
					}
				}
			} catch (Exception e) {
				log("Failed to process annotated field[" + field.getDeclaringClass().getName() + "." + field.getName() + "]:" + e);
				e.printStackTrace();
			}
			
		}
		if(localAttribsAdded) {
			updateMBeanInfo();
		}
	}
	
	
	/**
	 * Converts an array of methods to a Set of methods.
	 * @param arr
	 * @return
	 */
	protected Set<Method> arrayToSet(Method[] arr) {
		Set<Method> set = new HashSet<Method>(arr.length);
		for(Method m: arr) {
			set.add(m);
		}
		return set;
	}
	
	/**
	 * In some cases, we may only have an annotated getter without an annotation on the setter.
	 * As a convenience, we want to assume the setter if: <ul>
	 * <li>The getter muttability indicates writability.
	 * <li>The setter is a simple java-bean style method by the name of <code>set&lt;AtrributeName&gt;</code>.
	 * </ul>
	 * @param methods
	 * @param pair
	 * @return true if a fill in is located.
	 */
	protected boolean testForFillIn(Set<Method> methods, AccessorPair pair) {
		String methodName = null;
		if(pair.getMutability().isWritable() && pair.getSetter()==null) {
			methodName = "set" + pair.getGetter().getName().replaceFirst("get", "");
			for(Method m: methods) {
				if(methodName.equals(m.getName()) && m.getParameterTypes().length==1 && m.getReturnType().equals(Void.TYPE)) {
					pair.setSetter(m);
					return true;
				}
			}			
		} else if(pair.getMutability().isReadable() && pair.getGetter()==null) {
			methodName = "get" + pair.getGetter().getName().replaceFirst("set", "");
			for(Method m: methods) {
				if(methodName.equals(m.getName()) && m.getParameterTypes().length==0 && !m.getReturnType().equals(Void.TYPE)) {
					pair.setGetter(m);
					return true;
				}
			}			
		}
		return false;
	}
	
	
	/**
	 * Reflects and exposes the unannotated methods of an object if the method has a java bean attribute pattern.
	 * @param targetObject The object being reflected.
	 * @param beanInfo A bean descriptor to determine the attribute.
	 * @param methods The methods so that if the object was declared=true, non-declared methods can be filtered out.
	 */
	protected void reflectUnAnnotatedAttributeMethods(Object targetObject, BeanInfo beanInfo, Set<Method> methods, boolean isManagedObject) {
		log("Reflecting UnAnnotatedAttributeMethods from [" + targetObject + "/" + targetObject.getClass().getName() + "]. Available Methods:" + methods.size());
		PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
		Method readMethod = null;
		Method writeMethod = null;
		MBeanAttributeInfo attrInfo = null;
		AttributeContainer attrContainer = null;
		Set<Method> altMethods = new HashSet<Method>();		
		for(Method objMethods: Object.class.getDeclaredMethods()) {
			altMethods.add(objMethods);
		}
		for(PropertyDescriptor pd : pds) {
			log("\tExamining Attribute:" + pd.getName());
			readMethod = pd.getReadMethod();
			if(readMethod != null) readMethod.setAccessible(true);
			writeMethod = pd.getWriteMethod();
			if(writeMethod != null) writeMethod.setAccessible(true);
			// ignore the JMXAttribute if the object is not managed.
			if((readMethod!=null && readMethod.isAnnotationPresent(JMXAttribute.class) && isManagedObject) || (writeMethod!=null && writeMethod.isAnnotationPresent(JMXAttribute.class) && isManagedObject)) {
				continue;  // this means that one of both of the methods is annotated, so we will not implement default java bean JMX exposure.
			} else {
				try {
					if((methods.contains(readMethod) || methods.contains(writeMethod)) && (!altMethods.contains(readMethod) && !altMethods.contains(writeMethod))) {
						attrInfo = new MBeanAttributeInfo(pd.getName(), JMXAttribute.DEFAULT_DESCRIPTION, readMethod, writeMethod);
						attrContainer = new AttributeContainer(targetObject, attrInfo, readMethod, writeMethod);
						log("\tAdding " + pd.getName() + " with [" + readMethod + "/" + writeMethod + "]");
						this.attributes.put(pd.getName(), attrContainer);
					}
				} catch (IntrospectionException e) {
					elog("Hit introspection exception on " + pd.getName() + ":" + e);
				}
			}
		}
		updateMBeanInfo();
	}
	
	protected static void log(Object message) {
		//System.out.println(message);
	}
	protected static void elog(Object message) {
		System.err.println(message);
	}
	
	
	/**
	 * Returns true if the passed method is in the passed array of methods.
	 * @param method
	 * @param methods
	 * @return
	 */
	protected boolean isMethodIn(Method method, Method[] methods) {
		if(method==null || methods==null || methods.length==0) return false;
		for(Method m: methods) {
			if(m.equals(method)) return true;
		}
		return false;
	}
	
	/**
	 * Determines if the passed method was declared in the class of the passed object.
	 * @param method
	 * @param object
	 * @return
	 */
	protected boolean isMethodDeclared(Method method, Object object) {
		if(method==null) return false;
		return method.getDeclaringClass().equals(object.getClass());
	}
	
	
	/**
	 * Exposes all un-annotated operations in the passed object.
	 * @param object
	 * @param beanInfo A BeanInfo instance so attribute methods can be filtered out. 
	 * @param declared If true, exposes declared operations only.
	 */
	protected void reflectUnAnnotatedOperations(Object object, BeanInfo beanInfo, boolean declared) {
		if(object==null) return;
		Set<Method> methods = new HashSet<Method>();
		Set<Method> attributeMethods = new HashSet<Method>();
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors(); 
		if(declared) {
			methods = arrayToSet(object.getClass().getDeclaredMethods());
		} else {
			methods = arrayToSet(object.getClass().getMethods());
		}
		for(PropertyDescriptor pd: propertyDescriptors) {
			if(pd.getReadMethod()!=null) attributeMethods.add(pd.getReadMethod());
			if(pd.getWriteMethod()!=null) attributeMethods.add(pd.getWriteMethod());
		}
		for(Method m: methods) {
			if(attributeMethods.contains(m)) continue;
			if(m.isAnnotationPresent(JMXOperation.class)) continue;
			if(!declared && !this.isMethodDeclared(m, object)) continue;			 
			operations.put(
					OperationContainer.hashOperationName(m.getName(), m.getParameterTypes()),
					new OperationContainer(object, JMXOperation.DEFAULT_DESCRIPTION, m));
		}		
	}
	
	
	/**
	 * Reflects and exposes <code>JMXOperation</code> annotated methods in the passed object.
	 * @param object The target object
	 * @param declared true if only declared methods should be exposed.
	 * @throws java.beans.IntrospectionException 
	 */
	@SuppressWarnings("unchecked")
	protected void reflectAnnotatedOperations(Object object) throws java.beans.IntrospectionException {
		if(object==null) return;		
		Class clazz = object.getClass();
		JMXManagedObject jmxObj = (JMXManagedObject)clazz.getAnnotation(JMXManagedObject.class);
		if(jmxObj==null) return;
		Set<Method> methods = new HashSet<Method>();
		MBeanParameterInfo[] pInfos = null;
		MBeanOperationInfo mInfo = null;
		JMXOperation jmxOper = null;
		String operName = null;
		String operDesc = null;
		if(jmxObj.declared()) {
			methods = arrayToSet(clazz.getDeclaredMethods());
		} else {
			methods = arrayToSet(clazz.getMethods());
		}
		for(Method m: methods) {
			if(!m.isAnnotationPresent(JMXOperation.class)) {
				continue;
			}
			m.setAccessible(true);
			jmxOper = m.getAnnotation(JMXOperation.class);
			boolean expose = jmxOper.expose();
			try {
				expose = extractExposure(object, clazz, jmxOper.introspectExpose());
			} catch (Exception e) {}
			
			if(!expose) continue;
			if(jmxOper.introspectName()) {
				try {
					operName = clazz.getMethod(jmxOper.name(), NO_SIG).invoke(object, NO_ARGS).toString();
				} catch (Exception e) {
					operName = jmxOper.name();
				}
			} else {
				operName = jmxOper.name();
			}
			if(jmxOper.introspectDescription()) {
				try {
					operDesc = clazz.getMethod(jmxOper.description(), NO_SIG).invoke(object, NO_ARGS).toString();
				} catch (Exception e) {
					operDesc = jmxOper.description();
				}
			} else {
				operDesc = jmxOper.description();
			}
			
			pInfos = generateParamInfo(m);
			mInfo = new MBeanOperationInfo(operName, operDesc, pInfos, m.getReturnType().getName(), jmxOper.impact());
			operations.put(
					OperationContainer.hashOperationName(operName, m.getParameterTypes()),
					new OperationContainer(object, mInfo, m, jmxOper.async()));
		}
		if(!jmxObj.annotated()) {
			// this means that we are not excluding non-annotated object operations,
			// so now we expose those too.
			reflectUnAnnotatedOperations(object, java.beans.Introspector.getBeanInfo(clazz), jmxObj.declared());
		}
		
	}
	
	/**
	 * Generates an array of <code>MBeanParameterInfo</code>s from the passed method.
	 * @param method
	 * @return
	 */
	protected MBeanParameterInfo[] generateParamInfo(Method method) {
		MBeanParameterInfo[] pInfos = new MBeanParameterInfo[method.getParameterTypes().length];		
		Map<Integer, JMXParameter> paramAnns = getJMXParameters(method);
		int index = 0;
		JMXParameter jmxParam = null;
		MBeanParameterInfo pInfo = null;
		for(Class clazz: method.getParameterTypes()) {
			jmxParam = paramAnns.get(index);
			if(jmxParam==null) {
				pInfo = new MBeanParameterInfo("p" + index, clazz.getName(), JMXParameter.DEFAULT_DESCRIPTION);
			} else {
				pInfo = new MBeanParameterInfo(jmxParam.name(), clazz.getName(), jmxParam.description());
			}
			pInfos[index] = pInfo;
			index++;
		}
		return pInfos;
	}
	
	/**
	 * Returns a hashmap of JMXParameter annotations for the passed method indexed by parameter sequence.
	 * @param method
	 * @return
	 */
	protected Map<Integer, JMXParameter> getJMXParameters(Method method) {
		Map<Integer, JMXParameter> params = new HashMap<Integer, JMXParameter>();
		Annotation[][] pAnns = method.getParameterAnnotations();
		int index = 0;
		for(Annotation[] anns: pAnns) {
			for(Annotation ann: anns) {
				if(ann instanceof JMXParameter) {
					params.put(index, (JMXParameter)ann);
				}
			}
			index++;
		}
		
		return params;
	}
	
	
	
	/**
	 * Set the value of a specific attribute of the Dynamic MBean.
	 * @param attribute Set the value of a specific attribute of the Dynamic MBean.
	 * @throws AttributeNotFoundException
	 * @throws InvalidAttributeValueException
	 * @throws MBeanException
	 * @throws ReflectionException
	 * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
	 */
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
		AttributeContainer ac = attributes.get(attribute.getName());
		if(ac==null) throw new AttributeNotFoundException("Attribute Not Found:" + attribute.getName());
		if(ac.isWriteOnce() && ac.isWriten()) throw new InvalidAttributeValueException("The attribute [" + attribute.getName() + "] is WRITE_ONCE and has already been written.");
		ac.setAttributeValue(attribute.getValue());
		if(ac.isWriteOnce() && ac.isWriten()) {
			updateMBeanInfo();
		}
	}
	
	
	/**
	 * Parses out an attribute name from a method name.
	 * @param m The method to parse the attribute name from.
	 * @return The attribute name.
	 */
	protected String generateAttributeName(Method m) {
		String methodName = m.getName();
		if(methodName.startsWith("get") || methodName.startsWith("set")) {
			return m.getName().substring(3);  
		} else if(methodName.startsWith("is")) {
			return m.getName().substring(2);
		} else {
			return methodName;
		}
		
	}

	/**
	 * Obtain the value of a specific attribute of the Dynamic MBean.
	 * @param attribute The name of the attribute to be retrieved
	 * @return The value of the attribute retrieved.
	 * @throws AttributeNotFoundException
	 * @throws MBeanException
	 * @throws ReflectionException
	 * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
	 */

	public Object getAttribute(String attribute)
			throws AttributeNotFoundException, MBeanException,
			ReflectionException {
		AttributeContainer ac = attributes.get(attribute);
		if(ac==null) throw new AttributeNotFoundException("Attribute " + attribute + " Not Found");
		try {			
			return ac.getAttributeValue();
		} catch (Exception e) {
			throw new ReflectionException(e, "Failed to Invoke " + ac.getTargetGetterMethod().getName());
		} 
	}

	/**
	 * Get the values of several attributes of the Dynamic MBean.
	 * @param attributes A list of the attributes to be retrieved.
	 * @return The list of attributes retrieved.
	 * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
	 */

	public AttributeList getAttributes(String[] attributes) {
		AttributeList list = new AttributeList();		
		for(String s: attributes) {
			try {
				Object o = getAttribute(s);
				list.add(new Attribute(s, o));
			} catch (Exception e) {}
		}		
		return list;		
	}


	/**
	 * Returns the MBeanInfo for this mbean.
	 * @return
	 */
	public MBeanInfo getMBeanInfo() {
		return mbeanInfo;
	}


	/**
	 * Invokes an exposes operation.
	 * If the managed object declared a JMXOperation annotation that marks the operation asynchronous, 
	 * the invocation will be passed to the asynch request queue for execution by the thread pool.
	 * Otherwise, the invocation will be executed in the current thread.
	 * Both asynch and synch invocations are passed to <code>internalInvoke</code>. 
	 * @param actionName The name of the operation to be invoked.
	 * @param params An array containing the parameters to be set when the action is invoked.
	 * @param signature An array containing the signature of the action. The class objects will be loaded through the same class loader as the one used for loading the MBean on which the action is invoked.
	 * @return The object returned by the action, which represents the result of invoking the action on the MBean specified or null if the call is asynchronous.
	 * @throws MBeanException
	 * @throws ReflectionException
	 * @see javax.management.DynamicMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		OperationContainer op = operations.get(OperationContainer.hashOperationName(actionName, signature));
		if(op==null) throw new OperationNotFoundException(new Exception(), actionName);
		//return internalInvoke(actionName, params, signature);
		try {
			return op.invokeOperation(params);
		} catch (Exception e) {
			throw new ReflectionException(e, "Failed to invoke operation [" + actionName + "]:" + e);
		}
	}
	
	/**
	 * Internal invoke.
	 * @param actionName
	 * @param params
	 * @param signature
	 * @return The applicable return object from the invoked method.
	 * @throws MBeanException
	 * @throws ReflectionException
	 */
	protected Object internalInvoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
		Object ret = null;
		OperationContainer op = operations.get(hashOperationName(actionName, signature));
		try {
			if(op.getTargetMethod().toString().contains(" static ")) {
				ret = op.getTargetMethod().invoke(null, params);
			} else {
				ret = op.getTargetMethod().invoke(op.getTargetObject(), params);
			}
			return ret;
		} catch (Exception e) {
			throw new ReflectionException(e, "Exception Invoking Operation " + op.getTargetMethod().getName());
		} 				
	}
	

	
	/**
	 * Generates a unique hash code for an operation.
	 * @param actionName
	 * @param signature
	 * @return
	 */
	public static String hashOperationName(String actionName, String[] signature) {
		StringBuilder buff = new StringBuilder(actionName);
		for(String s: signature) {
			buff.append(s);
		}
		return "" + buff.toString().hashCode();
	}
	

	/**
	 * Sets the values of several attributes of the Dynamic MBean.
	 * @param attributes A list of attributes: The identification of the attributes to be set and the values they are to be set to.
	 * @return The list of attributes that were set, with their new values.
	 * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
	 */
	public AttributeList setAttributes(AttributeList attributes) {
		AttributeList list = new AttributeList();
		for(int i = 0; i < attributes.size(); i++) {			
			Attribute attr = (Attribute)attributes.get(i);
			try {
				setAttribute(attr);
				list.add(attr);
			} catch (Exception e) {}
			
		}
		return list;
	}
	
	
	/**
	 * Sets the MODB's JMX ObjectName
	 * @param objectName The ObjectName
	 */
	public void setObjectName(ObjectName objectName) {
		this.objectName = objectName;
	}		
	
	/**
	 * Gets the MODB's JMX ObjectName
	 * @return The assigned ObjectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}


	/**
	 * Allows the MBean to perform any operations needed after having been unregistered in the MBean server. 
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	public void postDeregister() {		
		registered = false;
	}


	/**
	 * Allows the MBean to perform any operations needed after having been registered in the MBean server or after the registration has failed. 
	 * @param registrationDone Indicates whether or not the MBean has been successfully registered in the MBean server. The value false means that the registration phase has failed.
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	public void postRegister(Boolean registrationDone) {
		registered = registrationDone;		
	}


	/**
	 * Allows the MBean to perform any operations it needs before being unregistered by the MBean server. 
	 * @throws Exception
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	public void preDeregister() throws Exception {		
		if(defaultThreadPool!=null) {
			try {
				defaultThreadPool.shutdownNow();
			} catch (Exception e) {
				elog("Error stopping default executor:" + e);
			}
		}
	}


	/**
	 * Allows the MBean to perform any operations it needs before being registered in the MBean server. 
	 * If the name of the MBean is not specified, the MBean can provide a name for its registration. 
	 * If any exception is raised, the MBean will not be registered in the MBean server. 
	 * @param mbeanServer The MBean server in which the MBean will be registered.
	 * @param objectName The object name of the MBean. This name is null if the name parameter to one of the createMBean or registerMBean methods in the MBeanServer  interface is null. In that case, this method must return a non-null ObjectName for the new MBean. 
	 * @return The name under which the MBean is to be registered. This value must not be null. If the name  parameter is not null, it will usually but not necessarily be the returned value. 
	 * @throws Exception
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	public ObjectName preRegister(MBeanServer mbeanServer, ObjectName objectName) throws Exception {
		if(this.server!=null) return objectName;
		this.server = mbeanServer;
		this.objectName = objectName;
		if(!this.registeredObjectIndex.contains(this.hashCode())) {
			this.reflectObject(this);
		}
		return objectName;
	}


	/**
	 * Indicates if the MODB is registered.
	 * @return true if registered.
	 */
	public boolean isRegistered() {
		return registered;
	}


	

}

