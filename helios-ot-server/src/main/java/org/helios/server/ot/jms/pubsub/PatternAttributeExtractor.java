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

import java.util.regex.Pattern;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeExtractorException;

/**
 * <p>Title: PatternAttributeExtractor</p>
 * <p>Description: Attribute extractor for a SubscriptionPattern pattern</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.jms.pubsub.PatternAttributeExtractor</code></p>
 */

public class PatternAttributeExtractor implements AttributeExtractor {

	/**
	 * {@inheritDoc}
	 * @see net.sf.ehcache.search.attribute.AttributeExtractor#attributeFor(net.sf.ehcache.Element, java.lang.String)
	 */
	@Override
	public Pattern attributeFor(Element element, String attributeName) throws AttributeExtractorException {
		Object value = element.getObjectValue();
		if("pattern".equalsIgnoreCase(attributeName) && value instanceof SubscriptionPattern) {
			return ((SubscriptionPattern)value).getPattern();
		}
		return null;
	}

}
