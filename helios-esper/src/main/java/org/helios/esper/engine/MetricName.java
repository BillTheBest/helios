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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.WriteAbortedException;
import java.util.Map;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

import org.apache.log4j.Logger;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.parser.JSONParser;


/**
 * <p>Title: MetricName</p>
 * <p>Description: Encapsulated value object for a unique metric name and type</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.MetricName</code></p>
 */
public class MetricName extends CompositeDataSupport implements Serializable  {
	
	public static final String[] ITEM_NAMES = {"fullName", "metricName", "typeName", "typeCode", "activateTime", "lastReceivedTime"};
	public static final String[] ITEM_DESCS = {"The fully qualified metric name", "The local metric name", "The metric type name", "The metric type code", "The time that first instance of this metric was received", "The last time that an instance of this metric was received"};
	public static final OpenType[] ITEM_TYPES = {SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.LONG, SimpleType.LONG};
	
	protected String fullName = null;
	protected String metricName = null;
	protected String typeName = null;
	protected int typeCode = -1;
	/** The time that first instance of this metric was received */
	protected long activateTime = 0L;
	/** The last time that an instance of this metric was received */
	protected long lastReceivedTime = 0L;
	public static final String FULL_NAME = ITEM_NAMES[0];
	public static final String NAME = ITEM_NAMES[1];	
	public static final String TYPE_NAME = ITEM_NAMES[2];
	public static final String TYPE_CODE = ITEM_NAMES[3];
	public static final String ACTIVATE = ITEM_NAMES[4];
	public static final String LAST = ITEM_NAMES[5];
	public static final Logger log = Logger.getLogger(MetricName.class);
	public static final TabularType METRIC_NAME_TABULAR_TYPE = getTabularType();
	
	/**
	 * Copy Constructor
	 *
	 * @param metricName a <code>MetricName</code> object
	 */
	public MetricName(MetricName metricName) throws OpenDataException { 
		super(getType(), ITEM_NAMES, new Object[]{metricName.fullName, metricName.metricName, metricName.typeName, metricName.typeCode, metricName.activateTime, System.currentTimeMillis()});
	    this.fullName = metricName.fullName;
	    this.metricName = metricName.metricName;
	    this.typeName = metricName.typeName;
	    this.typeCode = metricName.typeCode;
	    this.activateTime = metricName.activateTime;
	    this.lastReceivedTime = (Long)this.get(LAST);
	    
	    
	}

	/**
	 * @param nameValue
	 * @throws OpenDataException
	 */
	public MetricName(Map<String, Object> nameValue) throws OpenDataException {
		super(getType(), ITEM_NAMES, new Object[]{nameValue.get(FULL_NAME).toString(), nameValue.get(NAME).toString(), nameValue.get(TYPE_NAME).toString(), (Integer)nameValue.get(TYPE_CODE), System.currentTimeMillis(), System.currentTimeMillis()});
		fullName = nameValue.get(FULL_NAME).toString();
		metricName = nameValue.get(NAME).toString();
		typeName = nameValue.get(TYPE_NAME).toString();
		typeCode = (Integer)nameValue.get(TYPE_CODE);
		activateTime = (Long)this.get(ACTIVATE);
		lastReceivedTime = (Long)this.get(LAST);
	}
	
	/**
	 * Creates a composite type for this class
	 * @return a composite type
	 */
	public static CompositeType getType() {
		try {
			return new CompositeType("MetricName", "A unique Helios metric name", ITEM_NAMES, ITEM_DESCS, ITEM_TYPES);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create CompositeType for MetricName", e);
		}
	}
	
	/**
	 * Creates a tabular type for this class
	 * @return a tabular type
	 */	
	public static TabularType getTabularType() {
		try {
			return new TabularType("MetricNameTable", "A table of unique Helios Metric name", getType(), new String[]{FULL_NAME});
		} catch (Exception e) {
			throw new RuntimeException("Failed to create CompositeType for MetricName", e);
		}		
	}
	
	
	
