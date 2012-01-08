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
package org.helios.patterns.queues;

import org.helios.patterns.queues.LongBitMaskFactory.LongBitMaskSequence;

/**
 * <p>Title: LongDiscriminator</p>
 * <p>Description: Defines a filter passed to the BitMaskFactory that selects which bit masks it wants.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.patterns.queues.LongDiscriminator</code></p>
 */

public interface LongDiscriminator {
	/**
	 * Determines if the passed value should be included in the generated bit set
	 * @param value The value to test
	 * @param order The sequence order of the item starting with 1 and ending with up to <code>LongDiscriminator.MAX_KEYS</code>.
	 * @return true to include, false to drop
	 */
	public boolean include(long value, int order);
	
	/** A built in discriminator for only even values */
	public static final LongDiscriminator EVENS = new LongDiscriminator() {
		public boolean include(long value, int order) {
			return LongBitMaskSequence.isEven(order);
		}
	};
	
	/** A built in discriminator for only odd values */
	public static final LongDiscriminator ODDS = new LongDiscriminator() {
		public boolean include(long value, int order) {
			return !LongBitMaskSequence.isEven(order);
		}
	};
	
}
