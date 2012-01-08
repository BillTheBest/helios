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

import static org.helios.helpers.ClassHelper.nvl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;


/**
 * <p>Title: FileHelper</p>
 * <p>Description: Static file helper utilities</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 222 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-utils/trunk/src/org/helios/helpers/FileHelper.java $
 * $Id: FileHelper.java 222 2008-07-08 21:05:12Z nwhitehead $
 */
public class FileHelper {
	/**
	 * Reads a byte array from a URL
	 * @param sourceUrl the URL to read from
	 * @return a byte array
	 */
	public static byte[] getBytesFromUrl(URL sourceUrl) {
		byte[] buffer = new byte[8192];
		int bytesRead = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(sourceUrl.openStream());
			while(true) {
				bytesRead = bis.read(buffer);
				if(bytesRead==-1) break;
				baos.write(buffer, 0, bytesRead);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read bytes from URL [" + sourceUrl + "]", e);
		} finally {
			try { bis.close(); } catch (Exception e) {}
		}
	}
	
	
	/**
	 * Replaces the textual content of a file with the passed content
	 * @param fileName The Java IO File Name
	 * @param content The content to write to the file
	 * @return the file that was operated on
	 */
	public static File replaceFileContent(CharSequence fileName, CharSequence content) {
		FileOutputStream fos = null;
		FileDescriptor fd = null;
		try {
			File f = new File(nvl(fileName, "The passed file name was null").toString());
			if(!f.exists()) {
				f.createNewFile();
			}
			if(!f.canWrite()) {
				throw new Exception("Cannot write to file [" + f + "]");
			}
			fos = new FileOutputStream(f, false);
			fd = fos.getFD();
			fos.write(content.toString().getBytes());
			return f;
		} catch (Exception e) {
			throw new RuntimeException("Failed to write content to file [" + fileName + "]", e);
		} finally {
			try { fos.flush(); } catch (Exception e) {}
			try { fos.close(); } catch (Exception e) {}
			try { fd.sync(); } catch (Exception e) {}
		}		
	}
	
	/**
	 * Writes a byte array to a file.
	 * @param file The file to write to
	 * @param append If true, an existing file is appended to, otherwise, the existing content (if any) is overwritten
	 * @param bytes The byte array to write.
	 */
	public static void writeBytesToFile(File file, boolean append, byte[] bytes) {
		FileOutputStream fos = null;
		FileDescriptor fd = null;
		try {
			
			if(!nvl(file, "The passed file was null").exists()) {
				file.createNewFile();
			}
			if(!file.canWrite()) {
				throw new Exception("Cannot write to file [" + file + "]");
			}
			fos = new FileOutputStream(file, append);
			fd = fos.getFD();
			fos.write(nvl(bytes, "The passed byte array was null"));
		} catch (Exception e) {
			throw new RuntimeException("Failed to write content to file [" + file + "]", e);
		} finally {
			try { fos.flush(); } catch (Exception e) {}
			try { fos.close(); } catch (Exception e) {}
			try { fd.sync(); } catch (Exception e) {}
		}				
	}
	
	/**
	 * Prepends a string value to the beginning of a file
	 * @param prepend The prepended value
	 * @param addEol If true, adds an EOL character to the prepend
	 * @param fileName The file name to prepend to
	 */
	public static void prependToFile(CharSequence prepend, boolean addEol, String fileName) {
		if(prepend==null) throw new IllegalArgumentException("Passed prepend was null", new Throwable());
		if(fileName==null) throw new IllegalArgumentException("Passed fileName was null", new Throwable());
		File f = new File(fileName);
		if(!f.exists() || !f.canRead() || !f.canWrite()) throw new IllegalArgumentException("The file [" + fileName + "] is not accessible", new Throwable());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		byte[] buffer = new byte[8096];
		int bytesRead = 0;
		try {
			baos.write(prepend.toString().getBytes());
			if(addEol) {
				baos.write(System.getProperty("line.separator", "\n").getBytes());
			}
			fis = new FileInputStream(f);
			bis = new BufferedInputStream(fis);
			while((bytesRead = bis.read(buffer)) != -1) {
				baos.write(buffer, 0, bytesRead);
			}
			bis.close(); bis = null;
			fis.close(); fis = null;
			fos = new FileOutputStream(f, false);
			bos = new BufferedOutputStream(fos);
			bos.write(baos.toByteArray());			
			bos.close();			
		} catch (Exception e) {
			throw new RuntimeException("Failed to prepend to file [" + fileName + "]", e);
		} finally {
			if(bis!=null) try { bis.close(); } catch (Exception e) {}
			if(fis!=null) try { fis.close(); } catch (Exception e) {}
			if(bos!=null) try { bos.close(); } catch (Exception e) {}
			if(fos!=null) try { fos.close(); } catch (Exception e) {}						
		}		
	}
	
