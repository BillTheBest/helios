package org.helios.jmx.opentypes;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;


/**
 * <p>Title: CompositeTypeFactory</p>
 * <p>Description: Factory for generating Composite Types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.opentypes.CompositeTypeFactory</code></p>
 */
public class CompositeTypeFactory {
	/** The type cache for created types */
	private static final Map<Class<?>, OpenType<?>> typeCache = new ConcurrentHashMap<Class<?>, OpenType<?>>();
	/** The index cache for created composite types */
	private static final Map<CompositeType, String[]> indexCache = new ConcurrentHashMap<CompositeType, String[]>();
	/** The type method cache for created types */
	private static final Map<Class<?>, Map<String, Method>> methodCache = new ConcurrentHashMap<Class<?>, Map<String, Method>>();
	/** A mapping of Collection returning methods to the array types they will be switched to */
	private static final Map<Method, Class<?>> collectionMappings = new ConcurrentHashMap<Method, Class<?>>();
	/** A set of Enum retunring methods that will be mapped to Strings */
	private static final Set<Method> mappedEnums = new CopyOnWriteArraySet<Method>();
	
	static {
		try {
			for(Field field: SimpleType.class.getDeclaredFields()) {
				if(!(OpenType.class.isAssignableFrom(field.getType()))) continue;
				field.setAccessible(true);
				OpenType<?> ot = (OpenType<?>)field.get(null);
				Class<?> clazz = Class.forName(ot.getClassName());
				typeCache.put(clazz, ot);
				try {
					clazz = (Class<?>)clazz.getDeclaredField("TYPE").get(null);
					typeCache.put(clazz, ot);					
				} catch (Exception e) {}				
			}
			System.out.println("Loaded [" + typeCache.size() + "] Bootstrap OpenTypes.");
		} catch (Exception e) {
			System.err.println("Failed to load initial OpenType type cache. Stack trace follows:");
			e.printStackTrace(System.err);
		}
	}
	/**
	 * Determines the OpenType for the passed class, adjusting for array types.
	 * @param clazz The class to get the OpenType for
	 * @return An OpenType
	 */
	public static OpenType<?> getOpenTypeFromCache(Class<?> clazz) {
		OpenType<?> ct = typeCache.get(clazz);
		if(ct==null) {
			synchronized(typeCache) {
				ct = typeCache.get(clazz);
				if(ct==null) {
					if(clazz.isArray()) {
						ArrayTypeDimension atd = new ArrayTypeDimension(clazz);
						OpenType<?> rootOpenType = typeCache.get(atd.getRootType());
						if(rootOpenType==null) {
							try {
								rootOpenType = getOpenType(atd.getRootType());
								typeCache.put(atd.getRootType(), rootOpenType);
							} catch (Exception e) {
								throw new RuntimeException("Failed to determine OpenType for array root type [" + atd.getRootType().getName() + "]", e);
							}
						}
						try {
							if(atd.getDimension()<2) {
								ct = new ArrayType((SimpleType) rootOpenType, atd.getRootType().isPrimitive());
							} else {
								ct = new ArrayType(atd.getDimension(), rootOpenType);
							}
							 
							typeCache.put(clazz, ct);
						} catch (Exception e) {
							throw new RuntimeException("Failed to create ArrayType for array root type [" + atd.getRootType().getName() + "]", e);
						}
					} else {
						return getOpenType(clazz);
					}
				}
			}
		}
		return ct;		
	}
	
