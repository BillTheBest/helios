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
package org.helios.ot.deltas;

/**
 * <p>Title: DeltaType</p>
 * <p>Description: Enumerates the different types of delta operations based on how the delta state is reset on a monotonic sequence breakage.
 * This occurs when the new value to be evaluated against state is less than the value in state. For example, consider a value of 760 is in state.
 * The new value to be evaluated against state is 200. The different options are as follows:<ul>
 * <li>REBASE:Resets the delta baseline. State is set to 200 and tracing is supressed.</li>
 * <li>ABSOLUTE:Returns the absolute difference between the value and state. Returns 560.</li>
 * <li>RELATIVE:Returns the signed difference between the value and state. Returns -560.</li>
 * </ul>
 * </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public enum DeltaType {
	
	/** Resets the delta baseline. */
	REBASE, 
	/** Returns the absolute difference between the value and state. */
	ABSOLUTE, 
	/** Returns the signed difference between the value and state. */
	RELATIVE;
	
	
}
