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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: NameValuePair</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class NameValuePair<K, V> {
	protected K name = null;
	protected V value = null;
	
	/**
	 * @param name
	 * @param value
	 */
	public NameValuePair(K name, V value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * @param name
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static NameValuePair getInstance(Object name, Object value) {
		return new NameValuePair(name, value);
	}
	
	/**
	 * @param pairs
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map map(NameValuePair...pairs) {
		if(pairs!=null && pairs.length > 0) {
			Map nvpMap = new HashMap(pairs.length);
			for(NameValuePair nvp: pairs) {
				nvpMap.put(nvp.name, nvp.value);
			}
			return nvpMap;
		} else {
			return Collections.EMPTY_MAP;
		}
	}
	
	/**
	 * @return the name
	 */
	public K getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(K name) {
		this.name = name;
	}
	/**
	 * @return the value
	 */
	public V getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(V value) {
		this.value = value;
	}
}
