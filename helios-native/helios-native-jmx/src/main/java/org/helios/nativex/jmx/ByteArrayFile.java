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
package org.helios.nativex.jmx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * <p>Title: ByteArrayFile</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.ByteArrayFile</code></p>
 */

public class ByteArrayFile implements Serializable {

	/**  */
	private static final long serialVersionUID = 313739302887727240L;
	protected final byte[] content;
	protected final String fileName;
	
	/**
	 * Creates a new ByteArrayFile
	 * @param content The file content
	 */
	public ByteArrayFile(byte[] content) {
		this(content, null);
	}
	
	/**
	 * Creates a new ByteArrayFile
	 * @param content The file content
	 * @param fileName The file name to deserialize as
	 */
	public ByteArrayFile(byte[] content, String fileName) {
		super();
		this.content = content;
		this.fileName = fileName;
	}
	
	/**
	 * Returns this object as a resolved file when the object is deserialized.
	 * @return
	 * @throws ObjectStreamException
	 */
	protected Object readResolve() throws ObjectStreamException {
		FileOutputStream fos = null;
		try {
			File f = fileName==null ? File.createTempFile(getClass().getSimpleName(), ".file") : new File(fileName);
			if(!f.canWrite()) {
				throw new Exception("Unable to write to designated file [" + fileName + "]", new Throwable());				
			}
			fos = new FileOutputStream(f);
			fos.write(content);
			fos.close();
			fos = null;
			return f;
		} catch (Exception e) {
			throw new RuntimeException("Failed to readResolve", e);
		} finally {
			try { if(fos!=null) fos.close(); } catch (Exception e) {}
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ByteArrayFile baf = new ByteArrayFile("Hello World".getBytes());
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(baf);
			oos.flush();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			File file = (File)ois.readObject();
			System.out.println("File:" + file.getAbsolutePath() + "  Size:" + file.length());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

}
