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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * <p>Title: TempLogger</p>
 * <p>Description: A factory for loggers that quietly log out tonamed files on the tmp drive.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.helpers.TempLogger</code></p>
 */

public class TempLogger {
	/** A cache of temp loggers keyed by logger name */
	private static final Map<String, Logger> loggerCache = new ConcurrentHashMap<String, Logger>();
	/** The temp dir */
	public static final String TMP_NAME = System.getProperty("java.io.tmpdir");
	/** Dynamic name */
	public static final String DYN_NAME = "helios.tmplog";
	/** Default layout */
	public static final String DEFAULT_LAYOUT = PatternLayout.TTCC_CONVERSION_PATTERN;
	/** Default append */
	public static final boolean DEFAULT_APPEND = false;
	/** Default buffer */
	public static final boolean DEFAULT_BUFFERED = false;
	/** Default buffer size */
	public static final int DEFAULT_BUFFER_SIZE = 1024;
	
	
	/**
	 * Acquires the default temp logger
	 * @return the default temp logger
	 */
	public static Logger getTempLogger() {
		return getTempLogger(null, null, null, null, null);
	}
	
	/**
	 * Acquires the named temp logger using the default configuration
	 * @param name The name of the logger
	 * @return the named temp logger
	 */
	public static Logger getTempLogger(String name) {
		return getTempLogger(name, null, null, null, null);
	}
	
	/**
	 * Acquires the named temp logger using the default configuration
	 * @param name The name of the logger
	 * @param layout The logger layout
	 * @return the named temp logger
	 */
	public static Logger getTempLogger(String name, String layout) {
		return getTempLogger(name, layout, null, null, null);
	}
	
	/**
	 * Acquires a temp logger
	 * @param name The name of the logger
	 * @param layout The logger layout
	 * @param append true if the log file should be appended to, false if it should overwrite.
	 * @param bufferedIO true if the file io should be buffered
	 * @param bufferSize the size of the io buffer in bytes
	 * @return the named logger
	 */
	public static Logger getTempLogger(String name, String layout, Boolean append, Boolean bufferedIO, Integer bufferSize) {
		if(name==null) name = DYN_NAME;
		if(append==null) append = DEFAULT_APPEND;
		if(bufferedIO==null) bufferedIO = DEFAULT_BUFFERED;
		if(bufferSize==null) bufferSize = bufferedIO ? DEFAULT_BUFFER_SIZE : 0;
		Logger log = loggerCache.get(name);
		if(log==null) {
			synchronized(loggerCache) {
				log = loggerCache.get(name);
				if(log==null) {
					log = Logger.getLogger("tmp." + name);
					try {						
						log.removeAllAppenders();
						log.addAppender(new FileAppender(new PatternLayout(layout), TMP_NAME + File.separator + name + ".log", append, bufferedIO, bufferSize));
						loggerCache.put(name, log);
					} catch (IOException e) {
						throw new RuntimeException("Failed to create temp logger [" + name + "]", e);
					}
					
				}
			}
		}
		return log;
	}
}
