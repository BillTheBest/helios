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
package org.helios.collectors.jmx;

import java.util.NoSuchElementException;

import javax.management.MBeanServerConnection;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: MBeanServerConnectionPool</p>
 * <p>Description: A simple pool for MBeanServerConnections to field MBeanServerConnection proxy requests </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.collectors.jmx.MBeanServerConnectionPool</code></p>
 */

@JMXManagedObject(annotated=true, declared=true)
public class MBeanServerConnectionPool implements ObjectPool {
	/** The actual pool implementation */
	protected final GenericObjectPool objectPool;
	/** The MBeanServerConnection instance provider to populate the pool */
	protected final IMBeanServerConnectionFactory connectionFactory;
	
	public MBeanServerConnectionPool() {
		objectPool = null;
		connectionFactory = null;
	}
	
	/**
	 * Creates a new MBeanServerConnectionPool for the passed connection factory
	 * @param connectionFactory the MBeanServerConnection provider to populate the pool
	 */
	public MBeanServerConnectionPool(IMBeanServerConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		objectPool = new GenericObjectPool(this.connectionFactory);
	}
	
//	public MBeanServerConnectionPool() {
//		this.connectionFactory = null;
//		objectPool = null;
//		
//	}


	/**
	 * Borrows an MBeanServerConnection from the pool
	 * @return an MBeanServerConnection
	 * @throws Exception
	 * @throws NoSuchElementException
	 * @throws IllegalStateException
	 */
	@Override
	public MBeanServerConnection borrowObject() throws Exception, NoSuchElementException, IllegalStateException {
		return (MBeanServerConnection)objectPool.borrowObject();
	}
	
