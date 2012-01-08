/**
 * 
 */
package org.helios.jmx.nv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanServer;
import javax.management.ObjectName;

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
 * <p>Title: BaseNameValueService</p>
 * <p>Description: A name-value binding service.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@JMXManagedObject(annotated=true, declared=false)
@JMXNotifications(notifications={
		@JMXNotification(description="NameValue Binding Change Notification", types={
				@JMXNotificationType(type=BaseNameValueService.NOTIF_NAME_ADDED),
				@JMXNotificationType(type=BaseNameValueService.NOTIF_NAME_REMOVED),
				@JMXNotificationType(type=BaseNameValueService.NOTIF_NAME_REMOVED)
		})		
})
public class BaseNameValueService extends ManagedObjectDynamicMBean implements INameValueService {
	/**  */
	private static final long serialVersionUID = -4779909866098540261L;
	/** The name/value map */
	protected Map<String, Object> values = null;
	/** The size of the map */
	protected int size = 0;
	/** This mbean's sequence factory */
	protected AtomicLong sequence = new AtomicLong(0);
	
	public static final int DEFAULT_SIZE = 100;
	
	/**
	 * Constructs a new BaseNameValueService of the specified size.
	 * @param size The size of the name value map.
	 */
	public BaseNameValueService(int size, MBeanServer server, ObjectName objectName) {
		this.size = size;
		this.objectName = objectName;
		this.server = server;
		values = new ConcurrentHashMap<String, Object>(this.size);
		this.reflectObject(this);
		try {
			server.registerMBean(this, objectName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register BaseNameValue Service [" + objectName + "]", e);
		}
	}

	/**
	 * Constructs a new BaseNameValueService of the default size.
	 */
	public BaseNameValueService(MBeanServer server, ObjectName objectName) {
		this(DEFAULT_SIZE, server, objectName);
	}

	/**
	 * @param name
	 * @return
	 * @see org.helios.jmx.nv.INameValueService#get(java.lang.String)
	 */
	@JMXOperation(name="get", description="Retrieves the named value")
	public Object get(@JMXParameter(name="name", description="The name of the value to retrieve")String name) {
		return values.get(name);
	}
	
	/**
	 * @param name
	 * @param value
	 * @return
	 * @see org.helios.jmx.nv.INameValueService#set(java.lang.String, java.lang.Object)
	 */
	@JMXOperation(name="set", description="Sets the named value. Returns the prior bound value.")
	public Object set(
			@JMXParameter(name="name", description="The name of the value to set")String name, 
			@JMXParameter(name="value", description="The value to set")Object value) {
		Object ret = values.put(name, value);
		if(ret==null) {
			fireAttributeBound(name, value);
		} else {
			fireAttributeUpdated(name, value, ret);
		}
		return ret;
	}
	
	/**
	 * @param name
	 * @return
	 * @see org.helios.jmx.nv.INameValueService#remove(java.lang.String)
	 */
	@JMXOperation(name="remove", description="Removes the named value")
	public Object remove(@JMXParameter(name="name", description="The name of the value to remove")String name) {
		Object ret = values.remove(name);
		if(ret!=null) {
			fireAttributeRemoved(name, ret);
		}
		return ret;
	}
	
	/**
	 * @param name
	 * @return
	 * @see org.helios.jmx.nv.INameValueService#exists(java.lang.String)
	 */
	@JMXOperation(name="exists", description="Checks for the existence of a value bound under the passed name")
	public boolean exists(@JMXParameter(name="name", description="The name of the value to check existence for")String name) {
		return values.containsKey(name);
	}
	
	/**
	 * @return
	 * @see org.helios.jmx.nv.INameValueService#names()
	 */
	@JMXAttribute(name="names", description="An array of all the names bound", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] names() {
		return values.keySet().toArray(new String[values.size()]);
	}
	
	
	/**
	 * @param name
	 * @param value
	 */
	protected void fireAttributeBound(String name, Object value) {
		this.sendNotification(new AttributeChangeNotification(objectName, sequence.incrementAndGet(), System.currentTimeMillis(), NOTIF_NAME_ADDED, name, value.getClass().getName(), null, value));
	}

	/**
	 * @param name
	 * @param value
	 */
	protected void fireAttributeRemoved(String name, Object value) {
		this.sendNotification(new AttributeChangeNotification(objectName, sequence.incrementAndGet(), System.currentTimeMillis(), NOTIF_NAME_REMOVED, name, value.getClass().getName(), value, null));
	}
	
	/**
	 * @param name
	 * @param newValue
	 * @param oldValue
	 */
	protected void fireAttributeUpdated(String name, Object newValue, Object oldValue) {
		this.sendNotification(new AttributeChangeNotification(objectName, sequence.incrementAndGet(), System.currentTimeMillis(), NOTIF_NAME_UPDATED, name, newValue.getClass().getName(), oldValue, newValue));
	}
	
	
}

/*
`
*/
