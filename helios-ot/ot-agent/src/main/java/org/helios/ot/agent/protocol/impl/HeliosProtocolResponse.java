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
package org.helios.ot.agent.protocol.impl;

import java.io.Serializable;

/**
 * <p>Title: HeliosProtocolResponse</p>
 * <p>Description: A container class for a helios invocation response</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.HeliosProtocolResponse</code></p>
 */

public class HeliosProtocolResponse implements Serializable {
	/**  */
	private static final long serialVersionUID = -541996939037054638L;
	/** The request ID of the request that this object is in response to */
	protected long requestSerial = -1;
	/** The response value */
	protected Object payload = null;
	/** The operation indicator code */
	protected final int opCode;
	
	
	/**
	 * Creates a new HeliosProtocolResponse
	 * @param opCode The operation code of the invocationthat this is the response for
	 * @param requestSerial The request ID of the request that this object is in response to
	 * @param payload The response value
	 * @return the new HeliosProtocolResponse
	 */
	public static HeliosProtocolResponse newInstance(int opCode, long requestSerial, Object payload) {
		return new HeliosProtocolResponse(opCode, requestSerial, payload);
	}
	
	/**
	 * Creates a new HeliosProtocolResponse
	 * @param opCode The operation code of the invocationthat this is the response for
	 * @param requestSerial The request ID of the request that this object is in response to
	 * @param payload The response value
	 * @return the new HeliosProtocolResponse
	 */
	public static HeliosProtocolResponse newInstance(ClientProtocolOperation opCode, long requestSerial, Object payload) {
		return new HeliosProtocolResponse(opCode.getOperationCode(), requestSerial, payload);
	}
	
	
	
	/**
	 * Creates a new HeliosProtocolResponse
	 * @param opCode The operation code of the invocationthat this is the response for
	 * @param requestSerial The request ID of the request that this object is in response to
	 * @param payload The response value
	 */
	public HeliosProtocolResponse(int opCode, long requestSerial, Object payload) {
		this.requestSerial = requestSerial;
		this.payload = payload;
		this.opCode = opCode;
	}
	
	/**
	 * Returns the operation indicator code
	 * @return the operation indicator code
	 */
	public int getOp() {
		return opCode;
	}
	

	/**
	 * Returns the requestID
	 * @return the requestSerial
	 */
	public long getRequestSerial() {
		return requestSerial;
	}

	/**
	 * Returns the response value
	 * @return the payload
	 */
	public Object getPayload() {
		return payload;
	}


	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("HeliosProtocolResponse [")
	        .append(TAB).append("requestSerial:").append(this.requestSerial)
	        .append(TAB).append("payload:").append(this.payload)
	        .append("\n]");    
	    return retValue.toString();
	}
	
	
}
