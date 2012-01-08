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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * <p>Title: StreamHelper</p>
 * <p>Description: Static helper methods for working with streams</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.helpers.StreamHelper</code></p>
 */

public class StreamHelper {
	/** The default buffer size to use for reading streams */
	public static final int DEFAULT_BUFFER_SIZE = 8192; 
	/**
	 * Reads and returns a byte array from an input stream using the default buffer size of {@literal DEFAULT_BUFFER_SIZE} 
	 * @param is the input stream to read from
	 * @return the read byte array
	 */
	public static byte[] readByteArrayFromStream(InputStream is) {
		return readByteArrayFromStream(DEFAULT_BUFFER_SIZE, is);
	}
	
	/**
	 * Reads and returns a byte array from an input stream
	 * @param bufferSize the buffer size to use
	 * @param is the input stream to read from
	 * @return the read byte array
	 */
	public static byte[] readByteArrayFromStream(int bufferSize, InputStream is) {
		if(is==null) throw new IllegalArgumentException("Passed input stream was null", new Throwable());
		byte[] buffer = new byte[bufferSize];
		int bytesRead = -1;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
			while((bytesRead=is.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read byte array from input stream", e);
		}		
	}
}
