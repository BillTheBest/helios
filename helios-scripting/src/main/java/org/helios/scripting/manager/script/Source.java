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
package org.helios.scripting.manager.script;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptEngine;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;



/**
 * <p>Title: Source</p>
 * <p>Description: Encapsulation for a script source. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.scripting.manager.script.Source</code></p>
 * @ToDo:  Store URL content type in case it has a helpful hint for the engine type
 * 
 */
@JMXManagedObject(annotated=true, declared=true)
public class Source {
	/** A reference to the source */
	protected final AtomicReference<String> source = new AtomicReference<String>(null);
	/** The last change timestamp */
	protected final AtomicLong lastChange = new AtomicLong(0L);
	/** The minimum time in ms. between checks for updated source */
	protected final long minChangeCheckTime;
	/** The content read size */
	protected final AtomicInteger contentSize = new AtomicInteger(0);
	/** A set of source change listeners for this source */
	protected final Set<SourceChangeListener> listeners = new CopyOnWriteArraySet<SourceChangeListener>();
	/** The designated name */
	protected final String name;
	/** The original source name */
	protected final URL location;
	/** the script extension */
	protected final String extension;
	/** indicates if the location source is timestamp aware */
	protected final AtomicBoolean lastChangeAware = new AtomicBoolean(false);
	/** instance logger */
	protected final Logger log;
	
	/** An anonymous script name suffix factory */
	private static final AtomicLong nameSerial = new AtomicLong(0L);
	/** The default script name prefix when a name cannot be determined */
	public static final String ANON_SCRIPT_NAME = "Anonymous";
	
	/**
	 * Creates a new source instance from the given URL
	 * @param minChangeCheckTime The minimum time in ms. between checks for updated source 
	 * @param sourceUrl The URL of the original source file.
	 * @param listeners An optional array of source change listeners that will be notified when the source changes.
	 */
	public Source(long minChangeCheckTime, URL sourceUrl, SourceChangeListener...listeners) {
		if(sourceUrl==null) throw new IllegalArgumentException("Passed sourceUrl was null", new Throwable());
		this.minChangeCheckTime = minChangeCheckTime;
		location = sourceUrl;
		String[] frags = location.toExternalForm().split("/");
		name = frags[frags.length-1];
		log = Logger.getLogger(getClass().getName() + "." + name.replace(".", "_"));
		frags = name.split("\\.");
		extension = frags[frags.length-1];	
		load();
		if(listeners!=null) {
			for(SourceChangeListener listener: listeners) {
				addListener(listener);
			}
		}
	}
	
