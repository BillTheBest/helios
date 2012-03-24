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
package org.helios.ot.generic.serialization;

import static org.helios.ot.generic.serialization.OptimizedObjectOutputStream.OPTIMIZED_DESCRIPTOR;
import static org.helios.ot.generic.serialization.OptimizedObjectOutputStream.STANDARD_DESCRIPTOR;
import static org.helios.ot.generic.serialization.OptimizedObjectOutputStream.DECODES;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * <p>Title: OptimizedObjectInputStream</p>
 * <p>Description: {@link java.io.ObjectInputStream} extension that reads a shrunk class descriptor of the object being serialized by representing it as the hashcode of the class name.
 * Use of this extension assumes that the serializer has encoded the hashcode using an int this instance can decode.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.generic.serialization.OptimizedObjectInputStream</code></p>
 */

public class OptimizedObjectInputStream extends ObjectInputStream {

	/**
	 * Creates a new OptimizedObjectInputStream
	 * @param in The input stream is stream reads from
	 * @throws IOException If an IO error occurs.
	 */
	public OptimizedObjectInputStream(InputStream in) throws IOException {
		super(in);
	}
	
	/**
	 * Read a class descriptor from the serialization stream. 
	 * @return the class descriptor read
	 * @throws IOException If an IO error occurs
	 * @throws ClassNotFoundException If the optimized class descriptor cannot be decoded, or the standard descriptor cannot find the represented class
	 */
	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
		int optimized = read();
		if(optimized != OPTIMIZED_DESCRIPTOR && optimized != STANDARD_DESCRIPTOR) {
            throw new EOFException("Failed to read the expected optimized flag from the input stream [" + optimized + "]");
		}
		if(optimized == OPTIMIZED_DESCRIPTOR) {
			int hashCode = read();
			Class<?> clazz = DECODES.get(hashCode);
			if(clazz==null) {
				throw new ClassNotFoundException("Failed to decode optimized class descriptor for hashcode [" + hashCode + "]", new Throwable());
			}
			return ObjectStreamClass.lookup(clazz);
		} else {
			return super.readClassDescriptor();
		}
	}

}
