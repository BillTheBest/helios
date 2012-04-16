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
package org.helios.ot.agent;

/**
 * <p>Title: EmptyHeliosOTClientEventListener</p>
 * <p>Description: An empty implementation of {@link HeliosOTClientEventListener} suitable for extending.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.EmptyHeliosOTClientEventListener</code></p>
 */

public class EmptyHeliosOTClientEventListener implements HeliosOTClientEventListener {
	/** Delegate listeners */
	protected final HeliosOTClientEventListener[] delegatedListeners;
	/** Indicates if delegate listeners have been assigned */
	protected final boolean delegates;
	
	/** An empty array of listeners */
	public static final EmptyHeliosOTClientEventListener[] EMPTY_ARRAY = new EmptyHeliosOTClientEventListener[0];
	
	/**
	 * Creates a new EmptyHeliosOTClientEventListener
	 * @param delegatedListeners An optional array of additional listeners that will have events delegated to 
	 */
	public EmptyHeliosOTClientEventListener(HeliosOTClientEventListener...delegatedListeners) {
		this.delegatedListeners = (delegatedListeners==null || delegatedListeners.length<1) ? EMPTY_ARRAY : delegatedListeners;
		delegates = this.delegatedListeners.length>0;
	}
	
	/**
	 * Creates a new EmptyHeliosOTClientEventListener
	 */
	public EmptyHeliosOTClientEventListener() {
		this(EMPTY_ARRAY);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClientEventListener#onConnect(org.helios.ot.agent.HeliosOTClient)
	 */
	@Override
	public void onConnect(HeliosOTClient client) {
		if(delegates) {
			for(HeliosOTClientEventListener listener: delegatedListeners) {
				listener.onConnect(client);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClientEventListener#onConnectFailure(org.helios.ot.agent.HeliosOTClient, java.lang.Throwable)
	 */
	@Override
	public void onConnectFailure(HeliosOTClient client, Throwable cause) {
		if(delegates) {
			for(HeliosOTClientEventListener listener: delegatedListeners) {
				listener.onConnectFailure(client, cause);
			}
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.agent.HeliosOTClientEventListener#onDisconnect(org.helios.ot.agent.HeliosOTClient, java.lang.Throwable)
	 */
	@Override
	public void onDisconnect(HeliosOTClient client, Throwable cause) {
		if(delegates) {
			for(HeliosOTClientEventListener listener: delegatedListeners) {
				listener.onDisconnect(client, cause);
			}
		}		
	}

}
