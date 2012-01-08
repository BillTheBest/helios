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
package org.helios.tracing;

import java.util.Date;
import java.util.regex.Pattern;

import org.helios.tracing.trace.MetricType;

/**
 * <p>Title: IMetric</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface IMetric {

	/** The delimeter between namespace entries */
	public static final String DELIM = "/";
	/** The delimeter before the metric value */
	public static final String VALUE_DELIM = ":";
	/** The application name property Id */
	public static final String APPLICATION_ID = "org.helios.application.name";
	/** The segment split regex pattern */
	public static final Pattern PARSE_PATTERN = Pattern.compile("/|:");

	public boolean isChanged();

	public void setChanged(boolean changed);

	/**
	 * @return the intervalStartTime
	 */
	public long getIntervalStartTime();

	/**
	 * @return the intervalEndTime
	 */
	public long getIntervalEndTime();

	/**
	 * @return the flushTime
	 */
	public long getFlushTime();

	/**
	 * @return the receivedTime
	 */
	public long getReceivedTime();

	/**
	 * @return the persistTime
	 */
	public long getPersistTime();

	/**
	 * @return the metricName
	 */
	public String getMetricName();

	/**
	 * @return the fullName
	 */
	public String getFullName();

	/**
	 * @return the localName
	 */
	public String getLocalName();

	/**
	 * @return the average
	 */
	public long getAverage();

	/**
	 * @return the high
	 */
	public long getHigh();

	/**
	 * @return the low
	 */
	public long getLow();

	/**
	 * @return the count
	 */
	public long getCount();

	/**
	 * @return the value
	 */
	public Object getValue();

	/**
	 * @return the type
	 */
	public MetricType getType();
	
	public String getTypeName();
	
	public int getTypeCode();

	/**
	 * @return the firstTraceTime
	 */
	public long getFirstTraceTime();

	/**
	 * @return
	 */
	public Date getFirstTraceDate();

	/**
	 * @return the lastTraceTime
	 */
	public long getLastTraceTime();

	/**
	 * Renders the metric in JSON format.
	 * @return A JSON string representing this metric.
	 */
	public String toJSON();

	/**
	 * @return the hostName
	 */
	public String getHostName();

	/**
	 * @return the applicationId
	 */
	public String getApplicationId();

	/**
	 * @param flushTime
	 */
	public void setFlushTime(long flushTime);

	/**
	 * @param receiveTime
	 */
	public void setReceivedTime(long receiveTime);

}