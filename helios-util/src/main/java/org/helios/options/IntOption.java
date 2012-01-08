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
 * <p>Title: IntOption</p>
 * <p>Description: Int typed option</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.options.IntOption</code></p>
 */

public class IntOption<T> extends AbstractOption<T> {

	/**
	 * The generic user preference load operation
	 * @return the user preference load
	 */
	public T load() {		
		Integer i = null;
		int val = prefs.getInt(key, Integer.MIN_VALUE);
		if(val==Integer.MIN_VALUE) {
			i = Integer.parseInt(defaultValue);
			value = (T)i;
			update();
		} else {
			i = val;
		}
		return (T)i;
	}
	
	/**
	 * Saves the option value to user preferences.
	 */
	public void update() {
		prefs.putInt(key, (Integer)value);
	}
	
	/**
	 * Sets the option's current value from the passed string
	 * @param value A string that will be converted into the correct value type 
	 */
	public void setValueAsString(String value) {
		if(value==null || value.length()<1) return;
		this.value = (T)new Integer(value.trim());
	}
}
