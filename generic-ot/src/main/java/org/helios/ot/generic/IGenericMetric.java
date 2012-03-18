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
package org.helios.ot.generic;
import java.util.Date;
import java.util.regex.Pattern;
/**
 * <p>Title: IGenericMetric</p>
 * <p>Description: Defines a generic metric value class.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.generic.IGenericMetric</code></p>
 */

public interface IGenericMetric {
	
	/** Regex to split resource */
	public static final Pattern resourcePattern = Pattern.compile("\\|");
	/** Regex to split resource and metric */
	public static final Pattern metricPattern = Pattern.compile(":");
	/** Regex to parse out local name */
	public static final Pattern localPattern = Pattern.compile("\\||:");
	/** Zero segment resource value */	
	public static final String[] EMPTY_RESOURCE =  new String[]{};
	
	/** The map constant name for */
	public static final String AGENT = "agent";
	/** The map constant name for */
	public static final String AVG = "avg";
	/** The map constant name for */
	public static final String COUNT = "count";
	/** The map constant name for */
	public static final String DOMAIN = "domain";
	/** The map constant name for */
	public static final String ENDDATE = "endDate";
	/** The map constant name for */
	public static final String ENDTIME = "endTime";
	/** The map constant name for */
	public static final String FULLNAME = "fullName";
	/** The map constant name for */
	public static final String HOST = "host";
	/** The map constant name for */
	public static final String LOCALNAME = "localName";
	/** The map constant name for */
	public static final String MAX = "max";
	/** The map constant name for */
	public static final String METRICDEF = "metricDef";
	/** The map constant name for */
	public static final String METRICNAME = "metricName";
	/** The map constant name for */
	public static final String MIN = "min";
	/** The map constant name for */
	public static final String PROCESS = "process";
	/** The map constant name for */
	public static final String RESOURCE = "resource";
	/** The map constant name for */
	public static final String SEGMENT = "segment";
	/** The map constant name for */
	public static final String STARTDATE = "startDate";
	/** The map constant name for */
	public static final String STARTTIME = "startTime";
	/** The map constant name for */
	public static final String TYPECODE = "typeCode";

	/**
	 * Returns the value idfentified by the passed property name (case insensitive)
	 * @param propName The property name
	 * @return the value
	 */
	public Object getProperty(String propName);
	
	/**
	 * Returns the metric Def for this instance
	 * @return the metric Def for this instance
	 */
	public GenericMetricDef getMetricDef();
	
	/**
	 * Returns the APM Domain
	 * @return the APM Domain
	 */
	public String getDomain();
	/**
	 * Returns the the host name of the agent 
	 * @return the host name of the agent
	 */
	public String getHost();
	/**
	 * Returns the agent process name
	 * @return the agent process name
	 */
	public String getProcess();
	/**
	 * Returns the agent name
	 * @return the agent name
	 */
	public String getAgent();
	/**
	 * Returns the resource segments
	 * @return the resource segments
	 */
	public String[] getResource();
	/**
	 * Returns the local full metric name which can be used to resubmit a metric.
	 * @return the local full name
	 */
	public String getLocalName();
	/**
	 * Returns the metric name
	 * @return the metric name
	 */
	public String getMetricName();
	/**
	 * Returns the indexed segment from the resource
	 * @param pos The index of the segment
	 * @return the indexed segment from the resource
	 */
	public String getSegment(int pos);
	/**
	 * Returns the average value for the interval
	 * @return the average value for the interval
	 */
	public long getAvg();
	/**
	 * Returns the minimum value for the interval
	 * @return the minimum value for the interval
	 */
	public long getMin();
	/**
	 * Returns the maximum value for the interval
	 * @return the maximum value for the interval
	 */
	public long getMax();
	/**
	 * Returns the number of samples values for this metric
	 * @return the number of samples values for this metric
	 */
	public long getCount();
	/**
	 * Returns the long UTC timestamp of the start of the interval
	 * @return the long UTC timestamp of the start of the interval
	 */
	public long getStartTime();
	/**
	 * Returns the long UTC timestamp of the end of the interval
	 * @return the long UTC timestamp of the end of the interval
	 */
	public long getEndTime();
	/**
	 * Returns the java date of the start of the interval
	 * @return the java date of the start of the interval
	 */
	public Date getStartDate();
	/**
	 * Returns the java date of the end of the interval
	 * @return the java date of the end of the interval
	 */
	public Date getEndDate();
	/**
	 * Returns the metric data type code
	 * @return the metric data type code
	 */
	public int getTypeCode();
	/**
	 * Returns the fully qualified metric name
	 * @return the fully qualified metric name
	 */
	public String getFullName();

}
