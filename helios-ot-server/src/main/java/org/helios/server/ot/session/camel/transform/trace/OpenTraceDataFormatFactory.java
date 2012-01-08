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
package org.helios.server.ot.session.camel.transform.trace;

import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.camel.spi.DataFormat;
import org.helios.ot.trace.Trace;
import org.reflections.Reflections;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>Title: OpenTraceDataFormatFactory</p>
 * <p>Description: Creates a custom XStream data format</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.transform.trace.OpenTraceDataFormatFactory</code></p>
 */

public class OpenTraceDataFormatFactory {

	/**
	 * Creates a new {@link XStreamDataFormat} with annotations processed for the OT {@link Trace} package.
	 * @return a new {@link XStreamDataFormat}
	 */
	public static DataFormat getInstance() {
		Reflections ref =  new Reflections(Trace.class.getPackage().getName());
		XStream xstream = new XStream();
		xstream.processAnnotations(ref.getTypesAnnotatedWith(XStreamAlias.class).toArray(new Class[0]));
		return new XStreamDataFormat(xstream);
	}
	
}
