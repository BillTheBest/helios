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

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;


/**
 * <p>Title: HeliosEndpointUDPConnector</p>
 * <p>Description: UDP Netty Connector for {@link HeliosEndpoint}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.HeliosEndpointUDPConnector</code></p>
 */

public class HeliosEndpointUDPConnector extends AbstractEndpointConnector {
	/** The channel socket factory */
	protected final NioDatagramChannelFactory datagramChannelFactory;
	/** The datagram channel */
	protected DatagramChannel datagramChannel;
	/** The UDP bootstrap */
	protected ConnectionlessBootstrap bootstrap; 

	

	
	/** This connector's protocol */
	public static final Protocol PROTOCOL = Protocol.UDP;
	
	
	/**
	 * Creates a new HeliosEndpointUDPConnector
	 * @param endpoint The endpoint this connector was created for
	 */
	public HeliosEndpointUDPConnector(@SuppressWarnings("rawtypes") HeliosEndpoint endpoint) {
		super(endpoint);
	    // Configure the client.
		datagramChannelFactory = new NioDatagramChannelFactory(workerExecutor);
		bootstrap = new ConnectionlessBootstrap(datagramChannelFactory);
		// Set up the pipeline factory.
		channelPipelineFactory = new ChannelPipelineFactory() {
	          public ChannelPipeline getPipeline() throws Exception {
	              return Channels.pipeline(
	            		  instrumentation,
	                      new ObjectEncoder(),
	                      new ObjectDecoder());
	          }
		};	                     
		bootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));		
		
//	    bootstrap.setOption("broadcast", "true");
//		bootstrap.setPipelineFactory(channelPipelineFactory);
//		bootstrap.setOption("remoteAddress", new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
//		bootstrap.setOption("tcpNoDelay", true);
//		bootstrap.setOption("receiveBufferSize", 1048576);
//		bootstrap.setOption("sendBufferSize", 1048576);
//		bootstrap.setOption("keepAlive", true);
//		bootstrap.setOption("connectTimeoutMillis", 2000);
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.helios.AbstractEndpointConnector#connect()
	 */
	public void connect() {
		if(isConnected()) throw new IllegalStateException("This connector is already connected", new Throwable());
		datagramChannel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
		setConnected();
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.helios.AbstractEndpointConnector#disconnect()
	 */
	@Override
	public void disconnect() {			
		channelFuture.getChannel().getCloseFuture().addListener(new ChannelFutureListener(){
			public void operationComplete(ChannelFuture future) throws Exception {				
				datagramChannelFactory.releaseExternalResources();	
				setDisconnected(true);
			}
		});		 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.helios.AbstractEndpointConnector#write(java.lang.Object)
	 */
	public void write(Object obj) {
		datagramChannel.write(obj);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.helios.AbstractEndpointConnector#getProtocol()
	 */
	protected Protocol getProtocol() {
		return PROTOCOL;
	}


}
