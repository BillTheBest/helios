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
package org.helios.nativex.jmx.disk;

import java.util.Map;

import org.helios.helpers.CollectionHelper;
import org.hyperic.sigar.FileSystem;

/**
 * <p>Title: FileSystemType</p>
 * <p>Description: Enumerates the file system types for Sigar FileSystems</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.disk.FileSystemType</code></p>
 */

public enum FileSystemType {
	TYPE_UNKNOWN(0, "Unknown"),
	TYPE_NONE(1, "None"),
	TYPE_LOCAL_DISK(2, "Local"),
	TYPE_NETWORK(3, "Network"),
	TYPE_RAM_DISK(4, "RAM"),
	TYPE_CDROM(5, "ROM"),
	TYPE_SWAP(6, "Swap");

	
	/** A map of FileSystemTypes indexed by code */
	private static final Map<Integer, FileSystemType> CODE2TYPE = CollectionHelper.createIndexedValueMap(false, true, FileSystemType.values());
	
	/**
	 * Creates a new FileSystemType
	 * @param code The sigar code for this file system type
	 * @param niceName The english name of the file system type
	 */
	private FileSystemType(int code, String niceName) {
		this.code = code;
		this.niceName = niceName;
	}
	
	/** The sigar code for this file system type */
	private final int code;
	/** The nice name for the type */
	private final String niceName;
	
	/**
	 * Determines if the passed file system is of this file system type. 
	 * @param fs The file system to test
	 * @return true if the passed file system is of this type.
	 */
	public boolean isType(FileSystem fs) {
		if(fs==null) return false;
		return fs.getType()==code;
	}

	/**
	 * The sigar code for this file system type
	 * @return the code
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * Returns the FileSystemType for the passed code.
	 * @param code The code to the get the FileSystemType for.
	 * @return the FileSystemType that maps to the passed code or null if it does not map. 
	 */
	public static FileSystemType typeForCode(int code) {
		return CODE2TYPE.get(code);
	}

	/**
	 * Returns the nice name for the type
	 * @return the niceName
	 */
	public String getNiceName() {
		return niceName;
	}
}
