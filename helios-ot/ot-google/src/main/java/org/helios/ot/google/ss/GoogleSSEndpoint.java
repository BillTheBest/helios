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
package org.helios.ot.google.ss;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.helios.ot.endpoint.AbstractEndpoint;
import org.helios.ot.endpoint.EndpointConnectException;
import org.helios.ot.endpoint.EndpointTraceException;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: GoogleSSEndpoint</p>
 * <p>Description: OpenTrace end point that applies formatted traces to a spreadhseet in Google docs.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.google.ss.GoogleSSEndpoint</code></p>
 */

public class GoogleSSEndpoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T> {
	/**  */
	private static final long serialVersionUID = -3368267964248341823L;
	/** The google data user name */
	protected String gUserName = null;
	/** The google data password */
	protected String gPassword = null;
	/** The spreadhseet headers and trace field names that are mapped into the spreadsheet */
	protected final List<String> traceFieldNames = new CopyOnWriteArrayList<String>();
	
	/** Constant mapping to trace field */
	public static final String TRACE_TS="timestamp";
	/** Constant mapping to trace field */
	public static final String TRACE_DATE="date";
	/** Constant mapping to trace field */
	public static final String TRACE_SVALUE="svalue";
	/** Constant mapping to trace field */
	public static final String TRACE_VALUE="value";
	/** Constant mapping to trace field */
	public static final String TRACE_TEMPORAL="temporal";
	/** Constant mapping to trace field */
	public static final String TRACE_URGENT="urgent";
	/** Constant mapping to trace field */
	public static final String TRACE_MODEL="model";
	/** Constant mapping to trace field */
	public static final String TRACE_FQN="fqn";
	/** Constant mapping to trace field */
	public static final String TRACE_POINT="point";
	/** Constant mapping to trace field */
	public static final String TRACE_FULLNAME="fullname";
	/** Constant mapping to trace field */
	public static final String TRACE_NAMESPACE="namespace";
	/** Constant mapping to trace field */
	public static final String TRACE_LNAMESPACE="lnamespace";
	/** Constant mapping to trace field */
	public static final String TRACE_APP_ID="agent";
	/** Constant mapping to trace field */
	public static final String TRACE_HOST="host";
	/** Constant mapping to trace field */
	public static final String TRACE_TYPE="type";
	/** Constant mapping to trace field */
	public static final String TRACE_TYPE_NAME="typename";
	/** Constant mapping to trace field */
	public static final String TRACE_TYPE_CODE="typecode";
	
	@Override
	public org.helios.ot.endpoint.AbstractEndpoint.Builder newBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void connectImpl() throws EndpointConnectException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean processTracesImpl(TraceCollection<T> traceCollection)
			throws EndpointConnectException, EndpointTraceException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void disconnectImpl() {
		// TODO Auto-generated method stub
		
	}

}




//import org.helios.ot.trace.*;
//
//Trace.class.getDeclaredFields().each() {
//    if(it.getName().startsWith("TRACE_")) {
//        println "\t/** Constant mapping to trace field */\n\tpublic static final String ${it.getName()}=\"${it.get(null)}\";";
//    }
//}
//
//MetricId.class.getDeclaredFields().each() {
//    if(it.getName().startsWith("TRACE_")) {
//        println "\t/** Constant mapping to trace field */\n\tpublic static final String ${it.getName()}=\"${it.get(null)}\";";
//    }
//}
//
