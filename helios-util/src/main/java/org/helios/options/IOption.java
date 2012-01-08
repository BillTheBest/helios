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

/**
 * <p>Title: IOption</p>
 * <p>Description: Defines basic typed option operations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.options.IOption</code></p>
 */

public interface IOption<T> {
	/**
	 * Returns the Option's value as an Object from the user preferences store.
	 * @return the value of the option from the user preferences store or null if it was not stored.
	 */
	public Object load();
	
	/**
	 * Saves the option value to user preferences.
	 */
	public void update();	
	
	/**
	 * Returns the option name
	 * @return the option name
	 */
	public String getName();
	
	/**
	 * Returns the option user-prefs key
	 * @return the option user-prefs key
	 */
	public String getKey();
	
	/**
	 * Returns the option's current value
	 * @return the option's current value
	 */
	public T getValue();
	
	/**
	 * Sets the option's current value
	 * @param value the option value to set to
	 */
	public void setValue(T value);
	
	/**
	 * Sets the option's current value from the passed string
	 * @param value A string that will be converted into the correct value type 
	 */
	public void setValueAsString(String value);
	
	/**
	 * Returns the option's description
	 * @return the option's description
	 */
	public String getDescription();
	
	/**
	 * Returns the option's default value
	 * @return the option's default value
	 */
	public String getDefaultValue();
	
	/**
	 * Returns the option's java type
	 * @return the option's java type
	 */
	public Class<T> getType();
	
	/**
	 * Displays an option tersely
	 * @return a tersely displayed option
	 */
	public String display();
	
	
}
