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
package org.helios.server.ot.session.camel.transform.trace;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.log4j.Logger;
import org.helios.ot.trace.Trace;

/**
 * <p>Title: ExchangeToTrace</p>
 * <p>Description: A Camel expression to convert an exchange into a Helios Trace </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.transform.trace.ExchangeToTrace</code></p>
 */

public class ExchangeToTrace implements Expression {
	/** Instance logger */
	protected Logger log = Logger.getLogger(getClass());
	
	/**
	 * {@inheritDoc}
	 * <p>Converts the passed exchange to a Trace
	 * @see org.apache.camel.Expression#evaluate(org.apache.camel.Exchange, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T evaluate(Exchange exchange, Class<T> type) {
		if(exchange==null) throw new IllegalArgumentException("Passed exchange was null", new Throwable());
		if(type==null) throw new IllegalArgumentException("Passed type was null", new Throwable());
		return (T)exchange.getIn().getBody(Trace.class);
	}

}
