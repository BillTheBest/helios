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
package org.helios.ot.agent.impl.netty.handler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.ot.agent.protocol.impl.HeliosProtocolInvocation;
import org.helios.ot.agent.protocol.impl.HeliosProtocolResponse;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * <p>Title: HeliosProtocolHandler</p>
 * <p>Description: A channel handler that implements the Helios Client Protocol.</p>
 * <p>Broadly, this handler provides event delegation to registered listeners for:<ul>
 * <li>State changes in the client's channel</li>
 * <li>Dispatches of messages or invocations being sent to the server</li>
 * <li>Receipt asynchronous of invocation responses or server pushes</li>
 * <li>Handling of synchronous invocations and the wait/return of the invocation response</li>
 * </ul></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.impl.netty.handler.HeliosProtocolHandler</code></p>
 */

public class HeliosProtocolHandler extends SimpleChannelHandler {
	/** static instance counter */
	private static final AtomicLong instanceCounter = new AtomicLong(0);
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The registered response listeners */
	protected final Set<InvocationResponseListener> responseListeners = new CopyOnWriteArraySet<InvocationResponseListener>();
	/** The registered request listeners (interceptors) */
	protected final Set<InvocationRequestListener> requestListeners = new CopyOnWriteArraySet<InvocationRequestListener>();
	/** The registered channel state change listeners */
	protected final Set<ChannelStateChangeListener> stateChangeListeners = new CopyOnWriteArraySet<ChannelStateChangeListener>();

	
	
	/**
	 * Creates a new HeliosProtocolHandler
	 * @param listeners An optional array of protocol listeners
	 */
	public HeliosProtocolHandler(ProtocolListener...listeners) {
		long instanceNumber = instanceCounter.incrementAndGet();
		if(log.isDebugEnabled()) log.debug("Created HeliosProtocolHandler Instance#" + instanceNumber);
		if(listeners!=null) {
			for(ProtocolListener pl: listeners) {
				if(pl==null) continue;
				if(pl instanceof ChannelStateChangeListener) {
					addChannleStateListener((ChannelStateChangeListener)pl);
				} else if(pl instanceof InvocationRequestListener) {
					addRequestListener((InvocationRequestListener)pl);
				} else if(pl instanceof InvocationResponseListener) {
					addResponseListener((InvocationResponseListener)pl);
				}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(ctx, e);
	}	
	
	/**
	 * Processes all messages sent from the server and dispatches to applicable registered listeners
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		if(!responseListeners.isEmpty()) {
			Object obj = e.getMessage();
			if(obj instanceof HeliosProtocolResponse) {
				HeliosProtocolResponse hpr = (HeliosProtocolResponse)obj;
				for(InvocationResponseListener listener: responseListeners) {
					if(listener instanceof FilteringInvocationResponseListener) {
						if(!((FilteringInvocationResponseListener)listener).isResponseEnabled(hpr)) {
							continue;
						}
					}
					listener.onInvocationResponse(hpr, ctx, e);				
				}
			}
		}
		ctx.sendUpstream(e);
	}
	
	/**
	 * Intercepts messages being sent to the server and dispatches to registered listeners so they 
	 * have an opportunity to futz with the payload before it is sent.
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#writeRequested(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) {
		if(!requestListeners.isEmpty()) {
			Object obj = e.getMessage();
			if(obj instanceof HeliosProtocolInvocation) {
				HeliosProtocolInvocation hpi = (HeliosProtocolInvocation)obj;
				for(InvocationRequestListener listener: requestListeners) {
					if(listener instanceof FilteringInvocationRequestListener) {
						if(!((FilteringInvocationRequestListener)listener).isRequestEnabled(hpi)) {
							continue;
						}
					}
					listener.onInvocationRequest(hpi, ctx, e);				
				}			
			}
		}
		ctx.sendDownstream(e);
	}
	
	/**
	 * Notifies registered listeners of an upstream channel state change event
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		
		if(!stateChangeListeners.isEmpty() && e instanceof ChannelStateEvent) {
			ChannelStateEvent event = (ChannelStateEvent)e;
			ChannelState state = event.getState();
			for(ChannelStateChangeListener listener: stateChangeListeners) {
				if(listener instanceof FilteringChannelStateChangeListener) {
					if(!((FilteringChannelStateChangeListener)listener).isStateChangeEnabled(true, state)) {
						continue;
					}
					listener.onChannelStateChange(true, state, event.getChannel(), ctx, e);
				}
			}
		}		
		super.handleUpstream(ctx, e);
	}
	
	/**
	 * Notifies registered listeners of a downstream channel state change event
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(!stateChangeListeners.isEmpty() && e instanceof ChannelStateEvent) {
			ChannelStateEvent event = (ChannelStateEvent)e;
			ChannelState state = event.getState();
			for(ChannelStateChangeListener listener: stateChangeListeners) {
				if(listener instanceof FilteringChannelStateChangeListener) {
					if(!((FilteringChannelStateChangeListener)listener).isStateChangeEnabled(false, state)) {
						continue;
					}
					listener.onChannelStateChange(true, state, event.getChannel(), ctx, e);
				}
			}
		}
		super.handleDownstream(ctx, e);
	}
	
	

	/**
	 * Registers a response listener that will be called back when this handler receives a response from the server
	 * @param listener The listener to register
	 */
	public void addResponseListener(InvocationResponseListener listener) {
		if(listener!=null) {
			responseListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a response listener
	 * @param listener The listener to unregister
	 */
	public void removeResponseListener(InvocationResponseListener listener) {
		if(listener!=null) {
			responseListeners.remove(listener);
		}
	}
	
	
	/**
	 * Registers a request listener that will be called back when this handler receives a server bound request.
	 * @param listener The listener to register
	 */
	public void addRequestListener(InvocationRequestListener listener) {
		if(listener!=null) {
			requestListeners.add(listener);
		}
	}
	
	
	
	/**
	 * Unregisters a request listener
	 * @param listener The listener to unregister
	 */
	public void removeRequestListener(InvocationRequestListener listener) {
		if(listener!=null) {
			requestListeners.remove(listener);
		}
	}
	
	/**
	 * Registers a channel state change listener that will be called back this handler detects a channel state change
	 * @param listener The listener to register
	 */
	public void addChannleStateListener(ChannelStateChangeListener listener) {
		if(listener!=null) {
			stateChangeListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a channel state change listener 
	 * @param listener The listener to unregister
	 */
	public void removeChannleStateListener(ChannelStateChangeListener listener) {
		if(listener!=null) {
			stateChangeListeners.add(listener);
		}
	}
	
	
}
