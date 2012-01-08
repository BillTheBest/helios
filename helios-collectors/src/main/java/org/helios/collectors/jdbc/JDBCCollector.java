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
package org.helios.collectors.jdbc;

import org.helios.collectors.AbstractCollector;
import org.helios.collectors.CollectionResult;
import org.helios.collectors.exceptions.CollectorStartException;
import org.helios.collectors.jdbc.connection.IJDBCConnectionFactory;

/**
 * <p>Title: JDBCCollector</p>
 * <p>Description: Helios collector for JDBC sources.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class JDBCCollector extends AbstractCollector {
	protected IJDBCConnectionFactory connectionFactory = null;
	protected long connectionTimeout = 5000;
	protected long operationTimeout = 5000;
	
	/**
	 * 
	 */
	public JDBCCollector() {
	}

	/**
	 * @return
	 * @see org.helios.collectors.AbstractCollector#collectCallback()
	 */
	@Override
	public CollectionResult collectCallback() {
		return null;
	}

	/**
	 * @return
	 * @see org.helios.collectors.AbstractCollector#getCollectorVersion()
	 */
	@Override
	public String getCollectorVersion() {
		return null;
	}

	/**
	 * @throws CollectorStartException
	 * @see org.helios.collectors.AbstractCollector#startCollector()
	 */
	@Override
	public void startCollector() throws CollectorStartException {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
