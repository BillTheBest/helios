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
package org.helios.collectors.jdbc;


import java.net.URL;

import org.helios.collectors.jdbc.extract.IProcessedResultSet;
import org.w3c.dom.Node;


/**
 * <p>Title: ResultSetMap</p>
 * <p>Description: A syntax parser for defining a subsidiary mapping within a <code>SQLMapping<code>.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ResultSetMap {
	/**	The segment portion of the metric */
	protected String[] metricSegment = null;
	/**	The metric name */
	protected String metricName = null;
	/** The column id the data should come from */
	protected int column = -1;
	/** The tracing counter type */
	protected String counterType = null;
	/**	The attribute name */
	protected String attributeName = null;
	/** Indicates if a CacheResult is applied to this query */
	protected boolean cacheResult = false;
	/** The attribute name within the MBean in which to store the cached result set */
	protected String cacheResultAttributeName = null;
	/** Indicates if a trace is applied to this query */
	protected boolean traceDefined = false;
	/** The URL for the source groovy code to perform the result set post-process */
	protected URL postProcessorURL = null;
	/** The last processing elapsed time for the post processor */
	protected long postProcessorElapsedTime = 0;
	/** Indicates if PreparedStatements and Bind variables should be used */
	protected boolean useBinds = true;
	/** Indicates if the trace should be scoped */
	protected boolean scoped = false;
	/** The scope value */
	protected Object scopeValue = null;
	
	/**
	 * @param rsetMapNode
	 */
	public ResultSetMap(Node rsetMapNode) {
		
	}
	
	/**
	 * Extracts and traces metrics from the processed result set.
	 * @param prs the IProcessedResultSet to extract metrics from.
	 */
	public void trace(IProcessedResultSet prs) {
		
	}
	
	/**
	 * Sample Query Output
	 * 		SCHEMA_NAME, RELNAME, SEQ_SCAN, TUPLE_READS
			"pg_catalog";"pg_class";25268;5697968
			"pg_catalog";"pg_database";12261;61245
			"pg_catalog";"pg_am";8416;8416
			"pg_catalog";"pg_attribute";56;304
			"pg_catalog";"pg_amop";8;40

	 */
	
	/*
	 		
	 */
	
	
	// Tokens:
		// RSET Independent
			// Context NVP
			// Cache Value
			// Spring Bean Value
			// JMX MBean Attribute Value
		// RSET Dependent
			// Col Name
			// Col Value
			// Flat Col Value
	
	
	/*
	 * <Mapping column="1" segment="Server Logins|{0}" metricName="Count" type="CINT"/>
	 * <Mapping column="1" segment="Server Logins|{0}" metricName="Count" type="CINT"/>
	 * <Mapping column="1" segment="09" metricName="HighWaterMark" type="STRING" attributeName="HighWaterMark09" attributeType="String"/>
	 * 
	 			<Bind number="1" source="attribute" type="String" objectName="com.adp.sbs.metrics:service=OCLHighWaterMarks" attributeName="HighWaterMark09"/>
				<Bind number="2" source="attribute" type="String" objectName="com.adp.sbs.metrics:service=OCLHighWaterMarks" attributeName="HighWaterMark09"/>					
				<Bind number="3" source="attribute" type="String" objectName="com.adp.sbs.metrics:service=OCLHighWaterMarks" attributeName="HighWaterMark09"/>
				<Bind number="4" source="attribute" type="String" objectName="com.adp.sbs.metrics:service=OCLHighWaterMarks" attributeName="HighWaterMark09"/>

		<Mapping column="0" segment="" metricName="Total Blocked Sessions" type="CINT"  scoped="true" scopeResetValue="0" />
		
						<CacheResult
							objectName="com.adp.sbs.monitoring:type=CachedResultSets,name=DC1"
							attributeName="BlockingLocks"
							ContainerClass="com.adp.sbs.metrics.tracing.collectors.jdbc.cache.HistoryBufferingCachedResultSet2">
							<attribute name="HistorySize">20</attribute>
						</CacheResult>

					<CacheResult
						objectName="com.adp.sbs.monitoring:type=CachedResultSets,name=DC1"
						attributeName="OracleConnectionsByServer"
						ContainerClass="com.adp.sbs.metrics.tracing.collectors.jdbc.cache.HistoryBufferingCachedResultSet2">
						<attribute name="HistorySize">1</attribute>
					</CacheResult>
					
					
					
					
import java.text.*;

public String[][] postProcess(String[][] input) {
	DecimalFormat df = new DecimalFormat("###,###,###");	
	int rowCnt = 0;	
	input.each() { row ->
		int colCnt = 0;
		row.each() { column ->
			if(!column.getClass().getName().equals("java.lang.String")) {				
				input[rowCnt][colCnt] = "&nbsp;";				
			} else {
				try {
					Number number = df.parse(column);
					input[rowCnt][colCnt] = df.format(number.doubleValue());
				} catch (Exception e) {}
			}
			colCnt++;
		}
		rowCnt++;
	}
	//Thread.currentThread().sleep(200);
	return input;
}					
		

	 */
}