	/**
	 * Returns the number of active MBeanServerConnections in the pool
	 * @return the number of active MBeanServerConnections
	 * @throws UnsupportedOperationException
	 */
	@Override
	@JMXAttribute(name="NumActive", description="The number of active MBeanServerConnections in the pool", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getNumActive() throws UnsupportedOperationException {
		return objectPool.getNumActive();
	}
	
	/**
	 * Returns the maximum number of active MBeanServerConnections allowed in the pool
	 * @return the maximum number of active MBeanServerConnections
	 * @throws UnsupportedOperationException
	 */
	@JMXAttribute(name="MaxActive", description="The maximum number of active MBeanServerConnections allowed in the pool", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getMaxActive() throws UnsupportedOperationException {
		return objectPool==null ? -1 : objectPool.getMaxActive();
	}
	
	/**
	 * Sets the maximum number of active MBeanServerConnections allowed in the pool
	 * @param max the maximum number of active MBeanServerConnections allowed in the pool
	 */
	public void setMaxActive(int max) {
		if(objectPool!=null) objectPool.setMaxActive(max);
	}

	/**
	 * Returns the maximum number of idle MBeanServerConnections allowed in the pool
	 * @return the maximum number of idle  MBeanServerConnections
	 * @throws UnsupportedOperationException
	 */
	@JMXAttribute(name="MaxIdle", description="The maximum number of idle MBeanServerConnections allowed in the pool", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getMaxIdle() throws UnsupportedOperationException {
		return objectPool==null ? -1 : objectPool.getMaxIdle();
	}
	
	/**
	 * Sets the maximum number of idle MBeanServerConnections allowed in the pool
	 * @param max the maximum number of idle MBeanServerConnections allowed in the pool
	 */
	public void setMaxIdle(int max) {
		if(objectPool!=null) objectPool.setMaxIdle(max);
	}
	
	
	/**
	 * Returns the minimum number of idle MBeanServerConnections allowed in the pool
	 * @return the minimum number of idle  MBeanServerConnections
	 * @throws UnsupportedOperationException
	 */
	@JMXAttribute(name="MinIdle", description="The minimum number of idle MBeanServerConnections allowed in the pool", mutability=AttributeMutabilityOption.READ_WRITE)
	public int getMinIdle() throws UnsupportedOperationException {
		return objectPool==null ? -1 : objectPool.getMinIdle();
	}

	/**
	 * Sets the minimum number of idle MBeanServerConnections allowed in the pool
	 * @param min the minimum number of idle MBeanServerConnections allowed in the pool
	 */
	public void setMinIdle(int min) {
		if(objectPool!=null) objectPool.setMinIdle(min);
	}
	
	/**
	 * Returns the maximum wait time to get an MBeanServerConnections from the pool (ms)
	 * @return the maximum wait time to get an MBeanServerConnections from the pool (ms)
	 * @throws UnsupportedOperationException
	 */
	@JMXAttribute(name="MaxWait", description="the maximum wait time to get an MBeanServerConnections from the pool (ms)", mutability=AttributeMutabilityOption.READ_WRITE)
	public long getMaxWait() throws UnsupportedOperationException {
		return objectPool==null ? -1 : objectPool.getMaxWait();
	}
	
	/**
	 * Sets the maximum wait time to get an MBeanServerConnections from the pool (ms)
	 * @param max the maximum wait time to get an MBeanServerConnections from the pool (ms)
	 */
	public void setMaxWait(long max) {
		if(objectPool!=null) objectPool.setMaxWait(max);
	}
	
	

	/**
	 * Returns the number of idle MBeanServerConnections in the pool
	 * @return the number of idle MBeanServerConnections in the pool
	 * @throws UnsupportedOperationException
	 */
	@Override
	@JMXAttribute(name="NumIdle", description="The number of idle MBeanServerConnections in the pool", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getNumIdle() throws UnsupportedOperationException {
		return objectPool.getNumIdle();
	}
	
	
	/**
	 * Returns an MBeanServerConnection to the pool
	 * @param obj an MBeanServerConnection
	 * @throws Exception
	 */
	@Override
	public void returnObject(Object obj) throws Exception {
		objectPool.returnObject(obj);
	}
	



	/**
	 * Closes the pool after which connections will not be available.
	 * @throws Exception
	 */
	@Override
	@JMXOperation(name="close", description="Closes the pool after which connections will not be available.")
	public void close() throws Exception {
		objectPool.close();
		
	}


	/**
	 * @param obj
	 * @throws Exception
	 */
	@Override
	public void invalidateObject(Object obj) throws Exception {
		objectPool.invalidateObject(obj);
		
	}

	/**
	 * @throws Exception
	 * @throws IllegalStateException
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void addObject() throws Exception, IllegalStateException, UnsupportedOperationException {
		objectPool.addObject();		
	}

	/**
	 * No Op
	 * @param factory
	 * @throws IllegalStateException
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void setFactory(PoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException {
		
	}

	/**
	 * Clears any connections sitting idle in the pool by removing them from the idle instance pool 
	 * @see org.apache.commons.pool.impl.GenericObjectPool#clear()
	 */
	@JMXOperation(name="clear", description="Clears any connections sitting idle in the pool by removing them from the idle instance pool.")
	public void clear() {
		objectPool.clear();
	}

	/**
	 * Sets the test on borrow option
	 * @param testOnBorrow if true, connections will be validated when borrowed from the pool
	 * @see org.apache.commons.pool.impl.GenericObjectPool#setTestOnBorrow(boolean)
	 */
	
	public void setTestOnBorrow(boolean testOnBorrow) {
		if(objectPool!=null) objectPool.setTestOnBorrow(testOnBorrow);
	}
	
	/**
	 * When true, connections will be validated before being returned by the borrowObject() method.
	 * @return true if connections are validated before being borrowed.
	 */
	@JMXAttribute(name="TestOnBorrow", description="When true, connections will be validated before being returned by the borrowObject() method.", mutability=AttributeMutabilityOption.READ_WRITE)
	public boolean getTestOnBorrow() {
		return objectPool==null ? false : objectPool.getTestOnBorrow();
	}

	/**
	 * Sets the number of milliseconds to sleep between runs of the idle object evictor thread.
	 * @param timeBetweenEvictionRunsMillis
	 * @see org.apache.commons.pool.impl.GenericObjectPool#setTimeBetweenEvictionRunsMillis(long)
	 */
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		if(objectPool!=null) objectPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
	}
	
	/**
	 * Returns the number of milliseconds to sleep between runs of the idle object evictor thread.
	 * @return the number of milliseconds to sleep between runs of the idle object evictor thread.
	 */
	@JMXAttribute(name="TimeBetweenEvictionRunsMillis", description="The number of milliseconds to sleep between runs of the idle object evictor thread.", mutability=AttributeMutabilityOption.READ_WRITE)
	public long getTimeBetweenEvictionRunsMillis() {
		return objectPool==null ? -1 : objectPool.getTimeBetweenEvictionRunsMillis();
	}
}
