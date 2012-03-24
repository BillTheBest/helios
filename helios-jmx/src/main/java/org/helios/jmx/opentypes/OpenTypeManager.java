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
package org.helios.jmx.opentypes;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

import org.apache.log4j.Logger;
import org.helios.helpers.ClassHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.opentypes.annotations.DelegateCompositeData;
import org.helios.jmx.opentypes.annotations.DelegateNonCompositeData;
import org.helios.jmx.opentypes.annotations.DelegateNonTabularData;
import org.helios.jmx.opentypes.annotations.DelegateTabularData;
import org.helios.jmx.opentypes.annotations.XCompositeAttribute;
import org.helios.jmx.opentypes.annotations.XCompositeType;
import org.helios.jmx.opentypes.property.ReadOnlyAttributeAccessor;
import org.helios.jmx.opentypes.property.ReadOnlyCompositeAttributeMap;
import org.helios.reflection.PrivateAccessor;

/**
 * <p>Title: OpenTypeManager</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmx.opentypes.OpenTypeManager</code></p>
 */

public class OpenTypeManager {
	/** The singleton instance */
	private static volatile OpenTypeManager singleton = null;
	/** The singleton ctor lock */
	private static final Object lock = new Object();
	/** Private registry of all recognized open types, keyed by class name */
	private final Map<String, OpenType<?>> REGISTERED_OPEN_TYPES = new ConcurrentHashMap<String, OpenType<?>>();
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	/**
	 * Acquires the OpenInstanceManager singleton
	 * @return the OpenInstanceManager singleton
	 */
	public static OpenTypeManager getInstance() {
		if(singleton==null) {
			synchronized(lock) {
				if(singleton==null) {
					singleton = new OpenTypeManager();
				}
			}
		}
		return singleton;
	}
	
	public static void main(String[] args) {
		log(getInstance().getOpenType(long[][][][][][].class.getName()));
		
	}
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/**
	 * Private ctor
	 */
	private OpenTypeManager() {
		REGISTERED_OPEN_TYPES.put("java.math.BigDecimal", SimpleType.BIGDECIMAL);
		REGISTERED_OPEN_TYPES.put("java.math.BigInteger", SimpleType.BIGINTEGER);
		REGISTERED_OPEN_TYPES.put("java.lang.Boolean", SimpleType.BOOLEAN);
		REGISTERED_OPEN_TYPES.put("boolean", SimpleType.BOOLEAN);
		REGISTERED_OPEN_TYPES.put("java.lang.Byte", SimpleType.BYTE);
		REGISTERED_OPEN_TYPES.put("byte", SimpleType.BYTE);
		REGISTERED_OPEN_TYPES.put("java.lang.Character", SimpleType.CHARACTER);
		REGISTERED_OPEN_TYPES.put("char", SimpleType.CHARACTER);
		REGISTERED_OPEN_TYPES.put("java.util.Date", SimpleType.DATE);
		REGISTERED_OPEN_TYPES.put("java.lang.Double", SimpleType.DOUBLE);
		REGISTERED_OPEN_TYPES.put("double", SimpleType.DOUBLE);
		REGISTERED_OPEN_TYPES.put("java.lang.Float", SimpleType.FLOAT);
		REGISTERED_OPEN_TYPES.put("float", SimpleType.FLOAT);
		REGISTERED_OPEN_TYPES.put("java.lang.Integer", SimpleType.INTEGER);
		REGISTERED_OPEN_TYPES.put("int", SimpleType.INTEGER);
		REGISTERED_OPEN_TYPES.put("java.lang.Long", SimpleType.LONG);
		REGISTERED_OPEN_TYPES.put("long", SimpleType.LONG);		
		REGISTERED_OPEN_TYPES.put("javax.management.ObjectName", SimpleType.OBJECTNAME);
		REGISTERED_OPEN_TYPES.put("java.lang.Short", SimpleType.SHORT);
		REGISTERED_OPEN_TYPES.put("short", SimpleType.SHORT);
		REGISTERED_OPEN_TYPES.put("java.lang.String", SimpleType.STRING);
		REGISTERED_OPEN_TYPES.put("String", SimpleType.STRING);
		REGISTERED_OPEN_TYPES.put("java.lang.StringBuilder", SimpleType.STRING);
		REGISTERED_OPEN_TYPES.put("java.lang.StringBuffer", SimpleType.STRING);
		REGISTERED_OPEN_TYPES.put("java.lang.Void", SimpleType.VOID);		
		REGISTERED_OPEN_TYPES.put("void", SimpleType.VOID);
		REGISTERED_OPEN_TYPES.put(long[].class.getName(), getPrimitiveArrayType(long[].class));
		REGISTERED_OPEN_TYPES.put(int[].class.getName(), getPrimitiveArrayType(int[].class));
		REGISTERED_OPEN_TYPES.put(boolean[].class.getName(), getPrimitiveArrayType(boolean[].class));
		REGISTERED_OPEN_TYPES.put(double[].class.getName(), getPrimitiveArrayType(double[].class));
		REGISTERED_OPEN_TYPES.put(float[].class.getName(), getPrimitiveArrayType(float[].class));
		REGISTERED_OPEN_TYPES.put(short[].class.getName(), getPrimitiveArrayType(short[].class));
		REGISTERED_OPEN_TYPES.put(char[].class.getName(), getPrimitiveArrayType(char[].class));
		REGISTERED_OPEN_TYPES.put(byte[].class.getName(), getPrimitiveArrayType(byte[].class));
		REGISTERED_OPEN_TYPES.put(String[].class.getName(), buildArrayType(1, SimpleType.STRING));
	}
	
	
	