	public static void main(String[] args) {
		Class<?> c = String[][][][].class;
		OpenType<?> ot = getOpenTypeFromCache(c);
		log("OpenType for String[][][][]:" + ot);
		c = int[].class;
		ot = getOpenTypeFromCache(c);
		log("OpenType for int[]:" + ot);
		c = int[][][][].class;
		ot = getOpenTypeFromCache(c);
		log("OpenType for int[][][][]:" + ot);
		
		
	}

	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * <p>Title: ArrayTypeDimension</p>
	 * <p>Description: A decomposed array type</p> 
	 * @version $LastChangedRevision$
	 * <p><code>com.ice.helpers.jmx.opentype.CompositeTypeFactory.ArrayTypeDimension</code></p>
	 */
	public static class ArrayTypeDimension {
		/** The root type of the array */
		private final Class<?> clazz;
		/** The dimension of the array */
		private final int dimension;
		
		
		/**
		 * Creates a new ArrayTypeDimension
		 * @param clazz The class to determine the ArrayTypeDimension for 
		 */
		public ArrayTypeDimension(Class<?> clazz) {
			if(clazz==null) throw new IllegalArgumentException("The passed class was null", new Throwable());
			if(!clazz.isArray()) throw new IllegalArgumentException("The passed class [" + clazz.getName() + "] is not an array type", new Throwable());
			int d = 1;
			Class<?> level = clazz.getComponentType();
			while(level.isArray()) {
				d++;
				level = level.getComponentType();
			}
			this.clazz = level;
			this.dimension = d;
		}
		
		/**
		 * Creates a new ArrayTypeDimension
		 * @param clazz The root type of the array
		 * @param dimension The dimension of the array
		 */
		public ArrayTypeDimension(Class<?> clazz, int dimension) {
			if(clazz==null) throw new IllegalArgumentException("The passed class was null", new Throwable());			
			this.clazz = clazz;
			this.dimension = dimension;
		}

		/**
		 * Returns the root type of the array
		 * @return the root type of the array
		 */
		public Class<?> getRootType() {
			return clazz;
		}

		/**
		 * Returns the dimension of the array
		 * @return the dimension of the array
		 */
		public int getDimension() {
			return dimension;
		}
		
		
		
		
	}
	
	
	/**
	 * Generates a composite type for the passed class
	 * @param clazz The class to generate a composite type for
	 * @return a composite type
	 */
	public static OpenType<?> getOpenType(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null", new Throwable());
		OpenType<?> ct = typeCache.get(clazz);
		if(ct==null) {
			synchronized(typeCache) {
				ct = typeCache.get(clazz);
				if(ct==null) {
					try {
						CompositeTypeClass ctc = clazz.getAnnotation(CompositeTypeClass.class);
						TabularTypeClass ttc = clazz.getAnnotation(TabularTypeClass.class);
						if(ctc==null && ttc==null) throw new IllegalArgumentException("The passed class [" + clazz.getName() + "] was not annotated with @CompositeTypeClass or @TabularTypeClass", new Throwable());
						if(ctc!=null) {
							ct =  buildCompositeType(clazz, ctc);
						} else {
							ct =  buildTabularType(clazz, ttc);
						}
					} catch (Exception e) {
						throw new RuntimeException("Failed to generate composite type for [" + clazz.getName() + "]", e);
					}
				}
			}
		}
		return ct;
	}
	
	/**
	 * Builds a tabular type for the passed class
	 * @param clazz the class to build a tabular type for
	 * @param ttc The TabularTypeClass annotation
	 * @return the created or cached tabular type
	 * @throws IntrospectionException
	 * @throws OpenDataException 
	 */
	protected static TabularType buildTabularType(Class<?> clazz, TabularTypeClass ttc) throws IntrospectionException, OpenDataException {
		String typeName = ttc.name()==null ? clazz.getName() : ttc.name();
		String typeDescription = ttc.description()==null ? (clazz.getName() + " Tabular Type") : ttc.description();
		CompositeType ct = (CompositeType) getOpenTypeFromCache(ttc.compositeType());
		TabularType tt = new TabularType(typeName, typeDescription, ct, indexCache.get(ct));
		typeCache.put(clazz, tt);
		return tt;
	}

