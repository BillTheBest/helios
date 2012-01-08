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
import java.util.regex.Pattern;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.expression.EqualTo;

/**
 * <p>Title: PatternMatchesTo</p>
 * <p>Description: Pattern criteria comparator</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.jms.pubsub.PatternMatchesTo</code></p>
 */

public class PatternMatchesTo extends EqualTo {
	protected final String value;
	protected final String name;
	
	/**
	 * Creates a new PatternMatchesTo
	 * @param attributeName The attribute name
	 * @param value The value to match to
	 */
	public PatternMatchesTo(String attributeName, String value) {
		super(attributeName, value);
		this.value = value;
		name = attributeName;
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.search.expression.EqualTo#execute(net.sf.ehcache.Element, java.util.Map)
	 */
	@Override
	public boolean execute(Element element, Map<String,AttributeExtractor> extractors) {
		AttributeExtractor ae = extractors.get(name);
		if(ae==null) return false;
		Pattern p = (Pattern)ae.attributeFor(element, name);
		return p.matcher(value).matches();		
	}

}
