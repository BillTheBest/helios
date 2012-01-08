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
package org.helios.io.ser;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: OptimizedObjectOutputStream</p>
 * <p>Description: {@link java.io.ObjectOutputStream} extension that shrinks the class descriptor of the object being serialized by representing it as the hashcode of the class name.
 * Use of this extension assumes that the deserializer can decode the hashcode back into a class name.
 * </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.io.ser.OptimizedObjectOutputStream</code></p>
 */

public class OptimizedObjectOutputStream extends ObjectOutputStream {
	/** A map of classes keyed by the class name hashcode. */
	protected static final Map<Integer, Class<?>> DECODES = new ConcurrentHashMap<Integer, Class<?>>(128);
	/** If true, all input classes are added to the cache */
	protected final boolean eager;
	/** The leading value in the stream indicating that the following class descriptor is optimized */
	public static final int OPTIMIZED_DESCRIPTOR = 1;
	/** The leading value in the stream indicating that the following class descriptor is standard */
	public static final int STANDARD_DESCRIPTOR = 0;
	
	
	/**
	 * Adds a class optimized hashcode to the decodes
	 * @param classes An array of classes to add
	 */
	public static void addClass(Class<?>...classes) {
		if(classes!=null) {
			for(Class<?> clazz: classes) {
				if(clazz==null) continue;
				DECODES.put(clazz.getName().hashCode(), clazz);
			}
		}
	}
	
	public static boolean isClassOptimized(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null", new Throwable());
		int hashCode = clazz.getName().hashCode();
		return DECODES.containsKey(hashCode);
	}
	
	/**
	 * Creates a new OptimizedObjectOutputStream
	 * @param out The output stream this stream feeds into
	 * @throws IOException
	 */
	public OptimizedObjectOutputStream(OutputStream out) throws IOException {
		this(out, false);
	}
	
	/**
	 * Creates a new OptimizedObjectOutputStream
	 * @param out The output stream this stream feeds into
	 * @param eager If true, all input classes are added to the cache
	 * @throws IOException
	 */
	public OptimizedObjectOutputStream(OutputStream out, boolean eager) throws IOException {
		super(out);
		this.eager = eager;
	}
	
	
    /**
     * Write the specified class descriptor to the ObjectOutputStream.
     * @param desc class descriptor to write to the stream 
     * @throws IOException If an IO error occurs
     */
    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {        
    	Class<?> clazz = desc.forClass();
    	int hashCode = clazz.getName().hashCode();
    	if(eager) {
    		DECODES.put(hashCode, clazz);
    	}
    	if(DECODES.containsKey(hashCode)) {
    		write(OPTIMIZED_DESCRIPTOR);
    		write(hashCode);
    	} else {
    		write(STANDARD_DESCRIPTOR);
    		super.writeClassDescriptor(desc);    		
    	}
        
    }

}
