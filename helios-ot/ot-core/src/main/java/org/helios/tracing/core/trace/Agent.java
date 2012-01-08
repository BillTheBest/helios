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
 * <p>Title: Agent</p>
 * <p>Description: TraceModel representation of the originating agent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.Agent</code></p>
 */
@Store(store="AGENT")
public class Agent implements Serializable {
	/**  */
	private static final long serialVersionUID = 5646335996649035053L;
	/** The unique Helios agent id */
	protected long id;
	/** The originating host */
	protected Host host;
	/** The agent name */
	protected String name;
	/** The date this agent first connected */
	protected Date firstConnected;
	/** The most recent date this agent connected */
	protected Date lastConnected;
	/** Indicates if the agent is currently connected */
	protected boolean connected = false;
	
	/** The sequence name for the agent id */
	public static final String SEQUENCE_KEY = "HELIOS_AGENT_ID";
	
	
	
	/**
	 * Creates a new agent, assumed to be connected 
	 * @param id The designated agent id
	 * @param host The host where the agent runs
	 * @param name The name of the agent
	 */
	public Agent(long id, Host host, String name) {
		super();
		this.id = id;
		this.host = host;
		this.name = name;
		Date dt = new Date();
		this.firstConnected = dt;
		this.lastConnected = dt;
		this.connected = true;
	}
	
	/**
	 * Creates a new Agent from a result set and the sql <code>SELECT AGENT_ID,FIRST_CONNECTED,HOST_ID,LAST_CONNECTED,NAME FROM AGENT</code>. 
	 * @param rset The pre-navigated result set
	 * @param cache the trace mode cache where the host will be retrieved from 
	 * @throws SQLException 
	 */
	public Agent(ResultSet rset, TraceModelCache cache) throws SQLException {
		int i = 0;
		id = rset.getLong(++i);
		firstConnected = new Date(rset.getTimestamp(++i).getTime());
		i++;
		lastConnected = new Date(rset.getTimestamp(++i).getTime());
		name = rset.getString(++i);
		this.host = cache.getHost(id);
		cache.putAgent(this);
		cache.addValue("Agent", id);
	}
	
	
	// SELECT AGENT_ID,FIRST_CONNECTED,HOST_ID,LAST_CONNECTED,NAME FROM AGENT
	
	/**
	 * The unique Helios agent id
	 * @return the id
	 */
	@PK
	@StoreField(name="AGENT_ID", type=Types.BIGINT)
	public long getId() {
		return id;
	}
	/**
	 * The agent name 
	 * @return the name
	 */
	@StoreField(name="NAME", type=Types.VARCHAR)
	public String getName() {
		return name;
	}
	
	/**
	 * The originating host
	 * @return the host
	 */
	@StoreField(name="HOST_ID", type=Types.BIGINT, fk=true)
	public Host getHost() {
		return host;
	}
	/**
	 * The date this agent first connected
	 * @return the firstConnected
	 */
	@StoreField(name="FIRST_CONNECTED", type=Types.TIMESTAMP)
	public Date getFirstConnected() {
		return firstConnected;
	}
	/**
	 * The most recent date this agent connected
	 * @return the lastConnected
	 */
	@StoreField(name="LAST_CONNECTED", type=Types.TIMESTAMP)
	public Date getLastConnected() {
		return lastConnected;
	}
	/**
	 * Indicates if the agent is currently connected
	 * @return true if the agent is currently connected, false if not.
	 */
	public boolean isConnected() {
		return connected;
	}
	/**
	 * Sets the connected state. If the current state is false, but being set to true,
	 * the last connected date will be set to current.
	 * @param true to set the state to connected
	 */
	public void setConnected(boolean connected) {
		if(!this.connected) {
			if(connected) {
				lastConnected = new Date();				
			}
		}
		this.connected = connected;
	}
	/**
	 * Constructs a <code>String</code> with all attributes in <code>name:value</code> format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder();    
	    retValue.append("Agent [")
		    .append(TAB).append("id:").append(this.id)
		    .append(TAB).append("name:").append(this.name)
		    .append(TAB).append("host:").append(this.host.fqn)
		    .append(TAB).append("connected:").append(this.connected)		
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
		Agent other = (Agent) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	
	
	

}