	/**
	 * Builds a composite type for the passed class
	 * @param clazz the class to build a composite type for
	 * @param ctc The CompositeTypeClass annotation
	 * @return the created or cached composite type
	 * @throws IntrospectionException
	 * @throws OpenDataException 
	 */
	protected static CompositeType buildCompositeType(Class<?> clazz, CompositeTypeClass ctc) throws IntrospectionException, OpenDataException {
		List<String> names = new ArrayList<String>();
		Set<String> indexes = new HashSet<String>();
		List<String> descriptions = new ArrayList<String>();
		List<OpenType<?>> types = new ArrayList<OpenType<?>>();		
		// String[] itemNames, String[] itemDescriptions, OpenType<?>[] itemTypes
		// Map<Class<?>, Map<String, Method>> 
		
		PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
		Map<String, Method> methodMap = new HashMap<String, Method>(pds.length);
		methodCache.put(clazz, methodMap);
		for(PropertyDescriptor pd : pds) {
			Method meth = pd.getReadMethod();
			OpenTypeAttribute ota = meth.getAnnotation(OpenTypeAttribute.class);
			if(ota==null) continue;
			String attrName = ota.name();
			String attrDesc = ota.description();
			Class<?> attrClass = ota.openType();
			String attributeName = attrName.equals("") ? pd.getName() : attrName;
			names.add(attributeName);
			descriptions.add(attrDesc.equals("") ? "OpenType Attribute for " + pd.getName() : attrDesc);
			Class<?> mappedType = null;
			Class<?> returnType = meth.getReturnType();
			if(attrClass.equals(OpenTypeAttribute.DefaultedType.class)) {
				if(Collection.class.isAssignableFrom(returnType)) {
					mappedType = getGenericCollectionType(meth);
					Class<?> arrayType = Array.newInstance(mappedType, 1).getClass();
//					types.add(getOpenTypeFromCache(arrayType));
					types.add(getOpenTypeFromCache(arrayType));
					collectionMappings.put(meth, mappedType);
				} else if(Enum.class.isAssignableFrom(returnType)) {
					mappedEnums.add(meth);
					types.add(getOpenTypeFromCache(String.class));
				} else {
					mappedType = returnType;
					types.add(getOpenTypeFromCache(mappedType));
				}
			} else {
				mappedType = attrClass;
				types.add(getOpenTypeFromCache(mappedType));
			}
			
			meth.setAccessible(true);
			methodMap.put(attributeName, meth);
			if(ota.index()) {
				indexes.add(attributeName);
			}
		}
		CompositeType compositeType = new CompositeType(
				ctc.name().equals("") ? clazz.getName() : ctc.name(),
				ctc.description().equals("") ? (clazz.getName() + " Composite Type") : ctc.description(),
				names.toArray(new String[names.size()]),
				descriptions.toArray(new String[descriptions.size()]),
				types.toArray(new OpenType[types.size()])
		); 
		typeCache.put(clazz, compositeType);
		indexCache.put(compositeType, indexes.toArray(new String[indexes.size()]));
		return compositeType;
	}
	
	protected static Class<?> getGenericCollectionType(Method method) {		
		Type t = method.getGenericReturnType();
		if(t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			return (Class<?>) pt.getActualTypeArguments()[0];
		} else {
			return method.getReturnType();
		}
	}
	
	/**
	 * Generates a CompositeDataSupport snapshot for the passed instance
	 * @param instance An instance that is expect to be mapped to a CompositeType
	 * @return a CompositeDataSupport snapshot 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws OpenDataException
	 */
	public static CompositeDataSupport getCDS(Object instance) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, OpenDataException {
		if(instance==null) throw new IllegalArgumentException("The passed instance was null", new Throwable());
		CompositeType ct = (CompositeType) getOpenTypeFromCache(instance.getClass());
		Map<String, Method> methodMap = methodCache.get(instance.getClass());
		Map<String, Object> values = new HashMap<String, Object>(methodMap.size());
		for(Map.Entry<String, Method> m: methodMap.entrySet()) {
			Method meth = m.getValue();
			Class<?> arrayType = collectionMappings.get(meth);			 
			if(arrayType!=null) {
				ArrayList<Object> collection = new ArrayList<Object>((Collection<?>)meth.invoke(Modifier.isStatic(meth.getModifiers()) ? null : instance));
				Object arr = Array.newInstance(arrayType, collection.size());
				int index = 0;
				for(Object o: collection) {
					Array.set(arr, index, o);
					index++;
				}
				values.put(m.getKey(), arr);
			} else if(mappedEnums.contains(meth)) {
				values.put(m.getKey(), ((Enum)meth.invoke(Modifier.isStatic(meth.getModifiers()) ? null : instance)).name());
			} else {
				values.put(m.getKey(), meth.invoke(Modifier.isStatic(meth.getModifiers()) ? null : instance));
			}
		}
		return new CompositeDataSupport(ct, values);
	}
}
