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
package org.helios.collectors.jdbc.connection;

import java.sql.Connection;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>Title: BeanReferenceJDBCConnectionFactory</p>
 * <p>Description: A JDBC Connection Factory that acquires connections from another spring bean.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class BeanReferenceJDBCConnectionFactory implements IJDBCConnectionFactory {
	/** A reference to the datasource */
	protected DataSource dataSource = null;

	/**
	 * Simple Constructor for BeanReferenceJDBCConnectionFactory
	 */
	public BeanReferenceJDBCConnectionFactory() {
	}
	
	/**
	 * Parameterized Constructor for BeanReferenceJDBCConnectionFactory
	 * @param dataSource The injected data source.
	 */
	public BeanReferenceJDBCConnectionFactory(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Acquires and returns a JDBC Connection
	 * @return A JDBC Connection
	 * @throws JDBCConnectionFactoryException
	 * @see org.helios.collectors.jdbc.connection.IJDBCConnectionFactory#getJDBCConnection()
	 */
	public Connection getJDBCConnection() throws JDBCConnectionFactoryException {
		if(dataSource==null) throw new JDBCConnectionFactoryException("Injected DataSource Was Null");
		try {
			return dataSource.getConnection();
		} catch (Exception e) {
			throw new JDBCConnectionFactoryException("Failed to get connection from datasource", e);
		}
	}
	
	/**
	 * Acquires and returns a JDBC Connection, requiring credentials.
	 * @param userName The username to associate with the connection.
	 * @param password The password to associate with the connection.
	 * @return A JDBC Connection.
	 * @throws JDBCConnectionFactoryException
	 */
	public Connection getJDBCConnection(String userName, String password) throws JDBCConnectionFactoryException {
		if(dataSource==null) throw new JDBCConnectionFactoryException("Injected DataSource Was Null");
		try {
			return dataSource.getConnection(userName, password);
		} catch (Exception e) {
			throw new JDBCConnectionFactoryException("Failed to get connection from datasource", e);
		}		
	}
	

	/**
	 * No Op for this class.
	 * @param configuration
	 * @see org.helios.collectors.jdbc.connection.IJDBCConnectionFactory#setConfiguration(java.util.Map)
	 */
	public void setConfiguration(Map<String, String> configuration) {}

	/**
	 * Sets the data source to be used by this factory.
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

}
