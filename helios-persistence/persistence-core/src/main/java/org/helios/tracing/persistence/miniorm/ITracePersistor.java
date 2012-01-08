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
package org.helios.tracing.persistence.miniorm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;



/**
 * <p>Title: ITracePersistor</p>
 * <p>Description: Defines a class that creates prepared statements for the persistence of TraceModels./</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.persistence.miniorm.ITracePersistor</code></p>
 */

public interface ITracePersistor {
	
	/**
	 * Executes a JDBC insert for the passed trace Object
	 * @param traceObject The object to be inserted
	 * @param conn  The JDBC connection
	 * @throws SQLException
	 */
	public void doInsert(Object traceObject, Connection conn) throws SQLException;
	
	/**
	 * Executes a batched JDBC insert for the passed trace Object
	 * @param traceObject The object to be inserted
	 * @param statement  The JDBC prepared statement
	 * @throws SQLException
	 */
	public void doInsert(Object traceObject, PreparedStatement statement) throws SQLException;
	
	/**
	 * Returns the configured SQL insert statement
	 * @return the configured SQL insert statement
	 */
	public String getInsertSql();
	
	/**
	 * Returns a compiled prepared statement to load all items from the DB
	 * @param conn A DB connection
	 * @return a compiled prepared statement
	 * @throws SQLException
	 */
	public PreparedStatement getLoadStatement(Connection conn) throws SQLException;
	
	/**
	 * Returns the number of items in the DB
	 * @param conn A JDBC connection
	 * @return the number of items in the DB
	 */
	public long getCount(Connection conn);
	
	
	
}