	/**
	 * Creates a new source instance from a passed string
	 * @param source the source
	 * @param listeners An optional array of source change listeners that will be notified when the source changes.
	 */
	public Source(CharSequence source, SourceChangeListener...listeners) {
		if(source==null) throw new IllegalArgumentException("Passed source was null", new Throwable());
		this.minChangeCheckTime = 0;
		location = null;
		lastChangeAware.set(false);
		lastChange.set(System.currentTimeMillis());
		contentSize.set(source.toString().getBytes().length);
		ScriptEngine scriptEngine = ScriptHelper.sniffEngine(source);
		if(scriptEngine==null) {
			throw new RuntimeException("The source for the passed script could not be matched to a script engine", new Throwable());
		}
		this.extension = scriptEngine.getFactory().getExtensions().iterator().next();
		CharSequence scriptName = null;
		try { scriptName = (CharSequence) ScriptHelper.getScriptVariable(source, StandardConstants.NAME.getVariable()); } catch (Exception e) {}
		if(scriptName==null) {
			try { scriptName = (CharSequence) ScriptHelper.getTopFunctionResult(scriptEngine, source, StandardConstants.NAME.getTopLevelMethod()); } catch (Exception e) {}
			if(scriptName==null) {
				try { scriptName = ScriptHelper.getScriptConstants(source).get(StandardConstants.NAME.getComment()); } catch (Exception e) {}
			}
		}
		if(scriptName==null) {
			scriptName = ANON_SCRIPT_NAME + nameSerial.incrementAndGet();
		}
		name = scriptName.toString();
		
		log = Logger.getLogger(getClass().getName() + "." + scriptName.toString().replace(".", "_"));
		if(listeners!=null) {
			for(SourceChangeListener listener: listeners) {
				addListener(listener);
			}
		}
	}
	
	
	/**
	 * Registers a source change listener
	 * @param listener
	 */
	public void addListener(SourceChangeListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a source change listener
	 * @param listener
	 */
	public void removeListener(SourceChangeListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}
	}
	
	
	/**
	 * Loads the source from the location URL
	 */
	protected boolean load() {
		if(location==null) return false;
		long now = System.currentTimeMillis();
		URLConnection conn = null;
		InputStream is = null;
		BufferedInputStream bis = null;
		ByteArrayOutputStream baos = null;		
		try {
			conn = location.openConnection();
			long lastModified = conn.getLastModified();
			if(lastModified==0) {
				lastChangeAware.set(false);
			} else {
				if(lastModified<lastChange.get()) {  // not 0, but older than last change
					return false;
				}
				lastChangeAware.set(true);
				lastChange.set(now);
			}
			is = conn.getInputStream();
			bis = new BufferedInputStream(is);
			int contentLength = conn.getContentLength();
			byte[] buffer = new byte[contentLength==-1 ? 8092 : contentLength];
			baos = new ByteArrayOutputStream(contentLength==-1 ? 8092 : contentLength);
			int bytesRead = -1;
			contentSize.set(0);
			while((bytesRead=bis.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
				contentSize.addAndGet(bytesRead);
			}
			source.set(baos.toString());
			return true;
		} catch (Exception e) {
			log.warn("Failed to load source", e);
			return false;
		} finally {
			try { if(bis!=null) bis.close(); } catch (Exception e) {}
			try { if(is!=null) is.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Checks for a source update
	 * @return true if a change was detected and loaded, false otherwise
	 */
	@JMXOperation(name="checkForUpdate", description="Checks for a source update and return true if a source update was completed")
	public boolean checkForUpdate() {
		long timeSinceLastUpdate = System.currentTimeMillis()-lastChange.get();
		if(timeSinceLastUpdate >= minChangeCheckTime) {
			load();
			fireSourceChange();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Notifies all listeners of a source change
	 */
	private void fireSourceChange() {
		for(SourceChangeListener listener: listeners) {
			listener.onSourceChange(this);
		}		
	}

	/**
	 * Returns the source code
	 * @return the source code
	 */
	@JMXAttribute(name="Source", description="The source code of the script", mutability=AttributeMutabilityOption.READ_WRITE)
	public String getSource() {
		return source.get();
	}
	
	/**
	 * Sets the source code for this source. Invoking this operation will automatically disable any auto refreshing.
	 * @param newSource
	 */
	public void setSource(String newSource) {
		if(newSource==null) throw new IllegalArgumentException("Passed source was null", new Throwable());
		source.set(newSource);
		lastChangeAware.set(false);
		lastChange.set(System.currentTimeMillis());
		contentSize.set(newSource.getBytes().length);
		fireSourceChange();
	}
	

	/**
	 * The last change timestamp
	 * @return the lastChange
	 */
	@JMXAttribute(name="LastChange", description="The last change timestamp", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastChange() {
		return lastChange.get();
	}
	
	/**
	 * The last change date
	 * @return the lastChange date
	 */
	@JMXAttribute(name="LastChangeDate", description="The last change date", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getLastChangeDate() {
		if(lastChange.get()<1) return null;
		return new Date(lastChange.get());
	}
	

	/**
	 * The size of the retrieved content
	 * @return size of the retrieved content
	 */
	@JMXAttribute(name="ContentSize", description="The source content size", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getContentSize() {
		return contentSize.get();
	}

	/**
	 * The designated name of this script source
	 * @return the name of the script
	 */
	@JMXAttribute(name="Name", description="The designated name of this script source", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getName() {
		return name;
	}

	/**
	 * Returns the URL of the original source file
	 * @return the source URL
	 */
	@JMXAttribute(name="Location", description="The URL of the original source file", mutability=AttributeMutabilityOption.READ_ONLY)
	public URL getLocation() {
		return location;
	}

	/**
	 * The extension of the source URL used to determine the script type
	 * @return extension of the source URL used to determine the script type
	 */
	@JMXAttribute(name="Extension", description="The extension of the source URL used to determine the script type", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getExtension() {
		return extension;
	}

	/**
	 * Indicates if the URL's server is last changed timestamp aware
	 * @return true if server is last changed timestamp aware, false otherwise
	 */
	@JMXAttribute(name="LastChangeAware", description="Indicates if the URL's server is last changed timestamp aware", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getLastChangeAware() {
		return lastChangeAware.get();
	}


}















