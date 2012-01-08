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
package org.helios.esper.engine;

import com.espertech.esper.client.StatementAwareUpdateListener;

/**
 * <p>Title: ListenerRegistration</p>
 * <p>Description: Defines a customized Helios esper update listener.</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.ListenerRegistration</code></p>
 */

public interface ListenerRegistration extends StatementAwareUpdateListener {
	/**
	 * EPL statements attached to this update listener
	 * @return an array of strings representing EPL statements.
	 */
	public String[] getTargetStatements();
	/**
	 * Returns the name of this update listener
	 * @return the name of this update listener
	 */
	public String getName();
}
