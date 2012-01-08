/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2011, Helios Development Group and individual contributors
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
package org.helios.tracing.core.trace;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.helios.tracing.core.trace.annotations.persistence.PK;
import org.helios.tracing.core.trace.annotations.persistence.Store;
import org.helios.tracing.core.trace.annotations.persistence.StoreField;
import org.helios.tracing.core.trace.cache.TraceModelCache;

/**
 * <p>Title: Host</p>
 * <p>Description: TraceModel representation of the originating host.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.Host</code></p>
 */
@Store(store="HOST")
public class Host implements Serializable {
	/**  */
	private static final long serialVersionUID = -1569932172304794673L;
	/** The unique Helios host id */
	protected long id;
	/** The simple host name */
	protected String name;
	/** The host's IP address */
	protected String ip;
	/** The host's fully qualified name */
	protected String fqn;
	/** The date this host first connected */
	protected Date firstConnected;
	/** The most recent date this host connected */
	protected Date lastConnected;

	/** The sequence name for the host id */
	public static final String SEQUENCE_KEY = "HELIOS_HOST_ID";
	
	/**
	 * Creates a new Host 
	 * @param id
	 * @param name
	 * @param ip
	 * @param fqn
	 */
	public Host(long id, String name, String ip, String fqn) {
		super();
		this.id = id;
		this.name = name;
		this.ip = ip;
		this.fqn = fqn;
		Date dt = new Date();
		this.firstConnected = dt;
		this.lastConnected = dt;
	}
	
	/**
	 * Creates a new Host from a result set and the sql <code>SELECT FIRST_CONNECTED,FQN,HOST_ID,IP,LAST_CONNECTED,NAME FROM HOST</code>. 
	 * @param rset The pre-navigated result set
	 * @param cache Updates the persisted indicator
	 * @throws SQLException 
	 */
	public Host(ResultSet rset, TraceModelCache cache) throws SQLException {
		int i = 0;		
		firstConnected = new Date(rset.getTimestamp(++i).getTime());
		fqn = rset.getString(++i);
		id = rset.getLong(++i);
		ip = rset.getString(++i);
		lastConnected = new Date(rset.getTimestamp(++i).getTime());
		name = rset.getString(++i);
		cache.putHost(this);
		cache.addValue("Host", id);
	}
	
	
	/**
	 * The unique Helios host id
	 * @return the id
	 */
	@PK
	@StoreField(name="HOST_ID", type=Types.BIGINT)
	public long getId() {
		return id;
	}
	/**
	 * The simple host name 
	 * @return the name
	 */
	@StoreField(name="NAME", type=Types.VARCHAR)
	public String getName() {
		return name;
	}
	/**
	 * he host's IP address
	 * @return the ip
	 */
	@StoreField(name="IP", type=Types.VARCHAR)
	public String getIp() {
		return ip;
	}
	/**
	 * The host's fully qualified name 
	 * @return the fqn
	 */
	@StoreField(name="FQN", type=Types.VARCHAR)
	public String getFqn() {
		return fqn;
	}
	/**
	 * The date this host first connected
	 * @return the firstConnected
	 */
	@StoreField(name="FIRST_CONNECTED", type=Types.TIMESTAMP)
	public Date getFirstConnected() {
		return firstConnected;
	}
	/**
	 * The most recent date this host connected
	 * @return the lastConnected
	 */
	@StoreField(name="LAST_CONNECTED", type=Types.TIMESTAMP)
	public Date getLastConnected() {
		return lastConnected;
	}
	/**
	 * Constructs a <code>String</code> with all attributes in <code>name:value</code> format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder();    
	    retValue.append("Host [")
		    .append(TAB).append("id:").append(this.id)
		    .append(TAB).append("name:").append(this.name)
		    .append(TAB).append("ip:").append(this.ip)
		    .append(TAB).append("fqn:").append(this.fqn)
		    .append(TAB).append("firstConnected:").append(this.firstConnected)
		    .append(TAB).append("lastConnected:").append(this.lastConnected)
	    	.append("\n]");    
	    return retValue.toString();
	}


	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}


	/**
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Host other = (Host) obj;
		if (id != other.id)
			return false;
		return true;
	}
	

	
}