	private static ArrayType<?> getPrimitiveArrayType(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed Class was null", new Throwable());
		try {
			return ArrayType.getPrimitiveArrayType(clazz);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create primitive array type for [" + clazz.getName() + "]", e);
		}
	}
	
	/**
	 * Returns the named OpenType or null if it was not found.
	 * @param name The name of the OpenType
	 * @return an OpenType or null if it was not found.
	 */
	public OpenType<?> getOpenTypeOrNull(String name) {
		return REGISTERED_OPEN_TYPES.get(ClassHelper.nvl(name, "Passed name was null"));
	}
		
	
	
	/**
	 * Returns the named OpenType
	 * @param name The name of the OpenType
	 * @return an OpenType
	 */
	public OpenType<?> getOpenType(String name) {
		
		OpenType<?> type = REGISTERED_OPEN_TYPES.get(ClassHelper.nvl(name, "Passed name was null"));
		if(type==null) {
			synchronized(REGISTERED_OPEN_TYPES) {
				type = REGISTERED_OPEN_TYPES.get(name);
				if(type==null) {
					try {
						Class<?> clazz = Class.forName(name);
						if(clazz.isArray()) {
							if(ClassHelper.isPrimitiveArray(clazz)) {
								OpenType<?> pot =  PrimitiveArrayType.getPrimitiveArray(name);
								REGISTERED_OPEN_TYPES.put(name, pot);
								return pot;
							}
							int dimensions = ClassHelper.getArrayTypeDimension(clazz)-1;
							clazz = clazz.getComponentType();
							if(REGISTERED_OPEN_TYPES.containsKey(clazz.getName())) {
								OpenType<?> ot = new ArrayType<Object>(dimensions,REGISTERED_OPEN_TYPES.get(clazz.getName()));
								REGISTERED_OPEN_TYPES.put(name, ot);
								return ot;
							}
						}
						type = buildCompositeType(clazz);
						if(type==null) {
							throw new Exception("Build open type named [" + name + "] returned null");
						}
						REGISTERED_OPEN_TYPES.put(name, type);
					} catch (Exception e) {
						throw new RuntimeException("Failed to build open type named [" + name + "]", e);
					}
				}
			}
		}
		return type;
	}
	
	
	/**
	 * Returns a CompositeData instance for the passed Obect
	 * @param target The target object to expose as a CompositeData
	 * @return a CompositeData instance
	 */
	public CompositeData getCompositeDataInstance(Object target) {
		if(target==null) throw new IllegalArgumentException("Passed object was null", new Throwable());
		if(target instanceof CompositeData) {
			if(target instanceof DelegateCompositeData) {
				if(((DelegateCompositeData)target).isReady()) {
					return ((DelegateCompositeData)target).getDelegate();
				}
			} else {
				return (CompositeData)target;
			}
		}
		Class<?> clazz = target.getClass();
		XCompositeType xType = clazz.getAnnotation(XCompositeType.class);
		if(xType==null) {
			throw new IllegalArgumentException("The class [" + target.getClass().getName() + "] is not annotated with @XCompositeType", new Throwable());
		}
		OpenType<?> oType = buildCompositeTypeInstance(target);
		if(oType==null) {
			throw new IllegalArgumentException("Failed to identify OpenType for class [" + target.getClass().getName() + "]", new Throwable());
		}
		if(!(oType instanceof CompositeType)) {
			throw new IllegalArgumentException("The passed type [" + target.getClass().getName() + "] does not conform to a CompositeType. It is a [" + oType.getTypeName() + "]", new Throwable());
		}
		CompositeType cType = (CompositeType)oType;
		Map<String, ReadOnlyAttributeAccessor<?>> accessors = new HashMap<String, ReadOnlyAttributeAccessor<?>>(cType.keySet().size());
		for(Field f: ClassHelper.getAnnotatedFields(clazz, XCompositeAttribute.class)) {			
			String key = getAttributeName(f);
			ReadOnlyAttributeAccessor<?> accessor = OpenTypeAttributeTableAccessorFactory.getAccessor(f, target);
			accessors.put(key, accessor);
		}
		for(Method m: ClassHelper.getAnnotatedMethods(clazz, XCompositeAttribute.class, true)) {
			String key = getAttributeName(m);
			ReadOnlyAttributeAccessor<?> accessor = OpenTypeAttributeTableAccessorFactory.getAccessor(m, target);
			accessors.put(key, accessor);			
		}
		return new ReadOnlyCompositeAttributeMap(accessors, cType);
		
		//ReadOnlyCompositeAttributeMap map = 
	}
	
	
	
