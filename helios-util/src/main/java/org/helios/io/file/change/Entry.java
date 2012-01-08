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

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: Entry</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.io.file.change.Entry</code></p>
 */

public class Entry {
	/** The name of the entry */
	protected final String name;
	/** The timestamp of the entry */
	protected long timestamp;
	/** Indicates if this entry is a dir */
	protected final boolean isDir;
	/** Indicates if this entry is a file */
	protected final boolean isFile;
	/** Sweep Marker */
	protected final AtomicLong sweepMarker = new AtomicLong(0L);

	

	/**
	 * Creates a new Entry for the passed file 
	 * @param file THe file to create an entry for
	 */
	public Entry(File file) {
		if(file==null || !file.exists()) {
			throw new IllegalArgumentException("Passed file was null or does not exist");
		}
		name = file.getName();
		timestamp = file.lastModified();
		isDir = file.isDirectory();
		isFile = file.isFile();
	}
	
	/**
	 * Returns the timestamp of the entry
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	/**
	 * Sets the timestamp of the entry
	 * @param timestamp the new timestamp to set
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	/**
	 * Returns the name of the entry
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Indicates if the entry is a dir
	 * @return true if the entry is a dir, false if it is a file.
	 */
	public boolean isDir() {
		return isDir;
	}

	/**
	 * Indicates if the entry is a file
	 * @return true if the entry is a file, false if it is a dir.
	 */
	public boolean isFile() {
		return isFile;
	}
	
}
