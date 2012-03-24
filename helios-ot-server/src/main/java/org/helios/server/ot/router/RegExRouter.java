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
package org.helios.server.ot.router;

import java.util.regex.Pattern;

import net.sf.ehcache.Cache;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.lmax.disruptor.EventFactory;

/**
 * <p>Title: RegExRouter</p>
 * <p>Description: A message router that routes messages to registered listeners based on regular expression pattern matching against interface, method name or annotated methods in the messages.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.router.RegExRouter</code></p>
 */

public class RegExRouter<T> extends ManagedObjectDynamicMBean implements CamelContextAware, ApplicationContextAware {
	/**  */
	private static final long serialVersionUID = -7915093851690834935L;
	/** The spring application context */
	protected ApplicationContext applicationContext = null;
	/** The camel context */
	protected CamelContext camelContext = null;
	/** The type of the message being routed */
	protected final Class<? extends T> messageClass;
	/** A cache of {@link IRouterListener}s keyed by recognized positive pattern matches */
	@Autowired(required=true)
	@Qualifier("patternMatchCache")
	protected Cache patternMatchCache;
	
	
	/**
	 * Creates a new RegExRouter
	 * @param messageClass the type of the message being routed
	 */
	public RegExRouter(Class<? extends T> messageClass) {
		this.messageClass = messageClass;
	}
	
	/**
	 * Routes the passed messages to matching listeners
	 * @param messages An array of messages which are expected to be of the same type within each call
	 */
	public void route(T...messages) {
		if(messages==null || messages.length<1) return;
		
	}
	
	public static class MessageEvent<T> {
		/** The routed message */
		private T message;

		/**
		 * Returns the event message
		 * @return the message
		 */
		public T getMessage() {
			return message;
		}

		/**
		 * Sets the event message
		 * @param message the message to set
		 */
		public void setMessage(T message) {
			this.message = message;
		}
		
		public final static  EventFactory<MessageEvent<?>> EVENT_FACTORY = new EventFactory<MessageEvent<?>>() {
			@SuppressWarnings("rawtypes")
			public MessageEvent<?> newInstance() {
				return new MessageEvent();
			}
		};
		
		
	}

	

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.CamelContextAware#setCamelContext(org.apache.camel.CamelContext)
	 */
	@Override
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
	}

	/**
	 * {@inheritDoc}
	 * @see org.apache.camel.CamelContextAware#getCamelContext()
	 */
	@Override
	public CamelContext getCamelContext() {
		return camelContext;
	}
	
	
	public static class RegexSubscription {
		/** The regular expression to match againts in incoming messages to determine if if they should be routed to the listener */
		private final Pattern pattern;
		/** The listener that matching messages should be routed to  */
		private final IRouterListener listener;
		
		/**
		 * Creates a new RegexSubscription
		 * @param pattern The regular expression to match againts in incoming messages to determine if if they should be routed to the listener
		 * @param listener The listener that matching messages should be routed to
		 */
		private RegexSubscription(Pattern pattern, IRouterListener listener) {
			this.pattern = pattern;
			this.listener = listener;
		}
		
		/**
		 * Creates a new RegexSubscription
		 * @param pattern The regular expression to match againts in incoming messages to determine if if they should be routed to the listener
		 * @param listener The listener that matching messages should be routed to
		 * @return a RegexSubscription
		 */
		public static RegexSubscription getInstance(Pattern pattern, IRouterListener listener) {
			if(pattern==null) throw new IllegalArgumentException("The passed pattern was null", new Throwable());
			if(listener==null) throw new IllegalArgumentException("The passed listener was null", new Throwable());
			return new RegexSubscription(pattern, listener);
		}

		/**
		 * Creates a new RegexSubscription
		 * @param pattern The regular expression to match againts in incoming messages to determine if if they should be routed to the listener
		 * @param listener The listener that matching messages should be routed to
		 * @return a RegexSubscription
		 */
		public static RegexSubscription getInstance(CharSequence pattern, IRouterListener listener) {
			if(pattern==null) throw new IllegalArgumentException("The passed pattern was null", new Throwable());
			if(listener==null) throw new IllegalArgumentException("The passed listener was null", new Throwable());
			return new RegexSubscription(Pattern.compile(pattern.toString()), listener);
		}

		/**
		 * Returns the regular expression to match againts in incoming messages to determine if if they should be routed to the listener
		 * @return the pattern
		 */
		public Pattern getPattern() {
			return pattern;
		}

		/**
		 * Returns the listener that matching messages should be routed tos
		 * @return the listener
		 */
		public IRouterListener getListener() {
			return listener;
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
		    StringBuilder retValue = new StringBuilder("RegexSubscription [")
		        .append(TAB).append("pattern[").append(this.pattern.pattern()).append("]")
		        .append(TAB).append("listener[").append(this.listener.getClass().getName()).append("] (").append(this.listener.toString()).append(")")
		        .append("\n]");    
		    return retValue.toString();
		}
		
		
		
	}

}
