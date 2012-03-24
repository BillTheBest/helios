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
package org.helios.esper.engine;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.client.EPSubscriberException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.context.ContextPartitionSelector;

/**
 * <p>Title: HeliosEPStatement</p>
 * <p>Description: Enhanced wrapper for Esper Statements.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 */
public class HeliosEPStatement implements EPStatement {
	protected EPStatement innerStatement = null;
	protected AtomicBoolean persist = new AtomicBoolean(false);
	protected String description = "HeliosEPStatement";
	

	/**
	 * @param innerStatement
	 */
	public HeliosEPStatement(EPStatement innerStatement) {
		this.innerStatement = innerStatement;
	}

	/**
	 * @param arg0
	 * @see com.espertech.esper.client.EPListenable#addListener(com.espertech.esper.client.StatementAwareUpdateListener)
	 */
	public void addListener(StatementAwareUpdateListener arg0) {
		innerStatement.addListener(arg0);
	}

	/**
	 * @param arg0
	 * @see com.espertech.esper.client.EPListenable#addListener(com.espertech.esper.client.UpdateListener)
	 */
	public void addListener(UpdateListener arg0) {
		innerStatement.addListener(arg0);
	}

	/**
	 * @param arg0
	 * @see com.espertech.esper.client.EPStatement#addListenerWithReplay(com.espertech.esper.client.UpdateListener)
	 */
	public void addListenerWithReplay(UpdateListener arg0) {
		innerStatement.addListenerWithReplay(arg0);
	}

	/**
	 * 
	 * @see com.espertech.esper.client.EPStatement#destroy()
	 */
	public void destroy() {
		innerStatement.destroy();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPIterable#getEventType()
	 */
	public EventType getEventType() {
		return innerStatement.getEventType();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#getName()
	 */
	public String getName() {
		return innerStatement.getName();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#getState()
	 */
	public EPStatementState getState() {
		return innerStatement.getState();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPListenable#getStatementAwareListeners()
	 */
	public Iterator<StatementAwareUpdateListener> getStatementAwareListeners() {
		return innerStatement.getStatementAwareListeners();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#getSubscriber()
	 */
	public Object getSubscriber() {
		return innerStatement.getSubscriber();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#getText()
	 */
	public String getText() {
		return innerStatement.getText();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#getTimeLastStateChange()
	 */
	public long getTimeLastStateChange() {
		return innerStatement.getTimeLastStateChange();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPListenable#getUpdateListeners()
	 */
	public Iterator<UpdateListener> getUpdateListeners() {
		return innerStatement.getUpdateListeners();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#getUserObject()
	 */
	public Object getUserObject() {
		return innerStatement.getUserObject();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#isDestroyed()
	 */
	public boolean isDestroyed() {
		return innerStatement.isDestroyed();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#isPattern()
	 */
	public boolean isPattern() {
		return innerStatement.isPattern();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#isStarted()
	 */
	public boolean isStarted() {
		return innerStatement.isStarted();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPStatement#isStopped()
	 */
	public boolean isStopped() {
		return innerStatement.isStopped();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPIterable#iterator()
	 */
	public Iterator<EventBean> iterator() {
		return innerStatement.iterator();
	}

	/**
	 * 
	 * @see com.espertech.esper.client.EPListenable#removeAllListeners()
	 */
	public void removeAllListeners() {
		innerStatement.removeAllListeners();
	}

	/**
	 * @param arg0
	 * @see com.espertech.esper.client.EPListenable#removeListener(com.espertech.esper.client.StatementAwareUpdateListener)
	 */
	public void removeListener(StatementAwareUpdateListener arg0) {
		innerStatement.removeListener(arg0);
	}

	/**
	 * @param arg0
	 * @see com.espertech.esper.client.EPListenable#removeListener(com.espertech.esper.client.UpdateListener)
	 */
	public void removeListener(UpdateListener arg0) {
		innerStatement.removeListener(arg0);
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.EPIterable#safeIterator()
	 */
	public SafeIterator<EventBean> safeIterator() {
		return innerStatement.safeIterator();
	}

	/**
	 * @param arg0
	 * @throws EPSubscriberException
	 * @see com.espertech.esper.client.EPStatement#setSubscriber(java.lang.Object)
	 */
	public void setSubscriber(Object arg0) throws EPSubscriberException {
		innerStatement.setSubscriber(arg0);
	}

	/**
	 * 
	 * @see com.espertech.esper.client.EPStatement#start()
	 */
	public void start() {
		innerStatement.start();
	}

	/**
	 * 
	 * @see com.espertech.esper.client.EPStatement#stop()
	 */
	public void stop() {
		innerStatement.stop();
	}

	/**
	 * Returns EPL or pattern statement annotations provided in the statement text, if any.
	 * @return annotations or a zero-length array if no annotaions have been specified.
	 * @see com.espertech.esper.client.EPStatement#getAnnotations()
	 */
	@Override
	public Annotation[] getAnnotations() {
		return innerStatement.getAnnotations();
	}

	/**
	 * Returns the name of the isolated service provided is the statement is currently isolated in terms of event visibility and scheduling, or returns null if the statement is live in the engine. 
	 * @return isolated service name or null for statements that are not currently isolated
	 * @see com.espertech.esper.client.EPStatement#getServiceIsolated()
	 */
	@Override
	public String getServiceIsolated() {
		return innerStatement.getServiceIsolated();
	}

	/**
	 * {@inheritDoc}
	 * @see com.espertech.esper.client.EPStatement#iterator(com.espertech.esper.client.context.ContextPartitionSelector)
	 */
	@Override
	public Iterator<EventBean> iterator(ContextPartitionSelector selector) {
		return innerStatement.iterator(selector);
	}

	/**
	 * {@inheritDoc}
	 * @see com.espertech.esper.client.EPStatement#safeIterator(com.espertech.esper.client.context.ContextPartitionSelector)
	 */
	@Override
	public SafeIterator<EventBean> safeIterator(ContextPartitionSelector selector) {
		return innerStatement.safeIterator(selector); 
	}

}