	/**
	 * Builds a composite type instance for the passed @XCompositeType annotated object which may contains @JMXAttribute and @JMXField annotations
	 * for which attributes may resolve into instance specific values. Composite types created by this method are not cached.
	 * @param instance The instance to build a composite type for
	 * @return A composite type
	 */
	public static OpenType<?> buildCompositeTypeInstance(Object obj) {
		if(obj==null) throw new IllegalArgumentException("Passed instance was null", new Throwable());
		return OpenTypeManager.getInstance().buildCompositeType(obj.getClass(), obj);
	}
	
	

	



	
	/**
	 * Retrieves the OpenType of an MBean or CompositeType attribute from an Object instance's field
	 * @param field The field to get the annotated value from
	 * @param target An instance of a class that contains the passed field. Only needed is the field is not static
	 * @return the attribute OpenType
	 */
	public static OpenType<?> getAttributeOpenType(Field field, Object target) {
		if(field==null) throw new IllegalArgumentException("Passed field was null", new Throwable());
		XCompositeAttribute xca = field.getAnnotation(XCompositeAttribute.class);
		if(xca!=null) {
			if(!"".equals(xca.openType())) {
				return getInstance().getOpenType(xca.openType()); 
			}
		}
		Class<?> fieldType = field.getType();
		Object fieldValue = null;
		if(target!=null || Modifier.isStatic(field.getModifiers())) {
			fieldValue = PrivateAccessor.getFieldValue(field, target);	
		}		
		if(fieldValue==null) {
			return getInstance().getOpenType(fieldType.getName());
		} else {
			OpenType<?> ot = getOpenTypeFromData(fieldValue);
			if(ot!=null) return ot;
			else {
				return getInstance().getOpenType(fieldType.getName());
			}
		}
	}
	
