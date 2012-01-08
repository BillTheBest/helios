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
package org.helios.server.ot.session.jms;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;

import org.helios.server.ot.session.SessionSubscriptionTerminator;

/**
 * <p>Title: TopicSubscription</p>
 * <p>Description: A session managed JMS topic subscription.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.jms.TopicSubscription</code></p>
 */

public class TopicSubscription implements SessionSubscriptionTerminator, ExceptionListener {
	/** A JMS connection factory shared amongst all subscribers */
	protected static final AtomicReference<ConnectionFactory> sharedConnectionFactory = new AtomicReference<ConnectionFactory>(null);
	/** A JMS connection shared amongst all subscribers */
	protected static final AtomicReference<Connection> sharedConnection = new AtomicReference<Connection>(null);
	/** The multiplexing exception listener attached to the shared connection */
	protected static final MultiplexedExceptionListener exceptionListener = new MultiplexedExceptionListener();
	/** The JMS Session for this subscription */
	protected Session session = null;
	/** The JMS Message Consumer for this subscription */
	protected MessageConsumer consumer = null;
	/** The JMS topic for this subscription */
	protected Topic topic = null;
	
	
	
	/**
	 * Sets the connection factory to acquire the shared connection. 
	 * @param connectionFactory The connection factory to acquire the shared connection.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		if(connectionFactory==null) throw new IllegalArgumentException("The passed connectionFactory was null", new Throwable());
		if(sharedConnectionFactory.get()==null) {
			synchronized(sharedConnectionFactory) {
				if(sharedConnectionFactory.get()==null) {
					sharedConnectionFactory.set(connectionFactory);
				}
			}
		}
		if(sharedConnection.get()==null) {
			synchronized(sharedConnection) {
				if(sharedConnection.get()==null) {
					try {
						sharedConnection.set(connectionFactory.createConnection());
						sharedConnection.get().setExceptionListener(exceptionListener);
						sharedConnection.get().start();
					} catch (Exception e) {
						throw new RuntimeException("Failed to acquire JMS Connection", e);
					}
				}
			}
		}
		exceptionListener.addListener(this);
		try {
			session = sharedConnection.get().createSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire JMS Session", e);
		}
	}
	
	
	/**
	 * <p>Title: MultiplexedExceptionListener</p>
	 * <p>Description: The topic subscribers exception listener.</p> 
	 */
	protected static class MultiplexedExceptionListener implements ExceptionListener {
		/** The exception listeners that are multiplexed to */
		protected Set<ExceptionListener> listeners = new CopyOnWriteArraySet<ExceptionListener>();
		
		/**
		 * Registers a new exception listener
		 * @param listener A JMS exception listener
		 */
		public void addListener(ExceptionListener listener) {
			if(listener==null) throw new IllegalArgumentException("Passed listener was null", new Throwable());
			listeners.add(listener);
		}
		
		/**
		 * Unregisters a new exception listener
		 * @param listener A JMS exception listener
		 */
		public void removeListener(ExceptionListener listener) {
			if(listener==null) throw new IllegalArgumentException("Passed listener was null", new Throwable());
			listeners.remove(listener);
		}
		
		
		/**
		 * {@inheritDoc}
		 * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
		 */
		@Override
		public void onException(JMSException exception) {
			// close shared connection
			// try to reconnect
			// if reconnect, call listeners so they can re-init
			// if reconnect fails, terminate all by passing null exception
			for(ExceptionListener listener: listeners) {
				listener.onException(exception);
			}
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.SessionSubscriptionTerminator#terminate()
	 */
	@Override
	public void terminate() {
		if(consumer!=null) try { consumer.close(); } catch (Exception e) {}
		if(session!=null) try { session.close(); } catch (Exception e) {}
		// fire terminate event
	}

	/**
	 * {@inheritDoc}
	 * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
	 */
	@Override
	public void onException(JMSException exception) {
		// if the exception is null, the shared connection could not be re-established.
		if(exception==null) {
			terminate();
		} else {
			if(consumer!=null) try { consumer.close(); } catch (Exception e) {}
			if(session!=null) try { session.close(); } catch (Exception e) {}
		}
	}

}
