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
package org.helios.containers.sets;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.set.hash.TLongHashSet;

/**
 * <p>Title: ReadOnlyTLongHashSet</p>
 * <p>Description: A read only extension of <code>gnu.trove.TLongHashSet</code> that throws a runtime exception if a update operation is invoked.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.containers.sets.ReadOnlyTLongHashSet</code></p>
 */

public class ReadOnlyTLongHashSet extends TLongHashSet {
	public static final RuntimeException RT = new RuntimeException("ReadOnlyTLongHashSet is read only. Data cannot be modified");
	
	public ReadOnlyTLongHashSet(long[] values) {
		super(values.length);
		for(long l: values) {
			super.add(l);
		}
	}
	
	public int indexOf(long l) {
		return this.index(l);
	}
	
	
	/**
	 * @param val
	 * @return
	 * @see gnu.trove.TLongHashSet#add(long)
	 */
	public boolean add(long val) {
		throw RT;
	}

	/**
	 * @param arg0
	 * @return
	 * @see gnu.trove.TLongHashSet#addAll(long[])
	 */
	public boolean addAll(long[] arg0) {
		throw RT;
	}

	/**
	 * 
	 * @see gnu.trove.TLongHashSet#clear()
	 */
	public void clear() {
		throw RT;
	}


	/**
	 * @return
	 * @see gnu.trove.TLongHashSet#iterator()
	 */
	public TLongIterator iterator() {
		return this.iterator();
	}


	/**
	 * @param val
	 * @return
	 * @see gnu.trove.TLongHashSet#remove(long)
	 */
	public boolean remove(long val) {
		throw RT;
	}

	/**
	 * @param arg0
	 * @return
	 * @see gnu.trove.TLongHashSet#removeAll(long[])
	 */
	public boolean removeAll(long[] arg0) {
		throw RT;
	}

	/**
	 * @param arg0
	 * @return
	 * @see gnu.trove.TLongHashSet#retainAll(long[])
	 */
	public boolean retainAll(long[] arg0) {
		throw RT;
	}

}
