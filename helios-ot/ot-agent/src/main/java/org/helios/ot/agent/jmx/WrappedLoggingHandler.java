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
package org.helios.ot.agent.jmx;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.helios.helpers.StringHelper;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;

/**
 * <p>Title: WrappedLoggingHandler</p>
 * <p>Description: A wrapped Netty logging handler so that we can modify the internal logging level</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.jmx.WrappedLoggingHandler</code></p>
 */
public class WrappedLoggingHandler extends NotificationBroadcasterSupport implements ChannelDownstreamHandler, ChannelHandler, ChannelUpstreamHandler {
	/** The name of the logger to create */
	protected final String loggerName;
	/** A reference to the current logging handler */
	protected final AtomicReference<LoggingHandler> delegate = new AtomicReference<LoggingHandler>(null);
	/** Indicates if logged events should be broadcast as JMX notifications */
	protected boolean jmxEnabled = false;
	/** An executor shared with the client used to propagate JMX notifications */
	protected final Executor executor;
	/** A sequence factory for jmx notifications */
	protected final AtomicLong sequence = new AtomicLong(0L);
	/** The ObjectName of the agent client */
	protected final ObjectName objectName;
	/** The provided Mbean notification info */
	protected final MBeanNotificationInfo infos = new MBeanNotificationInfo(
			new String[]{"state", "message", "exception", "write", "idle", "other"},
			Notification.class.getName(),
			"Netty Channel Event Broadcaster"
	);
			
	
	
	/** The current logging level */
	protected InternalLogLevel logLevel = null; 
	
	/**
	 * Creates a new WrappedLoggingHandler
	 * @param objectName The ObjectName of the agent client
	 * @param executor The notification broadcaster's thread pool
	 * @param loggerName The name of the logger to create
	 * @param enableJmxNotifications Indicates if logged events should be broadcast as JMX notifications
	 */
	public WrappedLoggingHandler(ObjectName objectName, Executor executor, String loggerName, boolean enableJmxNotifications) {
		super(executor);
		this.executor = executor;
		this.loggerName = loggerName;
		this.objectName = objectName;
		jmxEnabled= enableJmxNotifications;
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());
		delegate.set(new LoggingHandler(loggerName));
		logLevel = delegate.get().getLevel();
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {	
		return new MBeanNotificationInfo[]{infos};
	}
	
	/**
	 * Returns the current log level
	 * @return the current log level
	 */
	public String getLogLevel() {
		return logLevel.name();
	}
	