	/**
	 * Prepends the contents of one file to the beginning of another file
	 * @param prepend The file containing the prepended value
	 * @param addEol If true, adds an EOL character to the prepend
	 * @param fileName The file name to prepend to
	 */
	public static void prependFileToFile(String prepend, boolean addEol, String fileName) {
		if(prepend==null) throw new IllegalArgumentException("Passed prepend file name was null", new Throwable());
		File f = new File(prepend);
		if(!f.exists() || !f.canRead()) throw new IllegalArgumentException("The prepend file [" + fileName + "] is not accessible", new Throwable());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		byte[] buffer = new byte[8096];
		int bytesRead = 0;
		try {
			fis = new FileInputStream(f);
			bis = new BufferedInputStream(fis);
			while((bytesRead = bis.read(buffer)) != -1) {
				baos.write(buffer, 0, bytesRead);
			}
			bis.close(); bis = null;
			fis.close(); fis = null;
			prependToFile(baos.toString(), addEol, fileName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to prepend value in file [" + prepend + "] to file [" + fileName + "]", e);
		} finally {
			if(bis!=null) try { bis.close(); } catch (Exception e) {}
			if(fis!=null) try { fis.close(); } catch (Exception e) {}			
		}		
	}
	
	
	public static void main(String[] args) {
		prependFileToFile("/tmp/prepend.txt", false, "/tmp/rule.txt");
	}
	
	/**
	 * Sets the timestamp of the passed file
	 * @param file The file to set the timestamp on
	 * @param timestamp the timestamp to set the file to
	 * @return true if the touch suceeded, false if it did not.
	 */
	public static boolean touch(File file, long timestamp) {
		try {
			nvl(file, "The passed file was null").setLastModified(timestamp);
			sync(file);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Sets the timestamp of the passed file to current time
	 * @param file The file to set the timestamp on
	 * @return true if the touch suceeded, false if it did not.
	 */	
	public static boolean touch(File file) {
		return touch(file, System.currentTimeMillis());
	}
	
	/**
	 * Ticks up the timestamp of the passed file to the minimum detectable time above the current time.
	 * @param file The file to set the timestamp on
	 * @return true if the touch suceeded, false if it did not.
	 */	
	public static boolean tick(File file) {
		if(!nvl(file, "The passed file was null").canRead()) return false;
		if(!file.canWrite()) return false;
		try {
			long currentTs = file.lastModified();
			file.setLastModified(currentTs+1);
			sync(file);
			if(currentTs==file.lastModified()) {				
				file.setLastModified(currentTs+1000);
			}
			sync(file);
			return true;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return false;
		}
	}
	

	

	
	/**
	 * Syncs the file to the underlying storage
	 * @param file the file to sync
	 * @return true if op completed successfully, false if not.
	 */
	public static boolean sync(File file) {
		FileInputStream fis = null;
		try {
			if(!nvl(file, "The passed file was null").exists()) throw new RuntimeException("The file [" + file + "] does not exist");
			fis = new FileInputStream(file);
			fis.getFD().sync();
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			try { if(fis!=null) fis.close(); } catch (Exception e) {}
		}
	}
	
	
}
