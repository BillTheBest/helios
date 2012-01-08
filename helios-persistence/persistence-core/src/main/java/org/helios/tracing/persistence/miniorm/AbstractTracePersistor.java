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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.StringHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.tracing.persistence.miniorm.TraceObjectIntrospector.TraceObjectMetaData;



/**
 * <p>Title: AbstractTracePersistor</p>
 * <p>Description: An abstract trace persistor.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.persistence.miniorm.AbstractTracePersistor</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public abstract class AbstractTracePersistor extends ManagedObjectDynamicMBean implements ITracePersistor {
	/**  */
	private static final long serialVersionUID = 2546569987847715728L;
	/** The generated sql for the insert */
	protected final String insertSql;
	/** The generated sql for the select */
	protected final String selectSql;
	/** The generated sql for the select count */
	protected final String selectCountSql;
	
	/** The class the persistor was created for */
	protected final Class<?> clazz;
	/** The class persistor meta-data */
	protected final TraceObjectMetaData metaData;
	
	/** The insert sql template */
	public static final String INSERT_SQL_FORMAT = "INSERT INTO {0} ({1}) VALUES ({2})";

	/** The insert counter */
	protected final AtomicLong insertCount = new AtomicLong(0L);
	/** The exception counter */
	protected final AtomicLong exceptionCount = new AtomicLong(0L);
	
	/** The last reset date */
	protected final AtomicLong lastReset = new AtomicLong(0L);
	/** The total execution time */
	protected final AtomicLong totalExecutionTime = new AtomicLong(0L);
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/**
	 * Creates a new AbstractTracePersistor
	 * @param metaData The class meta data
	 */
	public AbstractTracePersistor(Class<?> clazz) {		
		if(log.isDebugEnabled()) log.debug("Creating Persistor for [" + clazz.getName() + "]");
		this.clazz = clazz;
		this.metaData = TraceObjectIntrospector.metaData(this.clazz);
		int columnCount = metaData.getColumnNames().size();
		String columns = StringHelper.fastConcatAndDelim(",", metaData.getColumnNames().toArray(new String[columnCount]));
		String[] binds = new String[columnCount];
		Arrays.fill(binds, "?");
		insertSql = MessageFormat.format(INSERT_SQL_FORMAT, metaData.getTableName(), StringHelper.fastConcatAndDelim(",", metaData.getColumnNames().toArray(new String[columnCount])), StringHelper.fastConcatAndDelim(",", binds));
		selectSql = "SELECT " + columns + " FROM " + metaData.getTableName();
		selectCountSql = "SELECT COUNT(*) FROM " + metaData.getTableName();
		try {
			ObjectName on = JMXHelper.objectName("org.helios.tracing.persistence:service=Persistor,type=" + getClass().getSimpleName());
			MBeanServer server = JMXHelper.getHeliosMBeanServer();
			if(server.isRegistered(on)) {
				try { server.unregisterMBean(on); } catch (Exception e) {}				
			}
			this.reflectObject(this);
			server.registerMBean(this, on);
		} catch (Exception e) {
			log.warn("Failed to create JMX interface for [" + getClass().getName() + "]. Continuing without.", e);
		}
	}
	
	
	
	/**
	 * Executes a JDBC insert for the passed trace Object
	 * @param traceObject The object to be inserted
	 * @param conn  The JDBC connection
	 * @throws SQLException
	 */
	public void doInsert(Object traceObject, Connection conn) throws SQLException {
		long start = System.currentTimeMillis();
		PreparedStatement ps = null;
		try {					
			ps = conn.prepareStatement(insertSql);
			doBinds(traceObject, ps);
			ps.executeUpdate();
			long elapsed = System.currentTimeMillis()-start;
			insertCount.incrementAndGet();
			totalExecutionTime.addAndGet(elapsed);
		} catch (SQLException e) {
			exceptionCount.incrementAndGet();
			log.warn("Unexpected exception executing [" + insertSql + "]", e);
			throw e;
		} catch (Exception e) {
			exceptionCount.incrementAndGet();
			log.warn("Unexpected exception executing [" + insertSql + "]", e);
			throw new RuntimeException("Unexpected exception executing [" + insertSql + "]", e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Binds the insert statement bind variables
	 * @param traceObject The tracing object providing the values
	 * @param statement The prepared statement
	 */
	protected abstract void doBinds(Object traceObject, PreparedStatement statement);
	
	/**
	 * Returns a compiled prepared statement to load all items from the DB
	 * @param conn A DB connection
	 * @return a compiled prepared statement
	 * @throws SQLException
	 */
	public PreparedStatement getLoadStatement(Connection conn) throws SQLException {
		return conn.prepareStatement(selectSql);
	}
	
	/**
	 * Returns the number of items in the DB
	 * @param conn A JDBC connection
	 * @return the number of items in the DB
	 */
	public long getCount(Connection conn) {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(selectCountSql);
			rset = ps.executeQuery();
			if(!rset.next()) throw new SQLException("No rows returned for count query [" + selectCountSql + "]");
			return rset.getLong(1);
		} catch (Exception e) {
			log.warn("Failed to get count for [" + clazz.getSimpleName() + "]", e);
			return -1L;
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Executes a batched JDBC insert for the passed trace Object
	 * @param traceObject The object to be inserted
	 * @param statement  The JDBC prepared statement
	 * @throws SQLException
	 */
	public void doInsert(Object traceObject, PreparedStatement statement) throws SQLException {
		try {
			long start = System.currentTimeMillis();
			doBinds(traceObject, statement);
			statement.addBatch();
			long elapsed = System.currentTimeMillis()-start;
			insertCount.incrementAndGet();
			totalExecutionTime.addAndGet(elapsed);
		} catch (SQLException e) {
			exceptionCount.incrementAndGet();
			log.warn("Unexpected exception executing [" + insertSql + "]", e);
			throw e;
		} catch (Exception e) {
			exceptionCount.incrementAndGet();
			log.warn("Unexpected exception executing [" + insertSql + "]", e);
			throw new RuntimeException("Unexpected exception executing [" + insertSql + "]", e);		
		}
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("AbstractTracePersistor [")
	        .append(TAB).append("insertSql = ").append(this.insertSql)
	        .append(TAB).append("clazz = ").append(this.clazz)
	        .append(TAB).append("metaData = ").append(this.metaData)
	        .append("\n]");    
	    return retValue.toString();
	}



	/**
	 * Returns the configured SQL insert statement
	 * @return the configured SQL insert statement
	 */
	@JMXAttribute(name="InsertSql", description="The configured SQL insert statement", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getInsertSql() {
		return insertSql;
	}

	/**
	 * Returns the configured SQL select statement
	 * @return the configured SQL select statement
	 */
	@JMXAttribute(name="SelectSql", description="The configured SQL select statement", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getSelectSql() {
		return selectSql;
	}
	
	/**
	 * Returns the configured SQL select count statement
	 * @return the configured SQL select count statement
	 */
	@JMXAttribute(name="SelectCountSql", description="The configured SQL select count statement", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getSelectCountSql() {
		return selectCountSql;
	}
	
	/**
	 * Returns the total insert count since the last reset
	 * @return the total insert count since the last reset
	 */
	@JMXAttribute(name="InsertCount", description="The total insert count since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getInsertCount() {
		return insertCount.get();
	}
	
	/**
	 * Returns the total exception count since the last reset
	 * @return the total exception count since the last reset
	 */
	@JMXAttribute(name="ExceptionCount", description="The total exception count since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getExceptionCount() {
		return exceptionCount.get();
	}
	
	
	/**
	 * Returns the total cummulative insert time for all executed inserts (ms.)
	 * @return the total cummulative insert time for all executed inserts (ms.)
	 */
	@JMXAttribute(name="TotalExecutionTime", description="The total cummulative insert time for all executed inserts (ms.)", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTotalExecutionTime() {
		return totalExecutionTime.get();
	}
	
	/**
	 * Returns the average insert time for all executed inserts since the last reset (ms.)
	 * @return the average insert time for all executed inserts since the last reset (ms.)
	 */
	@JMXAttribute(name="AverageExecutionTime", description="The average insert time for all executed inserts since the last reset (ms.)", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAverageExecutionTime() {
		return avg(totalExecutionTime.get(), insertCount.get());
	}
	
	/**
	 * Returns the persistor class name
	 * @return the persistor class name
	 */
	@JMXAttribute(name="PersistorClassName", description="The persistor class name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getPersistorClassName() {
		return this.getClass().getName();
	}
	
	
	/**
	 * Returns the timestamp of the last metrics reset
	 * @return the timestamp of the last metrics reset
	 */
	@JMXAttribute(name="LastReset", description="timestamp of the last metrics reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getLastReset() {
		long time = lastReset.get();
		if(time==0) return null;
		return new Date(time);
	}
	
	/**
	 * Resets the counters
	 */
	@JMXOperation(name="reset", description="Resets the counters")
	public void reset() {
		lastReset.set(System.currentTimeMillis());
		insertCount.set(0L);
		totalExecutionTime.set(0L);
		exceptionCount.set(0L);
	}
	
	
	/**
	 * Calculates the average
	 * @param total
	 * @param count
	 * @return
	 */
	protected static long avg(float total, float count) {
		float f = total/count;
		return (long)f;
	}

}
