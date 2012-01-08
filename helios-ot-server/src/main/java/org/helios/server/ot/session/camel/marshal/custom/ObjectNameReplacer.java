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
package org.helios.server.ot.session.camel.marshal.custom;

import javax.management.ObjectName;

import org.helios.helpers.JMXHelper;

/**
 * <p>Title: ObjectNameReplacer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.marshal.custom.ObjectNameReplacer</code></p>
 */

public class ObjectNameReplacer<F, T> implements ProviderTypeReplacer<ObjectName, String> {

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.marshal.custom.ProviderTypeReplacer#marshalReplace(java.lang.Object)
	 */
	@Override
	public String marshalReplace(ObjectName f) {
		if(f==null) throw new IllegalArgumentException("The passed object name was null", new Throwable());
		return f.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.server.ot.session.camel.marshal.custom.ProviderTypeReplacer#unmarshalReplace(java.lang.Object)
	 */
	@Override
	public ObjectName unmarshalReplace(String t) {
		if(t==null) throw new IllegalArgumentException("The passed string was null", new Throwable());
		return JMXHelper.objectName(t);
	}

}