	/**
	 * Sets the logging level of the delegate logger
	 * @param level The name of the level to set 
	 */
	public synchronized void setLogLevel(String level) {
		if(level==null) throw new IllegalArgumentException("The passed level was null", new Throwable());
		try {
			InternalLogLevel newLevel = InternalLogLevel.valueOf(level.trim().toUpperCase());
			if(newLevel!=logLevel) {
				delegate.set(new LoggingHandler(loggerName, newLevel, false));
				logLevel = newLevel;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed level [" + level + "] was not a valid logging level. Levels are " + Arrays.toString(InternalLogLevel.values()), new Throwable());
		}
	}
	
	/**
	 * Returns the hash code of the current delegate logging handler
	 * @return the hash code of the current delegate logging handler
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return delegate.get().hashCode();
	}
	/**
	 * Compares the passed object to the delegate to compare for equality
	 * @param obj The object to compare to 
	 * @return true if they are equal, false otherwise
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return delegate.get().equals(obj);
	}
	/**
	 * Returns the current delegate logger
	 * @return the current delegate logger
	 * @see org.jboss.netty.handler.logging.LoggingHandler#getLogger()
	 */
	public InternalLogger getLogger() {
		return delegate.get().getLogger();
	}
	/**
	 * Returns the current delegate logger level
	 * @return the current delegate logger level
	 * @see org.jboss.netty.handler.logging.LoggingHandler#getLevel()
	 */
	public InternalLogLevel getLevel() {
		return delegate.get().getLevel();
	}
	/**
	 * Logs the specified event to the InternalLogger returned by getLogger().
	 * @param e The channel event to log
	 * @see org.jboss.netty.handler.logging.LoggingHandler#log(org.jboss.netty.channel.ChannelEvent)
	 */
	public void log(ChannelEvent e) {
		Notification n = null;
		final StringBuilder b = new StringBuilder();
		if(jmxEnabled) {
			// "state", "message", "exception", "write", "idle", "other"
			if(e instanceof ExceptionEvent) {
				ExceptionEvent ee = (ExceptionEvent)e;
				n = new Notification("exception", objectName, sequence.incrementAndGet(), 
						b.append("Exception on [").append(e.getChannel().toString()).append("]")
						.append(ee.getCause())
						.append("\n")
						.append(StringHelper.formatStackTrace(ee.getCause()))
						.toString()
				);
				sendNotification(n);
				return;
			}
			b.append("Channel Event [" + e.getChannel().toString()).append("]\n");
			String type = getType(e, b);
			n = new Notification(type, objectName, sequence.incrementAndGet(), b.toString());
			sendNotification(n);
			return;
		}
	}
	
	
	
	//"state", "message", "exception", "write", "idle", "other"
	protected String getType(ChannelEvent ce, final StringBuilder b) {
		if(ce instanceof MessageEvent) {
			Object obj = ((MessageEvent)ce).getMessage();
			b.append("Message Type:[" + obj.getClass().getName()).append("] Value:[").append(obj.toString()).append("]");
			return "message";
		} else if(ce instanceof ChannelStateEvent) {
			ChannelStateEvent cse = (ChannelStateEvent)ce;
			b.append("Channel State Change To:").append(cse.getState().name());
			return "state";
		} else if(ce instanceof WriteCompletionEvent) {
			b.append("Write Completed. Bytes Writen:").append(((WriteCompletionEvent)ce).getWrittenAmount());
			return "write";			
		} else if(ce instanceof IdleStateEvent) {
			IdleStateEvent ise = (IdleStateEvent)ce;
			b.append("Idle State Event. Idle State [").append(ise.getState().name()).append("] Idle Time Ms:").append(System.currentTimeMillis()-ise.getLastActivityTimeMillis());
			return "idle";						
		} else {
			b.append("Other:").append(ce);
			return "other";
		}
	}
	
	/**
	 * Handles the specified upstream event.
	 * @param ctx the context object for this handler
	 * @param e the upstream event to process or intercept 
	 * @throws Exception
	 * @see org.jboss.netty.handler.logging.LoggingHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		log(e);
		delegate.get().handleUpstream(ctx, e);		
	}
	
	
	/**
	 * Handles the specified downstream event.
	 * @param ctx the context object for this handler
	 * @param e the downstream event to process or intercept 
	 * @throws Exception
	 * @see org.jboss.netty.handler.logging.LoggingHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		log(e);
		delegate.get().handleDownstream(ctx, e);		
	}

	/**
	 * Constructs a <code>String</code> with all attributes in <code>name:value</code> format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder();    
	    retValue.append("WrappedLoggingHandler [")
		    .append(TAB).append("loggerName:").append(this.loggerName)
		    .append(TAB).append("delegate:").append(this.delegate)
		    .append(TAB).append("jmxEnabled:").append(this.jmxEnabled)
		    .append(TAB).append("logLevel:").append(this.logLevel)
	    	.append("\n]");    
	    return retValue.toString();
	}

	/**
	 * Indicates if JMX events are being logged
	 * @return true if JMX events are being logged, false otherwise
	 */
	public boolean isJmxEnabled() {
		return jmxEnabled;
	}

	/**
	 * Sets the enabled state of jmx logging
	 * @param jmxEnabled true to enable jmx logging
	 */
	public void setJmxEnabled(boolean jmxEnabled) {
		this.jmxEnabled = jmxEnabled;
	}


}
