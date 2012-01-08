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
package org.helios.io.file.change;

import java.util.HashSet;
import java.util.Set;

import org.helios.enums.IBinaryCounter;

/**
 * <p>Title: FileChangeType</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.io.file.change.FileChangeType</code></p>
 */

public enum FileChangeType implements IBinaryCounter {
	/** Listens for File Changed Events */
	FILE_CHANGED(counter.next()),
	/** Listens for File Added Events */
	FILE_ADDED(counter.next()),
	/** Listens for File Deleted Events */
	FILE_DELETED(counter.next()),
	/** Listens for Events in a directory recursively (or only in the root) */
	FILE_RECURSIVE(counter.next()),
	/** Events for added and deleted include directories (or files only)  */
	FILE_DIRS_INLCUDED(counter.next());
	
	/**
	 * Creates a new FileChangeType 
	 * @param type a binary counter code
	 */
	private FileChangeType(int type) {
		this.type = type;
	}
	
	private final int type;

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Creates an option bit mask indiacating the enablement of the passed FileChangeTypes
	 * @param types the FileChangeTypes to enable
	 * @return an option bit mask 
	 */
	public static int getCodeFor(FileChangeType...types) {
		int c = 0;
		if(types!=null) {
			for(FileChangeType fct: types) {
				c = c | fct.type;
			}
		}
		return c;
	}
	
	/**
	 * Returns the FileChangeTypes enabled by the passed bitmask
	 * @param bitMask the bit mask to interpret
	 * @return an array of enabled FileChangeTypes
	 */
	public static FileChangeType[] getFileChangeTypes(int bitMask) {
		Set<FileChangeType> enabled = new HashSet<FileChangeType>(3);
		for(FileChangeType fct: FileChangeType.values()) {
			if(fct.isEnabled(bitMask)) {
				enabled.add(fct);
			}
		}
		return enabled.toArray(new FileChangeType[enabled.size()]);
	}
	
	/**
	 * Determines if this FileChangeType is enabled for the passed bitMask
	 * @param bitMask the bitMask to test
	 * @return true if this FileChangeType is enabled for the passed bitMask
	 */
	public boolean isEnabled(int bitMask) {
		return (bitMask & type)==type;
	}
		
	
}