	/**
	 * Retrieves the OpenType of an MBean or CompositeType attribute from an Object instance's method
	 * @param method The method to get the type from
	 * @param target An instance of a class that contains the passed method. Only needed is the method is not static
	 * @return the attribute OpenType
	 */
	public static OpenType<?> getAttributeOpenType(Method method, Object target) {
		if(method==null) throw new IllegalArgumentException("Passed method was null", new Throwable());
		XCompositeAttribute xca = method.getAnnotation(XCompositeAttribute.class);
		if(xca!=null) {
			if(!"".equals(xca.openType())) {
				return getInstance().getOpenType(xca.openType()); 
			}
		}
		Class<?> methodType = method.getReturnType();
		Object methodResult = null;
		if(target!=null || Modifier.isStatic(method.getModifiers())){
			methodResult = PrivateAccessor.getMethodResult(method, target);
		}
		if(methodResult==null) {
			return getInstance().getOpenType(methodType.getName());
		} else {
			OpenType<?> ot = getOpenTypeFromData(methodResult);
			if(ot!=null) return ot;
			else {
				return getInstance().getOpenType(methodType.getName());
			}
		}
	}	
	
	
	/**
	 * Attempts to extract the OpenType of the passed Object by testing it for interface compliance to Composite and Tabular Data.
	 * @param dataInstance The object to test
	 * @return An OpenType or null if one was not found
	 */
	public static OpenType<?> getOpenTypeFromData(Object dataInstance) {
		if(dataInstance==null) return null;
		OpenType<?> ot = singleton.REGISTERED_OPEN_TYPES.get(dataInstance.getClass().getName());
		if(ot!=null) return ot;
		if(dataInstance instanceof CompositeData) {
			if(dataInstance instanceof DelegateCompositeData) {
				ot = ((DelegateCompositeData)dataInstance).getDelegate().getCompositeType();
			} else {
				ot = ((CompositeData)dataInstance).getCompositeType();
			}
		} else if(dataInstance instanceof TabularData) {
			if(dataInstance instanceof DelegateTabularData) {
				ot = ((DelegateTabularData)dataInstance).getDelegate().getTabularType();
			} else {		 	
				ot =  ((TabularData)dataInstance).getTabularType();
			}
		} else if(dataInstance instanceof DelegateNonCompositeData) {
			ot = ((DelegateNonCompositeData)dataInstance).getDelegate().getCompositeType();
		} else if(dataInstance instanceof DelegateNonTabularData) {
			ot = ((DelegateNonTabularData)dataInstance).getDelegate().getTabularType();
		}		
		if(ot!=null) {
			singleton.REGISTERED_OPEN_TYPES.put(dataInstance.getClass().getName(), ot);
		}
		return ot;
	}
	
	/**
	 * Retrieves the description of an MBean or CompositeType attribute from an Object instance's accessible object
	 * @param ao The accessible to get the annotated value or description from
	 * @return the attribute description
	 */
	public static String getAttributeDescription(AccessibleObject ao) {
		String aoName = getAccessibleObjectName(ao);		
		XCompositeAttribute xca = ao.getAnnotation(XCompositeAttribute.class);
		JMXAttribute jmxa = ao.getAnnotation(JMXAttribute.class);
		if(jmxa!=null) {
			if(!"".equals(jmxa.description()) && !dynamicTokenPattern.matcher(jmxa.description()).find()) {
				return jmxa.description();
			}
		}		
		if(xca==null) {
			throw new IllegalArgumentException("AO [" + ao + "] was not annotated with @XCompositeAttribute", new Throwable());
		}
		String name = xca.description();
		if("".equals(name)) {
			name = aoName;
		}
		return name;				
	}
	
