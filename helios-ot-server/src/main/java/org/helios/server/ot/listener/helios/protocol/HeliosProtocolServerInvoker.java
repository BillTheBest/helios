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
package org.helios.server.ot.listener.helios.protocol;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.StartupListener;
import org.apache.camel.component.netty.NettyConstants;
import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.logging.LoggerControl;
import org.helios.ot.agent.protocol.impl.ClientProtocolOperation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolResponse;
import org.helios.ot.trace.Trace;
import org.helios.server.ot.listener.helios.protocol.jmx.ChannelGroupJMXWrapper;
import org.helios.server.ot.listener.helios.protocol.jmx.ChannelMXBean;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * <p>Title: HeliosProtocolServerInvoker</p>
 * <p>Description: A camel processor that handles incoming helios protocol invocations and delegates them to spring beans and/or camel endpoints</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.listener.helios.protocol.HeliosProtocolServerInvoker</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class HeliosProtocolServerInvoker extends ManagedObjectDynamicMBean implements Processor, StartupListener, CamelContextAware {
	/** A cache of endpoints */
	protected final Map<String, Endpoint> endpoints = new ConcurrentHashMap<String, Endpoint>();
	/** The camel context */
	protected CamelContext camelContext;
	/** Instance logger */
	protected Logger log = Logger.getLogger(getClass());
	/** Logger control */
	protected final LoggerControl loggerControl = new LoggerControl(log);
	/** The camel exchange producer template */
	protected ProducerTemplate producer = null;
	/** The camel endpoint for sending trace submissions */
	protected Endpoint otAgentEndpoint = null;
	/** The channel group to manage channel connections for all connected remote agents */
	protected final ChannelGroup channelGroup = new DefaultChannelGroup(getClass().getSimpleName());
	
	/** An MXBean wrapper for the channel group */
	protected final ChannelGroupJMXWrapper channelGroupMx = new ChannelGroupJMXWrapper(channelGroup);
	
	/** The ID of the endpoint that all OT agent traces are forwarded to  */
	public static final String OT_AGENT_ENDPOINT = "OTAgentEndpoint";
	/** The ID of the route that contains the  OT agent trace endpoint  */
	public static final String OT_AGENT_ROUTE = "RemoteTraceSubmissionHandler";
	/** The exchange header key for the invocation return value */
	public static final String OT_AGENT_RESPONSE = "HeliosInvocationResponse";
	
	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
	 */
	@Override
	public void process(Exchange exchange) throws Exception {
		Message msg = exchange.getIn();
		HeliosProtocolInvocation hpi = msg.getBody(HeliosProtocolInvocation.class);
		
		msg.setHeader(HeliosProtocolInvocation.HPI_HEADER, hpi.getOp());
		msg.setBody(hpi.getPayload());
		final SocketAddress remoteAddress = exchange.getIn().getHeader(NettyConstants.NETTY_REMOTE_ADDRESS, SocketAddress.class);
		final long requestId = hpi.getRequestId();
		final ChannelHandlerContext channelHandlerContext = exchange.getIn().getHeader(NettyConstants.NETTY_CHANNEL_HANDLER_CONTEXT, ChannelHandlerContext.class);
		// These ops we fast forward.
		if(hpi.getOp()==ClientProtocolOperation.PING.ordinal()) {
			if(log.isDebugEnabled()) log.debug("Processing PING from [" + remoteAddress + "]");
			exchange.getOut().setBody(HeliosProtocolResponse.newInstance(ClientProtocolOperation.PING, requestId, hpi.getPayload()));			
		} else if(hpi.getOp()==ClientProtocolOperation.TRACE.ordinal()) {
			//producer.send(otAgentEndpoint, exchange);
			if(log.isDebugEnabled()) log.debug("Processing TRACE from [" + remoteAddress + "]");
			producer.asyncSend(otAgentEndpoint, exchange.copy());		
			int traceCount = ((Trace[])hpi.getPayload()).length;
			exchange.getOut().setBody(traceCount);
		} else if(hpi.getOp()==ClientProtocolOperation.CONNECT.ordinal()) {			
			if(log.isDebugEnabled()) log.debug("Processing CONNECT from [" + remoteAddress + "]");
			Channel channel = channelHandlerContext.getChannel();
			final int channelId = channel.getId();
			if(channelGroup.find(channelId)==null) {
				// This is a new channel
				channelGroup.add(channel);
			} else {
				// This channel has already registered. Just return the session ID.
			}
			
			
			
			String[] agentId = (String[])hpi.getPayload();
			StringBuilder b = new StringBuilder("\n\tAgent Connection:");
			b.append("\n\t\tHost:").append(agentId[0]);
			b.append("\n\t\tAgent:").append(agentId[1]);
			b.append("\n\t\tRemote Address:").append(remoteAddress);
			b.append("\n\t\tChannel:").append(channel);
			b.append("\n\t\tChannel ID:").append(channel.getId());
			b.append("\n\t\tChannelHandlerCtx:").append(channelHandlerContext.getName());
			b.append("\n\t\tInitiating Route:").append(exchange.getFromRouteId());
			b.append("\n\t\tInitiating Endpoint Key:").append(exchange.getFromEndpoint().getEndpointKey());
			b.append("\n\t\tInitiating Endpoint URI:").append(exchange.getFromEndpoint().getEndpointUri());

			
			log.info(b);
			exchange.getOut().setBody(HeliosProtocolResponse.newInstance(ClientProtocolOperation.CONNECT, requestId, (exchange.getFromRouteId() + ":" + channelId)));
		}
	}
	
	/**
	 * Returns the JMX represented channel group
	 * @return the channel group
	 */
	@JMXAttribute(name="ChannelGroup", description="The Netty Channel Group", mutability=AttributeMutabilityOption.READ_ONLY)
	public ChannelMXBean[] getChannels() {
		return this.channelGroupMx.getChannels();
	}


	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.StartupListener#onCamelContextStarted(org.apache.camel.CamelContext, boolean)
	 */
	@Override
	public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) throws Exception {
		this.camelContext = context;
		producer = context.createProducerTemplate();
		try {
			otAgentEndpoint = context.getRoute(OT_AGENT_ROUTE).getEndpoint();
			if(otAgentEndpoint==null) {
				throw new Exception();
			}
		} catch (Exception e) {
			throw new Exception("Failed to acquire the OT_AGENT_ENDPOINT. Expected to be in Route [" + OT_AGENT_ROUTE + "]", e);
		}
		reflectObject(this);
		reflectObject(loggerControl);
		JMXHelper.getRuntimeHeliosMBeanServer().registerMBean(this, JMXHelper.objectName("org.helios.netty:service=NettyServer"));
	}


	/**
	 * Returns 
	 * @return the camelContext
	 */
	public CamelContext getCamelContext() {
		return camelContext;
	}


	/**
	 * Sets 
	 * @param camelContext the camelContext to set
	 */
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
		try {
			this.camelContext.addStartupListener(this);
		} catch (Exception e) {
			throw new RuntimeException("Failed to add [" + getClass().getSimpleName() + "] as a startup listener on Camel Context [" + camelContext.getName() + "]", e);
		}
	}


	//@Converter
	
//    exchange.getIn().setHeader(NettyConstants.NETTY_CHANNEL_HANDLER_CONTEXT, ctx);
//    exchange.getIn().setHeader(NettyConstants.NETTY_MESSAGE_EVENT, messageEvent);
//    exchange.getIn().setHeader(NettyConstants.NETTY_REMOTE_ADDRESS, messageEvent.getRemoteAddress());
//    NettyPayloadHelper.setIn(exchange, messageEvent.getMessage());
	
    

}
