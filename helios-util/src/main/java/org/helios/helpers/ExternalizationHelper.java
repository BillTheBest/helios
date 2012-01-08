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
package org.helios.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.helios.io.ser.OptimizedObjectInputStream;
import org.helios.io.ser.OptimizedObjectOutputStream;

/**
 * <p>Title: ExternalizationHelper</p>
 * <p>Description: Static helper methods to manage externalization.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ExternalizationHelper {
	
	
	
	  public static void externalizeString(ObjectOutput out, String s) throws IOException {
	    if (s == null) {
	      out.writeUTF("NULL");
	    }
	    else {
	      out.writeUTF(s);
	    }
	  }

	  public static String unExternalizeString(ObjectInput in) throws IOException {
	    String tmp = in.readUTF();
	    if(tmp==null || tmp.equalsIgnoreCase("NULL")) {
	      return null;
	    } else {
	      return tmp;
	    }
	  }

	  public static void externalizeByteArray(ObjectOutput out, byte[] bytes) throws IOException {
	    if(bytes==null) {
	      out.writeInt(0);
	    } else {	      
	      out.writeInt(bytes.length);
	      out.write(bytes);
	    }
	  }

	  public static byte[] unExternalizeByteArray(ObjectInput in) throws IOException {
	    int size = in.readInt();
	    byte[] bytes = new byte[size];
	    in.read(bytes);
	    return bytes;
	  }


	  protected static boolean isExternalizable(Class clazz) {
	    try {
	      return (clazz.newInstance() instanceof java.io.Externalizable);
	    }
	    catch (Exception ex) {
	      return false;
	    }
	  }

	  public static Object unExternalizeObject(ObjectInput in)  throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
	    return unExternalizeObject(in, null);
	  }

	  public static void externalizeCollection(ObjectOutput out, Collection collection) throws IOException {
	    if(collection==null || collection.size()==0) {
	      out.writeLong(Long.MAX_VALUE);
	      return;
	    } else {
	      out.writeLong((long)collection.size());
	    }
	    Iterator iterator = collection.iterator();
	    while(iterator.hasNext()) {
	      Object o = iterator.next();
	      externalizeObject(out, o);
	    }
	  }

	  public static Collection unExternalizeCollection(ObjectInput in, Class clazz) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
	    Collection collection = new ArrayList();
	    long instances = in.readLong();
	    if(instances==Long.MAX_VALUE || instances==0) {
	      return collection;
	    }
	    for(int i = 0; i < instances; i++) {
	      collection.add(unExternalizeObject(in, clazz));
	    }
	    return collection;
	  }

	  public static Object unExternalizeObject(ObjectInput in, Class clazz)  throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
	    if(in.readLong()==Long.MAX_VALUE) {
	      return null;
	    }
	    if(clazz==null) {
	      return in.readObject();
	    } else {
	      Externalizable ext = null;
	      try {
	         ext = (Externalizable) clazz.newInstance();
	      } catch (Exception e) {	        
	        return in.readObject();
	      }
	      ext.readExternal(in);
	      return ext;
	    }
	  }


	  public static void externalizeObject(ObjectOutput out, Object object) throws IOException {
	    if(object==null) {
	      out.writeLong(Long.MAX_VALUE);
	      return;
	    } else {
	      out.writeLong(Long.MIN_VALUE);
	    }
	    if(object instanceof Externalizable) {
	      Externalizable ext = (Externalizable)object;
	      ext.writeExternal(out);

	    } else {
	      out.writeObject(object);
	    }
	  }

	   public static void externalizeMap(ObjectOutput out, Map map)  throws IOException {
	    if(map==null || map.size()==0) {
	      out.writeLong(Long.MAX_VALUE);
	      return;
	    } else {
	      out.writeLong((long)map.size());
	    }
	    externalizeCollection(out, map.keySet());
	    externalizeCollection(out, map.values());
	  }

	  public static Map unExternalizeMap(ObjectInput in, Class clazz) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
	    HashMap map = new HashMap();
	    long instances = in.readLong();
	    if (instances == Long.MAX_VALUE || instances==0) {
	      return map;
	    }
	    Collection keys = unExternalizeCollection(in, null);
	    Collection values = unExternalizeCollection(in, clazz);

	    Iterator keyIterator = keys.iterator();
	    Iterator valueIterator = values.iterator();
	    while(keyIterator.hasNext()) {
	      map.put(keyIterator.next(), valueIterator.next() );
	    }
	    return map;
	  }

	  public static void externalizeStringArray(ObjectOutput out, String[] array) throws IOException {
	    if(array==null || array.length==0) {
	      out.writeInt(Integer.MAX_VALUE);
	    } else {
	      out.writeInt(array.length);
	      for(int i = 0; i < array.length; i++) {
	        externalizeString(out, array[i]);
	      }
	    }
	  }

	  public static String[] unExternalizeStringArray(ObjectInput in) throws IOException {
	    int size = in.readInt();
	    if(size==Integer.MAX_VALUE) {
	      return new String[0];
	    } else {
	      String[] result = new String[size];
	      for(int i = 0; i < size; i++) {
	        result[i] = unExternalizeString(in);
	      }
	      return result;
	    }
	  }

	  public static void externalizeDate(ObjectOutput out, java.util.Date date) throws IOException {
	    if(date==null) {
	      out.writeLong(Long.MIN_VALUE);
	    } else {
	      out.writeLong(date.getTime());
	    }
	  }

	  public static java.util.Date unExternalizeDate(ObjectInput in) throws IOException {
	    long time = in.readLong();
	    if(time==Long.MIN_VALUE) return null;
	    else return new java.util.Date(time);
	  }

	  public static java.sql.Date unExternalizeSqlDate(ObjectInput in) throws IOException {
	    long time = in.readLong();
	    if(time==Long.MIN_VALUE) return null;
	    else return new java.sql.Date(time);
	  }
	  
	/**
	 * Serializes the passed object to a byte array
	 * @param obj The object to serialize
	 * @return a byet array
	 */
	public static byte[] serialize(Object obj) {
		return serialize(obj, false);
	}
	  
	  
	/**
	 * Serializes the passed object to a byte array
	 * @param obj The object to serialize
	 * @param eager If true, all input classes are added to the cache
	 * @return a byet array
	 */
	public static byte[] serialize(Object obj, boolean eager) {
		  if(obj==null) throw new IllegalArgumentException("Passed object was null", new Throwable());
		  try {
			  OptimizedObjectOutputStream.addClass(obj.getClass());
			  ByteArrayOutputStream baos = new ByteArrayOutputStream();
			  ObjectOutputStream oos = null;
			  if(eager && OptimizedObjectOutputStream.isClassOptimized(obj.getClass())) {
				  oos =  new  OptimizedObjectOutputStream(baos, eager);
			  } else {
				  oos = new ObjectOutputStream(baos);
			  }
			  oos.writeObject(obj);
			  oos.flush();
			  baos.flush();
			  return baos.toByteArray();
		  } catch (Exception e) {
			  throw new RuntimeException("Failed to serialize instance of [" + obj.getClass().getName(), e);
			  
		  }
	  }
	
	/**
	 * Deserializes a byte array back into an Object
	 * @param ser The byte array
	 * @return the desrialized object
	 */
	public static <T> T deserialize(byte[] ser) {
		if(ser==null) throw new IllegalArgumentException("Passed byte array was null", new Throwable());
		  try {
			  ByteArrayInputStream bais = new ByteArrayInputStream(ser);
			  ObjectInputStream ois = new OptimizedObjectInputStream(bais);
			  try {
				  return (T)ois.readObject();  
			  } catch (Exception e) {
				  bais = new ByteArrayInputStream(ser);
				  ois = new ObjectInputStream(bais);
				  return (T)ois.readObject();
			  }
		  } catch (Exception e) {
			  throw new RuntimeException("Failed to deserialize byte array", e);
			  
		  }
		
	}

}
