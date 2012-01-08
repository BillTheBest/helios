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
package org.helios.esper.engine;

import java.io.Serializable;
import java.util.Map;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

import org.apache.log4j.Logger;

/**
 * <p>Title: AgentName</p>
 * <p>Description: Catalog item for connected tracing Agents</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.AgentName</code></p>
 */

public class AgentName extends CompositeDataSupport implements Serializable {

	/**  */
	private static final long serialVersionUID = -9163859436814919189L;
	/**  */
	public static final String[] ITEM_NAMES = {"host", "agent", "connectTime", "touchTime"};
	/**  */
	public static final String[] ITEM_DESCS = {"The host name", "The application name", "The time the agent connected", "The time the agent last communicated"};
	/**  */
	public static final OpenType[] ITEM_TYPES = {SimpleType.STRING, SimpleType.STRING, SimpleType.LONG, SimpleType.LONG};
	/**  */
	public static final Logger log = Logger.getLogger(MetricName.class);
	/**  */
	public static final TabularType AGENT_NAME_TABULAR_TYPE = getTabularType();
	
	/**  */
	public static final String HOST_NAME = ITEM_NAMES[0];
	/**  */
	public static final String AGENT_NAME = ITEM_NAMES[1];	
	/**  */
	public static final String CONNECT_TIME = ITEM_NAMES[2];
	/**  */
	public static final String TOUCH_TIME = ITEM_NAMES[3];


	/** The host name */
	protected final String host;
	/** The agent name */
	protected final String agent;
	/** The connect time */
	protected final long connectTime;
	/** The last touch time */
	protected long touchTime;
	
	/**
	 * Creates a new AgentName
	 * @param nameValue
	 * @throws OpenDataException
	 */
	public AgentName(Map<String, Object> nameValue) throws OpenDataException {
		super(getType(), ITEM_NAMES, new Object[]{nameValue.get(HOST_NAME).toString(), nameValue.get(AGENT_NAME).toString(), (Long)nameValue.get(CONNECT_TIME), (Long)nameValue.get(CONNECT_TIME)});
		host = nameValue.get(HOST_NAME).toString();
		agent = nameValue.get(AGENT_NAME).toString();
		connectTime = (Long)nameValue.get(CONNECT_TIME);
		touchTime = connectTime;
	}
	
	


	/**
	 * Creates a new AgentName
	 * @param host The agent's host
	 * @param agent The agent's agent name
	 * @param connectTime The time the agent connected
	 * @throws OpenDataException
	 */
	public AgentName(String host, String agent, long connectTime) throws OpenDataException {
		super(getType(), ITEM_NAMES, new Object[]{host, agent, connectTime, connectTime});
		this.host = host;
		this.agent = agent;
		this.connectTime = connectTime;
		this.touchTime = connectTime;
	}

	/**
	 * Creates a composite type for this class
	 * @return a composite type
	 */
	public static CompositeType getType() {
		try {
			return new CompositeType("AgentName", "A unique Helios agent name", ITEM_NAMES, ITEM_DESCS, ITEM_TYPES);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create CompositeType for AgentName", e);
		}
	}
	
	/**
	 * Creates a tabular type for this class
	 * @return a tabular type
	 */	
	public static TabularType getTabularType() {
		try {
			return new TabularType("AgentNameTable", "A table of unique Helios agents", getType(), new String[]{HOST_NAME, AGENT_NAME});
		} catch (Exception e) {
			throw new RuntimeException("Failed to create CompositeType for MetricName", e);
		}		
	}
	
	/**
	 * Returns the last time the agent communicated
	 * @return the last time the agent communicated
	 */
	public long getTouchTime() {
		return touchTime;
	}


	/**
	 * Sets the touch time
	 * @param touchTime
	 */
	public void setTouchTime(long touchTime) {
		this.touchTime = touchTime;
	}
	
	/**
	 * Touches the agent
	 */
	public void touch() {
		touchTime = System.currentTimeMillis();
	}


	/**
	 * Returns the agent's host name
	 * @return the agent's host name
	 */
	public String getHost() {
		return host;
	}


	/**
	 * Returns the agent's application name
	 * @return the agent's application name
	 */
	public String getAgent() {
		return agent;
	}


	/**
	 * Returns the agent's connect time
	 * @return the agent's connect time
	 */
	public long getConnectTime() {
		return connectTime;
	}

}
