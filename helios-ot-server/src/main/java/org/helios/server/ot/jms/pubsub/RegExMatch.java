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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.expression.BaseCriteria;

/**
 * <p>Title: RegExMatch</p>
 * <p>Description: Purge regex matcher ehcache search criteria</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.jms.pubsub.RegExMatch</code></p>
 */

public class RegExMatch extends BaseCriteria {
	/** A cache of created RegExMatches */
	protected static final Map<String, RegExMatch> matchers = new ConcurrentHashMap<String, RegExMatch>();
	
	/** The attribute name to compare against */
	protected final String attributeName;	
	/** The compiled pattern to match against */
	protected final Pattern pattern;
	
	/**
	 * Reetrieves the specified RegExMatch
	 * @param attributeName The attribute name to compare against
	 * @param regex The compiled pattern to match against
	 */
	public static RegExMatch getInstance(String attributeName, String regex) {
		if(attributeName==null) throw new IllegalArgumentException("The passed attribute name was null", new Throwable());
		if(regex==null) throw new IllegalArgumentException("The passed regex was null", new Throwable());
		final String key = key(attributeName, regex);
		RegExMatch matcher = matchers.get(key);
		if(matcher==null) {
			synchronized(matchers) {
				matcher = matchers.get(key);
				if(matcher==null) {
					matcher = new RegExMatch(attributeName.trim(), regex.trim());
					matchers.put(key, matcher);
				}
			}
		}
		return matcher;
	}
	
	
	/**
	 * Creates a lookup key
	 * @param attributeName The attribute name to compare against
	 * @param regex The compiled pattern to match against
	 * @return the lookup key
	 */
	protected static String key(String attributeName, String regex) {
		return attributeName.trim() + ":" + regex.trim();
	}
	

	/**
	 * Creates a new RegExMatch
	 * @param attributeName The attribute name to compare against
	 * @param regex The compiled pattern to match against
	 */
	private RegExMatch(String attributeName, String regex) {
		this.attributeName = attributeName;
		this.pattern = Pattern.compile(regex);
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.search.expression.Criteria#execute(net.sf.ehcache.Element, java.util.Map)
	 */
	@Override
	public boolean execute(Element element, Map<String, AttributeExtractor> attributeExtractors) {
        Object value = attributeExtractors.get(attributeName).attributeFor(element, attributeName);
        if (value == null) {
            return false;
        }
        String asString = value.toString().trim();
        return pattern.matcher(asString).matches();
	}
	
    /**
     * Return attribute name.
     * @return String attribute name
     */
    public String getAttributeName() {
        return attributeName;
    }
    
    /**
     * Return regex string.
     * @return String regex.
     */
    public String getRegex() {
        return pattern.pattern();
    }    

}
