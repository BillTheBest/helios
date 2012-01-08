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
package org.helios.tracing.extended.logging;

import java.util.Date;

import org.apache.log4j.or.ObjectRenderer;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: TraceRenderer</p>
 * <p>Description: A log4j custom object renderer for logging formatted OpenTrace traces.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class TraceRenderer implements ObjectRenderer {

	/**
	 * Render the object passed as parameter as a String.
	 * @param obj The object to render.
	 * @return The string rendering of the passed object.
	 * @see org.apache.log4j.or.ObjectRenderer#doRender(java.lang.Object)
	 */
	public String doRender(Object obj) {
		if(obj instanceof Trace) {
			Trace trace = (Trace)obj;
			StringBuilder b = new StringBuilder(trace.getFQN());
			b.append("[").append(new Date(trace.getTimeStamp())).append("]");
			b.append("\n\tValue:").append(trace.getValue());			
			b.append("\n\tType:").append(trace.getMetricType().name());
			b.append("\n\tMod:").append(trace.getMod());
			b.append("\n\tSerial:").append(trace.getSerial());
			b.append("\n\tTemporal:").append(trace.isTemporal());			
			return b.toString();
		} else {
			return obj.toString();
		}
	}

}
