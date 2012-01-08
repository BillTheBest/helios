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
package org.helios.esper.engine.service.interactive;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: StatementOptions</p>
 * <p>Description: A container for a session's statement options</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.service.interactive.StatementOptions</code></p>
 */

public class StatementOptions implements Serializable {
	/**  */
	private static final long serialVersionUID = -6625879342099995621L;
	/** A map of option values keyed by the option name */
	protected final Map<String, Object> options = new ConcurrentHashMap<String, Object>();
	
	/** The key for the option that defines the event renderer */
	public static final String RENDER = "render";
	/** The key for the option that defines the event waiting timeout */
	public static final String EVENT_TIMEOUT = "event_timeout";
	/** The key for the option that defines the event count timeout */
	public static final String EVENT_COUNT = "event_count";
	/** The key for the option that defines the session object name */
	public static final String SESSION_OBJECT_NAME = "session_object_name";
	
	/** the event timeout default in ms. */
	public static final long DEFAULT_EVENT_TIMEOUT = 15000L;
	/** the event count timeout default */
	public static final int DEFAULT_EVENT_COUNT = 10;
	
	/**
	 * Creates a new Statement options and populates the default
	 */
	public StatementOptions() {
		options.put(RENDER, Render.defaultRender());
		options.put(EVENT_TIMEOUT, DEFAULT_EVENT_TIMEOUT);
		options.put(EVENT_COUNT, DEFAULT_EVENT_COUNT);
	}
	
	
	/**
	 * Sets an option value
	 * @param name The option name
	 * @param value The option value
	 */
	public void setOption(String name, Object value) {
		if(name!=null && value!=null) {
			if(RENDER.equals(name) && value instanceof CharSequence) {
				if(Render.isRender(value.toString())) {
					value = Render.valueOf(value.toString().toUpperCase());
				} else {
					value = Render.defaultRender().name();
				}
			}
			options.put(name, value);
		} else {
			throw new RuntimeException("Name or value was null [" + name + "/" + value + "]");
		}
	}
	
	/**
	 * Retrieves the value of an option
	 * @param name the option name
	 * @return the option value or null if the name is not bound
	 */
	public Object getOption(String name) {
		if(name!=null ) {
			return options.get(name);
		} else {
			throw new RuntimeException("Name was null [" + name + "]");
		}		
	}
	
	/**
	 * Returns an array of the bound option names
	 * @return an array of the bound option names
	 */
	public String[] getOptionNames() {
		return options.keySet().toArray(new String[options.size()]);
	}
	
	/**
	 * Returns the option map
	 * @return the option map
	 */
	public Map<String, Object> getOptions() {
		return Collections.unmodifiableMap(options);
	}
}
