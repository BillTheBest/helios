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

/**
 * <p>Title: ReadOnlyTLongIterator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.containers.sets.ReadOnlyTLongIterator</code></p>
 */

public class ReadOnlyTLongIterator implements TLongIterator {
	protected final TLongIterator inner;
	/**
	 * Creates a new ReadOnlyTLongIterator
	 * @param hash
	 */
	public ReadOnlyTLongIterator(TLongIterator inner) {
		this.inner = inner;
	}
	
	
	
	public void remove() {
		throw ReadOnlyTLongHashSet.RT;
	}



	/**
	 * @param arg0
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {
		return inner.equals(arg0);
	}



	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return inner.hashCode();
	}



	/**
	 * @return
	 * @see gnu.trove.TIterator#hasNext()
	 */
	public boolean hasNext() {
		return inner.hasNext();
	}



	/**
	 * @return
	 * @see gnu.trove.TLongIterator#next()
	 */
	public long next() {
		return inner.next();
	}



	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return inner.toString();
	}
	
	
	

}
