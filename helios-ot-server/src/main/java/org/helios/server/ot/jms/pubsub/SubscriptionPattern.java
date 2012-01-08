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
package org.helios.server.ot.jms.pubsub;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import net.sf.ehcache.Element;

/**
 * <p>Title: SubscriptionPattern</p>
 * <p>Description: A wrapper for a metric subscriber JMS topic pattern</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.jms.pubsub.SubscriptionPattern</code></p>
 * Example FQN:  host4/agent4/Threading/Daemon Thread Count
 * Example Topic: host1\..*\..*
 */

public class SubscriptionPattern implements Serializable {
	/** The number of subscribers to this pattern */
	protected final AtomicInteger subCount = new AtomicInteger(0);
	/** The regex pattern */
	protected final Pattern pattern;
	/** The represented topic pattern */
	protected final String topic;
	
	/** The topic prefix to strip from the destination */
	public static final String TOPIC_PREFIX = "topic://helios.metrictree.";
	
	/**
	 * Creates a new SubscriptionPattern
	 * @param topic The sub topic
	 * @return a new SubscriptionPattern
	 */
	public static SubscriptionPattern newInstance(String topic) {
		if(topic==null) throw new IllegalArgumentException("The passed topic was null", new Throwable());
		return new SubscriptionPattern(topic);
	}
	
	/**
	 * Creates a new SubscriptionPattern
	 * @param topic The original topic pattern
	 * getDestination().toString().replace("topic://helios.metrictree.", "").replaceAll(">$", "*");
	 */
	private SubscriptionPattern(String topic) {			
		this.topic = topic;
		pattern = Pattern.compile(topic
				.replace(TOPIC_PREFIX, "")
				.replace(".*.", "/.*?/")
				.replaceAll(".>$", "/.*")				
		);		
	}
	
	/**
	 * Increments the sub count and returns the new count
	 * @return this SubscriptionPattern 
	 */
	public SubscriptionPattern incr() {
		subCount.incrementAndGet();
		return this;
	}
	
	/**
	 * dEcrements the sub count and returns the new count
	 * @return this SubscriptionPattern 
	 */
	public SubscriptionPattern decr() {
		subCount.decrementAndGet();
		return this;
	}
	
	public Element getElement() {
		return new Element(getPatternValue(), this);
	}
	
	/**
	 * Tests the passed matchPattern for matching to the pattern
	 * @param matchPattern the pattern to test
	 * @return true if it matches, false otherwise
	 */
	public boolean matches(CharSequence matchPattern) {
		return pattern.matcher(matchPattern).matches();
	}
	

	/**
	 * The number of subscribers to this pattern
	 * @return the subCount
	 */
	public int getSubCount() {
		return subCount.get();
	}

	/**
	 * The sub topic regex pattern
	 * @return the pattern
	 */
	public Pattern getPattern() {
		return pattern;
	}
	
	/**
	 * Returns the pattern expression
	 * @return the pattern expression
	 */
	public String getPatternValue() {
		return pattern.pattern();
	}

	/**
	 * The original topic name
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}
	
	
	
	
}
