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
package org.helios.ot.helios;

import java.util.Map;


/**
 * <p>Title: ClientProtocolOperation</p>
 * <p>Description: Enumerates the client side operations recognized by the Helios Agent Protocol</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.ClientProtocolOperation</code></p>
 */

public enum ClientProtocolOperation implements IProtocolOperation {
	PING(false),
	CONNECT,
	TRACE;
	
	private static final Map<Integer, ClientProtocolOperation> ORDINAL2ENUM = new EnumOrdinalMapper().getOrdinalToEnumMap(ClientProtocolOperation.class);

	private ClientProtocolOperation(boolean async) {
		this.async = async;
	}
	
	private ClientProtocolOperation() {
		this(true);
	}
	
	private final boolean async;
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.helios.IProtocolOperation#getOperationCode()
	 */
	@Override
	public int getOperationCode() {
		return this.ordinal();
	}
	
	public static ClientProtocolOperation getOp(int ordinal) {
		ClientProtocolOperation cpo = ORDINAL2ENUM.get(ordinal);
		if(cpo==null) throw new IllegalArgumentException("The ordinal [" + ordinal + "] is not valid for Enum ClientProtocolOperation", new Throwable());
		return cpo;
	}
	
	public static boolean isOp(int ordinal) {
		return ORDINAL2ENUM.containsKey(ordinal);
	}

	/**
	 * Indicates if this op is asynchronous
	 * @return if this op is asynchronous
	 */
	public boolean isAsync() {
		return async;
	}
	
	
}
