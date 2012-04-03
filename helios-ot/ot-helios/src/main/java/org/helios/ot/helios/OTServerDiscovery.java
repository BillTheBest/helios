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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

/**
 * <p>Title: OTServerDiscovery</p>
 * <p>Description: Multicast discovery agent for Helios OT Endpoint</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.OTServerDiscovery</code></p>
 */
public class OTServerDiscovery implements ChannelUpstreamHandler, ChannelDownstreamHandler {
	/** The response latch to wait on */
	protected CountDownLatch responseLatch = null;
	/** The multicast network to broadcast on */
	protected String network = "224.9.3.7";
	/** The multicast port to broadcast on */
	protected int port = 1836;
	/** The worker thread pool */
	protected Executor workerExecutor;
	/** The pipeline factory */
	protected ChannelPipelineFactory channelPipelineFactory;
	/** The connection's channel future */
	protected ChannelFuture channelFuture;
	/** The client bootstrap */
	protected ConnectionlessBootstrap bootstrap; 
	/** The channel factory */
	protected OioDatagramChannelFactory datagramChannelFactory;
	
	protected long responseWaitTime = 5000;
	protected long requestCount = 5;
	protected long timeBetweenRequests = 750;
	protected final AtomicBoolean responded = new AtomicBoolean(false);
	protected final Logger log = Logger.getLogger(getClass());
	protected DatagramChannel datagramChannel = null;
	
	
	/**
	 * Tests the OTServerDiscovery component
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger LOG = Logger.getLogger(OTServerDiscovery.class);
		LOG.info("OTServerDiscovery Test");
		OTServerDiscovery otsd = new OTServerDiscovery();
		boolean b = otsd.start();
		LOG.info("Responded:" + b);
		otsd.stop();
	}
	
	
	public void stop() {
		try {
			if(datagramChannel!=null) datagramChannel.close();
			datagramChannelFactory.releaseExternalResources();
			log.info("Closed Discovery Agent");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * Starts the discovery request
	 */
	public boolean start() {
		try {
			
			String MCAST_GROUP_IP = "224.9.3.7";
			int MCAST_GROUP_PORT = port;
			String BIND_ADDRESS = InetAddress.getLocalHost().getHostAddress();
			
			
			
			workerExecutor = Executors.newCachedThreadPool();
		    // Configure the client.
			datagramChannelFactory = new OioDatagramChannelFactory(workerExecutor);
			bootstrap = new ConnectionlessBootstrap(datagramChannelFactory);
			// Set up the pipeline factory.
			final ChannelUpstreamHandler uhandler = this;
			channelPipelineFactory = new ChannelPipelineFactory() {
		          public ChannelPipeline getPipeline() throws Exception {
		              return Channels.pipeline(
		                      new StringEncoder(),
		                      new StringDecoder(),
		                      uhandler);	          
		              }
			};	                     
			bootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));		
			bootstrap.setPipelineFactory(channelPipelineFactory);
//			bootstrap.setOption("remoteAddress", new InetSocketAddress(network, port));
		    bootstrap.setOption("broadcast", "true");
		    
		    
		    InetAddress multicastAddress = InetAddress.getByName(MCAST_GROUP_IP);	        
		    datagramChannel = (DatagramChannel) bootstrap.bind(new InetSocketAddress("localhost", 6789));
	        datagramChannel.joinGroup(multicastAddress); 
//	        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(BIND_ADDRESS));
//	        datagramChannel.joinGroup(multicastAddress, networkInterface);		    
		    
		    
		    
		    responseLatch = new CountDownLatch(1);
		    for(int i = 0; i < requestCount; i++) {
		    	datagramChannel.write("Hello World");
		    	log.info("Issued Request #" + (i+1));
		    	Thread.currentThread().join(timeBetweenRequests);
		    	if(responded.get()) break;		    	
		    }
		    responseLatch.await(responseWaitTime, TimeUnit.MILLISECONDS);
		    return responded.get();
		} catch (Exception e) {
			log.error("Failed to issue request", e);
			return false;
		}
		
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		//log.info("Upstream Event:" + e);
		if(e instanceof MessageEvent) {
			String response = ((MessageEvent)e).getMessage().toString();
			log.info("Upstream: [" + response + "]" );
//			responded.set(true);
//			responseLatch.countDown();
		}
		ctx.sendUpstream(e);
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelDownstreamHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		//log.info("DownStream Event:" + e);
		if(e instanceof MessageEvent) {
			String response = ((MessageEvent)e).getMessage().toString();
			log.info("Upstream: [" + response + "]" );
		}		
		ctx.sendDownstream(e);
	}



}