	/**
	 * Retrieves the name of an MBean or CompositeType attribute from an Object instance's accessible object
	 * @param ao The accessible to get the annotated value from
	 * @return the attribute name
	 */
	public static String getAttributeName(AccessibleObject ao) {
		String aoName = getAccessibleObjectName(ao);
		XCompositeAttribute xca = ao.getAnnotation(XCompositeAttribute.class);
		JMXAttribute jmxa = ao.getAnnotation(JMXAttribute.class);
		if(jmxa!=null) {
			if(!"".equals(jmxa.name()) && !dynamicTokenPattern.matcher(jmxa.name()).find()) {
				return jmxa.name();
			}
		}
		if(xca==null) {
			throw new IllegalArgumentException("AO [" + ao + "] was not annotated with @XCompositeAttribute", new Throwable());
		}
		String name = xca.name();
		if("".equals(name)) {
			name = aoName;
		}
		return name;				
	}
	/** pattern to extract <code>{f:*}</code> or <code>{a:*}</code> patterns from an annotation value */ 
	protected static Pattern dynamicTokenPattern = Pattern.compile("(\\{[F|A]:\\w+\\})", Pattern.CASE_INSENSITIVE );

	
	/**
	 * Returns the name of a field or method
	 * @param ao An accessible object that must be a (non-null) field or method, otherwise an IllegalArgumentException is thrown.
	 * @return the name of the passed AccessibleObject
	 */
	public static String getAccessibleObjectName(AccessibleObject ao) {
		if(ao==null) throw new IllegalArgumentException("Passed ao was null", new Throwable());
		if(!(ao instanceof Field) && !(ao instanceof Method)) {
			throw new IllegalArgumentException("Passed ao [" + ao.toString() + "] was not a Field or a Method", new Throwable());
		}
		return (ao instanceof Field) ? ((Field)ao).getName() : ((Method)ao).getName().replaceFirst("get", "");  
	}
	
	/**
	 * Builds a composite type for the passed @XCompositeType annotated class
	 * @param clazz The class to build a composite type for
	 * @return A composite type
	 */
	protected OpenType<?> buildCompositeType(Class<?> clazz) {
		return buildCompositeType(clazz, null);
	}
	
	/**
	 * Builds a composite type for the passed @XCompositeType annotated class
	 * @param clazz The class to build a composite type for
	 * @param instance The object instance to cross-reference
	 * @return A composite type
	 */
	protected OpenType<?> buildCompositeType(Class<?> clazz, Object instance) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null", new Throwable());
		int dimensions = 0;
		String className = null;
		
