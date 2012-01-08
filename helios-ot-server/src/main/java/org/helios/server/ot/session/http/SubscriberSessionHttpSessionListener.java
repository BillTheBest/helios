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
package org.helios.server.ot.session.http;

import java.util.Date;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.server.ot.session.SubscriberSession;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>Title: SubscriberSessionHttpSessionListener</p>
 * <p>Description: An HTTP session listener that terminates a SubscriberSession when the session is invalidated.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.http.SubscriberSessionHttpSessionListener</code></p>
 */
@JMXManagedObject(annotated=true, declared=true)
public class SubscriberSessionHttpSessionListener implements HttpSessionBindingListener, ApplicationContextAware {
	/** The managed subscriber session */
	protected final SubscriberSession subscriberSession;
	/** The http session */
	protected HttpSession session = null;
	/** The Spring app context */
	protected ApplicationContext applicationContext = null;
	/** The initial session timeout in s. */
	protected int initialSessionIdleTimeout = 120;
	
	
	/**
	 * Creates a new SubscriberSessionHttpSessionListener
	 * @param subscriberSession The managed subscriber session
	 */
	public SubscriberSessionHttpSessionListener(SubscriberSession subscriberSession) {
		this.subscriberSession = subscriberSession;
		this.subscriberSession.reflectObject(this);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.servlet.http.HttpSessionBindingListener#valueBound(javax.servlet.http.HttpSessionBindingEvent)
	 */
	@Override
	public void valueBound(HttpSessionBindingEvent event) {
		session = event.getSession();
		session.setMaxInactiveInterval(initialSessionIdleTimeout);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.servlet.http.HttpSessionBindingListener#valueUnbound(javax.servlet.http.HttpSessionBindingEvent)
	 */
	@Override
	public void valueUnbound(HttpSessionBindingEvent event) {
		subscriberSession.terminate();
	}

	/**
	 * Returns the session creation timestamp
	 * @return The session creation timestamp
	 * @see javax.servlet.http.HttpSession#getCreationTime()
	 */
	@JMXAttribute(name="CreationTime", description="The session creation timestamp", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCreationTime() {
		return session.getCreationTime();
	}
	
	/**
	 * Returns the session creation date
	 * @return The session creation date
	 * @see javax.servlet.http.HttpSession#getCreationTime()
	 */
	@JMXAttribute(name="CreationDate", description="The session creation date", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getCreationDate() {
		return new Date(session.getCreationTime());
	}
	

	/**
	 * Returns the session Id
	 * @return the session Id
	 * @see javax.servlet.http.HttpSession#getId()
	 */
	@JMXAttribute(name="SessionId", description="The session id", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getId() {
		return session.getId();
	}

	/**
	 * Returns the last accessed timestamp
	 * @return the last accessed timestamp
	 * @see javax.servlet.http.HttpSession#getLastAccessedTime()
	 */
	@JMXAttribute(name="LastAccessedTime", description="The session last accessed timestamp", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastAccessedTime() {
		return session.getLastAccessedTime();
	}
	
	/**
	 * Returns the last accessed date
	 * @return the last accessed date
	 * @see javax.servlet.http.HttpSession#getLastAccessedTime()
	 */
	@JMXAttribute(name="LastAccessedDate", description="The session last accessed date", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getLastAccessedDate() {
		return new Date(session.getLastAccessedTime());
	}
	
	/**
	 * Returns the number of ms. until this session terminates without activity
	 * @return the number of ms. until this session terminates without activity
	 */
	@JMXAttribute(name="TimeToTerminate", description="The number of ms. until this session terminates without activity", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTimeToTerminate() {
		long time = System.currentTimeMillis()-session.getLastAccessedTime();
		return (getMaxInactiveInterval()*1000)-time;
	}
	

	/**
	 * Returns the maximum inactive interval time in s.
	 * @return the maximum inactive interval time in s.
	 * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
	 */
	@JMXAttribute(name="MaxInactiveInterval", description="The session maximum inactive interval time in s.", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getMaxInactiveInterval() {
		return session.getMaxInactiveInterval();
	}
	
	/**
	 * Sets the maximum inactive interval time in s.
	 * @param interval the maximum inactive interval time in s.
	 * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
	 */
	public void setMaxInactiveInterval(int interval) {
		session.setMaxInactiveInterval(interval);
	}
	

	/**
	 * Invalidates this session 
	 * @see javax.servlet.http.HttpSession#invalidate()
	 */
	@JMXOperation(name="terminate", description="Terminates this session")
	public void invalidate() {
		session.invalidate();
	}

	/**
	 * Sets the Spring app contex
	 * @param applicationContext the applicationContext to set
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Sets the initial session idle timeout in s.
	 * @param sessionIdleTimeout the sessionIdleTimeout to set
	 */
	public void setInitialSessionIdleTimeout(int sessionIdleTimeout) {
		this.initialSessionIdleTimeout = sessionIdleTimeout;
	}

	

}
