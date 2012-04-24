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
package org.helios.ot.endpoint;

import java.util.Collection;
import java.util.Random;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.helios.jmx.dynamic.annotations.JMXManagedObject;
//import org.helios.jmx.threadservices.scheduling.HeliosScheduler;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: HSQLDBEndpoint</p>
 * <p>Description: A concrete implementation against HSQL DB endpoint - this class is 
 *    only used for testing.  It creates TRACE table with ID, Metric and value columns
 *    and then for each processTrace callback - it inserts one record for metric CPU 
 *    with random;y generated value between 0 and 100.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
@JMXManagedObject (declared=false, annotated=true)
public class HSQLDBEndpoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T> implements Runnable{

	private static final long serialVersionUID = 2260146643500467998L;
	protected Connection conn = null; 
	protected String hsqldbDriverClass = "org.hsqldb.jdbcDriver";
	//jdbc:hsqldb:hsql://localhost/ -> for standalone DB server running on default port 9001
	protected String dbUrl = null;
	protected String user = null;
	protected String password = null;
	private Random randomGenerator = new Random();
	
	protected void connectImpl() throws EndpointConnectException {
        try {
			Class.forName(hsqldbDriverClass);
			conn = DriverManager.getConnection(dbUrl,user,password);
			createTraceTable();
		} catch (ClassNotFoundException e) {
			log.error(e);
			throw new EndpointConnectException(e);
		}catch (SQLException e) {
			log.error(e);
			throw new EndpointConnectException(e);
		}                      
		
	}

	protected void disconnectImpl() {
        Statement st = null;
        try {
			st = conn.createStatement();
			st.executeUpdate("shutdown");   
			st.close();
		} catch (SQLException e) {
			log.error(e);
		}    
         
       
	}

	
	/**
	 * For testing purpose, we will ignore the incoming traces but will populate one
	 * record with CPU value randomly generated between 1 and 100
	 * It also prints the number of records after insert for debugging purpose
	 */	
	@Override
	public void processTraces(TraceCollection<T> traceCollection) throws Exception {
        try {
			update("INSERT INTO Trace(metric,value) VALUES('CPU', "+randomGenerator.nextInt(101)+")");
			query("select count(*) from trace");
		} catch (java.sql.SQLTransientConnectionException e) {
			log.error("Connection error...");
			throw new EndpointConnectException(e);
		} catch( SQLException hex){
			log.error(hex);
			throw new EndpointTraceException(hex);
		}		
	}
	
	protected boolean processTracesImpl(TraceCollection<T> traces) throws EndpointConnectException, EndpointTraceException {
		return true;
	}


	
    private synchronized void update(String expression) throws SQLException {
        Statement st = null;
        st = conn.createStatement();    
        int i = st.executeUpdate(expression);    
        if (i == -1) {
            log.error("db error : " + expression);
        }
        st.close();
    }    

    private synchronized void query(String expression) throws SQLException {
        Statement st = null;
        ResultSet rs = null;
        st = conn.createStatement();
        rs = st.executeQuery(expression);    
        for (; rs.next(); ) {
            for (int i = 0; i <= rs.getFetchSize(); i++) {
            	Object o = rs.getObject(i + 1);
            	log.error(o.toString() + " ");
            }
        }
        st.close();    
    }
    
    /** 
     * For testing purpose, a table TRACE will be created if it doesn't exist
     */
	private void createTraceTable() {
        try {
            update("CREATE TABLE Trace ( id INTEGER IDENTITY, metric VARCHAR(256), value INTEGER)");
        } catch (SQLException ex2) {
        	log.trace("Ignore the exception as it's thrown because TRACE table already exist.");
        }		
	}

	/**
	 * @return the dbUrl
	 */
	public String getDbUrl() {
		return dbUrl;
	}

	/**
	 * @param dbUrl the dbUrl to set
	 */
	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/** 
	 * A method introduced just for testing purpose which will be scheduled 
	 * to mimic callback from EndPointBatchConsumer to process traces
	 */
	public void run() {
		// for now the Collection of traces is not used as we only insert
		// one metric per processTrace callback
		//processTraces(null);
		//HeliosScheduler hScheduler = HeliosScheduler.getInstance();
	}

	@Override
	public org.helios.ot.endpoint.AbstractEndpoint.Builder newBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	
//	public static void main(String[] args){
//		HSQLDBEndpoint endpoint = new HSQLDBEndpoint();
//		endpoint.connect();
//		//endpoint.createTraceTable();
//		try {
//			endpoint.processTracesImpl(null);
//		} catch (EndpointConnectException e) {
//			e.printStackTrace();
//		} catch (EndpointTraceException e) {
//			e.printStackTrace();
//		} 
//		//endpoint.disconnect();
//	}
    
}
