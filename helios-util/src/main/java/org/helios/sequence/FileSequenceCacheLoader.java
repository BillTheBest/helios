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
package org.helios.sequence;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.helios.helpers.ClassHelper;

/**
 * <p>Title: FileSequenceCacheLoader</p>
 * <p>Description: A SequenceCacheLoader implementation that persists the sequence cache to the configured file. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.sequence.FileSequenceCacheLoader</code></p>
 */

public class FileSequenceCacheLoader implements SequenceCacheLoader {
	/** The store file */
	protected final File storeFile;

	/** The default file for the store */
	public static final File DEFAULT_FILE = new File(System.getProperty("java.io.tmpdir") + File.separator + FileSequenceCacheLoader.class.getName() + ".ser");
	/**
	 * Creates a new FileSequenceCacheLoader using the passed file to persist
	 * @param storeFile The store file
	 */
	public FileSequenceCacheLoader(File storeFile) {
		this.storeFile = ClassHelper.nvl(storeFile, "The passed file was null");
		if(!this.storeFile.exists()) {
			try {
				if(!this.storeFile.createNewFile()) {
					throw new RuntimeException("The store file [" + this.storeFile + "] could not be created");
				}
			} catch (Exception e) {
				throw new RuntimeException("The store file [" + this.storeFile + "] could not be created", e);
			}
		}
		if(!this.storeFile.canRead()) throw new RuntimeException("The store file [" + this.storeFile + "] cannot be read");
		if(!this.storeFile.canRead()) throw new RuntimeException("The store file [" + this.storeFile + "] cannot be read");

	}
	
	/**
	 * Creates a new FileSequenceCacheLoader 
	 * @param storeFile The name of the file to store to
	 */
	public FileSequenceCacheLoader(String storeFile) {
		this.storeFile = ClassHelper.nvl(new File(storeFile), "The passed file name was null");
	}
	
	/**
	 * Creates a new FileSequenceCacheLoader using the default file store.
	 */
	public FileSequenceCacheLoader() {
		this(DEFAULT_FILE);
	}
	
	/**
	 * Saves a new sequence into the persistent cache store
	 * @param sequence The new sequence
	 * @param cache The sequence cache the sequence has already been writen into
	 */
	public void saveSequence(Sequence sequence, SequenceCache cache) {
		saveToFile(cache);
	}
	
	
	
	
	/**
	 * Loads the sequence cache from the persistent store.
	 * The number of uncommited sequences consumed since these sequences
	 * were saved is unknown so we need to push the highwater mark up by the batch size
	 * and re-save each sequence.
	 * @return a sequence cache
	 */
	public SequenceCache loadSequenceCache() {
		SequenceCache cache = loadFromFile();
		for(Sequence s: cache.values()) {
			s.hwm.addAndGet(s.batchSize);			
		}
		saveToFile(cache);
		return cache;
	}
	
	/**
	 * Loads the next batch window for the passed sequence and persists persists the cache to store.
	 * @param sequence The sequence to reload
	 * @return the next seed for the passed sequence.
	 */
	public long reloadSequence(Sequence sequence) {
		return 0;
	}
	
	protected SequenceCache loadFromFile() {
		SequenceCache cache = null;
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		ObjectInputStream ois = null;
		long fileLength = 0;
		try {
			fileLength = storeFile.length();
			if(fileLength<=0) {
				cache = new SequenceCache();
				saveToFile(cache);
				return cache;
			}
			int bufferSize = storeFile.length()>Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)fileLength;
			fis = new FileInputStream(storeFile);
			bis = new BufferedInputStream(fis, bufferSize);
			ois = new ObjectInputStream(bis);
			cache = (SequenceCache)ois.readObject();
			return cache;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load cache from file [" + storeFile + "]. Length was [" + fileLength + "]", e);
		} finally {
			try { fis.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Purges the underlying data store, deleting all sequence cache entries.
	 */
	public void purge() {
		storeFile.delete();
	}
	
	/**
	 * Saves the passed sequence cache, overwriting all values
	 * @param cache The cache to save
	 */
	public void saveSequenceCache(SequenceCache cache) {
		purge();
		saveToFile(cache);
	}
	
	
	
	protected void saveToFile(SequenceCache cache) {
		FileOutputStream fos = null;
		ObjectOutputStream ois = null;
		try {
			if(!storeFile.exists()) {
				storeFile.createNewFile();
			}
			fos = new FileOutputStream(storeFile, false);
			ois = new ObjectOutputStream(fos);
			ois.writeObject(cache);
			ois.flush();
			fos.flush();
		} catch (Exception e) {
			throw new RuntimeException("Failed to save cache to file [" + storeFile + "]", e);
		} finally {
			try { ois.flush(); } catch (Exception e) {}
			try { fos.flush(); } catch (Exception e) {}
			try { fos.close(); } catch (Exception e) {}
			
		}
	}
	
	public static void main(String[] args) {
		try {
			FileSequenceCacheLoader loader = new FileSequenceCacheLoader();
			SequenceCache cache = loader.loadFromFile();
			cache.put("Dude", new Sequence(1, 1, 1, "Dude",  null));
			loader.saveToFile(cache);
			System.out.println("Done");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    StringBuilder retValue = new StringBuilder("FileSequenceCacheLoader [")
	        .append("storeFile = ").append(this.storeFile)
	        .append(" ]");    
	    return retValue.toString();
	}
}
