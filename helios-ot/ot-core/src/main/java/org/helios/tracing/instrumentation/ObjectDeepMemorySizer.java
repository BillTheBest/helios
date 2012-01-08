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
package org.helios.tracing.instrumentation;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;




/**
 * <p>Title: ObjectDeepMemorySizer</p>
 * <p>Description: A utility class that uses the <code>java.lang.instrument.Instrumentation</code> to determine the deep size of an object.</p>
 * <p>Based on the <code>SizeOfAgent</code> from Maxim Zakharenkov. (http://jroller.com/maxim/entry/again_about_determining_size_of).</p> 
 * <p>Company: ADP NAS Performance Group</p>
 * @author Whitehead 
 * $Id: ObjectDeepMemorySizer.java 1762 2010-06-06 20:47:05Z nwhitehead $
 */
@JMXManagedObject(annotated=true, declared=true)
public class ObjectDeepMemorySizer extends ManagedObjectDynamicMBean  {
	/** The singleton reference */
	protected volatile static ObjectDeepMemorySizer singleton = null;
	/** Singelton creation lock */
	protected static final Object lock = new Object();
	/** The <code>java.lang.instrumentation</code> reference from the javaagent. */
	protected Instrumentation instrumentation = null;
	/** the deep size invocation count  */
	protected final AtomicLong invocationCount = new AtomicLong(0);
	/** the total deep size invocation time */
	protected final AtomicLong invocationTime = new AtomicLong(0);
	/** the last deep size invocation time */
	protected final AtomicLong lastInvocationTime = new AtomicLong(0);
	
	/**
	 * Singleton accessor.
	 * @param instrumentation
	 * @return
	 */
	public static final ObjectDeepMemorySizer getInstance(Instrumentation instrumentation) {
		if(singleton!=null) {
			synchronized (lock) {
				if(singleton!=null) {
					singleton = new ObjectDeepMemorySizer(instrumentation);
				}
			}
		}
		return singleton;
	}

	/**
	 * Constructs a new ObjectDeepMemorySizer that will use the passed instrumentation.
	 * @param instrumentation The instrumentation that will provide individual low level object sizes.
	 */
	private ObjectDeepMemorySizer(Instrumentation instrumentation) {
		if(instrumentation==null) throw new RuntimeException("Cannot create an ObjectDeepMemorySizer with a null instrumentation ");
		this.instrumentation = instrumentation;
		try {
			reflectObject(this);
			objectName = JMXHelper.objectName("org.helios.jagent:service=ObjectSizer");
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);
		} catch (Exception e) {
			System.err.println("Failed to register ObjectDeepMemorySizer management interface:" + e);
		}
	}

	/**
	 * Returns object size without member sub-objects.
	 * @param object object to get size of
	 * @return object size, or 0 if the object is null.
	 */
	@JMXOperation(name="sizeOf", description="Returns the simple size of the passed object")
	public long sizeOf(@JMXParameter(name="object", description="The object to size")Object object) {
		return instrumentation.getObjectSize(object);
	}
	
	/**
	 * Calculates full size of object iterating over
	 * its hierarchy graph.
	 * @param object object to calculate size of
	 * @return object size, or 0 if the object is null.
	 */
	@JMXOperation(name="deepSizeOf", description="Returns the deep size of the passed object")
	public long deepSizeOf(@JMXParameter(name="object", description="The object to size")Object object) {
		if(object==null) return 0;
		long start = System.currentTimeMillis();
		Map<Object, Object> visited = new IdentityHashMap<Object, Object>();
		Stack<Object> stack = new Stack<Object>();

	    long result = internalSizeOf(object, stack, visited, instrumentation);
	    while (!stack.isEmpty()) {
	      result += internalSizeOf(stack.pop(), stack, visited, instrumentation);
	    }
	    visited.clear();
	    invocationCount.incrementAndGet();
	    long elapsed = System.currentTimeMillis()-start;
	    invocationTime.addAndGet(elapsed);
	    lastInvocationTime.set(elapsed);
	    return result;
	}		
	  
    /**
     * Determines if the passed object should be skipped.
     * An object will be skipped if a reference to the object has already been sized, or if the object is a string that has been interned.
     * @param obj The object to test for a skip.
     * @param visited A map of already visited objects.
     * @return true if the object should be skipped.
     */
    private static boolean skipObject(Object obj, Map<Object, Object> visited) {
	    if (obj instanceof String) {
	      // skip interned string
	      if (obj == ((String) obj).intern()) {
	        return true;
	      }
	    }
	    return (obj == null) // skip visited object
	        || visited.containsKey(obj);
	 }

    /**
     * Sizes each field and reference in an object.
     * @param obj the object to size.
     * @param stack a running stack of objects to size.
     * @param visited a map of already visited objects.
     * @param inst the instrumentation instance.
     * @return the internal size of the object.
     */
    private long internalSizeOf(Object obj, Stack<Object> stack, Map<Object, Object> visited, Instrumentation inst) {
	    if (skipObject(obj, visited)){
	    	return 0;
	    }
	    visited.put(obj, null);
	    
	    long result = 0;
	    // get size of object + primitive variables + member pointers 
	    result += sizeOf(obj);
	    
	    // process all array elements
	    Class clazz = obj.getClass();
	    if (clazz.isArray()) {
	      if(clazz.getName().length() != 2) {// skip primitive type array
	    	  int length =  Array.getLength(obj);
			  for (int i = 0; i < length; i++) {
				  stack.add(Array.get(obj, i));
		      }	
	      }	      
	      return result;
	    }
	    
	    // process all fields of the object
	    while (clazz != null) {
	      Field[] fields = clazz.getDeclaredFields();
	      for (int i = 0; i < fields.length; i++) {
	        if (!Modifier.isStatic(fields[i].getModifiers())) {
	          if (fields[i].getType().isPrimitive()) {
	        	  continue; // skip primitive fields
	          } else {
	            fields[i].setAccessible(true);
	            try {
	              // objects to be estimated are put to stack
	              Object objectToAdd = fields[i].get(obj);
	              if (objectToAdd != null) {	            	
	                stack.add(objectToAdd);
	              }
	            } catch (IllegalAccessException ex) { 
	            	assert false; 
	            }
	          }
	        }
	      }
	      clazz = clazz.getSuperclass();
	    }
	    return result;
	 }


}
