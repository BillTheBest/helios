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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.ot.agent.Configuration;


/**
 * <p>Title: HeliosProtocolInvocation</p>
 * <p>Description: A wrapped invocation to be invoked across the Helios Protocol layer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.HeliosProtocolInvocation</code></p>
 */
public class HeliosProtocolInvocation implements Serializable {
	/**  */
	private static final long serialVersionUID = 958761629660003733L;
	/** The operation indicator code */
	protected int opCode;
	/** The invocation payload */
	protected Object payload;
	/** The request serial number */
	private long requestId = -1;
	/** The timestamp this invocation was created */
	public final long timestamp = System.currentTimeMillis();
	/** A transient context map */
	protected final transient Map<String, Object> context = new HashMap<String, Object>();
	/** A latch for waiting on synch op responses */
	protected transient CountDownLatch synchOpLatch = null;
	/** The synch op response */
	protected transient Object synchOpResponse = null;
	
	protected transient long startTimeNanos = -1;
	protected transient long elapsedTimeNanos = -1;
	
	
	/** The header name where the op code is stored */
	public static final String HPI_HEADER = HeliosProtocolInvocation.class.getSimpleName();
	/** The request serial number factory */
	private static final AtomicLong requestSerial = new AtomicLong(0L);

	/** The header name for an optional ms timeout override on an invocation */
	public static final String TIMEOUT_HEADER = "invocation.timeout";
	/** The header name to indicate payload gzip compression */
	public static final String COMPRESSION_HEADER = "invocation.payload.compression";

	
	/**
	 * Creates a new HeliosProtocolInvocation
	 * @param op The operation indicator code
	 * @param payload The invocation payload
	 * @return a new HeliosProtocolInvocation
	 */
	public static HeliosProtocolInvocation newInstance(int op, Object payload) {
		return new HeliosProtocolInvocation(op, requestSerial.incrementAndGet(), payload);
		
	}
	
	/**
	 * Creates a new HeliosProtocolInvocation
	 * @param op The operation indicator enum
	 * @param payload The invocation payload
	 * @return a new HeliosProtocolInvocation
	 */
	public static HeliosProtocolInvocation newInstance(Enum<?> op, Object payload) {
		return new HeliosProtocolInvocation(op.ordinal(), requestSerial.incrementAndGet(), payload);
	}
	
	/**
	 * Creates a new HeliosProtocolInvocation
	 */
	public HeliosProtocolInvocation() {
		super();
	}
	
	/**
	 * Creates a new HeliosProtocolInvocation
	 * @param op The operation indicator code
	 * @param requestId The request serial number
	 * @param payload The invocation payload
	 */
	public HeliosProtocolInvocation(int op, long requestId, Object payload) {
		this.opCode = op;
		this.payload = payload;
		this.requestId = requestId;
		if(!ClientProtocolOperation.getOp(opCode).isAsync()) {
			synchOpLatch = new CountDownLatch(1);
		}
	}
	
	/**
	 * Indicates if this is an asynchronous operation
	 * @return true if this is an asynchronous operation, false otherwise
	 */
	public boolean isAsync() {
		return ClientProtocolOperation.getOp(opCode).isAsync();
	}

	/**
	 * Returns the operation indicator code
	 * @return the operation indicator code
	 */
	public int getOp() {
		return opCode;
	}

	/**
	 * Sets the operation indicator code
	 * @param op the operation indicator code
	 */
	public void setOp(int op) {
		this.opCode = op;
	}

	/**
	 * Returns the invocation payload
	 * @return the invocation payload
	 */
	public Object getPayload() {
		return payload;
	}

	/**
	 * Sets the invocation payload
	 * @param payload the payload to set
	 */
	public void setPayload(Object payload) {
		this.payload = payload;
	}
	
	/**
	 * Adds a context item to this invocation
	 * @param key The context item key
	 * @param value The context item value
	 * @return this invocation
	 */
	public HeliosProtocolInvocation context(String key, Object value) {
		if(key==null) throw new IllegalArgumentException("The passed key was null", new Throwable());
		if(value==null) throw new IllegalArgumentException("The passed value for key [" + key + "] was null", new Throwable());
		context.put(key, value);
		return this;
	}
	
	/**
	 * Indicates if the context contains an itemed with the passed key
	 * @param key The key to check for 
	 * @return true if the named context item exists, false if the key was null or is not in the context
	 */
	public boolean hasContextItem(String key) {
		if(key==null) return false;
		return context.containsKey(key);		
	}
	
	/**
	 * Returns the named context item
	 * @param key The context item key
	 * @return The context item value or null if the key was null or is not in the context
	 */
	public Object getContextItem(String key) {
		if(key==null) return null;
		return context.get(key);
	}
	
	
	/**
	 * Waits on the response to a synchronous operation
	 * @return the operation response 
	 * @throws InterruptedException
	 */
	public Object getSynchronousResponse() throws InterruptedException {		
		if(synchOpLatch==null) throw new IllegalStateException("No Synchronous Operation Latch was registered", new Throwable());
		synchOpLatch.await();
		if(synchOpResponse!=null && synchOpResponse instanceof Throwable) {
			throw new RuntimeException((Throwable)synchOpResponse);
		}
		return synchOpResponse;
	}
	
	
	

	
	/**
	 * Callback point for the synch request handler to set the return value
	 * @param result The result of the synch op 
	 */
	public void setSynchResponse(Object result) {
		this.synchOpResponse = result;
		if(synchOpLatch!=null) {
			synchOpLatch.countDown();
		}		
	}
	

	/**
	 * Returns the request serial number
	 * @return the request serial number
	 */
	public long getRequestId() {
		return requestId;
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
	    StringBuilder retValue = new StringBuilder("HeliosProtocolInvocation [")
	        .append(TAB).append("opCode:").append(ClientProtocolOperation.getOp(opCode))	        
	        .append(TAB).append("requestId:").append(this.requestId)
	        .append(TAB).append("waiting:").append(synchOpLatch==null ? false : synchOpLatch.getCount()>0)
	        .append("\n]");    
	    return retValue.toString();
	}

	/**
	 * Returns the start time in ns.
	 * @return the startTimeNanos
	 */
	public long getStartTimeNanos() {
		return startTimeNanos;
	}

	/**
	 * Opens the start time
	 */
	public void setStartTimeNanos() {
		this.startTimeNanos = System.nanoTime();
	}

	/**
	 * Returns the synch invocation elapsed time in ns.
	 * @return the elapsedTimeNanos
	 */
	public long getElapsedTimeNanos() {
		return elapsedTimeNanos;
	}

	/**
	 * Closes the elapsed time
	 */
	public void setElapsedTimeNanos() {
		this.elapsedTimeNanos = System.nanoTime() - startTimeNanos;
	}

	
}