		XCompositeType xComp = clazz.getAnnotation(XCompositeType.class);
		if(xComp==null) throw new IllegalArgumentException("Passed class was not annotated with @XCompositeType", new Throwable());
		className = "".equals(xComp.name()) ? clazz.getName() : xComp.name();
		OpenType<?> oType = REGISTERED_OPEN_TYPES.get(className);
		if(oType==null) {
			synchronized(REGISTERED_OPEN_TYPES) {
				oType = REGISTERED_OPEN_TYPES.get(className);
				if(oType==null) {
					int fillInSequence = 0;
					Map<Integer, String> names = new TreeMap<Integer, String>();
					Map<Integer, String> descriptions = new TreeMap<Integer, String>();
					Map<Integer, OpenType<?>> types = new TreeMap<Integer, OpenType<?>>();
					for(Method method: ClassHelper.getAnnotatedMethods(clazz, XCompositeAttribute.class, true)) {
						XCompositeAttribute attr = method.getAnnotation(XCompositeAttribute.class);
						if(attr==null) throw new RuntimeException("Unexpected null XCompositeAttribute on method [" + clazz.getName() + "." + method.toGenericString() + "]", new Throwable());
						int sequence = attr.sequence();
						if(sequence<0) {
							fillInSequence--;
							sequence = fillInSequence;
						}
						String attrName = getAttributeName(method);
						String attrDescription = getAttributeDescription(method);			
						//================================================
						// !! Need to resolved recursive references !!
						//================================================
						OpenType<?> type = getAttributeOpenType(method, instance);
						if(type==null) {
							throw new RuntimeException("Failed to get OpenType for OpenType name [" + attr.openType() + "] on method [" + clazz.getName() + "." + method.toGenericString() + "]", new Throwable());
						}
						types.put(sequence, type);	
						names.put(sequence, attrName);
						descriptions.put(sequence, attrDescription);
					}
					for(Field field: ClassHelper.getAnnotatedFields(clazz, XCompositeAttribute.class)) {
						try {
							XCompositeAttribute attr = field.getAnnotation(XCompositeAttribute.class);
							if(attr==null) throw new RuntimeException("Unexpected null XCompositeAttribute on field [" + clazz.getName() + "." + field.getName() + "]", new Throwable());
							int sequence = attr.sequence();
							if(sequence<0) {
								fillInSequence--;
								sequence = fillInSequence;
							}
							String attrName = getAttributeName(field);
							String attrDescription = getAttributeDescription(field);
							//================================================
							// !! Need to resolved recursive references !!
							//================================================
							//=========== !! ================
							// Need to recursively call this method with the instance in the field.
							OpenType<?> type = getAttributeOpenType(field, instance);
							if(type==null) {
								throw new RuntimeException("Failed to get OpenType for OpenType name [" + attr.openType() + "] on method [" + clazz.getName() + "." + field.getName() + "]", new Throwable());
							}
							types.put(sequence, type);	
							names.put(sequence, attrName);
							descriptions.put(sequence, attrDescription);
						} catch (Exception e) {
							log.warn("Failed to process field [" + field.getDeclaringClass().getSimpleName() + "." + field.toGenericString() + "]", e);
						}
						
					}
					
					String typeName = "".equals(xComp.name()) ? className : xComp.name();
					boolean isArray = clazz.isArray();
					try {
						if(isArray) {
							oType = new ArrayType<Object>(dimensions, getOpenType(clazz.getName()));
						} else {
							oType = new CompositeType(typeName, xComp.description(), names.values().toArray(new String[names.size()]), descriptions.values().toArray(new String[descriptions.size()]), types.values().toArray(new OpenType[types.size()]));
						}
					} catch (Exception e) {
						throw new RuntimeException("Failed to create CompositeDataType for class [" + clazz.getName() + "]", e);
					}					
				}
			}
		}
		return oType;
	}
	
	public TabularType getTabularType(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null", new Throwable());
		XCompositeType xComp = clazz.getAnnotation(XCompositeType.class);
		if(xComp==null) {
			throw new IllegalArgumentException("Passed class [" + clazz.getName() + "] was not annotated with @XCompositeType", new Throwable());
		}
		String typeName = ("".equals(xComp.name()) ? clazz.getName() : xComp.name()) + "TabularType";
		TabularType tabType = (TabularType) REGISTERED_OPEN_TYPES.get(typeName);
		CompositeType ct = null;
		if(tabType==null) {
			synchronized(REGISTERED_OPEN_TYPES) {
				tabType = (TabularType) REGISTERED_OPEN_TYPES.get(typeName);
				if(tabType==null) {
					ct = (CompositeType)getOpenType(clazz.getName());
					try {
						tabType = new TabularType(typeName, "A table of [" + ct.getDescription() + "]", ct, getCompositeTypeKeys(clazz));
						REGISTERED_OPEN_TYPES.put(typeName, tabType);
					} catch (OpenDataException e) {
						throw new RuntimeException("Failed to get Tabular Type for [" + clazz.getName() + "]", e);
					}
				}
			}
		}
		return tabType;		
	}
	
	
	public static String[] getCompositeTypeKeys(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null", new Throwable());
		XCompositeType xComp = clazz.getAnnotation(XCompositeType.class);
		if(xComp==null) {
			throw new IllegalArgumentException("Passed class [" + clazz.getName() + "] was not annotated with @XCompositeType", new Throwable());
		}
		Set<String> keys = new HashSet<String>();
		for(Method m: ClassHelper.getAnnotatedMethods(clazz, XCompositeAttribute.class, true)) {
			XCompositeAttribute xca = m.getAnnotation(XCompositeAttribute.class);
			if(xca.key()) {
				keys.add(getAttributeName(m));
			}
		}
		for(Field f: ClassHelper.getAnnotatedFields(clazz, XCompositeAttribute.class)) {
			XCompositeAttribute xca = f.getAnnotation(XCompositeAttribute.class);
			if(xca.key()) {
				keys.add(getAttributeName(f));
			}
		}		
		return keys.toArray(new String[keys.size()]);
		
	}
	
	/**
	 * Adds a new OpenType to the registry
	 * @param type the OpenType
	 */
	public void putOpenType(OpenType<?> type) {
		String typeName = ClassHelper.nvl(type.getTypeName(), "Passed type was null");
		if(!REGISTERED_OPEN_TYPES.containsKey(typeName)) {
			REGISTERED_OPEN_TYPES.put(typeName, type);
		}
	}
	
	
	/**
	 * Builds an array type
	 * @param dim The dimension of the array
	 * @param type The type of the array elements
	 * @return the Array type
	 */
	@SuppressWarnings("unchecked")
	public static OpenType<?> buildArrayType(int dim, OpenType<?> type) {
		try {
			return new ArrayType(dim, type);
		} catch (Exception e) {
			throw new RuntimeException("Failed to build array type for [" + type + "]", e);
		}
	}
	
	public static class PrimitiveArrayType {
		final Class<?> type;
		final Class<?> originalType;
		final int dimension;
		
		public PrimitiveArrayType(CharSequence arrayTypeName)  {
			this(getTypeForName(arrayTypeName));
		}
		
		private static Class<?> getTypeForName(CharSequence name) {
			if(name==null) throw new IllegalArgumentException("Passed  type name was null", new Throwable());
			try {
				return Class.forName(name.toString());
			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to find class for [" + name + "]", e);
			}
		}
		
		public ArrayType<?> getArrayType() {		
			try {
				ArrayType<?> arrType = (ArrayType<?>) singleton.REGISTERED_OPEN_TYPES.get(originalType.getName());
				if(arrType==null) {				
					arrType = new ArrayType<Object>((SimpleType<?>)singleton.REGISTERED_OPEN_TYPES.get(type.getName()), true);
					for(int i = 0; i < dimension-1; i++) {
						arrType = new ArrayType<Object>(1, arrType);
					}
				}
				
				return arrType;
			} catch (Exception e) {
				throw new RuntimeException("Failed to create ArrayType ["  + dimension + "] for primitive [" + type.getName() + "]", e);
			}
		}
		
		public PrimitiveArrayType(Class<?> arrayType) {
			if(arrayType==null) throw new IllegalArgumentException("Passed arrayType was null", new Throwable());
			if(!arrayType.isArray()) throw new IllegalArgumentException("Passed Type [" + arrayType.getName() + "] is not an array type", new Throwable());
			originalType = arrayType;
			type = ClassHelper.getCoreType(arrayType);
			if(!type.isPrimitive()) throw new IllegalArgumentException("Passed Type [" + arrayType.getName() + "] is not a primitive type", new Throwable());
			dimension = ClassHelper.getArrayTypeDimension(arrayType);
		}
		
		public static OpenType<?> getPrimitiveArray(CharSequence name) {
			PrimitiveArrayType pat = new PrimitiveArrayType(name);
			return pat.getArrayType();
		}
	}
		

}
