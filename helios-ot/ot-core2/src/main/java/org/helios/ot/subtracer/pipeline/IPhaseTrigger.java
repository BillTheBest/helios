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
package org.helios.ot.subtracer.pipeline;

import org.helios.ot.trace.Trace;

/**
 * <p>Title: IPhaseTrigger</p>
 * <p>Description: A procedure fired when an {@link org.helios.ot.trace.types.ITraceValue} is passed through a specific phase of the OpenTrace pipeline. </p>
 * <p><b><font color='red'>NOTE:</font></b> Phase triggers are currently considered a testing construct and may introduce additional latency in the processing pipeline.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.subtracer.pipeline.IPhaseTrigger</code></p>
 */

public interface IPhaseTrigger {
	/**
	 * A callback from the named phase of the OpenTrace pipeline.
	 * @param phaseName The name of the phase
	 * @param trace The trace containing the phase trigger
	 */
	public void phaseTrigger(String phaseName, Trace trace);
}
