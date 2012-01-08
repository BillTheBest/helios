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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: SequenceCache</p>
 * <p>Description: A serializable cache of Sequences keyed by sequence name. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.sequence.SequenceCache</code></p>
 */

public class SequenceCache implements Serializable {
	/** A map of sequences keyed by sequence name */
	protected final Map<String, Sequence> sequences = new ConcurrentHashMap<String, Sequence>();



	/**
	 * 
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		sequences.clear();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object arg0) {
		return sequences.containsKey(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object arg0) {
		return sequences.containsValue(arg0);
	}

	/**
	 * @return
	 * @see java.util.Map#entrySet()
	 */
	public Set<Entry<String, Sequence>> entrySet() {
		return sequences.entrySet();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Map#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {
		return sequences.equals(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Sequence get(Object arg0) {
		return sequences.get(arg0);
	}

	/**
	 * @return
	 * @see java.util.Map#hashCode()
	 */
	public int hashCode() {
		return sequences.hashCode();
	}

	/**
	 * @return
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return sequences.isEmpty();
	}

	/**
	 * @return
	 * @see java.util.Map#keySet()
	 */
	public Set<String> keySet() {
		return sequences.keySet();
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Sequence put(String arg0, Sequence arg1) {
		return sequences.put(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends String, ? extends Sequence> arg0) {
		sequences.putAll(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Sequence remove(Object arg0) {
		return sequences.remove(arg0);
	}

	/**
	 * @return
	 * @see java.util.Map#size()
	 */
	public int size() {
		return sequences.size();
	}

	/**
	 * @return
	 * @see java.util.Map#values()
	 */
	public Collection<Sequence> values() {
		return sequences.values();
	}

}
