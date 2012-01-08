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
import org.helios.tracing.trace.MetricType;

/**
 * <p>Title: Metric</p>
 * <p>Description: TraceModel representation of a unique metric type</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.core.trace.Metric</code></p>
 */
@Store(store="METRIC")
public class Metric implements Serializable {
	/**  */
	private static final long serialVersionUID = -8547884078800143495L;
	/** The unique Helios metric id */
	protected long id;
	/** The metric type code */
	protected int type;
	/** the full metric name */
	protected String fullName;
	/** the metric name (point) */
	protected String name;
	/** the metric name space */
	protected String[] namespace;
	/** The timestamp that this metric was first seen */
	protected Date firstSeen;
	/** The timestamp that this metric was last seen */
	protected Date lastSeen;
	
	/** The sequence name for the metric id */
	public static final String SEQUENCE_KEY = "HELIOS_METRIC_ID";
	/** The metric name delimeter */
	public static final String NAME_DELIM = "/";

	/**
	 * Creates a new Metric 
	 * @param id The designated unique Helios metric Id
	 * @param type The MetricType
	 * @param fullName The full name
	 */
	public Metric(long id, int type, String fullName) {
		this.id = id;
		this.type = type;
		this.fullName = fullName;
		Date dt = new Date();
		firstSeen = dt;
		lastSeen = dt;
		String[] frags = this.fullName.split(NAME_DELIM);
		name = frags[frags.length-1];
		namespace = new String[frags.length-1];
		System.arraycopy(frags, 0, namespace, 0, frags.length-1);		
	}
	
	/**
	 * Creates a new Metric from a result set and the sql <code>SELECT FIRST_SEEN,FULL_NAME,LAST_SEEN,METRIC_ID,NAME,TYPE_ID FROM METRIC</code>. 
	 * @param rset The pre-navigated result set
	 * @param cache not used. Can be null
	 * @throws SQLException 
	 */
	public Metric(ResultSet rset, TraceModelCache cache) throws SQLException {
		int i = 0;		
		firstSeen= new Date(rset.getTimestamp(++i).getTime());
		fullName = rset.getString(++i);
		lastSeen= new Date(rset.getTimestamp(++i).getTime());
		id = rset.getLong(++i);
		name = rset.getString(++i);
		type = rset.getInt(++i);
		cache.putMetric(this);
		cache.addValue("Metric", id);
	}
	

	
	/**
	 * The timestamp that this metric was last seen
	 * @return the lastSeen
	 */
	@StoreField(name="LAST_SEEN", type=Types.TIMESTAMP)
	public Date getLastSeen() {
		return lastSeen;
	}

	/**
	 * Sets the last seen date
	 * @param lastSeen the lastSeen to set
	 */
	public void setLastSeen(Date lastSeen) {
		this.lastSeen = lastSeen;
	}
	/**
	 * The unique Helios metric id 
	 * @return the id
	 */
	@PK
	@StoreField(name="METRIC_ID", type=Types.BIGINT)	
	public long getId() {
		return id;
	}
	
	/**
	 * The metric type code
	 * @return the type
	 */
	@StoreField(name="TYPE_ID", type=Types.INTEGER)
	public int getTypeCode() {
		return type;
	}
	
	/**
	 * Returns the metric type name
	 * @return the metric type name
	 */
	public String getTypeName() {
		return MetricType.typeForCode(type).name();
	}

	/**
	 * Returns the metric type
	 * @return the metric type
	 */
	public MetricType getType() {
		return MetricType.typeForCode(type);
	}

	
	/**
	 * the full metric name
	 * @return the fullName
	 */
	@StoreField(name="FULL_NAME", type=Types.VARCHAR)
	public String getFullName() {
		return fullName;
	}
	/**
	 * the metric name (point)
	 * @return the name
	 */
	@StoreField(name="NAME", type=Types.VARCHAR)
	public String getName() {
		return name;
	}
	/**
	 * The timestamp that this metric was first seen
	 * @return the firstSeen
	 */
	@StoreField(name="FIRST_SEEN", type=Types.TIMESTAMP)
	public Date getFirstSeen() {
		return firstSeen;
	}
	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("Metric [")
	        .append(TAB).append("id = ").append(this.id)
	        .append(TAB).append("type = ").append(this.type)
	        .append(TAB).append("fullName = ").append(this.fullName)
	        .append(TAB).append("name = ").append(this.name)
	        .append(TAB).append("firstSeen = ").append(this.firstSeen)
	        .append(TAB).append("lastSeen = ").append(this.lastSeen)
	        .append("\n]");    
	    return retValue.toString();
	}


	/**
	 * @return the namespace
	 */
	public String[] getNamespace() {
		return namespace;
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
		Metric other = (Metric) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
