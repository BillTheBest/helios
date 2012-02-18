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
package org.helios.scripting.manager;

import groovy.lang.Binding;

import java.io.ObjectStreamException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import javax.script.Bindings;


/**
 * <p>Title: ConcurrentBindings</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.scripting.manager.ConcurrentBindings</code></p>
 */

public class ConcurrentBindings extends ConcurrentHashMap<String, Object> implements Bindings {
	/**  */
	private static final long serialVersionUID = 4062721271195777376L;
	/** The OpenType to represent ConcurrentBindings */
	private static final TabularType tType = getType();
	/** A groovy binding wrapper of this binding */
	protected final Binding groovyBinding;
	/**
	 * Creates a new ConcurrentBindings
	 */
	public ConcurrentBindings() {
		super();
		groovyBinding = new Binding(this);
	}
	
	public Object put(String s, Object o) {
		return super.put(s, o);
	}
	
	/**
	 * Returns the Groovy binding wrapper for this binding
	 * @return the Groovy binding wrapper for this binding
	 */
	public Binding getGroovyBinding() {
		Binding binding = new Binding();
		for(Map.Entry<String, Object> entry: entrySet()) {
			binding.setProperty(entry.getKey(), entry.getValue());
		}
		return binding;
	}

	/**
	 * Creates a new ConcurrentBindings 
	 * @param initialCapacity
	 * @param loadFactor
	 * @param concurrencyLevel
	 */
	public ConcurrentBindings(int initialCapacity, float loadFactor,
			int concurrencyLevel) {
		super(initialCapacity, loadFactor, concurrencyLevel);
		groovyBinding = new Binding(this);
	}

	/**
	 * Creates a new ConcurrentBindings 
	 * @param initialCapacity
	 * @param loadFactor
	 */
	public ConcurrentBindings(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		groovyBinding = new Binding(this);
	}


	/**
	 * Creates a new ConcurrentBindings
	 * @param initialCapacity
	 */
	public ConcurrentBindings(int initialCapacity) {
		super(initialCapacity);
		groovyBinding = new Binding(this);
	}
	
	/**
	 * Returns a TabularData representation of these bindings
	 * @return a TabularData
	 */
	public TabularData getTabularData() {
		TabularDataSupport tds = new TabularDataSupport(tType, this.size(), 0.75f);
		for(Map.Entry<String, Object> entry: entrySet()) {
			tds.put(entry.getKey(), entry.getValue().toString());
		}
		return tds;
	}

	/**
	 * @return
	 * @throws ObjectStreamException
	 */
	Object writeReplace() throws ObjectStreamException {
		return getTabularData();
	}

	/**
	 * Generates the tabular type for ConcurrentBindings
	 * @return the tabular type for ConcurrentBindings
	 */
	public static TabularType getType() {
		try {
			return new TabularType(
					ConcurrentBindings.class.getName(),
					"javax.script Script Context Bindings",
					new CompositeType(
							Bindings.class.getName(),
							"A javax.script Script Context Binding",
							new String[]{"key", "value"},
							new String[]{"The name associated with the value", "The value associated with the name"},
							new OpenType[]{SimpleType.STRING, SimpleType.STRING}							
					),
					new String[]{"key"}
			);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ConcurrentBindings TabularType", e);
		}
	}

}
