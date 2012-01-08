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
package org.helios.options;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.prefs.Preferences;

import org.helios.helpers.StringHelper;

/**
 * <p>Title: AbstractOption</p>
 * <p>Description: Encapsulates an option instance. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.options.OptionImpl</code></p>
 */

public abstract class AbstractOption<T> implements IOption<T> {
	/** The name of the option */
	protected final String name;
	/** The user preferneces key for this option */
	protected final String key;
	/** The value for this option */
	protected T value;
	/** the default value as a string */
	protected final String defaultValue;
	/** The type for this option */
	protected final Class<T> type;
	/** The option description */
	protected final String description;
	
	/** The user preferences manager */
	protected static final Preferences prefs = Preferences.userNodeForPackage(AbstractOption.class);
	
	/** Constant for an empty byte array */
	public static final byte[] EMPTY_BYTE_ARR = {};
	
	/**
	 * Creates a new AbstractOption.
	 */
	public AbstractOption() {
		Option opt = this.getClass().getAnnotation(Option.class);
		if(opt==null) throw new RuntimeException("No @Option annotation on class [" + this.getClass().getName() + "]");
		this.name = opt.name();
		this.key = opt.key();
		this.description = opt.description();
		this.defaultValue = StringHelper.tokenReplaceSysProps(opt.defaultValue());
		this.type = (Class<T>) opt.type();
		String defValue = opt.defaultValue();
		T loadedVal = load();
		this.value = loadedVal!=null ? loadedVal : getDefaultValue(defValue);
		if(loadedVal==null) {
			update();
		}
	}
	
	/**
	 * Returns the typed default value for this option.
	 * @param defValue The default value initialization string
	 * @return the typed default value
	 */
	protected T getDefaultValue(String defValue) {
		PropertyEditor pe = PropertyEditorManager.findEditor(type);
		if(pe==null) throw new RuntimeException("No property editor found for type [" + type.getName() + "] defined by option [" + this.key + "]");
		pe.setAsText(defValue);
		return (T) pe.getValue();
	}
	
	/**
	 * Displays an option tersely
	 * @return a tersely displayed option
	 */
	public String display() {
		StringBuilder b = new StringBuilder("\n\t");
		b.append(":").append(value).append("\n");
		return b.toString();
	}
	
	/**
	 * The generic user preference load operation
	 * @return the user preference load
	 */
	public abstract T load();		
	
	/**
	 * Saves the option value to user preferences.
	 */
	public abstract void update();
	
	/**
	 * The name of this option
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * The user prefs key for this option
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * The option's current value
	 * @return the value
	 */
	public T getValue() {
		return value;
	}
	
	/**
	 * Sets the value of the option and triggers a save to user prefs.
	 * @param newValue the new value of the pref
	 */
	public void setValue(T newValue) {
		if(newValue==null) throw new RuntimeException("Cannot set option values to null");
		if(!newValue.equals(value)) {
			value = newValue;
			update();			
		}		
	}

	/**
	 * The option java type
	 * @return the type
	 */
	public Class<T> getType() {
		return type;
	}

	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Constructs a <code>String</code> with key attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "\n\t";
	    
	    StringBuilder retValue = new StringBuilder();
	    
	    retValue.append("AbstractOption [")
			.append(TAB)        .append("name = ").append(this.name)
			.append(TAB)        .append("key = ").append(this.key)
			.append(TAB)        .append("value = ").append(this.value)
			.append(TAB)        .append("defaultValue = ").append(this.defaultValue)
			.append(TAB)        .append("type = ").append(this.type)
			.append(TAB)        .append("description = ").append(this.description)
	        .append(" )");
	    
	    return retValue.toString();
	}
	
	
	
	
	/*
	 * Init: 
	 * 	Iterate Options.embeddedClasses that extend AbstractOption.
	 * 		Instantiate each.
	 * 		On load, call load. If result is null, set value to default and save pref.
	 *  
	 */
	

	
	
}
