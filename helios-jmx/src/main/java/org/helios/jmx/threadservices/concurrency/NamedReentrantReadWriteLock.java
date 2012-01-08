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
package org.helios.jmx.threadservices.concurrency;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>Title: NamedReentrantReadWriteLock</p>
 * <p>Description: An extension of ReadWriteLock that defines a custom name and event broadcasting (not implemented yet). </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class NamedReentrantReadWriteLock extends ReentrantReadWriteLock {
	/** Serial UID */
	private static final long serialVersionUID = 4444396729619461011L;
	/** the name of the lock */
	protected String lockName = null;
	/** A unique serial number generator for default names. */ 
	protected static AtomicLong serialNumber = new AtomicLong(0);
	/** The prefix for the default name */
	public static final String PREFIX = "NamedReentrantReadWriteLock#";
	/** the named underlying readLock */
	protected NamedReadLock readLock = null; 
	/** the named underlying writeLock */
	protected NamedWriteLock writeLock = null; 
	
	/**
	 * Creates a new NamedReentrantReadWriteLock with a default name and default ordering properties.
	 */
	public NamedReentrantReadWriteLock() {
		super();
		lockName = PREFIX + serialNumber.incrementAndGet();
		initSubLocks();
	}
	
	/**
	 * Creates a new NamedReentrantReadWriteLock with the provided name and default ordering properties.
	 * @param name The name of the lock.
	 */
	public NamedReentrantReadWriteLock(String name) {
		super();
		lockName = name;
		initSubLocks();
	}
	
	/**
	 * Initialized the sub locks.
	 */
	protected void initSubLocks() {
		readLock = new NamedReadLock(this);
		writeLock = new NamedWriteLock(this);
	}
	

	/**
	 * Creates a new NamedReentrantReadWriteLock with a default name and the given fairness policy.  
	 * @param fair true if this lock should use a fair ordering policy
	 */
	public NamedReentrantReadWriteLock(boolean fair) {
		super(fair);
		lockName = PREFIX + serialNumber.incrementAndGet();
	}
	
	/**
	 * Creates a new NamedReentrantReadWriteLock with the given name and the given fairness policy.
	 * @param fair true if this lock should use a fair ordering policy
	 * @param name The name of the lock.
	 */
	public NamedReentrantReadWriteLock(boolean fair, String name) {
		super(fair);
		lockName = name;
	}	
	
	/**
	 * The name assigned to this lock.
	 * @return The name of the lock.
	 */
	public String getName() {
		return lockName;
	}
	
	/**
	 * Returns a string identifying this lock and name, as well as its lock state. 
	 * @return a string identifying this lock and name, as well as its lock state.
	 * @see java.util.concurrent.locks.ReentrantReadWriteLock#toString()
	 */
	public String toString() {
		return lockName + "/" + super.toString();
	}

	/**
	 * <p>Title: NamedWriteLock</p>
	 * <p>Description: An extension of ReentrantReadWriteLock.WriteLock that defines a custom name</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 * @version $LastChangedRevision$
	 * $HeadURL$
	 * $Id$
	 */
	public static class NamedWriteLock extends ReentrantReadWriteLock.WriteLock {
		/** Serial UID */
		private static final long serialVersionUID = -2974141263992452283L;
		/** The prefix for the default name */
		public static final String SUFFIX = "NamedWriteLock";
		/** the name of the lock */
		protected String lockName = null;
		

		/**
		 * Protected constructor for a new NamedWriteLock with a default name.
		 * @param readWriteLock the outer lock object 
		 */
		protected NamedWriteLock(NamedReentrantReadWriteLock readWriteLock) {
			super(readWriteLock);
			lockName = readWriteLock.getName() + SUFFIX;
		}
		

		/**
		 * The name assigned to this lock.
		 * @return The name of the lock.
		 */
		public String getName() {
			return lockName;
		}			
		
		/**
		 * Returns a string identifying this lock and name, as well as its lock state. 
		 * @return a string identifying this lock and name, as well as its lock state.
		 * @see java.util.concurrent.locks.ReentrantReadWriteLock#toString()
		 */
		public String toString() {
			return lockName + "/" + super.toString();
		}		
	}
	
	/**
	 * <p>Title: NamedReadLock</p>
	 * <p>Description: An extension of ReentrantReadWriteLock.ReadLock that defines a custom name </p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 * @version $LastChangedRevision$
	 * $HeadURL$
	 * $Id$
	 */
	public static class NamedReadLock extends ReentrantReadWriteLock.ReadLock {
		/** Serial UID */
		private static final long serialVersionUID = 9038418436019755903L;
		/** The prefix for the default name */
		public static final String SUFFIX = "NamedReadLock";
		/** the name of the lock */
		protected String lockName = null;
		

		/**
		 * Protected constructor for a new NamedReadLock with a default name.
		 * @param readWriteLock the outer lock object 
		 */
		protected NamedReadLock(NamedReentrantReadWriteLock readWriteLock) {
			super(readWriteLock);
			lockName = readWriteLock.getName() + SUFFIX;
		}
		
		/**
		 * The name assigned to this lock.
		 * @return The name of the lock.
		 */
		public String getName() {
			return lockName;
		}	
		
		/**
		 * Returns a string identifying this lock and name, as well as its lock state. 
		 * @return a string identifying this lock and name, as well as its lock state.
		 * @see java.util.concurrent.locks.ReentrantReadWriteLock#toString()
		 */
		public String toString() {
			return lockName + "/" + super.toString();
		}		
	}
	

}