	/**
	 * @param fullName
	 * @param metricName
	 * @param typeName
	 * @param typeCode
	 * @throws OpenDataException 
	 */
	public MetricName(String fullName, String metricName, String typeName, int typeCode) throws OpenDataException {
		super(getType(), ITEM_NAMES, new Object[]{fullName, metricName, typeName, typeCode, System.currentTimeMillis(), System.currentTimeMillis()});
		this.fullName = fullName;
		this.metricName = metricName;
		this.typeName = typeName;
		this.typeCode = typeCode;
		activateTime = (Long)this.get(ACTIVATE);
		lastReceivedTime = (Long)this.get(LAST); 
	}
	
	/**
	 * Updates the last received time.
	 */
	public void touch() {
		lastReceivedTime = System.currentTimeMillis();
	}
	
	/**
	 * @return
	 */
	public String toJSON() {
		try {
			return JSONMapper.toJSON(this).render(false);
		} catch (Exception e) {
			throw new RuntimeException("Failed to render MetricName to JSON", e);
		}
	}
	
	
	public static void log(Object message) {
		System.out.println(message);
	}
	
	/**
	 * @param json
	 * @return
	 */
	public static MetricName fromJSON(String json) {
		try {
			JSONParser parser = new JSONParser(new StringReader(json));
			return (MetricName) JSONMapper.toJava (parser.nextValue(), MetricName.class );
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse json for MetricName", e);
		}
	}
	
	/**
	 * @return the fullName
	 */
	public String getFullName() {
		return fullName;
	}
	/**
	 * @return the metricName
	 */
	public String getMetricName() {
		return metricName;
	}
	/**
	 * @return the typeName
	 */
	public String getTypeName() {
		return typeName;
	}
	/**
	 * @return the typeCode
	 */
	public int getTypeCode() {
		return typeCode;
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {
	    final String DELIM = "\n\t";
	    final String CR = "\n";
	    final String VALUE_OPEN = "[";
	    final String VALUE_CLOSE = "]";
	    StringBuilder retValue = new StringBuilder();    
	    retValue.append(CR).append("MetricName ( ")
	        .append(DELIM).append("fullName:").append(VALUE_OPEN).append(this.fullName).append(VALUE_CLOSE)
	        .append(DELIM).append("metricName:").append(VALUE_OPEN).append(this.metricName).append(VALUE_CLOSE)
	        .append(DELIM).append("typeName:").append(VALUE_OPEN).append(this.typeName).append(VALUE_CLOSE)
	        .append(DELIM).append("typeCode:").append(VALUE_OPEN).append(this.typeCode).append(VALUE_CLOSE)
	        .append(CR).append(")");    
	    return retValue.toString();
	}

	/**
	 * @param fullName the fullName to set
	 */
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	/**
	 * @param metricName the metricName to set
	 */
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	/**
	 * @param typeName the typeName to set
	 */
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	/**
	 * @param typeCode the typeCode to set
	 */
	public void setTypeCode(int typeCode) {
		this.typeCode = typeCode;
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fullName == null) ? 0 : fullName.hashCode());
		return result;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetricName other = (MetricName) obj;
		if (fullName == null) {
			if (other.fullName != null)
				return false;
		} else if (!fullName.equals(other.fullName))
			return false;
		return true;
	}
	
	/**
	 * Replaces an instance of this class with a pure CompositeData instance for serialization.
	 * @return a CompositeData instance
	 * @throws ObjectStreamException
	 */
	protected Object writeReplace() throws ObjectStreamException {
		try {
			CompositeDataSupport cds = new CompositeDataSupport(getType(), ITEM_NAMES, new Object[]{fullName, metricName, typeName, typeCode, activateTime, lastReceivedTime});			
			return cds;
		} catch (Exception e) {
			throw new WriteAbortedException("Failed to create a new CompositeDataSupport", e);
		}
	}

	/**
	 * 	The time that first instance of this metric was received
	 * @return the activateTime
	 */
	public long getActivateTime() {
		return activateTime;
	}

	/**
	 * 	The last time that an instance of this metric was received
	 * @return the lastReceivedTime
	 */
	public long getLastReceivedTime() {
		return lastReceivedTime;
	}
}
