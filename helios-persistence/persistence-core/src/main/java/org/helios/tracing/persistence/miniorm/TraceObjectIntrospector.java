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
package org.helios.tracing.persistence.miniorm;

import java.lang.reflect.Method;
import java.sql.Date;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.helios.helpers.ClassHelper;
import org.helios.tracing.core.trace.annotations.persistence.PK;
import org.helios.tracing.core.trace.annotations.persistence.Store;
import org.helios.tracing.core.trace.annotations.persistence.StoreField;

/**
 * <p>Title: TraceObjectIntrospector</p>
 * <p>Description: Utility class to extract and index the mini-orm annotations from passed classes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.persistence.miniorm.TraceObjectIntrospector</code></p>
 */

public class TraceObjectIntrospector {
	
	/**
	 * Creates a new TraceObjectMetaData for the passed class
	 * @param clazz The class
	 * @return a new TraceObjectMetaData for the passed class
	 */
	public static TraceObjectMetaData metaData(Class<?> clazz) {
		return new TraceObjectMetaData(clazz);
	}
	
	/**
	 * <p>Title: TraceObjectMetaData</p>
	 * <p>Description: A value container class for mini-orm meta-data for a class</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	public static class TraceObjectMetaData {
		/** The class that this meta-data represents */
		protected final Class<?> clazz;
		/** The name of the table where instances of <code>clazz</code> are stored to  */
		protected final String tableName;
		/** A map of <code>java.sql.Type</code> codes keyed by Column Name */
		protected final Map<String, Integer> types = new TreeMap<String, Integer>();
		/** A map of Attribute value accessor methods keyed by the column name  */
		protected final Map<String, String> accessors = new TreeMap<String, String>();
		/** The ordered column names */
		protected final Set<String> columnNames = new TreeSet<String>();
		/** A map of result set mapping types */
		public static final Map<String, Class<?>> rsetTypes = new TreeMap<String, Class<?>>();
		/** A set of mapped types */
		public static final Set<Class<?>> mappedTypes = new HashSet<Class<?>>();
		
		static {
			rsetTypes.put("BIGINT", long.class);
			rsetTypes.put("SMALLINT", int.class);
			rsetTypes.put("INTEGER", int.class);
			rsetTypes.put("NUMERIC", long.class);
			rsetTypes.put("VARCHAR", String.class);
			rsetTypes.put("DATE", Date.class);
			rsetTypes.put("TIMESTAMP", Date.class);
			
			mappedTypes.addAll(rsetTypes.values());
		}
	
		/**
		 * Creates a new for the passed class
		 * @param clazz The class to create a TraceObjectMetaData for
		 */
		protected TraceObjectMetaData(Class<?> clazz) {
			if(clazz==null) throw new RuntimeException("Passed class was null", new Throwable());
			this.clazz = clazz;
			Store store = this.clazz.getAnnotation(Store.class);
			if(store==null) throw new RuntimeException("Failed to find @Store annotation on class [" + clazz.getName() + "]", new Throwable());
			tableName = store.store();
			for(Method method: ClassHelper.getAnnotatedMethods(clazz, StoreField.class, true)) {
				StoreField storeField = method.getAnnotation(StoreField.class);
				if(storeField==null) throw new RuntimeException("Unexpected failure retrieving @StoreField annotation on method [" + clazz.getName() + "." + method.getName() + "]", new Throwable());
				String columnName = storeField.name();
				boolean fk = storeField.fk();
				int bindType = storeField.type();
				if(method.getReturnType().equals(void.class) || method.getParameterTypes().length > 0) throw new RuntimeException("Unexpected failure processing @StoreField annotation on method [" + clazz.getName() + "." + method.toGenericString() + "]. Not valid accessor.", new Throwable());
				columnNames.add(columnName);
				if(!fk) {
					accessors.put(columnName, method.getName() + "()");
				} else {
					Set<Method> pks = ClassHelper.getAnnotatedMethods(method.getReturnType(), PK.class, true);
					if(pks.size()!=1) throw new RuntimeException("Unexpected failure resolving @PK on method [" + clazz.getName() + "." + method.getName() + "] PK Count was [" + pks.size() + "]", new Throwable());
					Method pkMethod = pks.iterator().next();
					accessors.put(columnName, method.getName() + "()." + pkMethod.getName() + "()");
				}
				types.put(columnName, bindType);
			}
		}
		
		
		/**
		 * Returns a sorted set of the column names
		 * @return a sorted set of the column names
		 */
		public Set<String> getColumnNames() {
			return Collections.unmodifiableSet(columnNames);
		}
		
		/**
		 * Returns the accessor signature for the passed column
		 * @param columnName The column name
		 * @return the accessor signature for the passed column
		 */
		public String getAccessor(String columnName) {
			return accessors.get(columnName);
		}
		/**
		 * Returns the bind type for the passed column
		 * @param columnName The column name
		 * @return the bind type for the passed column
		 */		
		public int getBindType(String columnName) {
			return types.get(columnName);
		}

		/**
		 * Returns the name of the table that this class persists to
		 * @return the tableName
		 */
		public String getTableName() {
			return tableName;
		}


		/**
		 * Constructs a <code>String</code> with key attributes in name = value format.
		 * @return a <code>String</code> representation of this object.
		 */
		public String toString() {
		    final String TAB = "\n\t";
		    StringBuilder retValue = new StringBuilder("TraceObjectMetaData [")
		        .append(TAB).append("clazz = ").append(this.clazz.getName())
		        .append(TAB).append("tableName = ").append(this.tableName)
		        .append(TAB).append("types = [");
		    	for(Map.Entry<String, Integer> e: types.entrySet()) {
		    		retValue.append(TAB).append("\t").append(e.getKey()).append(":").append(e.getValue());
		    	}
		    	retValue.append(TAB).append("]");
		        retValue.append(TAB).append("accessors = [");
		    	for(Map.Entry<String, String> e: accessors.entrySet()) {
		    		retValue.append(TAB).append("\t").append(e.getKey()).append(":").append(e.getValue());
		    	}
		    	retValue.append(TAB).append("]");
		    	retValue.append("\n]");    
		    return retValue.toString();
		}
		
	}
}

