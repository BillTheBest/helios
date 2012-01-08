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
package org.helios.tracing.extended.graphite;

import java.util.Collection;

import org.helios.helpers.EpochHelper;
import org.helios.helpers.StringHelper;
import org.helios.tracing.bridge.AbstractStatefulTracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;

/**
 * <p>Title: GraphiteBridge</p>
 * <p>Description: A bridge implementation for Graphite that wraps a graphite client.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.extended.graphite.GraphiteBridge</code></p>
 */

public class GraphiteBridge extends AbstractStatefulTracingBridge {
	/**  */
	private static final long serialVersionUID = 5257609083983268795L;
	/** The bridge data submitter  */
	protected GraphiteClient graphiteClient = null;
	
	/** The configuration prefix */
	public final String CONFIG_PREFIX = getClass().getPackage().getName() + ".client.";
	
	
	
	/**
	 * Creates a new GraphiteBridge
	 * @param bufferSize The maximum size of the trace bufffer before it is flushed. 0 will disable size triggered flushes.
	 * @param frequency The frequency on which the trace buffer is flushed (ms.) 0 will disable time triggered flushes.
	 */
	public GraphiteBridge(int bufferSize, long frequency) {
		super(GraphiteBridge.class.getSimpleName(), bufferSize, frequency, true);
		graphiteClient = GraphiteClient.getClient();
	}
	
	/**
	 * Creates a new GraphiteBridge targeted at the passed graphite server
	 * @param graphiteHost The graphite server host name or ip address
	 * @param graphitePort the graphite server port
	 * @param bufferSize The maximum size of the trace bufffer before it is flushed. 0 will disable size triggered flushes.
	 * @param frequency The frequency on which the trace buffer is flushed (ms.) 0 will disable time triggered flushes.
	 */
	public GraphiteBridge(String graphiteHost, int graphitePort, int bufferSize, long frequency) {
		super(GraphiteBridge.class.getSimpleName(), bufferSize, frequency, true);
		graphiteClient = GraphiteClient.newBuilder(graphiteHost, graphitePort)
			.setSizeTrigger(bufferSize)
			.setTimeTrigger(frequency)
			.setAutoConnect(true)
			.build();
	}
	
	
	/**
	 * Default everything constructor for the GraphiteBridge. 
	 */
	public GraphiteBridge() {
		super(GraphiteBridge.class.getSimpleName(), GraphiteClient.getClient().sizeTrigger, GraphiteClient.getClient().timeTrigger , true);
		graphiteClient = GraphiteClient.newBuilder().setAutoConnect(true).build();
	}
	
	/**
	 * Creates a new GraphiteBridge targeted at the passed graphite server
	 * @param graphiteHost The graphite server host name or ip address
	 * @param graphitePort the graphite server port
	 */
	public GraphiteBridge(String graphiteHost, int graphitePort) {
		super(GraphiteBridge.class.getSimpleName(), GraphiteClient.getClient(graphiteHost, graphitePort).sizeTrigger, GraphiteClient.getClient(graphiteHost, graphitePort).timeTrigger , true);				
		graphiteClient = GraphiteClient.newBuilder(graphiteHost, graphitePort)
		.setAutoConnect(true)
		.build();
		
		
	}

	/**
	 * Prompts the graphite client to connect.
	 */
	@Override
	protected void doConnect() {
		if(graphiteClient==null) throw new RuntimeException("GraphiteClient was null", new Throwable());
		graphiteClient.connect();		
	}

	/**
	 * Prompts the graphite client to disconnect.
	 */
	@Override
	protected void doDisconnect() {
		if(graphiteClient==null) throw new RuntimeException("GraphiteClient was null", new Throwable());
		graphiteClient.disconnect();		
	}


	/**
	 * 
	 */
	@Override
	public void startReconnectPoll() {
		
	}

	/**
	 * 
	 */
	@Override
	public void stopReconnectPoll() {
		
	}

	/**
	 * @param flushedItems
	 */
	@Override
	public void flushTo(Collection<Trace> flushedItems) {
		if(flushedItems!=null && !flushedItems.isEmpty()) {
			//StringBuilder b = StringHelper.getStringBuilder(flushedItems.size()*20);			
			for(Trace trace: flushedItems) {
				if(trace.getMetricId().getType().isNumber()) {				
					graphiteClient.submit(StringHelper.fastConcatAndDelim(" ", trace.getFQN(), trace.getValue().toString(), EpochHelper.getUnixTimeStr(), "\n" ));
				}
			}
		}
	}

	/**
	 * @param traces
	 */
	@Override
	public void submitTraces(Collection<Trace> traces) {
		if(traces!=null && !traces.isEmpty()) {
			//StringBuilder b = StringHelper.getStringBuilder(flushedItems.size()*20);			
			for(Trace trace: traces) {
				if(trace.getMetricId().getType().isNumber()) {				
					graphiteClient.submit(StringHelper.fastConcatAndDelim(" ", trace.getFQN().replace(" ", "").replace('(', '_').replace(")", "").replace('@', '_'), trace.getValue().toString(), EpochHelper.getUnixTimeStr(), "\n" ).replace('/', '.'));
					//graphiteClient.submit(StringHelper.fastConcatAndDelim(" ", trace.getFQN(), trace.getValue().toString(), EpochHelper.getUnixTimeStr(), "\n" ).replace('/', '.'));
				}
			}
		}		
	}

	/**
	 * @param intervalTraces
	 */
	@Override
	public void submitIntervalTraces(
			Collection<IIntervalTrace> intervalTraces) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Returns the graphite server identifier
	 * @return the graphite server identifier
	 */
	@Override
	public String getEndPointName() {
		return graphiteClient.getName();
	}



}
