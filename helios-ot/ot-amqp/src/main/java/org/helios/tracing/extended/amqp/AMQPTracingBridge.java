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
package org.helios.tracing.extended.amqp;

import java.util.Collection;

import org.helios.tracing.bridge.AbstractStatefulTracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: AMQPTracingBridge</p>
 * <p>Description: Bridge impl. for AMQP</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.amqp.AMQPTracingBridge</code></p>
 */

public class AMQPTracingBridge extends AbstractStatefulTracingBridge {
	/** The AMQP Client */
	protected final AMQPClient amqpClient;
	
	/**
	 * Creates a new AMQPTracingBridge
	 * @param bufferSize
	 * @param frequency
	 */
	public AMQPTracingBridge(int bufferSize, long frequency) {
		super("AMQP", bufferSize, frequency, true);
		this.amqpClient = new AMQPClient();
	}
	
	/**
	 * Creates a new AMQPTracingBridge with the default bufferSize and frequency
	 */
	public AMQPTracingBridge() {
		this(-1, -1);
	}
	

	/**
	 * 
	 */
	@Override
	protected void doConnect() {
		if(!amqpClient.isConnected()) {
			try {
				amqpClient.connect();
			} catch (Exception e) {
				throw new RuntimeException("AMQPClient [" + amqpClient + "] Failed to connect", e);
			}
		}

	}

	/**
	 * 
	 */
	@Override
	protected void doDisconnect() {
		if(amqpClient.isConnected()) {
			amqpClient.stop();
		}
	}

	/**
	 * @param intervalTraces
	 */
	@Override
	public void submitIntervalTraces(Collection<IIntervalTrace> intervalTraces) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param traces
	 */
	@Override
	public void submitTraces(Collection<Trace> traces) {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 */
	@Override
	public void startReconnectPoll() {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 */
	@Override
	public void stopReconnectPoll() {
		// TODO Auto-generated method stub

	}

	/**
	 * @return
	 */
	@Override
	public String getEndPointName() {
		return "[AMQP]" + amqpClient.amqpHost + ":" + amqpClient.amqpPort + "/" + amqpClient.exchange;
	}

	/**
	 * @param flushedItems
	 */
	@Override
	public void flushTo(Collection<Trace> flushedItems) {
		submitTraces(flushedItems);

	}

}
