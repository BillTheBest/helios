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
package org.helios.ot.agent.impl.netty.tcp;

import java.util.concurrent.Executor;

import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.threads.ExecutorBuilder;
import org.helios.ot.agent.impl.netty.AbstractNettyHeliosOTClient;
import org.helios.ot.agent.protocol.impl.ClientProtocolOperation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation;
import org.helios.ot.trace.Trace;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.SocketChannel;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

/**
 * <p>Title: TCPNettyHeliosOTClient</p>
 * <p>Description: The TCP protocol HeliosOTClient implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.impl.netty.tcp.TCPNettyHeliosOTClient</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class TCPNettyHeliosOTClient extends AbstractNettyHeliosOTClient {
	/**  */
	private static final long serialVersionUID = 4505329393495223651L;
	/** The channel socket factory */
	protected NioClientSocketChannelFactory socketChannelFactory;
	/** The channel socket  */
	protected SocketChannel socketChannel;	
	/** The boss thread pool */
	protected Executor bossExecutor;

	/** The default listening port */
	public static final int DEFAULT_LISTENING_PORT = 9428;
	/**
	 * Creates a new TCPNettyHeliosOTClient
	 */
	public TCPNettyHeliosOTClient() {

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.AbstractNettyHeliosOTClient#initChannelFactory()
	 */
	@Override
	protected void initChannelFactory() {
		bossExecutor = ExecutorBuilder.newBuilder()
				.setCoreThreads(1)
				.setCoreThreadTimeout(false)
				.setDaemonThreads(true)
				.setExecutorType(true)
				.setFairSubmissionQueue(false)
				.setKeepAliveTime(15000)
				.setMaxThreads(3)
				.setJmxDomains(JMXHelper.getRuntimeHeliosMBeanServer().getDefaultDomain())
				//.setPoolObjectName(new StringBuilder("org.helios.endpoints:name=").append(getClass().getSimpleName()).append(",service=ThreadPool,type=Boss,protocol=").append(getProtocol().name()))
				.setPrestartThreads(1)
				.setTaskQueueSize(100)
				.setTerminationTime(5000)
				.setThreadGroupName(getClass().getSimpleName() + "BossThreadGroup")
				//.setUncaughtExceptionHandler(endpoint)
				.build();
	    // Configure the client.
		socketChannelFactory = new NioClientSocketChannelFactory(bossExecutor, workerExecutor);
		channelFactory = socketChannelFactory;
		bootstrap = new ClientBootstrap(socketChannelFactory);
		bootstrap.setOptions(bootstrapOptions);
		// Set up the pipeline factory.
		channelPipelineFactory = new ChannelPipelineFactory() {
	          public ChannelPipeline getPipeline() throws Exception {
	              return Channels.pipeline(
	            		  instrumentation, 
	                      new ObjectEncoder(),
	                      new ObjectDecoder(),
	                      synchronousRequestHandler
	                      );
	          }
		};	                     
		bootstrap.setPipelineFactory(channelPipelineFactory);
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.impl.netty.AbstractNettyHeliosOTClient#getDefaultTargetPort()
	 */
	protected int getDefaultTargetPort() {
		return DEFAULT_LISTENING_PORT;
	}
	/**
	 * Returns the connection channel for this client instance
	 * @return the connection channel for this client instance
	 */
	public Channel getConnectionChannel() {
		return socketChannel;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClient#getProtocol()
	 */
	@Override
	@JMXAttribute(name="Protocol", description="The name of the remoting protocol implemented by this client", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getProtocol() {
		return "tcp";
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.AbstractHeliosOTClientImpl#doSubmitTraces(org.helios.ot.trace.Trace[])
	 */
	@Override
	protected void doSubmitTraces(Trace[] traces) {
		socketChannel.write(HeliosProtocolInvocation.newInstance(ClientProtocolOperation.TRACE, traces)).addListener(sendListener);
	}



	@Override
	protected ChannelFactory getChannelFactory() {
		// TODO Auto-generated method stub
		return null;
	}


}
